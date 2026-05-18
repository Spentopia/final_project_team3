// src/domains/admin/components/AdminDashboard.tsx
//
// 관리자 대시보드.
//
// 역할:
// - 운영자가 처음 /admin에 들어왔을 때 빠르게 봐야 하는 지표 표시
// - 통계 카드 표시
// - 최근 7일 가입자 추이 그래프
// - 최근 7일 신고 접수 추이 그래프
// - 최근 신고 5개 표시
//
// 그래프 구현 방식:
// - 외부 차트 라이브러리 없이 CSS 막대 그래프로 구현한다.
// - Recharts 같은 라이브러리를 추가하지 않아도 되므로 의존성/타입 에러 위험이 낮다.
// - 시연용 운영 대시보드에는 충분히 깔끔하게 보인다.

import { Card } from "@/shared/ui/card";

import {
    Bar,
    BarChart,
    CartesianGrid,
    Line,
    LineChart,
    ResponsiveContainer,
    Tooltip,
    XAxis,
    YAxis,
} from "recharts";

import type {
    AdminContentReportResponse,
    AdminDashboardTrendPoint,
    AdminDashboardTrendsResponse,
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
    // 대시보드 통계 카드 데이터.
    dashboardStats: AdminDashboardStatsResponse | null;
    isDashboardStatsLoading: boolean;
    dashboardStatsError: string | null;

    // 대시보드 추이 그래프 데이터.
    dashboardTrends: AdminDashboardTrendsResponse | null;
    isDashboardTrendsLoading: boolean;
    dashboardTrendsError: string | null;

    // 새로고침 버튼 클릭 시 호출.
    // 현재는 통계 카드 + 추이 그래프를 함께 다시 불러온다.
    onRefreshDashboardStats: () => void;

    // 최근 신고 목록.
    recentReports: AdminContentReportResponse[];
    isReportsLoading: boolean;

    // 신고 관리 탭으로 이동할 때 사용.
    onTabChange: (tab: AdminTab) => void;
};

// 평균 신고 처리 시간을 화면용 문자열로 변환한다.
//
// 백엔드 값은 "분" 단위다.
// null이면 처리된 신고가 아직 없다는 의미로 "-"를 보여준다.
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

// YYYY-MM-DD 문자열을 그래프 라벨용 M/D 형태로 변환한다.
//
// 예:
// 2026-05-15 -> 5/15
const formatTrendDateLabel = (date: string): string => {
    const parts = date.split("-");

    if (parts.length !== 3) {
        return date;
    }

    const month = Number(parts[1]);
    const day = Number(parts[2]);

    if (Number.isNaN(month) || Number.isNaN(day)) {
        return date;
    }

    return `${month}/${day}`;
};

type TrendChartProps = {
    title: string;
    description: string;
    data: AdminDashboardTrendPoint[];
};

// Recharts Tooltip에 들어가는 날짜 라벨을 사람이 읽기 좋게 변환한다.
//
// payload로 들어오는 값은 백엔드에서 내려준 YYYY-MM-DD 문자열이다.
const formatTooltipLabel = (date: string): string => {
    return date;
};

// Recharts Tooltip 숫자 포맷.
//
// 기본 Tooltip은 숫자만 덜렁 보여서 의미가 약하다.
// "3건"처럼 보여주면 관리자 화면에서 더 직관적이다.
const formatTooltipValue = (value: number | string) => {
    if (typeof value === "number") {
        return [`${value.toLocaleString()}건`, "건수"];
    }

    return [value, "건수"];
};

