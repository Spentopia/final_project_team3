// src/domains/admin/pages/AdminPage.tsx
//
// 관리자 페이지 최상위 컨테이너.
//
// 이 파일의 역할:
// - 관리자 페이지 전체 레이아웃 조립
// - 현재 선택된 탭 상태 관리
// - 신고 목록 / 회원 목록 조회
// - 신고 처리 완료 / 반려 처리
// - 회원 활성/비활성 처리
// - 로그아웃 처리
//
// 하위 컴포넌트 역할:
// - AdminSidebar: 좌측 메뉴/로그아웃 UI
// - AdminDashboard: 요약 카드/최근 신고
// - AdminReportsPanel: 신고 관리 테이블
// - AdminReportDetailModal: 신고 상세 모달
// - AdminUsersPanel: 회원 관리 테이블
//
// 실무식 분리 기준:
// - API 호출과 상태 관리는 컨테이너(AdminPage)에 둔다.
// - 화면 조각은 components 폴더로 분리한다.
// - 하위 컴포넌트는 props를 받아 화면만 그린다.

import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router";
import { toast } from "sonner";

import { signOut } from "@/domains/auth/api/auth";

import AdminSidebar from "@/domains/admin/components/AdminSidebar";
import AdminDashboard from "@/domains/admin/components/AdminDashboard";
import AdminReportsPanel from "@/domains/admin/components/AdminReportsPanel";
import AdminReportDetailModal from "@/domains/admin/components/AdminReportDetailModal";
import AdminUsersPanel from "@/domains/admin/components/AdminUsersPanel";

import {
    listAdminContentReports,
    listAdminUsers,
    rejectAdminContentReport,
    resolveAdminContentReport,
    updateAdminUserActive,
    type AdminContentReportResponse,
    type AdminUserResponse,
} from "@/domains/admin/api/adminApi";

import type {
    AdminTab,
    ReportStatusFilter,
} from "@/domains/admin/types/adminViewTypes";

