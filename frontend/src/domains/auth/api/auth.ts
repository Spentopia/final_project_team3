// domains/auth/api/auth.ts
// ─────────────────────────────────────────────────────────────
// Supabase Auth API를 호출하는 함수 모음
// 스프링부트로 치면 AuthService 클래스에 해당
//
// DDL 참고 포인트:
// - auth.users에 row가 INSERT되면
//   handle_new_user() 트리거가 자동으로 실행되어
//   public.users + user_settings + streaks 테이블에 row를 생성함
// - 트리거는 id, email, login_provider만 넣어줌
// - nickname, phone은 회원가입 후 completeProfile()로 별도 UPDATE 필요
// - nickname + phone 모두 입력되면
//   handle_profile_completed() 트리거가 profile_completed = true로 자동 변경
//
// 로그인 방식별 처리:
// - 자체 이메일 로그인: signInWithPassword() → Supabase가 JWT 발급
// - 소셜 로그인 (구글/카카오): signInWithOAuth() → 외부 페이지로 리다이렉트 → JWT 발급
// - 지갑 로그인: 백엔드 API 호출 (여기가 아님, WalletLoginButton에서 처리)
//
// 모든 로그인 방식의 결과는 동일한 Supabase JWT.
// 이 JWT를 백엔드 API 호출 시 Authorization 헤더에 넣으면
// 백엔드 middleware.rs가 검증함.

import {supabase} from "@/shared/lib/supabase"
import type { LoginRequest, LoginResponse,SignUpRequest } from "@/domains/auth/model/types";

// ── 자체 로그인 ─────────────────────────────────────────────
// 이메일 + 비밀번호로 로그인
// Supabase SDK가 auth.users에서 확인 → 성공 시 JWT(session) 반환
export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const {data, error} = await supabase.auth.signInWithPassword({
    email: payload.email,
    password: payload.password
  });

  if (error) throw new Error(error.message);

  // public.users에서 닉네임과 프로필 완성 여부 가져오기
  // .from("users") → public.users 테이블
  // .select("nickname, profile_completed") → 이 두 컬럼만 조회
  // .eq("id", data.user.id) → WHERE id = 로그인한 유저의 UUID
  // .single() → 결과가 1개임을 보장 (0개 또는 2개 이상이면 에러)
  const {data: profile} = await supabase
  .from("users")
  .select("nickname, profile_completed")
  .eq("id", data.user.id)
  .single();

  return {
    accessToken: data.session.access_token,
    refreshToken: data.session.refresh_token,
    user: {
      id: data.user.id,
      email: data.user.email ??"",
      nickname: profile?.nickname ?? undefined,
      profileCompleted: profile?.profile_completed ?? false,
    },
  };
};

// ── 자체 회원가입 ───────────────────────────────────────────
// Step 1에서 이메일/비밀번호로 auth.users에 가입
// → handle_new_user 트리거가 public.users 자동 생성
// Step 2에서 닉네임/전화번호 입력 시 completeProfile() 호출
export const signUp = async (payload: SignUpRequest): Promise<LoginResponse> => {
  const { data, error } = await supabase.auth.signUp({
    email: payload.email,
    password: payload.password,
  });

  if (error) throw new Error(error.message);

  // 이메일 인증이 필요한 경우 session이 null
  if (!data.session) {
    return {
      accessToken: "",
      refreshToken: "",
      user: {
        id: data.user?.id ?? "",
        email: data.user?.email ?? "",
        profileCompleted: false,
      },
    };
  }

  // 이메일 인증 비활성화 시 바로 session이 생성
  return {
    accessToken: data.session.access_token,
    refreshToken: data.session.refresh_token,
    user: {
      id: data.user?.id ?? "",
      email: data.user?.email ?? "",
      profileCompleted: false,
    },
  };
};


