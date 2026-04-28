use crate::constants::*;
use crate::errors::SpentopiaError;
use crate::state::PlatformConfig;
use anchor_lang::prelude::*;
use anchor_spl::token_interface::{Mint, TokenInterface};
use mpl_token_metadata::{
    instructions::CreateV1CpiBuilder,
    types::{CollectionDetails, PrintSupply, TokenStandard},
    ID as TOKEN_METADATA_ID,
};

/// 프로젝트 전용 아바타 컬렉션 NFT를 1회 초기화한다.
///
/// - collection mint / metadata / master edition 생성
/// - admin 지갑 ATA에 collection NFT 1개 민팅
/// - 이후 개별 아바타 NFT는 이 컬렉션에 verified 상태로 귀속된다.
pub fn init_avatar_collection_handler(
    ctx: Context<InitAvatarCollection>,
    name: String,
    symbol: String,
    uri: String,
) -> Result<()> {
    let platform_config = &mut ctx.accounts.platform_config;
    require!(
        platform_config.collection_mint == Pubkey::default(),
        SpentopiaError::AvatarCollectionAlreadyInitialized
    );

    let collection_mint_bump = &[ctx.bumps.collection_mint];
    let collection_signer_seeds: &[&[u8]] = &[AVATAR_COLLECTION_MINT_SEED, collection_mint_bump];

    let authority_bump = &[platform_config.spt_authority_bump];
    let authority_signer_seeds: &[&[u8]] = &[SPT_TOKEN_AUTHORITY_SEED, authority_bump];

    CreateV1CpiBuilder::new(&ctx.accounts.metadata_program.to_account_info())
        .metadata(&ctx.accounts.collection_metadata.to_account_info())
        .master_edition(Some(&ctx.accounts.collection_master_edition.to_account_info()))
        .mint(&ctx.accounts.collection_mint.to_account_info(), true)
        .authority(&ctx.accounts.spt_token_authority.to_account_info())
        .payer(&ctx.accounts.admin.to_account_info())
        .update_authority(&ctx.accounts.spt_token_authority.to_account_info(), true)
        .system_program(&ctx.accounts.system_program.to_account_info())
        .sysvar_instructions(&ctx.accounts.sysvar_instructions.to_account_info())
        .spl_token_program(Some(&ctx.accounts.token_program.to_account_info()))
        .name(name)
        .symbol(symbol)
        .uri(uri)
        .seller_fee_basis_points(0)
        .token_standard(TokenStandard::NonFungible)
        .collection_details(CollectionDetails::V1 { size: 0 })
        .print_supply(PrintSupply::Zero)
        .invoke_signed(&[collection_signer_seeds, authority_signer_seeds])?;

    platform_config.collection_mint = ctx.accounts.collection_mint.key();

    msg!(
        "아바타 컬렉션 초기화 완료 | collection_mint: {}",
        platform_config.collection_mint
    );

    Ok(())
}

#[derive(Accounts)]
pub struct InitAvatarCollection<'info> {
    #[account(
        mut,
        seeds = [PLATFORM_CONFIG_SEED],
        bump = platform_config.bump,
        has_one = admin,
    )]
    pub platform_config: Account<'info, PlatformConfig>,

    #[account(mut)]
    pub admin: Signer<'info>,

    #[account(
        init,
        payer = admin,
        seeds = [AVATAR_COLLECTION_MINT_SEED],
        bump,
        mint::decimals = 0,
        mint::authority = spt_token_authority,
        mint::freeze_authority = spt_token_authority,
        mint::token_program = token_program,
    )]
    pub collection_mint: InterfaceAccount<'info, Mint>,

    /// CHECK: mpl-token-metadata PDA이며 CPI 내부에서 검증된다.
    #[account(mut)]
    pub collection_metadata: UncheckedAccount<'info>,

    /// CHECK: mpl-token-metadata master edition PDA이며 CPI 내부에서 검증된다.
    #[account(mut)]
    pub collection_master_edition: UncheckedAccount<'info>,

    /// CHECK: seeds + bump으로 검증되는 프로그램 authority PDA다.
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump = platform_config.spt_authority_bump,
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// CHECK: mpl-token-metadata 프로그램 고정 주소
    #[account(address = TOKEN_METADATA_ID)]
    pub metadata_program: UncheckedAccount<'info>,

    /// CHECK: 고정 sysvar 주소
    #[account(address = anchor_lang::solana_program::sysvar::instructions::ID)]
    pub sysvar_instructions: UncheckedAccount<'info>,

    pub token_program: Interface<'info, TokenInterface>,
    pub system_program: Program<'info, System>,
}
