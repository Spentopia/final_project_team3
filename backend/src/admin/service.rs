// admin/service.rs
//
// 관리자 전용 비즈니스 로직.
//
// 현재 기능:
// 1. 신고 목록 조회
// 2. 신고 처리 완료
// 3. 신고 반려
// 4. 회원 목록 조회
// 5. 회원 활성/비활성 변경
// 6. 공지사항 목록 조회
// 7. 공지사항 생성
// 8. 공지사항 수정
// 9. 공지사항 삭제
//
// 주의:
// - 이 파일은 service_role key로 Supabase REST API를 호출한다.
// - 실제 관리자 권한 검사는 route.rs의 admin_routes에서
//   admin_middleware가 먼저 수행한다.
// - 따라서 이 service 함수들은 "이미 관리자 검증을 통과했다"는 전제로 동작한다.
//
// 이번 수정 핵심:
// - 신고 목록을 public.content_reports에서 직접 조회하지 않는다.
// - public.admin_content_reports_view를 조회한다.
// - view에는 reporter_nickname, reporter_email이 포함되어 있다.
// - 그래서 관리자 화면에서 신고자를 UUID가 아니라 닉네임/이메일로 표시할 수 있다.
//
// 실제 content_reports 컬럼:
// - detail 사용
// - description 사용 안 함
// - reviewed_at / reviewed_by 사용

// 공지사항 설계:
// - 별도 notices 테이블을 만들지 않는다.
// - posts 테이블을 재사용한다.
// - post_type = 'notice'인 row를 공지사항으로 본다.
// - 삭제는 물리 삭제가 아니라 is_deleted = true, deleted_at = now()로 처리한다.

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc, Duration, Months, NaiveDate};
use serde_json::{json, Value};
use uuid::Uuid;

use crate::state::AppState;

use super::{
    dto::{
        AdminAuditLogResponse,
        AdminContentReportListResponse,
        AdminContentReportResponse,
        AdminContestResponse,
        AdminDashboardStatsResponse,
        AdminDashboardTrendPoint,
        AdminDashboardTrendsResponse,
        AdminNoticeResponse,
        AdminReportAction,
        AdminReportTargetDetailResponse,
        AdminUserListResponse,
        AdminUserResponse,
        AdminWithdrawnUserResponse,
        AdminWithdrawnUserListResponse,
        ApplyContentReportActionRequest,
        CreateAdminContestRequest,
        CreateAdminNoticeRequest,
        ResolveReportActionType,
        UpdateAdminContestRequest,
        UpdateAdminContestStatusRequest,
        UpdateAdminNoticeRequest,
    },
    handler::{AdminUserQuery, ContentReportQuery},
    model::{
        AdminAuditLog,
        AdminCommentTargetRow,
        AdminContentReport,
        AdminContest,
        AdminNotice,
        AdminPostTargetRow,
        AdminUser,
        AdminUserTargetRow,
        ContentReportBaseRow,
    },
};

/// DB/view 모델을 관리자 신고 응답 DTO로 변환한다.
///
/// DB row를 그대로 내려도 되지만,
/// 응답 DTO를 따로 두면 프론트 전용 필드나 기본값 처리를 한곳에서 관리할 수 있다.
fn to_report_response(row: AdminContentReport) -> AdminContentReportResponse {
    AdminContentReportResponse {
        id: row.id,
        reporter_id: row.reporter_id,
        reporter_nickname: row.reporter_nickname,
        reporter_email: row.reporter_email,
        target_type: row.target_type,
        target_id: row.target_id,
        reason: row.reason,
        detail: row.detail,
        status: row.status,
        created_at: row.created_at,
        reviewed_at: row.reviewed_at,
        reviewed_by: row.reviewed_by,

        // view에서 계산된 누적 신고 횟수.
        //
        // 혹시 view 반영 전이거나 null로 내려오는 경우에도
        // 최소 1회 신고된 row이므로 1로 fallback한다.
        target_report_count: row.target_report_count.unwrap_or(1),
    }
}

/// DB row를 관리자 감사 로그 응답 DTO로 변환한다.
///
/// 감사 로그는 관리자 작업 이력 표시용이다.
/// 현재는 신고 처리 완료/반려 이력을 상세 모달에서 보여준다.
fn to_audit_log_response(row: AdminAuditLog) -> AdminAuditLogResponse {
    AdminAuditLogResponse {
        id: row.id,
        admin_id: row.admin_id,
        action: row.action,
        target_type: row.target_type,
        target_id: row.target_id,
        before_status: row.before_status,
        after_status: row.after_status,
        metadata: row.metadata,
        created_at: row.created_at,
    }
}

