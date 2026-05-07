// notification/handler.rs

use axum::{
    Extension, Json,
    extract::{Path, State},
    http::StatusCode,
    response::IntoResponse,
};
use uuid::Uuid;

use crate::state::AppState;

use super::{dto::MarkReadRequest, service};

fn map_notification_error(error: anyhow::Error) -> axum::response::Response {
    let message = error.to_string();

    if message.contains("찾을 수 없습니다") {
        return (StatusCode::NOT_FOUND, message).into_response();
    }

    if message.contains("알림이 없습니다") {
        return (StatusCode::BAD_REQUEST, message).into_response();
    }

    (StatusCode::INTERNAL_SERVER_ERROR, message).into_response()
}

#[utoipa::path(
    get,
    path = "/api/notifications",
    tag = "알림",
    security(("bearer_auth" = [])),
    responses(
        (status = 200, description = "내 알림 목록 조회 성공", body = [crate::notification::dto::NotificationResponse]),
        (status = 401, description = "인증 실패"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn list_notifications(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::list_my_notifications(&state, user_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_notification_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/notifications/{id}/read",
    tag = "알림",
    security(("bearer_auth" = [])),
    params(
        ("id" = Uuid, Path, description = "읽음 처리할 알림 ID")
    ),
    responses(
        (status = 204, description = "알림 1개 읽음 처리 성공"),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 404, description = "알림 없음"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn read_notification(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Path(notification_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::mark_notifications_read(&state, user_id, vec![notification_id]).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => map_notification_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/notifications/read",
    tag = "알림",
    security(("bearer_auth" = [])),
    request_body = MarkReadRequest,
    responses(
        (status = 204, description = "선택한 알림 읽음 처리 성공"),
        (status = 400, description = "읽음 처리할 알림이 없음"),
        (status = 401, description = "인증 실패"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn mark_read(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
    Json(body): Json<MarkReadRequest>,
) -> impl IntoResponse {
    match service::mark_notifications_read(&state, user_id, body.notification_ids).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => map_notification_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/notifications/read-all",
    tag = "알림",
    security(("bearer_auth" = [])),
    responses(
        (status = 204, description = "내 모든 알림 읽음 처리 성공"),
        (status = 401, description = "인증 실패"),
        (status = 500, description = "서버 내부 오류")
    )
)]
pub async fn read_all_notifications(
    State(state): State<AppState>,
    Extension(user_id): Extension<Uuid>,
) -> impl IntoResponse {
    match service::mark_all_notifications_read(&state, user_id).await {
        Ok(_) => StatusCode::NO_CONTENT.into_response(),
        Err(e) => map_notification_error(e),
    }
}
