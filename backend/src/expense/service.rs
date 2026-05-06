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

pub struct DeletedExpense {
    pub date: chrono::NaiveDate,
    pub transaction_type: String,
}

pub async fn create_expense(
    state: &AppState,
    user_id: Uuid,
    req: CreateExpenseWebRequest,
) -> Result<ExpenseWebResponse> {
    tracing::info!(
        user_id = %user_id,
        amount = req.amount,
        category = %req.category,
        transaction_type = %req.transaction_type,
        "소비 저장 요청 수신"
    );

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
        transaction_type: String,
        receipt_verified: bool,
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
            transaction_type: req.transaction_type,
            receipt_verified: false, // 서버 제어 — 클라이언트에서 설정 불가
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

    // 직접 입력도 소비 기록/일기/예산/스트릭 성실도에 반영한다.
    // 영수증 인증 점수와 상자 오픈권만 OCR 인증 성공 후 receipt_verified=true인 기록으로 반영한다.
    Ok(to_web_response(expense))
}

pub async fn list_expenses(state: &AppState, user_id: Uuid) -> Result<Vec<ExpenseWebResponse>> {
    tracing::info!(user_id = %user_id, "소비 목록 조회 요청 수신");

    let base_url = state.config.supabase_url.trim_end_matches('/');
    let select_candidates = [
        "id,ledger_id,user_id,expense_date,amount,category,memo,one_line_diary,transaction_type,source,receipt_verified,created_at",
        "id,ledger_id,user_id,expense_date,amount,category,memo,one_line_diary,source,created_at",
        "id,ledger_id,user_id,expense_date,amount,category,memo,source,created_at",
    ];

    let mut last_error = String::new();

    for select in select_candidates {
        let url = format!(
            "{}/rest/v1/expenses?user_id=eq.{}&select={}&order=expense_date.desc,created_at.desc",
            base_url, user_id, select,
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
            .context("expenses SELECT 요청 실패")?;

        if res.status().is_success() {
            let expenses: Vec<Expense> =
                res.json().await.context("expenses SELECT 응답 파싱 실패")?;
            return Ok(expenses.into_iter().map(to_web_response).collect());
        }

        last_error = res.text().await.unwrap_or_default();
        tracing::warn!("expenses SELECT fallback 시도: {}", last_error);
    }

    return Err(anyhow!("expenses SELECT 실패: {}", last_error));
}

pub async fn delete_expense(
    state: &AppState,
    user_id: Uuid,
    expense_id: Uuid,
) -> Result<Option<DeletedExpense>> {
    tracing::info!(user_id = %user_id, expense_id = %expense_id, "소비 삭제 요청 수신");

    #[derive(Deserialize)]
    struct DeleteTargetRow {
        expense_date: chrono::NaiveDate,
        transaction_type: Option<String>,
    }

    let target_url = format!(
        "{}/rest/v1/expenses?id=eq.{}&user_id=eq.{}&select=expense_date,transaction_type&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        expense_id,
        user_id,
    );

    let target_res = state
        .http_client
        .get(&target_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("삭제 대상 expenses SELECT 요청 실패")?;

    if !target_res.status().is_success() {
        let body = target_res.text().await.unwrap_or_default();
        return Err(anyhow!("삭제 대상 expenses SELECT 실패: {}", body));
    }

    let target = target_res
        .json::<Vec<DeleteTargetRow>>()
        .await
        .context("삭제 대상 expenses SELECT 응답 파싱 실패")?
        .into_iter()
        .next();

    let Some(target) = target else {
        return Ok(None);
    };

    let url = format!(
        "{}/rest/v1/expenses?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        expense_id,
        user_id,
    );

    let res = state
        .http_client
        .delete(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("expenses DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("expenses DELETE 실패: {}", body));
    }

    Ok(Some(DeletedExpense {
        date: target.expense_date,
        transaction_type: target
            .transaction_type
            .unwrap_or_else(|| "expense".to_string()),
    }))
}

pub async fn verify_receipt_ocr(
    state: &AppState,
    mut multipart: Multipart,
) -> Result<ReceiptOcrResult> {
    tracing::info!("영수증 OCR 검증 요청 수신");

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

fn to_web_response(expense: Expense) -> ExpenseWebResponse {
    ExpenseWebResponse {
        id: expense.id,
        date: expense.expense_date,
        amount: expense.amount,
        category: expense.category,
        memo: expense.memo,
        transaction_type: expense
            .transaction_type
            .unwrap_or_else(|| "expense".to_string()),
        receipt_verified: expense.receipt_verified.unwrap_or(false),
        diary: expense.one_line_diary,
    }
}

// ── 영수증 하루 3건 제한 ───────────────────────────────────────

#[derive(Debug)]
pub enum ReceiptLimitError {
    TooMany,          // 429
    Duplicate,        // 409
    Internal(String), // 500
}

/// 영수증 OCR 전 호출. 하루 3건 초과 or expense_id 중복이면 에러 반환.
/// receipts 테이블에 user_id 컬럼이 있다고 가정.
/// uploaded_at 범위 필터로 오늘 건수 조회 (receipt_date 컬럼 없음).
pub async fn check_receipt_limit(
    state: &AppState,
    user_id: Uuid,
    expense_id: Option<Uuid>,
) -> Result<(), ReceiptLimitError> {
    let base_url = state.config.supabase_url.trim_end_matches('/');
    let key = &state.config.supabase_secret_key;

    let today = chrono::Local::now().date_naive();

    // ── 오늘 날짜 소비 중 receipt_verified = true 건수 조회 ──
    let count_url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&expense_date=eq.{}&receipt_verified=eq.true&transaction_type=eq.expense&select=id",
        base_url, user_id, today
    );

    let count_res = state
        .http_client
        .get(&count_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key.as_str())
        .header("Prefer", "count=exact")
        .send()
        .await
        .map_err(|e| ReceiptLimitError::Internal(e.to_string()))?;

    if !count_res.status().is_success() {
        let body = count_res.text().await.unwrap_or_default();
        return Err(ReceiptLimitError::Internal(format!(
            "오늘 영수증 인증 건수 조회 실패: {}",
            body
        )));
    }

    let total_count = count_res
        .headers()
        .get("content-range")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.split('/').nth(1))
        .and_then(|n| n.parse::<usize>().ok())
        .unwrap_or(0);

    if total_count >= 3 {
        return Err(ReceiptLimitError::TooMany);
    }

    // ── expense_id 중복 체크 (expenses.receipt_verified 기준) ──
    if let Some(expense_id) = expense_id {
        let dup_url = format!(
            "{}/rest/v1/expenses?id=eq.{}&user_id=eq.{}&receipt_verified=eq.true&select=id&limit=1",
            base_url, expense_id, user_id
        );

        let dup_res = state
            .http_client
            .get(&dup_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .send()
            .await
            .map_err(|e| ReceiptLimitError::Internal(e.to_string()))?;

        if !dup_res.status().is_success() {
            let body = dup_res.text().await.unwrap_or_default();
            return Err(ReceiptLimitError::Internal(format!(
                "영수증 중복 인증 조회 실패: {}",
                body
            )));
        }

        let rows: Vec<serde_json::Value> = dup_res
            .json()
            .await
            .map_err(|e| ReceiptLimitError::Internal(e.to_string()))?;
        if !rows.is_empty() {
            return Err(ReceiptLimitError::Duplicate);
        }
    }

    Ok(())
}

// ── OCR 인증 성공 시 receipt_verified 서버 업데이트 ────────────────
//
// handler의 verify_receipt_ocr에서 is_verified == true이고
// expense_id가 제공된 경우에만 호출됨.
// user_id 조건을 함께 걸어 다른 유저 expense 수정 방지.
pub async fn update_receipt_verified(
    state: &AppState,
    user_id: Uuid,
    expense_id: Uuid,
    receipt_date: chrono::NaiveDate,
    receipt_amount: i32,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/expenses?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        expense_id,
        user_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "receipt_verified": true,
            "expense_date": receipt_date,
            "amount": receipt_amount
        }))
        .send()
        .await
        .context("expenses receipt_verified UPDATE 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "expenses receipt_verified UPDATE 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<serde_json::Value> = res
        .json()
        .await
        .context("expenses receipt_verified UPDATE 응답 파싱 실패")?;
    if rows.is_empty() {
        return Err(anyhow!(
            "인증 처리할 소비 내역이 없거나 본인 소유가 아닙니다"
        ));
    }

    Ok(())
}
