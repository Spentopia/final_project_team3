// market/handler.rs
//
// 마켓 관련 HTTP 핸들러 모음
//
// 역할:
//  - HTTP 요청을 받아서 service 함수에 위임
//  - service 결과를 HTTP 응답으로 변환 (StatusCode + JSON)
//  - 핸들러 레벨에서만 처리할 수 있는 유효성 검사 수행
//    (ex. tx_signature 존재 여부 → 400 Bad Request)
//
// 보호 라우트 (JWT 필수):
//  POST /market/listings → create_listing
//  PATCH /market/listings/:listing_id/escrow → update_escrow
//  POST /market/purchase → purchase
//
// Path 파라미터 추출:
//  Path(listing_id): Path<Uuid> → /market/listings/:listing_id/escrow에서 UUID 추출

use axum::{
    Extension, Json,
    extract::{Path, State},
    response::IntoResponse,
};

use axum::http::StatusCode;
use serde::Deserialize;
use uuid::Uuid;

use super::{
    dto::{CreateListingRequest, PurchaseRequest},
    service,
};

// GET /api/market/listings
//
/// status=active인 판매 목록 전체를 최신순으로 반환한다.
///
/// # 응답
/// 200 OK + Vec<ListingResponse>
#[utoipa::path(
    get, path = "/api/market/listings",
    tag = "마켓",
    responses((status = 200, description = "판매 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_listings(State(state): State<AppState>) -> impl IntoResponse {
    match service::get_listings(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
use crate::state::AppState;

// POST /market/listings
//
// NFT 아이템을 마켓에 판매 등록한다.
///
/// 흐름:
///   1. public.market_listings에 INSERT (status="active", seller_id=JWT user_id)
///   2. INSERT 후 listing id로 JOIN 조회해 ListingResponse 구성
///      (seller nickname, 아이템 이름/이미지/카테고리/희귀도 포함)
///
/// 참고:
///   escrow_address는 이 시점에 null이어도 된다.
///   프론트가 list_nft 온체인 호출 완료 후 PATCH /market/listings/:id/escrow로 따로 업데이트한다.
/// # 응답
/// 201 Created + ListingResponse
#[utoipa::path(
    post, path = "/api/market/listings",
    tag = "마켓",
    request_body = CreateListingRequest,
    responses((status = 201, description = "판매 등록 성공")),
    security(("bearer_auth" = []))
)]
pub async fn create_listing(
    State(state): State<AppState>,         // 공유 앱 상태
    Extension(user_id): Extension<Uuid>,   // JWT 인증된 유저 UUID (=seller_id)
    Json(req): Json<CreateListingRequest>, // 요청 바디: {item_id, price_spt}
) -> impl IntoResponse {
    match service::create_listing(&state, user_id, &req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// PATCH /market/listings/:listing_id/escrow
//
/// 판매 등록 후 생성된 에스크로 PDA 주소를 DB에 저장한다.
///
/// 흐름:
///   1. 프론트가 list_nft 온체인 호출 → 에스크로 PDA 주소 생성
///   2. 이 API로 escrow_address 전달
///   3. 백엔드가 public.market_listings의 escrow_address 컬럼 PATCH
///
/// 보안: service에서 seller_id=eq.{user_id} 필터를 걸어 본인 listing만 수정 가능
///
/// Path 파라미터:
/// :listing_id → PATCH 대상 market_listings row UUID
///
/// # 응답
/// 200 OK + { "message": "escrow 주소 저장 완료" }
#[utoipa::path(
    patch, path = "/api/market/listings/{id}/escrow",
    tag = "마켓",
    params(("id" = Uuid, Path, description = "listing ID")),
    responses((status = 200, description = "escrow 주소 저장 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_escrow(
    State(state): State<AppState>,        // 공유 앱 상태
    Extension(user_id): Extension<Uuid>,  // JWT 인증된 유저 UUID (= seller_id)
    Path(listing_id): Path<Uuid>,         // URL 경로에서 추출한 listing_UUID
    Json(req): Json<UpdateEscrowRequest>, // 요청 바디: { escrow+address }
) -> impl IntoResponse {
    match service::update_escrow(&state, user_id, listing_id, req.escrow_address).await {
        Ok(_) => (
            StatusCode::OK,
            // 단순 메세지 응답 → serde_json::json! 매크로의 인라인 생성
            Json(serde_json::json!({"message": "escrow 주소가 저장되었습니다."})),
        )
            .into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /market/purchase
//
/// 마켓에 올라온 NFT 아이템을 구매한다.
///
/// tx_signature 필수 검증 (핸들러 레벨):
///   온체인 트랜잭션이 완료되지 않은 상태로 INSERT하면 크로스체크를 서명이 없어 데이터 무결성이 깨지므로 400 반환.
///   서비스 호출 전에 핸들러에서 먼저 막는다.
///
/// 흐름:
///   1. tx_signature 존재 여부 확인 (없으면 즉시 400 에러)
///   2. service::purchase 호출
///   3. service 내부에서 market_transaction INSERT
///
/// DB 트리거(handle_market_purchase) 자동 처리 항목:
///  - 구매자 SPT 잔액 차감(price 만큼)
///  - 판매자 SPT 잔액 증가(price - fee 만큼)
///  - market_listings.status → "sold", sold_at = now()
///  - user_items.user_id → buyer_idm is_equipped = false
///  → 백엔드에서 별도 PATCH 불필요
///
/// # 응답
/// 201 Created + TransactionResponse
/// 400 Bad Request: tx_signature 누락
#[utoipa::path(
    post, path = "/api/market/purchase",
    tag = "마켓",
    request_body = PurchaseRequest,
    responses((status = 201, description = "구매 성공"), (status = 400, description = "tx_signature 누락")),
    security(("bearer_auth" = []))
)]
pub async fn purchase(
    State(state): State<AppState>,       // 공유 앱 상태
    Extension(user_id): Extension<Uuid>, // JWT 인증된 유저 UUID (=buyer_id)
    Json(req): Json<PurchaseRequest>,    // 요청 바디: { listing_id, tx_signature }
) -> impl IntoResponse {
    // tx_signature 필수 검증: None이면 온체인 트랜잭션 미완료 상태
    // service로 넘기기 전에 핸들러 레벨에서 차단해 명확한 400 응답 보장
    if req.tx_signature.is_none() {
        return (
            StatusCode::BAD_REQUEST,
            "tx_signature는 필수입니다. 온체인 트랜잭션 완료 후 요청해 주세요.".to_string(),
        )
            .into_response();
    }
    match service::purchase(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// 로컬 요청 DTO
//
// update_escrow 전용 요청 바디.
// 단일 엔드포인트에서만 쓰이므로 dto.rs 분리 없이 handler 내부에 정의한다.
/// PATCH /market/listings/:listing_id/escrow 요청 바디
#[derive(Deserialize)]
pub struct UpdateEscrowRequest {
    /// Solana 에스크로 PDA 주소
    /// Anchor 프로그램의 seeds: ["escrow"m seller_pubkey, nft_mint_pubkey ]
    pub escrow_address: String,
}
