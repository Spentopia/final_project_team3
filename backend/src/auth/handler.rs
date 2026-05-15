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
//
// complete_profile 핸들러에 닉네임 검증 추가
// - filter::validate_nickname() 으로 길이 + 금칙어 한번에 검사
// - 실패 시 400 + 구체적인 에러 메시지 반환

use axum::{
    Json,
    extract::{Extension, Multipart, Query, State},
    http::{HeaderMap, HeaderValue, StatusCode, header::SET_COOKIE},
    response::{IntoResponse, Redirect, Response},
};

use crate::auth::handoff::{create_handoff_token, exchange_handoff_token};
use crate::auth::turnstile::verify_turnstile;
use cookie::{Cookie, SameSite};
use serde_json::json;
use uuid::Uuid;

use crate::auth::dto::{
    AppKakaoStartResponse, AppLoginResponse, AppRefreshResponse, CheckEmailRequest,
    CheckEmailResponse, CheckProfileAvailabilityRequest, CheckResetPasswordEmailRequest,
    CheckResetPasswordEmailResponse, CompleteProfileRequest, CompleteProfileResponse,
    ExchangeTokenRequest, FindEmailRequest, FindEmailResponse, HandoffExchangeRequest,
    HandoffExchangeResponse, HandoffRequest, HandoffResponse, KakaoLoginRequest,
    KakaoStartResponse, NonceRequest, NonceResponse, ProfileImageUrlQuery, ProfileImageUrlResponse,
    RefreshRequest, WalletLoginRequest, WebLoginResponse, WebRefreshResponse, WebviewCallbackQuery,
    WebviewIssueRequest, WebviewIssueResponse,
};
use crate::auth::service;
use crate::auth::webview::{consume_webview_token, create_webview_token, sanitize_redirect_path};
use crate::filter;
use crate::state::AppState;
use utoipa;