// ── 프로필 완성 (회원가입 Step 2~3 / 소셜 첫 가입 후) ───────
// public.users 테이블에 nickname, phone을 UPDATE
// → DDL의 handle_profile_completed() 트리거가 자동으로
//   profile_completed = true로 바꿔줌
//
// 자체 회원가입: SignupPage Step 2~3에서 호출
// 소셜 첫 가입: CompleteProfilePage에서 호출
export const completeProfile = async (params: {
  nickname: string,
  phone: string,
  avatar?: number,
  profileImage?: string;
}) => {
  // 현재 로그인 한 유저 정보 가져오기
  const {data: {user}} = await supabase.auth.getUser();
  if (!user) throw new Error("로그인 상태가 아닙니다");

  const {error} = await supabase
  .from("users")
  .update({
    nickname: params.nickname,
    phone: params.phone,
    profileImage: params.profileImage ?? null,
  })
  .eq("id", user.id);

  if (error) throw new Error(error.message);

};

// ── 구글 소셜 로그인 ────────────────────────────────────────
// Supabase SDK가 구글 OAuth 페이지로 리다이렉트시킴
// 흐름: 버튼 클릭 → 구글 로그인 페이지 → 인증 완료 → redirectTo URL로 돌아옴
// → Supabase가 자동으로 auth.users에 row 생성 (첫 가입 시)
// → handle_new_user() 트리거 실행 → public.users 생성
// → 앱으로 돌아오면 ProtectedRoute가 profile_completed 체크
// → 미완성이면 /complete-profile로 리다이렉트
export const signInWithGoogle = async () => {
  const {error} = await supabase.auth.signInWithOAuth({
    provider: "google",
    options: {
      redirectTo: window.location.origin,
    },
  });

  if (error) throw new Error(error.message);

};


// ── 카카오 로그인 (자체 구현) ────────────────────────────────
// Supabase OAuth를 안 쓰고 직접 카카오 OAuth를 처리하는 이유:
// Supabase의 카카오 provider가 account_email 스코프를 자동으로 요청하는데
// 비즈앱이 아니면 account_email 권한이 없어서 에러가 남.
// 그래서 프론트에서 직접 카카오 인가 URL을 만들어서
// profile_nickname, profile_image만 요청하는 방식으로 우회.
//
// 전체 흐름:
// 1) redirectToKakao() → 카카오 로그인 페이지로 이동
// 2) 유저가 카카오에서 로그인 완료
// 3) 카카오가 redirect_uri(?code=xxx)로 인가 코드를 보내줌
// 4) KakaoCallbackPage에서 loginWithKakaoCode(code) 호출
// 5) 백엔드가 인가 코드 → 카카오 token 교환 → 유저 정보 조회 → JWT 발급
// 6) 프론트가 JWT 저장 → 메인 또는 프로필 완성 페이지로 이동

// [1단계] 카카오 인가 페이지로 리다이렉트
// 이 함수가 호출되면 현재 페이지를 떠나서 카카오 로그인 페이지로 이동함
// 유저가 로그인하면 카카오가 redirect_uri로 인가 코드를 붙여서 돌려보냄
export const redirectToKakao = () => {

  // .env에서 카카오 REST API 키와 콜백 URL을 가져옴
  // REST API Key는 공개 키라서 프론트에 노출돼도 안전함
  // (Client Secret과는 다름 — Secret은 백엔드에서만 사용)
  const clientId = import.meta.env.VITE_KAKAO_REST_API_KEY;
  const redirectUri = import.meta.env.VITE_KAKAO_REDIRECT_URI;

  // 카카오 OAuth 인가 요청 URL
  // 유저가 카카오 로그인 페이지에서 로그인하면
  // redirectUri로 인가 코드(code)를 보내줌
  const kakaoAuthUrl =
    `https://kauth.kakao.com/oauth/authorize` +
    `?client_id=${clientId}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&response_type=code` +
    `&scope=profile_nickname,profile_image`;

  //카카오 로그인 페이지로 이동
  window.location.href = kakaoAuthUrl;
};

