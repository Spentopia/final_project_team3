use anchor_lang::prelude::*;
use anchor_spl::token_interface::{Mint, TokenInterface};

use crate::constants::*;


/// SPT Token 민트를 초기화하는 실제 handler 함수.
///
/// 실질적인 민트 생성 및 초기화는 Accounts 제약조건의 `#[account(init, mint::...)]` 부분에서 자동으로 처리된다.
/// 따라서 여기서는 추가 로직 없이 로그만 남긴다.
pub fn init_spt_token_handler(_ctx: Context<InitSptToken>) -> Result<()> {
    msg!("SPT Token 민트 초기화 완료");
    Ok(())
}

#[derive(Accounts)]
pub struct InitSptToken<'info> {
    /// 트랜잭션 수수료의 계정 생성 비용을 지불하는 서명자.
    #[account(mut)]
    pub payer: Signer<'info>,

    /// SPT Token 민트 계정.
    ///
    /// - PDA로 생성된다.
    /// - decimals는 6으로 설정된다.
    /// - 민트 권한(authority)은 `spt_token_authority` PDA가 가진다.
    /// - freeze 권한도 동일한 PDA가 가진다.
    ///
    ///  `init`을 사용했기 때문에 이미 존재하는 경우 에러가 발생한다.
    #[account(
        init,
        payer = payer,
        seeds= [SPT_TOKEN_MINT_SEED],
        bump,
        mint::decimals=SPT_TOKEN_DECIMALS,
        mint::authority=spt_token_authority,
        mint::freeze_authority=spt_token_authority,
        mint::token_program=token_program,
    )]
    pub spt_token_mint: InterfaceAccount<'info, Mint>,

    /// SPT Token의 민트 권한 / freeze 권한으로 사용할 PDA.
    ///
    /// 이 계정은 데이터를 저장하는 목적이 아니라 "권한 주소" 역할만 수행한다.
    /// 따라서 별도의 deserialize가 필요 없어서 UncheckedAccount로 받는다.
    ///
    /// CHECK:
    /// seeds와 bump로 올바른 PDA 주소인지 검증하므로 안전하다.
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// SPL Token Program.
    /// 민트 초기화에 필요하다.
    pub token_program: Interface<'info, TokenInterface>,

    /// System Program
    /// 새 계정을 생성할 때 필요하다.
    pub system_program: Program<'info, System>,

}