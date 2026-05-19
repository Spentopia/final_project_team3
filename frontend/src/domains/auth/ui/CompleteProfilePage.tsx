// domains/auth/ui/CompleteProfilePage.tsx
// ─────────────────────────────────────────────────────────────
// 소셜 로그인 후 프로필 완성 페이지
//
// 소셜 로그인(구글/카카오)으로 처음 가입하면:
// - auth.users에는 row가 생김
// - handle_new_user() 트리거가 public.users를 만들지만
//   닉네임/전화번호는 비어있음
// - ProtectedRoute가 profile_completed=false를 감지해서 여기로 보냄
//
// 닉네임 + 전화번호 + 프로필 이미지 입력 → completeProfile() 호출
//   → 이미지가 선택되어 있으면 먼저 업로드 후 path를 받아서
//   → completeProfile에 profileImage로 전달
//   → public.users UPDATE → profile_completed = true → 메인으로

import { useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";
import { completeProfile, checkProfileAvailability, checkNicknameAvailable, signOut } from "@/domains/auth/api/auth";
import { useProfileImage } from "@/domains/auth/hooks/useProfileImage";
import ProfileImageUploader from "@/domains/auth/ui/ProfileImageUploader";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Card } from "@/shared/ui/card";
import { Dices } from "lucide-react";
import { formatPhone, isValidPhone } from "@/shared/lib/phone";

const NICKNAME_PREFIXES = [
  "플렉스", "제로", "갓생", "흑자", "스마트", "럭키", "코어", "알뜰", "골든", "메타",
  "네오", "다이아", "슈퍼", "픽", "데이터", "비트", "리얼", "어반", "부스트", "위너",
];
const NICKNAME_SUFFIXES = [
  "천국", "로그", "라이프", "밸런스", "모드", "클럽", "포인트", "팩토리", "가든", "스테이지",
  "존", "랩", "노트", "메이커", "뷰", "코드", "로프트", "라운지", "파크", "빌드",
];

const NICKNAME_MIN_LENGTH = 2;
const NICKNAME_MAX_LENGTH = 8;
const NICKNAME_RANDOM_NUMBER_DIGITS = 2;

function generateNickname(): string {
  // 랜덤 닉네임 정책:
  // - prefix + suffix + 숫자 2자리
  // - 전체 길이 8자 이하만 허용
  //
  // 예:
  // - 알뜰존05       OK
  // - 리얼라운지05   길이에 따라 OK
  // - 알뜰로프트05   8자 초과면 후보에서 제외
  //
  // 핵심:
  // slice로 억지로 자르는 방식이 아니라,
  // 처음부터 8자 이하 후보만 만들어서 그중 하나를 반환한다.
  const candidates: string[] = [];

  for (const prefix of NICKNAME_PREFIXES) {
    for (const suffix of NICKNAME_SUFFIXES) {
      for (let i = 0; i < 100; i += 1) {
        const num = i
            .toString()
            .padStart(NICKNAME_RANDOM_NUMBER_DIGITS, "0");

        const candidate = `${prefix}${suffix}${num}`;

        if (candidate.length <= NICKNAME_MAX_LENGTH) {
          candidates.push(candidate);
        }
      }
    }
  }

  // 방어 코드.
  // 현재 prefix/suffix 배열 기준으로 candidates가 비어 있을 가능성은 낮지만,
  // 나중에 긴 단어만 남게 되면 빈 배열이 될 수 있으므로 fallback을 둔다.
  if (candidates.length === 0) {
    return "픽존00";
  }

  return candidates[Math.floor(Math.random() * candidates.length)];
}

