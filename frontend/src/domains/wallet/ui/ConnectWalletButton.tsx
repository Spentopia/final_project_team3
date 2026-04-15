import {useWalletConnection} from "@/domains/wallet/hooks/useWalletConnection";
import {useEffect, useMemo, useRef} from "react";
import {shortenWalletAddress} from "@/domains/wallet/lib/solana";
import {Button} from "@/shared/ui/button.tsx";
import {Link as LinkIcon, Wallet} from "lucide-react";

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
      isProcessing,
  }=useWalletConnection();

  // connected && walletAddress 둘 다 준비됐을 때를 "ready" 상태로 판단
  const wasReady = useRef(connected && !!walletAddress);

  // ready 상태가 false → true로 바뀌는 순간 연동 시도
  // connected와 walletAddress가 별도 렌더에서 세팅되는 경우도 안전하게 처리
  useEffect(() => {
    const isReady = connected && !!walletAddress;
    if (!wasReady.current && isReady) {
      void linkWallet();
    }
    wasReady.current = isReady;
  }, [connected, walletAddress, linkWallet]);

  const label = useMemo(()=>{
    if (connecting){
      return '지갑 연결 중...';
    }

    if (disconnecting){
      return '지갑 연결 해제 중...';
    }

    if (isProcessing) {
      return '연동 중...';
    }

    if (connected && walletAddress){
      return `${walletName ?? 'Wallet'} · ${shortenWalletAddress(walletAddress)}`;
    }
    return '지갑 연결';
  },[connected,connecting,disconnecting,walletAddress,walletName,isProcessing]);

  const handleClick = async () => {
    if (connected){
      await disconnectWallet();
      return;
    }

    openWalletModal();
  };

  return (
      <Button
        type="button"
        className={className}
        onClick={()=>{
          void handleClick();
        }}
        disabled={connecting || disconnecting || isProcessing}
        variant={connected ? "outline" : "default"}
      >
        {connected ? <Wallet className="h-4 w-4" /> : <LinkIcon className="h-4 w-4" />}
        {label}
      </Button>
  );
}
