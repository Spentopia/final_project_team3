// domains/auth/ui/SignupPendingPage.tsx
//
// 이메일 인증이 필요한 회원가입 직후 보여주는 안내 페이지.
// 이메일 인증 전에는 아직 로그인 상태가 아니므로,
// 프로필 입력 단계로 보내지 않고 여기서 안내만 한다.
//
// SignupPage에서 navigate state로 email을 받아와서
// 도메인에 맞는 메일 서비스 버튼을 보여준다.

import { useNavigate, useLocation } from "react-router";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { MailCheck, Sparkles } from "lucide-react";

// 이메일 도메인 → 메일 서비스 매핑
const MAIL_SERVICES: Record<string, { name: string; url: string }> = {
  "gmail.com": { name: "Gmail", url: "https://mail.google.com" },
  "naver.com": { name: "네이버 메일", url: "https://mail.naver.com" },
  "daum.net": { name: "다음 메일", url: "https://mail.daum.net" },
  "hanmail.net": { name: "한메일", url: "https://mail.daum.net" },
  "kakao.com": { name: "카카오 메일", url: "https://mail.kakao.com" },
  "nate.com": { name: "네이트 메일", url: "https://mail.nate.com" },
  "outlook.com": { name: "Outlook", url: "https://outlook.live.com" },
  "hotmail.com": { name: "Outlook", url: "https://outlook.live.com" },
  "yahoo.com": { name: "Yahoo Mail", url: "https://mail.yahoo.com" },
};

function getMailService(email: string) {
  const domain = email.split("@")[1]?.toLowerCase();
  if (!domain) return null;
  return MAIL_SERVICES[domain] ?? null;
}

export default function SignupPendingPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const email = (location.state as { email?: string })?.email ?? "";
  const mailService = email ? getMailService(email) : null;

  const openMailService = () => {
    if (mailService) {
      window.location.href = mailService.url;
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.16),transparent_34%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-gradient-to-br dark:from-purple-900 dark:via-pink-900 dark:to-blue-900">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#60a5fa] to-[#2563eb] shadow-lg shadow-blue-500/20">
              <MailCheck className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
              이메일 인증이 필요해요
            </h1>
            
          </div>

          <div className="mb-6 rounded-lg border border-[#bfdbfe] bg-[#eff6ff] dark:border-cyan-700 dark:bg-cyan-900/30 p-4">
            <div className="mb-2 flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-[#2563eb]" />
              <p className="text-sm font-semibold text-[#1e3a8a] dark:text-purple-100">
                안내
              </p>
            </div>
            <p className="text-sm text-[#2563eb] dark:text-purple-300 leading-6">
              메일 링크는 다른 브라우저에서 열릴 수 있어요.
              <br />
              인증 완료 후 다시 로그인하면 서비스를 이용할 수 있어요.
            </p>
          </div>

          <div className="space-y-3">
            {mailService ? (
              <Button
                type="button"
                onClick={openMailService}
                variant="outline"
                className="w-full spentopia-light-nft-button"
              >
                이메일 열기
              </Button>
            ) : (
              <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                메일함에서 인증 링크를 확인해주세요.
              </p>
            )}

            
            <button
              type="button"
              onClick={() => navigate("/login")}
              className="block mx-auto text-sm text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 underline underline-offset-2 transition-colors"
            >
             다른 계정으로 로그인하기
            </button>
          
          </div>
        </div>
      </Card>
    </div>
  );
}
