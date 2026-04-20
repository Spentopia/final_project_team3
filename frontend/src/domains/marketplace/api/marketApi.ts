// marketApi.ts - 마켓플레이스 도메인 API 함수 모음
// avatarApi.ts와 동일한 패턴: axios 캡슐화 레이어

import {apiClient} from "@/shared/api/client.ts";
import type {
    CreateListingRequest,
    ListingResponse,
    UpdateEscrowResponse,
    PurchaseRequest,
    TransactionResponse,
} from "@/domains/marketplace/model/types.ts";

// 에러 파싱 헬퍼 - 3개 함수 모두 같은 패턴이라 함수로 추출
// 왜 별도 헬퍼: DRY 원칙 (Don't Repeat Yourself)
// 위치: 이 파일 내부 private 함수 (export 없음)
function parseAxiosError(error: unknown, prefix: string): Error {
    if (error && typeof error === "object" && "response" in error) {
        const axiosError = error as { response?: { status?: number; data?: unknown } };
        const detail =
            typeof axiosError.response?.data === "string"
                ? axiosError.response.data
                : JSON.stringify(axiosError.response?.data);
        return new Error(`${prefix}: ${axiosError.response?.status} ${detail}`);
    }
    // AxiosError가 아닌 경우 (타입 가드 실패) → Error로 래핑
    return error instanceof Error ? error : new Error(String(error));
}

// POST /api/market/listings
// NFT 아이템을 마켓에 판매 등록
// 주의: is_nft === true인 아이템만 등록 가능 (백엔드에서도 검증)
export async function createListing(req: CreateListingRequest): Promise<ListingResponse>{
    try{
        const res = await apiClient.post<ListingResponse>("/api/market/listings",req);
        return res.data;
    }catch(error:unknown){
        throw parseAxiosError(error, "판매 등록 실패");
    }
}

// PATCH /api/market/listings/{id}/escrow
// Solana의 list_nft 명령어 실행 후 생성된 escrow PDA를 DB에 저장
//
// escrow PDA란:
// - Solana 프로그램이 "seeds + programId"로 결정론적 생성한 주소
// - 아이템이 팔릴 때까지 NFT를 이 주소가 임시 보관
// - 개인키가 없으므로 프로그램만 제어 가능 (trustless)
export async function updateEscrow(
    listingId: string,          // URL 경로에 삽입: /api/market/listings/{listingId}/escrow
    escrowAddress: string       // body에 담아 전송
): Promise<UpdateEscrowResponse>{
    try{
        const res = await apiClient.patch<UpdateEscrowResponse>(
            `/api/market/listings/${listingId}/escrow`,
            {escrow_address: escrowAddress} // UpdateEscrowRequest 인라인 구성
        );
        return res.data;
    }catch(error: unknown){
        throw parseAxiosError(error, "에스크로 주소 저장 실패");
    }
}

// POST /api/market/purchase
// Solana 구매 트랜잭션 완료 후 → DB에 거래 기록
// tx_signature 없으면 백엔드가 400 반환
export async function purchaseItem(req: PurchaseRequest): Promise<TransactionResponse>{
    try{
        const res = await apiClient.post<TransactionResponse>("/api/market/purchase",req);
        return res.data;
    }catch(error:unknown){
        throw parseAxiosError(error, "구매 실패");
    }
}