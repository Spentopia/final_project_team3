// domains/auth/model/types.ts
//
// 인증 도메인에서 공통으로 쓰는 타입 정의 파일.
//
// 역할:
// - 로그인 요청 타입
// - 회원가입 요청 타입
// - 로그인 응답 타입
//
// 왜 분리하나?
// - auth.ts, LoginPage.tsx, Signup.tsx 등 여러 파일에서 재사용 가능
// - 타입이 바뀌면 이 파일만 수정하면 됨
// - API 응답 구조를 한눈에 보기 쉬움

// ─────────────────────────────────────────────────────────────
// 이메일 로그인 요청
//
// 프론트가 이메일/비밀번호 로그인 시 auth.ts의 login()에 넘기는 타입
// ─────────────────────────────────────────────────────────────
export interface LoginRequest {
  email: string;
  password: string;
}

// ─────────────────────────────────────────────────────────────
// 회원가입 요청
//
// 프론트가 Supabase 회원가입 요청 시 auth.ts의 signUp()에 넘기는 타입
// ─────────────────────────────────────────────────────────────
export interface SignUpRequest {
  email: string;
  password: string;
}

// ─────────────────────────────────────────────────────────────
// 공통 유저 정보 타입
//
// 로그인 응답에 포함되는 최소 유저 정보
// 여기서는 화면 분기나 디버깅에 필요한 정도만 둠
// ─────────────────────────────────────────────────────────────
export interface AuthUser {
  id: string;
  email: string;
  profileCompleted: boolean;
}

// ─────────────────────────────────────────────────────────────
// 로그인 응답 타입
//
// 중요:
// accessToken / refreshToken 은 "Supabase 토큰"이 아니라
// 최종적으로 프론트가 저장하는 "백엔드 앱 JWT" 기준으로 쓴다.
//
// 이메일 로그인:
//   Supabase 1차 인증 -> /auth/exchange -> 앱 JWT 반환
//
// 구글 로그인:
//   Supabase OAuth -> ProtectedRoute에서 /auth/exchange -> 앱 JWT 저장
//
// 카카오 로그인:
//   백엔드가 바로 앱 JWT 반환
// ─────────────────────────────────────────────────────────────
export interface LoginResponse {
  accessToken: string;
  isNewUser: boolean;
  roleType: "user" | "admin";
}

// ─────────────────────────────────────────────────────────────
// 프로필 완성 요청 타입
//
// 회원가입/소셜 로그인 이후 nickname, phone, profile image 저장에 사용
// 지금 auth.ts의 completeProfile()에서 body 생성할 때 참고 가능
// ─────────────────────────────────────────────────────────────
export interface CompleteProfileRequest {
  nickname: string;
  phone: string;
  profileImage?: string;
}