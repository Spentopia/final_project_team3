// src/main.rs
//
// 서버 진입점
//
// 변경 핵심:
// - CORS origin 하드코딩 제거
// - allow_credentials(true) 추가
// - 쿠키/Authorization 헤더 허용
//
// 이유:
// - 웹 refresh token을 HttpOnly 쿠키로 보낼 것이므로
//   브라우저가 쿠키를 포함할 수 있게 credentials 허용이 필요함.

mod config;
mod state;
mod auth;
mod route;
mod openapi;
pub mod wallet;

use axum::http::{header, HeaderValue, Method};
use tower_http::cors::CorsLayer;
use tower_http::trace::TraceLayer;

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();

    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .with_target(true)
        .with_file(true)
        .with_line_number(true)
        .init();

    let config = config::Config::from_env().expect("설정 로드 실패");
    tracing::info!("Supabase URL: {}", config.supabase_url);

    let state = state::AppState::new(config.clone());

    tracing::info!("서버 시작중...");

    // credentials(true)와 allow_origin("*")은 같이 못 쓴다.
    // 반드시 구체적인 origin을 명시해야 한다.
    let cors = CorsLayer::new()
        .allow_origin(config.cors_origin.parse::<HeaderValue>().unwrap())
        .allow_methods([
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::PATCH,
            Method::DELETE,
            Method::OPTIONS,
        ])
        .allow_headers([
            header::CONTENT_TYPE,
            header::AUTHORIZATION,
            header::COOKIE,
            "x-client-type".parse().unwrap(),
        ])
        .allow_credentials(true);

    let app = route::create_router(state)
        .layer(cors)
        .layer(TraceLayer::new_for_http());

    let listener = tokio::net::TcpListener::bind("127.0.0.1:1113").await
        .expect("포트 바인딩 실패");

    tracing::info!("서버 실행: http://localhost:1113");

    axum::serve(listener, app)
        .await
        .expect("서버 실행 실패");
}