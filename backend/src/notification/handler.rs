// notification/handler.rs
// 알림 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET  /api/notifications      → list_notifications
//  POST /api/notifications/read → mark_read

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::MarkReadRequest, service};

// GET /api/notifications
pub async fn list_notifications(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_notifications(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /api/notifications/read
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
