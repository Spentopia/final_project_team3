import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";
import { login, signInWithGoogle, redirectToKakao } from "@/domains/auth/api/auth";
import { authStorage } from "@/shared/lib/auth";
import { WalletLoginButton } from "@/domains/auth/ui/WalletLoginButton";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const result = await login({ email, password });

      // ✅ 실제 앱 JWT 저장
      authStorage.setToken(result.accessToken);

      navigate("/");
    } catch (error) {
      const message = error instanceof Error ? error.message : "알 수 없는 오류";
      alert(`로그인 실패: ${message}`);
    }
  };

  const handleSocialLogin = async (provider: string) => {
    try {
      if (provider === "google") {
        await signInWithGoogle();
      } else if (provider === "kakao") {
        redirectToKakao();
      }
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

          {/* Login Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <Label htmlFor="email">이메일</Label>
              <Input
                id="email"
                type="email"
                placeholder="test@test.com"
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
                placeholder="Test1234!"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                className="mt-1"
              />
            </div>

            <Button
              type="submit"
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
            >
              로그인
            </Button>
          </form>

          {/* Divider */}
          <div className="my-6 flex items-center gap-4">
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700"></div>
            <span className="text-sm text-gray-500 dark:text-gray-400">또는</span>
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700"></div>
          </div>

          {/* Social Login */}
          <div className="space-y-3">
            <Button
              type="button"
              variant="outline"
              className="w-full border-gray-200 bg-white text-[#191600] shadow-sm hover:bg-gray-50"
              onClick={() => handleSocialLogin("kakao")}
            >
              카카오로 로그인
            </Button>

            <Button
              type="button"
              variant="outline"
              className="w-full border-gray-200 bg-white text-gray-800 shadow-sm hover:bg-gray-50"
              onClick={() => handleSocialLogin("google")}
            >
              구글로 로그인
            </Button>

            <WalletLoginButton className="w-full inline-flex items-center justify-center gap-2 rounded-md border border-gray-200 bg-white px-4 py-2 text-sm font-medium text-gray-800 shadow-sm hover:bg-gray-50 disabled:opacity-50" />
          </div>

          {/* 이메일/비밀번호 찾기 */}
          <div className="mt-4 flex justify-center gap-4 text-sm text-gray-500 dark:text-gray-400">
            <Link to="/find-email" className="hover:text-cyan-600 dark:hover:text-cyan-400">
              이메일 찾기
            </Link>
            <span>|</span>
            <Link to="/forgot-password" className="hover:text-cyan-600 dark:hover:text-cyan-400">
              비밀번호 찾기
            </Link>
          </div>

          {/* Sign Up */}
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
