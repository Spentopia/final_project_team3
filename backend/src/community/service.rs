// community/service.rs
// 콘테스트, 게시물, 투표, 챗봇 비즈니스 로직
//
// 챗봇:
//  백엔드가 클라이언트 역할로 AI 서버(FastAPI)를 호출한다.
//  대화 내용은 chatbot_logs에 저장.

use anyhow::{Context, Result, anyhow};
use axum::extract::Multipart;
use chrono::Utc;
use serde::{Deserialize, Serialize};
use serde_json::{Map, Value};
use std::collections::{HashMap, HashSet};
use uuid::Uuid;

use super::{
    dto::{
        ChatResponse, CommentResponse, ContentReportResponse, ContestEventResponse,
        CreateCommentRequest, CreateContentReportRequest, CreatePostRequest, PostListResponse,
        PostResponse, PostSort, PostType, UpdateCommentRequest, UpdatePostRequest,
        UploadCommunityImageResponse,
    },
    model::{ChatbotLog, Comment, ContentReport, ContestEvent, Post},
};
use crate::{filter, state::AppState};

const POST_TITLE_MAX_LENGTH: usize = 200;
const POST_CONTENT_MAX_LENGTH: usize = 500;
const NOTIFICATION_TITLE_MAX_LENGTH: usize = 18;

// reactions 테이블에서 post_id만 가져오기 위한 응답 구조체.
//
// Supabase REST에서 select=post_id만 조회할 것이기 때문에
// 기존 model::Reaction을 그대로 쓰면 안 된다.
// Reaction 구조체는 id, user_id, voted_at 같은 필드도 기대하기 때문에
// select=post_id 결과를 역직렬화할 때 필드 부족 문제가 생길 수 있다.
//
// 그래서 목록 조회 최적화용으로 post_id만 받는 작은 구조체를 따로 둔다.
#[derive(Debug, Deserialize)]
struct ReactedPostIdRow {
    pub post_id: Uuid,
}

async fn create_profile_image_signed_url(state: &AppState, path: &str) -> Result<String> {
    let path = path.trim();
    if path.is_empty() {
        return Err(anyhow!("프로필 이미지 path가 비어 있습니다"));
    }

    let url = format!(
        "{}/storage/v1/object/sign/{}/{}",
        state.config.supabase_url.trim_end_matches('/'),
        state.config.supabase_profile_image_bucket,
        path,
    );

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .json(&serde_json::json!({ "expiresIn": 60 * 60 * 24 }))
        .send()
        .await
        .context("작성자 프로필 이미지 signed URL 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("작성자 프로필 이미지 signed URL 실패: {}", body));
    }

    let body: Value = res
        .json()
        .await
        .context("작성자 프로필 이미지 signed URL 응답 파싱 실패")?;

    let signed_url = body
        .get("signedURL")
        .or_else(|| body.get("signed_url"))
        .and_then(|value| value.as_str())
        .ok_or_else(|| anyhow!("작성자 프로필 이미지 signed URL 응답이 비어 있습니다"))?;

    if signed_url.starts_with("http") {
        Ok(signed_url.to_string())
    } else {
        Ok(format!(
            "{}/storage/v1{}",
            state.config.supabase_url.trim_end_matches('/'),
            signed_url
        ))
    }
}

// ─────────────────────────────────────────────────────────────
// 게시글 응답 변환 헬퍼
// ─────────────────────────────────────────────────────────────
//
// Post 엔티티(DB 조회 결과)를 프론트에 내려줄 PostResponse로 변환한다.
//
// 여기서 중요한 점:
// - 게시글 목록 조회(list_posts)에서는 N+1 방지를 위해
//   is_reacted 값을 이미 한 번에 계산해서 넘긴다.
// - 게시글 상세/생성/수정 같은 단건 조회에서는
//   기존처럼 해당 post_id 하나에 대해서만 reactions를 조회해도 부담이 작다.
//
// 그래서 변환 함수를 2개로 나눈다.
//
// 1) to_post_response_with_reacted()
//    - is_reacted 값을 밖에서 받아서 그대로 응답에 넣음
//    - 목록 조회에서 사용
//
// 2) to_post_response()
//    - 내부에서 is_reacted_by_user()를 호출함
//    - 상세/생성/수정 같은 단건 조회에서 사용
// ─────────────────────────────────────────────────────────────

async fn to_post_response_with_reacted(
    state: &AppState,
    post: Post,
    is_reacted: bool,
    profile_signed_url_cache: &mut HashMap<String, Option<String>>,
) -> Result<PostResponse> {
    // posts 조회 시 users!posts_user_id_fkey로 같이 가져온 작성자 정보.
    // 즉, 작성자 nickname/profile_image는 별도 users 조회를 하지 않는다.
    let author_nickname = post
        .users
        .as_ref()
        .and_then(|author| author.nickname.clone());

    let author_profile_image = post
        .users
        .as_ref()
        .and_then(|author| author.profile_image.clone());

    // ─────────────────────────────────────────────
    // 프로필 이미지 signed URL 캐싱
    // ─────────────────────────────────────────────
    //
    // 기존 문제:
    // - 게시글마다 create_profile_image_signed_url() 호출
    // - 같은 작성자가 글을 여러 개 썼거나,
    //   같은 profile_image path가 반복되면 같은 signed URL 요청이 여러 번 나감
    //
    // 개선:
    // - HashMap<String, Option<String>>을 path 기준 캐시로 사용
    // - 같은 path가 이미 캐시에 있으면 Supabase Storage 요청을 다시 보내지 않음
    //
    // 예:
    // - 같은 작성자의 글 5개
    // - 기존: signed URL 요청 5번
    // - 변경: signed URL 요청 1번
    let author_profile_image_url = match author_profile_image.as_deref() {
        Some(path) => {
            if let Some(cached_url) = profile_signed_url_cache.get(path) {
                cached_url.clone()
            } else {
                let signed_url = create_profile_image_signed_url(state, path).await.ok();
                profile_signed_url_cache.insert(path.to_string(), signed_url.clone());
                signed_url
            }
        }
        None => None,
    };

    Ok(PostResponse {
        id: post.id,
        user_id: post.user_id,
        author_nickname,
        author_profile_image,
        author_profile_image_url,
        contest_id: post.contest_id,
        post_type: post.post_type,
        title: post.title,
        image_url: post.image_url,
        content: post.content,
        reaction_count: post.reaction_count,
        is_reacted,
        view_count: post.view_count,
        created_at: post.created_at,
    })
}

async fn to_post_response(
    state: &AppState,
    current_user_id: Uuid,
    post: Post,
) -> Result<PostResponse> {
    // 단건 조회용 함수.
    //
    // 상세 조회, 게시글 생성 직후 응답, 게시글 수정 직후 응답에서는
    // 게시글이 1개뿐이라 reactions를 1번 조회해도 N+1 문제가 거의 없다.
    let is_reacted = is_reacted_by_user(state, current_user_id, post.id).await?;

    // 단건 응답에서도 아래 공통 변환 함수를 사용하기 위해 캐시를 만든다.
    // 단건에서는 캐시 효과가 크진 않지만, 함수 시그니처를 맞추기 위한 용도다.
    let mut profile_signed_url_cache: HashMap<String, Option<String>> = HashMap::new();

    to_post_response_with_reacted(state, post, is_reacted, &mut profile_signed_url_cache).await
}

