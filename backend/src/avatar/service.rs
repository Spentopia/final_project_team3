// avatar/service.rs
//
// 아바타 관련 비즈니스 로직 모음
//
// 역할:
//  - Supabase REST API를 호출해 DB 조회/수정
//  - 핸들러로부터 위임받은 실제 로직 처리
//  - 결과를 DTO로 변환해 핸들러에 반환
//
// Supabase REST API 공통 패턴:
//  - Authorization: Bearer {supabase_secret_key} → service_role 권한 (RLS 우회)
//  - apiKey: {supabase_secret_key} → Supabase API 인증
//  - Prefer: return = representation → INSERT/PATCH 후 변경된 row 반환
//
// 에러 처리: anyhow::Result, anyhow!("메세지"), .context("설명")

use anyhow::{Context, Result, anyhow};
use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

use super::dto::{
    MintNftRequest, MintNftResponse, TransferNftRequest, TransferNftResponse, UserItemResponse,
};
use crate::state::AppState;

// mint_nft
//
/// public.user_items 테이블에서 지정한 row의 NFT 관련 컬럼을 업데이트한다.
///
/// 업데이트 대상 컬럼:
///     - nft_mint_address: 온체인에서 민팅된 Solana NFT mint 주소
///     - is_nft: true (NFT 발행 완료 표시)
///
/// 보안: `user_id=eq.{user_id}` 필터를 함께 걸어 본인 소유 아이템만 수정할 수 있도록 제한한다.
///       다른 유저의 user_item_id를 넘겨도 조건 불일치로 PATCH되지 않는다.
pub async fn mint_nft(
    state: &AppState,
    user_id: Uuid,       // JWT에서 추출한 현재 로그인 유저 UUID
    req: MintNftRequest, // { user_item_id, nft_mint_address }
) -> Result<MintNftResponse> {
    // Supabase PostgREST URL 구성
    // 필터: id=eq.{user_item_id} AND user_id=eq.{user_id} → 본인 소유 아이템만 수정 가능 (보안 필터)
    let url = format!(
        "{}/rest/v1/user_items?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'), // 끝 슬래시 중복 방지
        req.user_item_id,
        user_id,
    );

    // PATCH 요청 바디: 변경할 컬럼만 포함
    // Serialize만 필요하므로 함수 내부에 로컬 구조체로 정의
    #[derive(Serialize)]
    struct PatchPayload {
        nft_mint_address: String, // 온체인 민팅 주소
        is_nft: bool,             // NFT 발행 완료 플래그 → true로 고정
    }

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation") // PATCH 후 변경된 row 반환 요청
        .json(&PatchPayload {
            nft_mint_address: req.nft_mint_address.clone(),
            is_nft: true,
        })
        .send()
        .await
        .context("user_items PATCH 요청 실패")?; // reqwest 네트워크 에러

    // HTTP 상태코드가 2xx가 아니면 Supbase 에러 메세지를 포함해 반환
    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_items PATCH 실패: {}", body));
    }

    Ok(MintNftResponse {
        message: "NFT 민팅 완료".to_string(),
        nft_mint_address: req.nft_mint_address, // 요청에서 받은 값을 그대로 응답
    })
}

// transfer_nft
//
/// public.avatars 테이블에서 지정한 row의 NFT 관련 컬럼을 업데이트한다.
///
/// 업데이트 대상 컬럼:
///     - nft_mint_address: 전송 완료된 Solana NFT mint 주소
///     - is_nft: true (NFT 발행/전송 완료 표시)
///
/// 보안: `user_id=eq.{user_id}` 필터를 함꼐 걸어 본인 소유 아바타만 수정할 수 있도록 제한한다.
pub async fn transfer_nft(
    state: &AppState,
    user_id: Uuid,           // JWT에서 추출한 현재 로그인 유저 UUID
    req: TransferNftRequest, // { avatar_id, nft_mint_address }
) -> Result<TransferNftResponse> {
    // Supabase PostgREST URL 구성
    // 필터: id=eq.{avatar_id} AND user_id=eq.{user_id}
    //  → 본인 소유 아바타만 수정 가능 (보안 필터)
    let url = format!(
        "{}/rest/v1/avatars?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),
        req.avatar_id,
        user_id,
    );

    // PATCH 요청 바디: 변경할 컬럼만 포함
    #[derive(Serialize)]
    struct PatchPayload {
        nft_mint_address: String, // 온체인 NFT mint 주소
        is_nft: bool,             // NFT 발행 완료 플래그 → true로 고정
    }

    let res = state
        .http_client
        .patch(&url)
        .header(
            "Authorization",
            format!("Bearer {}", state.config.supabase_secret_key),
        )
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")
        .json(&PatchPayload {
            nft_mint_address: req.nft_mint_address.clone(),
            is_nft: true,
        })
        .send()
        .await
        .context("avatars PATCH 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("avatars PATCH 실패: {}", body));
    }

    Ok(TransferNftResponse {
        message: "NFT 전송 완료".to_string(),
        nft_mint_address: req.nft_mint_address,
    })
}

