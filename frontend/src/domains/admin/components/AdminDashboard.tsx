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
// 데이터 조회는 여기서 하지 않는다.
// 상위 AdminPage가 조회한 reports/users를 props로 넘겨준다.
// 이렇게 하면 데이터 흐름이 한 곳(AdminPage)에 모여서 관리하기 쉽다.

import { Card } from "@/shared/ui/card";

import type { AdminContentReportResponse } from "@/domains/admin/api/adminApi";
import type { AdminTab } from "@/domains/admin/types/adminViewTypes";

import {
    formatDateTime,
    getTextValue,
    REASON_LABEL,
    REPORT_STATUS_LABEL,
    REPORT_STATUS_STYLE,
    TARGET_TYPE_LABEL,
} from "@/domains/admin/utils/adminViewUtils";

type AdminDashboardProps = {
    pendingReportCount: number;
    totalUserCount: number;
    activeUserCount: number;
    recentReports: AdminContentReportResponse[];
    isReportsLoading: boolean;
    onTabChange: (tab: AdminTab) => void;
};

export default function AdminDashboard({
                                           pendingReportCount,
                                           totalUserCount,
                                           activeUserCount,
                                           recentReports,
                                           isReportsLoading,
                                           onTabChange,
                                       }: AdminDashboardProps) {
    return (
        <>
            {/* 요약 카드 */}
            <div className="mb-6 grid grid-cols-1 gap-4 md:grid-cols-3">
                <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                    <p className="text-sm text-muted-foreground">
                        처리 대기 신고
                    </p>

                    <p className="mt-2 text-3xl font-extrabold text-yellow-500">
                        {pendingReportCount}
                    </p>
                </Card>

                <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                    <p className="text-sm text-muted-foreground">
                        전체 회원
                    </p>

                    <p className="mt-2 text-3xl font-extrabold text-cyan-500">
                        {totalUserCount}
                    </p>
                </Card>

                <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                    <p className="text-sm text-muted-foreground">
                        활성 회원
                    </p>

                    <p className="mt-2 text-3xl font-extrabold text-emerald-500">
                        {activeUserCount}
                    </p>
                </Card>
            </div>

            {/* 최근 신고 */}
            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-4 flex items-center justify-between">
                    <div>
                        <h3 className="text-lg font-bold">
                            최근 신고
                        </h3>

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
                    <div className="overflow-hidden rounded-xl border border-border">
                        <table className="w-full text-sm">
                            <thead className="bg-[var(--surface-subtle)] text-left text-muted-foreground">
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
                                    className="border-t border-border"
                                >
                                    <td className="px-4 py-3">
                                            <span
                                                className={`rounded-full px-2 py-1 text-xs font-bold ${
                                                    REPORT_STATUS_STYLE[
                                                        report.status
                                                        ]
                                                }`}
                                            >
                                                {
                                                    REPORT_STATUS_LABEL[
                                                        report.status
                                                        ]
                                                }
                                            </span>
                                    </td>

                                    <td className="px-4 py-3">
                                        {
                                            TARGET_TYPE_LABEL[
                                                report.target_type
                                                ]
                                        }
                                    </td>

                                    <td className="px-4 py-3">
                                        {REASON_LABEL[report.reason]}
                                    </td>

                                    <td className="px-4 py-3 text-muted-foreground">
                                        {formatDateTime(
                                            getTextValue(report, [
                                                "created_at",
                                                "createdAt",
                                            ])
                                        )}
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>
        </>
    );
}