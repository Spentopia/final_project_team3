import {useNavigate} from "react-router";
import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useRef, useState} from "react";

interface WalletLoginButtonProps {
    className?: string;
    redirectTo?: string;
}

export function WalletLoginButton({
    className,
    redirectTo = '/',
}: WalletLoginButtonProps) {
    const navigate = useNavigate();

    const {
        wallet,
        connected,
        connecting,
        walletAddress,
        canSignMessage,
        openWalletModal,
        connectWallet,
        loginWithWallet,
        isProcessing,
        currentProcess,
        errorMessage,
        successMessage,
    } = useWalletConnection();

    const [localMessage, setLocalMessage] = useState<string | null>(null);

    // loginWithWallet을 ref로 보관해 effect 의존성에서 제외한다.
    const loginWithWalletRef = useRef(loginWithWallet);
    useEffect(() => { loginWithWalletRef.current = loginWithWallet; }, [loginWithWallet]);

    // 사용자가 이 버튼을 눌러 로그인 흐름을 시작했을 때만 true.
    const pendingRef = useRef(false);

    // 이전 wallet 어댑터 이름을 기억해 "새 지갑이 선택됐는지"를 판단한다.
    const prevWalletNameRef = useRef<string | null>(wallet?.adapter.name ?? null);

    // autoConnect=false 이므로 모달에서 지갑이 선택(wallet 변경)되면 직접 connect()를 호출한다.
    useEffect(() => {
        const currentName = wallet?.adapter.name ?? null;
        const prevName = prevWalletNameRef.current;
        prevWalletNameRef.current = currentName;

        if (currentName && currentName !== prevName && pendingRef.current && !connected && !connecting) {
            connectWallet().catch(() => {});
        }
    }, [wallet, connected, connecting, connectWallet]);

    // 지갑이 연결되면 자동으로 로그인 서명 요청.
    useEffect(() => {
        if (connected && walletAddress && pendingRef.current) {
            pendingRef.current = false;
            loginWithWalletRef.current().then(result => {
                setLocalMessage(result.message);
                if (result.success) void navigate(redirectTo);
            }).catch(() => {});
        }
    }, [connected, walletAddress, navigate, redirectTo]);

    const handleClick = async () => {
        setLocalMessage(null);

        if (!connected) {
            pendingRef.current = true;
            // 이전 선택 기록을 초기화해서 같은 지갑을 다시 선택해도 connect()가 호출되도록 한다.
            prevWalletNameRef.current = null;
            openWalletModal();
            return;
        }

        if (!canSignMessage) {
            setLocalMessage('현재 지갑은 signMessage를 지원하지 않습니다.');
            return;
        }

        const result = await loginWithWallet();
        setLocalMessage(result.message);
        if (result.success) void navigate(redirectTo);
    };

    return (
        <div>
            <button
                type="button"
                className={className}
                onClick={() => { void handleClick(); }}
                disabled={isProcessing}
            >
                {isProcessing && currentProcess === 'login'
                    ? '지갑 로그인 중...'
                    : '지갑으로 로그인'}
            </button>

            {localMessage ? <p>{localMessage}</p> : null}
            {!localMessage && successMessage ? <p>{successMessage}</p> : null}
            {!localMessage && errorMessage ? <p>{errorMessage}</p> : null}
        </div>
    );
}
