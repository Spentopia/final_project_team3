// ledger/handler.rs
// 가계부, 공유 멤버 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET    /api/ledgers             → list_ledgers
//  POST   /api/ledgers             → create_ledger
//  PATCH  /api/ledgers/:id         → update_ledger
//  DELETE /api/ledgers/:id         → delete_ledger
//  POST   /api/ledgers/:id/members → invite_member

use axum::{
    extract::{Path, State},
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::{CreateLedgerRequest, InviteMemberRequest, UpdateLedgerRequest}, service};

// GET /api/ledgers
pub async fn list_ledgers(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_ledgers(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /api/ledgers
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

// PATCH /api/ledgers/:id
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

// DELETE /api/ledgers/:id
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

// POST /api/ledgers/:id/members
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
