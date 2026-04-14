// domains/auth/api/auth.ts
//
// 프론트 인증 관련 API 모음.
//
// ── 최종 구조 ───────────────────────────────────────────────
// 1) 이메일 로그인
//    프론트가 Supabase로 이메일/비밀번호 인증
//    -> Supabase access_token 획득
//    -> 백엔드 /auth/exchange 로 전달
//    -> 백엔드가 "앱 JWT" 발급
//
// 2) 구글 로그인
//    프론트가 Supabase OAuth로 구글 로그인
//    -> 리다이렉트 후 Supabase session 생김
//    -> ProtectedRoute가 /auth/exchange 로 앱 JWT 교환
//
// 3) 카카오 로그인
//    백엔드가 직접 카카오 API 호출
//    -> 바로 앱 JWT 발급
//
// 4) 프로필 저장
//    이제 Supabase SDK로 직접 update 하지 않고,
//    백엔드 보호 API(/profile/complete)로 저장
//
// 핵심 원칙:
// "프론트가 최종적으로 들고 다니는 토큰은 spentopia_auth 하나"
// 그리고 그 값은 항상 "백엔드 앱 JWT"

import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import type {
  LoginRequest,
  LoginResponse,
  SignUpRequest,
} from "@/domains/auth/model/types";

// ─────────────────────────────────────────────────────────────
// 기존 인증 상태를 완전히 정리하는 함수
//
// 왜 필요한가?
// - 카카오 로그인 후 구글 로그인하면 예전 토큰이 남아서 꼬일 수 있음
// - Supabase session이 남아 있으면 새 구조와 충돌할 수 있음
// - 로그인 시작 전에는 항상 깨끗한 상태를 만드는 게 안전함
// ─────────────────────────────────────────────────────────────
const clearAllAuthState = async () => {
  // 앱 JWT 삭제
  localStorage.removeItem("spentopia_auth");

  // authStorage 메모리 캐시도 삭제
  authStorage.clear?.();

  // Supabase session도 정리
  try {
    await supabase.auth.signOut();
  } catch (e) {
    console.warn("Supabase 세션 정리 실패:", e);
  }
};

