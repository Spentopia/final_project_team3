use anchor_lang::prelude::*;

// 프로젝트 내부 모듈들을 선언
pub mod constants;
pub mod errors;
pub mod instructions;
pub mod state;

// instructions 모듈에서 re-export한 항목들을 가져온다.
use instructions::*;

// 이 프로그램의 온체인 Program ID.
// 배포된 프로그램 주소와 반드시 일치해야 한다.
declare_id!("9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61");

#[program]
pub mod spentopia {
    use super::*;

    /// SPT Token 민트를 초기화하는 instruction.
    ///
    /// - SPT Token 민트 계정을 PDA로 생성한다.
    /// - 민트 권한(authority)은 프로그램 PDA가 가진다.
    /// - decimals는 6으로 설정된다.
    /// - `name`, `symbol`, `uri`를 받아 토큰 메타데이터도 함께 생성한다.
    pub fn init_spt_token(
        ctx: Context<InitSptToken>,
        name: String,
        symbol: String,
        uri: String,
    ) -> Result<()> {
        init_spt_token_handler(ctx, name, symbol, uri)
    }

    /// 플랫폼 전역 설정을 초기화한다.
    ///
    /// - `fee_rate`: 마켓 거래 수수료 소각률 (basis points, 200 = 2%)
    /// - `max_supply`: SPT 최대 발행량 (base units, 1억 SPT = 100_000_000_000_000)
    /// - 이후 모든 관리자 전용 instruction의 권한 기준이 되는 계정을 생성한다.
    pub fn init_platform(ctx: Context<InitPlatform>, fee_rate: u16, max_supply: u64) -> Result<()> {
        init_platform_handler(ctx, fee_rate, max_supply)
    }

    /// 프로젝트 전용 아바타 컬렉션 NFT를 초기화한다.
    ///
    /// - `name`: 컬렉션 이름
    /// - `symbol`: 컬렉션 심볼
    /// - `uri`: 컬렉션 metadata JSON URI
    pub fn init_avatar_collection(
        ctx: Context<InitAvatarCollection>,
        name: String,
        symbol: String,
        uri: String,
    ) -> Result<()> {
        init_avatar_collection_handler(ctx, name, symbol, uri)
    }

    /// 성실도 보상 SPI를 유저에게 민팅한다.
    ///
    /// - `amount`: 민팅할 SPT 수량 (base units 기준, 1 SPT = 1_000_000)
    /// - 백엔드(Axum)가 admin 키페어로 서명해서 호출한다.
    pub fn mint_spt_to_user(ctx: Context<MintSptToUser>, amount: u64) -> Result<()> {
        mint_spt_to_user_handler(ctx, amount)
    }

    /// 아바타 파츠 NFT를 유저에게 민팅한다.
    ///
    /// - `mint_seed`: 민트 고유 식별자 (백엔드 관리, PDA seed에 사용)
    /// - `name`: NFT 이름
    /// - `symbol`: NFT 심볼
    /// - `uri`: Pinata metadata JSON URI
    pub fn mint_avatar_nft(
        ctx: Context<MintAvatarNft>,
        mint_seed: String,
        name: String,
        symbol: String,
        uri: String,
    ) -> Result<()> {
        mint_avatar_nft_handler(ctx, mint_seed, name, symbol, uri)
    }

    /// NFT를 마켓에 판매 등록한다.
    ///
    /// - `price`: 판매 가격 (SPT base units)
    /// - 유저가 프론트에서 지갑 서명으로 직접 호출
    pub fn list_nft(ctx: Context<ListNft>, price: u64) -> Result<()> {
        list_nft_handler(ctx, price)
    }

    /// NFT를 SPT로 구매한다.
    ///
    /// - SPT 결제 → 수수료 5% 플랫폼 수령 + 나머지 판매자 수령
    /// - escrow NFT → 구매자 ATA 전송
    /// - 거래 완료 후 ListingAccount + escrow ATA 소멸
    pub fn buy_nft(ctx: Context<BuyNft>) -> Result<()> {
        buy_nft_handler(ctx)
    }

    /// 판매 등록을 취소한다.
    ///
    /// - escrow NFT → 판매자 ATA 반환
    /// - ListingAccount → escrow ATA 소멸
    /// - 판매자 본인만 호출 가능
    pub fn cancel_listing(ctx: Context<CancelListing>) -> Result<()> {
        cancel_listing_handler(ctx)
    }

    /// 아바타 NFT를 유저에게 직접 전송한다.
    ///
    /// 용도: 콘테스트 수상자, 이벤트 당첨자 특별 지급
    /// - admin 키페어로 백엔드에서 호출
    /// - mint_avatar_nft와 달리 이미 민팅된 NFT를 admin 보유분에서 전송
    pub fn transfer_avatar_nft(ctx: Context<TransferAvatarNft>) -> Result<()> {
        transfer_avatar_nft_handler(ctx)
    }
}