async fn to_comment_response_with_cache(
    state: &AppState,
    comment: Comment,
    profile_signed_url_cache: &mut HashMap<String, Option<String>>,
) -> CommentResponse {
    // 작성자가 탈퇴자(users.deleted_at IS NOT NULL)면 가공
    // - 닉네임: "탈퇴한 회원"
    // - 프로필 이미지: 기본 이미지
    // - 내용: "[작성자가 탈퇴했습니다]"
    //
    // 탈퇴자는 signed URL을 만들지 않는다.
    let is_author_withdrawn = comment
        .users
        .as_ref()
        .and_then(|author| author.deleted_at)
        .is_some();

    if is_author_withdrawn {
        return CommentResponse {
            id: comment.id,
            post_id: comment.post_id,
            parent_id: comment.parent_id,
            user_id: comment.user_id,
            author_nickname: Some("탈퇴한 회원".to_string()),
            author_profile_image: Some("defaults/avatar.png".to_string()),
            author_profile_image_url: None,
            content: "[작성자가 탈퇴했습니다]".to_string(),
            created_at: comment.created_at,
            updated_at: comment.updated_at,
        };
    }

    let author_nickname = comment
        .users
        .as_ref()
        .and_then(|author| author.nickname.clone());

    let author_profile_image = comment
        .users
        .as_ref()
        .and_then(|author| author.profile_image.clone());

    // ─────────────────────────────────────────────
    // 댓글 작성자 프로필 signed URL 캐싱
    // ─────────────────────────────────────────────
    //
    // 기존:
    // - 댓글마다 create_profile_image_signed_url() 호출 가능
    //
    // 개선:
    // - 같은 profile_image path는 한 번만 signed URL 생성
    // - 같은 사용자가 댓글/대댓글을 여러 개 달아도 요청 수 감소
    let author_profile_image_url = match author_profile_image.as_deref() {
        Some(path) => {
            if let Some(cached_url) = profile_signed_url_cache.get(path) {
                cached_url.clone()
            } else {
                let signed_url = create_profile_image_signed_url(state, path).await.ok();
                profile_signed_url_cache.insert(path.to_string(), signed_url.clone());
                signed_url
            }
        }
        None => None,
    };

    CommentResponse {
        id: comment.id,
        post_id: comment.post_id,
        parent_id: comment.parent_id,
        user_id: comment.user_id,
        author_nickname,
        author_profile_image,
        author_profile_image_url,
        content: comment.content,
        created_at: comment.created_at,
        updated_at: comment.updated_at,
    }
}

async fn to_comment_response(state: &AppState, comment: Comment) -> CommentResponse {
    // 댓글 생성/수정처럼 댓글 1개만 응답하는 경우를 위한 wrapper.
    // 단건에서는 캐시 효과가 크지 않지만, 기존 호출부를 크게 안 바꾸기 위해 유지한다.
    let mut profile_signed_url_cache: HashMap<String, Option<String>> = HashMap::new();

    to_comment_response_with_cache(state, comment, &mut profile_signed_url_cache).await
}

async fn ensure_admin(state: &AppState, user_id: Uuid) -> Result<()> {
    let role = crate::auth::service::get_user_role(state, user_id).await?;
    if role.as_deref() != Some("admin") {
        return Err(anyhow!("관리자 권한이 필요합니다."));
    }
    Ok(())
}

async fn get_post(state: &AppState, post_id: Uuid) -> Result<Post> {
    // ?users.deleted_at=is.null 필터로 탈퇴자 게시글 차단
    // 이거 하나로 게시글 상세/수정/삭제/반응/댓글 진입 다 자동으로 404 처리됨
    // is_hidden=eq.false 추가:
    //   신고 자동 숨김된 게시글은 상세/수정/삭제/반응/댓글/신고 진입에서
    //   전부 "게시물을 찾을 수 없습니다"로 막힌다.
    //   get_post가 이 모든 흐름에서 공용으로 쓰이기 때문에 여기 한 곳만 막으면 됨.
    let url = format!(
        "{}/rest/v1/posts?id=eq.{}&is_deleted=eq.false&is_hidden=eq.false&users.deleted_at=is.null&select=*,users!posts_user_id_fkey!inner(nickname,profile_image,deleted_at)&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        post_id,
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
        .context("posts 단건 SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts 단건 SELECT 실패: {}", body));
    }

    let posts: Vec<Post> = res.json().await.context("posts 단건 역직렬화 실패")?;
    posts
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("게시물을 찾을 수 없습니다."))
}