// 최근 7일 가입자 추이 라인 차트.
//
// 시간 흐름의 증가/감소를 보는 지표는 실무 대시보드에서 보통 LineChart를 많이 쓴다.
// 가입자 수는 "추이"를 보는 성격이 강하므로 라인 차트가 가장 자연스럽다.
function SignupLineChart({ title, description, data }: TrendChartProps) {
    return (
        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
            <div className="mb-5">
                <h3 className="text-lg font-bold">{title}</h3>

                <p className="mt-1 text-sm text-muted-foreground">
                    {description}
                </p>
            </div>

            {/*
                ResponsiveContainer는 부모 높이가 반드시 있어야 보인다.
                그래서 h-72로 차트 영역 높이를 고정한다.
            */}
            <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart
                        data={data}
                        margin={{
                            top: 12,
                            right: 16,
                            left: -12,
                            bottom: 0,
                        }}
                    >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />

                        <XAxis
                            dataKey="date"
                            tickFormatter={formatTrendDateLabel}
                            tickLine={false}
                            axisLine={false}
                            fontSize={12}
                        />

                        <YAxis
                            allowDecimals={false}
                            tickLine={false}
                            axisLine={false}
                            fontSize={12}
                        />

                        <Tooltip
                            labelFormatter={formatTooltipLabel}
                            formatter={formatTooltipValue}
                            contentStyle={{
                                borderRadius: "12px",
                                border: "1px solid hsl(var(--border))",
                            }}
                        />

                        <Line
                            type="monotone"
                            dataKey="count"
                            stroke="#d4af37"
                            strokeWidth={3}
                            dot={{
                                r: 4,
                                strokeWidth: 2,
                            }}
                            activeDot={{
                                r: 6,
                            }}
                        />
                    </LineChart>
                </ResponsiveContainer>
            </div>
        </Card>
    );
}

// 최근 7일 신고 접수 추이 막대 차트.
//
// 신고 접수는 날짜별 발생 건수 비교 성격이 강하다.
// 그래서 라인보다 BarChart가 더 직관적이다.
function ReportBarChart({ title, description, data }: TrendChartProps) {
    return (
        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
            <div className="mb-5">
                <h3 className="text-lg font-bold">{title}</h3>

                <p className="mt-1 text-sm text-muted-foreground">
                    {description}
                </p>
            </div>

            {/* ResponsiveContainer는 부모 높이가 없으면 차트가 안 보인다. */}
            <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart
                        data={data}
                        margin={{
                            top: 12,
                            right: 16,
                            left: -12,
                            bottom: 0,
                        }}
                    >
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />

                        <XAxis
                            dataKey="date"
                            tickFormatter={formatTrendDateLabel}
                            tickLine={false}
                            axisLine={false}
                            fontSize={12}
                        />

                        <YAxis
                            allowDecimals={false}
                            tickLine={false}
                            axisLine={false}
                            fontSize={12}
                        />

                        <Tooltip
                            labelFormatter={formatTooltipLabel}
                            formatter={formatTooltipValue}
                            contentStyle={{
                                borderRadius: "12px",
                                border: "1px solid hsl(var(--border))",
                            }}
                        />

                        <Bar
                            dataKey="count"
                            fill="#f97316"
                            radius={[8, 8, 0, 0]}
                            barSize={28}
                        />
                    </BarChart>
                </ResponsiveContainer>
            </div>
        </Card>
    );
}

