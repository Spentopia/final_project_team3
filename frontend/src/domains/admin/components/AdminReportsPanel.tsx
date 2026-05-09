// src/domains/admin/components/AdminReportsPanel.tsx
//
// 신고 관리 패널.
//
// 역할:
// - 신고 상태 필터 UI
// - 신고 목록 테이블
// - 상세 보기 버튼
// - 처리 완료 버튼
// - 반려 버튼
//
// 데이터 조회/API 호출은 여기서 하지 않는다.
// 상위 AdminPage가 상태와 handler를 props로 내려준다.
// 이 구조가 실무에서 흔한 "Container + Presentational Component" 패턴이다.

import { Card } from "@/shared/ui/card";

import type {
    AdminContentReportResponse} from "@/domains/admin/api/adminApi";

import type { ReportStatusFilter } from "@/domains/admin/types/adminViewTypes";

import {
    formatDateTime,
    getReporterPrimaryText,
    getReporterSecondaryText,
    REASON_LABEL,
    REPORT_STATUS_LABEL,
    REPORT_STATUS_STYLE,
    TARGET_TYPE_LABEL,
} from "@/domains/admin/utils/adminViewUtils";

type AdminReportsPanelProps = {
    reports: AdminContentReportResponse[];
    reportStatus: ReportStatusFilter;
    isReportsLoading: boolean;
    processingId: string | null;
    onReportStatusChange: (status: ReportStatusFilter) => void;
    onSelectReport: (report: AdminContentReportResponse) => void;
    onResolveReport: (reportId: string) => void;
    onRejectReport: (reportId: string) => void;
};

const REPORT_FILTERS: Array<{
    value: ReportStatusFilter;
    label: string;
}> = [
    { value: "all", label: "전체" },
    { value: "pending", label: "대기중" },
    { value: "resolved", label: "처리완료" },
    { value: "rejected", label: "반려" },
];

export default function AdminReportsPanel({
                                              reports,
                                              reportStatus,
                                              isReportsLoading,
                                              processingId,
                                              onReportStatusChange,
                                              onSelectReport,
                                              onResolveReport,
                                              onRejectReport,
                                          }: AdminReportsPanelProps) {
    return (
        <>
            {/* 신고 상태 필터 */}
            <div className="mb-4 flex flex-wrap gap-2">
                {REPORT_FILTERS.map((filter) => (
                    <button
                        key={filter.value}
                        type="button"
                        onClick={() => onReportStatusChange(filter.value)}
                        className={`rounded-xl px-4 py-2 text-sm font-semibold transition ${
                            reportStatus === filter.value
                                ? "bg-cyan-500 text-white shadow-lg shadow-cyan-500/20"
                                : "bg-white/70 text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground dark:bg-gray-800/70"
                        }`}
                    >
                        {filter.label}
                    </button>
                ))}
            </div>

            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-4 flex items-center justify-between">
                    <div>
                        <h3 className="text-lg font-bold">신고 목록</h3>

                        <p className="mt-1 text-sm text-muted-foreground">
                            신고를 확인하고 처리 상태를 변경합니다.
                        </p>
                    </div>
                </div>

                {isReportsLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        신고 목록을 불러오는 중입니다.
                    </div>
                )}

                {!isReportsLoading && reports.length === 0 && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        조건에 맞는 신고가 없습니다.
                    </div>
                )}

                {!isReportsLoading && reports.length > 0 && (
                    <div className="overflow-hidden rounded-xl border border-border">
                        <table className="w-full table-fixed text-sm">
                            <colgroup>
                                {/* 상태 */}
                                <col className="w-[110px]" />

                                {/* 신고대상 */}
                                <col className="w-[120px]" />

                                {/* 신고사유 */}
                                <col className="w-[130px]" />

                                {/* 신고자 */}
                                <col className="w-[220px]" />

                                {/* 신고일 */}
                                <col className="w-[150px]" />

                                {/* 처리 버튼 */}
                                <col className="w-[230px]" />
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3 pl-7">상태</th>
                                <th className="px-4 py-3 pl-2.5">신고대상</th>
                                <th className="px-4 py-3 pl-2.5">신고사유</th>
                                <th className="px-4 py-3 pl-7">신고자</th>
                                <th className="px-4 py-3 pl-11">신고일</th>
                                <th className="px-4 py-3 pr-22 text-right">처리</th>
                            </tr>
                            </thead>

                            <tbody>
                            {reports.map((report) => (
                                <tr key={report.id} className="border-t border-border">
                                    <td className="px-4 py-3">
                      <span
                          className={`rounded-full px-2 py-1 text-xs font-bold ${
                              REPORT_STATUS_STYLE[report.status]
                          }`}
                      >
                        {REPORT_STATUS_LABEL[report.status]}
                      </span>
                                    </td>

                                    <td className="px-4 py-3">
                                        {TARGET_TYPE_LABEL[report.target_type]}
                                    </td>

                                    <td className="px-4 py-3">
                                        {REASON_LABEL[report.reason]}
                                    </td>

                                    {/* 신고자 표시 */}
                                    <td className="px-4 py-3">
                                        <div className="flex min-w-0 flex-col">
                        <span className="truncate font-medium text-foreground">
                          {getReporterPrimaryText(report)}
                        </span>

                                            <span
                                                className="truncate text-xs text-muted-foreground"
                                                title={report.reporter_id}
                                            >
                          {getReporterSecondaryText(report)}
                        </span>
                                        </div>
                                    </td>

                                    <td className="px-4 py-3 text-muted-foreground">
                                        {formatDateTime(report.created_at)}
                                    </td>

                                    <td className="px-4 py-3">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                type="button"
                                                onClick={() => onSelectReport(report)}
                                                className="rounded-lg border border-border px-3 py-1.5 text-xs font-semibold transition hover:bg-[var(--surface-subtle)]"
                                            >
                                                상세
                                            </button>

                                            <button
                                                type="button"
                                                disabled={
                                                    report.status !== "pending" ||
                                                    processingId === report.id
                                                }
                                                onClick={() => onResolveReport(report.id)}
                                                className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-50"
                                            >
                                                처리 완료
                                            </button>

                                            <button
                                                type="button"
                                                disabled={
                                                    report.status !== "pending" ||
                                                    processingId === report.id
                                                }
                                                onClick={() => onRejectReport(report.id)}
                                                className="rounded-lg bg-gray-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                                            >
                                                반려
                                            </button>
                                        </div>
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