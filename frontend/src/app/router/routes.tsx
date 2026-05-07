// app/router/routes.tsx
//
// 전체 프론트 라우트 정의.
// 새로 추가되는 인증 관련 페이지:
// - /auth/kakao/callback
// - /signup-pending
// - /email-confirmed

import { createBrowserRouter } from "react-router";
import ProtectedRoute from "@/app/router/ProtectedRoute";
import RootLayout from "@/shared/layout/RootLayout";
import NotFoundPage from "@/shared/layout/NotFoundPage";

import LoginPage from "@/domains/auth/ui/LoginPage";
import SignupPage from "@/domains/auth/ui/SignupPage";
import CompleteProfilePage from "@/domains/auth/ui/CompleteProfilePage";
import ForgotPasswordPage from "@/domains/auth/ui/ForgotPasswordPage";
import FindEmailPage from "@/domains/auth/ui/FindEmailPage";
import ResetPasswordPage from "@/domains/auth/ui/ResetPasswordPage";
import KakaoCallbackPage from "@/domains/auth/ui/KakaoCallbackPage";
import GoogleCallbackPage from "@/domains/auth/ui/GoogleCallbackPage";
import SignupPendingPage from "@/domains/auth/ui/SignupPendingPage";
import EmailConfirmedPage from "@/domains/auth/ui/EmailConfirmedPage";

import DashboardPage from "@/domains/dashboard/pages/DashboardPage";
import BudgetPage from "@/domains/budget/pages/BudgetPage";
import AnalyticsPage from "@/domains/analytics/pages/AnalyticsPage";
import AvatarPage from "@/domains/avatar/pages/AvatarPage";
import MarketplacePage from "@/domains/marketplace/pages/MarketplacePage";
import ProfilePage from "@/domains/profile/ui/ProfilePage";
import CommunityPage from "@/domains/community/pages/CommunityPage";
import PlazaPage from "@/domains/plaza/pages/PlazaPage";
import ReceiptOcrPanel from "@/components/receipt/ReceiptOcrPanel";
import GuidePage from "@/guide/guidePage";
import styles from "./routes.module.css";

function ReceiptOcrTestPage() {
  return (
    <div className={styles.receiptOcrTestPage}>
      <h2>영수증 OCR 테스트</h2>
      <ReceiptOcrPanel
        expectedDate="2026-04-15"
        expectedAmount="12000"
        onApplyOcrResult={(data) => {
          console.log("OCR 결과:", data);
        }}
      />
    </div>
  );
}

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
      { path: "nft-market", Component: MarketplacePage },
      { path: "profile", Component: ProfilePage },
      { path: "profile/items", Component: AvatarPage },
      { path: "avatar-items", Component: AvatarPage },
      { path: "community", Component: CommunityPage },
      { path: "plaza", Component: PlazaPage },
      { path: "guide", Component: GuidePage },
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
    path: "/signup-pending",
    Component: SignupPendingPage,
  },
  {
    path: "/email-confirmed",
    Component: EmailConfirmedPage,
  },
  {
    path: "/auth/kakao/callback",
    Component: KakaoCallbackPage,
  },
  {
    path: "/auth/google/callback",
    Component: GoogleCallbackPage,
  },
  {
    path: "/complete-profile",
    Component: () => (
      <ProtectedRoute>
        <CompleteProfilePage />
      </ProtectedRoute>
    ),
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
    path: "/receipt-test",
    Component: ReceiptOcrTestPage ,
  },
]);
