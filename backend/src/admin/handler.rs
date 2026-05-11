// admin/handler.rs
//
// 관리자 전용 HTTP 핸들러.
//
// 역할:
// - 요청 파라미터 추출
// - service 호출
// - HTTP 응답 변환
//
// 권한 체크:
// - 이 파일 내부에서 role_type을 직접 확인하지 않는다.
// - route.rs의 admin_routes에 admin_middleware를 적용해서
//   관리자만 이 핸들러에 도달하게 만든다.

use axum::{
    extract::{Path, Query, State},
    http::StatusCode,
    response::IntoResponse,
    Extension, Json,
};
use serde::Deserialize;
use uuid:: Uuid;

use crate::state::AppState;

use super::{
    dto::{
        CreateAdminNoticeRequest, UpdateAdminNoticeRequest,
        UpdateUserActiveRequest, CreateAdminContestRequest, UpdateAdminContestRequest,
        UpdateAdminContestStatusRequest,
    },
    service,
};

/// 관리자 신고 목록 조회 쿼리
///
/// 예:
/// GET /api/admin/content-reports
/// GET /api/admin/content-reports?status=pending
#[derive(Deserialize, Debug)]
pub struct ContentReportQuery {
    pub status: Option<String>,
}


#[derive(Debug, Deserialize)]
pub struct AdminUserQuery {
    pub keyword: Option<String>,
}

fn map_admin_error(error: anyhow::Error) -> axum::response::Response {
    let message = error.to_string();

    // 존재하지 않는 리소스
    //
    // 예:
    // - 신고를 찾을 수 없습니다.
    // - 회원을 찾을 수 없습니다.
    // - 공지사항을 찾을 수 없습니다.
    // - 콘테스트를 찾을 수 없습니다.
    if message.contains("찾을 수 없습니다") {
        return (StatusCode::NOT_FOUND, message).into_response();
    }

    // 잘못된 요청
    //
    // 예:
    // - 제목을 입력해 주세요.
    // - 콘테스트 종료일은 시작일 이후여야 합니다.
    // - 지원하지 않는 콘테스트 상태입니다.
    // - 수정할 콘테스트 내용이 없습니다.
    if message.contains("입력해 주세요")
        || message.contains("수정할 공지사항 내용이 없습니다")
        || message.contains("콘테스트 종료일")
        || message.contains("지원하지 않는 콘테스트 상태")
        || message.contains("수정할 콘테스트")
    {
        return (StatusCode::BAD_REQUEST, message).into_response();
    }

    (StatusCode::INTERNAL_SERVER_ERROR, message).into_response()
}