async fn get_user_nickname_for_notification(state: &AppState, user_id: Uuid) -> String {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&select=nickname&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    #[derive(Deserialize)]
    struct NicknameRow {
        nickname: Option<String>,
    }

    let Ok(res) = state
        .http_client
        .get(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .send()
        .await
    else {
        return "누군가".to_string();
    };

    if !res.status().is_success() {
        return "누군가".to_string();
    }

    let Ok(rows) = res.json::<Vec<NicknameRow>>().await else {
        return "누군가".to_string();
    };

    rows.into_iter()
        .next()
        .and_then(|row| row.nickname)
        .map(|nickname| nickname.trim().to_string())
        .filter(|nickname| !nickname.is_empty())
        .unwrap_or_else(|| "누군가".to_string())
}

fn truncate_notification_title(title: &str) -> String {
    let trimmed = title.trim();
    if trimmed.chars().count() <= NOTIFICATION_TITLE_MAX_LENGTH {
        return trimmed.to_string();
    }

    let shortened = trimmed
        .chars()
        .take(NOTIFICATION_TITLE_MAX_LENGTH)
        .collect::<String>();

    format!("{}...", shortened)
}

// 현재 로그인한 사용자가 "현재 페이지에 표시된 게시글들 중"
// 어떤 게시글에 이미 반응했는지 한 번에 가져온다.
//
// 기존 문제:
// - list_posts에서 게시글 10개 조회
// - 각 게시글마다 is_reacted_by_user() 호출
// - reactions SELECT 10번 발생
//
// 개선 후:
// - list_posts에서 게시글 10개 조회
// - post_id 10개를 모음
// - reactions를 post_id=in.(...) 조건으로 1번만 조회
// - 결과를 HashSet<Uuid>로 만들어서 contains()로 빠르게 확인
//
// 결과:
// - 게시글 10개여도 reactions 조회 1번
// - 게시글 50개여도 reactions 조회 1번
async fn get_reacted_post_ids_by_user(
    state: &AppState,
    user_id: Uuid,
    post_ids: &[Uuid],
) -> Result<HashSet<Uuid>> {
    // 현재 페이지에 게시글이 하나도 없으면 reactions를 조회할 필요가 없다.
    // 빈 in.() 쿼리를 만들면 Supabase/PostgREST 쪽에서 에러가 날 수 있으므로
    // 여기서 바로 빈 HashSet을 반환한다.
    if post_ids.is_empty() {
        return Ok(HashSet::new());
    }

    // Supabase REST의 in 필터 형식:
    //
    // post_id=in.(uuid1,uuid2,uuid3)
    //
    // UUID 문자열은 특수문자가 없어서 별도 urlencoding 없이 사용해도 된다.
    let post_ids_filter = post_ids
        .iter()
        .map(Uuid::to_string)
        .collect::<Vec<String>>()
        .join(",");

    let url = format!(
        "{}/rest/v1/reactions?user_id=eq.{}&post_id=in.({})&select=post_id",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        post_ids_filter,
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
        .context("reactions 일괄 SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("reactions 일괄 SELECT 실패: {}", body));
    }

    let rows: Vec<ReactedPostIdRow> = res
        .json()
        .await
        .context("reactions 일괄 SELECT 응답 역직렬화 실패")?;

    // HashSet으로 바꾸면 아래처럼 O(1)에 가깝게 확인 가능하다.
    //
    // reacted_post_ids.contains(&post.id)
    Ok(rows.into_iter().map(|row| row.post_id).collect())
}

// 현재 로그인한 사용자가 특정 게시글에 이미 반응했는지 확인한다.
//
// reactions 테이블에는 user_id + post_id가 저장된다.
// 해당 row가 있으면:
// - contest: 이미 투표함
// - free/request: 이미 좋아요 누름
async fn is_reacted_by_user(state: &AppState, user_id: Uuid, post_id: Uuid) -> Result<bool> {
    let url = format!(
        "{}/rest/v1/reactions?user_id=eq.{}&post_id=eq.{}&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        post_id,
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
        .context("reactions SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("reactions SELECT 실패: {}", body));
    }

    let rows: Vec<Value> = res
        .json()
        .await
        .context("reactions SELECT 응답 역직렬화 실패")?;

    Ok(!rows.is_empty())
}

async fn get_comment(state: &AppState, comment_id: Uuid) -> Result<Comment> {
    // is_hidden=eq.false 추가:
    //   자동 숨김된 댓글은 단건 조회/수정/삭제/대댓글 작성 검증에서 제외된다.
    let url = format!(
        "{}/rest/v1/comments?id=eq.{}&is_deleted=eq.false&is_hidden=eq.false&select=*,users!comments_user_id_fkey(nickname,profile_image,deleted_at)&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        comment_id,
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
        .context("comments 단건 SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("comments 단건 SELECT 실패: {}", body));
    }

    let comments: Vec<Comment> = res.json().await.context("comments 단건 역직렬화 실패")?;
    comments
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("댓글을 찾을 수 없습니다."))
}

async fn increment_post_view_count(state: &AppState, post_id: Uuid) -> Result<i32> {
    let url = format!(
        "{}/rest/v1/rpc/increment_post_view_count",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Content-Type", "application/json")
        .json(&serde_json::json!({
            "p_post_id": post_id
        }))
        .send()
        .await
        .context("posts view_count RPC 요청 실패")?;

    if !res.status().is_success() {
        let status = res.status();
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts view_count RPC 실패: {} {}", status, body));
    }

    res.json::<i32>()
        .await
        .context("posts view_count RPC 응답 역직렬화 실패")
}

async fn ensure_can_modify_post(state: &AppState, user_id: Uuid, post: &Post) -> Result<()> {
    if post.post_type == "notice" {
        ensure_admin(state, user_id).await?;
        return Ok(());
    }

    if post.user_id != user_id {
        return Err(anyhow!("본인 게시글만 수정/삭제할 수 있습니다."));
    }

    Ok(())
}

fn validate_post_type(req: &CreatePostRequest) -> Result<()> {
    if req.title.trim().is_empty() {
        return Err(anyhow!("title은 비어 있을 수 없습니다."));
    }

    match req.post_type {
        PostType::Notice => {
            if req.contest_id.is_some() {
                return Err(anyhow!("공지사항에는 contest_id를 넣을 수 없습니다."));
            }
        }
        PostType::Request => {
            if req.contest_id.is_some() {
                return Err(anyhow!(
                    "이거 만들어주세요 게시글에는 contest_id를 넣을 수 없습니다."
                ));
            }
        }
        PostType::Free => {
            if req.contest_id.is_some() {
                return Err(anyhow!("자유 게시글에는 contest_id를 넣을 수 없습니다."));
            }
        }
        PostType::Contest => {
            if req.contest_id.is_none() {
                return Err(anyhow!(
                    "아바타 콘테스트 게시글에는 contest_id가 필요합니다."
                ));
            }

            if req
                .image_url
                .as_deref()
                .map(str::trim)
                .unwrap_or("")
                .is_empty()
            {
                return Err(anyhow!("아바타 콘테스트 게시글에는 이미지가 필요합니다."));
            }
        }
    }

    Ok(())
}

fn validate_post_text(title: Option<&str>, content: Option<&str>) -> Result<()> {
    if let Some(title) = title {
        let title = title.trim();
        if title.is_empty() {
            return Err(anyhow!("title은 비어 있을 수 없습니다."));
        }
        if title.chars().count() > POST_TITLE_MAX_LENGTH {
            return Err(anyhow!(
                "게시글 제목은 {}자 이내로 입력해주세요.",
                POST_TITLE_MAX_LENGTH
            ));
        }
        if !filter::check(title) {
            return Err(anyhow!(
                "게시글 제목에 사용할 수 없는 표현이 포함되어 있습니다."
            ));
        }
    }

    if let Some(content) = content {
        let content = content.trim();
        if content.is_empty() {
            return Err(anyhow!("content는 비어 있을 수 없습니다."));
        }
        if content.chars().count() > POST_CONTENT_MAX_LENGTH {
            return Err(anyhow!(
                "게시글 내용은 {}자 이내로 입력해주세요.",
                POST_CONTENT_MAX_LENGTH
            ));
        }
        if !filter::check(content) {
            return Err(anyhow!(
                "게시글 내용에 사용할 수 없는 표현이 포함되어 있습니다."
            ));
        }
    }

    Ok(())
}

fn validate_comment_content(content: &str) -> Result<String> {
    let content = content.trim();

    if content.is_empty() {
        return Err(anyhow!("댓글 내용은 비어 있을 수 없습니다."));
    }

    if !filter::check(content) {
        return Err(anyhow!(
            "댓글 내용에 사용할 수 없는 표현이 포함되어 있습니다."
        ));
    }

    Ok(content.to_string())
}

const CHOSEONG_COMPAT: [char; 19] = [
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ', 'ㅋ',
    'ㅌ', 'ㅍ', 'ㅎ',
];

fn hangul_choseong(value: char) -> Option<char> {
    let code = value as u32;
    if !(0xAC00..=0xD7A3).contains(&code) {
        return None;
    }

    let index = ((code - 0xAC00) / 588) as usize;
    Some(CHOSEONG_COMPAT[index])
}

fn contains_choseong_query(value: &str) -> bool {
    value.chars().any(|ch| CHOSEONG_COMPAT.contains(&ch))
}

fn title_matches_choseong(title: &str, query: &str) -> bool {
    let title_choseong: String = title
        .chars()
        .filter_map(|ch| {
            if CHOSEONG_COMPAT.contains(&ch) {
                Some(ch)
            } else {
                hangul_choseong(ch)
            }
        })
        .collect();
    title_choseong.contains(query)
}

fn normalize_optional_string(value: Option<String>) -> Option<String> {
    value
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty())
}

fn extension_from_content_type(content_type: &str) -> Result<&'static str> {
    match content_type {
        "image/png" => Ok("png"),
        "image/jpeg" | "image/jpg" => Ok("jpg"),
        "image/webp" => Ok("webp"),
        _ => Err(anyhow!("png, jpg, webp 이미지만 업로드 가능합니다.")),
    }
}

fn build_upload_path(
    post_type: &str,
    contest_id: Option<&str>,
    post_id: Option<&str>,
    user_id: Uuid,
    extension: &str,
) -> Result<String> {
    let file_id = Uuid::new_v4();

    match post_type {
        "contest" => {
            let contest_id =
                contest_id.ok_or_else(|| anyhow!("contest 이미지에는 contest_id가 필요합니다."))?;
            Ok(format!(
                "contest/{}/{}/{}.{}",
                contest_id, user_id, file_id, extension
            ))
        }
        "notice" => {
            let post_id =
                post_id.ok_or_else(|| anyhow!("공지사항 이미지에는 post_id가 필요합니다."))?;
            Ok(format!("notices/{}/{}.{}", post_id, file_id, extension))
        }
        "request" => Ok(format!("requests/{}/{}.{}", user_id, file_id, extension)),
        "free" => Ok(format!("free/{}/{}.{}", user_id, file_id, extension)),
        _ => Err(anyhow!("지원하지 않는 post_type입니다.")),
    }
}

// ── 커뮤니티 이미지 업로드 ─────────────────────────────────────

pub async fn upload_post_image(
    state: &AppState,
    user_id: Uuid,
    mut multipart: Multipart,
) -> Result<UploadCommunityImageResponse> {
    let mut file_bytes: Option<Vec<u8>> = None;
    let mut content_type: Option<String> = None;
    let mut post_type: Option<String> = None;
    let mut contest_id: Option<String> = None;
    let mut post_id: Option<String> = None;

    while let Some(field) = multipart
        .next_field()
        .await
        .context("멀티파트 파싱에 실패했습니다.")?
    {
        let name = field.name().unwrap_or_default().to_string();

        match name.as_str() {
            "file" => {
                if let Some(ct) = field.content_type() {
                    content_type = Some(ct.to_string());
                }

                let bytes = field.bytes().await.context("파일 읽기에 실패했습니다.")?;
                if bytes.len() > 5 * 1024 * 1024 {
                    return Err(anyhow!("파일 크기는 5MB 이하여야 합니다."));
                }

                file_bytes = Some(bytes.to_vec());
            }
            "post_type" => {
                post_type = Some(
                    field
                        .text()
                        .await
                        .context("post_type 필드 읽기에 실패했습니다.")?
                        .trim()
                        .to_string(),
                );
            }
            "contest_id" => {
                let value = field
                    .text()
                    .await
                    .context("contest_id 필드 읽기에 실패했습니다.")?
                    .trim()
                    .to_string();
                if !value.is_empty() {
                    contest_id = Some(value);
                }
            }
            "post_id" => {
                let value = field
                    .text()
                    .await
                    .context("post_id 필드 읽기에 실패했습니다.")?
                    .trim()
                    .to_string();
                if !value.is_empty() {
                    post_id = Some(value);
                }
            }
            _ => {}
        }
    }

    let file_bytes = file_bytes.ok_or_else(|| anyhow!("file 필드가 없습니다."))?;
    let content_type = content_type.unwrap_or_else(|| "image/png".to_string());
    let extension = extension_from_content_type(&content_type)?;
    let post_type = post_type.ok_or_else(|| anyhow!("post_type은 필수입니다."))?;

    if post_type == "notice" {
        ensure_admin(state, user_id).await?;
    }

    let object_path = build_upload_path(
        &post_type,
        contest_id.as_deref(),
        post_id.as_deref(),
        user_id,
        extension,
    )?;

    let upload_url = format!(
        "{}/storage/v1/object/{}/{}",
        state.config.supabase_url.trim_end_matches('/'),
        state.config.supabase_community_bucket,
        object_path
    );

    let res = state
        .http_client
        .post(&upload_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Content-Type", content_type)
        .header("x-upsert", "false")
        .body(file_bytes)
        .send()
        .await
        .context("스토리지 업로드에 실패했습니다.")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("스토리지 업로드 실패: {}", body));
    }

    Ok(UploadCommunityImageResponse { path: object_path })
}

// ── 콘테스트 목록 조회 ─────────────────────────────────────────

pub async fn list_contests(state: &AppState) -> Result<Vec<ContestEventResponse>> {
    let url = format!(
        "{}/rest/v1/contest_events?select=*&status=in.(active,upcoming)&order=start_date.desc",
        state.config.supabase_url.trim_end_matches('/'),
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
        .context("contest_events SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("contest_events SELECT 실패: {}", body));
    }

    let events: Vec<ContestEvent> = res.json().await.context("contest_events 역직렬화 실패")?;
    Ok(events
        .into_iter()
        .map(|e| ContestEventResponse {
            id: e.id,
            title: e.title,
            description: e.description,
            start_date: e.start_date,
            end_date: e.end_date,
            status: e.status,
            reward_description: e.reward_description,
        })
        .collect())
}

// ── 게시물 목록 조회 ───────────────────────────────────────────
//
// N+1 개선 포인트:
//
// 기존 구조:
// 1. posts 목록 조회 1번
// 2. for post in posts
// 3. to_post_response()
// 4. is_reacted_by_user()
// 5. reactions를 게시글마다 1번씩 조회
//
// 예:
// - page_size = 10이면 reactions 조회 10번
// - page_size = 50이면 reactions 조회 50번
//
// 개선 구조:
// 1. posts 목록 조회 1번
// 2. 현재 페이지의 post_id 목록 수집
// 3. reactions를 post_id=in.(...)으로 1번만 조회
// 4. HashSet<Uuid>로 바꾼 뒤 각 post의 is_reacted를 contains()로 계산
//
// 결과:
// - page_size가 10이어도 reactions 조회 1번
// - page_size가 50이어도 reactions 조회 1번

pub async fn list_posts(
    state: &AppState,
    user_id: Uuid,
    contest_id: Option<Uuid>,
    post_type: Option<PostType>,
    sort: PostSort,
    title: Option<String>,
    page: u32,
    page_size: u32,
) -> Result<PostListResponse> {
    // page는 최소 1부터 시작하게 강제한다.
    // page=0 같은 잘못된 값이 들어오면 offset 계산이 꼬일 수 있기 때문.
    let page = page.max(1);

    // page_size는 최소 1, 최대 50으로 제한한다.
    // 너무 큰 page_size를 허용하면 한 번에 너무 많은 데이터를 조회해서
    // DB/백엔드/프론트 모두 부담이 커진다.
    let page_size = page_size.clamp(1, 50);

    // Supabase REST Range는 0-based index를 사용한다.
    //
    // 예:
    // page=1, page_size=10 => 0-9
    // page=2, page_size=10 => 10-19
    let offset = (page - 1) * page_size;
    let end = offset + page_size - 1;

    // title 검색어는 앞뒤 공백을 제거하고,
    // 빈 문자열이면 None으로 바꾼다.
    let search_title = normalize_optional_string(title);

    // 초성 검색인지 판단한다.
    //
    // 일반 검색:
    // - DB에서 title=ilike.*검색어* 필터 가능
    //
    // 초성 검색:
    // - DB에서 바로 처리하기 애매해서 일단 posts를 가져온 뒤
    // - Rust에서 title_matches_choseong()으로 필터링한다.
    let use_choseong_filter = search_title
        .as_deref()
        .map(contains_choseong_query)
        .unwrap_or(false);

    // 기본 조건:
    // soft delete 된 게시글은 목록에서 제외한다.
    let mut filters = vec!["is_deleted=eq.false".to_string()];

    // 신고 자동 숨김된 게시글도 목록에서 제외한다.
    filters.push("is_hidden=eq.false".to_string());

    // 콘테스트 ID가 들어오면 해당 콘테스트 게시글만 조회한다.
    if let Some(id) = contest_id {
        filters.push(format!("contest_id=eq.{}", id));
    }

    // post_type이 들어오면 notice/request/contest/free 중 해당 타입만 조회한다.
    if let Some(post_type) = post_type {
        filters.push(format!("post_type=eq.{}", post_type.as_str()));
    }

    // 일반 제목 검색은 DB에서 ilike로 처리한다.
    //
    // 초성 검색은 DB에서 처리하지 않고,
    // 아래에서 Rust 코드로 필터링하므로 여기서는 title 필터를 붙이지 않는다.
    if let Some(title) = search_title.as_deref() {
        if !use_choseong_filter {
            filters.push(format!("title=ilike.*{}*", urlencoding::encode(title)));
        }
    }

    // 정렬 조건.
    //
    // Date  => 최신순
    // Likes => reaction_count 높은 순, 같으면 최신순
    // Views => view_count 높은 순, 같으면 최신순
    let order = match sort {
        PostSort::Date => "created_at.desc",
        PostSort::Likes => "reaction_count.desc,created_at.desc",
        PostSort::Views => "view_count.desc,created_at.desc",
    };

    // 게시글 목록 조회.
    //
    // users!posts_user_id_fkey!inner(nickname,profile_image)
    // - 작성자 정보를 posts와 함께 가져온다.
    // - 이 부분은 이미 N+1이 아니다.
    //
    // users.deleted_at=is.null
    // - 탈퇴한 사용자의 게시글은 목록에서 제외한다.
    //
    // Prefer: count=exact
    // - content-range 헤더에서 전체 개수를 얻기 위함.
    let url = format!(
        "{}/rest/v1/posts?{}&users.deleted_at=is.null&select=*,users!posts_user_id_fkey!inner(nickname,profile_image)&order={}",
        state.config.supabase_url.trim_end_matches('/'),
        filters.join("&"),
        order,
    );

    let req = state
        .http_client
        .get(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "count=exact")
        .header("Range-Unit", "items");

    // 일반 검색은 DB Range로 페이지네이션한다.
    //
    // 초성 검색은 Rust에서 필터링해야 하므로,
    // 먼저 전체 후보를 가져온 뒤 아래에서 skip/take 처리한다.
    let req = if use_choseong_filter {
        req
    } else {
        req.header("Range", format!("{}-{}", offset, end))
    };

    let res = req.send().await.context("posts SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts SELECT 실패: {}", body));
    }

    // Supabase/PostgREST의 count=exact 결과는 content-range 헤더에 들어온다.
    //
    // 예:
    // content-range: 0-9/123
    //
    // 여기서 마지막 123이 전체 개수다.
    let db_total_count = res
        .headers()
        .get("content-range")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.rsplit('/').next())
        .and_then(|value| value.parse::<i64>().ok())
        .unwrap_or(0);

    let mut posts: Vec<Post> = res.json().await.context("posts 역직렬화 실패")?;

    // 초성 검색일 때는 DB에서 title 필터를 못 걸었으므로
    // Rust에서 직접 제목 초성 매칭을 수행한다.
    let total_count = if use_choseong_filter {
        if let Some(title) = search_title.as_deref() {
            posts.retain(|post| title_matches_choseong(&post.title, title));
        }

        posts.len() as i64
    } else {
        db_total_count
    };

    // 초성 검색일 때는 필터링 후 Rust에서 직접 페이지네이션한다.
    if use_choseong_filter {
        posts = posts
            .into_iter()
            .skip(offset as usize)
            .take(page_size as usize)
            .collect();
    }

    // ─────────────────────────────────────────────────────────
    // 여기부터가 N+1 해결 핵심
    // ─────────────────────────────────────────────────────────

    // 현재 페이지에 표시될 게시글 id만 모은다.
    //
    // 예:
    // posts = [A, B, C, D]
    // post_ids = [A.id, B.id, C.id, D.id]
    let post_ids = posts.iter().map(|post| post.id).collect::<Vec<Uuid>>();

    // reactions를 게시글마다 조회하지 않고,
    // 현재 페이지의 post_id 전체에 대해 한 번만 조회한다.
    //
    // 내부 쿼리 예:
    // /rest/v1/reactions?user_id=eq.{user_id}&post_id=in.(A,B,C,D)&select=post_id
    let reacted_post_ids = get_reacted_post_ids_by_user(state, user_id, &post_ids).await?;

    let mut items = Vec::with_capacity(posts.len());

    // 게시글 목록 응답 생성 중 같은 프로필 이미지 path가 반복될 수 있으므로
    // signed URL을 path 기준으로 캐싱한다.
    //
    // 이 캐시는 이 API 응답을 만드는 동안만 살아있는 요청 단위 캐시다.
    // 전역 캐시가 아니므로 만료/동시성 문제를 신경 쓸 필요가 적다.
    let mut profile_signed_url_cache: HashMap<String, Option<String>> = HashMap::new();

    for post in posts {
        // HashSet에 post.id가 있으면 현재 로그인한 사용자가 이미 반응한 게시글이다.
        let is_reacted = reacted_post_ids.contains(&post.id);

        items.push(
            to_post_response_with_reacted(state, post, is_reacted, &mut profile_signed_url_cache)
                .await?,
        );
    }

    Ok(PostListResponse { items, total_count })
}

