// domains/auth/api/auth.ts
// Supabase Auth 연동
//
// DDL 참고 포인트:
// - auth.users INSERT → handle_new_user() 트리거가 public.users 자동 생성
// - 트리거는 id, email, login_provider만 넣어줌
// - nickname, phone은 회원가입 후 별도 UPDATE 필요
// - nickname + phone 모두 입력 시 profile_completed = true (트리거 자동 판단)

import {supabase} from "@/shared/lib/supabase"
import type { LoginRequest, LoginResponse,SignUpRequest } from "@/domains/auth/model/types";

// 자체 로그인
export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  const {data, error} = await supabase.auth.signInWithPassword({
    email: payload.email,
    password: payload.password
  });

  if (error) throw new Error(error.message);

  //public.users에서 닉네임 가져오기
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

// ── 프로필 완성 (회원가입 Step 2, 3) ────────────────────────
// 닉네임 + 전화번호를 public.users에 UPDATE
// DDL의 handle_profile_completed 트리거가 자동으로
// profile_completed = true로 바꿔줌
export const completeProfile = async (params: {
  nickname: string,
  phone: string,
  avatar?: number,
  profileImage?: string;
}) => {
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
export const signInWithKakao = async () => {
  const { error } = await supabase.auth.signInWithOAuth({
    provider: "kakao",
    options: {
      redirectTo: window.location.origin,
    },
  });

  if (error) throw new Error(error.message);
};

// ── 로그아웃 ────────────────────────────────────────────────
export const signOut = async () => {
  await supabase.auth.signOut();
};