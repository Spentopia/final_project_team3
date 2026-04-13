// shared/lib/auth.ts
// ─────────────────────────────────────────────────────────────
// 토큰 저장/삭제/확인 유틸리티
//
// 역할: localStorage에 JWT 토큰을 저장하고 관리하는 것
// api/auth.ts와의 차이:
//   api/auth.ts → Supabase에 로그인/회원가입 요청을 보내는 함수들
//   lib/auth.ts → 받은 토큰을 localStorage에 저장/관리하는 유틸
//
// ProtectedRoute에서 isLoggedIn()을 동기적으로 호출하기 때문에
// async가 아닌 동기 함수로 만들어야 함.
// (async면 ProtectedRoute가 결과를 기다리지 않고 바로 리다이렉트해버림

import {supabase} from "./supabase"


const TOKEN_KEY = "spentopia_access_token";

export const authStorage = {
  // ── 토큰 저장 ─────────────────────────────────────────────
  // 로그인 성공 후 Supabase에서 받은 access_token을 localStorage에 저장
  // 이후 백엔드 API 호출할 때 이 토큰을 Authorization 헤더에 넣어서 보냄
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  },

  // ── 토큰 가져오기 ─────────────────────────────────────────
  // 백엔드 API 호출 시 Authorization: Bearer {토큰} 에 넣을 값
  getToken() {
    return localStorage.getItem(TOKEN_KEY);
  },

  // ── 로그아웃 시 정리 ──────────────────────────────────────
  // Supabase 세션도 종료하고, localStorage의 토큰도 삭제
  // 기존 앱 호환용 키(spentopia_auth)도 같이 삭제
  async clear() {
    await supabase.auth.signOut();
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("spentopia_auth");
    sessionStorage.removeItem("spentopia_auth");
  },

  // ── 로그인 상태 확인 (동기) ───────────────────────────────
  // ProtectedRoute에서 호출함.
  // localStorage에 토큰이 있으면 로그인 상태로 판단.
  // 기존 앱 호환용 키도 같이 체크함.
  isLoggedIn(): boolean {
    return (
      !!localStorage.getItem(TOKEN_KEY) ||
      !!localStorage.getItem("spentopia_auth")
    );
  },
};