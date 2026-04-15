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
//  public.market_transactions에 INSERT 하면 PostgreSQL 트리거가 자동 실행되어 아래 작업을 원자적(atomic)으로 처리한다:
//  1. 구매자 spt_balance 차감
//  2. 판매자 spt_balance 증가 (수수료 제외)
//  3. market_listings.status → "sold", sold_at = now()
//  4. user_items.user_id → buyer_id, is_equipped = false
// 따라서 백엔드에서 별도로 PATCH하지 않아도 된다.
//
// ※ PostgREST embedding(!kf 문법):
//  FK가 여러 개일 때 어느 FK로 JOIN할지 명시해야 한다.
//  예: users!seller_id(nickname) → market_listings.seller_id FK를 통해 users 테이블 JOIN
//  명시하지 않으면 PostgREST가 ambigous 에러를 뱉는다.

use std::fmt::format;
use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{
    dto::{CreateListingRequest, ListingResponse, PurchaseRequest, TransactionResponse},
    model::MarketListing,
};
use crate::state::AppState;

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
    // 1. market_listings INSERT
    let url = format!(
        "{}/rest/v1/market_listings",
        state.config.supabase_url.trim_end_matches('/'),
    );
    // INSERT 바디: seller_id, items_id, item_id,
    // escrow_address, sold_at은 null이 기본값이므로 생략
    #[derive(Serialize)]
    struct InsertPayload {
        seller_id: Uuid,        // 판매자 = 현재 로그인 유저
        item_id: Uuid,          // 판매할 user_items.id (NFT 발행된 아이템)
        price_spt: i32,         // 판매 가격 (SPT 토큰 단위)
        status: &'static str,   // "active" 고정: 판매 등록 직후 상태
    }

    let res = state
        .http_client
        .post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Prefer", "return=representation")  // INSERT 후 생성된 row 반환
        .json(&InsertPayload {
            seller_id: user_id,
            item_id: req.item_id,
            price_spt: req.price_spt,
            status: "active",
        })
        .send()
        .await
        .context("market_listings INSERT 요청 실패")?;

    if !res.status().is_success(){
      let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings INSERT 실패: {}", body));
    }

    // Prefer: return=representation이면 Vec<T>로 반환됨 (배열 형태)
    // → 첫 번쨰 요소가 방금 생성된 row
    let inserted: Vec<MarketListing> = res
        .json()
        .await
        .context("market_listings 역직렬화 실패")?;
    let listing = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("INSERT 결과가 비어있음"))?;

    // 2. JOIN 조회로 ListingResponse 구성
    // INSERT 응답에는 sellet, nickname, 아이템 상세가 없으므로 별도 SELECT(embedding)로 가져온다.
    fetch_listing_response(state,listing.id).await;
}


