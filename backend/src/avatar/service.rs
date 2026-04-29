// avatar/service.rs
//
// 아바타 관련 비즈니스 로직 모음
//
// 역할:
//  - Supabase REST API를 호출해 DB 조회/수정
//  - 핸들러로부터 위임받은 실제 로직 처리
//  - 결과를 DTO로 변환해 핸들러에 반환
//
// Supabase REST API 공통 패턴:
//  - Authorization: Bearer {supabase_secret_key} → service_role 권한 (RLS 우회)
//  - apiKey: {supabase_secret_key} → Supabase API 인증
//  - Prefer: return = representation → INSERT/PATCH 후 변경된 row 반환
//
// 에러 처리: anyhow::Result, anyhow!("메세지"), .context("설명")

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::dto::{
    EquipItemRequest, EquipmentSlotResponse, MintNftRequest, MintNftResponse, OwnedNftResponse,
    SyncOwnedNftsResponse, TransferNftRequest, TransferNftResponse, UserItemResponse,
};
use crate::clients::solana_client;
use crate::state::AppState;

async fn ensure_nft_record_not_reused(
    state: &AppState,
    table: &str,
    nft_mint_address: &str,
    tx_signature: &str,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/{}?or=(nft_mint_address.eq.{},nft_tx_signature.eq.{})&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        table,
        urlencoding::encode(nft_mint_address),
        urlencoding::encode(tx_signature)
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .with_context(|| format!("{} NFT 재사용 여부 조회 요청 실패", table))?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "{} NFT 재사용 여부 조회 실패: {}",
            table,
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<serde_json::Value> = res
        .json()
        .await
        .with_context(|| format!("{} NFT 재사용 여부 역직렬화 실패", table))?;
    if !rows.is_empty() {
        return Err(anyhow!("이미 기록된 NFT mint 또는 트랜잭션 서명입니다"));
    }

    Ok(())
}

