// auth/service.rs
//
// 지갑(Solana) 로그인의 전체 비즈니스 로직을 담당하는 파일
//
// ■ 전체 흐름
// 1단계: 앱 → 서버: 지갑 주소 전송
//      서버 → 앱: 랜덤 문자열(nonce) 발급
// 2단계: 앱: nonce를 지갑 개인키로 서명
//        앱 → 서버: 지갑 주소 → nonce + 서명 전송
//      서버: 서명 검증 → DB에서 유저 조회 → JWT 발급 → 앱으로 반환
//
// ■ 왜 nonce가 필요한가?
//  지갑 로그인은 비밀번호가 없음. 대신 "이 지갑의 주인이 맞는가"를 서명으로 증명함.
//  서버가 매번 새로운 랜덤 문자열(nonce)을 주고, 그걸 서명하게 만들면 같은 서명을 훔쳐서 재사용(리플레이 공격)이 불가능함.
//
// ■ 유저 생성은 이 파일에서 하지 않음
//  흐름: 일반 회원가입 → 상세정보 입력 → 지갑 연동(wallet/service.rs)
//  이 파일은 "이미 지갑 연동을 마친 유저"의 로그인만 처리함.

// 현재 최종 구조:
// - 이메일 로그인 / 구글 로그인:
//   프론트가 Supabase로 1차 인증 -> access_token 획득
//   -> 백엔드 /auth/exchange 로 전달
//   -> 백엔드가 Supabase 토큰 검증 후 "우리 앱 JWT" 발급
//
// - 카카오 로그인:
//   백엔드가 카카오 인가 코드를 받아 직접 카카오 API 호출
//   -> 유저 찾거나 생성
//   -> "우리 앱 JWT" 발급
//
// - 지갑 로그인:
//   기존 nonce + 서명 검증 로직 유지
//   -> "우리 앱 JWT" 발급
//
// 즉 최종적으로 프론트가 저장하는 건 항상 "우리 앱 JWT" 하나뿐임.

// 이 파일이 담당하는 것:
// 1) 지갑 로그인용 nonce 발급
// 2) 지갑 서명 검증 + 로그인
// 3) Supabase access token -> 우리 앱 토큰 교환
// 4) 카카오 로그인
// 5) refresh token rotation
// 6) public.users / user_settings / streaks 보장
//
// 최종 구조:
// - access token: 짧게 사용, DB 저장 안 함
// - refresh token: refresh_sessions 테이블에서 관리
// - 웹: access는 body, refresh는 HttpOnly 쿠키
// - 앱: access/refresh 둘 다 body
//
// 중요:
// - 이 파일은 "토큰 발급/검증용 비즈니스 로직" 담당
// - 웹/앱 응답 분기, 쿠키 세팅은 handler.rs에서 담당

// ─────────────────────────────────────────────────────────────
// 회원탈퇴 라이프사이클 정책
// ─────────────────────────────────────────────────────────────
//
// 우리 서비스는 3단계 데이터 라이프사이클을 적용한다.
//
// [Phase 1] 탈퇴 즉시 (withdraw_user)
//   - is_active=false, deleted_at=now()
//   - wallet_address 즉시 null (양도 가능한 자산이라 식별자로 부적절)
//   - google_connected = false
//   - refresh_sessions 전부 revoke
//   - email/phone/nickname 등 식별 정보는 유지 (30일 쿨다운 체크용)
//
// [Phase 2] 탈퇴 후 30일 (배치 처리)
//   - provider_id, login_provider, google_connected 정리
//   - 같은 카카오/구글 계정으로 신규 가입 가능
//
// [Phase 3] 탈퇴 후 5년 (배치 처리)
//   - 부가 데이터 hard delete
//   - users row 익명화 (anonymized_at 마커, email/phone/nickname 등 모두 익명화)
//   - 거래/결제 이력은 user_id 외래키로 유지 (전자상거래법 5년 보관)
//
// 법적 근거:
// - 전자상거래법 시행령 제6조: 결제/거래 기록 5년
// - 전자금융거래법 시행령 제12조: 전자금융거래 기록 5년
// - 개인정보보호법 제21조: 5년 보관 의무 종료 후 익명화로 파기 원칙 충족
// ─────────────────────────────────────────────────────────────

// anyhow: Rust의 에러 처리 라이브러리
// anyhow!("메세지") → 에러 생성
// Context → .context("설명") 으로 에러에 설명 추가
// Result → 성공이면 Ok(값), 실패면 Err(에러)
use anyhow::{Context, Result, anyhow};
use std::time::{Duration, SystemTime};
use chrono::{DateTime, Utc};

// ed25519_dalek: Solana 지갑이 사용하는 ed25519 서명 알고리즘 라이브러리
// Signature → 서명 값을 담는 구조체 (64바이트)
// Verifier → verify() 메소드를 제공하는 trait (서명 검증용)
// VerifyingKey → 공개키 구조체 (지갑 주소를 공개키로 변환한 것)
use ed25519_dalek::{Signature, Verifier, VerifyingKey};

// rand: 랜덤 값 생성 라이브러리. nonce(랜덤 문자열) 만들 때 사용
use rand::Rng;

// serde: 직렬화/역직렬화 라이브러리
// Deserialize → HTTP 응답 JSON을 Rust 구조체로 자동 변환할 때 사용
use serde::Deserialize;

// 서버 전체 공유 상태 (Supabase URL, secret key, nonce_store 등)
use crate::state::{AppState, NonceEntry};

use serde_json::{Value, json};
use uuid::Uuid;

use crate::auth::app_jwt::{generate_token_pair, verify_app_refresh_token};
use crate::auth::refresh_store::{
    create_refresh_session, revoke_refresh_session, revoke_refresh_session_as_reused,
    revoke_refresh_session_for_rotation, verify_refresh_session,
};

/// 로그인 성공 시 공통으로 반환할 내부 결과
///
/// handler.rs는 이 값을 받아서
/// - web이면 access만 body, refresh는 쿠키
/// - app이면 access/refresh 둘 다 body
/// 로 분기한다.
#[derive(Debug, Clone)]
pub struct LoginIssueResult {
    pub access_token: String,
    pub refresh_token: String,
    pub is_new_user: bool,
    pub user_id: Uuid,
}

/// refresh rotation 성공 시 반환할 내부 결과
#[derive(Debug, Clone)]
pub struct RefreshIssueResult {
    pub access_token: String,
    pub refresh_token: String,
}

// UserRow
// Supabase PostgRest API로 public.users 테이블을 조회할 때 응답 JSON을 파싱하는 구조체.
//
// 예: GET /rest/v1/users?wallet_address=eq.7xKXt...
//     → [{"id": "550e8400-e29b-41d4-a716-446655440000"}]
//
// id만 있는 이유: 지갑 로그인에서는 user_id만 알면 JWT를 발급할 수 있음. 다른 컬럼은 불필요하므올 최소한만 가져옴 (성능 최적화)
#[derive(Debug, Deserialize)]
struct UserRow {
    // uuid::Uuid 타입으로 받으면 String → UUID 변환을 자동으로 해줌
    id: Uuid,
}

// ─────────────────────────────────────────────────────────────
// 로그인/refresh 허용 여부 확인용 users row
// ─────────────────────────────────────────────────────────────
//
// 로그인/토큰 재발급 단계에서는 public.users 전체 정보가 필요하지 않다.
// 필요한 것은 딱 2개다.
//
// - is_active:
//   관리자가 회원을 비활성화했는지 확인한다.
//   false면 로그인/refresh를 막는다.
//
// - deleted_at:
//   탈퇴 회원인지 확인한다.
//   Some이면 탈퇴한 계정이므로 로그인/refresh를 막는다.
//
// 이 구조체는 Supabase REST 응답을 역직렬화하기 위한 내부 전용 row다.
#[derive(Debug, Deserialize)]
struct LoginAllowedUserRow {
    is_active: Option<bool>,
    deleted_at: Option<String>,

    // 비활성 사유.
    inactive_reason: Option<String>,

    // 비활성 해제 예정일.
    inactive_until: Option<DateTime<Utc>>,
}

#[derive(Debug, Deserialize)]
struct PublicUserProfileStatusRow {
    profile_completed: Option<bool>,
}

const APP_SIGNUP_REQUIRED_MESSAGE: &str = "웹에서 회원가입 완료 후 다시 이용해 주세요.";
const REJOIN_COOLDOWN_DAYS: i64 = 30;

/// UTC DateTime을 한국 시간 기준 문자열로 변환한다.
///
/// DB에는 timestamptz를 UTC 기준으로 저장하고,
/// 사용자 안내 메시지에서는 KST 기준으로 보여준다.
///
/// 예:
/// 2026-05-20T09:00:00Z
/// → 2026.05.20 18:00
fn format_kst_datetime(value: DateTime<Utc>) -> String {
    let kst = value + chrono::Duration::hours(9);
    kst.format("%Y.%m.%d %H:%M").to_string()
}



