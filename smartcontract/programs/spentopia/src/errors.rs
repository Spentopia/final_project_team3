use anchor_lang::prelude::*;

/// 프로젝트 전용 커스텀 에러 코드들.
///
/// 현재 init_spt_token에서는 꼭 필요하지 않지만, 이후 관리자 검증이나 정책 검증 로직이 추가될 때 확장하기 쉽도록
/// 미리 분리해 둔다.
#[error_code]
pub enum SpentopiaError {
    /// 권한이 없는 관리자가 호출했을 때 사용할 수 있는 에러.
    #[msg("권한이 없는 관리자입니다.")]
    UnauthorizedAdmin,

    /// fee_rate가 허용 범위(0~10000 basis points)를 벗어났을 때.
    #[msg("수수료율은 0 이상 10000 이하여야 합니다. (basis points 기준)")]
    InvalidFeeRate,

    /// 가격을 0으로 설정했을 때.
    #[msg("가격은 0보다 커야 합니다.")]
    InvalidPrice,

    /// 산술 연산 오버플로우 발생 시
    #[msg("산술 연산 오버플로우가 발생했습니다.")]
    ArithmeticOverflow,

    /// SPT 최대 발행량 초과 시
    #[msg("SPT 최대 발행량을 초과했습니다.")]
    MaxSupplyExceeded,

    /// 본인 NFT 구매 시도 시
    #[msg("본인의 NFT는 구매할 수 없습니다.")]
    SelfPurchase,

    /// 아바타 컬렉션이 이미 초기화된 경우
    #[msg("아바타 컬렉션이 이미 초기화되어 있습니다.")]
    AvatarCollectionAlreadyInitialized,

    /// 아바타 컬렉션이 아직 초기화되지 않은 경우
    #[msg("아바타 컬렉션이 아직 초기화되지 않았습니다.")]
    AvatarCollectionNotInitialized,
}
