// auth/model.rs
// DB에서 읽어오거나 저장할 때 쓰는 구조체들을 모아둔 파일임.
// DTO(요청/응답용)는 dto.rs에 있고, 여기는 순수하게 DB 엔티티만 있음.

use serde::{Deserialize, Serialize};
use uuid::Uuid;

// public.users 테이블에 대응하는 구조체
// Supabase auth.users의 id를 PK로 참조하는 구조
#[derive(Debug,Clone,Serialize,Deserialize)]
pub struct User {
    // auth.users의 id와 동일한 값
    // 로그인하면 Supabase가 auth.users에 row를 만들고
    // Trigger가 이 id를 그대로 public.users에도 넣어줌
    pub id: Uuid,

    // 사용자가 설정한 닉네임
    pub nickname: String,

    pub profile_image: Option<String>,

    // 앎림 수신 여부
    pub notification_on: bool,
}