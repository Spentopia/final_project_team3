// src/domains/admin/pages/AdminPage.tsx
//
// 관리자 페이지 최상위 컨테이너.
//
// 이 파일의 역할:
// - 관리자 페이지 전체 레이아웃 조립
// - 현재 선택된 탭 상태 관리
// - 대시보드용 신고 목록 조회
// - 신고 관리용 필터 신고 목록 조회
// - 회원 목록 조회
// - 공지사항 목록 조회
// - 신고 처리 완료 / 반려 처리
// - 회원 활성/비활성 처리
// - 공지사항 작성/수정/삭제
// - 로그아웃 처리

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";

import { signOut } from "@/domains/auth/api/auth";

import AdminSidebar from "@/domains/admin/components/AdminSidebar";
import AdminDashboard from "@/domains/admin/components/AdminDashboard";
import AdminReportsPanel from "@/domains/admin/components/AdminReportsPanel";
import AdminReportDetailModal from "@/domains/admin/components/AdminReportDetailModal";
import AdminUsersPanel from "@/domains/admin/components/AdminUsersPanel";
import AdminNoticesPanel from "@/domains/admin/components/AdminNoticesPanel";
import AdminContestsPanel from "@/domains/admin/components/AdminContestsPanel.tsx";

import {
    createAdminNotice,
    createAdminContest,
    deleteAdminNotice,
    listAdminContentReports,
    listAdminContests,
    listAdminNotices,
    listAdminUsers,
    rejectAdminContentReport,
    resolveAdminContentReport,
    updateAdminContest,
    updateAdminContestStatus,
    updateAdminNotice,
    updateAdminUserActive,
    type AdminContentReportResponse,
    type AdminContestResponse,
    type AdminContestStatus,
    type AdminNoticeResponse,
    type AdminUserResponse,
} from "@/domains/admin/api/adminApi";

import type {
    AdminTab,
    ReportStatusFilter,
} from "@/domains/admin/types/adminViewTypes";

