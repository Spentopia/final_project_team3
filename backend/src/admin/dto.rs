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
    pub reporter_id: Uuid,

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