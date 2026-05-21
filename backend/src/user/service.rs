// user/service.rs
// 유저 프로필, 설정 비즈니스 로직
// 변경사항:
// - update_profile: nickname 변경 시 validate_nickname() 으로
//   길이(2~10자) + 금칙어 한번에 검증

use anyhow::{Context, Result, anyhow};
use serde::Serialize;
use uuid::Uuid;

use super::{
    dto::{UpdateProfileRequest, UpdateSettingsRequest, UserResponse, UserSettingsResponse},
    model::{User, UserSettings},
};
use crate::filter;
use crate::state::AppState;

// ── 프로필 조회 ───────────────────────────────────────────────

pub async fn get_profile(state: &AppState, user_id: Uuid) -> Result<UserResponse> {
    // users + streaks 병렬 조회
    let (user, current_streak) = tokio::try_join!(
        get_user_by_id(state, user_id),
        get_current_streak(state, user_id),
    )?;
    Ok(to_response(user, current_streak))
}

async fn get_current_streak(state: &AppState, user_id: Uuid) -> Result<i32> {
    let url = format!(
        "{}/rest/v1/streaks?user_id=eq.{}&select=current_streak&limit=1",
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
        .context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        // streak 조회 실패는 치명적이지 않으므로 0 반환
        return Ok(0);
    }

    #[derive(serde::Deserialize)]
    struct StreakRow {
        current_streak: i32,
    }

    let rows: Vec<StreakRow> = res.json().await.context("streaks 역직렬화 실패")?;

    Ok(rows
        .into_iter()
        .next()
        .map(|s| s.current_streak)
        .unwrap_or(0))
}

// ── 프로필 수정 ───────────────────────────────────────────────

pub async fn update_profile(
    state: &AppState,
    user_id: Uuid,
    req: UpdateProfileRequest,
) -> Result<UserResponse> {
    let current_user = get_user_by_id(state, user_id).await?;

    // 닉네임 검증
    if let Some(ref nickname) = req.nickname {
        filter::validate_nickname(nickname).map_err(|msg| anyhow!(msg))?;
    }

    if let Some(Some(ref introduction)) = req.introduction {
        if !filter::check(introduction) {
            return Err(anyhow!(
                "한 줄 소개에 사용할 수 없는 표현이 포함되어 있습니다."
            ));
        }
    }

    // 전화번호 정책:
    // - 최초 프로필 완성 단계에서 current_user.phone이 비어 있을 때만 등록 허용
    // - 이미 등록된 전화번호는 마이페이지/API 직접 호출 모두에서 변경 불가
    // - req.phone이 None이면 "전화번호를 수정하지 않겠다"는 의미로 보고 기존 값을 유지
    if let Some(ref requested_phone) = req.phone {
        if current_user.phone.is_some()
            && Some(requested_phone.as_str()) != current_user.phone.as_deref()
        {
            return Err(anyhow!("전화번호는 변경할 수 없습니다"));
        }
    }

    // 이메일은 Supabase Auth의 로그인 식별 정보이므로 이 프로필 수정 API에서 변경하지 않는다.
    // 전화번호는 최초 프로필 완성 시에만 등록하고, 이미 등록된 이후에는 변경을 허용하지 않는다.
    // 마이페이지에서는 닉네임, 한 줄 소개, 프로필 이미지 등 서비스 프로필 정보만 수정한다.
    let url = format!(
        "{}/rest/v1/users?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    #[derive(Serialize)]
    struct PatchPayload {
        #[serde(skip_serializing_if = "Option::is_none")]
        nickname: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        phone: Option<String>,
        #[serde(skip_serializing_if = "Option::is_none")]
        introduction: Option<Option<String>>,
        #[serde(skip_serializing_if = "Option::is_none")]
        profile_image: Option<String>,
        profile_completed: bool,
    }

    let final_nickname = req.nickname.clone().or(current_user.nickname.clone());
    let final_phone = req.phone.clone().or(current_user.phone.clone());
    let profile_completed = final_nickname.is_some() && final_phone.is_some();

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
            introduction: req.introduction,
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

    Ok(to_response(user, 0))
}

async fn get_user_by_id(state: &AppState, user_id: Uuid) -> Result<User> {
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
    users
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("유저를 찾을 수 없음: {}", user_id))
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
        alert_social: Some(true),
        notification_listener: Some(true),
        updated_at: None,
    });

    Ok(UserSettingsResponse {
        alert_budget: s.alert_budget,
        alert_reward: s.alert_reward,
        alert_streak: s.alert_streak,
        alert_social: s.alert_social,
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
        "{}/rest/v1/user_settings?on_conflict=user_id",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", "application/json")
        .header(
            "Prefer",
            "resolution=merge-duplicates,return=representation",
        )
        .json(&serde_json::json!([{
            "user_id": user_id,
            "alert_budget": req.alert_budget,
            "alert_reward": req.alert_reward,
            "alert_streak": req.alert_streak,
            "alert_social": req.alert_social,
            "notification_listener": req.notification_listener,
        }]))
        .send()
        .await
        .context("user_settings UPSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_settings UPSERT 실패: {}", body));
    }

    let updated: Vec<UserSettings> = res
        .json()
        .await
        .context("user_settings UPSERT 역직렬화 실패")?;
    let s = updated
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("수정된 설정을 찾을 수 없음"))?;

    Ok(UserSettingsResponse {
        alert_budget: s.alert_budget,
        alert_reward: s.alert_reward,
        alert_streak: s.alert_streak,
        alert_social: s.alert_social,
        notification_listener: s.notification_listener,
    })
}

