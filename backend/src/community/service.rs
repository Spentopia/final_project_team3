// community/service.rs
// 콘테스트, 게시물, 투표, 챗봇 비즈니스 로직
//
// 챗봇:
//  백엔드가 클라이언트 역할로 AI 서버(FastAPI)를 호출한다.
//  대화 내용은 chatbot_logs에 저장.

use anyhow::{anyhow, Context, Result};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use crate::state::AppState;
use super::{
    dto::{
        ChatbotLogResponse, ChatRequest, ChatResponse, ContestEventResponse,
        CreatePostRequest, PostResponse,
    },
    model::{ChatbotLog, ContestEvent, Post},
};

// ── 콘테스트 목록 조회 ─────────────────────────────────────────

pub async fn list_contests(state: &AppState) -> Result<Vec<ContestEventResponse>> {
    let url = format!(
        "{}/rest/v1/contest_events?select=*&order=start_date.desc",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("contest_events SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("contest_events SELECT 실패: {}", body));
    }

    let events: Vec<ContestEvent> = res.json().await.context("contest_events 역직렬화 실패")?;
    Ok(events.into_iter().map(|e| ContestEventResponse {
        id: e.id,
        title: e.title,
        description: e.description,
        start_date: e.start_date,
        end_date: e.end_date,
        status: e.status,
        reward_description: e.reward_description,
    }).collect())
}

// ── 게시물 목록 조회 ───────────────────────────────────────────

pub async fn list_posts(
    state: &AppState,
    contest_id: Option<Uuid>,
) -> Result<Vec<PostResponse>> {
    let filter = match contest_id {
        Some(id) => format!("contest_id=eq.{}&", id),
        None => String::new(),
    };

    let url = format!(
        "{}/rest/v1/posts?{}select=*&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        filter,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("posts SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts SELECT 실패: {}", body));
    }

    let posts: Vec<Post> = res.json().await.context("posts 역직렬화 실패")?;
    Ok(posts.into_iter().map(|p| PostResponse {
        id: p.id,
        user_id: p.user_id,
        author_nickname: None,
        author_profile_image: None,
        contest_id: p.contest_id,
        image_url: p.image_url,
        content: p.content,
        vote_count: p.vote_count,
        created_at: p.created_at,
    }).collect())
}

// ── 게시물 생성 ───────────────────────────────────────────────

pub async fn create_post(
    state: &AppState,
    user_id: Uuid,
    req: CreatePostRequest,
) -> Result<PostResponse> {
    let url = format!(
        "{}/rest/v1/posts",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        contest_id: Option<Uuid>,
        image_url: String,
        content: Option<String>,
    }

    let res = state.http_client.post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertPayload {
            user_id,
            contest_id: req.contest_id,
            image_url: req.image_url,
            content: req.content,
        })
        .send().await.context("posts INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts INSERT 실패: {}", body));
    }

    let inserted: Vec<Post> = res.json().await.context("posts INSERT 역직렬화 실패")?;
    let post = inserted.into_iter().next()
        .ok_or_else(|| anyhow!("posts INSERT 결과가 비어있음"))?;

    Ok(PostResponse {
        id: post.id,
        user_id: post.user_id,
        author_nickname: None,
        author_profile_image: None,
        contest_id: post.contest_id,
        image_url: post.image_url,
        content: post.content,
        vote_count: post.vote_count,
        created_at: post.created_at,
    })
}

// ── 게시물 삭제 ───────────────────────────────────────────────

pub async fn delete_post(
    state: &AppState,
    user_id: Uuid,
    post_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/posts?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        post_id, user_id,
    );

    let res = state.http_client.delete(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("posts DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts DELETE 실패: {}", body));
    }
    Ok(())
}

// ── 게시물 투표 ───────────────────────────────────────────────

pub async fn vote_post(
    state: &AppState,
    user_id: Uuid,
    post_id: Uuid,
) -> Result<()> {
    let url = format!(
        "{}/rest/v1/votes",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload { user_id: Uuid, post_id: Uuid }

    let res = state.http_client.post(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&InsertPayload { user_id, post_id })
        .send().await.context("votes INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("votes INSERT 실패: {}", body));
    }
    Ok(())
}

// ── 챗봇 대화 ─────────────────────────────────────────────────
// 백엔드 → AI 서버 클라이언트 호출 후 chatbot_logs에 저장

pub async fn chat(
    state: &AppState,
    user_id: Uuid,
    req: ChatRequest,
) -> Result<ChatResponse> {
    // 1. AI 서버 호출 (ai_client 모듈로 중앙화)
    let ai_response = crate::clients::ai_client::chat(
        state,
        crate::clients::ai_client::ChatPayload {
            user_id: user_id.to_string(),
            message: req.message.clone(),
        },
    ).await?;

    // 2. chatbot_logs에 대화 기록 저장
    let log_url = format!(
        "{}/rest/v1/chatbot_logs",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct LogPayload {
        user_id: Uuid,
        user_message: String,
        bot_response: String,
    }

    let _ = state.http_client.post(&log_url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&LogPayload {
            user_id,
            user_message: req.message,
            bot_response: ai_response.response.clone(),
        })
        .send().await;

    Ok(ChatResponse { response: ai_response.response })

}

// ── 챗봇 대화 이력 조회 ────────────────────────────────────────

pub async fn list_chat_logs(
    state: &AppState,
    user_id: Uuid,
) -> Result<Vec<ChatbotLogResponse>> {
    let url = format!(
        "{}/rest/v1/chatbot_logs?user_id=eq.{}&select=*&order=created_at.desc",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    let res = state.http_client.get(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .send().await.context("chatbot_logs SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("chatbot_logs SELECT 실패: {}", body));
    }

    let logs: Vec<ChatbotLog> = res.json().await.context("chatbot_logs 역직렬화 실패")?;
    Ok(logs.into_iter().map(|l| ChatbotLogResponse {
        id: l.id,
        user_message: l.user_message,
        bot_response: l.bot_response,
        created_at: l.created_at,
    }).collect())
}
