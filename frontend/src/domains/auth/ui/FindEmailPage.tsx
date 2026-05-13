// domains/auth/ui/FindEmailPage.tsx
// ─────────────────────────────────────────────────────────────
// 이메일 찾기 페이지
//
// 이메일을 까먹은 유저가 전화번호를 입력하면
// 백엔드가 public.users에서 해당 전화번호로 계정을 찾아서
// 로그인 가능한 방식에 따라 결과를 다르게 반환한다.
//
// - email only         : 마스킹된 이메일 표시 + 이메일 로그인/비밀번호 찾기 유도
// - email + google     : 마스킹된 이메일 표시 + 이메일 로그인/구글 로그인/비밀번호 찾기 유도
// - google only        : 마스킹된 이메일 표시 + 구글 로그인 유도
// - kakao              : 이메일은 표시하지 않고 카카오 로그인 유도
//
// 추가:
// - Cloudflare Turnstile 적용
// - 사람이 맞는지 확인한 뒤에만 /auth/find-email 호출
//
// 전화번호 입력:
// - 화면에서는 "010-1234-5678" 형식으로 자동 포맷팅
// - API 전송 시 auth.ts의 findEmailByPhone에서 숫자만 추출 또는 백엔드 format 처리
//
// 왜 백엔드를 거치나?
// 이메일 찾기는 로그인 전 상태에서 호출됨.
// 로그인 안 되어있으면 Supabase RLS가 auth.uid()를 모르므로
// 프론트에서 public.users 조회해도 아무 결과가 안 나옴.
// 백엔드는 service_role 키로 RLS를 우회할 수 있어서
// 전화번호로 조회 → 로그인 방식 기준으로 안전한 형태로 반환한다.

import { useEffect, useRef, useState } from "react";
import { Link } from "react-router";
import { toast } from "sonner";
import {
  findEmailByPhone,
  redirectToKakao,
  signInWithGoogle,
} from "@/domains/auth/api/auth";
import { formatPhone } from "@/shared/lib/phone";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";

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

type FindEmailResult = {
  masked_email: string | null;
  login_provider: string;
  google_connected: boolean;
  message: string;
};

