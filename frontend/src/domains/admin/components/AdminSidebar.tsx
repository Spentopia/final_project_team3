// src/domains/admin/components/AdminSidebar.tsx
//
// 관리자 페이지 좌측 사이드바.
//
// 역할:
// - 관리자 메뉴 표시
// - 현재 선택된 탭 강조
// - 대시보드 / 신고 관리 / 회원 관리 탭 변경
// - 준비중 메뉴 표시
// - 일반 사용자 Sidebar와 같은 디자인의 로그아웃 확인 다이얼로그 제공
//
// 주의:
// - 실제 로그아웃 처리는 상위 AdminPage에서 handleLogout으로 관리한다.
// - 이 컴포넌트는 UI만 담당하고, 로그아웃 API 호출은 직접 하지 않는다.

import {
    AlertTriangle,
    BarChart3,
    Megaphone,
    Shield,
    Trophy,
    Users,
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

export default function AdminSidebar({
                                         activeTab,
                                         onTabChange,
                                         isLoggingOut,
                                         onLogout,
                                     }: AdminSidebarProps) {
    return (
        <aside className="flex w-64 flex-col border-r border-border bg-[var(--surface-elevated)] p-5">
            {/* 관리자 로고 / 타이틀 */}
            <div className="mb-8">
                <div className="flex items-center gap-2">
                    <Shield className="h-6 w-6 text-cyan-500" />

                    <h1 className="text-xl font-extrabold">
                        관리자
                    </h1>
                </div>

                <p className="mt-2 text-xs text-muted-foreground">
                    Spentopia 운영 관리 페이지
                </p>
            </div>

            {/* 관리자 메뉴 */}
            <nav className="space-y-2">
                {/* 대시보드 */}
                <button
                    type="button"
                    onClick={() => onTabChange("dashboard")}
                    className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                        activeTab === "dashboard"
                            ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/20"
                            : "text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground"
                    }`}
                >
                    <BarChart3 className="h-4 w-4" />
                    대시보드
                </button>

                {/* 신고 관리 */}
                <button
                    type="button"
                    onClick={() => onTabChange("reports")}
                    className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                        activeTab === "reports"
                            ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/20"
                            : "text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground"
                    }`}
                >
                    <AlertTriangle className="h-4 w-4" />
                    신고 관리
                </button>

                {/* 회원 관리 */}
                <button
                    type="button"
                    onClick={() => onTabChange("users")}
                    className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                        activeTab === "users"
                            ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/20"
                            : "text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground"
                    }`}
                >
                    <Users className="h-4 w-4" />
                    회원 관리
                </button>

                {/* 공지사항 관리 - 2차 구현 예정 */}
                <button
                    type="button"
                    disabled
                    className="flex w-full cursor-not-allowed items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-muted-foreground/50"
                >
                    <Megaphone className="h-4 w-4" />
                    공지사항 관리
                    <span className="ml-auto text-[10px]">준비중</span>
                </button>

                {/* 콘테스트 관리 - 2차 구현 예정 */}
                <button
                    type="button"
                    disabled
                    className="flex w-full cursor-not-allowed items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold text-muted-foreground/50"
                >
                    <Trophy className="h-4 w-4" />
                    콘테스트 관리
                    <span className="ml-auto text-[10px]">준비중</span>
                </button>
            </nav>

            {/* 하단 로그아웃 */}
            <div className="mt-auto border-t border-border pt-4">
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
                            <AlertDialogTitle>
                                로그아웃 하시겠습니까?
                            </AlertDialogTitle>

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