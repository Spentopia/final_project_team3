// auth/middleware.rs
// 모든 보호된 API 앞단에서 JWT를 검증하는 미들웨어
// 검증 통과하면 user_id를 request extension에 넣어서 핸들러로 넘김.
// 검증 실패하면 여기서 바로 401 반환
//
// [보완 사항 반영]
// 1. JWKS 캐시: 매 요청마다 HTTP 호출하지 않고 메모리에 캐시.
//    kid 못 찾을 때만 새로 fetch함.
// 2. kid 매칭: JWT 헤더의 kid와 JWKS의 kid를 매칭해서 키 선택
// 3. iss 검증: 토큰이 우리 Supabase 프로젝트에서 발급된 건지 확인
// 4. aud를 클레임 구조체에 포함 (디버깅용)
// 5. 키 타입/알고리즘 방어 체크 (kty, alg, crv)
// 6. 불필요한 .to_string() 제거

use axum::{
    extract::Request,
    http::{header::AUTHORIZATION, StatusCode},
    middleware::Next,
    response::Response,
};
use jsonwebtoken::{decode, decode_header, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};
use std::sync::Arc;
use tokio::sync::RwLock;
use uuid::Uuid;

use crate::auth::app_jwt::verify_app_access_token;
use crate::state::AppState;

// ── JWKS 캐시 구조체 ────────────────────────────────────────
// 매 요청마다 Supabase JWKS 엔드포인트를 호출하면:
// - 성능 낭비 (매번 HTTP 왕복)
// - Supabase 응답 지연 시 API 전체 지연
// - 네트워크 이슈에 인증이 직접 영향
//
// 그래서 JWKS를 메모리에 캐시하고, 일정 시간마다 또는
// kid를 못 찾을 때만 새로 가져옴.

#[derive(Clone)]
pub struct JwksCache {
    // Arc<RwLock<>>: 여러 요청이 동시에 읽을 수 있고,
    // 갱신할 때만 쓰기 잠금을 거는 구조
    inner: Arc<RwLock<JwksCacheInner>>,
}

struct JwksCacheInner {
    // JWKS 응답 원본 (keys 배열)
    keys: Vec<serde_json::Value>,
    // 마지막으로 fetch한 시각
    fetched_at: std::time::Instant,
}

impl JwksCache {
    pub fn new() -> Self {
        Self {
            inner: Arc::new(RwLock::new(JwksCacheInner {
                keys: Vec::new(),
                fetched_at: std::time::Instant::now(),
            })),
        }
    }

    // 캐시에서 kid에 해당하는 키를 찾음
    // 못 찾으면 None 반환 → 호출자가 refresh 후 재시도
    async fn find_key(&self, kid: &str) -> Option<serde_json::Value> {
        let inner = self.inner.read().await;
        inner.keys.iter()
            .find(|k| k["kid"].as_str() == Some(kid))
            .cloned()
    }

    // 캐시가 비어있거나 5분 이상 지났으면 true
    async fn needs_refresh(&self) -> bool {
        let inner = self.inner.read().await;
        inner.keys.is_empty()
            || inner.fetched_at.elapsed() > std::time::Duration::from_secs(300)
    }

    // JWKS 엔드포인트에서 새로 가져와서 캐시 갱신
    async fn refresh(
        &self,
        http_client: &reqwest::Client,
        jwks_url: &str,
    ) -> Result<(), (StatusCode, String)> {
        let jwks: serde_json::Value = http_client.get(jwks_url)
            .send()
            .await
            .map_err(|e| {
                tracing::error!("JWKS 요청 실패: {}", e);
                (StatusCode::INTERNAL_SERVER_ERROR, "JWKS 요청 실패".to_string())
            })?
            .json()
            .await
            .map_err(|e| {
                tracing::error!("JWKS JSON 파싱 실패: {}", e);
                (StatusCode::INTERNAL_SERVER_ERROR, "JWKS 파싱 실패".to_string())
            })?;

        let keys = jwks["keys"]
            .as_array()
            .ok_or_else(|| {
                tracing::error!("JWKS 응답에 keys 배열 없음");
                (StatusCode::INTERNAL_SERVER_ERROR, "JWKS 키 없음".to_string())
            })?
            .clone();

        let mut inner = self.inner.write().await;
        inner.keys = keys;
        inner.fetched_at = std::time::Instant::now();

        tracing::debug!("JWKS 캐시 갱신 완료 (키 {}개)", inner.keys.len());
        Ok(())
    }
}

