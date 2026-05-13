// src/domains/admin/components/AdminReportDetailModal.tsx
//
// 신고 상세 모달.
//
// 역할:
// - 신고 상세 정보 표시
// - 신고 상태 표시
// - 신고자/대상 ID/신고일/상세 내용 표시
// - 누적 신고 횟수 표시
// - 처리자/처리일 표시
// - 감사 로그 접기/펼치기 표시
// - 반려 / 처리 완료 버튼 제공
//
// 감사 로그 설계:
// - 모달이 열릴 때 바로 조회하지 않는다.
// - 운영자가 "감사 로그 보기"를 눌렀을 때만 조회한다.
// - 한 번 조회한 뒤에는 접었다 펼쳐도 재조회하지 않는다.
// - "새로고침" 버튼을 누르면 강제로 다시 조회한다.
//
// 이유:
// - 감사 로그는 핵심 정보가 아니라 보조 정보다.
// - 모든 신고 상세를 열 때마다 audit log API를 호출하면 불필요한 네트워크 요청이 생긴다.
// - 필요할 때만 조회하는 방식이 실무적으로 더 자연스럽다.

import { useState } from "react";
import {
    CheckCircle2,
    ChevronDown,
    ChevronUp,
    History,
    RefreshCcw,
    XCircle,
} from "lucide-react";

import {
    listAdminContentReportAuditLogs,
    type AdminAuditLogResponse,
    type AdminContentReportResponse,
} from "@/domains/admin/api/adminApi";

import {
    formatDateTime,
    REASON_LABEL,
    REPORT_STATUS_LABEL,
    REPORT_STATUS_STYLE,
    shortId,
    TARGET_TYPE_LABEL,
} from "@/domains/admin/utils/adminViewUtils";

type AdminReportDetailModalProps = {
    report: AdminContentReportResponse;
    processingId: string | null;
    onClose: () => void;
    onResolve: (reportId: string) => void;
    onReject: (reportId: string) => void;
};

/**
 * 감사 로그 action 값을 화면 표시용 한글 라벨로 변환한다.
 *
 * 백엔드 action 예:
 * - content_report_resolved
 * - content_report_rejected
 * - content_report_status_changed
 */
function getAuditActionLabel(action: string): string {
    switch (action) {
        case "content_report_resolved":
            return "신고 처리 완료";
        case "content_report_rejected":
            return "신고 반려";
        case "content_report_status_changed":
            return "신고 상태 변경";
        default:
            return action;
    }
}

/**
 * 상태값을 화면 표시용 한글 라벨로 변환한다.
 *
 * REPORT_STATUS_LABEL은 ContentReportStatus 기준 타입일 가능성이 있으므로
 * 감사 로그처럼 string으로 들어오는 값은 안전하게 switch로 처리한다.
 */
function getStatusLabel(status: string | null | undefined): string {
    switch (status) {
        case "pending":
            return "대기중";
        case "resolved":
            return "처리완료";
        case "rejected":
            return "반려";
        default:
            return status || "-";
    }
}

