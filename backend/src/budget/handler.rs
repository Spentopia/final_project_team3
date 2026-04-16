// budget/handler.rs

use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use serde::Deserialize;
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{
        CreateBudgetRequest, GenerateAiPlanRequest, UpdateBudgetCategoriesRequest,
        UpdateBudgetRequest,
    },
    service,
};

#[derive(Deserialize)]
pub struct BudgetQuery {
    pub year: i32,
    pub month: i32,
}

#[utoipa::path(
    get, path = "/api/budget",
    tag = "예산",
    params(("year" = i32, Query, description = "연도"), ("month" = i32, Query, description = "월")),
    responses((status = 200, description = "예산 조회 성공"), (status = 404, description = "없음")),
    security(("bearer_auth" = []))
)]
pub async fn get_budget(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Query(query): Query<BudgetQuery>,
) -> impl IntoResponse {
    match service::get_budget(&state, user_id, query.year, query.month).await {
        Ok(Some(res)) => (StatusCode::OK, Json(res)).into_response(),
        Ok(None) => StatusCode::NOT_FOUND.into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/budget",
    tag = "예산",
    request_body = CreateBudgetRequest,
    responses((status = 201, description = "예산 생성 성공"), (status = 400, description = "잘못된 요청")),
    security(("bearer_auth" = []))
)]
pub async fn create_budget(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<CreateBudgetRequest>,
) -> impl IntoResponse {
    if req.month < 1 || req.month > 12 {
        return (StatusCode::BAD_REQUEST, "month는 1~12 사이여야 합니다.".to_string()).into_response();
    }
    if req.total_budget <= 0 {
        return (StatusCode::BAD_REQUEST, "total_budget은 0보다 커야 합니다.".to_string()).into_response();
    }
    match service::create_budget(&state, user_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    patch, path = "/api/budget/{id}",
    tag = "예산",
    params(("id" = Uuid, Path, description = "예산 ID")),
    request_body = UpdateBudgetRequest,
    responses((status = 200, description = "예산 수정 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_budget(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(budget_id): Path<Uuid>,
    Json(req): Json<UpdateBudgetRequest>,
) -> impl IntoResponse {
    match service::update_budget(&state, user_id, budget_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    patch, path = "/api/budget/{id}/categories",
    tag = "예산",
    params(("id" = Uuid, Path, description = "예산 ID")),
    request_body = UpdateBudgetCategoriesRequest,
    responses((status = 200, description = "카테고리 배분 수정 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_categories(
    State(state): State<AppState>,
    Extension(_user_id): Extension<Uuid>,
    Path(budget_id): Path<Uuid>,
    Json(req): Json<UpdateBudgetCategoriesRequest>,
) -> impl IntoResponse {
    match service::update_categories(&state, budget_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

#[utoipa::path(
    post, path = "/api/budget/{id}/ai-plan",
    tag = "예산",
    params(("id" = Uuid, Path, description = "예산 ID")),
    responses((status = 200, description = "AI 예산 플랜 생성 성공")),
    security(("bearer_auth" = []))
)]
pub async fn generate_ai_plan(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(budget_id): Path<Uuid>,
) -> impl IntoResponse {
    let req = GenerateAiPlanRequest { budget_id };
    match service::generate_ai_plan(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}
