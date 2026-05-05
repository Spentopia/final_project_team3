// src/auth/webview.rs
// ─────────────────────────────────────────────────────────────
// 안드로이드 웹뷰 진입용 일회용 교환 토큰 발급/교환 로직
//
// webview란?
//   안드로이드 앱이 NFT 마켓 같은 페이지를 웹뷰로 띄울 때
//   앱의 로그인 상태를 웹으로 안전하게 넘기기 위한 1회용 교환권.
//
// 왜 필요한가?
//   - 앱은 자체 access/refresh 토큰을 가지고 있음
//   - 웹뷰 안의 React는 webview의 메모리/쿠키를 따로 가짐
//   - 앱 토큰을 URL 쿼리로 그대로 넘기면 Referer/로그 노출
//   - 앱 토큰을 직접 쿠키로 심으면 앱-웹 토큰 정책 충돌
//
// 해결:
//   1) 앱이 자기 access token으로 /auth/webview/issue 호출
//      → 30초짜리 일회용 webview token 받음
//   2) 앱이 webView.loadUrl(".../auth/webview/callback?token=xxx")
//   3) 백엔드 callback이 token 검증 + 즉시 삭제
//      → 웹용 access token 발급 (URL fragment로 전달)
//      → 웹용 refresh token 발급 (httpOnly 쿠키로 셋팅)
//      → 프론트엔드 페이지로 302 리다이렉트
//   4) 웹뷰 안 React가 fragment에서 access token 추출 → 메모리 저장
//
// 저장 방식:
//   - DB가 아닌 메모리(DashMap)에 저장
//   - 30초 TTL + 1회용이라 DB까지 갈 필요 없음
//   - handoff_store와 동일한 철학
//
// 보안:
//   - 원문 token은 저장하지 않고 SHA-256 해시만 저장
//   - 검증 시 들어온 token을 해시해서 비교
//   - exchange 시 remove()로 먼저 선점 → 1회용 보장 (handoff와 동일)
//
// handoff와의 차이:
//   - handoff는 access+refresh body 응답 (유니티 exe용)
//   - webview는 쿠키 + URL fragment 리다이렉트 (브라우저용)

use anyhow::{Result, anyhow};
use rand::Rng;
use sha2::{Digest, Sha256};
use std::time::{Duration, SystemTime};
use uuid::Uuid;

use crate::state::{AppState, WebviewEntry};

// ─────────────────────────────────────────────────────────────
// webview token 원문 → SHA-256 해시
// 메모리에 원문을 저장하지 않기 위해 해시만 저장.
// ─────────────────────────────────────────────────────────────
pub fn hash_webview_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}

// ─────────────────────────────────────────────────────────────
// webview token 발급
//
// 호출 시점: 앱이 NFT 마켓 웹뷰 띄우기 직전
// 전제: jwt_middleware 통과 (앱의 access token 검증됨)
//
// 동작:
// 1) 64자리 랜덤 문자열 생성 (webview token 원문)
// 2) SHA-256 해시 계산
// 3) WebviewEntry를 webview_store에 저장
//    - key: token_hash
//    - 만료: 30초
// 4) 원문 token을 앱에 반환
//    (앱이 webView.loadUrl 쿼리 파라미터로 사용)
//
// 반환: webview token 원문 (1회만 볼 수 있음)
// ─────────────────────────────────────────────────────────────
pub fn create_webview_token(
    state: &AppState,
    user_id: Uuid,
    redirect_path: &str,
) -> String {
    // 64자리 랜덤 문자열
    // handoff와 동일한 길이 (높은 엔트로피)
    let token: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(64)
        .map(char::from)
        .collect();

    let token_hash = hash_webview_token(&token);

    // 메모리에 저장
    // 30초 후 만료 → main.rs의 백그라운드 태스크가 정리
    state.webview_store.insert(
        token_hash,
        WebviewEntry {
            user_id,
            redirect_path: redirect_path.to_string(),
            expires_at: SystemTime::now() + Duration::from_secs(30),
        },
    );

    tracing::info!(
        "webview token 발급: user_id={}, redirect_path={}",
        user_id,
        redirect_path
    );

    // 원문 반환 (해시가 아님)
    token
}

// ─────────────────────────────────────────────────────────────
// webview token 검증 + 소비 (1회용)
//
// 호출 시점: 웹뷰가 /auth/webview/callback?token=xxx로 진입
//
// 동작:
// 1) 들어온 token을 해시
// 2) webview_store에서 remove()로 선점 (handoff와 동일 패턴)
// 3) 만료 체크
// 4) 선점 성공한 요청만 user_id + redirect_path 반환
//
// 반환:
// - Ok((user_id, redirect_path)): 정상
// - Err(_): 토큰 없음, 만료, 또는 이미 사용됨
// ─────────────────────────────────────────────────────────────
pub fn consume_webview_token(
    state: &AppState,
    token: &str,
) -> Result<(Uuid, String)> {
    let token_hash = hash_webview_token(token);

    // ── 1) webview_store에서 즉시 remove()로 선점 ───────────
    // handoff와 동일하게 "조회 후 삭제"가 아니라
    // "먼저 가져간 요청만 성공"하는 방식으로 1회용 보장.
    // 만료 검증 전에 remove가 일어나지만,
    // 30초 TTL이라 만료된 토큰이 재사용될 위험은 없음.
    let (_, entry) = state
        .webview_store
        .remove(&token_hash)
        .ok_or_else(|| {
            tracing::warn!("webview token 없음 또는 이미 사용됨");
            anyhow!("유효하지 않은 webview token입니다.")
        })?;

    // ── 2) 만료 체크 ────────────────────────────────────────
    if SystemTime::now() > entry.expires_at {
        tracing::warn!("webview token 만료: user_id={}", entry.user_id);
        return Err(anyhow!("webview token이 만료되었습니다."));
    }

    tracing::info!(
        "webview token 교환 성공: user_id={}, redirect_path={}",
        entry.user_id,
        entry.redirect_path
    );

    Ok((entry.user_id, entry.redirect_path))
}

// ─────────────────────────────────────────────────────────────
// 만료된 엔트리 청소
//
// main.rs의 백그라운드 태스크가 주기적으로 호출.
// 1회용으로 소비되지 않은 엔트리(앱이 token만 받고 웹뷰 진입 안 한 경우 등)가
// 메모리에 남는 걸 방지.
// ─────────────────────────────────────────────────────────────
pub fn cleanup_expired_webview_tokens(state: &AppState) {
    let now = SystemTime::now();
    state.webview_store.retain(|_, entry| entry.expires_at > now);
}

// ─────────────────────────────────────────────────────────────
// 리다이렉트 경로 검증 (Open Redirect 방지)
//
// 허용:    /marketplace, /marketplace/listing/abc123
// 차단:    //evil.com (프로토콜 상대 URL)
// 차단:    https://evil.com (절대 URL)
// 차단:    빈 문자열, "."로 시작
// ─────────────────────────────────────────────────────────────
pub fn sanitize_redirect_path(input: Option<&str>) -> String {
    let default = "/marketplace".to_string();

    let Some(path) = input else { return default };
    let trimmed = path.trim();

    if trimmed.is_empty() {
        return default;
    }

    // 반드시 / 로 시작하면서 // 로는 시작하지 않아야 함
    if !trimmed.starts_with('/') || trimmed.starts_with("//") {
        tracing::warn!("webview redirect_path 차단됨: {}", trimmed);
        return default;
    }

    trimmed.to_string()
}