/// 게시글 신고 대상 row를 프론트 응답 DTO로 변환한다.
///
/// author_profile_image_url:
/// - 게시글 작성자 프로필 이미지를 관리자 모달에서 보여주기 위한 signed URL.
async fn to_post_target_detail(
    state: &AppState,
    row: AdminPostTargetRow,
) -> AdminReportTargetDetailResponse {
    let author_profile_image_url = maybe_profile_image_signed_url(
        state,
        row.author_profile_image.as_deref(),
    )
        .await;

    AdminReportTargetDetailResponse::Post {
        id: row.id,

        author_id: row.author_id,
        author_nickname: row.author_nickname,
        author_email: row.author_email,
        author_profile_image: row.author_profile_image,
        author_profile_image_url,

        title: row.title,
        content: row.content,
        image_url: row.image_url,

        is_deleted: row.is_deleted.unwrap_or(false),
        deleted_at: row.deleted_at,
        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// 댓글 신고 대상 row를 프론트 응답 DTO로 변환한다.
async fn to_comment_target_detail(
    state: &AppState,
    row: AdminCommentTargetRow,
) -> AdminReportTargetDetailResponse {
    let author_profile_image_url = maybe_profile_image_signed_url(
        state,
        row.author_profile_image.as_deref(),
    )
        .await;

    AdminReportTargetDetailResponse::Comment {
        id: row.id,
        post_id: row.post_id,
        parent_id: row.parent_id,

        author_id: row.author_id,
        author_nickname: row.author_nickname,
        author_email: row.author_email,
        author_profile_image: row.author_profile_image,
        author_profile_image_url,

        content: row.content,

        is_deleted: row.is_deleted.unwrap_or(false),
        deleted_at: row.deleted_at,
        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// 사용자 신고 대상 row를 프로필 사진 신고 응답 DTO로 변환한다.
///
/// profile_image_url:
/// - users.profile_image path를 signed URL로 변환한 값.
/// - 프론트는 이 값을 img src로 사용한다.
async fn to_user_profile_target_detail(
    state: &AppState,
    row: AdminUserTargetRow,
) -> AdminReportTargetDetailResponse {
    let profile_image_url =
        maybe_profile_image_signed_url(state, row.profile_image.as_deref()).await;

    AdminReportTargetDetailResponse::UserProfile {
        user_id: row.id,
        nickname: row.nickname,
        email: row.email,
        profile_image: row.profile_image,
        profile_image_url,
        is_active: row.is_active.unwrap_or(true),
        deleted_at: row.deleted_at,
        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// 사용자 신고 대상 row를 닉네임 신고 응답 DTO로 변환한다.
///
/// 닉네임 신고는 현재 닉네임/이메일/계정 상태만 보여주면 충분하다.
fn to_user_nickname_target_detail(
    row: AdminUserTargetRow,
) -> AdminReportTargetDetailResponse {
    AdminReportTargetDetailResponse::UserNickname {
        user_id: row.id,
        nickname: row.nickname,
        email: row.email,
        is_active: row.is_active.unwrap_or(true),
        deleted_at: row.deleted_at,
        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// DB users row를 관리자 회원 응답 DTO로 변환한다.
///
/// users 테이블의 일부 컬럼은 nullable일 수 있으므로,
/// 프론트에서 쓰기 쉬운 기본값을 지정한다.
fn to_user_response(row: AdminUser) -> AdminUserResponse {
    AdminUserResponse {
        id: row.id,
        email: row.email,
        nickname: row.nickname,
        phone: row.phone,
        profile_image: row.profile_image,
        login_provider: row.login_provider,
        wallet_address: row.wallet_address,
        role_type: row.role_type.unwrap_or_else(|| "user".to_string()),
        profile_completed: row.profile_completed.unwrap_or(false),
        is_active: row.is_active.unwrap_or(true),
        deleted_at: row.deleted_at,

        // 비활성 정보.
        //
        // is_active=false일 때 관리자 화면과 로그인 차단 메시지에서 사용한다.
        inactive_reason: row.inactive_reason,
        inactive_at: row.inactive_at,
        inactive_until: row.inactive_until,
        inactive_by: row.inactive_by,

        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// 탈퇴 회원 row를 탈퇴 회원 모니터링 응답 DTO로 변환한다.
///
/// 기준:
/// - deleted_at이 반드시 있어야 탈퇴 회원이다.
///
/// 정책:
/// - 30일 재가입 제한:
///   deleted_at + 30 days
///
/// - 5년 보관 만료:
///   deleted_at + 60 months
///
/// Months::new(60)을 사용하는 이유:
/// - 단순 365 * 5일보다 달력 기준 5년에 더 가깝다.
/// - 실패할 경우 안전하게 365*5일 fallback을 사용한다.
fn to_withdrawn_user_response(row: AdminUser) -> Result<AdminWithdrawnUserResponse> {
    let deleted_at = row
        .deleted_at
        .ok_or_else(|| anyhow!("탈퇴 회원이 아닙니다."))?;

    let now = Utc::now();

    let rejoin_cooldown_until = deleted_at + Duration::days(30);

    let retention_expires_at = deleted_at
        .checked_add_months(Months::new(60))
        .unwrap_or_else(|| deleted_at + Duration::days(365 * 5));

    let is_rejoin_cooldown_active = now < rejoin_cooldown_until;

    let retention_days_left = if retention_expires_at > now {
        (retention_expires_at - now).num_days()
    } else {
        0
    };

    Ok(AdminWithdrawnUserResponse {
        id: row.id,
        email: row.email,
        nickname: row.nickname,
        deleted_at,
        created_at: row.created_at,
        rejoin_cooldown_until,
        is_rejoin_cooldown_active,
        retention_expires_at,
        retention_days_left,
    })
}


/// posts row를 관리자 공지사항 응답 DTO로 변환한다.
///
/// title/content가 Option인 이유:
/// - DB 구조상 nullable일 가능성을 안전하게 처리하기 위함.
/// - 관리자 화면에는 빈 문자열 fallback으로 내려준다.
fn to_notice_response(row: AdminNotice) -> AdminNoticeResponse {
    AdminNoticeResponse {
        id: row.id,
        user_id: row.user_id,
        title: row.title.unwrap_or_default(),
        content: row.content.unwrap_or_default(),
        post_type: row.post_type,
        view_count: row.view_count.unwrap_or(0),
        is_deleted: row.is_deleted.unwrap_or(false),
        deleted_at: row.deleted_at,
        created_at: row.created_at,
        updated_at: row.updated_at,
    }
}

/// contest_events row를 관리자 콘테스트 응답 DTO로 변환한다.
fn to_contest_response(row: AdminContest) -> AdminContestResponse {
    AdminContestResponse {
        id: row.id,
        title: row.title,
        description: row.description,
        start_date: row.start_date,
        end_date: row.end_date,
        status: row.status.unwrap_or_else(|| "upcoming".to_string()),
        reward_description: row.reward_description,
        created_at: row.created_at,
    }
}

/// 콘테스트 상태값 검증
///
/// 허용 상태:
/// - upcoming
/// - active
/// - ended
fn validate_contest_status(status: &str) -> Result<()> {
    match status {
        "upcoming" | "active" | "ended" => Ok(()),
        _ => Err(anyhow!("지원하지 않는 콘테스트 상태입니다.")),
    }
}

/// 콘테스트 입력값 검증
///
/// 제목은 필수.
/// 종료일은 시작일보다 이후여야 한다.
fn validate_contest_input(
    title: &str,
    start_date: chrono::DateTime<Utc>,
    end_date: chrono::DateTime<Utc>,
) -> Result<()> {
    if title.trim().is_empty() {
        return Err(anyhow!("콘테스트 제목을 입력해 주세요."));
    }

    if end_date <= start_date {
        return Err(anyhow!("콘테스트 종료일은 시작일 이후여야 합니다."));
    }

    Ok(())
}

/// 관리자 신고 view에서 신고 1건 조회
///
/// 사용 위치:
/// - 신고 처리 완료 후 최종 응답 반환
/// - 신고 반려 후 최종 응답 반환
///
/// 왜 필요한가?
/// PATCH /content_reports 응답은 content_reports 테이블 row만 반환한다.
/// 즉 reporter_nickname, reporter_email이 없다.
/// 그래서 PATCH 성공 후 admin_content_reports_view에서 다시 조회해서
/// 프론트에 닉네임/이메일이 포함된 응답을 반환한다.
async fn get_content_report_from_view_by_id(
    state: &AppState,
    report_id: Uuid,
) -> Result<AdminContentReportResponse> {
    let url = format!(
        "{}/rest/v1/admin_content_reports_view?select=id,reporter_id,reporter_nickname,reporter_email,target_type,target_id,reason,detail,status,created_at,reviewed_at,reviewed_by,target_report_count&id=eq.{}&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 신고 view 단건 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 view 단건 조회 실패: {}", body));
    }

    let rows: Vec<AdminContentReport> = res
        .json()
        .await
        .context("관리자 신고 view 단건 조회 응답 역직렬화 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고를 찾을 수 없습니다."))?;

    Ok(to_report_response(row))
}

/// 날짜 필터 시작값 보정.
///
/// 프론트 date input은 보통 "2026-05-01" 형태로 보낸다.
/// 그런데 created_at은 timestamptz라서 그냥 날짜만 비교하면 의도와 다를 수 있다.
///
/// 그래서 날짜만 들어온 경우:
/// - 시작일: 2026-05-01T00:00:00+09:00
///
/// 이미 ISO 문자열이 들어온 경우는 그대로 사용한다.
fn normalize_start_date_filter(value: &str) -> String {
    let trimmed = value.trim();

    if trimmed.len() == 10 {
        format!("{}T00:00:00+09:00", trimmed)
    } else {
        trimmed.to_string()
    }
}

/// 날짜 필터 종료값 보정.
///
/// 날짜만 들어온 경우:
/// - 종료일: 2026-05-13T23:59:59+09:00
///
/// 이렇게 해야 2026-05-13 하루 전체가 포함된다.
fn normalize_end_date_filter(value: &str) -> String {
    let trimmed = value.trim();

    if trimmed.len() == 10 {
        format!("{}T23:59:59+09:00", trimmed)
    } else {
        trimmed.to_string()
    }
}

// ─────────────────────────────────────────────
// 관리자 대시보드 통계
// ─────────────────────────────────────────────
//
// 관리자 첫 화면에서 보여줄 운영 요약 지표를 계산한다.
//
// 현재 1차 범위:
// - 회원 수 통계
// - 신고 상태별 통계
// - 평균 신고 처리 시간
//
// 정확한 DAU/MAU는 별도 activity log가 있어야 하므로 여기서는 제외한다.

/// PostgREST Content-Range 헤더에서 전체 count를 추출한다.
///
/// Supabase REST에서 count를 얻으려면:
/// - Prefer: count=exact
/// - Range: 0-0
///
/// 응답 헤더 예:
/// - content-range: 0-0/153
/// - content-range: */0
///
/// 이 함수는 slash 뒤의 값을 i64로 파싱한다.
/// 파싱 실패 시 0으로 fallback한다.
fn parse_content_range_count(value: Option<&reqwest::header::HeaderValue>) -> i64 {
    value
        .and_then(|header_value| header_value.to_str().ok())
        .and_then(|content_range| content_range.rsplit('/').next())
        .and_then(|count| count.parse::<i64>().ok())
        .unwrap_or(0)
}

/// 특정 테이블의 row 수를 조회한다.
///
/// table:
/// - "users"
/// - "content_reports"
///
/// filter_query:
/// - 빈 문자열이면 필터 없음.
/// - 예: "status=eq.pending"
/// - 예: "deleted_at=is.null&is_active=eq.true"
///
/// 구현 방식:
/// - 실제 데이터는 1개만 받아오도록 Range: 0-0 사용
/// - 전체 개수는 Content-Range 헤더에서 읽음
///
/// 주의:
/// - table 이름은 외부 입력을 그대로 받는 용도가 아니다.
/// - 이 함수는 service 내부에서 고정된 테이블명만 넣어 호출한다.
async fn count_rows(
    state: &AppState,
    table: &str,
    filter_query: &str,
) -> Result<i64> {
    let base_url = state.config.supabase_url.trim_end_matches('/');

    let url = if filter_query.trim().is_empty() {
        format!("{}/rest/v1/{}?select=id", base_url, table)
    } else {
        format!(
            "{}/rest/v1/{}?select=id&{}",
            base_url,
            table,
            filter_query
        )
    };

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Range-Unit", "items")
        .header("Range", "0-0")
        .header("Prefer", "count=exact")
        .send()
        .await
        .with_context(|| format!("관리자 대시보드 count 조회 요청 실패: {}", table))?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!(
            "관리자 대시보드 count 조회 실패: table={}, body={}",
            table,
            body
        ));
    }

    Ok(parse_content_range_count(res.headers().get("content-range")))
}

/// 평균 신고 처리 시간을 분 단위로 계산한다.
///
/// 기준:
/// - content_reports.created_at
/// - content_reports.reviewed_at
///
/// reviewed_at이 있는 신고만 대상으로 한다.
/// 아직 처리되지 않은 pending 신고는 평균 처리 시간 계산에서 제외한다.
///
/// 구현 방식:
/// - 처리된 신고 row의 created_at/reviewed_at만 조회
/// - Rust에서 duration을 계산해 평균을 낸다.
///
/// 현재 프로젝트/시연 규모에서는 이 방식이 충분하다.
/// 데이터가 수십만 건 이상 쌓이는 서비스라면 DB 함수나 materialized view로 옮기는 게 좋다.
async fn get_average_report_handle_minutes(
    state: &AppState,
) -> Result<Option<i64>> {
    #[derive(Debug, serde::Deserialize)]
    struct ReportTimeRow {
        created_at: Option<DateTime<Utc>>,
        reviewed_at: Option<DateTime<Utc>>,
    }

    let url = format!(
        "{}/rest/v1/content_reports?select=created_at,reviewed_at&created_at=not.is.null&reviewed_at=not.is.null&limit=1000",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("평균 신고 처리 시간 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!("평균 신고 처리 시간 조회 실패: {}", body));
    }

    let rows: Vec<ReportTimeRow> = res
        .json()
        .await
        .context("평균 신고 처리 시간 응답 파싱 실패")?;

    let mut total_minutes: i64 = 0;
    let mut count: i64 = 0;

    for row in rows {
        let Some(created_at) = row.created_at else {
            continue;
        };

        let Some(reviewed_at) = row.reviewed_at else {
            continue;
        };

        // 혹시 데이터가 꼬여 reviewed_at이 created_at보다 빠른 경우는 제외한다.
        if reviewed_at < created_at {
            continue;
        }

        let duration = reviewed_at - created_at;
        total_minutes += duration.num_minutes();
        count += 1;
    }

    if count == 0 {
        Ok(None)
    } else {
        Ok(Some(total_minutes / count))
    }
}

/// 관리자: 대시보드 통계 조회.
///
/// API:
/// GET /api/admin/dashboard/stats
///
/// 반환 지표:
/// - 전체 회원 수
/// - 활성 회원 수
/// - 비활성 회원 수
/// - 탈퇴 회원 수
/// - 대기중 신고 수
/// - 처리완료 신고 수
/// - 반려 신고 수
/// - 평균 신고 처리 시간
pub async fn get_dashboard_stats(
    state: &AppState,
) -> Result<AdminDashboardStatsResponse> {
    // ─────────────────────────────────────────────
    // 회원 통계
    // ─────────────────────────────────────────────
    //
    // users 기준:
    // - total_users: 전체 row 수
    // - active_users: 탈퇴하지 않았고 활성 상태
    // - inactive_users: 탈퇴하지 않았고 비활성 상태
    // - withdrawn_users: deleted_at이 있는 탈퇴 회원
    let total_users = count_rows(state, "users", "").await?;

    let active_users = count_rows(
        state,
        "users",
        "deleted_at=is.null&is_active=eq.true",
    )
        .await?;

    let inactive_users = count_rows(
        state,
        "users",
        "deleted_at=is.null&is_active=eq.false",
    )
        .await?;

    let withdrawn_users = count_rows(
        state,
        "users",
        "deleted_at=not.is.null",
    )
        .await?;

    // ─────────────────────────────────────────────
    // 신고 통계
    // ─────────────────────────────────────────────
    //
    // content_reports.status 기준:
    // - pending: 대기중
    // - resolved: 처리완료
    // - rejected: 반려
    let pending_reports = count_rows(
        state,
        "content_reports",
        "status=eq.pending",
    )
        .await?;

    let resolved_reports = count_rows(
        state,
        "content_reports",
        "status=eq.resolved",
    )
        .await?;

    let rejected_reports = count_rows(
        state,
        "content_reports",
        "status=eq.rejected",
    )
        .await?;

    // ─────────────────────────────────────────────
    // 평균 신고 처리 시간
    // ─────────────────────────────────────────────
    //
    // reviewed_at이 있는 신고만 대상으로 평균을 계산한다.
    let average_report_handle_minutes =
        get_average_report_handle_minutes(state).await?;

    Ok(AdminDashboardStatsResponse {
        total_users,
        active_users,
        inactive_users,
        withdrawn_users,
        pending_reports,
        resolved_reports,
        rejected_reports,
        average_report_handle_minutes,
    })
}

// ─────────────────────────────────────────────
// 관리자 대시보드 추이 그래프
// ─────────────────────────────────────────────
//
// 대시보드 하단 그래프에서 사용할 최근 7일 데이터를 만든다.
//
// 현재 그래프:
// - 최근 7일 가입자 추이: users.created_at 기준
// - 최근 7일 신고 접수 추이: content_reports.created_at 기준
//
// 주의:
// - DAU/MAU는 활동 로그가 있어야 정확히 계산 가능하므로 여기서는 제외한다.
// - 날짜별 0건인 날도 그래프에 보여주기 위해 Rust에서 0으로 채워준다.

/// 최근 N일 날짜 목록을 만든다.
///
/// days = 7이면:
/// - 오늘 포함
/// - 오늘로부터 6일 전 ~ 오늘
///
/// 예:
/// 오늘이 2026-05-15라면
/// 2026-05-09 ~ 2026-05-15
fn recent_date_range(days: i64) -> Vec<NaiveDate> {
    let today = Utc::now().date_naive();
    let start = today - Duration::days(days - 1);

    (0..days)
        .map(|offset| start + Duration::days(offset))
        .collect()
}

/// NaiveDate를 YYYY-MM-DD 문자열로 변환한다.
///
/// 프론트 그래프 x축 라벨과 key로 사용한다.
fn format_date_key(date: NaiveDate) -> String {
    date.format("%Y-%m-%d").to_string()
}

/// 특정 테이블의 created_at 기준 최근 N일 count를 계산한다.
///
/// table:
/// - "users"
/// - "content_reports"
///
/// date_column:
/// - 보통 "created_at"
///
/// 동작:
/// 1. 최근 N일 날짜 목록 생성
/// 2. 각 날짜를 0으로 초기화
/// 3. Supabase REST에서 해당 기간의 created_at row 조회
/// 4. Rust에서 날짜별 count 집계
///
/// 왜 0을 미리 채우는가?
/// - 신고가 0건인 날도 그래프에는 날짜가 보여야 한다.
/// - 그래야 막대 그래프가 날짜 순서대로 안정적으로 보인다.
///
/// 주의:
/// - table/date_column은 외부 입력을 그대로 받는 용도가 아니다.
/// - service 내부에서 고정된 테이블명/컬럼명만 넣어 호출한다.
async fn get_created_at_daily_trend(
    state: &AppState,
    table: &str,
    date_column: &str,
    days: i64,
) -> Result<Vec<AdminDashboardTrendPoint>> {
    #[derive(Debug, serde::Deserialize)]
    struct CreatedAtRow {
        created_at: Option<DateTime<Utc>>,
    }

    let dates = recent_date_range(days);

    let first_date = dates
        .first()
        .copied()
        .ok_or_else(|| anyhow!("추이 그래프 날짜 범위를 만들 수 없습니다."))?;

    let last_date = dates
        .last()
        .copied()
        .ok_or_else(|| anyhow!("추이 그래프 날짜 범위를 만들 수 없습니다."))?;

    // 첫날 00:00:00 UTC
    let start_at = first_date
        .and_hms_opt(0, 0, 0)
        .ok_or_else(|| anyhow!("추이 시작 날짜 변환 실패"))?
        .and_utc();

    // 마지막 날 다음날 00:00:00 UTC
    // lt 조건으로 사용해서 마지막 날 전체를 포함한다.
    let end_at = (last_date + Duration::days(1))
        .and_hms_opt(0, 0, 0)
        .ok_or_else(|| anyhow!("추이 종료 날짜 변환 실패"))?
        .and_utc();

    // 날짜별 count를 0으로 초기화한다.
    //
    // BTreeMap을 쓰면 날짜 문자열 순서가 보장된다.
    let mut counts = std::collections::BTreeMap::<String, i64>::new();

    for date in &dates {
        counts.insert(format_date_key(*date), 0);
    }

    // to_rfc3339()는 String을 새로 만든다.
    //
    // 아래처럼 바로 참조하면 안 된다:
    // urlencoding::encode(&start_at.to_rfc3339())
    //
    // 이유:
    // - start_at.to_rfc3339()가 임시 String을 만든다.
    // - urlencoding::encode()는 그 String을 빌린 Cow<str>를 반환할 수 있다.
    // - 그런데 임시 String은 그 줄이 끝나면 drop된다.
    // - 이후 format!에서 start_encoded/end_encoded를 쓰려고 하면
    //   이미 사라진 임시 값을 참조하는 상태가 되어
    //   "temporary value dropped while borrowed" 에러가 난다.
    //
    // 해결:
    // - to_rfc3339() 결과를 먼저 변수에 보관한다.
    // - encode 결과도 into_owned()로 소유 String으로 만든다.
    let start_at_rfc3339 = start_at.to_rfc3339();
    let end_at_rfc3339 = end_at.to_rfc3339();

    let start_encoded = urlencoding::encode(&start_at_rfc3339).into_owned();
    let end_encoded = urlencoding::encode(&end_at_rfc3339).into_owned();

    let url = format!(
        "{}/rest/v1/{}?select={}&{}=gte.{}&{}=lt.{}&limit=10000",
        state.config.supabase_url.trim_end_matches('/'),
        table,
        date_column,
        date_column,
        start_encoded,
        date_column,
        end_encoded
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .with_context(|| format!("관리자 대시보드 추이 조회 요청 실패: {}", table))?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!(
            "관리자 대시보드 추이 조회 실패: table={}, body={}",
            table,
            body
        ));
    }

    let rows: Vec<CreatedAtRow> = res
        .json()
        .await
        .with_context(|| format!("관리자 대시보드 추이 응답 파싱 실패: {}", table))?;

    for row in rows {
        let Some(created_at) = row.created_at else {
            continue;
        };

        let key = format_date_key(created_at.date_naive());

        if let Some(count) = counts.get_mut(&key) {
            *count += 1;
        }
    }

    let trend = counts
        .into_iter()
        .map(|(date, count)| AdminDashboardTrendPoint { date, count })
        .collect();

    Ok(trend)
}

/// 관리자: 대시보드 추이 그래프 조회.
///
/// API:
/// GET /api/admin/dashboard/trends
///
/// 반환:
/// - 최근 7일 가입자 추이
/// - 최근 7일 신고 접수 추이
pub async fn get_dashboard_trends(
    state: &AppState,
) -> Result<AdminDashboardTrendsResponse> {
    let user_signup_trend =
        get_created_at_daily_trend(state, "users", "created_at", 7).await?;

    let report_created_trend =
        get_created_at_daily_trend(state, "content_reports", "created_at", 7).await?;

    Ok(AdminDashboardTrendsResponse {
        user_signup_trend,
        report_created_trend,
    })
}

/// 관리자: 신고 목록 조회 (페이지네이션 + 검색 + 필터 + 날짜 범위 + 정렬)
///
/// API 예시:
/// GET /api/admin/content-reports?status=pending&page=1&page_size=20
/// GET /api/admin/content-reports?keyword=은영&target_type=post&reason=spam
/// GET /api/admin/content-reports?start_date=2026-05-01&end_date=2026-05-13
/// GET /api/admin/content-reports?sort_by=reviewed_at&sort_order=desc
///
/// 동작:
/// - admin_content_reports_view에서 조회
/// - 필터:
///   - status
///   - target_type
///   - reason
///   - keyword: 신고자 닉네임/이메일
///   - start_date/end_date: 신고일 created_at 기준
/// - 정렬:
///   - created_at: 신고일
///   - reviewed_at: 처리일
/// - 페이지네이션:
///   - Range 헤더
///   - Prefer: count=exact
pub async fn list_content_reports(
    state: &AppState,
    query: ContentReportQuery,
) -> Result<AdminContentReportListResponse> {
    // ─────────────────────────────────────────────
    // 1. 페이지 / 페이지 크기 정규화
    // ─────────────────────────────────────────────
    //
    // page는 1부터 시작.
    // page_size는 너무 큰 요청을 막기 위해 1~100 사이로 제한한다.
    let page = query.page.unwrap_or(1).max(1);
    let page_size = query.page_size.unwrap_or(20).clamp(1, 100);

    // PostgREST Range 헤더는 0-base inclusive다.
    //
    // page=1, page_size=20 → 0-19
    // page=2, page_size=20 → 20-39
    let from = (page - 1) * page_size;
    let to = from + page_size - 1;

    // ─────────────────────────────────────────────
    // 2. 정렬 파라미터 정규화
    // ─────────────────────────────────────────────
    //
    // 허용하지 않는 값이 들어오면 안전하게 기본값으로 되돌린다.
    let sort_by = match query.sort_by.as_deref().map(str::trim) {
        Some("reviewed_at") => "reviewed_at",
        _ => "created_at",
    };

    let sort_order = match query.sort_order.as_deref().map(str::trim) {
        Some("asc") => "asc",
        _ => "desc",
    };

    // reviewed_at은 pending 상태에서는 null일 수 있다.
    // 처리일순 정렬에서 null이 위로 몰리면 보기 불편하므로 nullslast를 붙인다.
    let nulls = if sort_by == "reviewed_at" {
        ".nullslast"
    } else {
        ""
    };

    // ─────────────────────────────────────────────
    // 3. 기본 조회 URL
    // ─────────────────────────────────────────────
    //
    // target_report_count는 view에서 계산된 누적 신고 횟수다.
    let mut url = format!(
        "{}/rest/v1/admin_content_reports_view?select=id,reporter_id,reporter_nickname,reporter_email,target_type,target_id,reason,detail,status,created_at,reviewed_at,reviewed_by,target_report_count&order={}.{}{}",
        state.config.supabase_url.trim_end_matches('/'),
        sort_by,
        sort_order,
        nulls
    );

    // ─────────────────────────────────────────────
    // 4. 일반 필터 추가
    // ─────────────────────────────────────────────
    //
    // 빈 문자열은 필터로 취급하지 않는다.
    if let Some(status) = query
        .status
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        url.push_str(&format!("&status=eq.{}", urlencoding::encode(status)));
    }

    if let Some(target_type) = query
        .target_type
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        url.push_str(&format!(
            "&target_type=eq.{}",
            urlencoding::encode(target_type)
        ));
    }

    if let Some(reason) = query
        .reason
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        url.push_str(&format!("&reason=eq.{}", urlencoding::encode(reason)));
    }

    // ─────────────────────────────────────────────
    // 5. 날짜 범위 필터
    // ─────────────────────────────────────────────
    //
    // 날짜 필터는 신고 접수일(created_at) 기준이다.
    //
    // start_date=2026-05-01
    // → created_at >= 2026-05-01T00:00:00+09:00
    //
    // end_date=2026-05-13
    // → created_at <= 2026-05-13T23:59:59+09:00
    if let Some(start_date) = query
        .start_date
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        let normalized = normalize_start_date_filter(start_date);
        url.push_str(&format!(
            "&created_at=gte.{}",
            urlencoding::encode(&normalized)
        ));
    }

    if let Some(end_date) = query
        .end_date
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        let normalized = normalize_end_date_filter(end_date);
        url.push_str(&format!(
            "&created_at=lte.{}",
            urlencoding::encode(&normalized)
        ));
    }

    // ─────────────────────────────────────────────
    // 6. 신고자 닉네임/이메일 검색
    // ─────────────────────────────────────────────
    //
    // PostgREST or 문법:
    // or=(reporter_nickname.ilike.*은영*,reporter_email.ilike.*은영*)
    if let Some(keyword) = query
        .keyword
        .as_deref()
        .map(str::trim)
        .filter(|value| !value.is_empty())
    {
        let encoded = urlencoding::encode(keyword);

        url.push_str(&format!(
            "&or=(reporter_nickname.ilike.*{}*,reporter_email.ilike.*{}*)",
            encoded, encoded
        ));
    }

    // ─────────────────────────────────────────────
    // 7. 요청
    // ─────────────────────────────────────────────
    //
    // Range 헤더로 현재 페이지 데이터만 받고,
    // Prefer: count=exact로 전체 개수를 Content-Range에서 받는다.
    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Range-Unit", "items")
        .header("Range", format!("{}-{}", from, to))
        .header("Prefer", "count=exact")
        .send()
        .await
        .context("관리자 신고 목록 view SELECT 요청 실패")?;

    // PostgREST는 페이지네이션 시 200 또는 206을 반환할 수 있다.
    // 둘 다 is_success()에 포함된다.
    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 목록 view SELECT 실패: {}", body));
    }

    // ─────────────────────────────────────────────
    // 8. 전체 개수 추출
    // ─────────────────────────────────────────────
    //
    // Content-Range 예:
    // 0-19/153
    //
    // 슬래시 뒤의 153이 total_count다.
    let total_count = res
        .headers()
        .get("content-range")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.rsplit('/').next())
        .and_then(|value| value.parse::<i64>().ok())
        .unwrap_or(0);

    // ─────────────────────────────────────────────
    // 9. 본문 파싱 및 DTO 변환
    // ─────────────────────────────────────────────
    let rows: Vec<AdminContentReport> = res
        .json()
        .await
        .context("관리자 신고 목록 view SELECT 응답 역직렬화 실패")?;

    let items: Vec<AdminContentReportResponse> = rows.into_iter().map(to_report_response).collect();

    Ok(AdminContentReportListResponse {
        items,
        total_count,
        page,
        page_size,
    })
}

