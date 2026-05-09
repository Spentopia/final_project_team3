// src/domains/admin/components/AdminNoticesPanel.tsx
//
// 공지사항 관리 패널.
//
// 역할:
// - 공지사항 목록 표시
// - 공지사항 작성
// - 공지사항 수정
// - 공지사항 삭제
//
// 데이터 조회/API 호출은 여기서 직접 하지 않는다.
// 상위 AdminPage가 notices 상태와 handler를 props로 내려준다.
//
// 이 방식은 실무에서 많이 쓰는 Container + Presentational 구조다.
// - AdminPage: 상태/API 담당
// - AdminNoticesPanel: 화면/UI 담당

import { useEffect, useState } from "react";

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";

import type { AdminNoticeResponse } from "@/domains/admin/api/adminApi";

import { formatDateTime } from "@/domains/admin/utils/adminViewUtils";

type AdminNoticesPanelProps = {
    notices: AdminNoticeResponse[];
    isNoticesLoading: boolean;
    processingId: string | null;
    onCreateNotice: (params: { title: string; content: string }) => void;
    onUpdateNotice: (
        noticeId: string,
        params: { title: string; content: string }
    ) => void;
    onDeleteNotice: (noticeId: string) => void;
};

export default function AdminNoticesPanel({
                                              notices,
                                              isNoticesLoading,
                                              processingId,
                                              onCreateNotice,
                                              onUpdateNotice,
                                              onDeleteNotice,
                                          }: AdminNoticesPanelProps) {
    // 현재 수정 중인 공지 ID
    //
    // null이면 새 공지 작성 모드.
    // 값이 있으면 해당 공지 수정 모드.
    const [editingNoticeId, setEditingNoticeId] = useState<string | null>(null);

    // 폼 입력값
    const [title, setTitle] = useState("");
    const [content, setContent] = useState("");

    // 수정 모드 여부
    const isEditing = editingNoticeId !== null;

    // 수정 중인 공지 객체
    const editingNotice = notices.find((notice) => notice.id === editingNoticeId);

    // editingNoticeId가 바뀔 때 폼에 기존 공지 내용을 채운다.
    useEffect(() => {
        if (!editingNotice) {
            return;
        }

        setTitle(editingNotice.title);
        setContent(editingNotice.content);
    }, [editingNotice]);

    // 폼 초기화
    const resetForm = () => {
        setEditingNoticeId(null);
        setTitle("");
        setContent("");
    };

    // 작성/수정 제출
    const handleSubmit = () => {
        const normalizedTitle = title.trim();
        const normalizedContent = content.trim();

        if (!normalizedTitle) {
            alert("공지 제목을 입력해 주세요.");
            return;
        }

        if (!normalizedContent) {
            alert("공지 내용을 입력해 주세요.");
            return;
        }

        if (isEditing && editingNoticeId) {
            onUpdateNotice(editingNoticeId, {
                title: normalizedTitle,
                content: normalizedContent,
            });
            return;
        }

        onCreateNotice({
            title: normalizedTitle,
            content: normalizedContent,
        });

        resetForm();
    };

    return (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
            {/* 작성/수정 폼 */}
            <Card className="h-fit border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-5">
                    <h3 className="text-lg font-bold">
                        {isEditing ? "공지사항 수정" : "공지사항 작성"}
                    </h3>

                    <p className="mt-1 text-sm text-muted-foreground">
                        작성한 공지는 커뮤니티 공지사항으로 노출됩니다.
                    </p>
                </div>

                <div className="space-y-4">
                    <div>
                        <label className="mb-2 block text-sm font-semibold">
                            제목
                        </label>

                        <Input
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="공지 제목을 입력하세요"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">
                            내용
                        </label>

                        <textarea
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            placeholder="공지 내용을 입력하세요"
                            className="min-h-48 w-full rounded-xl border border-border bg-background px-3 py-3 text-sm outline-none transition focus:border-cyan-500"
                        />
                    </div>

                    <div className="flex justify-end gap-2">
                        {isEditing && (
                            <button
                                type="button"
                                onClick={resetForm}
                                className="rounded-lg border border-border px-4 py-2 text-sm font-semibold transition hover:bg-[var(--surface-subtle)]"
                            >
                                취소
                            </button>
                        )}

                        <button
                            type="button"
                            onClick={handleSubmit}
                            disabled={processingId === "notice-form"}
                            className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-cyan-600 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {processingId === "notice-form"
                                ? "처리 중..."
                                : isEditing
                                    ? "수정하기"
                                    : "작성하기"}
                        </button>
                    </div>
                </div>
            </Card>

            {/* 공지 목록 */}
            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-4 flex items-center justify-between">
                    <div>
                        <h3 className="text-lg font-bold">공지사항 목록</h3>

                        <p className="mt-1 text-sm text-muted-foreground">
                            등록된 공지사항을 수정하거나 삭제할 수 있습니다.
                        </p>
                    </div>
                </div>

                {isNoticesLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        공지사항 목록을 불러오는 중입니다.
                    </div>
                )}

                {!isNoticesLoading && notices.length === 0 && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        등록된 공지사항이 없습니다.
                    </div>
                )}

                {!isNoticesLoading && notices.length > 0 && (
                    <div className="overflow-hidden rounded-xl border border-border">
                        <table className="w-full table-fixed text-sm">
                            <colgroup>
                                {/* 제목 */}
                                <col className="w-auto" />

                                {/* 작성일 */}
                                <col className="w-[160px]" />

                                {/* 조회수 */}
                                <col className="w-[90px]" />

                                {/* 관리 */}
                                <col className="w-[150px]" />
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3 pl-20">제목</th>
                                <th className="px-4 py-3">작성일</th>
                                <th className="px-4 py-3">조회수</th>
                                <th className="px-4 py-3 text-right">관리</th>
                            </tr>
                            </thead>

                            <tbody>
                            {notices.map((notice) => (
                                <tr key={notice.id} className="border-t border-border">
                                    <td className="px-4 py-3">
                                      <span className="block truncate font-semibold text-foreground" title={notice.title}>
                                        {notice.title}
                                      </span>
                                    </td>

                                    <td className="px-4 py-3 text-muted-foreground">
                                      <span className="block truncate">
                                        {formatDateTime(notice.created_at)}
                                      </span>
                                    </td>

                                    <td className="px-4 py-3 text-right text-muted-foreground">
                                        {notice.view_count}
                                    </td>

                                    <td className="px-4 py-3">
                                        <div className="flex justify-end gap-2">
                                            <button
                                                type="button"
                                                onClick={() => setEditingNoticeId(notice.id)}
                                                className="rounded-lg border border-border px-3 py-1.5 text-xs font-semibold transition hover:bg-[var(--surface-subtle)]"
                                            >
                                                수정
                                            </button>

                                            <button
                                                type="button"
                                                disabled={processingId === notice.id}
                                                onClick={() => {
                                                    const ok = window.confirm(
                                                        "이 공지사항을 삭제하시겠습니까?"
                                                    );

                                                    if (ok) {
                                                        onDeleteNotice(notice.id);
                                                    }
                                                }}
                                                className="rounded-lg bg-gray-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                                            >
                                                {processingId === notice.id ? "삭제 중..." : "삭제"}
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
        </div>
    );
}