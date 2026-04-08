// auth/service.rs
// 지갑(Solana) 로그인의 전체 비즈니스 로직을 담당하는 파일임.
//
// 지갑 로그인은 2단계로 진행됨:
// [1단계] 앱이 지갑 주소를 보내면 → 서버가 랜덤 nonce를 만들어서 돌려줌
// [2단계] 앱이 그 nonce를 지갑으로 서명해서 보내면
//        → 서버가 서명 검증 → Supabase에 유저 생성/조회 → JWT 발급해서 돌려줌
//
// 왜 nonce가 필요한가?
// 지갑 로그인은 비밀번호가 없음. 대신 "이 지갑의 주인이 맞는지"를 증명해야 함.
// 서버가 랜덤 문자열(nonce)을 주고, 유저가 그걸 지갑 개인키로 서명하면
// 서버는 공개키(지갑 주소)로 서명을 검증해서 본인임을 확인할 수 있음.
// nonce는 1회용이라 재사용(리플레이 공격)이 불가능함.

use anyhow::{anyhow, Context, Result};
use ed25519_dalek::{Signature, Verifier, VerifyingKey};
use rand::Rng;
use serde::Deserialize;

use crate::state::AppState;
use super::dto::LoginResponse;

// ── Supabase Admin API 응답 구조체 ──────────────────────────

// Supabase Admin API로 토큰 발급하면 이 형태로 응답이 옴.
// access_token: 앱이 이후 모든 API 요청에 붙여 보낼 JWT
// refresh_token: access_token 만료 시 갱신용 토큰
#[derive(Debug, Deserialize)]
struct SupabaseTokenResponse {
    access_token: String,
    refresh_token: String,
}

// ═══════════════════════════════════════════════════════════════
// [1단계] nonce 발급
// ═══════════════════════════════════════════════════════════════
//
// 앱에서 "나 지갑 로그인 할게" → 지갑 주소를 보냄
// 서버는 랜덤 32자리 문자열(nonce)을 만들어서
// - 메모리(nonce_store)에 {지갑주소: nonce} 형태로 저장하고
// - nonce를 앱에 돌려줌
//
// 앱은 이 nonce를 받아서 지갑으로 서명한 뒤 2단계에서 보냄.

pub fn generate_nonce(state: &AppState, wallet_address: &str) -> String {
    // 영문+숫자 조합으로 32자리 랜덤 문자열 생성
    // 예: "aB3kQ9xZ2mL7pR4wT1yN8cV5hJ0gF6s"
    let nonce: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(32)
        .map(char::from)
        .collect();

    // DashMap에 저장. 같은 지갑 주소로 다시 요청하면 이전 nonce는 덮어써짐.
    // 이래야 한 지갑당 유효한 nonce가 항상 1개뿐이라 보안에 좋음.
    state.nonce_store.insert(wallet_address.to_string(), nonce.clone());

    tracing::info!("nonce 발급 완료: wallet={}", wallet_address);
    nonce
}

// ═══════════════════════════════════════════════════════════════
// [2단계] 서명 검증 + 로그인
// ═══════════════════════════════════════════════════════════════
//
// 앱에서 {지갑주소, nonce, 서명} 세 개를 보냄
// 서버는 다음 순서로 처리:
// 1) 저장된 nonce와 비교 (위조 방지)
// 2) Solana ed25519 서명 검증 (지갑 주인 확인)
// 3) Supabase에서 유저 찾거나 새로 생성
// 4) Supabase Admin API로 JWT 발급
// 5) 앱에 {access_token, refresh_token, is_new_user} 반환

pub async fn verify_and_login(
    state: &AppState,
    wallet_address: &str,
    nonce: &str,
    signature: &str,
) -> Result<LoginResponse> {

    // ── 1) nonce 검증 ──────────────────────────────────────
    // 아까 1단계에서 저장해둔 nonce를 꺼내서 비교함.
    // 없으면 → 1단계를 안 했거나 이미 사용된 nonce임.
    let stored_nonce = state.nonce_store.get(wallet_address)
        .ok_or_else(|| anyhow!("nonce가 없거나 만료됨 (1단계를 먼저 호출해야 함)"))?;

    if stored_nonce.value() != nonce {
        return Err(anyhow!("nonce 불일치 (위조된 요청일 수 있음)"));
    }

    // nonce는 1회용이므로 검증 후 즉시 삭제.
    // drop()을 먼저 해야 DashMap 락이 풀려서 remove()가 가능함.
    drop(stored_nonce);
    state.nonce_store.remove(wallet_address);

    // ── 2) Solana 서명 검증 ─────────────────────────────────
    // 지갑 주소(공개키)로 서명을 검증해서 실제 지갑 주인인지 확인.
    verify_solana_signature(wallet_address, nonce, signature)?;

    tracing::info!("서명 검증 성공: wallet={}", wallet_address);

    // ── 3) Supabase 유저 생성/조회 ──────────────────────────
    // Supabase auth.users에 이 지갑 주소로 가입한 유저가 있는지 확인.
    // 없으면 새로 만들고, 있으면 기존 유저 ID를 가져옴.
    let (user_id, is_new_user) = find_or_create_user(state, wallet_address).await?;

    // ── 4) JWT 발급 ─────────────────────────────────────────
    // Supabase Admin API를 통해 해당 유저의 access_token과 refresh_token을 발급.
    let tokens = generate_supabase_token(state, &user_id).await?;

    // ── 5) 응답 반환 ────────────────────────────────────────
    Ok(LoginResponse {
        access_token: tokens.access_token,
        refresh_token: tokens.refresh_token,
        is_new_user,
    })
}