// ── 게시물 상세 조회 ───────────────────────────────────────────

pub async fn get_post_detail(
    state: &AppState,
    user_id: Uuid,
    post_id: Uuid,
) -> Result<PostResponse> {
    let mut post = get_post(state, post_id).await?;

    let next_view_count = increment_post_view_count(state, post_id).await?;
    post.view_count = next_view_count;

    to_post_response(state, user_id, post).await
}

// ── 게시물 생성 ───────────────────────────────────────────────

pub async fn create_post(
    state: &AppState,
    user_id: Uuid,
    req: CreatePostRequest,
) -> Result<PostResponse> {
    validate_post_type(&req)?;
    validate_post_text(Some(&req.title), req.content.as_deref())?;

    if matches!(req.post_type, PostType::Notice) {
        ensure_admin(state, user_id).await?;
    }

    let url = format!(
        "{}/rest/v1/posts",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        post_type: String,
        title: String,
        contest_id: Option<Uuid>,
        image_url: Option<String>,
        content: Option<String>,
    }

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&InsertPayload {
            user_id,
            post_type: req.post_type.as_str().to_string(),
            title: req.title.trim().to_string(),
            contest_id: req.contest_id,
            image_url: normalize_optional_string(req.image_url),
            content: normalize_optional_string(req.content),
        })
        .send()
        .await
        .context("posts INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts INSERT 실패: {}", body));
    }

    let inserted: Vec<Post> = res.json().await.context("posts INSERT 역직렬화 실패")?;
    let post_id = inserted
        .into_iter()
        .next()
        .map(|post| post.id)
        .ok_or_else(|| anyhow!("posts INSERT 결과가 비어있음"))?;

    let post = get_post(state, post_id).await?;
    to_post_response(state, user_id, post).await
}