export default function AdminPage() {
    const navigate = useNavigate();

    // ─────────────────────────────────────────────
    // 로그아웃 상태
    // ─────────────────────────────────────────────
    //
    // 로그아웃 버튼 중복 클릭 방지용.
    const [isLoggingOut, setIsLoggingOut] = useState(false);

    // ─────────────────────────────────────────────
    // 현재 선택 중인 관리자 탭
    // ─────────────────────────────────────────────
    //
    // 처음 진입하면 대시보드를 보여준다.
    const [activeTab, setActiveTab] = useState<AdminTab>("dashboard");

    // ─────────────────────────────────────────────
    // 신고 관리 상태
    // ─────────────────────────────────────────────

    // 신고 상태 필터
    //
    // all: 전체
    // pending: 대기중
    // resolved: 처리완료
    // rejected: 반려
    const [reportStatus, setReportStatus] =
        useState<ReportStatusFilter>("pending");

    // 신고 목록
    const [reports, setReports] = useState<AdminContentReportResponse[]>([]);

    // 선택된 신고 상세
    //
    // null이면 상세 모달 닫힘.
    // 값이 있으면 AdminReportDetailModal 렌더링.
    const [selectedReport, setSelectedReport] =
        useState<AdminContentReportResponse | null>(null);

    // 신고 목록 로딩 상태
    const [isReportsLoading, setIsReportsLoading] = useState(false);

    // ─────────────────────────────────────────────
    // 회원 관리 상태
    // ─────────────────────────────────────────────

    // 회원 목록
    const [users, setUsers] = useState<AdminUserResponse[]>([]);

    // 검색 입력값
    const [userKeyword, setUserKeyword] = useState("");

    // 디바운스된 검색어
    //
    // 사용자가 입력할 때마다 바로 API를 호출하지 않고,
    // 입력이 잠시 멈췄을 때만 API를 호출하기 위해 사용한다.
    const [debouncedUserKeyword, setDebouncedUserKeyword] = useState("");

    // 회원 목록 로딩 상태
    const [isUsersLoading, setIsUsersLoading] = useState(false);

    // ─────────────────────────────────────────────
    // 공통 처리 상태
    // ─────────────────────────────────────────────
    //
    // 신고 처리/회원 비활성화 버튼 중복 클릭 방지용.
    // 현재 처리 중인 row id를 저장한다.
    const [processingId, setProcessingId] = useState<string | null>(null);

    // ─────────────────────────────────────────────
    // 계산 값
    // ─────────────────────────────────────────────

    // 처리 대기 신고 수
    const pendingReportCount = useMemo(() => {
        return reports.filter((report) => report.status === "pending").length;
    }, [reports]);

    // 활성 회원 수
    const activeUserCount = useMemo(() => {
        return users.filter((user) => user.is_active).length;
    }, [users]);

    // 최근 신고 5개
    const recentReports = useMemo(() => {
        return reports.slice(0, 5);
    }, [reports]);

    // ─────────────────────────────────────────────
    // 신고 목록 조회
    // ─────────────────────────────────────────────
    //
    // reportStatus가 바뀔 때마다 다시 조회한다.
    // 대시보드에서도 최근 신고/처리 대기 수를 쓰므로
    // AdminPage 진입 시 기본 pending 신고를 조회한다.
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

    // ─────────────────────────────────────────────
    // 회원 검색 디바운스
    // ─────────────────────────────────────────────
    //
    // 검색어 입력마다 즉시 요청하지 않고 300ms 뒤에 반영한다.
    // 이렇게 해야 검색 API가 과도하게 호출되지 않는다.
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedUserKeyword(userKeyword.trim());
        }, 300);

        return () => {
            window.clearTimeout(timer);
        };
    }, [userKeyword]);

    // ─────────────────────────────────────────────
    // 회원 목록 조회
    // ─────────────────────────────────────────────
    //
    // debouncedUserKeyword가 바뀔 때마다 회원 목록을 조회한다.
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

    // ─────────────────────────────────────────────
    // 신고 처리 완료
    // ─────────────────────────────────────────────
    const handleResolveReport = async (reportId: string) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await resolveAdminContentReport(reportId);

            // 목록 갱신
            setReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            // 상세 모달이 열려 있으면 상세 데이터도 갱신
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

    // ─────────────────────────────────────────────
    // 신고 반려
    // ─────────────────────────────────────────────
    const handleRejectReport = async (reportId: string) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await rejectAdminContentReport(reportId);

            // 목록 갱신
            setReports((prev) =>
                prev.map((report) => (report.id === reportId ? updated : report))
            );

            // 상세 모달이 열려 있으면 상세 데이터도 갱신
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

    // ─────────────────────────────────────────────
    // 회원 활성/비활성 변경
    // ─────────────────────────────────────────────
    const handleToggleUserActive = async (user: AdminUserResponse) => {
        if (processingId) return;

        setProcessingId(user.id);

        try {
            const updated = await updateAdminUserActive(user.id, !user.is_active);

            // 목록 갱신
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

    // ─────────────────────────────────────────────
    // 로그아웃
    // ─────────────────────────────────────────────
    //
    // 일반 사용자 Sidebar의 로그아웃과 같은 흐름이다.
    //
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
                <AdminSidebar
                    activeTab={activeTab}
                    onTabChange={setActiveTab}
                    isLoggingOut={isLoggingOut}
                    onLogout={() => void handleLogout()}
                />

                <main className="flex-1 overflow-y-auto p-8">
                    {/* 페이지 헤더 */}
                    <div className="mb-8">
                        <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Admin Console
                        </p>

                        <h2 className="mt-1 text-3xl font-extrabold">
                            {activeTab === "dashboard" && "대시보드"}
                            {activeTab === "reports" && "신고 관리"}
                            {activeTab === "users" && "회원 관리"}
                        </h2>

                        <p className="mt-2 text-sm text-muted-foreground">
                            {activeTab === "dashboard" &&
                                "운영에 필요한 핵심 지표를 빠르게 확인합니다."}
                            {activeTab === "reports" &&
                                "접수된 신고를 확인하고 처리 상태를 변경합니다."}
                            {activeTab === "users" &&
                                "가입 회원을 조회하고 활성 상태를 관리합니다."}
                        </p>
                    </div>

                    {activeTab === "dashboard" && (
                        <AdminDashboard
                            pendingReportCount={pendingReportCount}
                            totalUserCount={users.length}
                            activeUserCount={activeUserCount}
                            recentReports={recentReports}
                            isReportsLoading={isReportsLoading}
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
                            onResolveReport={(reportId) =>
                                void handleResolveReport(reportId)
                            }
                            onRejectReport={(reportId) =>
                                void handleRejectReport(reportId)
                            }
                        />
                    )}

                    {activeTab === "users" && (
                        <AdminUsersPanel
                            users={users}
                            userKeyword={userKeyword}
                            isUsersLoading={isUsersLoading}
                            processingId={processingId}
                            onUserKeywordChange={setUserKeyword}
                            onToggleUserActive={(user) =>
                                void handleToggleUserActive(user)
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