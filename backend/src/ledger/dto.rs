// ledger/dto.rs
// 가계부, 멤버 관련 요청/응답 구조체

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;
use chrono::{DateTime, Utc};

// ── 가계부 생성 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateLedgerRequest {
    pub title: String,
}

// ── 가계부 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct LedgerResponse {
    pub id: Uuid,
    pub title: String,
    pub is_shared: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
}

// ── 가계부 수정 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateLedgerRequest {
    pub title: String,
}

// ── 멤버 초대 요청 ────────────────────────────────────────────
// owner만 다른 유저를 초대할 수 있음

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct InviteMemberRequest {
    pub user_id: Uuid,
}

// ── 멤버 응답 ─────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct LedgerMemberResponse {
    pub id: Uuid,
    pub user_id: Uuid,
    pub nickname: Option<String>,
    pub role: Option<String>,
    pub joined_at: Option<DateTime<Utc>>,
}
