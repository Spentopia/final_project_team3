// ledger/handler.rs

use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::{CreateLedgerRequest, InviteMemberRequest, UpdateLedgerRequest}, service};

#[utoipa::path(
    get, path = "/api/ledgers",
    tag = "가계부",
    responses((status = 200, description = "가계부 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_ledgers(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_ledgers(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/ledgers",
    tag = "가계부",
    request_body = CreateLedgerRequest,
    responses((status = 201, description = "가계부 생성 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn create_ledger(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreateLedgerRequest>,
) -> impl IntoResponse {
    if req.title.trim().is_empty() {
        return (StatusCode::BAD_REQUEST, "title은 필수입니다.".to_string()).into_response();
    }
    match service::create_ledger(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    patch, path = "/api/ledgers/{id}",
    tag = "가계부",
    params(("id" = Uuid, Path, description = "가계부 ID")),
    request_body = UpdateLedgerRequest,
    responses((status = 200, description = "가계부 수정 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_ledger(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(ledger_id): Path<Uuid>,
    Json(req): Json<UpdateLedgerRequest>,
) -> impl IntoResponse {
    match service::update_ledger(&state, user_id, ledger_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    delete, path = "/api/ledgers/{id}",
    tag = "가계부",
    params(("id" = Uuid, Path, description = "가계부 ID")),
    responses((status = 204, description = "가계부 삭제 성공")),
    security(("bearer_auth" = []))
)]
pub async fn delete_ledger(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(ledger_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::delete_ledger(&state, user_id, ledger_id).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/ledgers/{id}/members",
    tag = "가계부",
    params(("id" = Uuid, Path, description = "가계부 ID")),
    request_body = InviteMemberRequest,
    responses((status = 201, description = "멤버 초대 성공")),
    security(("bearer_auth" = []))
)]
pub async fn invite_member(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
    Path(ledger_id): Path<Uuid>,
    Json(req): Json<InviteMemberRequest>,
) -> impl IntoResponse {
    match service::invite_member(&state, ledger_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
