// useSptBalance.ts
// 연결된 지갑의 SPT 토큰 잔액을 온체인에서 직접 읽는 훅.
//
// SPT 민트 주소는 스마트컨트랙트 PDA("spt_token_mint") 에서 결정론적으로 유도.
// ATA가 없으면(잔액 0) 에러 대신 0을 반환.

import { useEffect, useState, useCallback } from "react";
import { useConnection, useWallet } from "@solana/wallet-adapter-react";
import { getAssociatedTokenAddressSync } from "@solana/spl-token";
import { PublicKey } from "@solana/web3.js";

const PROGRAM_ID = new PublicKey(
    import.meta.env.VITE_SPENTOPIA_PROGRAM_ID?.trim() ||
    "9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61",
);

// spt_token_mint PDA — 스마트컨트랙트와 동일한 seed
const SPT_MINT = PublicKey.findProgramAddressSync(
    [Buffer.from("spt_token_mint")],
    PROGRAM_ID,
)[0];

// 온체인 raw amount → 사람이 읽는 정수 SPT (소수점 6자리)
const SPT_DECIMALS = 1_000_000;

export interface UseSptBalanceReturn {
    sptBalance: number | null; // null = 아직 로딩 중
    sptLoading: boolean;
    refreshSptBalance: () => void;
}

export function useSptBalance(): UseSptBalanceReturn {
    const { connection } = useConnection();
    const { publicKey, connected } = useWallet();
    const [sptBalance, setSptBalance] = useState<number | null>(null);
    const [sptLoading, setSptLoading] = useState(false);

    const fetchBalance = useCallback(() => {
        if (!connected || !publicKey) {
            setSptBalance(null);
            return;
        }

        let cancelled = false;
        setSptLoading(true);

        const ata = getAssociatedTokenAddressSync(SPT_MINT, publicKey);

        connection
            .getTokenAccountBalance(ata)
            .then((res) => {
                if (cancelled) return;
                const raw = res.value.amount; // string (u64)
                setSptBalance(Math.floor(Number(raw) / SPT_DECIMALS));
            })
            .catch(() => {
                // ATA 미존재(잔액 0) 포함 모든 에러 → 0으로 처리
                if (!cancelled) setSptBalance(0);
            })
            .finally(() => {
                if (!cancelled) setSptLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [connection, publicKey, connected]);

    useEffect(() => {
        const cleanup = fetchBalance();
        return cleanup;
    }, [fetchBalance]);

    return { sptBalance, sptLoading, refreshSptBalance: fetchBalance };
}