// get_user_items
//
/// 유저가 보유한 꾸미기 아이템 전체를 아이템 마스터 정보와 함께 조회한다.
///
/// PostgREST embedding(JOIN) 방식:
///     public.user_items.item_id → public.avatar_items.id
///
/// 쿼리 파라미터:
///     select=*.avatar_items(name,image_url,category,rarity)
///     - *: user_items의 모든 컬럼
///     - avatar_items(): FK(item_id)로 연결된 avatar_items에서 지정 컬럼만 가져옴
///
/// PostgREST 응답 예시:
/// [
///  {
///     "id": "...", "user_id": "...", "item_id": "...", "is_equipped": false, "is_nft": false, "nft_mint_address": null,
///     "acquired_at": "...",
///     "avatar_items": { ← 테이블명이 그대로 키가 됨
///         "name": "골든 프레임",
///         "image_url": "https//...",
///         "category": "frame",
///         "rarity"': "epic
///         }
///    }
/// ]
pub async fn get_user_items(
    state: &AppState,
    user_id: Uuid, // JWT에서 추출한 현재 로그인 유저 UUID
) -> Result<Vec<UserItemResponse>> {
    // PostgREST embedding URL:
    //  user_items?user_id=eq.{user_id} → 본인 아이템만 필터
    //  &select=*.avatar_items(...) → avatar_items 테이블 JOIN
    let url = format!(
        "{}/rest/v1/user_items?user_id=eq.{}&select=*,avatar_items(name,image_url,category,rarity)",
        state.config.supabase_url.trim_end_matches('/'),
        user_id,
    );

    // PostgREST 응답 역직렬화용 내부 구조체
    // UserItemResponse(dto)와 달리 avatar_items가 중첩 객체로 들어오므로 별도 Raw 구조체로 받은 뒤 매핑한다.

    /// PostgREST embedding으로 함께 오는 avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct AvatarItemEmbed {
        name: String,      // 아이템 이름 (예: "골든 프레임")
        image_url: String, // 아이템 이미지 URL
        category: String,  // 카테고리 (background / frame / effect / motion)
        rarity: String,    // 희귀도
    }

    /// PostgREST 응답 원형: user_items 컬럼 + avatar_items 중첩 객체
    #[derive(Deserialize)]
    struct UserItemRaw {
        id: Uuid,
        user_id: Uuid,
        item_id: Uuid,                      // avatar_items FK
        is_equipped: Option<bool>,          // 현재 장착 여부
        is_nft: Option<bool>,               // NFT 발행 여부
        nft_mint_address: Option<String>,   // Solana NFT mint 주소 (없으면 null)
        acquired_at: Option<DateTime<Utc>>, // 획득 시각
        avatar_items: AvatarItemEmbed,      // JOIN 결과 (중첩 객체)
    }

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
        .context("user_items SELECT 요청 실패")?;

    if !res.status().is_success() {
        let body = res.text().await.unwrap_or_default();
        return Err(anyhow!("user_items SELECT 실패: {}", body));
    }

    // Vec<UserItemRaw> 역직렬화
    let raw: Vec<UserItemRaw> = res.json().await.context("user_items 역직렬화 실패")?;

    // UserItemRaw → UserItemResponse 매핑
    // avatar_items 중첩 객체의 필드를 flat하게 꺼내 ResponseDTO에 채운다.
    let items = raw
        .into_iter()
        .map(|r| UserItemResponse {
            id: r.id,
            item_id: r.item_id,
            is_equipped: r.is_equipped,
            is_nft: r.is_nft,
            acquired_at: r.acquired_at,
            // avatar_items 중첩 객체에서 flat하게 꺼냄
            name: r.avatar_items.name,
            image_url: r.avatar_items.image_url,
            category: r.avatar_items.category,
            rarity: r.avatar_items.rarity,
        })
        .collect();
    Ok(items)
}
