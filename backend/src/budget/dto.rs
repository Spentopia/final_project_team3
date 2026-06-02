// budget/dto.rs
// 예산, 카테고리 배분 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use utoipa::ToSchema;
use uuid::Uuid;

// ── 예산 생성 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateBudgetRequest {
    pub year: i32,
    pub month: i32, // 1~12
    pub total_budget: i32,
    pub savings_goal: Option<i32>,
}

// ── 예산 수정 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateBudgetRequest {
    pub total_budget: Option<i32>,
    pub savings_goal: Option<i32>,
    #[serde(default)]
    pub lock_budget: bool,
}

// ── 카테고리 배분 항목 ─────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct BudgetCategoryItem {
    pub category: String,
    pub allocated_amount: i32,
}

// ── 카테고리 배분 일괄 수정 요청 ──────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateBudgetCategoriesRequest {
    pub categories: Vec<BudgetCategoryItem>,
}

// ── 예산 응답 (카테고리 포함) ─────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct BudgetResponse {
    pub id: Uuid,
    pub year: i32,
    pub month: i32,
    pub total_budget: i32,
    pub savings_goal: Option<i32>,
    pub ai_plan: Option<String>,
    pub categories: Vec<BudgetCategoryItem>,
    pub locked_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
}

// ── AI 예산 플랜 생성 요청 ────────────────────────────────────
// 고정 지출 데이터는 서버에서 DB 조회하므로 별도 파라미터 없음

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GenerateAiPlanRequest {
    pub budget_id: Uuid,
    pub total_budget: Option<i32>,
    pub savings_goal: Option<i32>,
    pub food: Option<i32>,
    pub transport: Option<i32>,
    pub living: Option<i32>,
    pub leisure: Option<i32>,
    pub fixed_expenses: Option<Value>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema, Default)]
pub struct GenerateAiPlanBody {
    pub total_budget: Option<i32>,
    pub savings_goal: Option<i32>,
    pub food: Option<i32>,
    pub transport: Option<i32>,
    pub living: Option<i32>,
    pub leisure: Option<i32>,
    pub fixed_expenses: Option<Value>,
    pub year: Option<i32>,
    pub month: Option<i32>,
}

// ── AI 예산 플랜 응답 ─────────────────────────────────────────

#[derive(Serialize, Deserialize, Clone, utoipa::ToSchema)]
pub struct Plan {
    pub name: String,
    pub budget: i32,
    pub savings: i32,
    pub food: i32,
    pub transport: i32,
    pub living: i32,
    pub leisure: i32,
    pub description: String,
}

#[derive(Serialize, Deserialize, utoipa::ToSchema)]
pub struct AiPlanResponse {
    pub plans: Vec<Plan>,
}