async fn get_user_wallet(state: &AppState, user_id: Uuid) -> Result<String> {
    #[derive(Deserialize)]
    struct WalletRow {
        wallet_address: Option<String>,
    }

    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=wallet_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("users wallet_address 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "users wallet_address 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<WalletRow> = res
        .json()
        .await
        .context("users wallet_address 역직렬화 실패")?;
    rows.into_iter()
        .next()
        .and_then(|r| r.wallet_address)
        .filter(|w| !w.trim().is_empty())
        .ok_or_else(|| anyhow!("지갑이 연동된 유저만 NFT 상태를 기록할 수 있습니다"))
}

async fn get_user_wallet_optional(state: &AppState, user_id: Uuid) -> Result<Option<String>> {
    #[derive(Deserialize)]
    struct WalletRow {
        wallet_address: Option<String>,
    }

    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=wallet_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("users wallet_address 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "users wallet_address 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<WalletRow> = res
        .json()
        .await
        .context("users wallet_address 역직렬화 실패")?;

    Ok(rows
        .into_iter()
        .next()
        .and_then(|r| r.wallet_address)
        .filter(|w| !w.trim().is_empty()))
}

// mint_nft
//
/// public.user_items 테이블에서 지정한 row의 NFT 관련 컬럼을 업데이트한다.
///
/// 업데이트 대상 컬럼:
///     - nft_mint_address: 온체인에서 민팅된 Solana NFT mint 주소
///     - is_nft: true (NFT 발행 완료 표시)
///
/// 보안: `user_id=eq.{user_id}` 필터를 함께 걸어 본인 소유 아이템만 수정할 수 있도록 제한한다.
///       다른 유저의 user_item_id를 넘겨도 조건 불일치로 PATCH되지 않는다.
pub async fn mint_nft(
    state: &AppState,
    user_id: Uuid,
    req: MintNftRequest, // { user_item_id }
) -> Result<MintNftResponse> {
    if state.config.solana_admin_keypair.is_empty() {
        return Err(anyhow!(
            "온체인 민팅이 설정되지 않았습니다 (SOLANA_ADMIN_KEYPAIR 누락)"
        ));
    }

    // 1. user_item 조회 — 본인 소유 확인 + avatar_items JOIN (이름, URI)
    #[derive(Deserialize)]
    struct AvatarItemEmbed {
        name: String,
        metadata_uri: Option<String>,
        image_url: String,
    }

    #[derive(Deserialize)]
    struct UserItemRaw {
        is_nft: Option<bool>,
        item_master: AvatarItemEmbed,
    }

    let item_url = format!(
        "{}/rest/v1/user_inventory?id=eq.{}&user_id=eq.{}&select=is_nft,item_master(name,metadata_uri,image_url)",
        state.config.supabase_url.trim_end_matches('/'),
        req.user_item_id,
        user_id,
    );

    let item_res = state
        .http_client
        .get(&item_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_items 조회 요청 실패")?;

    if !item_res.status().is_success() {
        return Err(anyhow!(
            "user_items 조회 실패: {}",
            item_res.text().await.unwrap_or_default()
        ));
    }

    let item = item_res
        .json::<Vec<UserItemRaw>>()
        .await
        .context("user_items 역직렬화 실패")?
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("아이템을 찾을 수 없거나 본인 소유가 아닙니다"))?;

    if item.is_nft.unwrap_or(false) {
        return Err(anyhow!("이미 NFT로 민팅된 아이템입니다"));
    }

    // 2. 유저 지갑 조회
    let wallet_address = get_user_wallet(state, user_id).await?;

    // 3. 온체인 NFT 민팅
    // item_seed: user_item_id(UUID)를 하이픈 없는 형태로 사용 → 32바이트 이하 보장
    let item_seed = req.user_item_id.simple().to_string();
    let nft_uri = item
        .item_master
        .metadata_uri
        .as_deref()
        .filter(|v: &&str| !v.trim().is_empty())
        .unwrap_or(&item.item_master.image_url);

    let (tx_signature, nft_mint_address) = solana_client::mint_avatar_nft_to_user(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_admin_keypair,
        &wallet_address,
        &state.config.solana_program_id,
        &item_seed,
        &item.item_master.name,
        "SPTA",
        nft_uri,
    )
    .await
    .context("NFT 온체인 민팅 실패")?;

    // 4. confirmed 상태 확인 — sendTransaction은 즉시 반환이므로 DB 기록 전에 확인
    // admin이 서명한 tx이므로 confirmed 이상이면 충분 (finalized는 ~30초로 UX 저하)
    if let Err(e) = solana_client::check_signature_confirmed(
        &state.config.solana_rpc_url,
        &state.http_client,
        &tx_signature,
    )
    .await
    {
        // 온체인은 성공했지만 확인에 실패한 경우 — 복구 정보를 로그로 남김
        tracing::error!(
            "[NFT 민팅 확인 실패] 온체인 민팅은 성공했으나 confirmed 확인 불가. \
             수동 DB 동기화 필요. \
             user_id={} user_item_id={} nft_mint_address={} tx_signature={} err={}",
            user_id,
            req.user_item_id,
            nft_mint_address,
            tx_signature,
            e
        );
        return Err(anyhow!("NFT 민팅 트랜잭션 확인 실패: {}", e));
    }

    // 5. DB 업데이트 — is_nft=true, mint 주소, tx 서명 기록
    let url = format!(
        "{}/rest/v1/user_inventory?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.user_item_id,
        user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        nft_mint_address: String,
        is_nft: bool,
        nft_tx_signature: String,
        minted_to_wallet: String,
        collection_mint: Option<String>,
    }

    let collection_mint = {
        let v = state.config.solana_avatar_collection_mint.trim();
        if v.is_empty() {
            None
        } else {
            Some(v.to_string())
        }
    };

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&PatchPayload {
            nft_mint_address: nft_mint_address.clone(),
            is_nft: true,
            nft_tx_signature: tx_signature.clone(),
            minted_to_wallet: wallet_address.clone(),
            collection_mint,
        })
        .send()
        .await
        .context("user_items PATCH 요청 실패")?;

    if !res.status().is_success() {
        // 온체인 민팅 성공 + DB 실패 — 재시도 시 동일 PDA init 충돌 가능
        tracing::error!(
            "[NFT DB 동기화 실패] 온체인 민팅은 성공했으나 DB 업데이트 실패. \
             수동 동기화 필요. \
             user_id={} user_item_id={} nft_mint_address={} tx_signature={}",
            user_id,
            req.user_item_id,
            nft_mint_address,
            tx_signature
        );
        return Err(anyhow!(
            "user_items PATCH 실패 (온체인은 성공): {}",
            res.text().await.unwrap_or_default()
        ));
    }

    Ok(MintNftResponse {
        message: "NFT 민팅 완료".to_string(),
        nft_mint_address,
    })
}

// transfer_nft
//
/// public.avatars 테이블에서 지정한 row의 NFT 관련 컬럼을 업데이트한다.
///
/// 업데이트 대상 컬럼:
///     - nft_mint_address: 전송 완료된 Solana NFT mint 주소
///     - is_nft: true (NFT 발행/전송 완료 표시)
///
/// 보안: `user_id=eq.{user_id}` 필터를 함꼐 걸어 본인 소유 아바타만 수정할 수 있도록 제한한다.
pub async fn transfer_nft(
    state: &AppState,
    user_id: Uuid,           // JWT에서 추출한 현재 로그인 유저 UUID
    req: TransferNftRequest, // { avatar_id, nft_mint_address }
) -> Result<TransferNftResponse> {
    let tx_signature = req
        .tx_signature
        .as_deref()
        .ok_or_else(|| anyhow!("tx_signature는 필수입니다"))?;
    let user_wallet = get_user_wallet(state, user_id).await?;
    solana_client::verify_program_instruction_tx(
        &state.config.solana_rpc_url,
        &state.config.helius_api_key,
        &state.http_client,
        tx_signature,
        &state.config.solana_program_id,
        "transfer_avatar_nft",
        &[user_wallet.as_str(), req.nft_mint_address.as_str()],
        None,
        None,
    )
    .await
    .context("NFT 전송 트랜잭션 검증 실패")?;
    ensure_nft_record_not_reused(state, "avatars", &req.nft_mint_address, tx_signature).await?;

    // Supabase PostgREST URL 구성
    // 필터: id=eq.{avatar_id} AND user_id=eq.{user_id}
    //  → 본인 소유 아바타만 수정 가능 (보안 필터)
    let url = format!(
        "{}/rest/v1/avatars?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.avatar_id,
        user_id,
    );

    // PATCH 요청 바디: 변경할 컬럼만 포함
    #[derive(Serialize)]
    struct PatchPayload {
        nft_mint_address: String, // 온체인 NFT mint 주소
        is_nft: bool,             // NFT 발행 완료 플래그 → true로 고정
        nft_tx_signature: String,
    }

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload {
            nft_mint_address: req.nft_mint_address.clone(),
            is_nft: true,
            nft_tx_signature: tx_signature.to_string(),
        })
        .send()
        .await
        .context("avatars PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("avatars PATCH 실패: {}", body));
    }

    Ok(TransferNftResponse {
        message: "NFT 전송 완료".to_string(),
        nft_mint_address: req.nft_mint_address,
    })
}

