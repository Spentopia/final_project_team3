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
    pub status: Option<String>, // upcoming / active / ended
    pub reward_description: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PostAuthor {
    pub nickname: Option<String>,
    pub profile_image: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CommentAuthor {
    pub nickname: Option<String>,
    pub profile_image: Option<String>,
    // 작성자 탈퇴 여부 판단용
    // service.rs의 to_comment_response에서 가공 처리에 사용
    pub deleted_at: Option<chrono::DateTime<chrono::Utc>>,
}

// public.posts 테이블
// 커뮤니티 게시물 (콘테스트 참가 / 일반)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Post {
    pub id: Uuid,
    pub user_id: Uuid,
    pub contest_id: Option<Uuid>, // 일반 게시물이면 null
    pub image_url: Option<String>,
    pub content: Option<String>,
    pub reaction_count: Option<i32>,
    pub created_at: Option<DateTime<Utc>>,
    pub post_type: String,
    pub title: String,
    pub updated_at: Option<DateTime<Utc>>,
    pub is_deleted: bool,
    pub deleted_at: Option<DateTime<Utc>>,

    // ── 자동 숨김 관련 ──
    // is_hidden: 신고 자동 숨김 여부.
    //   DB에 not null default false라 항상 값이 옴 → bool로 받음.
    pub is_hidden: bool,
    // hidden_reason: 숨김 사유. 자동 숨김이면 "auto_hide_inappropriate".
    //   숨김이 아니면 null이라 Option.
    pub hidden_reason: Option<String>,
    // hidden_at: 숨겨진 시각. 숨김이 아니면 null이라 Option.
    pub hidden_at: Option<DateTime<Utc>>,

    pub view_count: i32,
    pub users: Option<PostAuthor>,
}

// public.comments 테이블
// 커뮤니티 게시물 댓글
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Comment {
    pub id: Uuid,
    pub post_id: Uuid,
    pub parent_id: Option<Uuid>,
    pub user_id: Uuid,
    pub content: String,
    pub is_deleted: bool,
    pub deleted_at: Option<DateTime<Utc>>,

    // ── 자동 숨김 관련 ──
    // 의미는 Post와 동일.
    pub is_hidden: bool,
    pub hidden_reason: Option<String>,
    pub hidden_at: Option<DateTime<Utc>>,

    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
    pub users: Option<CommentAuthor>,
}

// public.reactions 테이블
// 게시물 반응
// - contest 게시글: 투표
// - free/request 게시글: 좋아요
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Reaction {
    pub id: Uuid,
    pub user_id: Uuid,
    pub post_id: Uuid,

    // DB 컬럼명이 아직 voted_at이면 voted_at 유지.
    // 나중에 reacted_at으로 바꾸면 여기도 reacted_at으로 변경.
    pub voted_at: Option<DateTime<Utc>>,
}

// public.content_reports 테이블
// 게시글/댓글/닉네임/프로필사진 신고 이력
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ContentReport {
    pub id: Uuid,
    pub reporter_id: Uuid,
    pub target_type: String,
    pub target_id: Uuid,
    pub reason: String,
    pub detail: Option<String>,
    pub status: String,
    pub created_at: Option<DateTime<Utc>>,
    pub reviewed_at: Option<DateTime<Utc>>,
    pub reviewed_by: Option<Uuid>,
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
