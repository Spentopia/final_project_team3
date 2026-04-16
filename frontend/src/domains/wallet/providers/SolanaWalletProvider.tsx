// 앱 전체에서 Solana wallet adapter context를 공급하는 Provider.
// 프론트는 여기서 "브라우저 지갑 연결 환경"만 준비하고,
// 실제 로그인/연동 검증은 useWalletConnection -> backend API에서 처리한다.
import {PropsWithChildren, useRef, useMemo} from "react";
import {getSolanaEndpoint} from "@/domains/wallet/lib/solana";
import {PhantomWalletAdapter, SolflareWalletAdapter} from "@solana/wallet-adapter-wallets";
import {BackpackWalletAdapter} from "@solana/wallet-adapter-backpack";
import {ConnectionProvider, WalletProvider} from "@solana/wallet-adapter-react";
import {WalletModalProvider} from "@solana/wallet-adapter-react-ui";
import "@solana/wallet-adapter-react-ui/styles.css";

export function SolanaWalletProvider({children}: PropsWithChildren) {
    // WalletProvider가 렌더되기 전에 이전 세션의 지갑 선택 상태를 지운다.
    // localStorage에 walletName이 남아있으면 autoConnect가 페이지 로드 시
    // 사용자 클릭 없이 이전 지갑(Phantom 등) 팝업을 자동으로 띄운다.
    const didClear = useRef(false);
    if (!didClear.current) {
        didClear.current = true;
        localStorage.removeItem('walletName');
    }

    const endpoint = useMemo(() => getSolanaEndpoint(), []);

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
            {/* autoConnect=false:
                true로 두면 취소 후에도 wallet adapter가 선택된 지갑을 계속 재시도한다.
                connect()는 각 버튼(ConnectWalletButton, WalletLoginButton)에서
                모달 wallet 선택 감지 후 직접 호출한다. */}
            <WalletProvider wallets={wallets} autoConnect={false}>
                <WalletModalProvider>{children}</WalletModalProvider>
            </WalletProvider>
        </ConnectionProvider>
    );
}
