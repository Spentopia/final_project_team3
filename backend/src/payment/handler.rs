// payment/handler.rs
// 결제 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  POST /api/payments         → create_payment
//  POST /api/payments/confirm → confirm_payment

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::{ConfirmPaymentRequest, CreatePaymentRequest}, service};

// POST /api/payments
pub async fn create_payment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreatePaymentRequest>,
) -> impl IntoResponse {
    if req.amount <= 0 {
        return (StatusCode::BAD_REQUEST, "amount는 0보다 커야 합니다.".to_string()).into_response();
    }
    match service::create_payment(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// POST /api/payments/confirm
pub async fn confirm_payment(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<ConfirmPaymentRequest>,
) -> impl IntoResponse {
    if req.payment_key.trim().is_empty() {
        return (StatusCode::BAD_REQUEST, "payment_key는 필수입니다.".to_string()).into_response();
    }
    match service::confirm_payment(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
