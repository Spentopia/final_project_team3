// community/dto.rs
// 콘테스트, 게시물, 투표, 챗봇 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 게시물 타입 / 정렬 ───────────────────────────────────────

#[derive(Debug, Clone, Copy, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "snake_case")]
pub enum PostType {
    Notice,
    Request,
    Contest,
    Free,
}

impl PostType {
    pub fn as_str(self) -> &'static str {
        match self {
            PostType::Notice => "notice",
            PostType::Request => "request",
            PostType::Contest => "contest",
            PostType::Free => "free",
        }
    }
}

#[derive(Debug, Clone, Copy, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "snake_case")]
pub enum PostSort {
    Date,
    Likes,
    Views,
}

impl Default for PostSort {
    fn default() -> Self {
        Self::Date
    }
}

// ── 콘테스트 이벤트 응답 ──────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ContestEventResponse {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub start_date: DateTime<Utc>,
    pub end_date: DateTime<Utc>,
    pub status: Option<String>, // upcoming / active / ended
    pub reward_description: Option<String>,
}

// ── 게시물 생성 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreatePostRequest {
    pub post_type: PostType,
    pub title: String,
    pub contest_id: Option<Uuid>,
    pub image_url: Option<String>,
    pub content: Option<String>,
}

// ── 게시물 수정 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdatePostRequest {
    pub title: Option<String>,
    pub image_url: Option<String>,
    pub content: Option<String>,
}

// ── 게시물 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PostResponse {
    pub id: Uuid,
    pub user_id: Uuid,
    pub author_nickname: Option<String>,
    pub author_profile_image: Option<String>,
    pub author_profile_image_url: Option<String>,
    pub contest_id: Option<Uuid>,
    pub post_type: String,
    pub title: String,
    pub image_url: Option<String>,
    pub content: Option<String>,
    pub reaction_count: Option<i32>,
    // DB 컬럼 아님.
    // 현재 로그인한 사용자가 이 게시글에 이미 반응했는지
    // 백엔드가 reactions 테이블을 조회해서 계산한 응답 전용 값.
    pub is_reacted: bool,
    pub view_count: i32,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PostListResponse {
    pub items: Vec<PostResponse>,
    pub total_count: i64,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UploadCommunityImageResponse {
    pub path: String,
}

// ── 댓글 요청/응답 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CommentResponse {
    pub id: Uuid,
    pub post_id: Uuid,
    
    // null이면 일반 댓글
    // 값이 있으면 해당 댓글의 대댓글
    pub parent_id: Option<Uuid>,
    
    pub user_id: Uuid,
    pub author_nickname: Option<String>,
    pub author_profile_image: Option<String>,
    pub author_profile_image_url: Option<String>,
    pub content: String,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateCommentRequest {
    pub content: String,
    pub parent_id: Option<Uuid>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateCommentRequest {
    pub content: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatRequest {
    pub message: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatResponse {
    pub response: String,
}
