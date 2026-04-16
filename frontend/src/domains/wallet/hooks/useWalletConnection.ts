import {
    WalletActionResult,
    WalletLoginRequest,
    WalletLoginResponse,
    WalletProcessType
} from "@/domains/wallet/model/types";
import {AxiosError} from "axios";
import {useConnection, useWallet} from "@solana/wallet-adapter-react";
import {useWalletModal} from "@solana/wallet-adapter-react-ui";
import {useCallback, useMemo, useState} from "react";
import {assertSolanaWalletReady, signNonceToBase58} from "@/domains/wallet/lib/solana";
import {loginWithWalletApi, linkWalletApi, requestWalletNonce, unlinkWalletApi} from "@/domains/wallet/api/wallet";
import {authStorage} from "@/shared/lib/auth";

// 지갑 로그인 성공 후 기존 앱 인증 체계와 호환되도록 토큰을 저장한다.
// 현재 프로젝트는 authStorage와 spentopia_auth 플래그를 함께 보고 있으므로 둘 다 맞춰준다.
function persistWalletTokens(payload: WalletLoginResponse): void {
    authStorage.setToken(payload.access_token);
    
}

// axios 에러 응답을 화면에 표시할 수 있는 문자열로 정규화한다.
// 백엔드 구현 중간 단계에서도 쓰기 쉽도록
// 1) plain text 응답
// 2) { message: string } JSON 응답
// 둘 다 수용한다.
function getErrorMessage(error: unknown): string {
    if (error instanceof AxiosError) {
        const responseData = error.response?.data;

        if (typeof responseData === 'string' && responseData.trim().length > 0) {
            return responseData;
        }

        if (
            typeof responseData === 'object' &&
            responseData !== null &&
            'message' in responseData &&
            typeof responseData.message === 'string'

        ) {
            return responseData.message;
        }
    }

    if (error instanceof Error) {
        const lower = error.message.toLowerCase();
        // 지갑 어댑터의 영문 에러를 한국어로 변환한다.
        if (lower.includes('user rejected') || lower.includes('rejected the request')) {
            return '취소했어요.';
        }
        if (lower.includes('plugin closed') || lower.includes('closed')) {
            return '지갑 창이 닫혔어요.';
        }
        return error.message;
    }
    return '알 수 없는 오류가 발생했어요';
}

