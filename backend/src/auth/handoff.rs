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
//   - 이전 버전은 exchange 시 remove()를 너무 먼저 해서
//     검증 실패여도 token이 날아갈 수 있었음
//   - 지금은 get()으로 먼저 확인하고
//     검증이 모두 끝난 뒤 remove()로 1회용 처리함

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

// ─────────────────────────────────────────────────────────────
// handoff token 발급
//
// 호출 시점: 웹에서 "게임 시작" 버튼 클릭
// 전제: JWT 미들웨어를 통과한 상태 (user_id 확인됨)
//
// 동작:
// 1) 64자리 랜덤 문자열 생성 (handoff token 원문)
// 2) SHA-256 해시 계산
// 3) HandoffEntry를 handoff_store에 저장
//    - key: token_hash
//    - 만료: 30초
// 4) 원문 token을 프론트에 반환
//    (프론트가 postMessage로 유니티에 전달)
//
// 반환: handoff token 원문 (1회만 볼 수 있음)
// ─────────────────────────────────────────────────────────────
pub fn create_handoff_token(state: &AppState, user_id: Uuid, target_service: &str) -> String {
    // 64자리 랜덤 문자열 생성
    // nonce(32자리)보다 길게 잡은 이유:
    // handoff는 access+refresh 교환이 가능하므로 더 높은 엔트로피 필요
    let token: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(64)
        .map(char::from)
        .collect();

    let token_hash = hash_handoff_token(&token);

    // 메모리에 저장
    // 30초 후 만료 → main.rs의 백그라운드 태스크가 정리
    state.handoff_store.insert(
        token_hash,
        HandoffEntry {
            user_id,
            target_service: target_service.to_string(),
            expires_at: SystemTime::now() + Duration::from_secs(30),
        },
    );

    tracing::info!(
        "handoff token 발급: user_id={}, target={}",
        user_id,
        target_service
    );

    // 원문 반환 (해시가 아님!)
    // 프론트가 이 값을 postMessage로 유니티에 전달
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
// 2) handoff_store에서 해시로 조회
// 3) 검증:
//    - 존재하는지
//    - 만료되지 않았는지
//    - target_service가 "unity"인지
// 4) 검증 성공 → handoff 즉시 삭제 (1회용)
// 5) 해당 user_id로 유니티용 access+refresh 발급
//    - client_type = "app" (유니티는 쿠키 안 쓰므로 body 방식)
//
// 반환: LoginIssueResult (access_token, refresh_token, is_new_user)
// ─────────────────────────────────────────────────────────────
pub async fn exchange_handoff_token(state: &AppState, token: &str) -> Result<LoginIssueResult> {
    let token_hash = hash_handoff_token(token);

    // ── 1) handoff_store에서 조회 ────────────────────────────
    // 주의:
    // 예전에는 remove()를 먼저 했는데,
    // 그러면 검증 실패 시에도 token이 날아가서 불필요한 소모가 생김.
    // 그래서 먼저 get()으로 확인하고,
    // 검증이 다 끝난 뒤 remove()로 1회용 처리함.
    let entry_ref = state.handoff_store.get(&token_hash).ok_or_else(|| {
        tracing::warn!("handoff token 없음 또는 이미 사용됨");
        anyhow!("유효하지 않은 handoff token입니다.")
    })?;

    let entry = entry_ref.clone();
    drop(entry_ref);

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

    // ── 4) 검증 성공 후 즉시 삭제 (1회용 보장) ──────────────
    // 여기서 remove()를 해야
    // 정상 검증이 끝난 token만 1회용으로 소모된다.
    state.handoff_store.remove(&token_hash);

    tracing::info!(
        "handoff token 교환 성공: user_id={}, target={}",
        entry.user_id,
        entry.target_service
    );

    // ── 5) 유니티용 access+refresh 발급 ─────────────────────
    // client_type = "app"
    //   → 유니티는 브라우저 쿠키를 쓸 수 없으므로
    //     refresh token도 body로 내려줘야 함
    //   → 기존 앱(Android/Kotlin) 방식과 동일
    //
    // is_new_user = false
    //   → handoff는 이미 로그인된 유저가 쓰는 거라 항상 false
    let result = issue_login_tokens(
        state,
        entry.user_id,
        "app", // 유니티 = 앱 방식 (body로 refresh 전달)
        false, // 이미 로그인된 유저
    )
    .await?;

    Ok(result)
}
