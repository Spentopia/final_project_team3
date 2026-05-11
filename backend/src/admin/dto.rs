// admin/dto.rs
//
// 관리자 API에서 사용하는 요청/응답 DTO.
//
// DTO는 HTTP 응답으로 내려줄 형태를 정의한다.
// DB 테이블 구조와 거의 비슷하지만,
// 외부에 내려줄 필드만 명시적으로 관리하기 위해 따로 둔다.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

/// 관리자 신고 목록/처리 응답 DTO
///
/// content_reports 테이블의 신고 데이터를
/// 관리자 페이지에서 표시하기 위한 응답 형태.
///
/// target_type:
/// - post
/// - comment
/// - user_nickname
/// - user_profile
///
/// status:
/// - pending: 처리 대기
/// - resolved: 처리 완료
/// - rejected: 반려
#[derive(Serialize, Deserialize, Debug, ToSchema)]
pub struct AdminContentReportResponse {
    // 신고 ID
    pub id: Uuid,

    // 신고한 사용자 ID
    //
    // 상세 모달이나 운영 추적용으로 필요하다.
    // 목록에서는 이 ID를 메인으로 보여주기보다,
    // reporter_nickname / reporter_email을 우선 보여주는 게 좋다.
    pub reporter_id: Uuid,

    // 신고자 닉네임
    //
    // public.users.nickname에서 가져온 값.
    // 탈퇴 유저이거나 users row가 없으면 null일 수 있다.
    pub reporter_nickname: Option<String>,

    // 신고자 이메일
    //
    // public.users.email에서 가져온 값.
    // 소셜 로그인 정책이나 임시 계정에 따라 null일 수 있다.
    pub reporter_email: Option<String>,

    // 신고 대상 타입
    pub target_type: String,

    /// 신고 대상 ID
    ///
    /// target_type에 따라 의미가 달라진다.
    /// - post: posts.id
    /// - comment: comments.id
    /// - user_nickname: users.id
    /// - user_profile: users.id
    pub target_id: Uuid,

    /// 신고 사유
    ///
    /// - abuse
    /// - inappropriate
    /// - spam
    /// - other
    pub reason: String,

    // 신고자가 작성한 상세 설명
    //
    // 실제 DB 컬럼명은 detail이다.
    // description이 아님.
    pub detail: Option<String>,

    // 신고 처리 상태
    pub status: String,

    // 신고 접수 시각
    pub created_at: Option<DateTime<Utc>>,

    // 관리자 처리 시각
    pub reviewed_at: Option<DateTime<Utc>>,

    // 처리한 관리자 ID
    // 나중에 팀원 관리자 계정이 늘어날 걸 대비
    pub reviewed_by: Option<Uuid>,
}

// ─────────────────────────────────────────────
// 회원 관리 응답 DTO
// ─────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AdminUserResponse {
    pub id: Uuid,
    pub email: Option<String>,
    pub nickname: Option<String>,
    pub phone: Option<String>,
    pub profile_image: Option<String>,
    pub login_provider: Option<String>,
    pub wallet_address: Option<String>,
    pub role_type: String,
    pub profile_completed: bool,

    // 회원 활성 여부.
    //
    // true  = 정상 활성 회원
    // false = 운영자가 비활성 처리한 회원
    //
    // 단, 탈퇴 여부는 is_active만으로 판단하지 않는다.
    // deleted_at이 Some이면 "탈퇴" 상태로 우선 표시한다.
    pub is_active: bool,

    // 탈퇴 시각.
    //
    // deleted_at이 Some이면 관리자 페이지에서는
    // 활성/비활성이 아니라 "탈퇴" 상태로 보여준다.
    pub deleted_at: Option<DateTime<Utc>>,

    pub created_at: Option<DateTime<Utc>>,
    pub updated_at: Option<DateTime<Utc>>,
}

// 회원 활성/비활성 변경 요청
#[derive(Serialize, Deserialize, Debug, ToSchema)]
pub struct UpdateUserActiveRequest {
    pub is_active: bool,
}

// ─────────────────────────────────────────────
// 공지사항 관리 DTO
// ─────────────────────────────────────────────
//
// 공지사항은 posts 테이블을 재사용한다.
// post_type = 'notice'인 row만 관리자 공지사항으로 본다.
//
// posts 주요 매핑:
// - id
// - user_id: 작성 관리자 ID
// - title
// - content
// - post_type = 'notice'
// - view_count
// - is_deleted
// - deleted_at
// - created_at
// - updated_at

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AdminNoticeResponse {
    // 공지 게시글 ID
    pub id: Uuid,

    // 작성 관리자 ID
    pub user_id: Uuid,

    // 공지 제목
    pub title: String,

    // 공지 내용
    pub content: String,

    // 항상 notice여야 한다.
    pub post_type: String,

    // 조회수
    pub view_count: i32,

    // 삭제 여부
    pub is_deleted: bool,

    // 삭제 시각
    pub deleted_at: Option<DateTime<Utc>>,

    // 생성 시각
    pub created_at: Option<DateTime<Utc>>,

    // 수정 시각
    pub updated_at: Option<DateTime<Utc>>,
}

/// 공지사항 생성 요청
///
/// 관리자 화면에서 공지 작성할 때 사용한다.
/// user_id, post_type 등은 프론트에서 받지 않고 백엔드에서 강제로 세팅한다.
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateAdminNoticeRequest {
    pub title: String,
    pub content: String,
}

/// 공지사항 수정 요청
///
/// 둘 다 Option인 이유:
/// - 제목만 수정 가능
/// - 내용만 수정 가능
/// - 둘 다 수정 가능
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateAdminNoticeRequest {
    pub title: Option<String>,
    pub content: Option<String>,
}

// ─────────────────────────────────────────────
// 아바타 콘테스트 관리 DTO
// ─────────────────────────────────────────────
//
// contest_events 테이블을 관리자 화면에서 관리하기 위한 DTO.
//
// 상태값:
// - upcoming: 예정
// - active: 진행중
// - ended: 종료

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AdminContestResponse {
    pub id: Uuid,

    // 콘테스트 제목
    pub title: String,

    // 콘테스트 설명
    pub description: Option<String>,

    // 시작일
    pub start_date: DateTime<Utc>,

    // 종료일
    pub end_date: DateTime<Utc>,

    // 상태
    //
    // upcoming / active / ended
    pub status: String,

    // 보상 설명
    pub reward_description: Option<String>,

    // 생성일
    pub created_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateAdminContestRequest {
    pub title: String,
    pub description: Option<String>,
    pub start_date: DateTime<Utc>,
    pub end_date: DateTime<Utc>,
    pub status: Option<String>,
    pub reward_description: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateAdminContestRequest {
    pub title: Option<String>,
    pub description: Option<String>,
    pub start_date: Option<DateTime<Utc>>,
    pub end_date: Option<DateTime<Utc>>,
    pub status: Option<String>,
    pub reward_description: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UpdateAdminContestStatusRequest {
    pub status: String,
}
