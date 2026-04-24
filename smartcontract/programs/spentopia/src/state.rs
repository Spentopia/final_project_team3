use anchor_lang::prelude::*;

/// 온체인 상태(Account) 구조체를 모아두는 파일.
///
/// - PDA로 생성되어 프로그램 전체에서 단 1개만 존재한다.
/// - 이후 instruction들이 `has_one = admin`으로 관리자 권한을 검증한다.
#[account]
pub struct PlatformConfig {
    /// 플랫폼 관리자 주소.
    /// `mint_spt_to_user` 등 백엔드 전용 instruction에서 `has_one = admin`으로 검증된다.
    pub admin: Pubkey,

    /// 마켓 거래 수수료율 (basis points).
    /// 500 = 5%. `buy_nft`에서 이 값을 읽어 수수료를 계산한다.
    pub fee_rate: u16,

    /// 이 계정 PDA의 bump.
    /// CPI 서명이 필요한 상황에서 seeds + bump 조합으로 PDA 서명에 사용된다.
    pub bump: u8,

    /// SPT 민트 PDA의 bump.
    pub spt_mint_bump: u8,

    /// SPT authority PDA의 bump.
    /// mint_spt_to_user CPI 서명 시 find_program_address 재탐색 없이 바로 사용.
    pub spt_authority_bump: u8,

    /// 지금까지 민팅된 SPT 총 누적량 (base units).
    /// mint_spt_to_user 호출 시마다 증가. max_supply 초과 시 에러.
    pub total_minted: u64,

    /// SPT 최대 발행 가능량 (base units).
    /// init_platform 시 설정. 1억 SPT = 100_000_000_000_000 (decimals=6 기준).
    pub max_supply: u64,

    /// Spentopia 아바타 컬렉션 mint 주소.
    /// 아직 초기화되지 않았다면 Pubkey::default() 상태다.
    pub collection_mint: Pubkey,
}

impl PlatformConfig {
    /// 계정이 온체인에서 차지하는 공간 (bytes).
    ///
    /// 8: Anchor discriminator (모든 #[account]에 자동 추가됨)
    /// 32: admin (Pubkey)
    /// 2: fee_rate (u16)
    /// 1: bump (u8)
    /// 1: spt_mint_bump: u8
    /// 1: spt_authority_bump: u8
    /// 8: total_minted (u64)
    /// 8: max_supply (u64)
    /// 32: collection_mint (Pubkey)
    pub const LEN: usize = 8 + 32 + 2 + 1 + 1 + 1 + 8 + 8 + 32;
}

/// NFT 판매 등록 정보를 온체인에 저장하는 계정.
///
/// - 판매 등록 시 생성, 구매 or 취소 시 소멸(close)
/// - escrow PDA가 NFT를 보관하는 동안 이 계정이 판매 조건을 기록
#[account]
pub struct ListingAccount {
    /// 판매자 주소. cancel_listring / buy_nft에서 검증 기준.
    pub seller: Pubkey,

    /// 판매 중인 NFT 민트 주소.
    pub nft_mint: Pubkey,

    /// 판매 가격 (SPT base units 기준).
    pub price: u64,

    /// 이 계정의 bump.
    pub bump: u8,

    /// escrow ATA의 bump,
    pub escrow_bump: u8,
}

impl ListingAccount {
    /// 8 + 32 + 32 + 8 + 1 + 1
    pub const LEN: usize = 8 + 32 + 32 + 8 + 1 + 1;
}
