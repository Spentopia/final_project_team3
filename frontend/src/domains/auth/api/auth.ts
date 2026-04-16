// domains/auth/api/auth.ts

import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { apiClient } from "@/shared/api/client";
import { stripPhone } from "@/shared/lib/phone";
import type {
  LoginRequest,
  LoginResponse,
  SignUpRequest,
} from "@/domains/auth/model/types";

// 기존 인증 상태를 정리
const clearAllAuthState = async () => {
  // 앱 access token 메모리 삭제
  authStorage.clear();

  // Supabase session 정리
  try {
    await supabase.auth.signOut();
  } catch (e) {
    console.warn("Supabase 세션 정리 실패:", e);
  }
};

// Supabase access_token -> 백엔드 앱 access token 교환
const exchangeSupabaseToken = async (accessToken: string) => {
  const res = await apiClient.post(
    "/auth/exchange",
    {
      access_token: accessToken,
    },
    {
      headers: {
        "X-Client-Type": "web",
      },
    }
  );

  return res.data;
};

// 이메일 로그인
export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  await clearAllAuthState();

  const { data, error } = await supabase.auth.signInWithPassword({
    email: payload.email,
    password: payload.password,
  });

  if (error) {
    throw new Error(error.message);
  }

  if (!data.session?.access_token) {
    throw new Error("Supabase 세션이 없습니다");
  }

  const exchanged = await exchangeSupabaseToken(data.session.access_token);

  return {
    accessToken: exchanged.access_token,
    isNewUser: exchanged.is_new_user ?? false,
  };
};

// 회원가입
export const signUp = async (payload: SignUpRequest): Promise<LoginResponse> => {
  await clearAllAuthState();

  // ── 1) 이메일 중복 확인 ────────────────────────────────────
  // 백엔드의 /auth/check-email은 public.users에서 이메일 존재 여부를 확인
  // 200 + { exists: true } → 이미 가입된 이메일
  // 404 → 가입 가능한 이메일
  try {
    const checkRes = await apiClient.post("/auth/check-email", {
      email: payload.email,
    });
 
    // 200이 왔다는 건 이메일이 존재한다는 뜻
    if (checkRes.data?.exists) {
      throw new Error("이미 가입된 이메일입니다.");
    }
  } catch (error: any) {
    // 404면 이메일이 없다는 뜻 → 정상, 회원가입 진행
    // 그 외 에러(이미 가입된 이메일 포함)는 그대로 throw
    if (error.response?.status !== 404) {
      throw error;
    }
  }

  const { data, error } = await supabase.auth.signUp({
    email: payload.email,
    password: payload.password,
    options: {
      emailRedirectTo: `${window.location.origin}/email-confirmed`,
    },
  });

  if (error) {
    throw new Error(error.message);
  }

  // 이메일 인증이 필요한 경우
  if (!data.session?.access_token) {
    return {
      accessToken: "",
      isNewUser: true,
    };
  }

  const exchanged = await exchangeSupabaseToken(data.session.access_token);

  return {
    accessToken: exchanged.access_token,
    isNewUser: exchanged.is_new_user ?? true,
  };
};

// 구글 로그인
export const signInWithGoogle = async () => {
  await clearAllAuthState();

  const { error } = await supabase.auth.signInWithOAuth({
    provider: "google",
    options: {
      redirectTo: window.location.origin,
      queryParams: {
        prompt: "select_account",
      },
    },
  });

  if (error) {
    throw new Error(error.message);
  }
};

// 카카오 로그인 시작
export const redirectToKakao = async () => {
  await clearAllAuthState();

  const clientId = import.meta.env.VITE_KAKAO_REST_API_KEY;
  const redirectUri = import.meta.env.VITE_KAKAO_REDIRECT_URI;

  const kakaoAuthUrl =
    `https://kauth.kakao.com/oauth/authorize` +
    `?client_id=${clientId}` +
    `&redirect_uri=${encodeURIComponent(redirectUri)}` +
    `&response_type=code` +
    `&scope=profile_nickname,profile_image` +
    `&prompt=select_account`;

  window.location.href = kakaoAuthUrl;
};

// 카카오 로그인 코드 -> 백엔드 로그인
export const loginWithKakaocode = async (code: string) => {
  const res = await apiClient.post(
    "/auth/kakao/login",
    { code },
    {
      headers: {
        "X-Client-Type": "web",
      },
    }
  );

  return res.data;
};

// 프로필 완성
export const completeProfile = async (params: {
  nickname: string;
  phone: string;
  profileImage?: string;
}) => {
  const res = await apiClient.patch("/profile/complete", {
    nickname: params.nickname,
    phone: params.phone,
    profile_image: params.profileImage ?? null,
  });

  return res.data;
};

// 비밀번호 재설정 메일 발송
export const resetPassword = async (email: string) => {
  const checkRes = await apiClient.post("/auth/check-email", { email });

  if (checkRes.status !== 200) {
    throw new Error("해당 이메일로 가입된 계정이 없습니다");
  }

  const { error } = await supabase.auth.resetPasswordForEmail(email, {
    redirectTo: `${window.location.origin}/reset-password`,
  });

  if (error) {
    throw new Error(error.message);
  }
};

// 새 비밀번호 설정
export const updatePassword = async (newPassword: string) => {
  const { error } = await supabase.auth.updateUser({
    password: newPassword,
  });

  if (error) {
    throw new Error(error.message);
  }
};

// 전화번호로 이메일 찾기
//
// 유저가 "010-1234-5678" 형식으로 입력해도
// DB에는 "01012345678"로 저장되어 있으므로
// stripPhone으로 숫자만 추출해서 검색
export const findEmailByPhone = async (phone: string): Promise<string> => {
  const res = await apiClient.post("/auth/find-email", {
    phone: stripPhone(phone),
  });
  return res.data.masked_email;
};

// 로그아웃
export const signOut = async () => {
  try {
    await apiClient.post(
      "/auth/logout",
      {},
      {
        headers: {
          "X-Client-Type": "web",
        },
      }
    );
  } finally {
    await clearAllAuthState();
  }
};