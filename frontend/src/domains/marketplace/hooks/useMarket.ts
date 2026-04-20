// useMarket.ts - 마켓플레이스 커스텀 훅
//
// 아바타 훅과의 차이점:
// - 마운트 시 자동 목록 조회가 없음 (GET 엔드포인트 미구현)
// - 대신 로컬 상태(listings)로 판매 목록을 관리
// - createListing 성공 → 응답을 직접 로컬 배열에 추가
import {useState, useCallback} from "react";
import {toast} from "sonner";
import {
    createListing as createListingApi,
    updateEscrow as updateEscrowApi,
    purchaseItem as purchaseItemApi,
} from "@/domains/marketplace/api/marketApi.ts";
import type {ListingResponse, TransactionResponse} from "@/domains/marketplace/model/types.ts";

// 훅 반환 타입
interface UseMarketReturn {
    listings: ListingResponse[];        // 로컬에서 관리하는 판매 목록
    createListing: (itemId: string, priceSpt: number) => Promise<ListingResponse | null>;
    updateEscrow: (listingId: string, escrowAddress: string) => Promise<void>;
    purchaseItem: (listingId: string, txSignature: string)=>Promise<TransactionResponse | null>;
    // 각 액션별 독립 로딩 상태 - 하나의 loading으로 묶으면 UX가 어색해짐
    creatingListing: boolean;
    updatingEscrow: boolean;
    purchasing: boolean;
    // 에러는 toast로 처리하지만, 컴포넌트에서 직접 쓸 수 있게 노출
    createError: string | null;
    purchaseError: string | null;
}

export function useMarket(): UseMarketReturn{
    // listings: 백엔드에 GET이 없으므로 로컬 배열로 관리
    // createListing 성공 응답을 직접 append
    // 새로고침하면 초기화됨 (한계 - 추후 GET 구현 시 교체)
    const [listings, setListings] = useState<ListingResponse[]>([]);
    const [creatingListing, setCreatingListing] = useState(false);
    const [updatingEscrow, setUpdatingEscrow] = useState(false);
    const [purchasing, setPurchasing] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);
    const [purchaseError, setPurchaseError] = useState<string | null>(null);

    // createListing: 판매 등록
    // 반환값: 성공 시 ListingResponse, 실패 시 null
    // null을 반환하는 이유: 컴포넌트에서 성공 여부를 반환값으로도 확인 가능하게 (toast만으로 처리해도 되지만, 유연성 확보)
    const createListing = useCallback(async (
        itemId: string,
        priceSpt: number
    ):Promise<ListingResponse | null>=>{
        setCreatingListing(true);
        setCreateError(null);
        try{
            const newListing = await createListingApi({item_id: itemId, price_spt:priceSpt});

            // 성공한 리스팅을 로컬 목록 앞에 추가 (최신 항목이 위에 오게)
            // prev: 이전 상태 - React가 비동기 환경에서 안전하게 상태를 업데이트하는 패턴
            setListings((prev)=>[newListing, ...prev]);

            toast.success("판매 등록이 완료되었습니다.");
            return newListing;
        }catch (err){
            const message = err instanceof Error ? err.message : "판매 등록 중 오류가 발생했습니다.";
            setCreateError(message);
            toast.error(message);
            return null;
        } finally {
            setCreatingListing(false);
        }
    }, []);

    // updateEscrow: escrow PDA 주소 저장
    // 성공/실패 모두 toast로만 처리 (반환값 불필요)
    const updateEscrow = useCallback(async(
        listingId: string,
        escrowAddress: string
    ): Promise<void>=>{
        setUpdatingEscrow(true);
        try{
            await updateEscrowApi(listingId, escrowAddress);
            toast.success("에스크로 주소가 저장되었습니다.");
        }catch (err){
            toast.error(err instanceof Error ? err.message : "에스크로 저장 중 오류가 발생했습니다.");
        } finally {
            setUpdatingEscrow(false);
        }
    },[]);


    // purchaseItem: 아이템 구매
    // 성공 시 해당 listingId를 로컬 목록에서 제거 (sold 처리)
    const purchaseItem = useCallback(async(
        listingId: string,
        txSignature: string
    ): Promise<TransactionResponse | null>=>{
        setPurchasing(true)
        setPurchaseError(null);
        try{
            const result = await purchaseItemApi({listing_id: listingId, tx_signature:txSignature});

            // 구매 완료된 리스팅을 로컬 목록에서 제거
            setListings((prev)=>prev.filter((l)=>l.id !==listingId));
            toast.success("구매가 완료되었습니다.");
            return result;
        }catch (err){
            const message = err instanceof Error ? err.message : "구매 중 오류가 발생했습니다.";
            setPurchaseError(message);
            toast.error(message);
            return null;
        }finally {
            setPurchasing(false);
        }
    },[]);

    return {
        listings,
        createListing,
        updateEscrow,
        purchaseItem,
        creatingListing,
        updatingEscrow,
        purchasing,
        createError,
        purchaseError,
    };


}