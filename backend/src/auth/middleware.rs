// auth/middleware.rs
// 모든 보호된 API 앞단에서 JWT를 검증하는 미들웨어
// 검증 통과하면 user_id를 request extension에 넣어서 핸들러로 넘김.
// 검증 실패하면 여기서 바로 401 반환
// 스프링부트의 filter & interceptor와 동일

use axum::{
    extract::{Request },
    http::{header::AUTHORIZATION, StatusCode},
    middleware::{Next},
    response::{ Response},
};
use jsonwebtoken::{decode, Algorithm, DecodingKey, Validation};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::state::AppState;

// Supabase JWT의 클레임 구조체
// Supabase가 발급하는 JWT안에 드러있는 필드들
#[derive(Debug, Serialize, Deserialize)]
pub struct SupabaseClaims {

    //유저 ID. Supabase auth.users의 id와 동일
    pub sub: String,

    //토큰 만료 시각 Unix timestamp 형태
    pub exp: u64,

    pub email: Option<String>,

    // Supabase 역할
    pub role: Option<String>,
}

pub async fn jwt_middleware(

    //AppState를 꺼내서 jwks_url 등 설정값 사용 가능
    axum::extract::State(state): axum::extract::State<AppState>,

    //현재 들어온 HTTP 요청
    mut request: Request,

    //다음 미들웨어 또는 핸들러로 넘기는 객체
    next: Next,
) -> Result<Response,(StatusCode, String)> {

    // Authorization 헤더가 없으면 401반환
    let header = request.headers().
    get(AUTHORIZATION)
        .ok_or_else(|| {
            tracing::warn!("Authorization 헤더 없음");
            (StatusCode::UNAUTHORIZED, "Authorization 헤더 없음".to_string())
        })?;

    //헤더값을 문자열로 변환
    let value = header.to_str().map_err(|e| {
        tracing::warn!("헤더 형식 오류: {}", e);
        (StatusCode::UNAUTHORIZED, "헤더 형식 오류".to_string())
    })?;

    // "Bearer 토큰값" 형태에서 토큰값만 추출
    let token = value.strip_prefix("Bearer ")
        .map(str::trim).filter(|t| !t.is_empty())
        .ok_or_else(|| {
            tracing::warn!("Bearer 토큰 없음 (헤더값: {})", value);
            (StatusCode::UNAUTHORIZED, "Bearer 토큰 없음".to_string())
        })?.to_string();

    // Supabase JWKS 엔드포인트에서 공개키를 가져옴.
    // 이 공개키로 JWT 서명을 검증함
    let jwks: serde_json::Value = state.http_client.get(&state.config.jwks_url)
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

    // JWKS에서 첫 번쨰 키의 x, y 값을 꺼내서 DecodingKey를 만듦
    let keys = jwks["keys"]
        .as_array()
        .ok_or_else(|| {
            tracing::error!("JWKS 응답에 keys 배열 없음");
            (StatusCode::INTERNAL_SERVER_ERROR, "JWKS 키 없음".to_string())
        })?;

    let key = keys
        .first()
        .ok_or_else(|| {
            tracing::error!("JWKS keys 배열이 비어있음");
            (StatusCode::INTERNAL_SERVER_ERROR, "JWKS 키 없음".to_string())
        })?;

    let x = key["x"]
        .as_str()
        .ok_or_else(|| {
            tracing::error!("JWKS 키에 x 값 없음");
            (StatusCode::INTERNAL_SERVER_ERROR, "JWKS x 없음".to_string())
        })?;

    let y = key["y"]
        .as_str()
        .ok_or_else(|| {
            tracing::error!("JWKS 키에 y 값 없음");
            (StatusCode::INTERNAL_SERVER_ERROR, "JWKS y 없음".to_string())
        })?;

    // x, y 값으로 ECC P-256 공개키 만들기
    let decoding_key = DecodingKey::from_ec_components(x,y)
        .map_err(|e| {
            tracing::error!("공개키 생성 실패: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, "공개키 생성 실패".to_string())
        })?;

    // ES256 알고리즘으로 JWT를 검증
    let mut validation = Validation::new(Algorithm::ES256);
    validation.set_audience(&["authenticated"]);


    let claims = decode::<SupabaseClaims>(&token, &decoding_key, &validation)
        .map_err(|e| {
            tracing::warn!("토큰 검증 실패: {}", e);
            (StatusCode::UNAUTHORIZED, "유효하지 않은 토큰".to_string())
        })?.claims;

    // 토큰에 들어있는 "sub"는 그냥 문자열이지만 DB의 PK는 Uuid타입임
    // sub(user_id)를 Uuid로 파싱해서 request extension에 넣으면
    // 이후 핸들러에서 Extension<Uuid>로 꺼내올 수 있음
    let user_id = Uuid::parse_str(&claims.sub)
        .map_err(|e| {
            tracing::warn!("유저 ID 파싱 실패: sub={}, error={}", claims.sub, e);
            (StatusCode::UNAUTHORIZED, "유저 ID 파싱 실패".to_string())
        })?;

    // Request 객체 안에는 extensions라는 공유 저장소가 있는데
    // 여기에 user_id를 넣어두면 이 요청이 끝날 때까지 해당 유저의 ID가 요청 객체에 잘 붙어다님
    request.extensions_mut().insert(user_id);

    tracing::debug!("JWT 검증 통과: user_id={}", user_id);

    //검증 통과했으니 다음 단계로 넘김
    Ok(next.run(request).await)

}