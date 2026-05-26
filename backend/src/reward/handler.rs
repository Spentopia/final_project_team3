// ============================================================
// reward/handler.rs — HTTP 요청 수신 및 응답 반환
//
// 핸들러의 책임:
//   - 경로 파라미터, Body, Extension(user_id) 추출
//   - service 함수 호출
//   - Ok → 200 + JSON / Err → 500 + 에러 메시지 변환
//
// 비즈니스 로직은 service.rs에 위임 — 핸들러는 얇게 유지
// ============================================================

use axum::{Extension, Json, extract::State, http::StatusCode, response::IntoResponse};
use uuid::Uuid;

use super::{dto::ContestRewardRequest, service};
use crate::state::AppState;

fn error_chain_message(error: &anyhow::Error) -> String {
    error
        .chain()
        .map(ToString::to_string)
        .collect::<Vec<_>>()
        .join(": ")
}

// ────────────────────────────────────────────────────────────
// 기존 핸들러 3개 — 변경 없음
// ────────────────────────────────────────────────────────────

pub async fn list_rewards(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_rewards(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn get_streak(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_streak(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn get_monthly_scores(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_monthly_scores(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn get_weekly_scores(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_weekly_scores(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// ────────────────────────────────────────────────────────────
// 신규 핸들러 1: get_current_monthly_score
//
// GET /api/rewards/monthly-score/current
// 오늘 기준 이번 달 성실도 점수 조회 (재계산 없음)
// ────────────────────────────────────────────────────────────
pub async fn get_current_monthly_score(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_current_monthly_score(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn get_current_weekly_score(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_current_weekly_score(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

// ────────────────────────────────────────────────────────────
// grant_contest_reward
//
// POST /api/admin/contest/reward
// 관리자가 공모전 수상자에게 SPT 수동 지급
// route.rs에서 admin_middleware + jwt_middleware가 라우트 레이어로 적용됨
//
// Json(req): request body를 ContestRewardRequest로 역직렬화
//   역직렬화 실패 시 Axum이 자동으로 422 반환
// ────────────────────────────────────────────────────────────
pub async fn grant_contest_reward(
    State(state): State<AppState>,
    Json(req): Json<ContestRewardRequest>,
) -> impl IntoResponse {
    match service::grant_contest_reward(&state, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn get_box_count(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::get_box_count(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => (StatusCode::INTERNAL_SERVER_ERROR, e.to_string()).into_response(),
    }
}

pub async fn open_box(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::open_box(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => {
            tracing::error!("open_box 실패: {}", error_chain_message(&e));
            let msg = error_chain_message(&e);
            if msg.contains("열 수 있는 상자가 없습니다")
                || msg.contains("지급 가능한 일반 아이템이 없습니다")
                || msg.contains("지급 가능한 NFT 아이템이 없습니다")
            {
                (StatusCode::BAD_REQUEST, msg).into_response()
            } else {
                (StatusCode::INTERNAL_SERVER_ERROR, msg).into_response()
            }
        }
    }
}