// ═══════════════════════════════════════════════════════════════
// Solana ed25519 서명 검증
// ═══════════════════════════════════════════════════════════════
//
// Solana 지갑 주소 = ed25519 공개키를 Base58로 인코딩한 것.
// 서명도 Base58로 인코딩되어 있음.
//
// 검증 과정:
// 1) 지갑 주소(Base58) → 32바이트 공개키로 디코딩
// 2) 서명(Base58) → 64바이트 서명으로 디코딩
// 3) 공개키로 "nonce 원문 + 서명"을 검증
//    → 일치하면 이 서명은 해당 지갑의 개인키로 만든 게 맞음

fn verify_solana_signature(
    wallet_address: &str,
    nonce: &str,
    signature: &str,
) -> Result<()> {
    // 지갑 주소를 Base58에서 바이트 배열로 변환
    // 예: "7xKXtg2CW87..." → [0x7a, 0x2b, 0x3c, ...]
    let pubkey_bytes = bs58::decode(wallet_address)
        .into_vec()
        .context("지갑 주소 Base58 디코딩 실패")?;

    // ed25519 공개키는 정확히 32바이트여야 함
    let pubkey_array: [u8; 32] = pubkey_bytes
        .try_into()
        .map_err(|_| anyhow!("공개키가 32바이트가 아님 (잘못된 지갑 주소)"))?;

    // 바이트 배열 → ed25519 검증용 공개키 객체로 변환
    let verifying_key = VerifyingKey::from_bytes(&pubkey_array)
        .context("ed25519 공개키 생성 실패")?;

    // 서명도 Base58에서 바이트 배열로 변환
    let sig_bytes = bs58::decode(signature)
        .into_vec()
        .context("서명 Base58 디코딩 실패")?;

    // ed25519 서명은 정확히 64바이트여야 함
    let sig_array: [u8; 64] = sig_bytes
        .try_into()
        .map_err(|_| anyhow!("서명이 64바이트가 아님 (잘못된 서명)"))?;

    let sig = Signature::from_bytes(&sig_array);

    // 핵심: nonce 원문(바이트)에 대해 서명이 유효한지 검증
    // 공개키(지갑 주소)로 서명을 열어봤을 때 nonce 원문이 나오면 → 검증 성공
    // 즉, 이 서명을 만든 사람이 이 지갑의 주인이라는 뜻
    verifying_key.verify(nonce.as_bytes(), &sig)
        .context("서명 검증 실패 (지갑 주인이 아니거나 nonce가 변조됨)")?;

    Ok(())
}

// ═══════════════════════════════════════════════════════════════
// Supabase 유저 생성/조회
// ═══════════════════════════════════════════════════════════════
//
// Supabase auth.users는 이메일 기반이라 지갑 주소를 직접 쓸 수 없음.
// 그래서 "지갑주소@wallet.local" 같은 가짜 이메일을 만들어서 우회함.
//
// 흐름:
// 1) Admin API로 전체 유저 목록 조회
// 2) 가짜 이메일과 일치하는 유저가 있으면 → 기존 유저 (is_new_user: false)
// 3) 없으면 Admin API로 새 유저 생성 → 신규 유저 (is_new_user: true)
//
// app_metadata에 provider:"wallet"과 실제 지갑 주소를 저장해서
// 나중에 이 유저가 지갑으로 가입했다는 걸 구분할 수 있게 함.

