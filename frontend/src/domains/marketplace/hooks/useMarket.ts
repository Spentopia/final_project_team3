// useMarket.ts - 마켓플레이스 커스텀 훅
import {useState, useCallback, useEffect} from "react";
import {toast} from "sonner";
import {
    getListings as getListingsApi,
    createListing as createListingApi,
    updateEscrow as updateEscrowApi,
    purchaseItem as purchaseItemApi,
} from "@/domains/marketplace/api/marketApi.ts";
import type {ListingResponse, TransactionResponse} from "@/domains/marketplace/model/types.ts";

// 훅 반환 타입
interface UseMarketReturn {
    listings: ListingResponse[];
    listingsLoading: boolean;
    createListing: (itemId: string, priceSpt: number) => Promise<ListingResponse | null>;
    updateEscrow: (listingId: string, escrowAddress: string) => Promise<void>;
    purchaseItem: (listingId: string, txSignature: string)=>Promise<TransactionResponse | null>;
    creatingListing: boolean;
    updatingEscrow: boolean;
    purchasing: boolean;
    createError: string | null;
    purchaseError: string | null;
}

export function useMarket(): UseMarketReturn{
    const [listings, setListings] = useState<ListingResponse[]>([]);
    const [listingsLoading, setListingsLoading] = useState(false);
    const [creatingListing, setCreatingListing] = useState(false);
    const [updatingEscrow, setUpdatingEscrow] = useState(false);
    const [purchasing, setPurchasing] = useState(false);
    const [createError, setCreateError] = useState<string | null>(null);
    const [purchaseError, setPurchaseError] = useState<string | null>(null);

    // 마운트 시 판매 목록 조회
    useEffect(() => {
        setListingsLoading(true);
        getListingsApi()
            .then(setListings)
            .catch(() => toast.error("판매 목록을 불러오지 못했습니다."))
            .finally(() => setListingsLoading(false));
    }, []);

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
        listingsLoading,
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