use anchor_lang::prelude::*;
use anchor_spl::associated_token::AssociatedToken;
use anchor_spl::token_interface::{mint_to, Mint, MintTo, TokenAccount, TokenInterface};

use crate::constants::*;
use crate::errors::SpentopiaError;
use crate::state::PlatformConfig;

/// 성실도 보상 SPT를 유저에게 민팅하는 handler.
///
/// - amount 계산은 백엔드(Axum)가 담당하고, 이 instruction은 검증 + 실행만 한다.
/// - admin 권한 검증은 `has_one = admin`으로 Anchor가 자동 처리한다.
/// - max_supply 초과 시 MaxSupplyExceeded 에러 반환.
pub fn mint_spt_to_user_handler(ctx: Context<MintSptToUser>, amount: u64) -> Result<()> {
    // max_supply 초과 검사 + total_minted 업데이트
    // 블록 스코프로 mutable borrow를 먼저 해소한 뒤 CPI 호출
    let authority_bump = {
        let config = &mut ctx.accounts.platform_config;
        let new_total = config
            .total_minted
            .checked_add(amount)
            .ok_or(SpentopiaError::ArithmeticOverflow)?;
        require!(new_total <= config.max_supply, SpentopiaError::MaxSupplyExceeded);
        config.total_minted = new_total;
        config.spt_authority_bump  // u8은 Copy → 블록 반환 후 config borrow 해소
    };

    // spt_token_authority PDA로 CPI 서명하기 위한 seeds.
    let authority_seeds: &[&[u8]] = &[
        SPT_TOKEN_AUTHORITY_SEED,
        &[authority_bump],
    ];
    let signer_seeds = &[authority_seeds];

    // SPL Token mint_to CPI 호출.
    // mint_authority = spt_token_authority PDA가 서명자 역할을 한다.
    mint_to(
        CpiContext::new_with_signer(
            ctx.accounts.token_program.to_account_info(),
            MintTo {
                mint: ctx.accounts.spt_token_mint.to_account_info(),
                to: ctx.accounts.user_token_account.to_account_info(),
                authority: ctx.accounts.spt_token_authority.to_account_info(),
            },
            signer_seeds,
        ),
        amount,
    )?;

    msg!(
        "SPT 민팅 완료 | 유저: {} | amount: {}",
        ctx.accounts.user.key(),
        amount
    );

    Ok(())
}

#[derive(Accounts)]
pub struct MintSptToUser<'info> {
    /// 플랫폼 전역 설정 계정.
    ///
    /// - `has_one = admin`: platform_config.admin == admin.key() 자동 검증.
    /// 일치하지 않으면 Anchor가 에러를 낸다. require! 직접 안 써도 됨.
    #[account(
        mut,
        seeds = [PLATFORM_CONFIG_SEED],
        bump = platform_config.bump,
        has_one = admin,
    )]
    pub platform_config: Account<'info, PlatformConfig>,

    /// 관리자 서명자.
    /// 백엔드가 admin 키페어로 서명해서 이 instruction을 호출한다.
    #[account(mut)]
    pub admin: Signer<'info>,

    /// SPT를 받을 유저 지갑 주소.
    /// 서명 불필요 - 받는 사람이므로 `UncheckedAccount`가 아닌 `SystemAccount`로 타입 안전하게 설정
    /// CHECK: 단순 수신자 주소이므로 추가 검증 불필요.
    pub user: SystemAccount<'info>,

    /// SPT Token 민트 계정.
    /// seeds로 올바른 PDA인지 검증한다.
    #[account(
        mut,
        seeds = [SPT_TOKEN_MINT_SEED],
        bump,
    )]
    pub spt_token_mint: InterfaceAccount<'info, Mint>,

    /// SPT Token의 mint authority PDA.
    /// 실제 mint_to CPI의 서명자 역할을 한다.
    ///
    /// CHECK: seeds + bump으로 올바른 PDA 주소 검증.
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump,
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// 유저의 SPT ATA (Associated Token Account).
    ///
    /// - `init_if_needed`: 없으면 자동 생성, 있으면 그냥 사용.
    /// - 생성 비용은 admin이 지불한다.
    /// - `associated_token::authority = user`: 이 ATA의 소유자가 user임을 검증.
    /// - `associated_token::mint = spt_token_mint`: SPT 민트 전용 ATA임을 검증.
    #[account(
        init_if_needed,
        payer = admin,
        associated_token::mint = spt_token_mint,
        associated_token::authority = user,
        associated_token::token_program = token_program,
    )]
    pub user_token_account: InterfaceAccount<'info, TokenAccount>,

    pub token_program: Interface<'info, TokenInterface>,
    pub associated_token_program: Program<'info, AssociatedToken>,
    pub system_program: Program<'info, System>,

}