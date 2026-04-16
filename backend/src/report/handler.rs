// report/handler.rs
// 소비 리포트 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  POST /api/reports → generate_report
//  GET  /api/reports → list_reports

use axum::{
    extract::State,
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use uuid::Uuid;

use crate::state::AppState;
use super::{dto::GenerateReportRequest, service};

// POST /api/reports
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

// GET /api/reports
pub async fn list_reports(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_reports(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
