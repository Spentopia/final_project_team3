// domains/auth/ui/KakaoCallbackPage.tsx
//
// 카카오 로그인 후 redirect_uri로 돌아오는 콜백 페이지.
//
// 흐름:
// 1) 카카오 로그인 페이지에서 인증 완료
// 2) redirect_uri 로 /auth/kakao/callback?code=xxx 로 이동
// 3) 여기서 code를 백엔드 /auth/kakao/login 으로 전달
// 4) 백엔드가 카카오 유저 조회 + 앱 JWT 발급
// 5) 프론트는 그 앱 JWT를 저장
// 6) / 로 이동 -> ProtectedRoute가 /me 체크

import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { loginWithKakaocode } from "@/domains/auth/api/auth";
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

    if (!code) {
      setError("카카오 인가 코드가 없습니다");
      return;
    }

    void handleKakaoLogin(code);
  }, [searchParams]);

  const handleKakaoLogin = async (code: string) => {
    try {
      const data = await loginWithKakaocode(code);

      // 최종적으로 받은 건 앱 JWT
      authStorage.setToken(data.access_token);

      navigate("/");
    } catch (err: any) {
      setError(err.message || "카카오 로그인 실패");
    }
  };

  if (error) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <div className="text-center">
          <p className="text-red-500 mb-4">{error}</p>
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
      <p className="text-gray-500">카카오 로그인 처리 중...</p>
    </div>
  );
}