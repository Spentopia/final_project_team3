// app/router/ProtectedRoute.tsx
//
// 보호된 페이지 접근 전 인증 상태 확인 컴포넌트
//
// 최종 흐름:
// 1) 먼저 메모리에 access token이 있는지 확인
// 2) 없으면 Supabase session이 있는지 확인
//    - 구글 로그인 직후 / 이메일 로그인 직후일 수 있음
// 3) session이 있으면 /auth/exchange 로 백엔드 앱 JWT 교환
// 4) 교환 성공 후 access token을 메모리에 저장
// 5) /me 호출해서 profile_completed 여부 확인
//
// 중요:
// - access token은 localStorage가 아니라 메모리에만 저장
// - refresh token은 HttpOnly 쿠키라 프론트 JS가 직접 만지지 않음

import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { apiClient } from "@/shared/api/client";
import { initAuth } from "@/shared/lib/initAuth";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

type AuthStatus = "loading" | "logged_in" | "need_profile" | "not_logged_in";

let authCheckInFlight: Promise<AuthStatus> | null = null;
let refreshRecoveryInFlight: Promise<boolean> | null = null;

function recoverAccessTokenOnce() {
  if (!refreshRecoveryInFlight) {
    refreshRecoveryInFlight = initAuth().finally(() => {
      refreshRecoveryInFlight = null;
    });
  }

  return refreshRecoveryInFlight;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const location = useLocation();

  // Supabase session의 access_token을 백엔드 앱 JWT로 교환
  const exchangeSupabaseToken = async (accessToken: string) => {
    const res = await apiClient.post("/auth/exchange", {
      access_token: accessToken,
    });

    return res.data;
  };

  const checkAuth = async (): Promise<AuthStatus> => {
    // 1) 현재 메모리에 저장된 앱 access token 확인
    let token = authStorage.getToken();

    const justLoggedIn = sessionStorage.getItem("just_logged_in") === "true";
      if (justLoggedIn) {
        sessionStorage.removeItem("just_logged_in");
      }

    // 2) access token이 없으면 refresh 쿠키로 앱 토큰 복구 시도
    //
    // 이 단계는 보호 라우트에서만 실행한다.
    // 그래서 공개 페이지(/login 등)에서는 불필요한 /auth/refresh가 발생하지 않는다.
    if (!token && !justLoggedIn) {
    const recovered = await recoverAccessTokenOnce();
    if (recovered) {
      token = authStorage.getToken();
    }
  }

    // 3) 그래도 access token이 없으면 Supabase session 확인
    //
    // 구글 로그인 직후 / 이메일 로그인 직후에는
    // 아직 앱 JWT로 교환되지 않고 Supabase session만 있을 수 있음
    if (!token) {
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (session?.access_token) {
        const exchanged = await exchangeSupabaseToken(session.access_token);

        token = exchanged.access_token;

        // 교환된 앱 access token을 메모리에 저장
        authStorage.setToken(exchanged.access_token);

        // 앱 내부 보호 API는 이제 우리 앱 JWT만 사용하므로
        // Supabase session은 정리
        try {
          await supabase.auth.signOut();
        } catch (e) {
          console.warn("Supabase signOut 실패:", e);
        }
      }
    }

    // 4) 그래도 token이 없으면 비로그인
    if (!token) {
      authStorage.clear();
      return "not_logged_in";
    }

    // 5) /me 호출해서 실제 로그인 유저 정보 확인
    const res = await apiClient.get("/me");

    const me = res.data;

    // 6) 프로필 완성 여부에 따라 라우팅 분기
    if (!me.profile_completed) {
      return "need_profile";
    }

    return "logged_in";
  };

  useEffect(() => {
    let cancelled = false;

    if (!authCheckInFlight) {
      authCheckInFlight = checkAuth().finally(() => {
        authCheckInFlight = null;
      });
    }

    void authCheckInFlight
      .then((nextStatus) => {
        if (!cancelled) {
          setStatus(nextStatus);
        }
      })
      .catch((error) => {
        console.error("인증 확인 실패:", error);
        authStorage.clear();
        if (!cancelled) {
          setStatus("not_logged_in");
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  if (status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">로딩 중...</p>
      </div>
    );
  }

  if (status === "not_logged_in") {
    return <Navigate to="/login" replace />;
  }

  if (status === "need_profile") {
    if (location.pathname === "/complete-profile") {
      return <>{children}</>;
    }
    return <Navigate to="/complete-profile" replace />;
  }

  return <>{children}</>;
}
