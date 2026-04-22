// ============================================================
// reward/dto.rs — API 요청/응답 DTO (Data Transfer Object)
//
// model.rs와의 차이:
//   - model: DB 구조 그대로 (id, user_id 등 내부 필드 포함)
//   - dto: 클라이언트에 노출할 필드만 선택적으로 포함
//
// ToSchema: utoipa가 Swagger 문서를 자동 생성할 때 사용
// ============================================================

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ────────────────────────────────────────────────────────────
// GET /api/rewards 응답 항목
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct RewardResponse {
    pub id: Uuid,
    pub reward_type: String,
    pub amount: i32,
    pub description: Option<String>,
    pub earned_at: Option<DateTime<Utc>>,
}

// ────────────────────────────────────────────────────────────
// GET /api/rewards/streak 응답
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct StreakResponse {
    pub current_streak: Option<i32>,
    pub longest_streak: Option<i32>,
    pub last_record_date: Option<NaiveDate>,
}

// ────────────────────────────────────────────────────────────
// GET /api/rewards/weekly-score 응답 항목
// GET /api/rewards/weekly-score/current 응답
//
// 변경: budget_check_score → budget_score
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WeeklyScoreResponse {
    pub id: Uuid,
    pub week_start: NaiveDate,
    pub record_days_score: Option<i32>,
    pub receipt_score: Option<i32>,
    pub diary_score: Option<i32>,
    pub budget_score: Option<i32>, // ← 변경
    pub streak_score: Option<i32>,
    pub total_score: Option<i32>,
    pub reward_granted: Option<bool>,
}

// ────────────────────────────────────────────────────────────
// POST /api/admin/contest/reward 요청 Body
// ────────────────────────────────────────────────────────────
#[derive(Debug, Deserialize, ToSchema)]
pub struct ContestRewardRequest {
    pub user_id: Uuid, // 수상자 UUID
    pub rank: u8,      // 1, 2, 3만 허용
}
