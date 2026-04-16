// payment/service.rs
// 결제 비즈니스 로직 (토스페이먼츠 PG사 연동)
//
// 백엔드 클라이언트 역할:
//  confirm 단계에서 토스페이먼츠 API를 호출한다.
//  TOSS_SECRET_KEY는 config에 추가 예정.

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{ConfirmPaymentRequest, CreatePaymentRequest, PaymentResponse},
    model::Payment,
};

// ── 결제 생성 (pending 상태로 DB 선등록) ─────────────────────

pub async fn create_payment(
    state: &AppState,
    user_id: Uuid,
    req: CreatePaymentRequest,
) -> Result<PaymentResponse> {
    let url = format!(
        "{}/rest/v1/payments",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        ledger_id: Uuid,
        payment_key: String,
        order_id: Option<String>,
        amount: i32,
        status: &'static str,
    }

    // 결제 생성 시점에는 payment_key를 order_id 기반으로 임시 생성
    let temp_key = req.order_id.clone().unwrap_or_else(|| uuid::Uuid::new_v4().to_string());

    let res = state.http_client.post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertPayload {
            user_id,
            ledger_id: req.ledger_id,
            payment_key: temp_key,
            order_id: req.order_id,
            amount: req.amount,
            status: "pending",
        })
        .send().await.context("payments INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("payments INSERT 실패: {}", body));
    }

    let inserted: Vec<Payment> = res.json().await.context("payments INSERT 역직렬화 실패")?;
    let payment = inserted.into_iter().next()
        .ok_or_else(|| anyhow!("payments INSERT 결과가 비어있음"))?;

    Ok(to_response(payment))
}

// ── 결제 승인 ─────────────────────────────────────────────────
// PG사 최종 승인 후 DB 상태 업데이트

pub async fn confirm_payment(
    state: &AppState,
    user_id: Uuid,
    req: ConfirmPaymentRequest,
) -> Result<PaymentResponse> {
    // 1. user_id로 해당 주문의 payment row 조회
    let select_url = format!(
        "{}/rest/v1/payments?order_id=eq.{}&user_id=eq.{}&select=*",
        state.config.supabase_url.trim_end_matches('/'),
        req.order_id, user_id,
    );

    let sel_res = state.http_client.get(&select_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("payments SELECT 요청 실패")?;

    if !sel_res.status().is_success() {
        let body = sel_res.text().await.unwrap_or_default();
        return Err(anyhow!("payments SELECT 실패: {}", body));
    }

    let payments: Vec<Payment> = sel_res.json().await.context("payments 역직렬화 실패")?;
    let payment = payments.into_iter().next()
        .ok_or_else(|| anyhow!("결제 정보를 찾을 수 없음: {}", req.order_id))?;

    // 금액 불일치 방어
    if payment.amount != req.amount {
        return Err(anyhow!("결제 금액 불일치: DB={}, 요청={}", payment.amount, req.amount));
    }

    // 2. payment_key, status → done 업데이트
    let patch_url = format!(
        "{}/rest/v1/payments?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        payment.id, user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        payment_key: String,
        status: &'static str,
    }

    let patch_res = state.http_client.patch(&patch_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload { payment_key: req.payment_key, status: "done" })
        .send().await.context("payments PATCH 요청 실패")?;

    if !patch_res.status().is_success() {
        let body = patch_res.text().await.unwrap_or_default();
        return Err(anyhow!("payments PATCH 실패: {}", body));
    }

    let updated: Vec<Payment> = patch_res.json().await.context("payments PATCH 역직렬화 실패")?;
    let updated_payment = updated.into_iter().next()
        .ok_or_else(|| anyhow!("결제 승인 후 row를 찾을 수 없음"))?;

    Ok(to_response(updated_payment))
}

fn to_response(p: Payment) -> PaymentResponse {
    PaymentResponse {
        id: p.id,
        ledger_id: p.ledger_id,
        payment_key: p.payment_key,
        order_id: p.order_id,
        amount: p.amount,
        status: p.status,
        paid_at: p.paid_at,
        created_at: p.created_at,
    }
}
