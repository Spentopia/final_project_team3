// user/model.rs
// public.users, public.user_settings 테이블에 대응하는 DB 엔티티 구조체

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.users 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct User {
    pub id: Uuid,
    pub email: Option<String>,
    pub nickname: Option<String>,
    pub phone: Option<String>,
    pub profile_image: Option<String>,
    pub login_provider: Option<String>,
    pub wallet_address: Option<String>,
    pub wallet_connected_at: Option<DateTime<Utc>>,
    pub role_type: String,
    pub profile_completed: bool,
    pub spt_balance: i32,
    pub created_at: DateTime<Utc>,
    pub updated_at: DateTime<Utc>,
    pub is_active: bool,
}

// public.user_settings 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserSettings {
    pub id: Uuid,
    pub user_id: Uuid,
    pub alert_budget: Option<bool>,
    pub alert_reward: Option<bool>,
    pub alert_streak: Option<bool>,
    pub notification_listener: Option<bool>,
    pub updated_at: Option<DateTime<Utc>>,
}
