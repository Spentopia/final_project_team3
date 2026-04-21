// auth/handler.rs
// HTTP 엔드포인트 핸들러 모음
// 컨트롤러 역할임. 요청 파싱 → service 호출 → 응답 반환
//
// 스프링부트로 치면 @RestController 클래스에 해당함.
// 비즈니스 로직은 service.rs에 있고, 여기는 요청/응답 변환만 함.

// 최종 정책:
// [웹]
// - access token  -> 응답 body
// - refresh token -> HttpOnly 쿠키
//
// [앱]
// - access token  -> 응답 body
// - refresh token -> 응답 body
//
// 클라이언트 구분 방식:
// - 헤더 X-Client-Type: web
// - 헤더 X-Client-Type: app
//
// 기본값:
// - 헤더가 없으면 web으로 간주
//
// 로컬 시연 기준:
// - production 분기 없음
// - refresh 쿠키는 항상 secure(false)

use axum::{
    Json,
    extract::{Multipart, Query, State},
    http::{HeaderMap, StatusCode, header::SET_COOKIE},
};

use crate::auth::turnstile::verify_turnstile;
use crate::auth::handoff::{create_handoff_token, exchange_handoff_token};
use uuid::Uuid;
use cookie::{Cookie, SameSite};
use serde_json::json;

use crate::auth::dto::{
    AppLoginResponse, AppRefreshResponse, CheckEmailRequest, CheckEmailResponse,
    CheckProfileAvailabilityRequest, CheckResetPasswordEmailRequest, CheckResetPasswordEmailResponse,
    CompleteProfileRequest, CompleteProfileResponse, ExchangeTokenRequest, FindEmailRequest,
    FindEmailResponse, KakaoLoginRequest, KakaoStartResponse, NonceRequest, NonceResponse,
    ProfileImageUrlQuery, ProfileImageUrlResponse, RefreshRequest, WalletLoginRequest,
    WebLoginResponse, WebRefreshResponse,  HandoffExchangeRequest, HandoffExchangeResponse,
    HandoffRequest, HandoffResponse,
};
use crate::auth::service;
use crate::state::AppState;
use utoipa;

fn ensure_app_request(headers: &HeaderMap) -> Result<(), (StatusCode, String)> {
    if headers.contains_key("origin") {
        return Err((
            StatusCode::FORBIDDEN,
            "브라우저에서는 사용할 수 없는 앱 전용 인증 엔드포인트입니다.".to_string(),
        ));
    }

    match headers
        .get("x-client-type")
        .and_then(|v| v.to_str().ok())
        .map(|v| v.to_ascii_lowercase())
        .as_deref()
    {
        Some("app") => Ok(()),
        _ => Err((
            StatusCode::BAD_REQUEST,
            "앱 전용 인증 요청에는 X-Client-Type: app 헤더가 필요합니다.".to_string(),
        )),
    }
}

