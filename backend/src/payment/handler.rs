// payment/handler.rs
// 결제 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  POST /api/payments         → create_payment
//  POST /api/payments/confirm → confirm_payment

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::{
    dto::{ConfirmPaymentRequest, CreatePaymentRequest},
    service,
};
use crate::state::AppState;

#[utoipa::path(
    post, path = "/api/payments",
    tag = "결제",
    request_body = CreatePaymentRequest,
    responses((status = 201, description = "결제 생성 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn create_payment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreatePaymentRequest>,
) -> impl IntoResponse {
    if req.amount <= 0 {
        return (
            StatusCode::BAD_REQUEST,
            "amount는 0보다 커야 합니다.".to_string(),
        )
            .into_response();
    }
    match service::create_payment(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/payments/confirm",
    tag = "결제",
    request_body = ConfirmPaymentRequest,
    responses((status = 200, description = "결제 승인 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn confirm_payment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<ConfirmPaymentRequest>,
) -> impl IntoResponse {
    if req.payment_key.trim().is_empty() {
        return (
            StatusCode::BAD_REQUEST,
            "payment_key는 필수입니다.".to_string(),
        )
            .into_response();
    }
    match service::confirm_payment(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
