// domains/auth/ui/EmailConfirmedPage.tsx
//
// 이메일 인증 링크 클릭 후 돌아왔을 때 보여줄 페이지.
// 여기서는 자동 로그인 시도하지 않고,
// "이제 로그인하세요" 흐름으로 안내한다.

import { useNavigate } from "react-router";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { CheckCircle2, Sparkles } from "lucide-react";

export default function EmailConfirmedPage() {
  const navigate = useNavigate();

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500 dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center text-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-400 to-blue-500 shadow-lg">
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
            className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
          >
            로그인하러 가기
          </Button>
        </div>
      </Card>
    </div>
  );
}