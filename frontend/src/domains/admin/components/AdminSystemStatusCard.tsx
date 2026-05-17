// domains/admin/components/AdminSystemStatusCard.tsx
//
// 관리자 대시보드용 서비스 상태 공지 관리 카드.
//
// 역할:
// - 점검/장애 배너 ON/OFF
// - 유형 선택
// - 제목/내용 수정
// - 시작/종료 시각 입력
//
// 지금은 안내형 배너만 관리한다.
// blocking은 저장은 가능하지만 1차 UI에서는 사용하지 않는다.

import { useEffect, useState } from "react";
import { toast } from "sonner";

import { Button } from "@/shared/ui/button";
import { Card } from "@/shared/ui/card";
import {
    getSystemStatus,
    updateSystemStatus,
    type SystemStatusResponse,
    type SystemStatusType,
} from "@/shared/api/systemStatusApi";

const toDatetimeLocalValue = (value: string | null): string => {
    if (!value) return "";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "";
    }

    const offset = date.getTimezoneOffset();
    const local = new Date(date.getTime() - offset * 60 * 1000);

    return local.toISOString().slice(0, 16);
};

const fromDatetimeLocalValue = (value: string): string | null => {
    if (!value) return null;

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return null;
    }

    return date.toISOString();
};

export default function AdminSystemStatusCard() {
    const [status, setStatus] = useState<SystemStatusResponse | null>(null);
    const [enabled, setEnabled] = useState(false);
    const [statusType, setStatusType] =
        useState<SystemStatusType>("maintenance");
    const [title, setTitle] = useState("");
    const [message, setMessage] = useState("");
    const [startsAt, setStartsAt] = useState("");
    const [endsAt, setEndsAt] = useState("");

    const [isLoading, setIsLoading] = useState(false);
    const [isSaving, setIsSaving] = useState(false);

    const loadStatus = async () => {
        setIsLoading(true);

        try {
            const data = await getSystemStatus();

            setStatus(data);
            setEnabled(data.enabled);
            setStatusType(data.status_type);
            setTitle(data.title);
            setMessage(data.message);
            setStartsAt(toDatetimeLocalValue(data.starts_at));
            setEndsAt(toDatetimeLocalValue(data.ends_at));
        } catch (error) {
            console.error("서비스 상태 조회 실패:", error);
            toast.error("서비스 상태 공지를 불러오지 못했습니다.");
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        void loadStatus();
    }, []);

    const handleSave = async () => {
        setIsSaving(true);

        try {
            const updated = await updateSystemStatus({
                enabled,
                blocking: status?.blocking ?? false,
                status_type: statusType,
                title,
                message,
                starts_at: fromDatetimeLocalValue(startsAt),
                ends_at: fromDatetimeLocalValue(endsAt),
            });

            setStatus(updated);
            toast.success("서비스 상태 공지가 저장되었습니다.");
        } catch (error) {
            console.error("서비스 상태 저장 실패:", error);
            toast.error(
                error instanceof Error
                    ? error.message
                    : "서비스 상태 공지를 저장하지 못했습니다.",
            );
        } finally {
            setIsSaving(false);
        }
    };

    return (
        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
            <div className="mb-5 flex flex-wrap items-start justify-between gap-3">
                <div>
                    <h3 className="text-lg font-bold">서비스 상태 공지</h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                        로그인 화면에 표시할 점검/장애 배너를 관리합니다.
                    </p>
                </div>

                <button
                    type="button"
                    onClick={() => setEnabled((prev) => !prev)}
                    className={[
                        "rounded-full px-3 py-1.5 text-sm font-bold transition",
                        enabled
                            ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-950/40 dark:text-emerald-300"
                            : "bg-gray-100 text-gray-500 dark:bg-gray-900 dark:text-gray-400",
                    ].join(" ")}
                >
                    {enabled ? "ON" : "OFF"}
                </button>
            </div>

            {isLoading ? (
                <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    서비스 상태 공지를 불러오는 중입니다.
                </div>
            ) : (
                <div className="space-y-4">
                    <div>
                        <label className="mb-1 block text-sm font-semibold">
                            유형
                        </label>

                        <select
                            value={statusType}
                            onChange={(event) =>
                                setStatusType(event.target.value as SystemStatusType)
                            }
                            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-cyan-500"
                        >
                            <option value="maintenance">점검 안내</option>
                            <option value="incident">장애 안내</option>
                        </select>
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-semibold">
                            제목
                        </label>

                        <input
                            value={title}
                            onChange={(event) => setTitle(event.target.value)}
                            placeholder="예: 서비스 점검 안내"
                            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-cyan-500"
                        />
                    </div>

                    <div>
                        <label className="mb-1 block text-sm font-semibold">
                            내용
                        </label>

                        <textarea
                            value={message}
                            onChange={(event) => setMessage(event.target.value)}
                            placeholder="예: 금일 22:00부터 23:00까지 서버 점검이 진행됩니다."
                            rows={4}
                            className="w-full resize-none rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-cyan-500"
                        />
                    </div>

                    <div className="grid grid-cols-1 gap-3 md:grid-cols-2">
                        <div>
                            <label className="mb-1 block text-sm font-semibold">
                                시작 시각
                            </label>

                            <input
                                type="datetime-local"
                                value={startsAt}
                                onChange={(event) => setStartsAt(event.target.value)}
                                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-cyan-500"
                            />
                        </div>

                        <div>
                            <label className="mb-1 block text-sm font-semibold">
                                종료 시각
                            </label>

                            <input
                                type="datetime-local"
                                value={endsAt}
                                onChange={(event) => setEndsAt(event.target.value)}
                                className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none focus:border-cyan-500"
                            />
                        </div>
                    </div>

                    <Button
                        type="button"
                        onClick={() => void handleSave()}
                        disabled={isSaving}
                        className="w-full"
                    >
                        {isSaving ? "저장 중..." : "서비스 상태 공지 저장"}
                    </Button>
                </div>
            )}
        </Card>
    );
}