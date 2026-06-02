// route.rs
// 모든 URL 경로(라우트)를 한곳에서 관리하는 파일
//
// ── 라우트 그룹 구조 ───────────────────────────────────
//
// 1) health_routes        : rate limit 없음 (헬스/공지)
// 2) sensitive_routes     : 빡빡한 rate limit (열거 공격 방어)
// 3) public_routes        : 표준 rate limit (인증 불필요)
// 4) protected_routes     : 표준 rate limit + JWT (인증 필요)
// 5) admin_routes         : 표준 rate limit + JWT + admin (관리자)
//
// 반환 타입:
//   (Router, Router) = (health_router, main_router)
//
// main_router 안에는 sensitive/public/protected/admin/swagger가 다 들어있음.
// main.rs에서 main_router 바깥에 governor_limiter를 한 번 더 걸어서
// 2~5번이 모두 표준 rate limit을 받게 한다.
// health_router는 governor 바깥에 따로 merge되므로 rate limit이 안 걸린다.

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
use crate::rate_limit::{CloudflareRailwayIpExtractor, UserIdExtractor};
use crate::report;
use crate::reward;
use crate::state::AppState;
use crate::system;
use crate::user;
use crate::wallet;

/// 라우터를 생성한다.
///
/// 반환값: (health_router, main_router)
/// - health_router: rate limit이 걸리면 안 되는 헬스/공지 엔드포인트
/// - main_router : 나머지 전부. main.rs에서 governor_limiter를 추가로 감쌈.
pub fn create_router(state: AppState) -> (Router, Router, Router) {
    // ─────────────────────────────────────────────────────
    // 1) health_routes : rate limit 없음
    //
    // 이 그룹에 들어가는 엔드포인트의 조건:
    //   - 인증 불필요
    //   - 부작용 없음 (DB 변경 없음)
    //   - 정보 노출 위험 없음
    //   - 정상 동작 중에도 자주/동시에 호출됨
    //
    // 왜 rate limit을 빼는가:
    //   - /health : 로드밸런서/모니터링이 수초 간격으로 호출
    //   - /api/system/status : 학원 18명이 동시에 앱 켜면 동시 호출 정상
    //     → 같은 공용 IP로 묶이는 환경에서 막히면 첫 화면이 깨짐
    //
    // 주의: 이 그룹은 .with_state()를 직접 호출해야 한다.
    // main.rs에서 main_router에만 .with_state()가 적용되는 게 아니라,
    // 각 Router가 독립적으로 자기 state를 가져야 하기 때문.
    // ─────────────────────────────────────────────────────
    let health_routes = Router::new()
        .route("/health", get(|| async { "ok" }))
        .route(
            "/api/system/status",
            get(system::handler::get_system_status),
        )
        .with_state(state.clone());

    // ─────────────────────────────────────────────────────
    // 2) sensitive_routes : 열거 공격 방어 + 공용 IP 환경 배려
    //
    // 대상:
    //   - /auth/check-email
    //   - /auth/find-email
    //   - /auth/check-reset-password-email
    //   - /profile/check-nickname
    //   - /profile/check-nicknames-batch  (batch 추가)
    //
    // 제한값 결정 근거 (35명 공용 IP 기준):
    //   - burst 200: 35명이 회원가입 흐름을 동시에 진행해도 안전
    //                  · 이메일 확인 1회 = 35
    //                  · 닉네임 확인 1~2회 = 35~70
    //                  · 주사위 batch (5개 후보 한 번에) × 일부 = 최대 35
    //                  · 합계 ~140, burst 200 안전 마진 60
    //   - 5초당 1개 회복: 정상 사용자 영향 없음
    //                      enumeration 공격 1만 시도 시 약 78분 봉인
    //
    // 공통 버킷 주의:
    //   5개 엔드포인트가 토큰을 공유함.
    //   batch API도 같은 버킷이지만 1회 호출로 5개 검증 가능 → 부담 1/5로 감소.
    //
    // 이전 → 현재 변경 이력:
    //   초기 20/30초 → 60/10초 → 120/5초 → 200/5초 (현재)
    //   batch API 적용으로 호출 빈도 감소했지만,
    //   공용 IP 환경 35명 시연 안전 마진까지 고려해 200 유지.
    // ─────────────────────────────────────────────────────
    let enumeration_rate_limit = Arc::new(
        GovernorConfigBuilder::default()
            .key_extractor(CloudflareRailwayIpExtractor)
            .per_millisecond(5_000)
            .burst_size(200)
            .finish()
            .unwrap(),
    );

    // ─────────────────────────────────────────────────────
    // user_id 기준 rate limit (protected_routes 용)
    //
    // IP가 아니라 사용자 단위로 카운트한다.
    // 같은 IP에서 18~20명이 동시에 써도 각자 자기 버킷을 가짐.
    //
    // 제한값 결정 근거:
    //   - burst 100: 한 사용자가 페이지 진입(7~8개 API) 후
    //                새로고침 10번 연타해도 통과 (5×7 = 35개)
    //                → 정상 사용에 영향 없음
    //   - 1초당 10개 회복: 빠른 페이지 전환에도 따라잡음
    //   - 봇 행동(분당 600+ 요청)은 burst 100 소진 후 차단
    //
    // admin_routes는 별도로 적용하지 않음:
    //   관리자는 신뢰된 사용자이고 대시보드 등에서 많은 호출 발생 가능.
    //   전역 IP 기준 governor로 충분.
    // ─────────────────────────────────────────────────────
    let user_rate_limit = Arc::new(
        GovernorConfigBuilder::default()
            .key_extractor(UserIdExtractor)
            .per_millisecond(100)
            .burst_size(300)
            .finish()
            .unwrap(),
    );

    // ─────────────────────────────────────────────────────
    // 3) auth_routes : 로그인/토큰 교환용 빡빡한 rate limit
    //
    // brute-force 공격 방어가 목적.
    //
    // 대상:
    //   - /auth/exchange, /auth/wallet/login, /auth/kakao/login 등
    //     비번/토큰/서명 검증으로 로그인 시도하는 엔드포인트
    //   - /auth/refresh : refresh token 탈취 시도 방어
    //   - /auth/wallet/nonce : nonce 발급 후 signature 검증 단계로 가는 시작점
    //
    // 제한값 결정 근거:
    //   - burst 160: 공용 IP 35명 동시 로그인 시연 안전 통과
    //                (카카오/월렛 로그인 흐름에서 70~100개 수준, 재시도 여유 포함)
    //   - 5초당 1개 회복: 정상 사용자는 영향 없음 (5초 안에 5번 로그인 시도 안 함)
    //                      공격자 1만개 dictionary는 14시간 소요
    //
    // 제외:
    //   - /auth/logout : 공격 가치 없음
    //   - /auth/kakao/callback : OAuth 브라우저 리다이렉트 (사용자 한 명당 1회)
    //   - /auth/handoff/exchange : 30초 TTL로 자체 방어 있음
    //   이들은 전역 governor(50/sec, burst 200)만 받음
    // ─────────────────────────────────────────────────────
    let auth_attempt_rate_limit = Arc::new(
        GovernorConfigBuilder::default()
            .key_extractor(CloudflareRailwayIpExtractor)
            .per_millisecond(5_000)
            .burst_size(160)
            .finish()
            .unwrap(),
    );

    let auth_routes = Router::new()
        .route("/auth/exchange", post(auth::handler::exchange_token))
        .route("/auth/app/exchange", post(auth::handler::exchange_token_app))
        .route("/auth/wallet/nonce", post(auth::handler::request_nonce))
        .route("/auth/wallet/login", post(auth::handler::wallet_login))
        .route("/auth/app/wallet/login", post(auth::handler::wallet_login_app))
        .route("/auth/kakao/start", post(auth::handler::kakao_start))
        .route("/auth/app/kakao/start", post(auth::handler::kakao_start_app))
        .route("/auth/kakao/login", post(auth::handler::kakao_login))
        .route("/auth/app/kakao/login", post(auth::handler::kakao_login_app))
        .route("/auth/refresh", post(auth::handler::refresh_token))
        .route("/auth/app/refresh", post(auth::handler::refresh_token_app))
        .route("/auth/unity/refresh", post(auth::handler::refresh_token_unity))
        .layer(GovernorLayer {
            config: auth_attempt_rate_limit,
        });

    let sensitive_routes = Router::new()
        .route("/auth/find-email", post(auth::handler::find_email))
        .route("/auth/check-email", post(auth::handler::check_email))
        .route(
            "/profile/check-nickname",
            post(auth::handler::check_nickname),
        )
        // batch API: 닉네임 후보 여러 개 한 번에 검증
        // (주사위 버튼이 5번 호출 → 1번 호출로 단축)
        .route(
            "/profile/check-nicknames-batch",
            post(auth::handler::check_nicknames_batch),
        )
        .route(
            "/auth/check-reset-password-email",
            post(auth::handler::check_reset_password_email),
        )
        .layer(GovernorLayer {
            config: enumeration_rate_limit,
        });

    // ─────────────────────────────────────────────────────
    // 4) public_routes : 인증 불필요, 표준 rate limit
    //
    // 로그인 시도가 아니라 부수적인 공개 엔드포인트.
    // (로그아웃, OAuth 콜백, handoff exchange)
    //
    // brute-force 위험 없음 → 전역 governor만 받음.
    // ─────────────────────────────────────────────────────
    let public_routes = Router::new()
        .route("/auth/logout", post(auth::handler::logout))
        .route("/auth/app/logout", post(auth::handler::logout_app))
        .route("/auth/unity/logout", post(auth::handler::logout_unity))
        .route("/auth/kakao/callback", get(auth::handler::kakao_callback))
        .route("/auth/webview/callback", get(auth::handler::webview_callback))
        .route("/auth/handoff/exchange", post(auth::handler::exchange_handoff));

    // ─────────────────────────────────────────────────────
    // 4) protected_routes : JWT 필수
    //
    // .route_layer()로 jwt_middleware를 박아둠.
    // .layer()와 .route_layer()의 차이:
    //   - .layer()    : 자기 자신과 모든 하위 라우트에 적용
    //                   404도 layer를 거침
    //   - .route_layer() : 매칭된 라우트만 layer를 거침
    //                      존재하지 않는 경로는 layer 없이 404
    //
    // jwt_middleware는 route_layer로 거는 게 맞음.
    // → /api/존재하지않는경로 호출 시 JWT 검증을 굳이 할 필요 없으니까.
    //
    // 이 그룹도 main_router에 들어가므로 표준 rate limit을 받음.
    // ─────────────────────────────────────────────────────
    let protected_routes = Router::new()
        // ── 인증 / 프로필 ──────────────────────────────────
        .route("/me", get(auth::handler::get_me))
        .route("/profile/complete", patch(auth::handler::complete_profile))
        .route(
            "/profile/check-availability",
            post(auth::handler::check_profile_availability),
        )
        .route("/auth/withdraw", post(auth::handler::withdraw))
        .route(
            "/profile/image/upload",
            post(auth::handler::upload_profile_image),
        )
        .route(
            "/profile/image-url",
            get(auth::handler::get_profile_image_signed_url),
        )
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
        .route(
            "/api/budget/ai-plan",
            post(budget::handler::preview_ai_plan),
        )
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
        .route(
            "/api/rewards/monthly-score/current",
            get(reward::handler::get_current_monthly_score),
        )
        .route(
            "/api/rewards/monthly-score",
            get(reward::handler::get_monthly_scores),
        )
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
            get(avatar::handler::get_user_game_items),
        )
        .route("/api/unity/avatar/equip", post(avatar::handler::equip_item))
        // ── 아바타 / 아이템 ───────────────────────────────
        .route("/api/avatar/mint-nft", post(avatar::handler::mint_nft))
        .route(
            "/api/avatar/transfer-nft",
            post(avatar::handler::transfer_nft),
        )
        .route("/api/avatar/items", get(avatar::handler::get_user_items))
        .route(
            "/api/avatar/game/items",
            get(avatar::handler::get_user_game_items),
        )
        .route("/api/avatar/nfts", get(avatar::handler::get_owned_nfts))
        .route(
            "/api/avatar/nfts/sync",
            post(avatar::handler::sync_owned_nfts),
        )
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
            "/api/market/listings/:id/abandon",
            patch(market::handler::abandon_pending_listing),
        )
        .route(
            "/api/market/listings/:id",
            delete(market::handler::cancel_listing),
        )
        .route("/api/market/purchase", post(market::handler::purchase))
        // ───────────────────────────────────────────────
        // user_id 기준 rate limit
        //
        // jwt_middleware보다 안쪽에 박혀있다.
        // → 실행 순서: 요청 → jwt_middleware → 이 governor → 핸들러
        //
        // jwt_middleware가 user_id를 extension에 박은 다음,
        // UserIdExtractor가 extension에서 그걸 꺼내 key로 사용.
        //
        // 같은 학원 IP에서 20명이 써도 각자 분리된 limit을 받음.
        // ───────────────────────────────────────────────
        .layer(GovernorLayer {
            config: user_rate_limit.clone(),
        })
        // JWT 미들웨어: 위 모든 라우트에 적용 (가장 바깥 = 먼저 실행)
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    // ─────────────────────────────────────────────────────
    // 5) admin_routes : JWT + admin 권한 필수
    //
    // route_layer를 두 개 거는 순서에 주의:
    //   .route_layer(admin_middleware)
    //   .route_layer(jwt_middleware)
    //
    // axum에서 layer는 나중에 추가한 게 바깥에 감싸짐.
    // 즉 실행 순서는:
    //   요청 → jwt_middleware → admin_middleware → 핸들러
    //
    // 이게 맞는 순서:
    //   1) jwt_middleware가 토큰 검증하고 user_id를 request에 박음
    //   2) admin_middleware가 그 user_id로 role 조회해서 admin인지 확인
    //
    // 만약 순서 반대면 admin_middleware가 user_id를 못 찾아서 터짐.
    // ─────────────────────────────────────────────────────
    let admin_routes = Router::new()
        .route(
            "/api/admin/contest/reward",
            post(reward::handler::grant_contest_reward),
        )
        .route(
            "/api/admin/system/status",
            patch(system::handler::update_system_status),
        )
        .route(
            "/api/admin/dashboard/stats",
            get(admin::handler::get_dashboard_stats),
        )
        .route(
            "/api/admin/dashboard/trends",
            get(admin::handler::get_dashboard_trends),
        )
        .route(
            "/api/admin/content-reports",
            get(admin::handler::list_content_reports),
        )
        .route(
            "/api/admin/content-reports/:id/target",
            get(admin::handler::get_content_report_target_detail),
        )
        .route(
            "/api/admin/content-reports/:id/resolve",
            patch(admin::handler::resolve_content_report),
        )
        .route(
            "/api/admin/content-reports/:id/reject",
            patch(admin::handler::reject_content_report),
        )
        .route(
            "/api/admin/content-reports/:id/audit-logs",
            get(admin::handler::list_content_report_audit_logs),
        )
        .route(
            "/api/admin/content-reports/:id/apply-action",
            patch(admin::handler::apply_content_report_action),
        )
        .route(
            "/api/admin/users/withdrawn",
            get(admin::handler::list_withdrawn_users),
        )
        .route("/api/admin/users", get(admin::handler::list_users))
        .route(
            "/api/admin/users/:id/active",
            patch(admin::handler::update_user_active),
        )
        .route(
            "/api/admin/notices",
            get(admin::handler::list_notices).post(admin::handler::create_notice),
        )
        .route(
            "/api/admin/notices/:id",
            patch(admin::handler::update_notice).delete(admin::handler::delete_notice),
        )
        .route(
            "/api/admin/contests",
            get(admin::handler::list_contests).post(admin::handler::create_contest),
        )
        .route(
            "/api/admin/contests/:id",
            patch(admin::handler::update_contest),
        )
        .route(
            "/api/admin/contests/:id/status",
            patch(admin::handler::update_contest_status),
        )
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::admin_middleware,
        ))
        .route_layer(middleware::from_fn_with_state(
            state.clone(),
            auth::middleware::jwt_middleware,
        ));

    // ─────────────────────────────────────────────────────
    // main_router 조립
    //
    // sensitive/public/protected/admin + Swagger를 한 라우터로 합침.
    // main.rs에서 이 main_router 바깥에 governor_limiter를 걸어서
    // 전체에 표준 rate limit이 적용되게 한다.
    //
    // sensitive_routes는 자체 layer가 박혀있으므로
    // governor_limiter와 이중으로 적용되지만,
    // 더 빡빡한 쪽(자체 layer)이 먼저 막으므로 실질적으로 자체값이 적용됨.
    // ─────────────────────────────────────────────────────
    // IP 기준 전역 governor를 받을 라우터.
    // 로그인 전/공개/관리자/민감 API만 여기에 둔다.
    let ip_limited_router = Router::new()
        .merge(sensitive_routes)
        .merge(auth_routes)
        .merge(public_routes)
        .merge(admin_routes)
        .merge(SwaggerUi::new("/swagger-ui").url("/api-docs/openapi.json", ApiDoc::openapi()))
        .with_state(state.clone());

    // protected_routes는 user_id 기준 governor만 받도록 별도 반환.
    // 학원/회사/카페 같은 공용 IP 환경에서 여러 사용자가 같은 공인 IP로 묶여도,
    // 로그인 후 API는 user_id별로 분리되게 하기 위함.
    let protected_router = protected_routes.with_state(state);

    (health_routes, ip_limited_router, protected_router)
}
