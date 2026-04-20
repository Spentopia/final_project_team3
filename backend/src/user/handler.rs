// user/handler.rs

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::{
    dto::{UpdateProfileRequest, UpdateSettingsRequest},
    service,
};
use crate::state::AppState;

#[utoipa::path(
    get, path = "/api/user/profile",
    tag = "유저",
    responses((status = 200, description = "프로필 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_profile(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_profile(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    patch, path = "/api/user/profile",
    tag = "유저",
    request_body = UpdateProfileRequest,
    responses((status = 200, description = "프로필 수정 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_profile(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<UpdateProfileRequest>,
) -> impl IntoResponse {
    match service::update_profile(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/user/settings",
    tag = "유저",
    responses((status = 200, description = "알림 설정 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn get_settings(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_settings(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    patch, path = "/api/user/settings",
    tag = "유저",
    request_body = UpdateSettingsRequest,
    responses((status = 200, description = "알림 설정 수정 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_settings(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<UpdateSettingsRequest>,
) -> impl IntoResponse {
    match service::update_settings(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