// fetch_listing_response (내부 유틸)
//
/// listing_id로 market_listings를 JOIN 조회해 ListingResponse를 반환한다.
///
/// PostgREST embedding 관계:
///     market_listings
///     → users!seller_id(nickname)
///     FK: market_listings.seller_id → users.id → seller의 nickname 가져옴
///     → user_items!item_id(avatar_items(name, image, url, category, rarity))
///     FK: market_listings.item_id → user_items.id
///     → user_items에서 다시 avatar_items JOIN
///     FK: user_items.item_id → avatar_items.id
///
/// !fk 문법이 필요한 이유:
///     market_listings → users 관계에서 FK가 seller_id 하나뿐이어도 PostREST는 명시적으로 `users!seler_id`처럼 적어야
///     의도를 정확히 인식한다. 생략하면 ambigous 에러 가능.
async fn fetch_listing_response(state: &AppState, listing_id: Uuid)-> Result<ListingResponse> {
    // embedding URL:
    //  users!seller_id(nickname)                   → seller 닉네임
    //  user_items!item_id(avatar_items(...))       → 아이템 마스터 정보
    let url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&select=*.users!seller_id(nickname),user_items!item_id(avatar_items(name,image_url,category\
        ,rarity))",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
    );

    // PostgREST 응답 역직렬화용 내부 고조체
    // 중첩 구조(embedding)를 표현하기 위해 Raw 타입들을 정의한다.

    /// users!seller_id 중첩 객체
    #[derive(Deserialize)]
    struct SellerEmbed{
        nickname: Option<String>,   // 닉네임 (프로필 미완성 유저는 null 가능)
    }

    /// avatar_items 중첩 객체 (user_items 안에 다시 중첩)
    #[derive(Deserialize)]
    struct AvatarItemEmbed{
        name: String,           // 아이템 이름
        image_url: String,      // 아이템 이미지 URL
        category: String,       // 카테고리 (background / frame / effect / motion)
        rarity: String,         // 희귀도 (common / rare / epic)
    }

    /// user_items!item_id 중첩 객체
    /// select에서 avatar_items만 지정했으므로 avatar_items 필드만 포함
    #[derive(Deserialize)]
    struct UserItemEmbed{
        avatar_items: AvatarItemEmbed,  // user_items.item_id → avatar_items
    }

    /// market_listings 최상위 응답 Row
    #[derive(Deserialize)]
    struct ListingRaw{
        id: Uuid,
        seller_id: Uuid,
        item_id: Uuid,              // user_items FK
        price_spt: i32,
        status: Option<String>,     // "active" / "sold" / "calcelled"
        listed_at: Option<DateTime<Utc>>,
        users: SellerEmbed,         // embedding: seller 정보
        user_items: UserItemEmbed,  // embedding: 아이템 정보
    }

    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apiKey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("market_listings JOIN 조회 요청 실패")?;

    if !res.status().is_success(){
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings JOIN 조회 실패: {}", body));
    }

    // PostgREST는 항상 배열로 반환 → 첫 번쨰 요소 추출
    let raw: Vec<ListingRaw> = res.json().await.context("ListingRaw 역직렬화 실패")?;
    let r = raw
        .into_iter()
        .next()
        .or_or_else(|| anyhow!("listing을 찾을 수 없음: {}", listing_id));

    // 중첩 구조를 flat한 ListingResponse DTO로 변환
    Ok(ListingResponse{
        id: r.id,
        seller_id: r.seller_id,
        seller_nickname: r.users.nickname,          // embedding에서 추출
        item_id: r.item_id,
        item_name: r.user_items.avatar_items.name,  // 2중 중첩에서 추출
        item_image_url: r.user_items.avatar_items.image_url,
        item_category: r.user_items.avatar_items.category,
        item_rarity: r.user_items.avatar_items.rarity,
        price_spt: r.price_spt,
        status: r.status,
        listed_at: r.listed_at,
    })
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
)->Result<()>{
    // 필터: id=eq.{listing_id} AND seller_id=eq.{user_id}
    //  → 본인이 등록한 listing만 수정 가능 (보안 필터)
    let url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&seller_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        listing_id,
        user_id,
    );

    // PATCH 바디: escrow_address 컬럼만 업데이트
    #[derive(Serialize)]
    struct PatchPayload{
        escrow_address: String,
    }

    let res = state
        .http_client
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apiKey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload { escrow_address})
        .send()
        .await
        .context("market_listings escrow PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("escrow_address PATH 실패: {}", body));
    }

    // 핸들러에서 { "message": "escrow 주소 저장 완료" } 응답을 만들기 떄문에 여기서는 성공 여부만 Ok(())로 전달
    Ok(())
}

