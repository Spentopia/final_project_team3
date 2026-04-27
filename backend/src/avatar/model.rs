// avatar/model.rs
// public.avatar_items, public.user_items, public.user_equipment,
// public.avatars, public.gacha_logs, public.gacha_tickets, public.user_screenshots 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.avatars 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Avatar {
    pub id: Uuid,
    pub user_id: Uuid,
    pub grade: Option<String>,
    pub image_url: String,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub condition_met: Option<String>,
    pub is_active: Option<bool>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.item_master 테이블 (아이템 마스터)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AvatarItem {
    pub id: Uuid,
    pub name: String,
    pub category: String, // hair / top / bottom / gloves / shoes / weapon / glasses
    pub rarity: String,   // common / rare / epic / legendary
    pub image_url: String,
    pub visual_parts: Option<serde_json::Value>, // 장착 시 캐릭터 적용 이미지/모델 경로 JSON
    pub created_at: Option<DateTime<Utc>>,
}

// public.user_inventory 테이블 (유저 인벤토리)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserItem {
    pub id: Uuid,
    pub user_id: Uuid,
    pub item_id: Uuid,
    pub is_equipped: Option<bool>,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub nft_tx_signature: Option<String>,
    pub minted_to_wallet: Option<String>, // 민팅 당시 지갑 주소 이력
    pub collection_mint: Option<String>,  // Solana 컬렉션 mint 주소
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.user_equipment 테이블 (부위별 장착 현황)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserEquipment {
    pub user_id: Uuid,
    pub slot_name: String,          // hair / top / bottom / gloves / shoes / weapon / glasses ENUM
    pub inventory_id: Option<Uuid>, // user_items.id 참조 (NULL = 슬롯 비어있음)
    pub equipped_at: Option<DateTime<Utc>>,
    pub is_visible: bool,
}

// public.gacha_logs 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GachaLog {
    pub id: Uuid,
    pub user_id: Uuid,
    pub item_id: Uuid,
    pub drawn_at: Option<DateTime<Utc>>,
}

// public.gacha_tickets 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GachaTicket {
    pub id: Uuid,
    pub user_id: Uuid,
    pub is_used: Option<bool>,
    pub used_at: Option<DateTime<Utc>>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.user_screenshots 테이블
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserScreenshot {
    pub id: Uuid,
    pub user_id: Uuid,
    pub image_url: String,
    pub caption: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