// ─────────────────────────────────────────────────────────────
// 로그인/토큰 발급 가능 여부 확인
// ─────────────────────────────────────────────────────────────
//
// 이 함수는 "이 계정에게 access/refresh token을 발급해도 되는가?"를 확인한다.
//
// 정책:
// - deleted_at is not null
//   → 탈퇴한 계정이므로 차단
//
// - is_active = false
//   → 관리자가 비활성/정지 처리한 계정이므로 차단
//
// - is_active = null
//   → 과거 데이터 호환을 위해 활성으로 간주
//
// 왜 여기서 확인하나?
// - 이메일/구글 로그인은 /auth/exchange 이후 issue_login_tokens()로 모인다.
// - 카카오 로그인도 최종적으로 issue_login_tokens()로 모인다.
// - 지갑 로그인도 최종적으로 issue_login_tokens()로 모인다.
// - refresh는 rotate_refresh_token()에서 별도로 이 함수를 호출한다.
//
// 즉, 로그인 방식별로 각각 막는 것보다
// 공통 토큰 발급 직전에 막는 것이 누락 가능성이 낮다.
async fn ensure_login_allowed(
    state: &AppState,
    user_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=is_active,deleted_at,inactive_reason,inactive_until&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("회원 상태 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("회원 상태 조회 실패: {}", body));
    }

    let rows: Vec<LoginAllowedUserRow> = res
        .json()
        .await
        .context("회원 상태 조회 응답 파싱 실패")?;

    let user = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("회원 정보를 찾을 수 없습니다."))?;

    // 탈퇴 회원 차단.
    //
    // 같은 이메일 영구 재가입 금지 정책과 별개로,
    // 이미 deleted_at이 있는 계정은 로그인도 막는다.
    if user.deleted_at.is_some() {
        return Err(anyhow!(
            "탈퇴한 계정입니다. 이 계정으로는 로그인할 수 없습니다."
        ));
    }

    // 비활성 회원 차단.
    //
    // 비활성 회원은 기존 콘텐츠는 유지하지만,
    // 로그인/refresh로 새 토큰을 받을 수 없다.
    if user.is_active == Some(false) {
        let reason = user
            .inactive_reason
            .as_deref()
            .map(str::trim)
            .filter(|v| !v.is_empty())
            .unwrap_or("운영정책 위반");

        let until_text = user
            .inactive_until
            .map(format_kst_datetime)
            .unwrap_or_else(|| "미정".to_string());

        // handler.rs에서 JSON 에러로 변환하기 쉽게 prefix를 붙인다.
        //
        // 왜 JSON을 여기서 바로 반환하지 않나?
        // - service.rs는 HTTP 응답을 직접 만들지 않는 비즈니스 계층이다.
        // - handler.rs가 HTTP 상태코드/JSON 응답으로 변환하는 책임을 가진다.
        //
        // 최종 프론트 UX:
        // - toast: "비활성화된 계정입니다."
        // - 로그인 화면 안내 박스:
        //   사유 / 해제 예정일 / 문의 이메일 표시
        return Err(anyhow!(
        "ACCOUNT_INACTIVE|{}|{}",
        reason,
        until_text
    ));
    }

    Ok(())
}


// ─────────────────────────────────────────────────────────────
// 공통 토큰 발급 + refresh session 저장
//
// 모든 로그인 방식(Supabase, 카카오, 지갑)이
// 최종적으로 이 함수로 모인다.
//
// 이 함수에서 is_active/deleted_at을 확인하면,
// 이메일/구글/카카오/지갑 로그인 모두 공통으로 차단할 수 있다.
// ─────────────────────────────────────────────────────────────
pub async fn issue_login_tokens(
    state: &AppState,
    user_id: Uuid,
    client_type: &str,
    is_new_user: bool,
) -> Result<LoginIssueResult> {
    // 0) 토큰 발급 전 계정 상태 확인.
    //
    // 여기서 막는 것:
    // - 탈퇴 계정: deleted_at is not null
    // - 비활성 계정: is_active = false
    //
    // 주의:
    // access token은 stateless JWT라 이미 발급된 토큰은 만료 전까지 살아 있을 수 있다.
    // 하지만 이 체크로 "새 로그인"과 "새 토큰 발급"은 막는다.
    ensure_login_allowed(state, user_id).await?;

    // refresh 세션의 고유 ID.
    // 이 값이 refresh JWT 안의 sid로 들어간다.
    let session_id = Uuid::new_v4();

    // access / refresh JWT 생성.
    //
    // access token:
    // - DB에 저장하지 않는 stateless JWT
    // - app_jwt.rs에서 30분 만료로 설정되어 있음
    //
    // refresh token:
    // - refresh_sessions 테이블의 session_id와 연결됨
    let pair = generate_token_pair(&state.config.app_jwt_secret, &user_id, &session_id)?;

    // refresh 세션을 DB에 저장.
    //
    // 이 row가 있어야 나중에 revoke / rotation / 로그아웃이 가능하다.
    create_refresh_session(state, session_id, user_id, client_type, &pair.refresh_token).await?;

    Ok(LoginIssueResult {
        access_token: pair.access_token,
        refresh_token: pair.refresh_token,
        is_new_user,
        user_id,
    })
}

// ─────────────────────────────────────────────────────────────
// 탈퇴 후 재가입 쿨다운 체크
//
// 두 가지 컨텍스트로 분리:
// - 회원가입 시도: check_rejoin_cooldown ("재가입할 수 없습니다")
// - 로그인 시도: check_login_cooldown ("다시 시도해 주세요")
//
// 사용처:
// - check_rejoin_cooldown:
//   - check_email 핸들러 (이메일 회원가입 전 중복 확인)
// - check_login_cooldown:
//   - exchange_supabase_token [Case 1, 2] (이메일/구글 로그인)
//   - find_or_create_social_user (카카오 로그인)
//
// 지갑 로그인은 wallet_address가 즉시 null로 처리되므로 별도 쿨다운 체크 불필요.
// ─────────────────────────────────────────────────────────────

/// 회원가입 컨텍스트용 쿨다운 체크
///
/// 메시지 예: "탈퇴 후 30일 동안 재가입할 수 없습니다. N일 후 다시 시도해 주세요."
pub fn check_rejoin_cooldown(deleted_at_str: &str) -> Result<()> {
    check_cooldown_internal(deleted_at_str, false)
}

/// 로그인 컨텍스트용 쿨다운 체크
///
/// 메시지 예: "탈퇴한 계정입니다. N일 후 다시 시도해 주세요."
pub fn check_login_cooldown(deleted_at_str: &str) -> Result<()> {
    check_cooldown_internal(deleted_at_str, true)
}

fn check_cooldown_internal(deleted_at_str: &str, is_login: bool) -> Result<()> {
    // RFC3339 → DateTime<FixedOffset>로 파싱
    let deleted_at = chrono::DateTime::parse_from_rfc3339(deleted_at_str)
        .context("deleted_at 파싱 실패")?;

    // 재가입 가능 시각 = 탈퇴 시각 + 30일
    let rejoin_at = deleted_at + chrono::Duration::days(REJOIN_COOLDOWN_DAYS);
    let now = chrono::Utc::now();

    if now < rejoin_at.with_timezone(&chrono::Utc) {
        // 남은 일수 계산 (올림 처리)
        let remaining = rejoin_at.with_timezone(&chrono::Utc) - now;
        let days_left = remaining.num_days() + 1;

        return if is_login {
            // 로그인 컨텍스트: "탈퇴한 계정입니다..."
            Err(anyhow!(
                "탈퇴한 계정입니다. {}일 후 다시 시도해 주세요.",
                days_left
            ))
        } else {
            // 회원가입 컨텍스트: "탈퇴 후 N일 동안 재가입할 수 없습니다..."
            Err(anyhow!(
                "탈퇴 후 {}일 동안 재가입할 수 없습니다. {}일 후 다시 시도해 주세요.",
                REJOIN_COOLDOWN_DAYS,
                days_left
            ))
        };
    }

    Ok(())
}

