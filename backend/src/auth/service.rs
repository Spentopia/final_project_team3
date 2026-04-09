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

// anyhow: Rust의 에러 처리 라이브러리
// anyhow!("메세지") → 에러 생성
// Context → .context("설명") 으로 에러에 설명 추가
// Result → 성공이면 Ok(값), 실패면 Err(에러)
use anyhow::{Context, Result, anyhow};

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
use crate::state::AppState;

// 로그인 성공 응답 구조체 (access_token, refresh_token, is_new_user)
// super = 현재 모듈(auth)의 상위 경로
use super::dto::LoginResponse;

// SupabaseTokenResponse
// Supabase Admin API로 JWT를 발급하면 이 형태로 응답이 옴.
// generate_supabase_token() 함수에서 JSON을 이 구조체로 파싱함.
//
// access_token → 앱이 이후 모든 API 요청 헤더에 넣는 JWT (유효기간 짧음, 보통 1시간)
// refresh_token → access_token 만료 시 새로 발급받는 토큰 (유효기간 김)
#[derive(Debug, Deserialize)]
struct SupabaseTokenResponse {
    access_token: String,
    refresh_token: String,
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

    // nonce store에 저장: 키=지갑주소, 값=nonce
    // 2단계 verify_and_login()에서 꺼내서 비교함
    state
        .nonce_store
        .insert(wallet_address.to_string(), nonce.clone());

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
) -> Result<LoginResponse> {
    // 1) nonce 검증
    // nonce_store에서 이 지갑 주소에 해당하는 nonce를 꺼냄.
    // ok_or_else: Option이 None이면 에러로 변환
    // None인 경우 → 1단계를 호출하지 않았거나 이미 사용된 nonce
    let stored_nonce = state
        .nonce_store
        .get(wallet_address)
        .ok_or_else(|| anyhow!("nonce가 없거나 만료됨. /auth/wallet/nonce를 먼저 호출하세요."))?;

    // 저장된 nonce와 앱이 보낸 nonce가 다르면 위조된 요청
    if stored_nonce.value() != nonce {
        return Err(anyhow!("nonce 불일치. 위조된 요청일 수 있음."));
    }

    // nonce는 1회용이므로 검증 즉시 삭제
    // drop()을 먼저 해야 DashMap의 읽기 잠금이 풀려서 remove()가 가능함
    drop(stored_nonce);
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

    // 4) Supabase JWT 발급
    // 찾은 user_id로 Supabase Admin API를 호출해서 JWT를 직접 발급
    // 지갑 로그인은 Supabase의 기본 OAuth 플로우를 타지 않아서 서버가 Admin 권한으로 대신 발급해줘야 함
    let tokens = generate_supabase_token(state, &user_id.to_string()).await?;

    // 5) 응답 반환
    // 지갑 로그인은 기존 유저만 가능 (신규 유저는 일반 회원가입으로만 생성)
    // 따라서 is_new_user는 항상 false
    Ok(LoginResponse {
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        is_new_user: false,
    })
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
    // Base58 문자열("7xKXtg2...") → 바이트 배열([0x7a, 0x2b, ...])로 변환
    let pubkey_bytes = bs58::decode(wallet_address)
        .into_vec()
        .context("지갑 주소 Base58 디코딩 실패. 유효한 Solana 주소인지 확인 필요.")?;

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

    // ed25519 서명은 정확히 64바이트여야 함
    let sig_array: [u8; 64] = sig_bytes
        .try_into()
        .map_err(|_| anyhow!("서명이 64바이트가 아님. 잘못된 서명."))?;

    let sig = Signature::from_bytes(&sig_array);

    // 핵심: 공개키(verifying_key)로 서명(sig)을 검증
    // nonce.as_bytes(): nonce 원문을 바이트로 변환해서 검증 대상으로 사용
    // 성공 → 이 서명은 이 공개키에 대응하는 개인키로 만든 것이 맞음
    // 실패 → 서명이 위조됐거나 다른 지갑으로 서명한 것
    verifying_key
        .verify(nonce.as_bytes(), &sig)
        .context("서명 검증 실패. 지갑 주인이 아니거나 nonce가 변조됨.")?;
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
async fn generate_supabase_token(
    state: &AppState,
    // JWT를 발급받을 유저의 UUID (문자열 형태)
    user_id: &str,
) -> Result<SupabaseTokenResponse> {
    // POST /auth/v1/admin/users/{}/token?grant_type=id_token
    // grant_type=id_token → 해당 유저의 JWT를 Admin 권한으로 직접 발급
    let url = format!(
        "{}/auth/v1/admin/users/{}/token?grant_type=id_token",
        state.config.supabase_url, user_id
    );

    let resp = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        // body는 비어있어도 됨 (Supabase 스펙상 필요 없음)
        .json(&serde_json::json!({}))
        .send()
        .await
        .context("토큰 발급 HTTP 요청 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("토큰 발급 실패: {}", err_text));
    }

    // 응답 JSON → SupabaseTokenResponse 구조체로 파싱
    // ::<SupabaseTokenResponse> → 어떤 타입으로 파싱할지 명시 (타입 추론 불가 상황)
    resp.json::<SupabaseTokenResponse>()
        .await
        .context("토큰 응답 JSON 파싱 실패")
}
