// expense/service.rs

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{CreateExpenseWebRequest, ExpenseWebResponse},
    model::Expense,
};

#[derive(Debug, Deserialize)]
struct LedgerRow {
    id: Uuid,
}

pub async fn create_expense(
    state: &AppState,
    user_id: Uuid,
    req: CreateExpenseWebRequest,
) -> Result<ExpenseWebResponse> {
    let ledger_id = get_or_create_personal_ledger(state, user_id).await?;

    #[derive(Serialize)]
    struct InsertExpensePayload {
        ledger_id: Uuid,
        user_id: Uuid,
        expense_date: chrono::NaiveDate,
        amount: i32,
        category: String,
        memo: Option<String>,
        one_line_diary: Option<String>,
        source: &'static str,
    }

    let url = format!(
        "{}/rest/v1/expenses",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let res = state
        .http_client
        .post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertExpensePayload {
            ledger_id,
            user_id,
            expense_date: req.date,
            amount: req.amount,
            category: req.category,
            memo: empty_to_none(req.memo),
            one_line_diary: empty_to_none(req.diary),
            source: "manual",
        })
        .send()
        .await
        .context("expenses INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("expenses INSERT 실패: {}", body));
    }

    let inserted: Vec<Expense> = res
        .json()
        .await
        .context("expenses INSERT 응답 파싱 실패")?;

    let expense = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("expenses INSERT 결과가 비어있음"))?;

    Ok(ExpenseWebResponse {
        id: expense.id,
        date: expense.expense_date,
        amount: expense.amount,
        category: expense.category,
        memo: expense.memo,
        receipt_verified: req.receipt_verified,
        diary: expense.one_line_diary,
    })
}

async fn get_or_create_personal_ledger(state: &AppState, user_id: Uuid) -> Result<Uuid> {
    if let Some(ledger_id) = find_personal_ledger(state, user_id).await? {
        return Ok(ledger_id);
    }

    #[derive(Serialize)]
    struct InsertLedgerPayload {
        user_id: Uuid,
        title: &'static str,
        is_shared: bool,
    }

    let url = format!(
        "{}/rest/v1/ledgers",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let res = state
        .http_client
        .post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertLedgerPayload {
            user_id,
            title: "개인 가계부",
            is_shared: false,
        })
        .send()
        .await
        .context("ledgers INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers INSERT 실패: {}", body));
    }

    let inserted: Vec<LedgerRow> = res
        .json()
        .await
        .context("ledgers INSERT 응답 파싱 실패")?;

    inserted
        .into_iter()
        .next()
        .map(|ledger| ledger.id)
        .ok_or_else(|| anyhow!("ledgers INSERT 결과가 비어있음"))
}

async fn find_personal_ledger(state: &AppState, user_id: Uuid) -> Result<Option<Uuid>> {
    let url = format!(
        "{}/rest/v1/ledgers?user_id=eq.{}&is_shared=eq.false&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("ledgers SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers SELECT 실패: {}", body));
    }

    let ledgers: Vec<LedgerRow> = res
        .json()
        .await
        .context("ledgers SELECT 응답 파싱 실패")?;

    Ok(ledgers.into_iter().next().map(|ledger| ledger.id))
}

fn empty_to_none(value: Option<String>) -> Option<String> {
    value.and_then(|v| {
        let trimmed = v.trim().to_string();
        if trimmed.is_empty() {
            None
        } else {
            Some(trimmed)
        }
    })
}
