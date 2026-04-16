// budget/service.rs
// 예산, 카테고리 배분, AI 플랜 생성 비즈니스 로직
//
// Supabase REST API 패턴:
//  - Authorization: Bearer {supabase_secret_key}
//  - apikey: {supabase_secret_key}
//  - Prefer: return=representation → INSERT/PATCH 후 변경 row 반환
//
// AI 플랜 생성:
//  백엔드가 클라이언트 역할로 AI 서버(FastAPI)를 호출한다.
//  고정 지출 조회 → AI 서버 요청 → 응답을 budgets.ai_plan에 저장

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{
        AiPlanResponse, BudgetCategoryItem, BudgetResponse, CreateBudgetRequest,
        GenerateAiPlanRequest, UpdateBudgetCategoriesRequest, UpdateBudgetRequest,
    },
    model::{Budget, BudgetCategory},
};

// ── 예산 조회 ─────────────────────────────────────────────────

pub async fn get_budget(
    state: &AppState,
    user_id: Uuid,
    year: i32,
    month: i32,
) -> Result<Option<BudgetResponse>> {
    let url = format!(
        "{}/rest/v1/budgets?user_id=eq.{}&year=eq.{}&month=eq.{}&select=*",
        state.config.supabase_url.trim_end_matches('/'),
        user_id, year, month,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("budgets SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets SELECT 실패: {}", body));
    }

    let budgets: Vec<Budget> = res.json().await.context("budgets 역직렬화 실패")?;
    let budget = match budgets.into_iter().next() {
        Some(b) => b,
        None => return Ok(None),
    };

    let categories = fetch_categories(state, budget.id).await?;

    Ok(Some(BudgetResponse {
        id: budget.id,
        year: budget.year,
        month: budget.month,
        total_budget: budget.total_budget,
        savings_goal: budget.savings_goal,
        ai_plan: budget.ai_plan,
        categories,
        created_at: budget.created_at,
    }))
}

// ── 예산 생성 ─────────────────────────────────────────────────

pub async fn create_budget(
    state: &AppState,
    user_id: Uuid,
    req: CreateBudgetRequest,
) -> Result<BudgetResponse> {
    let url = format!(
        "{}/rest/v1/budgets",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        year: i32,
        month: i32,
        total_budget: i32,
        savings_goal: Option<i32>,
    }

    let res = state.http_client.post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertPayload {
            user_id,
            year: req.year,
            month: req.month,
            total_budget: req.total_budget,
            savings_goal: req.savings_goal,
        })
        .send().await.context("budgets INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets INSERT 실패: {}", body));
    }

    let inserted: Vec<Budget> = res.json().await.context("budgets INSERT 역직렬화 실패")?;
    let budget = inserted.into_iter().next()
        .ok_or_else(|| anyhow!("budgets INSERT 결과가 비어있음"))?;

    Ok(BudgetResponse {
        id: budget.id,
        year: budget.year,
        month: budget.month,
        total_budget: budget.total_budget,
        savings_goal: budget.savings_goal,
        ai_plan: budget.ai_plan,
        categories: vec![],
        created_at: budget.created_at,
    })
}

// ── 예산 수정 ─────────────────────────────────────────────────

pub async fn update_budget(
    state: &AppState,
    user_id: Uuid,
    budget_id: Uuid,
    req: UpdateBudgetRequest,
) -> Result<BudgetResponse> {
    let url = format!(
        "{}/rest/v1/budgets?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id, user_id,
    );

    let res = state.http_client.patch(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&req)
        .send().await.context("budgets PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets PATCH 실패: {}", body));
    }

    let updated: Vec<Budget> = res.json().await.context("budgets PATCH 역직렬화 실패")?;
    let budget = updated.into_iter().next()
        .ok_or_else(|| anyhow!("수정된 예산을 찾을 수 없음"))?;

    let categories = fetch_categories(state, budget.id).await?;

    Ok(BudgetResponse {
        id: budget.id,
        year: budget.year,
        month: budget.month,
        total_budget: budget.total_budget,
        savings_goal: budget.savings_goal,
        ai_plan: budget.ai_plan,
        categories,
        created_at: budget.created_at,
    })
}

// ── 카테고리 배분 일괄 수정 ────────────────────────────────────
// 기존 categories 삭제 후 새로 INSERT (upsert 대신 delete + insert)

