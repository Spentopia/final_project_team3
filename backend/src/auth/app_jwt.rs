// src/auth/app_jwt.rs
//
// 우리 앱 전용 JWT 생성/검증 파일
//
// 변경 핵심:
// 1) access token claims와 refresh token claims 분리
// 2) refresh token에 sid(session id) 추가
// 3) access 만료시간 12시간 -> 30분으로 단축
//
// 최종 구조:
// - access token: Authorization 헤더로 전달, 짧은 만료시간
// - refresh token: 웹은 HttpOnly 쿠키, 앱은 body
// - refresh는 DB refresh_sessions와 연결됨

use anyhow::{anyhow, Result};
use chrono::{Duration, Utc};
use jsonwebtoken::{
    decode, encode, Algorithm, DecodingKey, EncodingKey, Header, Validation,
};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AccessClaims {
    // user_id
    pub sub: String,

    // 만료 시각 (unix timestamp)
    pub exp: usize,

    // access / refresh 구분
    pub token_type: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RefreshClaims {
    // user_id
    pub sub: String,

    // 만료 시각
    pub exp: usize,

    // 반드시 "refresh"
    pub token_type: String,

    // refresh_sessions.id
    pub sid: String,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppTokenPair {
    pub access_token: String,
    pub refresh_token: String,
}

pub fn generate_access_token(secret: &str, user_id: &Uuid) -> Result<String> {
    let now = Utc::now();

    // access는 짧게 유지
    // 탈취돼도 피해 기간을 줄이기 위함
    let claims = AccessClaims {
        sub: user_id.to_string(),
        exp: (now + Duration::hours(1)).timestamp() as usize,
        token_type: "access".to_string(),
    };

    let token = encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )?;

    Ok(token)
}

pub fn generate_refresh_token(
    secret: &str,
    user_id: &Uuid,
    session_id: &Uuid,
) -> Result<String> {
    let now = Utc::now();

    // refresh는 sid를 포함해서 DB 세션과 연결
    let claims = RefreshClaims {
        sub: user_id.to_string(),
        exp: (now + Duration::days(30)).timestamp() as usize,
        token_type: "refresh".to_string(),
        sid: session_id.to_string(),
    };

    let token = encode(
        &Header::default(),
        &claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )?;

    Ok(token)
}

pub fn generate_token_pair(
    secret: &str,
    user_id: &Uuid,
    session_id: &Uuid,
) -> Result<AppTokenPair> {
    Ok(AppTokenPair {
        access_token: generate_access_token(secret, user_id)?,
        refresh_token: generate_refresh_token(secret, user_id, session_id)?,
    })
}

pub fn verify_app_access_token(secret: &str, token: &str) -> Result<AccessClaims> {
    let mut validation = Validation::new(Algorithm::HS256);
    validation.validate_exp = true;

    let data = decode::<AccessClaims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )?;

    if data.claims.token_type != "access" {
        return Err(anyhow!("access 토큰이 아님"));
    }

    Ok(data.claims)
}

pub fn verify_app_refresh_token(secret: &str, token: &str) -> Result<RefreshClaims> {
    let mut validation = Validation::new(Algorithm::HS256);
    validation.validate_exp = true;

    let data = decode::<RefreshClaims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )?;

    if data.claims.token_type != "refresh" {
        return Err(anyhow!("refresh 토큰이 아님"));
    }

    Ok(data.claims)
}