/// 관리자 감사 로그를 기록한다.
///
/// 이 함수는 중요한 관리자 작업이 성공한 뒤 호출한다.
///
/// 현재 사용 위치:
/// - 신고 처리 완료
/// - 신고 반려
///
/// 왜 별도 테이블에 남기는가?
/// - content_reports.reviewed_by/reviewed_at은 최종 상태만 보여준다.
/// - admin_audit_logs는 "누가 언제 어떤 작업을 했는지" 이력을 남긴다.
/// - 나중에 회원 비활성화, 공지 삭제, 콘테스트 상태 변경도 같은 테이블로 확장 가능하다.
async fn create_admin_audit_log(
    state: &AppState,
    admin_id: Uuid,
    action: &str,
    target_type: &str,
    target_id: Uuid,
    before_status: Option<&str>,
    after_status: Option<&str>,
    metadata: serde_json::Value,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/admin_audit_logs",
        state.config.supabase_url.trim_end_matches('/')
    );

    let payload = serde_json::json!([{
        "admin_id": admin_id,
        "action": action,
        "target_type": target_type,
        "target_id": target_id,
        "before_status": before_status,
        "after_status": after_status,
        "metadata": metadata,
        "created_at": Utc::now().to_rfc3339()
    }]);

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&payload)
        .send()
        .await
        .context("관리자 감사 로그 생성 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 감사 로그 생성 실패: {}", body));
    }

    Ok(())
}

