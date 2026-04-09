// route.rs
// 모든 URL 경로(라우트)를 한곳에서 관리하는 파일
//
// 스프링부트로 치면 @RequestMapping들을 모아놓은 설정 파일임.
// 핸들러 함수와 URL을 연결하고,
// 어떤 라우트에 JWT 미들웨어를 적용할지 결정함.
//
// ── 왜 main.rs에서 분리했나? ─────────────────────────────────
// main.rs에 라우트를 직접 쓰면 나중에 API가 50개, 100개 될 때
// main.rs가 수백 줄이 되어서 관리가 안 됨.
// route.rs로 분리하면:
// - main.rs는 서버 실행만 담당 (깔끔)
// - route.rs에서 URL 구조를 한눈에 파악 가능
// - 새 API 추가할 때 여기만 수정하면 됨
//
// ── 공개 vs 보호 라우트 ──────────────────────────────────────
// 공개 라우트: JWT 없이 접근 가능
//   → 로그인, 회원가입, 헬스체크 등
//   → 아직 로그인 안 한 유저가 쓰는 API
//
// 보호 라우트: JWT 필수 (Authorization: Bearer 토큰)
//   → 가계부, 마이페이지, 마켓 등
//   → 로그인한 유저만 쓸 수 있는 API
//   → jwt_middleware가 앞단에서 토큰 검증하고,
//     통과하면 핸들러에 user_id를 넘겨줌

use axum::{Router, routing::{get, post},middleware};
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

use crate::state::AppState;
use crate::auth;
use crate::openapi::ApiDoc;

// create_router: 모든 라우트를 조립해서 하나의 Router로 반환하는 함수
// main.rs에서 이걸 호출해서 서버에 등록함
//
// 매개변수 state: AppState
//   → 핸들러들이 config, http_client, nonce_store 등에 접근하려면
//     Router에 state를 등록해야 함
//   → 스프링부트에서 @Autowired로 Bean을 주입받는 것과 비슷한 개념
pub fn create_router(state: AppState) -> Router {
    // ── 공개 라우트 ─────────────────────────────────────────
    // JWT 없이 누구나 접근 가능한 엔드포인트들
    //
    // /health
    //   → 서버가 살아있는지 확인하는 용도
    //   → 프론트나 로드밸런서가 주기적으로 찔러봄
    //   → 200 OK + "ok" 반환하면 정상
    //
    // /auth/wallet/nonce
    //   → 지갑 로그인 1단계: 앱이 지갑 주소를 보내면 nonce 발급
    //   → 아직 로그인 전이니까 JWT 없음 → 공개 라우트
    //
    // /auth/wallet/login
    //   → 지갑 로그인 2단계: 서명 검증 후 JWT 발급
    //   → JWT를 "발급받는" API니까 당연히 JWT 없이 접근해야 함

    let public_routes = Router::new()
        .route("/health", get(|| async { "ok" }))
        .route("/auth/wallet/nonce", post(auth::handler::request_nonce))
        .route("/auth/wallet/login", post(auth::handler::wallet_login))
        .route("/auth/test/login", post(auth::handler::test_email_login));


    // ── 보호 라우트 ─────────────────────────────────────────
    // JWT 필수. jwt_middleware를 통과해야 핸들러에 도달함.
    //
    // .route_layer(middleware::from_fn_with_state(...))
    //   → 이 Router에 등록된 모든 라우트 앞에 jwt_middleware를 붙임
    //   → 스프링부트의 @PreAuthorize 또는 SecurityFilterChain과 비슷
    //
    // from_fn_with_state(state.clone(), jwt_middleware)
    //   → 미들웨어 안에서 AppState를 쓸 수 있게 state를 넘겨줌
    //   → jwt_middleware가 state.jwks_cache, state.config 등에 접근해야 하니까
    //   → .clone()인 이유: state를 공개 라우트에도 넘겨야 해서 소유권 공유
    //
    // 여기에 가계부, 마이페이지, 마켓 등 인증 필요한 API를 추가하면 됨
    // 추가하는 모든 라우트는 자동으로 JWT 검증을 탐

    let protected_routes = Router::new()
        .route("/me", get(auth::handler::get_me))
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    // ── 합치기 ──────────────────────────────────────────────
    // 공개 라우트와 보호 라우트를 하나의 Router로 합침
    //
    // .merge(): 두 Router를 합치는 메서드
    //   → 같은 경로가 겹치면 먼저 등록된 게 우선
    //
    // .with_state(state): 모든 핸들러에서 State<AppState>로 꺼내 쓸 수 있게 등록
    //   → handler.rs에서 State(state): State<AppState> 하면 이게 들어감

    Router::new()
        .merge(public_routes)
        .merge(protected_routes)
        .merge(
            SwaggerUi::new("/swagger-ui")
                .url("/api-docs/openapi.json", ApiDoc::openapi())
        )
        .with_state(state)
}
