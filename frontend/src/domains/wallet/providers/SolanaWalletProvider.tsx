// 앱 전체에서 Solana wallet adapter context를 공급하는 Provider.
// 프론트는 여기서 "브라우저 지갑 연결 환경"만 준비하고,
// 실제 로그인/연동 검증은 useWalletConnection -> backend API에서 처리한다.
import {PropsWithChildren, useMemo} from "react";
import {getSolanaEndpoint} from "@/domains/wallet/lib/solana";
import {PhantomWalletAdapter, SolflareWalletAdapter} from "@solana/wallet-adapter-wallets";
import {BackpackWalletAdapter} from "@solana/wallet-adapter-backpack";
import {ConnectionProvider, WalletProvider} from "@solana/wallet-adapter-react";
import {WalletModalProvider} from "@solana/wallet-adapter-react-ui";
import "@solana/wallet-adapter-react-ui/styles.css";

export function SolanaWalletProvider({children}: PropsWithChildren) {
    // RPC endpoint는 앱 전체에서 동일하게 쓰므로 한 번만 계산한다.
    const endpoint = useMemo(() => getSolanaEndpoint(), []);

    // 브라우저에서 노출할 지갑 어댑터 목록.
    // 여기서 지갑 종류를 추가해도 백엔드 계약은 바뀌지 않는다.
    // 백엔드는 최종적으로 wallet_address / nonce / signature만 받는다.
    const wallets = useMemo(
        () => [
            new PhantomWalletAdapter(),
            new SolflareWalletAdapter(),
            new BackpackWalletAdapter(),
        ],
        [],
    );

    return (
        <ConnectionProvider endpoint={endpoint}>
            {/* autoConnect:
                사용자가 이전에 승인했던 지갑이 있으면 브라우저에서 재연결을 시도한다.
                이건 "지갑 연결 상태" 복원일 뿐, 서버 로그인 상태를 복원하는 것은 아니다. */}
            <WalletProvider wallets={wallets} autoConnect>
                {/* wallet-adapter 기본 모달 UI.
                    ConnectWalletButton / WalletLoginButton 에서 이 모달을 띄운다. */}
                <WalletModalProvider>{children}</WalletModalProvider>
            </WalletProvider>
        </ConnectionProvider>
    )

}
