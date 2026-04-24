// user/handler.rs

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::{
    dto::{ChangePasswordRequest, UpdateProfileRequest, UpdateSettingsRequest},
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
        Err(e) => {
            let msg = e.to_string();
            if msg.contains("소셜 로그인") {
                (StatusCode::FORBIDDEN, msg).into_response()
            } else if msg.contains("사용할 수 없는 닉네임")
                || msg.contains("닉네임")
                || msg.contains("한 줄 소개에 사용할 수 없는 표현")
            {
                (StatusCode::BAD_REQUEST, msg).into_response()
            } else {
                (StatusCode::INTERNAL_SERVER_ERROR, msg).into_response()
            }
        }
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

#[utoipa::path(
      patch, path = "/api/user/password",
      tag = "유저",
      request_body = ChangePasswordRequest,
      responses(
          (status = 200, description = "비밀번호 변경 성공"),
          (status = 400, description = "현재 비밀번호 불일치 또는 유효성 오류"),
          (status = 403, description = "소셜 로그인 계정"),
      ),
      security(("bearer_auth" = []))
)]
pub async fn change_password(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<ChangePasswordRequest>,
) -> impl IntoResponse {
    match service::change_password(&state, user_id, &req.current_password, &req.new_password).await
    {
        Ok(()) => StatusCode::OK.into_response(),
        Err(e) => {
            let msg = e.to_string();
            if msg.contains("소셜 로그인") {
                (StatusCode::FORBIDDEN, msg).into_response()
            } else if msg.contains("올바르지 않습니다")
                || msg.contains("포함해야")
                || msg.contains("이상이어야")
            {
                (StatusCode::BAD_REQUEST, msg).into_response()
            } else {
                (StatusCode::INTERNAL_SERVER_ERROR, msg).into_response()
            }
        }
    }
}
