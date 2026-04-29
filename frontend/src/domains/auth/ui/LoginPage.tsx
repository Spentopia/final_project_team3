import { useState } from "react";
import type { SubmitEvent as ReactSubmitEvent } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles, Sun, Moon } from "lucide-react";
import { toast } from "sonner";
import { login, signInWithGoogle, redirectToKakao } from "@/domains/auth/api/auth";
import { validateEmail } from "@/domains/auth/lib/email";
import PasswordInput from "@/domains/auth/ui/PasswordInput";
import { authStorage } from "@/shared/lib/auth";
import { WalletLoginButton } from "@/domains/auth/ui/WalletLoginButton";
import { useTheme } from "next-themes";

type SocialLoginProvider = "google" | "kakao";

const getErrorMessage = (error: unknown, fallback: string) => {
  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
};

export default function Login() {
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const { theme, setTheme } = useTheme();

  const handleLogin = async (e: ReactSubmitEvent<HTMLFormElement>) => {
    e.preventDefault();

    const emailError = validateEmail(email);
    if (emailError) {
      toast.error(emailError);
      return;
    }

    try {
      const result = await login({ email, password });

      sessionStorage.setItem("just_logged_in", "true");
      // ✅ 실제 앱 JWT 저장
      authStorage.setToken(result.accessToken);

      navigate("/", { replace: true });
    } catch (error) {
      const message = getErrorMessage(error, "알 수 없는 오류");
      toast.error(`로그인 실패: ${message}`);
    }
  };

  const handleSocialLogin = async (provider: SocialLoginProvider) => {
    try {
      if (provider === "google") {
        await signInWithGoogle();
      } else if (provider === "kakao") {
        await redirectToKakao();
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "소셜 로그인 실패"));
    }
  };

  return (
    <div className="relative flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-400 via-blue-500 to-teal-500 dark:from-gray-950 dark:via-blue-950 dark:to-gray-900 p-4">

      {/* 테마 토글 — 우상단 고정 */}
      <button
        type="button"
        aria-label="테마 변경"
        onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        className="absolute right-5 top-5 flex h-9 w-9 items-center justify-center rounded-full bg-white/20 text-white backdrop-blur-sm transition hover:bg-white/30"
      >
        <Sun className="h-4 w-4 dark:hidden" />
        <Moon className="hidden h-4 w-4 dark:block" />
      </button>

      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/90 backdrop-blur-xl shadow-2xl">
        <div className="p-8">

          {/* Logo */}
          <div className="mb-8 flex flex-col items-center">
            <img src="/favicon.svg" alt="Spentopia" className="mb-4 h-16 w-16" />
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-white">
              Spentopia
            </h1>
            <p className="text-center text-gray-500 dark:text-gray-400">
              내가 기록한 소비가 나를 만든다
            </p>
          </div>

          {/* Login Form */}
          <form onSubmit={handleLogin} className="space-y-4">
            <div>
              <Label htmlFor="email" className="text-gray-700 dark:text-gray-300">이메일</Label>
              <Input
                id="email"
                type="text"
                placeholder="이메일을 입력해주세요"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="mt-1 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:placeholder-gray-500"
              />
            </div>

            <div>
              <Label htmlFor="password" className="text-gray-700 dark:text-gray-300">비밀번호</Label>
              <PasswordInput
                id="password"
                placeholder="비밀번호를 입력해주세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:placeholder-gray-500"
              />
            </div>

            <Button
              type="submit"
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600"
            >
              로그인
            </Button>
          </form>

          {/* Divider */}
          <div className="my-6 flex items-center gap-4">
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
            <span className="text-sm text-gray-400 dark:text-gray-500">또는</span>
            <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
          </div>

          {/* Social Login */}
          <div className="space-y-3">
            <Button
              type="button"
              className="relative h-12 w-full rounded-xl border-0 bg-[#FEE500] text-[#191600] hover:bg-[#FEE500] dark:bg-[#FEE500] dark:text-[#191600] dark:hover:bg-[#FEE500]"
              onClick={() => handleSocialLogin("kakao")}
            >
              <img
                src="/login-kakao.png"
                alt=""
                aria-hidden="true"
                className="absolute left-4 h-7 w-7 object-contain"
              />
              카카오 로그인
            </Button>

            <Button
              type="button"
              variant="outline"
              className="relative h-12 w-full rounded-xl border border-gray-200 bg-white text-gray-900 hover:bg-gray-50 dark:border-gray-700 dark:bg-white dark:text-gray-900 dark:hover:bg-gray-50"
              onClick={() => handleSocialLogin("google")}
            >
              <img
                src="/login-google.png"
                alt=""
                aria-hidden="true"
                className="absolute left-5 h-5 w-5 object-contain"
              />
              구글 로그인
            </Button>

            <WalletLoginButton className="relative h-12 w-full rounded-xl border-0 bg-gradient-to-r from-[#111827] via-[#15152A] to-[#32145D] px-4 text-sm font-semibold text-white shadow-sm hover:from-[#0F172A] hover:via-[#17172F] hover:to-[#3B1770] disabled:opacity-50" />
          </div>

          {/* 이메일/비밀번호 찾기 */}
          <div className="mt-4 flex justify-center gap-4 text-sm text-gray-400 dark:text-gray-500">
            <Link to="/find-email" className="hover:text-cyan-600 dark:hover:text-cyan-400 transition-colors">
              이메일 찾기
            </Link>
            <span>|</span>
            <Link to="/forgot-password" className="hover:text-cyan-600 dark:hover:text-cyan-400 transition-colors">
              비밀번호 찾기
            </Link>
          </div>

          {/* Sign Up */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500 dark:text-gray-400">
              계정이 없으신가요?{" "}
              <Link to="/signup" className="font-bold text-cyan-600 dark:text-cyan-400 hover:underline">
                회원가입
              </Link>
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
