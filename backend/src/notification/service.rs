use anyhow::{Context, Result, anyhow};
use chrono::{Datelike, NaiveDate};
use serde_json::json;
use uuid::Uuid;

use crate::state::AppState;

use super::{dto::NotificationResponse, model::Notification};

#[derive(serde::Deserialize)]
struct UserSettingsRow {
    alert_budget: Option<bool>,
    alert_reward: Option<bool>,
    alert_streak: Option<bool>,
    notification_listener: Option<bool>,
}

#[derive(serde::Deserialize)]
struct BudgetRow {
    total_budget: i32,
}

#[derive(serde::Deserialize)]
struct ExpenseAmountRow {
    amount: i32,
}

#[derive(Clone, Copy)]
enum NotificationPreference {
    Budget,
    Reward,
    Streak,
}

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
pub async fn mark_all_notifications_read(state: &AppState, user_id: Uuid) -> Result<()> {
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

async fn get_notification_settings(
    state: &AppState,
    user_id: Uuid,
) -> Result<Option<UserSettingsRow>> {
    let url = format!(
        "{}/rest/v1/user_settings?user_id=eq.{}&select=alert_budget,alert_reward,alert_streak,notification_listener&limit=1",
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
        .context("user_settings SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_settings SELECT 실패: {}", body));
    }

    let rows: Vec<UserSettingsRow> = res
        .json()
        .await
        .context("user_settings SELECT 역직렬화 실패")?;

    Ok(rows.into_iter().next())
}

async fn is_notification_enabled(
    state: &AppState,
    user_id: Uuid,
    preference: NotificationPreference,
) -> Result<bool> {
    let settings = get_notification_settings(state, user_id).await?;

    let Some(settings) = settings else {
        return Ok(true);
    };

    if settings.notification_listener == Some(false) {
        return Ok(false);
    }

    let enabled = match preference {
        NotificationPreference::Budget => settings.alert_budget,
        NotificationPreference::Reward => settings.alert_reward,
        NotificationPreference::Streak => settings.alert_streak,
    };

    Ok(enabled.unwrap_or(true))
}

async fn notification_exists(
    state: &AppState,
    user_id: Uuid,
    notification_type: &str,
) -> Result<bool> {
    let url = format!(
        "{}/rest/v1/notifications?user_id=eq.{}&notification_type=eq.{}&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        notification_type
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
        .context("notifications 중복 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("notifications 중복 조회 실패: {}", body));
    }

    let rows: Vec<serde_json::Value> = res
        .json()
        .await
        .context("notifications 중복 조회 역직렬화 실패")?;

    Ok(!rows.is_empty())
}

pub async fn notify_budget_threshold_if_needed(
    state: &AppState,
    user_id: Uuid,
    record_date: NaiveDate,
) -> Result<()> {
    if !is_notification_enabled(state, user_id, NotificationPreference::Budget).await? {
        return Ok(());
    }

    let year = record_date.year();
    let month = record_date.month() as i32;
    let base_url = state.config.supabase_url.trim_end_matches('/');

    let budget_url = format!(
        "{}/rest/v1/budgets?user_id=eq.{}&year=eq.{}&month=eq.{}&select=total_budget&limit=1",
        base_url, user_id, year, month
    );
    let budget_res = state
        .http_client
        .get(&budget_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("예산 알림용 budgets SELECT 요청 실패")?;

    if !budget_res.status().is_success() {
        let body = budget_res.text().await.unwrap_or_default();
        return Err(anyhow!("예산 알림용 budgets SELECT 실패: {}", body));
    }

    let budgets: Vec<BudgetRow> = budget_res
        .json()
        .await
        .context("예산 알림용 budgets 역직렬화 실패")?;
    let Some(budget) = budgets.into_iter().next() else {
        return Ok(());
    };

    if budget.total_budget <= 0 {
        return Ok(());
    }

    let month_start = record_date.with_day(1).expect("valid month start");
    let next_month_start = if month == 12 {
        NaiveDate::from_ymd_opt(year + 1, 1, 1).expect("valid next month")
    } else {
        NaiveDate::from_ymd_opt(year, (month + 1) as u32, 1).expect("valid next month")
    };

    let expense_url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=gte.{}&expense_date=lt.{}&select=amount",
        base_url, user_id, month_start, next_month_start
    );
    let expense_res = state
        .http_client
        .get(&expense_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("예산 알림용 expenses SELECT 요청 실패")?;

    if !expense_res.status().is_success() {
        let body = expense_res.text().await.unwrap_or_default();
        return Err(anyhow!("예산 알림용 expenses SELECT 실패: {}", body));
    }

    let expenses: Vec<ExpenseAmountRow> = expense_res
        .json()
        .await
        .context("예산 알림용 expenses 역직렬화 실패")?;
    let total_spent: i32 = expenses.into_iter().map(|row| row.amount).sum();

    if total_spent * 100 < budget.total_budget * 80 {
        return Ok(());
    }

    let notification_type = format!("budget_alert_{}_{:02}", year, month);
    if notification_exists(state, user_id, &notification_type).await? {
        return Ok(());
    }

    create_notification(
        state,
        user_id,
        &notification_type,
        "예산의 80%를 사용했어요!",
    )
    .await
}

pub async fn notify_streak_if_needed(
    state: &AppState,
    user_id: Uuid,
    streak_days: i32,
    record_date: NaiveDate,
) -> Result<()> {
    if streak_days <= 0 || streak_days % 7 != 0 {
        return Ok(());
    }

    if !is_notification_enabled(state, user_id, NotificationPreference::Streak).await? {
        return Ok(());
    }

    let notification_type = format!(
        "streak_{}_{}_{}_{}",
        streak_days,
        record_date.year(),
        record_date.month(),
        record_date.day()
    );
    if notification_exists(state, user_id, &notification_type).await? {
        return Ok(());
    }

    create_notification(
        state,
        user_id,
        &notification_type,
        &format!("{}일 연속 기록을 달성했어요!", streak_days),
    )
    .await
}

pub async fn notify_reward_granted_if_needed(
    state: &AppState,
    user_id: Uuid,
    week_start: NaiveDate,
    message: &str,
) -> Result<()> {
    if !is_notification_enabled(state, user_id, NotificationPreference::Reward).await? {
        return Ok(());
    }

    let notification_type = format!(
        "reward_weekly_{}_{}_{}",
        week_start.year(),
        week_start.month(),
        week_start.day()
    );
    if notification_exists(state, user_id, &notification_type).await? {
        return Ok(());
    }

    create_notification(state, user_id, &notification_type, message).await
}