#[utoipa::path(
    get,
    path = "/api/admin/content-reports",
    tag = "관리자",
    params(
        ("status" = Option<String>, Query, description = "신고 상태 필터: pending/resolved/rejected")
    ),
    responses(
        (status = 200, description = "관리자 신고 목록 조회 성공", body = [crate::admin::dto::AdminContentReportResponse]),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn list_content_reports(
    State(state): State<AppState>,
    Query(query): Query<ContentReportQuery>,
) -> impl IntoResponse {
    match service::list_content_reports(&state, query.status).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/content-reports/{id}/resolve",
    tag = "관리자",
    params(
        ("id" = Uuid, Path, description = "신고 ID")
    ),
    responses(
        (status = 200, description = "신고 처리 완료", body = crate::admin::dto::AdminContentReportResponse),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "신고 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn resolve_content_report(
    State(state): State<AppState>,
    Extension(admin_id): Extension<Uuid>,
    Path(report_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::resolve_content_report(&state, admin_id, report_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/content-reports/{id}/reject",
    tag = "관리자",
    params(
        ("id" = Uuid, Path, description = "신고 ID")
    ),
    responses(
        (status = 200, description = "신고 반려", body = crate::admin::dto::AdminContentReportResponse),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "신고 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn reject_content_report(
    State(state): State<AppState>,
    Extension(admin_id): Extension<Uuid>,
    Path(report_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::reject_content_report(&state, admin_id, report_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    get,
    path = "/api/admin/users",
    tag = "관리자",
    params(("keyword" = Option<String>, Query, description = "닉네임/이메일 검색어")),
    responses((status = 200, description = "회원 목록 조회 성공")),
    security(("bearer_auth" = []))
)]
pub async fn list_users(
    State(state): State<AppState>,
    Query(query): Query<AdminUserQuery>,
) -> impl IntoResponse {
    match service::list_users(&state, query.keyword).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/users/{id}/active",
    tag = "관리자",
    request_body = UpdateUserActiveRequest,
    params(("id" = Uuid, Path, description = "회원 ID")),
    responses((status = 200, description = "회원 활성 상태 변경 성공")),
    security(("bearer_auth" = []))
)]
pub async fn update_user_active(
    State(state): State<AppState>,
    Path(user_id): Path<Uuid>,
    Json(req): Json<UpdateUserActiveRequest>,
) -> impl IntoResponse {
    match service::update_user_active(&state, user_id, req.is_active).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

// ─────────────────────────────────────────────
// 공지사항 관리 핸들러
// ─────────────────────────────────────────────

#[utoipa::path(
    get,
    path = "/api/admin/notices",
    tag = "관리자",
    responses(
        (status = 200, description = "관리자 공지사항 목록 조회 성공", body = [crate::admin::dto::AdminNoticeResponse]),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn list_notices(
    State(state): State<AppState>,
) -> impl IntoResponse {
    match service::list_notices(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    post,
    path = "/api/admin/notices",
    tag = "관리자",
    request_body = CreateAdminNoticeRequest,
    responses(
        (status = 201, description = "관리자 공지사항 생성 성공", body = crate::admin::dto::AdminNoticeResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn create_notice(
    State(state): State<AppState>,
    Extension(admin_id): Extension<Uuid>,
    Json(req): Json<CreateAdminNoticeRequest>,
) -> impl IntoResponse {
    match service::create_notice(&state, admin_id, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/notices/{id}",
    tag = "관리자",
    request_body = UpdateAdminNoticeRequest,
    params(("id" = Uuid, Path, description = "공지사항 ID")),
    responses(
        (status = 200, description = "관리자 공지사항 수정 성공", body = crate::admin::dto::AdminNoticeResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "공지사항 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn update_notice(
    State(state): State<AppState>,
    Path(notice_id): Path<Uuid>,
    Json(req): Json<UpdateAdminNoticeRequest>,
) -> impl IntoResponse {
    match service::update_notice(&state, notice_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    delete,
    path = "/api/admin/notices/{id}",
    tag = "관리자",
    params(("id" = Uuid, Path, description = "공지사항 ID")),
    responses(
        (status = 200, description = "관리자 공지사항 삭제 성공", body = crate::admin::dto::AdminNoticeResponse),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "공지사항 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn delete_notice(
    State(state): State<AppState>,
    Path(notice_id): Path<Uuid>,
) -> impl IntoResponse {
    match service::delete_notice(&state, notice_id).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    get,
    path = "/api/admin/contests",
    tag = "관리자",
    responses(
        (status = 200, description = "관리자 콘테스트 목록 조회 성공", body = [crate::admin::dto::AdminContestResponse]),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn list_contests(
    State(state): State<AppState>,
) -> impl IntoResponse {
    match service::list_contests(&state).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    post,
    path = "/api/admin/contests",
    tag = "관리자",
    request_body = CreateAdminContestRequest,
    responses(
        (status = 201, description = "관리자 콘테스트 생성 성공", body = crate::admin::dto::AdminContestResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn create_contest(
    State(state): State<AppState>,
    Json(req): Json<CreateAdminContestRequest>,
) -> impl IntoResponse {
    match service::create_contest(&state, req).await {
        Ok(res) => (StatusCode::CREATED, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/contests/{id}",
    tag = "관리자",
    request_body = UpdateAdminContestRequest,
    params(("id" = Uuid, Path, description = "콘테스트 ID")),
    responses(
        (status = 200, description = "관리자 콘테스트 수정 성공", body = crate::admin::dto::AdminContestResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "콘테스트 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn update_contest(
    State(state): State<AppState>,
    Path(contest_id): Path<Uuid>,
    Json(req): Json<UpdateAdminContestRequest>,
) -> impl IntoResponse {
    match service::update_contest(&state, contest_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}

#[utoipa::path(
    patch,
    path = "/api/admin/contests/{id}/status",
    tag = "관리자",
    request_body = UpdateAdminContestStatusRequest,
    params(("id" = Uuid, Path, description = "콘테스트 ID")),
    responses(
        (status = 200, description = "관리자 콘테스트 상태 변경 성공", body = crate::admin::dto::AdminContestResponse),
        (status = 400, description = "잘못된 요청"),
        (status = 401, description = "인증 실패"),
        (status = 403, description = "관리자 권한 없음"),
        (status = 404, description = "콘테스트 없음")
    ),
    security(("bearer_auth" = []))
)]
pub async fn update_contest_status(
    State(state): State<AppState>,
    Path(contest_id): Path<Uuid>,
    Json(req): Json<UpdateAdminContestStatusRequest>,
) -> impl IntoResponse {
    match service::update_contest_status(&state, contest_id, req).await {
        Ok(res) => (StatusCode::OK, Json(res)).into_response(),
        Err(e) => map_admin_error(e),
    }
}