import { useState, useEffect } from "react";
import type { SubmitEvent as ReactSubmitEvent } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sun, Moon } from "lucide-react";
import { toast } from "sonner";
import {
  login,
  signInWithGoogle,
  redirectToKakao,
  AccountInactiveError,
  type AccountInactiveErrorPayload,
} from "@/domains/auth/api/auth";
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

  // 비활성 계정 로그인 실패 시 상세 안내 박스에 표시할 정보.
  //
  // 백엔드가 ACCOUNT_INACTIVE JSON 에러를 내려주면
  // auth.ts에서 AccountInactiveError로 변환하고,
  // 여기 catch에서 payload를 이 state에 저장한다.
  //
  // 이 값이 있으면 로그인 폼 위에:
  // - 사유
  // - 해제 예정일
  // - 문의 이메일
  // 을 표시한다.
  const [inactiveInfo, setInactiveInfo] =
      useState<AccountInactiveErrorPayload | null>(null);

  useEffect(() => {
    const raw = sessionStorage.getItem("account_inactive_info");

    if (!raw) return;

    try {
      const parsed = JSON.parse(raw) as AccountInactiveErrorPayload;

      if (parsed.code === "ACCOUNT_INACTIVE") {
        setInactiveInfo(parsed);
      }
    } catch (error) {
      console.warn("비활성 계정 안내 정보 파싱 실패:", error);
    } finally {
      sessionStorage.removeItem("account_inactive_info");
    }
  }, []);

  const loginButtonClass = "w-full rounded-xl spentopia-light-nft-button";

  const handleLogin = async (e: ReactSubmitEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 새 로그인 시도를 시작할 때 이전 비활성 안내 박스를 지운다.
    //
    // 예:
    // 1. A 계정으로 로그인 실패 → 비활성 안내 표시
    // 2. B 계정으로 다시 로그인 시도
    //
    // 이때 A 계정의 비활성 안내가 계속 남아 있으면 UX가 이상하므로 초기화한다.
    setInactiveInfo(null);

    const emailError = validateEmail(email);
    if (emailError) {
      toast.error(emailError);
      return;
    }

    try {
      const result = await login({ email, password });

      sessionStorage.setItem("just_logged_in", "true");

      // 실제 앱 JWT 저장.
      //
      // Supabase access token이 아니라 백엔드에서 발급한 우리 앱 access token이다.
      authStorage.setToken(result.accessToken);

      // 운영자면 관리자 페이지로 이동.
      if (result.roleType === "admin") {
        navigate("/admin", { replace: true });
        return;
      }

      navigate("/", { replace: true });
    } catch (error) {
      // 비활성 계정 전용 에러.
      //
      // 백엔드 응답 예:
      // {
      //   code: "ACCOUNT_INACTIVE",
      //   message: "비활성화된 계정입니다.",
      //   reason: "...",
      //   inactive_until_text: "...",
      //   support_email: "spentopia.official@gmail.com"
      // }
      //
      // toast에는 짧은 메시지만 띄우고,
      // 상세 내용은 로그인 폼 위 안내 박스에 표시한다.
      if (error instanceof AccountInactiveError) {
        setInactiveInfo(error.payload);
        toast.error(error.payload.message);
        return;
      }

      // 일반 로그인 실패.
      //
      // 예:
      // - 이메일/비밀번호 불일치
      // - 탈퇴 계정
      // - 서버 오류
      setInactiveInfo(null);

      const message = getErrorMessage(error, "알 수 없는 오류");
      toast.error(`로그인 실패: ${message}`);
    }
  };

  const handleSocialLogin = async (provider: SocialLoginProvider) => {
    try {
      // 소셜 로그인 버튼을 누를 때도 이전 비활성 안내는 지운다.
      //
      // 실제 구글/카카오 콜백에서 ACCOUNT_INACTIVE가 발생하면
      // 콜백 페이지에서 처리해야 한다.
      setInactiveInfo(null);

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
      <div className="relative flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(125,211,252,0.18),transparent_34%),radial-gradient(circle_at_bottom_left,rgba(37,99,235,0.08),transparent_30%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-[#090b16] dark:bg-none">
        {/* 테마 토글 — 우상단 고정 */}
        <button
            type="button"
            aria-label="테마 변경"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
            className="absolute right-5 top-5 flex h-9 w-9 items-center justify-center rounded-full bg-white/70 text-[#1e3a8a] shadow-sm ring-1 ring-blue-100 backdrop-blur-sm transition hover:bg-white dark:bg-white/20 dark:text-white dark:hover:bg-white/30"
        >
          <Sun className="h-4 w-4 dark:hidden" />
          <Moon className="hidden h-4 w-4 dark:block" />
        </button>

        <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 shadow-2xl backdrop-blur-xl dark:bg-[#0b1020]/95 dark:shadow-black/40">
          <div className="p-8">
            {/* Logo */}
            <div className="mb-8 flex flex-col items-center">
              <img src="/favicon.svg" alt="Spentopia" className="mb-4 h-16 w-16" />

              <h1 className="mb-2 text-3xl font-bold text-[#1e3a8a] dark:text-white">
                Spentopia
              </h1>

              <p className="text-center text-[#52647e] dark:text-gray-400">
                지출을 관리하면 열리는 나만의 세계
              </p>
            </div>

            {/* 비활성 계정 상세 안내 */}
            {inactiveInfo && (
                <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 p-4 text-sm text-red-900 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-100">
                  <p className="font-bold">계정 이용이 제한되었습니다.</p>

                  <div className="mt-3 space-y-1.5">
                    <p>
                      <span className="font-semibold">사유: </span>
                      {inactiveInfo.reason || "운영정책 위반"}
                    </p>

                    <p>
                      <span className="font-semibold">해제 예정일: </span>
                      {inactiveInfo.inactive_until_text || "미정"}
                    </p>

                    <p>
                      <span className="font-semibold">문의 이메일: </span>
                      <span>{inactiveInfo.support_email}</span>
                    </p>
                  </div>
                </div>
            )}

            {/* Login Form */}
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <Label htmlFor="email" className="text-gray-700 dark:text-gray-300">
                  이메일
                </Label>

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
                <Label
                    htmlFor="password"
                    className="text-gray-700 dark:text-gray-300"
                >
                  비밀번호
                </Label>

                <PasswordInput
                    id="password"
                    placeholder="비밀번호를 입력해주세요"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="mt-1 dark:border-gray-600 dark:bg-gray-800 dark:text-white dark:placeholder-gray-500"
                />
              </div>

              <Button type="submit" variant="outline" className={loginButtonClass}>
                로그인
              </Button>
            </form>

            {/* Divider */}
            <div className="my-6 flex items-center gap-4">
              <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
              <span className="text-sm text-gray-400 dark:text-gray-500">
              또는
            </span>
              <div className="h-px flex-1 bg-gray-200 dark:bg-gray-700" />
            </div>

            {/* Social Login */}
            <div className="space-y-3">
              <Button
                  type="button"
                  variant="outline"
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

              <WalletLoginButton className="relative h-12 w-full rounded-xl bg-gradient-to-r from-[#090b16] via-[#111827] to-[#2d1847] px-4 text-sm font-semibold text-white shadow-lg shadow-[#0B1020]/30 hover:opacity-90 hover:shadow-[#2D1847]/40 disabled:opacity-50 dark:bg-gradient-to-r dark:from-[#0B1020] dark:via-[#111827] dark:to-[#2D1847] dark:text-white dark:shadow-[#0B1020]/25 dark:hover:brightness-110" />
            </div>

            {/* 이메일/비밀번호 찾기 */}
            <div className="mt-4 flex justify-center gap-4 text-sm text-gray-400 dark:text-gray-500">
              <Link
                  to="/find-email"
                  className="transition-colors hover:text-[#2563eb] dark:hover:text-cyan-400"
              >
                이메일 찾기
              </Link>

              <span>|</span>

              <Link
                  to="/forgot-password"
                  className="transition-colors hover:text-[#2563eb] dark:hover:text-cyan-400"
              >
                비밀번호 찾기
              </Link>
            </div>

            {/* Sign Up */}
            <div className="mt-6 text-center">
              <p className="text-sm text-gray-500 dark:text-gray-400">
                계정이 없으신가요?{" "}

                <Link
                    to="/signup"
                    className="font-bold text-[#2563eb] hover:underline dark:text-cyan-400"
                >
                  회원가입
                </Link>
              </p>
            </div>
          </div>
        </Card>
      </div>
  );
}
