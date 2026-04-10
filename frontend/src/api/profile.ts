import { apiClient } from "@/api/client";
import type { ProfileStatus } from "@/types/profile";

export async function getProfileCompletionStatus(): Promise<ProfileStatus> {
  // TODO: 백엔드 -> Supabase 프로필 상태 조회 엔드포인트에 연결
  // 예시: GET /users/me/profile-status
  // const { data } = await apiClient.get<ProfileStatus>("/users/me/profile-status");
  // return data;
  void apiClient;
  return {
    isProfileComplete: false,
    email: null,
    nickname: null,
  };
}
