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

// anyhow: Rust의 에러 처리 라이브러리
// anyhow!("메세지") → 에러 생성
// Context → .context("설명") 으로 에러에 설명 추가
// Result → 성공이면 Ok(값), 실패면 Err(에러)
use anyhow::{Context, Result, anyhow};
use std::time::{Duration, SystemTime};

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


use serde_json::{json, Value};
use uuid::Uuid;

use crate::auth::app_jwt::{
    generate_token_pair, verify_app_refresh_token,
};
use crate::auth::refresh_store::{
    create_refresh_session,
    revoke_refresh_session,
    revoke_refresh_session_as_reused,
    verify_refresh_session,
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
    id: uuid::Uuid,
}

// ─────────────────────────────────────────────────────────────
// 공통 토큰 발급 + refresh session 저장
//
// 모든 로그인 방식(Supabase, 카카오, 지갑)이
// 최종적으로 이 함수로 모인다.
// ─────────────────────────────────────────────────────────────
pub async fn issue_login_tokens(
    state: &AppState,
    user_id: Uuid,
    client_type: &str,
    is_new_user: bool,
) -> Result<LoginIssueResult> {
    // refresh 세션의 고유 ID
    // 이 값이 refresh JWT 안의 sid로 들어간다.
    let session_id = Uuid::new_v4();

    // access / refresh JWT 생성
    let pair = generate_token_pair(
        &state.config.app_jwt_secret,
        &user_id,
        &session_id,
    )?;

    // refresh 세션을 DB에 저장
    // 이 row가 있어야 나중에 revoke / rotation / 로그아웃이 가능하다.
    create_refresh_session(
        state,
        session_id,
        user_id,
        client_type,
        &pair.refresh_token,
    )
        .await?;

    Ok(LoginIssueResult {
        access_token: pair.access_token,
        refresh_token: pair.refresh_token,
        is_new_user,
    })
}

/// refresh rotation
///
/// 흐름:
/// 1) refresh JWT 자체 검증
/// 2) sid로 DB refresh_sessions 조회
/// 3) DB 세션 검증
///    - revoked 아님
///    - expires_at 안 지남
///    - replaced_by_session_id 없음
///    - hash 일치
/// 4) 새 access / refresh 발급
/// 5) 새 refresh session 저장
/// 6) 기존 refresh session revoke + replaced_by_session_id 기록
///
/// 보안 포인트:
/// - 이미 rotation된 refresh token이 다시 들어오면
///   verify_refresh_session() 단계에서 reuse 감지로 차단됨
pub async fn rotate_refresh_token(
    state: &AppState,
    refresh_token: &str,
    client_type: &str,
) -> Result<RefreshIssueResult> {
    // 1) refresh JWT 자체 검증
    //
    // 여기서 JWT 서명 위조 여부와 JWT exp를 1차로 체크한다.
    let claims = verify_app_refresh_token(
        &state.config.app_jwt_secret,
        refresh_token,
    )?;

    let session_id = Uuid::parse_str(&claims.sid)
        .context("refresh sid UUID 파싱 실패")?;

    let user_id = Uuid::parse_str(&claims.sub)
        .context("refresh sub UUID 파싱 실패")?;

    // 2) DB에 저장된 refresh session 검증
    //
    // 여기서 추가로:
    // - revoked
    // - expires_at
    // - replaced_by_session_id (reuse 감지)
    // - token_hash
    // 를 체크한다.
    //
    // 즉 JWT만 믿지 않고 DB 상태까지 본다.
    let session = match verify_refresh_session(state, session_id, refresh_token).await {
        Ok(session) => session,
        Err(e) => {
            let msg = e.to_string();

            // reuse 감지 시에는 해당 세션을 한 번 더 명시적으로 revoke 해둔다.
            // (이미 replaced된 세션이라면 사실상 죽어있지만,
            //  보안상 "재사용 시도된 세션"임을 명확히 남기는 용도)
            if msg.contains("reuse") {
                let _ = revoke_refresh_session_as_reused(state, session_id).await;
            }

            return Err(e);
        }
    };

    // 3) client_type 일치 여부 확인
    //
    // 웹에서 발급된 refresh token을 앱이 쓰거나,
    // 앱에서 발급된 refresh token을 웹이 쓰는 걸 막는다.
    if session.client_type != client_type {
        return Err(anyhow!("refresh client_type 불일치"));
    }

    // 4) 새 refresh 세션 ID 생성
    let new_session_id = Uuid::new_v4();

    // 5) 새 access / refresh JWT 발급
    let pair = generate_token_pair(
        &state.config.app_jwt_secret,
        &user_id,
        &new_session_id,
    )?;

    // 6) 새 refresh session 저장
    create_refresh_session(
        state,
        new_session_id,
        user_id,
        client_type,
        &pair.refresh_token,
    )
        .await?;

    // 7) 기존 refresh session revoke
    //
    // replaced_by_session_id에 새 session_id를 기록해 둔다.
    // 그러면 나중에 옛 refresh token이 다시 들어왔을 때
    // "이미 교체된 토큰"으로 판단 가능하다.
    revoke_refresh_session(state, session_id, Some(new_session_id)).await?;

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
        return Err(anyhow!("nonce가 만료됨. /auth/wallet/nonce를 다시 호출하세요."));
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

    // 최종 토큰 발급 + refresh session 저장
    // 지갑 로그인은 기존 유저만 가능하므로 is_new_user = false
    issue_login_tokens(state, user_id, client_type, false).await
}

