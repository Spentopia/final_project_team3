// auth/dto.rs
// 클라이언트와 주고받는 요청/응답 구조체를 모아둔 파일임.
// DB 엔티티(model.rs)와 분리해서 관리함.

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

// ── 소셜 로그인 ───────────────────────────────────────────────

// 구글/카카오 소셜 로그인 요청 DTO임.
// 프론트에서 소셜 로그인 후 받은 access_token을 백엔드로 보낼 때 씀.
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct SocialLoginRequest {
    pub provider_token: String,
}

// ── 자체 로그인 ───────────────────────────────────────────────

// 이메일/비밀번호 로그인 요청 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct EmailLoginRequest {
    pub email: String,
    pub password: String,
}

// 자체 회원가입 요청 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct SignUpRequest {
    pub email: String,
    pub password: String,
}


// ── 지갑 로그인 ───────────────────────────────────────────────

// 지갑 로그인 1단계: nonce 요청 DTO임.
// 클라이언트가 지갑 주소를 보내면 서버가 nonce를 발급해줌.
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct NonceRequest {
    pub wallet_address: String,
}

//nonce 발급 응답 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct NonceResponse {
    pub nonce: String,
}


// 지갑 로그인 2단계: 서명 검증 요청 DTO임.
// 클라이언트가 nonce에 서명한 값을 보내면 서버가 검증 후 JWT를 발급함.
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WalletLoginRequest {
    pub wallet_address: String,
    pub nonce: String,
    pub signature: String,
}


// ── 공통 응답 ─────────────────────────────────────────────────

// 로그인 성공 응답 DTO임
// 모든 로그인 방식(소셜/자체/지갑)이 동일한 응답을 반환함.
// 입구만 다르고 나오는 건 항상 Supabase JWT
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct LoginResponse {

    // Supabase가 발급한 JWT. 이후 모든 API 요청 헤더에 이 값을 넣어서 보냄
    pub access_token: String,

    pub refresh_token: String,

    pub is_new_user: bool,

}

// ── 이메일 찾기 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct FindEmailRequest {
    pub phone: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct FindEmailResponse {
    pub masked_email: String,
}

// ── 카카오 로그인 ─────────────────────────────────────────────

// 카카오 인가 코드 요청 DTO
// 프론트 콜백 페이지에서 카카오가 준 인가 코드를 백엔드로 보냄
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct KakaoLoginRequest {
    pub code: String,
}
