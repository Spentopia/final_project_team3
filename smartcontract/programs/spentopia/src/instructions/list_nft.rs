use anchor_lang::prelude::*;
use anchor_spl::token_interface::{
    transfer_checked, Mint, TokenAccount, TokenInterface, TransferChecked,
};

use crate::constants::*;
use crate::state::ListingAccount;

/// NFT를 마켓에 판매 등록하는 handler.
///
/// - 유저의 NFT ATA → escrow PDA ATA로 NFT 전송
/// - ListingAccount에 판매 조건(seller, price) 저장
/// - 유저가 프론트에서 직접 서명해서 호출
pub fn list_nft_handler(ctx: Context<ListNft>, price: u64) -> Result<()> {
    // 가격 0은 의미 없으므로 차단
    require!(price > 0, crate::errors::SpentopiaError::InvalidPrice);

    // ListingAccount 초기화
    let listing = &mut ctx.accounts.listing;
    listing.seller = ctx.accounts.seller.key();
    listing.nft_mint = ctx.accounts.nft_mint.key();
    listing.price = price;
    listing.bump = ctx.bumps.listing;
    listing.escrow_bump = ctx.bumps.escrow_token_account;

    // 유저 ATA → escrow ATA로 NFT 1개 전송
    // seller가 서명자이므로 invoke_signed 불필요 (일반 CpiContext)
    transfer_checked(
        CpiContext::new(
            ctx.accounts.token_program.to_account_info(),
            TransferChecked {
                from: ctx.accounts.seller_token_account.to_account_info(),
                mint: ctx.accounts.nft_mint.to_account_info(),
                to: ctx.accounts.escrow_token_account.to_account_info(),
                authority: ctx.accounts.seller.to_account_info(),
            },
        ),
        1, // NFT는 항상 1개
        0, // decimals = 0
    )?;

    msg!(
        "NFT 판매 등록 완료 | seller: {} | mint: {} | price: {} SPT",
        ctx.accounts.seller.key(),
        ctx.accounts.nft_mint.key(),
        price
    );

    Ok(())
}

#[derive(Accounts)]
pub struct ListNft<'info> {
    /// 판매자. 유저가 직접 서명.
    #[account(mut)]
    pub seller: Signer<'info>,

    /// 판매할 NFT 민트 계정.
    pub nft_mint: InterfaceAccount<'info, Mint>,

    /// 판매자의 NFT ATA.
    /// NFT가 실제로 있는지 검증 (amount >= 1)
    #[account(
        mut,
        associated_token::mint = nft_mint,
        associated_token::authority = seller,
        associated_token::token_program = token_program,
        constraint = seller_token_account.amount >= 1,
    )]
    pub seller_token_account: InterfaceAccount<'info, TokenAccount>,

    /// 판매 등록 정보 계정.
    ///
    /// - seller + nft_mint를 seed에 포함 → 동일 NFT 중복 등록 시 init 에러
    #[account(
        init,
        payer = seller,
        space = ListingAccount::LEN,
        seeds = [LISTING_SEED, seller.key().as_ref(), nft_mint.key().as_ref()],
        bump,
    )]
    pub listing: Account<'info, ListingAccount>,

    /// escrow NFT 보관 ATA
    ///
    /// - authority = listing PDA → 판매자도 임의인출 불가
    /// - 구매 or 취소 시에만 이 NFT가 이동
    #[account(
        init,
        payer = seller,
        seeds = [ESCROW_SEED, listing.key().as_ref()],
        bump,
        token::mint = nft_mint,
        token::authority = listing,
        token::token_program = token_program,
    )]
    pub escrow_token_account: InterfaceAccount<'info, TokenAccount>,

    pub token_program: Interface<'info, TokenInterface>,
    /// CHECK: 표준 SPL Associated Token Program 고정 주소
    #[account(address = pubkey!("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJe8bYh"))]
    pub associated_token_program: UncheckedAccount<'info>,
    pub system_program: Program<'info, System>,
}
