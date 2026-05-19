// src/auth/refresh_store.rs
//
// refresh_sessions 테이블 전담 로직
//
// 역할:
// 1) refresh token 원문은 저장하지 않고 hash만 저장
// 2) refresh 요청 시 sid(session_id)로 row를 찾음
// 3) incoming refresh token을 hash해서 DB 값과 비교
// 4) expires_at 검사
// 5) revoked / revoked_at 둘 다 검사
// 6) replaced_by_session_id 검사 (reuse detection)
// 7) revoke / rotation 지원
//
// 왜 revoked_at 추가?
// - 단순히 "폐기됐는지"만 보는 게 아니라
// - "언제 폐기됐는지"까지 기록해서
//   cleanup / 디버깅 / 감사 로그에 더 유리하게 하기 위함
//
// 왜 revoked도 같이 보냐?
// - 지금은 기존 구조와 호환을 위해 boolean도 남겨둠
// - 즉 현재는 하이브리드 방식:
//   살아있음  = revoked=false && revoked_at is null
//   폐기됨    = revoked=true  || revoked_at is not null

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Duration, Utc};
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
    // 지금은 하위 호환용으로 유지
    pub revoked: bool,

    // 세션 만료 시각
    pub expires_at: String,

    // 새로 추가되는 폐기 시각
    // NULL이면 살아있는 세션
    // 값이 있으면 이미 폐기된 세션
    pub revoked_at: Option<String>,

    // refresh rotation으로 새 세션으로 교체됐는지 추적
    // 값이 있으면 예전 refresh token 재사용 시도(reuse)로 봄
    pub replaced_by_session_id: Option<Uuid>,
}

// refresh token 원문 -> SHA256 hash
//
// DB에는 절대 원문을 저장하지 않고 hash만 저장한다.
pub fn hash_refresh_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}

// 새 refresh session 생성
//
// 로그인 성공 시 / refresh rotation 성공 시 호출된다.
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
        "revoked_at": null,
        "expires_at": expires_at,
        "replaced_by_session_id": null,
        "created_at": Utc::now().to_rfc3339(),
        "updated_at": Utc::now().to_rfc3339()
    }]);

    let resp = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

// sid(session_id) 기준 refresh session 조회
pub async fn get_refresh_session_by_id(
    state: &AppState,
    session_id: Uuid,
) -> Result<RefreshSessionRow> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}&select=id,user_id,client_type,token_hash,revoked,revoked_at,expires_at,replaced_by_session_id",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let resp = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("refresh_sessions 조회 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh_sessions 조회 실패: {}", err));
    }

    let rows: Vec<RefreshSessionRow> = resp
        .json()
        .await
        .context("refresh_sessions 응답 파싱 실패")?;

    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("refresh 세션을 찾을 수 없습니다."))
}

// refresh session 검증
//
// 검증 순서:
// 1) row 존재 확인
// 2) revoked / revoked_at 검사
// 3) expires_at 검사
// 4) replaced_by_session_id 검사 (reuse detection)
// 5) token_hash 비교
pub async fn verify_refresh_session(
    state: &AppState,
    session_id: Uuid,
    refresh_token: &str,
) -> Result<RefreshSessionRow> {
    let session = get_refresh_session_by_id(state, session_id).await?;

    // 1) 기존 boolean revoked 호환 체크
    if session.revoked {
        return Err(anyhow!("이미 폐기된 refresh session"));
    }

    // 2) 새 revoked_at 체크
    if session.revoked_at.is_some() {
        return Err(anyhow!("이미 폐기된 refresh session"));
    }

    // 3) DB 기준 만료 검사
    let expires_at = DateTime::parse_from_rfc3339(&session.expires_at)
        .context("refresh expires_at 파싱 실패")?
        .with_timezone(&Utc);

    if Utc::now() > expires_at {
        return Err(anyhow!("refresh session 만료됨"));
    }

    // 4) 이미 새 세션으로 교체된 경우
    //    = 예전 refresh token 재사용 시도(reuse detection)
    if session.replaced_by_session_id.is_some() {
        return Err(anyhow!("이미 교체된 refresh token (reuse 감지)"));
    }

    // 5) hash 비교
    let incoming_hash = hash_refresh_token(refresh_token);

    if session.token_hash != incoming_hash {
        return Err(anyhow!("refresh token hash 불일치"));
    }

    Ok(session)
}

