// app/router/routes.tsx
//
// 전체 프론트 라우트 정의.
//
// 핵심 변경:
// - /admin을 일반 사용자 RootLayout children에서 제거
// - /admin을 최상위 라우트로 분리
//
// 이유:
// - 기존에는 /admin도 RootLayout 안에서 렌더링되어서
//   일반 사용자용 Header, Sidebar, 지갑 연결, 게임 시작 버튼이 같이 보였음
// - 관리자 페이지는 별도 콘솔 화면이므로
//   일반 사용자 레이아웃과 분리하는 게 맞음

import { createBrowserRouter } from "react-router";
import ProtectedRoute from "@/app/router/ProtectedRoute";
import RootLayout from "@/shared/layout/RootLayout";
import NotFoundPage from "@/shared/layout/NotFoundPage";
import { FinanceProvider } from "@/shared/providers/FinanceProvider";

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

import AdminPage from "@/domains/admin/pages/AdminPage.tsx";

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
        <FinanceProvider>
          <RootLayout />
        </FinanceProvider>
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

  // ─────────────────────────────────────────────
  // 관리자 페이지
  //
  // RootLayout 밖으로 분리한다.
  // 그래서 일반 사용자용 Header / Sidebar / 게임시작 / 지갑연결이 안 뜬다.
  //
  // ProtectedRoute는 그대로 적용한다.
  // ProtectedRoute 안에 이미 location.pathname.startsWith("/admin")일 때
  // me.role_type !== "admin"이면 forbidden 처리하는 로직이 있음.
  // ─────────────────────────────────────────────
  {
    path: "/admin",
    Component: () => (
        <ProtectedRoute>
          <AdminPage />
        </ProtectedRoute>
    ),
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
    Component: ReceiptOcrTestPage,
  },
]);