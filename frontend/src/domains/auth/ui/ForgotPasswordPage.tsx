// domains/auth/ui/ForgotPasswordPage.tsx
// ─────────────────────────────────────────────────────────────
// 비밀번호 찾기 페이지
//
// 흐름:
// 1) 유저가 이메일 입력 → "재설정 링크 보내기" 버튼 클릭
// 2) Turnstile 사람 인증 통과
// 3) 백엔드에서 해당 이메일이 DB에 있는지 확인
//    (없는 이메일에 발송하는 걸 방지)
// 4) DB에 있으면 Supabase가 비밀번호 재설정 링크를 이메일로 발송
// 5) 해당 이메일 서비스 열기 버튼 + 안내 메시지 표시
// 6) 유저가 이메일의 링크 클릭 → /reset-password 페이지로 이동

import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { resetPassword } from "@/domains/auth/api/auth";
import { validateEmail } from "@/domains/auth/lib/email";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles, MailCheck } from "lucide-react";
import { toast } from "sonner";

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

// Turnstile 전역 타입 선언
declare global {
  interface Window {
    turnstile?: {
      render: (
        container: string | HTMLElement,
        options: {
          sitekey: string;
          callback?: (token: string) => void;
          "expired-callback"?: () => void;
          "error-callback"?: () => void;
        }
      ) => string;
      reset: (widgetId?: string) => void;
      remove: (widgetId?: string) => void;
    };
  }
}

const TURNSTILE_SITE_KEY = import.meta.env.VITE_TURNSTILE_SITE_KEY;

export default function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);

  // 이메일 발송 완료 여부 — true면 입력 폼 대신 안내 메시지 표시
  const [sent, setSent] = useState(false);

  // Turnstile 토큰
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);

  // 위젯 id / container ref
  const widgetIdRef = useRef<string | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

  const mailService = getMailService(email);

  // Turnstile 스크립트 로드 + 위젯 렌더
  useEffect(() => {
    const scriptId = "cf-turnstile-script";

    const renderWidget = () => {
      if (!window.turnstile || !containerRef.current || widgetIdRef.current) {
        return;
      }

      widgetIdRef.current = window.turnstile.render(containerRef.current, {
        sitekey: TURNSTILE_SITE_KEY,
        callback: (token: string) => {
          setCaptchaToken(token);
        },
        "expired-callback": () => {
          setCaptchaToken(null);
        },
        "error-callback": () => {
          setCaptchaToken(null);
        },
      });
    };

    const existingScript = document.getElementById(scriptId);
    if (existingScript) {
      renderWidget();
      return;
    }

    const script = document.createElement("script");
    script.id = scriptId;
    script.src =
      "https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit";
    script.async = true;
    script.defer = true;
    script.onload = renderWidget;
    document.head.appendChild(script);

    return () => {
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.remove(widgetIdRef.current);
        widgetIdRef.current = null;
      }
    };
  }, []);

  const resetCaptcha = () => {
    setCaptchaToken(null);

    if (widgetIdRef.current && window.turnstile) {
      window.turnstile.reset(widgetIdRef.current);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    const emailError = validateEmail(email);
    if (emailError) {
      toast.error(emailError);
      return;
    }

    if (!captchaToken) {
      toast.error("사람 인증을 먼저 완료해주세요.");
      return;
    }

    setLoading(true);

    try {
      // 1) 백엔드에서 이메일 존재 여부 + Turnstile 검증
      // 2) 있으면 Supabase가 재설정 링크 이메일 발송
      await resetPassword(email, captchaToken);
      setSent(true); // 발송 완료 → 안내 메시지로 전환
    } catch (error: any) {
      if (error.response?.status === 403) {
        toast.error(
          "소셜 로그인 계정은 비밀번호 재설정을 할 수 없습니다. 해당 소셜 로그인으로 다시 로그인해주세요."
        );
      } else {
        toast.error(error.message || "이메일 발송 실패");
      }

      resetCaptcha();
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
                <span className="font-semibold text-cyan-600 dark:text-cyan-400">
                  {email}
                </span>
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
                  type="text"
                  placeholder="your@email.com"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  className="mt-1"
                />
              </div>

              {/* Turnstile 위젯 */}
              <div className="pt-2">
                <div ref={containerRef} />
              </div>

              <Button
                type="submit"
                disabled={loading || !captchaToken}
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading ? "발송 중..." : "재설정 링크 보내기"}
              </Button>

              <div className="text-center">
                <Link
                  to="/login"
                  className="text-sm text-gray-500 dark:text-gray-400 hover:text-cyan-600"
                >
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