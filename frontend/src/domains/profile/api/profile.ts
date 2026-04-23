import { apiClient } from "@/shared/api/client";
import { supabase } from "@/shared/lib/supabase";
import type {
  ProfileStatus,
  UpdateUserProfileRequest,
  UserProfile,
} from "@/domains/profile/model/types";

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

export async function getUserProfile(): Promise<UserProfile> {
  const { data } = await apiClient.get<UserProfile>("/api/user/profile");
  return data;
}

export async function updateUserProfile(
  payload: UpdateUserProfileRequest,
): Promise<UserProfile> {
  const { data } = await apiClient.patch<UserProfile>("/api/user/profile", payload);
  return data;
}

export async function changePassword(
    currentPassword: string,
    newPassword: string,
): Promise<void> {
  await apiClient.patch("/api/user/password", {
    current_password: currentPassword,
    new_password: newPassword,
  });
}

export async function changeEmail(newEmail: string): Promise<void> {
  const { error } = await supabase.auth.updateUser(
    { email: newEmail },
    { emailRedirectTo: `${window.location.origin}/email-confirmed` },
  );
  if (error) throw new Error(error.message);
}