/// refresh rotation
///
/// 흐름:
/// 1) refresh JWT 자체 검증
/// 2) sid로 DB refresh_sessions 조회
/// 3) DB 세션 검증
///    - revoked = false
///    - revoked_at is null
///    - expires_at 안 지남
///    - replaced_by_session_id 없음
///    - token_hash 일치
/// 4) client_type 일치 확인
/// 5) 사용자 상태 확인
///    - 탈퇴 계정 차단
///    - 비활성 계정 차단
/// 6) 새 access / refresh 발급
/// 7) 새 refresh session 저장
/// 8) 기존 refresh session revoke
pub async fn rotate_refresh_token(
    state: &AppState,
    refresh_token: &str,
    client_type: &str,
) -> Result<RefreshIssueResult> {
    // 1) refresh JWT 자체 검증.
    //
    // 여기서는 JWT 서명, exp, token_type=refresh 여부를 확인한다.
    let claims = verify_app_refresh_token(&state.config.app_jwt_secret, refresh_token)?;

    let session_id = Uuid::parse_str(&claims.sid)
        .context("refresh sid UUID 파싱 실패")?;

    let user_id = Uuid::parse_str(&claims.sub)
        .context("refresh sub UUID 파싱 실패")?;

    // 2) DB refresh session 검증.
    //
    // refresh token은 JWT 자체만 맞다고 끝이 아니다.
    // DB refresh_sessions row와도 일치해야 한다.
    //
    // 여기서:
    // - revoked
    // - revoked_at
    // - expires_at
    // - replaced_by_session_id
    // - token_hash
    // 를 모두 확인한다.
    let session = match verify_refresh_session(state, session_id, refresh_token).await {
        Ok(session) => session,
        Err(e) => {
            let msg = e.to_string();

            // reuse 감지 시엔 보수적으로 한 번 더 revoke 처리.
            //
            // 예:
            // - 이미 rotation된 refresh token을 누군가 다시 사용
            // - 토큰 재사용 공격 가능성
            if msg.contains("reuse") || msg.contains("이미 교체된 refresh token") {
                let _ = revoke_refresh_session_as_reused(state, session_id).await;
            }

            return Err(e);
        }
    };

    // 3) web/app/unity 클라이언트 타입 불일치 방지.
    //
    // 예:
    // - web에서 발급된 refresh token을 app refresh endpoint에 넣는 것 차단
    if session.client_type != client_type {
        return Err(anyhow!("refresh client_type 불일치"));
    }

    // 4) 새 access/refresh 발급 전 계정 상태 확인.
    //
    // 관리자가 사용자를 비활성화했으면
    // 기존 refresh token이 아직 유효하더라도 새 access token을 발급하면 안 된다.
    //
    // 그래서 token pair 생성 전에 반드시 확인한다.
    ensure_login_allowed(state, user_id).await?;

    // 5) 새 refresh 세션 ID 생성.
    let new_session_id = Uuid::new_v4();

    // 6) 새 access / refresh JWT 발급.
    let pair = generate_token_pair(&state.config.app_jwt_secret, &user_id, &new_session_id)?;

    // 7) 새 refresh session 저장.
    create_refresh_session(
        state,
        new_session_id,
        user_id,
        client_type,
        &pair.refresh_token,
    )
        .await?;

    // 8) 기존 refresh session revoke.
    //
    // rotation이므로 replaced_by_session_id에 새 세션 ID 기록.
    if let Err(e) = revoke_refresh_session_for_rotation(state, session_id, new_session_id).await {
        // 같은 refresh token으로 동시에 들어온 요청 중 늦게 온 쪽이면
        // 여기서 실패한다.
        //
        // 이미 만든 새 세션은 클라이언트에 반환하지 않으므로
        // best-effort로 폐기해 DB에 살아있는 고아 세션을 남기지 않는다.
        let _ = revoke_refresh_session(state, new_session_id, None).await;
        return Err(e);
    }

    Ok(RefreshIssueResult {
        access_token: pair.access_token,
        refresh_token: pair.refresh_token,
    })
}

// generate_nonce - nonce 발급 (로그인 1단계)
//
// 역할: 앱에서 지갑 주소를 보내면 랜덤 문자열(nonce)을 만들어서 반환.
//  만든 nonce는 메모리(nonce_store)에 임시 저장 → 2단계 검증 때 꺼내 비교.
//
// ■ DashMap이란?
//  HashMap과 같지만 멀티스레드 환경에서 안전하게 쓸 수 있음.
//  Axum은 비동기 멀티스레드로 동작하므로 일반 HashMap 대신 DashMap을 사용.
//  nonce_store: { "지갑주소": "nonce값" } 형태로 저장함.
//
// ■ 같은 지갑 주소로 다시 요청하면?
//  기존 nonce가 새 nonce로 덮어써짐 → 한 지갑당 유효한 nonce는 항상 1개
pub fn generate_nonce(state: &AppState, wallet_address: &str) -> String {
    // Alphanumeric: 영문자(대소문자) + 숫자로 구성된 문자 집합
    // .take(32) → 32글자만 뽑음
    // .map(char::from) → u8 바이트 → char로 변환
    // .collect() → 문자들을 모아서 String으로 조립
    // 결과 예시: "aB3kQ9xZ2mL7pR4wT1yN8cV5hJ0gF6s"
    let nonce: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(32)
        .map(char::from)
        .collect();

    // nonce store에 저장: 키=지갑주소, 값=NonceEntry(nonce + 5분 후 만료시각)
    // 2단계 verify_and_login()에서 꺼내서 비교함
    state.nonce_store.insert(
        wallet_address.to_string(),
        NonceEntry {
            nonce: nonce.clone(),
            expires_at: SystemTime::now() + Duration::from_secs(300),
        },
    );

    tracing::info!("nonce 발급 완료: wallet={}", wallet_address);

    nonce
}

pub fn build_wallet_sign_message(wallet_address: &str, nonce: &str) -> String {
    format!(
        "Spentopia 지갑 인증\n\n이 서명은 Spentopia 로그인 또는 지갑 연동을 위한 요청입니다.\n블록체인 트랜잭션이 아니며, 가스비가 발생하지 않습니다.\n\n지갑 주소: {}\n인증 코드: {}\n유효 시간: 5분\n\n본인이 요청한 경우에만 서명하세요.",
        wallet_address, nonce
    )
}

// verify_and_login - 서명 검증 + 로그인 처리 (로그인 2단계)
//
// 역할: 앱에서 보낸 {지갑주소, nonce, 서명}을 검증하고 JWT를 발급.
//
// ■ 처리 순서
//  1) nonce 검증 → 1단계에서 발급한 nonce가 맞는지 확인 (위조 방지)
//  2) 서명 검증 → 실제로 이 지갑의 주인이 서명한 게 맞는지 확인
//  3) 유저 조회 → DB(public.users)에서 이 지갑 주소로 등록된 유저 찾기
//  4) JWT 발급 → 찾은 유저의 access_token + refresh_token 생성
//  5) 응답 반환 → 앱으로 토큰 전달
pub async fn verify_and_login(
    state: &AppState,
    wallet_address: &str,
    nonce: &str,
    signature: &str,
    client_type: &str,
) -> Result<LoginIssueResult> {
    // 1) nonce 검증
    // nonce_store에서 이 지갑 주소에 해당하는 NonceEntry를 꺼냄.
    // ok_or_else: Option이 None이면 에러로 변환
    // None인 경우 → 1단계를 호출하지 않았거나 이미 사용된 nonce
    let entry = state
        .nonce_store
        .get(wallet_address)
        .ok_or_else(|| anyhow!("nonce가 없거나 만료됨. /auth/wallet/nonce를 먼저 호출하세요."))?;

    // TTL 체크: 발급 후 5분이 지났으면 만료 처리
    if SystemTime::now() > entry.expires_at {
        drop(entry);
        state.nonce_store.remove(wallet_address);
        return Err(anyhow!(
            "nonce가 만료됨. /auth/wallet/nonce를 다시 호출하세요."
        ));
    }

    // 저장된 nonce와 앱이 보낸 nonce가 다르면 위조된 요청
    if entry.nonce != nonce {
        return Err(anyhow!("nonce 불일치. 위조된 요청일 수 있음."));
    }

    // nonce는 1회용이므로 검증 즉시 삭제
    // drop()을 먼저 해야 DashMap의 읽기 잠금이 풀려서 remove()가 가능함
    drop(entry);
    state.nonce_store.remove(wallet_address);

    // 2) Solana 서명검증
    // 지갑 주소(공개키)로 서명을 검증
    // 성공 → 이 서명을 만든 사람이 이 지갑의 개인키 소유자임이 증명됨
    // ?는 에러면 즉시 이 함수를 종료하고 에러를 반환
    verify_solana_signature(wallet_address, nonce, signature)?;

    tracing::info!("서명 검증 성공: wallet={}", wallet_address);

    // 3) DB에서 지갑 주소로 유저 조회
    // public.users.wallet_address = wallet_address인 row를 찾음
    // 찾으면 → 해당 유저의 UUID 반환
    // 없으면 → 지갑 연동을 안 한 유저이므로 로그인 불가 에러 반환
    let user_id = find_user_by_wallet(state, wallet_address).await?;

    if client_type == "app"
        && !get_public_user_profile_completed_by_user_id(state, &user_id.to_string())
            .await?
            .unwrap_or(false)
    {
        tracing::warn!(
            "앱 지갑 로그인 차단: 회원가입 미완료 user_id={} wallet={}",
            user_id,
            wallet_address
        );
        return Err(anyhow!(APP_SIGNUP_REQUIRED_MESSAGE));
    }

    // 최종 토큰 발급 + refresh session 저장
    // 지갑 로그인은 기존 유저만 가능하므로 is_new_user = false
    issue_login_tokens(state, user_id, client_type, false).await
}

