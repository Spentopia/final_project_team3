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
use rand::{Rng, seq::SliceRandom};
use uuid::Uuid;

use crate::clients::solana_client;

use super::{
    dto::{
        BoxCountResponse, ContestRewardRequest, OpenBoxResponse, RewardResponse, StreakResponse,
        UnityAvatarItemResponse, WeeklyScoreResponse,
    },
    model::{Reward, Streak, WeeklyScore},
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("rewards SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "rewards SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "streaks SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
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

pub async fn get_weekly_scores(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<WeeklyScoreResponse>> {
    let url = format!(
        "{}/rest/v1/weekly_scores?user_id=eq.{}&select=*&order=week_start.desc&limit=12",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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
// 헬퍼: receipt_score 계산
//
// 영수증 인증은 건수가 아니라 인증한 날짜 수를 기준으로 계산한다.
// 같은 날 여러 건을 인증해도 성실도 점수는 하루치만 오른다.
// ────────────────────────────────────────────────────────────
fn calc_receipt_score(days: usize) -> i32 {
    match days {
        0 => 0,
        1 => 4,
        2 => 7,
        3 => 11,
        4 => 15,
        5 => 18,
        6 => 22,
        _ => 25,
    }
}

// ────────────────────────────────────────────────────────────
// 헬퍼: budget_score 기본점 계산
//
// 예산을 지켰더라도 첫날부터 15점을 모두 주지 않는다.
// 주간 성실도 취지에 맞게 소비 기록 일수에 따라 예산 점수 한도를 점진적으로 연다.
// ────────────────────────────────────────────────────────────
fn calc_budget_base_score(record_days: usize) -> i32 {
    match record_days {
        0 => 0,
        1 => 2,
        2 => 4,
        3 => 6,
        4 => 9,
        5 => 11,
        6 => 13,
        _ => 15,
    }
}

// ────────────────────────────────────────────────────────────
// 헬퍼: 주간 총점 상한
//
// 주간 100점 구조에서 하루에 과도한 점수를 얻지 못하도록
// 활동한 날짜 수에 따라 총점 상한을 둔다.
// ────────────────────────────────────────────────────────────
fn calc_total_score_cap(record_days: usize) -> i32 {
    match record_days {
        0 => 0,
        1 => 15,
        2 => 30,
        3 => 45,
        4 => 60,
        5 => 75,
        6 => 90,
        _ => 100,
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

async fn get_wallet_address(state: &AppState, user_id: Uuid) -> Result<Option<String>> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=wallet_address",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("users wallet_address 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "users wallet_address 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<serde_json::Value> = res
        .json()
        .await
        .context("users wallet_address 역직렬화 실패")?;
    Ok(rows
        .first()
        .and_then(|r| r["wallet_address"].as_str())
        .filter(|v| !v.trim().is_empty())
        .map(str::to_string))
}

async fn reset_weekly_reward_claim(state: &AppState, weekly_score_id: Uuid) {
    let url = format!(
        "{}/rest/v1/weekly_scores?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        weekly_score_id
    );
    match state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({ "reward_granted": false }))
        .send()
        .await
    {
        Ok(res) if res.status().is_success() => {}
        Ok(res) => {
            tracing::error!(
                "weekly_scores reward_granted 복구 실패: id={} body={}",
                weekly_score_id,
                res.text().await.unwrap_or_default()
            );
        }
        Err(e) => {
            tracing::error!(
                "weekly_scores reward_granted 복구 요청 실패: id={} err={}",
                weekly_score_id,
                e
            );
        }
    }
}

async fn grant_untradeable_avatar_item(
    state: &AppState,
    user_id: Uuid,
    description: &str,
) -> Result<()> {
    #[derive(serde::Deserialize)]
    struct AvatarItemRow {
        id: Uuid,
    }

    let key = &state.config.supabase_secret_key;
    let base_url = state.config.supabase_url.trim_end_matches('/');
    let item_url = format!(
        "{}/rest/v1/item_master?item_type=eq.normal&drop_weight=gt.0&select=id",
        base_url
    );
    let item_res = state
        .http_client
        .get(&item_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .send()
        .await
        .context("avatar_items 보상 후보 조회 요청 실패")?;

    if !item_res.status().is_success() {
        return Err(anyhow!(
            "avatar_items 보상 후보 조회 실패: {}",
            item_res.text().await.unwrap_or_default()
        ));
    }

    let items: Vec<AvatarItemRow> = item_res
        .json()
        .await
        .context("avatar_items 보상 후보 역직렬화 실패")?;
    let item_id = items
        .choose(&mut rand::thread_rng())
        .map(|item| item.id)
        .ok_or_else(|| anyhow!("지급 가능한 avatar_items가 없습니다"))?;

    let user_item_url = format!("{}/rest/v1/user_inventory", base_url);
    let user_item_res = state
        .http_client
        .post(&user_item_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "user_id": user_id,
            "item_id": item_id,
            "is_equipped": false,
            "is_nft": false,
        }))
        .send()
        .await
        .context("user_items 보상 INSERT 요청 실패")?;

    if !user_item_res.status().is_success() {
        return Err(anyhow!(
            "user_items 보상 INSERT 실패: {}",
            user_item_res.text().await.unwrap_or_default()
        ));
    }

    let reward_url = format!("{}/rest/v1/rewards", base_url);
    let reward_res = state
        .http_client
        .post(&reward_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "user_id": user_id,
            "reward_type": "weekly_score_avatar_item",
            "amount": 0,
            "description": description,
        }))
        .send()
        .await
        .context("avatar item reward INSERT 요청 실패")?;

    if !reward_res.status().is_success() {
        return Err(anyhow!(
            "avatar item reward INSERT 실패: {}",
            reward_res.text().await.unwrap_or_default()
        ));
    }

    Ok(())
}

async fn grant_nft_avatar_item(
    state: &AppState,
    user_id: Uuid,
    wallet_address: &str,
    reward_key: Uuid,
    description: &str,
) -> Result<()> {
    #[derive(serde::Deserialize)]
    struct AvatarItemRow {
        id: Uuid,
        name: String,
        image_url: String,
        metadata_uri: Option<String>,
    }

    let key = &state.config.supabase_secret_key;
    let base_url = state.config.supabase_url.trim_end_matches('/');
    let item_url = format!(
        "{}/rest/v1/item_master?item_type=eq.nft&drop_weight=gt.0&select=id,name,image_url,metadata_uri",
        base_url
    );
    let item_res = state
        .http_client
        .get(&item_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .send()
        .await
        .context("NFT avatar_items 보상 후보 조회 요청 실패")?;

    if !item_res.status().is_success() {
        return Err(anyhow!(
            "NFT avatar_items 보상 후보 조회 실패: {}",
            item_res.text().await.unwrap_or_default()
        ));
    }

    let items: Vec<AvatarItemRow> = item_res
        .json()
        .await
        .context("NFT avatar_items 보상 후보 역직렬화 실패")?;
    let item = items
        .choose(&mut rand::thread_rng())
        .ok_or_else(|| anyhow!("NFT로 지급 가능한 avatar_items가 없습니다"))?;

    let item_seed = reward_key.simple().to_string();
    let nft_uri = item
        .metadata_uri
        .as_deref()
        .filter(|v| !v.trim().is_empty())
        .unwrap_or(&item.image_url);
    let (tx_signature, nft_mint_address) = solana_client::mint_avatar_nft_to_user(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_admin_keypair,
        wallet_address,
        &state.config.solana_program_id,
        &item_seed,
        &item.name,
        "SPTA",
        nft_uri,
    )
    .await
    .context("주간 보상 NFT 아바타 민팅 실패")?;

    if let Err(e) = solana_client::check_signature_confirmed(
        &state.config.solana_rpc_url,
        &state.http_client,
        &tx_signature,
    )
    .await
    {
        tracing::error!(
            "[보상 NFT 민팅 확인 실패] 온체인 민팅은 성공했으나 confirmed 확인 불가. \
             수동 DB 동기화 필요. user_id={} reward_key={} nft_mint_address={} tx_signature={} err={}",
            user_id,
            reward_key,
            nft_mint_address,
            tx_signature,
            e
        );
        return Err(anyhow!("보상 NFT 민팅 트랜잭션 확인 실패: {}", e));
    }

    insert_user_inventory(
        state,
        user_id,
        item.id,
        true,
        Some(nft_mint_address),
        Some(tx_signature),
        Some(wallet_address.to_string()),
    )
    .await
    .context("NFT user_items 보상 INSERT 실패")?;

    let reward_url = format!("{}/rest/v1/rewards", base_url);
    let reward_res = state
        .http_client
        .post(&reward_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "user_id": user_id,
            "reward_type": "weekly_score_avatar_nft",
            "amount": 0,
            "description": description,
        }))
        .send()
        .await
        .context("NFT avatar reward INSERT 요청 실패")?;

    if !reward_res.status().is_success() {
        return Err(anyhow!(
            "NFT avatar reward INSERT 실패: {}",
            reward_res.text().await.unwrap_or_default()
        ));
    }

    Ok(())
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
pub async fn update_streak(state: &AppState, user_id: Uuid, record_date: NaiveDate) -> Result<()> {
    // ── 현재 streak 조회 ──
    let url = format!(
        "{}/rest/v1/streaks?user_id=eq.{}&select=*&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );
    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("streaks SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "streaks SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Prefer",
            "resolution=merge-duplicates,return=representation",
        )
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

    if let Err(e) = crate::notification::service::notify_streak_if_needed(
        state,
        user_id,
        new_streak,
        record_date,
    )
    .await
    {
        tracing::warn!("스트릭 알림 생성 실패: {}", e);
    }

    Ok(())
}

pub async fn rebuild_streak_from_expenses(state: &AppState, user_id: Uuid) -> Result<()> {
    #[derive(serde::Deserialize)]
    struct ExpenseDateRow {
        expense_date: NaiveDate,
    }

    let base_url = state.config.supabase_url.trim_end_matches('/');
    let key = &state.config.supabase_secret_key;

    let url = format!(
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&select=expense_date&order=expense_date.asc",
        base_url, user_id
    );

    let res = state
        .http_client
        .get(&url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key.as_str())
        .send()
        .await
        .context("스트릭 재계산용 expenses SELECT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "스트릭 재계산용 expenses SELECT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<ExpenseDateRow> = res
        .json()
        .await
        .context("스트릭 재계산용 expenses 역직렬화 실패")?;

    let mut dates: Vec<NaiveDate> = rows.into_iter().map(|row| row.expense_date).collect();
    dates.sort_unstable();
    dates.dedup();

    let mut current_streak = 0;
    let mut longest_streak = 0;
    let mut previous_date: Option<NaiveDate> = None;

    for date in &dates {
        current_streak = match previous_date {
            Some(previous) if *date == previous + chrono::Duration::days(1) => current_streak + 1,
            _ => 1,
        };
        longest_streak = longest_streak.max(current_streak);
        previous_date = Some(*date);
    }

    let last_record_date = dates.last().copied();
    if last_record_date.is_none() {
        current_streak = 0;
    }

    let upsert_url = format!("{}/rest/v1/streaks", base_url);
    let payload = serde_json::json!({
        "user_id": user_id,
        "current_streak": current_streak,
        "longest_streak": longest_streak,
        "last_record_date": last_record_date.map(|date| date.to_string()),
    });

    let upsert_res = state
        .http_client
        .post(&upsert_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key.as_str())
        .header(
            "Prefer",
            "resolution=merge-duplicates,return=representation",
        )
        .json(&payload)
        .send()
        .await
        .context("streaks 재계산 UPSERT 요청 실패")?;

    if !upsert_res.status().is_success() {
        return Err(anyhow!(
            "streaks 재계산 UPSERT 실패: {}",
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
        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=gte.{}&expense_date=lte.{}&select=expense_date,one_line_diary,receipt_verified",
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
        return Err(anyhow!(
            "expenses SELECT 실패: {}",
            exp_res.text().await.unwrap_or_default()
        ));
    }

    // expenses 조회용 임시 구조체 (model.rs의 Expense 전체 대신 필요한 필드만)
    #[derive(serde::Deserialize)]
    struct ExpenseRow {
        expense_date: NaiveDate,
        one_line_diary: Option<String>,
        receipt_verified: Option<bool>,
    }

    let expenses: Vec<ExpenseRow> = exp_res.json().await.context("expenses 역직렬화 실패")?;

    // 소비 기록/일기/예산/스트릭은 전체 소비 기록을 기준으로 계산한다.
    // 영수증 인증 점수만 OCR 인증 완료된 소비(receipt_verified=true)를 기준으로 계산한다.
    let verified_expenses: Vec<&ExpenseRow> = expenses
        .iter()
        .filter(|e| e.receipt_verified == Some(true))
        .collect();

    // ── 3. record_days_score ──
    // 소비 기록이 있는 날의 집합 (중복 제거)
    // HashSet 대신 Vec + dedup 로도 가능하지만 HashSet이 더 직관적
    let record_dates: std::collections::HashSet<NaiveDate> =
        expenses.iter().map(|e| e.expense_date).collect();
    let record_days_score = calc_record_days_score(record_dates.len());

    // ── 4. receipt_score ──
    // receipt_verified == true인 날짜 수 기준. 같은 날 여러 건은 하루치만 인정한다.
    let receipt_dates: std::collections::HashSet<NaiveDate> =
        verified_expenses.iter().map(|e| e.expense_date).collect();
    let receipt_score = calc_receipt_score(receipt_dates.len());

    // ── 5. diary_score ──
    // one_line_diary가 Some이고 비어있지 않은 날 수
    let diary_dates: std::collections::HashSet<NaiveDate> = expenses
        .iter()
        .filter(|e| {
            e.one_line_diary
                .as_deref()
                .map_or(false, |s| !s.trim().is_empty())
        })
        .map(|e| e.expense_date)
        .collect();
    let diary_score = calc_diary_score(diary_dates.len());

    // ── 6. budget_score ──
    // 해당 월 budgets 조회 → 월 예산 진행률 기준으로 평가
    // 예: 월 예산 * (평가일 / 해당 월 일수) = 현재까지 허용 소비
    // 월초부터 평가일까지의 누적 소비와 비교한다.
    let budget_score = {
        let today = Local::now().date_naive();
        let evaluation_date = target_date.min(today);
        let year = evaluation_date.year();
        let month = evaluation_date.month();

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
                    let budget_base_score = calc_budget_base_score(record_dates.len());
                    let month_start = evaluation_date
                        .with_day(1)
                        .ok_or_else(|| anyhow!("월 시작일 계산 실패"))?;
                    let next_month_start = if month == 12 {
                        NaiveDate::from_ymd_opt(year + 1, 1, 1)
                    } else {
                        NaiveDate::from_ymd_opt(year, month + 1, 1)
                    }
                    .ok_or_else(|| anyhow!("다음 달 시작일 계산 실패"))?;
                    let days_in_month = (next_month_start - month_start).num_days() as i32;
                    let elapsed_days = evaluation_date.day() as i32;
                    let allowed_spend =
                        b.total_budget.saturating_mul(elapsed_days) / days_in_month.max(1);

                    let spent_url = format!(
                        "{}/rest/v1/expenses?user_id=eq.{}&transaction_type=eq.expense&expense_date=gte.{}&expense_date=lte.{}&select=amount",
                        base_url, user_id, month_start, evaluation_date
                    );
                    let spent_res = state
                        .http_client
                        .get(&spent_url)
                        .header("Authorization", format!("Bearer {}", key))
                        .header("apikey", key.as_str())
                        .send()
                        .await
                        .context("월 예산 체크용 expenses SELECT 요청 실패")?;

                    if !spent_res.status().is_success() {
                        tracing::warn!(
                            "월 예산 체크용 expenses SELECT 실패, budget_score=0으로 처리: {}",
                            spent_res.text().await.unwrap_or_default()
                        );
                        0
                    } else {
                        #[derive(serde::Deserialize)]
                        struct MonthlyExpenseAmountRow {
                            amount: i32,
                        }

                        let monthly_expenses: Vec<MonthlyExpenseAmountRow> = spent_res
                            .json()
                            .await
                            .context("월 예산 체크용 expenses 역직렬화 실패")?;
                        let monthly_spent: i32 = monthly_expenses.iter().map(|e| e.amount).sum();

                        if allowed_spend <= 0 {
                            // 평가일 기준 허용 소비가 0 이하면 예산 설정이 사실상 유효하지 않음
                            0
                        } else {
                            let over = monthly_spent - allowed_spend;
                            if over <= 0 {
                                budget_base_score
                            } else {
                                let ratio = over as f64 / allowed_spend as f64;
                                if ratio < 0.10 {
                                    budget_base_score * 2 / 3
                                } else if ratio < 0.20 {
                                    budget_base_score / 3
                                } else {
                                    0
                                }
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
    let raw_total_score =
        record_days_score + receipt_score + diary_score + budget_score + streak_score;
    let total_score = raw_total_score.min(calc_total_score_cap(record_dates.len()));

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
        .header(
            "Prefer",
            "resolution=merge-duplicates,return=representation",
        )
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

    let upserted: Vec<WeeklyScore> = upsert_res
        .json()
        .await
        .context("weekly_scores UPSERT 역직렬화 실패")?;

    let score_row = upserted
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("weekly_scores UPSERT 결과가 비어있음"))?;

    // ── 10. 보상 지급 판단 ──
    // reward_granted가 false이고 total_score >= 60이면 SPT 지급
    let already_granted = score_row.reward_granted.unwrap_or(false);

    if !already_granted && total_score >= 60 {
        let wallet_address = get_wallet_address(state, user_id).await?;
        if wallet_address.is_some() && state.config.solana_admin_keypair.is_empty() {
            return Err(anyhow!(
                "지갑 연동 유저에게 NFT/SPT를 지급하려면 SOLANA_ADMIN_KEYPAIR가 필요합니다"
            ));
        }

        let claim_url = format!(
            "{}/rest/v1/weekly_scores?id=eq.{}&or=(reward_granted.is.false,reward_granted.is.null)",
            base_url, score_row.id
        );
        let claim_res = state
            .http_client
            .patch(&claim_url)
            .header("Authorization", format!("Bearer {}", key))
            .header("apikey", key.as_str())
            .header("Prefer", "return=representation")
            .json(&serde_json::json!({ "reward_granted": true }))
            .send()
            .await
            .context("weekly_scores reward claim 업데이트 실패")?;

        if !claim_res.status().is_success() {
            return Err(anyhow!(
                "weekly_scores reward claim 실패: {}",
                claim_res.text().await.unwrap_or_default()
            ));
        }

        let claimed_rows: Vec<WeeklyScore> = claim_res
            .json()
            .await
            .context("weekly_scores reward claim 역직렬화 실패")?;
        if claimed_rows.is_empty() {
            return Ok(WeeklyScoreResponse {
                id: score_row.id,
                week_start: score_row.week_start,
                record_days_score: score_row.record_days_score,
                receipt_score: score_row.receipt_score,
                diary_score: score_row.diary_score,
                budget_score: score_row.budget_score,
                streak_score: score_row.streak_score,
                total_score: score_row.total_score,
                reward_granted: Some(true),
            });
        }

        if wallet_address.is_none() {
            if let Err(e) = grant_untradeable_avatar_item(
                state,
                user_id,
                &format!(
                    "{}주차 성실도 점수 {}점 보상: 지갑 미연동으로 교환불가 아바타 지급",
                    week_start, total_score
                ),
            )
            .await
            {
                reset_weekly_reward_claim(state, score_row.id).await;
                return Err(e);
            }
            if let Err(e) = crate::notification::service::notify_reward_granted_if_needed(
                state,
                user_id,
                week_start,
                "이번 주 성실도 보상으로 새로운 아바타를 획득했어요.",
            )
            .await
            {
                tracing::warn!("주간 보상 알림 생성 실패: {}", e);
            }
            return Ok(WeeklyScoreResponse {
                id: score_row.id,
                week_start: score_row.week_start,
                record_days_score: score_row.record_days_score,
                receipt_score: score_row.receipt_score,
                diary_score: score_row.diary_score,
                budget_score: score_row.budget_score,
                streak_score: score_row.streak_score,
                total_score: score_row.total_score,
                reward_granted: Some(true),
            });
        }

        let wallet_address = wallet_address.expect("wallet_address is checked above");
        let base_spt = base_spt_from_score(total_score);
        // 반감기 적용
        let today = Local::now().date_naive();
        let actual_spt = calc_halving_reward(base_spt, state.config.service_launch_date, today);

        if let Err(e) = grant_nft_avatar_item(
            state,
            user_id,
            &wallet_address,
            score_row.id,
            &format!(
                "{}주차 성실도 점수 {}점 보상: NFT 아바타 지급",
                week_start, total_score
            ),
        )
        .await
        {
            reset_weekly_reward_claim(state, score_row.id).await;
            return Err(e);
        }

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

        // 온체인 SPT 민팅 — 지갑 연동 유저만 SPT 보상을 받는다.
        if !state.config.solana_admin_keypair.is_empty() {
            let amount_base_units = actual_spt as u64 * solana_client::SPT_DECIMALS;
            if let Err(e) = solana_client::mint_spt_to_user(
                &state.config.solana_rpc_url,
                &state.http_client,
                &state.config.solana_admin_keypair,
                &wallet_address,
                &state.config.solana_program_id,
                amount_base_units,
            )
            .await
            {
                tracing::error!(
                    "온체인 SPT 민팅 실패 (DB 보상은 유지): user={} err={}",
                    user_id,
                    e
                );
            }
        }

        if let Err(e) = crate::notification::service::notify_reward_granted_if_needed(
            state,
            user_id,
            week_start,
            &format!(
                "이번 주 성실도 보상으로 새로운 아바타와 {} SPT를 획득했어요.",
                actual_spt
            ),
        )
        .await
        {
            tracing::warn!("주간 보상 알림 생성 실패: {}", e);
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

pub async fn get_box_count(state: &AppState, user_id: Uuid) -> Result<BoxCountResponse> {
    let box_count = fetch_box_count(state, user_id).await?;
    let daily_earned = crate::expense::service::count_today_verified_receipts(state, user_id)
        .await
        .map_err(|e| anyhow!(e))? as i32;

    Ok(BoxCountResponse {
        box_count,
        daily_earned,
        daily_limit: crate::expense::service::DAILY_RECEIPT_LIMIT as i32,
    })
}

pub async fn increment_box_count(state: &AppState, user_id: Uuid) -> Result<i32> {
    for _ in 0..3 {
        let current = fetch_box_count(state, user_id).await?;
        match patch_box_count_if_current(state, user_id, current, current + 1).await? {
            Some(updated) => return Ok(updated),
            None => continue,
        }
    }

    Err(anyhow!("box_count 증가 충돌이 반복되었습니다"))
}

pub async fn open_box(state: &AppState, user_id: Uuid) -> Result<OpenBoxResponse> {
    let current = fetch_box_count(state, user_id).await?;
    if current <= 0 {
        return Err(anyhow!("열 수 있는 상자가 없습니다"));
    }

    let wallet_address = get_wallet_address(state, user_id).await?;
    let next_count = current - 1;
    let claimed_count = patch_box_count_if_current(state, user_id, current, next_count)
        .await?
        .ok_or_else(|| anyhow!("상자 개수가 변경되었습니다. 다시 시도해주세요"))?;

    let grant_result = match wallet_address {
        Some(wallet) => open_box_for_nft_user(state, user_id, &wallet).await,
        None => open_box_for_normal_user(state, user_id).await,
    };

    match grant_result {
        Ok(reward) => Ok(build_open_box_response(claimed_count, reward)),
        Err(e) => {
            if let Err(refund_err) = increment_box_count(state, user_id).await {
                tracing::error!(
                    "상자 보상 실패 후 box_count 복구 실패: user={} grant_err={} refund_err={}",
                    user_id,
                    e,
                    refund_err
                );
            }
            Err(e)
        }
    }
}

// 상자 보상 종류
enum BoxReward {
    Miss,
    Spt { amount: i32 },
    Avatar(UnityAvatarItemResponse),
}

fn build_open_box_response(remaining_box_count: i32, reward: BoxReward) -> OpenBoxResponse {
    match reward {
        BoxReward::Miss => OpenBoxResponse {
            remaining_box_count,
            reward_type: "miss".to_string(),
            is_win: false,
            message: "꽝입니다".to_string(),
            spt_amount: None,
            item: None,
        },
        BoxReward::Spt { amount } => OpenBoxResponse {
            remaining_box_count,
            reward_type: "spt".to_string(),
            is_win: true,
            message: format!("{} SPT를 획득했습니다", amount),
            spt_amount: Some(amount),
            item: None,
        },
        BoxReward::Avatar(item) => OpenBoxResponse {
            remaining_box_count,
            reward_type: "avatar".to_string(),
            is_win: true,
            message: "아바타를 획득했습니다".to_string(),
            spt_amount: None,
            item: Some(item),
        },
    }
}

// 지갑 미연동 유저: 90% 꽝 / 10% 일반 아바타(균등 랜덤, 중복 제외)
async fn open_box_for_normal_user(state: &AppState, user_id: Uuid) -> Result<BoxReward> {
    let roll = rand::thread_rng().gen_range(0..100);
    if roll < 90 {
        return Ok(BoxReward::Miss);
    }

    let items = fetch_box_items(state, "normal").await?;
    if items.is_empty() {
        return Err(anyhow!("지급 가능한 일반 아이템이 없습니다"));
    }

    let owned = fetch_owned_normal_item_ids(state, user_id).await?;
    let candidates: Vec<BoxItemRow> = items
        .into_iter()
        .filter(|item| !owned.contains(&item.id))
        .collect();

    // 줄 수 있는 미보유 아이템이 없으면 꽝 처리
    let Some(item) = candidates.choose(&mut rand::thread_rng()).cloned() else {
        return Ok(BoxReward::Miss);
    };

    let inventory_id =
        insert_user_inventory(state, user_id, item.id, false, None, None, None).await?;

    Ok(BoxReward::Avatar(UnityAvatarItemResponse {
        inventory_id,
        item_id: item.id,
        name: item.name,
        image_url: item.image_url,
        is_equipped: false,
        slot_name: item.category,
    }))
}

// 지갑 연동 유저: 70% 꽝 / 20% SPT(5~10) / 10% NFT 아바타(균등 랜덤)
async fn open_box_for_nft_user(
    state: &AppState,
    user_id: Uuid,
    wallet_address: &str,
) -> Result<BoxReward> {
    let roll = rand::thread_rng().gen_range(0..100);
    if roll < 70 {
        return Ok(BoxReward::Miss);
    }
    if roll < 90 {
        let amount = grant_box_spt(state, wallet_address).await?;
        return Ok(BoxReward::Spt { amount });
    }

    let items = fetch_box_items(state, "nft").await?;
    let Some(item) = items.choose(&mut rand::thread_rng()).cloned() else {
        return Err(anyhow!("지급 가능한 NFT 아이템이 없습니다"));
    };

    Ok(BoxReward::Avatar(
        mint_and_insert_box_nft_avatar_item(state, user_id, wallet_address, item).await?,
    ))
}

// 상자 SPT 보상 지급: 온체인 민팅만 수행 (DB에 저장하지 않음).
// SPT 잔액의 진실의 원천은 온체인이며, 프론트는 온체인 잔액을 직접 읽는다.
// 민팅 실패 시 에러를 던져 호출부(open_box)에서 box_count가 환불되게 한다.
// 반환: 지급한 SPT 수량
async fn grant_box_spt(state: &AppState, wallet_address: &str) -> Result<i32> {
    if state.config.solana_admin_keypair.is_empty() {
        return Err(anyhow!(
            "SPT 보상을 지급하려면 SOLANA_ADMIN_KEYPAIR가 필요합니다"
        ));
    }

    let amount: i32 = rand::thread_rng().gen_range(5..=10);
    let amount_base_units = amount as u64 * solana_client::SPT_DECIMALS;

    solana_client::mint_spt_to_user(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_admin_keypair,
        wallet_address,
        &state.config.solana_program_id,
        amount_base_units,
    )
    .await
    .context("상자 SPT 온체인 민팅 실패")?;

    Ok(amount)
}

async fn fetch_box_items(state: &AppState, item_type: &str) -> Result<Vec<BoxItemRow>> {
    let key = &state.config.supabase_secret_key;
    let base_url = state.config.supabase_url.trim_end_matches('/');

    let item_url = format!(
        "{}/rest/v1/item_master?item_type=eq.{}&drop_weight=gt.0&select=id,name,category,image_url,metadata_uri,drop_weight",
        base_url, item_type
    );
    let item_res = state
        .http_client
        .get(&item_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .send()
        .await
        .context("상자 아이템 후보 조회 요청 실패")?;

    if !item_res.status().is_success() {
        return Err(anyhow!(
            "상자 아이템 후보 조회 실패: {}",
            item_res.text().await.unwrap_or_default()
        ));
    }

    item_res
        .json()
        .await
        .context("상자 아이템 후보 역직렬화 실패")
}

async fn fetch_owned_normal_item_ids(
    state: &AppState,
    user_id: Uuid,
) -> Result<std::collections::HashSet<Uuid>> {
    let key = &state.config.supabase_secret_key;
    let base_url = state.config.supabase_url.trim_end_matches('/');

    let owned_url = format!(
        "{}/rest/v1/user_inventory?user_id=eq.{}&nft_mint_address=is.null&select=item_id",
        base_url, user_id
    );
    let owned_res = state
        .http_client
        .get(&owned_url)
        .header("Authorization", format!("Bearer {}", key))
        .header("apikey", key)
        .send()
        .await
        .context("보유 일반 아이템 조회 요청 실패")?;

    if !owned_res.status().is_success() {
        return Err(anyhow!(
            "보유 일반 아이템 조회 실패: {}",
            owned_res.text().await.unwrap_or_default()
        ));
    }

    #[derive(serde::Deserialize)]
    struct OwnedRow {
        item_id: Uuid,
    }

    let owned_rows: Vec<OwnedRow> = owned_res
        .json()
        .await
        .context("보유 일반 아이템 역직렬화 실패")?;
    Ok(owned_rows.into_iter().map(|row| row.item_id).collect())
}

async fn mint_and_insert_box_nft_avatar_item(
    state: &AppState,
    user_id: Uuid,
    wallet_address: &str,
    item: BoxItemRow,
) -> Result<UnityAvatarItemResponse> {
    if state.config.solana_admin_keypair.is_empty() {
        return Err(anyhow!(
            "NFT 상자 보상을 지급하려면 SOLANA_ADMIN_KEYPAIR가 필요합니다"
        ));
    }

    let item_seed = Uuid::new_v4().simple().to_string();
    let nft_uri = item
        .metadata_uri
        .as_deref()
        .filter(|v| !v.trim().is_empty())
        .unwrap_or(&item.image_url);
    let (tx_signature, nft_mint_address) = solana_client::mint_avatar_nft_to_user(
        &state.config.solana_rpc_url,
        &state.http_client,
        &state.config.solana_admin_keypair,
        wallet_address,
        &state.config.solana_program_id,
        &item_seed,
        &item.name,
        "SPTA",
        nft_uri,
    )
    .await
    .context("상자 NFT 아바타 민팅 실패")?;

    solana_client::check_signature_confirmed(
        &state.config.solana_rpc_url,
        &state.http_client,
        &tx_signature,
    )
    .await
    .context("상자 NFT 민팅 트랜잭션 확인 실패")?;

    let inventory_id = insert_user_inventory(
        state,
        user_id,
        item.id,
        true,
        Some(nft_mint_address),
        Some(tx_signature),
        Some(wallet_address.to_string()),
    )
    .await?;

    Ok(UnityAvatarItemResponse {
        inventory_id,
        item_id: item.id,
        name: item.name,
        image_url: item.image_url,
        is_equipped: false,
        slot_name: item.category,
    })
}

async fn fetch_box_count(state: &AppState, user_id: Uuid) -> Result<i32> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=box_count&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
    );

    let res = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .send()
        .await
        .context("users box_count 조회 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "users box_count 조회 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    #[derive(serde::Deserialize)]
    struct UserBoxRow {
        box_count: Option<i32>,
    }

    let rows: Vec<UserBoxRow> = res.json().await.context("users box_count 역직렬화 실패")?;
    rows.into_iter()
        .next()
        .map(|row| row.box_count.unwrap_or(0))
        .ok_or_else(|| anyhow!("users row를 찾을 수 없습니다"))
}

async fn patch_box_count_if_current(
    state: &AppState,
    user_id: Uuid,
    current: i32,
    next: i32,
) -> Result<Option<i32>> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&box_count=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        current
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({ "box_count": next }))
        .send()
        .await
        .context("users box_count UPDATE 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "users box_count UPDATE 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    #[derive(serde::Deserialize)]
    struct UserBoxRow {
        box_count: i32,
    }

    let rows: Vec<UserBoxRow> = res
        .json()
        .await
        .context("users box_count UPDATE 역직렬화 실패")?;
    Ok(rows.into_iter().next().map(|row| row.box_count))
}

#[derive(Clone, serde::Deserialize)]
struct BoxItemRow {
    id: Uuid,
    name: String,
    category: String,
    image_url: String,
    metadata_uri: Option<String>,
}

#[derive(serde::Deserialize)]
struct InventoryInsertRow {
    id: Uuid,
}

async fn insert_user_inventory(
    state: &AppState,
    user_id: Uuid,
    item_id: Uuid,
    is_nft: bool,
    nft_mint_address: Option<String>,
    nft_tx_signature: Option<String>,
    minted_to_wallet: Option<String>,
) -> Result<Uuid> {
    let url = format!(
        "{}/rest/v1/user_inventory",
        state.config.supabase_url.trim_end_matches('/')
    );
    let collection_mint = if is_nft {
        let value = state.config.solana_avatar_collection_mint.trim();
        if value.is_empty() {
            None
        } else {
            Some(value.to_string())
        }
    } else {
        None
    };

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "user_id": user_id,
            "item_id": item_id,
            "is_equipped": false,
            "is_nft": is_nft,
            "nft_mint_address": nft_mint_address,
            "nft_tx_signature": nft_tx_signature,
            "minted_to_wallet": minted_to_wallet,
            "collection_mint": collection_mint,
        }))
        .send()
        .await
        .context("user_inventory 상자 보상 INSERT 요청 실패")?;

    if !res.status().is_success() {
        return Err(anyhow!(
            "user_inventory 상자 보상 INSERT 실패: {}",
            res.text().await.unwrap_or_default()
        ));
    }

    let rows: Vec<InventoryInsertRow> = res
        .json()
        .await
        .context("user_inventory 상자 보상 INSERT 역직렬화 실패")?;
    rows.into_iter()
        .next()
        .map(|row| row.id)
        .ok_or_else(|| anyhow!("user_inventory 상자 보상 INSERT 결과가 비어있음"))
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
        _ => {
            return Err(anyhow!(
                "유효하지 않은 rank: {}. 1, 2, 3만 허용됩니다.",
                req.rank
            ));
        }
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
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

    if !state.config.solana_admin_keypair.is_empty() {
        let wallet_url = format!(
            "{}/rest/v1/users?id=eq.{}&select=wallet_address",
            state.config.supabase_url.trim_end_matches('/'),
            req.user_id
        );
        let wallet_res = state
            .http_client
            .get(&wallet_url)
            .header(
                "Authorization",
                format!("Bearer {}", state.config.supabase_secret_key),
            )
            .header("apikey", &state.config.supabase_secret_key)
            .send()
            .await
            .context("contest 보상 대상 지갑 조회 요청 실패")?;

        if wallet_res.status().is_success() {
            let rows: Vec<serde_json::Value> = wallet_res
                .json()
                .await
                .context("contest 보상 대상 지갑 역직렬화 실패")?;
            if let Some(wallet_addr) = rows
                .first()
                .and_then(|r| r["wallet_address"].as_str())
                .filter(|v| !v.trim().is_empty())
            {
                solana_client::mint_spt_to_user(
                    &state.config.solana_rpc_url,
                    &state.http_client,
                    &state.config.solana_admin_keypair,
                    wallet_addr,
                    &state.config.solana_program_id,
                    amount as u64 * solana_client::SPT_DECIMALS,
                )
                .await
                .context("contest 온체인 SPT 민팅 실패")?;
            }
        }
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
