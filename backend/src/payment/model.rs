// payment/model.rs
// public.payments 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.payments 테이블
// 공유 가계부 유료 결제 (토스페이먼츠 등 PG사)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Payment {
    pub id: Uuid,
    pub user_id: Uuid,
    pub ledger_id: Uuid,
    pub payment_key: String, // PG사 결제 키
    pub order_id: Option<String>,
    pub amount: i32,
    pub status: Option<String>, // pending / done / cancelled / refunded
    pub paid_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
}
