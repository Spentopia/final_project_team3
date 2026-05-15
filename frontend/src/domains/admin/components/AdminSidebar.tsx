// src/domains/admin/components/AdminSidebar.tsx
//
// 관리자 페이지 좌측 사이드바.
//
// 일반 사용자 Sidebar와 최대한 같은 시각 규칙으로 맞춘 버전.
//
// 맞춘 부분:
// - aside 배경: bg-sidebar
// - 텍스트: text-sidebar-foreground
// - 너비: w-64
// - 로고 영역 높이: h-16
// - 메뉴 padding: p-4
// - 메뉴 아이콘: h-5 w-5
// - 메뉴 글씨: font-medium
// - active 스타일: bg-sidebar-accent + 왼쪽 gold bar
// - hover 스타일: hover:bg-sidebar-accent/70
// - 로그아웃 버튼 디자인 동일 계열 유지

import {
    AlertTriangle,
    BarChart3,
    Megaphone,
    Shield,
    Trophy,
    Users,
    UserX
} from "lucide-react";

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

import type { AdminTab } from "@/domains/admin/types/adminViewTypes";

type AdminSidebarProps = {
    activeTab: AdminTab;
    onTabChange: (tab: AdminTab) => void;
    isLoggingOut: boolean;
    onLogout: () => void;
};

const menuItems: Array<{
    tab: AdminTab;
    icon: typeof BarChart3;
    label: string;
}> = [
    { tab: "dashboard", icon: BarChart3, label: "대시보드" },
    { tab: "reports", icon: AlertTriangle, label: "신고 관리" },
    { tab: "users", icon: Users, label: "회원 관리" },
    { tab: "withdrawn", icon: UserX, label: "탈퇴 회원 관리" },
    { tab: "notices", icon: Megaphone, label: "공지사항 관리" },
    { tab: "contests", icon: Trophy, label: "콘테스트 관리" },
];

export default function AdminSidebar({
                                         activeTab,
                                         onTabChange,
                                         isLoggingOut,
                                         onLogout,
                                     }: AdminSidebarProps) {
    return (
        <aside className="sticky top-0 flex h-screen w-64 shrink-0 flex-col border-r border-sidebar-border bg-sidebar text-sidebar-foreground shadow-[8px_0_30px_rgba(15,23,42,0.04)] backdrop-blur-xl">
            {/* Logo */}
            {/* 관리자 로고 / 타이틀 */}
            <div className="border-b border-sidebar-border px-5 py-5">
                <div className="flex items-center gap-2">
                    <Shield className="h-6 w-6 text-cyan-500" />

                    <h1 className="text-xl font-extrabold text-sidebar-foreground">
                        관리자
                    </h1>
                </div>

                <p className="mt-2 text-xs text-muted-foreground">
                    Spentopia 운영 관리 페이지
                </p>
            </div>

            {/* Navigation */}
            <nav className="min-h-0 flex-1 space-y-3 overflow-y-auto p-4">
                {menuItems.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeTab === item.tab;

                    return (
                        <button
                            key={item.tab}
                            type="button"
                            onClick={() => onTabChange(item.tab)}
                            className={`relative flex w-full items-center gap-3 rounded-xl px-4 py-3 text-left transition-all ${
                                isActive
                                    ? "bg-sidebar-accent text-sidebar-accent-foreground shadow-card before:absolute before:left-1.5 before:top-1/2 before:h-6 before:w-1 before:-translate-y-1/2 before:rounded-full before:bg-luxury-gold"
                                    : "text-muted-foreground hover:bg-sidebar-accent/70 hover:text-sidebar-foreground"
                            }`}
                        >
                            <Icon className="h-5 w-5" />
                            <span className="font-medium">{item.label}</span>
                        </button>
                    );
                })}
            </nav>

            {/* Bottom area */}
            <div className="shrink-0 border-t border-sidebar-border p-4">
                <AlertDialog>
                    <AlertDialogTrigger asChild>
                        <button
                            type="button"
                            disabled={isLoggingOut}
                            className="mt-2 w-full rounded-lg px-3 py-2 text-center transition-colors hover:bg-sidebar-accent/70 disabled:cursor-not-allowed disabled:opacity-60"
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
                                현재 관리자 계정에서 로그아웃하고 로그인 화면으로 이동합니다.
                            </AlertDialogDescription>
                        </AlertDialogHeader>

                        <AlertDialogFooter>
                            <AlertDialogCancel disabled={isLoggingOut}>
                                취소
                            </AlertDialogCancel>

                            <AlertDialogAction
                                disabled={isLoggingOut}
                                onClick={() => {
                                    onLogout();
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