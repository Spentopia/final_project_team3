import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useMemo, useRef, useState} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Button} from "@/shared/ui/button.tsx";
import {Link as LinkIcon, Wallet} from "lucide-react";
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
      connectWallet,
      disconnectWallet,
      deselectWallet,
      linkWallet,
      unlinkWallet,
      isProcessing,
  } = useWalletConnection();

  // 최신 함수를 ref로 보관해 effect 의존성에서 제외한다.
  const linkWalletRef = useRef(linkWallet);
  useEffect(() => { linkWalletRef.current = linkWallet; }, [linkWallet]);

  // 사용자가 직접 버튼을 눌렀을 때만 true.
  // 모달에서 지갑 선택 → connect() → linkWallet() 흐름을 직접 제어하기 위한 플래그.
  const pendingRef = useRef(false);

  // 이전 wallet 어댑터 이름을 기억해 "새 지갑이 선택됐는지"를 판단한다.
  const prevWalletNameRef = useRef<string | null>(wallet?.adapter.name ?? null);

  // deselectWallet()이 disconnect를 유발하면 disconnecting=true가 되고
  // wallet-adapter 내부에서 connect()가 즉시 return된다.
  // 새 지갑이 선택됐는데 아직 disconnecting 중이면 이 플래그를 세우고,
  // disconnecting이 끝난 뒤 effect가 재발화할 때 connect()를 호출한다.
  const pendingConnectRef = useRef(false);

  // autoConnect=false 이므로 모달에서 지갑이 선택(wallet 변경)되면 직접 connect()를 호출한다.
  useEffect(() => {
    const currentName = wallet?.adapter.name ?? null;
    const prevName = prevWalletNameRef.current;
    prevWalletNameRef.current = currentName;

    // 새 지갑이 선택되면 connect 대기 플래그를 세운다.
    if (currentName && currentName !== prevName && pendingRef.current && !connected) {
      pendingConnectRef.current = true;
    }

    // disconnecting이 끝나야 connect()가 실제로 실행된다.
    if (pendingConnectRef.current && wallet && !connected && !connecting && !disconnecting) {
      pendingConnectRef.current = false;
      connectWallet().catch(() => {
        pendingRef.current = false;
      });
    }
  }, [wallet, connected, connecting, disconnecting, connectWallet]);

  // 지갑이 연결되면 연동(linkWallet) 시도.
  useEffect(() => {
    if (connected && walletAddress && pendingRef.current) {
      pendingRef.current = false;
      void linkWalletRef.current();
    }
  }, [connected, walletAddress]);

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
    pendingRef.current = true;
    // 이전 선택 기록을 초기화해서 같은 지갑을 다시 선택해도 connect()가 호출되도록 한다.
    prevWalletNameRef.current = null;
    // wallet을 null로 초기화해야 같은 지갑을 다시 선택할 때 wallet 상태가 변해서 useEffect가 발화한다.
    // (이미 Phantom이 선택된 상태에서 Phantom을 다시 선택하면 상태 변화가 없어 connect()가 불리지 않음)
    deselectWallet();
    openWalletModal();
  };

  const handleUnlinkConfirm = async () => {
    await unlinkWallet();
    await disconnectWallet();
  };

  return (
    <>
      <Button
        type="button"
        className={className}
        onClick={handleClick}
        disabled={connecting || disconnecting || isProcessing}
        variant={connected ? "outline" : "default"}
      >
        {connected ? <Wallet className="h-4 w-4" /> : <LinkIcon className="h-4 w-4" />}
        {label}
      </Button>

      <AlertDialog open={showUnlinkDialog} onOpenChange={setShowUnlinkDialog}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>지갑 연동을 해제하시겠습니까?</AlertDialogTitle>
            <AlertDialogDescription>
              지갑 연동을 해제하면 지갑 로그인을 사용할 수 없게 됩니다.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel>취소</AlertDialogCancel>
            <AlertDialogAction onClick={() => { void handleUnlinkConfirm(); }}>
              해제
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  );
}
