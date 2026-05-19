// market/service.rs
//
// 마켓 관련 비즈니스 로직 모음
//
// 역할:
//  - Supabase REST API를 호출해 DB 조회/수정
//  - 수수료 계산 등 비즈니스 규칙 적용
//  - 결과를 DTO로 변환해 핸들러에 반환
//
// ※ DB 트리거 활용 (handle_market_purchase):
//  SPT 잔액은 온체인 ATA가 source of truth다.
//  public.market_transactions INSERT 시 DB 트리거는 거래 기록/리스팅 상태/아이템 소유권만 동기화한다.
//  users.spt_balance는 캐시 용도이므로 마켓 구매 성공/실패 판정에 사용하지 않는다.
//
// ※ PostgREST embedding(!kf 문법):
//  FK가 여러 개일 때 어느 FK로 JOIN할지 명시해야 한다.
//  예: users!seller_id(nickname) → market_listings.seller_id FK를 통해 users 테이블 JOIN
//  명시하지 않으면 PostgREST가 ambigous 에러를 뱉는다.

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{
    dto::{CreateListingRequest, ListingResponse, PurchaseRequest, TransactionResponse},
    model::MarketListing,
};
use crate::clients::solana_client;
use crate::state::AppState;

// fetch_listing_response / get_listings 공용 내부 역직렬화 구조체

#[derive(Deserialize)]
struct SellerEmbed {
    nickname: Option<String>,
    wallet_address: Option<String>,
}

#[derive(Deserialize)]
struct AvatarItemEmbed {
    name: String,
    image_url: String,
    category: String,
}

#[derive(Deserialize)]
struct UserItemEmbed {
    item_master: AvatarItemEmbed,
    nft_mint_address: Option<String>,
}

#[derive(Deserialize)]
struct ListingRaw {
    id: Uuid,
    seller_id: Uuid,
    item_id: Uuid,
    price_spt: i32,
    status: Option<String>,
    escrow_address: Option<String>,
    listed_at: Option<DateTime<Utc>>,
    users: SellerEmbed,
    user_inventory: UserItemEmbed,
}

#[derive(Debug, Deserialize)]
struct UserWalletRow {
    wallet_address: Option<String>,
}

#[derive(Debug, Deserialize)]
struct UserItemNftRow {
    is_nft: Option<bool>,
    nft_mint_address: Option<String>,
}

