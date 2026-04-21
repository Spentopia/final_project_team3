import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
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

async function completeGoogleLogin() {
  const accessToken = await resolveSupabaseAccessToken();
  const exchanged = await apiClient.post("/auth/exchange", { access_token: accessToken });

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

      const message =
        err?.response?.data?.message ||
        err?.response?.data ||
        err?.message ||
        "구글 로그인 실패";

      setError(String(message));
    }
  };

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <p className="mb-4 text-red-500">{error}</p>
          <button
            onClick={() => navigate("/login")}
            className="text-cyan-600 hover:underline"
          >
            로그인으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-gray-500">구글 로그인 처리 중...</p>
    </div>
  );
}
