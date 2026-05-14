use anyhow::{Context, Result, anyhow};
use chrono::NaiveDate;
use uuid::Uuid;

use crate::state::AppState;

#[derive(serde::Deserialize)]
struct UserSettingsRow {
    user_id: Uuid,
}

pub async fn process_daily_streak_reminders(
    state: &AppState,
    record_date: NaiveDate,
) -> Result<usize> {
    let users = list_streak_alert_users(state).await?;
    let mut sent_count = 0;

    for user in users {
        if has_expense_record_on_date(state, user.user_id, record_date).await? {
            continue;
        }

        if let Err(error) = crate::notification::service::notify_streak_reminder_if_needed(
            state,
            user.user_id,
            record_date,
        )
        .await
        {
            tracing::error!(
                "스트릭 리마인드 알림 생성 실패: user_id={}, error={}",
                user.user_id,
                error
            );
            continue;
        }

        sent_count += 1;
    }

    Ok(sent_count)
}

async fn list_streak_alert_users(state: &AppState) -> Result<Vec<UserSettingsRow>> {
    let url = format!(
        "{}/rest/v1/user_settings?select=user_id&or=(alert_streak.is.null,alert_streak.eq.true)",
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
        .context("스트릭 리마인드 대상 user_settings SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("스트릭 리마인드 대상 SELECT 실패: {}", body));
    }

    res.json::<Vec<UserSettingsRow>>()
        .await
        .context("스트릭 리마인드 대상 역직렬화 실패")
}

async fn has_expense_record_on_date(
    state: &AppState,
    user_id: Uuid,
    record_date: NaiveDate,
) -> Result<bool> {
    let url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=eq.{}&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        record_date
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
        .context("스트릭 리마인드용 expenses SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("스트릭 리마인드용 expenses SELECT 실패: {}", body));
    }

    let rows = res
        .json::<Vec<serde_json::Value>>()
        .await
        .context("스트릭 리마인드용 expenses 역직렬화 실패")?;

    Ok(!rows.is_empty())
}
