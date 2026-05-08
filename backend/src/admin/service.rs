// admin/service.rs
//
// 관리자 전용 비즈니스 로직.
//
// 현재 기능:
// 1. 신고 목록 조회
// 2. 신고 처리 완료
// 3. 신고 반려
//
// 주의:
// - 이 파일은 service_role key로 Supabase REST API를 호출한다.
// - 실제 관리자 권한 검사는 route.rs의 admin_routes에서
//   admin_middleware가 먼저 수행한다.
// - 따라서 이 service 함수들은 "이미 관리자 검증을 통과했다"는 전제로 동작한다.

use anyhow::{anyhow, Context, Result};
use chrono::Utc;
use uuid::Uuid;

use crate::state::AppState;

use super::{dto::{AdminContentReportResponse,AdminUserResponse},
            model::{AdminContentReport, AdminUser},};

/// DB 모델을 관리자 응답 DTO로 변환한다.
///
/// DB row를 그대로 내려도 되긴 하지만,
/// 나중에 관리자 화면 전용 필드가 추가될 수 있으므로
/// 변환 함수를 따로 둔다.
fn to_report_response(row: AdminContentReport) -> AdminContentReportResponse {
    AdminContentReportResponse{
        id: row.id,
        reporter_id: row.reporter_id,
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
pub async fn list_content_reports(
    state: &AppState,
    status: Option<String>,
) -> Result<Vec<AdminContentReportResponse>> {
    //status가 빈 문자열이면 필터를 적용하지 않음
    let status_filter = status
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty());

    // 기본 조회 URL
    let mut url = format!(
        "{}/rest/v1/content_reports?select=*&order=created_at.desc&limit=100",
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
        .context("관리자 content_reports SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 content_reports SELECT 실패: {}", body));
    }

    let rows: Vec<AdminContentReport> = res
        .json()
        .await
        .context("관리자 content_reports SELECT 응답 역직렬화 실패")?;

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
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "status": status,
            "reviewed_at": Utc::now(),
            "reviewed_by": admin_id
        }))
        .send()
        .await
        .context("관리자 신고 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("관리자 신고 상태 변경 실패: {}", body));
    }

    let rows: Vec<AdminContentReport> = res
        .json()
        .await
        .context("관리자 신고 상태 변경 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고를 찾을 수 없습니다."))?;

    Ok(to_report_response(row))
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
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
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
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "is_active": is_active,
            "updated_at": Utc::now()
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