fn ensure_app_request(headers: &HeaderMap) -> Result<(), (StatusCode, String)> {
    // origin: 브라우저 cross-origin 요청 시 자동 포함
    // sec-fetch-site: 브라우저 same-origin 포함 모든 fetch/XHR에 자동 포함, JS 위조 불가
    // 둘 중 하나라도 있으면 브라우저 요청으로 간주하여 차단
    // /auth/handoff/exchange는 ensure_app_request를 사용하지 않으므로 이 차단 대상 외
    if headers.contains_key("origin") || headers.contains_key("sec-fetch-site") {
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

fn ensure_unity_request(headers: &HeaderMap) -> Result<(), (StatusCode, String)> {
    // origin: 브라우저 cross-origin 요청 시 자동 포함
    // sec-fetch-site: 브라우저 same-origin 포함 모든 fetch/XHR에 자동 포함, JS 위조 불가
    // 둘 중 하나라도 있으면 브라우저 요청으로 간주하여 차단
    if headers.contains_key("origin") || headers.contains_key("sec-fetch-site") {
        return Err((
            StatusCode::FORBIDDEN,
            "브라우저에서는 사용할 수 없는 유니티 전용 인증 엔드포인트입니다.".to_string(),
        ));
    }

    match headers
        .get("x-client-type")
        .and_then(|v| v.to_str().ok())
        .map(|v| v.to_ascii_lowercase())
        .as_deref()
    {
        Some("unity") => Ok(()),
        _ => Err((
            StatusCode::BAD_REQUEST,
            "유니티 전용 인증 요청에는 X-Client-Type: unity 헤더가 필요합니다.".to_string(),
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

// ─────────────────────────────────────────────────────────────
// 인증 service 에러 → HTTP 상태코드 변환
// ─────────────────────────────────────────────────────────────
//
// auth/service.rs에서 올라오는 에러를 handler에서 HTTP 응답으로 바꾼다.
//
// 401 Unauthorized:
// - access token / refresh token이 없거나 잘못됨
// - 인증 자체가 실패한 경우
//
// 403 Forbidden:
// - 계정은 식별됐지만 서비스 이용이 금지된 경우
// - 예: 탈퇴 계정, 비활성화 계정
//
// 왜 이 함수를 따로 두나?
// - exchange_token, refresh_token, kakao_login 등에서 같은 분기 로직을 반복하지 않기 위해
// - 프론트에서 "비활성화된 계정입니다..." 메시지를 toast로 정확히 띄울 수 있게 하기 위해
fn map_auth_service_error(error: anyhow::Error) -> Response {
    let msg = error.to_string();

    // 비활성 계정 에러.
    //
    // service.rs에서 다음 형태로 올린다:
    // ACCOUNT_INACTIVE|{reason}|{inactive_until_text}
    //
    // 예:
    // ACCOUNT_INACTIVE|부적절한 게시글 반복 작성|2026.05.20 18:00
    if let Some(rest) = msg.strip_prefix("ACCOUNT_INACTIVE|") {
        let mut parts = rest.splitn(2, '|');

        let reason = parts
            .next()
            .map(str::trim)
            .filter(|v| !v.is_empty())
            .unwrap_or("운영정책 위반");

        let inactive_until_text = parts
            .next()
            .map(str::trim)
            .filter(|v| !v.is_empty())
            .unwrap_or("미정");

        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({
                "code": "ACCOUNT_INACTIVE",
                "message": "비활성화된 계정입니다.",
                "reason": reason,
                "inactive_until_text": inactive_until_text,
                "support_email": "spentopia.official@gmail.com"
            })),
        )
            .into_response();
    }

    // 탈퇴 계정.
    //
    // 탈퇴는 상세 박스까지는 필요 없고,
    // 짧은 메시지만 내려줘도 충분하다.
    if msg.contains("탈퇴한 계정") {
        return (
            StatusCode::FORBIDDEN,
            Json(serde_json::json!({
                "code": "ACCOUNT_WITHDRAWN",
                "message": msg
            })),
        )
            .into_response();
    }

    // 그 외 인증 실패.
    (
        StatusCode::UNAUTHORIZED,
        Json(serde_json::json!({
            "code": "AUTH_FAILED",
            "message": msg
        })),
    )
        .into_response()
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
    let message = service::build_wallet_sign_message(&body.wallet_address, &nonce);

    // 200 OK + nonce 반환
    Ok(Json(NonceResponse { nonce, message }))
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
        // service에서 올라온 에러를 HTTP 상태코드로 변환
        //
        // 케이스 분기:
        // - nonce/서명 관련 → 401 (인증 실패, 클라이언트 잘못)
        // - 연동된 계정 없음 → 404 (탈퇴자 + 미연동 모두 포함)
        //   ※ 보안상 둘을 구분해서 알려주지 않음
        //   ※ 탈퇴자도 wallet_address가 NULL이라 자연스럽게 여기로 떨어짐
        // - 그 외 → 500 (Supabase API 실패 등 서버 잘못)
        let msg = e.to_string();
        if msg.contains("nonce") || msg.contains("서명") {
            tracing::warn!("지갑 로그인 실패 (인증): {}", msg);
            (StatusCode::UNAUTHORIZED, msg)
        } else if msg.contains("비활성화된 계정") || msg.contains("탈퇴한 계정") {
            tracing::warn!("지갑 로그인 차단 (계정 상태): {}", msg);
            (StatusCode::FORBIDDEN, msg)
        } else if msg.contains("연동된 계정이 없습니다") {
            tracing::info!(
                "지갑 로그인 실패 (미연동/탈퇴): wallet={}",
                body.wallet_address
            );
            (StatusCode::NOT_FOUND, msg)
        } else {
            tracing::error!("지갑 로그인 실패 (서버): {}", msg);
            (StatusCode::INTERNAL_SERVER_ERROR, msg)
        }
    })?;

    tracing::info!("지갑 로그인 성공: wallet={}", body.wallet_address);

    let cookie_headers = build_refresh_cookie(&state, &response.refresh_token);

    let body = WebLoginResponse {
        access_token: response.access_token,
        is_new_user: response.is_new_user,
        role_type: "user".to_string(),
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
        // service에서 올라온 에러를 HTTP 상태코드로 변환
        //
        // 케이스 분기:
        // - nonce/서명 관련         → 401 (인증 실패)
        // - 연동된 계정 없음         → 404 (탈퇴자 + 미연동)
        // - 회원가입 미완료          → 403 (앱 전용 차단)
        // - 그 외                   → 500 (서버 에러)
        let msg = e.to_string();
        if msg.contains("nonce") || msg.contains("서명") {
            tracing::warn!("앱 지갑 로그인 실패 (인증): {}", msg);
            (StatusCode::UNAUTHORIZED, msg)
        } else if msg.contains("비활성화된 계정") || msg.contains("탈퇴한 계정") {
            tracing::warn!("앱 지갑 로그인 차단 (계정 상태): {}", msg);
            (StatusCode::FORBIDDEN, msg)
        } else if msg.contains("연동된 계정이 없습니다") {
            tracing::info!(
                "앱 지갑 로그인 실패 (미연동/탈퇴): wallet={}",
                body.wallet_address
            );
            (StatusCode::NOT_FOUND, msg)
        } else if msg.contains("웹에서 회원가입 완료 후 다시 이용해 주세요.") {
            tracing::warn!("앱 지갑 로그인 차단 (가입 미완료): {}", msg);
            (StatusCode::FORBIDDEN, msg)
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
) -> Response {
    if body.access_token.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({
                "code": "BAD_REQUEST",
                "message": "access_token을 입력해 주세요."
            })),
        )
            .into_response();
    }

    let issued = match service::exchange_supabase_token(&state, &body.access_token, "web").await {
        Ok(issued) => issued,
        Err(e) => {
            tracing::error!("토큰 교환 실패: {}", e);
            return map_auth_service_error(e);
        }
    };

    let cookie_headers = build_refresh_cookie(&state, &issued.refresh_token);
    tracing::debug!("토큰 교환 응답: web refresh 쿠키를 Set-Cookie로 반환");

    let role_type = match service::get_user_role(&state, issued.user_id).await {
        Ok(role) => role.unwrap_or_else(|| "user".to_string()),
        Err(e) => {
            tracing::error!(
                "이메일 로그인 role_type 조회 실패: user_id={}, error={}",
                issued.user_id,
                e
            );

            return (
                StatusCode::INTERNAL_SERVER_ERROR,
                Json(serde_json::json!({
                    "code": "ROLE_LOOKUP_FAILED",
                    "message": "사용자 권한 조회에 실패했습니다."
                })),
            )
                .into_response();
        }
    };

    let body = WebLoginResponse {
        access_token: issued.access_token,
        is_new_user: issued.is_new_user,
        role_type,
    };

    (cookie_headers, Json(serde_json::to_value(body).unwrap())).into_response()
}

pub async fn exchange_token_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<ExchangeTokenRequest>,
) -> Response {
    // 앱 전용 엔드포인트 검증.
    //
    // ensure_app_request()는 기존에 Result<(), (StatusCode, String)>를 반환하므로,
    // 여기서는 match로 받아서 Response로 직접 변환한다.
    if let Err((status, message)) = ensure_app_request(&headers) {
        return (
            status,
            Json(serde_json::json!({
                "code": "APP_REQUEST_INVALID",
                "message": message
            })),
        )
            .into_response();
    }

    if body.access_token.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({
                "code": "BAD_REQUEST",
                "message": "access_token을 입력해 주세요."
            })),
        )
            .into_response();
    }

    let issued = match service::exchange_supabase_token(&state, &body.access_token, "app").await {
        Ok(issued) => issued,
        Err(e) => {
            let msg = e.to_string();
            tracing::error!("앱 토큰 교환 실패: {}", msg);

            // 앱 전용 정책:
            // 모바일 앱은 프로필 완성 전 사용을 막는다.
            if msg.contains("웹에서 회원가입 완료 후 다시 이용해 주세요.") {
                return (
                    StatusCode::FORBIDDEN,
                    Json(serde_json::json!({
                        "code": "PROFILE_INCOMPLETE",
                        "message": msg
                    })),
                )
                    .into_response();
            }

            // 비활성/탈퇴/일반 인증 실패는 공통 JSON 응답으로 변환.
            return map_auth_service_error(e);
        }
    };

    let body = AppLoginResponse {
        access_token: issued.access_token,
        refresh_token: issued.refresh_token,
        is_new_user: issued.is_new_user,
    };

    (HeaderMap::new(), Json(serde_json::to_value(body).unwrap())).into_response()
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
        "{}/rest/v1/users?id=eq.{}&select=id,email,profile_completed,login_provider,nickname,phone,profile_image,wallet_address,role_type",
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
    Extension(user_id): Extension<Uuid>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    service::withdraw_user(&state, user_id).await.map_err(|e| {
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
//
// 변경사항:
// - filter::validate_nickname() 으로 닉네임 검증 추가
//   검사 순서: 앞뒤공백 → 길이(2~8자) → 금칙어
// - 기존 nickname.trim().is_empty() 체크는 validate_nickname이 커버하므로 제거
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
    Extension(user_id): Extension<Uuid>,
    Json(body): Json<CompleteProfileRequest>,
) -> Result<Json<CompleteProfileResponse>, (StatusCode, String)> {
    if body.nickname.trim().is_empty() {
        return Err((
            StatusCode::BAD_REQUEST,
            "닉네임을 입력해 주세요.".to_string(),
        ));
    }

    // 닉네임 검증: 앞뒤공백 → 길이(2~8자) → 금칙어
    // 실패 시 구체적인 에러 메시지를 그대로 400으로 반환
    filter::validate_nickname(&body.nickname)
        .map_err(|msg| (StatusCode::BAD_REQUEST, msg.to_string()))?;

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
        "profile_completed": true,
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
        .ok_or_else(|| {
            (
                StatusCode::BAD_REQUEST,
                "닉네임을 입력해 주세요.".to_string(),
            )
        })?;

    let available = crate::auth::service::check_nickname_available(&state, nickname)
        .await
        .map_err(|e| {
            tracing::error!("닉네임 중복 확인 실패: {}", e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "닉네임 중복 확인에 실패했습니다.".to_string(),
            )
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
                || msg.contains("닉네임은")
                || msg.contains("사용할 수 없는 닉네임입니다")
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
        "email" => "이메일 로그인 계정입니다.",
        "google" => "구글 로그인으로 가입된 계정입니다.",
        "kakao" => "카카오 로그인으로 가입된 계정입니다. 카카오 로그인을 이용해주세요.",
        _ => "로그인 방식 정보를 확인할 수 없습니다.",
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

    // 케이스별 처리:
    // 1) 이메일 없음 → 404 (가입 가능)
    // 2) 이메일 있음 + deleted_at IS NULL → 200 exists:true (이미 가입된 이메일)
    // 3) 이메일 있음 + deleted_at IS NOT NULL → 403 (탈퇴 이메일, 영구 재가입 불가)
    //
    // 정책:
    // - 같은 이메일은 영구 재가입 금지
    // - 30일 쿨다운 후 재가입 허용 정책이 아님
    // - 따라서 check_rejoin_cooldown()을 호출하지 않는다.
    // 정책: 같은 이메일은 영구 차단 (NFT 거래 무결성 + 5년 보관 의무)
    if let Some(row) = rows.first() {
        if row["deleted_at"].is_string() {
            // 이메일은 영구 차단 정책 (NFT 거래 이력 무결성)
            // 30일 쿨다운 체크조차 안 함 — 어차피 영구라 의미 없음
            return Err((
                StatusCode::FORBIDDEN,
                "이미 탈퇴한 회원입니다. 다른 이메일로 가입해 주세요.".to_string(),
            ));
        } else {
            return Ok(Json(CheckEmailResponse { exists: true }));
        }
    }

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
) -> Response {
    if body.code.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({
                "code": "BAD_REQUEST",
                "message": "인가 코드가 비어있음"
            })),
        )
            .into_response();
    }

    if body.state.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            Json(serde_json::json!({
                "code": "BAD_REQUEST",
                "message": "state가 비어있음"
            })),
        )
            .into_response();
    }

    let cookie_state = match extract_kakao_state_cookie(&headers) {
        Some(value) => value,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(serde_json::json!({
                    "code": "OAUTH_STATE_MISSING",
                    "message": "OAuth state 쿠키가 없습니다."
                })),
            )
                .into_response();
        }
    };

    if cookie_state != body.state {
        tracing::warn!(
            "카카오 OAuth state 불일치: cookie_state={}, body_state={}",
            cookie_state,
            body.state
        );

        return (
            StatusCode::UNAUTHORIZED,
            Json(serde_json::json!({
                "code": "OAUTH_STATE_INVALID",
                "message": "유효하지 않은 로그인 요청입니다."
            })),
        )
            .into_response();
    }

    let issued = match service::kakao_login(&state, &body.code, "web").await {
        Ok(issued) => issued,
        Err(e) => {
            tracing::warn!("카카오 로그인 실패/차단: {}", e);
            return map_auth_service_error(e);
        }
    };

    let mut merged_headers = build_clear_kakao_state_cookie(&state);
    let refresh_headers = build_refresh_cookie(&state, &issued.refresh_token);

    for value in refresh_headers.get_all(SET_COOKIE).iter() {
        merged_headers.append(SET_COOKIE, value.clone());
    }

    let body = WebLoginResponse {
        access_token: issued.access_token,
        is_new_user: issued.is_new_user,
        role_type: "user".to_string(),
    };

    (merged_headers, Json(serde_json::to_value(body).unwrap())).into_response()
}

