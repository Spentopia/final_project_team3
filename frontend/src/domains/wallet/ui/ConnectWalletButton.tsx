import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useMemo, useRef, useState} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Link as LinkIcon, Wallet, Loader2} from "lucide-react";
import {apiClient} from "@/shared/api/client";
import {toast} from "sonner";
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
      walletModalVisible,
      openWalletModal,
      connectWallet,
      disconnectWallet,
      deselectWallet,
      linkWallet,
      unlinkWallet,
      isProcessing,
  } = useWalletConnection();

  const [linkedWalletAddress, setLinkedWalletAddress] = useState<string | null>(null);
  const displayedWalletAddress = linkedWalletAddress;
  const isLinkedWalletConnected = Boolean(
    linkedWalletAddress && connected && walletAddress === linkedWalletAddress
  );

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

  useEffect(() => {
    const handleWalletChange = (event: Event) => {
      const walletAddress = (event as CustomEvent<{ walletAddress: string | null }>).detail
        ?.walletAddress;
      setLinkedWalletAddress(walletAddress ?? null);
    };

    window.addEventListener("spentopia:wallet-change", handleWalletChange);
    return () => {
      window.removeEventListener("spentopia:wallet-change", handleWalletChange);
    };
  }, []);

  const [linkRequested, setLinkRequested] = useState(false);
  const [unlinkRequested, setUnlinkRequested] = useState(false);
  const [openingWalletModal, setOpeningWalletModal] = useState(false);
  const connectInFlightRef = useRef(false);
  const linkInFlightRef = useRef(false);
  const unlinkInFlightRef = useRef(false);

  useEffect(() => {
    if (!openingWalletModal || wallet || disconnecting) return;

    setOpeningWalletModal(false);
    openWalletModal();
  }, [openingWalletModal, wallet, disconnecting, openWalletModal]);

  useEffect(() => {
    if (
      !(linkRequested || unlinkRequested) ||
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
        setUnlinkRequested(false);
      })
      .finally(() => {
        connectInFlightRef.current = false;
      });
  }, [
    linkRequested,
    unlinkRequested,
    wallet,
    connected,
    connecting,
    disconnecting,
    openingWalletModal,
    walletModalVisible,
    connectWallet,
  ]);

  // 지갑이 연결되면 연동(linkWallet) 시도.
  useEffect(() => {
    if (
      !linkRequested ||
      !connected ||
      !walletAddress ||
      connecting ||
      disconnecting ||
      openingWalletModal ||
      walletModalVisible ||
      linkInFlightRef.current
    ) return;

    linkInFlightRef.current = true;
    setLinkRequested(false);
    linkWalletRef.current().then((result) => {
      if (result.success) {
        setLinkedWalletAddress(walletAddress);
        window.dispatchEvent(
          new CustomEvent("spentopia:wallet-change", {
            detail: { walletAddress },
          })
        );
        toast.success(result.message);
      } else {
        toast.error(result.message);
        void disconnectWallet().catch(() => {
          // 연결 실패 후 로컬 adapter 정리만 시도한다.
        }).finally(() => {
          deselectWallet();
        });
      }
    }).finally(() => {
      linkInFlightRef.current = false;
    });
  }, [
    linkRequested,
    connected,
    walletAddress,
    connecting,
    disconnecting,
    openingWalletModal,
    walletModalVisible,
    disconnectWallet,
    deselectWallet,
  ]);

  useEffect(() => {
    if (
      !unlinkRequested ||
      !connected ||
      !walletAddress ||
      connecting ||
      disconnecting ||
      unlinkInFlightRef.current
    ) return;

    unlinkInFlightRef.current = true;
    setUnlinkRequested(false);

    if (linkedWalletAddress && walletAddress !== linkedWalletAddress) {
      toast.error("계정에 연동된 지갑과 현재 연결된 지갑이 다릅니다.");
      void disconnectWallet().catch(() => {
        // 잘못 연결된 adapter 정리만 시도한다.
      }).finally(() => {
        deselectWallet();
        unlinkInFlightRef.current = false;
      });
      return;
    }

    unlinkWallet().then((result) => {
      if (result.success) {
        setLinkedWalletAddress(null);
        window.dispatchEvent(
          new CustomEvent("spentopia:wallet-change", {
            detail: { walletAddress: null },
          })
        );
        void disconnectWallet().catch(() => {
          // 서버 연동 해제는 성공했으므로 로컬 adapter 선택 상태 정리는 계속 진행한다.
        }).finally(() => {
          deselectWallet();
        });
        toast.success(result.message);
      } else {
        toast.error(result.message);
      }
    }).finally(() => {
      unlinkInFlightRef.current = false;
    });
  }, [
    unlinkRequested,
    connected,
    walletAddress,
    connecting,
    disconnecting,
    unlinkWallet,
    disconnectWallet,
    deselectWallet,
  ]);

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
    if (displayedWalletAddress) {
      return `${isLinkedWalletConnected ? (walletName ?? 'Wallet') : '연동 지갑'} · ${shortenWalletAddress(displayedWalletAddress)}`;
    }
    return '지갑 연결';
  }, [isLinkedWalletConnected, connecting, disconnecting, displayedWalletAddress, walletName, isProcessing]);

  const handleClick = () => {
    if (linkedWalletAddress) {
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
    setShowUnlinkDialog(false);

    if (connected && linkedWalletAddress && walletAddress !== linkedWalletAddress) {
      toast.error("계정에 연동된 지갑과 현재 연결된 지갑이 다릅니다.");
      return;
    }

    setUnlinkRequested(true);

    if (!connected && wallet) {
      return;
    }

    if (!connected) {
      toast.error("지갑 앱에서 연결을 먼저 허용해 주세요.");
      return;
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
                계정에 연동된 지갑:{" "}
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
