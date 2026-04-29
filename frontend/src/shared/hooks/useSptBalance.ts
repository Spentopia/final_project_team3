// useSptBalance.ts
// DB에 저장된 wallet_address로 온체인 SPT 잔액을 직접 읽는 훅.
// wallet adapter 연결 여부와 무관하게 동작한다.

import { useEffect, useState, useCallback } from "react";
import { useConnection } from "@solana/wallet-adapter-react";
import { getAssociatedTokenAddressSync } from "@solana/spl-token";
import { PublicKey } from "@solana/web3.js";

const PROGRAM_ID = new PublicKey(
    import.meta.env.VITE_SPENTOPIA_PROGRAM_ID?.trim() ||
    "9s5Z96GSLVgVsnj5NAZ1HoxPvaF8Re8B1LeSmcBKQv61",
);

const SPT_MINT = PublicKey.findProgramAddressSync(
    [Buffer.from("spt_token_mint")],
    PROGRAM_ID,
)[0];

const SPT_DECIMALS = 1_000_000;

export interface UseSptBalanceReturn {
    sptBalance: number | null;
    sptLoading: boolean;
    refreshSptBalance: () => void;
}

export function useSptBalance(walletAddress: string | null): UseSptBalanceReturn {
    const { connection } = useConnection();
    const [sptBalance, setSptBalance] = useState<number | null>(null);
    const [sptLoading, setSptLoading] = useState(false);

    const fetchBalance = useCallback(() => {
        if (!walletAddress) {
            setSptBalance(null);
            return;
        }

        let cancelled = false;
        setSptLoading(true);

        let ownerKey: PublicKey;
        try {
            ownerKey = new PublicKey(walletAddress);
        } catch {
            setSptBalance(0);
            setSptLoading(false);
            return;
        }

        const ata = getAssociatedTokenAddressSync(SPT_MINT, ownerKey);

        connection
            .getTokenAccountBalance(ata)
            .then((res) => {
                if (cancelled) return;
                const raw = res.value.amount;
                setSptBalance(Math.floor(Number(raw) / SPT_DECIMALS));
            })
            .catch(() => {
                if (!cancelled) setSptBalance(0);
            })
            .finally(() => {
                if (!cancelled) setSptLoading(false);
            });

        return () => {
            cancelled = true;
        };
    }, [connection, walletAddress]);

    useEffect(() => {
        const cleanup = fetchBalance();
        return cleanup;
    }, [fetchBalance]);

    return { sptBalance, sptLoading, refreshSptBalance: fetchBalance };
}
