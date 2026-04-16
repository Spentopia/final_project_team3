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

// 브라우저 지갑 연결 + 계정 연동 버튼
// 지갑이 새로 연결되면 자동으로 /wallet/link 를 호출해 DB에 지갑 주소를 저장한다.
export function ConnectWalletButton({className}: ConnectWalletButtonProps){
  const {
      connected,
      connecting,
      disconnecting,
      walletAddress,
      walletName,
      openWalletModal,
      disconnectWallet,
      linkWallet,
      unlinkWallet,
      isProcessing,
  }=useWalletConnection();

  // connected && walletAddress 둘 다 준비됐을 때를 "ready" 상태로 판단
  const wasReady = useRef(connected && !!walletAddress);

  // linkWallet을 ref로 보관해 effect 의존성에서 제외한다.
  // linkWallet 레퍼런스 변경으로 인한 불필요한 effect 재실행을 막기 위해서다.
  const linkWalletRef = useRef(linkWallet);
  useEffect(() => {
    linkWalletRef.current = linkWallet;
  }, [linkWallet]);

  // ready 상태가 false → true로 바뀌는 순간 연동 시도
  useEffect(() => {
    const isReady = connected && !!walletAddress;
    if (!wasReady.current && isReady) {
      void linkWalletRef.current();
    }
    wasReady.current = isReady;
  }, [connected, walletAddress]);

  // 지갑 연동 해제 확인 팝업 상태
  const [showUnlinkDialog, setShowUnlinkDialog] = useState(false);

  const label = useMemo(()=>{
    if (connecting){
      return '지갑 연결 중...';
    }

    if (disconnecting){
      return '지갑 연결 해제 중...';
    }

    if (isProcessing) {
      return '처리 중...';
    }

    if (connected && walletAddress){
      return `${walletName ?? 'Wallet'} · ${shortenWalletAddress(walletAddress)}`;
    }
    return '지갑 연결';
  },[connected,connecting,disconnecting,walletAddress,walletName,isProcessing]);

  const handleClick = () => {
    if (connected){
      setShowUnlinkDialog(true);
      return;
    }
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
              <AlertDialogAction
                onClick={() => { void handleUnlinkConfirm(); }}
              >
                해제
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </>
  );
}
