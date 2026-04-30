// community/handler.rs

use axum::{
    Extension, Json,
    extract::{Multipart, Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
};
use serde::Deserialize;
use uuid::Uuid;

use super::{
    dto::{
        ChatRequest, CreateCommentRequest, CreatePostRequest, PostSort, PostType,
        UpdateCommentRequest, UpdatePostRequest,
    },
    service,
};
use crate::state::AppState;

#[derive(Deserialize)]
pub struct PostQuery {
    pub contest_id: Option<Uuid>,
    pub post_type: Option<PostType>,
    pub sort: Option<PostSort>,
    pub title: Option<String>,
    pub page: Option<u32>,
    pub page_size: Option<u32>,
}

fn map_community_error(error: anyhow::Error) -> axum::response::Response {
    let message = error.to_string();

    if message.contains("권한")
        || message.contains("본인 게시글")
        || message.contains("본인 댓글")
        || message.contains("관리자")
    {
        return (StatusCode::FORBIDDEN, message).into_response();
    }

    if message.contains("찾을 수 없습니다") {
        return (StatusCode::NOT_FOUND, message).into_response();
    }

    if message.contains("null value in column")
        && message.contains("image_url")
        && message.contains("violates not-null constraint")
    {
        return (
            StatusCode::BAD_REQUEST,
            "posts.image_url 컬럼이 아직 NOT NULL입니다. 사진 없는 글을 허용하려면 DB에서 image_url NOT NULL 제약을 제거해야 합니다.".to_string(),
        )
            .into_response();
    }

    if message.contains("필수")
        || message.contains("비어")
        || message.contains("contest_id")
        || message.contains("post_id")
        || message.contains("투표는")
        || message.contains("수정할 필드")
        || message.contains("멀티파트")
        || message.contains("파일")
        || message.contains("이미지만")
        || message.contains("지원하지 않는 post_type")
        || message.contains("사용할 수 없는 표현")
        || message.contains("댓글 내용")
        || message.contains("공지사항에는 댓글")
    {
        return (StatusCode::BAD_REQUEST, message).into_response();
    }

    (StatusCode::INTERNAL_SERVER_ERROR, message).into_response()
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
    match service::list_posts(
        &state,
        query.contest_id,
        query.post_type,
        query.sort.unwrap_or_default(),
        query.title,
        query.page.unwrap_or(1),
        query.page_size.unwrap_or(10),
    )
    .await
    {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/posts/{id}",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    responses((status = 200, description = "게시물 상세 조회 성공"), (status = 404, description = "게시물 없음")),
    security(("bearer_auth" = []))
)]
pub async fn get_post(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::get_post_detail(&state, post_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_community_error(e),
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
    match service::create_post(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    post, path = "/api/posts/image/upload",
    tag = "커뮤니티",
    responses((status = 200, description = "커뮤니티 이미지 업로드 성공")),
    security(("bearer_auth" = []))
)]
pub async fn upload_post_image(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    multipart: Multipart,
) -> impl IntoResponse {
    match service::upload_post_image(&state, user_id, multipart).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    patch, path = "/api/posts/{id}",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    request_body = UpdatePostRequest,
    responses((status = 200, description = "게시물 수정 성공"), (status = 403, description = "권한 없음")),
    security(("bearer_auth" = []))
)]
pub async fn update_post(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
    Json(req): Json<UpdatePostRequest>,
) -> impl IntoResponse {
    match service::update_post(&state, user_id, post_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_community_error(e),
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
        Err(e) => map_community_error(e),
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
                return (
                    StatusCode::CONFLICT,
                    "이미 투표한 게시물입니다.".to_string(),
                )
                    .into_response();
            }
            map_community_error(e)
        }
    }
}

#[utoipa::path(
    get, path = "/api/posts/{id}/comments",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    responses((status = 200, description = "댓글 목록 조회 성공"), (status = 400, description = "공지사항 댓글 불가")),
    security(("bearer_auth" = []))
)]
pub async fn list_comments(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::list_comments(&state, post_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    post, path = "/api/posts/{id}/comments",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "게시물 ID")),
    request_body = CreateCommentRequest,
    responses((status = 201, description = "댓글 작성 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn create_comment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(post_id): Path<Uuid>,
    Json(req): Json<CreateCommentRequest>,
) -> impl IntoResponse {
    match service::create_comment(&state, user_id, post_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    patch, path = "/api/comments/{id}",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "댓글 ID")),
    request_body = UpdateCommentRequest,
    responses((status = 200, description = "댓글 수정 성공"), (status = 403, description = "권한 없음")),
    security(("bearer_auth" = []))
)]
pub async fn update_comment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(comment_id): Path<Uuid>,
    Json(req): Json<UpdateCommentRequest>,
) -> impl IntoResponse {
    match service::update_comment(&state, user_id, comment_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    delete, path = "/api/comments/{id}",
    tag = "커뮤니티",
    params(("id" = Uuid, Path, description = "댓글 ID")),
    responses((status = 204, description = "댓글 삭제 성공"), (status = 403, description = "권한 없음")),
    security(("bearer_auth" = []))
)]
pub async fn delete_comment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(comment_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::delete_comment(&state, user_id, comment_id).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => map_community_error(e),
    }
}

#[utoipa::path(
    post, path = "/api/chat",
    tag = "커뮤니티",
    request_body = ChatRequest,
    responses((status = 200, description = "챗봇 응답 성공")),
    security(("bearer_auth" = []))
)]
pub async fn chat(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<ChatRequest>,
) -> impl IntoResponse {
    if req.message.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            "message는 비어 있을 수 없습니다.".to_string(),
        )
            .into_response();
    }

    match service::chat_with_bot(&state, user_id, req.message).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::BAD_GATEWAY, e.to_string()).into_response(),
    }
}
