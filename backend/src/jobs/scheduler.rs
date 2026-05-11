// ─────────────────────────────────────────────────────────────
// 백그라운드 배치 스케줄러
// ─────────────────────────────────────────────────────────────
//
// 매일 KST 새벽 3시에 회원탈퇴 정리 배치를 실행한다.
//
// 왜 새벽 3시?
// - 한국 사용자 대상 서비스 기준
// - 트래픽 가장 적은 시간대
// - DB 부하 최소화
//
// 실행 내용:
// 1) Phase 2: 30일 지난 탈퇴자의 쿨다운 식별자 정리
//    (provider_id, login_provider, google_connected 등)
// 2) Phase 3: 5년 지난 탈퇴자 완전 익명화
//    (부가 데이터 hard delete + users row 익명화)
//
// 동시 실행 보호:
// - 단일 인스턴스 가정 → 별도 락 없음
// - 멀티 인스턴스 환경에선 PostgreSQL advisory lock 추가 필요
//
// 수동 실행:
// - run_now(&state).await 로 즉시 1회 실행 가능
// - 관리자 페이지의 "지금 정리 실행" 버튼에서 호출

use chrono::{Datelike, TimeZone, Utc};
use chrono_tz::Asia::Seoul;

use crate::jobs::cleanup;
use crate::state::AppState;

/// 스케줄러 시작 (main.rs에서 한 번 호출)
///
/// tokio::spawn으로 백그라운드 태스크를 띄우고,
/// 무한 루프로 매일 새벽 3시(KST)마다 정리 배치를 실행한다.
pub fn start(state: AppState) {
    tokio::spawn(async move {
        tracing::info!("배치 스케줄러 시작: 매일 KST 03:00 실행");

        loop {
            // 다음 실행 시각까지 대기
            let sleep_dur = duration_until_next_run();
            let hours = sleep_dur.as_secs() / 3600;
            let minutes = (sleep_dur.as_secs() % 3600) / 60;

            tracing::info!(
                "다음 배치 실행까지 대기: {}시간 {}분",
                hours,
                minutes
            );

            tokio::time::sleep(sleep_dur).await;

            // 일일 정리 배치 실행
            run_daily_cleanup(&state).await;
        }
    });
}

/// 다음 새벽 3시(KST)까지 남은 시간 계산
///
/// 현재 시각이:
/// - 새벽 3시 이전 → 오늘 새벽 3시
/// - 새벽 3시 이후 → 내일 새벽 3시
fn duration_until_next_run() -> std::time::Duration {
    let now_kst = Utc::now().with_timezone(&Seoul);

    // 오늘 KST 03:00 (DST 등 예외 케이스가 있을 수 있어 unwrap 안 씀)
    let today_3am = match Seoul.with_ymd_and_hms(
        now_kst.year(),
        now_kst.month(),
        now_kst.day(),
        3,
        0,
        0,
    ) {
        chrono::LocalResult::Single(dt) => dt,
        _ => {
            // 예상치 못한 케이스 → 24시간 뒤로 fallback
            tracing::warn!("KST 03:00 계산 실패, 24시간 후로 fallback");
            return std::time::Duration::from_secs(24 * 3600);
        }
    };

    // 이미 오늘 03:00 지났으면 내일 03:00
    let next_run = if now_kst < today_3am {
        today_3am
    } else {
        today_3am + chrono::Duration::days(1)
    };

    // chrono Duration → std Duration
    (next_run - now_kst)
        .to_std()
        .unwrap_or_else(|_| std::time::Duration::from_secs(24 * 3600))
}

/// 일일 정리 배치 1회 실행
///
/// Phase 2 (30일) → Phase 3 (5년) 순서로 실행.
/// 한 단계가 실패해도 다음 단계는 계속 진행 (독립적)
async fn run_daily_cleanup(state: &AppState) {
    tracing::info!("=== 일일 탈퇴자 정리 배치 시작 ===");

    // Phase 2: 30일 쿨다운 정리
    match cleanup::cleanup_30day_withdrawals(state).await {
        Ok(count) => {
            tracing::info!("Phase 2 (30일 쿨다운 정리) 완료: {}건 처리", count);
        }
        Err(e) => {
            tracing::error!("Phase 2 (30일 쿨다운 정리) 실패: {}", e);
        }
    }

    // Phase 3: 5년 익명화
    match cleanup::cleanup_5year_withdrawals(state).await {
        Ok(count) => {
            tracing::info!("Phase 3 (5년 익명화) 완료: {}건 처리", count);
        }
        Err(e) => {
            tracing::error!("Phase 3 (5년 익명화) 실패: {}", e);
        }
    }

    tracing::info!("=== 일일 탈퇴자 정리 배치 종료 ===");
}

/// 수동 실행용 (관리자 트리거)
///
/// 관리자 페이지의 "지금 정리 실행" 버튼에서 호출.
/// 발표 시연이나 긴급 정리 시에도 사용 가능.
pub async fn run_now(state: &AppState) {
    tracing::info!("배치 수동 실행 요청");
    run_daily_cleanup(state).await;
}