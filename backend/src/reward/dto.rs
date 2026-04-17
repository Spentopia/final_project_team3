// reward/dto.rs
// 보상, 스트릭, 주간 성실도 관련 요청/응답 구조체

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 보상 응답 ─────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct RewardResponse {
    pub id: Uuid,
    pub reward_type: String, // report / contest
    pub amount: i32,
    pub description: Option<String>,
    pub earned_at: Option<DateTime<Utc>>,
}

// ── 스트릭 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct StreakResponse {
    pub current_streak: Option<i32>,
    pub longest_streak: Option<i32>,
    pub last_record_date: Option<NaiveDate>,
}

// ── 주간 성실도 점수 응답 ──────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WeeklyScoreResponse {
    pub id: Uuid,
    pub week_start: NaiveDate,
    pub record_days_score: Option<i32>,
    pub receipt_score: Option<i32>,
    pub diary_score: Option<i32>,
    pub budget_check_score: Option<i32>,
    pub streak_score: Option<i32>,
    pub total_score: Option<i32>,
    pub reward_granted: Option<bool>,
}
