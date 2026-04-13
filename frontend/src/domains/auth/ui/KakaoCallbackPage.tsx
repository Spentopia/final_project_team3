// domains/auth/ui/KakaoCallbackPage.tsx
// ─────────────────────────────────────────────────────────────
// 카카오 로그인 후 리다이렉트되는 콜백 페이지
//
// 카카오 로그인 흐름에서 이 페이지의 위치:
// 1) 유저가 LoginPage에서 "카카오로 계속하기" 클릭
// 2) redirectToKakao() → 카카오 로그인 페이지로 이동
// 3) 유저가 카카오에서 로그인 완료
// 4) 카카오가 이 페이지로 리다이렉트 (URL: /auth/kakao/callback?code=xxx)
//    → 여기서 code 파라미터가 카카오 인가 코드
// 5) 이 페이지가 code를 백엔드로 전송 → 백엔드가 JWT 발급
// 6) JWT 저장 후 메인 또는 프로필 완성 페이지로 이동
//
// 유저에게는 "카카오 로그인 처리 중..." 메시지만 보이고
// 자동으로 처리됨 (수동 조작 불필요)

import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { loginWithKakaocode } from "@/domains/auth/api/auth";
import { authStorage } from "@/shared/lib/auth";

export default function KakaoCallbackPage() {
    const navigate = useNavigate();

    // URL 파라미터에서 카카오 인가 코드를 꺼냄
    // 예: /auth/kakao/callback?code=abc123 → searchParams.get("code") = "abc123"
    const [searchParams] = useSearchParams();
    const [error, setError] = useState<string | null>(null);

    //페이지가 로드되면 자동으로 로그인 처리 시작
    useEffect(() => {
        //URL에서 인가 코드 추출
        const code = searchParams.get("code");

        //인가 코드가 없으면 에러 (카카오에서 로그인 취소했거나 URL 직접 접근)
        if (!code) {
            setError("카카오 인가 코드가 없습니다");
            return;
        }

        //인가 코드가 있으면 백엔드로 전송해서 로그인 처리
        handleKakaoLogin(code);
    }, []);

    const handleKakaoLogin = async (code: string) => {
        try {
            // 백엔드 /auth/kakao/login으로 인가 코드 전송
            // 백엔드가 하는 일:
            // 1) 인가 코드 → 카카오 access_token 교환
            // 2) access_token → 카카오 유저 정보 조회 (id, nickname 등)
            // 3) 카카오 ID로 Supabase 유저 찾거나 생성
            // 4) Supabase JWT 발급해서 반환
            const data = await loginWithKakaocode(code);

            // 받은 JWT를 localStorage에 저장
            // 이후 백엔드 API 호출 시 이 토큰이 Authorization 헤더에 들어감
            authStorage.setToken(data.access_token);
            localStorage.setItem("spentopia_auth", data.access_token);

            // 첫 가입이면 프로필 완성 페이지로 (닉네임/전화번호 입력)
            // 기존 유저면 메인 페이지로
            if (data.is_new_user) {
                navigate("/complete-profile");
            } else {
                navigate("/");
            }
        } catch (err:any) {
            setError(err.message || "카카오 로그인 실패");
        }
    };

    // 에러 발생 시 에러 메시지 + 로그인으로 돌아가기 버튼
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

  // 정상 처리 중일 때 로딩 메시지
  return (
    <div className="flex min-h-screen items-center justify-center">
      <p className="text-gray-500">카카오 로그인 처리 중...</p>
    </div>
  );
}


