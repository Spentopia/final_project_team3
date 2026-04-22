// state.rs
// Axum 서버 전체에서 공유하는 상태를 담는 파일임.
// 핸들러마다 Config나 HTTP 클라이언트를 새로 만들면 낭비라서
// 여기서 한 번만 만들고 모든 핸들러에 전달함.

// 변경 핵심:
// - nonce_store 타입 변경
//   기존: DashMap<String, String>
//   변경: DashMap<String, NonceEntry>
//
// 이유:
// - nonce에 TTL(예: 5분)을 부여하려면
//   발급 시각/만료 시각 정보가 같이 필요함.

use crate::config::Config;
use dashmap::DashMap;
use reqwest::Client;
use std::sync::Arc;
use std::time::{Duration, SystemTime};
use uuid::Uuid;

// nonce + 만료 시각을 묶어서 저장하는 구조체
#[derive(Clone)]
pub struct NonceEntry {
    pub nonce: String,
    pub expires_at: SystemTime,
}

// ─────────────────────────────────────────────────────────────
// handoff token 임시 저장 구조체
//
// 웹에서 유니티로 로그인 상태를 안전하게 넘기기 위한 1회용 교환권.
//
// 흐름:
// 1) 웹에서 "게임 시작" → 백엔드가 handoff token 발급 → 메모리 저장
// 2) 웹이 postMessage로 유니티에 handoff token 전달
// 3) 유니티가 /auth/handoff/exchange 호출 → 백엔드가 검증
// 4) 검증 성공 → handoff 즉시 삭제 → 유니티용 access+refresh 발급
//
// - key는 handoff token 원문의 SHA-256 해시를 사용
// - 그래서 value 안에 token_hash를 또 들고 있을 필요가 없음
// - used 플래그도 필요 없음
//   -> 검증이 끝난 뒤 DashMap에서 remove() 하면서 1회용 보장
#[derive(Clone)]
pub struct HandoffEntry {
    // 이 handoff가 어떤 유저를 위한 것인지
    pub user_id: Uuid,

    // 대상 서비스 (현재는 "unity"만 사용)
    // 나중에 다른 서비스로 확장 가능
    pub target_service: String,

    // handoff token 만료 시각
    // 생성 후 30초만 유효
    pub expires_at: SystemTime,
}

#[derive(Clone)]
pub struct AppState {
    pub config: Config,

    //Supabase Admin API나 소셜 로그인 공급자 API 호출시 사용
    //reqwest::Client는 내부적으로 커넥션 풀을 관리하기 때문에 하나를 공유하는게 좋음
    pub http_client: Client,

    // 지갑주소 -> NonceEntry(nonce + 만료시각) 임시 저장소
    // Arc로 감쌈 → AppState clone 시에도 같은 DashMap을 공유
    pub nonce_store: Arc<DashMap<String, NonceEntry>>,

    // handoff token hash -> HandoffEntry 임시 저장소
    // 키: handoff token의 SHA-256 해시
    // 값: HandoffEntry (user_id, 만료시각, 사용 여부 등)
    pub handoff_store: Arc<DashMap<String, HandoffEntry>>,
}

impl AppState {
    pub fn new(config: Config) -> Self {
        Self {
            config,
            http_client: Client::new(),
            nonce_store: Arc::new(DashMap::new()),
            handoff_store: Arc::new(DashMap::new()),
        }
    }
}
