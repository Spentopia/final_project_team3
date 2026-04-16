// report/handler.rs

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::GenerateReportRequest, service};

#[utoipa::path(
    post, path = "/api/reports",
    tag = "리포트",
    request_body = GenerateReportRequest,
    responses((status = 201, description = "리포트 생성 성공"), (status = 400, description = "잘못된 날짜")),
    security(("bearer_auth" = []))
)]
pub async fn generate_report(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<GenerateReportRequest>,
) -> impl IntoResponse {
    if req.start_date > req.end_date {
        return (StatusCode::BAD_REQUEST, "start_date가 end_date보다 늦을 수 없습니다.".to_string()).into_response();
    }
    match service::generate_report(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
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
