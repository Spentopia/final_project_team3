// domains/auth/ui/FindEmailPage.tsx
// ─────────────────────────────────────────────────────────────
// 이메일 찾기 페이지
//
// 이메일을 까먹은 유저가 전화번호를 입력하면
// 백엔드가 public.users에서 해당 전화번호로 이메일을 찾아서
// 마스킹 처리해서 반환 (예: te***@gmail.com)
//
// 추가:
// - Cloudflare Turnstile 적용
// - 사람이 맞는지 확인한 뒤에만 /auth/find-email 호출
//
// 전화번호 입력:
// - 화면에서는 "010-1234-5678" 형식으로 자동 포맷팅
// - API 전송 시 auth.ts의 findEmailByPhone에서 숫자만 추출
// - DB에는 "01012345678"로 저장되어 있으므로 숫자로 검색
//
// 왜 백엔드를 거치나?
// 이메일 찾기는 로그인 전 상태에서 호출됨.
// 로그인 안 되어있으면 Supabase RLS가 auth.uid()를 모르므로
// 프론트에서 public.users 조회해도 아무 결과가 안 나옴.
// 백엔드는 service_role 키로 RLS를 우회할 수 있어서
// 전화번호로 조회 → 이메일을 마스킹해서 안전하게 반환.

import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { findEmailByPhone } from "@/domains/auth/api/auth";
import { formatPhone } from "@/shared/lib/phone";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

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

export default function FindEmailPage() {
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);

  // 조회 결과 — null이면 아직 조회 안 함, 값이 있으면 결과 표시
  const [maskedEmail, setMaskedEmail] = useState<string | null>(null);

  // Turnstile이 발급한 토큰
  const [captchaToken, setCaptchaToken] = useState<string | null>(null);

  // Turnstile 위젯 id / container ref
  const widgetIdRef = useRef<string | null>(null);
  const containerRef = useRef<HTMLDivElement | null>(null);

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
      // 페이지를 벗어날 때 위젯 제거
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.remove(widgetIdRef.current);
        widgetIdRef.current = null;
      }
    };
  }, []);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // captcha 통과 전에는 요청 막기
    if (!captchaToken) {
      alert("사람 인증을 먼저 완료해주세요.");
      return;
    }

    setLoading(true);

    try {
      // 백엔드 /auth/find-email로 전화번호 + captcha token 전송
      // auth.ts의 findEmailByPhone 내부에서 stripPhone 처리함
      const result = await findEmailByPhone(phone, captchaToken);
      setMaskedEmail(result); // "te***@gmail.com"
    } catch (error: any) {
      // "해당 전화번호로 등록된 계정이 없습니다" 등
      alert(error.message || "이메일 찾기 실패");

      // 실패 시 token 초기화 + widget reset
      setCaptchaToken(null);
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.reset(widgetIdRef.current);
      }
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

          {/* 결과가 있으면 마스킹된 이메일 표시 + 로그인/비번찾기 버튼 */}
          {/* 결과가 없으면 전화번호 입력 폼 */}
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
                  onChange={(e) => setPhone(formatPhone(e.target.value))}
                  required
                  maxLength={13}
                  className="mt-1"
                />
              </div>

              {/* Turnstile 위젯 자리 */}
              <div className="pt-2">
                <div ref={containerRef} />
              </div>

              <Button
                type="submit"
                disabled={loading || !captchaToken}
                className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading ? "찾는 중..." : "이메일 찾기"}
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