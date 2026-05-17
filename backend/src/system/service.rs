// backend/src/system/service.rs
//
// 서비스 상태 공지 비즈니스 로직.
//
// 주의:
// - 이 서비스는 Supabase REST API를 service_role key로 호출한다.
// - public 조회 API는 비회원도 접근 가능하다.
// - admin 수정 API는 route.rs의 admin_routes 안에서
//   jwt_middleware + admin_middleware를 통과한 관리자만 접근하게 해야 한다.

use anyhow::{Context, Result, anyhow};
use chrono::Utc;
use serde_json::json;
use uuid::Uuid;

use crate::state::AppState;

use super::{
    dto::{SystemStatusResponse, UpdateSystemStatusRequest},
    model::SystemStatusRow,
};

/// DB row를 프론트 응답 DTO로 변환한다.
fn to_response(row: SystemStatusRow) -> SystemStatusResponse {
    SystemStatusResponse {
        enabled: row.enabled,
        blocking: row.blocking,
        status_type: row.status_type,
        title: row.title,
        message: row.message,
        starts_at: row.starts_at,
        ends_at: row.ends_at,
        updated_at: row.updated_at,
    }
}

/// status_type 유효성 검사.
///
/// 현재 허용:
/// - maintenance: 점검
/// - incident: 장애
fn validate_status_type(status_type: &str) -> Result<()> {
    match status_type {
        "maintenance" | "incident" => Ok(()),
        _ => Err(anyhow!("지원하지 않는 서비스 상태 유형입니다.")),
    }
}

/// 관리자 입력값 검증.
///
/// enabled=true일 때는 최소한 제목/내용이 있어야
/// 사용자에게 의미 있는 배너를 보여줄 수 있다.
fn validate_update_request(req: &UpdateSystemStatusRequest) -> Result<()> {
    validate_status_type(req.status_type.trim())?;

    if req.enabled {
        if req.title.trim().is_empty() {
            return Err(anyhow!("서비스 상태 공지 제목을 입력해 주세요."));
        }

        if req.message.trim().is_empty() {
            return Err(anyhow!("서비스 상태 공지 내용을 입력해 주세요."));
        }
    }

    if let (Some(starts_at), Some(ends_at)) = (req.starts_at, req.ends_at) {
        if ends_at <= starts_at {
            return Err(anyhow!("종료 시각은 시작 시각 이후여야 합니다."));
        }
    }

    Ok(())
}

/// 공개: 현재 서비스 상태 조회.
///
/// API:
/// GET /api/system/status
///
/// 비회원도 호출 가능하다.
///
/// row가 없으면 안전하게 disabled 상태를 반환한다.
/// 단, SQL에서 기본 row를 insert했으면 보통 이 fallback은 타지 않는다.
pub async fn get_system_status(state: &AppState) -> Result<SystemStatusResponse> {
    let url = format!(
        "{}/rest/v1/system_status?id=eq.global&select=*&limit=1",
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
        .context("서비스 상태 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("서비스 상태 조회 실패: {}", body));
    }

    let rows: Vec<SystemStatusRow> = res
        .json()
        .await
        .context("서비스 상태 조회 응답 파싱 실패")?;

    if let Some(row) = rows.into_iter().next() {
        return Ok(to_response(row));
    }

    // fallback.
    Ok(SystemStatusResponse {
        enabled: false,
        blocking: false,
        status_type: "maintenance".to_string(),
        title: "".to_string(),
        message: "".to_string(),
        starts_at: None,
        ends_at: None,
        updated_at: Utc::now(),
    })
}

/// 관리자: 서비스 상태 수정.
///
/// API:
/// PATCH /api/admin/system/status
///
/// 관리자 페이지에서 점검/장애 공지 ON/OFF와 내용을 수정한다.
pub async fn update_system_status(
    state: &AppState,
    admin_id: Uuid,
    req: UpdateSystemStatusRequest,
) -> Result<SystemStatusResponse> {
    validate_update_request(&req)?;

    let url = format!(
        "{}/rest/v1/system_status?id=eq.global",
        state.config.supabase_url.trim_end_matches('/')
    );

    let payload = json!({
        "enabled": req.enabled,
        "blocking": req.blocking.unwrap_or(false),
        "status_type": req.status_type.trim(),
        "title": req.title.trim(),
        "message": req.message.trim(),
        "starts_at": req.starts_at,
        "ends_at": req.ends_at,
        "updated_at": Utc::now().to_rfc3339(),
        "updated_by": admin_id,
    });

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
        .json(&payload)
        .send()
        .await
        .context("서비스 상태 수정 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("서비스 상태 수정 실패: {}", body));
    }

    let rows: Vec<SystemStatusRow> = res
        .json()
        .await
        .context("서비스 상태 수정 응답 파싱 실패")?;

    let row = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("서비스 상태 수정 결과를 찾을 수 없습니다."))?;

    Ok(to_response(row))
}