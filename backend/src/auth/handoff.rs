// src/auth/handoff.rs
// ─────────────────────────────────────────────────────────────
// 유니티 exe 진입용 handoff token 발급/교환 로직
//
// handoff란?
//   웹에서 유니티 exe로 로그인 상태를 안전하게 넘기기 위한
//   1회용 교환권(one-time exchange token).
//
// 왜 필요한가?
//   - 웹의 access token은 JS 메모리에만 있음
//   - exe는 웹 탭 메모리를 공유하지 않음
//   - 웹의 refresh token은 HttpOnly 쿠키라 JS가 읽을 수 없음
//   - 따라서 원본 토큰(access/refresh)을 직접 넘기면
//     웹에서 유지하던 보안 모델이 깨짐
//
// 해결:
//   - 웹이 백엔드에서 짧은 수명(30초)의 handoff token을 발급받음
//   - 웹이 exe 실행용 프로토콜/런처에 handoff token을 실어 전달
//   - 유니티 exe가 handoff token으로 자기만의 access+refresh를 교환
//   - 교환 성공 시 handoff token 즉시 삭제 (1회용)
//
// 저장 방식:
//   - DB가 아닌 메모리(DashMap)에 저장
//   - 30초 TTL + 1회용이라 DB까지 갈 필요 없음
//   - nonce_store와 같은 철학
//
// 보안:
//   - 원문 token은 저장하지 않고 SHA-256 해시만 저장
//   - 검증 시 들어온 token을 해시해서 비교
//   - refresh_store와 같은 패턴
//
// 중요 수정 포인트:
//   - 경쟁 상황에서도 1회용이 보장되도록
//     exchange 시 remove()로 먼저 선점한다.
//   - 선점에 성공한 요청만 검증/토큰 발급까지 진행하고
//     나머지는 "이미 사용됨"으로 실패한다.

use anyhow::{Result, anyhow};
use rand::Rng;
use sha2::{Digest, Sha256};
use std::time::{Duration, SystemTime};
use uuid::Uuid;

use crate::auth::service::{LoginIssueResult, issue_login_tokens};
use crate::state::{AppState, HandoffEntry};

// ─────────────────────────────────────────────────────────────
// handoff token 원문 → SHA-256 해시
//
// 메모리에 원문을 저장하지 않기 위해 해시만 저장.
// 검증 시 들어온 token을 이 함수로 해시해서 저장된 값과 비교.
// ─────────────────────────────────────────────────────────────
pub fn hash_handoff_token(token: &str) -> String {
    let mut hasher = Sha256::new();
    hasher.update(token.as_bytes());
    hex::encode(hasher.finalize())
}

/// handoff token 발급.
///
/// 현재는 Steam 실행 인자 전달이 불안정하므로,
/// 웹에서 긴 토큰을 자동 전달하는 방식이 아니라
/// 사용자가 Unity에 직접 입력할 수 있는 8자리 게임 로그인 코드로 사용한다.
///
/// 정책:
/// - 8자리 영문+숫자 코드
/// - 60초 TTL
/// - 원문은 저장하지 않고 SHA-256 hash만 저장
/// - exchange 시 remove()로 먼저 선점해서 1회용 보장
///
/// 주의:
/// - 함수명은 기존 호환을 위해 create_handoff_token 그대로 둔다.
/// - 프론트/유니티/백엔드 DTO도 기존 handoff_token 필드를 그대로 쓴다.
/// - 화면 문구만 "게임 로그인 코드"로 바꿔 보여주면 된다.
pub fn create_handoff_token(state: &AppState, user_id: Uuid, target_service: &str) -> String {
    // 사용자가 직접 입력해야 하므로 64자 대신 8자리로 줄인다.
    //
    // Alphanumeric은 대소문자가 섞일 수 있다.
    // Unity 입력 UX를 더 쉽게 하려면 나중에 대문자/숫자만 쓰는 방식으로 바꿀 수 있다.
    let token: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(8)
        .map(char::from)
        .collect();

    let token_hash = hash_handoff_token(&token);

    // 60초 후 만료.
    //
    // 기존 30초는 자동 실행 handoff 기준이었다.
    // 지금은 사용자가 웹에서 코드를 보고 Unity에 직접 입력해야 하므로 60초로 늘린다.
    state.handoff_store.insert(
        token_hash,
        HandoffEntry {
            user_id,
            target_service: target_service.to_string(),
            expires_at: SystemTime::now() + Duration::from_secs(60),
        },
    );

    tracing::info!(
        "game login code 발급: user_id={}, target={}, ttl_secs=60",
        user_id,
        target_service
    );

    // 원문 반환.
    // 프론트는 이 값을 화면에 크게 보여주고,
    // Unity는 사용자가 입력한 값을 /auth/handoff/exchange로 보낸다.
    token
}

