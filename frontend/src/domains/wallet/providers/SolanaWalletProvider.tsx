// 앱 전체에서 Solana wallet adapter context를 공급하는 Provider.
// 프론트는 여기서 "브라우저 지갑 연결 환경"만 준비하고,
// 실제 로그인/연동 검증은 useWalletConnection -> backend API에서 처리한다.
import {PropsWithChildren, useMemo} from "react";
import {getSolanaEndpoint} from "@/domains/wallet/lib/solana";
import {ConnectionProvider, WalletProvider} from "@solana/wallet-adapter-react";
import {WalletModalProvider} from "@solana/wallet-adapter-react-ui";
import "@solana/wallet-adapter-react-ui/styles.css";

export function SolanaWalletProvider({children}: PropsWithChildren) {
    const endpoint = useMemo(() => getSolanaEndpoint(), []);

    // Phantom, Solflare, Backpack은 모두 Wallet Standard를 지원하므로
    // WalletProvider가 자동으로 감지한다. 여기서 명시적으로 추가하면
    // 어댑터가 중복 등록되어 "Connection rejected" 충돌이 발생한다.
    const wallets = useMemo(() => [], []);

    return (
        <ConnectionProvider endpoint={endpoint}>
            {/* autoConnect=false:
                true로 두면 취소 후에도 wallet adapter가 선택된 지갑을 계속 재시도한다.
                connect()는 각 버튼(ConnectWalletButton, WalletLoginButton)에서
                모달 wallet 선택 감지 후 직접 호출한다. */}
            <WalletProvider
                wallets={wallets}
                autoConnect={false}
                onError={(error) => {
                    // 사용자 취소(rejected)나 팝업 닫기(closed/Plugin Closed)는 정상 흐름이므로 무시한다.
                    const msg = (error.message ?? '').toLowerCase();
                    if (msg.includes('rejected') || msg.includes('closed')) return;
                    console.error('[Wallet]', error);
                }}
            >
                <WalletModalProvider>{children}</WalletModalProvider>
            </WalletProvider>
        </ConnectionProvider>
    );
}
