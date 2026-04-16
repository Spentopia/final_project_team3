// domains/auth/api/profileImage.ts
// ─────────────────────────────────────────────────────────────
// 프로필 이미지 관련 API 함수
//
// 백엔드 엔드포인트:
// - POST /profile/image/upload  → 이미지 업로드 (multipart/form-data)
// - GET  /profile/image-url     → signed URL 발급 (query param: path)
//
// 왜 백엔드를 거치나?
// Supabase Storage 버킷이 private이라
// 프론트에서 직접 접근 불가.
// 백엔드가 service_role 키로 대신 업로드하고,
// 이미지를 보여줄 때는 signed URL(24시간 유효)을 발급해줌.
//
// DB 저장 방식:
// users.profile_image에는 URL이 아니라 path만 저장
// 예: "defaults/avatar.png", "유저UUID/avatar.png"

import { apiClient } from "@/shared/api/client";

// ─────────────────────────────────────────────────────────────
// 프로필 이미지 업로드
//
// 파일을 multipart/form-data로 백엔드에 전송.
// 백엔드가 Supabase Storage private 버킷에 저장하고
// object path를 반환함.
//
// apiClient에 interceptor가 있어서
// Authorization 헤더는 자동으로 붙음.
// 단, Content-Type은 설정하지 않아야 함.
// → axios가 FormData를 감지해서 자동으로
//   multipart/form-data + boundary를 설정해줌.
//   수동으로 넣으면 boundary가 누락돼서 에러남.
//
// @param file - 업로드할 이미지 파일 (png, jpg, webp / 5MB 이하)
// @returns { path: string } - 저장된 object path
// ─────────────────────────────────────────────────────────────
export const uploadProfileImage = async (file: File): Promise<{ path: string }> => {
  const formData = new FormData();
  formData.append("file", file);

  const res = await apiClient.post("/profile/image/upload", formData);

  return res.data;
};

// ─────────────────────────────────────────────────────────────
// signed URL 발급
//
// private 버킷의 이미지를 브라우저에서 보려면
// 시간 제한이 있는 signed URL이 필요함.
// 백엔드가 24시간짜리 URL을 만들어서 반환.
//
// 사용 예시:
//   const { signed_url } = await getProfileImageUrl("defaults/avatar.png");
//   <img src={signed_url} />
//
// @param path - DB에 저장된 이미지 path
// @returns { signed_url: string } - 24시간 유효한 접근 URL
// ─────────────────────────────────────────────────────────────
export const getProfileImageUrl = async (path: string): Promise<{ signed_url: string }> => {
  const res = await apiClient.get("/profile/image-url", {
    params: { path },
  });

  return res.data;
};