// ============================================================
// reward/service.rs — 비즈니스 로직 레이어
//
// 핵심 원칙:
//   - handler는 HTTP 관련 처리만 (상태코드, JSON 변환)
//   - 실제 계산/DB 접근은 service에서 담당
//   - anyhow::Result로 에러 전파 → handler에서 500으로 변환
// ============================================================

use anyhow::{Context, Result, anyhow};
use chrono::{Datelike, Local, NaiveDate};
use uuid::Uuid;

use super::{
    dto::{ContestRewardRequest, RewardResponse, StreakResponse, WeeklyScoreResponse},
    model::{Reward, Streak, TokenBurn, WeeklyScore},
};
use crate::state::AppState;

// ────────────────────────────────────────────────────────────
// 기존 함수 3개 — 조회 전용 (변경 없음)
// ────────────────────────────────────────────────────────────

pub async fn list_rewards(state: &AppState, user_id: Uuid) -> Result<Vec<RewardResponse>> {
    let url = format!(
        "{}/rest/v1/rewards?user_id=eq.{}&select=*&order=earned_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("rewards SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!("rewards SELECT 실패: {}", res.text().await.unwrap_or_default()));
    }

    let rewards: Vec<Reward> = res.json().await.context("rewards 역직렬화 실패")?;
    Ok(rewards
        .into_iter()
        .map(|r| RewardResponse {
            id: r.id,
            reward_type: r.reward_type,
            amount: r.amount,
            description: r.description,
            earned_at: r.earned_at,
        })
        .collect())
}

pub async fn get_streak(state: &AppState, user_id: Uuid) -> Result<StreakResponse> {
    let url = format!(
        "{}/rest/v1/streaks?user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!("streaks SELECT 실패: {}", res.text().await.unwrap_or_default()));
    }

    let streaks: Vec<Streak> = res.json().await.context("streaks 역직렬화 실패")?;

    // 레코드가 없으면 기본값(streak 0) 반환 — 신규 유저 대응
    let streak = streaks.into_iter().next().unwrap_or(Streak {
        id: Uuid::nil(),
        user_id,
        current_streak: Some(0),
        longest_streak: Some(0),
        last_record_date: None,
        updated_at: None,
    });

    Ok(StreakResponse {
        current_streak: streak.current_streak,
        longest_streak: streak.longest_streak,
        last_record_date: streak.last_record_date,
    })
}

