// budget/handler.rs
// 예산, 카테고리 배분, AI 플랜 관련 HTTP 핸들러
//
// 보호 라우트 (JWT 필수):
//  GET    /api/budget              → get_budget (year, month 쿼리 파라미터)
//  POST   /api/budget              → create_budget
//  PATCH  /api/budget/:id          → update_budget
//  PATCH  /api/budget/:id/categories → update_categories
//  POST   /api/budget/:id/ai-plan  → generate_ai_plan

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

// GET /api/budget?year=&month=
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

// POST /api/budget
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

// PATCH /api/budget/:id
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

// PATCH /api/budget/:id/categories
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

// POST /api/budget/:id/ai-plan
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
