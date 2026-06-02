// budget/handler.rs

use axum::{
    Extension, Json,
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
};
use serde::Deserialize;
use uuid::Uuid;

use super::{
    dto::{
        CreateBudgetRequest, GenerateAiPlanBody, GenerateAiPlanRequest, UpdateBudgetCategoriesRequest,
        UpdateBudgetRequest,
    },
    service,
};
use crate::state::AppState;

fn budget_error_response(e: anyhow::Error) -> axum::response::Response {
    match e.to_string().as_str() {
        "budget_locked" => (
            StatusCode::CONFLICT,
            "이번 달 예산 설정이 완료되어 수정할 수 없습니다.".to_string(),
        )
            .into_response(),
        "budget_not_found" => StatusCode::NOT_FOUND.into_response(),
        _ => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

fn first_day_of_month(year: i32, month: i32) -> Option<chrono::NaiveDate> {
    chrono::NaiveDate::from_ymd_opt(year, month as u32, 1)
}

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
        return (
            StatusCode::BAD_REQUEST,
            "month는 1~12 사이여야 합니다.".to_string(),
        )
            .into_response();
    }
    if req.total_budget <= 0 {
        return (
            StatusCode::BAD_REQUEST,
            "total_budget은 0보다 커야 합니다.".to_string(),
        )
            .into_response();
    }
    match service::create_budget(&state, user_id, req).await {
        Ok(res) => {
            if let Some(target_date) = first_day_of_month(res.year, res.month) {
                let state_clone = state.clone();
                tokio::spawn(async move {
                    if let Err(e) = crate::reward::service::recalculate_monthly_score(
                        &state_clone,
                        user_id,
                        target_date,
                    )
                    .await
                    {
                        tracing::warn!("예산 생성 후 월간 성실도 재계산 실패: {}", e);
                    }
                });
            }
            (StatusCode::CREATED, Json(res)).into_response()
        }
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
        Err(e) => budget_error_response(e),
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
    Extension(user_id): Extension<Uuid>,
    Path(budget_id): Path<Uuid>,
    Json(req): Json<UpdateBudgetCategoriesRequest>,
) -> impl IntoResponse {
    match service::update_categories(&state, user_id, budget_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => budget_error_response(e),
    }
}

#[utoipa::path(
    post, path = "/api/budget/ai-plan",
    tag = "예산",
    request_body = GenerateAiPlanBody,
    responses((status = 200, description = "AI 예산 플랜 미리보기 성공")),
    security(("bearer_auth" = []))
)]
pub async fn preview_ai_plan(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(req): Json<GenerateAiPlanBody>,
) -> impl IntoResponse {
    match service::preview_ai_plan(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => budget_error_response(e),
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
    body: Option<Json<GenerateAiPlanBody>>,
) -> impl IntoResponse {
    let body = body.map(|Json(body)| body).unwrap_or_default();
    let req = GenerateAiPlanRequest {
        budget_id,
        total_budget: body.total_budget,
        savings_goal: body.savings_goal,
        food: body.food,
        transport: body.transport,
        living: body.living,
        leisure: body.leisure,
        fixed_expenses: body.fixed_expenses,
    };
    match service::generate_ai_plan(&state, user_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => budget_error_response(e),
    }
}