pub async fn get_weekly_scores(state: &AppState, user_id: Uuid) -> Result<Vec<WeeklyScoreResponse>> {
    let url = format!(
        "{}/rest/v1/weekly_scores?user_id=eq.{}&select=*&order=week_start.desc&limit=12",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("weekly_scores SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "weekly_scores SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let scores: Vec<WeeklyScore> = res.json().await.context("weekly_scores 역직렬화 실패")?;
    Ok(scores
        .into_iter()
        .map(|s| WeeklyScoreResponse {
            id: s.id,
            week_start: s.week_start,
            record_days_score: s.record_days_score,
            receipt_score: s.receipt_score,
            diary_score: s.diary_score,
            budget_score: s.budget_score,
            streak_score: s.streak_score,
            total_score: s.total_score,
            reward_granted: s.reward_granted,
        })
        .collect())
}

// ────────────────────────────────────────────────────────────
// 헬퍼: week_start 계산
//
// chrono의 weekday().num_days_from_monday():
//   월요일 = 0, 화요일 = 1, ..., 일요일 = 6
// target_date에서 그 일수만큼 빼면 해당 주 월요일이 나옴
//
// 예) 2024-01-17(수요일): 17 - 2 = 15 → 2024-01-15(월요일)
// ────────────────────────────────────────────────────────────
fn get_week_start(target_date: NaiveDate) -> NaiveDate {
    let days_from_monday = target_date.weekday().num_days_from_monday() as i64;
    target_date - chrono::Duration::days(days_from_monday)
}

// ────────────────────────────────────────────────────────────
// 헬퍼: record_days_score 계산
//
// 스텝 함수 형태 — 날수에 따라 불연속적으로 증가
// 매직 넘버를 match로 명시 → 기획 변경 시 수정 포인트 명확
// ────────────────────────────────────────────────────────────
fn calc_record_days_score(days: usize) -> i32 {
    match days {
        0 => 0,
        1 => 5,
        2 => 10,
        3 => 15,
        4 => 18,
        5 => 22,
        6 => 26,
        _ => 30, // 7일 이상
    }
}

// ────────────────────────────────────────────────────────────
// 헬퍼: diary_score 계산
// ────────────────────────────────────────────────────────────
fn calc_diary_score(days: usize) -> i32 {
    match days {
        0 => 0,
        1 => 3,
        2 => 6,
        3 => 9,
        4 => 12,
        5 => 15,
        6 => 18,
        _ => 20, // 7일
    }
}

// ────────────────────────────────────────────────────────────
// 헬퍼: streak_score 계산
// ────────────────────────────────────────────────────────────
fn calc_streak_score(streak: i32) -> i32 {
    match streak {
        0 => 0,
        1..=3 => 2,
        4..=6 => 4,
        7..=13 => 6,
        14..=20 => 8,
        _ => 10, // 21일 이상
    }
}

// ────────────────────────────────────────────────────────────
// 헬퍼: 반감기 적용 SPT 계산
//
// 공식: 기준 SPT × (0.5 ^ (경과일수 / 365))
// f64로 계산 후 floor()로 버림, 최소 1
//
// powi() 아닌 powf() 사용 이유: 지수가 f64이므로
// ────────────────────────────────────────────────────────────
fn calc_halving_reward(base_spt: i32, launch_date: NaiveDate, today: NaiveDate) -> i32 {
    let elapsed_days = (today - launch_date).num_days().max(0) as f64;
    let halving_factor = 0.5_f64.powf(elapsed_days / 365.0);
    let actual = (base_spt as f64 * halving_factor).floor() as i32;
    // 60점 이상이면 최소 1 SPT 보장
    actual.max(1)
}

// ────────────────────────────────────────────────────────────
// 헬퍼: 총점 → 기준 SPT 매핑
// 60점 미만은 0 반환 → 호출부에서 0이면 보상 지급 안 함
// ────────────────────────────────────────────────────────────
fn base_spt_from_score(total_score: i32) -> i32 {
    match total_score {
        0..=59 => 0,
        60..=69 => 10,
        70..=79 => 20,
        80..=89 => 35,
        90..=99 => 55,
        _ => 80, // 100점
    }
}

// ────────────────────────────────────────────────────────────
// 신규 함수 1: update_streak
//
// 호출: create_expense() → tokio::spawn (fire-and-forget)
// 목적: 소비 기록 저장 시 연속 활동 스트릭 갱신
//
// 로직:
//   - last_record_date == 오늘 → 이미 기록, 변화 없음
//   - last_record_date == 어제 → streak + 1
//   - 그 외(어제도 아님) → streak = 1 리셋
//   - longest_streak = max(longest, current)
//   - UPSERT (user_id unique)
// ────────────────────────────────────────────────────────────
pub async fn update_streak(
    state: &AppState,
    user_id: Uuid,
    record_date: NaiveDate,
) -> Result<()> {
    // ── 현재 streak 조회 ──
    let url = format!(
        "{}/rest/v1/streaks?user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!("streaks SELECT 실패: {}", res.text().await.unwrap_or_default()));
    }

    let streaks: Vec<Streak> = res.json().await.context("streaks 역직렬화 실패")?;

    // 신규 유저: 기본값으로 초기화
    let current = streaks.into_iter().next().unwrap_or(Streak {
        id: Uuid::nil(),
        user_id,
        current_streak: Some(0),
        longest_streak: Some(0),
        last_record_date: None,
        updated_at: None,
    });

    let last = current.last_record_date;
    let prev_streak = current.current_streak.unwrap_or(0);
    let prev_longest = current.longest_streak.unwrap_or(0);

    // ── 날짜 비교로 새 streak 계산 ──
    let new_streak = match last {
        Some(d) if d == record_date => {
            // 오늘 이미 기록함 → 변화 없음
            return Ok(());
        }
        Some(d) if d == record_date - chrono::Duration::days(1) => {
            // 어제 기록 → 연속 +1
            prev_streak + 1
        }
        _ => {
            // 연속 끊김 → 1로 리셋
            1
        }
    };

    let new_longest = prev_longest.max(new_streak);

    // ── UPSERT ──
    // Prefer: resolution=merge-duplicates → (user_id) 충돌 시 UPDATE
    let upsert_url = format!(
        "{}/rest/v1/streaks",
        state.config.supabase_url.trim_end_matches('/')
    );

    // serde_json::json! 매크로로 인라인 JSON 구성
    let payload = serde_json::json!({
        "user_id": user_id,
        "current_streak": new_streak,
        "longest_streak": new_longest,
        "last_record_date": record_date.to_string(), // "YYYY-MM-DD"
    });

    let upsert_res = state
        .http_client
        .post(&upsert_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "resolution=merge-duplicates,return=representation")
        .json(&payload)
        .send()
        .await
        .context("streaks UPSERT 요청 실패")?;

    if !upsert_res.status().is_success() {
        return Err(anyhow!(
            "streaks UPSERT 실패: {}",
            upsert_res.text().await.unwrap_or_default()
        ));
    }

    Ok(())
}

// ────────────────────────────────────────────────────────────
// 신규 함수 2: recalculate_weekly_score
//
// 호출: create_expense() → tokio::spawn (fire-and-forget)
// 목적: 소비 기록 저장 때마다 해당 주 성실도 점수 재계산
//
// 처리 순서:
//   1. week_start 계산 (target_date → 해당 주 월요일)
//   2. 해당 주 expenses 조회
//   3. 5개 항목 점수 계산
//   4. weekly_scores UPSERT
//   5. 보상 미지급 + 60점 이상이면 SPT 지급 + reward_granted=true 업데이트
// ────────────────────────────────────────────────────────────
pub async fn recalculate_weekly_score(
    state: &AppState,
    user_id: Uuid,
    target_date: NaiveDate,
) -> Result<WeeklyScoreResponse> {
    let base_url = state.config.supabase_url.trim_end_matches('/');
    let key = &state.config.supabase_secret_key;

    // ── 1. week_start: 해당 주 월요일 ──
    let week_start = get_week_start(target_date);
    let week_end = week_start + chrono::Duration::days(6); // 일요일

    // ── 2. 해당 주 expenses 조회 ──
    // expense_date: week_start ≤ date ≤ week_end
    // gte = greater than or equal, lte = less than or equal (Supabase 필터 연산자)
    let exp_url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=gte.{}&expense_date=lte.{}&select=expense_date,one_line_diary,receipt_verified,amount",
        base_url, user_id, week_start, week_end
    );
    let exp_res = state
        .http_client
        .get(&exp_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key.as_str())
        .send()
        .await
        .context("expenses SELECT 요청 실패")?;

    if !exp_res.status().is_success() {
        return Err(anyhow!("expenses SELECT 실패: {}", exp_res.text().await.unwrap_or_default()));
    }

    // expenses 조회용 임시 구조체 (model.rs의 Expense 전체 대신 필요한 필드만)
    #[derive(serde::Deserialize)]
    struct ExpenseRow {
        expense_date: NaiveDate,
        one_line_diary: Option<String>,
        receipt_verified: Option<bool>,
        amount: i32,
    }

    let expenses: Vec<ExpenseRow> = exp_res.json().await.context("expenses 역직렬화 실패")?;

    // ── 3. record_days_score ──
    // 소비 기록이 있는 날의 집합 (중복 제거)
    // HashSet 대신 Vec + dedup 로도 가능하지만 HashSet이 더 직관적
    let record_dates: std::collections::HashSet<NaiveDate> =
        expenses.iter().map(|e| e.expense_date).collect();
    let record_days_score = calc_record_days_score(record_dates.len());

    // ── 4. receipt_score ──
    // receipt_verified == true인 건수 × 2, 상한 25
    let verified_count = expenses
        .iter()
        .filter(|e| e.receipt_verified == Some(true))
        .count() as i32;
    let receipt_score = (verified_count * 2).min(25);

    // ── 5. diary_score ──
    // one_line_diary가 Some이고 비어있지 않은 날 수
    let diary_dates: std::collections::HashSet<NaiveDate> = expenses
        .iter()
        .filter(|e| e.one_line_diary.as_deref().map_or(false, |s| !s.is_empty()))
        .map(|e| e.expense_date)
        .collect();
    let diary_score = calc_diary_score(diary_dates.len());

    // ── 6. budget_score ──
    // 해당 월 budgets 조회 → 주간 예산 = total_budget / 4
    // 주간 지출 합계와 비교
    let budget_score = {
        let year = week_start.year();
        let month = week_start.month();

        let budget_url = format!(
            "{}/rest/v1/budgets?user_id=eq.{}&year=eq.{}&month=eq.{}&select=total_budget&limit=1",
            base_url, user_id, year, month,
        );
        let budget_res = state
            .http_client
            .get(&budget_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .send()
            .await
            .context("budgets SELECT 요청 실패")?;

        #[derive(serde::Deserialize)]
        struct BudgetRow {
            total_budget: i32,
        }

        if budget_res.status().is_success() {
            let budgets: Vec<BudgetRow> =
                budget_res.json().await.context("budgets 역직렬화 실패")?;

            match budgets.into_iter().next() {
                None => 0, // 예산 미설정 → 0점
                Some(b) => {
                    // 주간 예산 = 월 예산 / 4 (정수 나눗셈)
                    let weekly_budget = b.total_budget / 4;
                    // 해당 주 실제 지출 합계
                    let weekly_spent: i32 = expenses.iter().map(|e| e.amount).sum();

                    if weekly_budget <= 0 {
                        // 예산이 0 이하면 설정 안 한 것으로 간주
                        0
                    } else {
                        // 초과율 = (지출 - 예산) / 예산
                        let over = weekly_spent - weekly_budget;
                        if over <= 0 {
                            15 // 초과 없음
                        } else {
                            let ratio = over as f64 / weekly_budget as f64;
                            if ratio < 0.10 {
                                10 // 10% 미만 초과
                            } else if ratio < 0.20 {
                                5  // 20% 미만 초과
                            } else {
                                0  // 20% 이상 초과
                            }
                        }
                    }
                }
            }
        } else {
            // budgets 조회 실패 → 점수 0으로 처리 (치명적이지 않음)
            tracing::warn!("budgets SELECT 실패, budget_score=0으로 처리");
            0
        }
    };

    // ── 7. streak_score ──
    // get_streak()으로 current_streak 조회 후 계산
    let streak_resp = get_streak(state, user_id).await?;
    let streak_score = calc_streak_score(streak_resp.current_streak.unwrap_or(0));

    // ── 8. total_score ──
    let total_score = record_days_score + receipt_score + diary_score + budget_score + streak_score;

    // ── 9. weekly_scores UPSERT ──
    // unique: (user_id, week_start) → 충돌 시 모든 점수 덮어쓰기
    let upsert_url = format!("{}/rest/v1/weekly_scores", base_url);
    let upsert_payload = serde_json::json!({
        "user_id": user_id,
        "week_start": week_start.to_string(),
        "record_days_score": record_days_score,
        "receipt_score": receipt_score,
        "diary_score": diary_score,
        "budget_score": budget_score,
        "streak_score": streak_score,
        "total_score": total_score,
        // reward_granted는 UPSERT 시 건드리지 않음
        // → merge-duplicates는 명시된 필드만 업데이트
    });

    let upsert_res = state
        .http_client
        .post(&upsert_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key.as_str())
        .header("Prefer", "resolution=merge-duplicates,return=representation")
        .json(&upsert_payload)
        .send()
        .await
        .context("weekly_scores UPSERT 요청 실패")?;

    if !upsert_res.status().is_success() {
        return Err(anyhow!(
            "weekly_scores UPSERT 실패: {}",
            upsert_res.text().await.unwrap_or_default()
        ));
    }

    let upserted: Vec<WeeklyScore> =
        upsert_res.json().await.context("weekly_scores UPSERT 역직렬화 실패")?;

    let score_row = upserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("weekly_scores UPSERT 결과가 비어있음"))?;

    // ── 10. 보상 지급 판단 ──
    // reward_granted가 false이고 total_score >= 60이면 SPT 지급
    let already_granted = score_row.reward_granted.unwrap_or(false);

    if !already_granted && total_score >= 60 {
        let base_spt = base_spt_from_score(total_score);
        // 반감기 적용
        let today = Local::now().date_naive();
        let actual_spt = calc_halving_reward(base_spt, state.config.service_launch_date, today);

        // rewards INSERT
        let reward_url = format!("{}/rest/v1/rewards", base_url);
        let reward_payload = serde_json::json!({
            "user_id": user_id,
            "reward_type": "weekly_score",
            "amount": actual_spt,
            "description": format!("{}주차 성실도 점수 {}점 보상", week_start, total_score),
        });

        let reward_res = state
            .http_client
            .post(&reward_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .header("Prefer", "return=representation")
            .json(&reward_payload)
            .send()
            .await
            .context("rewards INSERT 요청 실패")?;

        if !reward_res.status().is_success() {
            return Err(anyhow!(
                "rewards INSERT 실패: {}",
                reward_res.text().await.unwrap_or_default()
            ));
        }

        // users.spt_balance 증가
        // PostgREST는 increment를 지원하지 않으므로 RPC 사용
        // supabase function: increment_spt_balance(user_id, amount)
        let rpc_url = format!("{}/rest/v1/rpc/increment_spt_balance", base_url);
        let balance_res = state
            .http_client
            .post(&rpc_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .json(&serde_json::json!({
                "p_user_id": user_id,
                "p_amount": actual_spt,
            }))
            .send()
            .await
            .context("spt_balance 증가 RPC 요청 실패")?;

        if !balance_res.status().is_success() {
            return Err(anyhow!(
                "spt_balance 증가 실패: {}",
                balance_res.text().await.unwrap_or_default()
            ));
        }

        // weekly_scores.reward_granted = true 업데이트
        // PATCH URL: id로 특정 행 지정
        let patch_url = format!(
            "{}/rest/v1/weekly_scores?id=eq.{}",
            base_url, score_row.id
        );
        let patch_res = state
            .http_client
            .patch(&patch_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .header("Prefer", "return=representation")
            .json(&serde_json::json!({ "reward_granted": true }))
            .send()
            .await
            .context("weekly_scores reward_granted 업데이트 실패")?;

        if !patch_res.status().is_success() {
            return Err(anyhow!(
                "weekly_scores PATCH 실패: {}",
                patch_res.text().await.unwrap_or_default()
            ));
        }
    }

    let did_grant = !already_granted && total_score >= 60;
    Ok(WeeklyScoreResponse {
        id: score_row.id,
        week_start: score_row.week_start,
        record_days_score: score_row.record_days_score,
        receipt_score: score_row.receipt_score,
        diary_score: score_row.diary_score,
        budget_score: score_row.budget_score,
        streak_score: score_row.streak_score,
        total_score: score_row.total_score,
        reward_granted: Some(already_granted || did_grant),
    })
}

// ────────────────────────────────────────────────────────────
// 신규 함수 3: get_current_weekly_score
//
// 호출: GET /api/rewards/weekly-score/current
// 목적: 오늘 기준 이번 주 점수 조회만 (재계산 없음)
//
// weekly_scores에 레코드가 없으면 전 항목 None인 기본값 반환
// → 이번 주에 소비 기록이 한 번도 없는 경우
// ────────────────────────────────────────────────────────────
pub async fn get_current_weekly_score(
    state: &AppState,
    user_id: Uuid,
) -> Result<WeeklyScoreResponse> {
    let today = Local::now().date_naive();
    let week_start = get_week_start(today);

    let url = format!(
        "{}/rest/v1/weekly_scores?user_id=eq.{}&week_start=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        week_start
    );

    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("weekly_scores(current) SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "weekly_scores(current) SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let scores: Vec<WeeklyScore> = res.json().await.context("weekly_scores 역직렬화 실패")?;

    // 레코드 없으면 전 항목 None으로 반환 (신규 유저, 이번 주 첫 기록 전)
    Ok(scores.into_iter().next().map_or_else(
        || WeeklyScoreResponse {
            id: Uuid::nil(),
            week_start,
            record_days_score: None,
            receipt_score: None,
            diary_score: None,
            budget_score: None,
            streak_score: None,
            total_score: None,
            reward_granted: Some(false),
        },
        |s| WeeklyScoreResponse {
            id: s.id,
            week_start: s.week_start,
            record_days_score: s.record_days_score,
            receipt_score: s.receipt_score,
            diary_score: s.diary_score,
            budget_score: s.budget_score,
            streak_score: s.streak_score,
            total_score: s.total_score,
            reward_granted: s.reward_granted,
        },
    ))
}

// ────────────────────────────────────────────────────────────
// 신규 함수 4: grant_contest_reward
//
// 호출: POST /api/admin/contest/reward
// 목적: 관리자가 공모전 수상자에게 SPT 수동 지급
//
// rank → amount, reward_type 결정 후 rewards INSERT
// ────────────────────────────────────────────────────────────
pub async fn grant_contest_reward(
    state: &AppState,
    req: ContestRewardRequest,
) -> Result<RewardResponse> {
    // rank 유효성 검사 — 1, 2, 3만 허용
    let (amount, reward_type) = match req.rank {
        1 => (500, "contest_1st"),
        2 => (300, "contest_2nd"),
        3 => (150, "contest_3rd"),
        _ => return Err(anyhow!("유효하지 않은 rank: {}. 1, 2, 3만 허용됩니다.", req.rank)),
    };

    let url = format!(
        "{}/rest/v1/rewards",
        state.config.supabase_url.trim_end_matches('/')
    );

    let payload = serde_json::json!({
        "user_id": req.user_id,
        "reward_type": reward_type,
        "amount": amount,
        "description": format!("공모전 {}등 보상", req.rank),
    });

    let res = state
        .http_client
        .post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&payload)
        .send()
        .await
        .context("rewards INSERT(contest) 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "rewards INSERT(contest) 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    // users.spt_balance 증가
    let rpc_url = format!(
        "{}/rest/v1/rpc/increment_spt_balance",
        state.config.supabase_url.trim_end_matches('/')
    );
    let balance_res = state
        .http_client
        .post(&rpc_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .json(&serde_json::json!({
            "p_user_id": req.user_id,
            "p_amount": amount,
        }))
        .send()
        .await
        .context("spt_balance 증가 RPC 요청 실패")?;

    if !balance_res.status().is_success() {
        return Err(anyhow!(
            "spt_balance 증가 실패: {}",
            balance_res.text().await.unwrap_or_default()
        ));
    }

    let rewards: Vec<Reward> = res.json().await.context("rewards 역직렬화 실패")?;
    let reward = rewards
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("rewards INSERT 결과가 비어있음"))?;

    Ok(RewardResponse {
        id: reward.id,
        reward_type: reward.reward_type,
        amount: reward.amount,
        description: reward.description,
        earned_at: reward.earned_at,
    })
}
