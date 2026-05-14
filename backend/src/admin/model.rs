// admin/model.rs
//
// 관리자 도메인에서 Supabase REST 응답을 역직렬화할 때 사용하는 모델.
//
// model은 DB row와 최대한 1:1로 맞춘다.
// service.rs에서 이 모델을 받은 뒤 dto.rs의 응답 DTO로 변환한다.

use chrono::{DateTime, Utc};
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
    // 같은 대상이 누적 몇 번 신고되었는지.
    //
    // admin_content_reports_view에서:
    // count(*) over (partition by target_type, target_id)
    // 로 계산해서 내려준다.
    pub target_report_count: Option<i64>,
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
    // 탈퇴 시각.
    //
    // public.users.deleted_at 컬럼을 그대로 받는다.
    // Some이면 관리자 회원관리 화면에서 "탈퇴"로 표시한다.
    pub deleted_at: Option<DateTime<Utc>>,

    // 비활성 사유.
    pub inactive_reason: Option<String>,

    // 비활성 처리 시각.
    pub inactive_at: Option<DateTime<Utc>>,

    // 비활성 해제 예정일.
    pub inactive_until: Option<DateTime<Utc>>,

    // 비활성 처리한 관리자 ID.
    pub inactive_by: Option<Uuid>,

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


// ─────────────────────────────────────────────
// 신고 원본 row 조회용 모델
// ─────────────────────────────────────────────
//
// apply-action에서는 view가 아니라 content_reports 원본 테이블을 조회한다.
//
// 이유:
// - target_type / target_id / status만 있으면 실제 조치를 판단할 수 있음.
// - 신고 처리 전 상태(before_status)를 감사 로그에 남겨야 함.
#[derive(Debug, Deserialize)]
pub struct ContentReportBaseRow {
    pub id: Uuid,
    pub reporter_id: Uuid,
    pub target_type: String,
    pub target_id: Uuid,
    pub status: String,
}

// ─────────────────────────────────────────────
// 관리자 감사 로그 모델
// ─────────────────────────────────────────────
//
// public.admin_audit_logs 테이블 row를 역직렬화하기 위한 모델.
// 이 모델은 Supabase REST API 응답을 받기 위해 사용한다.

#[derive(Debug, serde::Deserialize)]
pub struct AdminAuditLog {
    pub id: Uuid,
    pub admin_id: Uuid,
    pub action: String,
    pub target_type: String,
    pub target_id: Uuid,
    pub before_status: Option<String>,
    pub after_status: Option<String>,
    pub metadata: serde_json::Value,
    pub created_at: Option<DateTime<Utc>>,
}


// ─────────────────────────────────────────────
// 관리자 신고 대상 상세: 게시글 view row
// ─────────────────────────────────────────────
//
// public.admin_post_targets_view 응답을 받기 위한 모델.
//
// target_type = "post" 신고일 때 사용한다.
#[derive(Debug, serde::Deserialize)]
pub struct AdminPostTargetRow {
    pub id: Uuid,
    pub author_id: Uuid,
    pub author_nickname: Option<String>,
    pub author_email: Option<String>,
    pub author_profile_image: Option<String>,

    pub title: Option<String>,
    pub content: Option<String>,
    pub image_url: Option<String>,

    pub is_deleted: Option<bool>,
    pub deleted_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

// ─────────────────────────────────────────────
// 관리자 신고 대상 상세: 댓글 view row
// ─────────────────────────────────────────────
//
// public.admin_comment_targets_view 응답을 받기 위한 모델.
//
// target_type = "comment" 신고일 때 사용한다.
#[derive(Debug, serde::Deserialize)]
pub struct AdminCommentTargetRow {
    pub id: Uuid,
    pub post_id: Uuid,
    pub parent_id: Option<Uuid>,

    pub author_id: Uuid,
    pub author_nickname: Option<String>,
    pub author_email: Option<String>,
    pub author_profile_image: Option<String>,

    pub content: Option<String>,

    pub is_deleted: Option<bool>,
    pub deleted_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

// ─────────────────────────────────────────────
// 관리자 신고 대상 상세: 사용자 row
// ─────────────────────────────────────────────
//
// user_profile / user_nickname 신고일 때 users 테이블에서 직접 조회한다.
#[derive(Debug, serde::Deserialize)]
pub struct AdminUserTargetRow {
    pub id: Uuid,
    pub email: Option<String>,
    pub nickname: Option<String>,
    pub profile_image: Option<String>,
    pub is_active: Option<bool>,
    pub deleted_at: Option<DateTime<Utc>>,
    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