// verify_solana_signature - Solana ed25519 서명 검증
//
// ■ 배경 지식
//  Solana 지갑 주소 = ed25519 공개키를 Base58로 인코딩한 문자열
//  서명 = 개인키로 인증 메시지를 서명한 64바이트 → Base58 인코딩
//
// ■ 검증 원리
//  공개키(지갑 주소)로 인증 메시지 서명을 검증할 수 있으면 → 검증 성공
//  즉, 이 서명을 만들 수 있는 건 이 지갑의 개인키 소유자뿐임을 수학적으로 증명
//
// ■ Base58이란?
//  Bitcoin/Solana에서 쓰는 인코딩 방식.
//  바이트 배열을 사람이 읽을 수 있는 문자열로 변환.
//  헷갈리는 문자(0, O, l, I)를 제외해서 가독성이 좋음.
fn verify_solana_signature(
    // "7xKXtg2CW87d..." 형태의 Solana 지갑 주소 (Base58 인코딩된 공개키)
    wallet_address: &str,
    // 서버가 1단계에서 발급한 nonce. 실제 서명 대상은 nonce를 포함한 인증 메시지다.
    nonce: &str,
    // 앱이 지갑으로 서명한 값 (Base58 인코딩된 64바이트)
    signature: &str,
) -> Result<()> {
    let message = build_wallet_sign_message(wallet_address, nonce);

    tracing::debug!(
        "[서명검증] 시작: wallet={}, nonce_len={}, message_len={}, sig_chars={}",
        wallet_address,
        nonce.len(),
        message.len(),
        signature.len()
    );

    // Base58 문자열("7xKXtg2...") → 바이트 배열([0x7a, 0x2b, ...])로 변환
    let pubkey_bytes = bs58::decode(wallet_address)
        .into_vec()
        .context("지갑 주소 Base58 디코딩 실패. 유효한 Solana 주소인지 확인 필요.")?;

    tracing::debug!("[서명검증] 공개키 바이트 수: {}", pubkey_bytes.len());

    // ed25519 공개키는 정확히 32바이트여야 함
    // try_into(): Vec<u8> → [u8; 32] 고정 배열로 변환 시도
    // 32바이트가 아니면 → 잘못된 지갑 주소
    let pubkey_array: [u8; 32] = pubkey_bytes
        .try_into()
        .map_err(|_| anyhow!("공개키가 32바이트가 아님. 잘못된 지갑 주소."))?;

    // 바이트 배열 → ed25519 검증용 공개키 객체로 변환
    let verifying_key =
        VerifyingKey::from_bytes(&pubkey_array).context("ed25519 공개키 객체 생성 실패")?;

    // Base58 문자열("3bNzR8K...") → 바이트 배열([0x3b, 0x4e, ...])로 변환
    let sig_bytes = bs58::decode(signature)
        .into_vec()
        .context("서명 Base58 디코딩 실패")?;

    tracing::debug!("[서명검증] 서명 바이트 수: {}", sig_bytes.len());

    // ed25519 서명은 정확히 64바이트여야 함
    let sig_array: [u8; 64] = sig_bytes
        .try_into()
        .map_err(|_| anyhow!("서명이 64바이트가 아님. 잘못된 서명."))?;

    let sig = Signature::from_bytes(&sig_array);

    // 한국어 안내 문구가 포함된 인증 메시지를 검증 대상으로 사용
    // 성공 → 이 서명은 이 공개키에 대응하는 개인키로 만든 것이 맞음
    // 실패 → 서명이 위조됐거나 다른 지갑으로 서명한 것
    let result = verifying_key.verify(message.as_bytes(), &sig);
    if let Err(ref e) = result {
        tracing::warn!(
            "[서명검증] 실패! message_len={}, 에러={:?}",
            message.len(),
            e
        );
    } else {
        tracing::debug!("[서명검증] 성공");
    }
    result.context("서명 검증 실패. 지갑 주인이 아니거나 nonce가 변조됨.")?;
    Ok(())
}

// wallet/service.rs에서 서명 검증 로직을 재사용하기 위한 pub 래퍼
// verify_solana_signature는 private 함수라서 외부 모듈에서 직접 호출 불가
// pub으로 감싸서 외부에서 접근할 수 있게 노출시킴
pub fn verify_solana_signature_pub(
    wallet_address: &str,
    nonce: &str,
    signature: &str,
) -> Result<()> {
    verify_solana_signature(wallet_address, nonce, signature)
}

// find_user_by_wallet - DB에서 지갑 주소로 유저 조회
//
// Supabase PostgREST API로 public.users를 조회
//
// ■ PostgREST란?
//  Supabase가 PostgreSQL 테이블을 자동으로 REST API로 변환해주는 기능.
//  URL 파라미터로 SQL 조건을 표현할 수 있음.
//   예: ?wallet_address=eq.7xKXt... → WHERE wallet_address = '7xKXt...'
//
// 변경 사항:
// - 쿼리에 deleted_at=is.null 필터 추가
//   → 탈퇴자는 자연스럽게 조회 결과에서 제외됨
//
// 왜 쿨다운 체크 안 하나:
// - withdraw_user에서 wallet_address를 즉시 null로 만듦
// - 따라서 같은 지갑 주소로 검색해도 탈퇴자 row는 매칭되지 않음
// - 다른 정상 유저가 같은 지갑을 새로 연결한 케이스는
//   정상적으로 그 유저의 id를 반환하면 됨
//
// 이중 안전장치:
// - 혹시 데이터 마이그레이션 중 wallet_address가 남아있어도
//   deleted_at=is.null 필터에 걸려서 안전하게 차단됨
//
// wallet/service.rs에서도 중복 연동 체크 시 재사용하므로 pub

pub async fn find_user_by_wallet(state: &AppState, wallet_address: &str) -> Result<Uuid> {
    // wallet_address=eq.{주소} → WHERE wallet_address = '{주소}'
    // select=id → id 컬럼만 가져옴 (불필요한 컬럼 제외)
    // trim_end_matches('/'): URL 끝 슬래시 제거 (중복 방지)
    let url = format!(
        "{}/rest/v1/users?wallet_address=eq.{}&deleted_at=is.null&select=id",
        state.config.supabase_url.trim_end_matches('/'),
        wallet_address,
    );

    let resp = state
        .http_client
        .get(&url)
        // supabase_secret_key: Admin 권한 → RLS(Row Level Security) 우회 가능
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        // apikey: Supabase REST API 호출 시 필수 헤더
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("유저 조회 HTTP 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("지갑 유저 조회 실패: {}", err));
    }

    // PostgRest는 항상 배열로 응답: [] 또는 [{"id": "..."}]
    // Vec<UserRow>로 파싱해서 받음
    let rows: Vec<UserRow> = resp.json().await.context("유저 조회 JSON 파싱 실패")?;

    // into_iter().next(): 첫 번쨰 요소만 꺼냄
    // wallet_address는 UNIQUE 컬럼이므로 결과는 항상 0개 또는 1개
    // ,map(|row| row.id): UserRow에서 id만 추출
    // .ok_or_else: None(결과 없음)이면 에러로 변환
    rows.into_iter().next().map(|row| row.id).ok_or_else(|| {
        anyhow!("해당 지갑 주소로 연동된 계정이 없습니다. 먼저 지갑 연동을 해주세요.")
    })
}

// generate_supabase_token - Supabase JWT 발급
//
// ■ 왜 서버에서 직접 발급하나?
//  일반 로그인(이메일/소셜)은 Supabase SDK가 클라이언트에서 JWT를 발급함.
//  지갑 로그인은 Supabase의 기본 인증 플로우를 타지 않아서 서버가 서명 검증 완료 후 Admin 권한으로 대신 발급해줘야 함.
//
// ■ 발급된 토큰의 형태
//  이메일/소셜 로그인 JWT와 완전히 동일한 형태.
//  middleware.rs의 jwt_middleware에서 동일하게 검증 가능.
// ═══════════════════════════════════════════════════════════════
// [이메일/구글] Supabase access_token -> 앱 JWT 교환
// ═══════════════════════════════════════════════════════════════

// ─────────────────────────────────────────────────────────────
// Supabase access_token -> 우리 앱 토큰 교환
//
// 기존 흐름:
// 프론트가 Supabase 로그인 성공 후 access_token 획득
// -> /auth/exchange 로 전달
// -> 백엔드가 Supabase user 조회
// -> public.users 보장
// -> 우리 access/refresh 발급
// ─────────────────────────────────────────────────────────────

