// notification/service.rs
// 알림 비즈니스 로직

use anyhow::{anyhow, Context, Result};
use serde::Serialize;
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{MarkReadRequest, NotificationResponse},
    model::Notification,
};

// ── 알림 목록 조회 ─────────────────────────────────────────────

pub async fn list_notifications(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<NotificationResponse>> {
    let url = format!(
        "{}/rest/v1/notifications?user_id=eq.{}&select=*&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("notifications SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications SELECT 실패: {}", body));
    }

    let notifications: Vec<Notification> = res.json().await.context("notifications 역직렬화 실패")?;
    Ok(notifications.into_iter().map(|n| NotificationResponse {
        id: n.id,
        notification_type: n.notification_type,
        message: n.message,
        is_read: n.is_read,
        created_at: n.created_at,
    }).collect())
}

// ── 알림 읽음 처리 ─────────────────────────────────────────────
// 요청에 포함된 notification_id 목록을 일괄 읽음 처리

pub async fn mark_read(
    state: &AppState,
    user_id: Uuid,
    req: MarkReadRequest,
) -> Result<()> {
    if req.notification_ids.is_empty() {
        return Ok(());
    }

    // PostgREST in 필터: id=in.(uuid1,uuid2,...)
    let ids: Vec<String> = req.notification_ids.iter().map(|id| id.to_string()).collect();
    let ids_param = format!("({})", ids.join(","));

    let url = format!(
        "{}/rest/v1/notifications?id=in.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        ids_param, user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload { is_read: bool }

    let res = state.http_client.patch(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&PatchPayload { is_read: true })
        .send().await.context("notifications PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications PATCH 실패: {}", body));
    }
    Ok(())
}