// get_user_items
//
/// 유저가 보유한 꾸미기 아이템 전체를 아이템 마스터 정보와 함께 조회한다.
///
/// PostgREST embedding(JOIN) 방식:
///     public.user_items.item_id → public.item_master.id
///
/// 쿼리 파라미터:
///     select=*.avatar_items(name,image_url,category)
///     - *: user_items의 모든 컬럼
///     - avatar_items(): FK(item_id)로 연결된 avatar_items에서 지정 컬럼만 가져옴
///
/// PostgREST 응답 예시:
/// [
///  {
///     "id": "...", "user_id": "...", "item_id": "...", "is_equipped": false, "is_nft": false, "nft_mint_address": null,
///     "acquired_at": "...",
///     "avatar_items": { ← 테이블명이 그대로 키가 됨
///         "name": "골든 프레임",
///         "image_url": "https//...",
///         "category": "frame"
///         }
///    }
/// ]
pub async fn get_user_items(
    state: &AppState,
    user_id: Uuid, // JWT에서 추출한 현재 로그인 유저 UUID
) -> Result<Vec<UserItemResponse>> {
    // PostgREST embedding URL:
    //  user_items?user_id=eq.{user_id} → 본인 아이템만 필터
    //  &select=*.avatar_items(...) → avatar_items 테이블 JOIN
    let url = format!(
        "{}/rest/v1/user_inventory?user_id=eq.{}&select=*,item_master(name,image_url,metadata_uri,category,visual_parts)",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    // PostgREST 응답 역직렬화용 내부 구조체
    // UserItemResponse(dto)와 달리 avatar_items가 중첩 객체로 들어오므로 별도 Raw 구조체로 받은 뒤 매핑한다.

    /// PostgREST embedding으로 함께 오는 avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct AvatarItemEmbed {
        name: String,
        image_url: String,
        metadata_uri: Option<String>,
        category: String,
        visual_parts: Option<serde_json::Value>,
    }

    /// PostgREST 응답 원형: user_items 컬럼 + avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct UserItemRaw {
        id: Uuid,
        item_id: Uuid,
        is_equipped: Option<bool>,
        is_nft: Option<bool>,
        nft_mint_address: Option<String>,
        minted_to_wallet: Option<String>,
        collection_mint: Option<String>,
        acquired_at: Option<DateTime<Utc>>,
        item_master: AvatarItemEmbed,
    }

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_items SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_items SELECT 실패: {}", body));
    }

    // Vec<UserItemRaw> 역직렬화
    let raw: Vec<UserItemRaw> = res.json().await.context("user_items 역직렬화 실패")?;

    // UserItemRaw → UserItemResponse 매핑
    // avatar_items 중첩 객체의 필드를 flat하게 꺼내 ResponseDTO에 채운다.
    let items = raw
        .into_iter()
        .map(|r| UserItemResponse {
            id: r.id,
            item_id: r.item_id,
            is_equipped: r.is_equipped,
            is_nft: r.is_nft,
            nft_mint_address: r.nft_mint_address,
            minted_to_wallet: r.minted_to_wallet,
            collection_mint: r.collection_mint,
            acquired_at: r.acquired_at,
            name: r.item_master.name,
            image_url: r.item_master.image_url,
            visual_parts: r.item_master.visual_parts,
            metadata_uri: r.item_master.metadata_uri,
            slot_name: Some(r.item_master.category.clone()),
            category: r.item_master.category,
        })
        .collect();
    Ok(items)
}