pub async fn exchange_supabase_token(
    state: &AppState,
    supabase_access_token: &str,
    client_type: &str,
) -> Result<LoginIssueResult> {
    let user_url = format!(
        "{}/auth/v1/user",
        state.config.supabase_url.trim_end_matches('/')
    );

    let resp = state
        .http_client
        .get(&user_url)
        .header("apikey", &state.config.supabase_publishable_key)
        .header("Authorization", format!("Bearer {}", supabase_access_token))
        .send()
        .await
        .context("Supabase 유저 조회 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("유효하지 않은 Supabase 토큰: {}", err));
    }

    let user_data: Value = resp.json().await.context("Supabase 유저 응답 파싱 실패")?;

    let user_id = user_data["id"]
        .as_str()
        .ok_or_else(|| anyhow!("Supabase 유저 ID 없음"))?;

    let email = user_data["email"].as_str();

    // 가입 차단 도메인 검사 (신규 유저만 차단, 기존 유저 로그인은 허용)
    if let Some(mail) = email {
        let domain = mail.split('@').nth(1).unwrap_or("").to_ascii_lowercase();
        if domain == "admin.com" {
            let existing = find_public_user_by_email(state, mail).await.unwrap_or(None);
            if existing.is_none() {
                return Err(anyhow!("해당 이메일으로는 가입할 수 없습니다."));
            }
        }
    }

    // 기본 가입 방식(대표 provider)
    let provider = user_data["app_metadata"]["provider"]
        .as_str()
        .unwrap_or("email");

    // Email + Google 같이 연결된 계정까지 감지해야 하므로
    // provider 하나만 믿지 말고 providers / identities를 같이 본다.
    let google_connected = user_data["app_metadata"]["providers"]
        .as_array()
        .map(|providers| {
            providers.iter().any(|p| {
                p.as_str()
                    .map(|x| x.eq_ignore_ascii_case("google"))
                    .unwrap_or(false)
            })
        })
        .or_else(|| {
            user_data["identities"].as_array().map(|arr| {
                arr.iter().any(|identity| {
                    identity["provider"]
                        .as_str()
                        .map(|p| p.eq_ignore_ascii_case("google"))
                        .unwrap_or(false)
                })
            })
        })
        .unwrap_or(false);

    let provider_id = user_data["user_metadata"]["provider_id"]
        .as_str()
        .or_else(|| user_data["user_metadata"]["sub"].as_str())
        .or_else(|| user_data["app_metadata"]["provider_id"].as_str())
        .or_else(|| {
            user_data["identities"]
                .as_array()
                .and_then(|arr| {
                    arr.iter().find(|identity| {
                        identity["provider"]
                            .as_str()
                            .map(|p| p.eq_ignore_ascii_case(provider))
                            .unwrap_or(false)
                    })
                })
                .and_then(|identity| identity["id"].as_str())
        });

    tracing::info!(
        "Supabase exchange: user_id={}, email={:?}, provider={}, provider_id={:?}, google_connected={}",
        user_id,
        email,
        provider,
        provider_id,
        google_connected
    );

    // ── 탈퇴 유저 로그인 차단 ────────────────────────────────
    //
    // [Case 1] user_id 기반 체크
    // - 이메일 로그인: 탈퇴 후 auth.users가 삭제됐으므로 보통 Supabase 레벨에서 차단됨.
    //   그러나 만료되지 않은 토큰이 남아있는 경우를 대비해 이 체크를 유지.
    //
    // [Case 2] email 기반 체크
    // - 구글 로그인: 탈퇴 후 auth.users가 삭제되면 재로그인 시 Supabase가
    //   동일 이메일로 새 auth.users row를 생성 → 새 user_id 발급.
    //   새 user_id로는 public.users에서 deleted_at을 찾을 수 없으므로
    //   email 기반으로 추가 확인해야 함.
    let deleted_check_url = format!(
        "{}/rest/v1/users?id=eq.{}&select=deleted_at&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let deleted_resp = state
        .http_client
        .get(&deleted_check_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("탈퇴 여부 조회 실패")?;

    // [Case 1] user_id 기반 탈퇴 체크
    //
    // 이메일 로그인은 탈퇴 시 auth.users도 살아있으므로
    // 동일 user_id로 다시 들어올 수 있다.
    // 이때 public.users.deleted_at이 살아있으면 차단 또는 쿨다운 체크.
    //
    // 쿨다운 만료 시: 같은 user_id로 부활시키지 않고 새 가입 유도
    //                (deleted_at은 그대로 두고, 사용자에게 새로 가입하라고 안내)
    if deleted_resp.status().is_success() {
        let rows: Vec<serde_json::Value> = deleted_resp.json().await.unwrap_or_default();
        if let Some(row) = rows.first() {
            if let Some(deleted_at_str) = row["deleted_at"].as_str() {
                // 30일 쿨다운 체크 → 미만이면 여기서 에러 던지고 종료
                check_login_cooldown(deleted_at_str)?;

                // 30일 지났어도 같은 row를 부활시키지 않음
                // 정책: 탈퇴자는 새 계정으로 가입해야 함
                tracing::info!(
                "탈퇴 후 쿨다운 만료 유저 로그인 시도: user_id={}",
                user_id
            );
                return Err(anyhow!(
                "탈퇴한 계정입니다. 새로 회원가입해 주세요."
            ));
            }
        }
    }

    // [Case 2] email 기반 탈퇴 체크
    //
    // 구글 재로그인 시 Supabase가 새 user_id를 발급할 수 있는 경우 대비.
    // (탈퇴 후 auth.users가 살아있으면 같은 user_id이지만,
    //  만약 정책 변경으로 auth.users가 지워진다면 새 user_id가 발급됨)
    //
    // 단, withdraw_user에서 email을 마스킹하므로 평소엔 이 분기 매칭 안 됨.
    // 그래도 안전망으로 남겨둠.
    if let Some(mail) = email {
        let email_check_url = format!(
            "{}/rest/v1/users?email=eq.{}&select=deleted_at&limit=1",
            state.config.supabase_url.trim_end_matches('/'),
            urlencoding::encode(mail)
        );
        let email_check_resp = state
            .http_client
            .get(&email_check_url)
            .header("apikey", &state.config.supabase_secret_key)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .send()
            .await
            .context("이메일 기반 탈퇴 여부 조회 실패")?;
        if email_check_resp.status().is_success() {
            let rows: Vec<serde_json::Value> = email_check_resp.json().await.unwrap_or_default();
            if let Some(row) = rows.first() {
                if let Some(deleted_at_str) = row["deleted_at"].as_str() {
                    tracing::warn!(
                    "탈퇴 유저 재로그인 시도 (email 기반): email={}",
                    mail
                );
                    // 30일 쿨다운 체크
                    check_login_cooldown(deleted_at_str)?;
                    // 쿨다운 지났어도 부활 X
                    return Err(anyhow!(
                    "탈퇴한 계정입니다. 새로 회원가입해 주세요."
                ));
                }
            }
        }
    }

    if client_type == "app" {
        let completed_user_exists =
            has_completed_public_user_for_app_login(state, user_id, email, google_connected)
                .await?;

        if !completed_user_exists {
            tracing::warn!(
                "앱 로그인 차단: 회원가입 미완료 user_id={} email={:?}",
                user_id,
                email
            );
            return Err(anyhow!(APP_SIGNUP_REQUIRED_MESSAGE));
        }
    }

    let resolved_user_id = ensure_public_user_exists(
        state,
        user_id,
        email,
        provider,
        provider_id,
        google_connected,
    )
    .await?;

    let user_uuid =
        Uuid::parse_str(&resolved_user_id).context("최종 사용자 user_id UUID 파싱 실패")?;

    issue_login_tokens(state, user_uuid, client_type, false).await
}

async fn has_completed_public_user_for_app_login(
    state: &AppState,
    user_id: &str,
    email: Option<&str>,
    google_connected: bool,
) -> Result<bool> {
    if let Some(is_completed) = get_public_user_profile_completed_by_user_id(state, user_id).await?
    {
        return Ok(is_completed);
    }

    if google_connected {
        if let Some(real_email) = email {
            return get_public_user_profile_completed_by_email(state, real_email).await;
        }
    }

    Ok(false)
}

async fn get_public_user_profile_completed_by_user_id(
    state: &AppState,
    user_id: &str,
) -> Result<Option<bool>> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&deleted_at=is.null&select=profile_completed&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(user_id)
    );

    let resp = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("앱 로그인용 public.users(user_id) 조회 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!(
            "앱 로그인용 public.users(user_id) 조회 실패: {}",
            err
        ));
    }

    let rows: Vec<PublicUserProfileStatusRow> = resp
        .json()
        .await
        .context("앱 로그인용 public.users(user_id) 응답 파싱 실패")?;

    Ok(rows
        .into_iter()
        .next()
        .map(|row| row.profile_completed.unwrap_or(false)))
}

async fn get_public_user_profile_completed_by_email(state: &AppState, email: &str) -> Result<bool> {
    let url = format!(
        "{}/rest/v1/users?email=eq.{}&deleted_at=is.null&select=profile_completed&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(email)
    );

    let resp = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("앱 로그인용 public.users(email) 조회 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!(
            "앱 로그인용 public.users(email) 조회 실패: {}",
            err
        ));
    }

    let rows: Vec<PublicUserProfileStatusRow> = resp
        .json()
        .await
        .context("앱 로그인용 public.users(email) 응답 파싱 실패")?;

    Ok(rows
        .into_iter()
        .next()
        .and_then(|row| row.profile_completed)
        .unwrap_or(false))
}

