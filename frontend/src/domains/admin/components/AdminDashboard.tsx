// src/domains/admin/components/AdminDashboard.tsx
//
// 관리자 대시보드.
//
// 역할:
// - 운영자가 처음 /admin에 들어왔을 때 빠르게 봐야 하는 지표 표시
// - 처리 대기 신고 수
// - 전체 회원 수
// - 활성 회원 수
// - 최근 신고 5개 표시
//
// 이번 수정:
// - 최근 신고 테이블을 더 부드러운 관리자 테이블 스타일로 변경
// - table-fixed + colgroup 적용
// - row hover 적용
// - 긴 텍스트 truncate 처리

import { Card } from "@/shared/ui/card";

import type {
    AdminContentReportResponse,
    AdminDashboardStatsResponse,
} from "@/domains/admin/api/adminApi";
import type { AdminTab } from "@/domains/admin/types/adminViewTypes";

import {
    formatDateTime,
    REASON_LABEL,
    REPORT_STATUS_LABEL,
    REPORT_STATUS_STYLE,
    TARGET_TYPE_LABEL,
} from "@/domains/admin/utils/adminViewUtils";

type AdminDashboardProps = {
    // 백엔드 GET /api/admin/dashboard/stats 응답.
    // 아직 로딩 전이거나 조회 실패 시 null일 수 있다.
    dashboardStats: AdminDashboardStatsResponse | null;

    // 통계 카드 로딩 여부.
    isDashboardStatsLoading: boolean;

    // 통계 조회 실패 메시지.
    dashboardStatsError: string | null;

    // 통계 새로고침 버튼 클릭 시 호출.
    onRefreshDashboardStats: () => void;

    // 최근 신고 5개.
    recentReports: AdminContentReportResponse[];

    // 최근 신고 목록 로딩 여부.
    isReportsLoading: boolean;

    // 사이드바/탭 전환.
    onTabChange: (tab: AdminTab) => void;
};

// 평균 신고 처리 시간을 화면에 보여줄 문자열로 변환한다.
//
// 백엔드 값은 "분" 단위다.
// null이면 아직 처리된 신고가 없다는 의미로 "-" 표시.
const formatAverageReportHandleTime = (minutes: number | null): string => {
    if (minutes == null) {
        return "-";
    }

    if (minutes < 60) {
        return `${minutes}분`;
    }

    const hours = Math.floor(minutes / 60);
    const restMinutes = minutes % 60;

    if (restMinutes === 0) {
        return `${hours}시간`;
    }

    return `${hours}시간 ${restMinutes}분`;
};

export default function AdminDashboard({
                                           dashboardStats,
                                           isDashboardStatsLoading,
                                           dashboardStatsError,
                                           onRefreshDashboardStats,
                                           recentReports,
                                           isReportsLoading,
                                           onTabChange,
                                       }: AdminDashboardProps) {
    return (
        <>
            {/* 요약 카드 */}
            <Card className="mb-6 border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
                    <div>
                        <h3 className="text-lg font-bold">운영 통계 대시보드</h3>
                        <p className="mt-1 text-sm text-muted-foreground">
                            회원 상태와 신고 처리 현황을 한눈에 확인합니다.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={onRefreshDashboardStats}
                        className="rounded-lg px-3 py-2 text-sm font-semibold text-cyan-600 transition hover:bg-cyan-50 dark:text-cyan-300 dark:hover:bg-cyan-950/40"
                    >
                        새로고침
                    </button>
                </div>

                {isDashboardStatsLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        대시보드 통계를 불러오는 중입니다.
                    </div>
                )}

                {!isDashboardStatsLoading && dashboardStatsError && (
                    <div className="rounded-xl border border-rose-200 bg-rose-50 p-4 text-sm text-rose-600 dark:border-rose-900/50 dark:bg-rose-950/30 dark:text-rose-300">
                        {dashboardStatsError}
                    </div>
                )}

                {!isDashboardStatsLoading && !dashboardStatsError && dashboardStats && (
                    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                        {[
                            {
                                label: "전체 회원",
                                value: dashboardStats.total_users.toLocaleString(),
                                colorClass: "text-cyan-500",
                                description: "탈퇴 회원 포함 전체 가입 row",
                            },
                            {
                                label: "활성 회원",
                                value: dashboardStats.active_users.toLocaleString(),
                                colorClass: "text-emerald-500",
                                description: "정상 이용 가능한 회원",
                            },
                            {
                                label: "비활성 회원",
                                value: dashboardStats.inactive_users.toLocaleString(),
                                colorClass: "text-orange-500",
                                description: "운영자가 이용 제한한 회원",
                            },
                            {
                                label: "탈퇴 회원",
                                value: dashboardStats.withdrawn_users.toLocaleString(),
                                colorClass: "text-gray-500",
                                description: "deleted_at이 기록된 회원",
                            },
                            {
                                label: "대기중 신고",
                                value: dashboardStats.pending_reports.toLocaleString(),
                                colorClass: "text-yellow-500",
                                description: "아직 처리되지 않은 신고",
                            },
                            {
                                label: "처리완료 신고",
                                value: dashboardStats.resolved_reports.toLocaleString(),
                                colorClass: "text-emerald-500",
                                description: "운영 검토가 완료된 신고",
                            },
                            {
                                label: "반려 신고",
                                value: dashboardStats.rejected_reports.toLocaleString(),
                                colorClass: "text-rose-500",
                                description: "정책 위반으로 보기 어려운 신고",
                            },
                            {
                                label: "평균 신고 처리 시간",
                                value: formatAverageReportHandleTime(
                                    dashboardStats.average_report_handle_minutes,
                                ),
                                colorClass: "text-violet-500",
                                description: "reviewed_at - created_at 평균",
                            },
                        ].map((card) => (
                            <div
                                key={card.label}
                                className="rounded-2xl border border-border bg-white/60 p-4 dark:bg-gray-900/20"
                            >
                                <p className="text-sm font-semibold text-muted-foreground">
                                    {card.label}
                                </p>

                                <p className={`mt-2 text-3xl font-extrabold ${card.colorClass}`}>
                                    {card.value}
                                </p>

                                <p className="mt-2 text-xs leading-5 text-muted-foreground">
                                    {card.description}
                                </p>
                            </div>
                        ))}
                    </div>
                )}

                {!isDashboardStatsLoading && !dashboardStatsError && !dashboardStats && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        표시할 대시보드 통계가 없습니다.
                    </div>
                )}
            </Card>
        </>
    );
}