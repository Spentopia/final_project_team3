// reward/service.rs
// 보상, 스트릭, 주간 성실도 비즈니스 로직

use anyhow::{anyhow, Context, Result};
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{RewardResponse, StreakResponse, WeeklyScoreResponse},
    model::{Reward, Streak, WeeklyScore},
};

// ── 보상 이력 조회 ─────────────────────────────────────────────

pub async fn list_rewards(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<RewardResponse>> {
    let url = format!(
        "{}/rest/v1/rewards?user_id=eq.{}&select=*&order=earned_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("rewards SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("rewards SELECT 실패: {}", body));
    }

    let rewards: Vec<Reward> = res.json().await.context("rewards 역직렬화 실패")?;
    Ok(rewards.into_iter().map(|r| RewardResponse {
        id: r.id,
        reward_type: r.reward_type,
        amount: r.amount,
        description: r.description,
        earned_at: r.earned_at,
    }).collect())
}

// ── 스트릭 조회 ───────────────────────────────────────────────

pub async fn get_streak(
    state: &AppState,
    user_id: Uuid,
) -> Result<StreakResponse> {
    let url = format!(
        "{}/rest/v1/streaks?user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("streaks SELECT 실패: {}", body));
    }

    let streaks: Vec<Streak> = res.json().await.context("streaks 역직렬화 실패")?;
    let streak = streaks.into_iter().next().unwrap_or(Streak {
        id: Uuid::nil(),
        user_id,
        current_streak: Some(0),
        longest_streak: Some(0),
        last_record_date: None,
        updated_at: None,
    });

    Ok(StreakResponse {
        current_streak: streak.current_streak,
        longest_streak: streak.longest_streak,
        last_record_date: streak.last_record_date,
    })
}

// ── 주간 성실도 점수 조회 ──────────────────────────────────────

pub async fn get_weekly_scores(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<WeeklyScoreResponse>> {
    let url = format!(
        "{}/rest/v1/weekly_scores?user_id=eq.{}&select=*&order=week_start.desc&limit=12",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("weekly_scores SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("weekly_scores SELECT 실패: {}", body));
    }

    let scores: Vec<WeeklyScore> = res.json().await.context("weekly_scores 역직렬화 실패")?;
    Ok(scores.into_iter().map(|s| WeeklyScoreResponse {
        id: s.id,
        week_start: s.week_start,
        record_days_score: s.record_days_score,
        receipt_score: s.receipt_score,
        diary_score: s.diary_score,
        budget_check_score: s.budget_check_score,
        streak_score: s.streak_score,
        total_score: s.total_score,
        reward_granted: s.reward_granted,
    }).collect())
}
