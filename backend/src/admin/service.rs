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
use chrono::{DateTime, Utc};
use uuid::Uuid;

use crate::state::AppState;

use super::{
    dto::{
        AdminAuditLogResponse, AdminContentReportListResponse, AdminContentReportResponse,
        AdminContestResponse, AdminNoticeResponse, AdminUserListResponse, AdminUserResponse,
        CreateAdminContestRequest, CreateAdminNoticeRequest, ResolveReportActionType,
        UpdateAdminContestRequest, UpdateAdminContestStatusRequest, UpdateAdminNoticeRequest,
    },
    handler::{AdminUserQuery, ContentReportQuery},
    model::{AdminAuditLog, AdminContentReport, AdminContest, AdminNotice, AdminUser},
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
