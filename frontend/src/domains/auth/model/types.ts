// domains/auth/model/types.ts
// ─────────────────────────────────────────────────────────────
// 인증 관련 타입 정의
// DDL의 public.users 테이블 구조에 맞춤
//
// LoginRequest  — 로그인 시 프론트 → API로 보내는 데이터
// LoginResponse — API → 프론트로 돌아오는 데이터
// SignUpRequest — 회원가입 시 보내는 데이터
// ProfileCompleteRequest — 프로필 완성 시 보내는 데이터

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