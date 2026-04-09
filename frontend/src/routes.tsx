import { createBrowserRouter } from "react-router";
import Root from "./components/Root";
import Login from "./components/auth/Login";
import Signup from "./components/auth/Signup";
import Dashboard from "./components/dashboard/Dashboard";
import Budget from "./components/budget/Budget";
import Analytics from "./components/analytics/Analytics";
import Avatar from "./components/avatar/Avatar";
import Marketplace from "./components/marketplace/Marketplace";
import Profile from "./components/profile/Profile";
import Community from "./components/community/Community";
import Plaza from "./components/plaza/Plaza";
import NotFound from "./components/NotFound";

export const router = createBrowserRouter([
  {
    path: "/",
    Component: Root,
    children: [
      { index: true, Component: Dashboard },
      { path: "budget", Component: Budget },
      { path: "analytics", Component: Analytics },
      { path: "avatar", Component: Avatar },
      { path: "marketplace", Component: Marketplace },
      { path: "profile", Component: Profile },
      { path: "community", Component: Community },
      { path: "plaza", Component: Plaza },
      { path: "*", Component: NotFound },
    ],
  },
  {
    path: "/login",
    Component: Login,
  },
  {
    path: "/signup",
    Component: Signup,
  },
]);