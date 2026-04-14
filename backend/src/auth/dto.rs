// auth/dto.rs
// 클라이언트와 주고받는 요청/응답 구조체를 모아둔 파일임.
// DB 엔티티(model.rs)와 분리해서 관리함.
//
// 지금 구조:
// - 이메일 로그인 / 구글 로그인: Supabase로 1차 인증 후
//   백엔드 /auth/exchange 로 access_token을 보내서
//   "우리 앱 JWT"로 교환함
// - 카카오 로그인: 백엔드가 직접 처리하고 바로 "우리 앱 JWT" 발급
// - 지갑 로그인: 백엔드가 직접 처리하고 바로 "우리 앱 JWT" 발급
//
// 즉 최종적으로 프론트가 들고 다니는 토큰은 항상 "우리 앱 JWT"임.

use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

// ── Supabase 토큰 교환 ───────────────────────────────────────
//
// 이메일 로그인 / 구글 로그인 후 프론트가 Supabase access_token을
// 백엔드 /auth/exchange 로 보낼 때 쓰는 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ExchangeTokenRequest {
    pub access_token: String,
}

// ── 지갑 로그인 ───────────────────────────────────────────────
//
// 지갑 로그인 1단계: nonce 요청 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct NonceRequest {
    pub wallet_address: String,
}

// nonce 발급 응답 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct NonceResponse {
    pub nonce: String,
}

// 지갑 로그인 2단계: 서명 검증 요청 DTO
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WalletLoginRequest {
    pub wallet_address: String,
    pub nonce: String,
    pub signature: String,
}

// ── 공통 응답 ─────────────────────────────────────────────────
//
// 로그인 성공 응답 DTO
// 모든 로그인 방식(이메일/구글/카카오/지갑)이
// 최종적으로 동일한 응답 형태를 반환함.
//
// 중요:
// 여기 들어가는 access_token / refresh_token은
// Supabase JWT가 아니라 "우리 백엔드가 발급한 앱 JWT"임.
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct LoginResponse {
    // 우리 앱 access token
    pub access_token: String,

    // 우리 앱 refresh token
    pub refresh_token: String,

    // 첫 가입 여부
    // 필요 없으면 나중에 제거 가능하지만, 카카오/구글 첫 가입 분기용으로 남겨둠
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

// ── 이메일 존재 확인 ─────────────────────────────────────────
//
// 비밀번호 재설정 전에
// "해당 이메일로 가입된 계정이 있는지" 확인할 때 사용
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckEmailRequest {
    pub email: String,
}

// ── 카카오 로그인 ─────────────────────────────────────────────
//
// 프론트 콜백 페이지에서 카카오가 준 인가 코드를 백엔드로 보낼 때 사용
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct KakaoLoginRequest {
    pub code: String,
}

// ── 프로필 완성 ───────────────────────────────────────────────
//
// 회원가입/소셜 로그인 후 nickname + phone + profile_image 저장
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CompleteProfileRequest {
    pub nickname: String,
    pub phone: String,
    pub profile_image: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CompleteProfileResponse {
    pub success: bool,
    pub profile_completed: bool,
}