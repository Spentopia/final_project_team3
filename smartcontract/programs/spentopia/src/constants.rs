/// SPT Token 민트 PDA를 만들 때 사용할 seed.
/// 이 seed를 기준으로 Spentopia의 공식 민트 주소가 고정된다.
pub const SPT_TOKEN_MINT_SEED: &[u8] = b"spt_token_mint";

/// SPT Token의 민트 권한(authority) PDA를 만들 때 사용할 seed.
/// 프로그램이 이 PDA를 통해 민팅 권한을 행사하게 된다.
pub const SPT_TOKEN_AUTHORITY_SEED: &[u8] = b"spt_token_authority";

/// SPT Token의 소수점 자리수.
/// 예: 1 SPT = 1_000_000 base units
pub const SPT_TOKEN_DECIMALS: u8 = 6;

/// 플랫폼 전역 설정 계정 PDA seed.
pub const PLATFORM_CONFIG_SEED: &[u8] = b"platform_config";

/// fee_rate 최댓값 (basis points 기준, 10000 = 100%).
/// 수수료가 100^를 초과하는 건 의미 없으므로 상한을 상수로 고정.
pub const MAX_FEE_RATE: u16 = 10_000;


/// 아바타 NFT 민트 PDA seed.
/// 유저별 + 아이템별로 고유한 민트를 만들기 위해 user_pubkey + item_id를 함께 사용.
pub const AVATAR_MINT_SEED: &[u8] = b"avatar_mint";

/// 판매 등록 계정 PDA seed.
/// seller + nft_mint 조합 → 동일 NFT 중복 등록 방지
pub const LISTING_SEED: &[u8] = b"listing";

/// 판매 중 NFT를 보관하는 escrow ATA seed.
pub const ESCROW_SEED: &[u8] = b"escrow";
