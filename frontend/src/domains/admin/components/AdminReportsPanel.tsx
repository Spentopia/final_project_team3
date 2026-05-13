// src/domains/admin/components/AdminReportsPanel.tsx
//
// 신고 관리 패널.
//
// 이번 수정:
// - 상태 필터 외에 대상 타입/사유 필터 추가
// - 신고자 닉네임/이메일 검색
// - 페이지네이션 적용 (공통 Pagination 컴포넌트)
// - 필터/검색은 상위 AdminPage에서 상태 관리, 이 컴포넌트는 props로 받음

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";
import Pagination from "@/components/page/Pagination.tsx";
import { Search } from "lucide-react";

import type { AdminContentReportResponse } from "@/domains/admin/api/adminApi";
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

// 신고 대상 타입/사유 필터 값.
// "all"은 프론트 전용. 백엔드로 보낼 때는 undefined로 변환한다.
export type ReportTargetTypeFilter =
    | "all"
    | "post"
    | "comment"
    | "user_nickname"
    | "user_profile";

export type ReportReasonFilter =
    | "all"
    | "abuse"
    | "inappropriate"
    | "spam"
    | "other";

type AdminReportsPanelProps = {
    reports: AdminContentReportResponse[];

    // 필터
    reportStatus: ReportStatusFilter;
    targetTypeFilter: ReportTargetTypeFilter;
    reasonFilter: ReportReasonFilter;
    keyword: string;

    // 페이지네이션
    page: number;
    totalCount: number;
    pageSize: number;

    isReportsLoading: boolean;
    processingId: string | null;

    // 핸들러
    onReportStatusChange: (status: ReportStatusFilter) => void;
    onTargetTypeChange: (type: ReportTargetTypeFilter) => void;
    onReasonChange: (reason: ReportReasonFilter) => void;
    onKeywordChange: (keyword: string) => void;
    onPageChange: (page: number) => void;

    onSelectReport: (report: AdminContentReportResponse) => void;
    onResolveReport: (reportId: string) => void;
    onRejectReport: (reportId: string) => void;
};

const REPORT_FILTERS: Array<{ value: ReportStatusFilter; label: string }> = [
    { value: "all", label: "전체" },
    { value: "pending", label: "대기중" },
    { value: "resolved", label: "처리완료" },
    { value: "rejected", label: "반려" },
];

const TARGET_TYPE_FILTERS: Array<{ value: ReportTargetTypeFilter; label: string }> = [
    { value: "all", label: "전체 대상" },
    { value: "post", label: "게시글" },
    { value: "comment", label: "댓글" },
    { value: "user_nickname", label: "닉네임" },
    { value: "user_profile", label: "프로필 사진" },
];

const REASON_FILTERS: Array<{ value: ReportReasonFilter; label: string }> = [
    { value: "all", label: "전체 사유" },
    { value: "abuse", label: "욕설/비방" },
    { value: "inappropriate", label: "부적합" },
    { value: "spam", label: "광고/도배" },
    { value: "other", label: "기타" },
];

export default function AdminReportsPanel({
                                              reports,
                                              reportStatus,
                                              targetTypeFilter,
                                              reasonFilter,
                                              keyword,
                                              page,
                                              totalCount,
                                              pageSize,
                                              isReportsLoading,
                                              processingId,
                                              onReportStatusChange,
                                              onTargetTypeChange,
                                              onReasonChange,
                                              onKeywordChange,
                                              onPageChange,
                                              onSelectReport,
                                              onResolveReport,
                                              onRejectReport,
                                          }: AdminReportsPanelProps) {
    // 전체 페이지 수.
    // pageSize=0 같은 잘못된 값이 들어와도 1 이상으로 강제.
    const totalPages = Math.max(1, Math.ceil(totalCount / Math.max(1, pageSize)));

    // "153건 중 21-40번째" 같은 표시 계산
    const rangeStart = totalCount === 0 ? 0 : (page - 1) * pageSize + 1;
    const rangeEnd = Math.min(page * pageSize, totalCount);

    return (
        <>
            {/* 상태 필터 (탭형) */}
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

            {/* 추가 필터 (셀렉트 + 검색) */}
            <div className="mb-4 flex flex-wrap items-center gap-3">
                {/* 대상 타입 셀렉트 */}
                <select
                    value={targetTypeFilter}
                    onChange={(e) => onTargetTypeChange(e.target.value as ReportTargetTypeFilter)}
                    className="h-10 rounded-xl border border-border bg-white px-3 text-sm outline-none transition focus:border-cyan-500 dark:bg-gray-800"
                >
                    {TARGET_TYPE_FILTERS.map((filter) => (
                        <option key={filter.value} value={filter.value}>
                            {filter.label}
                        </option>
                    ))}
                </select>

                {/* 사유 셀렉트 */}
                <select
                    value={reasonFilter}
                    onChange={(e) => onReasonChange(e.target.value as ReportReasonFilter)}
                    className="h-10 rounded-xl border border-border bg-white px-3 text-sm outline-none transition focus:border-cyan-500 dark:bg-gray-800"
                >
                    {REASON_FILTERS.map((filter) => (
                        <option key={filter.value} value={filter.value}>
                            {filter.label}
                        </option>
                    ))}
                </select>

                {/* 신고자 검색 */}
                <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        placeholder="닉네임 / 이메일 검색"
                        className="h-11 w-64 pl-10 text-base"
                        value={keyword}
                        onChange={(e) => onKeywordChange(e.target.value)}
                    />
                </div>

                {/* 결과 카운트 표시 */}
                <span className="ml-auto text-xs text-muted-foreground">
                      {totalCount > 0
                          ? `총 ${totalCount.toLocaleString()}건 · ${rangeStart.toLocaleString()}-${rangeEnd.toLocaleString()} 표시 중`
                          : "총 0건"}
                    </span>
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
                    <div className="overflow-hidden rounded-2xl border border-border bg-white/60 dark:bg-gray-900/20">
                        <table className="w-full table-fixed text-sm">
                            <colgroup>
                                <col className="w-[110px]" />
                                <col className="w-[120px]" />
                                <col className="w-[130px]" />
                                <col className="w-[220px]" />
                                <col className="w-[150px]" />
                                <col className="w-[230px]" />
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3">상태</th>
                                <th className="px-4 py-3">신고대상</th>
                                <th className="px-4 py-3">신고사유</th>
                                <th className="px-4 py-3">신고자</th>
                                <th className="px-4 py-3">신고일</th>
                                <th className="px-4 py-3 text-right">처리</th>
                            </tr>
                            </thead>

                            <tbody>
                            {reports.map((report) => (
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

                                    <td className="px-4 py-3 align-middle">
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

                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                      <span className="block truncate">
                        {formatDateTime(report.created_at)}
                      </span>
                                    </td>

                                    <td className="px-4 py-3 align-middle">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                type="button"
                                                onClick={() => onSelectReport(report)}
                                                className="rounded-lg border border-border bg-white/60 px-3 py-1.5 text-xs font-semibold transition hover:bg-[var(--surface-subtle)] dark:bg-gray-800/60"
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

                {/* 페이지네이션 */}
                <Pagination
                    currentPage={page}
                    totalPages={totalPages}
                    onPageChange={onPageChange}
                />
            </Card>
        </>
    );
}