async fn repair_pending_onchain_listings(state: &AppState) -> Result<()> {
    #[derive(Deserialize)]
    struct PendingListingInventoryRow {
        nft_mint_address: Option<String>,
    }

    #[derive(Deserialize)]
    struct PendingListingSellerRow {
        wallet_address: Option<String>,
    }

    #[derive(Deserialize)]
    struct PendingListingRow {
        id: Uuid,
        price_spt: i32,
        user_inventory: PendingListingInventoryRow,
        users: PendingListingSellerRow,
    }

    let url = format!(
        "{}/rest/v1/market_listings?status=eq.pending_onchain&select=id,price_spt,user_inventory!item_id(nft_mint_address),users!seller_id(wallet_address)&limit=20",
        state.config.supabase_url.trim_end_matches('/'),
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
        .context("pending_onchain market_listings 조회 요청 실패")?;

    if !res.status().is_success() {
        tracing::warn!(
            "pending_onchain market_listings 조회 실패: {}",
            res.text().await.unwrap_or_default()
        );
        return Ok(());
    }

    let rows: Vec<PendingListingRow> = res
        .json()
        .await
        .context("pending_onchain market_listings 역직렬화 실패")?;

    for row in rows {
        let Some(seller_wallet) = row.users.wallet_address.as_deref() else {
            continue;
        };
        let Some(nft_mint) = row.user_inventory.nft_mint_address.as_deref() else {
            continue;
        };

        let listing_pda = match solana_client::derive_listing_address(
            seller_wallet,
            nft_mint,
            &state.config.solana_program_id,
        ) {
            Ok(value) => value,
            Err(err) => {
                tracing::warn!("pending listing PDA 계산 실패: listing_id={} err={}", row.id, err);
                continue;
            }
        };
        let escrow_address = match solana_client::derive_escrow_address(
            &listing_pda,
            &state.config.solana_program_id,
        ) {
            Ok(value) => value,
            Err(err) => {
                tracing::warn!("pending escrow PDA 계산 실패: listing_id={} err={}", row.id, err);
                continue;
            }
        };

        let listing_exists = match solana_client::account_exists(
            &state.config.solana_rpc_url,
            &state.http_client,
            &listing_pda,
        )
        .await
        {
            Ok(value) => value,
            Err(err) => {
                tracing::warn!("pending listing 온체인 조회 실패: listing_id={} err={}", row.id, err);
                continue;
            }
        };
        if !listing_exists {
            continue;
        }

        let escrow_amount = match solana_client::get_spl_token_account_amount(
            &state.config.solana_rpc_url,
            &state.http_client,
            &escrow_address,
        )
        .await
        {
            Ok(value) => value.unwrap_or(0),
            Err(err) => {
                tracing::warn!("pending escrow 온체인 조회 실패: listing_id={} err={}", row.id, err);
                continue;
            }
        };
        if escrow_amount < 1 {
            continue;
        }

        let patch_url = format!(
            "{}/rest/v1/market_listings?id=eq.{}&status=eq.pending_onchain",
            state.config.supabase_url.trim_end_matches('/'),
            row.id,
        );
        let patch_res = state
            .http_client
            .patch(&patch_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .header("Prefer", "return=minimal")
            .json(&serde_json::json!({
                "escrow_address": escrow_address,
                "status": "active",
            }))
            .send()
            .await
            .context("pending_onchain market_listings 복구 PATCH 요청 실패")?;

        if patch_res.status().is_success() {
            tracing::info!(
                "pending_onchain listing 복구 완료: listing_id={} price_spt={}",
                row.id,
                row.price_spt
            );
        } else {
            tracing::warn!(
                "pending_onchain market_listings 복구 PATCH 실패: listing_id={} body={}",
                row.id,
                patch_res.text().await.unwrap_or_default()
            );
        }
    }

    Ok(())
}

async fn get_user_wallet(state: &AppState, user_id: Uuid) -> Result<String> {
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

    let rows: Vec<UserWalletRow> = res
        .json()
        .await
        .context("users wallet_address 역직렬화 실패")?;
    rows.into_iter()
        .next()
        .and_then(|r| r.wallet_address)
        .filter(|w| !w.trim().is_empty())
        .ok_or_else(|| anyhow!("지갑이 연동된 유저만 마켓을 사용할 수 있습니다"))
}

async fn get_user_item_nft(state: &AppState, user_id: Uuid, user_item_id: Uuid) -> Result<String> {
    let url = format!(
        "{}/rest/v1/user_inventory?id=eq.{}&user_id=eq.{}&select=is_nft,nft_mint_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_item_id,
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
        .context("user_items NFT 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_items NFT 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<UserItemNftRow> = res.json().await.context("user_items NFT 역직렬화 실패")?;
    let item = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("본인 소유 아이템을 찾을 수 없습니다"))?;

    if item.is_nft != Some(true) {
        return Err(anyhow!("NFT로 발행된 아이템만 판매 등록할 수 있습니다"));
    }

    item.nft_mint_address
        .filter(|v| !v.trim().is_empty())
        .ok_or_else(|| anyhow!("NFT mint 주소가 없는 아이템입니다"))
}

async fn ensure_item_not_open_listing(state: &AppState, user_item_id: Uuid) -> Result<()> {
    let url = format!(
        "{}/rest/v1/market_listings?item_id=eq.{}&status=in.(pending_onchain,active)&select=id,status&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_item_id,
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
        .context("market_listings 중복 판매 등록 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "market_listings 중복 판매 등록 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<serde_json::Value> = res
        .json()
        .await
        .context("market_listings 중복 판매 등록 역직렬화 실패")?;
    if !rows.is_empty() {
        return Err(anyhow!("이미 판매 등록 중인 NFT입니다"));
    }

    Ok(())
}

// create_listing
//
/// public.market_listings에 판매 등록 row를 생성하고 ListingResponse를 반환한다.
///
/// 2단계로 처리:
///     1. INSERT → 생성된 listing의 id 확보
///     2. JOIN 조회 → seller nickname + 아이템 상세 정보를 포함한 응답 구성
///
/// INSERT 시 escrow_address는 null;
///     프론트가 list_nft 온체인 호출 후 별도로 PATCH /escrow로 업데이트하므로 최초 등록 시점에는 null로 둔다.
pub async fn create_listing(
    state: &AppState, // JWT에서 추출
    user_id: Uuid,    // { item_id, price_spt }
    req: &CreateListingRequest,
) -> Result<ListingResponse> {
    if req.price_spt <= 0 {
        return Err(anyhow!("판매 가격은 0보다 커야 합니다"));
    }

    let seller_wallet = get_user_wallet(state, user_id).await?;
    let nft_mint_address = get_user_item_nft(state, user_id, req.item_id).await?;
    ensure_item_not_open_listing(state, req.item_id).await?;
    solana_client::derive_listing_address(
        &seller_wallet,
        &nft_mint_address,
        &state.config.solana_program_id,
    )
    .context("판매 등록 PDA 계산 실패")?;

    // 1. market_listings INSERT
    let url = format!(
        "{}/rest/v1/market_listings",
        state.config.supabase_url.trim_end_matches('/'),
    );
    // INSERT 바디: seller_id, items_id, item_id,
    // escrow_address, sold_at은 null이 기본값이므로 생략
    #[derive(Serialize)]
    struct InsertPayload {
        seller_id: Uuid,      // 판매자 = 현재 로그인 유저
        item_id: Uuid,        // 판매할 user_items.id (NFT 발행된 아이템)
        price_spt: i32,       // 판매 가격 (SPT 토큰 단위)
        status: &'static str, // 온체인 list_nft 검증 전까지 pending_onchain
    }

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation") // INSERT 후 생성된 row 반환
        .json(&InsertPayload {
            seller_id: user_id,
            item_id: req.item_id,
            price_spt: req.price_spt,
            status: "pending_onchain",
        })
        .send()
        .await
        .context("market_listings INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings INSERT 실패: {}", body));
    }

    // Prefer: return=representation이면 Vec<T>로 반환됨 (배열 형태)
    // → 첫 번쨰 요소가 방금 생성된 row
    let inserted: Vec<MarketListing> = res.json().await.context("market_listings 역직렬화 실패")?;
    let listing = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("INSERT 결과가 비어있음"))?;

    // 2. JOIN 조회로 ListingResponse 구성
    // INSERT 응답에는 seller, nickname, 아이템 상세가 없으므로 별도 SELECT(embedding)로 가져온다.
    fetch_listing_response(state, listing.id).await
}

// fetch_listing_response (내부 유틸)
//
/// listing_id로 market_listings를 JOIN 조회해 ListingResponse를 반환한다.
///
/// PostgREST embedding 관계:
///     market_listings
///     → users!seller_id(nickname)
///     FK: market_listings.seller_id → users.id → seller의 nickname 가져옴
///     → user_items!item_id(avatar_items(name, image, url, category))
///     FK: market_listings.item_id → user_items.id
///     → user_items에서 다시 avatar_items JOIN
///     FK: user_items.item_id → avatar_items.id
///
/// !fk 문법이 필요한 이유:
///     market_listings → users 관계에서 FK가 seller_id 하나뿐이어도 PostREST는 명시적으로 `users!seler_id`처럼 적어야
///     의도를 정확히 인식한다. 생략하면 ambigous 에러 가능.
async fn fetch_listing_response(state: &AppState, listing_id: Uuid) -> Result<ListingResponse> {
    let url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&select=*,users!seller_id(nickname,wallet_address),user_inventory!item_id(nft_mint_address,item_master(name,image_url,category))",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apiKey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("market_listings JOIN 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings JOIN 조회 실패: {}", body));
    }

    // PostgREST는 항상 배열로 반환 → 첫 번쨰 요소 추출
    let raw: Vec<ListingRaw> = res.json().await.context("ListingRaw 역직렬화 실패")?;
    let r = raw
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("listing을 찾을 수 없음: {}", listing_id))?;

    // 중첩 구조를 flat한 ListingResponse DTO로 변환
    Ok(ListingResponse {
        id: r.id,
        seller_id: r.seller_id,
        seller_nickname: r.users.nickname, // embedding에서 추출
        seller_wallet_address: r.users.wallet_address,
        item_id: r.item_id,
        item_name: r.user_inventory.item_master.name, // 2중 중첩에서 추출
        item_image_url: r.user_inventory.item_master.image_url,
        item_category: r.user_inventory.item_master.category,
        nft_mint_address: r.user_inventory.nft_mint_address,
        escrow_address: r.escrow_address,
        price_spt: r.price_spt,
        status: r.status,
        listed_at: r.listed_at,
    })
}

