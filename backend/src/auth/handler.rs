// auth/handler.rs
// HTTP 엔드포인트 핸들러 모음
// 컨트롤러 역할임. 요청 파싱 → service 호출 → 응답 반환
//
// 스프링부트로 치면 @RestController 클래스에 해당함.
// 비즈니스 로직은 service.rs에 있고, 여기는 요청/응답 변환만 함.

use axum::{
    extract::State,
    http::StatusCode,
    Json,
};

use utoipa;
use crate::state::AppState;
use super::dto::{NonceRequest, NonceResponse, WalletLoginRequest, LoginResponse,
FindEmailRequest, FindEmailResponse,EmailLoginRequest, KakaoLoginRequest};
use super::service;

// ═══════════════════════════════════════════════════════════════
// [지갑 로그인 1단계] POST /auth/wallet/nonce
// ═══════════════════════════════════════════════════════════════
//
// 앱에서 지갑 주소를 보내면 nonce를 발급해서 돌려줌.
//
// 요청 예시:
// POST /auth/wallet/nonce
// { "wallet_address": "7xKXtg2CW87d..." }
//
// 응답 예시:
// 200 OK
// { "nonce": "aB3kQ9xZ2mL7pR4wT1yN8cV5hJ0gF6s" }

#[utoipa::path(
    post,
    path = "/auth/wallet/nonce",
    tag = "지갑 로그인",
    request_body = NonceRequest,
    responses(
        (status = 200, description = "nonce 발급 성공", body = NonceResponse),
        (status = 400, description = "지갑 주소가 비어있음")
    )
)]
pub async fn request_nonce(
    // State: main.rs에서 with_state(state)로 등록한 AppState를 꺼내옴
    // 스프링부트의 @Autowired와 비슷함
    State(state): State<AppState>,

    // Json: 요청 body를 NonceRequest 구조체로 자동 파싱
    // 스프링부트의 @RequestBody와 동일
    Json(body): Json<NonceRequest>,
) -> Result<Json<NonceResponse>, (StatusCode, String)> {

    // 지갑 주소가 비어있으면 400 반환
    if body.wallet_address.trim().is_empty() {
        tracing::warn!("nonce 요청: 지갑 주소 비어있음");
        return Err((StatusCode::BAD_REQUEST, "지갑 주소가 비어있음".to_string()));
    }

    // service에서 nonce 생성
    let nonce = service::generate_nonce(&state, &body.wallet_address);

    // 200 OK + nonce 반환
    Ok(Json(NonceResponse { nonce }))
}

// ═══════════════════════════════════════════════════════════════
// [지갑 로그인 2단계] POST /auth/wallet/login
// ═══════════════════════════════════════════════════════════════
//
// 앱에서 {지갑주소, nonce, 서명}을 보내면
// 서명 검증 → Supabase 유저 생성/조회 → JWT 발급해서 돌려줌.
//
// 요청 예시:
// POST /auth/wallet/login
// {
//   "wallet_address": "7xKXtg2CW87d...",
//   "nonce": "aB3kQ9xZ2mL7pR4wT1yN8cV5hJ0gF6s",
//   "signature": "3bNzR8K..."
// }
//
// 성공 응답 예시:
// 200 OK
// {
//   "access_token": "eyJhbGciOiJFUzI1NiIs...",
//   "refresh_token": "v1.MDA3YTk2...",
//   "is_new_user": true
// }

