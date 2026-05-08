// src/domains/admin/pages/AdminPage.tsx
//
// 관리자 페이지
//
// 현재 기능:
// 1. 신고 관리
//    - 신고 목록 조회
//    - 신고 처리 완료
//    - 신고 반려
//    - 신고 상세 보기
//
// 2. 회원 관리
//    - 회원 목록 조회
//    - 회원 검색
//    - 회원 활성/비활성 처리
//
// 디자인 방향:
// - 현재 프로젝트의 Community/마이페이지 느낌 유지
// - 카드 기반 UI
// - dark mode 대응
// - cyan/luxury-gold 포인트 컬러 사용

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { signOut } from "@/domains/auth/api/auth";

import {
    AlertTriangle,
    Shield,
    Users,
} from "lucide-react";

import { toast } from "sonner";

import { Card } from "@/shared/ui/card";

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
    listAdminContentReports,
    listAdminUsers,
    rejectAdminContentReport,
    resolveAdminContentReport,
    updateAdminUserActive,
    type AdminContentReportResponse,
    type AdminUserResponse,
    type ContentReportStatus,
} from "@/domains/admin/api/adminApi";

// 현재 선택 중인 탭 타입
type AdminTab = "reports" | "users";

// 신고 상태 표시 텍스트
const REPORT_STATUS_LABEL: Record<ContentReportStatus, string> = {
    pending: "대기중",
    resolved: "처리완료",
    rejected: "반려",
};

// 신고 상태별 스타일
const REPORT_STATUS_STYLE: Record<ContentReportStatus, string> = {
    pending:
        "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",

    resolved:
        "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",

    rejected:
        "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300",
};

// 신고 대상 타입 표시 텍스트
const TARGET_TYPE_LABEL: Record<
    AdminContentReportResponse["target_type"],
    string
> = {
    post: "게시글",
    comment: "댓글",
    user_nickname: "닉네임",
    user_profile: "프로필 사진",
};

// 신고 사유 표시 텍스트
const REASON_LABEL: Record<
    AdminContentReportResponse["reason"],
    string
> = {
    abuse: "욕설/비방",
    inappropriate: "부적절",
    spam: "광고/도배",
    other: "기타",
};

// 날짜 포맷 함수
//
// 예:
// 2026.05.08 15:32
function formatDateTime(value: string | null) {
    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    const year = date.getFullYear();

    const month = String(date.getMonth() + 1).padStart(2, "0");

    const day = String(date.getDate()).padStart(2, "0");

    const hour = String(date.getHours()).padStart(2, "0");

    const minute = String(date.getMinutes()).padStart(2, "0");

    return `${year}.${month}.${day} ${hour}:${minute}`;
}

// UUID 너무 길어서 일부만 표시
function shortId(id: string) {
    return `${id.slice(0, 8)}...`;
}