// ═══════════════════════════════════════════════════════════════
// public.users / user_settings / streaks 보장
//
// auth.users는 있어도 public.users가 비는 경우가 있어서,
// 로그인 시점에 안전하게 upsert함.
// ═══════════════════════════════════════════════════════════════
async fn ensure_public_user_exists(
    state: &AppState,
    user_id: &str,
    email: Option<&str>,
    provider: &str,
    provider_id: Option<&str>,
    google_connected: bool,
) -> Result<String> {
    // ─────────────────────────────────────────────────────────
    // 0) 구글 연결 특수 처리
    //
    // 이미 같은 이메일의 기존 email 계정이 있고,
    // 이번 Supabase 유저가 google도 연결된 상태라면
    // public.users는 기존 계정을 유지하고 google_connected만 true로 바꾼다.
    // ─────────────────────────────────────────────────────────
    if google_connected {
        if let Some(real_email) = email {
            if let Some(existing_user_id) = find_public_user_by_email(state, real_email).await? {
                // 현재 auth.users의 id와 public.users 기존 id가 다를 때만 연결 처리
                if existing_user_id != user_id {
                    tracing::info!(
                        "기존 이메일 계정 발견 → google_connected 연결: email={}, existing_user_id={}, supabase_user_id={}",
                        real_email,
                        existing_user_id,
                        user_id
                    );

                    link_google_to_existing_user(state, &existing_user_id).await?;
                    ensure_settings_and_streaks_exist(state, &existing_user_id).await?;
                    return Ok(existing_user_id);
                }
            }
        }
    }

    let users_url = format!(
        "{}/rest/v1/users",
        state.config.supabase_url.trim_end_matches('/')
    );

    // 1) 신규 유저 INSERT (이미 존재하면 무시)
    let insert_payload = json!([{
        "id": user_id,
        "email": email,
        "login_provider": provider,
        "provider_id": provider_id,
        "google_connected": google_connected,
        "profile_image": "defaults/avatar.png",
        "created_at": chrono::Utc::now(),
        "updated_at": chrono::Utc::now(),
    }]);

    let insert_resp = state
        .http_client
        .post(&users_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=ignore-duplicates")
        .json(&insert_payload)
        .send()
        .await
        .context("public.users INSERT 요청 실패")?;

    if !insert_resp.status().is_success() {
        let err = insert_resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users INSERT 실패: {}", err));
    }

    // 2) 기존/신규 모두 활성 상태 갱신
    let patch_url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(user_id)
    );

    // email이 Some일 때만 PATCH에 포함 (None이면 기존 이메일 유지)
    let mut patch_data = serde_json::Map::new();
    patch_data.insert("updated_at".to_string(), json!(chrono::Utc::now()));
    patch_data.insert("google_connected".to_string(), json!(google_connected));
    if let Some(mail) = email {
        patch_data.insert("email".to_string(), json!(mail));
    }

    let patch_resp = state
        .http_client
        .patch(&patch_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&patch_data)
        .send()
        .await
        .context("public.users 활성 상태 갱신 실패")?;

    if !patch_resp.status().is_success() {
        let err = patch_resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users 활성 상태 갱신 실패: {}", err));
    }

    // 3) user_settings / streaks 보장
    ensure_settings_and_streaks_exist(state, user_id).await?;

    Ok(user_id.to_string())
}

async fn ensure_settings_and_streaks_exist(state: &AppState, user_id: &str) -> Result<()> {
    let settings_url = format!(
        "{}/rest/v1/user_settings?on_conflict=user_id",
        state.config.supabase_url.trim_end_matches('/')
    );

    let settings_resp = state
        .http_client
        .post(&settings_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .json(&json!([{
            "user_id": user_id
        }]))
        .send()
        .await
        .context("user_settings 보장 실패")?;

    if !settings_resp.status().is_success() {
        let err = settings_resp.text().await.unwrap_or_default();
        return Err(anyhow!("user_settings 보장 실패: {}", err));
    }

    let streaks_url = format!(
        "{}/rest/v1/streaks?on_conflict=user_id",
        state.config.supabase_url.trim_end_matches('/')
    );

    let streaks_resp = state
        .http_client
        .post(&streaks_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=merge-duplicates,return=minimal")
        .json(&json!([{
            "user_id": user_id,
            "current_streak": 0,
            "longest_streak": 0
        }]))
        .send()
        .await
        .context("streaks 보장 실패")?;

    if !streaks_resp.status().is_success() {
        let err = streaks_resp.text().await.unwrap_or_default();
        return Err(anyhow!("streaks 보장 실패: {}", err));
    }

    Ok(())
}

// ═══════════════════════════════════════════════════════════════
// 카카오 로그인
// 1) code -> 카카오 access_token 교환
// 2) 카카오 유저 정보 조회
// 3) provider + provider_id 기준 기존 유저 찾기 / 없으면 생성
// 4) 최종 토큰 발급은 issue_login_tokens()로 통일
// ═══════════════════════════════════════════════════════════════
pub async fn kakao_login(
    state: &AppState,
    code: &str,
    client_type: &str,
) -> Result<LoginIssueResult> {
    // OAuth 인가 code는 단기 자격증명이므로 로그에 남기지 않음.
    // 로그에 code가 기록되면 로그 열람 권한을 가진 사람이
    // 만료(약 10분) 전에 재사용할 수 있어 계정 탈취로 이어질 수 있음.
    tracing::info!("카카오 로그인 시작");

    let token_url = "https://kauth.kakao.com/oauth/token";

    let kakao_client_id = if client_type == "app" {
        std::env::var("KAKAO_ANDROID_REST_API_KEY")
            .context("KAKAO_ANDROID_REST_API_KEY 환경변수 없음")?
    } else {
        std::env::var("KAKAO_REST_API_KEY").context("KAKAO_REST_API_KEY 환경변수 없음")?
    };

    let kakao_client_secret = if client_type == "app" {
        std::env::var("KAKAO_ANDROID_CLIENT_SECRET").unwrap_or_default()
    } else {
        std::env::var("KAKAO_CLIENT_SECRET").unwrap_or_default()
    };

    let redirect_uri = if client_type == "app" {
        state.config.kakao_app_redirect_uri.clone()
    } else {
        state.config.kakao_redirect_uri.clone()
    };

    let mut token_form = vec![
        ("grant_type", "authorization_code".to_string()),
        ("client_id", kakao_client_id),
        ("redirect_uri", redirect_uri),
        ("code", code.to_string()),
    ];

    if !kakao_client_secret.trim().is_empty() {
        token_form.push(("client_secret", kakao_client_secret));
    }

    let token_resp = state
        .http_client
        .post(token_url)
        .form(&token_form)
        .send()
        .await
        .context("카카오 토큰 요청 실패")?;

    if !token_resp.status().is_success() {
        let err = token_resp.text().await.unwrap_or_default();
        tracing::error!("카카오 토큰 교환 실패: {}", err);
        return Err(anyhow!("카카오 토큰 교환 실패: {}", err));
    }

    let token_data: Value = token_resp
        .json()
        .await
        .context("카카오 토큰 응답 파싱 실패")?;

    let kakao_access_token = token_data["access_token"]
        .as_str()
        .ok_or_else(|| anyhow!("카카오 access_token 없음"))?;

    let user_resp = state
        .http_client
        .get("https://kapi.kakao.com/v2/user/me")
        .header("Authorization", format!("Bearer {}", kakao_access_token))
        .send()
        .await
        .context("카카오 유저 정보 요청 실패")?;

    if !user_resp.status().is_success() {
        let err = user_resp.text().await.unwrap_or_default();
        tracing::error!("카카오 유저 정보 조회 실패: {}", err);
        return Err(anyhow!("카카오 유저 정보 조회 실패: {}", err));
    }

    let user_data: Value = user_resp
        .json()
        .await
        .context("카카오 유저 정보 파싱 실패")?;

    let kakao_id = user_data["id"]
        .as_i64()
        .ok_or_else(|| anyhow!("카카오 유저 ID 없음"))?;

    let provider = "kakao";
    let provider_id = kakao_id.to_string();

    let nickname = user_data["kakao_account"]["profile"]["nickname"]
        .as_str()
        .map(|s| s.trim())
        .filter(|s| !s.is_empty());

    let email = user_data["kakao_account"]["email"]
        .as_str()
        .map(|s| s.trim())
        .filter(|s| !s.is_empty());

    tracing::info!(
        "카카오 유저 정보 조회 성공: provider={}, provider_id={}, nickname={:?}, email={:?}",
        provider,
        provider_id,
        nickname,
        email
    );

    let (user_id, is_new_user) =
        find_or_create_social_user(state, provider, &provider_id, email, nickname).await?;

    let user_uuid = Uuid::parse_str(&user_id).context("카카오 user_id UUID 파싱 실패")?;

    if client_type == "app"
        && !get_public_user_profile_completed_by_user_id(state, &user_id)
            .await?
            .unwrap_or(false)
    {
        tracing::warn!(
            "앱 카카오 로그인 차단: 회원가입 미완료 user_id={} email={:?}",
            user_id,
            email
        );
        return Err(anyhow!(APP_SIGNUP_REQUIRED_MESSAGE));
    }

    issue_login_tokens(state, user_uuid, client_type, is_new_user).await
}

// ═══════════════════════════════════════════════════════════════
// 회원탈퇴 처리
// ═══════════════════════════════════════════════════════════════
//
// 처리 순서:
// 1) public.users → soft delete + 일부 식별자 정리
// 2) refresh_sessions → 전체 revoke
//
// 즉시 처리:
// - deleted_at = now()
// - is_active = false
// - wallet_address = null  (양도 가능 자산이라 식별자로 부적절)
// - google_connected = false
//
// 유지 대상 (30일 쿨다운 체크용):
// - email
// - phone
// - nickname
// - provider_id, login_provider
//
// 30일 후 (Phase 2 배치): provider_id, login_provider 정리
// 5년 후 (Phase 3 배치): 모든 식별 정보 익명화
//
// 왜 auth.users는 안 지우나:
// - public.users.id가 auth.users(id) ON DELETE CASCADE로 묶여있음
// - auth.users 삭제 시 public.users row가 함께 날아감 → soft delete 의도 무너짐
// - 로그인 차단은 deleted_at 체크로 처리 (이미 4군데 구현됨)
//
// 왜 즉시 마스킹 안 하나:
// - 거래/결제 5년 보관 의무 때문에 어차피 row는 살아있음
// - 30일 쿨다운 체크 단순화 (deleted_at 체크만으로 충분)
// - 정책 일관성 (provider_id 등도 30일까지 유지하는 것과 동일)
// - 5년 후 Phase 3 배치에서 일괄 익명화로 처리
//
// 법적 근거:
// - 개인정보보호법: 5년 후 완전 익명화로 즉시 파기 원칙 충족
// - 전자상거래법: 거래/결제 5년 보관 → row 자체는 유지
// - 두 법이 충돌하는 부분을 라이프사이클로 분리
pub async fn withdraw_user(state: &AppState, user_id: Uuid) -> Result<()> {
    let now = chrono::Utc::now();

    // ── 1) public.users soft delete ────────────────────────
    //
    // 처리 정책:
    // - deleted_at 마커 + is_active=false (즉시 비활성)
    // - wallet_address NULL (양도 가능 자산이라 식별자로 부적절)
    // - google_connected false
    //
    // 유지 대상 (30일 쿨다운 체크용):
    // - email
    // - phone
    // - nickname
    // - provider_id, login_provider
    //
    // 5년 후 Phase 3 배치에서 일괄 익명화됨.
    //
    // 마스킹을 즉시 안 하는 이유:
    // - 거래/결제 5년 보관 의무 때문에 어차피 row는 살아있음
    // - 30일 쿨다운 체크 단순화 (deleted_at 체크만으로 충분)
    // - 정책 일관성 (provider_id 등도 30일까지 유지하는 것과 동일)

    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches("/"),
        user_id
    );

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&json!({
        "deleted_at": now,
        "is_active": false,
        "wallet_address": null,
        "google_connected": false,
        "updated_at": now,
    }))
        .send()
        .await
        .context("public.users 탈퇴 처리 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users 탈퇴 처리 실패: {}", err));
    }

    // ── 2) refresh_sessions 전체 revoke ────────────────────
    //
    // 다른 기기에 남아있는 모든 로그인 세션을 즉시 만료시킴.
    // 실패해도 탈퇴 자체는 성공으로 처리 (warn 로그만 남김).
    // 어차피 사용자는 deleted_at 체크에 막혀서 access token 갱신 불가능.
    let sessions_url = format!(
        "{}/rest/v1/refresh_sessions?user_id=eq.{}&revoked=eq.false",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let revoke_resp = state
        .http_client
        .patch(&sessions_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&json!({
            "revoked": true,
            "revoked_at": now,
            "updated_at": now
        }))
        .send()
        .await
        .context("refresh_sessions revoke 요청 실패")?;

    if !revoke_resp.status().is_success() {
        let err = revoke_resp.text().await.unwrap_or_default();
        tracing::warn!(
            "회원탈퇴 세션 revoke 실패 (탈퇴 자체는 성공): user_id={}, error={}",
            user_id,
            err
        );
    }

    tracing::info!("회원탈퇴 soft delete 완료: user_id={}", user_id);
    Ok(())
}

