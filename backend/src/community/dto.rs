// community/dto.rs
// 콘테스트, 게시물, 투표, 챗봇 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

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
    pub contest_id: Option<Uuid>,
    pub image_url: String,
    pub content: Option<String>,
}

// ── 게시물 수정 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdatePostRequest {
    pub content: Option<String>,
}

// ── 게시물 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PostResponse {
    pub id: Uuid,
    pub user_id: Uuid,
    pub author_nickname: Option<String>,
    pub author_profile_image: Option<String>,
    pub contest_id: Option<Uuid>,
    pub image_url: String,
    pub content: Option<String>,
    pub vote_count: Option<i32>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatRequest {
    pub message: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChatResponse {
    pub response: String,
}
