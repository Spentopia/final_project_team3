// user/dto.rs
// 클라이언트와 주고받는 유저 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::de::{Deserializer, Visitor};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 프로필 조회 응답 ───────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UserResponse {
    pub id: Uuid,
    pub email: Option<String>,
    pub nickname: Option<String>,
    pub phone: Option<String>,
    pub introduction: Option<String>,
    pub profile_image: Option<String>,
    pub login_provider: Option<String>,
    pub wallet_address: Option<String>,
    pub role_type: String,
    pub profile_completed: bool,
    pub spt_balance: i32,
    pub created_at: DateTime<Utc>,
    pub current_streak: i32,
}

// ── 프로필 수정 요청 ───────────────────────────────────────────
// nickname + phone 모두 입력되면 profile_completed = true로 자동 전환

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateProfileRequest {
    pub nickname: Option<String>,
    pub phone: Option<String>,
    #[serde(default, deserialize_with = "deserialize_nullable_string_field")]
    pub introduction: Option<Option<String>>,
    pub profile_image: Option<String>,
}

fn deserialize_nullable_string_field<'de, D>(
    deserializer: D,
) -> Result<Option<Option<String>>, D::Error>
where
    D: Deserializer<'de>,
{
    struct NullableStringVisitor;

    impl<'de> Visitor<'de> for NullableStringVisitor {
        type Value = Option<Option<String>>;

        fn expecting(&self, formatter: &mut std::fmt::Formatter) -> std::fmt::Result {
            formatter.write_str("a string or null")
        }

        fn visit_none<E>(self) -> Result<Self::Value, E>
        where
            E: serde::de::Error,
        {
            Ok(Some(None))
        }

        fn visit_unit<E>(self) -> Result<Self::Value, E>
        where
            E: serde::de::Error,
        {
            Ok(Some(None))
        }

        fn visit_some<D>(self, deserializer: D) -> Result<Self::Value, D::Error>
        where
            D: Deserializer<'de>,
        {
            let value = String::deserialize(deserializer)?;
            Ok(Some(Some(value)))
        }
    }

    deserializer.deserialize_option(NullableStringVisitor)
}

// ── 알림 설정 조회 응답 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UserSettingsResponse {
    pub alert_budget: Option<bool>,
    pub alert_reward: Option<bool>,
    pub alert_streak: Option<bool>,
    pub notification_listener: Option<bool>,
}

// ── 알림 설정 수정 요청 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateSettingsRequest {
    pub alert_budget: Option<bool>,
    pub alert_reward: Option<bool>,
    pub alert_streak: Option<bool>,
    pub notification_listener: Option<bool>,
}

// ── 비밀번호 변경 요청 ─────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ChangePasswordRequest {
    pub current_password: String,
    pub new_password: String,
}
