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
]);
