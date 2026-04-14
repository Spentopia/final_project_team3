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
      // 1) 먼저 Supabase 세션 확인 (이메일/구글)
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (session) {
        authStorage.setToken(session.access_token);
        localStorage.setItem("spentopia_auth", session.access_token);

        const { data: profile, error } = await supabase
          .from("users")
          .select("profile_completed")
          .eq("id", session.user.id)
          .single();

        if (error) {
          console.error("Supabase profile 조회 실패:", error);
          setStatus("not_logged_in");
          return;
        }

        if (profile && !profile.profile_completed) {
          setStatus("need_profile");
        } else {
          setStatus("logged_in");
        }
        return;
      }

      // 2) Supabase 세션 없으면 자체 JWT 확인 (카카오/지갑)
      const token =
        authStorage.getToken?.() || localStorage.getItem("spentopia_auth");

      if (!token) {
        setStatus("not_logged_in");
        return;
      }

      const res = await fetch(`${import.meta.env.VITE_BACKEND_URL}/auth/me`, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        localStorage.removeItem("spentopia_auth");
        setStatus("not_logged_in");
        return;
      }

      const me = await res.json();

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