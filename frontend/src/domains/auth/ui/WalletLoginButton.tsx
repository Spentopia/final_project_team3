import {useNavigate} from "react-router";
import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useState} from "react";

interface WalletLoginButtonProps {
    className?: string;

    // 로그인 성공 후 이동할 경로
    redirectTo?: string;
}

// 지갑 로그인 버튼.
// 화면 역할은 단순하다.
// 1) 아직 브라우저 지갑이 없으면 wallet modal을 연다.
// 2) 지갑이 연결되어 있으면 useWalletConnection.loginWithWallet()을 호출한다.
// 실제 nonce 발급/서명/백엔드 검증은 훅 내부로 숨겨서 UI를 단순하게 유지한다.
export function WalletLoginButton({
                                      className,
                                      redirectTo = '/',
                                  }: WalletLoginButtonProps) {
    const navigate = useNavigate();

    const {
        connected,
        canSignMessage,
        openWalletModal,
        loginWithWallet,
        isProcessing,
        currentProcess,
        errorMessage,
        successMessage,
    } = useWalletConnection();

    // 이 컴포넌트 단에서만 잠깐 보여줄 메세지
    const [localMessage, setLocalMessage] = useState<string | null>(null);

    const handleClick = async () => {
        setLocalMessage(null);

        // 아직 브라우저 지갑이 연결되지 않았으면 먼저 연결 유도
        if (!connected) {
            openWalletModal();
            return;
        }

        // 일부 지갑은 signMessage를 지원하지 않을 수 있음
        if (!canSignMessage) {
            setLocalMessage('현재 지갑은 signMessage를 지원하지 않습니다.');
            return;
        }

        const result = await loginWithWallet();

        setLocalMessage(result.message);

        if (result.success) {
            navigate(redirectTo);
        }
    };

    return (
        <div>
            <button
                type="button"
                className={className}
                onClick={() => {
                    void handleClick();
                }}
                disabled={isProcessing}
            >
                {isProcessing && currentProcess === 'login'
                    ? '지갑 로그인 중...'
                    : '지갑으로 로그인'}
            </button>

            {localMessage ? <p>{localMessage}</p> : null}
            {!localMessage && successMessage ? <p>{successMessage}</p> : null}
            {!localMessage && errorMessage?<p>{errorMessage}</p> : null}
        </div>
    );
}
