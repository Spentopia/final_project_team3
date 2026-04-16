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

    // ── CSRF 방지: state 검증 ───────────────────────────────
    //
    // redirectToKakao()에서 생성해 sessionStorage에 저장해 둔 state와
    // 카카오가 콜백 URL에 실어 돌려준 state가 일치하는지 확인.
    //
    // 검증 순서:
    //   1) sessionStorage에서 저장된 state 읽기
    //   2) 즉시 삭제 (일회성 사용 — 재사용 공격 방지)
    //   3) URL 파라미터의 state와 비교
    //   4) 불일치 시 로그인 중단
    //
    // 불일치가 발생하는 경우:
    //   - 공격자가 만든 콜백 URL을 피해자가 직접 열었을 때
    //   - sessionStorage가 없는 환경(탭 간 이동 등)
    // ──────────────────────────────────────────────────────────
    const returnedState = searchParams.get("state");
    const storedState = sessionStorage.getItem("kakao_oauth_state");
    sessionStorage.removeItem("kakao_oauth_state"); // 검증 직후 즉시 삭제

    if (!returnedState || !storedState || returnedState !== storedState) {
      setError("잘못된 로그인 요청입니다. 다시 시도해 주세요.");
      return;
    }

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