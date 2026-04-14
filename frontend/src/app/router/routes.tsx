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
import KakaoCallbackPage from "@/domains/auth/ui/KakaoCallbackPage";
import SignupPendingPage from "@/domains/auth/ui/SignupPendingPage";
import EmailConfirmedPage from "@/domains/auth/ui/EmailConfirmedPage";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: () => (
      <ProtectedRoute>
        <RootLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, Component: DashboardPage },
      { path: "budget", Component: BudgetPage },
      { path: "analytics", Component: AnalyticsPage },
      { path: "avatar", Component: AvatarPage },
      { path: "marketplace", Component: MarketplacePage },
      { path: "profile", Component: ProfilePage },
      { path: "community", Component: CommunityPage },
      { path: "plaza", Component: PlazaPage },
      { path: "*", Component: NotFoundPage },
    ],
  },
  {
    path: "/login",
    Component: LoginPage,
  },
  {
    path: "/signup",
    Component: SignupPage,
  },
  {
    // 소셜 로그인 후 프로필 미완성 시 여기로 리다이렉트
    path: "/complete-profile",
    Component: CompleteProfilePage,
  },
  {
    path: "/forgot-password",
    Component: ForgotPasswordPage,
  },
  {
    path: "/find-email",
    Component: FindEmailPage,
  },
  {
    path: "/reset-password",
    Component: ResetPasswordPage,
  },
  {
  // 카카오 로그인 후 인가 코드를 받는 콜백 페이지
    path: "/auth/kakao/callback",
    Component: KakaoCallbackPage,
  },
  {
    path: "/signup-pending",
    Component: SignupPendingPage,
  },
  {
    path: "/email-confirmed",
    Component: EmailConfirmedPage,
  },
]);
