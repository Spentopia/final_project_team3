// src/domains/admin/components/AdminReportDetailModal.tsx
//
// 신고 상세 모달.
//
// 역할:
// - 신고 대상의 실제 내용을 가장 먼저 보여준다 (운영자 판단의 핵심).
// - 신고자 정보를 간단히 보여준다.
// - 운영 조치 / 반려 / 처리완료 버튼을 제공한다.
// - 신고 ID·신고자 ID·대상 ID 같은 기술 정보는 접기로 숨긴다.
// - 감사 로그 접기/펼치기를 제공한다.

import { useState, useEffect } from "react";
import {
    CheckCircle2,
    ChevronDown,
    ChevronUp,
    History,
    RefreshCcw,
    XCircle,
    ShieldAlert,
    Trash2,
    UserRoundX,
} from "lucide-react";

import {
    getAdminContentReportTargetDetail,
    listAdminContentReportAuditLogs,
    type AdminAuditLogResponse,
    type AdminContentReportResponse,
    type AdminReportAction,
    type AdminReportTargetDetailResponse,
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
    onApplyAction: (reportId: string, action: AdminReportAction) => void;
};

function getAuditActionLabel(action: string): string {
    switch (action) {
        case "content_report_resolved":
            return "신고 처리 완료";
        case "content_report_rejected":
            return "신고 반려";
        case "content_report_status_changed":
            return "신고 상태 변경";
        case "content_report_action_applied":
            return "운영 조치 적용";
        default:
            return action;
    }
}

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

function getActionConfirmMessage(action: AdminReportAction): string {
    switch (action) {
        case "delete_post":
            return "게시글을 삭제하고 신고를 처리 완료하시겠습니까?";
        case "delete_comment":
            return "댓글을 삭제하고 신고를 처리 완료하시겠습니까?";
        case "clear_profile_image":
            return "사용자의 프로필 사진을 기본 이미지로 변경하고 신고를 처리 완료하시겠습니까?";
        case "request_profile_image_change":
            return "프로필 사진 변경 요청 조치를 처리 완료하시겠습니까?\n\n현재 알림 발송은 제외되어 있고, 감사 로그만 남습니다.";
        case "request_nickname_change":
            return "닉네임 변경 요청 조치를 처리 완료하시겠습니까?\n\n현재 알림 발송은 제외되어 있고, 감사 로그만 남습니다.";
        default:
            return "운영 조치를 적용하시겠습니까?";
    }
}

const SUPABASE_URL = import.meta.env.VITE_SUPABASE_URL as string;
const COMMUNITY_BUCKET = "posts";

function buildCommunityImageUrl(path: string | null | undefined): string | null {
    if (!path) return null;

    if (path.startsWith("http")) {
        return path;
    }

    return `${SUPABASE_URL}/storage/v1/object/public/${COMMUNITY_BUCKET}/${path}`;
}

