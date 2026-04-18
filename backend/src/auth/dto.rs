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

// 변경 핵심:
// - web / app 응답 분리
// - refresh 요청 DTO 추가
//
// 최종 정책:
// - 웹 로그인 응답: access만 body, refresh는 쿠키
// - 앱 로그인 응답: access + refresh 둘 다 body
// - 웹 refresh 요청: body 없이 가능 (쿠키에서 refresh 추출)
// - 앱 refresh 요청: body에 refresh_token 포함

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

// ── refresh 요청 ─────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct RefreshRequest {
    pub refresh_token: Option<String>,
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

// ── 로그인 응답 분리 ─────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WebLoginResponse {
    pub access_token: String,
    pub is_new_user: bool,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AppLoginResponse {
    pub access_token: String,
    pub refresh_token: String,
    pub is_new_user: bool,
}

// ── refresh 응답 분리 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WebRefreshResponse {
    pub access_token: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AppRefreshResponse {
    pub access_token: String,
    pub refresh_token: String,
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

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckProfileAvailabilityRequest {
    pub nickname: String,
    pub phone: String,
}

// ── 카카오 로그인 ─────────────────────────────────────────────
//
// 프론트 콜백 페이지에서 카카오가 준 인가 코드를 백엔드로 보낼 때 사용
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct KakaoLoginRequest {
    pub code: String,
    pub state: String,
    
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct KakaoStartResponse {
    pub auth_url: String,
}

// ── 프로필 완성 ───────────────────────────────────────────────
//
// 회원가입/소셜 로그인 후 nickname + phone + profile_image 저장
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CompleteProfileRequest {
    pub nickname: String,
    pub phone: String,

    // 여기는 최종 url이 아니라 Storage patth 저장
    // 예: defaults/avatar.png
    // 예: user-uuid/avatar.png
    pub profile_image: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CompleteProfileResponse {
    pub success: bool,
    pub profile_completed: bool,
}

// ─────────────────────────────────────────────────────────────
// private bucket 이미지 signed URL 요청용 query
// 예: /profile/image-url?path=defaults/avatar.png
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ProfileImageUrlQuery {
    pub path: String,
}

// ─────────────────────────────────────────────────────────────
// signed URL 응답
// 프론트는 이 값을 img src에 넣어 보여주면 됨
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ProfileImageUrlResponse {
    pub signed_url: String,
}
