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
//
// 탭 상태 관리 방식:
// - 기존에는 activeTab을 useState로 따로 관리하고 URL query와 수동 동기화했다.
// - 이제는 Community.tsx와 동일하게 URL query가 진실의 원천이다.
// - 즉 activeTab은 useState가 아니라 searchParams.get("tab")에서 매 렌더링마다 계산한다.
//
// 예:
// - /admin                 → dashboard
// - /admin?tab=reports     → reports
// - /admin?tab=users       → users
// - /admin?tab=notices     → notices
// - /admin?tab=contests    → contests
//
// 장점:
// - 새로고침해도 현재 탭 유지
// - URL 공유 가능
// - 브라우저 뒤로가기/앞으로가기와 자연스럽게 동작
// - Community.tsx의 URL 기반 상태 관리 방식과 통일됨

import { useEffect, useMemo, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import { toast } from "sonner";

import { signOut } from "@/domains/auth/api/auth";

import AdminSidebar from "@/domains/admin/components/AdminSidebar.tsx";
import AdminDashboard from "@/domains/admin/components/AdminDashboard.tsx";
import AdminReportDetailModal from "@/domains/admin/components/AdminReportDetailModal.tsx";
import AdminUsersPanel from "@/domains/admin/components/AdminUsersPanel.tsx";
import AdminNoticesPanel from "@/domains/admin/components/AdminNoticesPanel.tsx";
import AdminContestsPanel from "@/domains/admin/components/AdminContestsPanel.tsx";
import AdminWithdrawnUsersPanel from "@/domains/admin/components/AdminWithdrawnUsersPanel.tsx";
import AdminReportsPanel, {
    type ReportReasonFilter,
    type ReportTargetTypeFilter,
    type ReportSortBy,
    type ReportSortOrder,
} from "@/domains/admin/components/AdminReportsPanel";

import {
    applyAdminContentReportAction,
    createAdminNotice,
    createAdminContest,
    deleteAdminNotice,
    getAdminDashboardStats,
    getAdminDashboardTrends,
    listAdminContentReports,
    listAdminContests,
    listAdminNotices,
    listAdminUsers,
    listAdminWithdrawnUsers,
    rejectAdminContentReport,
    resolveAdminContentReport,
    updateAdminContest,
    updateAdminContestStatus,
    updateAdminNotice,
    updateAdminUserActive,
    type AdminContentReportResponse,
    type AdminContestResponse,
    type AdminContestStatus,
    type AdminDashboardStatsResponse,
    type AdminDashboardTrendsResponse,
    type AdminReportAction,
    type AdminNoticeResponse,
    type AdminUserResponse,
    type AdminWithdrawnUserResponse,
    type ResolveReportActionType,
} from "@/domains/admin/api/adminApi";

import type {
    AdminTab,
    ReportStatusFilter,
} from "@/domains/admin/types/adminViewTypes";

// ─────────────────────────────────────────────
// 관리자 탭 URL 파싱 헬퍼
// ─────────────────────────────────────────────
//
// URL query의 tab 값을 AdminTab으로 안전하게 변환한다.
//
// 허용값:
// - dashboard
// - reports
// - users
// - notices
// - contests
//
// 잘못된 값이 들어오면 dashboard로 fallback한다.
//
// 예:
// /admin?tab=reports  → reports
// /admin?tab=unknown  → dashboard
// /admin              → dashboard
function getAdminTabFromUrl(tab: string | null): AdminTab {
    if (
        tab === "dashboard" ||
        tab === "reports" ||
        tab === "users" ||
        tab === "withdrawn" ||
        tab === "notices" ||
        tab === "contests"
    ) {
        return tab;
    }

    return "dashboard";
}

function getReportActionSuccessMessage(action: AdminReportAction): string {
    switch (action) {
        case "delete_post":
            return "게시글을 삭제하고 신고를 처리 완료했습니다.";
        case "delete_comment":
            return "댓글을 삭제하고 신고를 처리 완료했습니다.";
        case "clear_profile_image":
            return "프로필 사진을 기본 이미지로 변경하고 신고를 처리 완료했습니다.";
        case "request_profile_image_change":
            return "프로필 사진 변경 요청 조치를 처리 완료했습니다.";
        case "request_nickname_change":
            return "닉네임 변경 요청 조치를 처리 완료했습니다.";
        default:
            return "운영 조치를 완료했습니다.";
    }
}

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

// datetime-local input 값은 보통 "2026-05-20T18:00" 형태다.
// 백엔드에는 ISO 문자열로 보내는 게 안전하므로 Date로 변환 후 toISOString()을 사용한다.
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
const WITHDRAWN_USERS_PAGE_SIZE = 20;

export default function AdminPage() {
    const navigate = useNavigate();

    // URL query parameter를 읽고/수정하기 위한 React Router hook.
    //
    // 예:
    // /admin?tab=reports
    //
    // searchParams.get("tab")으로 현재 탭 값을 읽고,
    // setSearchParams(...)로 URL을 갱신한다.
    const [searchParams, setSearchParams] = useSearchParams();

    // ─────────────────────────────────────────────
    // URL 기반 관리자 탭 상태
    // ─────────────────────────────────────────────
    //
    // activeTab은 useState로 따로 관리하지 않는다.
    // URL query string이 진실의 원천이다.
    //
    // 기존:
    // const [activeTab, setActiveTab] = useState<AdminTab>(getInitialAdminTab);
    //
    // 변경:
    // const activeTab = getAdminTabFromUrl(searchParams.get("tab"));
    //
    // 이렇게 하면 브라우저 뒤로가기/앞으로가기로 URL의 tab이 바뀌어도
    // activeTab이 즉시 URL을 따라간다.
    const activeTab: AdminTab = getAdminTabFromUrl(searchParams.get("tab"));

    const [isLoggingOut, setIsLoggingOut] = useState(false);

    // 관리자 탭 변경 함수.
    //
    // Community.tsx의 updateSearchParams와 같은 방향이다.
    // React state를 직접 바꾸지 않고 URL만 바꾼다.
    // 그러면 activeTab은 위에서 searchParams를 기준으로 다시 계산된다.
    //
    // dashboard는 기본 탭이므로 URL을 깔끔하게 하기 위해 tab 파라미터를 제거한다.
    //
    // 예:
    // setAdminTab("reports")   → /admin?tab=reports
    // setAdminTab("dashboard") → /admin
    const setAdminTab = (tab: AdminTab) => {
        const next = new URLSearchParams(searchParams);

        if (tab === "dashboard") {
            next.delete("tab");
        } else {
            next.set("tab", tab);
        }

        setSearchParams(next, { replace: true });
    };

    // ─────────────────────────────────────────────
    // 대시보드 통계 상태
    // ─────────────────────────────────────────────
    //
    // dashboardStats:
    // - GET /api/admin/dashboard/stats 응답.
    // - 전체 회원 수, 활성/비활성/탈퇴 회원 수,
    //   신고 상태별 수, 평균 신고 처리 시간을 담는다.
    //
    // isDashboardStatsLoading:
    // - 통계 카드 영역 로딩 표시용.
    //
    // dashboardStatsError:
    // - 통계 조회 실패 시 카드 영역에 표시할 에러 메시지.
    const [dashboardStats, setDashboardStats] =
        useState<AdminDashboardStatsResponse | null>(null);

    const [isDashboardStatsLoading, setIsDashboardStatsLoading] =
        useState(false);

    const [dashboardStatsError, setDashboardStatsError] =
        useState<string | null>(null);

    // ─────────────────────────────────────────────
    // 대시보드 추이 그래프 상태
    // ─────────────────────────────────────────────
    //
    // dashboardTrends:
    // - GET /api/admin/dashboard/trends 응답.
    // - 최근 7일 가입자 추이와 신고 접수 추이를 담는다.
    //
    // isDashboardTrendsLoading:
    // - 그래프 영역 로딩 표시용.
    //
    // dashboardTrendsError:
    // - 그래프 조회 실패 시 그래프 영역에 표시할 에러 메시지.
    const [dashboardTrends, setDashboardTrends] =
        useState<AdminDashboardTrendsResponse | null>(null);

    const [isDashboardTrendsLoading, setIsDashboardTrendsLoading] =
        useState(false);

    const [dashboardTrendsError, setDashboardTrendsError] =
        useState<string | null>(null);

    // ─────────────────────────────────────────────
    // 대시보드 최근 신고 목록 상태
    // ─────────────────────────────────────────────
    //
    // dashboardReports:
    // - 대시보드의 "최근 신고" 테이블에 보여줄 신고 목록.
    // - 통계 카드와는 별개다.
    // - 통계 카드는 GET /api/admin/dashboard/stats로 가져오고,
    //   최근 신고 목록은 기존 listAdminContentReports로 가져온다.
    //
    // isDashboardReportsLoading:
    // - 최근 신고 목록 영역 로딩 표시용.
    // - 신고 관리 탭의 isReportsLoading과 구분한다.
    const [dashboardReports, setDashboardReports] = useState<
        AdminContentReportResponse[]
    >([]);

    const [isDashboardReportsLoading, setIsDashboardReportsLoading] =
        useState(false);

    // ─────────────────────────────────────────────
    // 신고 관리 상태
    // ─────────────────────────────────────────────
    //
    // 이 필터들은 아직 URL 동기화하지 않는다.
    // 현재 요청은 관리자 탭 activeTab만 커뮤니티 방식과 통일하는 것이다.
    //
    // 나중에 필요하면 reportStatus/reportTargetType/reportReason/page도
    // ?status=pending&page=2 같은 식으로 URL 동기화할 수 있다.
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

        // 정렬이 바뀌면 1페이지로 돌린다.
        setReportPage(1);
    };

    // ─────────────────────────────────────────────
    // 회원 관리 상태
    // ─────────────────────────────────────────────
    const [users, setUsers] = useState<AdminUserResponse[]>([]);
    const [userTotalCount, setUserTotalCount] = useState(0);
    const [userKeyword, setUserKeyword] = useState("");
    const [debouncedUserKeyword, setDebouncedUserKeyword] = useState("");
    const [userPage, setUserPage] = useState(1);
    const [isUsersLoading, setIsUsersLoading] = useState(false);

    // ─────────────────────────────────────────────
    // 탈퇴 회원 모니터링 상태
    // ─────────────────────────────────────────────
    //
    // withdrawnUsers:
    // - deleted_at이 있는 탈퇴 회원 목록.
    //
    // withdrawnUserKeyword:
    // - 닉네임/이메일 검색어.
    //
    // debouncedWithdrawnUserKeyword:
    // - API 과호출 방지를 위한 디바운스 검색어.
    //
    // withdrawnUserPage:
    // - 탈퇴 회원 목록 페이지.
    //
    // withdrawnUserTotalCount:
    // - 전체 탈퇴 회원 수.
    //
    // isWithdrawnUsersLoading:
    // - 목록 로딩 상태.
    const [withdrawnUsers, setWithdrawnUsers] = useState<
        AdminWithdrawnUserResponse[]
    >([]);

    const [withdrawnUserKeyword, setWithdrawnUserKeyword] = useState("");
    const [debouncedWithdrawnUserKeyword, setDebouncedWithdrawnUserKeyword] =
        useState("");

    const [withdrawnUserPage, setWithdrawnUserPage] = useState(1);
    const [withdrawnUserTotalCount, setWithdrawnUserTotalCount] = useState(0);
    const [isWithdrawnUsersLoading, setIsWithdrawnUsersLoading] = useState(false);

    // 공지사항 관리
    const [notices, setNotices] = useState<AdminNoticeResponse[]>([]);
    const [isNoticesLoading, setIsNoticesLoading] = useState(false);

    // 콘테스트 관리
    const [contests, setContests] = useState<AdminContestResponse[]>([]);
    const [isContestsLoading, setIsContestsLoading] = useState(false);

    // 공통 처리 상태.
    // 신고 처리, 회원 활성화, 공지 삭제 등에서 중복 클릭을 막기 위해 사용한다.
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

    const recentReports = useMemo(() => {
        return dashboardReports.slice(0, 5);
    }, [dashboardReports]);

    // ─────────────────────────────────────────────
    // 신고 검색어 디바운스
    // ─────────────────────────────────────────────
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedReportKeyword(reportKeyword.trim());
            setReportPage(1);
        }, 300);

        return () => window.clearTimeout(timer);
    }, [reportKeyword]);

    // 신고 필터가 바뀌면 1페이지로 이동.
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

    // 회원 검색어 디바운스.
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedUserKeyword(userKeyword.trim());
            setUserPage(1);
        }, 300);

        return () => window.clearTimeout(timer);
    }, [userKeyword]);

    // 탈퇴 회원 검색어 디바운스.
    useEffect(() => {
        const timer = window.setTimeout(() => {
            setDebouncedWithdrawnUserKeyword(withdrawnUserKeyword.trim());
            setWithdrawnUserPage(1);
        }, 300);

        return () => window.clearTimeout(timer);
    }, [withdrawnUserKeyword]);

    // ─────────────────────────────────────────────
    // 대시보드 통계 조회
    // ─────────────────────────────────────────────
    //
    // 관리자 대시보드 카드에 표시할 운영 지표를 가져온다.
    //
    // 가져오는 값:
    // - 전체 회원 수
    // - 활성 회원 수
    // - 비활성 회원 수
    // - 탈퇴 회원 수
    // - 대기중 신고 수
    // - 처리완료 신고 수
    // - 반려 신고 수
    // - 평균 신고 처리 시간
    //
    // 실패해도 관리자 페이지 전체가 깨지지 않도록
    // 통계 영역에만 에러 메시지를 표시한다.
    const fetchDashboardStats = async () => {
        setIsDashboardStatsLoading(true);
        setDashboardStatsError(null);

        try {
            const data = await getAdminDashboardStats();

            setDashboardStats(data);
        } catch (error) {
            console.error("관리자 대시보드 통계 조회 실패:", error);

            setDashboardStats(null);
            setDashboardStatsError("대시보드 통계를 불러오지 못했습니다.");
        } finally {
            setIsDashboardStatsLoading(false);
        }
    };

    // ─────────────────────────────────────────────
    // 대시보드 추이 그래프 조회
    // ─────────────────────────────────────────────
    //
    // 관리자 대시보드의 그래프에 표시할 최근 7일 추이 데이터를 가져온다.
    //
    // 가져오는 값:
    // - 최근 7일 가입자 추이
    // - 최근 7일 신고 접수 추이
    //
    // 실패해도 대시보드 전체가 깨지지 않도록
    // 그래프 영역에만 에러 메시지를 표시한다.
    const fetchDashboardTrends = async () => {
        setIsDashboardTrendsLoading(true);
        setDashboardTrendsError(null);

        try {
            const data = await getAdminDashboardTrends();

            setDashboardTrends(data);
        } catch (error) {
            console.error("관리자 대시보드 추이 조회 실패:", error);

            setDashboardTrends(null);
            setDashboardTrendsError("대시보드 추이 데이터를 불러오지 못했습니다.");
        } finally {
            setIsDashboardTrendsLoading(false);
        }
    };

    // 관리자 페이지 진입 시 대시보드 통계를 한 번 조회한다.
    //
    // activeTab이 dashboard일 때만 조회해도 되지만,
    // 관리자 첫 진입 화면이 dashboard이므로 여기서는 mount 시 1회 조회한다.
    // 새로고침 버튼은 AdminDashboard에서 onRefreshDashboardStats로 다시 호출한다.
    useEffect(() => {
        void fetchDashboardStats();
        void fetchDashboardTrends();
    }, []);

    // ─────────────────────────────────────────────
    // 대시보드용 신고 목록 조회
    // ─────────────────────────────────────────────
    //
    // 대시보드는 페이지네이션 UI를 보여주지 않는다.
    // 최근 신고 몇 개와 pending 카운트만 필요하므로 page_size 50 정도로 조회한다.
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
    // 신고 관리 탭용 신고 목록 조회
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
    // 회원 목록 조회
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

    // ─────────────────────────────────────────────