pub async fn kakao_login_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<KakaoLoginRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_app_request(&headers)?;

    if body.code.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "인가 코드가 비어있음".to_string()));
    }

    if body.state.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "state가 비어있음".to_string()));
    }

    let issued = service::kakao_login(&state, &body.code, "app")
        .await
        .map_err(|e| {
            // service에서 올라온 에러를 HTTP 상태코드로 변환
            //
            // 케이스 분기:
            // - 탈퇴 관련 메시지        → 403
            // - 회원가입 미완료 (앱 전용) → 403
            // - 그 외                   → 500
            let msg = e.to_string();

            if msg.contains("탈퇴") || msg.contains("비활성화된 계정") {
                tracing::warn!("앱 카카오 로그인 차단 (계정 상태): {}", msg);
                (StatusCode::FORBIDDEN, msg)
            } else if msg.contains("웹에서 회원가입 완료 후 다시 이용해 주세요.")
            {
                tracing::warn!("앱 카카오 로그인 차단 (가입 미완료): {}", msg);
                (StatusCode::FORBIDDEN, msg)
            } else {
                tracing::error!("앱 카카오 로그인 실패 (서버): {}", msg);
                (StatusCode::INTERNAL_SERVER_ERROR, msg)
            }
        })?;

    let body = AppLoginResponse {
        access_token: issued.access_token,
        refresh_token: issued.refresh_token,
        is_new_user: issued.is_new_user,
    };

    Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
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

