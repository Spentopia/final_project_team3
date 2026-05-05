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
use uuid::Uuid;

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
    pub message: String,
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
    pub captcha_token: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct FindEmailResponse {
    pub masked_email: Option<String>,
    pub login_provider: String,
    pub google_connected: bool,
    pub message: String,
}

// ── 이메일 존재 확인 ─────────────────────────────────────────
//
// 비밀번호 재설정 전에
// "해당 이메일로 가입된 계정이 있는지" 확인할 때 사용
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckEmailRequest {
    pub email: String,
    pub captcha_token: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckEmailResponse {
    pub exists: bool,
}

// ── 비밀번호 재설정 이메일 확인 ──────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckResetPasswordEmailRequest {
    pub email: String,
    pub captcha_token: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CheckResetPasswordEmailResponse {
    pub exists: bool,
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

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AppKakaoStartResponse {
    pub auth_url: String,
    pub state: String,
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

// ─────────────────────────────────────────────────────────────
// handoff token 발급 요청
//
// POST /auth/handoff
// Authorization: Bearer <access_token>
//
// 웹에서 "게임 실행" 버튼 클릭 시 호출.
// JWT 미들웨어가 user_id를 확인한 뒤,
// 유니티 exe 진입용 1회용 handoff token을 발급한다.
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct HandoffRequest {
    // 현재는 "unity"만 허용
    pub target_service: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct HandoffResponse {
    // 웹이 exe 실행용 프로토콜/런처에 실어 전달할 1회용 token 원문
    pub handoff_token: String,

    // 프론트 디버깅/UX용
    pub expires_in: i32,
}

// ─────────────────────────────────────────────────────────────
// handoff token 교환 요청
//
// POST /auth/handoff/exchange
//
// 유니티 exe가 실행 시 전달받은 handoff token을
// 자기 서비스용 access/refresh로 교환할 때 사용.
//
// 중요:
// - target_service는 클라이언트가 보내지 않음
// - 서버 메모리에 저장된 target_service를 기준으로만 검증
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct HandoffExchangeRequest {
    pub handoff_token: String,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct HandoffExchangeResponse {
    pub access_token: String,
    pub refresh_token: String,
    pub user_id: Uuid,
    pub nickname: Option<String>,
}

// ─────────────────────────────────────────────────────────────
// 안드로이드 웹뷰 진입용 토큰 발급
//
// POST /auth/webview/issue
// Authorization: Bearer <앱_access_token>
//
// 앱이 NFT 마켓 웹뷰 띄우기 직전 호출.
// jwt_middleware가 user_id를 확인한 뒤,
// 30초 TTL의 1회용 webview token을 발급한다.
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WebviewIssueRequest {
    /// 웹뷰 진입 후 이동할 프론트엔드 경로
    /// 예: "/marketplace" / "/marketplace/listing/abc123"
    /// 미지정 시 백엔드가 "/marketplace"로 기본 처리
    pub redirect_path: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WebviewIssueResponse {
    /// 30초 후 만료되는 1회용 토큰
    /// 앱은 이걸 받아서 즉시 webView.loadUrl 쿼리에 사용
    pub webview_token: String,

    /// 만료까지 남은 초 (참고용)
    pub expires_in: i32,
}

// ─────────────────────────────────────────────────────────────
// 웹뷰 콜백 쿼리 파라미터
//
// GET /auth/webview/callback?token=xxx
//
// 인증 미들웨어 없음 (webview token 자체가 인증 수단).
// 검증 성공 시:
// - Set-Cookie로 refresh token 셋팅 (httpOnly)
// - Location 헤더로 프론트엔드 리다이렉트
//   (access token은 URL fragment(#access_token=xxx)로 전달)
// ─────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct WebviewCallbackQuery {
    pub token: String,
}