async fn find_or_create_user(
    state: &AppState,
    wallet_address: &str,
) -> Result<(String, bool)> {
    let base_url = &state.config.supabase_url;
    let secret_key = &state.config.supabase_secret_key;

    // 지갑 주소로 가짜 이메일 생성
    // 예: "7xKXtg2CW87d@wallet.local"
    let fake_email = format!("{}@wallet.local", wallet_address);

    // ── 기존 유저 조회 ──────────────────────────────────────
    // GET /auth/v1/admin/users → 전체 유저 목록을 가져옴
    // ⚠️ 유저가 많아지면 페이징 처리가 필요함 (현재는 MVP이라 생략)
    let list_url = format!("{}/auth/v1/admin/users", base_url);

    let list_resp = state.http_client
        .get(&list_url)
        .header("Authorization", format!("Bearer {}", secret_key))
        .header("apikey", secret_key)
        .send()
        .await
        .context("유저 목록 조회 HTTP 요청 실패")?;

    let list_body: serde_json::Value = list_resp.json().await
        .context("유저 목록 JSON 파싱 실패")?;

    // users 배열을 돌면서 이메일이 일치하는 유저 찾기
    if let Some(users) = list_body["users"].as_array() {
        for user in users {
            if user["email"].as_str() == Some(&fake_email) {
                // 기존 유저 발견 → ID 반환
                let id = user["id"].as_str()
                    .ok_or_else(|| anyhow!("유저 객체에 id 필드 없음"))?;
                tracing::info!("기존 지갑 유저 발견: id={}", id);
                return Ok((id.to_string(), false));
            }
        }
    }

    // ── 신규 유저 생성 ──────────────────────────────────────
    // POST /auth/v1/admin/users → 새 유저를 만듦
    // email_confirm: true → 이메일 인증 절차 건너뜀 (가짜 이메일이니까)
    let create_url = format!("{}/auth/v1/admin/users", base_url);

    let create_body = serde_json::json!({
        "email": fake_email,
        "email_confirm": true,        // 이메일 인증 스킵
        "app_metadata": {
            "provider": "wallet",      // 가입 경로 표시
            "wallet_address": wallet_address  // 실제 지갑 주소 보관
        }
    });

    let create_resp = state.http_client
        .post(&create_url)
        .header("Authorization", format!("Bearer {}", secret_key))
        .header("apikey", secret_key)
        .json(&create_body)
        .send()
        .await
        .context("유저 생성 HTTP 요청 실패")?;

    // 생성 실패 시 에러 메시지 포함해서 반환
    if !create_resp.status().is_success() {
        let err_text = create_resp.text().await.unwrap_or_default();
        return Err(anyhow!("유저 생성 실패: {}", err_text));
    }

    let created: serde_json::Value = create_resp.json().await
        .context("생성된 유저 JSON 파싱 실패")?;

    let user_id = created["id"].as_str()
        .ok_or_else(|| anyhow!("생성된 유저 객체에 id 필드 없음"))?;

    tracing::info!("새 지갑 유저 생성 완료: id={}, wallet={}", user_id, wallet_address);

    Ok((user_id.to_string(), true))
}

// ═══════════════════════════════════════════════════════════════
// Supabase JWT 발급
// ═══════════════════════════════════════════════════════════════
//
// Supabase Admin API를 사용해서 특정 유저의 JWT를 서버 측에서 직접 발급함.
// 일반적으로는 프론트에서 Supabase SDK가 하는 일이지만,
// 지갑 로그인은 Supabase OAuth 플로우를 안 타기 때문에
// 백엔드가 Admin 권한으로 직접 토큰을 만들어줘야 함.
//
// 발급된 access_token은 다른 소셜/이메일 로그인으로 받은 JWT와 동일한 형태라
// middleware.rs의 jwt_middleware에서 똑같이 검증 가능함.

async fn generate_supabase_token(
    state: &AppState,
    user_id: &str,
) -> Result<SupabaseTokenResponse> {
    let base_url = &state.config.supabase_url;
    let secret_key = &state.config.supabase_secret_key;

    // POST /auth/v1/admin/users/{user_id}/token
    // grant_type=id_token → 해당 유저의 JWT를 직접 생성해달라는 뜻
    let url = format!(
        "{}/auth/v1/admin/users/{}/token?grant_type=id_token",
        base_url, user_id
    );

    let resp = state.http_client
        .post(&url)
        .header("Authorization", format!("Bearer {}", secret_key))
        .header("apikey", secret_key)
        .json(&serde_json::json!({}))  // body는 비어있어도 됨
        .send()
        .await
        .context("토큰 발급 HTTP 요청 실패")?;

    if !resp.status().is_success() {
        let err_text = resp.text().await.unwrap_or_default();
        return Err(anyhow!("토큰 발급 실패: {}", err_text));
    }

    resp.json::<SupabaseTokenResponse>().await
        .context("토큰 응답 JSON 파싱 실패")
}