// ── 비밀번호 변경 ──────────────────────────────────────────────

pub async fn change_password(
    state: &AppState,
    user_id: Uuid,
    current_password: &str,
    new_password: &str,
) -> Result<()> {
    // 1) 유저 조회 (email, login_provider 확인)
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=email,login_provider&limit=1",
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
        .context("유저 조회 요청 실패")?;

    #[derive(serde::Deserialize)]
    struct UserRow {
        email: Option<String>,
        login_provider: Option<String>,
    }

    let rows: Vec<UserRow> = res.json().await.context("유저 조회 역직렬화 실패")?;
    let user = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("유저를 찾을 수 없음"))?;

    // 2) 이메일 로그인 유저만 허용
    if user.login_provider.as_deref() != Some("email") {
        return Err(anyhow!("소셜 로그인 계정은 비밀번호를 변경할 수 없습니다"));
    }

    let email = user
        .email
        .ok_or_else(|| anyhow!("이메일 정보가 없습니다"))?;

    // 3) 현재 비밀번호 검증 (Supabase 로그인 시도로 확인)
    let token_url = format!(
        "{}/auth/v1/token?grant_type=password",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let verify_res = state
        .http_client
        .post(&token_url)
        .header("apikey", &state.config.supabase_publishable_key)
        .json(&serde_json::json!({
            "email": email,
            "password": current_password,
        }))
        .send()
        .await
        .context("현재 비밀번호 검증 요청 실패")?;

    if !verify_res.status().is_success() {
        return Err(anyhow!("현재 비밀번호가 올바르지 않습니다"));
    }

    // 4) 새 비밀번호 유효성 검사
    validate_password(new_password)?;

    // 5) Admin API로 비밀번호 변경
    let auth_url = format!(
        "{}/auth/v1/admin/users/{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let update_res = state
        .http_client
        .put(&auth_url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .json(&serde_json::json!({ "password": new_password }))
        .send()
        .await
        .context("비밀번호 변경 요청 실패")?;

    if !update_res.status().is_success() {
        let body = update_res.text().await.unwrap_or_default();
        return Err(anyhow!("비밀번호 변경 실패: {}", body));
    }

    Ok(())
}

fn validate_password(password: &str) -> Result<()> {
    if password.len() < 8 {
        return Err(anyhow!("비밀번호는 8자 이상이어야 합니다"));
    }
    if !password.chars().any(|c| c.is_uppercase()) {
        return Err(anyhow!("비밀번호에 영문 대문자를 포함해야 합니다"));
    }
    if !password.chars().any(|c| c.is_lowercase()) {
        return Err(anyhow!("비밀번호에 영문 소문자를 포함해야 합니다"));
    }
    if !password.chars().any(|c| c.is_ascii_digit()) {
        return Err(anyhow!("비밀번호에 숫자를 포함해야 합니다"));
    }
    if !password.chars().any(|c| !c.is_alphanumeric()) {
        return Err(anyhow!("비밀번호에 특수문자를 포함해야 합니다"));
    }
    Ok(())
}

fn to_response(u: User, current_streak: i32) -> UserResponse {
    UserResponse {
        id: u.id,
        email: u.email,
        nickname: u.nickname,
        phone: u.phone,
        introduction: u.introduction,
        profile_image: u.profile_image,
        login_provider: u.login_provider,
        google_connected: u.google_connected.unwrap_or(false),
        wallet_address: u.wallet_address,
        role_type: u.role_type,
        profile_completed: u.profile_completed,
        spt_balance: u.spt_balance,
        created_at: u.created_at,
        current_streak,
    }
}
