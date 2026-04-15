// domains/auth/ui/Signup.tsx
//
// Step 기반 회원가입 페이지.
//
// Step 1:
// - 이메일/비밀번호로 Supabase 회원가입
// - Confirm Email 켜져 있으면 accessToken 없이 끝날 수 있음
// - 그 경우 /signup-pending 으로 이동
//
// Step 2 ~ 3:
// - accessToken(=앱 JWT)이 있는 경우에만 진행
// - 마지막에 /profile/complete 호출

import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { authStorage } from "@/shared/lib/auth";
import { signUp, completeProfile } from "@/domains/auth/api/auth";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import type { FormEvent } from "react";
import { Sparkles, Upload } from "lucide-react";

const avatarOptions = [
  { id: 1, name: "해피", emoji: "😊" },
  { id: 2, name: "쿨가이", emoji: "😎" },
  { id: 3, name: "러블리", emoji: "🥰" },
  { id: 4, name: "파이터", emoji: "💪" },
  { id: 5, name: "스마일", emoji: "😄" },
  { id: 6, name: "로봇", emoji: "🤖" },
];

export default function Signup() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);

  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    phone: "",
    nickname: "",
    avatar: 1,
    profileImage: "",
  });

  const handleNext = async (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (loading) return;

    // ── Step 1: 이메일/비밀번호 회원가입 ──────────────────────
    if (step === 1) {
      if (formData.password !== formData.confirmPassword) {
        alert("비밀번호가 일치하지 않습니다");
        return;
      }

      setLoading(true);
      try {
        const result = await signUp({
          email: formData.email,
          password: formData.password,
        });

        // 이메일 인증이 필요한 경우
        // 아직 로그인 상태가 아니므로 Step2로 보내면 안 됨
        if (!result.accessToken) {
          alert("회원가입 완료! 이메일 인증 후 로그인해주세요.");
          navigate("/signup-pending", { state: { email: formData.email } });
          return;
        }

        // Confirm Email 꺼져 있어서 즉시 로그인 가능한 경우
        authStorage.setToken(result.accessToken);

        setStep(2);
      } catch (error: any) {
        alert(error.message || "회원가입 실패");
      } finally {
        setLoading(false);
      }
      return;
    }

    // ── Step 2 -> Step 3 ────────────────────────────────────
    if (step === 2) {
      setStep(3);
      return;
    }

    // ── Step 3: 프로필 저장 ──────────────────────────────────
    setLoading(true);
    try {
      await completeProfile({
        nickname: formData.nickname,
        phone: formData.phone,
        profileImage: formData.profileImage || undefined,
      });

      navigate("/");
    } catch (error: any) {
      alert(error.message || "프로필 저장 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    if (step > 1) {
      setStep(step - 1);
    }
  };

  const updateFormData = (field: string, value: string | number) => {
    setFormData({ ...formData, [field]: value });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500 dark:from-cyan-900 dark:via-blue-900 dark:to-teal-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 shadow-lg">
              <Sparkles className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">회원가입</h1>
            <p className="text-center text-gray-600 dark:text-gray-400">
              Step {step} / 3
            </p>
          </div>

          <div className="mb-6 flex gap-2">
            {[1, 2, 3].map((s) => (
              <div
                key={s}
                className={`h-2 flex-1 rounded-full ${
                  s <= step
                    ? "bg-gradient-to-br from-cyan-500 via-blue-500 to-teal-500"
                    : "bg-gray-200 dark:bg-gray-700"
                }`}
              />
            ))}
          </div>

          <form onSubmit={handleNext} className="space-y-4">
            {step === 1 && (
              <>
                <div>
                  <Label htmlFor="email">이메일</Label>
                  <Input
                    id="email"
                    type="email"
                    placeholder="your@email.com"
                    value={formData.email}
                    onChange={(e) => updateFormData("email", e.target.value)}
                    required
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="password">비밀번호</Label>
                  <Input
                    id="password"
                    type="password"
                    placeholder="8자 이상 입력해주세요"
                    value={formData.password}
                    onChange={(e) => updateFormData("password", e.target.value)}
                    required
                    minLength={8}
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                  <Input
                    id="confirmPassword"
                    type="password"
                    placeholder="비밀번호를 다시 입력해주세요"
                    value={formData.confirmPassword}
                    onChange={(e) => updateFormData("confirmPassword", e.target.value)}
                    required
                    className="mt-1"
                  />
                </div>
              </>
            )}

            {step === 2 && (
              <>
                <div>
                  <Label htmlFor="phone">전화번호</Label>
                  <Input
                    id="phone"
                    type="tel"
                    placeholder="010-1234-5678"
                    value={formData.phone}
                    onChange={(e) => updateFormData("phone", e.target.value)}
                    required
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="nickname">닉네임</Label>
                  <Input
                    id="nickname"
                    type="text"
                    placeholder="멋진 닉네임을 입력해주세요"
                    value={formData.nickname}
                    onChange={(e) => updateFormData("nickname", e.target.value)}
                    required
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="profile">프로필 이미지 (선택)</Label>
                  <div className="mt-2 flex items-center gap-4">
                    <div className="flex h-20 w-20 items-center justify-center rounded-full border-2 border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-800">
                      <Upload className="h-6 w-6 text-gray-400" />
                    </div>
                    <Button type="button" variant="outline" size="sm">
                      이미지 업로드
                    </Button>
                  </div>
                  <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                    나중에 설정할 수 있어요
                  </p>
                </div>
              </>
            )}

            {step === 3 && (
              <>
                <div>
                  <Label>아바타 캐릭터 선택</Label>
                  <p className="mb-4 text-sm text-gray-600 dark:text-gray-400">
                    Spentopia에서 사용할 아바타를 선택해주세요
                  </p>

                  <div className="grid grid-cols-3 gap-3">
                    {avatarOptions.map((avatar) => (
                      <button
                        key={avatar.id}
                        type="button"
                        onClick={() => updateFormData("avatar", avatar.id)}
                        className={`flex flex-col items-center justify-center rounded-xl border-2 p-4 transition-all ${
                          formData.avatar === avatar.id
                            ? "border-cyan-500 bg-cyan-50 dark:bg-cyan-900/30 shadow-lg"
                            : "border-gray-200 dark:border-gray-700 hover:border-cyan-300 dark:hover:border-cyan-600"
                        }`}
                      >
                        <span className="mb-2 text-4xl">{avatar.emoji}</span>
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                          {avatar.name}
                        </span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-lg border border-cyan-200 dark:border-cyan-700 bg-cyan-50 dark:bg-cyan-900/30 p-4">
                  <p className="mb-2 font-bold text-purple-900 dark:text-purple-100">🎁 가입 축하 선물!</p>
                  <p className="text-sm text-purple-700 dark:text-purple-300">
                    회원가입 완료 시 기본 아바타를 지급하고 프로필 설정을 바로 이어서 할 수 있어요.
                  </p>
                </div>
              </>
            )}

            <div className="flex gap-3 pt-4">
              {step > 1 && (
                <Button type="button" variant="outline" onClick={handleBack} className="flex-1">
                  이전
                </Button>
              )}
              <Button
                type="submit"
                disabled={loading}
                className="flex-1 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading ? "처리 중..." : step === 3 ? "가입 완료" : "다음"}
              </Button>
            </div>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              이미 계정이 있으신가요?{" "}
              <Link
                to="/login"
                className="font-bold text-cyan-600 dark:text-cyan-400 hover:text-purple-700 dark:hover:text-purple-300"
              >
                로그인
              </Link>
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}