pub async fn kakao_start_app(
    State(state): State<AppState>,
    headers: HeaderMap,
) -> Result<(HeaderMap, Json<AppKakaoStartResponse>), (StatusCode, String)> {
    ensure_app_request(&headers)?;

    let oauth_state = uuid::Uuid::new_v4().to_string();

    // Android 앱용 카카오 REST API 키를 별도로 사용한다.
    // 기존 웹용 KAKAO_REST_API_KEY는 건드리지 않음.
    let kakao_android_rest_api_key = std::env::var("KAKAO_ANDROID_REST_API_KEY").map_err(|_| {
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "KAKAO_ANDROID_REST_API_KEY 환경변수가 설정되지 않았습니다.".to_string(),
        )
    })?;

    let auth_url = format!(
        "https://kauth.kakao.com/oauth/authorize?client_id={}&redirect_uri={}&response_type=code&scope=profile_nickname,profile_image&prompt=select_account&state={}",
        kakao_android_rest_api_key,
        urlencoding::encode(&state.config.kakao_app_redirect_uri),
        oauth_state
    );

    Ok((
        HeaderMap::new(),
        Json(AppKakaoStartResponse {
            auth_url,
            state: oauth_state,
        }),
    ))
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
) -> Response {
    tracing::debug!("refresh 요청 수신: client_type=web");

    let refresh_token = match extract_refresh_cookie(&headers) {
        Some(token) => token,
        None => {
            return (
                StatusCode::UNAUTHORIZED,
                Json(serde_json::json!({
                    "code": "REFRESH_COOKIE_MISSING",
                    "message": "refresh 쿠키가 없습니다."
                })),
            )
                .into_response();
        }
    };

    let rotated = match service::rotate_refresh_token(&state, &refresh_token, "web").await {
        Ok(rotated) => rotated,
        Err(e) => {
            tracing::warn!("refresh 실패: {}", e);
            return map_auth_service_error(e);
        }
    };

    let cookie_headers = build_refresh_cookie(&state, &rotated.refresh_token);
    tracing::debug!("refresh 성공 응답: web refresh 쿠키 재발급");

    let body = WebRefreshResponse {
        access_token: rotated.access_token,
    };

    (cookie_headers, Json(serde_json::to_value(body).unwrap())).into_response()
}

