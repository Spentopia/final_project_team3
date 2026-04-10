// market/model.rs
// public.market_listings, public.market_transactions 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.market_listings 테이블
// 마켓 판매 등록
// 지갑 미연동 유저는 마켓 거래 불가 (NFT 미발행 상태)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MarketListing {
    pub id: Uuid,
    pub seller_id: Uuid,
    pub item_id: Uuid,
    pub price_spt: i32,
    pub status: Option<String>,          // active / sold / cancelled
    pub escrow_address: Option<String>,  // Solana 에스크로 PDA 주소
    pub listed_at: Option<DateTime<Utc>>,
    pub sold_at: Option<DateTime<Utc>>,
}

// public.market_transactions 테이블
// 마켓 거래 내역
// tx_signature로 온체인 크로스체크 가능
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MarketTransaction {
    pub id: Uuid,
    pub listing_id: Uuid,
    pub buyer_id: Uuid,
    pub price: i32,
    pub fee: i32,                        // 수수료 5% (burn 처리)
    pub tx_signature: Option<String>,    // Solana 트랜잭션 서명
    pub transacted_at: Option<DateTime<Utc>>,
}
