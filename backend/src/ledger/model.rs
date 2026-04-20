// ledger/model.rs
// public.ledgers, public.ledger_members 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.ledgers 테이블
// 가계부 (개인 / 공유)
// is_shared = true이면 공유 가계부 (결제 완료 시 자동 전환)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Ledger {
    pub id: Uuid,
    pub user_id: Uuid, // 생성자
    pub title: String,
    pub is_shared: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
}

// public.ledger_members 테이블
// 공유 가계부 멤버 (owner / member)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct LedgerMember {
    pub id: Uuid,
    pub ledger_id: Uuid,
    pub user_id: Uuid,
    pub role: Option<String>, // owner / member
    pub joined_at: Option<DateTime<Utc>>,
}
