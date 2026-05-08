// openapi.rs
// Swagger(OpenAPI) 문서 정의

use utoipa::OpenApi;
use utoipa::openapi::security::{HttpAuthScheme, HttpBuilder, SecurityScheme};

use crate::admin;
use crate::auth;
use crate::avatar;
use crate::budget;
use crate::community;
use crate::expense;
use crate::ledger;
use crate::market;
use crate::notification;
use crate::payment;
use crate::report;
use crate::reward;
use crate::user;
use crate::wallet;

#[derive(OpenApi)]
#[openapi(
    paths(
        // 관리자
        crate::admin::handler::list_content_reports,
        crate::admin::handler::resolve_content_report,
        crate::admin::handler::reject_content_report,
        crate::admin::handler::list_users,
        crate::admin::handler::update_user_active,

        // ── 인증 ──────────────────────────────────────────────
        crate::auth::handler::request_nonce,
        crate::auth::handler::wallet_login,
        crate::auth::handler::exchange_token,
        crate::auth::handler::refresh_token,
        crate::auth::handler::logout,
        crate::auth::handler::find_email,
        crate::auth::handler::check_email,
        crate::auth::handler::kakao_login,
        crate::auth::handler::get_me,
        crate::auth::handler::complete_profile,
        crate::auth::handler::webview_issue,
        crate::auth::handler::webview_callback,

        // ── 지갑 ──────────────────────────────────────────────
        crate::wallet::handler::link_wallet,
        crate::wallet::handler::unlink_wallet,

        // ── 유저 ──────────────────────────────────────────────
        crate::user::handler::get_profile,
        crate::user::handler::update_profile,
        crate::user::handler::get_settings,
        crate::user::handler::update_settings,

        // ── 소비 ──────────────────────────────────────────────
        crate::expense::handler::list_expenses,
        crate::expense::handler::create_expense,
        crate::expense::handler::delete_expense,

        // ── 예산 ──────────────────────────────────────────────
        crate::budget::handler::get_budget,
        crate::budget::handler::create_budget,
        crate::budget::handler::update_budget,
        crate::budget::handler::update_categories,
        crate::budget::handler::generate_ai_plan,

        // ── 가계부 ────────────────────────────────────────────
        crate::ledger::handler::list_ledgers,
        crate::ledger::handler::create_ledger,
        crate::ledger::handler::update_ledger,
        crate::ledger::handler::delete_ledger,
        crate::ledger::handler::invite_member,

        // ── 커뮤니티 / 챗봇 ───────────────────────────────────
        crate::community::handler::list_contests,
        crate::community::handler::chat,
        crate::community::handler::list_posts,
        crate::community::handler::get_post,
        crate::community::handler::create_post,
        crate::community::handler::upload_post_image,
        crate::community::handler::update_post,
        crate::community::handler::delete_post,
        crate::community::handler::react_post,
        crate::community::handler::list_comments,
        crate::community::handler::create_comment,
        crate::community::handler::update_comment,
        crate::community::handler::delete_comment,

        // ── 알림 ──────────────────────────────────────────────
        crate::notification::handler::list_notifications,
        crate::notification::handler::read_notification,
        crate::notification::handler::mark_read,
        crate::notification::handler::read_all_notifications,

        // ── 결제 ──────────────────────────────────────────────
        crate::payment::handler::create_payment,
        crate::payment::handler::confirm_payment,

        // ── 리포트 ────────────────────────────────────────────
        crate::report::handler::generate_report,
        crate::report::handler::list_reports,

        // ── 아바타 ────────────────────────────────────────────
        crate::avatar::handler::mint_nft,
        crate::avatar::handler::transfer_nft,
        crate::avatar::handler::get_user_items,

        // ── 마켓 ──────────────────────────────────────────────
        crate::market::handler::create_listing,
        crate::market::handler::update_escrow,
        crate::market::handler::purchase,
    ),
    components(
        schemas(
            // admin DTO
            admin::dto::AdminContentReportResponse,
            admin::dto::AdminContentReportResponse,
            admin::dto::AdminUserResponse,
            admin::dto::UpdateUserActiveRequest,

            // ── auth DTO ──────────────────────────────────────
            auth::dto::NonceRequest,
            auth::dto::NonceResponse,
            auth::dto::WalletLoginRequest,
            auth::dto::ExchangeTokenRequest,
            auth::dto::RefreshRequest,
            auth::dto::WebLoginResponse,
            auth::dto::AppLoginResponse,
            auth::dto::WebRefreshResponse,
            auth::dto::AppRefreshResponse,
            auth::dto::FindEmailRequest,
            auth::dto::FindEmailResponse,
            auth::dto::CheckEmailRequest,
            auth::dto::KakaoLoginRequest,
            auth::dto::CompleteProfileRequest,
            auth::dto::CompleteProfileResponse,
            auth::dto::ProfileImageUrlResponse,
            auth::dto::ProfileImageUrlQuery,
            auth::dto::WebviewIssueRequest,
            auth::dto::WebviewIssueResponse,
            auth::dto::WebviewCallbackQuery,

            // ── wallet DTO ────────────────────────────────────
            wallet::dto::LinkWalletRequest,
            wallet::dto::LinkWalletResponse,
            wallet::dto::UnlinkWalletResponse,

            // ── user DTO ──────────────────────────────────────
            user::dto::UserResponse,
            user::dto::UpdateProfileRequest,
            user::dto::UserSettingsResponse,
            user::dto::UpdateSettingsRequest,

            // ── expense DTO ───────────────────────────────────
            expense::dto::CreateExpenseWebRequest,
            expense::dto::ExpenseWebResponse,
            expense::dto::ExpenseListWebResponse,

            // ── budget DTO ────────────────────────────────────
            budget::dto::CreateBudgetRequest,
            budget::dto::UpdateBudgetRequest,
            budget::dto::BudgetCategoryItem,
            budget::dto::UpdateBudgetCategoriesRequest,
            budget::dto::BudgetResponse,
            budget::dto::GenerateAiPlanRequest,
            budget::dto::AiPlanResponse,

            // ── ledger DTO ────────────────────────────────────
            ledger::dto::CreateLedgerRequest,
            ledger::dto::LedgerResponse,
            ledger::dto::UpdateLedgerRequest,
            ledger::dto::InviteMemberRequest,
            ledger::dto::LedgerMemberResponse,

            // ── community DTO ─────────────────────────────────
            community::dto::ContestEventResponse,
            community::dto::ChatRequest,
            community::dto::ChatResponse,
            community::dto::PostType,
            community::dto::PostSort,
            community::dto::CreatePostRequest,
            community::dto::UpdatePostRequest,
            community::dto::PostResponse,
            community::dto::UploadCommunityImageResponse,
            community::dto::CommentResponse,
            community::dto::CreateCommentRequest,
            community::dto::UpdateCommentRequest,


            // ── notification DTO ──────────────────────────────
            notification::dto::NotificationResponse,
            notification::dto::MarkReadRequest,

            // ── payment DTO ───────────────────────────────────
            payment::dto::CreatePaymentRequest,
            payment::dto::ConfirmPaymentRequest,
            payment::dto::PaymentResponse,

            // ── report DTO ────────────────────────────────────
            report::dto::GenerateReportRequest,
            report::dto::ReportResponse,

            // ── reward DTO ────────────────────────────────────
            reward::dto::RewardResponse,
            reward::dto::StreakResponse,
            reward::dto::WeeklyScoreResponse,

            // ── avatar DTO ────────────────────────────────────
            avatar::dto::MintNftRequest,
            avatar::dto::MintNftResponse,
            avatar::dto::TransferNftRequest,
            avatar::dto::TransferNftResponse,
            avatar::dto::AvatarResponse,
            avatar::dto::UserItemResponse,
            avatar::dto::GachaTicketResponse,
            avatar::dto::UseGachaTicketRequest,
            avatar::dto::GachaResultResponse,
            avatar::dto::CreateScreenshotRequest,
            avatar::dto::ScreenshotResponse,

            // ── market DTO ────────────────────────────────────
            market::dto::CreateListingRequest,
            market::dto::ListingResponse,
            market::dto::PurchaseRequest,
            market::dto::TransactionResponse,
        )
    ),
    tags(
        (name = "인증", description = "로그인 / 토큰 관련 API"),
        (name = "지갑 연동", description = "Solana 지갑 연동/해제 API"),
        (name = "유저", description = "프로필 / 알림 설정 API"),
        (name = "소비", description = "소비 내역 API"),
        (name = "예산", description = "월별 예산 / AI 플랜 API"),
        (name = "가계부", description = "가계부 / 공유 멤버 API"),
        (name = "커뮤니티", description = "콘테스트 / 게시물 / 투표 API"),
        (name = "챗봇", description = "AI 챗봇 상담 API"),
        (name = "알림", description = "알림 조회 / 읽음 처리 API"),
        (name = "결제", description = "공유 가계부 결제 API"),
        (name = "리포트", description = "소비 리포트 / AI 분석 API"),
        (name = "보상", description = "SPT 보상 / 스트릭 / 주간 성실도 API"),
        (name = "아바타", description = "아바타 / 아이템 NFT API"),
        (name = "마켓", description = "NFT 마켓 거래 API"),
    ),
    modifiers(&SecurityAddon)
)]
pub struct ApiDoc;

struct SecurityAddon;

impl utoipa::Modify for SecurityAddon {
    fn modify(&self, openapi: &mut utoipa::openapi::OpenApi) {
        let components = openapi.components.get_or_insert_with(Default::default);
        components.add_security_scheme(
            "bearer_auth",
            SecurityScheme::Http(
                HttpBuilder::new()
                    .scheme(HttpAuthScheme::Bearer)
                    .bearer_format("JWT")
                    .build(),
            ),
        );
    }
}