/// 실제 content_reports 테이블에서 현재 신고 상태를 조회한다.
///
/// 감사 로그의 before_status를 남기기 위해 PATCH 전에 호출한다.
/// view가 아니라 실제 테이블을 보는 이유:
/// - 필요한 값은 status 하나뿐이다.
/// - view 변경과 무관하게 안정적으로 동작한다.
struct ContentReportReviewTarget {
    status: String,
    reporter_id: Uuid,
    target_type: String,
    target_id: Uuid,
}

async fn get_content_report_review_target_by_id(
    state: &AppState,
    report_id: Uuid,
) -> Result<ContentReportReviewTarget> {
    let url = format!(
        "{}/rest/v1/content_reports?id=eq.{}&select=status,reporter_id,target_type,target_id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("신고 현재 상태 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("신고 현재 상태 조회 실패: {}", body));
    }

    #[derive(serde::Deserialize)]
    struct ReportReviewTargetRow {
        status: String,
        reporter_id: Uuid,
        target_type: String,
        target_id: Uuid,
    }

    let rows: Vec<ReportReviewTargetRow> = res
        .json()
        .await
        .context("신고 현재 상태 조회 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고를 찾을 수 없습니다."))?;

    Ok(ContentReportReviewTarget {
        status: row.status,
        reporter_id: row.reporter_id,
        target_type: row.target_type,
        target_id: row.target_id,
    })
}

async fn get_report_target_owner_id(
    state: &AppState,
    target_type: &str,
    target_id: Uuid,
) -> Result<Option<Uuid>> {
    let base_url = state.config.supabase_url.trim_end_matches('/');
    let (table, select) = match target_type {
        "post" => ("posts", "user_id"),
        "comment" => ("comments", "user_id"),
        "user_nickname" | "user_profile" => return Ok(Some(target_id)),
        _ => return Ok(None),
    };

    let url = format!(
        "{}/rest/v1/{}?id=eq.{}&select={}&limit=1",
        base_url, table, target_id, select
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("신고 대상 소유자 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("신고 대상 소유자 조회 실패: {}", body));
    }

    #[derive(serde::Deserialize)]
    struct OwnerRow {
        user_id: Uuid,
    }

    let rows: Vec<OwnerRow> = res
        .json()
        .await
        .context("신고 대상 소유자 조회 응답 파싱 실패")?;

    Ok(rows.into_iter().next().map(|row| row.user_id))
}

fn resolve_report_notification_messages(
    action_type: ResolveReportActionType,
    _target_type: &str,
) -> (&'static str, Option<&'static str>) {
    match action_type {
        ResolveReportActionType::NoAction => {
            ("접수하신 신고가 조치 없이 처리 완료되었습니다.", None)
        }
        ResolveReportActionType::PostDeleted => (
            "신고해 주신 게시글을 검토한 결과, 운영 정책에 따라 조치가 완료되었습니다.",
            Some(
                "작성하신 게시글이 운영 정책 위반으로 삭제되었습니다. 반복 위반 시 계정 이용이 제한될 수 있습니다.",
            ),
        ),
        ResolveReportActionType::CommentDeleted => (
            "신고해 주신 댓글을 검토한 결과, 운영 정책에 따라 조치가 완료되었습니다.",
            Some(
                "작성하신 댓글이 운영 정책 위반으로 삭제되었습니다. 반복 위반 시 계정 이용이 제한될 수 있습니다.",
            ),
        ),
        ResolveReportActionType::ProfileImageChangeRequested => (
            "신고해 주신 프로필 사진을 검토한 결과, 운영 정책에 따라 변경 요청 조치가 완료되었습니다.",
            Some(
                "프로필 사진이 운영 정책 위반 가능성으로 신고되었습니다. 부적절한 이미지일 경우 프로필 사진을 변경해 주세요. 반복 위반 시 계정 이용이 제한될 수 있습니다.",
            ),
        ),
        ResolveReportActionType::ProfileImageReset => (
            "신고해 주신 프로필 사진을 검토한 결과, 운영 정책에 따라 조치가 완료되었습니다.",
            Some(
                "프로필 사진이 운영 정책 위반으로 기본 이미지로 변경되었습니다. 반복 위반 시 계정 이용이 제한될 수 있습니다.",
            ),
        ),
        ResolveReportActionType::NicknameChangeRequested => (
            "신고해 주신 닉네임을 검토한 결과, 운영 정책에 따라 변경 요청 조치가 완료되었습니다.",
            Some(
                "닉네임이 운영 정책 위반 가능성으로 신고되었습니다. 부적절한 표현이 포함되어 있다면 닉네임을 변경해 주세요. 반복 위반 시 계정 이용이 제한될 수 있습니다.",
            ),
        ),
    }
}

fn validate_resolve_action_for_target(
    action_type: ResolveReportActionType,
    target_type: &str,
) -> Result<()> {
    let valid = match action_type {
        ResolveReportActionType::NoAction => true,
        ResolveReportActionType::PostDeleted => target_type == "post",
        ResolveReportActionType::CommentDeleted => target_type == "comment",
        ResolveReportActionType::ProfileImageChangeRequested
        | ResolveReportActionType::ProfileImageReset => target_type == "user_profile",
        ResolveReportActionType::NicknameChangeRequested => target_type == "user_nickname",
    };

    if valid {
        Ok(())
    } else {
        Err(anyhow!("신고 대상과 처리 조치가 맞지 않습니다."))
    }
}

/// content_reports 원본 테이블에서 신고 정보를 조회한다.
///
/// apply-action에서는 다음 정보가 필요하다.
/// - target_type: 어떤 대상 신고인지
/// - target_id: 실제 조치 대상 id
/// - status: 이미 처리된 신고인지 확인
///
/// view가 아니라 원본 테이블을 보는 이유:
/// - 필요한 정보가 명확하고 적다.
/// - 신고 상태 변경 전 before_status를 감사 로그에 남기기 좋다.
async fn get_content_report_base_by_id(
    state: &AppState,
    report_id: Uuid,
) -> Result<ContentReportBaseRow> {
    let url = format!(
        "{}/rest/v1/content_reports?id=eq.{}&select=id,reporter_id,target_type,target_id,status&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("신고 원본 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("신고 원본 조회 실패: {}", body));
    }

    let rows: Vec<ContentReportBaseRow> = res
        .json()
        .await
        .context("신고 원본 조회 응답 파싱 실패")?;

    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고를 찾을 수 없습니다."))
}

