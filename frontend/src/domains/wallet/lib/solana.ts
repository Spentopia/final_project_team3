import bs58 from "bs58";

// 현재 프로젝트에서 사용할 Solana RPC endpoint를 결정한다.
// 이 값은 순수하게 "브라우저 지갑이 어느 클러스터에 붙을지"만 결정한다.
// 인증 자체는 체인에 직접 쓰는 것이 아니라, 프론트가 서버 인증 메시지를 서명해서 백엔드로 보내고
// 백엔드가 서명을 검증한 뒤 Supabase 기준 사용자 계정과 연결하는 구조다.
import {clusterApiUrl} from "@solana/web3.js";

export function getSolanaEndpoint(): string {
    const heliusUrl = import.meta.env.VITE_HELIUS_RPC_URL?.trim();
    if (heliusUrl) return heliusUrl;

    const network = import.meta.env.VITE_SOLANA_NETWORK?.trim();
    if (network === 'devnet' || network === 'testnet' || network === 'mainnet-beta') {
        return clusterApiUrl(network);
    }

    return clusterApiUrl('devnet');
}

// Solana signMessage는 문자열이 아니라 Uint8Array를 받는다.
// 백엔드가 내려준 인증 메시지 원문 그대로 서명해야 하므로, UTF-8 인코딩만 담당한다.
export function encodeUtf8(value: string): Uint8Array {
    return new TextEncoder().encode(value);
}

// 서버가 준 인증 메시지를 signMessage로 서명하고 Base58 문자열로 바꿔준다.
// 현재 백엔드 계약이 Base58 signature를 기대하므로 여기서 인코딩을 통일한다.
// 결과적으로 프론트는 다음 3개를 백엔드에 보낸다.
// 1) wallet_address
// 2) nonce
// 3) signature(Base58)

export async function signMessageToBase58(
    message: string,
    signMessage: (message: Uint8Array) => Promise<Uint8Array>,
): Promise<string> {
    const signedBytes = await signMessage(encodeUtf8(message));
    return bs58.encode(signedBytes);
}

// UI 표시용 지갑 주소 축약 함수.
// 예: 7xKX...
export function shortenWalletAddress(address: string | null | undefined): string {
    if (!address) {
        return '-';
    }

    if (address.length <= 6) {
        return address;
    }

    return `${address.slice(0, 4)}...`;
}

// 로그인/연동 직전에 현재 지갑이 인증 가능한 상태인지 검증한다.
// wallet adapter는 연결은 되어 있어도 signMessage를 지원하지 않을 수 있으므로
// "연결됨"과 "인증 가능"은 별개로 본다.
export function assertSolanaWalletReady(
    walletAddress: string | null,
    signMessage?: ((message: Uint8Array) => Promise<Uint8Array>) | undefined,
): asserts signMessage is (message: Uint8Array) => Promise<Uint8Array> {
    if (!walletAddress) {
        throw new Error('지갑이 연결되어 있지 않습니다.');
    }
    if (!signMessage) {
        throw new Error('현재 지갑은 signMessage를 지원하지 않습니다.');
    }

}
