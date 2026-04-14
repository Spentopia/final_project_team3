// domains/auth/ui/SignupPendingPage.tsx
//
// 이메일 인증이 필요한 회원가입 직후 보여주는 안내 페이지.
// 이메일 인증 전에는 아직 로그인 상태가 아니므로,
// 프로필 입력 단계로 보내지 않고 여기서 안내만 한다.

import { useNavigate } from "react-router";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { MailCheck, Sparkles } from "lucide-react";

export default function SignupPendingPage() {
  const navigate = useNavigate();

  const openNaverMail = () => {
    window.open("https://mail.naver.com", "_blank");
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-purple-500 via-pink-500 to-blue-500 dark:from-purple-900 dark:via-pink-900 dark:to-blue-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500 shadow-lg">
              <MailCheck className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              이메일 인증이 필요해요
            </h1>
            <p className="text-sm text-gray-600 dark:text-gray-400 leading-6">
              회원가입은 완료됐어요.
              <br />
              메일함에서 인증 링크를 눌러주세요.
            </p>
          </div>

          <div className="mb-6 rounded-lg border border-purple-200 dark:border-purple-700 bg-purple-50 dark:bg-purple-900/30 p-4">
            <div className="mb-2 flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-purple-500" />
              <p className="text-sm font-semibold text-purple-900 dark:text-purple-100">
                안내
              </p>
            </div>
            <p className="text-sm text-purple-700 dark:text-purple-300 leading-6">
              메일 링크는 다른 브라우저에서 열릴 수 있어요.
              <br />
              인증 완료 후 다시 로그인하면 서비스를 이용할 수 있어요.
            </p>
          </div>

          <div className="space-y-3">
            <Button
              type="button"
              onClick={openNaverMail}
              className="w-full bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600"
            >
              네이버 메일 열기
            </Button>

            <Button
              type="button"
              variant="outline"
              onClick={() => navigate("/login")}
              className="w-full"
            >
              로그인하러 가기
            </Button>
          </div>
        </div>
      </Card>
    </div>
  );
}