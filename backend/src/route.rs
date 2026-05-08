// route.rs
// 모든 URL 경로(라우트)를 한곳에서 관리하는 파일
//
// ── 공개 라우트: JWT 없이 접근 가능 (로그인, nonce 등)
// ── 보호 라우트: JWT 필수. jwt_middleware를 통과해야 핸들러에 도달

use axum::{
    Router,
    extract::DefaultBodyLimit,
    middleware,
    routing::{delete, get, patch, post},
};
use std::sync::Arc;
use tower_governor::{GovernorLayer, governor::GovernorConfigBuilder};
use utoipa::OpenApi;
use utoipa_swagger_ui::SwaggerUi;

use crate::admin;
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
        .route(
            "/auth/app/exchange",
            post(auth::handler::exchange_token_app),
        )
        .route("/auth/wallet/nonce", post(auth::handler::request_nonce))
        .route("/auth/refresh", post(auth::handler::refresh_token))
        .route("/auth/app/refresh", post(auth::handler::refresh_token_app))
        .route(
            "/auth/unity/refresh",
            post(auth::handler::refresh_token_unity),
        )
        .route("/auth/logout", post(auth::handler::logout))
        .route("/auth/app/logout", post(auth::handler::logout_app))
        .route("/auth/unity/logout", post(auth::handler::logout_unity))
        .route("/auth/wallet/login", post(auth::handler::wallet_login))
        .route(
            "/auth/app/wallet/login",
            post(auth::handler::wallet_login_app),
        )
        .route("/auth/kakao/start", post(auth::handler::kakao_start))
        .route(
            "/auth/app/kakao/start",
            post(auth::handler::kakao_start_app),
        )
        .route("/auth/kakao/login", post(auth::handler::kakao_login))
        .route(
            "/auth/app/kakao/login",
            post(auth::handler::kakao_login_app),
        )
        .route("/auth/kakao/callback", get(auth::handler::kakao_callback))
        .route(
            "/auth/webview/callback",
            get(auth::handler::webview_callback),
        )
        // ── handoff 교환 (공개) ─────────────────────────────
        // 유니티 exe가 실행 시 전달받은 handoff token을 여기로 보냄
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
            "/profile/check-nickname",
            post(auth::handler::check_nickname),
        )
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
        //회원 탈퇴
        .route("/auth/withdraw", post(auth::handler::withdraw))
        .route(
            "/profile/image/upload",
            post(auth::handler::upload_profile_image),
        )
        .route(
            "/profile/image-url",
            get(auth::handler::get_profile_image_signed_url),
        )
        // ── handoff 발급 (보호) ─────────────────────────────
        // 웹에서 "게임 실행" 클릭 시 호출
        // JWT 필수 → 누구의 handoff인지 알아야 하니까
        .route("/auth/handoff", post(auth::handler::create_handoff))
        // ── 지갑 연동 ──────────────────────────────────────
        .route("/wallet/link", post(wallet::handler::link_wallet))
        .route("/wallet/unlink", delete(wallet::handler::unlink_wallet))
        // ── 유저 프로필 / 설정 ─────────────────────────────
        .route("/api/user/profile", get(user::handler::get_profile))
        .route("/api/user/profile", patch(user::handler::update_profile))
        .route("/api/user/settings", get(user::handler::get_settings))
        .route("/api/user/settings", patch(user::handler::update_settings))
        .route("/api/user/password", patch(user::handler::change_password))
        // 웹뷰용
        .route("/auth/webview/issue", post(auth::handler::webview_issue))
        // ── 소비 내역 ──────────────────────────────────────
        .route(
            "/api/expenses",
            get(expense::handler::list_expenses).post(expense::handler::create_expense),
        )
        .route(
            "/api/expenses/:expense_id",
            delete(expense::handler::delete_expense),
        )
        .route(
            "/api/receipt/ocr",
            post(expense::handler::verify_receipt_ocr)
                .layer(DefaultBodyLimit::max(10 * 1024 * 1024)),
        )
        .route(
            "/api/receipt/ocr/",
            post(expense::handler::verify_receipt_ocr)
                .layer(DefaultBodyLimit::max(10 * 1024 * 1024)),
        )
        .route(
            "/api/v1/receipt/ocr",
            post(expense::handler::verify_receipt_ocr)
                .layer(DefaultBodyLimit::max(10 * 1024 * 1024)),
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
        .route("/api/chat", post(community::handler::chat))
        .route("/api/posts", get(community::handler::list_posts))
        .route("/api/posts", post(community::handler::create_post))
        .route(
            "/api/posts/image/upload",
            post(community::handler::upload_post_image)
                .layer(DefaultBodyLimit::max(10 * 1024 * 1024)),
        )
        .route(
            "/api/posts/:id",
            get(community::handler::get_post)
                .patch(community::handler::update_post)
                .delete(community::handler::delete_post),
        )
        .route(
            "/api/posts/:id/react",
            post(community::handler::react_post).delete(community::handler::unreact_post),
        )
        .route(
            "/api/posts/:id/comments",
            get(community::handler::list_comments).post(community::handler::create_comment),
        )
        .route(
            "/api/comments/:id",
            patch(community::handler::update_comment).delete(community::handler::delete_comment),
        )
        // 신고 접수
        .route(
            "/api/content-reports",
            post(community::handler::create_content_report),
        )
        // ── 알림 ──────────────────────────────────────────
        .route(
            "/api/notifications",
            get(notification::handler::list_notifications),
        )
        .route(
            "/api/notifications/:id/read",
            patch(notification::handler::read_notification),
        )
        .route(
            "/api/notifications/read",
            patch(notification::handler::mark_read),
        )
        .route(
            "/api/notifications/read-all",
            patch(notification::handler::read_all_notifications),
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
        // ── Unity 보상 / 아바타 ────────────────────────────
        .route(
            "/api/unity/rewards/box-count",
            get(reward::handler::get_box_count),
        )
        .route(
            "/api/unity/rewards/open-box",
            post(reward::handler::open_box),
        )
        .route(
            "/api/unity/avatar/inventory",
            get(avatar::handler::get_user_items),
        )
        .route("/api/unity/avatar/equip", post(avatar::handler::equip_item))
        // ── 아바타 / 아이템 ───────────────────────────────
        .route("/api/avatar/mint-nft", post(avatar::handler::mint_nft))
        .route(
            "/api/avatar/transfer-nft",
            post(avatar::handler::transfer_nft),
        )
        .route("/api/avatar/items", get(avatar::handler::get_user_items))
        .route("/api/avatar/nfts", get(avatar::handler::get_owned_nfts))
        .route(
            "/api/avatar/nfts/sync",
            post(avatar::handler::sync_owned_nfts),
        )
        // 장착 관련
        .route(
            "/api/avatar/equipment",
            get(avatar::handler::get_equipment).post(avatar::handler::equip_item),
        )
        .route(
            "/api/avatar/equipment/:slot_name",
            delete(avatar::handler::unequip_item),
        )
        // ── 마켓 ──────────────────────────────────────────
        .route(
            "/api/market/listings",
            get(market::handler::get_listings).post(market::handler::create_listing),
        )
        .route(
            "/api/market/listings/:id/escrow",
            patch(market::handler::update_escrow),
        )
        .route(
            "/api/market/listings/:id",
            delete(market::handler::cancel_listing),
        )
        .route("/api/market/purchase", post(market::handler::purchase))
        // JWT 미들웨어: 위 모든 라우트에 적용
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    let admin_routes = Router::new()
        .route(
            "/api/admin/contest/reward",
            post(reward::handler::grant_contest_reward),
        )
        // ── 관리자 신고 관리 ─────────────────────────────
        .route("/api/admin/content-reports",
            get(admin::handler::list_content_reports))
        .route("/api/admin/content-reports/:id/resolve",
            patch(admin::handler::resolve_content_report))
        .route("/api/admin/content-reports/:id/reject",
            patch(admin::handler::reject_content_report))
        .route(
            "/api/admin/users",
            get(admin::handler::list_users),
        )
        .route(
            "/api/admin/users/:id/active",
            patch(admin::handler::update_user_active),
        )
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::admin_middleware,
        ))
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    // ── 합치기 ──────────────────────────────────────────────
    Router::new()
        .merge(public_routes)
        .merge(sensitive_routes)
        .merge(protected_routes)
        .merge(admin_routes)
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .with_state(state)
}