/// 웹용 refresh 쿠키 생성
///
/// 로컬 시연 기준이라 secure(false) 고정
///
/// 옵션 설명:
/// - HttpOnly:
///   JS(document.cookie)로 읽지 못하게 해서 XSS로 refresh 탈취를 어렵게 함
///
/// - SameSite=Lax:
///   기본적인 CSRF 위험을 줄이기 위한 설정
///
/// - Path=/auth:
///   refresh 쿠키가 /auth 하위 요청들에만 붙도록 제한
///   -> /auth/refresh 뿐 아니라 /auth/logout 에도 자동 포함됨
///
/// 왜 refresh만 쿠키냐:
/// - access는 프론트 메모리에 저장하고 Authorization 헤더로 보냄
/// - refresh는 브라우저 JS가 직접 읽지 못하게 숨기기 위함
fn build_refresh_cookie(state: &AppState, refresh_token: &str) -> HeaderMap {
    let mut headers = HeaderMap::new();
    let secure = should_use_secure_cookies(state);

    let cookie = Cookie::build(("spentopia_refresh", refresh_token.to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth")
        .secure(secure)
        .build();

    headers.append(SET_COOKIE, cookie.to_string().parse().unwrap());

    tracing::debug!(
        "refresh 쿠키 발급: name=spentopia_refresh path=/auth same_site=Lax secure={}",
        secure
    );

    headers
}

/// 웹 로그아웃 시 refresh 쿠키 제거용 Set-Cookie 생성
///
/// Max-Age=0 으로 만료시켜서 브라우저가 쿠키를 지우게 함
fn build_clear_refresh_cookie(state: &AppState) -> HeaderMap {
    let mut headers = HeaderMap::new();
    let secure = should_use_secure_cookies(state);

    let cookie = Cookie::build(("spentopia_refresh", "".to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth")
        .secure(secure)
        .max_age(cookie::time::Duration::seconds(0))
        .build();

    headers.append(SET_COOKIE, cookie.to_string().parse().unwrap());

    tracing::debug!("refresh 쿠키 삭제 헤더 발급: name=spentopia_refresh path=/auth");

    headers
}

/// 현재 요청의 Cookie 헤더에서 refresh 쿠키 추출
///
/// 웹 refresh/logout에서 사용
///
/// 앱은 쿠키를 안 쓰고 body.refresh_token을 쓰므로
/// 앱 쪽에서는 이 함수 사용 안 함
fn extract_refresh_cookie(headers: &HeaderMap) -> Option<String> {
    let raw_cookie = headers.get("cookie").and_then(|v| v.to_str().ok());

    tracing::debug!(
        "요청 Cookie 헤더 존재 여부: present={} path_sensitive_cookie_check=spentopia_refresh",
        raw_cookie.is_some()
    );

    let extracted = raw_cookie.and_then(|cookie_header| {
        Cookie::split_parse(cookie_header)
            .filter_map(Result::ok)
            .find(|c| c.name() == "spentopia_refresh")
            .map(|c| c.value().to_string())
    });

    tracing::debug!(
        "spentopia_refresh 쿠키 추출 결과: found={}",
        extracted.is_some()
    );

    extracted
}

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
        (status = 400, description = "지갑 주소가 비어있습니다")
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
        return Err((
            StatusCode::BAD_REQUEST,
            "지갑 주소를 입력해 주세요.".to_string(),
        ));
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
        (status = 200, description = "로그인 성공"),
        (status = 400, description = "필수 필드 비어있음"),
        (status = 401, description = "서명 검증 실패 또는 nonce 불일치"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn wallet_login(
    State(state): State<AppState>,
    Json(body): Json<WalletLoginRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    // 필수 필드 비어있는지 체크
    if body.wallet_address.trim().is_empty() {
        tracing::warn!("지갑 로그인: 지갑 주소 비어있음");
        return Err((
            StatusCode::BAD_REQUEST,
            "지갑 주소를 입력해 주세요.".to_string(),
        ));
    }
    if body.nonce.trim().is_empty() {
        tracing::warn!("지갑 로그인: nonce 비어있음");
        return Err((
            StatusCode::BAD_REQUEST,
            "nonce를 입력해 주세요.".to_string(),
        ));
    }
    if body.signature.trim().is_empty() {
        tracing::warn!("지갑 로그인: 서명 비어있음");
        return Err((StatusCode::BAD_REQUEST, "서명을 입력해 주세요.".to_string()));
    }

    // service에서 서명 검증 + 로그인 처리
    // 성공하면 LoginResponse(access_token, refresh_token, is_new_user) 반환
    // 실패하면 anyhow::Error가 올라옴
    let response = service::verify_and_login(
        &state,
        &body.wallet_address,
        &body.nonce,
        &body.signature,
        "web",
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

    let cookie_headers = build_refresh_cookie(&state, &response.refresh_token);

    let body = WebLoginResponse {
        access_token: response.access_token,
        is_new_user: response.is_new_user,
    };

    Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
}

pub async fn wallet_login_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<WalletLoginRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_app_request(&headers)?;

    if body.wallet_address.trim().is_empty() {
        tracing::warn!("앱 지갑 로그인: 지갑 주소 비어있음");
        return Err((
            StatusCode::BAD_REQUEST,
            "지갑 주소를 입력해 주세요.".to_string(),
        ));
    }
    if body.nonce.trim().is_empty() {
        tracing::warn!("앱 지갑 로그인: nonce 비어있음");
        return Err((
            StatusCode::BAD_REQUEST,
            "nonce를 입력해 주세요.".to_string(),
        ));
    }
    if body.signature.trim().is_empty() {
        tracing::warn!("앱 지갑 로그인: 서명 비어있음");
        return Err((StatusCode::BAD_REQUEST, "서명을 입력해 주세요.".to_string()));
    }

    let response = service::verify_and_login(
        &state,
        &body.wallet_address,
        &body.nonce,
        &body.signature,
        "app",
    )
    .await
    .map_err(|e| {
        let msg = e.to_string();
        if msg.contains("nonce") || msg.contains("서명") {
            tracing::warn!("앱 지갑 로그인 실패 (클라이언트): {}", msg);
            (StatusCode::UNAUTHORIZED, msg)
        } else {
            tracing::error!("앱 지갑 로그인 실패 (서버): {}", msg);
            (StatusCode::INTERNAL_SERVER_ERROR, msg)
        }
    })?;

    let body = AppLoginResponse {
        access_token: response.access_token,
        refresh_token: response.refresh_token,
        is_new_user: response.is_new_user,
    };

    Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
}

// ═══════════════════════════════════════════════════════════════
// [이메일/구글] Supabase 토큰 -> 우리 앱 토큰 교환
// POST /auth/exchange
// ═══════════════════════════════════════════════════════════════
//
// 프론트가 Supabase 로그인 성공 후 받은 access_token을 전달하면
// 백엔드가 Supabase user를 조회하고
// 우리 앱 access/refresh를 발급한다.
//
// 응답:
// [웹] access는 body, refresh는 쿠키
// [앱] access/refresh 둘 다 body
#[utoipa::path(
    post,
    path = "/auth/exchange",
    tag = "인증",
    request_body = ExchangeTokenRequest,
    responses(
        (status = 200, description = "토큰 교환 성공"),
        (status = 400, description = "access_token 비어있음"),
        (status = 401, description = "유효하지 않은 Supabase 토큰")
    )
)]
pub async fn exchange_token(
    State(state): State<AppState>,
    Json(body): Json<ExchangeTokenRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    if body.access_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "access_token을 입력해 주세요.".to_string(),
        ));
    }

    let issued = service::exchange_supabase_token(&state, &body.access_token, "web")
        .await
        .map_err(|e| {
            tracing::error!("토큰 교환 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    let cookie_headers = build_refresh_cookie(&state, &issued.refresh_token);
    tracing::debug!("토큰 교환 응답: web refresh 쿠키를 Set-Cookie로 반환");

    let body = WebLoginResponse {
        access_token: issued.access_token,
        is_new_user: issued.is_new_user,
    };

    Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
}

pub async fn exchange_token_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<ExchangeTokenRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_app_request(&headers)?;

    if body.access_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "access_token을 입력해 주세요.".to_string(),
        ));
    }

    let issued = service::exchange_supabase_token(&state, &body.access_token, "app")
        .await
        .map_err(|e| {
            tracing::error!("앱 토큰 교환 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    let body = AppLoginResponse {
        access_token: issued.access_token,
        refresh_token: issued.refresh_token,
        is_new_user: issued.is_new_user,
    };

    Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
}

// ═══════════════════════════════════════════════════════════════
// [내 인증 상태 확인] GET /me
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    get,
    path = "/me",
    tag = "인증",
    security(
        ("bearer_auth" = [])
    ),
    responses(
        (status = 200, description = "인증 성공"),
        (status = 401, description = "토큰 없음 또는 유효하지 않음"),
        (status = 404, description = "유저 없음")
    )
)]
pub async fn get_me(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=id,email,profile_completed,login_provider,nickname,phone,profile_image,wallet_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
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
        .map_err(|e| {
            tracing::error!("/me 유저 조회 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "사용자 조회에 실패했습니다.".to_string(),
            )
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::error!("/me 응답 실패: {}", err);
        return Err((StatusCode::INTERNAL_SERVER_ERROR, err));
    }

    let users: Vec<serde_json::Value> = resp.json().await.map_err(|e| {
        tracing::error!("/me 응답 파싱 실패: {}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "응답 파싱에 실패했습니다.".to_string(),
        )
    })?;

    let user = users.first().cloned().ok_or_else(|| {
        (
            StatusCode::NOT_FOUND,
            "사용자를 찾을 수 없습니다.".to_string(),
        )
    })?;

    Ok(Json(user))
}

// POST /auth/withdraw
//
// 보호 라우트 → JWT 필수
// 탈퇴 처리 후 refresh 쿠키도 삭제해서 즉시 로그아웃 상태로 만듦
pub async fn withdraw(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    service::withdraw_user(&state, user_id)
        .await
        .map_err(|e| {
            tracing::error!("회원탈퇴 실패: user_id={}, error={}", user_id, e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "회원탈퇴 처리에 실패했습니다.".to_string(),
            )
        })?;

    // 탈퇴 성공 → refresh 쿠키 삭제 (브라우저에서 쿠키 만료 처리)
    // access token은 프론트에서 메모리 삭제 처리
    let headers = build_clear_refresh_cookie(&state);

    Ok((headers, Json(json!({ "withdrawn": true }))))
}

// ═══════════════════════════════════════════════════════════════
// [프로필 완성] PATCH /profile/complete
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    patch,
    path = "/profile/complete",
    tag = "인증",
    security(
        ("bearer_auth" = [])
    ),
    request_body = CompleteProfileRequest,
    responses(
        (status = 200, description = "프로필 저장 성공", body = CompleteProfileResponse),
        (status = 400, description = "닉네임 또는 전화번호 비어있음"),
        (status = 401, description = "토큰 없음 또는 유효하지 않음"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn complete_profile(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
    Json(body): Json<CompleteProfileRequest>,
) -> Result<Json<CompleteProfileResponse>, (StatusCode, String)> {
    if body.nickname.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "닉네임을 입력해 주세요.".to_string(),
        ));
    }

    if body.phone.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "전화번호를 입력해 주세요.".to_string(),
        ));
    }

    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    // profile_image가 None이면 페이로드에서 제외.
    //
    // 이유: PATCH에 "profile_image": null을 포함하면
    // Supabase가 해당 컬럼을 NULL로 덮어씀.
    // 사용자가 이미지를 선택하지 않은 경우
    // 트리거가 삽입해 둔 "defaults/avatar.png"가 지워지는 버그 발생.
    // 필드를 아예 빼면 DB는 기존 값을 그대로 유지함.
    let mut payload = json!({
        "nickname": body.nickname,
        "phone": body.phone,
    });

    if let Some(ref img) = body.profile_image {
        payload["profile_image"] = json!(img);
    }

    let resp = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .map_err(|e| {
            tracing::error!("프로필 업데이트 요청 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "프로필 저장에 실패했습니다.".to_string(),
            )
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::error!("프로필 업데이트 실패: {}", err);

        if err.contains("users_nickname_key") {
            return Err((
                StatusCode::BAD_REQUEST,
                "이미 사용 중인 닉네임입니다".to_string(),
            ));
        }

        if err.contains("users_phone_key") {
            return Err((
                StatusCode::BAD_REQUEST,
                "이미 사용 중인 전화번호입니다".to_string(),
            ));
        }

        if err.contains("profile_image") && err.contains("schema cache") {
            return Err((
                StatusCode::BAD_REQUEST,
                "profile_image 컬럼 설정을 확인해주세요".to_string(),
            ));
        }

        return Err((StatusCode::INTERNAL_SERVER_ERROR, err));
    }

    let rows: Vec<serde_json::Value> = resp.json().await.map_err(|e| {
        tracing::error!("프로필 업데이트 응답 파싱 실패: {}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "응답 파싱에 실패했습니다.".to_string(),
        )
    })?;

    let updated = rows.first().cloned().unwrap_or_default();
    let profile_completed = updated["profile_completed"].as_bool().unwrap_or(false);

    Ok(Json(CompleteProfileResponse {
        success: true,
        profile_completed,
    }))
}

