// admin/model.rs
//
// 관리자 도메인에서 Supabase REST 응답을 역직렬화할 때 사용하는 모델.
//
// model은 DB row와 최대한 1:1로 맞춘다.
// service.rs에서 이 모델을 받은 뒤 dto.rs의 응답 DTO로 변환한다.

use chrono:: {DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

/// 관리자 신고 조회 row 모델
///
/// 조회 대상:
/// - public.admin_content_reports_view
///
/// 이 view는 content_reports 기반이지만,
/// users와 join해서 reporter_nickname, reporter_email을 추가로 포함한다.
///
/// 신고 처리 PATCH 직후에는 content_reports 테이블만 업데이트하지만,
/// 최종 응답은 다시 admin_content_reports_view에서 조회해서 반환한다.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AdminContentReport {
    pub id: Uuid,
    pub reporter_id: Uuid,
    // view에서 추가로 내려주는 신고자 표시 정보
    pub reporter_nickname: Option<String>,
    pub reporter_email: Option<String>,
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

/// 관리자 공지사항 row 모델
///
/// 조회 대상:
/// - public.posts
///
/// 조건:
/// - post_type = 'notice'
/// - is_deleted = false인 것만 목록에 보여준다.
///
/// 공지사항은 일반 게시글과 같은 posts 테이블을 사용하지만,
/// 관리자 API에서는 notice만 다룬다.
#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AdminNotice {
    pub id: Uuid,
    pub user_id: Uuid,
    pub title: Option<String>,
    pub content: Option<String>,
    pub post_type: String,
    pub view_count: Option<i32>,
    pub is_deleted: Option<bool>,
    pub deleted_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

// ─────────────────────────────────────────────
// 관리자 아바타 콘테스트 모델
// ─────────────────────────────────────────────
//
// 조회 대상:
// - public.contest_events
//
// 관리자 페이지에서 콘테스트 목록/생성/수정/상태 변경에 사용한다.

#[derive(Serialize, Deserialize, Debug, Clone)]
pub struct AdminContest {
    pub id: Uuid,
    pub title: String,
    pub description: Option<String>,
    pub start_date: DateTime<Utc>,
    pub end_date: DateTime<Utc>,
    pub status: Option<String>,
    pub reward_description: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}