// ── 게시물 수정 ───────────────────────────────────────────────

pub async fn update_post(
    state: &AppState,
    user_id: Uuid,
    post_id: Uuid,
    req: UpdatePostRequest,
) -> Result<PostResponse> {
    if req.title.is_none() && req.image_url.is_none() && req.content.is_none() {
        return Err(anyhow!("수정할 필드가 없습니다."));
    }

    validate_post_text(req.title.as_deref(), req.content.as_deref())?;

    let post = get_post(state, post_id).await?;
    ensure_can_modify_post(state, user_id, &post).await?;

    let mut payload = Map::new();

    if let Some(title) = req.title {
        let title = title.trim();
        if title.is_empty() {
            return Err(anyhow!("title은 비어 있을 수 없습니다."));
        }
        payload.insert("title".to_string(), Value::String(title.to_string()));
    }

    if let Some(image_url) = req.image_url {
        let image_url = image_url.trim();
        if image_url.is_empty() {
            payload.insert("image_url".to_string(), Value::Null);
        } else {
            payload.insert(
                "image_url".to_string(),
                Value::String(image_url.to_string()),
            );
        }
    }

    if let Some(content) = req.content {
        payload.insert(
            "content".to_string(),
            Value::String(content.trim().to_string()),
        );
    }

    let url = format!(
        "{}/rest/v1/posts?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        post_id,
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
        .json(&payload)
        .send()
        .await
        .context("posts UPDATE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts UPDATE 실패: {}", body));
    }

    let updated: Vec<Post> = res.json().await.context("posts UPDATE 역직렬화 실패")?;
    let updated_post_id = updated
        .into_iter()
        .next()
        .map(|post| post.id)
        .ok_or_else(|| anyhow!("posts UPDATE 결과가 비어있음"))?;

    let post = get_post(state, updated_post_id).await?;
    to_post_response(state, user_id, post).await
}

