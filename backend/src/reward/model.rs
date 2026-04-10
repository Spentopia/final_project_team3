// reward/model.rs
// public.rewards, public.streaks, public.weekly_scores 테이블 엔티티

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.rewards 테이블
// SPT 보상 획득 이력
// reward_type: 'report'(주간 성실도) / 'contest'(콘테스트 수상)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Reward {
    pub id: Uuid,
    pub user_id: Uuid,
    pub reward_type: String,     // report / contest
    pub amount: i32,
    pub description: Option<String>,
    pub earned_at: Option<DateTime<Utc>>,
}

// public.streaks 테이블
// 연속 활동 스트릭
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Streak {
    pub id: Uuid,
    pub user_id: Uuid,
    pub current_streak: Option<i32>,
    pub longest_streak: Option<i32>,
    pub last_record_date: Option<NaiveDate>,
    pub updated_at: Option<DateTime<Utc>>,
}

// public.weekly_scores 테이블
// 주간 성실도 점수 (만점 100)
// 70점 이상: 기본 보상 / 90점 이상: 추가 보상
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct WeeklyScore {
    pub id: Uuid,
    pub user_id: Uuid,
    pub week_start: NaiveDate,          // 해당 주 시작일 (월요일)
    pub record_days_score: Option<i32>, // 소비 기록 일수 점수 (만점 40)
    pub receipt_score: Option<i32>,     // 영수증 인증 점수 (만점 20)
    pub diary_score: Option<i32>,       // 한줄 일기 점수 (만점 15)
    pub budget_check_score: Option<i32>,// 예산 체크인 점수 (만점 15)
    pub streak_score: Option<i32>,      // 스트릭 유지 점수 (만점 10)
    pub total_score: Option<i32>,       // 총점 (자동 계산)
    pub reward_granted: Option<bool>,   // 보상 지급 완료 여부
    pub created_at: Option<DateTime<Utc>>,
}