/// 관리자: 게시글 신고 대상 상세 조회.
async fn get_admin_post_target_detail(
    state: &AppState,
    post_id: Uuid,
) -> Result<AdminReportTargetDetailResponse> {
    let url = format!(
        "{}/rest/v1/admin_post_targets_view?id=eq.{}&select=id,author_id,author_nickname,author_email,author_profile_image,title,content,image_url,is_deleted,deleted_at,created_at,updated_at&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        post_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 게시글 신고 대상 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 게시글 신고 대상 조회 실패: {}", body));
    }

    let rows: Vec<AdminPostTargetRow> = res
        .json()
        .await
        .context("관리자 게시글 신고 대상 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고 대상 게시글을 찾을 수 없습니다."))?;

    Ok(to_post_target_detail(state, row).await)
}

/// 관리자: 댓글 신고 대상 상세 조회.
async fn get_admin_comment_target_detail(
    state: &AppState,
    comment_id: Uuid,
) -> Result<AdminReportTargetDetailResponse> {
    let url = format!(
        "{}/rest/v1/admin_comment_targets_view?id=eq.{}&select=id,post_id,parent_id,author_id,author_nickname,author_email,author_profile_image,content,is_deleted,deleted_at,created_at,updated_at&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        comment_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 댓글 신고 대상 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 댓글 신고 대상 조회 실패: {}", body));
    }

    let rows: Vec<AdminCommentTargetRow> = res
        .json()
        .await
        .context("관리자 댓글 신고 대상 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고 대상 댓글을 찾을 수 없습니다."))?;

    Ok(to_comment_target_detail(state, row).await)
}

/// 관리자: 사용자 신고 대상 row 조회.
///
/// user_profile / user_nickname 신고에서 공통으로 사용한다.
async fn get_admin_user_target_row(
    state: &AppState,
    user_id: Uuid,
) -> Result<AdminUserTargetRow> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=id,email,nickname,profile_image,is_active,deleted_at,created_at,updated_at&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 사용자 신고 대상 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 사용자 신고 대상 조회 실패: {}", body));
    }

    let rows: Vec<AdminUserTargetRow> = res
        .json()
        .await
        .context("관리자 사용자 신고 대상 응답 파싱 실패")?;

    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고 대상 사용자를 찾을 수 없습니다."))
}

/// 관리자: 신고 상태 변경 공통 함수
///
/// resolve/reject가 하는 일이 거의 같기 때문에
/// status만 인자로 받아서 공통 처리한다.
///
/// 처리 시 업데이트하는 컬럼:
/// - status: resolved 또는 rejected
/// - reviewed_at: 현재 시각
/// - reviewed_by: 처리한 관리자 user_id
///
/// 추가:
/// - 상태 변경 성공 후 admin_audit_logs에 감사 로그를 남긴다.
async fn update_content_report_status(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
    status: &str,
    action_type: ResolveReportActionType,
) -> Result<AdminContentReportResponse> {
    // 1. 변경 전 상태 조회.
    //
    // 감사 로그에 before_status를 남기기 위해 PATCH 전에 조회한다.
    let review_target = get_content_report_review_target_by_id(state, report_id).await?;
    let before_status = review_target.status;

    if status == "resolved" {
        validate_resolve_action_for_target(action_type, &review_target.target_type)?;
    }

    // 2. 이미 같은 상태라면 불필요한 중복 처리를 막는다.
    //
    // 예:
    // 이미 resolved인데 다시 resolved 요청이 온 경우.
    if before_status == status {
        return get_content_report_from_view_by_id(state, report_id).await;
    }

    let url = format!(
        "{}/rest/v1/content_reports?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let reviewed_at = Utc::now().to_rfc3339();

    // 3. 실제 신고 상태 변경.
    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "status": status,
            "reviewed_at": reviewed_at,
            "reviewed_by": admin_id
        }))
        .send()
        .await
        .context("관리자 신고 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 상태 변경 실패: {}", body));
    }

    // PATCH 응답 본문은 여기서 쓰지 않는다.
    // reporter_nickname/reporter_email/target_report_count가 필요해서
    // 최종 응답은 view에서 다시 조회한다.

    // 4. 감사 로그 기록.
    //
    // action은 status에 따라 명확하게 분리한다.
    let action = match status {
        "resolved" => "content_report_resolved",
        "rejected" => "content_report_rejected",
        _ => "content_report_status_changed",
    };

    create_admin_audit_log(
        state,
        admin_id,
        action,
        "content_report",
        report_id,
        Some(before_status.as_str()),
        Some(status),
        serde_json::json!({
            "report_id": report_id,
            "reviewed_by": admin_id,
            "reviewed_at": reviewed_at,
            "action_type": action_type.as_str()
        }),
    )
    .await?;

    let (notification_type, message) = if status == "resolved" {
        let (reporter_message, target_message) =
            resolve_report_notification_messages(action_type, &review_target.target_type);

        if let Some(target_message) = target_message {
            match get_report_target_owner_id(
                state,
                &review_target.target_type,
                review_target.target_id,
            )
            .await
            {
                Ok(Some(owner_id)) if owner_id != review_target.reporter_id => {
                    let target_notification_type =
                        format!("report_target_{}", &report_id.to_string()[..8]);
                    if let Err(e) = crate::notification::service::create_notification(
                        state,
                        owner_id,
                        &target_notification_type,
                        target_message,
                    )
                    .await
                    {
                        tracing::error!("신고 대상자 조치 알림 생성 실패: {}", e);
                    }
                }
                Ok(_) => {}
                Err(e) => {
                    tracing::error!("신고 대상자 조회 실패: {}", e);
                }
            }
        }

        (
            format!("report_res_{}", &report_id.to_string()[..8]),
            reporter_message,
        )
    } else if status == "rejected" {
        (
            format!("report_rej_{}", &report_id.to_string()[..8]),
            "접수하신 신고가 검토 후 반려되었어요.",
        )
    } else {
        (
            format!("report_{}_{}", status, &report_id.to_string()[..8]),
            "접수하신 신고 상태가 변경되었어요.",
        )
    };

    if let Err(e) = crate::notification::service::create_notification(
        state,
        review_target.reporter_id,
        &notification_type,
        message,
    )
    .await
    {
        tracing::error!("신고 처리 결과 알림 생성 실패: {}", e);
    }

    // 5. 최종 응답은 view에서 다시 조회한다.
    get_content_report_from_view_by_id(state, report_id).await
}

/// 관리자: 신고 처리 완료
///
/// API:
/// PATCH /api/admin/content-reports/{id}/resolve
///
/// 의미:
/// - 신고 내용이 타당하다고 보고 처리 완료 처리
/// - 실제 게시글/댓글 삭제까지 자동으로 하지는 않음
/// - 삭제는 기존 관리자 기능이나 별도 API에서 처리하는 방식이 더 안전함
pub async fn resolve_content_report(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
    action_type: Option<ResolveReportActionType>,
) -> Result<AdminContentReportResponse> {
    update_content_report_status(
        state,
        admin_id,
        report_id,
        "resolved",
        action_type.unwrap_or(ResolveReportActionType::NoAction),
    )
    .await
}

/// 신고를 처리완료 상태로 변경한다.
///
/// apply-action은 실제 운영 조치를 수행한 뒤 신고를 resolved 처리한다.
/// 기존 resolve API와 역할은 비슷하지만,
/// 여기서는 운영 조치 action과 감사 로그 metadata를 함께 관리하기 위해 별도 함수로 둔다.
async fn mark_content_report_resolved_by_action(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
    reviewed_at: &str,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/content_reports?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&json!({
            "status": "resolved",
            "reviewed_by": admin_id,
            "reviewed_at": reviewed_at
        }))
        .send()
        .await
        .context("운영 조치 후 신고 처리완료 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("운영 조치 후 신고 처리완료 실패: {}", body));
    }

    Ok(())
}

/// 사용자의 프로필 이미지를 기본 이미지로 되돌린다.
///
/// 주의:
/// - profile_image를 null로 만들지 않는다.
/// - 프로젝트 기본 프로필 이미지 경로가 "defaults/avatar.png"이므로
///   운영자가 프로필 이미지를 제거하면 이 기본 이미지로 교체한다.
///
/// 왜 storage 파일을 직접 삭제하지 않는가?
/// - 업로드된 파일이 다른 곳에서 참조될 수 있다.
/// - storage object 삭제 권한/경로 처리까지 같이 하면 로직이 커진다.
/// - 관리자 조치의 목적은 "사용자 화면에서 부적절한 이미지가 더 이상 보이지 않게 하는 것"이므로
///   users.profile_image 값을 기본 이미지 경로로 바꾸는 것으로 충분하다.
async fn clear_user_profile_image_by_admin(
    state: &AppState,
    user_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&serde_json::json!({
            // 프로젝트 기본 프로필 이미지 경로.
            //
            // 기존 코드처럼 null로 두면 프론트에서 null 처리 분기가 필요하거나
            // 이미지가 깨질 수 있다.
            "profile_image": "defaults/avatar.png",

            // users 테이블에 updated_at을 운영 중이라면 같이 갱신한다.
            "updated_at": Utc::now().to_rfc3339()
        }))
        .send()
        .await
        .context("관리자 프로필 이미지 기본값 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!(
            "관리자 프로필 이미지 기본값 변경 실패: {}",
            body
        ));
    }

    Ok(())
}

