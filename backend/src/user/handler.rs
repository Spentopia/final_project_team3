// user/handler.rs
// 유저 프로필, 설정 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET   /api/user/profile   → get_profile
//  PATCH /api/user/profile   → update_profile
//  GET   /api/user/settings  → get_settings
//  PATCH /api/user/settings  → update_settings

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::{UpdateProfileRequest, UpdateSettingsRequest}, service};

// GET /api/user/profile
pub async fn get_profile(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_profile(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// PATCH /api/user/profile
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

// GET /api/user/settings
pub async fn get_settings(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_settings(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// PATCH /api/user/settings
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
