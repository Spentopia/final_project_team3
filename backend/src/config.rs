// src/config.rs
//
// 서버에서 쓰는 환경변수들을 읽어서 Config 구조체로 만드는 파일.
// 프론트의 .env와 달리 여기는 절대 브라우저로 노출되면 안 되는 값들도 포함됨.
//
// 기존 역할:
// - Supabase URL / secret / publishable key
// - 앱 JWT secret
// - 카카오 OAuth 설정
//
// 이번 변경:
// - CORS_ORIGIN 추가
// - ENVIRONMENT 추가
//   → production 여부 판단해서 쿠키 Secure 옵션에 사용

use anyhow::{Context, Result};
use chrono::NaiveDate;

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
    pub kakao_app_redirect_uri: String,

    // 프로필 이미지 저장용 private bucket 이름
    pub supabase_profile_image_bucket: String,

    // 커뮤니티 게시글 이미지 저장용 bucket 이름
    pub supabase_community_bucket: String,

    // CORS 허용 origin
    pub cors_origin: String,

    // 로컬/배포 구분
    pub environment: String,

    // AI 서버 URL (챗봇, 소비 분석, 예산 플랜)
    pub ai_server_url: String,

    // Unity 서버 URL (아바타 렌더링, 게임 연동)
    pub unity_server_url: String,

    pub turnstile_secret_key: String,

    // 서비스 시작일 — 반감기 계산 기준점
    // 환경변수: SERVICE_LAUNCH_DATE (형식: YYYY-MM-DD), 없으면 2025-01-01
    pub service_launch_date: NaiveDate,

    // Solana RPC 엔드포인트 (Helius RPC URL 권장: https://devnet.helius-rpc.com/?api-key=xxx)
    pub solana_rpc_url: String,

    // x402 분석 결제 설정
    pub solana_x402_network: String,
    pub solana_usdc_mint: String,
    pub solana_platform_wallet: String,
    pub analysis_weekly_price_micro: u64,
    pub analysis_monthly_price_micro: u64,
    pub analysis_weekly_free_limit: i32,
    pub analysis_monthly_free_limit: i32,

    // Helius API 키 — Enhanced Transaction API 트랜잭션 검증에 사용
    pub helius_api_key: String,

    // admin 키페어 (base58 인코딩된 64바이트 ed25519 키페어)
    // `solana-keygen` 출력 JSON을 base58로 인코딩해서 저장
    pub solana_admin_keypair: String,

    // 배포된 Spentopia 프로그램 ID
    pub solana_program_id: String,

    // 프로젝트 전용 아바타 컬렉션 mint 주소
    // 없으면 NFT collection 조회 API는 빈 결과/에러 처리한다.
    pub solana_avatar_collection_mint: String,

    // Unity 상자에서 아무 아이템도 나오지 않는 "꽝" 가중치.
    // item_master.drop_weight와 같은 단위로 계산한다.
    pub reward_box_miss_weight: u32,

    pub frontend_origin: String,
}

impl Config {
    pub fn from_env() -> Result<Self> {
        let helius_api_key =
            std::env::var("HELIUS_API_KEY").context("HELIUS_API_KEY 환경변수 없음")?;
        let solana_rpc_url = std::env::var("SOLANA_RPC_URL").unwrap_or_else(|_| {
            format!("https://devnet.helius-rpc.com/?api-key={}", helius_api_key)
        });

        Ok(Self {
            supabase_url: std::env::var("SUPABASE_URL").context("SUPABASE_URL 환경변수 없음")?,

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

            kakao_app_redirect_uri: std::env::var("KAKAO_APP_REDIRECT_URI")
                .or_else(|_| std::env::var("KAKAO_REDIRECT_URI"))
                .context("KAKAO_APP_REDIRECT_URI 또는 KAKAO_REDIRECT_URI 환경변수 없음")?,

            supabase_profile_image_bucket: std::env::var("SUPABASE_PROFILE_IMAGE_BUCKET")
                .context("SUPABASE_PROFILE_IMAGE_BUCKET 환경변수 없음")?,

            supabase_community_bucket: std::env::var("SUPABASE_COMMUNITY_BUCKET")
                .unwrap_or_else(|_| "posts".to_string()),

            // 프론트 origin 하드코딩 대신 환경변수 사용
            cors_origin: std::env::var("CORS_ORIGIN")
                .unwrap_or_else(|_| "http://localhost:5173".to_string()),

            environment: std::env::var("ENVIRONMENT").unwrap_or_else(|_| "local".to_string()),

            ai_server_url: std::env::var("AI_SERVER_URL")
                .unwrap_or_else(|_| "http://localhost:8000".to_string()),

            unity_server_url: std::env::var("UNITY_SERVER_URL")
                .unwrap_or_else(|_| "http://localhost:9000".to_string()),

            turnstile_secret_key: std::env::var("TURNSTILE_SECRET_KEY")
                .expect("TURNSTILE_SECRET_KEY 환경변수 없음"),

            service_launch_date: NaiveDate::parse_from_str(
                &std::env::var("SERVICE_LAUNCH_DATE").unwrap_or_else(|_| "2025-01-01".to_string()),
                "%Y-%m-%d",
            )
            .expect("SERVICE_LAUNCH_DATE 형식 오류. YYYY-MM-DD 형식으로 입력하세요."),

            solana_rpc_url,

            solana_x402_network: std::env::var("SOLANA_X402_NETWORK")
                .unwrap_or_else(|_| "solana-devnet".to_string()),

            solana_usdc_mint: std::env::var("SOLANA_USDC_MINT")
                .unwrap_or_else(|_| "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU".to_string()),

            solana_platform_wallet: std::env::var("SOLANA_PLATFORM_WALLET").unwrap_or_default(),

            analysis_weekly_price_micro: std::env::var("ANALYSIS_WEEKLY_PRICE_MICRO")
                .ok()
                .and_then(|v| v.parse::<u64>().ok())
                .unwrap_or(100_000),

            analysis_monthly_price_micro: std::env::var("ANALYSIS_MONTHLY_PRICE_MICRO")
                .ok()
                .and_then(|v| v.parse::<u64>().ok())
                .unwrap_or(200_000),

            analysis_weekly_free_limit: std::env::var("ANALYSIS_WEEKLY_FREE_LIMIT")
                .ok()
                .and_then(|v| v.parse::<i32>().ok())
                .unwrap_or(2),

            analysis_monthly_free_limit: std::env::var("ANALYSIS_MONTHLY_FREE_LIMIT")
                .ok()
                .and_then(|v| v.parse::<i32>().ok())
                .unwrap_or(4),

            helius_api_key,

            solana_admin_keypair: std::env::var("SOLANA_ADMIN_KEYPAIR").unwrap_or_default(),

            solana_program_id: std::env::var("SOLANA_PROGRAM_ID")
                .unwrap_or_else(|_| "9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61".to_string()),

            solana_avatar_collection_mint: std::env::var("SOLANA_AVATAR_COLLECTION_MINT")
                .unwrap_or_default(),

            reward_box_miss_weight: std::env::var("REWARD_BOX_MISS_WEIGHT")
                .ok()
                .and_then(|v| v.parse::<u32>().ok())
                .unwrap_or(9000),
            frontend_origin: std::env::var("FRONTEND_ORIGIN")?,
        })
    }
}
