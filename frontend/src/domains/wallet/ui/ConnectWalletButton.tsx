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

  const prevConnected = useRef(connected);

  // 지갑이 새로 연결되는 순간(false → true) 자동으로 계정 연동 시도
  useEffect(() => {
    if (!prevConnected.current && connected && walletAddress) {
      void linkWallet();
    }
    prevConnected.current = connected;
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
