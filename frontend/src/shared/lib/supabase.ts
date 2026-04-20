// shared/lib/supabase.ts
//
// Supabase client 인스턴스를 만드는 파일.
//
// 현재 구조에서 Supabase 역할:
// - 이메일 로그인 1차 인증
// - 회원가입
// - 구글 OAuth
// - 비밀번호 재설정
//
// 하지만 중요한 점:
// "보호 API 호출용 토큰"은 이제 Supabase 토큰이 아니라 백엔드 앱 JWT다.
//
// 즉 Supabase client는 인증 입구/유틸 역할만 하고,
// 앱 내부 보호 라우팅은 spentopia_auth(앱 JWT) 기준으로 동작한다.

import { createClient } from "@supabase/supabase-js";

// 환경변수 읽기
const supabaseUrl = import.meta.env.VITE_SUPABASE_URL;
const supabaseAnonKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY;

// 개발 중 실수 방지:
// 환경변수가 없으면 앱 시작 시점에 바로 에러를 터뜨려서
// "왜 로그인 안 되지?" 같은 애매한 상태를 막음
if (!supabaseUrl) {
  throw new Error("VITE_SUPABASE_URL 환경변수가 없습니다");
}

if (!supabaseAnonKey) {
  throw new Error("VITE_SUPABASE_PUBLISHABLE_KEY 환경변수가 없습니다");
}

// Supabase client 생성
//
// auth 설정 설명:
// - persistSession: true
//   브라우저에 Supabase session을 저장
//   구글 로그인 후 돌아왔을 때 session을 한 번 읽어 /auth/exchange 할 수 있게 도와줌
//
// - autoRefreshToken: true
//   Supabase session 자체를 유지하는 옵션
//   다만 앱의 최종 보호 API는 Supabase 토큰을 직접 쓰지 않음
//
// - detectSessionInUrl: true
//   OAuth 리다이렉트 후 URL에서 session 정보를 감지
//   구글 로그인 직후 ProtectedRoute가 session을 읽을 수 있게 함
export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    flowType: "pkce",
  },
});
