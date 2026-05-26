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
  ChevronDown,
} from "lucide-react";

const menuItems = [
  { path: "/", icon: Calendar, label: "가계부" },
  { path: "/budget", icon: Wallet, label: "예산 설정" },
  { path: "/analytics", icon: BarChart3, label: "소비 분석" },
  { path: "/marketplace", icon: Store, label: "NFT 마켓" },
  { path: "/plaza", icon: Gamepad2, label: "다운로드" },
  { path: "/community", icon: Users, label: "커뮤니티" },
];

const profileSubItems = [
  { path: "/profile", label: "내 프로필" },
  { path: "/profile/items", label: "내 아바타 아이템" },
];

type SidebarProps = {
  onWeeklyScoreClick?: () => void;
  weeklyScore?: number;
};

export default function Sidebar({ onWeeklyScoreClick, weeklyScore = 0 }: SidebarProps) {
  const location = useLocation();
  const navigate = useNavigate();
  const [isLoggingOut, setIsLoggingOut] = useState(false);
  const [isProfileMenuOpen, setIsProfileMenuOpen] = useState(() =>
    location.pathname.startsWith("/profile"),
  );
  const isProfileSectionActive = location.pathname.startsWith("/profile");

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
      <Link
          to="/"
          className="relative z-50 flex h-16 w-full cursor-pointer items-center gap-3 border-b border-sidebar-border px-6 transition-all hover:opacity-80 active:scale-[0.98]"
          style={{ pointerEvents: 'auto' }} // 브라우저가 간혹 이벤트를 차단하는 것을 방지
      >
        <div className="flex h-10 w-10 shrink-0 items-center justify-center overflow-hidden rounded-xl border border-sidebar-border bg-card shadow-card">
          <img
              src="/favicon.svg"
              alt="Spentopia"
              className="h-8 w-8 object-contain"
          />
        </div>
        <div className="pointer-events-none"> {/* 내부 글씨 때문에 클릭이 씹히는 현상 방지 */}
          <h1 className="font-bold text-sidebar-foreground text-sm leading-tight">Spentopia</h1>
          <p className="text-[11px] text-[#52647e] dark:text-muted-foreground mt-0.5">소비를 자산으로</p>
        </div>
      </Link>

      {/* Navigation */}
      <nav className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;

          return (
            <Link
              key={item.path}
              to={item.path}
              className={`relative flex items-center gap-3 rounded-xl px-4 py-3 transition-all ${
                isActive
                  ? "bg-[linear-gradient(135deg,#3b82f6,#2563eb)] text-white shadow-[0_10px_24px_rgba(37,99,235,0.22)] before:absolute before:left-1.5 before:top-1/2 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-white/80 dark:!bg-[#2d1847] dark:!bg-none dark:border dark:border-violet-300/35 dark:text-violet-50 dark:shadow-lg dark:shadow-[#7c3aed]/20 dark:before:bg-[#a78bfa]"
                  : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-foreground"
              }`}
            >
              <Icon className="h-5 w-5" />
              <span className="font-medium">{item.label}</span>
            </Link>
          );
        })}

        <div className="space-y-1">
          <button
            type="button"
            onClick={() => {
              setIsProfileMenuOpen((prev) => !prev);
              if (!isProfileSectionActive) {
                navigate("/profile");
              }
            }}
            className={`relative flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left transition-all ${
              isProfileSectionActive
                ? "bg-[linear-gradient(135deg,#3b82f6,#2563eb)] text-white shadow-[0_10px_24px_rgba(37,99,235,0.22)] before:absolute before:left-1.5 before:top-1/2 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-white/80 dark:!bg-[#2d1847] dark:!bg-none dark:border dark:border-violet-300/35 dark:text-violet-50 dark:shadow-lg dark:shadow-[#7c3aed]/20 dark:before:bg-[#a78bfa]"
                : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-foreground"
            }`}
          >
            <User className="h-5 w-5" />
            <span className="flex-1 font-medium">마이페이지</span>
            <ChevronDown
              className={`h-4 w-4 transition-transform ${isProfileMenuOpen ? "rotate-180" : ""}`}
            />
          </button>

          {isProfileMenuOpen && (
            <div className="ml-4 space-y-1 border-l border-sidebar-border/80 pl-3">
              {profileSubItems.map((item) => {
                const isActive = location.pathname === item.path;

                return (
                  <Link
                    key={item.path}
                    to={item.path}
                    className={`block rounded-lg px-3 py-2 text-sm transition-colors ${
                      isActive
                        ? "bg-[linear-gradient(135deg,#3b82f6,#2563eb)] text-white shadow-[0_8px_18px_rgba(37,99,235,0.18)] dark:!bg-[#2d1847] dark:!bg-none dark:border dark:border-violet-300/30 dark:text-violet-50 dark:shadow-[#7c3aed]/18"
                        : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-foreground"
                    }`}
                  >
                    {item.label}
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </nav>

      <div className="shrink-0 border-t border-sidebar-border p-4">
        <button
          type="button"
          onClick={onWeeklyScoreClick}
          className="flex w-full flex-col items-start justify-center rounded-xl border border-sidebar-border bg-card px-4 py-3 text-card-foreground shadow-card transition-all hover:-translate-y-0.5 hover:border-[#2563eb]/30 hover:shadow-[0_14px_34px_rgba(37,99,235,0.16)] dark:hover:border-luxury-gold/30"
        >
          <span className="flex items-center gap-2 font-semibold">
            <Zap className="h-4 w-4 text-[#2563eb] dark:text-luxury-gold" />
            이번 달 성실도
          </span>
          <span className="mt-1 text-2xl font-extrabold text-foreground">{weeklyScore}점</span>
        </button>

        {/* 이용가이드 버튼 */}
        <Link
          to="/guide"
          className="mt-6 flex w-full items-center justify-center rounded-xl border border-sidebar-border bg-[linear-gradient(135deg,#ffffff_0%,#f8fafc_48%,#dbeafe_100%)] px-4 py-3 font-semibold text-sidebar-foreground shadow-card transition-all hover:-translate-y-0.5 hover:border-[#2563eb]/30 hover:bg-[linear-gradient(135deg,#3b82f6,#2563eb)] hover:text-white hover:shadow-[0_14px_34px_rgba(37,99,235,0.18)] dark:!bg-[#2d1847] dark:!bg-none dark:border-violet-300/35 dark:text-violet-50 dark:shadow-lg dark:shadow-[#7c3aed]/20 dark:hover:border-violet-300/50 dark:hover:bg-[#3b245f]"
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