#[utoipa::path(
    post,
    path = "/profile/check-availability",
    tag = "인증",
    request_body = CheckProfileAvailabilityRequest,
    responses(
        (status = 200, description = "닉네임/전화번호 사용 가능"),
        (status = 400, description = "중복 또는 입력값 오류")
    )
)]
pub async fn check_nickname(
    State(state): State<AppState>,
    Json(body): Json<serde_json::Value>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    let nickname = body["nickname"]
        .as_str()
        .map(str::trim)
        .filter(|s| !s.is_empty())
        .ok_or_else(|| (StatusCode::BAD_REQUEST, "닉네임을 입력해 주세요.".to_string()))?;

    let available = crate::auth::service::check_nickname_available(&state, nickname)
        .await
        .map_err(|e| {
            tracing::error!("닉네임 중복 확인 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "닉네임 중복 확인에 실패했습니다.".to_string())
        })?;

    Ok(Json(json!({ "available": available })))
}

pub async fn check_profile_availability(
    State(state): State<AppState>,
    Json(body): Json<CheckProfileAvailabilityRequest>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    if body.nickname.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "닉네임을 입력해 주세요.".to_string(),
        ));
    }

    if body.phone.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "전화번호를 입력해 주세요.".to_string(),
        ));
    }

    service::check_profile_availability(&state, &body.nickname, &body.phone)
        .await
        .map_err(|e| {
            let msg = e.to_string();

            if msg.contains("이미 사용 중인 닉네임입니다")
                || msg.contains("이미 사용 중인 전화번호입니다")
            {
                return (StatusCode::BAD_REQUEST, msg);
            }

            tracing::error!("프로필 중복 확인 실패: {}", msg);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "프로필 중복 확인에 실패했습니다.".to_string(),
            )
        })?;

    Ok(Json(json!({ "available": true })))
}

