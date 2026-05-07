// admin/mod.rs
//
// 관리자 전용 기능을 모아두는 도메인 모듈.
//
// 현재 포함 기능:
// - 컨텐츠 신고 목록 조회
// - 컨텐츠 신고 처리 완료
// - 컨텐츠 신고 반려
//
// route.rs에서 admin_routes에 연결하고,
// admin_middleware + jwt_middleware를 적용해서 관리자만 접근하게 만든다.

pub mod dto;
pub mod handler;
pub mod model;
pub mod service;