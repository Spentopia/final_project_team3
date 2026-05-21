// avatar/dto.rs
// 아바타, 아이템, 장착, 가챠, 스크린샷 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── NFT 민팅 요청/응답 ───────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct MintNftRequest {
    pub user_item_id: Uuid,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct MintNftResponse {
    pub message: String,
    pub nft_mint_address: String,
}

// ── NFT 전송 요청/응답 ───────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct TransferNftRequest {
    pub avatar_id: Uuid,
    pub nft_mint_address: String,
    pub tx_signature: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct TransferNftResponse {
    pub message: String,
    pub nft_mint_address: String,
}

// ── 아바타 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct AvatarResponse {
    pub id: Uuid,
    pub grade: Option<String>,
    pub image_url: String,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub condition_met: Option<String>,
    pub is_active: Option<bool>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// ── 아이템 응답 ───────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UserItemResponse {
    pub id: Uuid,
    pub item_id: Uuid,
    pub name: String,
    pub category: String,
    pub image_url: String,
    pub visual_parts: Option<serde_json::Value>, // 장착용 이미지/모델 경로 JSON
    pub metadata_uri: Option<String>,
    pub slot_name: Option<String>,
    pub is_equipped: Option<bool>,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub minted_to_wallet: Option<String>,
    pub collection_mint: Option<String>,
    pub acquired_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UnityInventoryItemResponse {
    pub id: Uuid,
    pub inventory_id: Uuid,
    pub item_id: Uuid,
    pub name: String,
    pub category: String,
    pub image_url: String,
    pub visual_parts: Option<serde_json::Value>,
    pub metadata_uri: Option<String>,
    pub slot_name: String,
    pub is_equipped: bool,
    pub is_nft: bool,
    pub nft_mint_address: Option<String>,
    pub wallet_address: Option<String>,
    pub token_id: Option<String>,
    pub contract_address: Option<String>,
}

// ── 장착 요청/응답 ────────────────────────────────────────────

// POST /api/avatar/equipment — 아이템 장착
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct EquipItemRequest {
    pub inventory_id: Uuid, // user_items.id
    pub slot_name: String,  // hair / top / bottom / shoes / weapon / hat
}

// DELETE /api/avatar/equipment/:slot_name — 슬롯 해제
// 라우트 파라미터로 slot_name 받으므로 별도 바디 없음

// GET /api/avatar/equipment 응답 — 슬롯 1개
#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct EquipmentSlotResponse {
    pub slot_name: String,
    pub inventory_id: Option<Uuid>,
    pub is_visible: bool,
    pub equipped_at: Option<DateTime<Utc>>,
    // 장착 아이템 정보 (inventory_id가 NULL이면 전부 None)
    pub name: Option<String>,
    pub category: Option<String>,
    pub visual_parts: Option<serde_json::Value>,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
}

// ── NFT 조회 응답 ─────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct OwnedNftResponse {
    pub mint_address: String,
    pub item_id: Option<Uuid>,
    pub inventory_id: Option<Uuid>, // user_inventory.id — 판매 등록(market_listings.item_id FK)용
    pub name: String,
    pub category: Option<String>,
    pub image_url: Option<String>,
    pub metadata_uri: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct SyncOwnedNftsResponse {
    pub synced_count: usize,
    pub skipped_count: usize,
}

// ── 가챠 티켓 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GachaTicketResponse {
    pub id: Uuid,
    pub is_used: Option<bool>,
    pub used_at: Option<DateTime<Utc>>,
    pub acquired_at: Option<DateTime<Utc>>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct UseGachaTicketRequest {
    pub ticket_id: Uuid,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct GachaResultResponse {
    pub item_id: Uuid,
    pub name: String,
    pub category: String,
    pub image_url: String,
}

// ── 스크린샷 요청/응답 ────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateScreenshotRequest {
    pub image_url: String,
    pub caption: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ScreenshotResponse {
    pub id: Uuid,
    pub image_url: String,
    pub caption: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
