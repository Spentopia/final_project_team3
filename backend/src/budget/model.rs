// budget/model.rs
// public.budgets, public.budget_categories 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.budgets 테이블
// 월별 예산
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Budget {
    pub id: Uuid,
    pub user_id: Uuid,
    pub year: i32,
    pub month: i32,          // 1~12
    pub total_budget: i32,
    pub savings_goal: Option<i32>,
    pub ai_plan: Option<String>,  // AI가 생성한 소비 플랜
    pub created_at: Option<DateTime<Utc>>,
}

// public.budget_categories 테이블
// 예산 카테고리별 배분
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct BudgetCategory {
    pub id: Uuid,
    pub budget_id: Uuid,
    pub category: String,
    pub allocated_amount: i32,
}
