// community/handler.rs

use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use serde::Deserialize;
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::{ChatRequest, CreatePostRequest}, service};

#[derive(Deserialize)]
pub struct PostQuery {
    pub contest_id: Option<Uuid>,
}

#[utoipa::path(
    get, path = "/api/contests",
    tag = "커뮤니티",
    responses((status = 200, description = "콘테스트 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_contests(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_contests(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/posts",
    tag = "커뮤니티",
    params(("contest_id" = Option<Uuid>, Query, description = "콘테스트 ID (선택)")),
    responses((status = 200, description = "게시물 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_posts(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
    Query(query): Query<PostQuery>,
) -> impl IntoResponse {
    match service::list_posts(&state, query.contest_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/posts",
    tag = "커뮤니티",
    request_body = CreatePostRequest,
    responses((status = 201, description = "게시물 생성 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn create_post(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreatePostRequest>,
) -> impl IntoResponse {
    if req.image_url.trim().is_empty() {
        return (StatusCode::BAD_REQUEST, "image_url은 필수입니다.".to_string()).into_response();
    }
    match service::create_post(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    delete, path = "/api/posts/{id}",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    responses((status = 204, description = "게시물 삭제 성공")),
    security(("bearer_auth" = []))
)]
pub async fn delete_post(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::delete_post(&state, user_id, post_id).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/posts/{id}/vote",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    responses((status = 200, description = "투표 성공"), (status = 409, description = "이미 투표함")),
    security(("bearer_auth" = []))
)]
pub async fn vote_post(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::vote_post(&state, user_id, post_id).await {
        Ok(_) => StatusCode::OK.into_response(),
        Err(e) => {
            if e.to_string().contains("duplicate") || e.to_string().contains("unique") {
                return (StatusCode::CONFLICT, "이미 투표한 게시물입니다.".to_string()).into_response();
            }
            (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response()
        }
    }
}

#[utoipa::path(
    post, path = "/api/chat",
    tag = "챗봇",
    request_body = ChatRequest,
    responses((status = 200, description = "챗봇 응답 성공"), (status = 400, description = "메시지 없음")),
    security(("bearer_auth" = []))
)]
pub async fn chat(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<ChatRequest>,
) -> impl IntoResponse {
    if req.message.trim().is_empty() {
        return (StatusCode::BAD_REQUEST, "message는 필수입니다.".to_string()).into_response();
    }
    match service::chat(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/chat/logs",
    tag = "챗봇",
    responses((status = 200, description = "챗봇 대화 이력 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_chat_logs(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_chat_logs(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
