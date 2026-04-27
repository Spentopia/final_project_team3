// 마켓플레이스 도메인 타입 파일
// 아바타 아이템을 SPT 토큰으로 사고파는 온체인 마켓 API 계약
//
// POST /api/market/listings 요청 타입 - 판매 등록
export interface CreateListingRequest{
    item_id: string;        // user_items.id - NFT 민팅된 아이템만 판매 가능
    price_spt: number;      // 판매가. SPT 토큰 정수 단위 (소수점 없음)
}

// POST /api/market/listings 응답 타입
// 등록된 리스팅의 전체 정보를 반환 - 화면에 바로 카드로 표시하기 위함
export interface ListingResponse{
    id: string;                             // listings.id (UUID) - 구매 시 이 id를 사용
    seller_id: string;                      // 판매자 user UUIDD
    seller_nickname: string | null;         // 판매자 닉네임 (없으면 null)
    item_id: string;                        // 판매 중인 아이템 UUID
    item_name: string;                      // 아이템 이름 (JOIN된 값)
    item_image_url: string;                 // 아이템 이미지 URL
    item_category: "background" | "frame" | "effect" | "motion";
    item_rarity: "common" | "rare" | "epic";
    price_spt: number;                      // 등록된 판매가
    status: string | null;                  // "active" | "sold" 등 - 현재는 "active"만
    listed_at: string | null;               // 등록 시각 ISO 8601
}

// PATCH /api/market/listings/{id}/escrow 요청 타입
// 흐름: Solana에서 list_nft 실행 → 생성된 escrow PDA 주소를 DB에 저장
// PDA(Program Derived Address): Solana 프로그램이 소유하는 결정론적 주소
export interface UpdateEscrowRequest{
    escrow_address: string; // Solana escrow PDA 주소 (base58)
    tx_signature: string;   // list_nft 온체인 트랜잭션 서명
}

// PATCH /api/market/listings/{id}/escrow 응답 타입
export interface UpdateEscrowResponse{
    message: string;        // "escrow 주소가 저장되었습니다."
}

// POST /api/market/purchase 요청 타입 - 아이템 구매
// tx_signature 필수: 온체인 트랜잭션이 완료된 후에만 DB 기록 허용
// 백엔드가 체인에서 tx를 검증하는 구조 (오프체인 신뢰 최소화)
export interface PurchaseRequest{
    listing_id: string;         // 어떤 리스팅을 구매하는지
    tx_signature: string;       // Solana 트랜잭션 서명 - 없으면 400 에러
}

// POST /api/market/purchase 응답 타입
export interface TransactionResponse{
    id: string;                     // transactions.id (UUID)
    listing_id: string;             // 어떤 리스팅인지
    buyer_id: string;               // 구매자 UUID
    price: number;                  // 실제 결제 금액 (SPT)
    fee: number;                    // 온체인 platform_config.fee_rate 기준 수수료
    tx_signature: string | null;    // 저장된 Solana tx 서명
    transacted_at: string | null;   // 거래 시각 ISO 8601
}