// ── 게시물 삭제 ───────────────────────────────────────────────

pub async fn delete_post(state: &AppState, user_id: Uuid, post_id: Uuid) -> Result<()> {
    let post = get_post(state, post_id).await?;
    ensure_can_modify_post(state, user_id, &post).await?;

    let url = format!(
        "{}/rest/v1/posts?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        post_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "is_deleted": true,
            "deleted_at": Utc::now(),
        }))
        .send()
        .await
        .context("posts DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts DELETE 실패: {}", body));
    }
    Ok(())
}

// ── 게시물 반응 (투표 / 좋아요) ─────────────────────────────
//
// 하나의 reactions 테이블을 사용하지만
// post_type에 따라 의미가 달라짐.
//
// contest  → 투표
// free     → 좋아요
// request  → 좋아요
// notice   → 반응 금지
//
pub async fn react_post(state: &AppState, user_id: Uuid, post_id: Uuid) -> Result<()> {
    let post = get_post(state, post_id).await?;

    // 2. 게시글 타입에 따른 반응 가능 여부 체크
    //
    // match를 쓰는 이유:
    // - 타입별 정책을 명확하게 분리
    // - 나중에 타입 추가될 때 안전하게 처리 가능
    //
    match post.post_type.as_str() {
        // 공지사항은 반응 금지
        "notice" => {
            return Err(anyhow!("공지사항에는 반응할 수 없습니다."));
        }

        // 허용되는 타입들
        // contest: 투표
        // free/request: 좋아요
        "contest" | "free" | "request" => {
            // 아무것도 안 하고 통과
        }

        // 예상 못한 타입 방어 코드 (실무에서 중요)
        _ => {
            return Err(anyhow!("지원하지 않는 게시글 타입입니다."));
        }
    }

    let url = format!(
        "{}/rest/v1/reactions",
        state.config.supabase_url.trim_end_matches('/'),
    );

    #[derive(Serialize)]
    struct InsertPayload {
        user_id: Uuid,
        post_id: Uuid,
    }

    let res = state
        .http_client
        .post(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&InsertPayload { user_id, post_id })
        .send()
        .await
        .context("votes INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("votes INSERT 실패: {}", body));
    }

    if post.user_id != user_id {
        let notification_type = format!(
            "react_{}_{}",
            &post_id.to_string()[..8],
            &user_id.to_string()[..8]
        );
        let actor_nickname = get_user_nickname_for_notification(state, user_id).await;
        let title = truncate_notification_title(&post.title);
        let message = match post.post_type.as_str() {
            "contest" => format!("{}님이 '{}' 게시글에 투표했습니다.", actor_nickname, title),
            _ => format!(
                "{}님이 '{}' 게시글에 좋아요를 눌렀습니다.",
                actor_nickname, title
            ),
        };

        if let Err(e) = crate::notification::service::notify_social_if_enabled(
            state,
            post.user_id,
            &notification_type,
            &message,
        )
        .await
        {
            tracing::error!("게시글 반응 알림 생성 실패: {}", e);
        }
    }

    Ok(())
}

// ── 게시물 반응 취소 ─────────────────────────────────────────────
//
// free/request 게시글의 좋아요는 취소 가능.
// contest 게시글의 투표는 취소 불가로 유지한다.
pub async fn unreact_post(state: &AppState, user_id: Uuid, post_id: Uuid) -> Result<()> {
    let post = get_post(state, post_id).await?;

    match post.post_type.as_str() {
        "contest" => {
            return Err(anyhow!("아바타 콘테스트 투표는 취소할 수 없습니다."));
        }
        "free" | "request" => {}
        "notice" => {
            return Err(anyhow!("공지사항에는 반응할 수 없습니다."));
        }
        _ => {
            return Err(anyhow!("지원하지 않는 게시글 타입입니다."));
        }
    }

    let url = format!(
        "{}/rest/v1/reactions?user_id=eq.{}&post_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
        post_id,
    );

    let res = state
        .http_client
        .delete(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=minimal")
        .send()
        .await
        .context("reactions DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("reactions DELETE 실패: {}", body));
    }

    Ok(())
}

// ── 댓글 ─────────────────────────────────────────────────────

pub async fn list_comments(state: &AppState, post_id: Uuid) -> Result<Vec<CommentResponse>> {
    let post = get_post(state, post_id).await?;

    if post.post_type == "notice" {
        return Err(anyhow!("공지사항에는 댓글을 사용할 수 없습니다."));
    }

    // 댓글은 작성자가 탈퇴해도 row는 가져온다 (대댓글 보존 위해)
    // to_comment_response에서 작성자 정보를 가공해서 반환함
    // users.deleted_at도 같이 select해서 가공 판단에 사용
    // is_hidden=eq.false 추가:
    //   숨겨진 댓글은 목록에서 바로 빠진다.
    let url = format!(
        "{}/rest/v1/comments?post_id=eq.{}&is_deleted=eq.false&is_hidden=eq.false&select=*,users!comments_user_id_fkey(nickname,profile_image,deleted_at)&order=created_at.asc",
        state.config.supabase_url.trim_end_matches('/'),
        post_id,
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
        .context("comments SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("comments SELECT 실패: {}", body));
    }

    let comments: Vec<Comment> = res.json().await.context("comments 역직렬화 실패")?;
    let mut items = Vec::with_capacity(comments.len());

    // 댓글 목록 응답을 만드는 동안 같은 profile_image path가 반복될 수 있다.
    // 예를 들어 한 사용자가 댓글과 대댓글을 여러 개 달면 같은 프로필 이미지 path가 반복된다.
    // 이 캐시는 현재 list_comments 요청 안에서만 쓰이는 요청 단위 캐시다.
    let mut profile_signed_url_cache: HashMap<String, Option<String>> = HashMap::new();

    for comment in comments {
        items.push(
            to_comment_response_with_cache(state, comment, &mut profile_signed_url_cache).await,
        );
    }

    Ok(items)
}

pub async fn create_comment(
    state: &AppState,
    user_id: Uuid,
    post_id: Uuid,
    req: CreateCommentRequest,
) -> Result<CommentResponse> {
    let post = get_post(state, post_id).await?;
    if post.post_type == "notice" {
        return Err(anyhow!("공지사항에는 댓글을 작성할 수 없습니다."));
    }

    let content = validate_comment_content(&req.content)?;

    // 4. parent_id가 있으면 대댓글 작성임
    // DB 트리거에서도 검사하지만, 백엔드에서도 한 번 더 검사해서
    // 클라이언트에게 더 명확한 에러를 줄 수 있게 함
    if let Some(parent_id) = req.parent_id {
        let parent = get_comment(state, parent_id).await?;

        // 부모 댓글과 현재 댓글은 같은 게시글에 속해야 함
        if parent.post_id != post_id {
            return Err(anyhow!(
                "대댓글은 같은 게시글의 댓글에만 작성할 수 있습니다"
            ));
        }

        // parent.parent_id가 Some이면 부모도 이미 대댓글 이라는 뜻
        if parent.parent_id.is_some() {
            return Err(anyhow!("대댓글에는 다시 대댓글을 작성할 수 없습니다."));
        }
    }

    let url = format!(
        "{}/rest/v1/comments",
        state.config.supabase_url.trim_end_matches('/'),
    );

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
            "post_id": post_id,
            "parent_id": req.parent_id,
            "user_id": user_id,
            "content": content,
        }))
        .send()
        .await
        .context("comments INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("comments INSERT 실패: {}", body));
    }

    let inserted: Vec<Comment> = res.json().await.context("comments INSERT 역직렬화 실패")?;
    let comment_id = inserted
        .into_iter()
        .next()
        .map(|comment| comment.id)
        .ok_or_else(|| anyhow!("comments INSERT 결과가 비어있음"))?;

    let comment = get_comment(state, comment_id).await?;
    if post.user_id != user_id {
        let notification_type = format!("cmt_{}", &comment_id.to_string()[..8]);
        let actor_nickname = get_user_nickname_for_notification(state, user_id).await;
        let title = truncate_notification_title(&post.title);
        let message = format!(
            "{}님이 '{}' 게시글에 댓글을 남겼습니다.",
            actor_nickname, title
        );

        if let Err(e) = crate::notification::service::notify_social_if_enabled(
            state,
            post.user_id,
            &notification_type,
            &message,
        )
        .await
        {
            tracing::error!("댓글 알림 생성 실패: {}", e);
        }
    }

    Ok(to_comment_response(state, comment).await)
}

