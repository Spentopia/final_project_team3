// src/shared/lib/auth.ts
//
// 웹 프론트에서 "access token만" 메모리에 저장하는 유틸
//
// 최종 구조:
// - access token  -> 메모리 저장
// - refresh token -> HttpOnly 쿠키 (브라우저가 자동 관리)
//
// 왜 localStorage 안 쓰냐?
// - XSS 발생 시 localStorage 토큰 탈취 위험이 큼
//
// 왜 메모리만 쓰냐?
// - 페이지 새로고침 시 access는 사라져도 괜찮음
// - 앱 시작 시 /auth/refresh를 호출해서 새 access를 다시 받으면 됨

let accessToken: string | null = null;

export const authStorage = {
  // 로그인/재발급 성공 시 access token 메모리에 저장
  setToken(token: string) {
    accessToken = token;
  },

  // 현재 메모리에 있는 access token 반환
  getToken(): string | null {
    return accessToken;
  },

  // 로그아웃/refresh 실패 시 메모리 비우기
  clear() {
    accessToken = null;
  },
};