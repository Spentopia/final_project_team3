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

// Rate Limiting 추가 (tower_governor)
//
// Rate Limiting을 왜 넣냐:
// - /auth/wallet/login, /auth/exchange, /auth/kakao/login 등
//   인증 엔드포인트에 요청 제한이 없으면
//   브루트포스(무차별 대입)나 DDoS에 노출됨
// - 보안 점검에서 HIGH로 분류된 이슈
// - tower_governor: token bucket 알고리즘 기반 IP별 요청 제한 미들웨어

mod auth;
pub mod avatar;
pub mod budget;
pub mod clients;
pub mod community;
mod config;
pub mod expense;
pub mod ledger;
pub mod market;
pub mod notification;
mod openapi;
pub mod payment;
pub mod report;
pub mod reward;
mod route;
mod state;
pub mod user;
pub mod wallet;

use axum::http::{HeaderValue, Method, header};
use std::net::SocketAddr;
use tower_http::cors::{AllowOrigin, CorsLayer};
use tower_http::trace::TraceLayer;

// ─────────────────────────────────────────────────────────────
// Rate Limiting 관련 import
//
// tower_governor: axum과 호환되는 rate limiting 미들웨어
// - GovernorConfigBuilder: 제한 규칙을 설정하는 빌더
// - GovernorLayer: axum 레이어로 등록하는 래퍼
//
// Cargo.toml에 추가 필요:
//   tower_governor = "0.4"
// ─────────────────────────────────────────────────────────────
use tower_governor::{GovernorLayer, governor::GovernorConfigBuilder};

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

    // ─────────────────────────────────────────────────────────
    // CORS 설정
    //
    // credentials(true)와 allow_origin("*")은 같이 못 쓴다.
    // 반드시 구체적인 origin을 명시해야 한다.
    //
    // allow_headers에 COOKIE와 x-client-type 추가:
    // - COOKIE: 웹 refresh 토큰이 httpOnly 쿠키로 전달되므로 필요
    // - x-client-type: 웹/앱 분기용 커스텀 헤더
    // ─────────────────────────────────────────────────────────
    let mut allowed_origins = vec![
        HeaderValue::from_static("http://localhost:5173"),
        HeaderValue::from_static("http://127.0.0.1:5173"),
    ];
    if let Ok(origin) = config.cors_origin.parse::<HeaderValue>() {
        if !allowed_origins.contains(&origin) {
            allowed_origins.push(origin);
        }
    }

    let cors = CorsLayer::new()
        .allow_origin(AllowOrigin::list(allowed_origins))
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

    // ─────────────────────────────────────────────────────────
    // Rate Limiting 설정
    //
    // token bucket 알고리즘:
    // - 버킷에 토큰이 있으면 요청 통과, 없으면 429 Too Many Requests
    // - per_second(2): 매초 2개의 토큰이 버킷에 보충됨
    // - burst_size(5): 버킷에 최대 5개까지 누적 가능
    //
    // 실제 동작:
    // - 버킷이 가득 찬 상태에서 연속 5회 요청 가능
    // - 이후부터는 초당 2회로 제한
    // - 일반 사용자가 체감할 일은 거의 없음
    //   (사람이 1초에 로그인 5번 누를 일은 없으니까)
    // - 공격자의 무차별 대입은 초당 2회로 제한됨
    //
    // IP 기반 식별:
    // - 기본적으로 요청의 소스 IP(peer_addr)를 기준으로 버킷 분리
    // - 같은 IP에서 오는 요청끼리 버킷을 공유
    // - 서로 다른 IP는 각각 독립된 버킷을 가짐
    //
    // 주의:
    // - 프록시/로드밸런서 뒤에 있으면 모든 요청이 같은 IP로 보일 수 있음
    // - 그 경우 X-Forwarded-For 기반으로 바꿔야 함
    //   (GovernorConfigBuilder에 key_extractor 설정)
    // - 지금은 로컬 시연이니까 기본값(peer_addr)으로 충분
    // ─────────────────────────────────────────────────────────
    let governor_conf = GovernorConfigBuilder::default()
        .per_second(10) // 초당 10개 토큰 보충
        .burst_size(30) // 최대 30개까지 누적 (페이지 로드 시 초기 요청 묶음 허용)
        .finish()
        .unwrap();

    let governor_limiter = GovernorLayer {
        config: std::sync::Arc::new(governor_conf),
    };

    // ─────────────────────────────────────────────────────────
    // nonce_store 주기적 정리 (백그라운드 태스크)
    //
    // nonce는 5분 TTL인데, verify_and_login에서만 만료 체크를 함.
    // nonce를 요청만 하고 로그인을 안 하면 만료된 nonce가
    // 메모리에 계속 남아서 메모리 누수가 됨.
    //
    // 5분마다 만료된 nonce를 정리해서 메모리를 깨끗하게 유지.
    // ─────────────────────────────────────────────────────────
    let nonce_store = state.nonce_store.clone();
    tokio::spawn(async move {
        loop {
            tokio::time::sleep(tokio::time::Duration::from_secs(300)).await;
            let now = std::time::SystemTime::now();
            nonce_store.retain(|_, entry| entry.expires_at > now);
            tracing::debug!("만료된 nonce 정리 완료");
        }
    });

    // ─────────────────────────────────────────────────────────
    // 라우터 구성 + 레이어 적용
    //
    // 레이어 적용 순서 (바깥 → 안쪽):
    //   요청 → TraceLayer → CORS → GovernorLayer → 라우터
    //
    // axum에서 .layer()는 나중에 추가한 게 바깥에 감싸지는 구조임.
    // 즉 코드상 순서의 역순으로 실행됨:
    //   1) TraceLayer: 요청/응답 로깅 (가장 바깥)
    //   2) CorsLayer: CORS 헤더 처리
    //   3) GovernorLayer: IP별 요청 제한 (라우터 바로 앞)
    //   4) 라우터: 실제 핸들러 실행
    //
    // governor_limiter를 전체 라우터에 걸었으므로 모든 API에 적용됨.
    // 인증 API에만 적용하고 싶으면 route.rs에서 공개 라우트에만
    // .layer(governor_limiter)를 걸면 됨.
    // 지금은 전체 적용이 더 안전함 (DDoS 방어).
    // ─────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────
    // handoff_store 주기적 정리 (백그라운드 태스크)
    //
    // handoff token은 30초 TTL.
    // nonce보다 훨씬 짧은 수명이므로 30초마다 정리.
    //
    // 정상적인 경우 exchange_handoff_token()에서 즉시 삭제되지만,
    // exchange 안 하고 방치된 handoff가 메모리에 쌓이는 걸 방지.
    //
    // 예: 유저가 "게임 시작" 누르고 취소한 경우
    //     → handoff가 발급됐지만 교환 안 됨
    //     → 30초 후 여기서 정리
    // ─────────────────────────────────────────────────────────
    let handoff_store = state.handoff_store.clone();
    tokio::spawn(async move {
        loop {
            tokio::time::sleep(tokio::time::Duration::from_secs(30)).await;
            let now = std::time::SystemTime::now();
            let before = handoff_store.len();
            handoff_store.retain(|_, entry| entry.expires_at > now);
            let removed = before - handoff_store.len();
            if removed > 0 {
                tracing::debug!("만료된 handoff {} 건 정리", removed);
            }
        }
    });

    let app = route::create_router(state)
        .layer(governor_limiter)
        .layer(cors)
        .layer(TraceLayer::new_for_http());

    let listener = tokio::net::TcpListener::bind("127.0.0.1:1113")
        .await
        .expect("포트 바인딩 실패");

    tracing::info!("서버 실행: http://localhost:1113");

    axum::serve(
        listener,
        app.into_make_service_with_connect_info::<SocketAddr>(),
    )
    .await
    .expect("서버 실행 실패");
}
