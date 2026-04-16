// domains/auth/hooks/useProfileImage.ts
// ─────────────────────────────────────────────────────────────
// 프로필 이미지 업로드 + 미리보기를 관리하는 커스텀 훅
//
// 왜 훅으로 분리했나?
// SignupPage, CompleteProfilePage 등 여러 곳에서
// 동일한 업로드 로직을 재사용하기 위함.
//
// 기능:
// 1) 파일 선택 시 브라우저 미리보기 (URL.createObjectURL)
// 2) 파일 유효성 검사 (타입, 크기)
// 3) 백엔드 업로드 호출
// 4) 업로드 완료 후 path 반환
// 5) 기존 이미지 path로 signed URL 로드
//
// 상태:
// - previewUrl: 브라우저에서 보여줄 이미지 URL
//   → 새 파일 선택 시: createObjectURL (로컬 미리보기)
//   → 기존 이미지 로드 시: signed URL (서버에서 발급)
// - uploading: 업로드 진행 중 여부
// - uploadedPath: 업로드 완료된 object path
//   → 이 값을 completeProfile()의 profileImage로 넘기면 됨

import { useState, useEffect, useCallback } from "react";
import { uploadProfileImage, getProfileImageUrl } from "@/domains/auth/api/profileImage";

// 허용하는 이미지 타입
const ALLOWED_TYPES = ["image/png", "image/jpeg", "image/jpg", "image/webp"];

// 최대 파일 크기 (5MB)
// 백엔드에서도 5MB 체크하지만, 프론트에서 먼저 걸러서
// 불필요한 네트워크 요청을 방지
const MAX_SIZE = 5 * 1024 * 1024;

interface UseProfileImageOptions {
  // 기존에 저장된 이미지 path가 있으면 전달
  // → 마운트 시 signed URL을 받아와서 미리보기에 표시
  initialPath?: string;
}

interface UseProfileImageReturn {
  // 현재 미리보기로 보여줄 이미지 URL
  // null이면 아직 이미지 없음 (기본 아이콘 표시)
  previewUrl: string | null;

  // 업로드 진행 중 여부
  uploading: boolean;

  // 업로드 완료된 storage object path
  // 이 값을 DB에 저장할 때 사용
  // null이면 아직 업로드 안 한 상태
  uploadedPath: string | null;

  // 에러 메시지
  error: string | null;

  // 파일 선택 핸들러
  // <input type="file" onChange={handleFileSelect} /> 에 연결
  handleFileSelect: (e: React.ChangeEvent<HTMLInputElement>) => void;

  // 선택한 파일을 서버에 업로드
  // 파일 선택만 하면 로컬 미리보기만 보이고,
  // 이 함수를 호출해야 실제 서버 업로드가 됨
  upload: () => Promise<string | null>;

  // 미리보기 + 상태 초기화
  reset: () => void;
}

export function useProfileImage(
  options: UseProfileImageOptions = {}
): UseProfileImageReturn {
  const { initialPath } = options;

  // 브라우저 미리보기 URL
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);

  // 선택된 파일 (아직 업로드 안 된 상태)
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  // 업로드 진행 중
  const [uploading, setUploading] = useState(false);

  // 업로드 완료된 path
  const [uploadedPath, setUploadedPath] = useState<string | null>(null);

  // 에러 메시지
  const [error, setError] = useState<string | null>(null);

  // ── 기존 이미지 로드 ──────────────────────────────────────
  // initialPath가 있으면 마운트 시 signed URL을 받아와서
  // 미리보기에 표시.
  // CompleteProfilePage 같은 곳에서 이미 이미지가 있는
  // 유저가 다시 들어왔을 때 기존 이미지를 보여주기 위함.
  useEffect(() => {
    if (!initialPath) return;

    let cancelled = false;

    const loadExistingImage = async () => {
      try {
        const { signed_url } = await getProfileImageUrl(initialPath);
        if (!cancelled) {
          setPreviewUrl(signed_url);
          setUploadedPath(initialPath);
        }
      } catch (err) {
        console.warn("기존 프로필 이미지 로드 실패:", err);
      }
    };

    loadExistingImage();

    // cleanup: 컴포넌트 언마운트 시 상태 업데이트 방지
    return () => {
      cancelled = true;
    };
  }, [initialPath]);

  // ── 파일 선택 핸들러 ──────────────────────────────────────
  // input[type=file]의 onChange에 연결.
  // 파일 유효성 검사 후 로컬 미리보기를 생성.
  // 이 시점에서는 서버 업로드 안 함.
  const handleFileSelect = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setError(null);

      const file = e.target.files?.[0];
      if (!file) return;

      // 타입 체크
      if (!ALLOWED_TYPES.includes(file.type)) {
        setError("png, jpg, webp 이미지만 업로드 가능합니다");
        return;
      }

      // 크기 체크
      if (file.size > MAX_SIZE) {
        setError("파일 크기는 5MB 이하여야 합니다");
        return;
      }

      // 이전 미리보기 URL 해제 (메모리 누수 방지)
      if (previewUrl && previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }

      // 로컬 미리보기 생성
      // createObjectURL: 파일을 브라우저 메모리에 올리고
      // blob:// URL을 반환. 서버 업로드 없이 즉시 보여줄 수 있음.
      const localUrl = URL.createObjectURL(file);
      setPreviewUrl(localUrl);
      setSelectedFile(file);

      // 새 파일을 선택했으므로 이전 업로드 결과 초기화
      setUploadedPath(null);
    },
    [previewUrl]
  );

  // ── 서버 업로드 ───────────────────────────────────────────
  // 선택된 파일을 백엔드 /profile/image/upload로 전송.
  // 성공 시 path를 반환하고 상태에도 저장.
  //
  // 반환값:
  // - 성공: 업로드된 path (예: "user-uuid/avatar.png")
  // - 실패: null
  const upload = useCallback(async (): Promise<string | null> => {
    if (!selectedFile) {
      // 파일이 없으면 기존 uploadedPath를 반환
      // (이미 업로드된 이미지가 있는 경우)
      return uploadedPath;
    }

    setError(null);
    setUploading(true);

    try {
      const { path } = await uploadProfileImage(selectedFile);
      setUploadedPath(path);
      setSelectedFile(null); // 업로드 완료했으므로 파일 참조 해제
      return path;
    } catch (err: any) {
      const msg = err.response?.data?.message || err.message || "이미지 업로드 실패";
      setError(msg);
      return null;
    } finally {
      setUploading(false);
    }
  }, [selectedFile, uploadedPath]);

  // ── 초기화 ────────────────────────────────────────────────
  const reset = useCallback(() => {
    if (previewUrl && previewUrl.startsWith("blob:")) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(null);
    setSelectedFile(null);
    setUploadedPath(null);
    setError(null);
  }, [previewUrl]);

  // ── 컴포넌트 언마운트 시 blob URL 정리 ────────────────────
  useEffect(() => {
    return () => {
      if (previewUrl && previewUrl.startsWith("blob:")) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  return {
    previewUrl,
    uploading,
    uploadedPath,
    error,
    handleFileSelect,
    upload,
    reset,
  };
}