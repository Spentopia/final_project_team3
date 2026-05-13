// domains/auth/ui/EmailConfirmedPage.tsx
//
// 3가지 진입 케이스를 처리:
// 1) ?sent=true        → 이메일 변경 요청 직후 (인증 메일 발송됨 안내)
// 2) #type=email_change → SMTP 링크 클릭 후 (변경 완료 안내 → 로그인)
// 3) 기본              → 회원가입 이메일 인증 완료

import { useEffect, useState } from "react";
import { useNavigate } from "react-router";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { CheckCircle2, Mail, Sparkles } from "lucide-react";

export default function EmailConfirmedPage() {
  const navigate = useNavigate();
  const [mode, setMode] = useState<"sent" | "email_change" | "signup">("signup");

  useEffect(() => {
    const searchParams = new URLSearchParams(window.location.search);
    const hashParams = new URLSearchParams(window.location.hash.replace("#", ""));

    if (searchParams.get("sent") === "true") {
      setMode("sent");
    } else if (hashParams.get("type") === "email_change") {
      setMode("email_change");
    } else {
      setMode("signup");
    }
  }, []);

  // 이메일 변경 요청 직후 — 메일함 확인 안내
  if (mode === "sent") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.16),transparent_34%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-gradient-to-br dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900">
        <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
          <div className="p-8">
            <div className="mb-8 flex flex-col items-center text-center">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#60a5fa] to-[#2563eb] shadow-lg shadow-blue-500/20">
                <Mail className="h-8 w-8 text-white" />
              </div>
              <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
                인증 메일 발송됨
              </h1>
              <p className="text-sm text-gray-600 dark:text-gray-400 leading-6">
                새 이메일로 인증 메일을 보냈습니다.
                <br />
                메일함을 확인하고 링크를 클릭해주세요.
              </p>
            </div>

            <div className="mb-6 rounded-lg border border-blue-200 dark:border-blue-700 bg-blue-50 dark:bg-blue-900/30 p-4">
              <div className="mb-2 flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-blue-600" />
                <p className="text-sm font-semibold text-blue-900 dark:text-blue-100">
                  링크 클릭 후 새 이메일로 로그인하세요
                </p>
              </div>
              <p className="text-sm text-blue-700 dark:text-blue-300">
                메일이 오지 않으면 스팸함도 확인해보세요.
              </p>
            </div>

            <Button
              type="button"
              variant="outline"
              onClick={() => navigate("/profile")}
              className="w-full"
            >
              마이페이지로 돌아가기
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  // SMTP 링크 클릭 후 — 변경 완료
  if (mode === "email_change") {
    return (
      <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.16),transparent_34%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-gradient-to-br dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900">
        <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
          <div className="p-8">
            <div className="mb-8 flex flex-col items-center text-center">
              <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#60a5fa] to-[#2563eb] shadow-lg shadow-blue-500/20">
                <CheckCircle2 className="h-8 w-8 text-white" />
              </div>
              <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
                이메일 변경 완료
              </h1>
              <p className="text-sm text-gray-600 dark:text-gray-400 leading-6">
                새 이메일 인증이 완료되었습니다.
                <br />
                새 이메일로 다시 로그인해주세요.
              </p>
            </div>

            <div className="mb-6 rounded-lg border border-green-200 dark:border-green-700 bg-green-50 dark:bg-green-900/30 p-4">
              <div className="mb-2 flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-green-600" />
                <p className="text-sm font-semibold text-green-900 dark:text-green-100">
                  변경이 완료됐어요
                </p>
              </div>
              <p className="text-sm text-green-700 dark:text-green-300">
                이제 새 이메일 주소로 로그인하면 마이페이지에서 확인할 수 있어요.
              </p>
            </div>

            <Button
              type="button"
              onClick={() => navigate("/login")}
              variant="outline"
              className="w-full spentopia-light-nft-button"
            >
              로그인하러 가기
            </Button>
          </div>
        </Card>
      </div>
    );
  }

  // 기본 — 회원가입 이메일 인증 완료
  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.16),transparent_34%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-gradient-to-br dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#60a5fa] to-[#2563eb] shadow-lg shadow-blue-500/20">
              <CheckCircle2 className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              이메일 인증 완료
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 leading-6">
              인증이 성공적으로 끝났어요.
              <br />
              이제 로그인해서 서비스를 이용하면 됩니다.
            </p>
          </div>

          <div className="mb-6 rounded-lg border border-green-200 dark:border-green-700 bg-green-50 dark:bg-green-900/30 p-4">
            <div className="mb-2 flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-green-600" />
              <p className="text-sm font-semibold text-green-900 dark:text-green-100">
                거의 끝났어요
              </p>
            </div>
            <p className="text-sm text-green-700 dark:text-green-300">
              로그인 후 프로필을 입력하면 바로 메인으로 들어갈 수 있어요.
            </p>
          </div>

          <Button
            type="button"
            onClick={() => navigate("/login")}
            variant="outline"
            className="w-full spentopia-light-nft-button"
          >
            로그인하러 가기
          </Button>
        </div>
      </Card>
    </div>
  );
}