export default function CompleteProfilePage() {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [nicknameChecking, setNicknameChecking] = useState(false);

  // 프로필 이미지 훅
  const profileImage = useProfileImage();

  const [formData, setFormData] = useState({
    phone: "",
    nickname: "",
  });

  const handleNext = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (loading) return;

    const nickname = formData.nickname.trim();

    // 닉네임 필수 검사
    if (!nickname) {
      toast.error("닉네임을 입력해주세요.");
      return;
    }

    // 닉네임 길이 검사
    //
    // 백엔드 validate_nickname() 기준과 맞춘다.
    if (
        nickname.length < NICKNAME_MIN_LENGTH ||
        nickname.length > NICKNAME_MAX_LENGTH
    ) {
      toast.error("닉네임은 2~8자까지 입력할 수 있습니다.");
      return;
    }

    // 전화번호 필수 검사
    if (!formData.phone.trim()) {
      toast.error("전화번호를 입력해주세요.");
      return;
    }

    // 추가
    if (!isValidPhone(formData.phone)) {
      toast.error("올바른 휴대폰 번호를 입력해주세요.");
      return;
    }

    setLoading(true);

    try {
      // 닉네임/전화번호 중복 확인
      //
      // nickname은 trim된 값을 보낸다.
      // formData.nickname을 그대로 보내면 앞뒤 공백이 포함될 수 있다.
      await checkProfileAvailability({
        nickname,
        phone: formData.phone,
      });

      // 프로필 이미지 업로드
      //
      // 사용자가 이미지를 선택하지 않았다면
      // profileImage.upload()는 null/undefined 계열 값을 반환하고,
      // completeProfile에는 profileImage를 넘기지 않는다.
      const imagePath = await profileImage.upload();

      const result = await completeProfile({
        nickname,
        phone: formData.phone,
        profileImage: imagePath || undefined,
      });

      if (!result?.profile_completed) {
        throw new Error("프로필 저장에 실패했습니다. 다시 시도해주세요.");
      }

      navigate("/");
    } catch (error: any) {
      toast.error(error.message || "프로필 저장 실패");
    } finally {
      setLoading(false);
    }
  };

  // 다른 계정으로 로그인하기
  // access token(메모리) + refresh 쿠키(백엔드) + Supabase 세션 모두 정리 후 로그인 페이지로 이동
  const handleSwitchAccount = async () => {
    try {
      await signOut();
    } catch {
      // 정리 실패해도 로그인 페이지로 이동
    }
    navigate("/login");
  };

  const handleGenerateNickname = async () => {
    setNicknameChecking(true);

    try {
      for (let i = 0; i < 5; i += 1) {
        const candidate = generateNickname();

        // generateNickname()에서 이미 8자 이하만 반환하지만,
        // 혹시 나중에 로직이 바뀌어도 긴 닉네임이 들어가지 않도록 한 번 더 방어한다.
        if (candidate.length > NICKNAME_MAX_LENGTH) {
          continue;
        }

        const available = await checkNicknameAvailable(candidate);

        if (available) {
          updateFormData("nickname", candidate);
          return;
        }
      }

      // 5회 모두 중복이면 중복 여부는 제출 시 다시 확인된다.
      // 그래도 길이는 반드시 8자 이하인 값만 사용한다.
      updateFormData("nickname", generateNickname());
    } catch {
      updateFormData("nickname", generateNickname());
    } finally {
      setNicknameChecking(false);
    }
  };

  const updateFormData = (field: string, value: string) => {
    setFormData({ ...formData, [field]: value });
  };

  const authPrimaryButtonClass =
      "spentopia-light-nft-button";

  return (
    <div className="flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,rgba(125,211,252,0.18),transparent_34%),radial-gradient(circle_at_bottom_left,rgba(37,99,235,0.08),transparent_30%),linear-gradient(180deg,#f8fbff_0%,#ffffff_48%,#eff6ff_100%)] p-4 dark:bg-[#090b16] dark:bg-none">
      <Card className="w-full max-w-md overflow-hidden border-none bg-white/95 shadow-2xl backdrop-blur-xl dark:bg-[#0b1020]/95 dark:shadow-black/40">
        <div className="p-8">
          <div className="mb-8 flex flex-col items-center">
            <img src="/favicon.svg" alt="Spentopia" className="mb-4 h-16 w-16" />
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              프로필 완성
            </h1>
          </div>

          <form onSubmit={handleNext} className="space-y-4">
            <div>
              <Label htmlFor="phone">전화번호</Label>
              <Input
                id="phone"
                type="tel"
                placeholder="010-1234-5678"
                value={formData.phone}
                onChange={(e) => updateFormData("phone", formatPhone(e.target.value))}
                maxLength={13}
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="nickname">닉네임</Label>
              <div className="mt-1 flex gap-2">
                <Input
                    id="nickname"
                    type="text"
                    placeholder="2~8자 닉네임을 입력해주세요"
                    value={formData.nickname}
                    maxLength={NICKNAME_MAX_LENGTH}
                    onChange={(e) =>
                        updateFormData(
                            "nickname",
                            e.target.value.slice(0, NICKNAME_MAX_LENGTH)
                        )
                    }
                />
                <button
                  type="button"
                  onClick={handleGenerateNickname}
                  disabled={nicknameChecking}
                  title="랜덤 닉네임 생성"
                  className="flex items-center justify-center rounded-md border border-sky-200/90 bg-white px-3 text-blue-700 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_8px_20px_rgba(37,99,235,0.08)] transition-colors hover:bg-[#f0f7ff] disabled:opacity-50 dark:border-gray-700 dark:bg-gray-800 dark:hover:bg-gray-700"
                >
                  <Dices className={`h-4 w-4 text-gray-500 dark:text-gray-400 ${nicknameChecking ? "animate-spin" : ""}`} />
                </button>
              </div>
            </div>

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
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                나중에 설정할 수 있어요
              </p>
            </div>

            <div className="flex gap-3 pt-4">
              <Button
                type="submit"
                disabled={loading || profileImage.uploading}
                variant="outline"
                className={`flex-1 ${authPrimaryButtonClass}`}
              >
                {loading || profileImage.uploading ? "처리 중..." : "완료"}
              </Button>
            </div>
          </form>

          {/* 다른 계정으로 로그인하기 — 잘못된 소셜 계정으로 들어온 경우 탈출 */}
          <div className="mt-6 text-center">
            <button
              type="button"
              onClick={handleSwitchAccount}
              className="text-sm font-bold text-[#2563eb] underline underline-offset-2 transition-colors hover:text-[#1d4ed8] dark:text-[#c4b5fd] dark:hover:text-[#ddd6fe]"
            >
              다른 계정으로 로그인하기
            </button>
          </div>
        </div>
      </Card>
    </div>
  );
}
