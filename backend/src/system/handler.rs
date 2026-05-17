// backend/src/system/handler.rs
//
// 서비스 상태 공지 HTTP 핸들러.
//
// public:
// - GET /api/system/status
//
// admin:
// - PATCH /api/admin/system/status

use axum::{
    Extension, Json,
    extract::State,
    http::StatusCode,
    response::{IntoResponse, Response},
};
use uuid::Uuid;

use crate::state::AppState;

use super::{
    dto::UpdateSystemStatusRequest,
    service,
};

fn map_system_error(error: anyhow::Error) -> Response {
    let message = error.to_string();

    if message.contains("입력해 주세요")
        || message.contains("지원하지 않는 서비스 상태")
        || message.contains("종료 시각")
    {
        return (StatusCode::BAD_REQUEST, message).into_response();
    }

    (StatusCode::INTERNAL_SERVER_ERROR, message).into_response()
}

/// 공개: 서비스 상태 공지 조회.
///
/// 로그인 전 화면에서도 보여야 하므로 인증 없이 접근 가능하다.
#[utoipa::path(
    get,
    path = "/api/system/status",
    tag = "시스템",
    responses(
        (status = 200, description = "서비스 상태 조회 성공", body = crate::system::dto::SystemStatusResponse)
    )
)]
pub async fn get_system_status(
    State(state): State<AppState>,
) -> impl IntoResponse {
    match service::get_system_status(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_system_error(e),
    }
}

/// 관리자: 서비스 상태 공지 수정.
///
/// 관리자만 점검/장애 공지를 켜고 끌 수 있다.
#[utoipa::path(
    patch,
    path = "/api/admin/system/status",
    tag = "관리자",
    request_body = UpdateSystemStatusRequest,
    responses(
        (status = 200, description = "서비스 상태 수정 성공", body = crate::system::dto::SystemStatusResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn update_system_status(
    State(state): State<AppState>,

    // jwt_middleware가 넣어준 현재 로그인 관리자 ID.
    Extension(admin_id): Extension<Uuid>,

    Json(req): Json<UpdateSystemStatusRequest>,
) -> impl IntoResponse {
    match service::update_system_status(&state, admin_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_system_error(e),
    }
}