/// 관리자: 신고에 실제 운영 조치를 적용한다.
///
/// API:
/// PATCH /api/admin/content-reports/{id}/apply-action
///
/// 지원 action:
/// - delete_post
/// - delete_comment
/// - clear_profile_image
/// - request_profile_image_change
/// - request_nickname_change
///
/// 알림 제외 버전 동작:
/// - 게시글/댓글/프로필 이미지 제거는 실제 데이터 변경
/// - 닉네임 변경 요청/프로필 사진 변경 요청은 실제 데이터 변경 없이
///   신고 resolved 처리와 감사 로그 기록만 수행
///
/// 처리 흐름:
/// 1. 신고 원본 조회
/// 2. 이미 처리된 신고인지 확인
/// 3. action과 target_type 매칭 검증
/// 4. 실제 조치 수행
/// 5. 신고 status = resolved 처리
/// 6. 관리자 감사 로그 기록
/// 7. 최신 신고 row를 view에서 다시 조회해 반환
pub async fn apply_content_report_action(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
    req: ApplyContentReportActionRequest,
) -> Result<AdminContentReportResponse> {
    // 1. 신고 원본 조회.
    let report = get_content_report_base_by_id(state, report_id).await?;

    // 2. 이미 처리된 신고인지 확인.
    //
    // 이미 resolved/rejected인 신고에 다시 운영 조치를 적용하면
    // 중복 삭제/중복 감사 로그가 발생할 수 있다.
    if report.status != "pending" {
        return Err(anyhow!("이미 처리된 신고입니다."));
    }

    // 3. action과 target_type 검증.
    validate_report_action_target(&req.action, &report.target_type)?;

    // 4. 실제 운영 조치 수행.
    //
    // request_profile_image_change / request_nickname_change는
    // 알림 기능을 제외했기 때문에 실제 데이터 변경은 없다.
    // 대신 아래 감사 로그 metadata에 어떤 요청 조치를 했는지 기록한다.
    match req.action {
        AdminReportAction::DeletePost => {
            soft_delete_post_by_admin(state, report.target_id).await?;
        }
        AdminReportAction::DeleteComment => {
            soft_delete_comment_by_admin(state, report.target_id).await?;
        }
        AdminReportAction::ClearProfileImage => {
            clear_user_profile_image_by_admin(state, report.target_id).await?;
        }
        AdminReportAction::RequestProfileImageChange => {
            // 알림 제외 버전:
            // 여기서는 실제 데이터 변경 없음.
            // 나중에 알림 담당 기능과 연결하면 이 action에서 알림을 생성하면 됨.
        }
        AdminReportAction::RequestNicknameChange => {
            // 알림 제외 버전:
            // 여기서는 실제 데이터 변경 없음.
            // 나중에 알림 담당 기능과 연결하면 이 action에서 알림을 생성하면 됨.
        }
    }

    // 5. 신고 상태 resolved 처리.
    let reviewed_at = Utc::now().to_rfc3339();

    mark_content_report_resolved_by_action(
        state,
        admin_id,
        report_id,
        &reviewed_at,
    )
        .await?;

    // 6. 감사 로그 기록.
    //
    // create_admin_audit_log는 이전에 감사 로그 기능 만들 때 추가한 함수다.
    // 같은 service.rs 안에 있으면 private 함수여도 호출 가능하다.
    create_admin_audit_log(
        state,
        admin_id,
        "content_report_action_applied",
        "content_report",
        report_id,
        Some(report.status.as_str()),
        Some("resolved"),
        json!({
            "report_action": admin_report_action_as_str(&req.action),
            "report_target_type": report.target_type,
            "report_target_id": report.target_id,
            "reviewed_by": admin_id,
            "reviewed_at": reviewed_at,
            "notification_excluded": true
        }),
    )
        .await?;

    // 7. 최신 신고 row 반환.
    //
    // get_content_report_from_view_by_id는 기존 신고 목록용 view에서
    // reporter_nickname, reporter_email, target_report_count 등을 포함해서 조회하는 함수다.
    get_content_report_from_view_by_id(state, report_id).await
}


/// 관리자: 신고 반려
///
/// API:
/// PATCH /api/admin/content-reports/{id}/reject
///
/// 의미:
/// - 신고가 부적절하거나 처리 대상이 아니라고 판단
/// - status를 rejected로 변경
pub async fn reject_content_report(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
) -> Result<AdminContentReportResponse> {
    update_content_report_status(
        state,
        admin_id,
        report_id,
        "rejected",
        ResolveReportActionType::NoAction,
    )
    .await
}

/// 관리자: 특정 신고의 감사 로그 조회
///
/// API:
/// GET /api/admin/content-reports/{id}/audit-logs
///
/// 동작:
/// - admin_audit_logs에서 target_type=content_report
/// - target_id=신고 ID
/// - created_at desc 정렬
pub async fn list_content_report_audit_logs(
    state: &AppState,
    report_id: Uuid,
) -> Result<Vec<AdminAuditLogResponse>> {
    let url = format!(
        "{}/rest/v1/admin_audit_logs?select=id,admin_id,action,target_type,target_id,before_status,after_status,metadata,created_at&target_type=eq.content_report&target_id=eq.{}&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        report_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 감사 로그 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 감사 로그 목록 조회 실패: {}", body));
    }

    let rows: Vec<AdminAuditLog> = res
        .json()
        .await
        .context("관리자 감사 로그 목록 응답 파싱 실패")?;

    Ok(rows.into_iter().map(to_audit_log_response).collect())
}

/// 관리자: 신고 대상 상세 조회.
///
/// API:
/// GET /api/admin/content-reports/{id}/target
///
/// 이 함수가 필요한 이유:
/// - 신고 상세 모달에서 target_id만 보여주면 관리자가 판단할 수 없다.
/// - 게시글이면 제목/내용/이미지,
///   댓글이면 댓글 내용,
///   프로필 신고면 현재 프로필 이미지,
///   닉네임 신고면 현재 닉네임을 보여줘야 한다.
///
/// 처리 흐름:
/// 1. 신고 ID로 content_reports 원본 row 조회
/// 2. target_type 확인
/// 3. target_type별 상세 조회
pub async fn get_content_report_target_detail(
    state: &AppState,
    report_id: Uuid,
) -> Result<AdminReportTargetDetailResponse> {
    let report = get_content_report_base_by_id(state, report_id).await?;

    match report.target_type.as_str() {
        "post" => get_admin_post_target_detail(state, report.target_id).await,

        "comment" => get_admin_comment_target_detail(state, report.target_id).await,

        "user_profile" => {
            let user = get_admin_user_target_row(state, report.target_id).await?;
            Ok(to_user_profile_target_detail(state, user).await)
        }

        "user_nickname" => {
            let user = get_admin_user_target_row(state, report.target_id).await?;
            Ok(to_user_nickname_target_detail(user))
        }

        other => Err(anyhow!("지원하지 않는 신고 대상 타입입니다: {}", other)),
    }
}

/// AdminReportAction을 감사 로그 metadata에 넣기 좋은 문자열로 변환한다.
///
/// 예:
/// AdminReportAction::DeletePost -> "delete_post"
fn admin_report_action_as_str(action: &AdminReportAction) -> &'static str {
    match action {
        AdminReportAction::DeletePost => "delete_post",
        AdminReportAction::DeleteComment => "delete_comment",
        AdminReportAction::ClearProfileImage => "clear_profile_image",
        AdminReportAction::RequestProfileImageChange => "request_profile_image_change",
        AdminReportAction::RequestNicknameChange => "request_nickname_change",
    }
}

/// 신고 대상 타입과 운영 조치 action이 맞는지 검증한다.
///
/// 프론트에서 버튼을 target_type별로 다르게 보여줘도,
/// 백엔드 검증은 반드시 필요하다.
///
/// 예:
/// - 닉네임 신고인데 delete_post를 보내면 차단
/// - 댓글 신고인데 clear_profile_image를 보내면 차단
fn validate_report_action_target(
    action: &AdminReportAction,
    target_type: &str,
) -> Result<()> {
    let valid = match action {
        AdminReportAction::DeletePost => target_type == "post",
        AdminReportAction::DeleteComment => target_type == "comment",
        AdminReportAction::ClearProfileImage => target_type == "user_profile",
        AdminReportAction::RequestProfileImageChange => target_type == "user_profile",
        AdminReportAction::RequestNicknameChange => target_type == "user_nickname",
    };

    if !valid {
        return Err(anyhow!(
            "신고 대상 타입과 운영 조치가 일치하지 않습니다. target_type={}, action={}",
            target_type,
            admin_report_action_as_str(action)
        ));
    }

    Ok(())
}

/// 관리자: 회원 목록 조회 (페이지네이션 + 검색)
///
/// API:
/// GET /api/admin/users?keyword=은영&page=1&page_size=20
///
/// 동작:
/// - users 테이블 조회
/// - keyword가 있으면 nickname 또는 email 부분 일치 검색
/// - 정렬: created_at desc (최신 가입자 먼저)
/// - 페이지네이션: Range 헤더 + count=exact
pub async fn list_users(state: &AppState, query: AdminUserQuery) -> Result<AdminUserListResponse> {
    // 페이지 / 페이지 크기 정규화.
    let page = query.page.unwrap_or(1).max(1);
    let page_size = query.page_size.unwrap_or(20).clamp(1, 100);

    let from = (page - 1) * page_size;
    let to = from + page_size - 1;

    let keyword = query
        .keyword
        .map(|v| v.trim().to_string())
        .filter(|v| !v.is_empty());

    let mut url = format!(
        "{}/rest/v1/users?select=*&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/')
    );

    // nickname 또는 email 기준 검색
    if let Some(keyword) = keyword {
        let encoded = urlencoding::encode(&keyword);
        url.push_str(&format!(
            "&or=(nickname.ilike.*{}*,email.ilike.*{}*)",
            encoded, encoded
        ));
    }

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Range-Unit", "items")
        .header("Range", format!("{}-{}", from, to))
        .header("Prefer", "count=exact")
        .send()
        .await
        .context("관리자 회원 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 회원 목록 조회 실패: {}", body));
    }

    // Content-Range에서 전체 건수 추출
    let total_count = res
        .headers()
        .get("content-range")
        .and_then(|v| v.to_str().ok())
        .and_then(|s| s.rsplit('/').next())
        .and_then(|s| s.parse::<i64>().ok())
        .unwrap_or(0);

    let rows: Vec<AdminUser> = res
        .json()
        .await
        .context("관리자 회원 목록 응답 파싱 실패")?;

    let items = rows.into_iter().map(to_user_response).collect();

    Ok(AdminUserListResponse {
        items,
        total_count,
        page,
        page_size,
    })
}

/// 관리자: 탈퇴 회원 모니터링 목록 조회.
///
/// API:
/// GET /api/admin/users/withdrawn?page=1&page_size=20&keyword=test
///
/// 기준:
/// - users.deleted_at is not null
///
/// 제공 목적:
/// - 탈퇴 회원 보관 기간 관리
/// - 30일 재가입 제한 상태 확인
/// - 5년 보관 만료 예정일 확인
///
/// keyword:
/// - nickname 또는 email 부분 검색
pub async fn list_withdrawn_users(
    state: &AppState,
    query: AdminUserQuery,
) -> Result<AdminWithdrawnUserListResponse> {
    let page = query.page.unwrap_or(1).max(1);
    let page_size = query.page_size.unwrap_or(20).clamp(1, 100);

    let from = (page - 1) * page_size;
    let to = from + page_size - 1;

    let keyword = query
        .keyword
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty());

    let mut url = format!(
        "{}/rest/v1/users?select=*&deleted_at=not.is.null&order=deleted_at.desc",
        state.config.supabase_url.trim_end_matches('/')
    );

    if let Some(keyword) = keyword {
        let encoded = urlencoding::encode(&keyword);

        url.push_str(&format!(
            "&or=(nickname.ilike.*{}*,email.ilike.*{}*)",
            encoded, encoded
        ));
    }

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Range-Unit", "items")
        .header("Range", format!("{}-{}", from, to))
        .header("Prefer", "count=exact")
        .send()
        .await
        .context("관리자 탈퇴 회원 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!("관리자 탈퇴 회원 목록 조회 실패: {}", body));
    }

    let total_count = res
        .headers()
        .get("content-range")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.rsplit('/').next())
        .and_then(|value| value.parse::<i64>().ok())
        .unwrap_or(0);

    let rows: Vec<AdminUser> = res
        .json()
        .await
        .context("관리자 탈퇴 회원 목록 응답 파싱 실패")?;

    let items = rows
        .into_iter()
        .map(to_withdrawn_user_response)
        .collect::<Result<Vec<_>>>()?;

    Ok(AdminWithdrawnUserListResponse {
        items,
        total_count,
        page,
        page_size,
    })
}