export default function AdminReportDetailModal({
                                                   report,
                                                   processingId,
                                                   onClose,
                                                   onResolve,
                                                   onReject,
                                                   onApplyAction,
                                               }: AdminReportDetailModalProps) {
    const isProcessing = processingId === report.id;
    const isPending = report.status === "pending";

    const detailText = report.detail?.trim()
        ? report.detail
        : "상세 내용이 없습니다.";

    const accumulatedCount = report.target_report_count ?? 1;

    const [isAuditOpen, setIsAuditOpen] = useState(false);
    const [auditLogs, setAuditLogs] = useState<AdminAuditLogResponse[]>([]);
    const [hasLoadedAuditLogs, setHasLoadedAuditLogs] = useState(false);
    const [isAuditLogsLoading, setIsAuditLogsLoading] = useState(false);
    const [auditLogsError, setAuditLogsError] = useState<string | null>(null);

    const [targetDetail, setTargetDetail] =
        useState<AdminReportTargetDetailResponse | null>(null);
    const [isTargetContentExpanded, setIsTargetContentExpanded] = useState(false);
    const [isTargetLoading, setIsTargetLoading] = useState(false);
    const [targetError, setTargetError] = useState<string | null>(null);

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

    useEffect(() => {
        let ignore = false;

        setIsTargetContentExpanded(false);

        async function loadTargetDetail() {
            setIsTargetLoading(true);
            setTargetError(null);

            try {
                const data = await getAdminContentReportTargetDetail(report.id);

                if (!ignore) {
                    setTargetDetail(data);
                }
            } catch (error) {
                console.error("신고 대상 상세 조회 실패:", error);

                if (!ignore) {
                    setTargetDetail(null);
                    setTargetError("신고 대상 정보를 불러오지 못했습니다.");
                }
            } finally {
                if (!ignore) {
                    setIsTargetLoading(false);
                }
            }
        }

        void loadTargetDetail();

        return () => {
            ignore = true;
        };
    }, [report.id]);

    const handleToggleAuditLogs = () => {
        const nextOpen = !isAuditOpen;

        setIsAuditOpen(nextOpen);

        if (nextOpen) {
            void fetchAuditLogs(false);
        }
    };

    const handleApplyAction = (action: AdminReportAction) => {
        const confirmed = window.confirm(getActionConfirmMessage(action));

        if (!confirmed) {
            return;
        }

        onApplyAction(report.id, action);
    };

    const renderLongText = (text: string | null | undefined, emptyText: string) => {
        const value = text?.trim();

        if (!value) {
            return <p className="text-sm text-muted-foreground">{emptyText}</p>;
        }

        const isLong = value.length > 120 || value.split("\n").length > 4;

        return (
            <div>
                <p
                    className={`whitespace-pre-wrap break-words text-sm leading-6 text-foreground ${
                        isTargetContentExpanded ? "" : "line-clamp-4"
                    }`}
                >
                    {value}
                </p>

                {isLong && (
                    <div className="mt-3 flex justify-end">
                        <button
                            type="button"
                            onClick={() => setIsTargetContentExpanded((prev) => !prev)}
                            className="rounded-full border border-border bg-background px-3 py-1 text-xs font-semibold text-muted-foreground transition hover:bg-[var(--surface-subtle)] hover:text-foreground"
                        >
                            {isTargetContentExpanded ? "접기" : "전체 보기"}
                        </button>
                    </div>
                )}
            </div>
        );
    };

    const renderTargetMeta = () => {
        if (!targetDetail) {
            return null;
        }

        if (targetDetail.kind === "post" || targetDetail.kind === "comment") {
            return (
                <div className="flex flex-wrap items-center justify-end gap-2 text-xs text-muted-foreground">
                    <span>작성일 {formatDateTime(targetDetail.created_at)}</span>

                    {targetDetail.is_hidden && (
                        <span className="rounded-full bg-amber-50 px-2 py-0.5 font-semibold text-amber-600 dark:bg-amber-900/30 dark:text-amber-300">
                            임시 숨김
                        </span>
                    )}

                    <span
                        className={`rounded-full px-2 py-0.5 font-semibold ${
                            targetDetail.is_deleted
                                ? "bg-rose-50 text-rose-600 dark:bg-rose-900/30 dark:text-rose-300"
                                : "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                        }`}
                    >
                        {targetDetail.is_deleted ? "삭제됨" : "삭제되지 않음"}
                    </span>
                </div>
            );
        }

        if (targetDetail.kind === "user_profile" || targetDetail.kind === "user_nickname") {
            const isInactive = !targetDetail.is_active || !!targetDetail.deleted_at;

            return (
                <div className="flex flex-wrap items-center justify-end gap-2 text-xs text-muted-foreground">
                    <span>가입일 {formatDateTime(targetDetail.created_at)}</span>

                    <span
                        className={`rounded-full px-2 py-0.5 font-semibold ${
                            isInactive
                                ? "bg-rose-50 text-rose-600 dark:bg-rose-900/30 dark:text-rose-300"
                                : "bg-slate-100 text-slate-600 dark:bg-slate-800 dark:text-slate-300"
                        }`}
                    >
                        {targetDetail.deleted_at
                            ? "탈퇴"
                            : targetDetail.is_active
                                ? "활성"
                                : "비활성"}
                    </span>
                </div>
            );
        }

        return null;
    };

    const renderTargetDetail = () => {
        if (isTargetLoading) {
            return (
                <p className="text-sm text-muted-foreground">
                    신고 대상 정보를 불러오는 중입니다.
                </p>
            );
        }

        if (targetError) {
            return <p className="text-sm text-rose-500">{targetError}</p>;
        }

        if (!targetDetail) {
            return (
                <p className="text-sm text-muted-foreground">
                    신고 대상 정보가 없습니다.
                </p>
            );
        }

        if (targetDetail.kind === "post") {
            const postImageUrl = buildCommunityImageUrl(targetDetail.image_url);

            return (
                <div className="space-y-5">
                    <div className="flex items-center gap-3 rounded-xl bg-background/60 p-3">
                        {targetDetail.author_profile_image_url ? (
                            <img
                                src={targetDetail.author_profile_image_url}
                                alt="게시글 작성자 프로필"
                                className="h-10 w-10 rounded-full border border-border object-cover"
                            />
                        ) : (
                            <div className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-background text-xs text-muted-foreground">
                                기본
                            </div>
                        )}

                        <div className="min-w-0">
                            <p className="font-semibold">
                                {targetDetail.author_nickname ||
                                    targetDetail.author_email ||
                                    "작성자 정보 없음"}
                            </p>
                            <p className="truncate text-xs text-muted-foreground">
                                {targetDetail.author_email || targetDetail.author_id}
                            </p>
                        </div>
                    </div>

                    <div className="space-y-1.5">
                        <p className="text-xs font-bold text-muted-foreground">
                            게시글 제목
                        </p>
                        <p className="break-words text-sm font-semibold leading-6 text-foreground">
                            {targetDetail.title || "제목 없음"}
                        </p>
                    </div>

                    <div className="space-y-1.5">
                        <p className="text-xs font-bold text-muted-foreground">
                            게시글 내용
                        </p>

                        <div className="rounded-xl bg-background/60 p-4">
                            {renderLongText(targetDetail.content, "내용 없음")}
                        </div>
                    </div>

                    {postImageUrl && (
                        <div className="space-y-3 border-t border-border pt-5">
                            <p className="text-xs font-bold text-muted-foreground">
                                첨부 이미지
                            </p>

                            <div className="h-56 overflow-hidden rounded-2xl border border-border bg-background/60">
                                <img
                                    src={postImageUrl}
                                    alt="신고 대상 게시글 이미지"
                                    className="h-full w-full object-cover"
                                />
                            </div>

                            <div className="flex justify-end">
                                <a
                                href={postImageUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="rounded-full border border-border bg-background px-3 py-1 text-xs font-semibold text-muted-foreground transition hover:bg-[var(--surface-subtle)] hover:text-foreground"
                                >
                                원본 보기
                            </a>
                        </div>
                        </div>
                        )}
    </div>
    );
    }

    if (targetDetail.kind === "comment") {
        return (
            <div className="space-y-3">
                <div className="flex items-center gap-3 rounded-lg bg-background/60 p-3">
                    {targetDetail.author_profile_image_url ? (
                        <img
                            src={targetDetail.author_profile_image_url}
                            alt="댓글 작성자 프로필"
                            className="h-10 w-10 rounded-full border border-border object-cover"
                        />
                    ) : (
                        <div className="flex h-10 w-10 items-center justify-center rounded-full border border-border bg-background text-xs text-muted-foreground">
                            기본
                        </div>
                    )}

                    <div className="min-w-0">
                        <p className="font-semibold">
                            {targetDetail.author_nickname ||
                                targetDetail.author_email ||
                                "작성자 정보 없음"}
                        </p>
                        <p className="truncate text-xs text-muted-foreground">
                            {targetDetail.author_email || targetDetail.author_id}
                        </p>
                    </div>
                </div>

                <div>
                    <p className="mb-1 text-xs font-semibold text-muted-foreground">
                        댓글 내용
                    </p>

                    {renderLongText(targetDetail.content, "내용 없음")}
                </div>

                <div className="grid gap-2 text-xs text-muted-foreground sm:grid-cols-2">
                    <p title={targetDetail.post_id}>
                        게시글 ID: {shortId(targetDetail.post_id)}
                    </p>
                </div>
            </div>
        );
    }

    if (targetDetail.kind === "user_profile") {
        return (
            <div className="space-y-5">
                <div className="flex items-center gap-3 rounded-xl bg-background/60 p-3">
                    {targetDetail.profile_image_url ? (
                        <img
                            src={targetDetail.profile_image_url}
                            alt="신고 대상 프로필 이미지"
                            className="h-12 w-12 rounded-full border border-border object-cover"
                        />
                    ) : (
                        <div className="flex h-12 w-12 items-center justify-center rounded-full border border-border bg-background text-xs text-muted-foreground">
                            기본
                        </div>
                    )}

                    <div className="min-w-0">
                        <p className="font-semibold">
                            {targetDetail.nickname || "닉네임 없음"}
                        </p>
                        <p className="truncate text-xs text-muted-foreground">
                            {targetDetail.email || "이메일 없음"}
                        </p>
                    </div>
                </div>

                <div className="space-y-2 border-t border-border pt-5">
                    <p className="text-xs font-bold text-muted-foreground">
                        현재 프로필 사진
                    </p>

                    <div className="flex justify-center rounded-2xl border border-border bg-background/60 p-5">
                        {targetDetail.profile_image_url ? (
                            <img
                                src={targetDetail.profile_image_url}
                                alt="신고 대상 프로필 이미지 확대"
                                className="h-32 w-32 rounded-full object-cover"
                            />
                        ) : (
                            <div className="flex h-32 w-32 items-center justify-center rounded-full bg-background text-sm text-muted-foreground">
                                이미지 없음
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    }

    if (targetDetail.kind === "user_nickname") {
        return (
            <div className="rounded-xl bg-background/60 p-4">
                <div>
                    <p className="mb-1 text-xs font-bold text-muted-foreground">
                        신고 대상 계정
                    </p>
                    <p className="truncate text-sm font-medium text-muted-foreground">
                        {targetDetail.email || "이메일 없음"}
                    </p>
                </div>

                <div className="mt-4 border-t border-border pt-4">
                    <p className="mb-2 text-xs font-bold text-muted-foreground">
                        현재 닉네임
                    </p>
                    <p className="break-words text-lg font-bold leading-7 text-foreground">
                        {targetDetail.nickname || "닉네임 없음"}
                    </p>
                </div>
            </div>
        );
    }

    return null;
};

return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4">
        <div className="flex max-h-[90vh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl border border-border bg-[var(--surface-elevated)] shadow-2xl">
            <div className="flex shrink-0 items-start justify-between gap-4 border-b border-border px-6 py-5">
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

            <div className="flex-1 overflow-y-auto px-6 py-5">
                <div className="space-y-5">
                    {/* ── 1. 요약 줄 ── */}
                    <div className="flex flex-wrap items-center gap-2">
                            <span
                                className={`rounded-full px-2.5 py-1 text-sm font-semibold ${
                                    REPORT_STATUS_STYLE[report.status]
                                }`}
                            >
                                {REPORT_STATUS_LABEL[report.status]}
                            </span>

                        <span className="text-sm text-muted-foreground">
                                {TARGET_TYPE_LABEL[report.target_type]}
                            {" · "}
                            {REASON_LABEL[report.reason]}
                            </span>

                        {accumulatedCount > 1 && (
                            <span className="text-sm font-semibold text-rose-600 dark:text-rose-300">
                                    누적 {accumulatedCount.toLocaleString()}회
                                </span>
                        )}
                    </div>

                    {/* ── 2. 신고 내용 검토 ── */}
                    <div className="rounded-2xl border border-border bg-[var(--surface-subtle)] p-6">
                        <div className="mb-5 flex items-start justify-between gap-4">
                            <p className="text-base font-semibold">신고 내용 검토</p>
                            {renderTargetMeta()}
                        </div>

                        <div className="space-y-6">
                            <div>
                                <p className="mb-2 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                                    신고 대상
                                </p>
                                {renderTargetDetail()}
                            </div>

                            <div className="border-t border-border pt-5">
                                <p className="mb-2 text-xs font-bold uppercase tracking-wide text-muted-foreground">
                                    신고자 상세 내용
                                </p>
                                <div className="rounded-lg bg-background/50 p-3">
                                    {renderLongText(detailText, "상세 내용이 없습니다.")}
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* ── 3. 신고자 정보 ── */}
                    <div className="rounded-xl border border-border bg-background/40 p-4">
                        <p className="mb-3 text-sm font-semibold">신고자</p>

                        <div className="space-y-2 text-sm">
                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">닉네임</span>
                                <span>{report.reporter_nickname || "닉네임 없음"}</span>
                            </div>

                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">이메일</span>
                                <span className="max-w-[320px] truncate">
                                        {report.reporter_email || "이메일 없음"}
                                    </span>
                            </div>

                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">신고일</span>
                                <span>{formatDateTime(report.created_at)}</span>
                            </div>
                        </div>
                    </div>

                    {/* ── 4. 운영 조치 ── */}
                    {isPending && (
                        <div className="rounded-xl border border-border bg-background/40 p-4">
                            <div className="mb-3 flex items-center gap-2">
                                <ShieldAlert className="h-4 w-4 text-rose-500" />
                                <p className="text-sm font-semibold">운영 조치</p>
                            </div>

                            <div className="flex flex-wrap gap-2">
                                {report.target_type === "post" && (
                                    <button
                                        type="button"
                                        disabled={isProcessing}
                                        onClick={() => handleApplyAction("delete_post")}
                                        className="inline-flex items-center gap-2 rounded-lg bg-rose-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        <Trash2 className="h-4 w-4" />
                                        게시글 삭제 후 처리완료
                                    </button>
                                )}

                                {report.target_type === "comment" && (
                                    <button
                                        type="button"
                                        disabled={isProcessing}
                                        onClick={() => handleApplyAction("delete_comment")}
                                        className="inline-flex items-center gap-2 rounded-lg bg-rose-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        <Trash2 className="h-4 w-4" />
                                        댓글 삭제 후 처리완료
                                    </button>
                                )}

                                {report.target_type === "user_profile" && (
                                    <>
                                        <button
                                            type="button"
                                            disabled={isProcessing}
                                            onClick={() =>
                                                handleApplyAction("request_profile_image_change")
                                            }
                                            className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            <ShieldAlert className="h-4 w-4" />
                                            프로필 사진 변경 요청 처리완료
                                        </button>

                                        <button
                                            type="button"
                                            disabled={isProcessing}
                                            onClick={() => handleApplyAction("clear_profile_image")}
                                            className="inline-flex items-center gap-2 rounded-lg bg-rose-500 px-3 py-2 text-sm font-semibold text-white transition hover:bg-rose-600 disabled:cursor-not-allowed disabled:opacity-50"
                                        >
                                            <UserRoundX className="h-4 w-4" />
                                            프로필 사진 기본 이미지로 변경
                                        </button>
                                    </>
                                )}

                                {report.target_type === "user_nickname" && (
                                    <button
                                        type="button"
                                        disabled={isProcessing}
                                        onClick={() => handleApplyAction("request_nickname_change")}
                                        className="inline-flex items-center gap-2 rounded-lg bg-blue-600 px-3 py-2 text-sm font-semibold text-white transition hover:bg-blue-700 disabled:cursor-not-allowed disabled:opacity-50"
                                    >
                                        <ShieldAlert className="h-4 w-4" />
                                        닉네임 변경 요청 처리완료
                                    </button>
                                )}
                            </div>

                            <p className="mt-3 text-xs leading-5 text-muted-foreground">
                                신고 대상에 적용할 운영 조치를 선택하세요. 이미 삭제되었거나 다른 신고로 처리된 대상이면
                                ‘조치 없이 처리완료’로 남은 신고를 정리할 수 있습니다.
                            </p>
                        </div>
                    )}

                    {/* ── 5. 기술 정보 (접기) ── */}
                    <details className="group rounded-xl border border-border bg-[var(--surface-subtle)]">
                        <summary className="flex cursor-pointer items-center justify-between gap-3 px-4 py-3 text-sm font-semibold">
                            <span>기술 정보 (ID·타임스탬프)</span>
                            <ChevronDown className="h-4 w-4 transition-transform group-open:rotate-180" />
                        </summary>

                        <div className="space-y-2 border-t border-border px-4 py-3 text-sm">
                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">신고 ID</span>
                                <span title={report.id}>{shortId(report.id)}</span>
                            </div>

                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">신고자 ID</span>
                                <span className="max-w-[320px] truncate" title={report.reporter_id}>
                                        {report.reporter_id}
                                    </span>
                            </div>

                            <div className="flex justify-between gap-4">
                                <span className="text-muted-foreground">대상 ID</span>
                                <span className="max-w-[320px] truncate" title={report.target_id}>
                                        {report.target_id}
                                    </span>
                            </div>

                            {report.reviewed_at && (
                                <div className="flex justify-between gap-4">
                                    <span className="text-muted-foreground">처리일</span>
                                    <span>{formatDateTime(report.reviewed_at)}</span>
                                </div>
                            )}

                            {report.reviewed_by && (
                                <div className="flex justify-between gap-4">
                                    <span className="text-muted-foreground">처리 관리자 ID</span>
                                    <span className="max-w-[320px] truncate" title={report.reviewed_by}>
                                            {report.reviewed_by}
                                        </span>
                                </div>
                            )}
                        </div>
                    </details>

                    {/* ── 6. 감사 로그 (접기) ── */}
                    <div className="rounded-xl border border-border bg-[var(--surface-subtle)] p-4">
                        <button
                            type="button"
                            onClick={handleToggleAuditLogs}
                            className="flex w-full items-center justify-between gap-3 text-left"
                        >
                                <span className="inline-flex items-center gap-2 text-sm font-semibold">
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
                                    <p className="text-sm text-rose-500">{auditLogsError}</p>
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
                                                    <span title={log.admin_id}>{log.admin_id}</span>
                                                </p>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            </div>

            <div className="flex shrink-0 justify-end gap-2 border-t border-border bg-[var(--surface-elevated)] px-6 py-4">
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
                    조치 없이 처리완료
                </button>
            </div>
        </div>
    </div>
);
}