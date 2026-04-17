// expense/handler.rs

use axum::{
    Extension, Json,
    extract::{Multipart, State},
    http::StatusCode,
    response::IntoResponse,
};
use uuid::Uuid;

use super::{dto::CreateExpenseWebRequest, service};
use crate::state::AppState;

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
    Extension(_user_id): Extension<Uuid>,
    multipart: Multipart,
) -> impl IntoResponse {
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
