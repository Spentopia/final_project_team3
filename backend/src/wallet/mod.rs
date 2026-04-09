// wallet/mod.rs
//
// wallet 모듈의 진입점
//
// Rust에서 폴더를 모듈로 만들려면 반드시 mod.rs 파일이 있어야 함.
// 여기서 하위 파일들을 pub mod로 선언해야 외부에서 접근 가능.

// 요청/응답 구조체 모음 (외부에서 wallet::dto::... 로 접근 가능)
pub mod dto;
// HTTP 핸들러 모음 (route.rs에서 wallet::handler::... 로 접근 가능)
pub mod handler;

// 비즈니스 로직 모음 (handler.rs에서 service::... 로 접근 가능)
pub mod service;