// ═══════════════════════════════════════════════════════════════
// provider + provider_id 기준으로 기존 유저 찾거나 생성
// ═══════════════════════════════════════════════════════════════
async fn find_or_create_social_user(
    state: &AppState,
    provider: &str,
    provider_id: &str,
    email: Option<&str>,
    nickname: Option<&str>,
) -> Result<(String, bool)> {
    // 1️⃣ 기존 소셜 계정 찾기 (provider + provider_id)
    if let Some(existing_user_id) =
        find_public_user_by_provider(state, provider, provider_id).await?
    {
        tracing::info!(
            "기존 소셜 유저 발견: provider={}, provider_id={}, user_id={}",
            provider,
            provider_id,
            existing_user_id
        );

        // ── 탈퇴 유저 로그인 차단 ───────────────────────────────
        // exchange_supabase_token 경로(이메일/구글)와 달리
        // 카카오는 이 경로를 타므로 별도로 deleted_at을 확인해야 함
        let deleted_check_url = format!(
            "{}/rest/v1/users?id=eq.{}&select=deleted_at&limit=1",
            state.config.supabase_url.trim_end_matches('/'),
            existing_user_id
        );
        let deleted_resp = state
            .http_client
            .get(&deleted_check_url)
            .header("apikey", &state.config.supabase_secret_key)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .send()
            .await
            .context("탈퇴 여부 조회 실패")?;

        // 탈퇴 유저 카카오 로그인 차단 + 쿨다운 체크
        //
        // 카카오는 provider + provider_id로 매칭되므로,
        // 탈퇴 후에도 같은 카카오 계정으로 로그인 시도하면 여기 걸린다.
        // (provider_id는 30일까지 유지되다가 배치로 정리됨)
        if deleted_resp.status().is_success() {
            let rows: Vec<serde_json::Value> = deleted_resp.json().await.unwrap_or_default();
            if let Some(row) = rows.first() {
                if let Some(deleted_at_str) = row["deleted_at"].as_str() {
                    tracing::warn!(
                "탈퇴 유저 카카오 로그인 시도: user_id={}",
                existing_user_id
            );
                    // 30일 쿨다운 체크 → 미만이면 에러
                    check_login_cooldown(deleted_at_str)?;
                    // 쿨다운 지났어도 부활 X (새 가입 유도)
                    return Err(anyhow!(
                "탈퇴한 계정입니다. 새로 회원가입해 주세요."
            ));
                }
            }
        }
        // ────────────────────────────────────────────────────────

        return Ok((existing_user_id, false));
    }

    // ⭐⭐⭐ 핵심 추가 로직 (구글만) ⭐⭐⭐
    if provider == "google" {
        if let Some(real_email) = email {
            if let Some(existing_user_id) = find_public_user_by_email(state, real_email).await? {
                tracing::info!(
                    "기존 이메일 계정 발견 → 구글 연결: email={}, user_id={}",
                    real_email,
                    existing_user_id
                );

                // 기존 계정에 google provider 연결
                link_google_to_existing_user(state, &existing_user_id).await?;

                return Ok((existing_user_id, false));
            }
        }
    }

    // 2️⃣ 신규 유저 생성
    let final_email = match email {
        Some(real_email) => real_email.to_string(),
        None => format!("{}@{}.local", provider_id, provider),
    };

    let create_url = format!(
        "{}/auth/v1/admin/users",
        state.config.supabase_url.trim_end_matches('/')
    );

    let app_metadata = json!({
        "provider": provider,
        "provider_id": provider_id,
    });

    let mut user_metadata = serde_json::Map::new();
    if let Some(nick) = nickname {
        user_metadata.insert("nickname".to_string(), json!(nick));
    }

    let create_body = json!({
        "email": final_email,
        "email_confirm": true,
        "app_metadata": app_metadata,
        "user_metadata": Value::Object(user_metadata),
    });

    let create_resp = state
        .http_client
        .post(&create_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .json(&create_body)
        .send()
        .await
        .context("소셜 유저 생성 요청 실패")?;

    if !create_resp.status().is_success() {
        let err_text = create_resp.text().await.unwrap_or_default();
        return Err(anyhow!("소셜 유저 생성 실패: {}", err_text));
    }

    let created: Value = create_resp
        .json()
        .await
        .context("생성된 소셜 유저 파싱 실패")?;

    let user_id = created["id"]
        .as_str()
        .ok_or_else(|| anyhow!("생성된 유저 ID 없음"))?
        .to_string();

    tracing::info!(
        "새 소셜 유저 생성 완료: provider={}, provider_id={}, user_id={}, email={}",
        provider,
        provider_id,
        user_id,
        final_email
    );

    upsert_public_user_social_fields(state, &user_id, &final_email, provider, provider_id).await?;

    Ok((user_id, true))
}

async fn find_public_user_by_email(state: &AppState, email: &str) -> Result<Option<String>> {
    let email_encoded = urlencoding::encode(email);

    // deleted_at=is.null 조건 추가:
    // 탈퇴 유저(deleted_at NOT NULL)는 제외해서 ensure_public_user_exists에서
    // 탈퇴 계정을 google_connected 복구 대상으로 인식하지 않도록 함.
    let url = format!(
        "{}/rest/v1/users?email=eq.{}&deleted_at=is.null&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        email_encoded
    );

    let resp = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await?;

    let rows: Vec<UserRow> = resp.json().await?;

    Ok(rows.into_iter().next().map(|row| row.id.to_string()))
}

async fn link_google_to_existing_user(state: &AppState, user_id: &str) -> Result<()> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(user_id)
    );

    let resp = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&json!({
            "google_connected": true,
            "updated_at": chrono::Utc::now(),
        }))
        .send()
        .await
        .context("기존 유저에 google 연결 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("기존 유저에 google 연결 실패: {}", err_text));
    }

    Ok(())
}