// ─────────────────────────────────────────────────────────────
// refresh rotation grace window
// ─────────────────────────────────────────────────────────────
//
// 새로고침 연타 / 브라우저 요청 중단 / 네트워크 지연으로
// 같은 refresh token이 아주 짧은 시간 안에 2번 서버에 도착할 수 있다.
//
// 정상 rotation:
//   A 사용 → A revoked + A.replaced_by_session_id = B → B 발급
//
// race 상황:
//   A가 이미 revoked 되었는데, 직후에 A가 한 번 더 들어옴
//
// 기존 로직은 이걸 무조건 reuse 공격으로 보고 401 처리했다.
// 하지만 revoked_at이 아주 최근이고 replaced_by_session_id가 있으면,
// 방금 rotation된 토큰의 지연 요청일 가능성이 높다.
//
// 이 함수는 "폐기된 A"가 들어왔을 때,
// A가 방금 rotation된 세션인지 확인하고,
// 현재 살아있는 대체 세션 B 또는 그 뒤의 최신 세션을 찾아 반환한다.
//
// 보안 조건:
// - incoming refresh token hash가 원래 A row의 token_hash와 일치해야 함
// - A.replaced_by_session_id가 있어야 함
// - A.revoked_at이 grace_seconds 이내여야 함
// - 대체 세션의 user_id/client_type이 A와 같아야 함
// - 대체 세션이 만료되지 않았어야 함
// - 최종적으로 살아있는 세션만 반환함
//
// 주의:
// - 로그아웃/탈퇴로 revoked된 세션은 replaced_by_session_id가 없으므로 grace 대상이 아님.
// - 오래전에 revoked된 토큰도 grace 대상이 아님.
pub async fn verify_refresh_session_with_rotation_grace(
    state: &AppState,
    original_session_id: Uuid,
    refresh_token: &str,
    grace_seconds: i64,
) -> Result<RefreshSessionRow> {
    let original = get_refresh_session_by_id(state, original_session_id).await?;

    // 1) 원문 refresh token이 original row의 hash와 일치해야 한다.
    //    hash가 다르면 단순 위조/잘못된 토큰이므로 grace 대상이 아니다.
    let incoming_hash = hash_refresh_token(refresh_token);
    if original.token_hash != incoming_hash {
        return Err(anyhow!("grace 대상 아님: refresh token hash 불일치"));
    }

    // 2) original은 실제로 폐기된 상태여야 한다.
    //    살아있는 세션이면 일반 verify_refresh_session 경로를 타야 한다.
    if !original.revoked && original.revoked_at.is_none() {
        return Err(anyhow!("grace 대상 아님: original session이 아직 살아있음"));
    }

    // 3) rotation으로 폐기된 세션이어야 한다.
    //    로그아웃/탈퇴/강제 revoke는 replaced_by_session_id가 없으므로 살려주면 안 된다.
    let mut next_session_id = original
        .replaced_by_session_id
        .ok_or_else(|| anyhow!("grace 대상 아님: replaced_by_session_id 없음"))?;

    // 4) original revoked_at이 grace window 안이어야 한다.
    let original_revoked_at_raw = original
        .revoked_at
        .as_deref()
        .ok_or_else(|| anyhow!("grace 대상 아님: revoked_at 없음"))?;

    let original_revoked_at = parse_utc_datetime(original_revoked_at_raw, "refresh revoked_at")?;
    let now = Utc::now();

    if now - original_revoked_at > Duration::seconds(grace_seconds) {
        return Err(anyhow!("grace window 만료"));
    }

    // 5) original 자체도 만료된 refresh token이면 살려주지 않는다.
    ensure_refresh_session_not_expired(&original)?;

    // 6) replaced_by_session_id 체인을 따라가며 현재 살아있는 세션을 찾는다.
    //
    // 왜 체인을 따라가나?
    // 새로고침 연타가 3~4번 겹치면:
    //   A → B
    //   stale A 요청이 B → C로 회전
    //   또 다른 stale A 요청이 들어왔을 때 B는 이미 revoked일 수 있다.
    //
    // 이 경우 A의 대체 세션 B가 다시 C로 교체됐으면,
    // 짧은 grace window 안에서는 C까지 따라가서 복구할 수 있게 한다.
    //
    // 무한 루프 방지를 위해 최대 5단계까지만 따라간다.
    for _ in 0..5 {
        let candidate = get_refresh_session_by_id(state, next_session_id).await?;

        if candidate.user_id != original.user_id {
            return Err(anyhow!("grace 중단: replacement user_id 불일치"));
        }

        if candidate.client_type != original.client_type {
            return Err(anyhow!("grace 중단: replacement client_type 불일치"));
        }

        ensure_refresh_session_not_expired(&candidate)?;

        // 최종적으로 살아있는 세션을 찾으면 이 세션을 기준으로 다시 rotation한다.
        if !candidate.revoked
            && candidate.revoked_at.is_none()
            && candidate.replaced_by_session_id.is_none()
        {
            return Ok(candidate);
        }

        // candidate도 이미 rotation된 상태라면,
        // candidate 역시 아주 최근에 revoked된 경우에만 다음 세션으로 따라간다.
        let candidate_revoked_at_raw = candidate
            .revoked_at
            .as_deref()
            .ok_or_else(|| anyhow!("grace 중단: replacement가 revoked인데 revoked_at 없음"))?;

        let candidate_revoked_at =
            parse_utc_datetime(candidate_revoked_at_raw, "replacement revoked_at")?;

        if now - candidate_revoked_at > Duration::seconds(grace_seconds) {
            return Err(anyhow!("grace 중단: replacement grace window 만료"));
        }

        next_session_id = candidate
            .replaced_by_session_id
            .ok_or_else(|| anyhow!("grace 중단: replacement가 다음 세션을 가리키지 않음"))?;
    }

    Err(anyhow!("grace 중단: replacement chain이 너무 김"))
}

