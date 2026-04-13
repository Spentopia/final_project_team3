import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";
import { login } from "@/domains/auth/api/auth";
import { authStorage } from "@/shared/lib/auth";

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();

    try {
      const result = await login({ email, password });

      // 🔥 핵심 1: 새 구조 토큰 저장
      authStorage.setToken(result.accessToken);

      // 🔥 핵심 2: 기존 앱 호환 (이거 없으면 튕김)
      localStorage.setItem("spentopia_auth", "mock_token");

      // 🔥 이동
      navigate("/");
    } catch (error) {
      alert("로그인 실패");
    }
  };

  const handleSocialLogin = (provider: string) => {
    // 🔥 소셜도 동일하게 처리
    authStorage.setToken("mock_token");
    localStorage.setItem("spentopia_auth", "mock_token");

    navigate("/");
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
            <Button type="button" variant="outline" className="w-full" onClick={() => handleSocialLogin("kakao")}>
              카카오로 계속하기
            </Button>

            <Button type="button" variant="outline" className="w-full" onClick={() => handleSocialLogin("naver")}>
              네이버로 계속하기
            </Button>

            <Button type="button" variant="outline" className="w-full" onClick={() => handleSocialLogin("google")}>
              구글로 계속하기
            </Button>
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