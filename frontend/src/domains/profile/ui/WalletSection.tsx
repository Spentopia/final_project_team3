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
import {useState} from "react";
import {ConnectWalletButton} from "@/domains/wallet/ui/ConnectWalletButton";
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
import {Link as LinkIcon, Wallet} from "lucide-react";

interface WalletSectionProps {
    isLoggedIn?: boolean;
    isProfileComplete?: boolean;
}

export function WalletSection({
    isLoggedIn = true,
    isProfileComplete = false,
}: WalletSectionProps) {
    const [isDialogOpen, setIsDialogOpen] = useState(false);
    const {
        walletAddress,
        connected,
        canSignMessage,
        linkWallet,
        unlinkWallet,
        isProcessing,
        currentProcess,
        errorMessage,
        successMessage,
    } = useWalletConnection();
    const [localMessage, setLocalMessage] = useState<string | null>(null);

    const handleLinkWallet = async() =>
    {
        setLocalMessage(null);

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
        setLocalMessage(result.message);
    }
    ;

    const handleUnlinkWallet = async () => {
        setLocalMessage(null);

        const result = await unlinkWallet();
        setLocalMessage(result.message);
    };

    const helperMessage = localMessage ?? successMessage ?? errorMessage;
    const helperTone = localMessage || errorMessage
        ? "text-amber-700 dark:text-amber-300 bg-amber-50 dark:bg-amber-950/30"
        : "text-emerald-700 dark:text-emerald-300 bg-emerald-50 dark:bg-emerald-950/30";

    return (
        <>
            <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
                <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">지갑 관리</h3>

                {connected ? (
                    <div className="space-y-4">
                        <div className="rounded-lg border-2 border-green-500 dark:border-green-600 bg-green-50 dark:bg-green-900/20 p-4">
                            <div className="mb-3 flex items-center gap-2 text-green-700 dark:text-green-400">
                                <Wallet className="h-5 w-5" />
                                <span className="font-bold">지갑 연결됨</span>
                            </div>
                            <p className="mb-2 font-mono text-sm text-gray-700 dark:text-gray-300">
                                {walletAddress ?? "0x1234...5678"}
                            </p>
                            <p className="text-xs text-gray-600 dark:text-gray-400">
                                1계정 1지갑 정책으로 하나의 지갑만 연결 가능합니다
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
                                onClick={() => setIsDialogOpen(true)}
                                className="bg-gradient-to-r from-cyan-500 to-blue-500"
                            >
                                <LinkIcon className="mr-2 h-4 w-4" />
                                지갑 연결하기
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
            </Card>

            <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
                <DialogContent className="sm:max-w-xl">
                    <DialogHeader>
                        <DialogTitle>지갑 연결 및 연동</DialogTitle>
                        <DialogDescription>
                            원래 페이지는 유지하고, 실제 지갑 연결과 계정 연동 작업만 별도 창에서 처리합니다.
                        </DialogDescription>
                    </DialogHeader>

                    <div className="space-y-4">
                        <div className="rounded-lg border border-gray-200 bg-gray-50 p-4 dark:border-gray-700 dark:bg-gray-900/40">
                            <p className="mb-2 text-sm font-semibold text-gray-900 dark:text-gray-100">
                                현재 브라우저 지갑 상태
                            </p>
                            <p className="text-sm text-gray-600 dark:text-gray-400">
                                {connected && walletAddress
                                    ? `${shortenWalletAddress(walletAddress)} 연결됨`
                                    : "연결된 지갑이 없습니다."}
                            </p>
                        </div>

                        <div className="flex flex-col gap-3 sm:flex-row">
                            <ConnectWalletButton className="flex-1" />
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
                                ? "지갑 연동 해제 중..."
                                : "지갑 연동 해제"}
                        </Button>

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
        </>
    );
}