export default function AdminDashboard({
                                           dashboardStats,
                                           isDashboardStatsLoading,
                                           dashboardStatsError,
                                           dashboardTrends,
                                           isDashboardTrendsLoading,
                                           dashboardTrendsError,
                                           onRefreshDashboardStats,
                                           recentReports,
                                           isReportsLoading,
                                           onTabChange,
                                       }: AdminDashboardProps) {
    const statCards = dashboardStats
        ? [
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
        ]
        : [];

    return (
        <div className="space-y-6">
            {/* 요약 카드 */}
            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
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

                {!isDashboardStatsLoading &&
                    !dashboardStatsError &&
                    dashboardStats && (
                        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
                            {statCards.map((card) => (
                                <div
                                    key={card.label}
                                    className="rounded-2xl border border-border bg-white/60 p-4 dark:bg-gray-900/20"
                                >
                                    <p className="text-sm font-semibold text-muted-foreground">
                                        {card.label}
                                    </p>

                                    <p
                                        className={`mt-2 text-3xl font-extrabold ${card.colorClass}`}
                                    >
                                        {card.value}
                                    </p>

                                    <p className="mt-2 text-xs leading-5 text-muted-foreground">
                                        {card.description}
                                    </p>
                                </div>
                            ))}
                        </div>
                    )}

                {!isDashboardStatsLoading &&
                    !dashboardStatsError &&
                    !dashboardStats && (
                        <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                            표시할 대시보드 통계가 없습니다.
                        </div>
                    )}
            </Card>


            {/* 추이 그래프 */}
            {isDashboardTrendsLoading && (
                <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                    <p className="text-sm text-muted-foreground">
                        대시보드 추이 데이터를 불러오는 중입니다.
                    </p>
                </Card>
            )}

            {!isDashboardTrendsLoading && dashboardTrendsError && (
                <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                    <p className="text-sm text-rose-500">
                        {dashboardTrendsError}
                    </p>
                </Card>
            )}

            {!isDashboardTrendsLoading &&
                !dashboardTrendsError &&
                dashboardTrends && (
                    <div className="grid grid-cols-1 gap-4 xl:grid-cols-2">
                        <SignupLineChart
                            title="최근 7일 가입자 추이"
                            description="일별 신규 가입자 수를 라인 차트로 확인합니다."
                            data={dashboardTrends.user_signup_trend}
                        />

                        <ReportBarChart
                            title="최근 7일 신고 접수 추이"
                            description="일별 신고 접수 건수를 막대 차트로 확인합니다."
                            data={dashboardTrends.report_created_trend}
                        />
                    </div>
                )}

            {/* 최근 신고 */}
            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-4 flex items-center justify-between">
                    <div>
                        <h3 className="text-lg font-bold">최근 신고</h3>

                        <p className="mt-1 text-sm text-muted-foreground">
                            최근 접수된 신고를 빠르게 확인합니다.
                        </p>
                    </div>

                    <button
                        type="button"
                        onClick={() => onTabChange("reports")}
                        className="rounded-lg px-3 py-2 text-sm font-semibold text-cyan-600 transition hover:bg-cyan-50 dark:text-cyan-300 dark:hover:bg-cyan-950/40"
                    >
                        신고 관리로 이동
                    </button>
                </div>

                {isReportsLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        신고 목록을 불러오는 중입니다.
                    </div>
                )}

                {!isReportsLoading && recentReports.length === 0 && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        표시할 신고가 없습니다.
                    </div>
                )}

                {!isReportsLoading && recentReports.length > 0 && (
                    <div className="overflow-hidden rounded-2xl border border-border bg-white/60 dark:bg-gray-900/20">
                        <table className="w-full table-fixed text-sm">
                            <colgroup>
                                <col className="w-[120px]" />
                                <col className="w-[130px]" />
                                <col className="w-[160px]" />
                                <col className="w-[180px]" />
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3">상태</th>
                                <th className="px-4 py-3">대상</th>
                                <th className="px-4 py-3">사유</th>
                                <th className="px-4 py-3">신고일</th>
                            </tr>
                            </thead>

                            <tbody>
                            {recentReports.map((report) => (
                                <tr
                                    key={report.id}
                                    className="border-t border-border transition-colors hover:bg-[var(--surface-subtle)]/70"
                                >
                                    <td className="px-4 py-3 align-middle">
                                            <span
                                                className={`inline-flex rounded-full px-2 py-1 text-xs font-bold ${
                                                    REPORT_STATUS_STYLE[report.status]
                                                }`}
                                            >
                                                {REPORT_STATUS_LABEL[report.status]}
                                            </span>
                                    </td>

                                    <td className="px-4 py-3 align-middle">
                                            <span className="block truncate">
                                                {TARGET_TYPE_LABEL[report.target_type]}
                                            </span>
                                    </td>

                                    <td className="px-4 py-3 align-middle">
                                            <span className="block truncate">
                                                {REASON_LABEL[report.reason]}
                                            </span>
                                    </td>

                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                                            <span className="block truncate">
                                                {formatDateTime(report.created_at)}
                                            </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>
        </div>
    );
}