// ─────────────────────────────────────────────────────────────
// Supabase access_token -> 백엔드 앱 JWT 교환
//
// 이메일 로그인 / 구글 로그인에서 공통 사용
// ─────────────────────────────────────────────────────────────
const exchangeSupabaseToken = async (accessToken: string) => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/auth/exchange`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      access_token: accessToken,
    }),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "토큰 교환 실패");
  }

  return await res.json();
};

// ─────────────────────────────────────────────────────────────
// 이메일 로그인
//
// 흐름:
// 1) Supabase signInWithPassword
// 2) Supabase access_token 획득
// 3) 백엔드 /auth/exchange 로 앱 JWT 교환
// 4) LoginPage에서 그 앱 JWT를 저장
// ─────────────────────────────────────────────────────────────
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
    refreshToken: exchanged.refresh_token,
    user: {
      id: data.user.id,
      email: data.user.email ?? "",
      profileCompleted: false,
    },
  };
};

// ─────────────────────────────────────────────────────────────
// 회원가입
//
// Confirm Email이 켜져 있으면
// - 회원가입 성공해도 session이 null일 수 있음
// - 이 경우 accessToken 없이 반환
// - Signup 페이지가 "이메일 인증 안내 페이지"로 이동
//
// Confirm Email이 꺼져 있으면
// - session이 즉시 생김
// - 그 토큰을 /auth/exchange 로 교환해서 앱 JWT 반환
// ─────────────────────────────────────────────────────────────
export const signUp = async (payload: SignUpRequest): Promise<LoginResponse> => {
  await clearAllAuthState();

  const { data, error } = await supabase.auth.signUp({
    email: payload.email,
    password: payload.password,
    options: {
      // 이메일 인증 링크 클릭 후 여기로 돌아오게 설정
      emailRedirectTo: `${window.location.origin}/email-confirmed`,
    },
  });

  if (error) {
    throw new Error(error.message);
  }

  // 이메일 인증 필요 -> session 없음
  if (!data.session?.access_token) {
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

  // session이 있으면 앱 JWT로 교환
  const exchanged = await exchangeSupabaseToken(data.session.access_token);

  return {
    accessToken: exchanged.access_token,
    refreshToken: exchanged.refresh_token,
    user: {
      id: data.user?.id ?? "",
      email: data.user?.email ?? "",
      profileCompleted: false,
    },
  };
};

// ─────────────────────────────────────────────────────────────
// 구글 로그인
//
// 여기서는 바로 앱 JWT를 받지 않음.
// Supabase OAuth로 이동만 시키고,
// 돌아온 뒤 ProtectedRoute가 session을 보고 /auth/exchange 수행
// ─────────────────────────────────────────────────────────────
export const signInWithGoogle = async () => {
  await clearAllAuthState();

  const { error } = await supabase.auth.signInWithOAuth({
    provider: "google",
    options: {
      redirectTo: window.location.origin,
      queryParams: {
        // 계정 선택 화면을 유도
        prompt: "select_account",
      },
    },
  });

  if (error) {
    throw new Error(error.message);
  }
};

// ─────────────────────────────────────────────────────────────
// 카카오 로그인 시작
//
// 카카오는 Supabase OAuth를 안 쓰고
// 카카오 authorize URL로 직접 이동
// ─────────────────────────────────────────────────────────────
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

// ─────────────────────────────────────────────────────────────
// 카카오 콜백 페이지에서 code를 백엔드로 보내기
//
// 백엔드는 카카오 토큰 교환 + 유저 조회 + 앱 JWT 발급까지 수행
// ─────────────────────────────────────────────────────────────
export const loginWithKakaocode = async (code: string) => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/auth/kakao/login`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ code }),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "카카오 로그인 실패");
  }

  return await res.json();
};

// ─────────────────────────────────────────────────────────────
// 프로필 완성
//
// nickname / phone / profile_image 저장
// Authorization 헤더에는 "앱 JWT"를 넣음
// ─────────────────────────────────────────────────────────────
export const completeProfile = async (params: {
  nickname: string;
  phone: string;
  profileImage?: string;
}) => {
  const token = localStorage.getItem("spentopia_auth");

  if (!token) {
    throw new Error("로그인 상태가 아닙니다");
  }

  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/profile/complete`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      nickname: params.nickname,
      phone: params.phone,
      profile_image: params.profileImage ?? null,
    }),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "프로필 저장 실패");
  }

  return await res.json();
};

// ─────────────────────────────────────────────────────────────
// 비밀번호 재설정 메일 발송
//
// 먼저 백엔드에서 해당 이메일이 존재하는지 확인 후,
// 존재할 때만 Supabase resetPasswordForEmail 호출
// ─────────────────────────────────────────────────────────────
export const resetPassword = async (email: string) => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const checkRes = await fetch(`${BACKEND_URL}/auth/check-email`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ email }),
  });

  if (!checkRes.ok) {
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

// ─────────────────────────────────────────────────────────────
// 전화번호로 이메일 찾기
// ─────────────────────────────────────────────────────────────
export const findEmailByPhone = async (phone: string): Promise<string> => {
  const BACKEND_URL = import.meta.env.VITE_BACKEND_URL;

  const res = await fetch(`${BACKEND_URL}/auth/find-email`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ phone }),
  });

  if (!res.ok) {
    const err = await res.text();
    throw new Error(err || "이메일 찾기 실패");
  }

  const data = await res.json();
  return data.masked_email;
};

// ─────────────────────────────────────────────────────────────
// 로그아웃
//
// 앱 JWT + Supabase session 모두 정리
// ─────────────────────────────────────────────────────────────
export const signOut = async () => {
  await clearAllAuthState();
};