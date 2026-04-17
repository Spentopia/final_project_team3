// user/service.rs
// 유저 프로필, 설정 비즈니스 로직

use anyhow::{Context, Result, anyhow};
use serde::Serialize;
use uuid::Uuid;

use super::{
    dto::{UpdateProfileRequest, UpdateSettingsRequest, UserResponse, UserSettingsResponse},
    model::{User, UserSettings},
};
use crate::state::AppState;

// ── 프로필 조회 ───────────────────────────────────────────────

pub async fn get_profile(state: &AppState, user_id: Uuid) -> Result<UserResponse> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("users SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("users SELECT 실패: {}", body));
    }

    let users: Vec<User> = res.json().await.context("users 역직렬화 실패")?;
    let user = users
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("유저를 찾을 수 없음: {}", user_id))?;

    Ok(to_response(user))
}

// ── 프로필 수정 ───────────────────────────────────────────────

pub async fn update_profile(
    state: &AppState,
    user_id: Uuid,
    req: UpdateProfileRequest,
) -> Result<UserResponse> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    // nickname + phone 모두 있으면 profile_completed = true
    #[derive(Serialize)]
    struct PatchPayload {
        #[serde(skip_serializing_if = "Option::is_none")]
        nickname: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        phone: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        profile_image: Option<String>,
        profile_completed: bool,
    }

    let profile_completed = req.nickname.is_some() && req.phone.is_some();

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload {
            nickname: req.nickname,
            phone: req.phone,
            profile_image: req.profile_image,
            profile_completed,
        })
        .send()
        .await
        .context("users PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("users PATCH 실패: {}", body));
    }

    let updated: Vec<User> = res.json().await.context("users PATCH 역직렬화 실패")?;
    let user = updated
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("수정된 유저 정보를 찾을 수 없음"))?;

    Ok(to_response(user))
}

// ── 알림 설정 조회 ─────────────────────────────────────────────

pub async fn get_settings(state: &AppState, user_id: Uuid) -> Result<UserSettingsResponse> {
    let url = format!(
        "{}/rest/v1/user_settings?user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("user_settings SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_settings SELECT 실패: {}", body));
    }

    let settings: Vec<UserSettings> = res.json().await.context("user_settings 역직렬화 실패")?;
    let s = settings.into_iter().next().unwrap_or(UserSettings {
        id: Uuid::nil(),
        user_id,
        alert_budget: Some(true),
        alert_reward: Some(true),
        alert_streak: Some(true),
        notification_listener: Some(true),
        updated_at: None,
    });

    Ok(UserSettingsResponse {
        alert_budget: s.alert_budget,
        alert_reward: s.alert_reward,
        alert_streak: s.alert_streak,
        notification_listener: s.notification_listener,
    })
}

// ── 알림 설정 수정 ─────────────────────────────────────────────

pub async fn update_settings(
    state: &AppState,
    user_id: Uuid,
    req: UpdateSettingsRequest,
) -> Result<UserSettingsResponse> {
    let url = format!(
        "{}/rest/v1/user_settings?user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&req)
        .send()
        .await
        .context("user_settings PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_settings PATCH 실패: {}", body));
    }

    let updated: Vec<UserSettings> = res
        .json()
        .await
        .context("user_settings PATCH 역직렬화 실패")?;
    let s = updated
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("수정된 설정을 찾을 수 없음"))?;

    Ok(UserSettingsResponse {
        alert_budget: s.alert_budget,
        alert_reward: s.alert_reward,
        alert_streak: s.alert_streak,
        notification_listener: s.notification_listener,
    })
}

fn to_response(u: User) -> UserResponse {
    UserResponse {
        id: u.id,
        email: u.email,
        nickname: u.nickname,
        phone: u.phone,
        profile_image: u.profile_image,
        login_provider: u.login_provider,
        wallet_address: u.wallet_address,
        role_type: u.role_type,
        profile_completed: u.profile_completed,
        spt_balance: u.spt_balance,
        created_at: u.created_at,
    }
}
