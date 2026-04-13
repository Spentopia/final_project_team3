// app/router/routes.tsx
// ─────────────────────────────────────────────────────────────
// 앱의 모든 URL 경로(라우트)를 정의하는 파일
// 백엔드의 route.rs와 동일한 역할
//
// 구조:
// "/" (루트) → ProtectedRoute로 감쌈 (로그인 + 프로필완성 필수)
//   ├── "/"             → DashboardPage (가계부 메인)
//   ├── "/budget"       → BudgetPage (예산 설정)
//   ├── "/analytics"    → AnalyticsPage (소비 분석)
//   ├── "/avatar"       → AvatarPage (아바타 꾸미기)
//   ├── "/marketplace"  → MarketplacePage (마켓)
//   ├── "/profile"      → ProfilePage (마이페이지)
//   ├── "/community"    → CommunityPage (커뮤니티)
//   └── "/plaza"        → PlazaPage (광장)
//
// 공개 라우트 (로그인 불필요):
// "/login"            → 로그인 페이지
// "/signup"           → 회원가입 페이지
// "/complete-profile" → 소셜 첫 가입 후 프로필 완성
// "/forgot-password"  → 비밀번호 찾기 (재설정 이메일 발송)
// "/find-email"       → 이메일 찾기 (전화번호로 조회)
// "/reset-password"   → 새 비밀번호 설정 (재설정 링크 클릭 후)

import { createBrowserRouter } from "react-router";
import ProtectedRoute from "@/app/router/ProtectedRoute";
import RootLayout from "@/shared/layout/RootLayout";
import NotFoundPage from "@/shared/layout/NotFoundPage";
import LoginPage from "@/domains/auth/ui/LoginPage";
import SignupPage from "@/domains/auth/ui/SignupPage";
import DashboardPage from "@/domains/dashboard/pages/DashboardPage";
import BudgetPage from "@/domains/budget/pages/BudgetPage";
import AnalyticsPage from "@/domains/analytics/pages/AnalyticsPage";
import AvatarPage from "@/domains/avatar/pages/AvatarPage";
import MarketplacePage from "@/domains/marketplace/pages/MarketplacePage";
import ProfilePage from "@/domains/profile/ui/ProfilePage";
import CommunityPage from "@/domains/community/pages/CommunityPage";
import PlazaPage from "@/domains/plaza/pages/PlazaPage";
import CompleteProfilePage from "@/domains/auth/ui/CompleteProfilePage";
import ForgotPasswordPage from "@/domains/auth/ui/ForgotPasswordPage";
import FindEmailPage from "@/domains/auth/ui/FindEmailPage";
import ResetPasswordPage from "@/domains/auth/ui/ResetPasswordPage";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: () => (
      <ProtectedRoute>
        <RootLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, Component: DashboardPage },       // 가계부 메인 (캘린더 + 소비내역)
      { path: "budget", Component: BudgetPage },        // 예산 설정 + AI 소비 플랜
      { path: "analytics", Component: AnalyticsPage },  // 소비 패턴 분석 리포트
      { path: "avatar", Component: AvatarPage },        // 아바타 꾸미기
      { path: "marketplace", Component: MarketplacePage }, // NFT 아이템 마켓
      { path: "profile", Component: ProfilePage },      // 마이페이지
      { path: "community", Component: CommunityPage },  // 커뮤니티 (콘테스트 + 챗봇)
      { path: "plaza", Component: PlazaPage },          // 광장 (Unity WebGL)
      { path: "*", Component: NotFoundPage },           // 404 페이지
    ],
  },

  // ── 공개 라우트 (로그인 불필요) ───────────────────────────
  {
    path: "/login",
    Component: LoginPage,
  },
  {
    path: "/signup",
    Component: SignupPage,
  },
  {
    // 소셜 로그인(구글/카카오) 후 첫 가입이면
    // ProtectedRoute가 profile_completed=false를 감지해서 여기로 보냄
    // 닉네임 + 전화번호 + 아바타 선택하면 프로필 완성
    path: "/complete-profile",
    Component: CompleteProfilePage,
  },
  {
    // 비밀번호 찾기: 이메일 입력 → Supabase가 재설정 링크 이메일 발송
    path: "/forgot-password",
    Component: ForgotPasswordPage,
  },
  {
    // 이메일 찾기: 전화번호 입력 → 백엔드가 마스킹된 이메일 반환
    path: "/find-email",
    Component: FindEmailPage,
  },
  {
    // 새 비밀번호 설정: Supabase 재설정 링크 클릭 후 도착하는 페이지
    // Supabase가 링크에 세션을 포함시켜서 별도 인증 없이 비밀번호 변경 가능
    path: "/reset-password",
    Component: ResetPasswordPage,
  },
]);
