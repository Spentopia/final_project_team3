// avatarApi.ts - 아바타 도메인 API 함수 모음
//
// 역할: axios 호출 로직을 캡슐화
// 분리하는 이유: 훅(useAvatarItems)이 직접 axios를 호출하면
// - 훅이 너무 많은 책임을 가짐 (관심사 분리 위반)
// - 같은 API를 여러 훅에서 쓸 때 중복 발생
// → api 함수를 따로 두거나 조합만 담당
import {apiClient} from "@/shared/api/client.ts";
// apiClient: baseURL + Bearer 토큰 자동 주입 + 401 자동 재시도가 이미 설정된 axios 인스턴스

import type {
    UserItemResponse,
    OwnedNftResponse,
    SyncOwnedNftsResponse,
    MintNftRequest,
    MintNftResponse,
    TransferNftRequest,
    TransferNftResponse,
} from "@/domains/avatar/model/types";
// "import type": 타입만 임포트 (런타임 번들에 포함 안 됨, 최적화)

// GET /api/avatar/items
// 내가 보유한 아바타 아이템 전체 목록 조회
// 인증: apiClient 인터셉터가 Bearer 토큰 자동 주입
export async function getUserItems(): Promise<UserItemResponse[]> {
    try {
        const res = await apiClient.get<UserItemResponse[]>("/api/avatar/items");
        return res.data;
    } catch (error: unknown) {
        if (error && typeof error === "object" && "response" in error) {
            const axiosError = error as { response?: { status?: number; data?: unknown } };
            const detail =
                typeof axiosError.response?.data === "string"
                    ? axiosError.response.data
                    : JSON.stringify(axiosError.response?.data);
            throw new Error(`아이템 목록 조회 실패: ${axiosError.response?.status} ${detail}`);
        }
        throw error;
    }
}

// GET /api/avatar/nfts
// 연동된 지갑의 온체인 NFT 목록 조회 (Helius DAS API)
export async function getOwnedNfts(): Promise<OwnedNftResponse[]> {
    try {
        const res = await apiClient.get<OwnedNftResponse[]>("/api/avatar/nfts");
        return res.data;
    } catch (error: unknown) {
        if (error && typeof error === "object" && "response" in error) {
            const axiosError = error as { response?: { status?: number; data?: unknown } };
            const detail =
                typeof axiosError.response?.data === "string"
                    ? axiosError.response.data
                    : JSON.stringify(axiosError.response?.data);
            throw new Error(`NFT 목록 조회 실패: ${axiosError.response?.status} ${detail}`);
        }
        throw error;
    }
}

let _syncOwnedNftsInflight: Promise<SyncOwnedNftsResponse> | null = null;
let _lastSyncOwnedNftsAt = 0;
const SYNC_OWNED_NFTS_COOLDOWN_MS = 30_000;

export async function syncOwnedNfts(options: { force?: boolean } = {}): Promise<SyncOwnedNftsResponse> {
    if (_syncOwnedNftsInflight) return _syncOwnedNftsInflight;

    const now = Date.now();
    if (!options.force && now - _lastSyncOwnedNftsAt < SYNC_OWNED_NFTS_COOLDOWN_MS) {
        return { synced_count: 0, skipped_count: 0 };
    }

    try {
        _syncOwnedNftsInflight = apiClient
            .post<SyncOwnedNftsResponse>("/api/avatar/nfts/sync")
            .then((res) => {
                _lastSyncOwnedNftsAt = Date.now();
                return res.data;
            })
            .finally(() => {
                _syncOwnedNftsInflight = null;
            });
        return await _syncOwnedNftsInflight;
    } catch (error: unknown) {
        if (error && typeof error === "object" && "response" in error) {
            const axiosError = error as { response?: { status?: number; data?: unknown } };
            const detail =
                typeof axiosError.response?.data === "string"
                    ? axiosError.response.data
                    : JSON.stringify(axiosError.response?.data);
            throw new Error(`NFT 동기화 실패: ${axiosError.response?.status} ${detail}`);
        }
        throw error;
    }
}

// POST /api/avatar/mint-nft
// Solana 온체인 민팅 완료 후 → DB에 mint_address 기록
// 이 함수 자체는 Solana와 통신하지 않음. DB 기록만 담당.
export async function mintNft(req: MintNftRequest): Promise<MintNftResponse> {
    try {
        const res = await apiClient.post<MintNftResponse>("api/avatar/mint-nft", req);
        // req 객체가 JSON body로 직렬화되어 전송됨
        return res.data;
    } catch (error: unknown) {
        if (error && typeof error === "object" && "response" in error) {
            const axiosError = error as { response?: { status?: number; data?: unknown } };
            const detail =
                typeof axiosError.response?.data === "string"
                    ? axiosError.response.data
                    : JSON.stringify(axiosError.response?.data);
            throw new Error(`NFT 민팅 기록 실패: ${axiosError.response?.status} ${detail}`);
        }
        throw error;
    }
}

// POST /api/avatar/transfer-nft
// Solana 온체인 전송 완료 후 → DB에 전송 기록
export async function transferNft(req: TransferNftRequest): Promise<TransferNftResponse> {
    try {
        const res = await apiClient.post<TransferNftResponse>("/api/avatar/transfer-nft", req);
        return res.data;
    } catch (error: unknown) {
        if (error && typeof error === "object" && "response" in error) {
            const axiosError = error as { response?: { status?: number; data?: unknown } };
            const detail =
                typeof axiosError.response?.data === "string"
                    ? axiosError.response.data
                    : JSON.stringify(axiosError.response?.data);
            throw new Error(`MFT 전송 기록 실패: ${axiosError.response?.status} ${detail}`);
        }
        throw error;
    }
}
