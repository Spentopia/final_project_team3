// auth/app_jwt.rs
use anyhow::{anyhow, Result};
use chrono::{Duration, Utc};
use jsonwebtoken::{decode, encode, Algorithm, DecodingKey, EncodingKey, Header, Validation};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize)]
pub struct AppClaims {
    pub sub: String,        // user_id
    pub exp: usize,         // 만료 시각
    pub token_type: String, // "access" | "refresh"
}

#[derive(Debug, Clone)]
pub struct AppTokenPair {
    pub access_token: String,
    pub refresh_token: String,
}

pub fn generate_app_tokens(secret: &str, user_id: &Uuid) -> Result<AppTokenPair> {
    let now = Utc::now();

    let access_claims = AppClaims {
        sub: user_id.to_string(),
        exp: (now + Duration::hours(1)).timestamp() as usize,
        token_type: "access".to_string(),
    };

    let refresh_claims = AppClaims {
        sub: user_id.to_string(),
        exp: (now + Duration::days(30)).timestamp() as usize,
        token_type: "refresh".to_string(),
    };

    let access_token = encode(
        &Header::default(),
        &access_claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )?;

    let refresh_token = encode(
        &Header::default(),
        &refresh_claims,
        &EncodingKey::from_secret(secret.as_bytes()),
    )?;

    Ok(AppTokenPair {
        access_token,
        refresh_token,
    })
}

pub fn verify_app_access_token(secret: &str, token: &str) -> Result<AppClaims> {
    let mut validation = Validation::new(Algorithm::HS256);
    validation.validate_exp = true;

    let data = decode::<AppClaims>(
        token,
        &DecodingKey::from_secret(secret.as_bytes()),
        &validation,
    )?;

    if data.claims.token_type != "access" {
        return Err(anyhow!("access 토큰이 아님"));
    }

    Ok(data.claims)
}