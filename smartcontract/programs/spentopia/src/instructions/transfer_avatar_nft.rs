use anchor_lang::prelude::*;
use anchor_spl::token_interface::{
    transfer_checked, Mint, TokenAccount, TokenInterface, TransferChecked,
};

use crate::constants::*;
use crate::state::PlatformConfig;

/// 아바타 NFT를 특정 유저에게 직접 전송하는 handler.
///
/// 용도:
/// - 콘테스트 1~3등 시즌 한정 아이템 직접 지급
/// - 이벤트 수상자 특별 보상
/// - 백엔드 admin 키페어로 호출
pub fn transfer_avatar_nft_handler(ctx: Context<TransferAvatarNft>) -> Result<()> {
    // admin ATA → 유저 ATA로 NFT 1개 전송
    // admin이 Signer이므로 invoke_signed 불필요
    transfer_checked(
        CpiContext::new(
            ctx.accounts.token_program.to_account_info(),
            TransferChecked {
                from: ctx.accounts.admin_nft_account.to_account_info(),
                mint: ctx.accounts.nft_mint.to_account_info(),
                to: ctx.accounts.user_nft_account.to_account_info(),
                authority: ctx.accounts.admin.to_account_info(),
            },
        ),
        1, // NFT 1개
        0, // decimals = 0
    )?;

    msg!(
        "아바타 NFT 전송 완료 | admin: {} | 수신자: {} | mint: {}",
        ctx.accounts.admin.key(),
        ctx.accounts.user.key(),
        ctx.accounts.nft_mint.key(),
    );
    Ok(())
}

#[derive(Accounts)]
pub struct TransferAvatarNft<'info> {
    /// 플랫폼 설정 계정.
    /// has_one = admin으로 관리자 검증.
    #[account(
        seeds = [PLATFORM_CONFIG_SEED],
        bump = platform_config.bump,
        has_one = admin,
    )]
    pub platform_config: Box<Account<'info, PlatformConfig>>,

    /// 관리자 서명자.
    /// 백엔드가 admin 키페어로 서명해서 호출.
    #[account(mut)]
    pub admin: Signer<'info>,

    /// NFT를 받을 유저 지갑 주소.
    /// CHECK: 단순 수신자
    pub user: SystemAccount<'info>,

    /// 전송할 NFT 민트 계정.
    pub nft_mint: Box<InterfaceAccount<'info, Mint>>,

    /// admin의 NFT ATA. 전송 출처.
    /// NFT가 실제로 있는지 검증.
    #[account(
        mut,
        associated_token::mint = nft_mint,
        associated_token::authority = admin,
        associated_token::token_program = token_program,
        constraint = admin_nft_account.amount >= 1,
    )]
    pub admin_nft_account: Box<InterfaceAccount<'info, TokenAccount>>,

    /// 유저의 NFT ATA. 없으면 자동 생성.
    /// 생성 비용은 admin이 지불.
    #[account(
        init_if_needed,
        payer = admin,
        associated_token::mint = nft_mint,
        associated_token::authority = user,
        associated_token::token_program = token_program,
    )]
    pub user_nft_account: Box<InterfaceAccount<'info, TokenAccount>>,

    pub token_program: Interface<'info, TokenInterface>,
    /// CHECK: 표준 SPL Associated Token Program 고정 주소
    #[account(address = pubkey!("ATokenGPvbdGVxr1b2hvZbsiqW5xWH25efTNsLJA8knL"))]
    pub associated_token_program: UncheckedAccount<'info>,
    pub system_program: Program<'info, System>,
}