#[utoipa::path(
    post,
    path = "/auth/wallet/login",
    tag = "지갑 로그인",
    request_body = WalletLoginRequest,
    responses(
        (status = 200, description = "로그인 성공", body = LoginResponse),
        (status = 400, description = "필수 필드 비어있음"),
        (status = 401, description = "서명 검증 실패 또는 nonce 불일치"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn wallet_login(
    State(state): State<AppState>,
    Json(body): Json<WalletLoginRequest>,
) -> Result<Json<LoginResponse>, (StatusCode, String)> {

    // 필수 필드 비어있는지 체크
    if body.wallet_address.trim().is_empty() {
        tracing::warn!("지갑 로그인: 지갑 주소 비어있음");
        return Err((StatusCode::BAD_REQUEST, "지갑 주소가 비어있음".to_string()));
    }
    if body.nonce.trim().is_empty() {
        tracing::warn!("지갑 로그인: nonce 비어있음");
        return Err((StatusCode::BAD_REQUEST, "nonce가 비어있음".to_string()));
    }
    if body.signature.trim().is_empty() {
        tracing::warn!("지갑 로그인: 서명 비어있음");
        return Err((StatusCode::BAD_REQUEST, "서명이 비어있음".to_string()));
    }

    // service에서 서명 검증 + 로그인 처리
    // 성공하면 LoginResponse(access_token, refresh_token, is_new_user) 반환
    // 실패하면 anyhow::Error가 올라옴
    let response = service::verify_and_login(
        &state,
        &body.wallet_address,
        &body.nonce,
        &body.signature,
    )
        .await
        .map_err(|e| {
            // service에서 올라온 에러를 여기서 로깅하고 적절한 HTTP 상태코드로 변환
            // 에러 메시지에 따라 401(인증실패) 또는 500(서버에러) 구분
            let msg = e.to_string();
            if msg.contains("nonce") || msg.contains("서명") {
                // nonce 불일치, 서명 검증 실패 → 클라이언트 잘못
                tracing::warn!("지갑 로그인 실패 (클라이언트): {}", msg);
                (StatusCode::UNAUTHORIZED, msg)
            } else {
                // Supabase API 실패 등 → 서버 잘못
                tracing::error!("지갑 로그인 실패 (서버): {}", msg);
                (StatusCode::INTERNAL_SERVER_ERROR, msg)
            }
        })?;

    tracing::info!("지갑 로그인 성공: wallet={}", body.wallet_address);

    Ok(Json(response))
}

/// 내 인증 상태 확인
///
/// JWT가 유효한지 테스트하는 용도입니다. 토큰이 유효하면 user_id를 반환합니다.
#[utoipa::path(
    get,
    path = "/me",
    tag = "인증 테스트",
    security(
        ("bearer_auth" = [])
    ),
    responses(
        (status = 200, description = "인증 성공"),
        (status = 401, description = "토큰 없음 또는 유효하지 않음")
    )
)]
pub async fn get_me(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=id,email,profile_completed,login_provider",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_publishable_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .map_err(|e| {
            tracing::error!("/me 유저 조회 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "유저 조회 실패".to_string())
        })?;

    let users: Vec<serde_json::Value> = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let user = users.first()
        .cloned()
        .ok_or_else(|| (StatusCode::NOT_FOUND, "유저 없음".to_string()))?;

    Ok(Json(user))
}

