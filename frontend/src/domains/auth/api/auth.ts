// domains/auth/api/auth.ts

import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { apiClient } from "@/shared/api/client";
import { stripPhone } from "@/shared/lib/phone";
import { PASSWORD_REQUIREMENTS_MESSAGE } from "@/domains/auth/lib/password";
import type {
  LoginRequest,
  LoginResponse,
  SignUpRequest,
} from "@/domains/auth/model/types";

const normalizeEmail = (email: string) => email.trim().toLowerCase();

const clearWalletAdapterState = () => {
  if (typeof window === "undefined") return;

  window.localStorage.removeItem("spentopiaWalletName");
  window.localStorage.removeItem("walletName");
};

const extractApiErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === "object" && "response" in error) {
    const response = (
      error as {
        response?: {
          data?: unknown;
        };
      }
    ).response;

    if (typeof response?.data === "string" && response.data.trim()) {
      return response.data;
    }

    if (
      response?.data &&
      typeof response.data === "object" &&
      "message" in response.data &&
      typeof response.data.message === "string" &&
      response.data.message.trim()
    ) {
      return response.data.message;
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
};

const mapSupabaseAuthError = (message: string, fallback: string) => {
  const normalizedMessage = message.toLowerCase();

  if (normalizedMessage.includes("invalid login credentials")) {
    return "이메일 또는 비밀번호가 일치하지 않습니다.";
  }

  if (normalizedMessage.includes("user already registered")) {
    return "이미 사용 중인 이메일이거나 가입할 수 없는 이메일입니다.";
  }

  if (normalizedMessage.includes("email not confirmed")) {
    return "이메일 인증 후 로그인해주세요.";
  }

  if (
    normalizedMessage.includes("weak password") ||
    normalizedMessage.includes("password should contain") ||
    normalizedMessage.includes("password should be at least") ||
    normalizedMessage.includes("password is too weak") ||
    normalizedMessage.includes("different from the old password") ||
    normalizedMessage.includes("same password")
  ) {
    if (
      normalizedMessage.includes("different from the old password") ||
      normalizedMessage.includes("same password")
    ) {
      return "이전과 다른 비밀번호를 입력해주세요.";
    }

    return PASSWORD_REQUIREMENTS_MESSAGE;
  }

  return fallback;
};

// 기존 인증 상태를 정리
const clearAllAuthState = async () => {
  // 백엔드 refresh 쿠키 정리
  try {
    await apiClient.post("/auth/logout", {}, { withCredentials: true });
  } catch (e) {
    console.warn("백엔드 refresh 쿠키 정리 실패:", e);
  }

  // 앱 access token 메모리 삭제
  authStorage.clear();
  clearWalletAdapterState();

  // Supabase session 정리
  try {
    await supabase.auth.signOut();
  } catch (e) {
    console.warn("Supabase 세션 정리 실패:", e);
  }
};

// Supabase access_token -> 백엔드 앱 access token 교환
const exchangeSupabaseToken = async (accessToken: string) => {
  const res = await apiClient.post("/auth/exchange", {
    access_token: accessToken,
  });

  return res.data;
};

// 이메일 로그인
export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  await clearAllAuthState();
  const normalizedEmail = normalizeEmail(payload.email);

  const { data, error } = await supabase.auth.signInWithPassword({
    email: normalizedEmail,
    password: payload.password,
  });

  if (error) {
    throw new Error(mapSupabaseAuthError(error.message, "로그인에 실패했습니다."));
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
export const signUp = async (payload: SignUpRequest, captchaToken: string): Promise<LoginResponse> => {
  await clearAllAuthState();
  const normalizedEmail = normalizeEmail(payload.email);

  const domain = normalizedEmail.split("@")[1];
  if (domain === "admin.com") {
    throw new Error("해당 이메일 도메인으로는 가입할 수 없습니다.");
  }

  // ── 1) 이메일 중복 확인 ────────────────────────────────────
  // 백엔드의 /auth/check-email은 public.users에서 이메일 존재 여부를 확인
  // 200 + { exists: true } → 이미 가입된 이메일
  // 404 → 가입 가능한 이메일
  try {
    const checkRes = await apiClient.post("/auth/check-email", {
      email: normalizedEmail,
      captcha_token: captchaToken,
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
    email: normalizedEmail,
    password: payload.password,
    options: {
      emailRedirectTo: `${window.location.origin}/email-confirmed`,
    },
  });

  if (error) {
    throw new Error(mapSupabaseAuthError(error.message, "회원가입에 실패했습니다."));
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
      redirectTo: `${window.location.origin}/auth/google/callback`,
      queryParams: {
        prompt: "select_account",
      },
    },
  });

  if (error) {
    throw new Error(mapSupabaseAuthError(error.message, "구글 로그인에 실패했습니다."));
  }
};

export const redirectToKakao = async () => {
  await clearAllAuthState();

  const res = await apiClient.post("/auth/kakao/start", {}, { withCredentials: true });

  window.location.href = res.data.auth_url;
};

export const loginWithKakaocode = async (code: string, state: string) => {
  const res = await apiClient.post("/auth/kakao/login", { code, state }, { withCredentials: true });

  return res.data;
};

// 프로필 완성
export const completeProfile = async (params: {
  nickname: string;
  phone: string;
  profileImage?: string;
}) => {
  try {
    const res = await apiClient.patch("/profile/complete", {
      nickname: params.nickname,
      phone: params.phone,
      profile_image: params.profileImage ?? null,
    });

    return res.data;
  } catch (error) {
    throw new Error(extractApiErrorMessage(error, "프로필 저장에 실패했습니다."));
  }
};

export const checkNicknameAvailable = async (nickname: string): Promise<boolean> => {
  const res = await apiClient.post("/profile/check-nickname", { nickname });
  return res.data?.available === true;
};

export const checkProfileAvailability = async (params: {
  nickname: string;
  phone: string;
}) => {
  try {
    const res = await apiClient.post("/profile/check-availability", {
      nickname: params.nickname,
      phone: params.phone,
    });

    return res.data;
  } catch (error) {
    throw new Error(extractApiErrorMessage(error, "중복 확인에 실패했습니다."));
  }
};

// 비밀번호 재설정 메일 발송
export const resetPassword = async (email: string, captchaToken: string) => {
  const normalizedEmail = normalizeEmail(email);

  try {
    await apiClient.post("/auth/check-reset-password-email", {
      email: normalizedEmail,
      captcha_token: captchaToken,
    });
  } catch (error) {
    throw new Error(
      extractApiErrorMessage(error, "입력한 정보와 일치하는 계정을 찾을 수 없습니다.")
    );
  }

  const { error } = await supabase.auth.resetPasswordForEmail(normalizedEmail, {
    redirectTo: `${window.location.origin}/reset-password`,
  });

  if (error) {
    throw new Error(
      mapSupabaseAuthError(error.message, "비밀번호 재설정 메일 발송에 실패했습니다.")
    );
  }
};

// 새 비밀번호 설정
export const updatePassword = async (newPassword: string) => {
  const { error } = await supabase.auth.updateUser({
    password: newPassword,
  });

  if (error) {
    throw new Error(
      mapSupabaseAuthError(error.message, "비밀번호 변경에 실패했습니다.")
    );
  }

  await supabase.auth.signOut();
};

// 전화번호로 이메일 찾기
//
// Turnstile captcha_token도 함께 전송한다.
// - phone: 사용자가 입력한 전화번호
// - captchaToken: Cloudflare Turnstile에서 발급받은 토큰
export type FindEmailResponse = {
  masked_email: string | null;
  login_provider: string;
  google_connected: boolean;
  message: string;
};

export const findEmailByPhone = async (
  phone: string,
  captchaToken: string
): Promise<FindEmailResponse> => {
  try {
    const res = await apiClient.post("/auth/find-email", {
      phone: stripPhone(phone),
      captcha_token: captchaToken,
    });

    return res.data;
  } catch (error) {
    throw new Error(
      extractApiErrorMessage(error, "입력한 정보와 일치하는 계정을 찾을 수 없습니다.")
    );
  }
};

// 회원탈퇴
//
// 처리 순서:
// 1) 백엔드 /auth/withdraw 호출 → DB soft delete + auth.users 삭제 + 세션 revoke
// 2) 로컬 access token 삭제 (메모리)
// 3) Supabase 세션 삭제 (로컬 스토리지)
//
// withCredentials: true → refresh 쿠키도 같이 전송해서 백엔드에서 쿠키 삭제 처리
export const withdrawAccount = async () => {
  await apiClient.post("/auth/withdraw", {}, { withCredentials: true });
  authStorage.clear();
  clearWalletAdapterState();
  await supabase.auth.signOut();
};

// 로그아웃
export const signOut = async () => {
  try {
    await apiClient.post("/auth/logout", {});
  } finally {
    await supabase.auth.signOut();  // ✅ 여기서만
    authStorage.clear();
    clearWalletAdapterState();
  }
};
