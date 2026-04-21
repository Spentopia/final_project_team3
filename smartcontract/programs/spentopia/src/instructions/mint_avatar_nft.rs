use crate::constants::*;
use crate::state::PlatformConfig;
use anchor_lang::prelude::*;
use anchor_spl::associated_token::AssociatedToken;
use anchor_spl::token_interface::{Mint, TokenAccount, TokenInterface};
use mpl_token_metadata::{
    instructions::{CreateV1CpiBuilder, MintV1CpiBuilder},
    types::{PrintSupply, TokenStandard},
    ID as TOKEN_METADATA_ID,
};

/// 아바타 파츠 NFT를 민팅하는 handler
///
/// - 백엔드가 랜덤 파츠 결정 → Pinata에 metadata 업로드 → URI 확보 → 이 instruction 호출
/// - NFT 1개 = 파츠 1개 (신발, 모자, 티 등)
/// - admin 단독 서명, 유저 서명 불필요
pub fn mint_avatar_nft_handler(
    ctx: Context<MintAvatarNft>,
    item_id: String, // 파츠 식별자 (백엔드 관리). PDA seed에도 사용됨
    name: String,    // NFT 이름 (예: "Spentopida Cap #001")
    symbol: String,  // NFT 심볼 (예: "SPT")
    uri: String,     // Pinata에 올린 metadata JSON URI
) -> Result<()> {
    let platform_config = &ctx.accounts.platform_config;

    // avatar_mint PDA 서명용 seeds.
    // CreateV1, MintV1 CPI 모두 mint가 서명자여야 하므로 signer_seeds 필요.
    let item_id_bytes = item_id.as_bytes();
    let user_key = ctx.accounts.user.key();
    let user_key_bytes = user_key.as_ref();
    let mint_bump = &[ctx.bumps.avatar_mint];

    let mint_signer_seeds: &[&[u8]] = &[AVATAR_MINT_SEED, user_key_bytes, item_id_bytes, mint_bump];
    let signer_seeds = &[mint_signer_seeds];

    // Step 1: NFT 메타데이터 계정 생성 (CreateV1)
    // CreateV1은 민트 계정 생성 + 메타데이터 계정 생성을 한 번에 처리한다.
    // TokenStandard::NonFungible → supply가 1로 고정된 진짜 NFT.
    CreateV1CpiBuilder::new(&ctx.accounts.metadata_program.to_account_info())
        .metadata(&ctx.accounts.metadata.to_account_info())
        .mint(&ctx.accounts.avatar_mint.to_account_info(), true) // true = mint가 서명자
        .authority(&ctx.accounts.spt_token_authority.to_account_info())
        .payer(&ctx.accounts.admin.to_account_info())
        .update_authority(&ctx.accounts.spt_token_authority.to_account_info(), true)
        .system_program(&ctx.accounts.system_program.to_account_info())
        .sysvar_instructions(&ctx.accounts.sysvar_instructions.to_account_info())
        .spl_token_program(Some(&ctx.accounts.token_program.to_account_info()))
        .name(name)
        .symbol(symbol)
        .uri(uri)
        .seller_fee_basis_points(0) // 2차 판매 로열티 없음
        .token_standard(TokenStandard::NonFungible)
        .print_supply(PrintSupply::Zero) // 에디션 복제 불가
        .invoke_signed(signer_seeds)?;

    // Step 2: 유저 ATA에 NFT 1개 민팅 (MintV1)
    // MintV1은 실제 토큰을 ATA에 전송한다.
    // authority = spt_token_authority PDA가 서명.
    let authority_bump = &[platform_config.spt_authority_bump];
    let authority_signer_seeds: &[&[u8]] = &[SPT_TOKEN_AUTHORITY_SEED, authority_bump];

    MintV1CpiBuilder::new(&ctx.accounts.metadata_program.to_account_info())
        .token(&ctx.accounts.user_token_account.to_account_info())
        .token_owner(Some(&ctx.accounts.user.to_account_info()))
        .metadata(&ctx.accounts.metadata.to_account_info())
        .mint(&ctx.accounts.avatar_mint.to_account_info())
        .authority(&ctx.accounts.spt_token_authority.to_account_info())
        .payer(&ctx.accounts.admin.to_account_info())
        .system_program(&ctx.accounts.system_program.to_account_info())
        .sysvar_instructions(&ctx.accounts.sysvar_instructions.to_account_info())
        .spl_token_program(&ctx.accounts.token_program.to_account_info())
        .spl_ata_program(&ctx.accounts.associated_token_program.to_account_info())
        .amount(1)
        .invoke_signed(&[authority_signer_seeds])?;

    msg!(
        "아바타 NFT 민팅 완료 | 유저: {} | item_id: {}",
        ctx.accounts.user.key(),
        item_id
    );

    Ok(())
}

