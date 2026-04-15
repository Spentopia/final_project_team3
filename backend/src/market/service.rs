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
    struct InsertPayload {}
}