// ─────────────────────────────────────────────
// 특정 회원의 refresh session 전체 폐기
// ─────────────────────────────────────────────
//
// 주의:
// - 전체 사용자의 refresh_sessions를 폐기하는 함수가 아니다.
// - 관리자에게 비활성 처리된 "해당 user_id"의 refresh session만 폐기한다.
//
// 사용 시점:
// - 관리자 회원관리에서 일반 회원을 비활성화할 때
//
// 효과:
// - 해당 회원의 브라우저/앱 refresh token이 더 이상 유효하지 않게 됨
// - 다음 refresh 시도 시 실패
// - 이후 auth/service.rs의 로그인 차단 로직 때문에 재로그인도 실패
//
// 왜 refresh_sessions만 폐기하나?
// - access token은 stateless JWT라 DB에 저장되어 있지 않음
// - 이미 발급된 access token은 만료 전까지 남을 수 있음
// - 대신 refresh token을 폐기하면 access token 연장이 불가능해짐
//
// 핵심:
// - 반드시 user_id=eq.{user_id} 조건을 넣어야 한다.
// - 이 조건이 빠지면 모든 회원의 refresh session을 폐기하는 사고가 날 수 있다.
async fn revoke_refresh_sessions_by_user_id(state: &AppState, user_id: Uuid) -> Result<()> {
    let now = Utc::now().to_rfc3339();

    // 핵심 조건:
    // user_id=eq.{user_id}
    //
    // 이 조건이 있어야 "비활성 처리 대상 회원의 세션만" 폐기된다.
    //
    // 절대 아래처럼 user_id 조건 없이 요청하면 안 된다.
    // /rest/v1/refresh_sessions?revoked=eq.false&revoked_at=is.null
    //
    // 그 경우 모든 회원의 살아있는 refresh session이 폐기될 수 있다.
    let url = format!(
        "{}/rest/v1/refresh_sessions?user_id=eq.{}&revoked=eq.false&revoked_at=is.null",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&serde_json::json!({
            "revoked": true,
            "revoked_at": now,
            "updated_at": now
        }))
        .send()
        .await
        .context("특정 회원 refresh session 폐기 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!("특정 회원 refresh session 폐기 실패: {}", body));
    }

    Ok(())
}

async fn get_admin_user_by_id(state: &AppState, user_id: Uuid) -> Result<AdminUser> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 회원 단건 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 회원 단건 조회 실패: {}", body));
    }

    let rows: Vec<AdminUser> = res
        .json()
        .await
        .context("관리자 회원 단건 조회 응답 파싱 실패")?;

    rows.into_iter()
        .next()
        .ok_or_else(|| anyhow!("회원을 찾을 수 없습니다."))
}

/// 관리자: 회원 활성/비활성 변경
///
/// is_active = true  -> 활성
/// is_active = false -> 비활성
///
/// 정책:
/// - 탈퇴한 회원(deleted_at is not null)은 활성/비활성 변경 불가
/// - 운영자(role_type = admin)는 활성/비활성 변경 불가
/// - 비활성 처리 시 reason / inactive_until 저장
/// - 비활성 처리 시 해당 user_id의 refresh_sessions를 전부 revoke
/// - 활성화 처리 시 inactive_* 정보 초기화
///
/// 주의:
/// - inactive_until은 이번 정책에서 자동 해제용이 아니다.
/// - 사용자 안내와 운영자 참고용이다.
/// - 실제 해제는 관리자가 활성화 버튼으로 직접 처리한다.
pub async fn update_user_active(
    state: &AppState,
    user_id: Uuid,
    is_active: bool,
    reason: Option<String>,
    inactive_until: Option<DateTime<Utc>>,
    admin_user_id: Uuid,
) -> Result<AdminUserResponse> {
    // 1. 대상 회원 조회.
    //
    // role_type / deleted_at / 현재 is_active 상태를 확인하기 위함.
    let current_user = get_admin_user_by_id(state, user_id).await?;

    // 2. 탈퇴자는 상태 변경 불가.
    if current_user.deleted_at.is_some() {
        return Err(anyhow!(
            "탈퇴한 회원은 활성/비활성 상태를 변경할 수 없습니다."
        ));
    }

    // 3. 운영자 계정은 상태 변경 불가.
    //
    // 운영자를 비활성화하면 관리자 접근이 꼬일 수 있다.
    // 운영자 권한 회수는 별도 role 관리 기능으로 처리하는 것이 안전하다.
    if current_user.role_type.as_deref() == Some("admin") {
        return Err(anyhow!(
            "운영자 계정은 활성/비활성 상태를 변경할 수 없습니다."
        ));
    }

    // 4. 같은 상태로 변경 요청한 경우.
    //
    // 단, 여기서는 사유/해제일을 수정하는 기능을 따로 만들지 않는다.
    // 이미 비활성인 회원의 사유를 바꾸고 싶으면 나중에 별도 endpoint를 만들면 된다.
    if current_user.is_active.unwrap_or(true) == is_active {
        return Ok(to_user_response(current_user));
    }

    let now = Utc::now();

    // 5. PATCH payload 구성.
    //
    // 활성화와 비활성화는 저장해야 할 컬럼이 다르므로 분기한다.
    let payload = if is_active {
        // 활성화 처리.
        //
        // 예전 비활성 사유/기간/처리자 정보는 초기화한다.
        serde_json::json!({
            "is_active": true,
            "inactive_reason": null,
            "inactive_at": null,
            "inactive_until": null,
            "inactive_by": null,
            "updated_at": now.to_rfc3339()
        })
    } else {
        // 비활성화 처리.
        //
        // 사유가 비어 있으면 기본 사유를 넣는다.
        // 운영자에게 사유 입력을 필수로 강제하고 싶으면 여기서 Err를 반환해도 된다.
        let normalized_reason = reason
            .as_deref()
            .map(str::trim)
            .filter(|v| !v.is_empty())
            .ok_or_else(|| anyhow!("비활성화 사유를 입력해 주세요."))?;

        serde_json::json!({
            "is_active": false,
            "inactive_reason": normalized_reason,
            "inactive_at": now.to_rfc3339(),
            "inactive_until": inactive_until.map(|v| v.to_rfc3339()),
            "inactive_by": admin_user_id,
            "updated_at": now.to_rfc3339()
        })
    };

    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("관리자 회원 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 회원 상태 변경 실패: {}", body));
    }

    let rows: Vec<AdminUser> = res
        .json()
        .await
        .context("관리자 회원 상태 변경 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("회원을 찾을 수 없습니다."))?;

    // 6. 비활성화 처리라면 해당 회원의 refresh session만 폐기한다.
    //
    // 전체 유저의 세션이 아니라 user_id 조건으로 대상 회원만 처리한다.
    //
    // 활성화 처리일 때는 기존 refresh session을 복구하지 않는다.
    // 사용자가 다시 로그인해서 새 세션을 받아야 한다.
    if !is_active {
        revoke_refresh_sessions_by_user_id(state, user_id).await?;
    }

    Ok(to_user_response(row))
}

// ─────────────────────────────────────────────
// 공지사항 관리
// ─────────────────────────────────────────────

/// 공지사항 제목/내용 유효성 검사
///
/// 관리자 공지는 빈 제목/빈 내용을 허용하지 않는다.
/// 프론트에서도 막겠지만, 백엔드에서도 반드시 한 번 더 검증한다.
fn validate_notice_input(title: &str, content: &str) -> Result<()> {
    if title.trim().is_empty() {
        return Err(anyhow!("공지 제목을 입력해 주세요."));
    }

    if content.trim().is_empty() {
        return Err(anyhow!("공지 내용을 입력해 주세요."));
    }

    Ok(())
}

/// 관리자: 공지사항 목록 조회
///
/// posts 테이블에서 post_type = 'notice'이고 삭제되지 않은 글만 조회한다.
pub async fn list_notices(state: &AppState) -> Result<Vec<AdminNoticeResponse>> {
    let url = format!(
        "{}/rest/v1/posts?select=id,user_id,title,content,post_type,view_count,is_deleted,deleted_at,created_at,updated_at&post_type=eq.notice&is_deleted=eq.false&order=created_at.desc&limit=100",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 공지사항 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 공지사항 목록 조회 실패: {}", body));
    }

    let rows: Vec<AdminNotice> = res
        .json()
        .await
        .context("관리자 공지사항 목록 응답 파싱 실패")?;

    Ok(rows.into_iter().map(to_notice_response).collect())
}

/// 관리자: 공지사항 생성
///
/// user_id는 현재 관리자 ID로 저장한다.
/// post_type은 프론트에서 받지 않고 백엔드에서 notice로 고정한다.
pub async fn create_notice(
    state: &AppState,
    admin_id: Uuid,
    req: CreateAdminNoticeRequest,
) -> Result<AdminNoticeResponse> {
    validate_notice_input(&req.title, &req.content)?;

    let url = format!(
        "{}/rest/v1/posts",
        state.config.supabase_url.trim_end_matches('/')
    );

    let now = Utc::now().to_rfc3339();

    let payload = serde_json::json!([{
        "user_id": admin_id,
        "title": req.title.trim(),
        "content": req.content.trim(),
        "post_type": "notice",
        "is_deleted": false,
        "view_count": 0,
        "created_at": now,
        "updated_at": now
    }]);

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("관리자 공지사항 생성 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 공지사항 생성 실패: {}", body));
    }

    let rows: Vec<AdminNotice> = res
        .json()
        .await
        .context("관리자 공지사항 생성 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("생성된 공지사항을 확인할 수 없습니다."))?;

    Ok(to_notice_response(row))
}

/// 관리자: 공지사항 수정
///
/// post_type = notice인 글만 수정한다.
/// 일반 게시글을 실수로 수정하지 않기 위해 id와 post_type을 같이 조건으로 건다.
pub async fn update_notice(
    state: &AppState,
    notice_id: Uuid,
    req: UpdateAdminNoticeRequest,
) -> Result<AdminNoticeResponse> {
    let mut patch = serde_json::Map::new();

    if let Some(title) = req.title {
        if title.trim().is_empty() {
            return Err(anyhow!("공지 제목을 입력해 주세요."));
        }

        patch.insert("title".to_string(), serde_json::json!(title.trim()));
    }

    if let Some(content) = req.content {
        if content.trim().is_empty() {
            return Err(anyhow!("공지 내용을 입력해 주세요."));
        }

        patch.insert("content".to_string(), serde_json::json!(content.trim()));
    }

    if patch.is_empty() {
        return Err(anyhow!("수정할 공지사항 내용이 없습니다."));
    }

    patch.insert(
        "updated_at".to_string(),
        serde_json::json!(Utc::now().to_rfc3339()),
    );

    let url = format!(
        "{}/rest/v1/posts?id=eq.{}&post_type=eq.notice&is_deleted=eq.false",
        state.config.supabase_url.trim_end_matches('/'),
        notice_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&patch)
        .send()
        .await
        .context("관리자 공지사항 수정 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 공지사항 수정 실패: {}", body));
    }

    let rows: Vec<AdminNotice> = res
        .json()
        .await
        .context("관리자 공지사항 수정 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("공지사항을 찾을 수 없습니다."))?;

    Ok(to_notice_response(row))
}

