// domains/auth/ui/ProfileImageUploader.tsx
// ─────────────────────────────────────────────────────────────
// 프로필 이미지 업로드 공통 컴포넌트
//
// SignupPage, CompleteProfilePage 등에서 재사용.
// useProfileImage 훅과 연결해서 사용.
//
// 기능:
// - 이미지 미리보기 (원형)
// - 이미지 없으면 기본 아이콘 표시
// - 클릭하면 파일 선택 다이얼로그 열림
// - 업로드 중 로딩 표시
// - 에러 메시지 표시
//
// 사용법:
//   const profileImage = useProfileImage();
//
//   <ProfileImageUploader
//     previewUrl={profileImage.previewUrl}
//     uploading={profileImage.uploading}
//     error={profileImage.error}
//     onFileSelect={profileImage.handleFileSelect}
//   />

import { useRef } from "react";
import { Upload, Loader2 } from "lucide-react";
import { Button } from "@/shared/ui/button";

interface ProfileImageUploaderProps {
  // 미리보기 URL (blob:// 또는 signed URL)
  previewUrl: string | null;

  // 업로드 진행 중
  uploading: boolean;

  // 에러 메시지
  error: string | null;

  // 파일 선택 핸들러 (useProfileImage의 handleFileSelect)
  onFileSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;
}

export default function ProfileImageUploader({
  previewUrl,
  uploading,
  error,
  onFileSelect,
}: ProfileImageUploaderProps) {
  // 숨겨진 file input을 참조
  // 버튼 클릭 시 이 input의 click()을 호출해서 파일 선택 다이얼로그를 열기 위함
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleButtonClick = () => {
    fileInputRef.current?.click();
  };

  return (
    <div>
      {/* 숨겨진 file input */}
      {/* accept: 이미지 파일만 선택 가능하도록 필터링 */}
      <input
        ref={fileInputRef}
        type="file"
        accept="image/png, image/jpeg, image/webp"
        onChange={onFileSelect}
        className="hidden"
      />

      <div className="flex items-center gap-4">
        {/* ── 이미지 미리보기 영역 ─────────────────────────── */}
        {/* 원형으로 보여주고, 이미지가 없으면 Upload 아이콘 표시 */}
        <div
          onClick={handleButtonClick}
          className="relative flex h-20 w-20 cursor-pointer items-center justify-center overflow-hidden rounded-full border-2 border-dashed border-gray-300 dark:border-gray-600 bg-gray-50 dark:bg-gray-800 transition-colors hover:border-cyan-400 dark:hover:border-cyan-500"
        >
          {previewUrl ? (
            // 이미지가 있으면 미리보기 표시
            <img
              src={previewUrl}
              alt="프로필 미리보기"
              className="h-full w-full object-cover"
            />
          ) : (
            // 이미지가 없으면 기본 아이콘
            <Upload className="h-6 w-6 text-gray-400" />
          )}

          {/* 업로드 중 오버레이 */}
          {uploading && (
            <div className="absolute inset-0 flex items-center justify-center bg-black/40 rounded-full">
              <Loader2 className="h-6 w-6 animate-spin text-white" />
            </div>
          )}
        </div>

        {/* ── 업로드 버튼 ────────────────────────────────── */}
        <div className="flex flex-col gap-1">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={handleButtonClick}
            disabled={uploading}
          >
            {uploading ? "업로드 중..." : previewUrl ? "이미지 변경" : "이미지 업로드"}
          </Button>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            png, jpg, webp · 5MB 이하
          </p>
        </div>
      </div>

      {/* ── 에러 메시지 ──────────────────────────────────── */}
      {error && (
        <p className="mt-2 text-sm text-red-500">{error}</p>
      )}
    </div>
  );
}
