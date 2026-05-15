// domains/auth/ui/ResetPasswordPage.tsx
// ─────────────────────────────────────────────────────────────
// 새 비밀번호 설정 페이지
//
// 이 페이지에 도착하는 경로:
// 1) 유저가 ForgotPasswordPage에서 이메일 입력
// 2) Supabase가 비밀번호 재설정 링크를 이메일로 발송
// 3) 유저가 이메일의 링크를 클릭
// 4) Supabase가 링크에 세션 토큰을 포함시킨 채로 이 페이지로 리다이렉트
//
// 이 페이지에 도착했을 때:
// - Supabase SDK가 URL의 세션 토큰을 자동으로 읽어서 임시 세션 활성화
// - 별도 인증 없이 updateUser()로 바로 비밀번호 변경 가능
//
// ⚠️ 이 URL은 Supabase 대시보드 → Authentication → URL Configuration
//    → Redirect URLs에 등록되어 있어야 함

import { useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { updatePassword } from "@/domains/auth/api/auth";
import {
  validatePassword,
} from "@/domains/auth/lib/password";
import PasswordInput from "@/domains/auth/ui/PasswordInput";
import { Button } from "@/shared/ui/button";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setErrorMessage("");

    const passwordError = validatePassword(password);
    if (passwordError) {
      setErrorMessage(passwordError);
      return;
    }

    // 비밀번호 확인 체크
    if (password !== confirmPassword) {
      setErrorMessage("비밀번호가 일치하지 않습니다.");
      return;
    }

    setLoading(true);
    try {
      // Supabase updateUser()로 비밀번호 변경
      // 이미 임시 세션이 활성화되어 있어서 별도 인증 불필요
      // 변경 후 reset용 Supabase 임시 세션은 updatePassword() 내부에서 정리
      await updatePassword(password);
      toast.success("비밀번호 변경이 완료되었습니다.");
      navigate("/login"); // 변경 완료 → 로그인 페이지로
    } catch (error: any) {
      setErrorMessage(error.message || "비밀번호 변경 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(125,211,252,0.18),transparent_34%),radial-gradient(circle_at_bottom_left,rgba(37,99,235,0.08),transparent_30%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-gradient-to-br dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-[#60a5fa] to-[#2563eb] shadow-lg shadow-blue-500/20">
              <Sparkles className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
              새 비밀번호 설정
            </h1>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {errorMessage && (
              <div className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-600">
                {errorMessage}
              </div>
            )}

            <div>
              <Label htmlFor="password">새 비밀번호</Label>
              <PasswordInput
                id="password"
                placeholder="영문 대소문자, 숫자, 특수문자를 포함해 주세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="confirmPassword">새 비밀번호 확인</Label>
              <PasswordInput
                id="confirmPassword"
                placeholder="비밀번호를 다시 입력해주세요"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="mt-1"
              />
            </div>

            <Button
              type="submit"
              disabled={loading}
              variant="outline"
              className="w-full spentopia-light-nft-button"
            >
              {loading ? "변경 중..." : "비밀번호 변경"}
            </Button>

            <Button
              type="button"
              variant="outline"
              className="w-full spentopia-light-nft-button"
              onClick={() => navigate("/login")}
            >
              로그인 화면으로 이동
            </Button>
          </form>
        </div>
      </Card>
    </div>
  );
}
