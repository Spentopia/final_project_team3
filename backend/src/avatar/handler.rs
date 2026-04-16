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
use axum::{
    extract::State,
    response::IntoResponse,
    Extension, Json,
};
use axum::http::StatusCode;
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{MintNftRequest, MintNftResponse, TransferNftRequest, TransferNftResponse},
    service,
};

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
pub async fn mint_nft(
    State(state): State<AppState>,  // 공유 앱 상태(http_client, config 등)
    Extension(user_id): Extension<Uuid>, // JWT 미들웨어가 삽입한 인증된 유저 UUID
    Json(req): Json<MintNftRequest>,// 요청 바디: { user_item_id, nft_mint_address }
)->impl IntoResponse{
    match service::mint_nft(&state, user_id, req).await{
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
pub async fn transfer_nft(
    State(state): State<AppState>,          // 공유 앱 상태
    Extension(user_id): Extension<Uuid>,    // JWT 인증된 유저 UUID
    Json(req): Json<TransferNftRequest>,    // 요청 바디: { avatar_id, nft_mint,address }
)->impl IntoResponse{
    match service::transfer_nft(&state, user_id, req).await{
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
pub async fn get_user_items(
    State(state): State<AppState>,      // 공유 앱 상태
    Extension(user_id): Extension<Uuid>,// JWT 인증된 유저 UUID
)->impl IntoResponse{
    match service::get_user_items(&state, user_id).await{
        Ok(items) => (StatusCode::OK, Json(items)).into_response(),
        Err(e) =>(StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}



