// domains/auth/ui/ForgotPasswordPage.tsx
// ─────────────────────────────────────────────────────────────
// 비밀번호 찾기 페이지
//
// 흐름:
// 1) 유저가 이메일 입력 → "재설정 링크 보내기" 버튼 클릭
// 2) 먼저 백엔드에서 해당 이메일이 DB에 있는지 확인
//    (없는 이메일에 발송하는 걸 방지)
// 3) DB에 있으면 Supabase가 비밀번호 재설정 링크를 이메일로 발송
// 4) 해당 이메일 서비스 열기 버튼 + 안내 메시지 표시
// 5) 유저가 이메일의 링크 클릭 → /reset-password 페이지로 이동

import { useState } from "react";
import { Link } from "react-router";
import { resetPassword } from "@/domains/auth/api/auth";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles, MailCheck } from "lucide-react";

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

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  // 이메일 발송 완료 여부 — true면 입력 폼 대신 안내 메시지 표시
  const [sent, setSent] = useState(false);

  const mailService = getMailService(email);

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

  const openMailService = () => {
    if (mailService) {
      window.open(mailService.url, "_blank");
    }
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500 dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 shadow-lg">
              {sent ? (
                <MailCheck className="h-8 w-8 text-white" />
              ) : (
                <Sparkles className="h-8 w-8 text-white" />
              )}
            </div>
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
              {sent ? "메일을 보냈어요" : "비밀번호 찾기"}
            </h1>
          </div>

          {/* 발송 완료 시 안내 메시지 + 메일 서비스 열기 버튼 */}
          {sent ? (
            <div className="space-y-4">
              <p className="text-center text-gray-600 dark:text-gray-400">
                <span className="font-semibold text-cyan-600 dark:text-cyan-400">{email}</span>
                으로
                <br />
                비밀번호 재설정 링크를 보냈습니다.
              </p>

              <div className="rounded-lg border border-cyan-200 dark:border-cyan-700 bg-cyan-50 dark:bg-cyan-900/30 p-4">
                <div className="mb-2 flex items-center gap-2">
                  <Sparkles className="h-4 w-4 text-purple-500" />
                  <p className="text-sm font-semibold text-purple-900 dark:text-purple-100">
                    안내
                  </p>
                </div>
                <p className="text-sm text-purple-700 dark:text-purple-300 leading-6">
                  메일에서 재설정 링크를 클릭하면
                  <br />
                  새 비밀번호를 설정할 수 있어요.
                </p>
              </div>

              <div className="space-y-3 pt-2">
                {mailService ? (
                  <Button
                    type="button"
                    onClick={openMailService}
                    className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
                  >
                    {mailService.name} 열기
                  </Button>
                ) : (
                  <p className="text-center text-sm text-gray-500 dark:text-gray-400">
                    메일함에서 재설정 링크를 확인해주세요.
                  </p>
                )}

                <Link to="/login">
                  <Button variant="outline" className="w-full">
                    로그인으로 돌아가기
                  </Button>
                </Link>
              </div>
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