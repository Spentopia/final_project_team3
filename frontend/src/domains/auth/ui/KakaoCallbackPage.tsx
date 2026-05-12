import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { toast } from "sonner";
import { loginWithKakaocode, AccountInactiveError } from "@/domains/auth/api/auth";
import { authStorage } from "@/shared/lib/auth";

export default function KakaoCallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [error, setError] = useState<string | null>(null);
  const calledRef = useRef(false);

  useEffect(() => {
    if (calledRef.current) return;
    calledRef.current = true;

    const code = searchParams.get("code");
    const state = searchParams.get("state");

    if (!code) {
      toast.error("로그인 링크가 만료되었어요. 다시 로그인해 주세요.");
      navigate("/login", { replace: true });
      return;
    }

    if (!state) {
      toast.error("잘못된 로그인 요청입니다. 다시 로그인해 주세요.");
      navigate("/login", { replace: true });
      return;
    }

    // 주소창의 code/state를 먼저 치워서 새로고침 재사용 가능성 줄임
    window.history.replaceState({}, document.title, "/auth/kakao/callback");

    void handleKakaoLogin(code, state);
  }, [searchParams, navigate]);

  const handleKakaoLogin = async (code: string, state: string) => {
    try {
      const data = await loginWithKakaocode(code, state);

      sessionStorage.setItem("just_logged_in", "true");
      authStorage.setToken(data.access_token);

      navigate("/", { replace: true });
    } catch (err: any) {
    console.error("카카오 로그인 실패:", err);

    if (err instanceof AccountInactiveError) {
      sessionStorage.setItem(
          "account_inactive_info",
          JSON.stringify(err.payload)
      );

      toast.error(err.payload.message);
      navigate("/login", { replace: true });
      return;
    }

    toast.error(
        err instanceof Error ? err.message : "카카오 로그인에 실패했습니다."
    );

    navigate("/login", { replace: true });
  }
  };

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
          <button
            onClick={() => navigate("/login", { replace: true })}
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
      <p className="text-gray-500">카카오 로그인 처리 중...</p>
    </div>
  );
}
