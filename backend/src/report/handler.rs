// report/handler.rs

use axum::{
    Extension, Json,
    extract::State,
    http::{HeaderMap, StatusCode},
    response::IntoResponse,
};
use uuid::Uuid;

use super::{
    analysis_x402::{self, AnalysisAccess},
    dto::GenerateReportRequest,
    service,
};
use crate::state::AppState;

#[utoipa::path(
    post,
    path = "/api/reports",
    tag = "리포트",
    request_body = GenerateReportRequest,
    responses(
        (status = 200, description = "AI 리포트 생성 성공")
    ),
    security(("bearer_auth" = []))
)]
pub async fn generate_report(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    headers: HeaderMap,
    Json(req): Json<GenerateReportRequest>,
) -> impl IntoResponse {
    let payment_header = headers
        .get("X-PAYMENT")
        .or_else(|| headers.get("x-payment"))
        .and_then(|v| v.to_str().ok());
    let resource = "/api/reports";

    let access =
        match analysis_x402::authorize_analysis(&state, user_id, &req, payment_header, resource)
            .await
        {
            Ok(access) => access,
            Err(e) => return (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
        };

    let tx_signature = match &access {
        AnalysisAccess::Paid { tx_signature } => Some(tx_signature.clone()),
        _ => None,
    };
    let paid = match access {
        AnalysisAccess::Free => false,
        AnalysisAccess::Paid { .. } => true,
        AnalysisAccess::PaymentRequired(body) => {
            return (StatusCode::PAYMENT_REQUIRED, Json(body)).into_response();
        }
    };

    match service::generate_report(&state, user_id, req.clone()).await {
        Ok(res) => {
            if let Err(e) = analysis_x402::record_usage(&state, user_id, &req, paid).await {
                return (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response();
            }
            if let Some(signature) = tx_signature {
                if let Err(e) = analysis_x402::mark_payment_consumed(&state, &signature).await {
                    return (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response();
                }
            }
            (StatusCode::OK, Json(res)).into_response()
        }
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    get, path = "/api/reports",
    tag = "리포트",
    responses((status = 200, description = "리포트 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_reports(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_reports(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
