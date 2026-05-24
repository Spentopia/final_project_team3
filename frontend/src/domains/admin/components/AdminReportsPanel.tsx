// src/domains/admin/components/AdminReportsPanel.tsx
//
// 관리자 신고 관리 패널.
//
// 이번 수정:
// - 카드형 목록을 테이블형으로 전환 (회원 관리 표와 동일 구조/밀도).
// - 컬럼: 상태 / 대상·사유 / 신고자 / 신고일 / 처리일 / 관리
// - 상태만 색 배지, 대상·사유는 무채색 텍스트, 누적은 경고 텍스트.
// - 헤더-셀 정렬 전부 왼쪽 통일. 세로선 없음(가로선만).

import { ArrowDown, ArrowUp, CalendarDays, Search } from "lucide-react";

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";
import Pagination from "@/components/page/Pagination.tsx";

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

export type ReportSortBy = "created_at" | "reviewed_at";

export type ReportSortOrder = "desc" | "asc";

type AdminReportsPanelProps = {
    reports: AdminContentReportResponse[];

    reportStatus: ReportStatusFilter;
    targetTypeFilter: ReportTargetTypeFilter;
    reasonFilter: ReportReasonFilter;
    keyword: string;

    startDate: string;
    endDate: string;

    sortBy: ReportSortBy;
    sortOrder: ReportSortOrder;

    page: number;
    totalCount: number;
    pageSize: number;

    isReportsLoading: boolean;

    onReportStatusChange: (status: ReportStatusFilter) => void;
    onTargetTypeChange: (type: ReportTargetTypeFilter) => void;
    onReasonChange: (reason: ReportReasonFilter) => void;
    onKeywordChange: (keyword: string) => void;
    onStartDateChange: (value: string) => void;
    onEndDateChange: (value: string) => void;
    onSortChange: (sortBy: ReportSortBy) => void;

    onPageChange: (page: number) => void;

    onSelectReport: (report: AdminContentReportResponse) => void;
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
    { value: "inappropriate", label: "부적절(음란/폭력/혐오)" },
    { value: "spam", label: "광고/도배" },
    { value: "other", label: "기타" },
];

const SORT_LABEL: Record<ReportSortBy, string> = {
    created_at: "신고일",
    reviewed_at: "처리일",
};