// get_listings
//
/// status=active인 마켓 리스팅 전체를 최신순으로 조회한다.
///
/// 페이지네이션 없이 전체 반환 (아이템 수가 적은 MVP 단계 기준).
pub async fn get_listings(state: &AppState) -> Result<Vec<ListingResponse>> {
    if let Err(err) = repair_pending_onchain_listings(state).await {
        tracing::warn!("pending_onchain listing 복구 중 오류: {}", err);
    }

    let url = format!(
        "{}/rest/v1/market_listings?status=eq.active&escrow_address=not.is.null&select=*,users!seller_id(nickname,wallet_address),user_inventory!item_id(nft_mint_address,item_master(name,image_url,category))&order=listed_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apiKey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("market_listings 전체 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings 전체 조회 실패: {}", body));
    }

    let raw: Vec<ListingRaw> = res.json().await.context("ListingRaw 역직렬화 실패")?;

    let listings = raw
        .into_iter()
        .map(|r| ListingResponse {
            id: r.id,
            seller_id: r.seller_id,
            seller_nickname: r.users.nickname,
            seller_wallet_address: r.users.wallet_address,
            item_id: r.item_id,
            item_name: r.user_inventory.item_master.name,
            item_image_url: r.user_inventory.item_master.image_url,
            item_category: r.user_inventory.item_master.category,
            nft_mint_address: r.user_inventory.nft_mint_address,
            escrow_address: r.escrow_address,
            price_spt: r.price_spt,
            status: r.status,
            listed_at: r.listed_at,
        })
        .collect();

    Ok(listings)
}

