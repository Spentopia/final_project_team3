// wallet/dto.rs
//
// 지갑 연동/해제 API의 요청·응답 구조체 모음
//
// DTO(Data Transfer Object): 클라이언트와 서버 사이에서 데이터를 주고받을 떄 쓰는 구조체
// DB 엔티티(model.rs)와 분리해서 관리함.
//
// Deserialize → 요청 JSON을 Rust 구조체로 자동 변환 (요청 DTO에 사용)
// Serialize → Rust 구조체를 응답 JSON으로 자동 변환 (응답 DTO에 사용)
use serde::{Deserialize, Serialize};

// ToSchema → Swagger UI 자동 문서화에 이 구조체의 스펙을 등록
use utoipa::ToSchema;

// LinkWalletRequest - 지갑 연동 요청 DTO
//
// POST /wallet/link의 요청 body 구조체
// 로그인된 유저가 자신의 계정에 지갑을 연결할 때 사용.
//
// ■ nonce를 포함하는 이유
//  "이 지갑이 정말 내 것"임을 증명해야 하므로 서명 검증이 필요함.
//  서명 검증을 하려면 서버가 발급한 nonce가 있어야 함.
//  → 연동 요청 전에 반드시 /auth/wallet/nonce를 먼저 호출해야 함.
#[derive(Debug, Deserialize, ToSchema)]
pub struct LinkWalletRequest {
    // 연동할 Solana 지갑 주소 (Base58 인코딩된 공개키)
    // 예: "7xKXtg2CW87d4UKSGcTFgWX9nP3vZmqR"
    pub wallet_address: String,

    // /auth/wallet/nonce 에서 발급받은 1회용 랜덤 문자열
    // 서버가 저장한 값과 비교해서 요청 위조 여부를 확인함
    pub nonce: String,

    // nonce를 지갑 개인키로 서명한 값 (Base58 인코딩된 64바이트)
    // 이 서명으로 "이 지갑의 주인이 맞다"는 것을 증명함
    pub signature: String,
}

// UnlinkWalletRequest - 지갑 해제 요청 DTO
//
// DELETE /wallet/unlink의 요청 body 구조체
// access token만 탈취된 상황에서 지갑 연동을 끊지 못하도록
// 현재 DB에 연동된 지갑으로 nonce 재서명을 요구한다.
#[derive(Debug, Deserialize, ToSchema)]
pub struct UnlinkWalletRequest {
    pub wallet_address: String,
    pub nonce: String,
    pub signature: String,
}

// LinkWalletResponse - 지갑 연동 성공 응답 DTO
//
// POST /wallet/link 성공 시 반환하는 응답 구조체
#[derive(Debug, Serialize, ToSchema)]
pub struct LinkWalletResponse {
    // 실제로 연동된 지갑 주소 (요청한 wallet_address를 그대로 돌려줌)
    // 클라이언트가 "어떤 지갑에 연동됐는지" 확인할 수 있게 포함
    pub wallet_address: String,

    //처리 결과 메세지 (예: "지갑 연동 완료")
    pub message: String,
}

// UnlinkWalletResponse - 지갑 해제 성공 응답 DTO
//
// DELETE wallet/unlink 성공 시 반환하는 응답 구조체
#[derive(Debug, Serialize, ToSchema)]
pub struct UnlinkWalletResponse {
    // 처리 결과 메세지 (예: "지갑 연동이 해제되었습니다.")
    pub message: String,
}
