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
use serde::{Serialize, Deserialize};
use uuid::Uuid;

use crate::state::AppState;
use super::dto::{MintNftRequest, MintNftResponse, TransferNftRequest, TransferNftResponse, UserItemResponse };

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
    user_id: Uuid,      // JWT에서 추출한 현재 로그인 유저 UUID
    req: MintNftRequest,// { user_item_id, nft_mint_address }
)->Result<MintNftResponse> {
    // Supabase PostgREST URL 구성
    // 필터: id=eq.{user_item_id} AND user_id=eq.{user_id} → 본인 소유 아이템만 수정 가능 (보안 필터)
    let url = format!(
        "{}/rest/v1/user_items?id=eq.{}&user_id=eq.{}",
        state.config.supabase_url.trim_end_matches('/'),    // 끝 슬래시 중복 방지
        req.user_item_id,
        user_id,
    );

    // PATCH 요청 바디: 변경할 컬럼만 포함
    // Serialize만 필요하므로 함수 내부에 로컬 구조체로 정의
    #[derive(Serialize)]
    struct PatchPayload{
        nft_mint_address: String, // 온체인 민팅 주소
        is_nft: bool,             // NFT 발행 완료 플래그 → true로 고정
    }

    let res = state.http_client
        .patch(&url)
        .header("Authorization", format!("Bearer {}", state.config.supabase_secret_key))
        .header("apikey", &state.config.supabase_secret_key)
        .header("Prefer", "return=representation")  // PATCH 후 변경된 row 반환 요청
        .json(&PatchPayload {
            nft_mint_address: req.nft_mint_address.clone(),
            is_nft: true,
        })
        .send()
        .await
        .context("user_items PATCH 요청 실패")?;    // reqwest 네트워크 에러

    // HTTP 상태코드가 2xx가 아니면 Supbase 에러 메세지를 포함해 반환



}
