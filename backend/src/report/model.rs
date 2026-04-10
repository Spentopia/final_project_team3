// report/model.rs
// public.reports 테이블 엔티티

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use uuid::Uuid;

// public.reports 테이블
// 주간 / 월간 소비 리포트
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Report {
    pub id: Uuid,
    pub user_id: Uuid,
    pub report_type: String,          // weekly / monthly
    pub start_date: NaiveDate,
    pub end_date: NaiveDate,
    pub category_summary: Option<Value>,  // 카테고리별 지출 요약 JSON
    pub daily_summary: Option<Value>,     // 날짜별 지출 요약 JSON
    pub ai_analysis: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
