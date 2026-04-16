// domains/auth/ui/CompleteProfilePage.tsx
// ─────────────────────────────────────────────────────────────
// 소셜 로그인 후 프로필 완성 페이지 (2단계)
//
// 소셜 로그인(구글/카카오)으로 처음 가입하면:
// - auth.users에는 row가 생김
// - handle_new_user() 트리거가 public.users를 만들지만
//   닉네임/전화번호는 비어있음
// - ProtectedRoute가 profile_completed=false를 감지해서 여기로 보냄
//
// Step 1: 닉네임 + 전화번호 + 프로필 이미지 입력
// Step 2: 아바타 선택 → completeProfile() 호출
//   → 이미지가 선택되어 있으면 먼저 업로드 후 path를 받아서
//   → completeProfile에 profileImage로 전달
//   → public.users UPDATE → profile_completed = true → 메인으로

import { useState } from "react";
import { useNavigate } from "react-router";
import { completeProfile } from "@/domains/auth/api/auth";
import { useProfileImage } from "@/domains/auth/hooks/useProfileImage";
import ProfileImageUploader from "@/domains/auth/ui/ProfileImageUploader";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Sparkles } from "lucide-react";

const avatarOptions = [
  { id: 1, name: "해피", emoji: "😊" },
  { id: 2, name: "쿨가이", emoji: "😎" },
  { id: 3, name: "러블리", emoji: "🥰" },
  { id: 4, name: "파이터", emoji: "💪" },
  { id: 5, name: "스마일", emoji: "😄" },
  { id: 6, name: "로봇", emoji: "🤖" },
];

export default function CompleteProfilePage() {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(false);

  // 프로필 이미지 훅
  const profileImage = useProfileImage();

  const [formData, setFormData] = useState({
    phone: "",
    nickname: "",
    avatar: 1,
  });

  const handleNext = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    // Step 1 → 2
    if (step === 1) {
      setStep(2);
      return;
    }

    // Step 2: 아바타 선택 완료 → 프로필 저장
    // 1) 선택된 이미지가 있으면 먼저 서버에 업로드
    // 2) 업로드된 path를 completeProfile에 전달
    setLoading(true);
    try {
      // 이미지 업로드 (선택된 파일이 있을 때만)
      const imagePath = await profileImage.upload();

      await completeProfile({
        nickname: formData.nickname,
        phone: formData.phone,
        profileImage: imagePath || undefined,
      });

      // 프로필 완성 → 메인 페이지로
      navigate("/");
    } catch (error: any) {
      alert(error.message || "프로필 저장 실패");
    } finally {
      setLoading(false);
    }
  };

  const handleBack = () => {
    if (step > 1) setStep(step - 1);
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
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">프로필 완성</h1>
            <p className="text-center text-gray-600 dark:text-gray-400">
              거의 다 왔어요! Step {step} / 2
            </p>
          </div>

          <div className="mb-6 flex gap-2">
            {[1, 2].map((s) => (
              <div
                key={s}
                className={`h-2 flex-1 rounded-full ${
                  s <= step ? "bg-gradient-to-r from-cyan-500 to-blue-500" : "bg-gray-200 dark:bg-gray-700"
                }`}
              ></div>
            ))}
          </div>

          <form onSubmit={handleNext} className="space-y-4">
            {step === 1 && (
              <>
                <div>
                  <Label htmlFor="nickname">닉네임</Label>
                  <Input
                    id="nickname"
                    type="text"
                    placeholder="닉네임을 입력해주세요"
                    value={formData.nickname}
                    onChange={(e) => updateFormData("nickname", e.target.value)}
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

                {/* ── 프로필 이미지 업로드 ────────────────────── */}
                <div>
                  <Label>프로필 이미지 (선택)</Label>
                  <div className="mt-2">
                    <ProfileImageUploader
                      previewUrl={profileImage.previewUrl}
                      uploading={profileImage.uploading}
                      error={profileImage.error}
                      onFileSelect={profileImage.handleFileSelect}
                    />
                  </div>
                </div>
              </>
            )}

            {step === 2 && (
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
                        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">{avatar.name}</span>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="rounded-lg border border-cyan-200 dark:border-cyan-700 bg-cyan-50 dark:bg-cyan-900/30 p-4">
                  <p className="mb-2 font-bold text-purple-900 dark:text-purple-100">🎁 가입 축하 선물!</p>
                  <p className="text-sm text-purple-700 dark:text-purple-300">
                    프로필 완성 시 기본 아바타를 지급해드려요!
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
                disabled={loading || profileImage.uploading}
                className="flex-1 bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              >
                {loading || profileImage.uploading
                  ? "저장 중..."
                  : step === 2
                    ? "완료"
                    : "다음"}
              </Button>
            </div>
          </form>
        </div>
      </Card>
    </div>
  );
}
