// expense/model.rs
// public.expenses, public.receipts, public.fixed_expenses 테이블 엔티티

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.expenses 테이블
// 소비 내역
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Expense {
    pub id: Uuid,
    pub ledger_id: Uuid,
    pub user_id: Uuid,
    pub expense_date: NaiveDate,
    pub amount: i32,
    pub category: String,
    pub memo: Option<String>,
    pub one_line_diary: Option<String>,
    pub source: Option<String>, // manual / auto
    pub created_at: Option<DateTime<Utc>>,
}

// public.receipts 테이블
// 영수증 인증
// 인증 성공 시 weekly_scores.receipt_score에만 반영 (SPT 즉시 지급 없음)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Receipt {
    pub id: Uuid,
    pub expense_id: Uuid,
    pub image_url: String,
    pub is_verified: Option<bool>,
    pub matched_amount: Option<i32>,
    pub matched_date: Option<NaiveDate>,
    pub uploaded_at: Option<DateTime<Utc>>,
}

// public.fixed_expenses 테이블
// 고정 지출 마스터 (월세, 보험, 통신비, 구독료 등)
// AI 예산 플랜 생성 시 고정 지출 우선 차감 후 잔여 예산 분배
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FixedExpense {
    pub id: Uuid,
    pub user_id: Uuid,
    pub name: String,
    pub amount: i32,
    pub due_day: i32, // 1~31
    pub category: String,
    pub is_active: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
}
