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
    <aside className="flex h-full w-64 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground shadow-[8px_0_30px_rgba(15,23,42,0.04)] backdrop-blur-xl">
      
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-sidebar-border px-6">
        <div className="flex h-10 w-10 items-center justify-center overflow-hidden rounded-xl border border-sidebar-border bg-card shadow-card">
          <img
            src="/favicon.svg"
            alt="Spentopia"
            className="h-8 w-8 object-contain"
          />
        </div>
        <div>
          <h1 className="font-bold text-sidebar-foreground">Spentopia</h1>
          <p className="text-xs text-muted-foreground">소비를 자산으로</p>
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
              className={`relative flex items-center gap-3 rounded-xl px-4 py-3 transition-all ${
                isActive
                  ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-card before:absolute before:left-1.5 before:top-1/2 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-luxury-gold"
                  : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-foreground"
              }`}
            >
              <Icon className="h-5 w-5" />
              <span className="font-medium">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      <div className="shrink-0 border-t border-sidebar-border p-4">
        <button
          type="button"
          onClick={onWeeklyScoreClick}
          className="flex w-full flex-col items-start justify-center rounded-xl border border-sidebar-border bg-card px-4 py-3 text-card-foreground shadow-card transition-all hover:-translate-y-0.5 hover:border-luxury-gold/30"
        >
          <span className="flex items-center gap-2 font-semibold">
            <Zap className="h-4 w-4 text-luxury-gold" />
            이번 주 성실도
          </span>
          <span className="mt-1 text-2xl font-extrabold text-foreground">85점</span>
        </button>

        {/* 이용가이드 버튼 */}
        <Link
          to="/guide"
          className="mt-6 flex w-full items-center justify-center rounded-xl border border-sidebar-border bg-[var(--surface-subtle)] px-4 py-3 font-semibold text-sidebar-foreground shadow-card transition-all hover:-translate-y-0.5 hover:border-luxury-gold/30"
        >
          이용가이드
        </Link>

        <AlertDialog>
          <AlertDialogTrigger asChild>
            <button
              type="button"
              className="mt-2 w-full rounded-lg px-3 py-2 text-center transition-colors hover:bg-sidebar-accent/70"
            >
              <span className="text-sm font-medium text-muted-foreground">
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
