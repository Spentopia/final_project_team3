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

// ── 카카오 소셜 로그인 ──────────────────────────────────────
// 구글과 동일한 흐름. 카카오 OAuth 페이지로 리다이렉트.
// 주의: 카카오는 이메일을 안 줄 수도 있음 (카카오 개발자센터 설정에 따라)
// → 유저 식별은 이메일이 아닌 auth.users.id(JWT sub)로 함
export const signInWithKakao = async () => {
  const { error } = await supabase.auth.signInWithOAuth({
    provider: "kakao",
    options: {
      redirectTo: window.location.origin,
    },
  });

  if (error) throw new Error(error.message);
};

// ── 비밀번호 찾기 (재설정 이메일 발송) ──────────────────────
// Supabase가 비밀번호 재설정 링크를 이메일로 보내줌
// 흐름: 이메일 입력 → Supabase가 메일 발송 → 유저가 링크 클릭
// → redirectTo URL(/reset-password)로 이동 → 새 비밀번호 입력
//
// Supabase가 링크에 세션 정보를 포함시키기 때문에
// /reset-password 페이지에서 updateUser()로 바로 비밀번호 변경 가능
export const resetPassword = async (email: string) => {
  const {error} = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: '${window.location.origin}/reset-password',
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

  const res = await fetch('${BACKEND_URL}/auth/find-email', {
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