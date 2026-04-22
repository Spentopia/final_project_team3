// useAvatarItems.ts - 아바타 아이템 커스텀 훅
//
// 훅의 역할:
// - 컴포넌트가 "무엇을 화면에 보여줄지"만 신경 쓰게
// - "어떻게 데이터를 가져오는지"는 훅이 캡슐화
// - 여러 컴포넌트에서 재사용 가능 (MarketplacePage에서도 사용)
//
// React 훅 규칙:
// - 이름은 반드시 "use"로 시작
// - 컴포넌트 최상위에서만 호출 가능 (조건문/반복문 내부 X)

import {useState, useEffect, useCallback} from "react";
// useState: 상태 선언
// useEffect: 마운트 시 사이드이펙트 (API 호출) 실행
// useCallback: 함수를 메모이제이션 - 불필요한 리렌더링 방지

import {toast} from "sonner";
// sonner: shadcn 기본 토스트 라이브러리. toast.success(), toast.error() 사용

import {getUserItems, mintNft as mintNftApi, transferNft as transferNftApi} from "@/domains/avatar/api/avatarApi.ts";
// mintNft, transferNft는 이미 사용 중인 이름이 될 수 있음 "Api" suffix로 alias

import type {UserItemResponse} from "@/domains/avatar/model/types.ts";

// 훅 반환 타입 명시 - 컴포넌트가 받는 인터페이스를 명확히
interface UseAvatarItemsReturn {
    items: UserItemResponse[];
    loading: boolean;
    error: string | null;
    refetch: () => void;  // 수동 재조회 트리거
    mintNft: (userItemId: string, mintAddress: string, txSignature: string) => Promise<void>;
    transferNft: (avatarId: string, mintAddress: string, txSignature: string) => Promise<void>;
    // 각 액션별 로딩 상태 (아이템 목록 로딩과 구분)
    minting: boolean;
    transferring: boolean;
}

export function useAvatarItems(): UseAvatarItemsReturn {
    // 상태 선언
    // items의 초기값이 [] (빈 배열)인 이유:
    // - null이면 컴포넌트에서 items.map() 호출 시 에러 발생
    // - 빈 배열이면 map이 그냥 아무것도 렌더링 안 함 (안전)
    const [items, setItems] = useState<UserItemResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [minting, setMinting] = useState(false);
    const [transferring, setTransferring] = useState(false);

    // fetchItems: 아이템 목록 조회 함수
    //
    // useCallback을 쓰는 이유:
    // - fetchItems를 useEffect 의존성 배열에 넣어야 하는데, 매 렌더링마다 새 함수 객체가 만들어지면 무한 루프 발생
    // - useCallback([])으로 한 번만 생성되게 고정
    const fetchItems = useCallback(async () => {
        setLoading(true);
        setError(null); // 이전 에러 초기화 (재시도 시 에러 메세지 사라지게)
        try {
            const data = await getUserItems();
            setItems(data);
        } catch (err) {
            // err instanceof Error로 체크: Error 객체면 message, 아니면 기본 메세지
            setError(err instanceof Error ? err.message : "아이템 목록 조회 중 오류가 발생했습니다.");
        } finally {
            // finally: try/catch 결과와 무관하게 항상 실행
            // loading을 여기서 false로: 성공/실패 모두 스피너를 멈춰야 하니까
            setLoading(false);
        }
    }, []); // 의존성 없음 - 함수 참조가 변경되지 않음

    // useEffect: 컴포넌트 마운트 시 자동 호출
    // 의존성 배열 [fetchItems]: fetchItems가 바뀔 때 재실행
    // → fetchItems는 useCallback([])으로 고정되어 있어 최초 1회만 실행
    useEffect(() => {
        fetchItems();
    }, [fetchItems]);

    // mintNft: NFT 민팅 기록 + 목록 갱신
    //
    // 순서:
    //  1. minting = true (버튼 비활성화)
    //  2. API 호출 (DB 기록)
    //  3. 성공 → toast + 목록 갱신 (is_nft가 true로 바뀌었기 때문)
    //  4. 실패 → toast.error
    //  5. finally: minting = false
    const mintNft = useCallback(async (userItemId: string, mintAddress: string, txSignature: string) => {
        setMinting(true);
        try {
            await mintNftApi({
                user_item_id: userItemId,
                nft_mint_address: mintAddress,
                tx_signature: txSignature,
            });
            toast.success("NFT 민팅이 완료되었습니다.");
            await fetchItems(); // 민팅 후 is_nft상태가 바뀌므로 목록 새로고침
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "NFT 민팅 중 오류가 발생했습니다.");
        } finally {
            setMinting(false);
        }
    }, [fetchItems]); // fetchItems에 의존 (fetchItems는 안정적인 참조라 괜찮음)

    // transferNft: NFT 전송 기록 + 목록 갱신
    // mintNft와 구조 동일. 액션만 다름
    const transferNft = useCallback(async (avatarId: string, mintAddress: string, txSignature: string) => {
        setTransferring(true);
        try {
            await transferNftApi({
                avatar_id: avatarId,
                nft_mint_address: mintAddress,
                tx_signature: txSignature,
            });
            toast.success("NFT 전송이 완료되었습니다.");
            await fetchItems(); // 전송 후 소유권 변경 반영
        } catch (err) {
            toast.error(err instanceof Error ? err.message : "NFT 전송 중 오류가 발생했습니다.");
        } finally {
            setTransferring(false);
        }
    }, [fetchItems]);

    // 반환값: 컴포넌트가 사용할 데이터와 함수들
    // refetch를 노출하는 이유: 에러 시 "재시도" 버튼에서 직접 호출 가능
    return {
        items,
        loading,
        error,
        refetch: fetchItems,
        mintNft,
        transferNft,
        minting,
        transferring,
    };


}
