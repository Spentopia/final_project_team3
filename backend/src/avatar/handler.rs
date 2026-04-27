// avatar/handler.rs
//
// 아바타 관련 HTTP 핸들러 모음
//
// 역할:
//  - HTTP 요청을 받아서 service 함수에 위임
//  - service 결과를 HTTP 응답으로 변환 (Statuscode + JSON)
//  - 비즈니스 로직은 service.rs에 위임 (핸들러는 얇게 유지)
//
// 보호 라우트 (JWT 필수):
// POST /avatar/mint-nft → mint-nft
// POST /avatar/transfer-nft → transfer_nft
// GET /avatar/items → get_user_items
//
// JWT 미들웨어가 토큰을 검증한 뒤 Extension<Uuid>에 user_id를 삽입하므로
// 핸들러 파라미터에서 Extension(user_id): Extension<Uuid>로 꺼내 쓴다.
use axum::http::StatusCode;
use axum::{Extension, Json, extract::{Path, State}, response::IntoResponse};
use uuid::Uuid;

use super::{
    dto::{EquipItemRequest, MintNftRequest, MintNftResponse, TransferNftRequest, TransferNftResponse},
    service,
};
use crate::state::AppState;

// POST /avatar/mint-nft
//
// 꾸미기 아이템(user_item)을 NFT로 민팅한 결과를 DB에 기록한다.
///
/// 백엔드는 온체인 트랜잭션을 직접 실행하지 않는다.
/// 프론트엔드가 Solana 프로그램을 호출해 NFT를 민팅하고, 생성된 nft_mint_address를 이 API로 전달하면
/// 백엔드는 public.user_items의 해당 row에 주소와 is_nft=true를 기록한다.
///
/// # 에러
/// service에서 anyhow::Error가 반환되면 500 Internal Server Error로 응답한다.
#[utoipa::path(
    post, path = "/api/avatar/mint-nft",
    tag = "아바타",
    request_body = MintNftRequest,
    responses((status = 200, description = "NFT 민팅 완료")),
    security(("bearer_auth" = []))
)]
pub async fn mint_nft(
    State(state): State<AppState>, // 공유 앱 상태(http_client, config 등)
    Extension(user_id): Extension<Uuid>, // JWT 미들웨어가 삽입한 인증된 유저 UUID
    Json(req): Json<MintNftRequest>, // 요청 바디: { user_item_id, nft_mint_address }
) -> impl IntoResponse {
    match service::mint_nft(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /avatar/transfer-nft
//
/// 아바타 캐릭터(avatars)의 NFT 전송 결과를 DB에 기록한다.
///
/// 백엔드는 온체인 트랜잭션을 직접 실행하지 않는다.
/// 프론트엔드가 Solana 프로그램을 통해 NFT를 다른 지갑으로 전송하고,
/// 완료된 nft_mint_address를 이 API로 전달하면 백엔드는 public.avatars의 해당 row에 주소와 is_nft = true를 기록한다.
///
///  # 에러
///  service에서 anyhow::Error가 반환되면 500 Internal Server Error로 응답한다.
#[utoipa::path(
    post, path = "/api/avatar/transfer-nft",
    tag = "아바타",
    request_body = TransferNftRequest,
    responses((status = 200, description = "NFT 전송 완료")),
    security(("bearer_auth" = []))
)]
pub async fn transfer_nft(
    State(state): State<AppState>,       // 공유 앱 상태
    Extension(user_id): Extension<Uuid>, // JWT 인증된 유저 UUID
    Json(req): Json<TransferNftRequest>, // 요청 바디: { avatar_id, nft_mint,address }
) -> impl IntoResponse {
    match service::transfer_nft(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// GET /avatar/items
//
/// 로그인한 유저가 보유한 꾸미기 아이템 전체 목록을 조회한다.
///
/// JWT에서 추출한 user_id로 public.user_items를 조회하고, 각 아이템의 마스터 정보(이름, 이미지 URL, 카테고리, 희귀도)를
/// public.avatar_items와 PostgREST embedding으로 JOIN해서 반환한다.
///
/// # 반환
/// Vec<UserItemResponse> 아이템 목록 (없으면 빈 배열)
///
/// # 에러
/// service에서 anyhow::Error가 반환되면 500 Internal Server Error로 응답한다.
#[utoipa::path(
    get, path = "/api/avatar/items",
    tag = "아바타",
    responses((status = 200, description = "보유 아이템 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_user_items(
    State(state): State<AppState>,       // 공유 앱 상태
    Extension(user_id): Extension<Uuid>, // JWT 인증된 유저 UUID
) -> impl IntoResponse {
    match service::get_user_items(&state, user_id).await {
        Ok(items) => (StatusCode::OK, Json(items)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/avatar/nfts",
    tag = "아바타",
    responses((status = 200, description = "연결된 지갑의 컬렉션 NFT 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_owned_nfts(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_owned_nfts(&state, user_id).await {
        Ok(items) => (StatusCode::OK, Json(items)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// GET /api/avatar/equipment
//
// 유저의 전체 장착 현황 조회.
// 각 슬롯(slot_name)에 어떤 아이템이 장착돼 있는지 반환한다.
#[utoipa::path(
    get, path = "/api/avatar/equipment",
    tag = "아바타",
    responses((status = 200, description = "장착 현황 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_equipment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_equipment(&state, user_id).await {
        Ok(slots) => (StatusCode::OK, Json(slots)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /api/avatar/equipment
//
// 아이템 장착.
// 같은 슬롯에 이미 아이템이 있으면 교체된다.
#[utoipa::path(
    post, path = "/api/avatar/equipment",
    tag = "아바타",
    request_body = EquipItemRequest,
    responses(
        (status = 200, description = "장착 완료"),
        (status = 403, description = "본인 소유 아이템 아님"),
    ),
    security(("bearer_auth" = []))
)]
pub async fn equip_item(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<EquipItemRequest>,
) -> impl IntoResponse {
    match service::equip_item(&state, user_id, req).await {
        Ok(()) => (StatusCode::OK, Json(serde_json::json!({"message": "장착 완료"}))).into_response(),
        Err(e) => {
            let msg = e.to_string();
            if msg.contains("본인 소유가 아닙니다") {
                (StatusCode::FORBIDDEN, msg).into_response()
            } else {
                (StatusCode::INTERNAL_SERVER_ERROR, msg).into_response()
            }
        }
    }
}

// DELETE /api/avatar/equipment/:slot_name
//
// 슬롯 해제. inventory_id를 NULL로 설정하여 슬롯을 비운다.
#[utoipa::path(
    delete, path = "/api/avatar/equipment/{slot_name}",
    tag = "아바타",
    params(("slot_name" = String, Path, description = "해제할 슬롯 이름")),
    responses((status = 200, description = "슬롯 해제 완료")),
    security(("bearer_auth" = []))
)]
pub async fn unequip_item(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(slot_name): Path<String>,
) -> impl IntoResponse {
    match service::unequip_item(&state, user_id, &slot_name).await {
        Ok(()) => (StatusCode::OK, Json(serde_json::json!({"message": "슬롯 해제 완료"}))).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
