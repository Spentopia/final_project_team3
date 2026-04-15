// openapi.rs
// Swagger(OpenAPI) 문서 정의 파일
//
// 스프링부트의 @OpenAPIDefinition과 동일.
// 모든 API 핸들러와 DTO를 여기서 한곳에 등록함.
//
// 새 API 추가할 때:
// 1) handler에 #[utoipa::path(...)] 어노테이션 붙이고
// 2) 여기 paths()에 핸들러 함수 추가
// 3) 새 DTO가 있으면 schemas()에도 추가
// 이러면 Swagger UI에 자동으로 나타남.

use utoipa::OpenApi;
use utoipa::openapi::security::{HttpAuthScheme, HttpBuilder, SecurityScheme};
use crate::auth;
use crate::expense;
// wallet 모듈 import: dto, handler를 schemas/paths에 등록하기 위해 필요
use crate::wallet;

#[derive(OpenApi)]
#[openapi(
    paths(
        // ─────────────────────────────────────────────
        // 인증 / 로그인
        // ─────────────────────────────────────────────
        crate::auth::handler::request_nonce,
        crate::auth::handler::wallet_login,
        crate::auth::handler::exchange_token,
        crate::auth::handler::refresh_token,
        crate::auth::handler::logout,
        crate::auth::handler::find_email,
        crate::auth::handler::check_email,
        crate::auth::handler::kakao_login,

        // ─────────────────────────────────────────────
        // 보호 API
        // ─────────────────────────────────────────────
        crate::auth::handler::get_me,
        crate::auth::handler::complete_profile,
        crate::expense::handler::create_expense,

        // ── 지갑 연동/해제 ──────────────────────────────
        // 로그인 후 지갑을 연결/해제하는 API → 보호 라우트
        crate::wallet::handler::link_wallet,
        crate::wallet::handler::unlink_wallet,
    ),
    components(
        schemas(
            // ─────────────────────────────────────────
            // auth DTO
            // ─────────────────────────────────────────
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
            expense::dto::CreateExpenseWebRequest,
            expense::dto::ExpenseWebResponse,
    



            // ── 지갑 연동/해제 DTO ───────────────────────
            // ToSchema가 붙어있어야 여기 등록 가능
            // Swagger UI의 Schemas 섹션에 자동으로 표시됨
            wallet::dto::LinkWalletRequest,
            wallet::dto::LinkWalletResponse,
            wallet::dto::UnlinkWalletResponse,
        )
    ),
    tags(
        (name = "지갑 로그인", description = "Solana 지갑 기반 로그인 API"),
        (name = "지갑 연동", description = "Solana 지갑 연동/해제 API"),
        (name = "인증 테스트", description = "JWT 인증 테스트용 API"),
        (name = "테스트", description = "개발/테스트 전용 API"),
    ),
    modifiers(&SecurityAddon)
)]
pub struct ApiDoc;

// ── Swagger UI Authorize 버튼 설정 ──────────────────────────
// 이걸 등록하면 Swagger UI 상단에 Authorize 버튼이 생김.
// 거기에 토큰 넣으면 이후 모든 요청에 자동으로
// Authorization: Bearer 토큰 헤더가 붙음.
// handler.rs의 #[utoipa::path]에서 security(("bearer_auth" = []))로
// 참조하는 키 이름("bearer_auth")이 여기 등록한 이름과 일치해야 함.
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
                    .build()
            ),
        );
    }
}