// purchase
//
/// 마켓 아이템을 구매한다.
///
/// 처리 순서:
///     1. tx_signature 재확인 (핸들러에서 이미 검증했지만 service에서도 방어)
///     2. market_listings 조회 → price_spt, status 확인
///     3. 수수료 계산: fee = price_spt * 5 / 100
///     4. market_transaction INSERT
///         → DB 트리거(handle_market_purchase)가 원자적으로 후처리 실행:
///             ① 구매자 spt_balance -= price
///             ② 판매자 spt_balance += (price - fee)
///             ③ market_listings.status = "sold", sold_at = now()
///             ④ user_items.user_id = buyer, is_equipped = false
///         → 따라서 백엔드에서 별도 PATCH 불필요
///
/// # 에러
/// - listing이 active 상태가 아닌 경우 → Err (트리거에서도 체크하지만 선제 차단)
/// - 트리거 내부에서 SPT 잔액 부족 → INSERT 실패 → Err
pub async fn purchase(
    state: &AppState,
    user_id: Uuid,          // JWT에서 추출한 현재 로그인 유저 UUID (= buyer_id)
    req: PurchaseRequest,   // { listing_id, tx_signature }
)->Result<PurchaseResponse> {
    // tx_signature 방어적 재확인
    // 핸들러에서 None이면 이미 400 반환했지만, service가 직접 호출되는 테스트 시나리오 등을 위해 재검증
    let tx_signature = req
        .tx_signature
        .ok_or_else(|| anyhow!("tx_signature 누락"))?;

    // 1. listing 조회
    // price_spt (수수료 계산에 필요), status (active 여부 확인) 조회
    // select로 필요한 컬럼만 지정해 페이로드 최소화
    let listing_url = format!(
        "{}/rest/v1/market_listings?id=eq.{}&select=price_spt,status",
        state.config.supabase_url.trim_end_matches('/'),
        req.listing_id,
    );

    /// listing 조회용 내부 구조체 (필요한 컬럼만)
    #[derive(Deserialize)]
    struct ListingInfo{
        price_spt: i32,         // 판매 가격 (SPT)
        status: Option<String>, // 현재 상태("active" 여야 구매 가능)
    }

    let listing_res = state
        .http_client
        .get(&listing_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apiKey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("market_listings 조회 요청 실패")?;

    if !listing_res.status().is_success() {
        let body = listing_res.text().await.unwrap_or_default();
        return Err(anyhow!("market_listings 조회 실패: {}", body));
    }

    let listings: Vec<ListingInfo> = listing_res
        .json()
        .await
        .context("listing 역직렬화 실패")?;

    let listing = listings
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("listing을 찾을 수 없음: {}",req.listing_id))?;

    // active 상태 확인: sold/cancelled 상태의 상품은 구매 불가
    // 트리거에서도 체크하지만 에러 메세지를 명확히 하기 위해 선제 차단
    if listing.status.as_deref() != Some("active"){
        return Err(anyhow!(
            "판매 중이 아닌 상품입니다 (현재 status: {:?})",
            listing.status
        ));
    }
    // 2. 수수료 계산
    // 수수료: price의 5% (정수 나눗셈 → 소수점 절사)
    // 예: price=1000 → fee = 50 / price=19 → fee=0
    let fee = listing.price_spt * 5 / 100;

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
    struct InsertPayload{
        listing_id: Uuid,       // 구매 대상 listing
        buyer_id: Uuid,         // 구매자 UUID (JWT에서 추출)
        price: i32,             // 거래 가격 (=listing.price_spt)
        fee: i32,               // 수수료 5% (burn 처리용)
        tx_signature: String,   // Solana 트랜잭션 서명 (온체인 크로스체크용, 필수)
    }

    /// market_transactions INSERT 응답 Row 역직렬화용
    #[derive(Deserialize)]
    struct TxRow{
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
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apiKey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")  // INSERT 후 생성된 row 반환
        .json(&InsertPayload{
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

    // INSERT된 row 데이터로 TransactionResponse 구성
    Ok(TransactionResponse{
        id: tx.id,
        listing_id: tx.listing_id,
        buyer_id: tx.buyer_id,
        price: tx.price,
        fee: tx.fee,
        tx_signature: tx.tx_signature,  // Some(서명값)으로 저장됨
        transacted_at: tx.transacted_at,// DB default now()
    })
}
