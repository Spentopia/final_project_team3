// market/dto.rs
// 마켓 판매 등록, 거래 관련 요청/응답 구조체

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use utoipa::ToSchema;
use uuid::Uuid;

// ── 판매 등록 요청 ────────────────────────────────────────────
// 지갑 연동 유저만 가능 (NFT 발행된 아이템만 거래 가능)

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct CreateListingRequest {
    pub item_id: Uuid,
    pub price_spt: i32,
}

// ── 판매 목록 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct ListingResponse {
    pub id: Uuid,
    pub seller_id: Uuid,
    pub seller_nickname: Option<String>,
    pub seller_wallet_address: Option<String>,
    pub item_id: Uuid,
    pub item_name: String,
    pub item_image_url: String,
    pub item_category: String,
    pub nft_mint_address: Option<String>,
    pub escrow_address: Option<String>,
    pub price_spt: i32,
    pub status: Option<String>,
    pub listed_at: Option<DateTime<Utc>>,
}

// ── 구매 요청 ─────────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct PurchaseRequest {
    pub listing_id: Uuid,
    pub tx_signature: Option<String>, // Solana 트랜잭션 서명
}

// ── 거래 내역 응답 ────────────────────────────────────────────

#[derive(Debug, Serialize, Deserialize, ToSchema)]
pub struct TransactionResponse {
    pub id: Uuid,
    pub listing_id: Uuid,
    pub buyer_id: Uuid,
    pub price: i32,
    pub fee: i32,
    pub tx_signature: Option<String>,
    pub transacted_at: Option<DateTime<Utc>>,
}