// verify_solana_signature - Solana ed25519 서명 검증
//
// ■ 배경 지식
//  Solana 지갑 주소 = ed25519 공개키를 Base58로 인코딩한 문자열
//  서명 = 개인키로 nonce를 암호화한 64바이트 → Base58 인코딩
//
// ■ 검증 원리
//  공개키(지갑 주소)로 서명을 "열어봤을 떄" nonce 원문이 나오면 → 검증 성공
//  즉, 이 서명을 만들 수 있는 건 이 지갑의 개인키 소유자뿐임을 수학적으로 증명
//
// ■ Base58이란?
//  Bitcoin/Solana에서 쓰는 인코딩 방식.
//  바이트 배열을 사람이 읽을 수 있는 문자열로 변환.
//  헷갈리는 문자(0, O, l, I)를 제외해서 가독성이 좋음.
fn verify_solana_signature(
    // "7xKXtg2CW87d..." 형태의 Solana 지갑 주소 (Base58 인코딩된 공개키)
    wallet_address: &str,
    // 서버가 1단계에서 발급한 nonce 원문
    nonce: &str,
    // 앱이 지갑으로 서명한 값 (Base58 인코딩된 64바이트)
    signature: &str,
) -> Result<()> {
    tracing::warn!(
        "[서명검증] 시작: wallet={}, nonce={:?}({}bytes), sig_chars={}",
        wallet_address, nonce, nonce.len(), signature.len()
    );

    // Base58 문자열("7xKXtg2...") → 바이트 배열([0x7a, 0x2b, ...])로 변환
    let pubkey_bytes = bs58::decode(wallet_address)
        .into_vec()
        .context("지갑 주소 Base58 디코딩 실패. 유효한 Solana 주소인지 확인 필요.")?;

    tracing::warn!("[서명검증] 공개키 바이트 수: {}", pubkey_bytes.len());

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

    tracing::warn!("[서명검증] 서명 바이트 수: {}", sig_bytes.len());

    // ed25519 서명은 정확히 64바이트여야 함
    let sig_array: [u8; 64] = sig_bytes
        .try_into()
        .map_err(|_| anyhow!("서명이 64바이트가 아님. 잘못된 서명."))?;

    let sig = Signature::from_bytes(&sig_array);

    // 핵심: 공개키(verifying_key)로 서명(sig)을 검증
    // nonce.as_bytes(): nonce 원문을 바이트로 변환해서 검증 대상으로 사용
    // 성공 → 이 서명은 이 공개키에 대응하는 개인키로 만든 것이 맞음
    // 실패 → 서명이 위조됐거나 다른 지갑으로 서명한 것
    let result = verifying_key.verify(nonce.as_bytes(), &sig);
    if let Err(ref e) = result {
        tracing::warn!(
            "[서명검증] 실패! nonce_bytes={:?}, 에러={:?}",
            nonce.as_bytes(), e
        );
    } else {
        tracing::warn!("[서명검증] 성공");
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
// wallet/service.rs에서도 중복 연동 체크 시 재사용하므로 pub으로 선언
pub async fn find_user_by_wallet(state: &AppState, wallet_address: &str) -> Result<uuid::Uuid> {
    // wallet_address=eq.{주소} → WHERE wallet_address = '{주소}'
    // select=id → id 컬럼만 가져옴 (불필요한 컬럼 제외)
    // trim_end_matches('/'): URL 끝 슬래시 제거 (중복 방지)
    let url = format!(
        "{}/rest/v1/users?wallet_address=eq.{}&select=id",
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
        anyhow!("해당 지갑 주소로 연동된 계정 없음. 앱에서 먼저 지갑 연동을 해주세요.")
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

    let resp = state.http_client
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

    let user_data: Value = resp.json().await
        .context("Supabase 유저 응답 파싱 실패")?;

    let user_id = user_data["id"]
        .as_str()
        .ok_or_else(|| anyhow!("Supabase 유저 ID 없음"))?;

    let email = user_data["email"].as_str();

    let provider = user_data["app_metadata"]["provider"]
        .as_str()
        .unwrap_or("email");

    let provider_id = user_data["user_metadata"]["provider_id"]
        .as_str()
        .or_else(|| {
            user_data["identities"]
                .as_array()
                .and_then(|arr| arr.first())
                .and_then(|x| x["id"].as_str())
        });

    // auth.users는 있어도 public.users / user_settings / streaks가 비어 있을 수 있어
    ensure_public_user_exists(
        state,
        user_id,
        email,
        provider,
        provider_id,
    )
        .await?;

    let user_uuid = Uuid::parse_str(user_id)
        .context("Supabase user_id UUID 파싱 실패")?;

    issue_login_tokens(state, user_uuid, client_type, false).await
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
) -> Result<()> {
    let users_url = format!(
        "{}/rest/v1/users",
        state.config.supabase_url.trim_end_matches('/')
    );

    // ── 1단계: 신규 유저 INSERT (이미 존재하면 무시) ──────────────
    //
    // resolution=ignore-duplicates:
    //   id 충돌(= 이미 가입된 유저)이면 아무것도 하지 않음.
    //   신규 유저면 전체 행을 새로 삽입함.
    //
    // profile_image를 여기서만 "defaults/avatar.png"로 세팅하는 이유:
    //   - merge-duplicates를 쓰면 기존 유저가 업로드한 프로필 이미지를
    //     로그인할 때마다 기본값으로 덮어쓰는 문제가 생김.
    //   - ignore-duplicates는 INSERT에만 적용되므로,
    //     신규 유저는 기본 이미지를 받고, 기존 유저는 영향받지 않음.
    //
    // created_at도 여기서만 세팅:
    //   - merge-duplicates였을 때는 매 로그인마다 created_at이 갱신되는
    //     버그가 있었음. ignore-duplicates로 바꾸면서 함께 해결됨.
    // ────────────────────────────────────────────────────────────
    let insert_payload = json!([{
        "id": user_id,
        "email": email,
        "login_provider": provider,
        "provider_id": provider_id,
        "profile_image": "defaults/avatar.png",
        "created_at": chrono::Utc::now(),
        "updated_at": chrono::Utc::now(),
        "is_active": true
    }]);

    let insert_resp = state.http_client
        .post(&users_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
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

    // ── 2단계: 기존/신규 모두 활성 상태 갱신 ─────────────────────
    //
    // INSERT에서 ignore-duplicates를 쓰면 기존 유저의 is_active / updated_at이
    // 갱신되지 않으므로, PATCH로 별도 업데이트.
    //
    // profile_image / created_at / email 등 초기값 필드는 건드리지 않음.
    // ────────────────────────────────────────────────────────────
    let patch_url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(user_id)
    );

    let patch_resp = state.http_client
        .patch(&patch_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .header("Prefer", "return=minimal")
        .json(&json!({
            "is_active": true,
            "updated_at": chrono::Utc::now()
        }))
        .send()
        .await
        .context("public.users 활성 상태 갱신 실패")?;

    if !patch_resp.status().is_success() {
        let err = patch_resp.text().await.unwrap_or_default();
        return Err(anyhow!("public.users 활성 상태 갱신 실패: {}", err));
    }

    let settings_url = format!(
        "{}/rest/v1/user_settings",
        state.config.supabase_url.trim_end_matches('/')
    );

    let _ = state.http_client
        .post(&settings_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=merge-duplicates")
        .json(&json!([{
            "user_id": user_id
        }]))
        .send()
        .await;

    let streaks_url = format!(
        "{}/rest/v1/streaks",
        state.config.supabase_url.trim_end_matches('/')
    );

    let _ = state.http_client
        .post(&streaks_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .header("Prefer", "resolution=merge-duplicates")
        .json(&json!([{
            "user_id": user_id,
            "current_streak": 0,
            "longest_streak": 0
        }]))
        .send()
        .await;

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

    let kakao_client_id = std::env::var("KAKAO_REST_API_KEY")
        .context("KAKAO_REST_API_KEY 환경변수 없음")?;
    let kakao_client_secret = std::env::var("KAKAO_CLIENT_SECRET")
        .context("KAKAO_CLIENT_SECRET 환경변수 없음")?;
    let redirect_uri = std::env::var("KAKAO_REDIRECT_URI")
        .context("KAKAO_REDIRECT_URI 환경변수 없음")?;

    let token_resp = state.http_client
        .post(token_url)
        .form(&[
            ("grant_type", "authorization_code"),
            ("client_id", &kakao_client_id),
            ("client_secret", &kakao_client_secret),
            ("redirect_uri", &redirect_uri),
            ("code", code),
        ])
        .send()
        .await
        .context("카카오 토큰 요청 실패")?;

    if !token_resp.status().is_success() {
        let err = token_resp.text().await.unwrap_or_default();
        tracing::error!("카카오 토큰 교환 실패: {}", err);
        return Err(anyhow!("카카오 토큰 교환 실패: {}", err));
    }

    let token_data: Value = token_resp.json().await
        .context("카카오 토큰 응답 파싱 실패")?;

    let kakao_access_token = token_data["access_token"].as_str()
        .ok_or_else(|| anyhow!("카카오 access_token 없음"))?;

    let user_resp = state.http_client
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

    let user_data: Value = user_resp.json().await
        .context("카카오 유저 정보 파싱 실패")?;

    let kakao_id = user_data["id"].as_i64()
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

    let (user_id, is_new_user) = find_or_create_social_user(
        state,
        provider,
        &provider_id,
        email,
        nickname,
    ).await?;

    let user_uuid = Uuid::parse_str(&user_id)
        .context("카카오 user_id UUID 파싱 실패")?;

    issue_login_tokens(state, user_uuid, client_type, is_new_user).await
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
    if let Some(existing_user_id) =
        find_public_user_by_provider(state, provider, provider_id).await?
    {
        tracing::info!(
            "기존 소셜 유저 발견: provider={}, provider_id={}, user_id={}",
            provider,
            provider_id,
            existing_user_id
        );
        return Ok((existing_user_id, false));
    }

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

    let create_resp = state.http_client
        .post(&create_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
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

    upsert_public_user_social_fields(
        state,
        &user_id,
        &final_email,
        provider,
        provider_id,
    ).await?;

    Ok((user_id, true))
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

    let resp = state.http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
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
    let resp = state.http_client
        .patch(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
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
        return Err(anyhow!("public.users social 필드 업데이트 실패: {}", err_text));
    }

    Ok(())
}

// ═══════════════════════════════════════════════════════════════
// 이메일 찾기
// ═══════════════════════════════════════════════════════════════
pub async fn find_email_by_phone(
    state: &AppState,
    phone: &str,
) -> Result<String> {
    // 프론트에서 "01012345678" 또는 "010-1234-5678" 어떤 형식으로 와도
    // DB 저장 형식인 "010-1234-5678"로 맞춰서 조회
    let formatted_phone = format_phone(phone);

    tracing::info!("이메일 찾기 요청 phone(raw) = {}", phone);
    tracing::info!("이메일 찾기 요청 phone(formatted) = {}", formatted_phone);

    let url = format!(
        "{}/rest/v1/users?select=email&phone=eq.{}",
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

    let rows: Vec<Value> = resp
        .json()
        .await
        .context("이메일 조회 응답 파싱 실패")?;

    let email = rows
        .first()
        .and_then(|row| row.get("email"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| anyhow!("해당 전화번호로 가입된 계정이 없음"))?;

    Ok(mask_email(email))
}

// ═══════════════════════════════════════════════════════════════
// 이메일 존재 여부 확인
// ═══════════════════════════════════════════════════════════════
pub async fn check_email_exists(
    state: &AppState,
    email: &str,
) -> Result<bool> {
    let normalized_email = email.trim().to_lowercase();
    let encoded_email = urlencoding::encode(&normalized_email);

    let url = format!(
        "{}/rest/v1/users?select=id&email=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        encoded_email
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .context("이메일 존재 확인 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("이메일 존재 확인 실패: {}", err));
    }

    let rows: Vec<Value> = resp.json().await
        .context("이메일 존재 응답 파싱 실패")?;

    Ok(!rows.is_empty())
}

pub async fn can_reset_password(
    state: &AppState,
    email: &str,
) -> Result<bool> {
    let normalized_email = email.trim().to_lowercase();
    let encoded_email = urlencoding::encode(&normalized_email);

    let url = format!(
        "{}/rest/v1/users?select=login_provider&email=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        encoded_email
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .context("비밀번호 재설정 가능 여부 확인 실패")?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        return Err(anyhow!("비밀번호 재설정 가능 여부 확인 실패: {}", err));
    }

    let rows: Vec<Value> = resp.json().await
        .context("비밀번호 재설정 가능 여부 응답 파싱 실패")?;

    let provider = rows
        .first()
        .and_then(|row| row.get("login_provider"))
        .and_then(|v| v.as_str())
        .ok_or_else(|| anyhow!("해당 이메일로 가입된 계정이 없습니다"))?;

    Ok(provider == "email")
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