// ─────────────────────────────────────────────────────────────
// 프로필 이미지 업로드
//
// 요청 형식:
// multipart/form-data
// field name = "file"
//
// 처리 흐름:
// 1) JWT에서 user_id 가져옴
// 2) 파일 타입 확인
// 3) 파일 크기 확인
// 4) service role key로 private bucket에 업로드
// 5) DB에 저장할 object path 반환
// ─────────────────────────────────────────────────────────────
pub async fn upload_profile_image(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
    mut multipart: Multipart,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    let bucket = &state.config.supabase_profile_image_bucket;

    let mut file_bytes: Option<Vec<u8>> = None;
    let mut content_type: Option<String> = None;
    let mut file_extension = "png".to_string();

    while let Some(field) = multipart.next_field().await.map_err(|e| {
        tracing::error!("multipart 파싱 실패: {}", e);
        (
            StatusCode::BAD_REQUEST,
            "멀티파트 파싱에 실패했습니다.".to_string(),
        )
    })? {
        let name = field.name().unwrap_or_default().to_string();

        if name == "file" {
            if let Some(ct) = field.content_type() {
                content_type = Some(ct.to_string());

                file_extension = match ct {
                    "image/png" => "png".to_string(),
                    "image/jpeg" | "image/jpg" => "jpg".to_string(),
                    "image/webp" => "webp".to_string(),
                    _ => {
                        return Err((
                            StatusCode::BAD_REQUEST,
                            "png, jpg, webp 이미지만 업로드 가능합니다".to_string(),
                        ));
                    }
                };
            }

            let bytes = field.bytes().await.map_err(|e| {
                tracing::error!("파일 읽기 실패: {}", e);
                (
                    StatusCode::BAD_REQUEST,
                    "파일 읽기에 실패했습니다.".to_string(),
                )
            })?;

            // 최대 5MB 제한
            if bytes.len() > 5 * 1024 * 1024 {
                return Err((
                    StatusCode::BAD_REQUEST,
                    "파일 크기는 5MB 이하여야 합니다.".to_string(),
                ));
            }

            file_bytes = Some(bytes.to_vec());
            break;
        }
    }

    let file_bytes =
        file_bytes.ok_or_else(|| (StatusCode::BAD_REQUEST, "file 필드가 없습니다".to_string()))?;

    let content_type = content_type.unwrap_or_else(|| "image/png".to_string());

    // 유저마다 고정 파일 경로 사용
    // 같은 유저가 다시 업로드하면 같은 경로를 덮어씀
    let object_path = format!("{}/avatar.{}", user_id, file_extension);

    let upload_url = format!(
        "{}/storage/v1/object/{}/{}",
        state.config.supabase_url.trim_end_matches('/'),
        bucket,
        object_path
    );

    let resp = state
        .http_client
        .post(&upload_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", content_type)
        .header("x-upsert", "true")
        .body(file_bytes)
        .send()
        .await
        .map_err(|e| {
            tracing::error!("스토리지 업로드 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "스토리지 업로드에 실패했습니다.".to_string(),
            )
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::error!("스토리지 업로드 응답 실패: {}", err);
        return Err((StatusCode::INTERNAL_SERVER_ERROR, err));
    }

    Ok(Json(json!({
        "path": object_path
    })))
}

// ─────────────────────────────────────────────────────────────
// private bucket 이미지 signed URL 발급
//
// 프론트는 DB에 저장된 path를 보내고,
// 백엔드는 signed URL을 만들어서 반환한다.
//
// URL은 24시간짜리로 줘서 쉽게 안 끊기게 함.
// 만료되면 프론트가 다시 요청하면 된다.
// ─────────────────────────────────────────────────────────────
pub async fn get_profile_image_signed_url(
    State(state): State<AppState>,
    axum::Extension(user_id): axum::Extension<uuid::Uuid>,
    Query(query): Query<ProfileImageUrlQuery>,
) -> Result<Json<ProfileImageUrlResponse>, (StatusCode, String)> {
    let bucket = &state.config.supabase_profile_image_bucket;

    let requested_path = query.path.trim();

    if requested_path.is_empty() {
        return Err((StatusCode::BAD_REQUEST, "path가 비어 있습니다".to_string()));
    }

    // 단순 경로 오용 방지
    if requested_path.contains("..") || requested_path.contains('\\') {
        return Err((
            StatusCode::BAD_REQUEST,
            "올바르지 않은 path 입니다".to_string(),
        ));
    }

    // 기본 이미지(defaults/) 또는 현재 로그인한 유저 폴더만 허용
    let allowed_user_prefix = format!("{}/", user_id);
    let is_default_image = requested_path.starts_with("defaults/");
    let is_user_owned_image = requested_path.starts_with(&allowed_user_prefix);

    if !is_default_image && !is_user_owned_image {
        tracing::warn!(
            "signed URL 권한 없음: user_id={}, requested_path={}",
            user_id,
            requested_path
        );
        return Err((
            StatusCode::FORBIDDEN,
            "해당 이미지에 접근할 수 없습니다".to_string(),
        ));
    }

    let url = format!(
        "{}/storage/v1/object/sign/{}/{}",
        state.config.supabase_url.trim_end_matches('/'),
        bucket,
        requested_path
    );

    let body = json!({
        "expiresIn": 86400
    });

    let resp = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", "application/json")
        .json(&body)
        .send()
        .await
        .map_err(|e| {
            tracing::error!("signed URL 생성 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "서명된 URL 생성에 실패했습니다.".to_string(),
            )
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::error!("signed URL 응답 실패: {}", err);
        return Err((StatusCode::INTERNAL_SERVER_ERROR, err));
    }

    let data: serde_json::Value = resp.json().await.map_err(|e| {
        tracing::error!("signed URL 응답 파싱 실패: {}", e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "서명된 URL 응답 파싱에 실패했습니다.".to_string(),
        )
    })?;

    let signed_path = data["signedURL"].as_str().ok_or_else(|| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "signedURL 필드가 없습니다".to_string(),
        )
    })?;

    let signed_url = format!(
        "{}/storage/v1{}",
        state.config.supabase_url.trim_end_matches('/'),
        signed_path
    );

    Ok(Json(ProfileImageUrlResponse { signed_url }))
}

// ═══════════════════════════════════════════════════════════════
// [이메일 찾기] POST /auth/find-email
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    post,
    path = "/auth/find-email",
    tag = "인증",
    request_body = FindEmailRequest,
    responses(
        (status = 200, description = "이메일 찾기 성공", body = FindEmailResponse),
        (status = 404, description = "해당 전화번호로 등록된 계정이 없습니다")
    )
)]
pub async fn find_email(
    State(state): State<AppState>,
    Json(body): Json<FindEmailRequest>,
) -> Result<Json<FindEmailResponse>, (StatusCode, String)> {
    // 1) captcha token 비어있는지 검사
    if body.captcha_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "사람 인증이 필요합니다.".to_string(),
        ));
    }

    // 2) Turnstile 검증
    verify_turnstile(
        &state.http_client,
        &state.config.turnstile_secret_key,
        &body.captcha_token,
    )
        .await
        .map_err(|e| {
            tracing::warn!("Turnstile 검증 실패: {}", e);
            (StatusCode::BAD_REQUEST, e.to_string())
        })?;

    // 3) 전화번호 값 검사
    if body.phone.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "전화번호를 입력해 주세요.".to_string(),
        ));
    }

    // 4) service 호출
    // 반환값:
    // - masked_email
    // - login_provider
    // - google_connected
    let (masked_email, provider, google_connected) =
        crate::auth::service::find_email_by_phone(&state, &body.phone)
            .await
            .map_err(|e| {
                let msg = e.to_string();
                (StatusCode::NOT_FOUND, msg)
            })?;

    // 5) 안내 메시지 생성
    let message = match provider.as_str() {
        "email" if google_connected => {
            "이 계정은 이메일 로그인과 구글 로그인을 모두 사용할 수 있습니다."
        }
        "email" => {
            "이메일 로그인 계정입니다."
        }
        "google" => {
            "구글 로그인으로 가입된 계정입니다."
        }
        "kakao" => {
            "카카오 로그인으로 가입된 계정입니다. 카카오 로그인을 이용해주세요."
        }
        _ => {
            "로그인 방식 정보를 확인할 수 없습니다."
        }
    };

    Ok(Json(FindEmailResponse {
        masked_email,
        login_provider: provider,
        google_connected,
        message: message.to_string(),
    }))
}

