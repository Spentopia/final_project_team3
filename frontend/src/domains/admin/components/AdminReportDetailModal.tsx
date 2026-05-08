// src/domains/admin/components/AdminReportDetailModal.tsx
//
// 신고 상세 모달.
//
// 역할:
// - 신고 상세 정보 표시
// - 신고 상태 표시
// - 신고자/대상 ID/신고일/상세 내용 표시
// - 반려 / 처리 완료 버튼 제공
//
// 이 컴포넌트는 selectedReport가 있을 때만 AdminPage에서 렌더링한다.
// 닫기/처리/반려 동작은 상위 AdminPage의 handler를 props로 받는다.

import { CheckCircle2, XCircle } from "lucide-react";

import type { AdminContentReportResponse } from "@/domains/admin/api/adminApi";

import {
    formatDateTime,
    getTextValue,
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

export default function AdminReportDetailModal({
                                                   report,
                                                   processingId,
                                                   onClose,
                                                   onResolve,
                                                   onReject,
                                               }: AdminReportDetailModalProps) {
    const isProcessing = processingId === report.id;
    const isPending = report.status === "pending";

    const targetId = getTextValue(report, ["target_id"]);
    const reporterText = getTextValue(report, [
        "reporter_nickname",
        "reporter_email",
        "reporter_id",
        "user_id",
    ]);

    const detailText = getTextValue(
        report,
        ["description", "content", "detail", "reason_detail"],
        "상세 내용이 없습니다."
    );

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
            <div className="w-full max-w-xl rounded-2xl border border-border bg-[var(--surface-elevated)] p-6 shadow-2xl">
                <div className="mb-5 flex items-start justify-between gap-4">
                    <div>
                        <p className="text-xs font-bold uppercase tracking-[0.18em] text-luxury-gold">
                            Report Detail
                        </p>

                        <h3 className="mt-1 text-xl font-extrabold">
                            신고 상세
                        </h3>
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
                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            상태
                        </span>

                        <span
                            className={`rounded-full px-2 py-1 text-xs font-bold ${
                                REPORT_STATUS_STYLE[report.status]
                            }`}
                        >
                            {REPORT_STATUS_LABEL[report.status]}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            신고 대상
                        </span>

                        <span>
                            {TARGET_TYPE_LABEL[report.target_type]}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            신고 사유
                        </span>

                        <span>
                            {REASON_LABEL[report.reason]}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            신고 ID
                        </span>

                        <span title={report.id}>
                            {shortId(report.id)}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            신고자
                        </span>

                        <span>
                            {reporterText}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            대상 ID
                        </span>

                        <span title={targetId}>
                            {shortId(targetId)}
                        </span>
                    </div>

                    <div className="flex justify-between gap-4 border-b border-border pb-2">
                        <span className="text-muted-foreground">
                            신고일
                        </span>

                        <span>
                            {formatDateTime(
                                getTextValue(report, [
                                    "created_at",
                                    "createdAt",
                                ])
                            )}
                        </span>
                    </div>

                    <div className="rounded-xl bg-[var(--surface-subtle)] p-4">
                        <p className="mb-2 font-semibold">
                            신고 상세 내용
                        </p>

                        <p className="whitespace-pre-wrap text-muted-foreground">
                            {detailText}
                        </p>
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