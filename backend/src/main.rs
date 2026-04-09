// main.rs
// 서버의 진입점(entry point). 자바의 main() 메서드와 동일.
// 여기서 하는 일:
// 1) .env 파일에서 환경변수 로드
// 2) 로깅 시스템 초기화
// 3) 설정값 로드 + AppState 생성
// 4) 라우터 구성 (route.rs에 위임)
// 5) 서버 실행
//
// 이전에는 main.rs 안에서 Router::new()로 직접 라우트를 등록했는데,
// 라우트가 많아지면 main.rs가 비대해지니까
// route.rs로 분리해서 create_router()를 호출하는 구조로 변경


mod config;
mod state;
mod auth;
mod route;
mod openapi;

use tower_http::trace::TraceLayer;

#[tokio::main]
async fn main() {

    // ── 1) .env 파일 로드 ────────────────────────────────────
    // .env 파일에 있는 SUPABASE_URL, SUPABASE_SECRET_KEY 등을
    // 환경변수로 등록함. ok()는 .env 파일이 없어도 패닉 안 하겠다는 뜻.
    // 배포 환경에서는 .env 대신 시스템 환경변수를 쓰니까 없을 수 있음.
    dotenv::dotenv().ok();

    // ── 2) 로깅 초기화 ──────────────────────────────────────
    // with_env_filter: RUST_LOG 환경변수로 로그 레벨 제어 (debug/info/warn/error)
    // with_target: 어느 모듈에서 발생했는지 표시 (예: backend::auth::middleware)
    // with_file: 파일명 표시 (예: src/auth/middleware.rs)
    // with_line_number: 몇 번째 줄인지 표시
    tracing_subscriber::fmt()
        .with_env_filter(tracing_subscriber::EnvFilter::from_default_env())
        .with_target(true)
        .with_file(true)
        .with_line_number(true)
        .init();

    // ── 3) 설정값 로드 + AppState 생성 ──────────────────────
    // Config::from_env()가 환경변수를 읽어서 Config 구조체를 만듦.
    // 하나라도 빠져있으면 여기서 에러 메시지와 함께 서버가 종료
    let config = config::Config::from_env().expect("설정 로드 실패");

    // 서버 시작 시 어떤 Supabase 프로젝트에 연결하는지 로그로 확인
    tracing::info!("Supabase URL: {}", config.supabase_url);

    //AppState 생성
    let state = state::AppState::new(config);

    tracing::info!("서버 시작중...");

    // ── 4) 라우터 구성 ──────────────────────────────────────
    // route::create_router()가 공개/보호 라우트를 조립해서 Router를 반환함.
    // .layer(TraceLayer): 모든 라우트에 요청/응답 자동 로깅 적용
    let app = route::create_router(state)
        .layer(TraceLayer::new_for_http());

    //서버 실행
    let listener = tokio::net::TcpListener::bind("127.0.0.1:1113").await
        .expect("포트 바인딩 실패");

    tracing::info!("서버 실행: http://localhost:1113");

    axum::serve(listener, app)
        .await
        .expect("서버 실행 실패");

}