/// 관리자: 공지사항 삭제
///
/// 물리 삭제하지 않고 soft delete 처리한다.
///
/// 처리:
/// - is_deleted = true
/// - deleted_at = now()
/// - updated_at = now()
pub async fn delete_notice(state: &AppState, notice_id: Uuid) -> Result<AdminNoticeResponse> {
    let url = format!(
        "{}/rest/v1/posts?id=eq.{}&post_type=eq.notice&is_deleted=eq.false",
        state.config.supabase_url.trim_end_matches('/'),
        notice_id
    );

    let now = Utc::now().to_rfc3339();

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "is_deleted": true,
            "deleted_at": now,
            "updated_at": now
        }))
        .send()
        .await
        .context("관리자 공지사항 삭제 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 공지사항 삭제 실패: {}", body));
    }

    let rows: Vec<AdminNotice> = res
        .json()
        .await
        .context("관리자 공지사항 삭제 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("공지사항을 찾을 수 없습니다."))?;

    Ok(to_notice_response(row))
}

/// 관리자: 아바타 콘테스트 목록 조회
///
/// 최신 생성순으로 최대 100개 조회한다.
pub async fn list_contests(state: &AppState) -> Result<Vec<AdminContestResponse>> {
    let url = format!(
        "{}/rest/v1/contest_events?select=id,title,description,start_date,end_date,status,reward_description,created_at&order=created_at.desc&limit=100",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("관리자 콘테스트 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 콘테스트 목록 조회 실패: {}", body));
    }

    let rows: Vec<AdminContest> = res
        .json()
        .await
        .context("관리자 콘테스트 목록 응답 파싱 실패")?;

    Ok(rows.into_iter().map(to_contest_response).collect())
}

/// 관리자: 아바타 콘테스트 생성
pub async fn create_contest(
    state: &AppState,
    req: CreateAdminContestRequest,
) -> Result<AdminContestResponse> {
    validate_contest_input(&req.title, req.start_date, req.end_date)?;

    let status = req.status.unwrap_or_else(|| "upcoming".to_string());
    validate_contest_status(&status)?;

    let url = format!(
        "{}/rest/v1/contest_events",
        state.config.supabase_url.trim_end_matches('/')
    );

    let payload = serde_json::json!([{
        "title": req.title.trim(),
        "description": req.description
            .map(|value| value.trim().to_string())
            .filter(|value| !value.is_empty()),
        "start_date": req.start_date.to_rfc3339(),
        "end_date": req.end_date.to_rfc3339(),
        "status": status,
        "reward_description": req.reward_description
            .map(|value| value.trim().to_string())
            .filter(|value| !value.is_empty())
    }]);

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("관리자 콘테스트 생성 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 콘테스트 생성 실패: {}", body));
    }

    let rows: Vec<AdminContest> = res
        .json()
        .await
        .context("관리자 콘테스트 생성 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("생성된 콘테스트를 확인할 수 없습니다."))?;

    Ok(to_contest_response(row))
}

/// 관리자: 아바타 콘테스트 수정
///
/// 일부 필드만 수정 가능.
/// 수정할 필드가 하나도 없으면 400 처리.
pub async fn update_contest(
    state: &AppState,
    contest_id: Uuid,
    req: UpdateAdminContestRequest,
) -> Result<AdminContestResponse> {
    let mut patch = serde_json::Map::new();

    if let Some(title) = req.title {
        if title.trim().is_empty() {
            return Err(anyhow!("콘테스트 제목을 입력해 주세요."));
        }

        patch.insert("title".to_string(), serde_json::json!(title.trim()));
    }

    if let Some(description) = req.description {
        let value = description.trim().to_string();

        patch.insert(
            "description".to_string(),
            if value.is_empty() {
                serde_json::Value::Null
            } else {
                serde_json::json!(value)
            },
        );
    }

    if let Some(start_date) = req.start_date {
        patch.insert(
            "start_date".to_string(),
            serde_json::json!(start_date.to_rfc3339()),
        );
    }

    if let Some(end_date) = req.end_date {
        patch.insert(
            "end_date".to_string(),
            serde_json::json!(end_date.to_rfc3339()),
        );
    }

    if let Some(status) = req.status {
        validate_contest_status(&status)?;
        patch.insert("status".to_string(), serde_json::json!(status));
    }

    if let Some(reward_description) = req.reward_description {
        let value = reward_description.trim().to_string();

        patch.insert(
            "reward_description".to_string(),
            if value.is_empty() {
                serde_json::Value::Null
            } else {
                serde_json::json!(value)
            },
        );
    }

    if patch.is_empty() {
        return Err(anyhow!("수정할 콘테스트 내용이 없습니다."));
    }

    let url = format!(
        "{}/rest/v1/contest_events?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        contest_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&patch)
        .send()
        .await
        .context("관리자 콘테스트 수정 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 콘테스트 수정 실패: {}", body));
    }

    let rows: Vec<AdminContest> = res
        .json()
        .await
        .context("관리자 콘테스트 수정 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("콘테스트를 찾을 수 없습니다."))?;

    Ok(to_contest_response(row))
}

/// 관리자: 아바타 콘테스트 상태 변경
///
/// 빠른 상태 변경 버튼에서 사용한다.
pub async fn update_contest_status(
    state: &AppState,
    contest_id: Uuid,
    req: UpdateAdminContestStatusRequest,
) -> Result<AdminContestResponse> {
    validate_contest_status(&req.status)?;

    let url = format!(
        "{}/rest/v1/contest_events?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        contest_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "status": req.status
        }))
        .send()
        .await
        .context("관리자 콘테스트 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 콘테스트 상태 변경 실패: {}", body));
    }

    let rows: Vec<AdminContest> = res
        .json()
        .await
        .context("관리자 콘테스트 상태 변경 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("콘테스트를 찾을 수 없습니다."))?;

    Ok(to_contest_response(row))
}

/// 게시글을 soft delete 처리한다.
///
/// 물리 삭제하지 않는 이유:
/// - 신고 처리 이력과 데이터 추적을 위해 원본 row를 남기는 게 좋다.
/// - 복구/감사/분쟁 대응 가능성이 있다.
async fn soft_delete_post_by_admin(
    state: &AppState,
    post_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/posts?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        post_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&json!({
            "is_deleted": true,
            "deleted_at": Utc::now().to_rfc3339()
        }))
        .send()
        .await
        .context("관리자 게시글 soft delete 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 게시글 soft delete 실패: {}", body));
    }

    Ok(())
}

/// 댓글을 soft delete 처리한다.
///
/// 부모 댓글을 삭제하면 자식 대댓글도 같이 삭제한다.
/// 기존 프론트 댓글 삭제 로직도 부모 댓글 삭제 시 자식 대댓글을 함께 제거하므로,
/// 관리자 조치도 같은 정책으로 맞춘다.
///
/// PostgREST or 문법:
/// or=(id.eq.{comment_id},parent_id.eq.{comment_id})
async fn soft_delete_comment_by_admin(
    state: &AppState,
    comment_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/comments?or=(id.eq.{},parent_id.eq.{})",
        state.config.supabase_url.trim_end_matches('/'),
        comment_id,
        comment_id
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&json!({
            "is_deleted": true,
            "deleted_at": Utc::now().to_rfc3339()
        }))
        .send()
        .await
        .context("관리자 댓글 soft delete 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 댓글 soft delete 실패: {}", body));
    }

    Ok(())
}

/// 관리자 화면에서 프로필 이미지를 보여주기 위한 signed URL을 생성한다.
///
/// 왜 프론트에서 직접 URL을 만들지 않는가?
/// - 프로필 이미지 버킷은 public이 아닐 수 있다.
/// - 실제 버킷명은 config의 supabase_profile_image_bucket을 사용한다.
/// - 커뮤니티 게시글/댓글 작성자 프로필 이미지도 백엔드에서 signed URL을 만들어 내려주는 구조다.
///
/// path 예:
/// - "defaults/avatar.png"
/// - "users/{user_id}/xxx.webp"
///
/// 반환:
/// - Supabase signedURL이 상대경로로 오면 supabase_url을 붙여서 full URL로 변환한다.
/// - 실패하면 호출부에서 .ok()로 None 처리할 수 있다.
async fn create_admin_profile_image_signed_url(
    state: &AppState,
    path: &str,
) -> Result<String> {
    let path = path.trim();

    if path.is_empty() {
        return Err(anyhow!("프로필 이미지 path가 비어 있습니다"));
    }

    let url = format!(
        "{}/storage/v1/object/sign/{}/{}",
        state.config.supabase_url.trim_end_matches('/'),
        state.config.supabase_profile_image_bucket,
        path,
    );

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .json(&json!({
            "expiresIn": 60 * 60 * 24
        }))
        .send()
        .await
        .context("관리자 프로필 이미지 signed URL 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        return Err(anyhow!(
            "관리자 프로필 이미지 signed URL 실패: {}",
            body
        ));
    }

    let body: Value = res
        .json()
        .await
        .context("관리자 프로필 이미지 signed URL 응답 파싱 실패")?;

    let signed_url = body
        .get("signedURL")
        .or_else(|| body.get("signed_url"))
        .and_then(|value| value.as_str())
        .ok_or_else(|| anyhow!("관리자 프로필 이미지 signed URL 응답이 비어 있습니다"))?;

    if signed_url.starts_with("http") {
        Ok(signed_url.to_string())
    } else {
        Ok(format!(
            "{}/storage/v1{}",
            state.config.supabase_url.trim_end_matches('/'),
            signed_url
        ))
    }
}

/// profile_image path를 signed URL로 변환한다.
///
/// 실패해도 신고 대상 상세 전체를 실패시키지는 않는다.
/// 이미지 URL 생성 실패 때문에 텍스트 신고 내용까지 못 보는 건 운영상 불편하기 때문.
async fn maybe_profile_image_signed_url(
    state: &AppState,
    path: Option<&str>,
) -> Option<String> {
    match path {
        Some(path) if !path.trim().is_empty() => {
            create_admin_profile_image_signed_url(state, path).await.ok()
        }
        _ => None,
    }
}
