// src/auth/refresh_store.rs
//
// refresh_sessions 테이블 전담 로직
//
// 역할:
// 1) refresh token 원문은 저장하지 않고 hash만 저장
// 2) refresh 요청 시 sid(session_id)로 row를 찾음
// 3) 들어온 refresh token을 hash해서 DB 값과 비교
// 4) expires_at 검사
// 5) revoked 검사
// 6) replaced_by_session_id 검사 (reuse detection)
// 7) revoke / rotation 지원
//
// 왜 hash 저장?
// - DB가 유출돼도 refresh token 원문이 바로 노출되지 않게 하기 위함
//
// 왜 expires_at 검사?
// - JWT exp만 믿지 말고 DB 세션 자체도 만료됐는지 한 번 더 확인하기 위함
//
// 왜 replaced_by_session_id 검사?
// - 이미 rotation으로 교체된 refresh token이 다시 들어오면
//   "토큰 재사용(reuse) 공격"으로 간주하고 차단하기 위함

use anyhow::{anyhow, Context, Result};
use chrono::{Duration, Utc, DateTime};
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
    // true면 더 이상 사용할 수 없는 refresh 세션
    pub revoked: bool,

    // DB 기준 세션 만료 시각
    // JWT exp와 별개로 DB row도 만료되었는지 한 번 더 확인
    pub expires_at: String,

    // 새 refresh 세션으로 교체되었으면 Some(UUID)
    // 이미 한 번 사용된 refresh token의 재사용(reuse) 탐지에 사용
    pub replaced_by_session_id: Option<Uuid>,
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
        .ok_or_else(|| anyhow!("refresh session 없음"))
}

// refresh 세션 검증
//
// 검증 순서:
// 1) sid로 DB row 조회
// 2) revoked 여부 확인
// 3) expires_at 확인
// 4) replaced_by_session_id 확인 (reuse detection)
// 5) incoming refresh token hash 비교
//
// 이 함수가 통과해야만 rotation 진행 가능
pub async fn verify_refresh_session(
    state: &AppState,
    session_id: Uuid,
    refresh_token: &str,
) -> Result<RefreshSessionRow> {
    let session = get_refresh_session_by_id(state, session_id).await?;

    if session.revoked {
        return Err(anyhow!("이미 폐기된 refresh session"));
    }

    // 2) DB 기준 만료 시각 검사
    //
    // 왜 또 검사하냐?
    // - JWT exp만으로 끝내지 않고
    // - DB 세션 row도 실제로 아직 살아있는지 확인하기 위함
    let expires_at = DateTime::parse_from_rfc3339(&session.expires_at)
        .context("refresh expires_at 파싱 실패")?
        .with_timezone(&Utc);

    if Utc::now() > expires_at {
        return Err(anyhow!("refresh session 만료됨"));
    }

    // 3) 이미 다른 새 세션으로 교체된(refresh rotation 완료된) 경우
    //
    // 이 경우는 "이미 한 번 사용된 refresh token이 또 들어온 것"일 수 있음
    // 즉 토큰 재사용(reuse) 공격 가능성을 의미하므로 차단
    if session.replaced_by_session_id.is_some() {
        return Err(anyhow!("이미 교체된 refresh token (reuse 감지)"));
    }

    let incoming_hash = hash_refresh_token(refresh_token);

    if session.token_hash != incoming_hash {
        return Err(anyhow!("refresh token hash 불일치"));
    }

    Ok(session)
}

// refresh session revoke
//
// 로그아웃하거나, refresh rotation 시 기존 세션을 죽일 때 사용
//
// replaced_by_session_id:
// - rotation이면 Some(new_session_id)
// - 그냥 로그아웃이면 None
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

// 특정 session_id의 refresh session을 "강제 재사용 공격"으로 처리하고 싶을 때 쓸 수 있는 함수
//
// 옵션 성격이지만, 나중에 reuse 감지 시
// - 해당 세션 폐기
// - 필요하면 user 전체 세션 폐기
// 같은 확장에 쓰기 좋다.
pub async fn revoke_refresh_session_as_reused(
    state: &AppState,
    session_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let payload = serde_json::json!({
        "revoked": true,
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
        .context("refresh session reuse revoke 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh session reuse revoke 실패: {}", err));
    }

    Ok(())
}