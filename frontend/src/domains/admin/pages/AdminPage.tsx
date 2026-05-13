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
import { useNavigate, useSearchParams } from "react-router";
import { toast } from "sonner";

import { signOut } from "@/domains/auth/api/auth";

import AdminSidebar from "@/domains/admin/components/AdminSidebar";
import AdminDashboard from "@/domains/admin/components/AdminDashboard";
import AdminReportDetailModal from "@/domains/admin/components/AdminReportDetailModal";
import AdminUsersPanel from "@/domains/admin/components/AdminUsersPanel";
import AdminNoticesPanel from "@/domains/admin/components/AdminNoticesPanel";
import AdminContestsPanel from "@/domains/admin/components/AdminContestsPanel.tsx";

// AdminPage.tsx 상단 import에 추가
import AdminReportsPanel, {
    type ReportReasonFilter,
    type ReportTargetTypeFilter,
    type ReportSortBy,
    type ReportSortOrder,
} from "@/domains/admin/components/AdminReportsPanel";

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


// ─────────────────────────────────────────────
// 관리자 API 에러 메시지 추출
// ─────────────────────────────────────────────
//
// 백엔드는 에러를 문자열 body로 내려주는 경우가 많다.
// 예:
// - "탈퇴한 회원은 활성/비활성 상태를 변경할 수 없습니다."
// - "운영자 계정은 활성/비활성 상태를 변경할 수 없습니다."
//
// AxiosError를 그대로 toast로 띄우면
// "Request failed with status code 400"처럼 보일 수 있으므로,
// response.data 문자열을 우선으로 꺼내서 사용자에게 보여준다.
function getAdminApiErrorMessage(error: unknown, fallback: string): string {
    if (error && typeof error === "object" && "response" in error) {
        const response = (
            error as {
                response?: {
                    data?: unknown;
                };
            }
        ).response;

        if (typeof response?.data === "string" && response.data.trim()) {
            return response.data;
        }

        if (
            response?.data &&
            typeof response.data === "object" &&
            "message" in response.data &&
            typeof response.data.message === "string" &&
            response.data.message.trim()
        ) {
            return response.data.message;
        }
    }

    if (error instanceof Error && error.message.trim()) {
        return error.message;
    }

    return fallback;
}

function toIsoFromDateTimeLocal(value: string): string | null {
    if (!value) return null;

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return null;
    }

    return date.toISOString();
}

const REPORTS_PAGE_SIZE = 20;
const USERS_PAGE_SIZE = 20;

