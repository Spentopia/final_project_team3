// src/domains/admin/components/AdminReportsPanel.tsx
//
// 관리자 신고 관리 패널.
//
// 포함 기능:
// 1. 신고 상태 필터
// 2. 신고 대상 타입 필터
// 3. 신고 사유 필터
// 4. 신고자 닉네임/이메일 검색
// 5. 신고일 날짜 범위 필터
// 6. 신고일/처리일 정렬 토글
// 7. 페이지네이션
// 8. 같은 대상 누적 신고 횟수 뱃지
//
// 주의:
// - 실제 데이터 조회 상태는 상위 AdminPage에서 관리한다.
// - 이 컴포넌트는 props를 받아 화면만 렌더링하고,
//   필터 변경 이벤트를 상위로 올려보낸다.

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

// 신고 대상 타입 필터.
//
// "all"은 프론트 전용 값.
// 백엔드로 보낼 때는 undefined로 변환한다.
export type ReportTargetTypeFilter =
    | "all"
    | "post"
    | "comment"
    | "user_nickname"
    | "user_profile";

// 신고 사유 필터.
//
// "all"은 프론트 전용 값.
// 백엔드로 보낼 때는 undefined로 변환한다.
export type ReportReasonFilter =
    | "all"
    | "abuse"
    | "inappropriate"
    | "spam"
    | "other";

// 신고 목록 정렬 기준.
//
// created_at  : 신고 접수일
// reviewed_at : 관리자 처리일
export type ReportSortBy = "created_at" | "reviewed_at";

// 신고 목록 정렬 방향.
//
// desc : 최신순
// asc  : 오래된순
export type ReportSortOrder = "desc" | "asc";