/// [테스트용] 이메일 로그인
///
/// Supabase Auth API를 대신 호출해서 토큰을 반환합니다.
#[utoipa::path(
    post,
    path = "/auth/test/login",
    tag = "테스트",
    request_body = EmailLoginRequest,
    responses(
        (status = 200, description = "로그인 성공"),
        (status = 401, description = "이메일 또는 비밀번호 틀림")
    )
)]
pub async fn test_email_login(
    State(state): State<AppState>,
    Json(body): Json<super::dto::EmailLoginRequest>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {

    let url = format!(
        "{}/auth/v1/token?grant_type=password",
        state.config.supabase_url.trim_end_matches('/')
    );

    let resp = state.http_client
        .post(&url)
        .header("apikey", &state.config.supabase_publishable_key)
        .header("Content-Type", "application/json")
        .json(&serde_json::json!({
            "email": body.email,
            "password": body.password
        }))
        .send()
        .await
        .map_err(|e| {
            tracing::error!("Supabase 로그인 요청 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, e.to_string())
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::warn!("로그인 실패: {}", err);
        return Err((StatusCode::UNAUTHORIZED, err));
    }

    let data: serde_json::Value = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    Ok(Json(data))
}

// ═══════════════════════════════════════════════════════════════
// [이메일 찾기] POST /auth/find-email
// ═══════════════════════════════════════════════════════════════
//
// 전화번호로 이메일을 찾아서 마스킹해서 반환.
// 로그인 전 상태에서 호출되므로 JWT 불필요 (공개 라우트).
//
// 왜 백엔드를 거치나?
// 이메일 찾기는 로그인 전이라 Supabase RLS가 auth.uid()를 모름
// → 프론트에서 public.users 조회해도 아무것도 안 나옴
// → 백엔드가 service_role 키(RLS 우회)로 조회 → 마스킹해서 반환
//
// 요청: { "phone": "010-1234-5678" }
// 응답: { "masked_email": "te***@gmail.com" }

/// 이메일 찾기
///
/// 전화번호로 이메일을 찾아서 마스킹 처리해서 반환합니다.
#[utoipa::path(
    post,
    path = "/auth/find-email",
    tag = "인증",
    request_body = FindEmailRequest,
    responses(
        (status = 200, description = "이메일 찾기 성공", body = FindEmailResponse),
        (status = 404, description = "해당 전화번호로 등록된 계정 없음")
    )
)]
pub async fn find_email(
    State(state): State<AppState>,
    Json(body): Json<FindEmailRequest>,
) -> Result<Json<FindEmailResponse>, (StatusCode, String)> {

    if body.phone.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "전화번호가 비어있음".to_string()));
    }

    // Supabase REST API로 public.users에서 전화번호로 이메일 조회
    // service_role 키를 쓰면 RLS를 우회할 수 있음
    let url = format!(
        "{}/rest/v1/users?phone=eq.{}&select=email",
        state.config.supabase_url.trim_end_matches('/'),
        body.phone
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_publishable_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .map_err(|e| {
            tracing::error!("이메일 찾기 요청 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "서버 내부 오류".to_string())
        })?;

    let users: Vec<serde_json::Value> = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    // 해당 전화번호로 등록된 유저가 없으면 404
    let email = users.first()
        .and_then(|u| u["email"].as_str())
        .ok_or_else(|| {
            (StatusCode::NOT_FOUND, "해당 전화번호로 등록된 계정이 없습니다".to_string())
        })?;

    // 이메일 마스킹 처리
    let masked = mask_email(email);

    Ok(Json(FindEmailResponse {
        masked_email: masked,
    }))
}

// ── 이메일 마스킹 함수 ──────────────────────────────────────
// test@gmail.com → te***@gmail.com
// ab@test.com → a***@test.com
// 아이디가 2자 이하면 1자만 보여주고, 그 외엔 앞 2자만 보여줌
fn mask_email(email: &str) -> String {
    let parts: Vec<&str> = email.split('@').collect();
    if parts.len() != 2 {
        return "***".to_string();
    }

    let local = parts[0];  // @ 앞부분
    let domain = parts[1]; // @ 뒷부분

    let visible = if local.len() <= 2 { 1 } else { 2 };

    let masked_local = format!("{}***", &local[..visible]);
    format!("{}@{}", masked_local, domain)
}


/// 이메일 존재 여부 확인
///
/// 해당 이메일로 가입된 유저가 있는지 확인합니다.
#[utoipa::path(
    post,
    path = "/auth/check-email",
    tag = "인증",
    responses(
        (status = 200, description = "이메일 존재함"),
        (status = 404, description = "해당 이메일로 가입된 계정 없음")
    )
)]
pub async fn check_email(
    State(state): State<AppState>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {

    let email = body["email"].as_str()
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "이메일이 비어있음".to_string()))?;

    // service_role 키로 public.users에서 이메일 조회 (RLS 우회)
    let url = format!(
        "{}/rest/v1/users?email=eq.{}&select=id",
        state.config.supabase_url.trim_end_matches('/'),
        email
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_publishable_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    let users: Vec<serde_json::Value> = resp.json().await
        .map_err(|e| (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()))?;

    if users.is_empty() {
        return Err((StatusCode::NOT_FOUND, "해당 이메일로 가입된 계정이 없습니다".to_string()));
    }

    Ok(Json(serde_json::json!({ "exists": true })))
}

/// 카카오 로그인
///
/// 카카오 인가 코드를 받아서 유저 정보 조회 후 JWT를 발급합니다.
#[utoipa::path(
    post,
    path = "/auth/kakao/login",
    tag = "소셜 로그인",
    request_body = KakaoLoginRequest,
    responses(
        (status = 200, description = "로그인 성공", body = LoginResponse),
        (status = 401, description = "카카오 인증 실패"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn kakao_login(
    State(state): State<AppState>,
    Json(body): Json<super::dto::KakaoLoginRequest>,
) -> Result<Json<LoginResponse>, (StatusCode, String)> {

    if body.code.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "인가 코드가 비어있음".to_string()));
    }

    let response = service::kakao_login(&state, &body.code)
        .await
        .map_err(|e| {
            let msg = e.to_string();
            tracing::error!("카카오 로그인 실패: {}", msg);
            (StatusCode::INTERNAL_SERVER_ERROR, msg)
        })?;

    tracing::info!("카카오 로그인 성공");

    Ok(Json(response))
}
