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
use crate::auth;

#[derive(OpenApi)]
#[openapi(
    paths(
        // ── 지갑 로그인 ─────────────────────────────────
        auth::handler::request_nonce,
        auth::handler::wallet_login,

        // TODO: 가계부
        // ledger::handler::list,
        // ledger::handler::create,

        // TODO: 소비내역
        // expense::handler::list,
        // expense::handler::create,
    ),
    components(
        schemas(
            // ── 지갑 로그인 DTO ──────────────────────────
            auth::dto::NonceRequest,
            auth::dto::NonceResponse,
            auth::dto::WalletLoginRequest,
            auth::dto::LoginResponse,
        )
    ),
    tags(
        (name = "지갑 로그인", description = "Solana 지갑 기반 로그인 API"),
        // (name = "가계부", description = "가계부 CRUD API"),
        // (name = "소비내역", description = "소비내역 기록/조회 API"),
    )
)]
pub struct ApiDoc;