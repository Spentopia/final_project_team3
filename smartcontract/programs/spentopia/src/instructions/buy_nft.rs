use anchor_lang::prelude::*;
use anchor_spl::associated_token::AssociatedToken;
use anchor_spl::token_interface::{
    burn, close_account, transfer_checked, Burn, CloseAccount, Mint, TokenAccount, TokenInterface,
    TransferChecked,
};

use crate::constants::*;
use crate::errors::SpentopiaError;
use crate::state::{ListingAccount, PlatformConfig};

/// NFT를 SPT로 구매하는 handler
///
/// 1. 구매자 SPT → 수수료(fee_rate%) 소각 + 나머지(판매자) 지급
/// 2. escrow NFT → 구매자 ATA 전송
/// 3. ListingAccount + escrow ATA close (rent 반환)
pub fn buy_nft_handler(ctx: Context<BuyNft>) -> Result<()> {
    // 본인 NFT 구매 방지
    require!(
        ctx.accounts.buyer.key() != ctx.accounts.seller.key(),
        SpentopiaError::SelfPurchase
    );

    let listing = &ctx.accounts.listing;
    let fee_rate = ctx.accounts.platform_config.fee_rate as u64;
    let price = listing.price;

    // 수수료 계산 (basis points)
    // 예: price = 1_000_000, fee_rate=200 → fee=20_000(소각), seller_amount=980_000
    let fee = price
        .checked_mul(fee_rate)
        .ok_or(SpentopiaError::ArithmeticOverflow)?
        .checked_div(10_000)
        .ok_or(SpentopiaError::ArithmeticOverflow)?;
    let seller_amount = price
        .checked_sub(fee)
        .ok_or(SpentopiaError::ArithmeticOverflow)?;

    // Step 1: 구매자 → 판매자 SPT 전송 (수수료 제외 금액)
    transfer_checked(
        CpiContext::new(
            ctx.accounts.token_program.to_account_info(),
            TransferChecked{
                from: ctx.accounts.buyer_spt_account.to_account_info(),
                mint: ctx.accounts.spt_token_mint.to_account_info(),
                to: ctx.accounts.seller_spt_account.to_account_info(),
                authority: ctx.accounts.buyer.to_account_info(),
            },
        ),
        seller_amount,
        SPT_TOKEN_DECIMALS,
    )?;

    // Step 2: 수수료 SPT 소각 (디플레이션 메커니즘)
    // fee_rate=0 이면 소각 생략
    if fee > 0 {
        burn(
            CpiContext::new(
                ctx.accounts.token_program.to_account_info(),
                Burn {
                    mint: ctx.accounts.spt_token_mint.to_account_info(),
                    from: ctx.accounts.buyer_spt_account.to_account_info(),
                    authority: ctx.accounts.buyer.to_account_info(),
                },
            ),
            fee,
        )?;
    }

    // Step 3: escrow → 구매자 ATA로 NFT 전송
    // escrow authority = listing PDA → invoke_signed 필요
    let seller_key = listing.seller.key();
    let nft_mint_key = listing.nft_mint.key();
    let listing_bump = &[listing.bump];
    let listing_signer_seeds: &[&[u8]] = &[
        LISTING_SEED,
        seller_key.as_ref(),
        nft_mint_key.as_ref(),
        listing_bump,
    ];

    transfer_checked(
        CpiContext::new_with_signer(
            ctx.accounts.token_program.to_account_info(),
            TransferChecked{
                from: ctx.accounts.escrow_token_account.to_account_info(),
                mint: ctx.accounts.nft_mint.to_account_info(),
                to: ctx.accounts.buyer_nft_account.to_account_info(),
                authority: ctx.accounts.listing.to_account_info(),
            },
            &[listing_signer_seeds],
        ),
        1,  // NFT 1개
        0, // decimals = 0
    )?;

    // Step 4: escrow ATA close → rent를 판매자에게 반환
    close_account(CpiContext::new_with_signer(
        ctx.accounts.token_program.to_account_info(),
        CloseAccount{
            account: ctx.accounts.escrow_token_account.to_account_info(),
            destination: ctx.accounts.seller.to_account_info(),
            authority: ctx.accounts.listing.to_account_info(),
        },
        &[listing_signer_seeds],
    ))?;

    msg!(
        "NFT 구매 완료 | buyer: {} | seller: {} | price: {} SPT | burned: {} SPT",
        ctx.accounts.buyer.key(),
        ctx.accounts.seller.key(),
        price,
        fee
    );


    Ok(())
}