pub async fn update_comment(
    state: &AppState,
    user_id: Uuid,
    comment_id: Uuid,
    req: UpdateCommentRequest,
) -> Result<CommentResponse> {
    let comment = get_comment(state, comment_id).await?;

    if comment.user_id != user_id {
        return Err(anyhow!("본인 댓글만 수정할 수 있습니다."));
    }

    let content = validate_comment_content(&req.content)?;
    let url = format!(
        "{}/rest/v1/comments?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        comment_id,
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
        .json(&serde_json::json!({ "content": content }))
        .send()
        .await
        .context("comments UPDATE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("comments UPDATE 실패: {}", body));
    }

    let updated: Vec<Comment> = res.json().await.context("comments UPDATE 역직렬화 실패")?;
    let updated_id = updated
        .into_iter()
        .next()
        .map(|comment| comment.id)
        .ok_or_else(|| anyhow!("comments UPDATE 결과가 비어있음"))?;

    let comment = get_comment(state, updated_id).await?;
    Ok(to_comment_response(state, comment).await)
}

pub async fn delete_comment(state: &AppState, user_id: Uuid, comment_id: Uuid) -> Result<()> {
    // 1. 삭제 대상 댓글 조회
    let comment = get_comment(state, comment_id).await?;

    // 2. 본인 댓글만 삭제 가능
    // 중요:
    // 여기서 먼저 부모 댓글 소유자 검사를 통과한 뒤,
    // 그 다음에 자식 대댓글까지 같이 숨김 처리한다.
    if comment.user_id != user_id {
        return Err(anyhow!("본인 댓글만 삭제할 수 있습니다."));
    }

    // 3. 댓글 soft delete
    // 부모 댓글이면 parent_id = comment_id인 대댓글도 같이 숨김 처리
    //
    // 이유:
    // 실제 DELETE가 아니라 is_deleted = true로 숨기는 구조라서
    // DB의 on delete cascade는 동작하지 않음.
    // 따라서 백엔드에서 직접 자식 대댓글까지 업데이트해야 함.
    let url = format!(
        "{}/rest/v1/comments?or=(id.eq.{},parent_id.eq.{})",
        state.config.supabase_url.trim_end_matches('/'),
        comment_id,
        comment_id,
    );

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "is_deleted": true,
            "deleted_at": Utc::now(),
        }))
        .send()
        .await
        .context("comments DELETE 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("comments DELETE 실패: {}", body));
    }

    Ok(())
}

pub async fn chat_with_bot(
    state: &AppState,
    user_id: Uuid,
    message: String,
) -> Result<ChatResponse> {
    let ai_response = crate::clients::ai_client::chat(
        state,
        crate::clients::ai_client::ChatPayload {
            user_id: user_id.to_string(),
            message: message.clone(),
        },
    )
    .await?;

    let log_url = format!(
        "{}/rest/v1/chatbot_logs",
        state.config.supabase_url.trim_end_matches('/'),
    );

    let insert_result = state
        .http_client
        .post(&log_url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "user_id": user_id,
            "user_message": message,
            "bot_response": ai_response.response,
        }))
        .send()
        .await
        .context("chatbot_logs INSERT 요청 실패");

    if let Ok(response) = insert_result {
        if response.status().is_success() {
            let _ = response.json::<Vec<ChatbotLog>>().await;
        } else {
            let body = response.text().await.unwrap_or_default();
            tracing::warn!("chatbot_logs INSERT 실패: {}", body);
        }
    } else if let Err(error) = insert_result {
        tracing::warn!("chatbot_logs 저장 중 오류: {error}");
    }

    Ok(ChatResponse {
        response: ai_response.response,
    })
}

// ── 컨텐츠 신고 ──────────────────────────────────────────────
//
// 신고 기능은 커뮤니티 도메인 안에서 처리한다.
// 이유:
// - 신고 대상이 게시글/댓글/사용자 닉네임/프로필사진이고,
// - 모두 커뮤니티 화면에서 발생하는 사용자 컨텐츠이기 때문.
//
// 일반 사용자:
// - POST /api/content-reports
//
// 관리자:
// - GET /api/admin/content-reports
// - PATCH /api/admin/content-reports/{id}/resolve
// - PATCH /api/admin/content-reports/{id}/reject
fn to_content_report_response(row: ContentReport) -> ContentReportResponse {
    ContentReportResponse {
        id: row.id,
        reporter_id: row.reporter_id,
        target_type: row.target_type,
        target_id: row.target_id,
        reason: row.reason,
        detail: row.detail,
        status: row.status,
        created_at: row.created_at,
        reviewed_at: row.reviewed_at,
        reviewed_by: row.reviewed_by,
    }
}