fn parse_utc_datetime(value: &str, label: &str) -> Result<DateTime<Utc>> {
    Ok(DateTime::parse_from_rfc3339(value)
        .with_context(|| format!("{} 파싱 실패", label))?
        .with_timezone(&Utc))
}

fn ensure_refresh_session_not_expired(session: &RefreshSessionRow) -> Result<()> {
    let expires_at = parse_utc_datetime(&session.expires_at, "refresh expires_at")?;

    if Utc::now() > expires_at {
        return Err(anyhow!("refresh session 만료됨"));
    }

    Ok(())
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
        "revoked_at": Utc::now().to_rfc3339(),
        "replaced_by_session_id": replaced_by_session_id,
        "updated_at": Utc::now().to_rfc3339()
    });

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

// refresh rotation 전용 revoke
//
// 같은 refresh token이 거의 동시에 두 번 들어오는 경우를 막기 위해
// "아직 살아있는 세션" 조건까지 걸고, 실제로 1행이 업데이트됐는지 확인한다.
pub async fn revoke_refresh_session_for_rotation(
    state: &AppState,
    session_id: Uuid,
    replaced_by_session_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}&revoked=eq.false&revoked_at=is.null&replaced_by_session_id=is.null",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let payload = serde_json::json!({
        "revoked": true,
        "revoked_at": Utc::now().to_rfc3339(),
        "replaced_by_session_id": replaced_by_session_id,
        "updated_at": Utc::now().to_rfc3339()
    });

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("refresh session rotation revoke 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("refresh session rotation revoke 실패: {}", err));
    }

    let rows: Vec<serde_json::Value> = resp
        .json()
        .await
        .context("refresh session rotation revoke 응답 파싱 실패")?;

    if rows.is_empty() {
        return Err(anyhow!("이미 사용된 refresh token입니다."));
    }

    Ok(())
}

// reuse 감지 시 명시적으로 다시 revoke
//
// 사실 replaced_by_session_id가 이미 있으면 이미 죽은 세션으로 봐도 되지만,
// 보안상 "재사용 시도된 세션"을 확실히 막고, updated_at도 갱신해두는 용도.
pub async fn revoke_refresh_session_as_reused(state: &AppState, session_id: Uuid) -> Result<()> {
    let url = format!(
        "{}/rest/v1/refresh_sessions?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        session_id
    );

    let payload = serde_json::json!({
        "revoked": true,
        "revoked_at": Utc::now().to_rfc3339(),
        "updated_at": Utc::now().to_rfc3339()
    });

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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