export default function AdminPage() {
    const navigate = useNavigate();

    const [isLoggingOut, setIsLoggingOut] = useState(false);

    const [activeTab, setActiveTab] = useState<AdminTab>("dashboard");

    // 대시보드용 전체 신고 목록
    const [dashboardReports, setDashboardReports] = useState<
        AdminContentReportResponse[]
    >([]);
    const [isDashboardReportsLoading, setIsDashboardReportsLoading] =
        useState(false);

    // 신고 관리 탭용 신고 목록
    const [reportStatus, setReportStatus] =
        useState<ReportStatusFilter>("pending");
    const [reports, setReports] = useState<AdminContentReportResponse[]>([]);
    const [selectedReport, setSelectedReport] =
        useState<AdminContentReportResponse | null>(null);
    const [isReportsLoading, setIsReportsLoading] = useState(false);

    // 회원 관리
    const [users, setUsers] = useState<AdminUserResponse[]>([]);
    const [userKeyword, setUserKeyword] = useState("");
    const [debouncedUserKeyword, setDebouncedUserKeyword] = useState("");
    const [isUsersLoading, setIsUsersLoading] = useState(false);

    // 공지사항 관리
    const [notices, setNotices] = useState<AdminNoticeResponse[]>([]);
    const [isNoticesLoading, setIsNoticesLoading] = useState(false);

    const [contests, setContests] = useState<AdminContestResponse[]>([]);
    const [isContestsLoading, setIsContestsLoading] = useState(false);

    // 공통 처리 상태
    const [processingId, setProcessingId] = useState<string | null>(null);

    // 대시보드 계산 값
    const pendingReportCount = useMemo(() => {
        return dashboardReports.filter((report) => report.status === "pending")
            .length;
    }, [dashboardReports]);

    const activeUserCount = useMemo(() => {
        return users.filter((user) => user.is_active).length;
    }, [users]);

    const recentReports = useMemo(() => {
        return dashboardReports.slice(0, 5);
    }, [dashboardReports]);

    // 대시보드용 전체 신고 목록 조회
    useEffect(() => {
        let ignore = false;

        async function fetchDashboardReports() {
            setIsDashboardReportsLoading(true);

            try {
                const data = await listAdminContentReports(undefined);

                if (!ignore) {
                    setDashboardReports(data);
                }
            } catch (error) {
                console.error("관리자 대시보드 신고 목록 조회 실패:", error);
                toast.error("대시보드 신고 정보를 불러오지 못했습니다.");
            } finally {
                if (!ignore) {
                    setIsDashboardReportsLoading(false);
                }
            }
        }

        void fetchDashboardReports();

        return () => {
            ignore = true;
        };
    }, []);

    // 신고 관리 탭용 신고 목록 조회
    useEffect(() => {
        let ignore = false;

        async function fetchReports() {
            setIsReportsLoading(true);

            try {
                const data = await listAdminContentReports(
                    reportStatus === "all" ? undefined : reportStatus
                );

                if (!ignore) {
                    setReports(data);
                }
            } catch (error) {
                console.error("관리자 신고 목록 조회 실패:", error);
                toast.error("신고 목록을 불러오지 못했습니다.");
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

    // 회원 검색 디바운스
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedUserKeyword(userKeyword.trim());
        }, 300);

        return () => {
            window.clearTimeout(timer);
        };
    }, [userKeyword]);

    // 회원 목록 조회
    useEffect(() => {
        let ignore = false;

        async function fetchUsers() {
            setIsUsersLoading(true);

            try {
                const data = await listAdminUsers(debouncedUserKeyword);

                if (!ignore) {
                    setUsers(data);
                }
            } catch (error) {
                console.error("관리자 회원 목록 조회 실패:", error);
                toast.error("회원 목록을 불러오지 못했습니다.");
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

    // 공지사항 목록 조회
    useEffect(() => {
        let ignore = false;

        async function fetchNotices() {
            setIsNoticesLoading(true);

            try {
                const data = await listAdminNotices();

                if (!ignore) {
                    setNotices(data);
                }
            } catch (error) {
                console.error("관리자 공지사항 목록 조회 실패:", error);
                toast.error("공지사항 목록을 불러오지 못했습니다.");
            } finally {
                if (!ignore) {
                    setIsNoticesLoading(false);
                }
            }
        }

        void fetchNotices();

        return () => {
            ignore = true;
        };
    }, []);

    useEffect(() => {
        let ignore = false;

        async function fetchContests() {
            setIsContestsLoading(true);

            try {
                const data = await listAdminContests();

                if (!ignore) {
                    setContests(data);
                }
            } catch (error) {
                console.error("관리자 콘테스트 목록 조회 실패:", error);
                toast.error("콘테스트 목록을 불러오지 못했습니다.");
            } finally {
                if (!ignore) {
                    setIsContestsLoading(false);
                }
            }
        }

        void fetchContests();

        return () => {
            ignore = true;
        };
    }, []);

    // 신고 처리 완료
    const handleResolveReport = async (reportId: string) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await resolveAdminContentReport(reportId);

            setReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            setDashboardReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            setSelectedReport((current) =>
                current?.id === reportId ? updated : current
            );

            toast.success("신고를 처리 완료했습니다.");
        } catch (error) {
            console.error("신고 처리 실패:", error);
            toast.error("신고 처리에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 신고 반려
    const handleRejectReport = async (reportId: string) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await rejectAdminContentReport(reportId);

            setReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            setDashboardReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            setSelectedReport((current) =>
                current?.id === reportId ? updated : current
            );

            toast.success("신고를 반려했습니다.");
        } catch (error) {
            console.error("신고 반려 실패:", error);
            toast.error("신고 반려에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 회원 활성/비활성 변경
    const handleToggleUserActive = async (user: AdminUserResponse) => {
        if (processingId) return;

        setProcessingId(user.id);

        try {
            const updated = await updateAdminUserActive(user.id, !user.is_active);

            setUsers((prev) =>
                prev.map((item) => (item.id === user.id ? updated : item))
            );

            toast.success(
                updated.is_active
                    ? "회원이 활성화되었습니다."
                    : "회원이 비활성화되었습니다."
            );
        } catch (error) {
            console.error("회원 상태 변경 실패:", error);
            toast.error("회원 상태 변경에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 공지사항 작성
    const handleCreateNotice = async (params: {
        title: string;
        content: string;
    }) => {
        if (processingId) return;

        setProcessingId("notice-form");

        try {
            const created = await createAdminNotice(params);

            setNotices((prev) => [created, ...prev]);

            toast.success("공지사항을 작성했습니다.");
        } catch (error) {
            console.error("공지사항 작성 실패:", error);
            toast.error("공지사항 작성에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 공지사항 수정
    const handleUpdateNotice = async (
        noticeId: string,
        params: { title: string; content: string }
    ) => {
        if (processingId) return;

        setProcessingId("notice-form");

        try {
            const updated = await updateAdminNotice(noticeId, params);

            setNotices((prev) =>
                prev.map((notice) => (notice.id === noticeId ? updated : notice))
            );

            toast.success("공지사항을 수정했습니다.");
        } catch (error) {
            console.error("공지사항 수정 실패:", error);
            toast.error("공지사항 수정에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 공지사항 삭제
    const handleDeleteNotice = async (noticeId: string) => {
        if (processingId) return;

        setProcessingId(noticeId);

        try {
            await deleteAdminNotice(noticeId);

            setNotices((prev) => prev.filter((notice) => notice.id !== noticeId));

            toast.success("공지사항을 삭제했습니다.");
        } catch (error) {
            console.error("공지사항 삭제 실패:", error);
            toast.error("공지사항 삭제에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    const handleCreateContest = async (params: {
        title: string;
        description: string | null;
        start_date: string;
        end_date: string;
        status: AdminContestStatus;
        reward_description: string | null;
    }) => {
        if (processingId) return;

        setProcessingId("contest-form");

        try {
            const created = await createAdminContest(params);

            setContests((prev) => [created, ...prev]);

            toast.success("콘테스트를 생성했습니다.");
        } catch (error) {
            console.error("콘테스트 생성 실패:", error);
            toast.error("콘테스트 생성에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    const handleUpdateContest = async (
        contestId: string,
        params: {
            title: string;
            description: string | null;
            start_date: string;
            end_date: string;
            status: AdminContestStatus;
            reward_description: string | null;
        }
    ) => {
        if (processingId) return;

        setProcessingId("contest-form");

        try {
            const updated = await updateAdminContest(contestId, params);

            setContests((prev) =>
                prev.map((contest) => (contest.id === contestId ? updated : contest))
            );

            toast.success("콘테스트를 수정했습니다.");
        } catch (error) {
            console.error("콘테스트 수정 실패:", error);
            toast.error("콘테스트 수정에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    const handleUpdateContestStatus = async (
        contestId: string,
        status: AdminContestStatus
    ) => {
        if (processingId) return;

        setProcessingId(contestId);

        try {
            const updated = await updateAdminContestStatus(contestId, status);

            setContests((prev) =>
                prev.map((contest) => (contest.id === contestId ? updated : contest))
            );

            toast.success("콘테스트 상태를 변경했습니다.");
        } catch (error) {
            console.error("콘테스트 상태 변경 실패:", error);
            toast.error("콘테스트 상태 변경에 실패했습니다.");
        } finally {
            setProcessingId(null);
        }
    };

    // 로그아웃
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
                <AdminSidebar
                    activeTab={activeTab}
                    onTabChange={setActiveTab}
                    isLoggingOut={isLoggingOut}
                    onLogout={() => void handleLogout()}
                />

                <main className="flex-1 overflow-y-auto p-8">
                    <div className="mb-8">
                        <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Admin Console
                        </p>

                        <h2 className="mt-1 text-3xl font-extrabold">
                            {activeTab === "dashboard" && "대시보드"}
                            {activeTab === "reports" && "신고 관리"}
                            {activeTab === "users" && "회원 관리"}
                            {activeTab === "notices" && "공지사항 관리"}
                            {activeTab === "contests" && "콘테스트 관리"}
                        </h2>

                        <p className="mt-2 text-sm text-muted-foreground">
                            {activeTab === "dashboard" &&
                                "운영에 필요한 핵심 지표를 빠르게 확인합니다."}
                            {activeTab === "reports" &&
                                "접수된 신고를 확인하고 처리 상태를 변경합니다."}
                            {activeTab === "users" &&
                                "가입 회원을 조회하고 활성 상태를 관리합니다."}
                            {activeTab === "notices" &&
                                "공지사항을 작성, 수정, 삭제합니다."}
                            {activeTab === "contests" &&
                                "아바타 콘테스트를 생성하고 상태를 관리합니다."}
                        </p>
                    </div>

                    {activeTab === "dashboard" && (
                        <AdminDashboard
                            pendingReportCount={pendingReportCount}
                            totalUserCount={users.length}
                            activeUserCount={activeUserCount}
                            recentReports={recentReports}
                            isReportsLoading={isDashboardReportsLoading}
                            onTabChange={setActiveTab}
                        />
                    )}

                    {activeTab === "reports" && (
                        <AdminReportsPanel
                            reports={reports}
                            reportStatus={reportStatus}
                            isReportsLoading={isReportsLoading}
                            processingId={processingId}
                            onReportStatusChange={setReportStatus}
                            onSelectReport={setSelectedReport}
                            onResolveReport={(reportId) => void handleResolveReport(reportId)}
                            onRejectReport={(reportId) => void handleRejectReport(reportId)}
                        />
                    )}

                    {activeTab === "users" && (
                        <AdminUsersPanel
                            users={users}
                            userKeyword={userKeyword}
                            isUsersLoading={isUsersLoading}
                            processingId={processingId}
                            onUserKeywordChange={setUserKeyword}
                            onToggleUserActive={(user) => void handleToggleUserActive(user)}
                        />
                    )}

                    {activeTab === "notices" && (
                        <AdminNoticesPanel
                            notices={notices}
                            isNoticesLoading={isNoticesLoading}
                            processingId={processingId}
                            onCreateNotice={(params) => void handleCreateNotice(params)}
                            onUpdateNotice={(noticeId, params) =>
                                void handleUpdateNotice(noticeId, params)
                            }
                            onDeleteNotice={(noticeId) => void handleDeleteNotice(noticeId)}
                        />
                    )}

                    {activeTab === "contests" && (
                        <AdminContestsPanel
                            contests={contests}
                            isContestsLoading={isContestsLoading}
                            processingId={processingId}
                            onCreateContest={(params) => void handleCreateContest(params)}
                            onUpdateContest={(contestId, params) =>
                                void handleUpdateContest(contestId, params)
                            }
                            onUpdateContestStatus={(contestId, status) =>
                                void handleUpdateContestStatus(contestId, status)
                            }
                        />
                    )}
                </main>
            </div>

            {selectedReport && (
                <AdminReportDetailModal
                    report={selectedReport}
                    processingId={processingId}
                    onClose={() => setSelectedReport(null)}
                    onResolve={(reportId) => void handleResolveReport(reportId)}
                    onReject={(reportId) => void handleRejectReport(reportId)}
                />
            )}
        </div>
    );
}