// update_escrow
//
/// public.market_listings의 escrow_address 컬럼을 업데이트한다.
///
/// 호출 시점:
///     프론트가 list_nft 온체인 호출로 에스크로 PDA를 생성한 직후.
///     seeds: ["escrow", seller_pubkey, nft_mint_pubkey ]
///
/// 보안:
///     `seller_id=eq.{user_id}` 필터를 함께 걸어 본인이 등록한 listing만 수정할 수 있도록 제한한다.
///     타인의 listing_id를 넘겨도 seller_id 불일치로 PATCH되지 않는다.
pub async fn update_escrow(
    state: &AppState,
    user_id: Uuid,          // JWT에서 추출한 현재 로그인 유저 UUID (= seller_id)
    listing_id: Uuid,       // URL path에서 추출한 대상 listing UUID
    escrow_address: String, // 저장할 Solana 에스크로 PDA 주소
    tx_signature: String,   // list_nft 온체인 트랜잭션 서명
) -> Result<()> {
    #[derive(Deserialize)]
    struct EscrowListingRow {
        seller_id: Uuid,
        price_spt: i32,
        status: Option<String>,
        escrow_address: Option<String>,
        user_inventory: UserItemNftRow,
        users: UserWalletRow,
    }

    let lookup_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}&select=seller_id,price_spt,status,escrow_address,user_inventory!item_id(is_nft,nft_mint_address),users!seller_id(wallet_address)",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
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
        .context("market_listings escrow 검증 조회 요청 실패")?;

    if !lookup_res.status().is_success() {
        return Err(anyhow!(
            "market_listings escrow 검증 조회 실패: {}",
            lookup_res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<EscrowListingRow> = lookup_res
        .json()
        .await
        .context("market_listings escrow 검증 역직렬화 실패")?;
    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("본인 판매 등록을 찾을 수 없습니다"))?;
    if row.seller_id != user_id {
        return Err(anyhow!("본인 판매 등록만 수정할 수 있습니다"));
    }
    if row.status.as_deref() != Some("pending_onchain") {
        return Err(anyhow!(
            "온체인 등록 대기 상태의 판매글만 escrow를 저장할 수 있습니다"
        ));
    }
    if row.escrow_address.is_some() {
        return Err(anyhow!("이미 escrow가 저장된 판매글입니다"));
    }
    if row.user_inventory.is_nft != Some(true) {
        return Err(anyhow!("NFT로 발행된 아이템만 escrow를 저장할 수 있습니다"));
    }

    let seller_wallet = row
        .users
        .wallet_address
        .ok_or_else(|| anyhow!("판매자 지갑 주소가 없습니다"))?;
    let nft_mint = row
        .user_inventory
        .nft_mint_address
        .ok_or_else(|| anyhow!("NFT mint 주소가 없습니다"))?;
    let listing_pda = solana_client::derive_listing_address(
        &seller_wallet,
        &nft_mint,
        &state.config.solana_program_id,
    )?;
    let expected_escrow =
        solana_client::derive_escrow_address(&listing_pda, &state.config.solana_program_id)?;

    if escrow_address != expected_escrow {
        return Err(anyhow!(
            "escrow 주소가 PDA와 일치하지 않습니다. expected={}, got={}",
            expected_escrow,
            escrow_address
        ));
    }

    let expected_price_base_units = (row.price_spt as u64)
        .checked_mul(solana_client::SPT_DECIMALS)
        .ok_or_else(|| anyhow!("판매 가격 base units 변환 중 overflow"))?;
    let onchain_price_base_units = solana_client::verify_program_instruction_tx_u64_arg(
        &state.config.solana_rpc_url,
        &state.config.helius_api_key,
        &state.http_client,
        &tx_signature,
        &state.config.solana_program_id,
        "list_nft",
        &[
            seller_wallet.as_str(),
            nft_mint.as_str(),
            listing_pda.as_str(),
            expected_escrow.as_str(),
        ],
        Some(seller_wallet.as_str()),
        Some(&[
            (0, seller_wallet.as_str()),
            (1, nft_mint.as_str()),
            (3, listing_pda.as_str()),
            (4, expected_escrow.as_str()),
        ]),
    )
    .await
    .context("판매 등록 트랜잭션 검증 실패")?;

    if onchain_price_base_units != expected_price_base_units {
        return Err(anyhow!(
            "온체인 판매 가격이 DB 가격과 일치하지 않습니다. expected_base_units={}, got_base_units={}",
            expected_price_base_units,
            onchain_price_base_units
        ));
    }

    // 필터: id=eq.{listing_id} AND seller_id=eq.{user_id} AND pending_onchain AND escrow NULL
    //  → 본인이 등록했고 아직 온체인 동기화가 완료되지 않은 listing만 수정 가능.
    let url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}&status=eq.pending_onchain&escrow_address=is.null",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
    );

    // PATCH 바디: escrow_address 컬럼만 업데이트
    #[derive(Serialize)]
    struct PatchPayload {
        escrow_address: String,
        status: &'static str,
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
            escrow_address,
            status: "active",
        })
        .send()
        .await
        .context("market_listings escrow PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("escrow_address PATH 실패: {}", body));
    }
    let updated: Vec<MarketListing> = res
        .json()
        .await
        .context("escrow_address PATCH 응답 역직렬화 실패")?;
    if updated.is_empty() {
        return Err(anyhow!(
            "escrow_address PATCH 대상이 없습니다. 이미 처리됐거나 상태가 변경되었습니다"
        ));
    }

    // 핸들러에서 { "message": "escrow 주소 저장 완료" } 응답을 만들기 떄문에 여기서는 성공 여부만 Ok(())로 전달
    Ok(())
}

