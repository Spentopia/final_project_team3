// src/auth/refresh_store.rs
//
// refresh_sessions 테이블 전담 로직
//
// 역할:
// 1) refresh token 원문은 저장하지 않고 해시만 저장
// 2) refresh 요청 시 sid(session_id)로 row를 찾음
// 3) incoming refresh token을 해시해서 DB 값과 비교
// 4) revoke / rotation 지원
//
// 왜 해시 저장?
// - DB 유출 시 refresh 원문이 그대로 노출되지 않게 하기 위함

use anyhow::{anyhow, Context, Result};
use chrono::{Duration, Utc};
use serde::Deserialize;
use sha2::{Digest, Sha256};
use uuid::Uuid;

use crate::state::AppState;

#[derive(Debug, Clone, Deserialize)]
pub struct RefreshSessionRow {
    pub id: Uuid,
    pub user_id: Uuid,
    pub client_type: String,
    pub token_hash: String,
    pub revoked: bool,
}

pub fn hash_refresh_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}

pub async fn create_refresh_session(
    state: &AppState,
    session_id: Uuid,
    user_id: Uuid,
    client_type: &str,
    refresh_token: &str,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/refresh_sessions",
        state.config.supabase_url.trim_end_matches('/')
    );

    let token_hash = hash_refresh_token(refresh_token);
    let expires_at = (Utc::now() + Duration::days(14)).to_rfc3339();

    let payload = serde_json::json!([{
        "id": session_id,
        "user_id": user_id,
        "token_hash": token_hash,
        "client_type": client_type,
        "revoked": false,
        "expires_at": expires_at,
        "created_at": Utc::now().to_rfc3339(),
        "updated_at": Utc::now().to_rfc3339()
    }]);

    let resp = state.http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .json(&payload)
        .send()
        .await
        .context("refresh_sessions insert 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh_sessions insert 실패: {}", err));
    }

    Ok(())
}

pub async fn get_refresh_session_by_id(
    state: &AppState,
    session_id: Uuid,
) -> Result<RefreshSessionRow> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}&select=id,user_id,client_type,token_hash,revoked",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .context("refresh_sessions 조회 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh_sessions 조회 실패: {}", err));
    }

    let rows: Vec<RefreshSessionRow> = resp.json().await
        .context("refresh_sessions 응답 파싱 실패")?;

    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("refresh 세션을 찾을 수 없습니다."))
}

pub async fn verify_refresh_session(
    state: &AppState,
    session_id: Uuid,
    refresh_token: &str,
) -> Result<RefreshSessionRow> {
    let session = get_refresh_session_by_id(state, session_id).await?;

    if session.revoked {
        return Err(anyhow!("이미 폐기된 refresh 세션입니다."));
    }

    let incoming_hash = hash_refresh_token(refresh_token);

    if session.token_hash != incoming_hash {
        return Err(anyhow!("refresh 토큰이 유효하지 않습니다."));
    }

    Ok(session)
}

pub async fn revoke_refresh_session(
    state: &AppState,
    session_id: Uuid,
    replaced_by_session_id: Option<Uuid>,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let payload = serde_json::json!({
        "revoked": true,
        "replaced_by_session_id": replaced_by_session_id,
        "updated_at": Utc::now().to_rfc3339()
    });

    let resp = state.http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .json(&payload)
        .send()
        .await
        .context("refresh session revoke 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh session revoke 실패: {}", err));
    }

    Ok(())
}