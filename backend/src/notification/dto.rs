// notification/dto.rs
// 알림 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 알림 응답 ─────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct NotificationResponse {
    pub id: Uuid,
    pub notification_type: String,  // budget_alert / reward / streak
    pub message: String,
    pub is_read: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
}

// ── 읽음 처리 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct MarkReadRequest {
    pub notification_ids: Vec<Uuid>,
}
