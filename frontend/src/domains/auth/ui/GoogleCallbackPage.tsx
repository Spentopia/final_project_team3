import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { toast } from "sonner";
import { apiClient } from "@/shared/api/client";
import type { Session } from "@supabase/supabase-js";

const sleep = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));
let googleLoginInFlight: Promise<string> | null = null;

async function waitForSupabaseSession() {
  for (let attempt = 0; attempt < 10; attempt += 1) {
    const {
      data: { session },
      error: sessionError,
    } = await supabase.auth.getSession();

    if (sessionError) {
      throw new Error(sessionError.message);
    }

    if (session?.access_token) {
      return session;
    }

    await sleep(300);
  }

  return null;
}

function readOAuthErrorFromUrl() {
  const url = new URL(window.location.href);
  const errorDescription =
    url.searchParams.get("error_description") ||
    new URLSearchParams(window.location.hash.replace(/^#/, "")).get("error_description");

  return errorDescription ? decodeURIComponent(errorDescription.replace(/\+/g, " ")) : null;
}

async function resolveSessionFromUrl(): Promise<Session | null> {
  const oauthError = readOAuthErrorFromUrl();
  if (oauthError) {
    throw new Error(oauthError);
  }

  const session = await waitForSupabaseSession();

  if (session?.access_token) {
    window.history.replaceState({}, document.title, "/auth/google/callback");
    return session;
  }

  return null;
}

async function resolveSupabaseAccessToken() {
  const session = await resolveSessionFromUrl();
  if (session?.access_token) {
    return session.access_token;
  }

  throw new Error("구글 로그인 세션이 없습니다");
}

type AccountInactiveErrorPayload = {
  code: "ACCOUNT_INACTIVE";
  message: string;
  reason: string;
  inactive_until_text: string;
  support_email: string;
};

function isAccountInactivePayload(data: unknown): data is AccountInactiveErrorPayload {
  return (
      !!data &&
      typeof data === "object" &&
      "code" in data &&
      (data as { code?: unknown }).code === "ACCOUNT_INACTIVE"
  );
}

async function completeGoogleLogin() {
  const accessToken = await resolveSupabaseAccessToken();

  let exchanged;

  try {
    exchanged = await apiClient.post("/auth/exchange", {
      access_token: accessToken,
    });
  } catch (error) {
    const data = (
        error as {
          response?: {
            data?: unknown;
          };
        }
    ).response?.data;

    if (isAccountInactivePayload(data)) {
      sessionStorage.setItem(
          "account_inactive_info",
          JSON.stringify(data)
      );

      throw new Error(data.message);
    }

    throw error;
  }

  const appAccessToken = exchanged?.data?.access_token;

  if (!appAccessToken) {
    throw new Error("앱 로그인 토큰을 받지 못했습니다.");
  }

  sessionStorage.setItem("just_logged_in", "true");
  authStorage.setToken(appAccessToken);

  try {
    await supabase.auth.signOut();
  } catch (signOutError) {
    console.warn("Supabase signOut 실패:", signOutError);
  }

  return appAccessToken;
}

export default function GoogleCallbackPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const calledRef = useRef(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;

    if (calledRef.current) return;
    calledRef.current = true;

    void handleGoogleLogin();

    return () => {
      mountedRef.current = false;
    };
  }, []);

  const handleGoogleLogin = async () => {
    try {
      if (authStorage.getToken()) {
        navigate("/", { replace: true });
        return;
      }

      if (!googleLoginInFlight) {
        googleLoginInFlight = completeGoogleLogin().finally(() => {
          googleLoginInFlight = null;
        });
      }

      await googleLoginInFlight;

      navigate("/", { replace: true });
    } catch (err: any) {
      if (!mountedRef.current) return;

      // 백엔드 응답 메시지 추출 (axios 응답 우선)
      // 우선순위:
      // 1) response.data가 string (백엔드가 직접 문자열 반환)
      // 2) response.data.message
      // 3) Error.message
      // 4) fallback
      const message =
          err?.response?.data?.message ||
          err?.response?.data ||
          err?.message ||
          "구글 로그인에 실패했습니다.";

      // toast + 자동 리다이렉트 (카카오 콜백과 동일한 UX)
      console.error("구글 로그인 실패:", err);
      toast.error(String(message));
      navigate("/login", { replace: true });
    }
  };

  // 처리 중 화면 (콜백 직후 잠깐 보임)
  return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">구글 로그인 처리 중...</p>
      </div>
  );
}
