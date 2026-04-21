// ============================================================
// reward/model.rs — DB 테이블과 1:1 대응하는 원시 구조체
//
// 역할: Supabase REST API 응답을 역직렬화(Deserialize)하는 컨테이너
// Serialize도 함께 derive: UPSERT 페이로드로 직렬화할 때 필요
//
// model ↔ dto 분리 이유:
//   - DB 컬럼 이름(snake_case)과 API 응답 필드를 독립적으로 변경 가능
//   - 민감한 DB 내부 필드(id, user_id 등)를 API에 노출하지 않을 수 있음
// ============================================================

use chrono::{DateTime, NaiveDate, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// ────────────────────────────────────────────────────────────
// rewards 테이블
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize)]
pub struct Reward {
    pub id: Uuid,
    pub user_id: Uuid,
    // "weekly_score" | "contest_1st" | "contest_2nd" | "contest_3rd"
    pub reward_type: String,
    pub amount: i32,
    pub description: Option<String>,
    pub earned_at: Option<DateTime<Utc>>,
}

// ────────────────────────────────────────────────────────────
// streaks 테이블
// unique constraint: (user_id)
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize)]
pub struct Streak {
    pub id: Uuid,
    pub user_id: Uuid,
    pub current_streak: Option<i32>,
    pub longest_streak: Option<i32>,
    pub last_record_date: Option<NaiveDate>, // NaiveDate: 시간대 없는 날짜 (YYYY-MM-DD)
    pub updated_at: Option<DateTime<Utc>>,
}

// ────────────────────────────────────────────────────────────
// weekly_scores 테이블
// unique constraint: (user_id, week_start)
//
// 변경: budget_check_score → budget_score (DB 컬럼명 통일)
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize)]
pub struct WeeklyScore {
    pub id: Uuid,
    pub user_id: Uuid,
    pub week_start: NaiveDate,           // 해당 주 월요일

    // 각 항목별 점수 (Option: DB에 아직 없는 경우 null)
    pub record_days_score: Option<i32>,  // 소비 기록 일수 (만점 30)
    pub receipt_score: Option<i32>,      // 영수증 인증 건수 (만점 25)
    pub diary_score: Option<i32>,        // 한줄 일기 작성 일수 (만점 20)
    pub budget_score: Option<i32>,       // 예산 준수 여부 (만점 15) ← budget_check_score에서 변경
    pub streak_score: Option<i32>,       // 연속 활동 보너스 (만점 10)
    pub total_score: Option<i32>,        // 총점 (만점 100)

    pub reward_granted: Option<bool>,    // 이번 주 보상이 이미 지급됐는지
    pub created_at: Option<DateTime<Utc>>,
}

// ────────────────────────────────────────────────────────────
// token_burns 테이블 — 마켓 거래 소각 기록용
// ────────────────────────────────────────────────────────────
#[derive(Debug, Serialize, Deserialize)]
pub struct TokenBurn {
    pub amount: i32,
    pub reason: String, // "marketplace_fee"
    // burned_at은 DB 기본값(now())으로 처리 → INSERT 시 생략 가능
}