// domains/auth/ui/ForgotPasswordPage.tsx
// ─────────────────────────────────────────────────────────────
// 비밀번호 찾기 페이지
//
// 흐름:
// 1) 유저가 이메일 입력 → "재설정 링크 보내기" 버튼 클릭
// 2) 먼저 백엔드에서 해당 이메일이 DB에 있는지 확인
//    (없는 이메일에 발송하는 걸 방지)
// 3) DB에 있으면 Supabase가 비밀번호 재설정 링크를 이메일로 발송
// 4) 화면에 "이메일을 확인하세요" 안내 메시지 표시
// 5) 유저가 이메일의 링크 클릭 → /reset-password 페이지로 이동

import { useState } from "react";
import { Link } from "react-router";
import { resetPassword } from "@/domains/auth/api/auth";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  // 이메일 발송 완료 여부 — true면 입력 폼 대신 안내 메시지 표시
  const [sent, setSent] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault(); // form 기본 동작(페이지 새로고침) 방지
    setLoading(true);

    try {
      // 1) 백엔드에서 이메일 존재 여부 확인
      // 2) 있으면 Supabase가 재설정 링크 이메일 발송
      await resetPassword(email);
      setSent(true); // 발송 완료 → 안내 메시지로 전환
    } catch (error: any) {
      // "해당 이메일로 가입된 계정이 없습니다" 등
      alert(error.message || "이메일 발송 실패");
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
              비밀번호 찾기
            </h1>
          </div>

          {/* 발송 완료 시 안내 메시지, 미완료 시 이메일 입력 폼 */}
          {sent ? (
            <div className="text-center space-y-4">
              <p className="text-gray-600 dark:text-gray-400">
                <span className="font-bold text-cyan-600">{email}</span>으로
                비밀번호 재설정 링크를 보냈습니다.
              </p>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                이메일을 확인하고 링크를 클릭해주세요.
              </p>
              <Link to="/login">
                <Button variant="outline" className="w-full mt-4">
                  로그인으로 돌아가기
                </Button>
              </Link>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="email">가입한 이메일</Label>
                <Input
                  id="email"
                  type="email"
                  placeholder="your@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="mt-1"
                />
              </div>

              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading ? "발송 중..." : "재설정 링크 보내기"}
              </Button>

              <div className="text-center">
                <Link to="/login" className="text-sm text-gray-500 dark:text-gray-400 hover:text-cyan-600">
                  로그인으로 돌아가기
                </Link>
              </div>
            </form>
          )}
        </div>
      </Card>
    </div>
  );
}