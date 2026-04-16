// community/handler.rs
// 콘테스트, 게시물, 투표, 챗봇 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET    /api/contests            → list_contests
//  GET    /api/posts               → list_posts (query: contest_id)
//  POST   /api/posts               → create_post
//  DELETE /api/posts/:id           → delete_post
//  POST   /api/posts/:id/vote      → vote_post
//  POST   /api/chat                → chat
//  GET    /api/chat/logs           → list_chat_logs

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

// GET /api/contests
pub async fn list_contests(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_contests(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// GET /api/posts?contest_id=
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

// POST /api/posts
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

// DELETE /api/posts/:id
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

// POST /api/posts/:id/vote
pub async fn vote_post(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::vote_post(&state, user_id, post_id).await {
        Ok(_) => StatusCode::OK.into_response(),
        Err(e) => {
            // 중복 투표는 409로 처리
            if e.to_string().contains("duplicate") || e.to_string().contains("unique") {
                return (StatusCode::CONFLICT, "이미 투표한 게시물입니다.".to_string()).into_response();
            }
            (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response()
        }
    }
}

// POST /api/chat
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

// GET /api/chat/logs
pub async fn list_chat_logs(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_chat_logs(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