// [2단계] 카카오 인가 코드를 백엔드로 전송
// KakaoCallbackPage에서 호출됨
// 카카오가 콜백 URL에 ?code=xxx 형태로 인가 코드를 보내주면
// 이 코드를 백엔드 /auth/kakao/login으로 POST 전송
//
// 백엔드가 하는 일:
// 1) 인가 코드 → 카카오 토큰 서버에서 access_token 교환
// 2) access_token → 카카오 유저 정보 API(/v2/user/me) 호출
// 3) 카카오 유저 ID로 Supabase에서 유저 찾거나 새로 생성
// 4) Supabase Admin API로 JWT 발급해서 반환
export const loginWithKakaocode = async (code: string) => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/auth/kakao/login`, {
    method: "POST",
    headers: {"Content-Type" : "application/json"},
    body: JSON.stringify({code}),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "카카오 로그인 실패");
  }

  return await res.json();

}



// ── 비밀번호 찾기 (재설정 이메일 발송) ──────────────────────
// Supabase가 비밀번호 재설정 링크를 이메일로 보내줌
// 흐름: 이메일 입력 → 백엔드에서 해당 이메일이 DB에 있는지 먼저 확인->
//  있으면 Supabase가 재설정 링크 이메일 발송 → 유저가 링크 클릭
// → redirectTo URL(/reset-password)로 이동 → 새 비밀번호 입력
//
// Supabase가 링크에 세션 정보를 포함시키기 때문에
// /reset-password 페이지에서 updateUser()로 바로 비밀번호 변경 가능
export const resetPassword = async (email: string) => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  //백엔드에서 이메일 존재 여부 확인
  const checkRes = await fetch(`${BACKEND_URL}/auth/check-email`, {
    method: "POST",
    headers: {"Content-Type" : "application/json"},
    body: JSON.stringify({email}),
  });

  if (!checkRes.ok) {
    throw new Error("해당 이메일로 가입된 계정이 없습니다");
  }

 // DB에 있으면 Supabase에 재설정 이메일 발송 요청
  // redirectTo: 유저가 이메일 링크 클릭 시 이동할 URL
  // Supabase가 이 URL에 세션 토큰을 포함시켜서 보내줌
  // → /reset-password에 도착하면 이미 임시 세션이 활성화되어 있음
  // → updateUser()로 바로 비밀번호 변경 가능
  //
  // ⚠️ 이 URL을 Supabase 대시보드 → Authentication → URL Configuration
  //    → Redirect URLs에 등록해야 함. 안 하면 Supabase가 리다이렉트 차단
  const {error} = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: `${window.location.origin}/reset-password`,
  });

  if (error) throw new Error(error.message);

};


// ── 새 비밀번호 설정 ────────────────────────────────────────
// 비밀번호 재설정 링크 클릭 후 ResetPasswordPage에서 호출
// Supabase가 링크 클릭 시 자동으로 세션을 생성해주기 때문에
// 별도 인증 없이 updateUser()로 바로 비밀번호 변경 가능
export const updatePassword = async (newPassword: string) => {
  const {error} = await supabase.auth.updateUser({
    password: newPassword,
  });

  if (error) throw new Error(error.message);

};

// ── 이메일 찾기 (전화번호로 조회) ───────────────────────────
// 전화번호로 백엔드 API를 호출해서 해쉬 처리된 이메일을 받음
// 예: test@gmail.com → te***@gmail.com
//
// 왜 백엔드를 거치나?
// 이메일 찾기는 로그인 전 상태에서 호출됨.
// 로그인이 안 되어있으면 Supabase RLS가 auth.uid()를 모르므로
// 프론트에서 public.users를 조회해도 아무 결과가 안 나옴.
// 백엔드는 service_role 키로 RLS를 우회할 수 있어서
// 전화번호로 조회 → 이메일을 마스킹해서 안전하게 반환함.
export const findEmailByPhone = async (phone: string): Promise<string> => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/auth/find-email`, {
    method: "POST",
    headers: {"Content-Type" : "application/json"},
    body: JSON.stringify({phone}),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "이메일 찾기 실패");
  }

  const data = await res.json();
  return data.masked_email;

}



// ── 로그아웃 ────────────────────────────────────────────────
// Supabase 세션 종료
// 실제 localStorage 정리는 authStorage.clear()에서 함
// (LoginPage에서 signOut() 호출 후 authStorage.clear()도 같이 호출)
export const signOut = async () => {
  await supabase.auth.signOut();
};