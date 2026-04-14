// state.rs
// Axum 서버 전체에서 공유하는 상태를 담는 파일임.
// 핸들러마다 Config나 HTTP 클라이언트를 새로 만들면 낭비라서
// 여기서 한 번만 만들고 모든 핸들러에 전달함.

use crate::config::Config;
use reqwest::Client;
use dashmap::DashMap;


#[derive(Clone)]
pub struct AppState {
    pub config: Config,

    //Supabase Admin API나 소셜 로그인 공급자 API 호출시 사용
    //reqwest::Client는 내부적으로 커넥션 풀을 관리하기 때문에 하나를 공유하는게 좋음
    pub http_client: Client,

    // 지갑주소 -> nonce 임시 저장소
    pub nonce_store: DashMap<String, String>,

}

impl AppState {
    pub fn new(config: Config) -> Self {
        Self{
            config,
            http_client: Client::new(),
            nonce_store: DashMap::new(),
            
        }
    }
}