#[derive(Accounts)]
#[instruction(item_id: String)]
pub struct MintAvatarNft<'info> {
    /// 플랫폼 설정 계정.
    /// has_one = admin으로 관리자 검증.
    #[account(
        seeds = [PLATFORM_CONFIG_SEED],
        bump = platform_config.bump,
        has_one = admin,
    )]
    pub platform_config: Account<'info, PlatformConfig>,

    /// 관리자 서명자. 계정 생성 비용 지불.
    #[account(mut)]
    pub admin: Signer<'info>,

    /// NFT를 받을 유저 지갑 주소.
    /// CHECK: 단순 수신자
    pub user: SystemAccount<'info>,

    /// 아바타 파츠 NFT 민트 계정.
    ///
    /// - user_pubkey + item_id를 seed에 포함 → 유저별 + 파츠별 고유 민트 보장
    /// - item_id가 같으면 같은 파츠 → 중복 민팅 방지 가능
    #[account(
        init,
        payer = admin,
        seeds = [AVATAR_MINT_SEED, user.key().as_ref(), item_id.as_bytes()],
        bump,
        mint::decimals = 0,     // NFT는 소수점 없음
        mint::authority = spt_token_authority,
        mint::freeze_authority = spt_token_authority,
        mint::token_program = token_program,
    )]
    pub avatar_mint: InterfaceAccount<'info, Mint>,

    /// NFT 메타데이터 계정.
    /// mpl-token-metadata가 관리하는 계정. 주소는 프로그램이 결정.
    ///
    /// CHECK: mpl-token-metadata CPI 내부에서 검증
    #[account(mut)]
    pub metadata: UncheckedAccount<'info>,

    /// SPT authority PDA. NFT 민트 권한 + 메타데이터 authority 역할.
    ///
    /// CHECK: seeds + bump 검증
    #[account(
        seeds = [SPT_TOKEN_AUTHORITY_SEED],
        bump = platform_config.spt_authority_bump,
    )]
    pub spt_token_authority: UncheckedAccount<'info>,

    /// 유저의 NFT ATA.
    /// init_if_needed로 없으면 자동 생성.
    #[account(
        init_if_needed,
        payer = admin,
        associated_token::mint=avatar_mint,
        associated_token::authority=user,
        associated_token::token_program = token_program,
    )]
    pub user_token_account: InterfaceAccount<'info, TokenAccount>,

    /// mpl-token-metadata 프로그램.
    /// CHECK: 고정된 프로그램 주소
    #[account(address = TOKEN_METADATA_ID)]
    pub metadata_program: UncheckedAccount<'info>,

    /// CreateV1 / MintV1 CPI 내부에서 내부적으로 필요한 sysvar.
    /// CHECK: 고정 sysvar 주소
    #[account(address = anchor_lang::solana_program::sysvar::instructions::ID)]
    pub sysvar_instructions: UncheckedAccount<'info>,

    pub token_program: Interface<'info, TokenInterface>,
    pub associated_token_program: Program<'info, AssociatedToken>,
    pub system_program: Program<'info, System>,
}