// ── Supabase JWT 클레임 구조체 ──────────────────────────────
// Supabase가 발급하는 JWT 안에 들어있는 필드들
// aud: 문자열일 수도 있고 배열일 수도 있어서 serde_json::Value로 받음
// iss: 토큰 발급자. 우리 Supabase 프로젝트 URL이어야 함

#[derive(Debug, Serialize, Deserialize)]
pub struct SupabaseClaims {
    // 유저 ID. Supabase auth.users의 id와 동일
    pub sub: String,
    // 토큰 만료 시각 (Unix timestamp)
    pub exp: u64,
    pub email: Option<String>,
    // Supabase 역할
    pub role: Option<String>,
    // 토큰 발급자 (예: "https://xxxxx.supabase.co/auth/v1")
    pub iss: Option<String>,
    // audience. 문자열 또는 배열일 수 있어서 Value로 받음
    pub aud: Option<serde_json::Value>,
}

pub async fn jwt_middleware(
    axum::extract::State(state): axum::extract::State<AppState>,
    mut request: Request,
    next: Next,
) -> Result<Response, (StatusCode, String)> {

    // ── 1) Authorization 헤더에서 토큰 추출 ──────────────────

    let header = request.headers()
        .get(AUTHORIZATION)
        .ok_or_else(|| {
            tracing::warn!("Authorization 헤더 없음");
            (StatusCode::UNAUTHORIZED, "Authorization 헤더 없음".to_string())
        })?;

    let value = header.to_str().map_err(|e| {
        tracing::warn!("헤더 형식 오류: {}", e);
        (StatusCode::UNAUTHORIZED, "헤더 형식 오류".to_string())
    })?;

    // strip_prefix가 &str을 돌려주므로 .to_string() 없이 &str 그대로 사용
    let token = value.strip_prefix("Bearer ")
        .map(str::trim).filter(|t| !t.is_empty())
        .ok_or_else(|| {
            tracing::warn!("Bearer 토큰 없음 (헤더값: {})", value);
            (StatusCode::UNAUTHORIZED, "Bearer 토큰 없음".to_string())
        })?;

    // 1) 먼저 우리 자체 JWT 검증 시도
    if let Ok(app_claims) = verify_app_access_token(&state.config.app_jwt_secret, token) {
        let user_id = Uuid::parse_str(&app_claims.sub).map_err(|e| {
            tracing::warn!("앱 JWT user_id 파싱 실패: sub={}, error={}", app_claims.sub, e);
            (StatusCode::UNAUTHORIZED, "유저 ID 파싱 실패".to_string())
        })?;

        tracing::debug!("앱 JWT 검증 통과: user_id={}", user_id);

        request.extensions_mut().insert(user_id);
        return Ok(next.run(request).await);
    }

    // 2) 실패하면 Supabase JWT 검증 시도
    let user_id = verify_supabase_jwt(&state, token).await?;
    request.extensions_mut().insert(user_id);

    Ok(next.run(request).await)
}

    // ── 2) JWT 헤더에서 kid 추출 ─────────────────────────────
    // JWT header에 있는 kid(Key ID)로 JWKS에서 맞는 공개키를 찾음

    async fn verify_supabase_jwt(
        state: &AppState,
        token: &str,
    ) -> Result<Uuid, (StatusCode, String)> {

    let jwt_header = decode_header(token).map_err(|e| {
        tracing::warn!("JWT 헤더 디코딩 실패: {}", e);
        (StatusCode::UNAUTHORIZED, "JWT 헤더 디코딩 실패".to_string())
    })?;

    let kid = jwt_header.kid.ok_or_else(|| {
        tracing::warn!("JWT 헤더에 kid 없음");
        (StatusCode::UNAUTHORIZED, "JWT에 kid가 없음".to_string())
    })?;

    // ── 3) JWKS 캐시에서 키 찾기 ────────────────────────────
    // 1차: 캐시가 오래됐으면 미리 갱신
    // 2차: 캐시에서 kid로 검색
    // 3차: 못 찾으면 강제 갱신 후 재검색 (키 로테이션 대응)

    let jwks_cache = &state.jwks_cache;

    // 캐시가 비어있거나 5분 지났으면 먼저 갱신
    if jwks_cache.needs_refresh().await {
        jwks_cache.refresh(&state.http_client, &state.config.jwks_url).await?;
    }

    // 캐시에서 kid로 키 검색
    let key = match jwks_cache.find_key(&kid).await {
        Some(k) => k,
        None => {
            // 캐시에 없으면 → 키 로테이션이 발생했을 수 있음
            // 강제로 JWKS를 다시 가져온 뒤 재검색
            tracing::info!("kid={} 캐시 미스, JWKS 강제 갱신", kid);
            jwks_cache.refresh(&state.http_client, &state.config.jwks_url).await?;
            jwks_cache.find_key(&kid).await.ok_or_else(|| {
                tracing::warn!("JWKS 갱신 후에도 kid={} 에 해당하는 키 없음", kid);
                (StatusCode::UNAUTHORIZED, format!("kid={}에 해당하는 공개키 없음", kid))
            })?
        }
    };

    // ── 4) 키 타입/알고리즘 방어 체크 ────────────────────────
    // JWKS 키가 우리가 기대하는 형태인지 확인
    // kty: "EC" (타원곡선), alg: "ES256", crv: "P-256"
    // 이걸 안 하면 잘못된 키 타입으로 검증 시도할 수 있음

    let kty = key["kty"].as_str().unwrap_or("");
    let alg = key["alg"].as_str().unwrap_or("");
    let crv = key["crv"].as_str().unwrap_or("");

    if kty != "EC" || alg != "ES256" || crv != "P-256" {
        tracing::error!(
            "JWKS 키 스펙 불일치: kid={}, kty={}, alg={}, crv={}",
            kid, kty, alg, crv
        );
        return Err((
            StatusCode::INTERNAL_SERVER_ERROR,
            "JWKS 키 스펙이 예상과 다름".to_string(),
        ));
    }

    // ── 5) 공개키 생성 + JWT 검증 ────────────────────────────

    let x = key["x"].as_str().ok_or_else(|| {
        tracing::error!("JWKS 키에 x 값 없음 (kid={})", kid);
        (StatusCode::INTERNAL_SERVER_ERROR, "JWKS x 없음".to_string())
    })?;

    let y = key["y"].as_str().ok_or_else(|| {
        tracing::error!("JWKS 키에 y 값 없음 (kid={})", kid);
        (StatusCode::INTERNAL_SERVER_ERROR, "JWKS y 없음".to_string())
    })?;

    let decoding_key = DecodingKey::from_ec_components(x, y)
        .map_err(|e| {
            tracing::error!("공개키 생성 실패 (kid={}): {}", kid, e);
            (StatusCode::INTERNAL_SERVER_ERROR, "공개키 생성 실패".to_string())
        })?;

    // aud + iss 동시 검증
    // aud: "authenticated" (Supabase 기본값)
    // iss: "{supabase_url}/auth/v1" (우리 프로젝트에서 발급된 건지 확인)
    let expected_issuer = format!("{}/auth/v1", state.config.supabase_url.trim_end_matches('/'));

    let mut validation = Validation::new(Algorithm::ES256);
    validation.set_audience(&["authenticated"]);
    validation.set_issuer(&[&expected_issuer]);

    let claims = decode::<SupabaseClaims>(token, &decoding_key, &validation)
        .map_err(|e| {
            tracing::warn!("토큰 검증 실패 (kid={}): {}", kid, e);
            (StatusCode::UNAUTHORIZED, "유효하지 않은 토큰".to_string())
        })?.claims;

    // ── 6) user_id를 request에 넣기 ─────────────────────────

    let user_id = Uuid::parse_str(&claims.sub)
        .map_err(|e| {
            tracing::warn!("유저 ID 파싱 실패: sub={}, error={}", claims.sub, e);
            (StatusCode::UNAUTHORIZED, "유저 ID 파싱 실패".to_string())
        })?;

        tracing::debug!("Supabase JWT 검증 통과: user_id={}, kid={}", user_id, kid);
        Ok(user_id)
}