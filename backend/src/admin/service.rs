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

use anyhow::{anyhow, Context, Result};
use chrono::Utc;
use uuid::Uuid;

use crate::state::AppState;

use super::{
    dto::{AdminContentReportResponse, AdminNoticeResponse, AdminUserResponse,
          CreateAdminNoticeRequest, UpdateAdminNoticeRequest,AdminContestResponse,
          CreateAdminContestRequest, UpdateAdminContestRequest, UpdateAdminContestStatusRequest},
    model::{AdminContentReport, AdminUser, AdminNotice, AdminContest},
};

/// DB/view 모델을 관리자 신고 응답 DTO로 변환한다.
///
/// DB row를 그대로 내려도 되긴 하지만,
/// 나중에 관리자 화면 전용 필드가 추가될 수 있으므로
/// 변환 함수를 따로 둔다.
///
/// 현재 AdminContentReport는 admin_content_reports_view의 응답 모델이다.
/// 그래서 reporter_nickname, reporter_email을 포함한다.
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
        "{}/rest/v1/admin_content_reports_view?select=id,reporter_id,reporter_nickname,reporter_email,target_type,target_id,reason,detail,status,created_at,reviewed_at,reviewed_by&id=eq.{}&limit=1",
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

/// 관리자: 신고 목록 조회
///
/// API:
/// GET /api/admin/content-reports
/// GET /api/admin/content-reports?status=pending
///
/// status 파라미터:
/// - None이면 전체 조회
/// - Some("pending")이면 처리 대기만 조회
/// - Some("resolved")이면 처리 완료만 조회
/// - Some("rejected")이면 반려만 조회
///
/// 반환:
/// - 최신 신고가 먼저 보이도록 created_at desc 정렬
/// - 관리자 페이지용으로 최대 100개 조회
/// - reporter_nickname, reporter_email 포함
pub async fn list_content_reports(
    state: &AppState,
    status: Option<String>,
) -> Result<Vec<AdminContentReportResponse>> {
    // status가 빈 문자열이면 필터를 적용하지 않음
    let status_filter = status
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty());

    // 기본 조회 URL
    //
    // 기존:
    // /rest/v1/content_reports?select=*
    //
    // 변경:
    // /rest/v1/admin_content_reports_view
    //
    // 이유:
    // content_reports에는 reporter_id만 있고 닉네임/이메일이 없다.
    // admin_content_reports_view는 users와 join되어 있어서
    // reporter_nickname, reporter_email을 함께 내려준다.
    let mut url = format!(
        "{}/rest/v1/admin_content_reports_view?select=id,reporter_id,reporter_nickname,reporter_email,target_type,target_id,reason,detail,status,created_at,reviewed_at,reviewed_by&order=created_at.desc&limit=100",
        state.config.supabase_url.trim_end_matches('/')
    );

    // 상태 필터가 있으면 PostgREST 쿼리 파라미터 추가
    if let Some(status) = status_filter {
        url.push_str(&format!("&status=eq.{}", urlencoding::encode(&status)));
    }

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
        .context("관리자 신고 목록 view SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 목록 view SELECT 실패: {}", body));
    }

    let rows: Vec<AdminContentReport> = res
        .json()
        .await
        .context("관리자 신고 목록 view SELECT 응답 역직렬화 실패")?;

    Ok(rows.into_iter().map(to_report_response).collect())
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
/// 중요:
/// PATCH 대상은 실제 테이블인 content_reports다.
/// view는 조회용으로만 사용한다.
///
/// 처리 후 반환:
/// - PATCH 결과를 그대로 반환하지 않는다.
/// - PATCH 결과에는 reporter_nickname, reporter_email이 없기 때문이다.
/// - 그래서 PATCH 성공 후 view에서 다시 단건 조회해서 반환한다.
async fn update_content_report_status(
    state: &AppState,
    admin_id: Uuid,
    report_id: Uuid,
    status: &str,
) -> Result<AdminContentReportResponse> {
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
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "status": status,
            "reviewed_at": Utc::now().to_rfc3339(),
            "reviewed_by": admin_id
        }))
        .send()
        .await
        .context("관리자 신고 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 상태 변경 실패: {}", body));
    }

    // PATCH가 성공했다면 최종 응답은 view에서 다시 조회한다.
    // 그래야 reporter_nickname, reporter_email이 포함된다.
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
) -> Result<AdminContentReportResponse> {
    update_content_report_status(state, admin_id, report_id, "resolved").await
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
    update_content_report_status(state, admin_id, report_id, "rejected").await
}

// ─────────────────────────────────────────────
// 회원 관리
// ─────────────────────────────────────────────

/// 관리자: 회원 목록 조회
///
/// keyword가 있으면 nickname 또는 email 기준으로 검색한다.
/// 최대 100개를 최신 가입순으로 조회한다.
pub async fn list_users(
    state: &AppState,
    keyword: Option<String>,
) -> Result<Vec<AdminUserResponse>> {
    let keyword = keyword
        .map(|v| v.trim().to_string())
        .filter(|v| !v.is_empty());

    let mut url = format!(
        "{}/rest/v1/users?select=*&order=created_at.desc&limit=100",
        state.config.supabase_url.trim_end_matches('/')
    );

    if let Some(keyword) = keyword {
        let encoded = urlencoding::encode(&keyword);

        // nickname 또는 email 기준 검색
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
        .send()
        .await
        .context("관리자 회원 목록 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 회원 목록 조회 실패: {}", body));
    }

    let rows: Vec<AdminUser> = res
        .json()
        .await
        .context("관리자 회원 목록 응답 파싱 실패")?;

    Ok(rows.into_iter().map(to_user_response).collect())
}

/// 관리자: 회원 활성/비활성 변경
///
/// is_active = true  -> 활성
/// is_active = false -> 비활성
pub async fn update_user_active(
    state: &AppState,
    user_id: Uuid,
    is_active: bool,
) -> Result<AdminUserResponse> {
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
        .json(&serde_json::json!({
            "is_active": is_active,
            "updated_at": Utc::now().to_rfc3339()
        }))
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
pub async fn delete_notice(
    state: &AppState,
    notice_id: Uuid,
) -> Result<AdminNoticeResponse> {
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