// cancel_listing
//
/// 판매자가 본인 리스팅을 취소한다.
///
/// 흐름:
///     1. listing 조회 → seller_id 일치 / active 상태 확인
///     2. 온체인 cancel_listing 트랜잭션 검증
///     3. market_listings.status → "cancelled" PATCH
pub async fn cancel_listing(
    state: &AppState,
    user_id: Uuid,
    listing_id: Uuid,
    tx_signature: String,
) -> Result<()> {
    #[derive(Deserialize)]
    struct CancelListingRow {
        seller_id: Uuid,
        status: Option<String>,
        escrow_address: Option<String>,
        user_inventory: UserItemNftRow,
        users: UserWalletRow,
    }

    let lookup_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}&select=seller_id,status,escrow_address,user_inventory!item_id(is_nft,nft_mint_address),users!seller_id(wallet_address)",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
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
        .context("market_listings 취소 조회 요청 실패")?;

    if !lookup_res.status().is_success() {
        return Err(anyhow!(
            "market_listings 취소 조회 실패: {}",
            lookup_res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<CancelListingRow> = lookup_res
        .json()
        .await
        .context("market_listings 취소 역직렬화 실패")?;
    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("본인 판매 등록을 찾을 수 없습니다"))?;

    if row.seller_id != user_id {
        return Err(anyhow!("본인 판매 등록만 취소할 수 있습니다"));
    }
    if row.status.as_deref() != Some("active") {
        return Err(anyhow!(
            "활성 상태의 판매글만 취소할 수 있습니다 (현재 status: {:?})",
            row.status
        ));
    }

    let seller_wallet = row
        .users
        .wallet_address
        .ok_or_else(|| anyhow!("판매자 지갑 주소가 없습니다"))?;
    let nft_mint = row
        .user_inventory
        .nft_mint_address
        .ok_or_else(|| anyhow!("NFT mint 주소가 없습니다"))?;
    let _escrow_address = row
        .escrow_address
        .ok_or_else(|| anyhow!("에스크로 주소가 없습니다"))?;

    let listing_pda = solana_client::derive_listing_address(
        &seller_wallet,
        &nft_mint,
        &state.config.solana_program_id,
    )?;
    let expected_escrow =
        solana_client::derive_escrow_address(&listing_pda, &state.config.solana_program_id)?;

    solana_client::verify_program_instruction_tx(
        &state.config.solana_rpc_url,
        &state.config.helius_api_key,
        &state.http_client,
        &tx_signature,
        &state.config.solana_program_id,
        "cancel_listing",
        &[
            seller_wallet.as_str(),
            listing_pda.as_str(),
            nft_mint.as_str(),
            expected_escrow.as_str(),
        ],
        Some(seller_wallet.as_str()),
        Some(&[
            (0, seller_wallet.as_str()),
            (1, listing_pda.as_str()),
            (2, nft_mint.as_str()),
            (3, expected_escrow.as_str()),
        ]),
    )
    .await
    .context("판매 취소 트랜잭션 검증 실패")?;

    let patch_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}&status=eq.active",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
    );

    #[derive(Serialize)]
    struct CancelPayload {
        status: &'static str,
    }

    let patch_res = state
        .http_client
        .patch(&patch_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&CancelPayload {
            status: "cancelled",
        })
        .send()
        .await
        .context("market_listings 취소 PATCH 요청 실패")?;

    if !patch_res.status().is_success() {
        return Err(anyhow!(
            "market_listings 취소 실패: {}",
            patch_res.text().await.unwrap_or_default()
        ));
    }

    Ok(())
}

