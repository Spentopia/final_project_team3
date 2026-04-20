// 타입 파일의 역할:
// - 백엔드 API 응답/요청의 "형태"를 TypeScript에게 알려주는 계약서
// - 이 파일 하나만 보면 아바타 도메인에서 어떤 데이터가 오가는지 파악 가능
// - interface vs type: 확장 가능성이 있을 때 interface 사용 (관례)
//
// GET /api/avatar/items 응답 타입
// 백엔드 DB의 user_items 테이블 + avatar_items 테이블 JOIN 결과
//
export interface UserItemResponse{
    id: string;     // user_items.id - 이 유저가 "소유한" 레코드 UUID
    item_id: string;// avatar_items.id - 아이템 원본 UUID
    name: string;   // 아이템 이름 (예: "별빛 배경")

    // union 타입: 이 4가지 문자열 외엔 들어올 수 없음
    // 백엔드 ENUM과 1:1 매핑되어야 함
    category: "background" | "frame" | "effect" | "motion";

    // 레어리티도 마찬가지로 3가지만 허용
    rarity: "common" | "rare" | "epic";

    image_url: string;      // S3 또는 CDN URL

    // null 허용: DB에서 아직 값이 없는 경우를 표현
    // boolean | null ≠ boolean - 반드시 null 체크 필요
    is_equipped: boolean | null;
    is_nft: boolean | null;     // NFT로 민팅되었는지 여부

    acquired_at: string | null; // ISO 8601 형식 (예: "2024-01-15T09:30:00Z")
                                // Date 객체가 아닌 string - 필요할 때 new Date() 변환
}

// POST /api/avatar/mint-nft 요청 타입
// 흐름: Solana 민팅 완료 → 생성된 mint address를 백엔드 DB에 기록
//
export interface MintNftRequest{
    user_item_id: string;       // 어떤 아이템을 민팅했는지 (user_items.id)
    nft_mint_address: string;   // Solana가 생성한 NFT mint 계정 주소 (32바이트 base58)
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
}

// POST /api/avatar/transfer-nft 응답 타입
export interface TransferNftResponse{
    message: string;            // 성공 메세지 (예: "NFT 전송 완료")
    nft_mint_address: string;   // 전송 완료된 mint address 확인용
}