export default function AdminReportDetailModal({
                                                   report,
                                                   processingId,
                                                   onClose,
                                                   onResolve,
                                                   onReject,
                                               }: AdminReportDetailModalProps) {
    const isProcessing = processingId === report.id;
    const isPending = report.status === "pending";

    const detailText = report.detail?.trim()
        ? report.detail
        : "상세 내용이 없습니다.";

    // ─────────────────────────────────────────────
    // 감사 로그 상태
    // ─────────────────────────────────────────────
    //
    // isAuditOpen:
    // - 감사 로그 영역이 펼쳐졌는지 여부.
    //
    // auditLogs:
    // - 백엔드에서 조회한 감사 로그 목록.
    //
    // hasLoadedAuditLogs:
    // - 한 번이라도 조회했는지 여부.
    // - true면 접었다가 다시 펼쳐도 재요청하지 않는다.
    //
    // isAuditLogsLoading:
    // - 감사 로그 조회 중 로딩 표시.
    //
    // auditLogsError:
    // - 조회 실패 시 사용자에게 보여줄 간단한 메시지.
    const [isAuditOpen, setIsAuditOpen] = useState(false);
    const [auditLogs, setAuditLogs] = useState<AdminAuditLogResponse[]>([]);
    const [hasLoadedAuditLogs, setHasLoadedAuditLogs] = useState(false);
    const [isAuditLogsLoading, setIsAuditLogsLoading] = useState(false);
    const [auditLogsError, setAuditLogsError] = useState<string | null>(null);

    /**
     * 감사 로그 조회.
     *
     * force=false:
     * - 이미 조회한 적이 있으면 다시 조회하지 않는다.
     *
     * force=true:
     * - "새로고침" 버튼에서 사용한다.
     * - 이미 조회했어도 다시 API를 호출한다.
     */
    const fetchAuditLogs = async (force = false) => {
        if (isAuditLogsLoading) {
            return;
        }

        if (hasLoadedAuditLogs && !force) {
            return;
        }

        setIsAuditLogsLoading(true);
        setAuditLogsError(null);

        try {
            const data = await listAdminContentReportAuditLogs(report.id);

            setAuditLogs(data);
            setHasLoadedAuditLogs(true);
        } catch (error) {
            console.error("신고 감사 로그 조회 실패:", error);

            setAuditLogs([]);
            setAuditLogsError("감사 로그를 불러오지 못했습니다.");
            setHasLoadedAuditLogs(true);
        } finally {
            setIsAuditLogsLoading(false);
        }
    };

    /**
     * 감사 로그 접기/펼치기.
     *
     * 펼칠 때만 감사 로그를 조회한다.
     * 이미 조회한 적이 있으면 fetchAuditLogs 내부에서 재요청하지 않는다.
     */
    const handleToggleAuditLogs = () => {
        const nextOpen = !isAuditOpen;

        setIsAuditOpen(nextOpen);

        if (nextOpen) {
            void fetchAuditLogs(false);
        }
    };

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-xl rounded-2xl border border-border bg-[var(--surface-elevated)] p-6 shadow-2xl">
                <div className="mb-5 flex items-start justify-between gap-4">
                    <div>
                        <p className="text-xs font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Report Detail
                        </p>

                        <h3 className="mt-1 text-xl font-extrabold">신고 상세</h3>
                    </div>

                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg px-3 py-2 text-sm text-muted-foreground transition hover:bg-[var(--surface-subtle)] hover:text-foreground"
                    >
                        닫기
                    </button>
                </div>

                <div className="space-y-3 text-sm">
                    {/* 신고 상태 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">상태</span>

                        <span
                            className={`rounded-full px-2 py-1 text-xs font-bold ${
                                REPORT_STATUS_STYLE[report.status]
                            }`}
                        >
                            {REPORT_STATUS_LABEL[report.status]}
                        </span>
                    </div>

                    {/* 신고 대상 타입 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고 대상</span>

                        <span>{TARGET_TYPE_LABEL[report.target_type]}</span>
                    </div>

                    {/* 같은 대상 누적 신고 횟수 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">누적 신고 횟수</span>

                        <span
                            className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                                report.target_report_count > 1
                                    ? "bg-rose-50 text-rose-600 dark:bg-rose-900/30 dark:text-rose-300"
                                    : "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                            }`}
                        >
                            {report.target_report_count.toLocaleString()}회
                        </span>
                    </div>

                    {/* 신고 사유 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고 사유</span>

                        <span>{REASON_LABEL[report.reason]}</span>
                    </div>

                    {/* 신고 ID */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고 ID</span>

                        <span title={report.id}>{shortId(report.id)}</span>
                    </div>

                    {/* 신고자 닉네임 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고자 닉네임</span>

                        <span>{report.reporter_nickname || "닉네임 없음"}</span>
                    </div>

                    {/* 신고자 이메일 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고자 이메일</span>

                        <span>{report.reporter_email || "이메일 없음"}</span>
                    </div>

                    {/* 신고자 ID 전체 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고자 ID</span>

                        <span className="max-w-[320px] truncate" title={report.reporter_id}>
                            {report.reporter_id}
                        </span>
                    </div>

                    {/* 신고 대상 ID 전체 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">대상 ID</span>

                        <span className="max-w-[320px] truncate" title={report.target_id}>
                            {report.target_id}
                        </span>
                    </div>

                    {/* 신고일 */}
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">신고일</span>

                        <span>{formatDateTime(report.created_at)}</span>
                    </div>

                    {/* 처리일 */}
                    {report.reviewed_at && (
                        <div className="flex justify-between gap-4 border-b border-border pb-2">
                            <span className="text-muted-foreground">처리일</span>

                            <span>{formatDateTime(report.reviewed_at)}</span>
                        </div>
                    )}

                    {/* 처리 관리자 ID */}
                    {report.reviewed_by && (
                        <div className="flex justify-between gap-4 border-b border-border pb-2">
                            <span className="text-muted-foreground">처리 관리자 ID</span>

                            <span
                                className="max-w-[320px] truncate"
                                title={report.reviewed_by}
                            >
                                {report.reviewed_by}
                            </span>
                        </div>
                    )}

                    {/* 신고 상세 내용 */}
                    <div className="rounded-xl bg-[var(--surface-subtle)] p-4">
                        <p className="mb-2 font-semibold">신고 상세 내용</p>

                        <p className="whitespace-pre-wrap text-muted-foreground">
                            {detailText}
                        </p>
                    </div>

                    {/* 감사 로그 접이식 영역 */}
                    <div className="rounded-xl border border-border bg-[var(--surface-subtle)] p-4">
                        <button
                            type="button"
                            onClick={handleToggleAuditLogs}
                            className="flex w-full items-center justify-between gap-3 text-left"
                        >
                            <span className="inline-flex items-center gap-2 font-semibold">
                                <History className="h-4 w-4 text-muted-foreground" />
                                감사 로그
                            </span>

                            <span className="inline-flex items-center gap-1 text-xs font-semibold text-muted-foreground">
                                {isAuditOpen ? "접기" : "보기"}
                                {isAuditOpen ? (
                                    <ChevronUp className="h-4 w-4" />
                                ) : (
                                    <ChevronDown className="h-4 w-4" />
                                )}
                            </span>
                        </button>

                        {isAuditOpen && (
                            <div className="mt-3 border-t border-border pt-3">
                                <div className="mb-3 flex items-center justify-between gap-3">
                                    <p className="text-xs text-muted-foreground">
                                        신고 처리 이력을 확인합니다.
                                    </p>

                                    <button
                                        type="button"
                                        onClick={() => void fetchAuditLogs(true)}
                                        disabled={isAuditLogsLoading}
                                        className="inline-flex items-center gap-1 rounded-lg border border-border px-2.5 py-1.5 text-xs font-semibold text-muted-foreground transition hover:bg-background hover:text-foreground disabled:cursor-not-allowed disabled:opacity-60"
                                    >
                                        <RefreshCcw
                                            className={`h-3.5 w-3.5 ${
                                                isAuditLogsLoading ? "animate-spin" : ""
                                            }`}
                                        />
                                        새로고침
                                    </button>
                                </div>

                                {isAuditLogsLoading ? (
                                    <p className="text-sm text-muted-foreground">
                                        감사 로그를 불러오는 중입니다.
                                    </p>
                                ) : auditLogsError ? (
                                    <p className="text-sm text-rose-500">
                                        {auditLogsError}
                                    </p>
                                ) : auditLogs.length === 0 ? (
                                    <p className="text-sm text-muted-foreground">
                                        아직 기록된 감사 로그가 없습니다.
                                    </p>
                                ) : (
                                    <div className="space-y-3">
                                        {auditLogs.map((log) => (
                                            <div
                                                key={log.id}
                                                className="rounded-lg border border-border bg-background/60 p-3"
                                            >
                                                <div className="mb-1 flex items-center justify-between gap-3">
                                                    <span className="text-sm font-semibold text-foreground">
                                                        {getAuditActionLabel(log.action)}
                                                    </span>

                                                    <span className="shrink-0 text-xs text-muted-foreground">
                                                        {formatDateTime(log.created_at)}
                                                    </span>
                                                </div>

                                                <p className="text-xs text-muted-foreground">
                                                    상태 변경:{" "}
                                                    <span className="font-semibold text-foreground">
                                                        {getStatusLabel(log.before_status)}
                                                    </span>{" "}
                                                    →{" "}
                                                    <span className="font-semibold text-foreground">
                                                        {getStatusLabel(log.after_status)}
                                                    </span>
                                                </p>

                                                <p className="mt-1 truncate text-xs text-muted-foreground">
                                                    처리 관리자:{" "}
                                                    <span title={log.admin_id}>
                                                        {log.admin_id}
                                                    </span>
                                                </p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>

                <div className="mt-6 flex justify-end gap-2">
                    <button
                        type="button"
                        onClick={onClose}
                        className="rounded-lg border border-border px-4 py-2 text-sm font-semibold transition hover:bg-[var(--surface-subtle)]"
                    >
                        닫기
                    </button>

                    <button
                        type="button"
                        disabled={!isPending || isProcessing}
                        onClick={() => onReject(report.id)}
                        className="inline-flex items-center gap-2 rounded-lg bg-gray-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        <XCircle className="h-4 w-4" />
                        반려
                    </button>

                    <button
                        type="button"
                        disabled={!isPending || isProcessing}
                        onClick={() => onResolve(report.id)}
                        className="inline-flex items-center gap-2 rounded-lg bg-emerald-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-50"
                    >
                        <CheckCircle2 className="h-4 w-4" />
                        처리 완료
                    </button>
                </div>
            </div>
        </div>
    );
}