// expense/dto.rs
// 소비 내역, 영수증, 고정 지출 관련 요청/응답 구조체

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 웹 소비 내역 생성 요청 ─────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct CreateExpenseWebRequest {
    pub date: NaiveDate,
    pub amount: i32,
    pub category: String,
    pub memo: Option<String>,
    pub receipt_verified: bool,
    pub diary: Option<String>,
}

// ── 웹 소비 내역 응답 ─────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
#[serde(rename_all = "camelCase")]
pub struct ExpenseWebResponse {
    pub id: Uuid,
    pub date: NaiveDate,
    pub amount: i32,
    pub category: String,
    pub memo: Option<String>,
    pub receipt_verified: bool,
    pub diary: Option<String>,
}

// ── 소비 내역 생성 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateExpenseRequest {
    pub ledger_id: Uuid,
    pub expense_date: NaiveDate,
    pub amount: i32,
    pub category: String,
    pub memo: Option<String>,
    pub one_line_diary: Option<String>,
    pub source: Option<String>, // manual / auto (기본값: manual)
}

// ── 소비 내역 수정 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateExpenseRequest {
    pub expense_date: Option<NaiveDate>,
    pub amount: Option<i32>,
    pub category: Option<String>,
    pub memo: Option<String>,
    pub one_line_diary: Option<String>,
}

// ── 소비 내역 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ExpenseResponse {
    pub id: Uuid,
    pub ledger_id: Uuid,
    pub expense_date: NaiveDate,
    pub amount: i32,
    pub category: String,
    pub memo: Option<String>,
    pub one_line_diary: Option<String>,
    pub source: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}

// ── 영수증 업로드 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UploadReceiptRequest {
    pub expense_id: Uuid,
    pub image_url: String,
}

// ── 영수증 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ReceiptResponse {
    pub id: Uuid,
    pub expense_id: Uuid,
    pub image_url: String,
    pub is_verified: Option<bool>,
    pub matched_amount: Option<i32>,
    pub matched_date: Option<NaiveDate>,
    pub uploaded_at: Option<DateTime<Utc>>,
}

// ── 고정 지출 생성 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateFixedExpenseRequest {
    pub name: String,
    pub amount: i32,
    pub due_day: i32, // 1~31
    pub category: String,
}

// ── 고정 지출 수정 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateFixedExpenseRequest {
    pub name: Option<String>,
    pub amount: Option<i32>,
    pub due_day: Option<i32>,
    pub category: Option<String>,
    pub is_active: Option<bool>,
}

// ── 고정 지출 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct FixedExpenseResponse {
    pub id: Uuid,
    pub name: String,
    pub amount: i32,
    pub due_day: i32,
    pub category: String,
    pub is_active: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
}
