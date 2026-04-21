// route.rs
// 모든 URL 경로(라우트)를 한곳에서 관리하는 파일
//
// ── 공개 라우트: JWT 없이 접근 가능 (로그인, nonce 등)
// ── 보호 라우트: JWT 필수. jwt_middleware를 통과해야 핸들러에 도달

use axum::{
    Router, middleware,
    routing::{delete, get, patch, post},
};
use std::sync::Arc;
use tower_governor::{GovernorLayer, governor::GovernorConfigBuilder};
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

use crate::auth;
use crate::avatar;
use crate::budget;
use crate::community;
use crate::expense;
use crate::ledger;
use crate::market;
use crate::notification;
use crate::openapi::ApiDoc;
use crate::payment;
use crate::report;
use crate::reward;
use crate::state::AppState;
use crate::user;
use crate::wallet;

pub fn create_router(state: AppState) -> Router {
    // ── 공개 라우트 ─────────────────────────────────────────
    let public_routes = Router::new()
        .route("/health", get(|| async { "ok" }))
        .route("/auth/exchange", post(auth::handler::exchange_token))
        .route("/auth/app/exchange", post(auth::handler::exchange_token_app))
        .route("/auth/wallet/nonce", post(auth::handler::request_nonce))
        .route("/auth/refresh", post(auth::handler::refresh_token))
        .route("/auth/app/refresh", post(auth::handler::refresh_token_app))
        .route("/auth/logout", post(auth::handler::logout))
        .route("/auth/app/logout", post(auth::handler::logout_app))
        .route("/auth/wallet/login", post(auth::handler::wallet_login))
        .route("/auth/app/wallet/login", post(auth::handler::wallet_login_app))
        .route("/auth/kakao/start", post(auth::handler::kakao_start))
        .route("/auth/kakao/login", post(auth::handler::kakao_login))
        // ── handoff 교환 (공개) ─────────────────────────────
        // 유니티가 postMessage로 받은 handoff token을 여기로 보냄
        // JWT 없이 접근 가능 (아직 유니티에 토큰이 없으므로)
        .route(
            "/auth/handoff/exchange",
            post(auth::handler::exchange_handoff),
        );

    // ── 열거 공격 방어 전용 rate limit ───────────────────────────
    let enumeration_rate_limit = Arc::new(
        GovernorConfigBuilder::default()
            .per_millisecond(30_000)
            .burst_size(3)
            .finish()
            .unwrap(),
    );

    let sensitive_routes = Router::new()
        .route("/auth/find-email", post(auth::handler::find_email))
        .route("/auth/check-email", post(auth::handler::check_email))
        .route(
            "/auth/check-reset-password-email",
            post(auth::handler::check_reset_password_email),
        )
        .layer(GovernorLayer {
            config: enumeration_rate_limit,
        });

    // ── 보호 라우트 ─────────────────────────────────────────
    let protected_routes = Router::new()
        // ── 인증 / 프로필 ──────────────────────────────────
        .route("/me", get(auth::handler::get_me))
        .route("/profile/complete", patch(auth::handler::complete_profile))
        .route(
            "/profile/check-availability",
            post(auth::handler::check_profile_availability),
        )
        .route(
            "/profile/image/upload",
            post(auth::handler::upload_profile_image),
        )
        .route(
            "/profile/image-url",
            get(auth::handler::get_profile_image_signed_url),
        )
        // ── handoff 발급 (보호) ─────────────────────────────
        // 웹에서 "게임 시작" 클릭 시 호출
        // JWT 필수 → 누구의 handoff인지 알아야 하니까
        .route(
            "/auth/handoff",
            post(auth::handler::create_handoff),
        )
        // ── 지갑 연동 ──────────────────────────────────────
        .route("/wallet/link", post(wallet::handler::link_wallet))
        .route("/wallet/unlink", delete(wallet::handler::unlink_wallet))
        // ── 유저 프로필 / 설정 ─────────────────────────────
        .route("/api/user/profile", get(user::handler::get_profile))
        .route("/api/user/profile", patch(user::handler::update_profile))
        .route("/api/user/settings", get(user::handler::get_settings))
        .route("/api/user/settings", patch(user::handler::update_settings))
        // ── 소비 내역 ──────────────────────────────────────
        .route(
            "/api/expenses",
            get(expense::handler::list_expenses).post(expense::handler::create_expense),
        )
        .route("/api/expenses/:expense_id", delete(expense::handler::delete_expense))
        .route(
            "/api/receipt/ocr",
            post(expense::handler::verify_receipt_ocr),
        )
        .route(
            "/api/receipt/ocr/",
            post(expense::handler::verify_receipt_ocr),
        )
        .route(
            "/api/v1/receipt/ocr",
            post(expense::handler::verify_receipt_ocr),
        )
        // ── 예산 ──────────────────────────────────────────
        .route("/api/budget", get(budget::handler::get_budget))
        .route("/api/budget", post(budget::handler::create_budget))
        .route("/api/budget/:id", patch(budget::handler::update_budget))
        .route(
            "/api/budget/:id/categories",
            patch(budget::handler::update_categories),
        )
        .route(
            "/api/budget/:id/ai-plan",
            post(budget::handler::generate_ai_plan),
        )
        // ── 가계부 ────────────────────────────────────────
        .route("/api/ledgers", get(ledger::handler::list_ledgers))
        .route("/api/ledgers", post(ledger::handler::create_ledger))
        .route("/api/ledgers/:id", patch(ledger::handler::update_ledger))
        .route("/api/ledgers/:id", delete(ledger::handler::delete_ledger))
        .route(
            "/api/ledgers/:id/members",
            post(ledger::handler::invite_member),
        )
        // ── 커뮤니티 ─────────────────────────────────────
        .route("/api/contests", get(community::handler::list_contests))
        .route("/api/posts", get(community::handler::list_posts))
        .route("/api/posts", post(community::handler::create_post))
        .route("/api/posts/:id", delete(community::handler::delete_post))
        .route("/api/posts/:id/vote", post(community::handler::vote_post))
        .route("/api/chat", post(community::handler::chat))
        .route("/api/chat/logs", get(community::handler::list_chat_logs))
        // ── 알림 ──────────────────────────────────────────
        .route(
            "/api/notifications",
            get(notification::handler::list_notifications),
        )
        .route(
            "/api/notifications/read",
            post(notification::handler::mark_read),
        )
        // ── 결제 ──────────────────────────────────────────
        .route("/api/payments", post(payment::handler::create_payment))
        .route(
            "/api/payments/confirm",
            post(payment::handler::confirm_payment),
        )
        // ── 리포트 ────────────────────────────────────────
        .route("/api/reports", post(report::handler::generate_report))
        .route("/api/reports", get(report::handler::list_reports))
        // ── 보상 / 스트릭 ─────────────────────────────────
        .route("/api/rewards", get(reward::handler::list_rewards))
        .route("/api/rewards/streak", get(reward::handler::get_streak))
        // current를 먼저 등록 (/weekly-score 패턴과 충돌 방지)
        .route(
            "/api/rewards/weekly-score/current",
            get(reward::handler::get_current_weekly_score),
        )
        .route(
            "/api/rewards/weekly-score",
            get(reward::handler::get_weekly_scores),
        )
        // 공모전 보상 지급 (관리자용, TODO: 관리자 미들웨어 추가 예정)
        .route(
            "/api/admin/contest/reward",
            post(reward::handler::grant_contest_reward),
        )
        // ── 아바타 / 아이템 ───────────────────────────────
        .route("/api/avatar/mint-nft", post(avatar::handler::mint_nft))
        .route(
            "/api/avatar/transfer-nft",
            post(avatar::handler::transfer_nft),
        )
        .route("/api/avatar/items", get(avatar::handler::get_user_items))
        // ── 마켓 ──────────────────────────────────────────
        .route(
            "/api/market/listings",
            get(market::handler::get_listings).post(market::handler::create_listing),
        )
        .route(
            "/api/market/listings/:id/escrow",
            patch(market::handler::update_escrow),
        )
        .route("/api/market/purchase", post(market::handler::purchase))
        // JWT 미들웨어: 위 모든 라우트에 적용
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    // ── 어드민 전용 라우트 ───────────────────────────────────
    // jwt_middleware → admin_middleware 순으로 실행됨
    // route_layer는 나중에 선언한 것이 바깥쪽(먼저 실행)이므로
    // jwt를 나중에 선언해야 jwt가 먼저 실행됨
    let admin_routes = Router::new();

    // ── 합치기 ──────────────────────────────────────────────
    Router::new()
        .merge(public_routes)
        .merge(sensitive_routes)
        .merge(protected_routes)
        .merge(admin_routes)
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .with_state(state)
}
