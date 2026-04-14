// shared/lib/auth.ts
//
// 앱 JWT를 브라우저에 저장하고 꺼내는 유틸 파일.
//
// 현재 구조의 핵심:
// - 최종적으로 프론트가 신뢰하는 토큰은 "spentopia_auth" 하나
// - 그 값은 항상 "백엔드 앱 JWT"
//
// 왜 별도 유틸로 빼나?
// - localStorage 접근 로직이 여기 한곳에 모임
// - 나중에 키 이름 바꾸거나 메모리 캐시 정책 바꿀 때 편함
// - 컴포넌트에서 localStorage 코드를 매번 직접 안 써도 됨

const AUTH_STORAGE_KEY = "spentopia_auth";

// 메모리 캐시
//
// 이유:
// - localStorage를 매번 읽는 대신 한 번 메모리에 올려둘 수 있음
// - 다만 "최종 진실"은 localStorage 기준으로 두는 게 안전함
//
// 그래서 ProtectedRoute에서는 localStorage를 우선으로 보고,
// authStorage는 보조적으로만 써도 됨.
let memoryToken: string | null = null;

export const authStorage = {
  // ───────────────────────────────────────────────────────────
  // 토큰 저장
  //
  // 로그인 성공 시 호출
  // localStorage + memory cache 둘 다 갱신
  // ───────────────────────────────────────────────────────────
  setToken(token: string) {
    memoryToken = token;
    localStorage.setItem(AUTH_STORAGE_KEY, token);
  },

  // ───────────────────────────────────────────────────────────
  // 토큰 조회
  //
  // 1) 메모리에 있으면 메모리값 반환
  // 2) 없으면 localStorage에서 읽어서 메모리에 올린 뒤 반환
  //
  // 주의:
  // 앱 구조상 토큰 꼬임이 있었기 때문에,
  // 중요한 인증 판정은 localStorage를 기준으로 보는 게 더 안전함.
  // 그래도 다른 화면/유틸에서 빠르게 꺼내 쓸 수 있게 제공함.
  // ───────────────────────────────────────────────────────────
  getToken(): string | null {
    if (memoryToken) return memoryToken;

    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    memoryToken = stored;
    return stored;
  },

  // ───────────────────────────────────────────────────────────
  // 로그인 상태 여부
  //
  // 단순히 토큰 존재 여부만 확인
  // 실제 유효성 검사는 /me 또는 middleware에서 이루어짐
  // ───────────────────────────────────────────────────────────
  isLoggedIn(): boolean {
    return !!localStorage.getItem(AUTH_STORAGE_KEY);
  },

  // ───────────────────────────────────────────────────────────
  // 토큰 삭제
  //
  // 로그아웃 또는 인증 꼬임 발생 시 호출
  // ───────────────────────────────────────────────────────────
  clear() {
    memoryToken = null;
    localStorage.removeItem(AUTH_STORAGE_KEY);
  },
};