// 프로필 상태 조회 응답 타입
// 백엔드의 GET /users/me/profile-status 응답에 맞춘 타입.
export interface ProfileStatus {
  isProfileComplete: boolean;
  email: string | null;
  nickname: string | null;
}
