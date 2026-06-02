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

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{
    dto::{
        AiPlanResponse, BudgetCategoryItem, BudgetResponse, CreateBudgetRequest,
        GenerateAiPlanRequest, UpdateBudgetCategoriesRequest, UpdateBudgetRequest,
    },
    model::{Budget, BudgetCategory},
};
use crate::state::AppState;

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
        user_id,
        year,
        month,
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
        .context("budgets SELECT 요청 실패")?;

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
        locked_at: budget.locked_at,
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
            year: req.year,
            month: req.month,
            total_budget: req.total_budget,
            savings_goal: req.savings_goal,
        })
        .send()
        .await
        .context("budgets INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets INSERT 실패: {}", body));
    }

    let inserted: Vec<Budget> = res.json().await.context("budgets INSERT 역직렬화 실패")?;
    let budget = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("budgets INSERT 결과가 비어있음"))?;

    Ok(BudgetResponse {
        id: budget.id,
        year: budget.year,
        month: budget.month,
        total_budget: budget.total_budget,
        savings_goal: budget.savings_goal,
        ai_plan: budget.ai_plan,
        categories: vec![],
        locked_at: budget.locked_at,
        created_at: budget.created_at,
    })
}

async fn fetch_budget_for_user(state: &AppState, user_id: Uuid, budget_id: Uuid) -> Result<Budget> {
    let url = format!(
        "{}/rest/v1/budgets?id=eq.{}&user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
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
        .context("budgets 잠금 확인 SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets 잠금 확인 SELECT 실패: {}", body));
    }

    let rows: Vec<Budget> = res.json().await.context("budgets 잠금 확인 역직렬화 실패")?;
    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("budget_not_found"))
}

// ── 예산 수정 ─────────────────────────────────────────────────

pub async fn update_budget(
    state: &AppState,
    user_id: Uuid,
    budget_id: Uuid,
    req: UpdateBudgetRequest,
) -> Result<BudgetResponse> {
    let current = fetch_budget_for_user(state, user_id, budget_id).await?;

    if current.locked_at.is_some() {
        return Err(anyhow!("budget_locked"));
    }

    let url = format!(
        "{}/rest/v1/budgets?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
        user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        total_budget: Option<i32>,
        savings_goal: Option<i32>,
        #[serde(skip_serializing_if = "Option::is_none")]
        ai_plan: Option<String>,
        locked_at: Option<DateTime<Utc>>,
    }

    let ai_plan = match (&current.ai_plan, &req.selected_plan_id) {
        (_, None) => None,
        (Some(stored), Some(selected_plan_id)) => {
            let mut value = serde_json::from_str::<serde_json::Value>(stored)
                .unwrap_or_else(|_| serde_json::json!({ "plans": [] }));
            value["selected_plan_id"] = serde_json::Value::String(selected_plan_id.clone());
            Some(value.to_string())
        }
        (None, Some(selected_plan_id)) => Some(
            serde_json::json!({
                "plans": [],
                "selected_plan_id": selected_plan_id
            })
            .to_string(),
        ),
    };

    let payload = PatchPayload {
        total_budget: req.total_budget,
        savings_goal: req.savings_goal,
        ai_plan,
        locked_at: req.lock_budget.then(Utc::now),
    };

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("budgets PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets PATCH 실패: {}", body));
    }

    let updated: Vec<Budget> = res.json().await.context("budgets PATCH 역직렬화 실패")?;
    let budget = updated
        .into_iter()
        .next()
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
        locked_at: budget.locked_at,
        created_at: budget.created_at,
    })
}

// ── 카테고리 배분 일괄 수정 ────────────────────────────────────
// 기존 categories 삭제 후 새로 INSERT (upsert 대신 delete + insert)