// equip_item
//
// user_equipment 테이블에 장착 정보를 upsert한다.
// (user_id, slot_name) PK 기준 — 같은 슬롯에 다시 장착하면 교체됨.
//
// 보안:
//   inventory_id가 실제로 본인(user_id) 소유인지 먼저 확인한다.
//   다른 유저의 inventory_id를 넘겨도 소유권 체크에서 차단된다.
pub async fn equip_item(state: &AppState, user_id: Uuid, req: EquipItemRequest) -> Result<()> {
    // 1. inventory_id가 본인 소유인지 확인
    let check_url = format!(
        "{}/rest/v1/user_inventory?id=eq.{}&user_id=eq.{}&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        req.inventory_id,
        user_id,
    );

    let check_res = state
        .http_client
        .get(&check_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_items 소유권 확인 요청 실패")?;

    if !check_res.status().is_success() {
        return Err(anyhow!(
            "user_items 소유권 확인 실패: {}",
            check_res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<serde_json::Value> = check_res
        .json()
        .await
        .context("user_items 소유권 확인 파싱 실패")?;
    if rows.is_empty() {
        return Err(anyhow!("해당 아이템이 없거나 본인 소유가 아닙니다"));
    }

    let previous_inventory_id = find_equipped_inventory_id(state, user_id, &req.slot_name).await?;

    // 2. user_equipment upsert
    // on_conflict=user_id,slot_name → 같은 슬롯이면 inventory_id, equipped_at 교체
    let upsert_url = format!(
        "{}/rest/v1/user_equipment?on_conflict=user_id,slot_name",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct UpsertPayload {
        user_id: Uuid,
        slot_name: String,
        inventory_id: Uuid,
        is_visible: bool,
    }

    let res = state
        .http_client
        .post(&upsert_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .json(&UpsertPayload {
            user_id,
            slot_name: req.slot_name,
            inventory_id: req.inventory_id,
            is_visible: true,
        })
        .send()
        .await
        .context("user_equipment upsert 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_equipment upsert 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    if let Some(previous_id) = previous_inventory_id {
        if previous_id != req.inventory_id {
            update_inventory_equipped(state, user_id, previous_id, false).await?;
        }
    }
    update_inventory_equipped(state, user_id, req.inventory_id, true).await?;

    Ok(())
}

// unequip_item
//
// 지정한 슬롯의 inventory_id를 NULL로 설정 (슬롯 비우기).
// 행 자체는 남겨두고 inventory_id만 NULL 처리한다.
pub async fn unequip_item(state: &AppState, user_id: Uuid, slot_name: &str) -> Result<()> {
    let previous_inventory_id = find_equipped_inventory_id(state, user_id, slot_name).await?;

    let url = format!(
        "{}/rest/v1/user_equipment?user_id=eq.{}&slot_name=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        slot_name,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        inventory_id: Option<Uuid>,
    }

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&PatchPayload { inventory_id: None })
        .send()
        .await
        .context("user_equipment unequip 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_equipment unequip 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    if let Some(previous_id) = previous_inventory_id {
        update_inventory_equipped(state, user_id, previous_id, false).await?;
    }

    Ok(())
}

async fn find_equipped_inventory_id(
    state: &AppState,
    user_id: Uuid,
    slot_name: &str,
) -> Result<Option<Uuid>> {
    let url = format!(
        "{}/rest/v1/user_equipment?user_id=eq.{}&slot_name=eq.{}&select=inventory_id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        slot_name,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_equipment 현재 장착 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_equipment 현재 장착 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    #[derive(Deserialize)]
    struct EquipmentRow {
        inventory_id: Option<Uuid>,
    }

    let rows: Vec<EquipmentRow> = res
        .json()
        .await
        .context("user_equipment 현재 장착 조회 파싱 실패")?;
    Ok(rows.into_iter().next().and_then(|row| row.inventory_id))
}

async fn update_inventory_equipped(
    state: &AppState,
    user_id: Uuid,
    inventory_id: Uuid,
    is_equipped: bool,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/user_inventory?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        inventory_id,
        user_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({ "is_equipped": is_equipped }))
        .send()
        .await
        .context("user_inventory is_equipped UPDATE 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_inventory is_equipped UPDATE 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    Ok(())
}

// get_equipment
//
// 유저의 전체 장착 현황을 슬롯별로 조회한다.
// user_equipment → user_items → avatar_items 순서로 JOIN하여
// 슬롯별 아이템 정보(name, category, visual_parts 등)를 함께 반환한다.
pub async fn get_equipment(state: &AppState, user_id: Uuid) -> Result<Vec<EquipmentSlotResponse>> {
    // PostgREST embedding:
    //   user_equipment.inventory_id → user_items.id (FK)
    //   user_items.item_id          → avatar_items.id (FK)
    let url = format!(
        "{}/rest/v1/user_equipment?user_id=eq.{}&select=slot_name,inventory_id,equipped_at,is_visible,user_inventory(id,is_nft,nft_mint_address,item_master(name,category,visual_parts))",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    #[derive(Deserialize)]
    struct AvatarItemEmbed {
        name: String,
        category: String,
        visual_parts: Option<serde_json::Value>,
    }

    #[derive(Deserialize)]
    struct UserItemEmbed {
        id: Uuid,
        is_nft: Option<bool>,
        nft_mint_address: Option<String>,
        item_master: Option<AvatarItemEmbed>,
    }

    #[derive(Deserialize)]
    struct EquipmentRaw {
        slot_name: String,
        inventory_id: Option<Uuid>,
        equipped_at: Option<DateTime<Utc>>,
        is_visible: bool,
        user_inventory: Option<UserItemEmbed>,
    }

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_equipment SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_equipment SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let raw: Vec<EquipmentRaw> = res.json().await.context("user_equipment 역직렬화 실패")?;

    let slots = raw
        .into_iter()
        .map(|r| {
            let item = r.user_inventory;
            EquipmentSlotResponse {
                slot_name: r.slot_name,
                inventory_id: r.inventory_id,
                is_visible: r.is_visible,
                equipped_at: r.equipped_at,
                name: item
                    .as_ref()
                    .and_then(|i| i.item_master.as_ref())
                    .map(|a| a.name.clone()),
                category: item
                    .as_ref()
                    .and_then(|i| i.item_master.as_ref())
                    .map(|a| a.category.clone()),
                visual_parts: item
                    .as_ref()
                    .and_then(|i| i.item_master.as_ref())
                    .and_then(|a| a.visual_parts.clone()),
                is_nft: item.as_ref().and_then(|i| i.is_nft),
                nft_mint_address: item.as_ref().and_then(|i| i.nft_mint_address.clone()),
            }
        })
        .collect();

    Ok(slots)
}

pub async fn get_owned_nfts(state: &AppState, user_id: Uuid) -> Result<Vec<OwnedNftResponse>> {
    let wallet_address = match get_user_wallet_optional(state, user_id).await? {
        Some(wallet) => wallet,
        None => return Ok(Vec::new()),
    };

    let collection_mint = state.config.solana_avatar_collection_mint.trim();
    if collection_mint.is_empty() {
        return Ok(Vec::new());
    }

    let assets = solana_client::get_collection_assets_by_owner(
        &state.config.solana_rpc_url,
        &state.http_client,
        &wallet_address,
        collection_mint,
    )
    .await
    .context("컬렉션 NFT 조회 실패")?;

    let mut owned = Vec::with_capacity(assets.len());
    for asset in assets {
        let mint_address = asset["id"].as_str().unwrap_or_default().to_string();
        let metadata_uri = asset["content"]["json_uri"].as_str().map(str::to_string);
        let fallback_name = asset["content"]["metadata"]["name"]
            .as_str()
            .unwrap_or("Unknown NFT")
            .to_string();
        let fallback_image = asset["content"]["links"]["image"]
            .as_str()
            .map(str::to_string);

        #[derive(Deserialize)]
        struct AvatarItemLookup {
            id: Uuid,
            name: String,
            category: String,
            image_url: String,
            metadata_uri: Option<String>,
        }

        let avatar_item = if let Some(uri) = metadata_uri.as_deref() {
            let lookup_url = format!(
                "{}/rest/v1/item_master?metadata_uri=eq.{}&select=id,name,category,image_url,metadata_uri&limit=1",
                state.config.supabase_url.trim_end_matches('/'),
                urlencoding::encode(uri)
            );

            let lookup_res = state
                .http_client
                .get(&lookup_url)
                .header(
                    "Authorization",
                    format!("Bearer {}", state.config.supabase_secret_key),
                )
                .header("apikey", &state.config.supabase_secret_key)
                .send()
                .await
                .context("avatar_items metadata_uri 조회 요청 실패")?;

            if lookup_res.status().is_success() {
                lookup_res
                    .json::<Vec<AvatarItemLookup>>()
                    .await
                    .context("avatar_items metadata_uri 역직렬화 실패")?
                    .into_iter()
                    .next()
            } else {
                None
            }
        } else {
            None
        };

        owned.push(OwnedNftResponse {
            mint_address,
            item_id: avatar_item.as_ref().map(|item| item.id),
            name: avatar_item
                .as_ref()
                .map(|item| item.name.clone())
                .unwrap_or(fallback_name),
            category: avatar_item.as_ref().map(|item| item.category.clone()),
            image_url: avatar_item
                .as_ref()
                .map(|item| Some(item.image_url.clone()))
                .unwrap_or(fallback_image),
            metadata_uri: avatar_item
                .as_ref()
                .and_then(|item| item.metadata_uri.clone())
                .or(metadata_uri),
        });
    }

    Ok(owned)
}

pub async fn sync_owned_nfts(state: &AppState, user_id: Uuid) -> Result<SyncOwnedNftsResponse> {
    let wallet_address = match get_user_wallet_optional(state, user_id).await? {
        Some(wallet) => wallet,
        None => {
            return Ok(SyncOwnedNftsResponse {
                synced_count: 0,
                skipped_count: 0,
            });
        }
    };

    let collection_mint = state.config.solana_avatar_collection_mint.trim();
    if collection_mint.is_empty() {
        return Ok(SyncOwnedNftsResponse {
            synced_count: 0,
            skipped_count: 0,
        });
    }

    let assets = solana_client::get_collection_assets_by_owner(
        &state.config.solana_rpc_url,
        &state.http_client,
        &wallet_address,
        collection_mint,
    )
    .await
    .context("컬렉션 NFT 동기화 조회 실패")?;

    #[derive(Deserialize)]
    struct ItemMasterLookup {
        id: Uuid,
    }

    #[derive(Deserialize)]
    struct InventoryLookup {
        id: Uuid,
        user_id: Uuid,
    }

    let mut synced_count = 0usize;
    let mut skipped_count = 0usize;

    for asset in assets {
        let mint_address = match asset["id"].as_str().filter(|v| !v.trim().is_empty()) {
            Some(value) => value.to_string(),
            None => {
                skipped_count += 1;
                continue;
            }
        };
        let metadata_uri = match asset["content"]["json_uri"]
            .as_str()
            .filter(|v| !v.trim().is_empty())
        {
            Some(value) => value.to_string(),
            None => {
                skipped_count += 1;
                continue;
            }
        };
        let asset_name = asset["content"]["metadata"]["name"]
            .as_str()
            .filter(|v| !v.trim().is_empty())
            .map(str::to_string);

        let existing_url = format!(
            "{}/rest/v1/user_inventory?nft_mint_address=eq.{}&select=id,user_id&limit=1",
            state.config.supabase_url.trim_end_matches('/'),
            urlencoding::encode(&mint_address)
        );

        let existing_res = state
            .http_client
            .get(&existing_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .send()
            .await
            .context("user_inventory NFT 기존 기록 조회 요청 실패")?;

        if !existing_res.status().is_success() {
            return Err(anyhow!(
                "user_inventory NFT 기존 기록 조회 실패: {}",
                existing_res.text().await.unwrap_or_default()
            ));
        }

        let existing_rows: Vec<InventoryLookup> = existing_res
            .json()
            .await
            .context("user_inventory NFT 기존 기록 역직렬화 실패")?;
        if let Some(existing) = existing_rows.first() {
            if existing.user_id != user_id {
                tracing::warn!(
                    "온체인 NFT 소유자와 DB 소유자가 다릅니다. 온체인 소유자 기준으로 DB 소유권을 보정합니다. mint={} db_inventory_id={} db_user_id={} current_user_id={}",
                    mint_address,
                    existing.id,
                    existing.user_id,
                    user_id
                );

                let update_url = format!(
                    "{}/rest/v1/user_inventory?id=eq.{}",
                    state.config.supabase_url.trim_end_matches('/'),
                    existing.id
                );
                let update_res = state
                    .http_client
                    .patch(&update_url)
                    .header(
                        "Authorization",
                        format!("Bearer {}", state.config.supabase_secret_key),
                    )
                    .header("apikey", &state.config.supabase_secret_key)
                    .header("Prefer", "return=minimal")
                    .json(&serde_json::json!({
                        "user_id": user_id,
                        "is_equipped": false,
                        "minted_to_wallet": wallet_address,
                        "collection_mint": collection_mint,
                    }))
                    .send()
                    .await
                    .context("user_inventory NFT 소유권 보정 PATCH 요청 실패")?;

                if !update_res.status().is_success() {
                    return Err(anyhow!(
                        "user_inventory NFT 소유권 보정 PATCH 실패: {}",
                        update_res.text().await.unwrap_or_default()
                    ));
                }

                let listing_update_url = format!(
                    "{}/rest/v1/market_listings?item_id=eq.{}&status=eq.active",
                    state.config.supabase_url.trim_end_matches('/'),
                    existing.id
                );
                let listing_update_res = state
                    .http_client
                    .patch(&listing_update_url)
                    .header(
                        "Authorization",
                        format!("Bearer {}", state.config.supabase_secret_key),
                    )
                    .header("apikey", &state.config.supabase_secret_key)
                    .header("Prefer", "return=minimal")
                    .json(&serde_json::json!({
                        "status": "sold",
                        "sold_at": Utc::now().to_rfc3339(),
                    }))
                    .send()
                    .await
                    .context("market_listings NFT 소유권 보정 상태 PATCH 요청 실패")?;

                if !listing_update_res.status().is_success() {
                    tracing::error!(
                        "market_listings NFT 소유권 보정 상태 PATCH 실패: {}",
                        listing_update_res.text().await.unwrap_or_default()
                    );
                }

                synced_count += 1;
                continue;
            }
            skipped_count += 1;
            continue;
        }

        let item_by_uri_url = format!(
            "{}/rest/v1/item_master?metadata_uri=eq.{}&select=id&limit=1",
            state.config.supabase_url.trim_end_matches('/'),
            urlencoding::encode(&metadata_uri)
        );

        let item_res = state
            .http_client
            .get(&item_by_uri_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .send()
            .await
            .context("item_master NFT metadata_uri 조회 요청 실패")?;

        if !item_res.status().is_success() {
            return Err(anyhow!(
                "item_master NFT metadata_uri 조회 실패: {}",
                item_res.text().await.unwrap_or_default()
            ));
        }

        let mut item_id = item_res
            .json::<Vec<ItemMasterLookup>>()
            .await
            .context("item_master NFT metadata_uri 역직렬화 실패")?
            .into_iter()
            .next()
            .map(|row| row.id);

        if item_id.is_none() {
            if let Some(name) = asset_name.as_deref() {
                let item_by_name_url = format!(
                    "{}/rest/v1/item_master?name=eq.{}&select=id&limit=1",
                    state.config.supabase_url.trim_end_matches('/'),
                    urlencoding::encode(name)
                );
                let item_by_name_res = state
                    .http_client
                    .get(&item_by_name_url)
                    .header(
                        "Authorization",
                        format!("Bearer {}", state.config.supabase_secret_key),
                    )
                    .header("apikey", &state.config.supabase_secret_key)
                    .send()
                    .await
                    .context("item_master NFT name 조회 요청 실패")?;

                if !item_by_name_res.status().is_success() {
                    return Err(anyhow!(
                        "item_master NFT name 조회 실패: {}",
                        item_by_name_res.text().await.unwrap_or_default()
                    ));
                }

                item_id = item_by_name_res
                    .json::<Vec<ItemMasterLookup>>()
                    .await
                    .context("item_master NFT name 역직렬화 실패")?
                    .into_iter()
                    .next()
                    .map(|row| row.id);
            }
        }

        let item_id = match item_id {
            Some(value) => value,
            None => {
                tracing::warn!(
                    "온체인 NFT와 매칭되는 item_master가 없습니다. mint={} metadata_uri={} name={:?}",
                    mint_address,
                    metadata_uri,
                    asset_name
                );
                skipped_count += 1;
                continue;
            }
        };

        let insert_url = format!(
            "{}/rest/v1/user_inventory",
            state.config.supabase_url.trim_end_matches('/')
        );
        let insert_res = state
            .http_client
            .post(&insert_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .header("Prefer", "return=minimal")
            .json(&serde_json::json!({
                "user_id": user_id,
                "item_id": item_id,
                "is_equipped": false,
                "is_nft": true,
                "nft_mint_address": mint_address,
                "nft_tx_signature": null,
                "minted_to_wallet": wallet_address,
                "collection_mint": collection_mint,
            }))
            .send()
            .await
            .context("user_inventory NFT 동기화 INSERT 요청 실패")?;

        if !insert_res.status().is_success() {
            return Err(anyhow!(
                "user_inventory NFT 동기화 INSERT 실패: {}",
                insert_res.text().await.unwrap_or_default()
            ));
        }

        synced_count += 1;
    }

    Ok(SyncOwnedNftsResponse {
        synced_count,
        skipped_count,
    })
}