export default function AdminReportsPanel({
                                              reports,
                                              reportStatus,
                                              targetTypeFilter,
                                              reasonFilter,
                                              keyword,
                                              startDate,
                                              endDate,
                                              sortBy,
                                              sortOrder,
                                              page,
                                              totalCount,
                                              pageSize,
                                              isReportsLoading,
                                              onReportStatusChange,
                                              onTargetTypeChange,
                                              onReasonChange,
                                              onKeywordChange,
                                              onStartDateChange,
                                              onEndDateChange,
                                              onSortChange,
                                              onPageChange,
                                              onSelectReport,
                                          }: AdminReportsPanelProps) {
    const totalPages = Math.max(1, Math.ceil(totalCount / Math.max(1, pageSize)));

    const rangeStart = totalCount === 0 ? 0 : (page - 1) * pageSize + 1;
    const rangeEnd = Math.min(page * pageSize, totalCount);

    const renderSortIcon = (target: ReportSortBy) => {
        if (sortBy !== target) {
            return null;
        }

        return sortOrder === "desc" ? (
            <ArrowDown className="h-3.5 w-3.5" />
        ) : (
            <ArrowUp className="h-3.5 w-3.5" />
        );
    };

    return (
        <>
            {/* 상태 필터 */}
            <div className="mb-4 flex flex-wrap gap-2">
                {REPORT_FILTERS.map((filter) => (
                    <button
                        key={filter.value}
                        type="button"
                        onClick={() => onReportStatusChange(filter.value)}
                        className={`rounded-xl px-4 py-2 text-sm font-semibold transition ${
                            reportStatus === filter.value
                                ? "bg-blue-600 text-white shadow-lg shadow-blue-500/20"
                                : "bg-white/70 text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground dark:bg-gray-800/70"
                        }`}
                    >
                        {filter.label}
                    </button>
                ))}
            </div>

            {/* 검색/필터/날짜/정렬 영역 */}
            <div className="mb-4 flex flex-wrap items-center gap-3">
                <select
                    value={targetTypeFilter}
                    onChange={(event) =>
                        onTargetTypeChange(event.target.value as ReportTargetTypeFilter)
                    }
                    aria-label="신고 대상 타입 필터"
                    className="h-11 rounded-xl border border-border bg-white px-3 text-base outline-none transition focus:border-blue-600 dark:bg-gray-800"
                >
                    {TARGET_TYPE_FILTERS.map((filter) => (
                        <option key={filter.value} value={filter.value}>
                            {filter.label}
                        </option>
                    ))}
                </select>

                <select
                    value={reasonFilter}
                    onChange={(event) =>
                        onReasonChange(event.target.value as ReportReasonFilter)
                    }
                    aria-label="신고 사유 필터"
                    className="h-11 rounded-xl border border-border bg-white px-3 text-base outline-none transition focus:border-blue-600 dark:bg-gray-800"
                >
                    {REASON_FILTERS.map((filter) => (
                        <option key={filter.value} value={filter.value}>
                            {filter.label}
                        </option>
                    ))}
                </select>

                <div className="relative">
                    <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <label htmlFor="report-search" className="sr-only">
                        신고자 닉네임 또는 이메일 검색
                    </label>
                    <Input
                        id="report-search"
                        name="reportSearch"
                        type="search"
                        placeholder="닉네임/이메일 검색"
                        className="h-11 w-64 pl-10 text-base"
                        value={keyword}
                        onChange={(event) => onKeywordChange(event.target.value)}
                    />
                </div>

                <div className="flex items-center gap-2">
                    <div className="relative">
                        <CalendarDays className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <Input
                            type="date"
                            className="h-11 w-40 pl-10 text-base"
                            value={startDate}
                            onChange={(event) => onStartDateChange(event.target.value)}
                            aria-label="신고일 시작일"
                        />
                    </div>

                    <span className="text-sm text-muted-foreground">~</span>

                    <div className="relative">
                        <CalendarDays className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <Input
                            type="date"
                            className="h-11 w-40 pl-10 text-base"
                            value={endDate}
                            onChange={(event) => onEndDateChange(event.target.value)}
                            aria-label="신고일 종료일"
                        />
                    </div>
                </div>

                <div className="flex items-center gap-1 rounded-xl border border-border bg-white p-1 dark:bg-gray-800">
                    {(["created_at", "reviewed_at"] as ReportSortBy[]).map((target) => {
                        const active = sortBy === target;

                        return (
                            <button
                                key={target}
                                type="button"
                                onClick={() => onSortChange(target)}
                                className={`flex h-9 items-center gap-1 rounded-lg px-3 text-sm font-medium transition ${
                                    active
                                        ? "bg-blue-600 text-white"
                                        : "text-muted-foreground hover:bg-[var(--surface-subtle)] hover:text-foreground"
                                }`}
                            >
                                {SORT_LABEL[target]}
                                {renderSortIcon(target)}
                            </button>
                        );
                    })}
                </div>

                <span className="ml-auto text-sm text-muted-foreground">
                    {totalCount > 0
                        ? `총 ${totalCount.toLocaleString()}건 · ${rangeStart.toLocaleString()}-${rangeEnd.toLocaleString()} 표시 중`
                        : "총 0건"}
                </span>
            </div>

            <Card className="overflow-hidden border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                {isReportsLoading ? (
                    <div className="p-8 text-center text-sm text-muted-foreground">
                        신고 목록을 불러오는 중입니다.
                    </div>
                ) : reports.length === 0 ? (
                    <div className="p-8 text-center text-sm text-muted-foreground">
                        조건에 맞는 신고가 없습니다.
                    </div>
                ) : (
                    <div className="overflow-x-auto rounded-2xl border border-border bg-white/60 dark:bg-gray-900/20">
                        <table className="min-w-[1000px] w-full table-fixed text-sm">
                            <colgroup>
                                <col className="w-[110px]" /> {/* 상태 */}
                                <col className="w-[240px]" /> {/* 대상·사유 */}
                                <col className="w-[260px]" /> {/* 신고자 */}
                                <col className="w-[160px]" /> {/* 신고일 */}
                                <col className="w-[160px]" /> {/* 처리일 */}
                                <col className="w-[100px]" /> {/* 관리 */}
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3">상태</th>
                                <th className="px-4 py-3">대상 · 사유</th>
                                <th className="px-4 py-3">신고자</th>
                                <th className="px-4 py-3">신고일</th>
                                <th className="px-4 py-3">처리일</th>
                                <th className="px-4 py-3">관리</th>
                            </tr>
                            </thead>

                            <tbody>
                            {reports.map((report) => {
                                const accumulatedCount = report.target_report_count ?? 1;

                                return (
                                    <tr
                                        key={report.id}
                                        className="border-t border-border transition-colors hover:bg-[var(--surface-subtle)]/70"
                                    >
                                        {/* 상태 */}
                                        <td className="px-4 py-3 align-middle">
                                            <span
                                                className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${REPORT_STATUS_STYLE[report.status]}`}
                                            >
                                                {REPORT_STATUS_LABEL[report.status]}
                                            </span>
                                        </td>

                                        {/* 대상 · 사유 + 누적 */}
                                        <td className="px-4 py-3 align-middle">
                                            <span className="text-foreground">
                                                {TARGET_TYPE_LABEL[report.target_type]}
                                                {" · "}
                                                {REASON_LABEL[report.reason]}
                                            </span>

                                            {accumulatedCount > 1 && (
                                                <span className="ml-2 text-xs font-semibold text-rose-600 dark:text-rose-300">
                                                    누적 {accumulatedCount.toLocaleString()}회
                                                </span>
                                            )}
                                        </td>

                                        {/* 신고자 */}
                                        <td className="px-4 py-3 align-middle">
                                            <p className="truncate font-medium text-foreground">
                                                {getReporterPrimaryText(report)}
                                            </p>
                                            <p className="truncate text-xs text-muted-foreground">
                                                {getReporterSecondaryText(report)}
                                            </p>
                                        </td>

                                        {/* 신고일 */}
                                        <td className="px-4 py-3 align-middle text-muted-foreground">
                                            {formatDateTime(report.created_at)}
                                        </td>

                                        {/* 처리일 */}
                                        <td className="px-4 py-3 align-middle text-muted-foreground">
                                            {report.reviewed_at
                                                ? formatDateTime(report.reviewed_at)
                                                : "-"}
                                        </td>

                                        {/* 관리 */}
                                        <td className="px-4 py-3 align-middle">
                                            <div className="flex justify-start">
                                                <button
                                                    type="button"
                                                    onClick={() => onSelectReport(report)}
                                                    className="rounded-lg border border-border px-3 py-1.5 text-xs font-semibold text-muted-foreground transition hover:bg-[var(--surface-subtle)] hover:text-foreground"
                                                >
                                                    상세
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                );
                            })}
                            </tbody>
                        </table>
                    </div>
                )}
            </Card>

            <Pagination
                currentPage={page}
                totalPages={totalPages}
                onPageChange={onPageChange}
            />
        </>
    );
}