// ═══════════════════════════════════════════════════════════════
// [이메일 존재 여부 확인] POST /auth/check-email
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    post,
    path = "/auth/check-email",
    tag = "인증",
    request_body = CheckEmailRequest,
    responses(
        (status = 200, description = "이미 사용중인 이메일이 존재합니다"),
        (status = 404, description = "해당 이메일로 가입된 계정이 없습니다")
    )
)]
pub async fn check_email(
    State(state): State<AppState>,
    Json(body): Json<CheckEmailRequest>,
) -> Result<Json<CheckEmailResponse>, (StatusCode, String)> {
    // 1) captcha token 비어있는지 검사
    if body.captcha_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "사람 인증이 필요합니다.".to_string(),
        ));
    }

    // 2) Turnstile 검증
    verify_turnstile(
        &state.http_client,
        &state.config.turnstile_secret_key,
        &body.captcha_token,
    )
        .await
        .map_err(|e| {
            tracing::warn!("Turnstile 검증 실패(check_email): {}", e);
            (
                StatusCode::BAD_REQUEST,
                "사람 인증 검증에 실패했습니다.".to_string(),
            )
        })?;

    // 3) 이메일 값 검사
    let email = body.email.trim().to_lowercase();
    if email.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "이메일을 입력해 주세요.".to_string(),
        ));
    }

    // 4) 가입 차단 도메인 검사
    let domain = email.split('@').nth(1).unwrap_or("");
    if domain == "admin.com" {
        return Err((
            StatusCode::FORBIDDEN,
            "해당 이메일 도메인으로는 가입할 수 없습니다.".to_string(),
        ));
    }

    // 5) 탈퇴 유저 재가입 차단 + 이메일 중복 확인 통합 처리
    //
    // 케이스별 처리:
    // 1) 이메일 없음 (rows 비어있음)          → 404 (가입 가능)
    // 2) 이메일 있고 deleted_at IS NULL       → 200 exists:true (이미 가입된 이메일)
    // 3) 이메일 있고 deleted_at IS NOT NULL   → 403 (탈퇴한 회원, 재가입 불가)
    let withdrawn_check_url = format!(
        "{}/rest/v1/users?select=deleted_at&email=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        urlencoding::encode(&email)
    );

    let withdrawn_resp = state
        .http_client
        .get(&withdrawn_check_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .map_err(|e| {
            tracing::error!("이메일 조회 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "이메일 확인에 실패했습니다.".to_string(),
            )
        })?;

    let rows: Vec<serde_json::Value> = withdrawn_resp.json().await.unwrap_or_default();

    if let Some(row) = rows.first() {
        if row["deleted_at"].is_string() {
            // 탈퇴한 유저 → 재가입 차단
            return Err((
                StatusCode::FORBIDDEN,
                "이미 탈퇴한 회원입니다.".to_string(),
            ));
        }
        // 탈퇴 안 한 기존 유저 → 이미 가입된 이메일
        return Ok(Json(CheckEmailResponse { exists: true }));
    }

    // 이메일 자체가 없음 → 가입 가능
    Err((
        StatusCode::NOT_FOUND,
        "가입 가능한 이메일입니다.".to_string(),
    ))
}

