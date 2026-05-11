// report/service.rs

use anyhow::{Context, Result, anyhow};
use serde::Serialize;
use serde_json::{json, Value};
use uuid::Uuid;

use super::{
    dto::{
        AnalyzeReportResponse,
        GenerateReportRequest,
        ReportResponse,
    },
    model::Report,
};

use crate::state::AppState;

pub async fn generate_report(
    state: &AppState,
    user_id: Uuid,
    req: GenerateReportRequest,
) -> Result<AnalyzeReportResponse> {

    // AI 서버 호출
    let ai_result = crate::clients::ai_client::analyze(
        state,
        crate::clients::ai_client::AnalyzePayload {
            user_id: user_id.to_string(),

            report_type: req.report_type.clone(),
            start_date: req.start_date.clone(),
            end_date: req.end_date.clone(),

            transactions: serde_json::to_value(&req.transactions)?,

            total_expense: req.total_expense,
            budget: req.budget,

            top_category: req.top_category.clone(),
            top_category_percent: req.top_category_percent,

            daily_average: req.daily_average,
            expense_change_rate: req.expense_change_rate,
            budget_usage: req.budget_usage,

            category_data: serde_json::to_value(&req.category_data)?,
        },
    )
        .await?;

    // reports 저장
    let url = format!(
        "{}/rest/v1/reports",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        report_type: String,
        start_date: String,
        end_date: String,
        category_summary: Value,
        ai_analysis: Value,
    }

    let ai_json = json!({
        "good": ai_result.good,
        "warning": ai_result.warning,
        "advice": ai_result.advice,
        "prediction": ai_result.prediction,
        "pattern": ai_result.pattern,
        "improvement": ai_result.improvement,
    });

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertPayload {
            user_id,

            report_type: req.report_type,

            start_date: req.start_date.to_string(),
            end_date: req.end_date.to_string(),

            category_summary: serde_json::to_value(&req.category_data)?,

            ai_analysis: ai_json.clone(),
        })
        .send()
        .await
        .context("reports INSERT 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!(body));
    }

    Ok(AnalyzeReportResponse {
        good: ai_result.good,
        warning: ai_result.warning,
        advice: ai_result.advice,
        prediction: ai_result.prediction,
        pattern: ai_result.pattern,
        improvement: ai_result.improvement,
    })
}

pub async fn list_reports(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<ReportResponse>> {

    let url = format!(
        "{}/rest/v1/reports?user_id=eq.{}&select=*&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await?;

    let reports: Vec<Report> = res.json().await?;

    Ok(reports
        .into_iter()
        .map(|r| ReportResponse {
            id: r.id,
            report_type: r.report_type,
            start_date: r.start_date,
            end_date: r.end_date,
            category_summary: r.category_summary,
            daily_summary: r.daily_summary,
            ai_analysis: r.ai_analysis,
            created_at: r.created_at,
        })
        .collect())
}