// GET /api/avatar/items 응답 타입
// 백엔드 DB의 user_inventory + item_master JOIN 결과
export interface UserItemResponse {
    id: string;
    item_id: string;
    name: string;
    category: string; // hair / top / bottom / shoes / weapon / hat
    slot_name: string | null;
    image_url: string;
    metadata_uri: string | null;
    is_equipped: boolean | null;
    is_nft: boolean | null;
    nft_mint_address: string | null;
    minted_to_wallet: string | null;
    collection_mint: string | null;
    acquired_at: string | null;
}

// GET /api/avatar/nfts 응답 타입
// Helius getAssetsByOwner + item_master 매칭 결과
export interface OwnedNftResponse {
    mint_address: string;
    item_id: string | null;
    inventory_id: string | null; // user_inventory.id — 판매 등록용
    name: string;
    category: string | null;
    image_url: string | null;
    metadata_uri: string | null;
}

export interface SyncOwnedNftsResponse {
    synced_count: number;
    skipped_count: number;
}

// POST /api/avatar/mint-nft 요청 타입
// 흐름: Solana 민팅 완료 → 생성된 mint address를 백엔드 DB에 기록
//
export interface MintNftRequest{
    user_item_id: string;       // 민팅할 아이템 (user_items.id) — 백엔드가 직접 온체인 민팅 후 mint_address 반환
}

// POST /api/avatar/mint-nft 응답 타입
export interface MintNftResponse{
    message: string;            // 성공 메세지 (예: "NFT 민팅 완료")
    nft_mint_address: string;   // 저장된 mint address 확인용 에코백
}

// POST /api/avatar/transfer-nft 요청 타입
// 흐름: Solana 전송 트랜잭션 완료 → 백엔드 DB에 전송 기록
// mint-nft와 구분: mint는 새로 생성, transfer는 기존 NFT를 다른 지갑으로 이동
//
export interface TransferNftRequest{
    avatar_id: string;          // avatars.id (전송할 아바타 레코드 UUID)
    nft_mint_address: string;   // 어떤 NFT를 전송했는지
    tx_signature?: string;      // 온체인 transfer_avatar_nft 트랜잭션 서명
}

// POST /api/avatar/transfer-nft 응답 타입
export interface TransferNftResponse{
    message: string;            // 성공 메세지 (예: "NFT 전송 완료")
    nft_mint_address: string;   // 전송 완료된 mint address 확인용
}