/// 신고 사유가 심각(inappropriate)하면 게시글/댓글을 즉시 임시 숨김 처리한다.
///
/// 정책:
/// - inappropriate(부적절한 표현/이미지): 1건 신고만으로 즉시 숨김.
///   음란/불법 이미지처럼 방치 시 피해가 큰 사유라 선조치한다.
/// - abuse / spam / other: 자동 숨김 안 함. 관리자가 직접 검토.
///
/// 대상:
/// - post / comment 만. (닉네임/프로필 신고는 숨김 대상이 아님)
///
/// 중요: 이 함수는 Result를 반환하지 않는다.
///   자동 숨김은 신고 접수의 부가 작업이라, 실패해도 신고 접수는 성공으로 둬야 한다.
///   호출부에서 `?`로 에러를 전파하면 신고는 이미 DB에 들어갔는데 응답은 에러가 되어
///   사용자가 "신고 실패했나?" 하고 또 누르는 혼란이 생긴다.
///   그래서 실패 시 로그만 남기고 조용히 넘어간다.
async fn auto_hide_if_inappropriate(
    state: &AppState,
    target_type: &str,
    target_id: Uuid,
    reason: &str,
) {
    // 게시글/댓글이 아니거나, 심각 사유가 아니면 아무것도 안 함.
    if (target_type != "post" && target_type != "comment") || reason != "inappropriate" {
        return;
    }

    // target_type에 따라 PATCH할 테이블 결정.
    let table = if target_type == "post" { "posts" } else { "comments" };

    let url = format!(
        "{}/rest/v1/{}?id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        table,
        target_id
    );

    // is_hidden=true 로 전환.
    // hidden_reason은 "auto_hide_inappropriate"로 고정 → 나중에 관리자 복구 시
    //   "자동 숨김된 것만 복구"를 판별하는 키로 쓴다.
    let res = state
        .http_client
        .patch(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=minimal")
        .json(&serde_json::json!({
            "is_hidden": true,
            "hidden_reason": "auto_hide_inappropriate",
            "hidden_at": Utc::now().to_rfc3339()
        }))
        .send()
        .await;

    // 실패해도 신고는 이미 접수됨. 로그만 남긴다.
    match res {
        Ok(r) if r.status().is_success() => {
            tracing::info!("신고 자동 숨김 처리됨: {} {}", target_type, target_id);
        }
        Ok(r) => {
            let body = r.text().await.unwrap_or_default();
            tracing::error!("자동 숨김 실패 (신고는 접수됨): {}", body);
        }
        Err(e) => {
            tracing::error!("자동 숨김 요청 실패 (신고는 접수됨): {}", e);
        }
    }
}

/// 신고 상세 내용 검증
///
/// 정책:
/// - 필수 입력
/// - 앞뒤 공백 제거
/// - 빈 문자열 불가
/// - 최대 500자 제한
fn validate_report_detail(detail: Option<String>) -> Result<String> {
    let detail = detail
        .map(|value| value.trim().to_string())
        .filter(|value| !value.is_empty())
        .ok_or_else(|| anyhow!("신고 상세 내용을 입력해주세요."))?;

    Ok(detail.chars().take(500).collect())
}

/// 사용자 존재 여부 확인
///
/// user_nickname / user_profile 신고에서 사용.
///
/// 정책:
/// - users.id 존재해야 함
/// - is_active = true 사용자만 허용
async fn ensure_user_exists(state: &AppState, user_id: Uuid) -> Result<Uuid> {
    let url = format!(
        "{}/rest/v1/users?id=eq.{}&is_active=eq.true&select=id&limit=1",
        state.config.supabase_url.trim_end_matches('/'),
        user_id
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
        .context("신고 대상 사용자 조회 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("신고 대상 사용자 조회 실패: {}", body));
    }

    let rows: Vec<Value> = res
        .json()
        .await
        .context("신고 대상 사용자 조회 응답 파싱 실패")?;

    if rows.is_empty() {
        return Err(anyhow!("신고 대상을 찾을 수 없습니다."));
    }

    Ok(user_id)
}

// 신고 대상 존재 여부 확인 + 신고 대상의 소유자/작성자 user_id 반환
//
// 반환값:
// - post: 게시글 작성자 posts.user_id
// - comment: 댓글 작성자 comments.user_id
// - user_nickname: 신고 대상 users.id
// - user_profile: 신고 대상 users.id
//
// 추가 정책:
// - post 신고 대상이 공지사항이면 신고 불가.
// - 프론트에서 공지사항 신고 버튼을 숨겨도,
//   사용자가 개발자도구/Postman으로 직접 API를 호출할 수 있으므로
//   백엔드에서도 반드시 한 번 더 막아야 한다.
async fn get_report_target_owner_id(
    state: &AppState,
    target_type: &str,
    target_id: Uuid,
) -> Result<Uuid> {
    match target_type {
        "post" => {
            let post = get_post(state, target_id).await?;

            // ─────────────────────────────────────────────
            // 공지사항 신고 차단
            // ─────────────────────────────────────────────
            //
            // 공지사항은 운영자가 작성한 공식 게시글이다.
            // 따라서 일반 사용자가 content_reports로 신고할 수 없게 막는다.
            //
            // 주의:
            // - 프론트에서 신고 버튼을 숨기는 것은 UX 처리
            // - 백엔드 차단은 실제 보안 처리
            if post.post_type == "notice" {
                return Err(anyhow!("공지사항은 신고할 수 없습니다."));
            }

            Ok(post.user_id)
        }

        "comment" => {
            let comment = get_comment(state, target_id).await?;
            Ok(comment.user_id)
        }

        "user_nickname" | "user_profile" => ensure_user_exists(state, target_id).await,

        _ => Err(anyhow!("지원하지 않는 신고 대상입니다.")),
    }
}

/// 일반 사용자: 게시글/댓글/닉네임/프로필사진 신고 접수
/// 일반 사용자: 게시글/댓글/닉네임/프로필사진 신고 접수
pub async fn create_content_report(
    state: &AppState,
    reporter_id: Uuid,
    req: CreateContentReportRequest,
) -> Result<ContentReportResponse> {
    let target_type = req.target_type.as_str();
    let reason = req.reason.as_str();

    // 여기 반드시 ? 필요
    // 안 붙이면 detail 타입이 Result<String, anyhow::Error>가 되어
    // serde_json::json! 안에서 Serialize 불가 에러가 남
    let detail = validate_report_detail(req.detail)?;

    // 신고 대상이 존재하는지 확인하고,
    // 동시에 그 대상의 작성자/소유자 user_id를 가져온다.
    let target_owner_id = get_report_target_owner_id(state, target_type, req.target_id).await?;

    // 본인 컨텐츠/프로필 신고 방지
    //
    // post:
    //   본인이 쓴 게시글 신고 불가
    //
    // comment:
    //   본인이 쓴 댓글 신고 불가
    //
    // user_nickname / user_profile:
    //   자기 자신의 닉네임/프로필사진 신고 불가
    if target_owner_id == reporter_id {
        return Err(anyhow!("자기 자신의 콘텐츠는 신고할 수 없습니다."));
    }

    let url = format!(
        "{}/rest/v1/content_reports",
        state.config.supabase_url.trim_end_matches('/')
    );

    let res = state
        .http_client
        .post(&url)
        .header("apikey", &state.config.supabase_secret_key)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("Prefer", "return=representation")
        .json(&serde_json::json!({
            "reporter_id": reporter_id,
            "target_type": target_type,
            "target_id": req.target_id,
            "reason": reason,
            "detail": detail,
            "status": "pending"
        }))
        .send()
        .await
        .context("content_reports INSERT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();

        // unique(reporter_id, target_type, target_id) 중복 처리
        if body.contains("content_reports_unique_reporter_target") || body.contains("duplicate key")
        {
            return Err(anyhow!("이미 신고한 콘텐츠입니다."));
        }

        return Err(anyhow!("content_reports INSERT 실패: {}", body));
    }

    let rows: Vec<ContentReport> = res
        .json()
        .await
        .context("content_reports INSERT 응답 역직렬화 실패")?;

    let report = rows
        .into_iter()
        .next()
        .ok_or_else(|| anyhow!("신고 접수 결과가 비어 있습니다."))?;

    // ── 자동 숨김 처리 ──
    // 심각 사유(inappropriate)면 게시글/댓글을 즉시 임시 숨김.
    // Result를 반환하지 않으므로 ? 없이 그냥 await. 실패해도 신고는 성공.
    auto_hide_if_inappropriate(state, target_type, req.target_id, reason).await;

    // 신고자에게 "신고 접수됨" 알림 발행 — report.id를 type에 포함해 중복 방지
    // 실패해도 신고 자체는 성공이므로 에러 로그만 남기고 진행
    let reporter_notification_type = format!("report_submitted_{}", &report.id.to_string()[..8]);
    if let Err(e) = crate::notification::service::create_notification(
        state,
        reporter_id,
        &reporter_notification_type,
        "신고가 정상 접수되었어요. 운영자가 확인 후 처리합니다.",
    )
    .await
    {
        tracing::error!("신고 접수 알림 생성 실패: {}", e);
    }

    Ok(to_content_report_response(report))
}
