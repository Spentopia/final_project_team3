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
    extract::State,
    http::{header::SET_COOKIE, HeaderMap, StatusCode},
    Json,
};

use cookie::{Cookie, SameSite};

use utoipa;
use crate::state::AppState;
use super::dto::{AppLoginResponse,
                 AppRefreshResponse,
                 CheckEmailRequest,
                 CompleteProfileRequest,
                 CompleteProfileResponse,
                 ExchangeTokenRequest,
                 FindEmailRequest,
                 FindEmailResponse,
                 KakaoLoginRequest,
                 NonceRequest,
                 NonceResponse,
                 RefreshRequest,
                 WalletLoginRequest,
                 WebLoginResponse,
                 WebRefreshResponse,};
use super::service;

/// 클라이언트 타입 판별
///
/// web/app 분기 기준:
/// - X-Client-Type: web
/// - X-Client-Type: app
///
/// 왜 필요하냐:
/// - 웹은 refresh token을 쿠키로 내려줘야 하고
/// - 앱은 refresh token을 body로 내려줘야 하기 때문
///
/// 기본값을 web으로 두는 이유:
/// - 브라우저 쪽에서 헤더를 깜빡 빼먹어도
///   최소한 웹 흐름으로는 동작하게 하기 위함
fn resolve_client_type(headers: &HeaderMap) -> &'static str {
    match headers
        .get("x-client-type")
        .and_then(|v| v.to_str().ok())
        .map(|v| v.to_ascii_lowercase())
        .as_deref()
    {
        Some("app") => "app",
        _ => "web",
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
/// - Path=/auth/refresh:
///   refresh 쿠키가 모든 요청에 붙지 않고
///   refresh 요청에만 붙도록 제한
///
/// 왜 refresh만 쿠키냐:
/// - access는 프론트 메모리에 저장하고 Authorization 헤더로 보냄
/// - refresh는 브라우저 JS가 직접 읽지 못하게 숨기기 위함
fn build_refresh_cookie(refresh_token: &str) -> HeaderMap {
    let mut headers = HeaderMap::new();

    let cookie = Cookie::build(("spentopia_refresh", refresh_token.to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth/refresh")
        .secure(false) // 로컬 시연용. HTTPS 아님.
        .build();

    headers.append(
        SET_COOKIE,
        cookie.to_string().parse().unwrap(),
    );

    headers
}

/// 웹 로그아웃 시 refresh 쿠키 제거용 Set-Cookie 생성
///
/// Max-Age=0 으로 만료시켜서 브라우저가 쿠키를 지우게 함
fn build_clear_refresh_cookie() -> HeaderMap {
    let mut headers = HeaderMap::new();

    let cookie = Cookie::build(("spentopia_refresh", "".to_string()))
        .http_only(true)
        .same_site(SameSite::Lax)
        .path("/auth/refresh")
        .secure(false) // 로컬 시연용
        .max_age(cookie::time::Duration::seconds(0))
        .build();

    headers.append(
        SET_COOKIE,
        cookie.to_string().parse().unwrap(),
    );

    headers
}

/// 현재 요청의 Cookie 헤더에서 refresh 쿠키 추출
///
/// 웹 refresh/logout에서 사용
///
/// 앱은 쿠키를 안 쓰고 body.refresh_token을 쓰므로
/// 앱 쪽에서는 이 함수 사용 안 함
fn extract_refresh_cookie(headers: &HeaderMap) -> Option<String> {
    headers
        .get("cookie")
        .and_then(|v| v.to_str().ok())
        .and_then(|cookie_header| {
            cookie::Cookie::split_parse(cookie_header)
                .filter_map(Result::ok)
                .find(|c| c.name() == "spentopia_refresh")
                .map(|c| c.value().to_string())
        })
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
    headers: HeaderMap,
    Json(body): Json<WalletLoginRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {

    let client_type = resolve_client_type(&headers);

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
        client_type,
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

    // 웹 / 앱 응답 분기
    if client_type == "web" {
        // 웹: refresh는 쿠키
        let cookie_headers = build_refresh_cookie(&response.refresh_token);

        let body = WebLoginResponse {
            access_token: response.access_token,
            is_new_user: response.is_new_user,
        };

        Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
    } else {
        // 앱: refresh도 body
        let body = AppLoginResponse {
            access_token: response.access_token,
            refresh_token: response.refresh_token,
            is_new_user: response.is_new_user,
        };

        Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
    }
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
    headers: HeaderMap,
    Json(body): Json<ExchangeTokenRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    if body.access_token.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "access_token이 비어있음".to_string()));
    }

    let client_type = resolve_client_type(&headers);

    let issued = service::exchange_supabase_token(
        &state,
        &body.access_token,
        client_type,
    )
        .await
        .map_err(|e| {
            tracing::error!("토큰 교환 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    if client_type == "web" {
        let cookie_headers = build_refresh_cookie(&issued.refresh_token);

        let body = WebLoginResponse {
            access_token: issued.access_token,
            is_new_user: issued.is_new_user,
        };

        Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
    } else {
        let body = AppLoginResponse {
            access_token: issued.access_token,
            refresh_token: issued.refresh_token,
            is_new_user: issued.is_new_user,
        };

        Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
    }
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
        "{}/rest/v1/users?id=eq.{}&select=id,email,profile_completed,login_provider,nickname,phone,profile_image",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let resp = state.http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .send()
        .await
        .map_err(|e| {
            tracing::error!("/me 유저 조회 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "유저 조회 실패".to_string())
        })?;

    if !resp.status().is_success() {
        let err = resp.text().await.unwrap_or_default();
        tracing::error!("/me 응답 실패: {}", err);
        return Err((StatusCode::INTERNAL_SERVER_ERROR, err));
    }

    let users: Vec<serde_json::Value> = resp.json().await
        .map_err(|e| {
            tracing::error!("/me 응답 파싱 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "응답 파싱 실패".to_string())
        })?;

    let user = users
        .first()
        .cloned()
        .ok_or_else(|| (StatusCode::NOT_FOUND, "유저 없음".to_string()))?;

    Ok(Json(user))
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
        return Err((StatusCode::BAD_REQUEST, "닉네임이 비어있음".to_string()));
    }

    if body.phone.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "전화번호가 비어있음".to_string()));
    }

    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let payload = serde_json::json!({
        "nickname": body.nickname,
        "phone": body.phone,
        "profile_image": body.profile_image,
    });

    let resp = state.http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .map_err(|e| {
            tracing::error!("프로필 업데이트 요청 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "프로필 저장 실패".to_string())
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

    let rows: Vec<serde_json::Value> = resp.json().await
        .map_err(|e| {
            tracing::error!("프로필 업데이트 응답 파싱 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "응답 파싱 실패".to_string())
        })?;

    let updated = rows.first().cloned().unwrap_or_default();
    let profile_completed = updated["profile_completed"].as_bool().unwrap_or(false);

    Ok(Json(CompleteProfileResponse {
        success: true,
        profile_completed,
    }))
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

    let masked_email = service::find_email_by_phone(&state, &body.phone)
        .await
        .map_err(|e| {
            tracing::error!("이메일 찾기 실패: {}", e);
            (StatusCode::BAD_REQUEST, e.to_string())
        })?;

    Ok(Json(FindEmailResponse { masked_email }))
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
        (status = 200, description = "이메일 존재함"),
        (status = 404, description = "해당 이메일로 가입된 계정 없음")
    )
)]
pub async fn check_email(
    State(state): State<AppState>,
    Json(body): Json<CheckEmailRequest>,
) -> Result<Json<serde_json::Value>, (StatusCode, String)> {
    if body.email.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "이메일이 비어있음".to_string()));
    }

    let exists = service::check_email_exists(&state, &body.email)
        .await
        .map_err(|e| {
            tracing::error!("이메일 존재 확인 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, e.to_string())
        })?;

    if !exists {
        return Err((
            StatusCode::NOT_FOUND,
            "해당 이메일로 가입된 계정이 없습니다".to_string(),
        ));
    }

    Ok(Json(serde_json::json!({ "exists": true })))
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
        (status = 200, description = "로그인 성공", body = LoginResponse),
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

    let client_type = resolve_client_type(&headers);

    let issued = service::kakao_login(&state, &body.code, client_type)
        .await
        .map_err(|e| {
            tracing::error!("카카오 로그인 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, e.to_string())
        })?;

    if client_type == "web" {
        let cookie_headers = build_refresh_cookie(&issued.refresh_token);

        let body = WebLoginResponse {
            access_token: issued.access_token,
            is_new_user: issued.is_new_user,
        };

        Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
    } else {
        let body = AppLoginResponse {
            access_token: issued.access_token,
            refresh_token: issued.refresh_token,
            is_new_user: issued.is_new_user,
        };

        Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
    }
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
    Json(body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    let client_type = resolve_client_type(&headers);

    let refresh_token = if client_type == "web" {
        // 웹은 쿠키에서 읽음
        extract_refresh_cookie(&headers)
            .ok_or_else(|| (StatusCode::UNAUTHORIZED, "refresh 쿠키 없음".to_string()))?
    } else {
        // 앱은 body에서 읽음
        body.refresh_token
            .filter(|v| !v.trim().is_empty())
            .ok_or_else(|| (StatusCode::BAD_REQUEST, "refresh_token이 비어있음".to_string()))?
    };

    let rotated = service::rotate_refresh_token(
        &state,
        &refresh_token,
        client_type,
    )
        .await
        .map_err(|e| {
            tracing::warn!("refresh 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    if client_type == "web" {
        // 웹은 새 refresh를 다시 쿠키로 넣음
        let cookie_headers = build_refresh_cookie(&rotated.refresh_token);

        let body = WebRefreshResponse {
            access_token: rotated.access_token,
        };

        Ok((cookie_headers, Json(serde_json::to_value(body).unwrap())))
    } else {
        // 앱은 새 access/refresh 둘 다 body
        let body = AppRefreshResponse {
            access_token: rotated.access_token,
            refresh_token: rotated.refresh_token,
        };

        Ok((HeaderMap::new(), Json(serde_json::to_value(body).unwrap())))
    }
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
    Json(body): Json<RefreshRequest>,
) -> Result<(HeaderMap, Json<serde_json::Value>), (StatusCode, String)> {
    let client_type = resolve_client_type(&headers);

    let refresh_token = if client_type == "web" {
        extract_refresh_cookie(&headers)
    } else {
        body.refresh_token
    };

    // refresh 토큰이 있다면 해당 세션 revoke 시도
    if let Some(token) = refresh_token {
        if let Ok(claims) = crate::auth::app_jwt::verify_app_refresh_token(
            &state.config.app_jwt_secret,
            &token,
        ) {
            if let Ok(session_id) = uuid::Uuid::parse_str(&claims.sid) {
                let _ = crate::auth::refresh_store::revoke_refresh_session(
                    &state,
                    session_id,
                    None,
                )
                    .await;
            }
        }
    }

    if client_type == "web" {
        // 웹은 쿠키도 같이 지워야 함
        let headers = build_clear_refresh_cookie();
        Ok((headers, Json(serde_json::json!({ "logged_out": true }))))
    } else {
        Ok((HeaderMap::new(), Json(serde_json::json!({ "logged_out": true }))))
    }
}





