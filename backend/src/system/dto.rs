// backend/src/system/dto.rs
//
// 서비스 상태 공지 API 요청/응답 DTO.
//
// 일반 공지사항과 다르게 system_status는 "게시글"이 아니라
// 현재 서비스 상태를 나타내는 전역 설정값이다.

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;

/// 서비스 상태 공지 응답.
///
/// GET /api/system/status
/// PATCH /api/admin/system/status
/// 둘 다 이 형태로 응답한다.
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct SystemStatusResponse {
    /// 배너 노출 여부.
    ///
    /// false면 프론트는 아무 배너도 보여주지 않는다.
    pub enabled: bool,

    /// 차단형 점검 모드 여부.
    ///
    /// 지금 1차 구현에서는 사용하지 않고,
    /// 나중에 /maintenance 페이지 강제 이동을 구현할 때 사용한다.
    pub blocking: bool,

    /// 공지 유형.
    ///
    /// maintenance: 점검
    /// incident: 장애
    pub status_type: String,

    /// 배너 제목.
    pub title: String,

    /// 배너 내용.
    pub message: String,

    /// 점검/장애 시작 시각.
    pub starts_at: Option<DateTime<Utc>>,

    /// 점검/장애 종료 예정 시각.
    pub ends_at: Option<DateTime<Utc>>,

    /// 마지막 수정 시각.
    pub updated_at: DateTime<Utc>,
}

/// 관리자 서비스 상태 수정 요청.
///
/// PATCH /api/admin/system/status
#[derive(Debug, Clone, Serialize, Deserialize, ToSchema)]
pub struct UpdateSystemStatusRequest {
    pub enabled: bool,

    /// 지금은 저장만 해두고 차단 페이지는 2차 확장으로 둔다.
    pub blocking: Option<bool>,

    /// maintenance / incident
    pub status_type: String,

    pub title: String,
    pub message: String,

    pub starts_at: Option<DateTime<Utc>>,
    pub ends_at: Option<DateTime<Utc>>,
}