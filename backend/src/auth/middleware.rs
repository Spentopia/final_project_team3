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
    http::{header::AUTHORIZATION, StatusCode},
    middleware::Next,
    response::Response,
};
use uuid::Uuid;

use crate::auth::app_jwt::verify_app_access_token;
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
    let header = request
        .headers()
        .get(AUTHORIZATION)
        .ok_or_else(|| {
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
    let claims = verify_app_access_token(&state.config.app_jwt_secret, token)
        .map_err(|e| {
            tracing::warn!("앱 JWT 검증 실패: {}", e);
            (
                StatusCode::UNAUTHORIZED,
                "유효하지 않은 토큰입니다.".to_string(),
            )
        })?;

    // claims.sub -> UUID 파싱
    let user_id = Uuid::parse_str(&claims.sub).map_err(|e| {
        tracing::warn!(
            "앱 JWT user_id 파싱 실패: sub={}, error={}",
            claims.sub,
            e
        );
        (
            StatusCode::UNAUTHORIZED,
            "사용자 ID를 확인할 수 없습니다.".to_string(),
        )
    })?;

    tracing::debug!("앱 JWT 검증 통과: user_id={}", user_id);

    // ── 3) 핸들러에서 꺼내 쓸 수 있도록 extension에 저장 ─────
    request.extensions_mut().insert(user_id);

    // 다음 핸들러로 진행
    Ok(next.run(request).await)
}