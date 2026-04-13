// domains/auth/ui/LoginPage.tsx
// ─────────────────────────────────────────────────────────────
// 로그인 페이지
//
// 지원하는 로그인 방식:
// 1) 자체 이메일/비밀번호 → login() 호출 → Supabase signInWithPassword
// 2) 구글 소셜 → signInWithGoogle() → 구글 OAuth 페이지로 리다이렉트
// 3) 카카오 소셜 → signInWithKakao() → 카카오 OAuth 페이지로 리다이렉트
// 4) 지갑(Solana) → WalletLoginButton 컴포넌트가 처리
//
// 로그인 성공 후:
// - authStorage에 토큰 저장
// - navigate("/")로 메인 페이지 이동
// - ProtectedRoute가 profile_completed 체크 → 미완성이면 /complete-profile로

import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";
import { login, signInWithGoogle, signInWithKakao } from "@/domains/auth/api/auth";
import { authStorage } from "@/shared/lib/auth";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false); // 로그인 요청 중 버튼 비활성화용

  // ── 이메일 로그인 ─────────────────────────────────────────
  const handleLogin = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault(); // form 기본 동작(페이지 새로고침) 방지
    setLoading(true);

    try {
      // Supabase signInWithPassword 호출 → JWT 반환
      const result = await login({ email, password });

      // 받은 토큰을 localStorage에 저장
      authStorage.setToken(result.accessToken);
      localStorage.setItem("spentopia_auth", result.accessToken);

      // 메인 페이지로 이동 (ProtectedRoute가 프로필 체크)
      navigate("/");
    } catch (error: any) {
      // 이메일 틀림, 비밀번호 틀림 등
      alert(error.message || "로그인 실패");
    } finally {
      setLoading(false); // 성공/실패 상관없이 로딩 해제
    }
  };

  // ── 소셜 로그인 ───────────────────────────────────────────
  // OAuth는 Supabase가 외부 페이지로 리다이렉트시킴
  // 로그인 후 돌아왔을 때 useAuth 훅이 세션을 감지해서 토큰 저장
  const handleSocialLogin = async (provider: string) => {
    try {
      if (provider === "google") {
        await signInWithGoogle();  // 구글 로그인 페이지로 이동
      } else if (provider === "kakao") {
        await signInWithKakao();   // 카카오 로그인 페이지로 이동
      }
      // 여기 아래 코드는 실행 안 됨 (리다이렉트 때문에 페이지가 바뀜)
    } catch (error: any) {
      alert(error.message || "소셜 로그인 실패");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500 dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">

          {/* Logo */}
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 shadow-lg">
              <Sparkles className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              Spentopia
            </h1>
            <p className="text-center text-gray-600 dark:text-gray-400">
              내가 기록한 소비가 나를 만든다
            </p>
          </div>

          {/* 이메일 로그인 폼 */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <Label htmlFor="email">이메일</Label>
              <Input
                id="email"
                type="email"
                placeholder="이메일을 입력하세요"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="password">비밀번호</Label>
              <Input
                id="password"
                type="password"
                placeholder="비밀번호를 입력하세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="mt-1"
              />
            </div>

            <Button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
            >
              {loading ? "로그인 중..." : "로그인"}
            </Button>
          </form>

          {/* 이메일/비밀번호 찾기 링크 */}
          <div className="mt-4 flex justify-center gap-4 text-sm text-gray-500 dark:text-gray-400">
            <Link to="/find-email" className="hover:text-cyan-600 dark:hover:text-cyan-400">
              이메일 찾기
            </Link>
            <span>|</span>
            <Link to="/forgot-password" className=
            "hover:text-cyan-600 dark:hover:text-cyan-400">
              비밀번호 찾기
            </Link>
          </div>

          {/* 구분선 */}
          <div className="my-6 flex items-center gap-4">
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700"></div>
            <span className="text-sm text-gray-500 dark:text-gray-400">또는</span>
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700"></div>
          </div>

          {/* 소셜 로그인 + 지갑 로그인 */}
          <div className="space-y-3">
            <Button type="button" variant="outline" className="w-full" onClick={() => handleSocialLogin("kakao")}>
              카카오로 계속하기
            </Button>

            <Button type="button" variant="outline" className="w-full" onClick={() => handleSocialLogin("google")}>
              구글로 계속하기
            </Button>

            {/* 지갑 로그인: WalletLoginButton이 nonce 발급 → 서명 → 백엔드 검증 전부 처리 */}
          
          </div>

          {/* 회원가입 링크 */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              계정이 없으신가요?{" "}
              <Link to="/signup" className="font-bold text-cyan-600 dark:text-cyan-400">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}