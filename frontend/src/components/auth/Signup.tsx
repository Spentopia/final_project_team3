import { useState } from "react";
import { useNavigate, Link } from "react-router";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Card } from "../ui/card";
import { Sparkles, Upload } from "lucide-react";
import { ImageWithFallback } from "../figma/ImageWithFallback";

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
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    confirmPassword: "",
    name: "",
    phone: "",
    nickname: "",
    avatar: 1,
  });

  const handleNext = (e: React.FormEvent) => {
    e.preventDefault();
    if (step < 3) {
      setStep(step + 1);
    } else {
      // Complete signup
      localStorage.setItem("spentopia_auth", "mock_token");
      navigate("/");
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
    <div className="flex min-h-screen items-center justify-center bg-gradient-to-br from-purple-500 via-pink-500 to-blue-500 dark:from-purple-900 dark:via-pink-900 dark:to-blue-900 p-4">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 dark:bg-gray-900/95 backdrop-blur-xl">
        <div className="p-8">
          {/* Logo */}
          <div className="mb-8 flex flex-col items-center">
            <div className="mb-4 flex h-16 w-16 items-center justify-center rounded-2xl bg-gradient-to-br from-purple-500 to-pink-500 shadow-lg">
              <Sparkles className="h-8 w-8 text-white" />
            </div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">회원가입</h1>
            <p className="text-center text-gray-600 dark:text-gray-400">
              Step {step} / 3
            </p>
          </div>

          {/* Progress Bar */}
          <div className="mb-6 flex gap-2">
            {[1, 2, 3].map((s) => (
              <div
                key={s}
                className={`h-2 flex-1 rounded-full ${
                  s <= step ? "bg-gradient-to-r from-purple-500 to-pink-500" : "bg-gray-200 dark:bg-gray-700"
                }`}
              ></div>
            ))}
          </div>

          <form onSubmit={handleNext} className="space-y-4">
            {/* Step 1: Account Info */}
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

            {/* Step 2: Personal Info */}
            {step === 2 && (
              <>
                <div>
                  <Label htmlFor="name">이름</Label>
                  <Input
                    id="name"
                    type="text"
                    placeholder="홍길동"
                    value={formData.name}
                    onChange={(e) => updateFormData("name", e.target.value)}
                    required
                    className="mt-1"
                  />
                </div>

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
                  <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">나중에 설정할 수 있어요</p>
                </div>
              </>
            )}

            {/* Step 3: Avatar Selection */}
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
                            ? "border-purple-500 bg-purple-50 dark:bg-purple-900/30 shadow-lg"
                            : "border-gray-200 dark:border-gray-700 hover:border-purple-300 dark:hover:border-purple-600"
                        }`}
                      >
                        <span className="mb-2 text-4xl">{avatar.emoji}</span>
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{avatar.name}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-lg border border-purple-200 dark:border-purple-700 bg-purple-50 dark:bg-purple-900/30 p-4">
                  <p className="mb-2 font-bold text-purple-900 dark:text-purple-100">🎁 가입 축하 선물!</p>
                  <p className="text-sm text-purple-700 dark:text-purple-300">
                    회원가입 완료 시 500 SPT와 랜덤 아바타 티켓을 드려요!
                  </p>
                </div>
              </>
            )}

            {/* Buttons */}
            <div className="flex gap-3 pt-4">
              {step > 1 && (
                <Button type="button" variant="outline" onClick={handleBack} className="flex-1">
                  이전
                </Button>
              )}
              <Button
                type="submit"
                className="flex-1 bg-gradient-to-r from-purple-500 to-pink-500 hover:from-purple-600 hover:to-pink-600"
              >
                {step === 3 ? "가입 완료" : "다음"}
              </Button>
            </div>
          </form>

          {/* Login Link */}
          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600 dark:text-gray-400">
              이미 계정이 있으신가요?{" "}
              <Link to="/login" className="font-bold text-purple-600 dark:text-purple-400 hover:text-purple-700 dark:hover:text-purple-300">
                로그인
              </Link>
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}