// wallet/handler.rs
//
// 지갑 연동/해제 HTTP 핸들러
//
// ■ link_wallet   → POST /wallet/link   (지갑 연동)
// ■ unlink_wallet → DELETE /wallet/unlink (지갑 해제)
//
// 두 핸들러 모두 보호 라우트 → jwt_middleware를 통과해야 실행됨.
// jwt_middleware가 JWT를 검증하고 user_id를 Extension에 넣어줌.
// 핸들러는 Extension<Uuid>로 user_id를 꺼내서 service 함수에 넘김.
//
// ■ 스프링부트와 비교하면?
// @RestController + @RequestMapping과 동일한 역할.
// @PreAuthorize는 jwt_middleware가 대신함.

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};

// Uuid: 유저 고유 식별자 타입
// jwt_middleware가 Extension에 넣어준 user_id르 꺼낼 때 사용
use uuid::Uuid;

use crate::state::AppState;

// LinkWalletRequest → POST /wallet/link 요청 body 구조체
// LinkWalletResponse → POST /wallet/link 성공 응답 구조체
// UnlinkWalletResponse → DELETE /wallet/unlink 성공 응답 구조체
use super::dto::{
    LinkWalletRequest, LinkWalletResponse, UnlinkWalletRequest, UnlinkWalletResponse,
};

// 비즈니스 로직 (nonce 검증 → 서명 검증 → 중복 체크 → DB 업데이트)
use super::service;

// map_err_status - anyhow 에러 메세지를 HTTP 상태코드로 변환
//
// service 함수는 anyhow:Error를 반환함.
// 핸들러는 에러 메세지를 보고 적잘한 StatusCode를 골라야 함.
//
// ■ 변환 규칙
//  "nonce" 또는 "서명" 포함 → 401 Unauthorized
//  → nonce 불일치, 서명 검증 실패 등 인증 관련 에러
//  "이미" 포함 → 409 Conflict
//  → 이미 다른 계정에 연동된 지갑 주소
//  "연동된 지갑이 없습니다" → 400 Bad Request
//  → unlink 시 지갑이 없는 경우
//  나머지 → 500 Internal Server Error
//  → DB 오류, HTTP 요청 실패 등 서버 내부 문제
fn map_err_status(e: &anyhow::Error) -> StatusCode {
    let msg = e.to_string();
    if msg.contains("nonce") || msg.contains("서명") {
        StatusCode::UNAUTHORIZED
    } else if msg.contains("이미") {
        StatusCode::CONFLICT
    } else if msg.contains("일치하지 않습니다") {
        StatusCode::UNAUTHORIZED
    } else if msg.contains("연동된 지갑이 없습니다") {
        StatusCode::BAD_REQUEST
    } else {
        StatusCode::INTERNAL_SERVER_ERROR
    }
}

