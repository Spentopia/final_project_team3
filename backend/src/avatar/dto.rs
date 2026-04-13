// avatar/dto.rs
// 아바타, 아이템, 가챠, 스크린샷 관련 요청/응답 구조체

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;
use chrono::{DateTime, Utc};

// ── 아바타 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AvatarResponse {
    pub id: Uuid,
    pub grade: Option<String>,
    pub image_url: String,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub condition_met: Option<String>,
    pub is_active: Option<bool>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// ── 아이템 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UserItemResponse {
    pub id: Uuid,
    pub item_id: Uuid,
    pub name: String,
    pub category: String,
    pub rarity: String,
    pub image_url: String,
    pub is_equipped: Option<bool>,
    pub is_nft: Option<bool>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// ── 가챠 티켓 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GachaTicketResponse {
    pub id: Uuid,
    pub is_used: Option<bool>,
    pub used_at: Option<DateTime<Utc>>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// ── 가챠 사용 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UseGachaTicketRequest {
    pub ticket_id: Uuid,
}

// ── 가챠 결과 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GachaResultResponse {
    pub item_id: Uuid,
    pub name: String,
    pub category: String,
    pub rarity: String,
    pub image_url: String,
}

// ── 스크린샷 생성 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateScreenshotRequest {
    pub image_url: String,
    pub caption: Option<String>,
}

// ── 스크린샷 응답 ─────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ScreenshotResponse {
    pub id: Uuid,
    pub image_url: String,
    pub caption: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
