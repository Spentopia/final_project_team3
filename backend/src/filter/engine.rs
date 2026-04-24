// src/filter/engine.rs
//
// 욕설/비속어 필터링 + 닉네임 유효성 검사 로직
//
// 구조:
// - wordlist.txt를 컴파일 타임에 바이너리에 포함 (include_str!)
// - 앱 시작 시 AhoCorasick 패턴 한 번만 빌드 (Lazy)
//
// 공개 함수:
// - validate_nickname(): 닉네임 전용 (길이 + 금칙어 + 공백 한번에 검사)
// - check()           : 게시글 등 일반 텍스트 금칙어 포함 여부
// - mask()            : 금칙어를 *로 치환 (게시글 마스킹 선택 시)
//
// 닉네임 정책:
// - 최소 2자, 최대 8자
// - 앞뒤 공백 불허 (trim 후 길이 체크)
// - 금칙어 포함 불허
//
// 정규화 전략 (check/mask):
// - 공백 제거: "시 발" → "시발"
// - 소문자 변환: "FUCK" → "fuck"
// - 초성 변형(ㅅㅂ 등)은 wordlist.txt에 직접 등록해서 처리

use aho_corasick::{AhoCorasick, AhoCorasickBuilder};
use once_cell::sync::Lazy;

static AC: Lazy<AhoCorasick> = Lazy::new(|| {
    let words: Vec<String> = include_str!("wordlist.txt")
        .lines()
        .map(str::trim)
        .filter(|l| !l.is_empty() && !l.starts_with('#'))
        .map(|l| l.to_lowercase()) // 패턴도 소문자로 정규화 — wordlist 대소문자 무관
        .collect();

    AhoCorasick::new(&words).expect("AhoCorasick 필터 초기화 실패")
});

// 닉네임 검증
//
// Ok(())         → 사용 가능
// Err(&'static str) → 사용 불가 + 사유 메시지 (프론트에 그대로 내려줄 수 있음)
//
// 검사 순서:
// 1) 앞뒤 공백
// 2) 최소 길이 (2자)
// 3) 최대 길이 (8자)
// 4) 금칙어
pub fn validate_nickname(nickname: &str) -> Result<(), &'static str> {
    let trimmed = nickname.trim();

    // 1) 앞뒤 공백 체크
    if trimmed != nickname {
        return Err("닉네임 앞뒤에 공백을 사용할 수 없습니다.");
    }

    // 2~3) 길이 체크
    // chars().count() 사용 - 한글은 바이트가 3이므로 len() 쓰면 안됨
    let char_count = trimmed.chars().count();
    if char_count < 2 {
        return Err("닉네임은 최소 2자 이상이어야 합니다.");
    }
    if char_count > 8 {
        return Err("닉네임은 최대 8자까지 입니다.");
    }

    // 4) 금칙어 체크
    // 공백 제거 + 소문자 정규화 후 검사
    let normalized = trimmed.replace(' ', "").to_lowercase();
    if AC.find(normalized.as_str()).is_some() {
        return Err("사용할 수 없는 닉네임입니다.");
    }

    Ok(())
}

// 금칙어 포함 여부 확인
// 반환값 : true -> 금칙어 없음, false -> 금칙어 있음
// 사용처: 닉네임 설정/변경 시 차단, 게시글 작성 시 차단
pub fn check(text: &str) -> bool {
    let normalized = text.replace(' ', "").to_lowercase();
    AC.find(normalized.as_str()).is_none()
}
