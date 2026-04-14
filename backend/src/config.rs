// src/config.rs
//
// 서버에서 쓰는 환경변수들을 읽어서 Config 구조체로 만드는 파일.
// 프론트의 .env와 달리 여기는 절대 브라우저로 노출되면 안 되는 값들도 포함됨.
//
// 역할:
// - Supabase 프로젝트 URL
// - Supabase service role key
// - Supabase publishable key
// - 우리 앱 JWT 서명용 secret
// - 카카오 OAuth 관련 설정

use anyhow::{Context, Result};

#[derive(Clone)]
pub struct Config {
    // Supabase 프로젝트 URL
    pub supabase_url: String,

    // 서버 전용. 절대 프론트에 노출 금지.
    // public.users 등을 service role 권한으로 읽고 쓸 때 사용
    pub supabase_secret_key: String,

    // Supabase Auth / OAuth 진입용 공개키
    pub supabase_publishable_key: String,

    // 우리 앱 자체 JWT 서명용 secret
    pub app_jwt_secret: String,

    // 카카오 OAuth
    pub kakao_rest_api_key: String,
    pub kakao_client_secret: String,
    pub kakao_redirect_uri: String,
}

impl Config {
    pub fn from_env() -> Result<Self> {
        Ok(Self {
            supabase_url: std::env::var("SUPABASE_URL")
                .context("SUPABASE_URL 환경변수 없음")?,

            supabase_secret_key: std::env::var("SUPABASE_SECRET_KEY")
                .context("SUPABASE_SECRET_KEY 환경변수 없음")?,

            supabase_publishable_key: std::env::var("SUPABASE_PUBLISHABLE_KEY")
                .context("SUPABASE_PUBLISHABLE_KEY 환경변수 없음")?,

            app_jwt_secret: std::env::var("APP_JWT_SECRET")
                .context("APP_JWT_SECRET 환경변수 없음")?,

            kakao_rest_api_key: std::env::var("KAKAO_REST_API_KEY")
                .context("KAKAO_REST_API_KEY 환경변수 없음")?,

            kakao_client_secret: std::env::var("KAKAO_CLIENT_SECRET")
                .context("KAKAO_CLIENT_SECRET 환경변수 없음")?,

            kakao_redirect_uri: std::env::var("KAKAO_REDIRECT_URI")
                .context("KAKAO_REDIRECT_URI 환경변수 없음")?,
        })
    }
}