type AdminReportsPanelProps = {
    reports: AdminContentReportResponse[];

    // 필터 상태
    reportStatus: ReportStatusFilter;
    targetTypeFilter: ReportTargetTypeFilter;
    reasonFilter: ReportReasonFilter;
    keyword: string;

    // 날짜 범위 필터
    startDate: string;
    endDate: string;

    // 정렬 상태
    sortBy: ReportSortBy;
    sortOrder: ReportSortOrder;

    // 페이지네이션
    page: number;
    totalCount: number;
    pageSize: number;

    isReportsLoading: boolean;
    processingId: string | null;

    // 필터 핸들러
    onReportStatusChange: (status: ReportStatusFilter) => void;
    onTargetTypeChange: (type: ReportTargetTypeFilter) => void;
    onReasonChange: (reason: ReportReasonFilter) => void;
    onKeywordChange: (keyword: string) => void;
    onStartDateChange: (value: string) => void;
    onEndDateChange: (value: string) => void;
    onSortChange: (sortBy: ReportSortBy) => void;

    // 페이지 변경
    onPageChange: (page: number) => void;

    // 신고 처리
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
    { value: "inappropriate", label: "부적절" },
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
                                              processingId,
                                              onReportStatusChange,
                                              onTargetTypeChange,
                                              onReasonChange,
                                              onKeywordChange,
                                              onStartDateChange,
                                              onEndDateChange,
                                              onSortChange,
                                              onPageChange,
                                              onSelectReport,
                                              onResolveReport,
                                              onRejectReport,
                                          }: AdminReportsPanelProps) {
    // 전체 페이지 수.
    //
    // pageSize가 0이 되는 실수를 막기 위해 Math.max(1, pageSize)를 사용한다.
    const totalPages = Math.max(1, Math.ceil(totalCount / Math.max(1, pageSize)));

    // 현재 페이지에서 몇 번째 데이터 범위를 보고 있는지 표시하기 위한 값.
    //
    // 예:
    // totalCount=15, page=1, pageSize=10 → 1-10
    // totalCount=15, page=2, pageSize=10 → 11-15
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
                {/* 신고 대상 타입 */}
                <select
                    value={targetTypeFilter}
                    onChange={(event) =>
                        onTargetTypeChange(event.target.value as ReportTargetTypeFilter)
                    }
                    className="h-11 rounded-xl border border-border bg-white px-3 text-base outline-none transition focus:border-blue-600 dark:bg-gray-800"
                >
                    {TARGET_TYPE_FILTERS.map((filter) => (
                        <option key={filter.value} value={filter.value}>
                            {filter.label}
                        </option>
                    ))}
                </select>

                {/* 신고 사유 */}
                <select
                    value={reasonFilter}
                    onChange={(event) =>
                        onReasonChange(event.target.value as ReportReasonFilter)
                    }
                    className="h-11 rounded-xl border border-border bg-white px-3 text-base outline-none transition focus:border-blue-600 dark:bg-gray-800"
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
                        placeholder="닉네임/이메일 검색"
                        className="h-11 w-64 pl-10 text-base"
                        value={keyword}
                        onChange={(event) => onKeywordChange(event.target.value)}
                    />
                </div>

                {/* 날짜 범위 필터 */}
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

                {/* 정렬 버튼 */}
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

                {/* 결과 개수 */}
                <span className="ml-auto text-xs text-muted-foreground">
                    {totalCount > 0
                        ? `총 ${totalCount.toLocaleString()}건 · ${rangeStart.toLocaleString()}-${rangeEnd.toLocaleString()} 표시 중`
                        : "총 0건"}
                </span>
            </div>

            <Card className="overflow-hidden border-none spentopia-surface-card backdrop-blur-xl">
                {isReportsLoading ? (
                    <div className="p-8 text-center text-sm text-muted-foreground">
                        신고 목록을 불러오는 중입니다.
                    </div>
                ) : reports.length === 0 ? (
                    <div className="p-8 text-center text-sm text-muted-foreground">
                        조건에 맞는 신고가 없습니다.
                    </div>
                ) : (
                    <div className="divide-y divide-border">
                        {reports.map((report) => {
                            const isProcessing = processingId === report.id;
                            const accumulatedCount = report.target_report_count ?? 1;

                            return (
                                <div
                                    key={report.id}
                                    className="flex flex-col gap-3 p-5 transition hover:bg-[var(--surface-subtle)] lg:flex-row lg:items-center lg:justify-between"
                                >
                                    <button
                                        type="button"
                                        onClick={() => onSelectReport(report)}
                                        className="min-w-0 flex-1 text-left"
                                    >
                                        <div className="mb-2 flex flex-wrap items-center gap-2">
                                            {/* 신고 상태 */}
                                            <span
                                                className={`rounded-full px-2.5 py-1 text-xs font-semibold ${REPORT_STATUS_STYLE[report.status]}`}
                                            >
                                                {REPORT_STATUS_LABEL[report.status]}
                                            </span>

                                            {/* 신고 대상 타입 */}
                                            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold text-slate-600 dark:bg-slate-800 dark:text-slate-300">
                                                {TARGET_TYPE_LABEL[report.target_type]}
                                            </span>

                                            {/* 신고 사유 */}
                                            <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700 dark:bg-blue-900/30 dark:text-blue-300">
                                                {REASON_LABEL[report.reason]}
                                            </span>

                                            {/* 누적 신고 횟수 뱃지 */}
                                            {accumulatedCount > 1 && (
                                                <span className="rounded-full bg-rose-50 px-2.5 py-1 text-xs font-semibold text-rose-600 dark:bg-rose-900/30 dark:text-rose-300">
                                                    누적 {accumulatedCount.toLocaleString()}회
                                                </span>
                                            )}
                                        </div>

                                        <div className="flex min-w-0 flex-col gap-1">
                                            <p className="truncate text-sm font-semibold text-foreground">
                                                신고자: {getReporterPrimaryText(report)}
                                            </p>
                                            <p className="truncate text-xs text-muted-foreground">
                                                {getReporterSecondaryText(report)}
                                            </p>
                                            <p className="text-xs text-muted-foreground">
                                                신고일: {formatDateTime(report.created_at)}
                                                {report.reviewed_at
                                                    ? ` · 처리일: ${formatDateTime(report.reviewed_at)}`
                                                    : ""}
                                            </p>
                                        </div>
                                    </button>

                                    <div className="flex shrink-0 items-center gap-2">
                                        <button
                                            type="button"
                                            onClick={() => onSelectReport(report)}
                                            className="rounded-lg border border-border px-3 py-2 text-sm font-medium text-muted-foreground transition hover:bg-[var(--surface-subtle)] hover:text-foreground"
                                        >
                                            상세
                                        </button>

                                        {report.status === "pending" && (
                                            <>
                                                <button
                                                    type="button"
                                                    disabled={isProcessing}
                                                    onClick={() => onResolveReport(report.id)}
                                                    className="rounded-lg bg-emerald-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-60"
                                                >
                                                    처리완료
                                                </button>

                                                <button
                                                    type="button"
                                                    disabled={isProcessing}
                                                    onClick={() => onRejectReport(report.id)}
                                                    className="rounded-lg bg-rose-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-60"
                                                >
                                                    반려
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
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