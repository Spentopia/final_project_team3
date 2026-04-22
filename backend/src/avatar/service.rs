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
    MintNftRequest, MintNftResponse, TransferNftRequest, TransferNftResponse, UserItemResponse,
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
        avatar_items: AvatarItemEmbed,
    }

    let item_url = format!(
        "{}/rest/v1/user_items?id=eq.{}&user_id=eq.{}&select=is_nft,avatar_items(name,metadata_uri,image_url)",
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
        .avatar_items
        .metadata_uri
        .as_deref()
        .filter(|v| !v.trim().is_empty())
        .unwrap_or(&item.avatar_items.image_url);

    let (tx_signature, nft_mint_address) = solana_client::mint_avatar_nft_to_user(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_admin_keypair,
        &wallet_address,
        &state.config.solana_program_id,
        &item_seed,
        &item.avatar_items.name,
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
        "{}/rest/v1/user_items?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.user_item_id,
        user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        nft_mint_address: String,
        is_nft: bool,
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
        .header("Prefer", "return=minimal")
        .json(&PatchPayload {
            nft_mint_address: nft_mint_address.clone(),
            is_nft: true,
            nft_tx_signature: tx_signature.clone(),
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
///     public.user_items.item_id → public.avatar_items.id
///
/// 쿼리 파라미터:
///     select=*.avatar_items(name,image_url,category,rarity)
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
///         "category": "frame",
///         "rarity"': "epic
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
        "{}/rest/v1/user_items?user_id=eq.{}&select=*,avatar_items(name,image_url,metadata_uri,unity_asset_key,category,rarity)",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    // PostgREST 응답 역직렬화용 내부 구조체
    // UserItemResponse(dto)와 달리 avatar_items가 중첩 객체로 들어오므로 별도 Raw 구조체로 받은 뒤 매핑한다.

    /// PostgREST embedding으로 함께 오는 avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct AvatarItemEmbed {
        name: String,                    // 아이템 이름 (예: "골든 프레임")
        image_url: String,               // 아이템 이미지 URL
        metadata_uri: Option<String>,    // NFT metadata JSON URI
        unity_asset_key: Option<String>, // Unity Addressable/Prefab key
        category: String,                // 카테고리 (background / frame / effect / motion)
        rarity: String,                  // 희귀도
    }

    /// PostgREST 응답 원형: user_items 컬럼 + avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct UserItemRaw {
        id: Uuid,
        user_id: Uuid,
        item_id: Uuid,                      // avatar_items FK
        is_equipped: Option<bool>,          // 현재 장착 여부
        is_nft: Option<bool>,               // NFT 발행 여부
        nft_mint_address: Option<String>,   // Solana NFT mint 주소 (없으면 null)
        acquired_at: Option<DateTime<Utc>>, // 획득 시각
        avatar_items: AvatarItemEmbed,      // JOIN 결과 (중첩 객체)
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
            acquired_at: r.acquired_at,
            // avatar_items 중첩 객체에서 flat하게 꺼냄
            name: r.avatar_items.name,
            image_url: r.avatar_items.image_url,
            metadata_uri: r.avatar_items.metadata_uri,
            unity_asset_key: r.avatar_items.unity_asset_key,
            category: r.avatar_items.category,
            rarity: r.avatar_items.rarity,
        })
        .collect();
    Ok(items)
}
