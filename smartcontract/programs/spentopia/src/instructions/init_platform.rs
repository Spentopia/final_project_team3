use anchor_lang::prelude::*;

use crate::constants::*;
use crate::errors::SpentopiaError;
use crate::state::PlatformConfig;

/// `init_platform` instruction의 실제 실행 로직.
///
/// - Accounts 제약조건에서 계정 생성 + 공간 할당은 이미 완료된 상태다.
/// - 여기선 필드 초기화 + 유효성 검사만 담당한다.
pub fn init_platform_handler(
    ctx: Context<InitPlatform>,
    fee_rate: u16,
    max_supply: u64,
) -> Result<()> {
    // fee_rate 유효성 검사.
    // Accounts 제약조건에서는 숫자 범위 검사를 할 수 없으므로 handler에서 처리.
    require!(fee_rate <= MAX_FEE_RATE, SpentopiaError::InvalidFeeRate);

    let config = &mut ctx.accounts.platform_config;

    // admin: 이 instruction을 호출한 서명자를 관리자로 등록.
    // 이후 모든 관리자 전용 instruction은 `has_one = admin`으로 이 값과 대조한다.
    config.admin = ctx.accounts.admin.key();

    // fee_rate: buy_nft에서 소각 비율 계산에 사용할 값
    config.fee_rate = fee_rate;

    // bump: PDA 서명이 필요한 CPI에서 seeds + bump 조합으로 활용.
    config.bump = ctx.bumps.platform_config;
    config.spt_mint_bump = ctx.bumps.spt_token_mint;
    config.spt_authority_bump = ctx.bumps.spt_token_authority;

    // 발행량 초기값: 0, 최대 발행량 설정
    config.total_minted = 0;
    config.max_supply = max_supply;

    msg!(
        "플랫폼 초기화 완료 | admin: {} | fee_rate: {} bps | max_supply: {}",
        config.admin,
        config.fee_rate,
        config.max_supply,
    );
    Ok(())
}

/// `init_platform` instruction에 필요한 계정 목록.
#[derive(Accounts)]
pub struct InitPlatform<'info> {
    /// 이 instruction을 호출하는 관리자 서명자.
    ///
    /// - 계정 생성 비용(rent)을 지불한다.
    /// - 이 주소가 PlatformConfig.admin으로 저장되어 이후 권한 검증 기준이 된다.
    #[account(mut)]
    pub admin: Signer<'info>,
    /// 플랫폼 전역 설정 계정.
    ///
    /// - PDA로 생성된다 (`init` 사용 -> 이미 존재하면 에러).
    /// - `space`는 PlatformConfig::LEN으로 정확히 지정해야 rent 계산이 맞다.
    /// - seeds가 고정 상수이므로 이 계정은 전체 프로그램에서 단 1개만 존재할 수 있다.
    #[account(
        init,
        payer=admin,
        space=PlatformConfig::LEN,
        seeds=[PLATFORM_CONFIG_SEED],
        bump,
    )]
    pub platform_config: Account<'info, PlatformConfig>,

    /// bump 저장 목적으로 추가.
    /// CHECK: seeds + bump 검증
    #[account(
        seeds = [SPT_TOKEN_MINT_SEED],
        bump,
    )]
    pub spt_token_mint: UncheckedAccount<'info>,

    /// bump 저장 목적으로 추가.
    /// CHECK: seeds + bump 검증
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump,
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// 새 계정 생성에 필요한 System Program.
    pub system_program: Program<'info, System>,
}