#[derive(Accounts)]
pub struct BuyNft<'info> {

    /// 플랫폼 설정 계정. fee_rate 읽기용.
    #[account(
        seeds = [PLATFORM_CONFIG_SEED],
        bump = platform_config.bump,
    )]
    pub platform_config: Box<Account<'info, PlatformConfig>>,

    /// 구매자. 직접 서명.
    #[account(mut)]
    pub buyer: Signer<'info>,

    /// 판매자 주소.
    /// listing.seller와 일치하는지 검증.
    /// CHECK: has_one = seller로 검증
    #[account(mut)]
    pub seller: SystemAccount<'info>,

    /// 판매 등록 계정.
    ///
    /// - has_one = seller: listing.seller == seller.key() 자동 검증
    /// - has_one = nft_mint: listing.nft_mint == nft.mint.key() 자동 검증
    /// - close = seller: 거래 완료 후 rent를 판매자에게 반환하며 계정 소멸
    #[account(
        mut,
        seeds = [LISTING_SEED, seller.key().as_ref(), nft_mint.key().as_ref()],
        bump = listing.bump,
        has_one = seller,
        has_one = nft_mint,
        close = seller,
    )]
    pub listing: Box<Account<'info, ListingAccount>>,

    /// 판매 중인 NFT 민트.
    pub nft_mint: Box<InterfaceAccount<'info, Mint>>,

    /// SPT 민트 계정. transfer_checked 및 burn에서 decimals 검증 / supply 감소용.
    #[account(
        mut,
        seeds = [SPT_TOKEN_MINT_SEED],
        bump = platform_config.spt_mint_bump,
    )]
    pub spt_token_mint: Box<InterfaceAccount<'info, Mint>>,

    /// 구매자 SPT ATA. 결제 + 소각 출처.
    #[account(
        mut,
        associated_token::mint = spt_token_mint,
        associated_token::authority = buyer,
        associated_token::token_program = token_program,
    )]
    pub buyer_spt_account: Box<InterfaceAccount<'info, TokenAccount>>,

    /// 판매자 SPT ATA. 판매 대금 수령.
    #[account(
        mut,
        associated_token::mint = spt_token_mint,
        associated_token::authority = seller,
        associated_token::token_program = token_program,
    )]
    pub seller_spt_account: Box<InterfaceAccount<'info, TokenAccount>>,

    /// escrow NFT 보관 ATA.
    /// 거래 완료 후 close.
    #[account(
        mut,
        seeds = [ESCROW_SEED, listing.key().as_ref()],
        bump = listing.escrow_bump,
        token::mint = nft_mint,
        token::authority = listing,
        token::token_program = token_program,
    )]
    pub escrow_token_account: Box<InterfaceAccount<'info, TokenAccount>>,

    /// 구매자 NFT ATA. 없으면 자동 생성.
    #[account(
        init_if_needed,
        payer = buyer,
        associated_token::mint = nft_mint,
        associated_token::authority = buyer,
        associated_token::token_program = token_program,
    )]
    pub buyer_nft_account: Box<InterfaceAccount<'info, TokenAccount>>,

    pub token_program: Interface<'info, TokenInterface>,
    pub associated_token_program: Program<'info, AssociatedToken>,
    pub system_program: Program<'info, System>,
}