// public.users에서 provider + provider_id로 기존 계정 조회
async fn find_public_user_by_provider(
    state: &AppState,
    provider: &str,
    provider_id: &str,
) -> Result<Option<String>> {
    let provider_encoded = urlencoding::encode(provider);
    let provider_id_encoded = urlencoding::encode(provider_id);

    let url = format!(
        "{}/rest/v1/users?login_provider=eq.{}&provider_id=eq.{}&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        provider_encoded,
        provider_id_encoded
    );

    let resp = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("public.users 조회 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users 조회 실패: {}", err_text));
    }

    let rows: Vec<UserRow> = resp
        .json()
        .await
        .context("public.users 조회 응답 파싱 실패")?;

    Ok(rows.into_iter().next().map(|row| row.id.to_string()))
}

// auth.users 생성 후 public.users 의 social 관련 컬럼 보정
async fn upsert_public_user_social_fields(
    state: &AppState,
    user_id: &str,
    email: &str,
    provider: &str,
    provider_id: &str,
) -> Result<()> {
    let user_id_encoded = urlencoding::encode(user_id);

    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id_encoded
    );

    // 카카오 신규 유저에게만 호출되는 함수이므로 (find_or_create_social_user 참고)
    // profile_image 기본값을 안전하게 세팅할 수 있음.
    // 기존 유저의 이미지를 덮어쓸 위험이 없음.
    let resp = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&json!({
            "email": email,
            "login_provider": provider,
            "provider_id": provider_id,
            "profile_image": "defaults/avatar.png",
        }))
        .send()
        .await
        .context("public.users social 필드 업데이트 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!(
            "public.users social 필드 업데이트 실패: {}",
            err_text
        ));
    }

    Ok(())
}

// ═══════════════════════════════════════════════════════════════
// 이메일 찾기
// ═══════════════════════════════════════════════════════════════
pub async fn find_email_by_phone(
    state: &AppState,
    phone: &str,
) -> Result<(Option<String>, String, bool)> {
    let formatted_phone = format_phone(phone);

    let url = format!(
        "{}/rest/v1/users?select=email,login_provider,google_connected&phone=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        formatted_phone
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
        .context("전화번호로 이메일 조회 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("전화번호로 이메일 조회 실패: {}", err));
    }

    let rows: Vec<Value> = resp.json().await.context("이메일 조회 응답 파싱 실패")?;

    let row = rows
        .first()
        .ok_or_else(|| anyhow!("입력한 정보와 일치하는 계정을 찾을 수 없습니다"))?;

    let provider = row
        .get("login_provider")
        .and_then(|v| v.as_str())
        .unwrap_or("unknown")
        .to_string();

    let google_connected = row
        .get("google_connected")
        .and_then(|v| v.as_bool())
        .unwrap_or(false);

    let email = row.get("email").and_then(|v| v.as_str());

    match provider.as_str() {
        "email" => {
            let masked = email.map(mask_email);
            Ok((masked, provider, google_connected))
        }
        "google" => {
            let masked = email.map(mask_email);
            Ok((masked, provider, google_connected))
        }
        "kakao" => Ok((None, provider, google_connected)),
        _ => Ok((None, provider, google_connected)),
    }
}
// ═══════════════════════════════════════════════════════════════
// 이메일 존재 여부 확인
// ═══════════════════════════════════════════════════════════════
// ─────────────────────────────────────────────────────────────
// user_id로 public.users.role_type 조회
// ─────────────────────────────────────────────────────────────
pub async fn get_user_role(state: &AppState, user_id: Uuid) -> Result<Option<String>> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=role_type&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
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
        .context("role_type 조회 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("role_type 조회 실패: {}", err));
    }

    #[derive(Deserialize)]
    struct RoleRow {
        role_type: Option<String>,
    }

    let rows: Vec<RoleRow> = resp.json().await.context("role_type 응답 파싱 실패")?;

    Ok(rows.into_iter().next().and_then(|r| r.role_type))
}

pub async fn check_nickname_available(state: &AppState, nickname: &str) -> Result<bool> {
    let encoded = urlencoding::encode(nickname.trim());

    let url = format!(
        "{}/rest/v1/users?select=id&nickname=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        encoded
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
        .context("닉네임 중복 확인 요청 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("닉네임 중복 확인 실패: {}", err));
    }

    let rows: Vec<Value> = resp
        .json()
        .await
        .context("닉네임 중복 확인 응답 파싱 실패")?;

    Ok(rows.is_empty())
}

pub async fn check_email_exists(state: &AppState, email: &str) -> Result<bool> {
    let normalized_email = email.trim().to_lowercase();
    let encoded_email = urlencoding::encode(&normalized_email);

    let url = format!(
        "{}/rest/v1/users?select=id&email=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        encoded_email
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
        .context("이메일 존재 확인 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("이메일 존재 확인 실패: {}", err));
    }

    let rows: Vec<Value> = resp.json().await.context("이메일 존재 응답 파싱 실패")?;

    Ok(!rows.is_empty())
}

pub async fn check_reset_password_email(state: &AppState, email: &str) -> Result<bool> {
    let normalized_email = email.trim().to_lowercase();
    let encoded_email = urlencoding::encode(&normalized_email);

    let url = format!(
        "{}/rest/v1/users?select=login_provider&email=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        encoded_email
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
        .context("비밀번호 재설정 가능 여부 확인 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("비밀번호 재설정 가능 여부 확인 실패: {}", err));
    }

    let rows: Vec<Value> = resp
        .json()
        .await
        .context("비밀번호 재설정 가능 여부 응답 파싱 실패")?;

    // 계정 없음
    let row = match rows.first() {
        Some(row) => row,
        None => return Ok(false),
    };

    let provider = row
        .get("login_provider")
        .and_then(|v| v.as_str())
        .unwrap_or("")
        .trim()
        .to_lowercase();

    // 이메일 로그인 계정 → OK
    if provider == "email" {
        return Ok(true);
    }

    // 소셜 로그인 계정 → 에러로 보내서 handler에서 403 처리
    Err(anyhow!(
        "소셜 로그인 계정은 비밀번호 재설정을 할 수 없습니다"
    ))
}

pub async fn check_profile_availability(
    state: &AppState,
    nickname: &str,
    phone: &str,
) -> Result<()> {
    // 닉네임 유효성 검사 (길이 2~8자 + 금칙어)
    // 실패 시 에러 메시지가 프론트 toast로 그대로 올라감
    crate::filter::validate_nickname(nickname).map_err(|msg| anyhow!(msg))?;

    let normalized_nickname = nickname.trim();
    let formatted_phone = format_phone(phone);

    let nickname_url = format!(
        "{}/rest/v1/users?select=id&nickname=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(normalized_nickname)
    );

    let nickname_resp = state
        .http_client
        .get(&nickname_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("닉네임 중복 확인 실패")?;

    if !nickname_resp.status().is_success() {
        let err = nickname_resp.text().await.unwrap_or_default();
        return Err(anyhow!("닉네임 중복 확인 실패: {}", err));
    }

    let nickname_rows: Vec<Value> = nickname_resp
        .json()
        .await
        .context("닉네임 중복 확인 응답 파싱 실패")?;

    if !nickname_rows.is_empty() {
        return Err(anyhow!("이미 사용 중인 닉네임입니다"));
    }

    let phone_url = format!(
        "{}/rest/v1/users?select=id&phone=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(&formatted_phone)
    );

    let phone_resp = state
        .http_client
        .get(&phone_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("전화번호 중복 확인 실패")?;

    if !phone_resp.status().is_success() {
        let err = phone_resp.text().await.unwrap_or_default();
        return Err(anyhow!("전화번호 중복 확인 실패: {}", err));
    }

    let phone_rows: Vec<Value> = phone_resp
        .json()
        .await
        .context("전화번호 중복 확인 응답 파싱 실패")?;

    if !phone_rows.is_empty() {
        return Err(anyhow!("이미 사용 중인 전화번호입니다"));
    }

    Ok(())
}

// 이메일 마스킹
fn mask_email(email: &str) -> String {
    let parts: Vec<&str> = email.split('@').collect();
    if parts.len() != 2 {
        return "***".to_string();
    }

    let local = parts[0];
    let domain = parts[1];

    let visible = if local.len() <= 2 { 1 } else { 2 };
    let masked_local = format!("{}***", &local[..visible]);

    format!("{}@{}", masked_local, domain)
}

// 전화번호 포맷 함수
fn format_phone(phone: &str) -> String {
    let digits: String = phone.chars().filter(|c| c.is_ascii_digit()).collect();

    if digits.len() == 11 {
        format!("{}-{}-{}", &digits[0..3], &digits[3..7], &digits[7..11])
    } else {
        digits
    }
}
