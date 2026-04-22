import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useMemo, useRef, useState} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Link as LinkIcon, Wallet, Loader2} from "lucide-react";
import {apiClient} from "@/shared/api/client";
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

  const [linkedWalletAddress, setLinkedWalletAddress] = useState<string | null>(null);
  const displayedWalletAddress = connected && walletAddress ? walletAddress : linkedWalletAddress;
  const isDbLinkedOnly = !connected && !!linkedWalletAddress;

  // 최신 함수를 ref로 보관해 effect 의존성에서 제외한다.
  const linkWalletRef = useRef(linkWallet);
  useEffect(() => { linkWalletRef.current = linkWallet; }, [linkWallet]);

  useEffect(() => {
    let cancelled = false;

    const loadLinkedWallet = async () => {
      try {
        const response = await apiClient.get<{wallet_address?: string | null}>("/me");
        if (!cancelled) {
          setLinkedWalletAddress(response.data.wallet_address ?? null);
        }
      } catch {
        if (!cancelled) {
          setLinkedWalletAddress(null);
        }
      }
    };

    void loadLinkedWallet();

    return () => {
      cancelled = true;
    };
  }, []);

  const [linkRequested, setLinkRequested] = useState(false);
  const [unlinkRequested, setUnlinkRequested] = useState(false);
  const [openingWalletModal, setOpeningWalletModal] = useState(false);
  const linkInFlightRef = useRef(false);
  const unlinkInFlightRef = useRef(false);

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
    linkWalletRef.current().then((result) => {
      if (result.success) {
        setLinkedWalletAddress(walletAddress);
      }
    }).finally(() => {
      linkInFlightRef.current = false;
    });
  }, [linkRequested, connected, walletAddress, connecting, disconnecting]);

  useEffect(() => {
    if (!unlinkRequested || !connected || !walletAddress || connecting || disconnecting || unlinkInFlightRef.current) return;

    unlinkInFlightRef.current = true;
    setUnlinkRequested(false);
    unlinkWallet().then(async (result) => {
      if (result.success) {
        setLinkedWalletAddress(null);
        await disconnectWallet();
      }
    }).finally(() => {
      unlinkInFlightRef.current = false;
    });
  }, [unlinkRequested, connected, walletAddress, connecting, disconnecting, unlinkWallet, disconnectWallet]);

  useEffect(() => {
    if (!linkRequested || wallet || openingWalletModal || connected) return;

    const timer = window.setTimeout(() => {
      setLinkRequested(false);
    }, 120000);

    return () => window.clearTimeout(timer);
  }, [linkRequested, wallet, openingWalletModal, connected]);

  useEffect(() => {
    if (!unlinkRequested || wallet || openingWalletModal || connected) return;

    const timer = window.setTimeout(() => {
      setUnlinkRequested(false);
    }, 120000);

    return () => window.clearTimeout(timer);
  }, [unlinkRequested, wallet, openingWalletModal, connected]);

  const [showUnlinkDialog, setShowUnlinkDialog] = useState(false);

  const label = useMemo(() => {
    if (connecting)    return '지갑 연결 중...';
    if (disconnecting) return '지갑 연결 해제 중...';
    if (isProcessing)  return '처리 중...';
    if (unlinkRequested) return '지갑 서명 대기 중...';
    if (displayedWalletAddress) {
      return `${connected ? (walletName ?? 'Wallet') : '연동 지갑'} · ${shortenWalletAddress(displayedWalletAddress)}`;
    }
    return '지갑 연결';
  }, [connected, connecting, disconnecting, displayedWalletAddress, walletName, isProcessing, unlinkRequested]);

  const handleClick = () => {
    if (connected || isDbLinkedOnly) {
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
    if (connected) {
      const result = await unlinkWallet();
      if (result.success) {
        setLinkedWalletAddress(null);
        await disconnectWallet();
      }
      return;
    }

    setUnlinkRequested(true);

    if (wallet) {
      setOpeningWalletModal(true);
      deselectWallet();
    } else {
      openWalletModal();
    }
  };

  const isLoading = connecting || disconnecting || isProcessing || linkRequested || unlinkRequested;

  return (
    <>
      {displayedWalletAddress ? (
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
                  {displayedWalletAddress ? shortenWalletAddress(displayedWalletAddress) : ""}
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
