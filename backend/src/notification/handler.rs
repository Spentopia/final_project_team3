// notification/handler.rs

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::{dto::MarkReadRequest, service};
use crate::state::AppState;

#[utoipa::path(
    get, path = "/api/notifications",
    tag = "알림",
    responses((status = 200, description = "알림 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_notifications(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_notifications(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/notifications/read",
    tag = "알림",
    request_body = MarkReadRequest,
    responses((status = 200, description = "읽음 처리 성공")),
    security(("bearer_auth" = []))
)]
pub async fn mark_read(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<MarkReadRequest>,
) -> impl IntoResponse {
    match service::mark_read(&state, user_id, req).await {
        Ok(_) => StatusCode::OK.into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
