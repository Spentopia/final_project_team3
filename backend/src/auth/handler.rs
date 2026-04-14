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
FindEmailRequest, FindEmailResponse,ExchangeTokenRequest,
                 CheckEmailRequest, KakaoLoginRequest, CompleteProfileRequest,
                 CompleteProfileResponse,};
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

// ═══════════════════════════════════════════════════════════════
// [이메일/구글] Supabase 토큰 -> 앱 JWT 교환
// POST /auth/exchange
// ═══════════════════════════════════════════════════════════════
#[utoipa::path(
    post,
    path = "/auth/exchange",
    tag = "인증",
    request_body = ExchangeTokenRequest,
    responses(
        (status = 200, description = "토큰 교환 성공", body = LoginResponse),
        (status = 400, description = "access_token 비어있음"),
        (status = 401, description = "유효하지 않은 Supabase 토큰")
    )
)]
pub async fn exchange_token(
    State(state): State<AppState>,
    Json(body): Json<ExchangeTokenRequest>,
) -> Result<Json<LoginResponse>, (StatusCode, String)> {
    if body.access_token.trim().is_empty() {
        return Err((StatusCode::BAD_REQUEST, "access_token이 비어있음".to_string()));
    }

    let response = service::exchange_supabase_token(&state, &body.access_token)
        .await
        .map_err(|e| {
            tracing::error!("토큰 교환 실패: {}", e);
            (StatusCode::UNAUTHORIZED, e.to_string())
        })?;

    Ok(Json(response))
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

    let user = users.first()
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

        // 닉네임 unique 충돌
        if err.contains("users_nickname_key") {
            return Err((
                StatusCode::BAD_REQUEST,
                "이미 사용 중인 닉네임입니다".to_string(),
            ));
        }

        // 전화번호 unique 충돌
        if err.contains("users_phone_key") {
            return Err((
                StatusCode::BAD_REQUEST,
                "이미 사용 중인 전화번호입니다".to_string(),
            ));
        }

        // profile_image 컬럼명 오류 같은 경우
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
    Json(body): Json<KakaoLoginRequest>,
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