#[utoipa::path(
    post,
    path = "/auth/check-reset-password-email",
    tag = "인증",
    request_body = CheckEmailRequest,
    responses(
        (status = 200, description = "비밀번호 재설정 가능한 이메일입니다"),
        (status = 403, description = "소셜 로그인 계정은 비밀번호 재설정을 할 수 없습니다"),
        (status = 404, description = "해당 이메일로 가입된 계정이 없습니다")
    )
)]
pub async fn check_reset_password_email(
    State(state): State<AppState>,
    Json(body): Json<CheckResetPasswordEmailRequest>,
) -> Result<Json<CheckResetPasswordEmailResponse>, (StatusCode, String)> {
    // 1) captcha token 비어있는지 검사
    if body.captcha_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "사람 인증이 필요합니다.".to_string(),
        ));
    }

    // 2) Turnstile 검증
    verify_turnstile(
        &state.http_client,
        &state.config.turnstile_secret_key,
        &body.captcha_token,
    )
        .await
        .map_err(|e| {
            tracing::warn!("Turnstile 검증 실패(check_reset_password_email): {}", e);
            (
                StatusCode::BAD_REQUEST,
                "사람 인증 검증에 실패했습니다.".to_string(),
            )
        })?;

    // 3) 이메일 값 검사
    let email = body.email.trim().to_lowercase();
    if email.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "이메일을 입력해 주세요.".to_string(),
        ));
    }

    // 4) 기존 비밀번호 재설정 가능 여부 확인 로직 호출
    //
    // 여기 서비스 함수는 네 프로젝트 함수명에 맞게 바꾸면 됨.
    // 기대하는 동작:
    // - 일반 이메일 계정이고 존재함 -> Ok(true)
    // - 존재하지 않음 -> Ok(false)
    // - 소셜 로그인 계정이라 재설정 불가 -> Err(anyhow!("소셜 로그인 계정..."))
    let exists = crate::auth::service::check_reset_password_email(&state, &email)
        .await
        .map_err(|e| {
            let msg = e.to_string();
            tracing::warn!("비밀번호 재설정 이메일 확인 실패: {}", msg);

            if msg.contains("소셜 로그인") {
                (
                    StatusCode::FORBIDDEN,
                    "소셜 로그인 계정은 비밀번호 재설정을 할 수 없습니다. 해당 소셜 로그인으로 다시 로그인해주세요.".to_string(),
                )
            } else {
                (
                    StatusCode::NOT_FOUND,
                    "입력한 정보와 일치하는 계정을 찾을 수 없습니다.".to_string(),
                )
            }
        })?;

    if exists {
        Ok(Json(CheckResetPasswordEmailResponse { exists: true }))
    } else {
        Err((
            StatusCode::NOT_FOUND,
            "입력한 정보와 일치하는 계정을 찾을 수 없습니다.".to_string(),
        ))
    }
}

// ═══════════════════════════════════════════════════════════════
// [카카오 로그인] POST /auth/kakao/login
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    post,
    path = "/auth/kakao/login",
    tag = "소셜 로그인",
    request_body = KakaoLoginRequest,
    responses(
        (status = 200, description = "로그인 성공"),
        (status = 400, description = "인가 코드 비어있음"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn kakao_login(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<KakaoLoginRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    if body.code.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "인가 코드가 비어있음".to_string()));
    }

    if body.state.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "state가 비어있음".to_string()));
    }

    let cookie_state = extract_kakao_state_cookie(&headers)
        .ok_or((StatusCode::UNAUTHORIZED, "OAuth state 쿠키가 없습니다.".to_string()))?;

    if cookie_state != body.state {
        tracing::warn!(
            "카카오 OAuth state 불일치: cookie_state={}, body_state={}",
            cookie_state,
            body.state
        );
        return Err((StatusCode::UNAUTHORIZED, "유효하지 않은 로그인 요청입니다.".to_string()));
    }

    let issued = service::kakao_login(&state, &body.code, "web")
        .await
        .map_err(|e| {
            let msg = e.to_string();
            tracing::error!("카카오 로그인 실패: {}", msg);
            (StatusCode::INTERNAL_SERVER_ERROR, msg)
        })?;

    let mut merged_headers = build_clear_kakao_state_cookie(&state);
    let refresh_headers = build_refresh_cookie(&state, &issued.refresh_token);

    for value in refresh_headers.get_all(SET_COOKIE).iter() {
        merged_headers.append(SET_COOKIE, value.clone());
    }

    let body = WebLoginResponse {
        access_token: issued.access_token,
        is_new_user: issued.is_new_user,
    };

    Ok((merged_headers, Json(serde_json::to_value(body).unwrap())))
}

