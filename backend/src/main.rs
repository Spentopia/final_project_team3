mod config;
mod state;
mod auth;
mod route;

use axum::{Router,routing::get};
use tower_http::trace::TraceLayer;

#[tokio::main]
async fn main() {

    //.env 파일 로드
    dotenv::dotenv().ok();

    //로깅 초기화
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .with_target(true)
        .with_file(true)
        .with_line_number(true)
        .init();

    //설정값 로드
    let config = config::Config::from_env().expect("설정 로드 실패");
    tracing::info!("Supabase URL: {}", config.supabase_url);

    //AppState 생성
    let state = state::AppState::new(config);

    tracing::info!("서버 시작중...");

    //라우터 구성
    let app = Router::new()
        .route("/health",get(|| async { "ok" }))
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    //서버 실행
    let listener = tokio::net::TcpListener::bind("127.0.0.1:1113").await
        .expect("포트 바인딩 실패");

    tracing::info!("서버 실행: http://localhost:1113");

    axum::serve(listener, app)
        .await
        .expect("서버 실행 실패");

}
