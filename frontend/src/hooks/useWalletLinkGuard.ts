import { useMemo } from "react";
import type { WalletLinkAvailability } from "@/types/wallet";

export function useWalletLinkGuard(
  isLoggedIn: boolean,
  isProfileComplete: boolean
): WalletLinkAvailability {
  return useMemo(() => {
    if (!isLoggedIn) {
      return {
        canLinkWallet: false,
        reason: "로그인 후 지갑을 연결할 수 있습니다.",
      };
    }

    if (!isProfileComplete) {
      return {
        canLinkWallet: false,
        reason: "상세정보 입력 완료 후 지갑 연동이 가능합니다.",
      };
    }

    return {
      canLinkWallet: true,
      reason: null,
    };
  }, [isLoggedIn, isProfileComplete]);
}
