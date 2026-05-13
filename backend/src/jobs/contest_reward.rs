// ─────────────────────────────────────────────────────────────
// 콘테스트 종료 후 수상자 자동 보상 배치
//
// 흐름:
//   1. status='active' 이고 end_date < 현재 시각인 콘테스트 조회
//   2. 즉시 status를 'ended'로 변경 → 중복 실행 방지
//   3. reaction_count 기준 상위 3개 게시글의 작성자에게 SPT 지급
//      - 1등: 500 SPT, 2등: 300 SPT, 3등: 150 SPT
// ─────────────────────────────────────────────────────────────

use std::collections::HashSet;

use anyhow::{Context, Result};
use serde::Deserialize;
use uuid::Uuid;

use crate::notification::service::create_notification;
use crate::reward::dto::ContestRewardRequest;
use crate::reward::service::grant_contest_reward;
use crate::state::AppState;

#[derive(Deserialize)]
struct ContestRow {
    id: Uuid,
    title: String,
}

#[derive(Deserialize)]
struct PostRow {
    user_id: Uuid,
}

/// 종료된 콘테스트 전체 처리 (스케줄러에서 호출)
pub async fn process_ended_contests(state: &AppState) -> Result<usize> {
    let contests = fetch_ended_active_contests(state).await?;
    let count = contests.len();

    for contest in contests {
        tracing::info!(
            "콘테스트 보상 처리 시작: {} ({})",
            contest.title,
            contest.id
        );

        // status를 먼저 ended로 변경 — 다음 배치에서 중복 처리 방지
        if let Err(e) = mark_contest_ended(state, contest.id).await {
            tracing::error!(
                "콘테스트 {} 상태 변경 실패, 보상 지급 건너뜀: {}",
                contest.id,
                e
            );
            continue;
        }

        let top_posts = match fetch_top_posts(state, contest.id).await {
            Ok(posts) => posts,
            Err(e) => {
                tracing::error!("콘테스트 {} 상위 게시글 조회 실패: {}", contest.id, e);
                continue;
            }
        };

        if top_posts.is_empty() {
            tracing::info!("콘테스트 {}: 참가 게시글 없음, 보상 생략", contest.id);
            continue;
        }

        let participant_user_ids = match fetch_participant_user_ids(state, contest.id).await {
            Ok(user_ids) => user_ids,
            Err(e) => {
                tracing::error!("콘테스트 {} 참가자 조회 실패: {}", contest.id, e);
                continue;
            }
        };
        let winner_user_ids: HashSet<Uuid> = top_posts.iter().map(|post| post.user_id).collect();

        for (idx, post) in top_posts.iter().enumerate() {
            let rank = (idx + 1) as u8;
            let req = ContestRewardRequest {
                user_id: post.user_id,
                rank,
            };
            match grant_contest_reward(state, req).await {
                Ok(reward) => {
                    tracing::info!(
                        "콘테스트 {} {}등 보상 지급 완료: user={}",
                        contest.id,
                        rank,
                        post.user_id
                    );

                    let notification_type =
                        format!("ct_win_{}_{}", &contest.id.to_string()[..8], rank);
                    let message = format!(
                        "축하합니다! 아바타 콘테스트 {}등을 수상해 {} SPT가 지급되었어요.",
                        rank, reward.amount
                    );
                    if let Err(e) =
                        create_notification(state, post.user_id, &notification_type, &message).await
                    {
                        tracing::error!(
                            "콘테스트 {} {}등 수상 알림 생성 실패: user={}, err={}",
                            contest.id,
                            rank,
                            post.user_id,
                            e
                        );
                    }
                }
                Err(e) => {
                    tracing::error!(
                        "콘테스트 {} {}등 보상 지급 실패: user={}, err={}",
                        contest.id,
                        rank,
                        post.user_id,
                        e
                    );
                }
            }
        }

        for user_id in participant_user_ids
            .into_iter()
            .filter(|user_id| !winner_user_ids.contains(user_id))
        {
            let notification_type = format!("ct_out_{}", &contest.id.to_string()[..8]);
            let message = "이번 콘테스트는 순위권에 들지 못했어요. 다음 도전을 기다릴게요.";
            if let Err(e) = create_notification(state, user_id, &notification_type, message).await {
                tracing::error!(
                    "콘테스트 {} 비수상 알림 생성 실패: user={}, err={}",
                    contest.id,
                    user_id,
                    e
                );
            }
        }
    }

    Ok(count)
}

/// status='active' AND end_date < now() 인 콘테스트 조회
async fn fetch_ended_active_contests(state: &AppState) -> Result<Vec<ContestRow>> {
    let now = chrono::Utc::now().to_rfc3339();
    let url = format!(
        "{}/rest/v1/contest_events?status=eq.active&end_date=lt.{}&select=id,title",
        state.config.supabase_url.trim_end_matches('/'),
        now,
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("종료 콘테스트 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow::anyhow!("종료 콘테스트 조회 실패: {}", body));
    }

    res.json::<Vec<ContestRow>>()
        .await
        .context("종료 콘테스트 응답 파싱 실패")
}

/// 콘테스트 status를 'ended'로 변경
async fn mark_contest_ended(state: &AppState, contest_id: Uuid) -> Result<()> {
    let url = format!(
        "{}/rest/v1/contest_events?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        contest_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&serde_json::json!({ "status": "ended" }))
        .send()
        .await
        .context("콘테스트 상태 변경 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow::anyhow!("콘테스트 상태 변경 실패: {}", body));
    }

    Ok(())
}

/// 콘테스트 게시글 중 reaction_count 상위 3개 조회
async fn fetch_top_posts(state: &AppState, contest_id: Uuid) -> Result<Vec<PostRow>> {
    let url = format!(
        "{}/rest/v1/posts?contest_id=eq.{}&is_deleted=eq.false&select=user_id&order=reaction_count.desc,created_at.asc&limit=3",
        state.config.supabase_url.trim_end_matches('/'),
        contest_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("콘테스트 상위 게시글 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow::anyhow!("콘테스트 상위 게시글 조회 실패: {}", body));
    }

    res.json::<Vec<PostRow>>()
        .await
        .context("콘테스트 상위 게시글 응답 파싱 실패")
}

/// 콘테스트에 참여한 사용자 전체 조회
async fn fetch_participant_user_ids(state: &AppState, contest_id: Uuid) -> Result<HashSet<Uuid>> {
    let url = format!(
        "{}/rest/v1/posts?contest_id=eq.{}&is_deleted=eq.false&select=user_id",
        state.config.supabase_url.trim_end_matches('/'),
        contest_id,
    );

    let res = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
        .context("콘테스트 참가자 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow::anyhow!("콘테스트 참가자 조회 실패: {}", body));
    }

    let posts = res
        .json::<Vec<PostRow>>()
        .await
        .context("콘테스트 참가자 응답 파싱 실패")?;

    Ok(posts.into_iter().map(|post| post.user_id).collect())
}
