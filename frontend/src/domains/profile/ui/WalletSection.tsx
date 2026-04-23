// 프로필/설정 페이지 등에 넣을 수 있는 지갑 섹션
// 현재 백엔드 확정된 것:
// - 지갑 연동 실행
// - 지갑 연동 해제 실행
//
// 아직 없는 것:
// - 현재 계정의 연동 상태 조회 API
// - 프로필 완료 여부 조회 API
//

import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useRef, useState} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Card} from "@/shared/ui/card.tsx";
import {Button} from "@/shared/ui/button.tsx";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogHeader,
    DialogTitle,
} from "@/shared/ui/dialog.tsx";
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import {Link as LinkIcon, Wallet} from "lucide-react";

interface WalletSectionProps {
    isLoggedIn?: boolean;
    isProfileComplete?: boolean;
    linkedWalletAddress?: string | null;
    onWalletLinked?: (walletAddress: string) => void;
    onWalletUnlinked?: () => void;
}

export function WalletSection({
    isLoggedIn = true,
    isProfileComplete = false,
    linkedWalletAddress = null,
    onWalletLinked,
    onWalletUnlinked,
}: WalletSectionProps) {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const [linkedAddress, setLinkedAddress] = useState<string | null>(linkedWalletAddress);
    const [openingWalletModal, setOpeningWalletModal] = useState(false);
    const [linkRequested, setLinkRequested] = useState(false);
    const [showUnlinkDialog, setShowUnlinkDialog] = useState(false);
    const {
        wallet,
        walletAddress,
        connected,
        connecting,
        disconnecting,
        walletModalVisible,
        canSignMessage,
        openWalletModal,
        connectWallet,
        disconnectWallet,
        deselectWallet,
        linkWallet,
        unlinkWallet,
        isProcessing,
        currentProcess,
        errorMessage,
        successMessage,
    } = useWalletConnection();
    const [localMessage, setLocalMessage] = useState<string | null>(null);
    const connectInFlightRef = useRef(false);

    useEffect(() => {
        setLinkedAddress(linkedWalletAddress || null);
    }, [linkedWalletAddress]);

    useEffect(() => {
        if (!openingWalletModal || wallet || disconnecting) return;

        setOpeningWalletModal(false);
        openWalletModal();
    }, [openingWalletModal, wallet, disconnecting, openWalletModal]);

    useEffect(() => {
        if (
            !linkRequested ||
            walletModalVisible ||
            wallet ||
            connected ||
            openingWalletModal
        ) {
            return;
        }

        setLinkRequested(false);
        setLocalMessage('지갑 선택이 취소되었습니다.');
    }, [linkRequested, walletModalVisible, wallet, connected, openingWalletModal]);

    useEffect(() => {
        if (
            !linkRequested ||
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
                setLinkRequested(false);
            })
            .finally(() => {
                connectInFlightRef.current = false;
            });
    }, [
        linkRequested,
        wallet,
        connected,
        connecting,
        disconnecting,
        openingWalletModal,
        walletModalVisible,
        connectWallet,
    ]);

    const openBrowserWalletModal = () => {
        setLocalMessage(null);

        if (wallet) {
            setOpeningWalletModal(true);
            deselectWallet();
            return;
        }

        setOpeningWalletModal(true);
    };

    const runSignedUnlinkWallet = async () => {
        if (!linkedAddress) {
            setLocalMessage('연동된 지갑이 없습니다.');
            return;
        }

        const result = await unlinkWallet();
        if (result.success) {
            setLinkedAddress(null);
            onWalletUnlinked?.();
            setIsDialogOpen(false);
            try {
                await disconnectWallet();
            } catch {
                // 서버 연동 해제는 이미 성공했으므로 로컬 adapter 정리만 계속 진행한다.
            } finally {
                deselectWallet();
            }
        }
        setLocalMessage(result.message);
    };

    const handleLinkWallet = async() =>
    {
        setLocalMessage(null);

        if (linkedAddress) {
            setLocalMessage('이미 계정에 지갑이 연동되어 있습니다. 변경하려면 먼저 연동을 해제해 주세요.');
            return;
        }

        // 연동 전에 현재 브라우저 지갑이 연결되어 있어야 함
        if (!connected || !walletAddress) {
            setLocalMessage('먼저 지갑을 연결해 주세요.');
            return;
        }

        if (!canSignMessage) {
            setLocalMessage('현재 지갑은 signMessage를 지원하지 않습니다.');
            return;
        }

        // TODO:
        // 프로필 완료 여부 API가 생기면 여기서 먼저 검사해서 "프로필 완료 사용자만 지갑 연동 가능" 규칙을 적용하면 됨.
        const result = await linkWallet();
        if (result.success) {
            setLinkedAddress(walletAddress);
            onWalletLinked?.(walletAddress);
            setIsDialogOpen(false);
        }
        setLocalMessage(result.message);
    }
    ;

    useEffect(() => {
        if (!linkRequested || !connected || !walletAddress || linkedAddress || isProcessing) {
            return;
        }

        setLinkRequested(false);
        void handleLinkWallet();
    }, [linkRequested, connected, walletAddress, linkedAddress, isProcessing]);

    const handleLinkWalletStart = async () => {
        setLocalMessage(null);

        if (linkedAddress) {
            setLocalMessage('이미 계정에 지갑이 연동되어 있습니다. 변경하려면 먼저 연동을 해제해 주세요.');
            return;
        }

        if (!isLoggedIn) {
            setLocalMessage('로그인 후 지갑을 연동할 수 있습니다.');
            return;
        }

        if (!isProfileComplete) {
            setLocalMessage('상세정보 입력이 완료된 계정만 지갑 연동이 가능합니다.');
            return;
        }

        setLinkRequested(true);
        openBrowserWalletModal();
    };

    const handleUnlinkWallet = async () => {
        setLocalMessage(null);

        if (!linkedAddress) {
            setLocalMessage('연동된 지갑이 없습니다.');
            return;
        }

        setShowUnlinkDialog(true);
    };

    const handleUnlinkConfirm = async () => {
        setLocalMessage(null);

        if (!linkedAddress) {
            setShowUnlinkDialog(false);
            setLocalMessage('연동된 지갑이 없습니다.');
            return;
        }

        setShowUnlinkDialog(false);
        await runSignedUnlinkWallet();
    };

    const browserWalletStatus = connected && walletAddress
        ? `${shortenWalletAddress(walletAddress)} 연결됨`
        : "연결된 지갑이 없습니다.";
    const isLinkedWalletConnected = Boolean(
        linkedAddress && connected && walletAddress === linkedAddress
    );
    const helperMessage = localMessage ?? successMessage ?? errorMessage;
    const helperTone = localMessage || errorMessage
        ? "text-amber-700 dark:text-amber-300 bg-amber-50 dark:bg-amber-950/30"
        : "text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/30";

    return (
        <>
            <Card className="h-full border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
                <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">지갑 관리</h3>

                {linkedAddress ? (
                    <div className="space-y-4">
                        <div className="rounded-lg border-2 border-green-500 dark:border-green-600 bg-green-50 dark:bg-green-900/20 p-4">
                            <div className="mb-3 flex items-center gap-2 text-green-700 dark:text-green-400">
                                <Wallet className="h-5 w-5" />
                                <span className="font-bold">계정에 지갑 연동됨</span>
                            </div>
                            <p className="mb-2 font-mono text-sm text-gray-700 dark:text-gray-300">
                                {linkedAddress}
                            </p>
                            <p className="text-xs text-gray-600 dark:text-gray-400">
                                {isLinkedWalletConnected
                                    ? "브라우저 지갑도 연결되어 있어요"
                                    : "연동 해제는 지갑 서명이 필요합니다"}
                            </p>
                        </div>

                        <Button
                            type="button"
                            onClick={() => {
                                void handleUnlinkWallet();
                            }}
                            disabled={isProcessing}
                            variant="outline"
                            className="w-full border-red-300 text-red-600 hover:bg-red-50"
                        >
                            {isProcessing && currentProcess === "unlink"
                                ? "지갑 연결 해제 중..."
                                : "지갑 연결 해제"}
                        </Button>
                    </div>
                ) : (
                    <div className="space-y-4">
                        <div className="rounded-lg border-2 border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-900/50 p-8 text-center">
                            <Wallet className="mx-auto mb-3 h-12 w-12 text-gray-400 dark:text-gray-500" />
                            <p className="mb-2 font-bold text-gray-900 dark:text-gray-100">지갑이 연결되지 않았어요</p>
                            <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
                                지갑을 연결하면 NFT 거래와 블록체인 기능을 이용할 수 있어요
                            </p>
                            <Button
                                type="button"
                                onClick={() => {
                                    void handleLinkWalletStart();
                                }}
                                disabled={isProcessing || linkRequested}
                                className="bg-gradient-to-r from-cyan-500 to-blue-500"
                            >
                                <LinkIcon className="mr-2 h-4 w-4" />
                                {linkRequested || (isProcessing && currentProcess === "link")
                                    ? "지갑 연동 중..."
                                    : "지갑 연결하기"}
                            </Button>
                        </div>

                        <Card className="bg-gradient-to-br from-cyan-50 to-blue-50 dark:from-cyan-900/30 dark:to-blue-900/30 p-4">
                            <h4 className="mb-2 font-bold text-gray-900 dark:text-gray-100">💡 지갑 연결 혜택</h4>
                            <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
                                <li>• NFT로 아바타 아이템 발행</li>
                                <li>• 마켓에서 자유롭게 거래</li>
                                <li>• 블록체인 기반 소유권 증명</li>
                                <li>• 지갑으로 간편 로그인</li>
                            </ul>
                        </Card>
                    </div>
                )}

                {helperMessage ? (
                    <p className={`mt-4 rounded-lg p-3 text-sm ${helperTone}`}>
                        {helperMessage}
                    </p>
                ) : null}
            </Card>

            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent className="sm:max-w-xl">
                    <DialogHeader>
                        <DialogTitle>{linkedAddress ? "연동 지갑 관리" : "지갑 연결 및 연동"}</DialogTitle>
                        <DialogDescription>
                            {linkedAddress
                                ? "이미 계정에 연동된 지갑이 있습니다. 변경하려면 먼저 연동을 해제해 주세요."
                                : "브라우저 지갑을 연결한 뒤 현재 계정에 연동합니다."}
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
                            <p className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">
                                현재 브라우저 지갑 상태
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                {browserWalletStatus}
                            </p>
                        </div>

                        <div className="flex flex-col gap-3 sm:flex-row">
                            <Button
                                type="button"
                                onClick={() => {
                                    void handleLinkWalletStart();
                                }}
                                disabled={isProcessing || disconnecting}
                                variant="outline"
                                className="flex-1"
                            >
                                <Wallet className="h-4 w-4" />
                                {connected ? "연결된 지갑 연동" : "브라우저 지갑 연결"}
                            </Button>
                            {!linkedAddress ? (
                                <Button
                                    type="button"
                                    onClick={() => {
                                        void handleLinkWallet();
                                    }}
                                    disabled={isProcessing || !isLoggedIn || !isProfileComplete}
                                    className="flex-1 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
                                >
                                    <LinkIcon className="h-4 w-4" />
                                    {isProcessing && currentProcess === "link" ? "지갑 연동 중..." : "계정에 지갑 연동"}
                                </Button>
                            ) : null}
                        </div>

                        {linkedAddress ? (
                            <Button
                                type="button"
                                onClick={() => {
                                void handleUnlinkWallet();
                            }}
                            disabled={isProcessing}
                            variant="outline"
                            className="w-full border-red-300 text-red-600 hover:bg-red-50"
                        >
                            {isProcessing && currentProcess === "unlink"
                                ? "지갑 연동 해제 중..."
                                : "지갑 연동 해제"}
                        </Button>
                        ) : null}

                        {!isProfileComplete ? (
                            <p className="rounded-lg bg-amber-50 p-3 text-sm text-amber-700 dark:bg-amber-950/30 dark:text-amber-300">
                                상세정보 입력이 완료된 계정만 지갑 연동이 가능합니다.
                            </p>
                        ) : null}

                        {helperMessage ? (
                            <p className={`rounded-lg p-3 text-sm ${helperTone}`}>
                                {helperMessage}
                            </p>
                        ) : null}
                    </div>
                </DialogContent>
            </Dialog>

            <AlertDialog open={showUnlinkDialog} onOpenChange={setShowUnlinkDialog}>
                <AlertDialogContent>
                    <AlertDialogHeader>
                        <AlertDialogTitle>지갑을 해제하시겠습니까?</AlertDialogTitle>
                        <AlertDialogDescription className="space-y-2">
                            <span className="block">
                                계정에 연동된 지갑:{" "}
                                <span className="font-mono font-semibold text-foreground">
                                    {linkedAddress ? shortenWalletAddress(linkedAddress) : ""}
                                </span>
                            </span>
                            <span className="block text-muted-foreground">
                                해제하려면 연동된 지갑으로 인증 메시지에 서명해야 합니다.
                            </span>
                        </AlertDialogDescription>
                    </AlertDialogHeader>
                    <AlertDialogFooter>
                        <AlertDialogCancel
                            disabled={isProcessing}
                            onClick={() => {
                                setLocalMessage('지갑 연결 해제를 취소했습니다.');
                            }}
                        >
                            취소
                        </AlertDialogCancel>
                        <Button
                            type="button"
                            disabled={isProcessing}
                            onClick={() => {
                                void handleUnlinkConfirm();
                            }}
                            className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
                        >
                            지갑 해제
                        </Button>
                    </AlertDialogFooter>
                </AlertDialogContent>
            </AlertDialog>
        </>
    );
}
