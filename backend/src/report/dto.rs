// report/dto.rs

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use utoipa::ToSchema;
use uuid::Uuid;

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct Transaction {
    pub date: String,
    pub amount: f64,
    pub category: String,
    pub r#type: String,
}

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct CategoryData {
    pub key: Option<String>,
    pub name: String,
    pub amount: f64,
    pub value: f64,
    pub color: Option<String>,
}

#[derive(Debug, Deserialize, Serialize, ToSchema)]
pub struct GenerateReportRequest {
    pub report_type: String,

    pub start_date: String,

    pub end_date: String,

    pub transactions: Vec<Transaction>,

    pub total_expense: f64,

    pub budget: f64,

    pub top_category: String,

    pub top_category_percent: f64,

    pub daily_average: f64,

    pub expense_change_rate: f64,

    pub budget_usage: f64,

    pub category_data: Vec<CategoryData>,
}

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

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AnalyzeReportResponse {
    pub good: String,
    pub warning: String,
    pub advice: String,
    pub prediction: String,
    pub pattern: String,
    pub improvement: String,
}