export default function AdminPage() {
    const navigate = useNavigate();

    // 관리자 로그아웃 처리 상태
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    // ─────────────────────────────────────────────
    // 탭 상태
    // ─────────────────────────────────────────────

    const [activeTab, setActiveTab] =
        useState<AdminTab>("reports");

    // ─────────────────────────────────────────────
    // 신고 관리 상태
    // ─────────────────────────────────────────────

    // 신고 상태 필터
    const [reportStatus, setReportStatus] =
        useState<ContentReportStatus | "all">("pending");

    // 신고 목록
    const [reports, setReports] = useState<
        AdminContentReportResponse[]
    >([]);

    // 선택된 신고 상세
    const [selectedReport, setSelectedReport] =
        useState<AdminContentReportResponse | null>(null);

    // 신고 목록 로딩 상태
    const [isReportsLoading, setIsReportsLoading] =
        useState(false);

    // ─────────────────────────────────────────────
    // 회원 관리 상태
    // ─────────────────────────────────────────────

    // 회원 목록
    const [users, setUsers] = useState<
        AdminUserResponse[]
    >([]);

    // 검색 입력값
    const [userKeyword, setUserKeyword] = useState("");

    // 디바운스된 검색어
    const [debouncedUserKeyword, setDebouncedUserKeyword] =
        useState("");

    // 회원 목록 로딩 상태
    const [isUsersLoading, setIsUsersLoading] =
        useState(false);

    // ─────────────────────────────────────────────
    // 공통 처리 상태
    // ─────────────────────────────────────────────

    // 현재 처리 중인 ID
    //
    // 버튼 중복 클릭 방지용
    const [processingId, setProcessingId] =
        useState<string | null>(null);

    // ─────────────────────────────────────────────
    // 계산 값
    // ─────────────────────────────────────────────

    // 처리 대기 신고 수
    const pendingReportCount = useMemo(() => {
        return reports.filter(
            (report) => report.status === "pending"
        ).length;
    }, [reports]);

    // 활성 회원 수
    const activeUserCount = useMemo(() => {
        return users.filter((user) => user.is_active).length;
    }, [users]);

    // ─────────────────────────────────────────────
    // 신고 목록 조회
    // ─────────────────────────────────────────────

    useEffect(() => {
        let ignore = false;

        async function fetchReports() {
            setIsReportsLoading(true);

            try {
                const data =
                    await listAdminContentReports(
                        reportStatus === "all"
                            ? undefined
                            : reportStatus
                    );

                if (!ignore) {
                    setReports(data);
                }
            } catch (error) {
                console.error(
                    "관리자 신고 목록 조회 실패:",
                    error
                );

                toast.error(
                    "신고 목록을 불러오지 못했습니다."
                );
            } finally {
                if (!ignore) {
                    setIsReportsLoading(false);
                }
            }
        }

        void fetchReports();

        return () => {
            ignore = true;
        };
    }, [reportStatus]);

    // ─────────────────────────────────────────────
    // 회원 검색 디바운스
    // ─────────────────────────────────────────────

    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedUserKeyword(
                userKeyword.trim()
            );
        }, 300);

        return () => {
            window.clearTimeout(timer);
        };
    }, [userKeyword]);

    // ─────────────────────────────────────────────
    // 회원 목록 조회
    // ─────────────────────────────────────────────

    useEffect(() => {
        let ignore = false;

        async function fetchUsers() {
            setIsUsersLoading(true);

            try {
                const data = await listAdminUsers(
                    debouncedUserKeyword
                );

                if (!ignore) {
                    setUsers(data);
                }
            } catch (error) {
                console.error(
                    "관리자 회원 목록 조회 실패:",
                    error
                );

                toast.error(
                    "회원 목록을 불러오지 못했습니다."
                );
            } finally {
                if (!ignore) {
                    setIsUsersLoading(false);
                }
            }
        }

        void fetchUsers();

        return () => {
            ignore = true;
        };
    }, [debouncedUserKeyword]);

    // ─────────────────────────────────────────────
    // 신고 처리 완료
    // ─────────────────────────────────────────────

    const handleResolveReport = async (
        reportId: string
    ) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated =
                await resolveAdminContentReport(reportId);

            // 목록 갱신
            setReports((prev) =>
                prev.map((report) =>
                    report.id === reportId
                        ? updated
                        : report
                )
            );

            // 상세 모달 열려있으면 그것도 갱신
            setSelectedReport((current) =>
                current?.id === reportId
                    ? updated
                    : current
            );

            toast.success(
                "신고를 처리 완료했습니다."
            );
        } catch (error) {
            console.error(
                "신고 처리 실패:",
                error
            );

            toast.error(
                "신고 처리에 실패했습니다."
            );
        } finally {
            setProcessingId(null);
        }
    };

    // ─────────────────────────────────────────────
    // 신고 반려
    // ─────────────────────────────────────────────

    const handleRejectReport = async (
        reportId: string
    ) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated =
                await rejectAdminContentReport(reportId);

            setReports((prev) =>
                prev.map((report) =>
                    report.id === reportId
                        ? updated
                        : report
                )
            );

            setSelectedReport((current) =>
                current?.id === reportId
                    ? updated
                    : current
            );

            toast.success(
                "신고를 반려했습니다."
            );
        } catch (error) {
            console.error(
                "신고 반려 실패:",
                error
            );

            toast.error(
                "신고 반려에 실패했습니다."
            );
        } finally {
            setProcessingId(null);
        }
    };

    // ─────────────────────────────────────────────
    // 회원 활성/비활성 변경
    // ─────────────────────────────────────────────

    const handleToggleUserActive = async (
        user: AdminUserResponse
    ) => {
        if (processingId) return;

        setProcessingId(user.id);

        try {
            const updated =
                await updateAdminUserActive(
                    user.id,
                    !user.is_active
                );

            setUsers((prev) =>
                prev.map((item) =>
                    item.id === user.id
                        ? updated
                        : item
                )
            );

            toast.success(
                updated.is_active
                    ? "회원이 활성화되었습니다."
                    : "회원이 비활성화되었습니다."
            );
        } catch (error) {
            console.error(
                "회원 상태 변경 실패:",
                error
            );

            toast.error(
                "회원 상태 변경에 실패했습니다."
            );
        } finally {
            setProcessingId(null);
        }
    };

    // ─────────────────────────────────────────────
    // 로그아웃
    // ─────────────────────────────────────────────
    //
    // 일반 사용자 Sidebar 로그아웃과 같은 흐름.
    // signOut() 내부에서:
    // 1) 백엔드 /auth/logout 호출
    // 2) Supabase session 정리
    // 3) authStorage access token 삭제
    // 를 처리한다.
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
        <div className="min-h-screen bg-[var(--surface)] text-foreground">
            <div className="flex min-h-screen">

                {/* ───────────────────────────── */}
                {/* 좌측 사이드바 */}
                {/* ───────────────────────────── */}

                <aside className="flex w-64 flex-col border-r border-border bg-[var(--surface-elevated)] p-5">

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

                    {/* 메뉴 */}
                    <nav className="space-y-2">

                        {/* 신고 관리 */}
                        <button
                            type="button"
                            onClick={() =>
                                setActiveTab("reports")
                            }
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
                            onClick={() =>
                                setActiveTab("users")
                            }
                            className={`flex w-full items-center gap-3 rounded-xl px-4 py-3 text-sm font-semibold transition ${
                                activeTab === "users"
                                    ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/20"
                                    : "text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground"
                            }`}
                        >
                            <Users className="h-4 w-4" />
                            회원 관리
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

                {/* ───────────────────────────── */}
                {/* 메인 컨텐츠 */}
                {/* ───────────────────────────── */}

                <main className="flex-1 p-8">

                    {/* 페이지 헤더 */}
                    <div className="mb-8">

                        <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Admin Console
                        </p>

                        <h2 className="mt-1 text-3xl font-extrabold">

                            {activeTab === "reports"
                                ? "신고 관리"
                                : "회원 관리"}

                        </h2>

                        <p className="mt-2 text-sm text-muted-foreground">

                            {activeTab === "reports"
                                ? "접수된 신고를 확인하고 처리 상태를 변경합니다."
                                : "가입 회원을 조회하고 활성 상태를 관리합니다."}

                        </p>
                    </div>

                    {/* 요약 카드 */}
                    <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">

                        {/* 처리 대기 신고 */}
                        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">

                            <p className="text-sm text-muted-foreground">
                                처리 대기 신고
                            </p>

                            <p className="mt-2 text-3xl font-extrabold text-yellow-500">
                                {pendingReportCount}
                            </p>
                        </Card>

                        {/* 전체 회원 */}
                        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">

                            <p className="text-sm text-muted-foreground">
                                전체 회원
                            </p>

                            <p className="mt-2 text-3xl font-extrabold text-cyan-500">
                                {users.length}
                            </p>
                        </Card>

                        {/* 활성 회원 */}
                        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">

                            <p className="text-sm text-muted-foreground">
                                활성 회원
                            </p>

                            <p className="mt-2 text-3xl font-extrabold text-emerald-500">
                                {activeUserCount}
                            </p>
                        </Card>
                    </div>
                </main>
            </div>
        </div>
    );
}