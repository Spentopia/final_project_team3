// expense/handler.rs

use axum::{
    Extension, Json,
    extract::{Multipart, Query, State},
    http::StatusCode,
    response::IntoResponse,
};
use serde::Deserialize;
use uuid::Uuid;

use super::{dto::CreateExpenseWebRequest, service};
use crate::state::AppState;

#[derive(Deserialize)]
pub struct ReceiptOcrQuery {
    pub expense_id: Uuid,
}

#[utoipa::path(
    post,
    path = "/api/expenses",
    tag = "소비",
    request_body = CreateExpenseWebRequest,
    responses(
        (status = 201, description = "소비 저장 성공"),
        (status = 400, description = "요청 값 오류"),
        (status = 500, description = "서버 내부 오류")
    ),
    security(("bearer_auth" = []))
)]
pub async fn create_expense(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreateExpenseWebRequest>,
) -> impl IntoResponse {
    if req.amount <= 0 {
        return (
            StatusCode::BAD_REQUEST,
            "금액은 0보다 커야 합니다".to_string(),
        )
            .into_response();
    }

    if req.category.trim().is_empty() {
        return (StatusCode::BAD_REQUEST, "카테고리는 필수입니다".to_string()).into_response();
    }

    match service::create_expense(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn verify_receipt_ocr(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Query(query): Query<ReceiptOcrQuery>,
    multipart: Multipart,
) -> impl IntoResponse {
    // 하루 3건 제한 + expense_id 중복 체크
    match service::check_receipt_limit(&state, user_id, query.expense_id).await {
        Ok(()) => {}
        Err(service::ReceiptLimitError::TooMany) => {
            return (
                StatusCode::TOO_MANY_REQUESTS,
                "오늘 영수증 인증은 최대 3건까지 가능합니다".to_string(),
            )
                .into_response();
        }
        Err(service::ReceiptLimitError::Duplicate) => {
            return (
                StatusCode::CONFLICT,
                "이미 인증된 소비 내역입니다".to_string(),
            )
                .into_response();
        }
        Err(service::ReceiptLimitError::Internal(e)) => {
            return (StatusCode::INTERNAL_SERVER_ERROR, e).into_response();
        }
    }

    match service::verify_receipt_ocr(&state, multipart).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => {
            let message = e.to_string();
            if message.contains("필수") || message.contains("이미지") || message.contains("금액")
            {
                return (StatusCode::BAD_REQUEST, message).into_response();
            }
            if message.contains("AI 서버") {
                return (StatusCode::SERVICE_UNAVAILABLE, message).into_response();
            }
            (StatusCode::INTERNAL_SERVER_ERROR, message).into_response()
        }
    }
}
