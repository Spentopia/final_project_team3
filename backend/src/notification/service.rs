use anyhow::{Context, Result, anyhow};
use serde_json::json;
use uuid::Uuid;

use crate::state::AppState;

use super::{dto::NotificationResponse, model::Notification};

fn to_response(row: Notification) -> NotificationResponse {
    NotificationResponse {
        id: row.id,
        user_id: row.user_id,
        notification_type: row.notification_type,
        message: row.message,
        is_read: row.is_read.unwrap_or(false),
        created_at: row.created_at,
    }
}

/// 내 알림 목록 조회
pub async fn list_my_notifications(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<NotificationResponse>> {
    let url = format!(
        "{}/rest/v1/notifications?user_id=eq.{}&select=*&order=created_at.desc&limit=50",
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
        .context("notifications SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications SELECT 실패: {}", body));
    }

    let rows: Vec<Notification> = res
        .json()
        .await
        .context("notifications SELECT 역직렬화 실패")?;

    Ok(rows.into_iter().map(to_response).collect())
}

/// 알림 1개 읽음 처리
pub async fn mark_notifications_read(
    state: &AppState,
    user_id: Uuid,
    notification_ids: Vec<Uuid>,
) -> Result<()> {
    if notification_ids.is_empty() {
        return Err(anyhow!("읽음 처리할 알림이 없습니다."));
    }

    let ids = notification_ids
        .iter()
        .map(|id| id.to_string())
        .collect::<Vec<_>>()
        .join(",");

    let url = format!(
        "{}/rest/v1/notifications?id=in.({})&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        ids,
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
        .header("Prefer", "return=minimal")
        .json(&json!({
            "is_read": true
        }))
        .send()
        .await
        .context("notifications 읽음 처리 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications 읽음 처리 실패: {}", body));
    }

    Ok(())
}

/// 내 알림 전체 읽음 처리
pub async fn mark_all_notifications_read(
    state: &AppState,
    user_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/notifications?user_id=eq.{}&is_read=eq.false",
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
        .header("Prefer", "return=minimal")
        .json(&json!({
            "is_read": true
        }))
        .send()
        .await
        .context("notifications 전체 읽음 UPDATE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications 전체 읽음 UPDATE 실패: {}", body));
    }

    Ok(())
}

/// 백엔드 내부용 알림 생성 함수
///
/// 프론트에서 직접 insert하지 않고,
/// 댓글/좋아요/보상 같은 서버 이벤트 발생 시 백엔드가 service_role로 insert한다.
pub async fn create_notification(
    state: &AppState,
    user_id: Uuid,
    notification_type: &str,
    message: &str,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/notifications",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=minimal")
        .json(&json!({
            "user_id": user_id,
            "notification_type": notification_type,
            "message": message,
            "is_read": false
        }))
        .send()
        .await
        .context("notifications INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications INSERT 실패: {}", body));
    }

    Ok(())
}