export default function FindEmailPage() {
  const [phone, setPhone] = useState("");
  const [loading, setLoading] = useState(false);

  // 조회 결과 — null이면 아직 조회 안 함
  const [result, setResult] = useState<FindEmailResult | null>(null);

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
      if (widgetIdRef.current && window.turnstile) {
        window.turnstile.remove(widgetIdRef.current);
        widgetIdRef.current = null;
      }
    };
  }, []);

  useEffect(() => {
    if (result !== null) return;
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
  }, [result]);

  const resetCaptcha = () => {
    setCaptchaToken(null);

    if (widgetIdRef.current && window.turnstile) {
      window.turnstile.reset(widgetIdRef.current);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (!captchaToken) {
      toast.error("사람 인증을 먼저 완료해주세요.");
      return;
    }

    setLoading(true);

    try {
      const apiResult = await findEmailByPhone(phone, captchaToken);
      setResult(apiResult);
    } catch (error: any) {
      toast.error(error.message || "이메일 찾기 실패");
      resetCaptcha();
    } finally {
      setLoading(false);
    }
  };

  const handleGoogleLogin = async () => {
    try {
      await signInWithGoogle();
    } catch (error: any) {
      toast.error(error.message || "구글 로그인 이동에 실패했습니다.");
    }
  };

  const handleKakaoLogin = async () => {
    try {
      await redirectToKakao();
    } catch (error: any) {
      toast.error(error.message || "카카오 로그인 이동에 실패했습니다.");
    }
  };

  const resetResult = () => {
    setResult(null);
    setPhone("");
    setCaptchaToken(null);
    widgetIdRef.current = null;
  };

  const isEmailOnly =
    result?.login_provider === "email" && !result.google_connected;

  const isEmailAndGoogle =
    result?.login_provider === "email" && result.google_connected;

  const isGoogleOnly = result?.login_provider === "google";

  const isKakao = result?.login_provider === "kakao";
  const authPrimaryButtonClass =
    "spentopia-light-nft-button";
  const googleLoginButtonClass =
    "w-full rounded-xl border border-gray-200 bg-white text-gray-900 hover:bg-gray-50 dark:border-gray-700 dark:bg-white dark:text-gray-900 dark:hover:bg-gray-50";
  const kakaoLoginButtonClass =
    "w-full rounded-xl border-0 bg-[#FEE500] text-[#191600] hover:bg-[#FEE500] dark:bg-[#FEE500] dark:text-[#191600] dark:hover:bg-[#FEE500]";

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.16),transparent_34%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-[#090b16] dark:bg-none">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 shadow-2xl backdrop-blur-xl dark:bg-[#0b1020]/95 dark:shadow-black/40">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <img src="/favicon.svg" alt="Spentopia" className="mb-4 h-16 w-16" />
            <h1 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
              이메일 찾기
            </h1>
          </div>

          {result ? (
            <div className="space-y-4 text-center">
              {isEmailOnly && (
                <>
                  <p className="text-gray-600 dark:text-gray-400">
                    등록된 이메일:
                  </p>
                  <p className="text-xl font-bold text-[#1E1B4B] dark:text-[#c4b5fd]">
                    {result.masked_email}
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {result.message}
                  </p>

                  <div className="flex gap-3 pt-4">
                    <Link to="/login" className="flex-1">
                      <Button className={`w-full ${authPrimaryButtonClass}`}>
                        로그인
                      </Button>
                    </Link>
                    <Link to="/forgot-password" className="flex-1">
	                      <Button variant="outline" className="w-full spentopia-light-nft-button">
                        비밀번호 찾기
                      </Button>
                    </Link>
                  </div>
                </>
              )}

              {isEmailAndGoogle && (
                <>
                  <p className="text-gray-600 dark:text-gray-400">
                    등록된 이메일:
                  </p>
                  <p className="text-xl font-bold text-[#1E1B4B] dark:text-[#c4b5fd]">
                    {result.masked_email}
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {result.message}
                  </p>

                  <div className="grid gap-3 pt-4">
                    <Link to="/login">
                      <Button className={`w-full ${authPrimaryButtonClass}`}>
                        이메일 로그인
                      </Button>
                    </Link>

                    <Button
                      type="button"
                      onClick={handleGoogleLogin}
                      className={googleLoginButtonClass}
                    >
                      구글 로그인
                    </Button>

                    <Link to="/forgot-password">
	                      <Button variant="outline" className="w-full spentopia-light-nft-button">
                        비밀번호 찾기
                      </Button>
                    </Link>
                  </div>
                </>
              )}

              {isGoogleOnly && (
                <>
                  <p className="text-gray-600 dark:text-gray-400">
                    등록된 이메일:
                  </p>
                  <p className="text-xl font-bold text-[#1E1B4B] dark:text-[#c4b5fd]">
                    {result.masked_email}
                  </p>
                  <p className="text-sm text-gray-500 dark:text-gray-400">
                    {result.message}
                  </p>

                  <div className="pt-4">
                    <Button
                      type="button"
                      onClick={handleGoogleLogin}
                      className={googleLoginButtonClass}
                    >
                      구글 로그인
                    </Button>
                  </div>
                </>
              )}

              {isKakao && (
                <>
                  <p className="text-gray-700 dark:text-gray-300">
                    {result.message}
                  </p>

                  <div className="pt-4">
                    <Button
                      type="button"
                      onClick={handleKakaoLogin}
                      className={kakaoLoginButtonClass}
                    >
                      카카오 로그인
                    </Button>
                  </div>
                </>
              )}

              {!isEmailOnly && !isEmailAndGoogle && !isGoogleOnly && !isKakao && (
                <>
                  <p className="text-gray-700 dark:text-gray-300">
                    {result.message}
                  </p>

                  <div className="pt-4">
                    <Link to="/login">
                      <Button className={`w-full ${authPrimaryButtonClass}`}>
                        로그인으로 이동
                      </Button>
                    </Link>
                  </div>
                </>
              )}

              <div className="pt-2">
                <Button
                  type="button"
                  variant="outline"
	                  className="w-full spentopia-light-nft-button"
                  onClick={resetResult}
                >
                  다시 찾기
                </Button>
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
                  maxLength={13}
                  className="mt-1"
                />
              </div>

              <div className="pt-2">
                <div ref={containerRef} />
              </div>

              <Button
                type="submit"
                disabled={loading || !captchaToken}
                className={`w-full ${authPrimaryButtonClass}`}
              >
                {loading ? "찾는 중..." : "이메일 찾기"}
              </Button>

              <div className="text-center">
                <Link
                  to="/login"
                  className="text-sm text-gray-500 hover:text-[#1E1B4B] dark:text-gray-400 dark:hover:text-[#c4b5fd]"
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