// link_wallet - 지갑 연동 핸들러
//
// POST /wallet/link
// JWT 필수 (보호 라우트) → jwt_middleware가 앞단에서 검증
//
// ■ 처리 흐름
//  jwt_middleware → JWT 검증 → user_id를 Extension에 삽입
//  → link_wallet 핸들러 실행
//  → service::link_wallet 호출
//  (1) nonce 검증 → 발급한 nonce가 맞는지 확인
//  (2) 서명 검증 → 이 지갑의 실제 주인인지 확인
//  (3) 중복 체크 → 이미 다른 계정에 연동된 지갑인지 확인
//  (4) DB 업데이트 → public.users.wallet_address 컬럼 업데이트
// → 성공: 200 OK + LinkWalletResponse
// → 실패: map err status()로 적절한 상태코드 반환
//
// ■ #[utoipa::path(...)] 란?
//  Swagger UI에 이 API의 스펙을 자동으로 등록해주는 어노테이션.
//  스프링부트의 @Operation, @ApiResponse와 동일한 역할.
//  openapi.rs의 paths()에도 이 함수를 등록해야 Swagger UI에 표시됨.
#[utoipa::path(
    post,
    path="/wallet/link",
    tag = "지갑 연동",
    // Swagger UI의 Authorize 버튼으로 넣은 토큰을 이 API에 자동 적용
    security(("bearer_auth"=[])),
    request_body = LinkWalletRequest,
    responses(
        (status = 200, description = "지갑 연동 성공", body = LinkWalletResponse),
        (status = 401, description = "nonce 불일치 또는 서명 검증 실패"),
        (status = 409, description = "이미 다른 계정에 연동된 지갑"),
        (status = 500, description = "서버 내부 오류 (DB, HTTP 등)"),
    )
)]
pub async fn link_wallet(
    // State: Axum이 route.rs의 .with_state(state)로 등록한 Appstate를 주입해줌
    // Supabase URL, secret key, http_client, nonce_sotre 등에 접근할 때 사용
    State(state): State<AppState>,

    // Extension: jwt_middleware가 검증 완료 후 삽입한 user_id를 꺼냄
    // jwt_middleware에서 request.extensions_mut().insert(user_id)로 넣어줬음
    // 로그인된 유저가 누구인지 확인하는 유일한 수단
    Extension(user_id): Extension<Uuid>,

    // Json: 요청 body를 LinkWalletRequest 구조체로 자동 역직렬화
    // wallet_address, nonce, signature 세 필드가 모두 있어야 함
    // 없으면 Axum이 자동으로 422 Unprocessable Entity 반환
    Json(req): Json<LinkWalletRequest>,
) -> impl IntoResponse {
    match service::link_wallet(
        &state,
        user_id,
        &req.wallet_address,
        &req.nonce,
        &req.signature,
    )
    .await
    {
        // 연동 성공 → 연동된 지갑 주소와 완료 메세지를 JSON으로 반환
        Ok(()) => (
            StatusCode::OK,
            Json(LinkWalletResponse {
                // 요청에서 받은 지갑 주소를 그대로 돌려줌
                // 클라이언트가 "어떤 지갑이 연동됐는지" 확인할 수 있게 포함
                wallet_address: req.wallet_address,
                message: "지갑이 연동되었습니다.".to_string(),
            }),
        )
            .into_response(),
        // 연동 실패 → 에러 메세지로 적절한 StatusCode 선택 후 반환
        // map_err_status()가 에러 내용을 보고 401/409/500 중 하나를 고름
        Err(e) => (map_err_status(&e), e.to_string()).into_response(),
    }
}

// unlink_wallet - 지갑 해제 핸들러
//
// DELETE /wallet/unlink
// JWT 필수 (보호 라우트) → jwt_middleware가 앞단에서 검증
//
// ■ 처리 흐름
//  jwt_middleware → JWT 검증 → user_id를 Extension에 삽입
//  → unlink_wallet 핸들러 실행 → service::unlink_wallet 호출
//  (1) 연동 여부 확인 → wallet_address가 NULL인지 체크
//  (2) nonce/서명 검증 → 현재 연동된 지갑 소유자인지 확인
//  (3) DB 업데이트 → public.users.wallet_address = NULL
//  → 성공: 200 OK + UnlinkWalletResponse
//  → 실패: map_err_status()로 적절한 상태코드 반환
//
#[utoipa::path(
    delete,
    path="/wallet/unlink",
    tag="지갑 연동",
    security(("bearer_auth"=[])),
    request_body = UnlinkWalletRequest,
    responses(
        (status = 200, description = "지갑 해제 성공", body = UnlinkWalletResponse),
        (status = 400, description = "연동된 지갑이 없음"),
        (status = 401, description = "nonce 불일치 또는 서명 검증 실패"),
        (status = 500, description = "서버 내부 오류 (DB, HTTP 등)"),
    )
)]
pub async fn unlink_wallet(
    State(state): State<AppState>,
    // link_wallet과 동일하게 jwt_middleware가 넣어준 user_id를 꺼냄
    // 이 user_id로 어떤 유저의 지갑을 해제할지 특정함
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<UnlinkWalletRequest>,
) -> impl IntoResponse {
    match service::unlink_wallet(
        &state,
        user_id,
        &req.wallet_address,
        &req.nonce,
        &req.signature,
    )
    .await
    {
        // 해제 성공 → 완료 메세지를 JSON으로 반환
        Ok(()) => (
            StatusCode::OK,
            Json(UnlinkWalletResponse {
                message: "지갑 연동이 해제되었습니다.".to_string(),
            }),
        )
            .into_response(),

        // 해제 실패 -> 에러 메세지로 적절한 StatusCode 선택 후 반환
        // "연동된 지갑이 없습니다" → 400, 나머지 → 500
        Err(e) => (map_err_status(&e), e.to_string()).into_response(),
    }
}
