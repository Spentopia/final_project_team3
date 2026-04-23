import {useNavigate} from "react-router";
import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useCallback, useEffect, useRef, useState} from "react";
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
        wallets,
        connected,
        connecting,
        disconnecting,
        walletAddress,
        canSignMessage,
        walletModalVisible,
        openWalletModal,
        connectWallet,
        deselectWallet,
        loginWithWallet,
        isProcessing,
        currentProcess,
    } = useWalletConnection();

    const isWalletReady = wallets.length > 0 || !!wallet;

    const [loginRequested, setLoginRequested] = useState(false);
    const [openingWalletModal, setOpeningWalletModal] = useState(false);
    const loginInFlightRef = useRef(false);
    const connectInFlightRef = useRef(false);

    const runWalletLogin = useCallback(async () => {
        if (loginInFlightRef.current) return;

        if (!canSignMessage) {
            toast.error('현재 지갑은 signMessage를 지원하지 않습니다.');
            setLoginRequested(false);
            return;
        }

        loginInFlightRef.current = true;
        setLoginRequested(false);

        try {
            const result = await loginWithWallet();
            if (result.success) {
                toast.success(result.message);
                void navigate(redirectTo);
            } else {
                toast.error(result.message);
                // 서명 취소 등 로그인 실패 시 지갑 선택을 초기화한다.
                deselectWallet();
            }
        } finally {
            loginInFlightRef.current = false;
        }
    }, [canSignMessage, deselectWallet, loginWithWallet, navigate, redirectTo]);

    useEffect(() => {
        if (!openingWalletModal || wallet || disconnecting) return;

        setOpeningWalletModal(false);
        openWalletModal();
    }, [openingWalletModal, wallet, disconnecting, openWalletModal]);

    useEffect(() => {
        if (!loginRequested || walletModalVisible || wallet || connected || openingWalletModal) {
            return;
        }

        setLoginRequested(false);
        toast.error('지갑 선택이 취소되었습니다.');
    }, [loginRequested, walletModalVisible, wallet, connected, openingWalletModal]);

    useEffect(() => {
        if (
            !loginRequested ||
            !wallet ||
            connected ||
            connecting ||
            disconnecting ||
            openingWalletModal ||
            walletModalVisible ||
            connectInFlightRef.current
        ) {
            return;
        }

        connectInFlightRef.current = true;
        connectWallet()
            .catch(() => {
                setLoginRequested(false);
            })
            .finally(() => {
                connectInFlightRef.current = false;
            });
    }, [
        loginRequested,
        wallet,
        connected,
        connecting,
        disconnecting,
        openingWalletModal,
        walletModalVisible,
        connectWallet,
    ]);

    useEffect(() => {
        if (!loginRequested || !connected || !walletAddress || connecting || disconnecting) return;
        void runWalletLogin();
    }, [loginRequested, connected, walletAddress, connecting, disconnecting, runWalletLogin]);

    const handleClick = async () => {
        if (connected) {
            await runWalletLogin();
            return;
        }

        setLoginRequested(true);

        if (wallet) {
            setOpeningWalletModal(true);
            deselectWallet();
            return;
        }

        openWalletModal();
    };

    return (
        <div>
            <button
                type="button"
                className={className}
                onClick={() => { void handleClick(); }}
                disabled={isProcessing || loginInFlightRef.current || !isWalletReady}
            >
                {!isWalletReady
                    ? '지갑 감지 중...'
                    : (isProcessing || loginRequested) && currentProcess === 'login'
                        ? '지갑 로그인 중...'
                        : '지갑으로 로그인'}
            </button>

        </div>
    );
}
