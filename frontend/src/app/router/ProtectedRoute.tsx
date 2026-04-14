// app/router/ProtectedRoute.tsx
//
// 보호된 페이지에 접근하기 전에 인증 상태를 확인하는 컴포넌트.
//
// 현재 구조의 핵심:
// 1) 먼저 localStorage의 spentopia_auth(앱 JWT) 확인
// 2) 없으면 Supabase session 있는지 확인
//    - 구글 로그인 직후나 이메일 로그인 직후일 수 있음
// 3) session이 있으면 /auth/exchange 로 백엔드 JWT 교환
// 4) /me 호출해서 profile_completed 여부 확인
//
// 결국 최종적으로는 항상 "앱 JWT"만 사용하게 만드는 게 목적

import { useEffect, useState } from "react";
import { Navigate } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

type AuthStatus = "loading" | "logged_in" | "need_profile" | "not_logged_in";

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const [status, setStatus] = useState<AuthStatus>("loading");

  useEffect(() => {
    void checkAuth();
  }, []);

  // Supabase session의 access_token을 백엔드 앱 JWT로 교환
  const exchangeSupabaseToken = async (accessToken: string) => {
    const res = await fetch(`${import.meta.env.VITE_BACKEND_URL}/auth/exchange`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        access_token: accessToken,
      }),
    });

    if (!res.ok) {
      throw new Error(`/auth/exchange 실패: ${res.status}`);
    }

    return await res.json();
  };

  const checkAuth = async () => {
    try {
      // 1) 이미 저장된 앱 JWT 먼저 확인
      let token = localStorage.getItem("spentopia_auth");

      // 2) 앱 JWT가 없으면 Supabase session 확인
      //    구글 로그인 후 돌아온 직후에는 session만 있고
      //    아직 앱 JWT로 교환되지 않았을 수 있음
      if (!token) {
        const {
          data: { session },
        } = await supabase.auth.getSession();

        if (session?.access_token) {
          const exchanged = await exchangeSupabaseToken(session.access_token);

          token = exchanged.access_token;

          // 교환된 앱 JWT 저장
          localStorage.setItem("spentopia_auth", exchanged.access_token);
          authStorage.setToken?.(exchanged.access_token);

          // 이제 앱 내부에서는 Supabase session 안 쓰므로 정리
          try {
            await supabase.auth.signOut();
          } catch (e) {
            console.warn("Supabase signOut 실패:", e);
          }
        }
      }

      // 3) 토큰이 없거나 JWT 형태가 이상하면 비로그인 처리
      if (!token || !token.includes(".")) {
        localStorage.removeItem("spentopia_auth");
        authStorage.clear?.();
        setStatus("not_logged_in");
        return;
      }

      // 4) /me 호출해서 실제 로그인 유저 정보 확인
      const res = await fetch(`${import.meta.env.VITE_BACKEND_URL}/me`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        console.error("/me 호출 실패:", res.status);
        localStorage.removeItem("spentopia_auth");
        authStorage.clear?.();
        setStatus("not_logged_in");
        return;
      }

      const me = await res.json();
      console.log("me response:", me);

      // 5) 프로필 완성 여부에 따라 라우팅 분기
      if (!me.profile_completed) {
        setStatus("need_profile");
      } else {
        setStatus("logged_in");
      }
    } catch (error) {
      console.error("인증 확인 실패:", error);
      localStorage.removeItem("spentopia_auth");
      authStorage.clear?.();
      setStatus("not_logged_in");
    }
  };

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
    return <Navigate to="/complete-profile" replace />;
  }

  return <>{children}</>;
}