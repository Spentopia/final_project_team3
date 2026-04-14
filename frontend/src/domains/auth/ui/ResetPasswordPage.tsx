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
import { updatePassword } from "@/domains/auth/api/auth";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

export default function ResetPasswordPage() {
  const navigate = useNavigate();
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // 비밀번호 확인 체크
    if (password !== confirmPassword) {
      alert("비밀번호가 일치하지 않습니다");
      return;
    }

    setLoading(true);
    try {
      // Supabase updateUser()로 비밀번호 변경
      // 이미 임시 세션이 활성화되어 있어서 별도 인증 불필요
      await updatePassword(password);
      alert("비밀번호가 변경되었습니다!");
      navigate("/login"); // 변경 완료 → 로그인 페이지로
    } catch (error: any) {
      alert(error.message || "비밀번호 변경 실패");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500 dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 shadow-lg">
              <Sparkles className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
              새 비밀번호 설정
            </h1>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <Label htmlFor="password">새 비밀번호</Label>
              <Input
                id="password"
                type="password"
                placeholder="8자 이상 입력해주세요"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                minLength={8}
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="confirmPassword">새 비밀번호 확인</Label>
              <Input
                id="confirmPassword"
                type="password"
                placeholder="비밀번호를 다시 입력해주세요"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
                className="mt-1"
              />
            </div>

            <Button
              type="submit"
              disabled={loading}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
            >
              {loading ? "변경 중..." : "비밀번호 변경"}
            </Button>
          </form>
        </div>
      </Card>
    </div>
  );
}