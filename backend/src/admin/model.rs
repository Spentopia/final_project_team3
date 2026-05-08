// admin/model.rs
//
// 관리자 도메인에서 Supabase REST 응답을 역직렬화할 때 사용하는 모델.
//
// model은 DB row와 최대한 1:1로 맞춘다.
// service.rs에서 이 모델을 받은 뒤 dto.rs의 응답 DTO로 변환한다.

use chrono:: {DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// public.content_reports 테이블 row 모델
///
/// Supabase PostgREST 응답을 이 구조체로 받는다.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AdminContentReport {
    pub id: Uuid,
    pub reporter_id: Uuid,
    pub target_type: String,
    pub target_id: Uuid,
    pub reason: String,
    pub detail: Option<String>,
    pub status: String,
    pub created_at: Option<DateTime<Utc>>,
    pub reviewed_at: Option<DateTime<Utc>>,
    pub reviewed_by: Option<Uuid>,
}

// public.users row
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AdminUser {
    pub id: Uuid,
    pub email: Option<String>,
    pub nickname: Option<String>,
    pub phone: Option<String>,
    pub profile_image: Option<String>,
    pub login_provider: Option<String>,
    pub wallet_address: Option<String>,
    pub role_type: Option<String>,
    pub profile_completed: Option<bool>,
    pub is_active: Option<bool>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}