// ─────────────────────────────────────────────────────────────
// handoff token 교환
//
// 호출 시점: 유니티 exe가 실행 시 전달받은 handoff token으로
//           /auth/handoff/exchange를 호출
//
// 동작:
// 1) 들어온 token을 해시
// 2) handoff_store에서 remove()로 선점
// 3) 검증:
//    - 존재하는지
//    - 만료되지 않았는지
//    - target_service가 "unity"인지
// 4) 선점 성공한 요청만 access+refresh 발급
// 5) 해당 user_id로 유니티용 access+refresh 발급
//    - client_type = "unity" (유니티는 쿠키 안 쓰므로 body 방식)
//
// 반환: LoginIssueResult (access_token, refresh_token, is_new_user)
// ─────────────────────────────────────────────────────────────
pub async fn exchange_handoff_token(state: &AppState, token: &str) -> Result<LoginIssueResult> {
    let token_hash = hash_handoff_token(token);

    // ── 1) handoff_store에서 즉시 remove()로 선점 ────────────
    // handoff는 경쟁 상황에서도 1회용이어야 하므로
    // "조회 후 나중에 삭제"가 아니라 "먼저 가져간 요청만 성공"하게 만든다.
    //
    // 주의:
    // 만료/target_service 검증 전에 remove()가 일어나므로
    // 잘못된 handoff도 재사용은 불가능하다.
    // handoff는 30초 TTL의 1회용 토큰이므로 이 동작이 더 안전하다.
    let (_, entry) = state.handoff_store.remove(&token_hash).ok_or_else(|| {
        tracing::warn!("handoff token 없음 또는 이미 사용됨");
        anyhow!("유효하지 않은 handoff token입니다.")
    })?;

    // ── 2) 만료 체크 ────────────────────────────────────────
    if SystemTime::now() > entry.expires_at {
        tracing::warn!("handoff token 만료: user_id={}", entry.user_id);
        return Err(anyhow!("handoff token이 만료되었습니다."));
    }

    // ── 3) 대상 서비스 검증 ─────────────────────────────────
    // 보안상 target_service를 클라이언트가 보내게 두지 않고
    // 서버에 저장된 값만 믿는다.
    if entry.target_service != "unity" {
        tracing::warn!(
            "handoff target_service 불일치: stored={}",
            entry.target_service
        );
        return Err(anyhow!("handoff token의 대상 서비스가 일치하지 않습니다."));
    }

    // ── 4) 선점 성공한 요청만 유니티용 access+refresh 발급 ────
    tracing::info!(
        "handoff token 교환 성공: user_id={}, target={}",
        entry.user_id,
        entry.target_service
    );

    // ── 5) 유니티용 access+refresh 발급 ─────────────────────
    // client_type = "unity"
    //   → 유니티는 브라우저 쿠키를 쓸 수 없으므로
    //     refresh token도 body로 내려줘야 함
    //   → 모바일 app 세션과 분리해서 Unity 종료/로그아웃 시 unity 세션만 끊을 수 있게 함
    //
    // is_new_user = false
    //   → handoff는 이미 로그인된 유저가 쓰는 거라 항상 false
    let result = issue_login_tokens(
        state,
        entry.user_id,
        "unity",
        false, // 이미 로그인된 유저
    )
    .await?;

    Ok(result)
}
