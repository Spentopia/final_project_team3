// src/domains/admin/components/AdminContestsPanel.tsx
//
// 아바타 콘테스트 관리 패널.
//
// 역할:
// - 콘테스트 목록 표시
// - 콘테스트 생성
// - 콘테스트 수정
// - 콘테스트 상태 변경
//
// 삭제 기능은 1차에서 제외한다.
// contest_events 테이블에 is_deleted/deleted_at이 없기 때문.

import { useEffect, useState } from "react";

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";

import type {
    AdminContestResponse,
    AdminContestStatus,
} from "@/domains/admin/api/adminApi";

import { formatDateTime } from "@/domains/admin/utils/adminViewUtils";

type AdminContestsPanelProps = {
    contests: AdminContestResponse[];
    isContestsLoading: boolean;
    processingId: string | null;
    onCreateContest: (params: {
        title: string;
        description: string | null;
        start_date: string;
        end_date: string;
        status: AdminContestStatus;
        reward_description: string | null;
    }) => void;
    onUpdateContest: (
        contestId: string,
        params: {
            title: string;
            description: string | null;
            start_date: string;
            end_date: string;
            status: AdminContestStatus;
            reward_description: string | null;
        }
    ) => void;
    onUpdateContestStatus: (
        contestId: string,
        status: AdminContestStatus
    ) => void;
};

const STATUS_LABEL: Record<AdminContestStatus, string> = {
    upcoming: "예정",
    active: "진행중",
    ended: "종료",
};

const STATUS_STYLE: Record<AdminContestStatus, string> = {
    upcoming:
        "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",
    active:
        "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",
    ended: "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300",
};

function toDateTimeLocalValue(value: string | null | undefined) {
    if (!value) return "";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");

    return `${year}-${month}-${day}T${hour}:${minute}`;
}

function toIsoStringFromDateTimeLocal(value: string) {
    return new Date(value).toISOString();
}