pub async fn update_categories(
    state: &AppState,
    budget_id: Uuid,
    req: UpdateBudgetCategoriesRequest,
) -> Result<Vec<BudgetCategoryItem>> {
    // 기존 카테고리 삭제
    let del_url = format!(
        "{}/rest/v1/budget_categories?budget_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
    );
    let del_res = state.http_client.delete(&del_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("budget_categories DELETE 요청 실패")?;

    if !del_res.status().is_success() {
        let body = del_res.text().await.unwrap_or_default();
        return Err(anyhow!("budget_categories DELETE 실패: {}", body));
    }

    if req.categories.is_empty() {
        return Ok(vec![]);
    }

    // 새 카테고리 INSERT
    #[derive(Serialize)]
    struct InsertItem {
        budget_id: Uuid,
        category: String,
        allocated_amount: i32,
    }

    let payload: Vec<InsertItem> = req.categories.iter().map(|c| InsertItem {
        budget_id,
        category: c.category.clone(),
        allocated_amount: c.allocated_amount,
    }).collect();

    let ins_url = format!(
        "{}/rest/v1/budget_categories",
        state.config.supabase_url.trim_end_matches('/'),
    );
    let ins_res = state.http_client.post(&ins_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&payload)
        .send().await.context("budget_categories INSERT 요청 실패")?;

    if !ins_res.status().is_success() {
        let body = ins_res.text().await.unwrap_or_default();
        return Err(anyhow!("budget_categories INSERT 실패: {}", body));
    }

    let inserted: Vec<BudgetCategory> = ins_res.json().await
        .context("budget_categories INSERT 역직렬화 실패")?;

    Ok(inserted.into_iter().map(|c| BudgetCategoryItem {
        category: c.category,
        allocated_amount: c.allocated_amount,
    }).collect())
}

// ── AI 예산 플랜 생성 ──────────────────────────────────────────
// 백엔드 → AI 서버 클라이언트 호출

pub async fn generate_ai_plan(
    state: &AppState,
    user_id: Uuid,
    req: GenerateAiPlanRequest,
) -> Result<AiPlanResponse> {
    // 1. 해당 예산 조회
    let budget_url = format!(
        "{}/rest/v1/budgets?id=eq.{}&user_id=eq.{}&select=*",
        state.config.supabase_url.trim_end_matches('/'),
        req.budget_id, user_id,
    );
    let b_res = state.http_client.get(&budget_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("budgets 조회 요청 실패")?;

    if !b_res.status().is_success() {
        let body = b_res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets 조회 실패: {}", body));
    }
    let budgets: Vec<Budget> = b_res.json().await.context("budgets 역직렬화 실패")?;
    let budget = budgets.into_iter().next()
        .ok_or_else(|| anyhow!("예산을 찾을 수 없음"))?;

    // 2. 고정 지출 조회
    let fe_url = format!(
        "{}/rest/v1/fixed_expenses?user_id=eq.{}&is_active=eq.true&select=name,amount,category",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    #[derive(Serialize, Deserialize)]
    struct FixedExpenseInfo {
        name: String,
        amount: i32,
        category: String,
    }

    let fe_res = state.http_client.get(&fe_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("fixed_expenses 조회 요청 실패")?;

    let fixed_expenses: Vec<FixedExpenseInfo> = if fe_res.status().is_success() {
        fe_res.json().await.unwrap_or_default()
    } else {
        vec![]
    };

    // 3. AI 서버에 예산 플랜 요청 (백엔드 → AI 서버 클라이언트 역할)
    #[derive(Serialize)]
    struct AiPlanRequestPayload {
        user_id: String,
        total_budget: i32,
        savings_goal: Option<i32>,
        year: i32,
        month: i32,
        fixed_expenses: Vec<FixedExpenseInfo>,
    }

    #[derive(Deserialize)]
    struct AiPlanAiResponse {
        plan: String,
        categories: Vec<BudgetCategoryItem>,
    }

    let ai_url = format!("{}/api/v1/budget-plan", state.config.ai_server_url.trim_end_matches('/'));
    let ai_res = state.http_client.post(&ai_url)
        .json(&AiPlanRequestPayload {
            user_id: user_id.to_string(),
            total_budget: budget.total_budget,
            savings_goal: budget.savings_goal,
            year: budget.year,
            month: budget.month,
            fixed_expenses,
        })
        .send().await.context("AI 서버 budget-plan 요청 실패")?;

    if !ai_res.status().is_success() {
        let body = ai_res.text().await.unwrap_or_default();
        return Err(anyhow!("AI 서버 budget-plan 실패: {}", body));
    }

    let ai_plan: AiPlanAiResponse = ai_res.json().await.context("AI 플랜 응답 역직렬화 실패")?;

    // 4. budgets.ai_plan 업데이트
    #[derive(Serialize)]
    struct PatchAiPlan { ai_plan: String }

    let patch_url = format!(
        "{}/rest/v1/budgets?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.budget_id,
    );
    let _ = state.http_client.patch(&patch_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&PatchAiPlan { ai_plan: ai_plan.plan.clone() })
        .send().await.context("budgets ai_plan 업데이트 실패")?;

    Ok(AiPlanResponse {
        ai_plan: ai_plan.plan,
        categories: ai_plan.categories,
    })
}

// ── 내부 유틸 ─────────────────────────────────────────────────

async fn fetch_categories(state: &AppState, budget_id: Uuid) -> Result<Vec<BudgetCategoryItem>> {
    let url = format!(
        "{}/rest/v1/budget_categories?budget_id=eq.{}&select=category,allocated_amount",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("budget_categories SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Ok(vec![]);
    }

    let rows: Vec<BudgetCategory> = res.json().await.unwrap_or_default();
    Ok(rows.into_iter().map(|c| BudgetCategoryItem {
        category: c.category,
        allocated_amount: c.allocated_amount,
    }).collect())
}
