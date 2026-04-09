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
use super::dto::{NonceRequest, NonceResponse, WalletLoginRequest, LoginResponse};
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
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
) -> String {
    format!("authenticated: user_id={}", user_id)
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