export default function AdminPage() {
    const navigate = useNavigate();

    // URL query parameter를 읽고/수정하기 위한 React Router hook.
    //
    // 예:
    // /admin?tab=contests
    //
    // searchParams.get("tab")으로 현재 탭 값을 읽고,
    // setSearchParams({ tab: "contests" })로 URL을 갱신한다.
    const [searchParams, setSearchParams] = useSearchParams();

    const [isLoggingOut, setIsLoggingOut] = useState(false);

    /**
     * URL query의 tab 값을 관리자 탭 타입으로 안전하게 변환한다.
     *
     * 예:
     * /admin?tab=contests → contests
     * /admin?tab=users    → users
     *
     * 잘못된 값이면 dashboard로 보낸다.
     */
    const getInitialAdminTab = (): AdminTab => {
        const tab = searchParams.get("tab");

        if (
            tab === "dashboard" ||
            tab === "reports" ||
            tab === "users" ||
            tab === "notices" ||
            tab === "contests"
        ) {
            return tab;
        }

        return "dashboard";
    };


    // activeTab은 URL query를 기준으로 초기화한다.
    //
    // 기존:
    // const [activeTab, setActiveTab] = useState<AdminTab>("dashboard");
    //
    // 문제:
    // 새로고침하면 항상 dashboard로 돌아감.
    //
    // 변경:
    // /admin?tab=contests면 contests로 초기화.
    const [activeTab, setActiveTab] = useState<AdminTab>(getInitialAdminTab);

    /**
     * 관리자 탭 변경 함수.
     *
     * 기존에는 setActiveTab만 호출해서 React 메모리에만 탭 상태가 저장됐다.
     * 그래서 새로고침하면 activeTab 초기값인 dashboard로 돌아갔다.
     *
     * 이제는 URL query에도 tab 값을 저장한다.
     *
     * 예:
     * setAdminTab("contests")
     * → activeTab = "contests"
     * → URL = /admin?tab=contests
     *
     * replace: true를 쓰는 이유:
     * - 탭 클릭할 때마다 브라우저 뒤로가기 기록이 쌓이는 것을 막기 위함.
     */
    const setAdminTab = (tab: AdminTab) => {
        setActiveTab(tab);

        setSearchParams(
            { tab },
            {
                replace: true,
            }
        );
    };


    // 대시보드용 전체 신고 목록
    const [dashboardReports, setDashboardReports] = useState<
        AdminContentReportResponse[]
    >([]);
    const [isDashboardReportsLoading, setIsDashboardReportsLoading] =
        useState(false);

    // ─────────────────────────────────────────────
    // 신고 관리 상태 (페이지네이션 + 필터 추가)
    // ─────────────────────────────────────────────
    const [reportStatus, setReportStatus] = useState<ReportStatusFilter>("pending");
    const [reportTargetType, setReportTargetType] =
        useState<ReportTargetTypeFilter>("all");
    const [reportReason, setReportReason] = useState<ReportReasonFilter>("all");
    const [reportKeyword, setReportKeyword] = useState("");
    const [debouncedReportKeyword, setDebouncedReportKeyword] = useState("");
    const [reportPage, setReportPage] = useState(1);

    const [reports, setReports] = useState<AdminContentReportResponse[]>([]);
    const [reportTotalCount, setReportTotalCount] = useState(0);
    const [selectedReport, setSelectedReport] =
        useState<AdminContentReportResponse | null>(null);
    const [isReportsLoading, setIsReportsLoading] = useState(false);
    // 신고일 날짜 범위 필터.
// input type="date" 값이므로 YYYY-MM-DD 문자열로 관리한다.
    const [reportStartDate, setReportStartDate] = useState("");
    const [reportEndDate, setReportEndDate] = useState("");

// 신고 목록 정렬 상태.
//
// created_at  : 신고일
// reviewed_at : 처리일
//
// 기본은 신고일 최신순.
    const [reportSortBy, setReportSortBy] = useState<ReportSortBy>("created_at");
    const [reportSortOrder, setReportSortOrder] =
        useState<ReportSortOrder>("desc");

    const handleReportSortChange = (nextSortBy: ReportSortBy) => {
        // 같은 정렬 기준을 다시 누르면 asc/desc 토글.
        if (nextSortBy === reportSortBy) {
            setReportSortOrder((prev) => (prev === "desc" ? "asc" : "desc"));
        } else {
            // 다른 정렬 기준으로 바꾸면 기본은 최신순 desc.
            setReportSortBy(nextSortBy);
            setReportSortOrder("desc");
        }

        setReportPage(1);
    };

    // ─────────────────────────────────────────────
    // 회원 관리 상태 (페이지네이션 추가)
    // ─────────────────────────────────────────────
    const [users, setUsers] = useState<AdminUserResponse[]>([]);
    const [userTotalCount, setUserTotalCount] = useState(0);
    const [userKeyword, setUserKeyword] = useState("");
    const [debouncedUserKeyword, setDebouncedUserKeyword] = useState("");
    const [userPage, setUserPage] = useState(1);
    const [isUsersLoading, setIsUsersLoading] = useState(false);

    // 공지사항 관리
    const [notices, setNotices] = useState<AdminNoticeResponse[]>([]);
    const [isNoticesLoading, setIsNoticesLoading] = useState(false);

    const [contests, setContests] = useState<AdminContestResponse[]>([]);
    const [isContestsLoading, setIsContestsLoading] = useState(false);

    // 공통 처리 상태
    const [processingId, setProcessingId] = useState<string | null>(null);

    // 비활성화 사유 입력 모달 대상 회원.
    const [inactiveTargetUser, setInactiveTargetUser] =
        useState<AdminUserResponse | null>(null);

// 비활성화 사유.
    const [inactiveReason, setInactiveReason] = useState("");

// 비활성 해제 예정일.
// datetime-local input 값.
// 예: "2026-05-20T18:00"
    const [inactiveUntil, setInactiveUntil] = useState("");

    // 대시보드 계산 값
    const pendingReportCount = useMemo(() => {
        return dashboardReports.filter((report) => report.status === "pending")
            .length;
    }, [dashboardReports]);

    // ─────────────────────────────────────────────
    // 대시보드 활성 회원 수 계산 (수정)
    // ─────────────────────────────────────────────
    //
    // 기존엔 users.filter()로 계산했는데, 이제 users는 페이지네이션된 일부만 있다.
    // 그래서 대시보드 활성 회원 수는 별도 처리가 필요.
    //
    // 간단히 처리하려면: totalUserCount는 userTotalCount를 그대로 사용.
    // 활성 회원 수까지 정확히 보려면 별도 API가 필요한데,
    // 졸작에서는 일단 "현재 페이지 활성 회원" 정도로만 표시하거나
    // 활성 회원 수 표시를 빼는 게 깔끔.
    //
    // 여기선 totalCount로 표시.
    const activeUserCount = useMemo(() => {
        return users.filter((u) => u.is_active && !u.deleted_at).length;
    }, [users]);

    const recentReports = useMemo(() => {
        return dashboardReports.slice(0, 5);
    }, [dashboardReports]);

    // ─────────────────────────────────────────────
    // 신고 필터 디바운스 (검색어만)
    // ─────────────────────────────────────────────
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedReportKeyword(reportKeyword.trim());
            setReportPage(1); // 검색어가 바뀌면 1페이지로
        }, 300);

        return () => window.clearTimeout(timer);
    }, [reportKeyword]);

    // 필터(셀렉트) 바뀌면 1페이지로
    useEffect(() => {
        setReportPage(1);
    }, [
        reportStatus,
        reportTargetType,
        reportReason,
        reportStartDate,
        reportEndDate,
        reportSortBy,
        reportSortOrder,
    ]);

    // 회원 검색 디바운스
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedUserKeyword(userKeyword.trim());
            setUserPage(1);
        }, 300);

        return () => window.clearTimeout(timer);
    }, [userKeyword]);

    // ─────────────────────────────────────────────
    // 대시보드용 신고는 별도 호출 (기존 유지)
    // ─────────────────────────────────────────────
    //
    // 대시보드는 페이지네이션 안 함.
    // 단순히 최근 5개 + pending 카운트만 필요.
    // listAdminContentReports의 응답 구조가 바뀌었으므로 .items로 접근.
    useEffect(() => {
        let ignore = false;

        async function fetchDashboardReports() {
            setIsDashboardReportsLoading(true);

            try {
                const data = await listAdminContentReports({ page: 1, page_size: 50 });

                if (!ignore) {
                    setDashboardReports(data.items);
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

    // ─────────────────────────────────────────────
    // 신고 관리 탭용 신고 목록 조회 (변경)
    // ─────────────────────────────────────────────
    useEffect(() => {
        let ignore = false;

        async function fetchReports() {
            setIsReportsLoading(true);

            try {
                const data = await listAdminContentReports({
                    status: reportStatus === "all" ? undefined : reportStatus,
                    target_type:
                        reportTargetType === "all" ? undefined : reportTargetType,
                    reason: reportReason === "all" ? undefined : reportReason,
                    keyword: debouncedReportKeyword || undefined,

                    // 신고일 날짜 범위
                    start_date: reportStartDate || undefined,
                    end_date: reportEndDate || undefined,

                    // 신고일/처리일 정렬
                    sort_by: reportSortBy,
                    sort_order: reportSortOrder,

                    page: reportPage,
                    page_size: REPORTS_PAGE_SIZE,
                });

                if (!ignore) {
                    setReports(data.items);
                    setReportTotalCount(data.total_count);
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
    }, [
        reportStatus,
        reportTargetType,
        reportReason,
        debouncedReportKeyword,
        reportStartDate,
        reportEndDate,
        reportSortBy,
        reportSortOrder,
        reportPage,
    ]);

    // ─────────────────────────────────────────────
    // 회원 목록 조회 (변경)
    // ─────────────────────────────────────────────
    useEffect(() => {
        let ignore = false;

        async function fetchUsers() {
            setIsUsersLoading(true);

            try {
                const data = await listAdminUsers({
                    keyword: debouncedUserKeyword || undefined,
                    page: userPage,
                    page_size: USERS_PAGE_SIZE,
                });

                if (!ignore) {
                    setUsers(data.items);
                    setUserTotalCount(data.total_count);
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
    }, [debouncedUserKeyword, userPage]);

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

        // 탈퇴한 회원은 활성/비활성 변경 불가.
        if (user.deleted_at) {
            toast.error("탈퇴한 회원은 활성/비활성 상태를 변경할 수 없습니다.");
            return;
        }

        // 운영자 계정은 활성/비활성 변경 불가.
        if (user.role_type === "admin") {
            toast.error("운영자 계정은 활성/비활성 상태를 변경할 수 없습니다.");
            return;
        }

        // 현재 활성 회원을 비활성화하려는 경우:
        // 바로 API를 호출하지 않고 사유/해제 예정일 입력 모달을 연다.
        if (user.is_active) {
            setInactiveTargetUser(user);
            setInactiveReason("");
            setInactiveUntil("");
            return;
        }

        // 현재 비활성 회원을 다시 활성화하는 경우:
        // 사유 입력 없이 바로 활성화한다.
        setProcessingId(user.id);

        try {
            const updated = await updateAdminUserActive(user.id, {
                is_active: true,
            });

            setUsers((prev) =>
                prev.map((item) => (item.id === user.id ? updated : item))
            );

            toast.success("회원이 활성화되었습니다.");
        } catch (error) {
            console.error("회원 활성화 실패:", error);
            toast.error(
                getAdminApiErrorMessage(error, "회원 활성화에 실패했습니다.")
            );
        } finally {
            setProcessingId(null);
        }
    };

    // 회원 비활성화 확정
//
// 이 함수는 비활성화 모달의 "비활성화" 버튼에서 호출된다.
//
// 처리 흐름:
// 1. 대상 회원 존재 확인
// 2. 사유 입력 검증
// 3. 해제 예정일을 ISO 문자열로 변환
// 4. 백엔드 PATCH /api/admin/users/:id/active 호출
// 5. 응답 받은 회원 row로 users 상태 갱신
// 6. 모달 닫기
    const handleConfirmDeactivateUser = async () => {
        if (!inactiveTargetUser || processingId) return;

        const reason = inactiveReason.trim();

        if (!reason) {
            toast.error("비활성화 사유를 입력해 주세요.");
            return;
        }

        const inactiveUntilIso = toIsoFromDateTimeLocal(inactiveUntil);

        setProcessingId(inactiveTargetUser.id);

        try {
            const updated = await updateAdminUserActive(inactiveTargetUser.id, {
                is_active: false,
                reason,
                inactive_until: inactiveUntilIso,
            });

            setUsers((prev) =>
                prev.map((item) =>
                    item.id === inactiveTargetUser.id ? updated : item
                )
            );

            toast.success("회원이 비활성화되었습니다.");

            setInactiveTargetUser(null);
            setInactiveReason("");
            setInactiveUntil("");
        } catch (error) {
            console.error("회원 비활성화 실패:", error);
            toast.error(
                getAdminApiErrorMessage(error, "회원 비활성화에 실패했습니다.")
            );
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
                    onTabChange={setAdminTab}
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
                            totalUserCount={userTotalCount}
                            activeUserCount={activeUserCount}
                            recentReports={recentReports}
                            isReportsLoading={isDashboardReportsLoading}
                            onTabChange={setAdminTab}
                        />
                    )}

                    {activeTab === "reports" && (
                        <AdminReportsPanel
                            reports={reports}
                            reportStatus={reportStatus}
                            targetTypeFilter={reportTargetType}
                            reasonFilter={reportReason}
                            keyword={reportKeyword}

                            // 날짜 필터
                            startDate={reportStartDate}
                            endDate={reportEndDate}

                            // 정렬
                            sortBy={reportSortBy}
                            sortOrder={reportSortOrder}

                            // 페이지네이션
                            page={reportPage}
                            totalCount={reportTotalCount}
                            pageSize={REPORTS_PAGE_SIZE}

                            isReportsLoading={isReportsLoading}
                            processingId={processingId}

                            onReportStatusChange={setReportStatus}
                            onTargetTypeChange={setReportTargetType}
                            onReasonChange={setReportReason}
                            onKeywordChange={setReportKeyword}

                            // 날짜 필터 변경
                            onStartDateChange={(value) => {
                                setReportStartDate(value);
                                setReportPage(1);
                            }}
                            onEndDateChange={(value) => {
                                setReportEndDate(value);
                                setReportPage(1);
                            }}

                            // 정렬 변경
                            onSortChange={handleReportSortChange}

                            onPageChange={setReportPage}
                            onSelectReport={setSelectedReport}
                            onResolveReport={handleResolveReport}
                            onRejectReport={handleRejectReport}
                        />
                    )}

                    {activeTab === "users" && (
                        <AdminUsersPanel
                            users={users}
                            userKeyword={userKeyword}
                            isUsersLoading={isUsersLoading}
                            processingId={processingId}
                            page={userPage}
                            totalCount={userTotalCount}
                            pageSize={USERS_PAGE_SIZE}
                            onPageChange={setUserPage}
                            onUserKeywordChange={setUserKeyword}
                            onToggleUserActive={(u) => void handleToggleUserActive(u)}
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

            {/* 회원 비활성화 사유/해제일 입력 모달 */}
            {inactiveTargetUser && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
                    <div className="w-full max-w-lg rounded-2xl border border-border bg-[var(--surface-elevated)] p-6 shadow-2xl">
                        <div className="mb-5">
                            <p className="text-xs font-bold uppercase tracking-[0.18em] text-luxury-gold">
                                Deactivate User
                            </p>

                            <h3 className="mt-1 text-xl font-extrabold">
                                회원 비활성화
                            </h3>

                        </div>

                        <div className="space-y-4">
                            <div className="rounded-xl bg-[var(--surface-subtle)] p-3 text-sm">
                                <p>
                        <span className="text-muted-foreground">
                            대상 회원:{" "}
                        </span>

                                    <span className="font-semibold">
                            {inactiveTargetUser.nickname ||
                                inactiveTargetUser.email ||
                                inactiveTargetUser.id}
                        </span>
                                </p>

                                {inactiveTargetUser.email && (
                                    <p className="mt-1 text-muted-foreground">
                                        {inactiveTargetUser.email}
                                    </p>
                                )}
                            </div>

                            <div>
                                <label className="mb-2 block text-sm font-semibold">
                                    비활성화 사유
                                </label>

                                <textarea
                                    value={inactiveReason}
                                    onChange={(e) => setInactiveReason(e.target.value)}
                                    placeholder="예: 부적절한 게시글 반복 작성"
                                    className="min-h-28 w-full rounded-xl border border-border bg-background px-3 py-3 text-sm outline-none transition focus:border-cyan-500"
                                />

                                <p className="mt-2 text-xs text-muted-foreground">
                                    사용자가 로그인 시도 시 이 사유가 안내 메시지에 포함됩니다.
                                </p>
                            </div>

                            <div>
                                <label className="mb-2 block text-sm font-semibold">
                                    해제 예정일
                                </label>

                                <input
                                    type="datetime-local"
                                    value={inactiveUntil}
                                    onChange={(e) => setInactiveUntil(e.target.value)}
                                    className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-cyan-500"
                                />

                                <p className="mt-2 text-xs text-muted-foreground">
                                    비워두면 해제 예정일은 “미정”으로 표시됩니다. 자동 해제는 하지 않습니다.
                                </p>
                            </div>
                        </div>

                        <div className="mt-6 flex justify-end gap-2">
                            <button
                                type="button"
                                onClick={() => {
                                    setInactiveTargetUser(null);
                                    setInactiveReason("");
                                    setInactiveUntil("");
                                }}
                                disabled={processingId === inactiveTargetUser.id}
                                className="rounded-lg border border-border px-4 py-2 text-sm font-semibold transition hover:bg-[var(--surface-subtle)] disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                취소
                            </button>

                            <button
                                type="button"
                                onClick={handleConfirmDeactivateUser}
                                disabled={processingId === inactiveTargetUser.id}
                                className="rounded-lg bg-red-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-red-600 disabled:cursor-not-allowed disabled:opacity-60"
                            >
                                {processingId === inactiveTargetUser.id
                                    ? "처리 중..."
                                    : "비활성화"}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}