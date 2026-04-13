// domains/auth/ui/FindEmailPage.tsx
// ─────────────────────────────────────────────────────────────
// 이메일 찾기 페이지
// 전화번호 입력 → 백엔드 API(/auth/find-email) 호출
// → 마스킹된 이메일 반환 (예: te***@gmail.com)
//
// 왜 백엔드를 거치나?
// RLS 때문에 프론트에서 다른 유저의 이메일을 직접 조회할 수 없음
// 백엔드가 service_role로 조회 → 마스킹해서 안전하게 반환

import { useState } from "react";
import { Link } from "react-router";
import { findEmailByPhone } from "@/domains/auth/api/auth";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

export default function FindEmailPage() {
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);
  const [maskedEmail, setMaskedEmail] = useState<string | null>(null); // 결과

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setLoading(true);

    try {
      const result = await findEmailByPhone(phone);
      setMaskedEmail(result); // "te***@gmail.com"
    } catch (error: any) {
      alert(error.message || "이메일 찾기 실패");
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
              이메일 찾기
            </h1>
          </div>

          {/* 결과가 있으면 마스킹된 이메일 표시, 없으면 입력 폼 */}
          {maskedEmail ? (
            <div className="text-center space-y-4">
              <p className="text-gray-600 dark:text-gray-400">
                등록된 이메일:
              </p>
              <p className="text-xl font-bold text-cyan-600 dark:text-cyan-400">
                {maskedEmail}
              </p>
              <div className="flex gap-3 pt-4">
                <Link to="/login" className="flex-1">
                  <Button className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600">
                    로그인하러 가기
                  </Button>
                </Link>
                <Link to="/forgot-password" className="flex-1">
                  <Button variant="outline" className="w-full">
                    비밀번호 찾기
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <Label htmlFor="phone">가입 시 입력한 전화번호</Label>
                <Input
                  id="phone"
                  type="tel"
                  placeholder="010-1234-5678"
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  required
                  className="mt-1"
                />
              </div>

              <Button
                type="submit"
                disabled={loading}
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading ? "찾는 중..." : "이메일 찾기"}
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