pub async fn refresh_token_app(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<RefreshRequest>,
) -> Response {
    if let Err((status, message)) = ensure_app_request(&headers) {
        return (
            status,
            Json(serde_json::json!({
                "code": "APP_REQUEST_INVALID",
                "message": message
            })),
        )
            .into_response();
    }

    tracing::debug!("refresh 요청 수신: client_type=app");

    let refresh_token = match body.refresh_token.filter(|v| !v.trim().is_empty()) {
        Some(token) => token,
        None => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({
                    "code": "BAD_REQUEST",
                    "message": "refresh_token을 입력해 주세요."
                })),
            )
                .into_response();
        }
    };

    let rotated = match service::rotate_refresh_token(&state, &refresh_token, "app").await {
        Ok(rotated) => rotated,
        Err(e) => {
            tracing::warn!("앱 refresh 실패: {}", e);
            return map_auth_service_error(e);
        }
    };

    let body = AppRefreshResponse {
        access_token: rotated.access_token,
        refresh_token: rotated.refresh_token,
    };

    (HeaderMap::new(), Json(serde_json::to_value(body).unwrap())).into_response()
}

pub async fn refresh_token_unity(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<RefreshRequest>,
) -> Response {
    if let Err((status, message)) = ensure_unity_request(&headers) {
        return (
            status,
            Json(serde_json::json!({
                "code": "UNITY_REQUEST_INVALID",
                "message": message
            })),
        )
            .into_response();
    }

    tracing::debug!("refresh 요청 수신: client_type=unity");

    let refresh_token = match body.refresh_token.filter(|v| !v.trim().is_empty()) {
        Some(token) => token,
        None => {
            return (
                StatusCode::BAD_REQUEST,
                Json(serde_json::json!({
                    "code": "BAD_REQUEST",
                    "message": "refresh_token을 입력해 주세요."
                })),
            )
                .into_response();
        }
    };

    let rotated = match service::rotate_refresh_token(&state, &refresh_token, "unity").await {
        Ok(rotated) => rotated,
        Err(e) => {
            tracing::warn!("유니티 refresh 실패: {}", e);
            return map_auth_service_error(e);
        }
    };

    let body = AppRefreshResponse {
        access_token: rotated.access_token,
        refresh_token: rotated.refresh_token,
    };

    (HeaderMap::new(), Json(serde_json::to_value(body).unwrap())).into_response()
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

    // refresh 토큰이 있다면 현재 웹 세션만 revoke한다.
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

pub async fn logout_unity(
    State(state): State<AppState>,
    headers: HeaderMap,
    Json(body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    ensure_unity_request(&headers)?;

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
        state
            .config
            .environment
            .trim()
            .to_ascii_lowercase()
            .as_str(),
        "prod" | "production"
    )
}

// ═══════════════════════════════════════════════════════════════
// [카카오 앱 콜백] GET /auth/kakao/callback
// ═══════════════════════════════════════════════════════════════
//
// 카카오 OAuth 인증 후 브라우저가 이 URL로 돌아오면
// 백엔드가 앱 딥링크(spentopia://kakao-callback)로 다시 넘겨준다.
//
// 흐름:
// Kakao -> http://10.0.2.2:1113/auth/kakao/callback?code=...&state=...
// Backend -> spentopia://kakao-callback?code=...&state=...
// Android 앱 -> code/state 추출 후 /auth/app/kakao/login 호출
pub async fn kakao_callback(
    Query(params): Query<std::collections::HashMap<String, String>>,
) -> impl IntoResponse {
    let code = params.get("code").cloned().unwrap_or_default();
    let state = params.get("state").cloned().unwrap_or_default();

    let redirect_url = format!(
        "spentopia://kakao-callback?code={}&state={}",
        urlencoding::encode(&code),
        urlencoding::encode(&state)
    );

    axum::response::Redirect::temporary(&redirect_url)
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
// 프론트는 이 handoff_token을 exe 실행용 프로토콜/런처에만 전달해야 한다.
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
    Extension(user_id): Extension<Uuid>,
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
        expires_in: 60,
    }))
}