// Solana wallet adapter와 백엔드 지갑 인증 API를 묶는 핵심 훅.
//
// 프론트 역할:
// 1) 브라우저 지갑 연결
// 2) 현재 publicKey 확보
// 3) 백엔드에서 nonce 발급받기
// 4) nonce 원문을 signMessage로 서명
// 5) wallet_address / nonce / signature를 백엔드로 전달
//
// 백엔드 역할:
// 1) nonce 생성 및 보관
// 2) 서명 검증
// 3) 로그인 시 토큰 발급
// 4) 연동 시 사용자 계정과 지갑 주소 매핑 저장
export function useWalletConnection() {
    const {connection} = useConnection();
    const {setVisible} = useWalletModal();

    const {
        wallet,
        publicKey,
        connected,
        connecting,
        disconnecting,
        connect,
        disconnect,
        select,
        signMessage,
    } = useWallet();

    // 현재 어떤 요청이 진행 중인지 UI에서 구분하기 위한 상태.
    // 버튼 라벨을 "지갑 로그인 중...", "지갑 연동 중..."처럼 나누는 데 쓴다.
    const [currentProcess, setCurrentProcess] = useState<WalletProcessType>(null);

    // 공통 로딩 상태.
    // 지갑 로그인/연동/해제는 모두 서버 왕복이 들어가므로 단일 플래그를 둔다.
    const [isProcessing, setIsProcessing] = useState<boolean>(false);

    // 최근 성공/실패 메세지.
    // 토스트 대신 인라인 메세지로도 사용할 수 있도록 훅에서 관리한다.
    const [errorMessage, setErrorMessage] = useState<string | null>(null);
    const [successMessage, setSuccessMessage] = useState<string | null>(null);

    // Solana PublicKey 객체를 화면/요청에서 바로 쓰기 좋은 Base58 문자열로 바꾼다.
    const walletAddress = useMemo(() => publicKey?.toBase58() ?? null, [publicKey]);

    // 현재 연결된 지갑 종류 이름.
    // 예: Phantom, Solflare
    const walletName = useMemo(() => wallet?.adapter.name ?? null, [wallet]);

    // 새로운 인증 작업을 시작하기 전에 이전 결과를 지운다.
    const resetMessages = useCallback(() => {
        setErrorMessage(null);
        setSuccessMessage(null);
    }, []);

    // 지갑 선택 모달만 여는 단순 UI 액션.
    // 아직 connect를 강제로 호출하지 않고, 사용자가 Phantom/Solflare를 고르도록 한다.
    const openWalletModal = useCallback(() => {
        resetMessages();
        setVisible(true);
    }, [resetMessages, setVisible]);

    // wallet adapter connect() 직접 호출.
    // 현재 UI에서는 주로 modal을 여는 방식이지만, 다른 화면에서 바로 연결이 필요할 때 재사용 가능하다.
    const connectWallet = useCallback(async (): Promise<void> => {
        resetMessages();

        try {
            await connect();
        } catch (error) {
            const message = getErrorMessage(error).toLowerCase();
            // 사용자가 취소하거나 팝업을 닫은 경우는 정상 흐름이므로 에러 메시지를 표시하지 않는다.
            // "rejected" → 승인 거부, "closed" → Plugin Closed(팝업 창 닫음)
            const isCancellation = message.includes('rejected') || message.includes('closed');
            if (!isCancellation) {
                setErrorMessage(getErrorMessage(error));
            }
            throw error;
        }
    }, [connect, resetMessages]);

    // 브라우저 wallet adapter 연결 해제.
    // 서버의 계정-지갑 매핑 해제와는 다른 개념이므로 unlinkWallet과 분리한다.
    const disconnectWallet = useCallback(async (): Promise<void> => {
        resetMessages();

        try {
            await disconnect();
        } catch (error) {
            setErrorMessage(getErrorMessage(error));
            throw error;
        }
    }, [disconnect, resetMessages]);

    // 지갑 선택 자체를 해제한다.
    // disconnect()는 연결만 끊을 뿐 adapter의 선택 상태(localStorage)를 초기화하지 않는다.
    // 모달을 다시 열어 다른 지갑을 고를 수 있도록 선택을 완전히 지운다.
    const deselectWallet = useCallback(() => {
        // select(null)은 현재 어댑터를 disconnect하고 localStorage의 지갑 이름도 초기화한다.
        // WalletName branded type이지만 null을 넘기면 wallet-adapter 내부에서 정상 처리된다.
        select(null as Parameters<typeof select>[0]);
    }, [select]);

    // 로그인과 연동에 공통으로 쓰이는 인증 payload 생성기.
    //
    // 순서:
    // 1) 현재 연결된 지갑 주소 확인
    // 2) 백엔드에서 nonce 발급
    // 3) 그 nonce 원문 자체를 signMessage
    // 4) 백엔드가 이해하는 DTO 형태로 반환
    //
    // 이 단계까지는 아직 "로그인"도 "연동"도 완료되지 않는다.
    // 서명된 payload를 어떤 API로 보내느냐에 따라
    // - /auth/wallet/login 이면 지갑 로그인
    // - /wallet/link 이면 기존 계정에 지갑 연결
    const createSignedNoncePayload = useCallback(async (): Promise<WalletLoginRequest> => {
        assertSolanaWalletReady(walletAddress, signMessage);
        if (!walletAddress) {
            throw new Error('지갑이 연결되어 있지 않습니다.');
        }
        const currentWalletAddress: string = walletAddress;

        // 1. 서버에서 1회용 nonce 발급
        const nonceResponse = await requestWalletNonce({
            wallet_address: currentWalletAddress,
        });

        // 2. 서버가 내려준 nonce 원문을 그대로 서명
        //    문자열을 가공하면 서버 검증이 실패하므로 문구를 붙이지 않는다.
        const signature = await signNonceToBase58(nonceResponse.nonce, signMessage);

        return {
            wallet_address: currentWalletAddress,
            nonce: nonceResponse.nonce,
            signature,
        };

    }, [walletAddress, signMessage]);

    // 지갑 로그인.
    // 전제: 이 wallet_address가 이미 어떤 사용자 계정에 연결되어 있어야 한다.
    //
    // 흐름:
    // 1) createSignedNoncePayload()
    // 2) /auth/wallet/login 호출
    // 3) 백엔드가 서명을 검증하고 access/refresh token 발급
    // 4) 기존 앱 인증 저장소에 토큰 반영
    const loginWithWallet = useCallback(async (): Promise<WalletActionResult> => {
        setCurrentProcess('login');
        setIsProcessing(true);
        resetMessages();

        try {
            const payload = await createSignedNoncePayload();

            const response = await loginWithWalletApi(payload);

            // 현재 백엔드 응답 DTO 기준.
            persistWalletTokens(response);

            const message = response.is_new_user
                ? '지갑 로그인에 성공했습니다.'
                : '지갑 로그인에 성공했습니다.';

            setSuccessMessage(message);

            return {
                success: true,
                message,
            };
        } catch (error) {
            const message = getErrorMessage(error);
            setErrorMessage(message);

            return {
                success: false,
                message,
            };
        } finally {
            setCurrentProcess(null);
            setIsProcessing(false);
        }
    }, [createSignedNoncePayload, resetMessages]);


    // 지갑 연동.
    // 전제: 사용자는 이미 일반 로그인된 상태여야 하고,
    // 백엔드는 Authorization 헤더로 "누구의 계정에 연결하는지"를 식별한다.
    //
    // 흐름:
    // 1) createSignedNoncePayload()
    // 2) /wallet/link 호출
    // 3) 백엔드가 서명을 검증하고 현재 로그인 사용자 계정과 지갑 주소를 연결
    //
    // 프로필 완료 여부는 이 훅의 책임이 아니라 상위 화면/가드에서 막는 것이 더 안전하다.
    const linkWallet = useCallback(async (): Promise<WalletActionResult> => {
        setCurrentProcess('link');
        setIsProcessing(true);
        resetMessages();

        try {
            const payload = await createSignedNoncePayload();

            const response = await linkWalletApi(payload);

            setSuccessMessage(response.message);

            return {
                success: true,
                message: response.message,
            };
        } catch (error) {
            const message = getErrorMessage(error);
            setErrorMessage(message);

            return {
                success: false,
                message,
            };
        } finally {
            setCurrentProcess(null);
            setIsProcessing(false);
        }

    }, [createSignedNoncePayload, resetMessages]);

    // 지갑 연동 해제.
    // 이 함수는 브라우저 wallet adapter disconnect가 아니라
    // 백엔드에 저장된 "계정-지갑 연결 관계"를 제거하는 요청이다.
    const unlinkWallet = useCallback(async (): Promise<WalletActionResult> => {
        setCurrentProcess('unlink');
        setIsProcessing(true);
        resetMessages();

        try{
            const response = await unlinkWalletApi();

            setSuccessMessage(response.message);

            return {
                success: true,
                message: response.message,
            };
        } catch (error) {
            const message = getErrorMessage(error);
            setErrorMessage(message);

            return {
                success: false,
                message,
            };

        }finally {
            setCurrentProcess(null);
            setIsProcessing(false);
        }
    },[resetMessages]);

    return{
        // wallet-adapter 상태
        connection,
        wallet,
        walletName,
        publicKey,
        walletAddress,
        connected,
        connecting,
        disconnecting,

        // 기능 지원 여부
        canSignMessage: typeof signMessage === 'function',

        // UI 액션
        openWalletModal,
        connectWallet,
        disconnectWallet,
        deselectWallet,

        // 비즈니스 액션
        loginWithWallet,
        linkWallet,
        unlinkWallet,

        // 공통 상태
        currentProcess,
        isProcessing,
        errorMessage,
        successMessage,
    };
}
