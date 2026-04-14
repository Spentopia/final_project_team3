// config.rs
// 환경변수에서 설정값을 읽어와서 Config 구조체에 담는 파일임.
// 서버 시작할 때 딱 한 번 실행되고, 이후에는 AppState를 통해 전달됨.

use anyhow::{Context, Result};

// Clone: 이 구조체를 복사할 수 있게 해줌.
// AppState에 넣어서 여러 핸들러에 전달할 때 필요함.
#[derive(Debug,Clone)]
pub struct Config {
    //Supabase 프로젝트 URL
    pub supabase_url: String,

    //프론트에서 Supabase 직접 호출할 때 쓰는 키
    pub supabase_publishable_key: String,

    //Supabase Admin API 호출할 때 이 키를 Authorization 헤더에 넣음
    pub supabase_secret_key: String,

    //Supabase jWT 검증에 필요한 공개키 목록 가져오는 URL
    pub jwks_url: String,

    pub app_jwt_secret: String,
}

impl Config {
    //환경변수에서 값을 읽어 Config를 만드는 함수
    pub fn from_env() -> Result<Self> {
        Ok(Self {
            supabase_url: std::env::var("SUPABASE_URL")
            .context("SUPABASE_URL 없음")?,
            supabase_publishable_key: std::env::var("SUPABASE_PUBLISHABLE_KEY")
            .context("SUPABASE_PUBLISHABLE_KEY 없음")?,
            supabase_secret_key: std::env::var("SUPABASE_SECRET_KEY")
            .context("SUPABASE_SECRET_KEY 없음")?,
            jwks_url: std::env::var("JWKS_URL")
            .context("JWKS_URL 없음")?,
            app_jwt_secret: std::env::var("APP_JWT_SECRET")
                .context("APP_JWT_SECRET 없음")?,
        })
    }
}