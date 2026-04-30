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
    pub vote_count: Option<i32>,
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

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatRequest {
    pub message: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatResponse {
    pub response: String,
}