// 탈퇴 회원 목록 조회
// ─────────────────────────────────────────────
//
// 탈퇴 회원 모니터링 탭에서 사용하는 목록.
// users.deleted_at is not null 인 회원만 조회한다.
    useEffect(() => {
        let ignore = false;

        async function fetchWithdrawnUsers() {
            setIsWithdrawnUsersLoading(true);

            try {
                const data = await listAdminWithdrawnUsers({
                    keyword: debouncedWithdrawnUserKeyword || undefined,
                    page: withdrawnUserPage,
                    page_size: WITHDRAWN_USERS_PAGE_SIZE,
                });

                if (!ignore) {
                    setWithdrawnUsers(data.items);
                    setWithdrawnUserTotalCount(data.total_count);
                }
            } catch (error) {
                console.error("관리자 탈퇴 회원 목록 조회 실패:", error);
                toast.error("탈퇴 회원 목록을 불러오지 못했습니다.");
            } finally {
                if (!ignore) {
                    setIsWithdrawnUsersLoading(false);
                }
            }
        }

        void fetchWithdrawnUsers();

        return () => {
            ignore = true;
        };
    }, [debouncedWithdrawnUserKeyword, withdrawnUserPage]);

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

    // 콘테스트 목록 조회
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

    // ─────────────────────────────────────────────