#[utoipa::path(
    post,
    path = "/auth/kakao/start",
    tag = "소셜 로그인",
    responses(
        (status = 200, description = "카카오 인가 URL 생성 성공", body = KakaoStartResponse)
    )
)]
pub async fn kakao_start(
    State(state): State<AppState>,
) -> Result<(HeaderMap, Json<KakaoStartResponse>), (StatusCode, String)> {
    let oauth_state = uuid::Uuid::new_v4().to_string();

    let auth_url = format!(
        "https://kauth.kakao.com/oauth/authorize?client_id={}&redirect_uri={}&response_type=code&scope=profile_nickname,profile_image&prompt=select_account&state={}",
        state.config.kakao_rest_api_key,
        urlencoding::encode(&state.config.kakao_redirect_uri),
        oauth_state
    );

    let cookie_headers = build_kakao_state_cookie(&state, &oauth_state);

    Ok((cookie_headers, Json(KakaoStartResponse { auth_url })))
}

// ═══════════════════════════════════════════════════════════════
// [refresh] POST /auth/refresh
// ═══════════════════════════════════════════════════════════════
//
// access token이 만료됐을 때 호출
//
// [웹]
// - refresh 쿠키에서 refresh token 추출
// - 응답 body에는 새 access만 반환
// - 새 refresh는 다시 쿠키로 발급
//
// [앱]
// - body.refresh_token 사용
// - 새 access/refresh 둘 다 body로 반환
#[utoipa::path(
    post,
    path = "/auth/refresh",
    tag = "인증",
    request_body = RefreshRequest,
    responses(
        (status = 200, description = "토큰 재발급 성공"),
        (status = 401, description = "유효하지 않은 refresh 토큰")
    )
)]
pub async fn refresh_token(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(_body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    tracing::debug!("refresh 요청 수신: client_type=web");

    let refresh_token = extract_refresh_cookie(&headers).ok_or_else(|| {
        (
            StatusCode::UNAUTHORIZED,
            "refresh 쿠키가 없습니다.".to_string(),
        )
    })?;

    let rotated = service::rotate_refresh_token(&state, &refresh_token, "web")
        .await
        .map_err(|e| {
            tracing::warn!("refresh 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    let cookie_headers = build_refresh_cookie(&state, &rotated.refresh_token);
    tracing::debug!("refresh 성공 응답: web refresh 쿠키 재발급");

    let body = WebRefreshResponse {
        access_token: rotated.access_token,
    };

    Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
}

pub async fn refresh_token_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_app_request(&headers)?;
    tracing::debug!("refresh 요청 수신: client_type=app");

    let refresh_token = body
        .refresh_token
        .filter(|v| !v.trim().is_empty())
        .ok_or_else(|| {
            (
                StatusCode::BAD_REQUEST,
                "refresh_token을 입력해 주세요.".to_string(),
            )
        })?;

    let rotated = service::rotate_refresh_token(&state, &refresh_token, "app")
        .await
        .map_err(|e| {
            tracing::warn!("앱 refresh 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    let body = AppRefreshResponse {
        access_token: rotated.access_token,
        refresh_token: rotated.refresh_token,
    };

    Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
}

// ═══════════════════════════════════════════════════════════════
// [logout] POST /auth/logout
// ═══════════════════════════════════════════════════════════════
//
// 웹:
// - refresh 쿠키 삭제
// - 해당 refresh session revoke
//
// 앱:
// - body.refresh_token 기반으로 해당 refresh session revoke
//
// access token은 DB 저장이 아니므로 서버에서 별도 폐기 대상이 아님.
// 대신 refresh를 revoke해서 "더 이상 연장"을 막는 구조다.
#[utoipa::path(
    post,
    path = "/auth/logout",
    tag = "인증",
    request_body = RefreshRequest,
    responses(
        (status = 200, description = "로그아웃 성공")
    )
)]
pub async fn logout(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(_body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    let refresh_token = extract_refresh_cookie(&headers);

    // refresh 토큰이 있다면 해당 세션 revoke 시도
    if let Some(token) = refresh_token {
        if let Ok(claims) =
            crate::auth::app_jwt::verify_app_refresh_token(&state.config.app_jwt_secret, &token)
        {
            if let Ok(session_id) = uuid::Uuid::parse_str(&claims.sid) {
                let _ =
                    crate::auth::refresh_store::revoke_refresh_session(&state, session_id, None)
                        .await;
            }
        }
    }

    let headers = build_clear_refresh_cookie(&state);
    Ok((headers, Json(json!({ "logged_out": true }))))
}

pub async fn logout_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_app_request(&headers)?;

    if let Some(token) = body.refresh_token {
        if let Ok(claims) =
            crate::auth::app_jwt::verify_app_refresh_token(&state.config.app_jwt_secret, &token)
        {
            if let Ok(session_id) = uuid::Uuid::parse_str(&claims.sid) {
                let _ =
                    crate::auth::refresh_store::revoke_refresh_session(&state, session_id, None)
                        .await;
            }
        }
    }

    Ok((HeaderMap::new(), Json(json!({ "logged_out": true }))))
}

fn build_kakao_state_cookie(app_state: &AppState, state: &str) -> HeaderMap {
    let mut headers = HeaderMap::new();
    let secure = should_use_secure_cookies(app_state);

    let cookie = Cookie::build(("spentopia_kakao_oauth_state", state.to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth")
        .secure(secure)
        .build();

    headers.append(SET_COOKIE, cookie.to_string().parse().unwrap());
    headers
}

fn build_clear_kakao_state_cookie(app_state: &AppState) -> HeaderMap {
    let mut headers = HeaderMap::new();
    let secure = should_use_secure_cookies(app_state);

    let cookie = Cookie::build(("spentopia_kakao_oauth_state", "".to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth")
        .secure(secure)
        .max_age(cookie::time::Duration::seconds(0))
        .build();

    headers.append(SET_COOKIE, cookie.to_string().parse().unwrap());
    headers
}

fn extract_kakao_state_cookie(headers: &HeaderMap) -> Option<String> {
    headers
        .get("cookie")
        .and_then(|v| v.to_str().ok())
        .and_then(|cookie_header| {
            Cookie::split_parse(cookie_header)
                .filter_map(Result::ok)
                .find(|c| c.name() == "spentopia_kakao_oauth_state")
                .map(|c| c.value().to_string())
        })
}
fn should_use_secure_cookies(state: &AppState) -> bool {
    matches!(
        state.config.environment.trim().to_ascii_lowercase().as_str(),
        "prod" | "production"
    )
}

// ═══════════════════════════════════════════════════════════════
// [handoff 발급] POST /auth/handoff
// ═══════════════════════════════════════════════════════════════
//
// 보호 라우트 → JWT 필수
//
// 왜 보호 라우트냐?
// - 누구의 handoff token인지 알아야 하므로
// - 반드시 현재 로그인된 user_id가 필요함
//
// 요청 예시:
// POST /auth/handoff
// Authorization: Bearer <앱 access token>
// {
//   "target_service": "unity"
// }
//
// 응답 예시:
// 200 OK
// {
//   "handoff_token": "64자리 랜덤문자열",
//   "expires_in": 30
// }
//
// 프론트는 이 handoff_token을 URL에 넣지 않고
// 부모 탭 -> 유니티 탭 postMessage로만 전달해야 함.
#[utoipa::path(
    post,
    path = "/auth/handoff",
    tag = "Handoff",
    security(
        ("bearer_auth" = [])
    ),
    request_body = HandoffRequest,
    responses(
        (status = 200, description = "handoff token 발급 성공", body = HandoffResponse),
        (status = 400, description = "target_service가 비어있거나 허용되지 않음"),
        (status = 401, description = "JWT 인증 실패")
    )
)]
pub async fn create_handoff(
    State(state): State<AppState>,
    // jwt_middleware가 넣어준 user_id
    axum::Extension(user_id): axum::Extension<Uuid>,
    Json(body): Json<HandoffRequest>,
) -> Result<Json<HandoffResponse>, (StatusCode, String)> {
    // ── 1) target_service 검증 ──────────────────────────────
    // 현재는 "unity"만 허용
    // 나중에 다른 서비스 추가 시 여기에 추가
    let target = body.target_service.trim().to_lowercase();

    if target.is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "target_service를 입력해 주세요.".to_string(),
        ));
    }

    let allowed_services = ["unity"];
    if !allowed_services.contains(&target.as_str()) {
        return Err((
            StatusCode::BAD_REQUEST,
            format!("허용되지 않은 target_service: {}", target),
        ));
    }

    // ── 2) handoff token 발급 ───────────────────────────────
    let token = create_handoff_token(&state, user_id, &target);

    // ── 3) 응답 ─────────────────────────────────────────────
    Ok(Json(HandoffResponse {
        handoff_token: token,
        expires_in: 30,
    }))
}

// ═══════════════════════════════════════════════════════════════
// [handoff 교환] POST /auth/handoff/exchange
// ═══════════════════════════════════════════════════════════════
//
// 공개 라우트 → JWT 불필요
//
// 이유:
// - 유니티는 아직 access token이 없는 상태에서 시작함
// - 부모 탭이 postMessage로 넘긴 handoff token만 가지고 교환해야 함
//
// 요청 예시:
// POST /auth/handoff/exchange
// {
//   "handoff_token": "aB3kQ9xZ2mL7pR4w...(64자리)"
// }
//
// 응답 예시:
// 200 OK
// {
//   "access_token": "eyJhbGci...",
//   "refresh_token": "eyJhbGci..."
// }
//
// 유니티는 이 토큰들을 메모리에 저장하고:
// - access_token으로 API 호출
// - refresh_token으로 /auth/refresh (body 방식)로 세션 연장
// - 이 시점부터 부모 탭(웹) 닫아도 유니티 독립 운영 가능
#[utoipa::path(
    post,
    path = "/auth/handoff/exchange",
    tag = "Handoff",
    request_body = HandoffExchangeRequest,
    responses(
        (status = 200, description = "교환 성공 — 유니티용 access+refresh 발급", body = HandoffExchangeResponse),
        (status = 400, description = "handoff_token 비어있음"),
        (status = 401, description = "유효하지 않은 handoff token")
    )
)]
pub async fn exchange_handoff(
    State(state): State<AppState>,
    Json(body): Json<HandoffExchangeRequest>,
) -> Result<Json<HandoffExchangeResponse>, (StatusCode, String)> {
    // ── 1) 입력 검증 ────────────────────────────────────────
    if body.handoff_token.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "handoff_token을 입력해 주세요.".to_string(),
        ));
    }

    // ── 2) handoff token 교환 ───────────────────────────────
    // 검증 + 1회용 삭제 + 유니티용 access+refresh 발급
    //
    // 주의:
    // target_service는 클라이언트 입력을 믿지 않고
    // 서버가 handoff_store에 저장해둔 값을 기준으로 검증한다.
    let result = exchange_handoff_token(&state, &body.handoff_token)
        .await
        .map_err(|e| {
            let msg = e.to_string();
            tracing::warn!("handoff 교환 실패: {}", msg);
            (StatusCode::UNAUTHORIZED, msg)
        })?;

    // ── 3) 응답 ─────────────────────────────────────────────
    // 유니티는 앱 방식이므로 access+refresh 둘 다 body로 반환
    Ok(Json(HandoffExchangeResponse {
        access_token: result.access_token,
        refresh_token: result.refresh_token,
    }))
}
