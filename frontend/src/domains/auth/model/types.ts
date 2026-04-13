export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    nickname?: string; //프로필 완성 전이면 undefined
    profileCompleted: boolean;
  };
}

export interface SignUpRequest {
  email: string;
  password: string;
}

export interface ProfileCompleteRequest {
  nickname: string;
  phone: string;
  avatar?: number;
  profileImage?: string;
}