// ═══════════════════════════════════════════════════════════════
// [handoff 교환] POST /auth/handoff/exchange
// ═══════════════════════════════════════════════════════════════
//
// 공개 라우트 → JWT 불필요
//
// 이유:
// - 유니티 exe는 아직 access token이 없는 상태에서 시작함
// - 실행 시 전달받은 handoff token만 가지고 교환해야 함
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
// - refresh_token으로 /auth/unity/refresh (body 방식)로 세션 연장
// - 유니티 종료/로그아웃 시 /auth/unity/logout 으로 해당 unity 세션만 종료
#[utoipa::path(
    post,
    path = "/auth/handoff/exchange",
    tag = "Handoff",
    request_body = HandoffExchangeRequest,
    responses(
        (status = 200, description = "교환 성공 — 유니티용 access+refresh 발급", body = HandoffExchangeResponse),
        (status = 400, description = "handoff_token 비어있음"),
        (status = 401, description = "유효하지 않거나 만료된 handoff token"),
        (status = 500, description = "유니티용 세션 발급 실패")
    )
)]
pub async fn exchange_handoff(
    State(state): State<AppState>,
    Json(body): Json<HandoffExchangeRequest>,
) -> Result<Json<HandoffExchangeResponse>, (StatusCode, String)> {
    // ── 1) 입력 검증 ────────────────────────────────────────
    if body.auth_code.trim().is_empty() {
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
    let result = exchange_handoff_token(&state, &body.auth_code)
        .await
        .map_err(|e| {
            let msg = e.to_string();
            tracing::warn!("handoff 교환 실패: {}", msg);
            let status = match msg.as_str() {
                "유효하지 않은 handoff token입니다."
                | "handoff token이 만료되었습니다."
                | "handoff token의 대상 서비스가 일치하지 않습니다." => {
                    StatusCode::UNAUTHORIZED
                }
                _ => StatusCode::INTERNAL_SERVER_ERROR,
            };

            (status, msg)
        })?;

    // ── 3) 닉네임 조회 ──────────────────────────────────────
    let nickname = fetch_nickname_by_user_id(&state, result.user_id).await;

    // ── 4) 응답 ─────────────────────────────────────────────
    // 유니티는 앱 방식이므로 access+refresh 둘 다 body로 반환
    Ok(Json(HandoffExchangeResponse {
        access_token: result.access_token,
        refresh_token: result.refresh_token,
        user_id: result.user_id,
        nickname: nickname,
    }))
}

// ============================================================================
// POST /auth/webview/issue
// ============================================================================

/// 웹뷰 진입용 1회용 토큰 발급
///
/// 흐름:
/// 1. jwt_middleware가 앱의 access token 검증 → user_id 추출
/// 2. 30초짜리 webview token 발급 → 메모리 저장
/// 3. 토큰 원문 반환 (앱은 이걸 webView.loadUrl 쿼리에 사용)
#[utoipa::path(
    post,
    path = "/auth/webview/issue",
    tag = "auth",
    request_body = WebviewIssueRequest,
    responses(
        (status = 200, description = "발급 성공", body = WebviewIssueResponse),
        (status = 401, description = "인증되지 않음"),
    ),
    security(("bearer_auth" = []))
)]
pub async fn webview_issue(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    headers: HeaderMap,
    Json(payload): Json<WebviewIssueRequest>,
) -> Result<Json<WebviewIssueResponse>, (StatusCode, String)> {
    // Android 앱 전용 진입점.
    // 브라우저에서 직접 호출해 웹 세션을 webview token으로 감싸는 우회를 막는다.
    ensure_app_request(&headers)?;

    // ── redirect_path 검증 ──────────────────────────────────
    // Open Redirect 방지를 위해 발급 시점에 미리 검증/정제
    let redirect_path = sanitize_redirect_path(payload.redirect_path.as_deref());

    // ── 1회용 토큰 발급 ─────────────────────────────────────
    let token = create_webview_token(&state, user_id, &redirect_path);

    Ok(Json(WebviewIssueResponse {
        webview_token: token,
        expires_in: 30,
    }))
}