// 신고 처리 후 현재 목록 상태 동기화
// ─────────────────────────────────────────────
//
// 신고 처리/반려/운영 조치 후에는 updated.status가 바뀐다.
//
// 문제:
// - 현재 필터가 "pending"인데 신고를 resolved/rejected로 바꿔도,
//   단순 map 교체만 하면 pending 목록에 계속 남아 보인다.
//
// 해결:
// - updated row가 현재 필터 조건에 맞으면 목록에서 교체
// - 현재 필터 조건에 맞지 않으면 목록에서 제거
//
// 예:
// - reportStatus = "pending"
// - updated.status = "resolved"
// → 현재 목록에서 제거
    const syncUpdatedReportInCurrentList = (updated: AdminContentReportResponse) => {
        const matchesCurrentFilter = (() => {
            // 상태 필터
            if (reportStatus !== "all" && updated.status !== reportStatus) {
                return false;
            }

            // 신고 대상 타입 필터
            if (
                reportTargetType !== "all" &&
                updated.target_type !== reportTargetType
            ) {
                return false;
            }

            // 신고 사유 필터
            if (reportReason !== "all" && updated.reason !== reportReason) {
                return false;
            }

            return true;
        })();

        setReports((prev) => {
            const existsInCurrentPage = prev.some((report) => report.id === updated.id);

            // 현재 필터에 맞지 않으면 목록에서 제거한다.
            if (!matchesCurrentFilter) {
                return prev.filter((report) => report.id !== updated.id);
            }

            // 현재 필터에 맞고 현재 페이지에 있으면 updated로 교체한다.
            if (existsInCurrentPage) {
                return prev.map((report) =>
                    report.id === updated.id ? updated : report
                );
            }

            // 현재 페이지에 없던 신고를 갑자기 추가하지는 않는다.
            // 페이지네이션/정렬이 꼬일 수 있기 때문이다.
            return prev;
        });

        // 현재 필터에서 빠지는 경우 totalCount도 1 감소시킨다.
        //
        // 예:
        // pending 목록 총 15건
        // 하나를 resolved 처리
        // → pending 총 14건
        if (!matchesCurrentFilter) {
            setReportTotalCount((prev) => Math.max(0, prev - 1));
        }

        // 대시보드용 신고 목록은 필터 목록이 아니므로 단순 교체.
        setDashboardReports((prev) =>
            prev.map((report) => (report.id === updated.id ? updated : report))
        );

        // 상세 모달도 최신 상태로 갱신.
        setSelectedReport((current) =>
            current?.id === updated.id ? updated : current
        );
    };


    // 신고 운영 조치 적용.
    //
    // 게시글 삭제, 댓글 삭제, 프로필 이미지 제거, 변경 요청 처리 같은
    // 실제 운영 액션을 처리한다.
    //
    // 알림 제외 버전이므로 request_* action은
    // 사용자 알림을 보내지는 않고, 신고 처리완료 + 감사 로그만 남긴다.
    const handleApplyReportAction = async (
        reportId: string,
        action: AdminReportAction
    ) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await applyAdminContentReportAction(reportId, {
                action,
            });

            syncUpdatedReportInCurrentList(updated);
            setSelectedReport(null);

            toast.success(getReportActionSuccessMessage(action));
        } catch (error) {
            console.error("신고 운영 조치 실패:", error);
            toast.error(
                getAdminApiErrorMessage(error, "신고 운영 조치에 실패했습니다.")
            );
        } finally {
            setProcessingId(null);
        }
    };

    // 신고 처리 완료
    const handleResolveReport = async (
        reportId: string,
        actionType: ResolveReportActionType = "no_action",
    ) => {
        if (processingId) return;

        setProcessingId(reportId);

        try {
            const updated = await resolveAdminContentReport(reportId, actionType);

            syncUpdatedReportInCurrentList(updated);
            setSelectedReport(null);

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

            syncUpdatedReportInCurrentList(updated);
            setSelectedReport(null);

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

    // 콘테스트 생성
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

    // 콘테스트 수정
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

    // 콘테스트 상태 변경
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
        <div className="h-screen overflow-hidden bg-[var(--surface)] text-foreground">
            <div className="flex h-screen">
                <AdminSidebar
                    activeTab={activeTab}
                    onTabChange={setAdminTab}
                    isLoggingOut={isLoggingOut}
                    onLogout={() => void handleLogout()}
                />

                <main className="h-screen flex-1 overflow-y-auto p-8">
                    <div className="mb-8">
                        <p className="text-[11px] font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Admin Console
                        </p>

                        <h2 className="mt-1 text-3xl font-extrabold">
                            {activeTab === "dashboard" && "대시보드"}
                            {activeTab === "reports" && "신고 관리"}
                            {activeTab === "users" && "회원 관리"}
                            {activeTab === "withdrawn" && "탈퇴 회원 모니터링"}
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
                            {activeTab === "withdrawn" &&
                                "탈퇴 회원의 보관 기간과 재가입 제한 상태를 확인합니다."}
                            {activeTab === "notices" &&
                                "공지사항을 작성, 수정, 삭제합니다."}
                            {activeTab === "contests" &&
                                "아바타 콘테스트를 생성하고 상태를 관리합니다."}
                        </p>
                    </div>

                    {activeTab === "dashboard" && (
                        <AdminDashboard
                            dashboardStats={dashboardStats}
                            isDashboardStatsLoading={isDashboardStatsLoading}
                            dashboardStatsError={dashboardStatsError}
                            dashboardTrends={dashboardTrends}
                            isDashboardTrendsLoading={isDashboardTrendsLoading}
                            dashboardTrendsError={dashboardTrendsError}
                            onRefreshDashboardStats={() => {
                                void fetchDashboardStats();
                                void fetchDashboardTrends();
                            }}
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
                            startDate={reportStartDate}
                            endDate={reportEndDate}
                            sortBy={reportSortBy}
                            sortOrder={reportSortOrder}
                            page={reportPage}
                            totalCount={reportTotalCount}
                            pageSize={REPORTS_PAGE_SIZE}
                            isReportsLoading={isReportsLoading}
                            onReportStatusChange={setReportStatus}
                            onTargetTypeChange={setReportTargetType}
                            onReasonChange={setReportReason}
                            onKeywordChange={setReportKeyword}
                            onStartDateChange={(value) => {
                                setReportStartDate(value);
                                setReportPage(1);
                            }}
                            onEndDateChange={(value) => {
                                setReportEndDate(value);
                                setReportPage(1);
                            }}
                            onSortChange={handleReportSortChange}
                            onPageChange={setReportPage}
                            onSelectReport={setSelectedReport}
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

                    {activeTab === "withdrawn" && (
                        <AdminWithdrawnUsersPanel
                            users={withdrawnUsers}
                            keyword={withdrawnUserKeyword}
                            isLoading={isWithdrawnUsersLoading}
                            page={withdrawnUserPage}
                            totalCount={withdrawnUserTotalCount}
                            pageSize={WITHDRAWN_USERS_PAGE_SIZE}
                            onKeywordChange={setWithdrawnUserKeyword}
                            onPageChange={setWithdrawnUserPage}
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
                    onApplyAction={(reportId, action) =>
                        void handleApplyReportAction(reportId, action)
                    }
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
