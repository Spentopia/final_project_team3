// ledger/service.rs
// 가계부, 공유 멤버 비즈니스 로직

use anyhow::{Context, Result, anyhow};
use serde::Serialize;
use uuid::Uuid;

use super::{
    dto::{
        CreateLedgerRequest, InviteMemberRequest, LedgerMemberResponse, LedgerResponse,
        UpdateLedgerRequest,
    },
    model::{Ledger, LedgerMember},
};
use crate::state::AppState;

// ── 가계부 목록 조회 ───────────────────────────────────────────
// 내가 생성했거나 멤버인 가계부 모두 반환

pub async fn list_ledgers(state: &AppState, user_id: Uuid) -> Result<Vec<LedgerResponse>> {
    // 직접 소유한 가계부
    let url = format!(
        "{}/rest/v1/ledgers?user_id=eq.{}&select=*&order=created_at.desc",
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

    let ledgers: Vec<Ledger> = res.json().await.context("ledgers 역직렬화 실패")?;
    Ok(ledgers
        .into_iter()
        .map(|l| LedgerResponse {
            id: l.id,
            title: l.title,
            is_shared: l.is_shared,
            created_at: l.created_at,
        })
        .collect())
}

// ── 가계부 생성 ───────────────────────────────────────────────

pub async fn create_ledger(
    state: &AppState,
    user_id: Uuid,
    req: CreateLedgerRequest,
) -> Result<LedgerResponse> {
    let url = format!(
        "{}/rest/v1/ledgers",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        title: String,
        is_shared: bool,
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
            title: req.title,
            is_shared: false,
        })
        .send()
        .await
        .context("ledgers INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers INSERT 실패: {}", body));
    }

    let inserted: Vec<Ledger> = res.json().await.context("ledgers INSERT 역직렬화 실패")?;
    let ledger = inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("ledgers INSERT 결과가 비어있음"))?;

    // 생성자를 owner 멤버로 등록
    let _ = add_member(state, ledger.id, user_id, "owner").await;

    Ok(LedgerResponse {
        id: ledger.id,
        title: ledger.title,
        is_shared: ledger.is_shared,
        created_at: ledger.created_at,
    })
}

// ── 가계부 수정 ───────────────────────────────────────────────

pub async fn update_ledger(
    state: &AppState,
    user_id: Uuid,
    ledger_id: Uuid,
    req: UpdateLedgerRequest,
) -> Result<LedgerResponse> {
    let url = format!(
        "{}/rest/v1/ledgers?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        ledger_id,
        user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        title: String,
    }

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload { title: req.title })
        .send()
        .await
        .context("ledgers PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers PATCH 실패: {}", body));
    }

    let updated: Vec<Ledger> = res.json().await.context("ledgers PATCH 역직렬화 실패")?;
    let ledger = updated
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("수정된 가계부를 찾을 수 없음"))?;

    Ok(LedgerResponse {
        id: ledger.id,
        title: ledger.title,
        is_shared: ledger.is_shared,
        created_at: ledger.created_at,
    })
}

// ── 가계부 삭제 ───────────────────────────────────────────────

pub async fn delete_ledger(state: &AppState, user_id: Uuid, ledger_id: Uuid) -> Result<()> {
    let url = format!(
        "{}/rest/v1/ledgers?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        ledger_id,
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
        .context("ledgers DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledgers DELETE 실패: {}", body));
    }
    Ok(())
}

// ── 멤버 초대 ─────────────────────────────────────────────────

pub async fn invite_member(
    state: &AppState,
    ledger_id: Uuid,
    req: InviteMemberRequest,
) -> Result<LedgerMemberResponse> {
    let member = add_member(state, ledger_id, req.user_id, "member").await?;
    Ok(LedgerMemberResponse {
        id: member.id,
        user_id: member.user_id,
        nickname: None,
        role: member.role,
        joined_at: member.joined_at,
    })
}

// ── 내부 유틸 ─────────────────────────────────────────────────

async fn add_member(
    state: &AppState,
    ledger_id: Uuid,
    user_id: Uuid,
    role: &str,
) -> Result<LedgerMember> {
    let url = format!(
        "{}/rest/v1/ledger_members",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        ledger_id: Uuid,
        user_id: Uuid,
        role: String,
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
            ledger_id,
            user_id,
            role: role.to_string(),
        })
        .send()
        .await
        .context("ledger_members INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("ledger_members INSERT 실패: {}", body));
    }

    let inserted: Vec<LedgerMember> = res
        .json()
        .await
        .context("ledger_members INSERT 역직렬화 실패")?;
    inserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("ledger_members INSERT 결과가 비어있음"))
}
