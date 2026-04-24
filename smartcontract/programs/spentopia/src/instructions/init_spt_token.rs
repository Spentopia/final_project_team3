use anchor_lang::prelude::*;
use anchor_spl::token_interface::{Mint, TokenInterface};
use mpl_token_metadata::{
    instructions::CreateV1CpiBuilder, types::TokenStandard, ID as TOKEN_METADATA_ID,
};

use crate::constants::*;

/// SPT Token 민트와 토큰 메타데이터를 함께 초기화한다.
///
/// - 민트 계정은 PDA로 생성
/// - 민트/업데이트 권한은 `spt_token_authority` PDA가 가진다
/// - 지갑 표시용 name/symbol/uri 메타데이터를 함께 만든다
pub fn init_spt_token_handler(
    ctx: Context<InitSptToken>,
    name: String,
    symbol: String,
    uri: String,
) -> Result<()> {
    let authority_bump = &[ctx.bumps.spt_token_authority];
    let authority_signer_seeds: &[&[u8]] = &[SPT_TOKEN_AUTHORITY_SEED, authority_bump];

    CreateV1CpiBuilder::new(&ctx.accounts.metadata_program.to_account_info())
        .metadata(&ctx.accounts.metadata.to_account_info())
        .mint(&ctx.accounts.spt_token_mint.to_account_info(), false)
        .authority(&ctx.accounts.spt_token_authority.to_account_info())
        .payer(&ctx.accounts.payer.to_account_info())
        .update_authority(&ctx.accounts.spt_token_authority.to_account_info(), true)
        .system_program(&ctx.accounts.system_program.to_account_info())
        .sysvar_instructions(&ctx.accounts.sysvar_instructions.to_account_info())
        .spl_token_program(Some(&ctx.accounts.token_program.to_account_info()))
        .name(name)
        .symbol(symbol)
        .uri(uri)
        .seller_fee_basis_points(0)
        .token_standard(TokenStandard::Fungible)
        .decimals(SPT_TOKEN_DECIMALS)
        .invoke_signed(&[authority_signer_seeds])?;

    msg!("SPT Token 민트 및 메타데이터 초기화 완료");
    Ok(())
}

#[derive(Accounts)]
pub struct InitSptToken<'info> {
    /// 트랜잭션 수수료와 계정 생성 비용을 지불하는 서명자.
    #[account(mut)]
    pub payer: Signer<'info>,

    /// SPT Token 민트 계정.
    ///
    /// - PDA로 생성된다.
    /// - decimals는 6으로 설정된다.
    /// - 민트 권한(authority)은 `spt_token_authority` PDA가 가진다.
    /// - freeze 권한도 동일한 PDA가 가진다.
    #[account(
        init,
        payer = payer,
        seeds = [SPT_TOKEN_MINT_SEED],
        bump,
        mint::decimals = SPT_TOKEN_DECIMALS,
        mint::authority = spt_token_authority,
        mint::freeze_authority = spt_token_authority,
        mint::token_program = token_program,
    )]
    pub spt_token_mint: InterfaceAccount<'info, Mint>,

    /// SPT 토큰 메타데이터 PDA.
    /// CHECK: mpl-token-metadata CPI 내부에서 검증된다.
    #[account(mut)]
    pub metadata: UncheckedAccount<'info>,

    /// SPT Token의 민트 권한 / freeze 권한 / 메타데이터 authority로 사용할 PDA.
    ///
    /// CHECK: seeds와 bump로 올바른 PDA 주소인지 검증하므로 안전하다.
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// mpl-token-metadata 프로그램.
    /// CHECK: 고정된 프로그램 주소
    #[account(address = TOKEN_METADATA_ID)]
    pub metadata_program: UncheckedAccount<'info>,

    /// CreateV1 CPI 내부에서 필요한 sysvar.
    /// CHECK: 고정 sysvar 주소
    #[account(address = anchor_lang::solana_program::sysvar::instructions::ID)]
    pub sysvar_instructions: UncheckedAccount<'info>,

    /// SPL Token Program.
    pub token_program: Interface<'info, TokenInterface>,

    /// System Program
    pub system_program: Program<'info, System>,
}
