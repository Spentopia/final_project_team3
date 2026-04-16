// payment/dto.rs
// 결제 관련 요청/응답 구조체 (토스페이먼츠 등 PG사 연동)

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 결제 생성 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreatePaymentRequest {
    pub ledger_id: Uuid,
    pub amount: i32,
    pub order_id: Option<String>,
}

// ── 결제 승인 요청 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ConfirmPaymentRequest {
    pub payment_key: String,
    pub order_id: String,
    pub amount: i32,
}

// ── 결제 응답 ─────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PaymentResponse {
    pub id: Uuid,
    pub ledger_id: Uuid,
    pub payment_key: String,
    pub order_id: Option<String>,
    pub amount: i32,
    pub status: Option<String>,
    pub paid_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
}
