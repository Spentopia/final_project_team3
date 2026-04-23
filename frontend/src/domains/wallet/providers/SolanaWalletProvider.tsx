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
    const walletStorageKey = "spentopiaWalletName";

    // Phantom, Solflare, Backpack은 모두 Wallet Standard를 지원하므로
    // WalletProvider가 자동으로 감지한다. 여기서 명시적으로 추가하면
    // 어댑터가 중복 등록되어 "Connection rejected" 충돌이 발생한다.
    const wallets = useMemo(() => [], []);

    if (typeof window !== "undefined") {
        // 연결/연동은 항상 사용자가 모달에서 직접 선택한 지갑으로 시작한다.
        // dev server 재시작이나 새로고침 후 stale adapter가 복원되면 signMessage가 빠질 수 있다.
        window.localStorage.removeItem(walletStorageKey);
        window.localStorage.removeItem("walletName");
    }

    return (
        <ConnectionProvider endpoint={endpoint}>
            {/* autoConnect=false:
                헤더/마이페이지 연동 표시는 DB wallet_address를 기준으로 하고,
                실제 서명 연결은 매번 지갑 모달에서 직접 선택한 지갑으로 진행한다. */}
            <WalletProvider
                wallets={wallets}
                autoConnect={false}
                localStorageKey={walletStorageKey}
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