pub async fn abandon_pending_listing(
    state: &AppState,
    user_id: Uuid,
    listing_id: Uuid,
) -> Result<()> {
    let patch_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}&status=eq.pending_onchain&escrow_address=is.null",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
    );

    let patch_res = state
        .http_client
        .patch(&patch_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "status": "cancelled",
        }))
        .send()
        .await
        .context("pending_onchain 판매 등록 폐기 PATCH 요청 실패")?;

    if !patch_res.status().is_success() {
        return Err(anyhow!(
            "pending_onchain 판매 등록 폐기 실패: {}",
            patch_res.text().await.unwrap_or_default()
        ));
    }

    let updated: Vec<MarketListing> = patch_res
        .json()
        .await
        .context("pending_onchain 판매 등록 폐기 응답 역직렬화 실패")?;
    if updated.is_empty() {
        return Err(anyhow!(
            "폐기할 pending_onchain 판매 등록이 없습니다. 이미 처리됐거나 상태가 변경되었습니다"
        ));
    }

    Ok(())
}

// purchase
//
/// 마켓 아이템을 구매한다.
///
/// 처리 순서:
///     1. tx_signature 재확인 (핸들러에서 이미 검증했지만 service에서도 방어)
///     2. market_listings 조회 → price_spt, status 확인
///     3. 수수료 계산: 온체인 platform_config.fee_rate 기준
///     4. market_transaction INSERT
///         → DB 트리거(handle_market_purchase)가 원자적으로 후처리 실행:
///             ① market_listings.status = "sold", sold_at = now()
///             ② user_items.user_id = buyer, is_equipped = false
///         → 따라서 백엔드에서 별도 PATCH 불필요
///
/// # 에러
/// - listing이 active 상태가 아닌 경우 → Err (트리거에서도 체크하지만 선제 차단)
pub async fn purchase(
    state: &AppState,
    user_id: Uuid,        // JWT에서 추출한 현재 로그인 유저 UUID (= buyer_id)
    req: PurchaseRequest, // { listing_id, tx_signature }
) -> Result<TransactionResponse> {
    // tx_signature 방어적 재확인
    // 핸들러에서 None이면 이미 400 반환했지만, service가 직접 호출되는 테스트 시나리오 등을 위해 재검증
    let tx_signature = req
        .tx_signature
        .ok_or_else(|| anyhow!("tx_signature 누락"))?;

    // 1. listing 조회
    // price_spt (수수료 계산에 필요), status (active 여부 확인) 조회
    // select로 필요한 컬럼만 지정해 페이로드 최소화
    let listing_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&select=price_spt,status,escrow_address,seller_id,user_inventory!item_id(nft_mint_address,item_master(name)),users!seller_id(wallet_address)",
        state.config.supabase_url.trim_end_matches('/'),
        req.listing_id,
    );

    /// listing 조회용 내부 구조체 (필요한 컬럼만)
    #[derive(Deserialize)]
    struct PurchaseItemMasterRow {
        name: String,
    }

    #[derive(Deserialize)]
    struct PurchaseUserInventoryRow {
        nft_mint_address: Option<String>,
        item_master: PurchaseItemMasterRow,
    }

    #[derive(Deserialize)]
    struct ListingInfo {
        price_spt: i32,         // 판매 가격 (SPT)
        status: Option<String>, // 현재 상태("active" 여야 구매 가능)
        escrow_address: Option<String>,
        seller_id: Uuid,
        user_inventory: PurchaseUserInventoryRow,
        users: UserWalletRow,
    }

    let listing_res = state
        .http_client
        .get(&listing_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apiKey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("market_listings 조회 요청 실패")?;

    if !listing_res.status().is_success() {
        let body = listing_res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings 조회 실패: {}", body));
    }

    let listings: Vec<ListingInfo> = listing_res.json().await.context("listing 역직렬화 실패")?;

    let listing = listings
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("listing을 찾을 수 없음: {}", req.listing_id))?;

    // active 상태 확인: sold/cancelled 상태의 상품은 구매 불가
    // 트리거에서도 체크하지만 에러 메세지를 명확히 하기 위해 선제 차단
    if listing.status.as_deref() != Some("active") {
        return Err(anyhow!(
            "판매 중이 아닌 상품입니다 (현재 status: {:?})",
            listing.status
        ));
    }
    if listing.seller_id == user_id {
        return Err(anyhow!("본인 상품은 구매할 수 없습니다"));
    }
    let escrow_address = listing
        .escrow_address
        .as_deref()
        .ok_or_else(|| anyhow!("온체인 escrow가 연결되지 않은 리스팅입니다"))?;
    let seller_wallet = listing
        .users
        .wallet_address
        .as_deref()
        .ok_or_else(|| anyhow!("판매자 지갑 주소가 없습니다"))?;
    let buyer_wallet = get_user_wallet(state, user_id).await?;
    let nft_mint = listing
        .user_inventory
        .nft_mint_address
        .as_deref()
        .ok_or_else(|| anyhow!("NFT mint 주소가 없습니다"))?;

    let listing_pda = solana_client::derive_listing_address(
        seller_wallet,
        nft_mint,
        &state.config.solana_program_id,
    )?;
    let expected_escrow =
        solana_client::derive_escrow_address(&listing_pda, &state.config.solana_program_id)?;
    if escrow_address != expected_escrow {
        return Err(anyhow!("DB escrow 주소가 온체인 PDA와 일치하지 않습니다"));
    }

    solana_client::verify_program_instruction_tx(
        &state.config.solana_rpc_url,
        &state.config.helius_api_key,
        &state.http_client,
        &tx_signature,
        &state.config.solana_program_id,
        "buy_nft",
        &[
            buyer_wallet.as_str(),
            seller_wallet,
            nft_mint,
            listing_pda.as_str(),
            escrow_address,
        ],
        Some(buyer_wallet.as_str()),
        Some(&[
            (1, buyer_wallet.as_str()),
            (2, seller_wallet),
            (3, listing_pda.as_str()),
            (4, nft_mint),
            (8, escrow_address),
        ]),
    )
    .await
    .context("구매 트랜잭션 검증 실패")?;

    // 2. 수수료 계산
    // 수수료율은 온체인 platform_config를 기준으로 계산한다.
    let fee_rate = solana_client::get_platform_fee_rate(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_program_id,
    )
    .await
    .context("온체인 수수료율 조회 실패")?;
    let fee = i32::try_from(
        i64::from(listing.price_spt)
            .checked_mul(i64::from(fee_rate))
            .ok_or_else(|| anyhow!("수수료 계산 중 overflow"))?
            / 10_000,
    )
    .map_err(|_| anyhow!("수수료 값 범위 초과"))?;

    // 3. market_transactions INSERT
    // INSERT 후 DB 트리거(handle_market_purchase)가 자동 실행:
    //  - 구매자/판매자 SPT 잔액 조정
    //  - listing status → "sold"
    //  - 아이템 소유권 이전
    // 트리거가 실패하면(잔액 부족 등) INSERT 자체가 롤백되어 에러 반환
    let tx_url = format!(
        "{}/rest/v1/market_transactions",
        state.config.supabase_url.trim_end_matches('/')
    );

    /// market_transactions INSERT 바디
    #[derive(Serialize)]
    struct InsertPayload {
        listing_id: Uuid,     // 구매 대상 listing
        buyer_id: Uuid,       // 구매자 UUID (JWT에서 추출)
        price: i32,           // 거래 가격 (=listing.price_spt)
        fee: i32,             // 수수료 5% (burn 처리용)
        tx_signature: String, // Solana 트랜잭션 서명 (온체인 크로스체크용, 필수)
    }

    /// market_transactions INSERT 응답 Row 역직렬화용
    #[derive(Deserialize)]
    struct TxRow {
        id: Uuid,
        listing_id: Uuid,
        buyer_id: Uuid,
        price: i32,
        fee: i32,
        tx_signature: Option<String>,
        transacted_at: Option<DateTime<Utc>>,
    }

    let tx_res = state
        .http_client
        .post(&tx_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apiKey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation") // INSERT 후 생성된 row 반환
        .json(&InsertPayload {
            listing_id: req.listing_id,
            buyer_id: user_id,
            price: listing.price_spt,
            fee,
            tx_signature: tx_signature.clone(),
        })
        .send()
        .await
        .context("market_transactions INSERT 요청 실패")?;

    // INSERT 실패 = 트리거 에러 포함 (잔액 부족, 이미 sold된 상품 등)
    if !tx_res.status().is_success() {
        let body = tx_res.text().await.unwrap_or_default();
        return Err(anyhow!(
            "market_transactions INSERT 실패 (트리거 에러포함): {}",
            body
        ));
    }

    let inserted: Vec<TxRow> = tx_res
        .json()
        .await
        .context("market_transactions 역직렬화 실패")?;

    let tx = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("INSERT 결과가 비어있음"))?;

    // token_burns INSERT — 온체인에서 소각된 수수료를 DB에 기록
    // 실패해도 구매 자체는 성공으로 처리 (non-critical)
    if tx.fee > 0 {
        let burns_url = format!(
            "{}/rest/v1/token_burns",
            state.config.supabase_url.trim_end_matches('/')
        );
        if let Err(e) = state
            .http_client
            .post(&burns_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .header("Prefer", "return=minimal")
            .json(&serde_json::json!({
                "amount": tx.fee,
                "reason": "marketplace_fee",
            }))
            .send()
            .await
        {
            tracing::error!("token_burns INSERT 실패 (구매는 완료됨): {}", e);
        }
    }

    // 판매자 알림 — listing.seller_id에게 "NFT 아이템이 판매되었어요" 알림 발행
    // 실패해도 구매 자체는 성공이므로 에러 로그만 남기고 진행
    let seller_message = format!(
        "등록하신 NFT {}이 {} SPT에 판매되었어요!",
        listing.user_inventory.item_master.name, tx.price
    );
    let seller_notification_type = format!("market_sold_{}", &tx.id.to_string()[..12]);
    if let Err(e) = crate::notification::service::create_notification(
        state,
        listing.seller_id,
        &seller_notification_type,
        &seller_message,
    )
    .await
    {
        tracing::error!("판매자 알림 생성 실패 (구매는 완료됨): {}", e);
    }

    // INSERT된 row 데이터로 TransactionResponse 구성
    Ok(TransactionResponse {
        id: tx.id,
        listing_id: tx.listing_id,
        buyer_id: tx.buyer_id,
        price: tx.price,
        fee: tx.fee,
        tx_signature: tx.tx_signature,   // Some(서명값)으로 저장됨
        transacted_at: tx.transacted_at, // DB default now()
    })
}
