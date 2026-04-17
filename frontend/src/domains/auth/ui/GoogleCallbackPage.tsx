import { useEffect, useRef, useState } from "react";
import { useNavigate } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { apiClient } from "@/shared/api/client";

export default function GoogleCallbackPage() {
  const navigate = useNavigate();
  const [error, setError] = useState<string | null>(null);
  const calledRef = useRef(false);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    void handleGoogleLogin();
  }, []);

  const handleGoogleLogin = async () => {
    try {
      const {
        data: { session },
        error: sessionError,
      } = await supabase.auth.getSession();

      if (sessionError) {
        throw new Error(sessionError.message);
      }

      if (!session?.access_token) {
        throw new Error("구글 로그인 세션이 없습니다");
      }

      const exchanged = await apiClient.post(
        "/auth/exchange",
        {
          access_token: session.access_token,
        },
        {
          headers: {
            "X-Client-Type": "web",
          },
        }
      );

      sessionStorage.setItem("just_logged_in", "true"); // 🔥 추가
      authStorage.setToken(exchanged.data.access_token);

      try {
        await supabase.auth.signOut();
      } catch (signOutError) {
        console.warn("Supabase signOut 실패:", signOutError);
      }

      navigate("/");
    } catch (err: any) {
      setError(err.message || "구글 로그인 실패");
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
