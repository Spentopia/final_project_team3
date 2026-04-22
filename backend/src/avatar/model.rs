// avatar/model.rs
// public.avatars, public.avatar_items, public.user_items,
// public.gacha_logs, public.gacha_tickets, public.user_screenshots 테이블 엔티티

use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.avatars 테이블
// 유저가 보유한 아바타 캐릭터 (보상/거래로만 획득)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Avatar {
    pub id: Uuid,
    pub user_id: Uuid,
    pub grade: Option<String>, // common / rare / epic / legendary
    pub image_url: String,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub condition_met: Option<String>,
    pub is_active: Option<bool>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.avatar_items 테이블
// 아바타 꾸미기 아이템 마스터 (관리자 등록)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AvatarItem {
    pub id: Uuid,
    pub name: String,
    pub category: String, // background / frame / effect / motion
    pub rarity: String,   // common / rare / epic
    pub image_url: String,
    pub created_at: Option<DateTime<Utc>>,
}

// public.user_items 테이블
// 유저가 보유한 꾸미기 아이템
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserItem {
    pub id: Uuid,
    pub user_id: Uuid,
    pub item_id: Uuid,
    pub is_equipped: Option<bool>,
    pub is_nft: Option<bool>,
    pub nft_mint_address: Option<String>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.gacha_logs 테이블
// 가챠(랜덤뽑기) 이력
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GachaLog {
    pub id: Uuid,
    pub user_id: Uuid,
    pub item_id: Uuid,
    pub drawn_at: Option<DateTime<Utc>>,
}

// public.gacha_tickets 테이블
// 랜덤 아바타 응모권 (성실도 보상으로 지급)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct GachaTicket {
    pub id: Uuid,
    pub user_id: Uuid,
    pub is_used: Option<bool>,
    pub used_at: Option<DateTime<Utc>>,
    pub acquired_at: Option<DateTime<Utc>>,
}

// public.user_screenshots 테이블
// 아바타 스크린샷 (마이페이지 SNS형 피드)
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct UserScreenshot {
    pub id: Uuid,
    pub user_id: Uuid,
    pub image_url: String,
    pub caption: Option<String>,
    pub created_at: Option<DateTime<Utc>>,
}
