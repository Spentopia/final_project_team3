import {useWalletConnection} from "@/hooks/useWalletConnection.ts";
import {useMemo} from "react";
import {shortenWalletAddress} from "@/lib/solana.ts";
import {Button} from "@/components/ui/button.tsx";
import {Link as LinkIcon, Wallet} from "lucide-react";

interface ConnectWalletButtonProps{
  className?: string;
}

// 브라우저 지갑 연결/해제 전용 버튼
// 이 버튼은 "계정-지갑 연동"이 아니라 프론트에서 Phantom/Solflare 같은 브라우저 지갑을 붙였다가 끊는 역할
export function ConnectWalletButton({className}: ConnectWalletButtonProps){
  const {
      connected,
      connecting,
      disconnecting,
      walletAddress,
      walletName,
      openWalletModal,
      disconnectWallet,
  }=useWalletConnection();

  const label = useMemo(()=>{
    if (connecting){
      return '지갑 연결 중...';
    }

    if (disconnecting){
      return '지갑 연결 해제 중...';
    }

    if (connected && walletAddress){
      return `${walletName ?? 'Wallet'} · ${shortenWalletAddress(walletAddress)}`;
    }
    return '지갑 연결';
  },[connected,connecting,disconnecting,walletAddress,walletName]);

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
        disabled={connecting || disconnecting}
        variant={connected ? "outline" : "default"}
      >
        {connected ? <Wallet className="h-4 w-4" /> : <LinkIcon className="h-4 w-4" />}
        {label}
      </Button>
  );
}
