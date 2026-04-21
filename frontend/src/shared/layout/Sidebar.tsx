import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router";
import { signOut } from "@/domains/auth/api/auth";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/shared/ui/alert-dialog";
import {
  Calendar,
  BarChart3,
  Wallet,
  Store,
  User,
  Users,
  Gamepad2,
  Sparkles,
  Zap,
} from "lucide-react";

const menuItems = [
  { path: "/", icon: Calendar, label: "가계부" },
  { path: "/budget", icon: Wallet, label: "예산 설정" },
  { path: "/analytics", icon: BarChart3, label: "소비 분석" },
  { path: "/marketplace", icon: Store, label: "NFT 마켓" },
  { path: "/plaza", icon: Gamepad2, label: "광장" },
  { path: "/community", icon: Users, label: "커뮤니티" },
  { path: "/profile", icon: User, label: "마이페이지" },
];

type SidebarProps = {
  onWeeklyScoreClick?: () => void;
};

export default function Sidebar({ onWeeklyScoreClick }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);

  const handleLogout = async () => {
    try {
      setIsLoggingOut(true);
      await signOut();
      navigate("/login", { replace: true });
    } finally {
      setIsLoggingOut(false);
    }
  };

  return (
    <aside className="flex h-full w-64 flex-col border-r border-white/50 bg-white/80 backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/80">
      
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-white/50 px-6 dark:border-gray-700/50">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-500">
          <Sparkles className="h-6 w-6 text-white" />
        </div>
        <div>
          <h1 className="font-bold text-gray-900 dark:text-gray-100">Spentopia</h1>
          <p className="text-xs text-gray-500 dark:text-gray-400">소비를 자산으로</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="min-h-0 flex-1 space-y-1 overflow-y-auto p-4">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 rounded-xl px-4 py-3 transition-all ${
                isActive
                  ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-lg shadow-cyan-200 dark:shadow-cyan-900/50"
                  : "text-gray-700 hover:bg-white/60 dark:text-gray-300 dark:hover:bg-gray-800/60"
              }`}
            >
              <Icon className="h-5 w-5" />
              <span className="font-medium">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="shrink-0 border-t border-white/50 p-4 dark:border-gray-700/50">
        <button
          type="button"
          onClick={onWeeklyScoreClick}
          className="flex w-full flex-col items-center justify-center rounded-xl border border-cyan-200 bg-white/80 px-4 py-3 text-gray-900 shadow-sm transition-all hover:-translate-y-0.5 hover:bg-cyan-50 hover:shadow-md dark:border-cyan-900/50 dark:bg-gray-800/80 dark:text-gray-100 dark:hover:bg-gray-800"
        >
          <span className="flex items-center gap-2 font-semibold">
            <Zap className="h-4 w-4 text-cyan-600 dark:text-cyan-300" />
            이번 주 성실도
          </span>
          <span className="mt-1 text-2xl font-extrabold text-cyan-600 dark:text-cyan-300">85점</span>
        </button>

        {/* 이용가이드 버튼 */}
        <Link
          to="/guide"
          className="mt-6 flex w-full items-center justify-center rounded-xl bg-gradient-to-r from-cyan-500 to-blue-500 px-4 py-3 font-semibold text-white shadow-lg shadow-cyan-200 transition-all hover:opacity-90 dark:shadow-cyan-900/50"
        >
          이용가이드
        </Link>

        <AlertDialog>
          <AlertDialogTrigger asChild>
            <button
              type="button"
              className="mt-2 w-full rounded-lg px-3 py-2 text-center transition-colors hover:bg-white/60 dark:hover:bg-gray-800/60"
            >
              <span className="text-sm font-medium text-gray-600 dark:text-gray-300">
                로그아웃
              </span>
            </button>
          </AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogTitle>로그아웃 하시겠습니까?</AlertDialogTitle>
              <AlertDialogDescription>
                현재 계정에서 로그아웃하고 로그인 화면으로 이동합니다.
              </AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter>
              <AlertDialogCancel disabled={isLoggingOut}>취소</AlertDialogCancel>
              <AlertDialogAction
                disabled={isLoggingOut}
                onClick={() => {
                  void handleLogout();
                }}
                className="bg-destructive text-white hover:bg-destructive/90 focus-visible:ring-destructive/20 dark:bg-destructive/60 dark:focus-visible:ring-destructive/40"
              >
                {isLoggingOut ? "로그아웃 중..." : "로그아웃"}
              </AlertDialogAction>
            </AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </div>
    </aside>
  );
}
