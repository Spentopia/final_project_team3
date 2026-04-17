// reward/handler.rs

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::service;
use crate::state::AppState;

#[utoipa::path(
    get, path = "/api/rewards",
    tag = "보상",
    responses((status = 200, description = "보상 이력 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_rewards(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_rewards(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/rewards/streak",
    tag = "보상",
    responses((status = 200, description = "스트릭 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_streak(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_streak(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/rewards/weekly-score",
    tag = "보상",
    responses((status = 200, description = "주간 성실도 점수 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_weekly_scores(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_weekly_scores(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
