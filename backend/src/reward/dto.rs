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
// GET /api/rewards/monthly-score 응답 항목
// GET /api/rewards/monthly-score/current 응답
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct MonthlyScoreResponse {
    pub id: Uuid,
    pub month_start: NaiveDate,
    pub record_days_score: Option<i32>,
    pub receipt_score: Option<i32>,
    pub diary_score: Option<i32>,
    pub budget_score: Option<i32>,
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

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct BoxCountResponse {
    pub box_count: i32,
    pub daily_earned: i32, // 오늘 당일 영수증 인증으로 받은 뽑기권 수
    pub daily_limit: i32,  // 하루 최대 (당일 영수증 인증) 뽑기권 수
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UnityAvatarItemResponse {
    pub inventory_id: Uuid,
    pub item_id: Uuid,
    pub name: String,
    pub image_url: String,
    pub is_equipped: bool,
    pub slot_name: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct OpenBoxResponse {
    pub remaining_box_count: i32,
    pub reward_type: String, // "miss" | "spt" | "avatar"
    pub is_win: bool,
    pub message: String,
    pub spt_amount: Option<i32>,
    pub item: Option<UnityAvatarItemResponse>,
}
