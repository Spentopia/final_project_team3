// backend/src/system/model.rs
//
// Supabase REST 응답을 역직렬화하기 위한 DB row 모델.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct SystemStatusRow {
    pub id: String,

    pub enabled: bool,
    pub blocking: bool,
    pub status_type: String,

    pub title: String,
    pub message: String,

    pub starts_at: Option<DateTime<Utc>>,
    pub ends_at: Option<DateTime<Utc>>,

    pub updated_at: DateTime<Utc>,
    pub updated_by: Option<Uuid>,
}