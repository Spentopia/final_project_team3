// expense/handler.rs

use axum::{
    Extension, Json,
    extract::{Multipart, Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
};
use chrono::Local;
use serde::Deserialize;
use uuid::Uuid;

use super::{
    dto::{CreateExpenseWebRequest, ExpenseListWebResponse},
    service,
};
use crate::state::AppState;

#[derive(Deserialize)]
pub struct ReceiptOcrQuery {
    pub expense_id: Option<String>,
}

#[utoipa::path(
    get,
    path = "/api/expenses",
    tag = "소비",
    responses(
        (status = 200, description = "소비 목록 조회 성공", body = ExpenseListWebResponse),
        (status = 500, description = "서버 내부 오류")
    ),
    security(("bearer_auth" = []))
)]
pub async fn list_expenses(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_expenses(&state, user_id).await {
        Ok(items) => (StatusCode::OK, Json(ExpenseListWebResponse { items })).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
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

    if req.transaction_type != "expense" && req.transaction_type != "income" {
        return (
            StatusCode::BAD_REQUEST,
            "transactionType은 expense 또는 income 이어야 합니다".to_string(),
        )
            .into_response();
    }

    match service::create_expense(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    delete,
    path = "/api/expenses/{expense_id}",
    tag = "소비",
    params(
        ("expense_id" = Uuid, Path, description = "삭제할 소비 ID")
    ),
    responses(
        (status = 204, description = "소비 삭제 성공"),
        (status = 500, description = "서버 내부 오류")
    ),
    security(("bearer_auth" = []))
)]
pub async fn delete_expense(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(expense_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::delete_expense(&state, user_id, expense_id).await {
        Ok(()) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn verify_receipt_ocr(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Query(query): Query<ReceiptOcrQuery>,
    multipart: Multipart,
) -> impl IntoResponse {
    let expense_id = match query.expense_id {
        Some(raw) => match Uuid::parse_str(&raw) {
            Ok(id) => Some(id),
            Err(_) => {
                return (
                    StatusCode::BAD_REQUEST,
                    "expense_id 형식이 올바르지 않습니다".to_string(),
                )
                    .into_response();
            }
        },
        None => None,
    };

    // 하루 3건 제한은 항상 적용하고, expense_id가 있을 때만 중복 체크한다.
    match service::check_receipt_limit(&state, user_id, expense_id).await {
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
        Ok(res) => {
            // OCR 인증 성공 + expense_id 있으면 서버에서 receipt_verified = true 업데이트
            if res.verification.is_verified {
                if let Some(expense_id) = expense_id {
                    if let Err(e) =
                        service::update_receipt_verified(&state, user_id, expense_id).await
                    {
                        tracing::warn!("receipt_verified UPDATE 실패: {}", e);
                    } else {
                        let state_clone = state.clone();
                        let today = Local::now().date_naive();
                        tokio::spawn(async move {
                            if let Err(e) = crate::reward::service::recalculate_weekly_score(
                                &state_clone,
                                user_id,
                                today,
                            )
                            .await
                            {
                                tracing::warn!("영수증 인증 후 성실도 재계산 실패: {}", e);
                            }
                        });
                    }
                }
            }
            (StatusCode::OK, Json(res)).into_response()
        }
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
