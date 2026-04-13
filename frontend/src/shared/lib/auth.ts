// shared/lib/auth.ts
// 기존 localStorage 기반에서 Supabase Auth 연동으로 변경
// Supabase가 세션을 자동 관리하지만,
// 백엔드 API 호출용 토큰은 여기서 꺼내 씀

import {supabase} from "./supabase"


const TOKEN_KEY = "spentopia_access_token";

export const authStorage = {
  // Supabase 세션에서 토큰 가져오기
  // Supabase SDK가 내부적으로 localStorage에 세션을 저장하고 있어서
  // getSession()으로 현재 유효한 토큰을 꺼낼 수 있음
  async getToken(): Promise<string | null> {
    const {data} = await supabase.auth.getSession(); //data 안에는 session정보가 포함됨
    // 만약 supabase 로그인이 활성화 되어있다면 메모리의 최신 토큰 반환
    // supabase 세션에 토큰이 없다면 브라우저의 localstorage에서 TOKEN_KEY라는 이름으로 저장된 값을 찾아 반환
    return data.session?.access_token ?? localStorage.getItem(TOKEN_KEY);
  },

  // 토큰을 localStorage에도 저장 (백엔드 API 호출 시 동기적으로 꺼내 쓰기 위함)
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token);
  },

  

  async clear() {
    await supabase.auth.signOut(); //supabase 서버에 이 세션을 무효화해달라는 요청을 보냄
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem("spentopia_auth");
    sessionStorage.removeItem("spentopia_auth");
  },

  isLoggedIn(): boolean {
    return (
      !!localStorage.getItem(TOKEN_KEY) ||
      !!localStorage.getItem("spentopia_auth")
    );
  },
};