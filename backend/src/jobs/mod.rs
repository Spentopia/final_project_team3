// ─────────────────────────────────────────────────────────────
// 백그라운드 배치 작업 모듈
// ─────────────────────────────────────────────────────────────
//
// 회원탈퇴 라이프사이클 정리 배치를 담당한다.
//
// 구성:
// - scheduler: 매일 새벽 3시(KST) 실행 트리거
// - cleanup: 실제 정리 로직 (Phase 2 / Phase 3)
//
// 시작:
// main.rs에서 `jobs::scheduler::start(state)` 호출 시
// tokio::spawn으로 백그라운드 태스크가 실행된다.
//
// 수동 실행:
// `jobs::scheduler::run_now(&state).await` (관리자용)

pub mod cleanup;
pub mod contest_reward;
pub mod scheduler;
pub mod streak_reminder;