// ============================================================================
// GET /auth/webview/callback
// ============================================================================

/// 웹뷰가 진입하는 엔드포인트
///
/// **인증 미들웨어 없음** — webview token 자체가 인증 수단.
///
/// 흐름:
/// 1. webview token 검증 + 즉시 소비 (1회용)
/// 2. service::issue_login_tokens 재사용해서 access + refresh 발급
///    - client_type = "web" (프론트의 /auth/refresh 흐름과 동일하게 회전)
/// 3. refresh token을 httpOnly 쿠키로 Set-Cookie
///    - 다른 핸들러들과 동일하게 cookie::Cookie 빌더 사용
///    - Path=/auth (다른 refresh 쿠키들과 일관)
/// 4. 프론트엔드로 302 리다이렉트
///    - access token은 URL fragment로 전달 (Referer/로그 노출 X)
#[utoipa::path(
    get,
    path = "/auth/webview/callback",
    tag = "auth",
    params(
        ("token" = String, Query, description = "1회용 webview token"),
    ),
    responses(
        (status = 302, description = "프론트엔드로 리다이렉트"),
        (status = 401, description = "토큰 무효 또는 만료"),
    ),
)]
pub async fn webview_callback(
    State(state): State<AppState>,
    Query(params): Query<WebviewCallbackQuery>,
) -> Result<Response, (StatusCode, String)> {
    // ── 1) webview token 검증 + 소비 ────────────────────────
    let (user_id, redirect_path) = consume_webview_token(&state, &params.token).map_err(|e| {
        tracing::warn!("webview callback 검증 실패: {}", e);
        (StatusCode::UNAUTHORIZED, e.to_string())
    })?;

    // ── 2) 웹뷰용 access + refresh 발급 ─────────────────────
    // client_type = "web"
    //   → 프론트 공용 /auth/refresh가 "web" 세션을 회전시키므로
    //     webview도 동일 타입으로 발급해야 새로고침/만료 후 복구가 가능하다.
    //
    // is_new_user = false
    //   → webview는 이미 로그인된 유저가 진입하는 거라 항상 false
    let tokens = service::issue_login_tokens(&state, user_id, "web", false)
        .await
        .map_err(|e| {
            tracing::error!("webview용 토큰 발급 실패: user_id={}, error={}", user_id, e);
            (
                StatusCode::INTERNAL_SERVER_ERROR,
                "토큰 발급에 실패했습니다.".to_string(),
            )
        })?;

    // ── 3) refresh 쿠키 빌드 (다른 핸들러와 동일한 패턴) ────
    // build_refresh_cookie를 그대로 재사용
    let cookie_headers = build_refresh_cookie(&state, &tokens.refresh_token);

    // ── 4) 프론트엔드 URL 조립 ───────────────────────────────
    // access token을 URL fragment(#)로 전달
    // - fragment는 서버에 전송되지 않음 (Referer/로그 노출 X)
    // - 프론트는 window.location.hash에서 파싱
    let frontend_url = format!(
        "{}{}#access_token={}",
        state.config.frontend_origin.trim_end_matches('/'),
        redirect_path,
        tokens.access_token
    );

    // ── 5) 302 리다이렉트 응답 + Set-Cookie ─────────────────
    let mut response = Redirect::to(&frontend_url).into_response();

    // build_refresh_cookie가 만든 Set-Cookie 헤더를 응답에 병합
    for value in cookie_headers.get_all(SET_COOKIE).iter() {
        response.headers_mut().append(SET_COOKIE, value.clone());
    }

    Ok(response)
}

async fn fetch_nickname_by_user_id(state: &AppState, user_id: uuid::Uuid) -> Option<String> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=nickname&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    #[derive(serde::Deserialize)]
    struct Row {
        nickname: Option<String>,
    }

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .ok()?;

    let rows: Vec<Row> = res.json().await.ok()?;
    rows.into_iter().next()?.nickname
}
