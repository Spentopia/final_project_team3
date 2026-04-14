// community/model.rs
// public.contest_events, public.posts, public.votes, public.chatbot_logs 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.contest_events 테이블
// 아바타 콘테스트 이벤트 (관리자 등록)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContestEvent {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub start_date: DateTime<Utc>,
    pub end_date: DateTime<Utc>,
    pub status: Option<String>,              // upcoming / active / ended
    pub reward_description: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

// public.posts 테이블
// 커뮤니티 게시물 (콘테스트 참가 / 일반)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Post {
    pub id: Uuid,
    pub user_id: Uuid,
    pub contest_id: Option<Uuid>,   // 일반 게시물이면 null
    pub image_url: String,
    pub content: Option<String>,
    pub vote_count: Option<i32>,
    pub created_at: Option<DateTime<Utc>>,
}

// public.votes 테이블
// 게시물 투표 (한 번 투표하면 취소 불가)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Vote {
    pub id: Uuid,
    pub user_id: Uuid,
    pub post_id: Uuid,
    pub voted_at: Option<DateTime<Utc>>,
}

// public.chatbot_logs 테이블
// AI 챗봇 상담 로그
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatbotLog {
    pub id: Uuid,
    pub user_id: Uuid,
    pub user_message: String,
    pub bot_response: String,
    pub created_at: Option<DateTime<Utc>>,
}
