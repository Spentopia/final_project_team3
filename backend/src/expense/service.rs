// expense/service.rs

use anyhow::{Context, Result, anyhow};
use axum::extract::Multipart;
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::{
    dto::{CreateExpenseWebRequest, ExpenseWebResponse},
    model::Expense,
};
use crate::clients::ai_client::{self, ReceiptOcrResult};
use crate::state::AppState;

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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

    let inserted: Vec<Expense> = res.json().await.context("expenses INSERT 응답 파싱 실패")?;

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

pub async fn verify_receipt_ocr(
    state: &AppState,
    mut multipart: Multipart,
) -> Result<ReceiptOcrResult> {
    let mut image_bytes: Option<Vec<u8>> = None;
    let mut file_name = "receipt".to_string();
    let mut mime_type: Option<String> = None;
    let mut expected_date: Option<String> = None;
    let mut expected_amount: Option<i32> = None;

    while let Some(field) = multipart
        .next_field()
        .await
        .context("영수증 OCR multipart 파싱 실패")?
    {
        let name = field.name().unwrap_or_default().to_string();

        match name.as_str() {
            "image" => {
                if let Some(content_type) = field.content_type() {
                    if !content_type.starts_with("image/") {
                        return Err(anyhow!("이미지 파일만 업로드할 수 있습니다."));
                    }
                    mime_type = Some(content_type.to_string());
                }

                if let Some(uploaded_name) = field.file_name() {
                    file_name = uploaded_name.to_string();
                }

                let bytes = field.bytes().await.context("영수증 이미지 읽기 실패")?;

                if bytes.is_empty() {
                    return Err(anyhow!("빈 이미지 파일입니다."));
                }

                image_bytes = Some(bytes.to_vec());
            }
            "expected_date" => {
                let value = field.text().await.context("expected_date 읽기 실패")?;
                expected_date = Some(value);
            }
            "expected_amount" => {
                let value = field.text().await.context("expected_amount 읽기 실패")?;
                let amount = value
                    .parse::<i32>()
                    .context("expected_amount는 숫자여야 합니다.")?;
                expected_amount = Some(amount);
            }
            _ => {}
        }
    }

    let image_bytes = image_bytes.ok_or_else(|| anyhow!("image는 필수입니다."))?;
    let mime_type = mime_type.unwrap_or_else(|| "application/octet-stream".to_string());
    let expected_date = expected_date.ok_or_else(|| anyhow!("expected_date는 필수입니다."))?;
    let expected_amount =
        expected_amount.ok_or_else(|| anyhow!("expected_amount는 필수입니다."))?;

    ai_client::receipt_ocr(
        state,
        ai_client::ReceiptOcrPayload {
            image_bytes,
            file_name,
            mime_type,
            expected_date,
            expected_amount,
        },
    )
    .await
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

    let inserted: Vec<LedgerRow> = res.json().await.context("ledgers INSERT 응답 파싱 실패")?;

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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("ledgers SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers SELECT 실패: {}", body));
    }

    let ledgers: Vec<LedgerRow> = res.json().await.context("ledgers SELECT 응답 파싱 실패")?;

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
