// community/service.rs
// 콘테스트, 게시물, 투표, 챗봇 비즈니스 로직
//
// 챗봇:
//  백엔드가 클라이언트 역할로 AI 서버(FastAPI)를 호출한다.
//  대화 내용은 chatbot_logs에 저장.

use anyhow::{Context, Result, anyhow};
use axum::extract::Multipart;
use chrono::Utc;
use serde::Serialize;
use serde_json::{Map, Value};
use uuid::Uuid;

use super::{
    dto::{
        ChatResponse, ContestEventResponse, CreatePostRequest, PostListResponse, PostResponse,
        PostSort, PostType, UpdatePostRequest, UploadCommunityImageResponse,
    },
    model::{ChatbotLog, ContestEvent, Post},
};
use crate::{filter, state::AppState};

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

async fn to_post_response(state: &AppState, post: Post) -> PostResponse {
    let author_nickname = post.users.as_ref().and_then(|author| author.nickname.clone());
    let author_profile_image = post
        .users
        .as_ref()
        .and_then(|author| author.profile_image.clone());
    let author_profile_image_url = match author_profile_image.as_deref() {
        Some(path) => create_profile_image_signed_url(state, path).await.ok(),
        None => None,
    };

    PostResponse {
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
        vote_count: post.vote_count,
        view_count: post.view_count,
        created_at: post.created_at,
    }
}

async fn ensure_admin(state: &AppState, user_id: Uuid) -> Result<()> {
    let role = crate::auth::service::get_user_role(state, user_id).await?;
    if role.as_deref() != Some("admin") {
        return Err(anyhow!("관리자 권한이 필요합니다."));
    }
    Ok(())
}

async fn get_post(state: &AppState, post_id: Uuid) -> Result<Post> {
    let url = format!(
        "{}/rest/v1/posts?id=eq.{}&is_deleted=eq.false&select=*,users!posts_user_id_fkey(nickname,profile_image)&limit=1",
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
                return Err(anyhow!("아바타 콘테스트 게시글에는 contest_id가 필요합니다."));
            }
        }
    }

    Ok(())
}

fn validate_post_text(title: Option<&str>, content: Option<&str>) -> Result<()> {
    if let Some(title) = title {
        if !filter::check(title) {
            return Err(anyhow!(
                "게시글 제목에 사용할 수 없는 표현이 포함되어 있습니다."
            ));
        }
    }

    if let Some(content) = content {
        if !filter::check(content) {
            return Err(anyhow!(
                "게시글 내용에 사용할 수 없는 표현이 포함되어 있습니다."
            ));
        }
    }

    Ok(())
}

const CHOSEONG_COMPAT: [char; 19] = [
    'ㄱ', 'ㄲ', 'ㄴ', 'ㄷ', 'ㄸ', 'ㄹ', 'ㅁ', 'ㅂ', 'ㅃ', 'ㅅ', 'ㅆ', 'ㅇ', 'ㅈ', 'ㅉ', 'ㅊ',
    'ㅋ', 'ㅌ', 'ㅍ', 'ㅎ',
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
        "{}/rest/v1/contest_events?select=*&order=start_date.desc",
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

pub async fn list_posts(
    state: &AppState,
    contest_id: Option<Uuid>,
    post_type: Option<PostType>,
    sort: PostSort,
    title: Option<String>,
    page: u32,
    page_size: u32,
) -> Result<PostListResponse> {
    let page = page.max(1);
    let page_size = page_size.clamp(1, 50);
    let offset = (page - 1) * page_size;
    let end = offset + page_size - 1;
    let search_title = normalize_optional_string(title);
    let use_choseong_filter = search_title
        .as_deref()
        .map(contains_choseong_query)
        .unwrap_or(false);

    let mut filters = vec!["is_deleted=eq.false".to_string()];

    if let Some(id) = contest_id {
        filters.push(format!("contest_id=eq.{}", id));
    }

    if let Some(post_type) = post_type {
        filters.push(format!("post_type=eq.{}", post_type.as_str()));
    }

    if let Some(title) = search_title.as_deref() {
        if !use_choseong_filter {
            filters.push(format!("title=ilike.*{}*", urlencoding::encode(title)));
        }
    }

    let order = match sort {
        PostSort::Date => "created_at.desc",
        PostSort::Likes => "vote_count.desc,created_at.desc",
        PostSort::Views => "view_count.desc,created_at.desc",
    };

    let url = format!(
        "{}/rest/v1/posts?{}&select=*,users!posts_user_id_fkey(nickname,profile_image)&order={}",
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

    let req = if use_choseong_filter {
        req
    } else {
        req.header("Range", format!("{}-{}", offset, end))
    };

    let res = req
        .send()
        .await
        .context("posts SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("posts SELECT 실패: {}", body));
    }

    let db_total_count = res
        .headers()
        .get("content-range")
        .and_then(|value| value.to_str().ok())
        .and_then(|value| value.rsplit('/').next())
        .and_then(|value| value.parse::<i64>().ok())
        .unwrap_or(0);

    let mut posts: Vec<Post> = res.json().await.context("posts 역직렬화 실패")?;

    let total_count = if use_choseong_filter {
        if let Some(title) = search_title.as_deref() {
            posts.retain(|post| title_matches_choseong(&post.title, title));
        }
        posts.len() as i64
    } else {
        db_total_count
    };

    if use_choseong_filter {
        posts = posts
            .into_iter()
            .skip(offset as usize)
            .take(page_size as usize)
            .collect();
    }

    let mut items = Vec::with_capacity(posts.len());
    for post in posts {
        items.push(to_post_response(state, post).await);
    }

    Ok(PostListResponse { items, total_count })
}

// ── 게시물 상세 조회 ───────────────────────────────────────────

pub async fn get_post_detail(state: &AppState, post_id: Uuid) -> Result<PostResponse> {
    let mut post = get_post(state, post_id).await?;
    let next_view_count = increment_post_view_count(state, post_id).await?;
    post.view_count = next_view_count;

    Ok(to_post_response(state, post).await)
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
    Ok(to_post_response(state, post).await)
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
            payload.insert("image_url".to_string(), Value::String(image_url.to_string()));
        }
    }

    if let Some(content) = req.content {
        if content.trim().is_empty() {
            payload.insert("content".to_string(), Value::Null);
        } else {
            payload.insert("content".to_string(), Value::String(content));
        }
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
    Ok(to_post_response(state, post).await)
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

// ── 게시물 투표 ───────────────────────────────────────────────

pub async fn vote_post(state: &AppState, user_id: Uuid, post_id: Uuid) -> Result<()> {
    let post = get_post(state, post_id).await?;
    if post.post_type != "contest" {
        return Err(anyhow!("투표는 아바타 콘테스트 게시글에만 가능합니다."));
    }

    let url = format!(
        "{}/rest/v1/votes",
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
