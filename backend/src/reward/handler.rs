// reward/handler.rs
// 보상, 스트릭, 주간 성실도 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET /api/rewards              → list_rewards
//  GET /api/rewards/streak       → get_streak
//  GET /api/rewards/weekly-score → get_weekly_scores

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::service;

// GET /api/rewards
pub async fn list_rewards(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_rewards(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// GET /api/rewards/streak
pub async fn get_streak(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_streak(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// GET /api/rewards/weekly-score
pub async fn get_weekly_scores(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_weekly_scores(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
