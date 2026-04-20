import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useMemo, useRef, useState} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Link as LinkIcon, Wallet, Loader2} from "lucide-react";
import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/shared/ui/alert-dialog";

interface ConnectWalletButtonProps{
  className?: string;
}

export function ConnectWalletButton({className}: ConnectWalletButtonProps){
  const {
      wallet,
      wallets,
      connected,
      connecting,
      disconnecting,
      walletAddress,
      walletName,
      openWalletModal,
      disconnectWallet,
      deselectWallet,
      linkWallet,
      unlinkWallet,
      isProcessing,
  } = useWalletConnection();

  // Wallet Standard 감지 완료 여부
  // wallets.length > 0 또는 wallet이 이미 설정된 경우 준비 완료
  const isWalletReady = wallets.length > 0 || !!wallet;

  // 최신 함수를 ref로 보관해 effect 의존성에서 제외한다.
  const linkWalletRef = useRef(linkWallet);
  useEffect(() => { linkWalletRef.current = linkWallet; }, [linkWallet]);

  const [linkRequested, setLinkRequested] = useState(false);
  const [openingWalletModal, setOpeningWalletModal] = useState(false);
  const linkInFlightRef = useRef(false);

  useEffect(() => {
    if (!openingWalletModal || wallet || disconnecting) return;

    setOpeningWalletModal(false);
    openWalletModal();
  }, [openingWalletModal, wallet, disconnecting, openWalletModal]);

  // 지갑이 연결되면 연동(linkWallet) 시도.
  useEffect(() => {
    if (!linkRequested || !connected || !walletAddress || connecting || disconnecting || linkInFlightRef.current) return;

    linkInFlightRef.current = true;
    setLinkRequested(false);
    linkWalletRef.current().finally(() => {
      linkInFlightRef.current = false;
    });
  }, [linkRequested, connected, walletAddress, connecting, disconnecting]);

  useEffect(() => {
    if (!linkRequested || wallet || openingWalletModal || connected) return;

    const timer = window.setTimeout(() => {
      setLinkRequested(false);
    }, 120000);

    return () => window.clearTimeout(timer);
  }, [linkRequested, wallet, openingWalletModal, connected]);

  const [showUnlinkDialog, setShowUnlinkDialog] = useState(false);

  const label = useMemo(() => {
    if (connecting)    return '지갑 연결 중...';
    if (disconnecting) return '지갑 연결 해제 중...';
    if (isProcessing)  return '처리 중...';
    if (connected && walletAddress) {
      return `${walletName ?? 'Wallet'} · ${shortenWalletAddress(walletAddress)}`;
    }
    return '지갑 연결';
  }, [connected, connecting, disconnecting, walletAddress, walletName, isProcessing]);

  const handleClick = () => {
    if (connected) {
      setShowUnlinkDialog(true);
      return;
    }

    setLinkRequested(true);

    if (wallet) {
      setOpeningWalletModal(true);
      deselectWallet();               // 이전 선택 초기화 → 모달에서 동일 지갑 재선택 가능
    } else {
      openWalletModal();
    }
  };

  const handleUnlinkConfirm = async () => {
    await unlinkWallet();
    await disconnectWallet();
  };

  const isLoading = connecting || disconnecting || isProcessing || linkRequested || !isWalletReady;

  return (
    <>
      {connected ? (
        // ── 연결됨: 솔라나 그린 테두리 + 주소 표시 ──
        <button
          type="button"
          onClick={handleClick}
          disabled={isLoading}
          className={[
            "flex items-center gap-2 rounded-full px-4 py-2 text-sm font-semibold transition-all",
            "border border-[#14F195]/50 bg-[#14F195]/10 text-[#14F195]",
            "hover:bg-[#14F195]/20 hover:border-[#14F195]",
            "disabled:opacity-50 disabled:cursor-not-allowed",
            className ?? "",
          ].join(" ")}
        >
          {/* 연결 상태 점 */}
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-[#14F195] opacity-60" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-[#14F195]" />
          </span>
          <Wallet className="h-4 w-4" />
          <span>{label}</span>
        </button>
      ) : (
        // ── 미연결: 솔라나 보라→초록 그라디언트 버튼 ──
        <button
          type="button"
          onClick={handleClick}
          disabled={isLoading}
          className={[
            "flex items-center gap-2 rounded-full px-4 py-2 text-sm font-bold transition-all",
            "bg-gradient-to-r from-[#9945FF] to-[#14F195] text-white",
            "hover:opacity-90 hover:shadow-lg hover:shadow-[#9945FF]/30 hover:-translate-y-0.5",
            "disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none",
            className ?? "",
          ].join(" ")}
        >
          {isLoading
            ? <Loader2 className="h-4 w-4 animate-spin" />
            : <LinkIcon className="h-4 w-4" />
          }
          <span>{label}</span>
        </button>
      )}


      <AlertDialog open={showUnlinkDialog} onOpenChange={setShowUnlinkDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>지갑을 해제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription className="space-y-2">
              <span className="block">
                현재 연결된 지갑:{" "}
                <span className="font-mono font-semibold text-foreground">
                  {walletAddress ? shortenWalletAddress(walletAddress) : ""}
                </span>
              </span>
              <span className="block text-muted-foreground">
                해제 후 기존 지갑 재등록 및 새로운 지갑 등록이 가능합니다.
              </span>
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              onClick={() => { void handleUnlinkConfirm(); }}
            >
              지갑 해제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
