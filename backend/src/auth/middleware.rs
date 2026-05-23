// auth/middleware.rs
// 모든 보호된 API 앞단에서 "우리 앱 JWT"만 검증하는 미들웨어
//
// 검증 통과하면 user_id를 request extension에 넣어서 핸들러로 넘김.
// 검증 실패하면 여기서 바로 401 반환.
//
// 이전 구조에서는:
// - 앱 JWT 검증
// - 실패하면 Supabase JWT 검증
// - JWKS 캐시 / kid 매칭 / iss / aud 검증
//
// 이런 하이브리드 구조였지만,
// 이제는 이메일/구글/카카오/지갑 전부 최종적으로 "우리 앱 JWT"만 쓰므로
// Supabase JWT 검증 로직은 전부 제거함.
//
// 즉 이제 이 파일의 책임은 단 하나:
//   Authorization: Bearer <앱 JWT>
// 를 검증해서 user_id를 Extension에 넣는 것.

use axum::{
    extract::{Request, State},
    http::{StatusCode, header::AUTHORIZATION},
    middleware::Next,
    response::Response,
};
use uuid::Uuid;

use crate::auth::app_jwt::verify_app_access_token;
use crate::auth::service::get_user_role;
use crate::state::AppState;

// jwt_middleware
//
// 보호 라우트에 붙는 미들웨어.
// 성공 시:
//   request.extensions_mut().insert(user_id)
// 실패 시:
//   401 Unauthorized 반환
pub async fn jwt_middleware(
    State(state): State<AppState>,
    mut request: Request,
    next: Next,
) -> Result<Response, (StatusCode, String)> {
    // ── 1) Authorization 헤더 꺼내기 ─────────────────────────
    let header = request.headers().get(AUTHORIZATION).ok_or_else(|| {
        tracing::warn!("Authorization 헤더 없음");
        (
            StatusCode::UNAUTHORIZED,
            "Authorization 헤더가 없습니다.".to_string(),
        )
    })?;

    // HeaderValue -> &str 변환
    let value = header.to_str().map_err(|e| {
        tracing::warn!("헤더 형식 오류: {}", e);
        (
            StatusCode::UNAUTHORIZED,
            "헤더 형식이 올바르지 않습니다.".to_string(),
        )
    })?;

    // "Bearer <token>" 형식 확인
    let token = value
        .strip_prefix("Bearer ")
        .map(str::trim)
        .filter(|t| !t.is_empty())
        .ok_or_else(|| {
            tracing::warn!("Bearer 토큰 없음 (헤더값: {})", value);
            (
                StatusCode::UNAUTHORIZED,
                "Bearer 토큰이 없습니다.".to_string(),
            )
        })?;

    // ── 2) 우리 앱 JWT 검증 ─────────────────────────────────
    let claims = verify_app_access_token(&state.config.app_jwt_secret, token).map_err(|e| {
        tracing::warn!("앱 JWT 검증 실패: {}", e);
        (
            StatusCode::UNAUTHORIZED,
            "유효하지 않은 토큰입니다.".to_string(),
        )
    })?;

    // claims.sub -> UUID 파싱
    let user_id = Uuid::parse_str(&claims.sub).map_err(|e| {
        tracing::warn!("앱 JWT user_id 파싱 실패: sub={}, error={}", claims.sub, e);
        (
            StatusCode::UNAUTHORIZED,
            "사용자 ID를 확인할 수 없습니다.".to_string(),
        )
    })?;

    // ── 3) 핸들러에서 꺼내 쓸 수 있도록 extension에 저장 ─────
    request.extensions_mut().insert(user_id);

    // 다음 핸들러로 진행
    Ok(next.run(request).await)
}

// admin_middleware
//
// jwt_middleware 이후에 적용되는 관리자 전용 미들웨어.
// extensions에서 user_id를 꺼내 DB의 role_type을 확인한다.
// role_type = "admin"이 아니면 403 반환.
//
// 라우트에 적용할 때 순서:
//   .route_layer(admin_middleware)  <- 먼저 선언
//   .route_layer(jwt_middleware)    <- 나중에 선언 (실제로는 먼저 실행됨)
pub async fn admin_middleware(
    State(state): State<AppState>,
    request: Request,
    next: Next,
) -> Result<Response, (StatusCode, String)> {
    let user_id = request
        .extensions()
        .get::<Uuid>()
        .cloned()
        .ok_or_else(|| (StatusCode::UNAUTHORIZED, "인증이 필요합니다.".to_string()))?;

    let role = get_user_role(&state, user_id).await.map_err(|e| {
        tracing::error!("role_type 조회 실패: user_id={}, error={}", user_id, e);
        (
            StatusCode::INTERNAL_SERVER_ERROR,
            "권한 확인에 실패했습니다.".to_string(),
        )
    })?;

    if role.as_deref() != Some("admin") {
        tracing::warn!("관리자 권한 없음: user_id={}, role={:?}", user_id, role);
        return Err((
            StatusCode::FORBIDDEN,
            "관리자 권한이 필요합니다.".to_string(),
        ));
    }

    Ok(next.run(request).await)
}
