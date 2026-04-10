// report/dto.rs
// 소비 리포트 관련 요청/응답 구조체

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use utoipa::ToSchema;
use uuid::Uuid;

// ── 리포트 생성 요청 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GenerateReportRequest {
    pub report_type: String,   // weekly / monthly
    pub start_date: NaiveDate,
    pub end_date: NaiveDate,
}

// ── 리포트 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ReportResponse {
    pub id: Uuid,
    pub report_type: String,
    pub start_date: NaiveDate,
    pub end_date: NaiveDate,
    pub category_summary: Option<Value>,
    pub daily_summary: Option<Value>,
    pub ai_analysis: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
