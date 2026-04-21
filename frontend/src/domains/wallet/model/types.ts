// 지갑 관련 API와 프론트 상태에서 공통으로 사용하는 타입 모음
// 백엔드의 실제 DTO 필드명(snake_case)에 맞춰 작성합니다.

// nonce 발급 요청 DTO
// POST /auth/wallet/nonce
export interface WalletNonceRequest {
    // 백엔드 auth/dto.rs의 NonceRequest.wallet_address와 일치해야 함
    wallet_address: string;
}

// nonce 발급 응답 DTO
// POST /auth/wallet/nonce → { nonce: string, message: string }
export interface WalletNonceResponse{
    nonce: string;
    message: string;
}

// 지갑 로그인 요청 DTO
// POST /auth/wallet/login
export interface WalletLoginRequest {
    // 지갑 주소(Base58)
    wallet_address: string;

    // 서버가 발급한 1회용 nonce
    nonce: string;

    // 서버가 내려준 한국어 인증 메시지를 signMessage 한 결과를 Base58 문자열로 인코딩한 값
    signature: string;
}

// 지갑 로그인 응답 DTO
// auth/dto.rs의 LoginResponse 기준
export interface WalletLoginResponse {
    access_token: string;
    refresh_token?: string; // web: 쿠키로 전달, app: body에 포함
    is_new_user: boolean;
}

// 지갑 연동 요청 DTO
// wallet/dto.rs의 LinkWalletRequest 기준
export interface LinkWalletRequest{
    wallet_address: string;
    nonce: string;
    signature: string;
}

export interface UnlinkWalletRequest {
    wallet_address: string;
    nonce: string;
    signature: string;
}

// 지갑 연동 성공 응답 DTO
export interface LinkWalletResponse {
    wallet_address: string;
    message: string;
}

// 지갑 해제 성공 응답 DTO
export interface UnlinkWalletResponse{
    message: string;
}

export interface WalletLinkAvailability {
    canLinkWallet: boolean;
    reason: string | null;
}

// 훅 내부에서 공통 결과를 단순화해서 다루기 위한 타입
export interface WalletActionResult{
    success: boolean;
    message: string;
}

// 현재 어떤 작업이 진행 중인지 표시하기 위한 타입
export type WalletProcessType = 'login' | 'link' | 'unlink' | null;
