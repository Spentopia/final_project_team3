use anchor_lang::prelude::*;
use anchor_spl::token_interface::{
    close_account, transfer_checked, CloseAccount, Mint, TokenAccount, TokenInterface,
    TransferChecked,
};

use crate::constants::*;
use crate::state::ListingAccount;

/// 판매 등록을 취소하는 handler.
///
/// - escrow → 판매자 ATA로 NFT 반환
/// - escrow ATA + ListingAccount 소멸 (rent 판매자 반환)
/// - 판매자 본인만 호출 가능
pub fn cancel_listing_handler(ctx: Context<CancelListing>) -> Result<()> {
    let listing = &ctx.accounts.listing;

    // listing PDA 서명용 seeds
    let seller_key = listing.seller.key();
    let nft_mint_key = listing.nft_mint.key();
    let listing_bump = &[listing.bump];
    let listing_signer_seeds: &[&[u8]] = &[
        LISTING_SEED,
        seller_key.as_ref(),
        nft_mint_key.as_ref(),
        listing_bump,
    ];

    // Step 1: escrow → 판매자 ATA로 NFT 반환
    transfer_checked(
        CpiContext::new_with_signer(
            ctx.accounts.token_program.to_account_info(),
            TransferChecked {
                from: ctx.accounts.escrow_token_account.to_account_info(),
                mint: ctx.accounts.nft_mint.to_account_info(),
                to: ctx.accounts.seller_nft_account.to_account_info(),
                authority: ctx.accounts.listing.to_account_info(),
            },
            &[listing_signer_seeds],
        ),
        1, // NFT 1개
        0, // decimals = 0
    )?;

    // Step 2: escrow ATA close → rent 판매자 반환
    close_account(CpiContext::new_with_signer(
        ctx.accounts.token_program.to_account_info(),
        CloseAccount {
            account: ctx.accounts.escrow_token_account.to_account_info(),
            destination: ctx.accounts.seller.to_account_info(),
            authority: ctx.accounts.listing.to_account_info(),
        },
        &[listing_signer_seeds],
    ))?;

    msg!(
        "판매 취소 완료 | seller: {} | mint: {}",
        ctx.accounts.seller.key(),
        ctx.accounts.nft_mint.key(),
    );

    Ok(())
}

#[derive(Accounts)]
pub struct CancelListing<'info> {
    /// 판매자. 본인만 취소 가능.
    #[account(mut)]
    pub seller: Signer<'info>,

    /// 판매 등록 계정.
    ///
    /// - has_one = seller; 판매자 본인인지 검증. 타인이 취소 불가.
    /// - has_one = nft_mint: 올바른 NFT 민트인지 검증.
    /// - close = seller: 취소 후 rent를 판매자에게 반환하며 계정 소멸.
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

    /// escrow NFT 보관 ATA.
    #[account(
        mut,
        seeds = [ESCROW_SEED, listing.key().as_ref()],
        bump = listing.escrow_bump,
        token::mint = nft_mint,
        token::authority = listing,
        token::token_program = token_program,
    )]
    pub escrow_token_account: Box<InterfaceAccount<'info, TokenAccount>>,

    /// 판매자 NFT ATA. NFT 반환 목적지.
    #[account(
        mut,
        associated_token::mint = nft_mint,
        associated_token::authority = seller,
        associated_token::token_program = token_program,
    )]
    pub seller_nft_account: Box<InterfaceAccount<'info, TokenAccount>>,

    pub token_program: Interface<'info, TokenInterface>,
    pub system_program: Program<'info, System>,
}
