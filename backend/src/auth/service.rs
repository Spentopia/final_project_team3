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
use ed25519_dalek::{Signature,Verifier, VerifyingKey};
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
    //영문+숫자 조합으로 32자리 랜덤 문자열 생성
    let nonce: String = rand::thread_rng()
        .sample_iter(&rand::distributions::Alphanumeric)
        .take(32)
        .map(char::from).collect();

    // DashMap에 저장. 같은 지갑 주소로 다시 요청하면 이전 nonce는 덮어써짐
    // 이래야 한 지갑당 유효한 nonce가 항상 1개뿐이라 보안에 좋음
    state.nonce_store.insert(wallet_address.to_string(),nonce.clone());

    tracing::info!("nonce 발급 완료: wallet={}", wallet_address);
    nonce
}