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

  const checkAuth = async () => {
    try {
      // 1) 먼저 local 토큰 확인
      let token =
        authStorage.getToken?.() || localStorage.getItem("spentopia_auth");

      // 2) 없으면 Supabase session에서 한 번 가져와서 동기화
      if (!token) {
        const {
          data: { session },
        } = await supabase.auth.getSession();

        if (session?.access_token) {
          token = session.access_token;
          authStorage.setToken?.(session.access_token);
          localStorage.setItem("spentopia_auth", session.access_token);
        }
      }

      if (!token) {
        setStatus("not_logged_in");
        return;
      }

      const res = await fetch(`${import.meta.env.VITE_BACKEND_URL}/me`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        console.error("/me 호출 실패:", res.status);
        localStorage.removeItem("spentopia_auth");

        // Supabase 세션도 같이 정리
        try {
          await supabase.auth.signOut();
        } catch (e) {
          console.warn("Supabase signOut 실패:", e);
        }

        setStatus("not_logged_in");
        return;
      }

      const me = await res.json();
      console.log("me response:", me);

      if (!me.profile_completed) {
        setStatus("need_profile");
      } else {
        setStatus("logged_in");
      }
    } catch (error) {
      console.error("인증 확인 실패:", error);
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