export default function AdminContestsPanel({
                                               contests,
                                               isContestsLoading,
                                               processingId,
                                               onCreateContest,
                                               onUpdateContest,
                                               onUpdateContestStatus,
                                           }: AdminContestsPanelProps) {
    const [editingContestId, setEditingContestId] = useState<string | null>(null);

    const [title, setTitle] = useState("");
    const [description, setDescription] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [status, setStatus] = useState<AdminContestStatus>("upcoming");
    const [rewardDescription, setRewardDescription] = useState("");

    const isEditing = editingContestId !== null;
    const editingContest = contests.find(
        (contest) => contest.id === editingContestId
    );

    useEffect(() => {
        if (!editingContest) return;

        setTitle(editingContest.title);
        setDescription(editingContest.description ?? "");
        setStartDate(toDateTimeLocalValue(editingContest.start_date));
        setEndDate(toDateTimeLocalValue(editingContest.end_date));
        setStatus(editingContest.status as AdminContestStatus);
        setRewardDescription(editingContest.reward_description ?? "");
    }, [editingContest]);

    const resetForm = () => {
        setEditingContestId(null);
        setTitle("");
        setDescription("");
        setStartDate("");
        setEndDate("");
        setStatus("upcoming");
        setRewardDescription("");
    };

    const handleSubmit = () => {
        const normalizedTitle = title.trim();

        if (!normalizedTitle) {
            alert("콘테스트 제목을 입력해 주세요.");
            return;
        }

        if (!startDate) {
            alert("시작일을 입력해 주세요.");
            return;
        }

        if (!endDate) {
            alert("종료일을 입력해 주세요.");
            return;
        }

        const start = new Date(startDate);
        const end = new Date(endDate);

        if (end <= start) {
            alert("종료일은 시작일 이후여야 합니다.");
            return;
        }

        const payload = {
            title: normalizedTitle,
            description: description.trim() || null,
            start_date: toIsoStringFromDateTimeLocal(startDate),
            end_date: toIsoStringFromDateTimeLocal(endDate),
            status,
            reward_description: rewardDescription.trim() || null,
        };

        if (isEditing && editingContestId) {
            onUpdateContest(editingContestId, payload);
            return;
        }

        onCreateContest(payload);
        resetForm();
    };

    return (
        <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_minmax(0,1fr)]">
            <Card className="h-fit border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-5">
                    <h3 className="text-lg font-bold">
                        {isEditing ? "콘테스트 수정" : "콘테스트 생성"}
                    </h3>

                    <p className="mt-1 text-sm text-muted-foreground">
                        아바타 콘테스트 기간, 상태, 보상을 관리합니다.
                    </p>
                </div>

                <div className="space-y-4">
                    <div>
                        <label className="mb-2 block text-sm font-semibold">제목</label>
                        <Input
                            value={title}
                            onChange={(e) => setTitle(e.target.value)}
                            placeholder="예: 5월 아바타 콘테스트"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">설명</label>
                        <textarea
                            value={description}
                            onChange={(e) => setDescription(e.target.value)}
                            placeholder="콘테스트 설명을 입력하세요"
                            className="min-h-28 w-full rounded-xl border border-border bg-background px-3 py-3 text-sm outline-none transition focus:border-cyan-500"
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">시작일</label>
                        <Input
                            type="datetime-local"
                            value={startDate}
                            onChange={(e) => setStartDate(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">종료일</label>
                        <Input
                            type="datetime-local"
                            value={endDate}
                            onChange={(e) => setEndDate(e.target.value)}
                        />
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">상태</label>
                        <select
                            value={status}
                            onChange={(e) => setStatus(e.target.value as AdminContestStatus)}
                            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-cyan-500"
                        >
                            <option value="upcoming">예정</option>
                            <option value="active">진행중</option>
                            <option value="ended">종료</option>
                        </select>
                    </div>

                    <div>
                        <label className="mb-2 block text-sm font-semibold">보상</label>
                        <Input
                            value={rewardDescription}
                            onChange={(e) => setRewardDescription(e.target.value)}
                            placeholder="예: 1등 500 SPT"
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
                            disabled={processingId === "contest-form"}
                            className="rounded-lg bg-cyan-500 px-4 py-2 text-sm font-semibold text-white transition hover:bg-cyan-600 disabled:cursor-not-allowed disabled:opacity-60"
                        >
                            {processingId === "contest-form"
                                ? "처리 중..."
                                : isEditing
                                    ? "수정하기"
                                    : "생성하기"}
                        </button>
                    </div>
                </div>
            </Card>

            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="mb-4">
                    <h3 className="text-lg font-bold">콘테스트 목록</h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                        등록된 아바타 콘테스트를 관리합니다.
                    </p>
                </div>

                {isContestsLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        콘테스트 목록을 불러오는 중입니다.
                    </div>
                )}

                {!isContestsLoading && contests.length === 0 && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        등록된 콘테스트가 없습니다.
                    </div>
                )}

                {!isContestsLoading && contests.length > 0 && (
                    <div className="overflow-x-auto rounded-xl border border-border">
                        <div className="w-max min-w-[980px] text-sm">
                            <div
                                className="grid items-center bg-[var(--surface-subtle)] text-muted-foreground"
                                style={{
                                    gridTemplateColumns:
                                        "220px 150px 150px 90px 180px 190px",
                                }}
                            >
                                <div className="px-4 py-3 font-semibold">제목</div>
                                <div className="px-4 py-3 font-semibold">시작일</div>
                                <div className="px-4 py-3 font-semibold">종료일</div>
                                <div className="px-4 py-3 font-semibold">상태</div>
                                <div className="px-4 py-3 font-semibold">보상</div>
                                <div className="px-4 py-3 text-right font-semibold">관리</div>
                            </div>

                            {contests.map((contest) => {
                                const currentStatus = contest.status as AdminContestStatus;

                                return (
                                    <div
                                        key={contest.id}
                                        className="grid items-center border-t border-border"
                                        style={{
                                            gridTemplateColumns:
                                                "220px 150px 150px 90px 180px 190px",
                                        }}
                                    >
                                        <div className="px-4 py-3 font-medium">
                      <span className="block truncate" title={contest.title}>
                        {contest.title}
                      </span>
                                        </div>

                                        <div className="px-4 py-3 text-muted-foreground">
                                            {formatDateTime(contest.start_date)}
                                        </div>

                                        <div className="px-4 py-3 text-muted-foreground">
                                            {formatDateTime(contest.end_date)}
                                        </div>

                                        <div className="px-4 py-3">
                      <span
                          className={`inline-flex rounded-full px-2 py-1 text-xs font-bold ${
                              STATUS_STYLE[currentStatus] ??
                              "bg-gray-100 text-gray-600"
                          }`}
                      >
                        {STATUS_LABEL[currentStatus] ?? contest.status}
                      </span>
                                        </div>

                                        <div className="px-4 py-3 text-muted-foreground">
                      <span
                          className="block truncate"
                          title={contest.reward_description ?? ""}
                      >
                        {contest.reward_description || "-"}
                      </span>
                                        </div>

                                        <div className="px-4 py-3">
                                            <div className="flex justify-end gap-2">
                                                <button
                                                    type="button"
                                                    onClick={() => setEditingContestId(contest.id)}
                                                    className="rounded-lg border border-border px-3 py-1.5 text-xs font-semibold transition hover:bg-[var(--surface-subtle)]"
                                                >
                                                    수정
                                                </button>

                                                {currentStatus !== "active" && (
                                                    <button
                                                        type="button"
                                                        disabled={processingId === contest.id}
                                                        onClick={() =>
                                                            onUpdateContestStatus(contest.id, "active")
                                                        }
                                                        className="rounded-lg bg-emerald-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-emerald-600 disabled:cursor-not-allowed disabled:opacity-50"
                                                    >
                                                        진행
                                                    </button>
                                                )}

                                                {currentStatus !== "ended" && (
                                                    <button
                                                        type="button"
                                                        disabled={processingId === contest.id}
                                                        onClick={() =>
                                                            onUpdateContestStatus(contest.id, "ended")
                                                        }
                                                        className="rounded-lg bg-gray-500 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-gray-600 disabled:cursor-not-allowed disabled:opacity-50"
                                                    >
                                                        종료
                                                    </button>
                                                )}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })}
                        </div>
                    </div>
                )}
            </Card>
        </div>
    );
}