pub async fn update_categories(
    state: &AppState,
    user_id: Uuid,
    budget_id: Uuid,
    req: UpdateBudgetCategoriesRequest,
) -> Result<Vec<BudgetCategoryItem>> {
    let current = fetch_budget_for_user(state, user_id, budget_id).await?;

    if current.locked_at.is_some() {
        return Err(anyhow!("budget_locked"));
    }

    // 기존 카테고리 삭제
    let del_url = format!(
        "{}/rest/v1/budget_categories?budget_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
    );
    let del_res = state
        .http_client
        .delete(&del_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("budget_categories DELETE 요청 실패")?;

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

    let payload: Vec<InsertItem> = req
        .categories
        .iter()
        .map(|c| InsertItem {
            budget_id,
            category: c.category.clone(),
            allocated_amount: c.allocated_amount,
        })
        .collect();

    let ins_url = format!(
        "{}/rest/v1/budget_categories",
        state.config.supabase_url.trim_end_matches('/'),
    );
    let ins_res = state
        .http_client
        .post(&ins_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("budget_categories INSERT 요청 실패")?;

    if !ins_res.status().is_success() {
        let body = ins_res.text().await.unwrap_or_default();
        return Err(anyhow!("budget_categories INSERT 실패: {}", body));
    }

    let inserted: Vec<BudgetCategory> = ins_res
        .json()
        .await
        .context("budget_categories INSERT 역직렬화 실패")?;

    Ok(inserted
        .into_iter()
        .map(|c| BudgetCategoryItem {
            category: c.category,
            allocated_amount: c.allocated_amount,
        })
        .collect())
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
        req.budget_id,
        user_id,
    );
    let b_res = state
        .http_client
        .get(&budget_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("budgets 조회 요청 실패")?;

    if !b_res.status().is_success() {
        let body = b_res.text().await.unwrap_or_default();
        return Err(anyhow!("budgets 조회 실패: {}", body));
    }
    let budgets: Vec<Budget> = b_res.json().await.context("budgets 역직렬화 실패")?;
    let budget = budgets
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("예산을 찾을 수 없음"))?;

    if budget.locked_at.is_some() {
        return Err(anyhow!("budget_locked"));
    }

    let effective_total_budget = if req.total_budget.unwrap_or_default() > 0 {
        req.total_budget.unwrap_or_default()
    } else {
        budget.total_budget
    };

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

    let fe_res = state
        .http_client
        .get(&fe_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("fixed_expenses 조회 요청 실패")?;

    let fixed_expenses_from_db: Vec<FixedExpenseInfo> = if fe_res.status().is_success() {
        fe_res.json().await.unwrap_or_default()
    } else {
        vec![]
    };
    let fixed_expenses: Vec<FixedExpenseInfo> = req
        .fixed_expenses
        .clone()
        .and_then(|value| serde_json::from_value(value).ok())
        .unwrap_or(fixed_expenses_from_db);

    let saved_categories = fetch_categories(state, budget.id).await?;

    #[derive(Serialize)]
    struct CategoryBudgetInfo {
        category: String,
        amount: i32,
    }

    let mut category_budgets: Vec<CategoryBudgetInfo> = saved_categories
        .into_iter()
        .map(|category| CategoryBudgetInfo {
            category: category.category,
            amount: category.allocated_amount,
        })
        .collect();

    for (category, amount) in [
        ("food", req.food),
        ("transport", req.transport),
        ("living", req.living),
        ("leisure", req.leisure),
    ] {
        if let Some(amount) = amount.filter(|amount| *amount > 0) {
            if let Some(existing) = category_budgets
                .iter_mut()
                .find(|item| item.category.eq_ignore_ascii_case(category))
            {
                existing.amount = amount;
            } else {
                category_budgets.push(CategoryBudgetInfo {
                    category: category.to_string(),
                    amount,
                });
            }
        }
    }

    tracing::debug!(
        budget_id = %budget.id,
        category_budgets = %serde_json::to_string(&category_budgets).unwrap_or_default(),
        "AI 예산 플랜 요청 카테고리 예산"
    );

    // 4. AI 서버 호출
    let ai_plan = crate::clients::ai_client::budget_plan(
        state,
        crate::clients::ai_client::BudgetPlanPayload {
            user_id: user_id.to_string(),
            total_budget: effective_total_budget,
            savings_goal: req.savings_goal.or(budget.savings_goal),
            year: budget.year,
            month: budget.month,
            fixed_expenses: serde_json::to_value(&fixed_expenses).unwrap_or_default(),
            category_budgets: serde_json::to_value(&category_budgets).unwrap_or_default(),
        },
    )
    .await?;

    let plans = ai_plan.plans;

    let stored_ai_plan = serde_json::to_string(&AiPlanResponse {
        plans: plans.clone(),
    })
    .context("AI 예산 플랜 저장 JSON 직렬화 실패")?;

    // ✅ 6. DB 업데이트
    #[derive(Serialize)]
    struct PatchAiPlan {
        total_budget: i32,
        ai_plan: String,
    }

    let patch_url = format!(
        "{}/rest/v1/budgets?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.budget_id,
    );

    state
        .http_client
        .patch(&patch_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchAiPlan {
            total_budget: effective_total_budget,
            ai_plan: stored_ai_plan,
        })
        .send()
        .await
        .context("budgets ai_plan 업데이트 실패")?;

    // ✅ 7. 프론트로는 plans 그대로 내려줌
    Ok(AiPlanResponse { plans })
}

pub async fn preview_ai_plan(
    state: &AppState,
    user_id: Uuid,
    req: super::dto::GenerateAiPlanBody,
) -> Result<AiPlanResponse> {
    #[derive(Serialize)]
    struct CategoryBudgetInfo {
        category: String,
        amount: i32,
    }

    let total_budget = req
        .total_budget
        .filter(|amount| *amount > 0)
        .ok_or_else(|| anyhow!("total_budget은 0보다 커야 합니다."))?;

    let fixed_expenses = req.fixed_expenses.unwrap_or_else(|| serde_json::json!([]));
    let category_budgets: Vec<CategoryBudgetInfo> = [
        ("food", req.food),
        ("transport", req.transport),
        ("living", req.living),
        ("leisure", req.leisure),
    ]
    .into_iter()
    .filter_map(|(category, amount)| {
        amount
            .filter(|amount| *amount > 0)
            .map(|amount| CategoryBudgetInfo {
                category: category.to_string(),
                amount,
            })
    })
    .collect();

    let ai_plan = crate::clients::ai_client::budget_plan(
        state,
        crate::clients::ai_client::BudgetPlanPayload {
            user_id: user_id.to_string(),
            total_budget,
            savings_goal: req.savings_goal,
            year: req.year.unwrap_or_default(),
            month: req.month.unwrap_or_default(),
            fixed_expenses,
            category_budgets: serde_json::to_value(&category_budgets).unwrap_or_default(),
        },
    )
    .await?;

    Ok(AiPlanResponse {
        plans: ai_plan.plans,
    })
}

// ── 내부 유틸 ─────────────────────────────────────────────────

async fn fetch_categories(state: &AppState, budget_id: Uuid) -> Result<Vec<BudgetCategoryItem>> {
    let url = format!(
        "{}/rest/v1/budget_categories?budget_id=eq.{}&select=category,allocated_amount",
        state.config.supabase_url.trim_end_matches('/'),
        budget_id,
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
        .context("budget_categories SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("budget_categories SELECT 실패: {}", body));
    }

    let rows: Vec<BudgetCategory> = res.json().await.unwrap_or_default();
    Ok(rows
        .into_iter()
        .map(|c| BudgetCategoryItem {
            category: c.category,
            allocated_amount: c.allocated_amount,
        })
        .collect())
}
