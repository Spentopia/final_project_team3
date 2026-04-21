// report/service.rs
// 소비 리포트 생성/조회 비즈니스 로직
//
// 백엔드 클라이언트 역할:
//  AI 서버를 호출해 소비 데이터 분석 결과를 받아 reports 테이블에 저장

use anyhow::{Context, Result, anyhow};
use serde::{Deserialize, Serialize};
use serde_json::Value;
use uuid::Uuid;

use super::{
    dto::{GenerateReportRequest, ReportResponse},
    model::Report,
};
use crate::state::AppState;

// ── 리포트 생성 ───────────────────────────────────────────────
// 기간 내 지출 데이터 집계 → AI 서버 분석 요청 → reports 테이블에 저장

pub async fn generate_report(
    state: &AppState,
    user_id: Uuid,
    req: GenerateReportRequest,
) -> Result<ReportResponse> {
    // 1. 기간 내 expenses 조회
    let exp_url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=gte.{}&expense_date=lte.{}&select=amount,category,expense_date",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        req.start_date,
        req.end_date,
    );

    #[derive(Serialize, Deserialize)]
    struct ExpenseInfo {
        amount: i32,
        category: String,
        expense_date: String,
    }

    let exp_res = state
        .http_client
        .get(&exp_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("expenses 조회 요청 실패")?;

    let expenses: Vec<ExpenseInfo> = if exp_res.status().is_success() {
        exp_res.json().await.unwrap_or_default()
    } else {
        vec![]
    };

    // 2. 카테고리별 합계 및 날짜별 합계 집계
    use std::collections::HashMap;
    let mut category_map: HashMap<String, i32> = HashMap::new();
    let mut daily_map: HashMap<String, i32> = HashMap::new();
    for e in &expenses {
        *category_map.entry(e.category.clone()).or_insert(0) += e.amount;
        *daily_map.entry(e.expense_date.clone()).or_insert(0) += e.amount;
    }

    let category_summary = serde_json::to_value(&category_map).ok();
    let daily_summary = serde_json::to_value(&daily_map).ok();

    // 3. AI 서버에 분석 요청 (ai_client 모듈로 중앙화)
    let ai_analysis = crate::clients::ai_client::analyze(
        state,
        crate::clients::ai_client::AnalyzePayload {
            user_id: user_id.to_string(),
            report_type: req.report_type.clone(),
            start_date: req.start_date.to_string(),
            end_date: req.end_date.to_string(),
            expenses: serde_json::to_value(&expenses).unwrap_or_default(),
            category_summary: category_summary.clone(),
        },
    )
    .await
    .ok()
    .map(|r| r.analysis);

    // 4. reports 테이블에 저장
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
        category_summary: Option<Value>,
        daily_summary: Option<Value>,
        ai_analysis: Option<String>,
    }

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
            category_summary,
            daily_summary,
            ai_analysis,
        })
        .send()
        .await
        .context("reports INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("reports INSERT 실패: {}", body));
    }

    let inserted: Vec<Report> = res.json().await.context("reports INSERT 역직렬화 실패")?;
    let report = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("reports INSERT 결과가 비어있음"))?;

    Ok(to_response(report))
}

// ── 리포트 목록 조회 ───────────────────────────────────────────

pub async fn list_reports(state: &AppState, user_id: Uuid) -> Result<Vec<ReportResponse>> {
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
        .await
        .context("reports SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("reports SELECT 실패: {}", body));
    }

    let reports: Vec<Report> = res.json().await.context("reports 역직렬화 실패")?;
    Ok(reports.into_iter().map(to_response).collect())
}

fn to_response(r: Report) -> ReportResponse {
    ReportResponse {
        id: r.id,
        report_type: r.report_type,
        start_date: r.start_date,
        end_date: r.end_date,
        category_summary: r.category_summary,
        daily_summary: r.daily_summary,
        ai_analysis: r.ai_analysis,
        created_at: r.created_at,
    }
}
