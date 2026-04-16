// clients/ai_client.rs
//
// AI 서버(FastAPI)와 통신하는 클라이언트 모듈.
// AI 서버 엔드포인트가 바뀌면 여기만 수정하면 된다.
//
// ── AI 서버 엔드포인트 목록 ──────────────────────────────────
//  POST /api/v1/chat        → 챗봇 대화
//  POST /api/v1/analyze     → 소비 분석 / 리포트 AI 분석
//  POST /api/v1/budget-plan → AI 예산 플랜 생성
//  POST /api/v1/receipt     → 영수증 OCR 분석
//  GET  /api/v1/history     → 대화 이력 조회

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use serde_json::Value;

use crate::state::AppState;

// ── 엔드포인트 상수 ───────────────────────────────────────────
const CHAT_PATH: &str = "/api/v1/chat";
const ANALYZE_PATH: &str = "/api/v1/analyze";
const BUDGET_PLAN_PATH: &str = "/api/v1/budget-plan";
const RECEIPT_PATH: &str = "/api/v1/receipt";
const HISTORY_PATH: &str = "/api/v1/history";

// ── 챗봇 ─────────────────────────────────────────────────────

#[derive(Serialize)]
pub struct ChatPayload {
    pub user_id: String,
    pub message: String,
}

#[derive(Deserialize)]
pub struct ChatResult {
    pub response: String,
}

pub async fn chat(state: &AppState, payload: ChatPayload) -> Result<ChatResult> {
    let url = format!("{}{}", state.config.ai_server_url.trim_end_matches('/'), CHAT_PATH);
    let res = state.http_client.post(&url)
        .json(&payload)
        .send().await.context("AI 서버 chat 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 chat 실패: {}", body));
    }
    res.json::<ChatResult>().await.context("AI chat 응답 역직렬화 실패")
}

// ── 소비 분석 / 리포트 ────────────────────────────────────────

#[derive(Serialize)]
pub struct AnalyzePayload {
    pub user_id: String,
    pub report_type: String,
    pub start_date: String,
    pub end_date: String,
    pub expenses: Value,
    pub category_summary: Option<Value>,
}

#[derive(Deserialize)]
pub struct AnalyzeResult {
    pub analysis: String,
}

pub async fn analyze(state: &AppState, payload: AnalyzePayload) -> Result<AnalyzeResult> {
    let url = format!("{}{}", state.config.ai_server_url.trim_end_matches('/'), ANALYZE_PATH);
    let res = state.http_client.post(&url)
        .json(&payload)
        .send().await.context("AI 서버 analyze 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 analyze 실패: {}", body));
    }
    res.json::<AnalyzeResult>().await.context("AI analyze 응답 역직렬화 실패")
}

// ── AI 예산 플랜 ──────────────────────────────────────────────

#[derive(Serialize)]
pub struct BudgetPlanPayload {
    pub user_id: String,
    pub total_budget: i32,
    pub savings_goal: Option<i32>,
    pub year: i32,
    pub month: i32,
    pub fixed_expenses: Value,
}

#[derive(Deserialize)]
pub struct BudgetPlanResult {
    pub plan: String,
    pub categories: Value,
}

pub async fn budget_plan(state: &AppState, payload: BudgetPlanPayload) -> Result<BudgetPlanResult> {
    let url = format!("{}{}", state.config.ai_server_url.trim_end_matches('/'), BUDGET_PLAN_PATH);
    let res = state.http_client.post(&url)
        .json(&payload)
        .send().await.context("AI 서버 budget-plan 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 budget-plan 실패: {}", body));
    }
    res.json::<BudgetPlanResult>().await.context("AI budget-plan 응답 역직렬화 실패")
}

// ── 영수증 OCR ────────────────────────────────────────────────

#[derive(Serialize)]
pub struct ReceiptPayload {
    pub user_id: String,
    pub image_url: String,
}

#[derive(Deserialize)]
pub struct ReceiptResult {
    pub amount: Option<i32>,
    pub date: Option<String>,
    pub items: Option<Value>,
}

pub async fn receipt(state: &AppState, payload: ReceiptPayload) -> Result<ReceiptResult> {
    let url = format!("{}{}", state.config.ai_server_url.trim_end_matches('/'), RECEIPT_PATH);
    let res = state.http_client.post(&url)
        .json(&payload)
        .send().await.context("AI 서버 receipt 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 receipt 실패: {}", body));
    }
    res.json::<ReceiptResult>().await.context("AI receipt 응답 역직렬화 실패")
}

// ── 대화 이력 조회 ────────────────────────────────────────────

#[derive(Deserialize)]
pub struct HistoryResult {
    pub messages: Value,
}

pub async fn history(state: &AppState, user_id: &str) -> Result<HistoryResult> {
    let url = format!(
        "{}{}?user_id={}",
        state.config.ai_server_url.trim_end_matches('/'),
        HISTORY_PATH,
        user_id,
    );
    let res = state.http_client.get(&url)
        .send().await.context("AI 서버 history 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 history 실패: {}", body));
    }
    res.json::<HistoryResult>().await.context("AI history 응답 역직렬화 실패")
}
