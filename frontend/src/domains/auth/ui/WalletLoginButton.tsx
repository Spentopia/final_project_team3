import {useNavigate} from "react-router";
import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useRef} from "react";
import {toast} from "sonner";

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
        deselectWallet,
        loginWithWallet,
        isProcessing,
        currentProcess,
    } = useWalletConnection();

    // loginWithWallet, deselectWallet을 ref로 보관해 effect 의존성에서 제외한다.
    const loginWithWalletRef = useRef(loginWithWallet);
    useEffect(() => { loginWithWalletRef.current = loginWithWallet; }, [loginWithWallet]);
    const deselectWalletRef = useRef(deselectWallet);
    useEffect(() => { deselectWalletRef.current = deselectWallet; }, [deselectWallet]);

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
            connectWallet().catch(() => {
                // 사용자가 취소하면 wallet 상태가 순간 초기화됐다가 복구되면서
                // useEffect가 재발화할 수 있다. pending을 내려서 재시도를 막는다.
                pendingRef.current = false;
            });
        }
    }, [wallet, connected, connecting, connectWallet]);

    // 지갑이 연결되면 자동으로 로그인 서명 요청.
    useEffect(() => {
        if (connected && walletAddress && pendingRef.current) {
            pendingRef.current = false;
            loginWithWalletRef.current().then(result => {
                if (result.success) {
                    toast.success(result.message);
                    void navigate(redirectTo);
                } else {
                    toast.error(result.message);
                    // 서명 취소 등 로그인 실패 시 지갑 연결을 해제한다.
                    // 그래야 다음 버튼 클릭 시 connected=false가 되어 모달이 열린다.
                    deselectWalletRef.current();
                }
            }).catch(() => {});
        }
    }, [connected, walletAddress, navigate, redirectTo]);

    const handleClick = async () => {
        if (!connected) {
            pendingRef.current = true;
            // 이전 선택 기록을 초기화해서 같은 지갑을 다시 선택해도 connect()가 호출되도록 한다.
            prevWalletNameRef.current = null;
            // wallet을 null로 초기화해야 같은 지갑을 다시 선택할 때 wallet 상태가 변해서 useEffect가 발화한다.
            // (이미 Phantom이 선택된 상태에서 Phantom을 다시 선택하면 상태 변화가 없어 connect()가 불리지 않음)
            deselectWallet();
            openWalletModal();
            return;
        }

        if (!canSignMessage) {
            toast.error('현재 지갑은 signMessage를 지원하지 않습니다.');
            return;
        }

        const result = await loginWithWallet();
        if (result.success) {
            toast.success(result.message);
            void navigate(redirectTo);
        } else {
            toast.error(result.message);
            // 서명 취소 등 로그인 실패 시 지갑 연결을 해제한다.
            deselectWallet();
        }
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

        </div>
    );
}
