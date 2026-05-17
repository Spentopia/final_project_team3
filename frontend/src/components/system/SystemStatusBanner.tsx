// shared/components/SystemStatusBanner.tsx
//
// 점검/장애 공지 배너.
//
// 사용 위치:
// - 로그인 페이지: 로그인 폼 위
// - 로그인 후 레이아웃: Header 아래 또는 상단 영역
//
// 정책:
// - enabled=false면 null 반환
// - maintenance는 amber 계열
// - incident는 rose 계열

import { AlertTriangle, Wrench } from "lucide-react";

import type { SystemStatusResponse } from "@/shared/api/systemStatusApi";

type SystemStatusBannerProps = {
    status: SystemStatusResponse | null;
    compact?: boolean;
};

const formatDateTime = (value: string) => {
    return new Date(value).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
    });
};

export default function SystemStatusBanner({
                                               status,
                                               compact = false,
                                           }: SystemStatusBannerProps) {
    if (!status?.enabled) {
        return null;
    }

    const isIncident = status.status_type === "incident";
    const Icon = isIncident ? AlertTriangle : Wrench;

    return (
        <div
            className={[
                "rounded-2xl border shadow-sm",
                compact ? "px-4 py-3" : "px-5 py-4",
                isIncident
                    ? "border-rose-200 bg-rose-50 text-rose-700 dark:border-rose-900/60 dark:bg-rose-950/30 dark:text-rose-300"
                    : "border-amber-200 bg-amber-50 text-amber-800 dark:border-amber-900/60 dark:bg-amber-950/30 dark:text-amber-300",
            ].join(" ")}
        >
            <div className="flex items-start gap-3">
                <Icon className="mt-0.5 h-5 w-5 shrink-0" />

                <div className="min-w-0">
                    <p className="font-bold">
                        {status.title ||
                            (isIncident ? "서비스 장애 안내" : "서비스 점검 안내")}
                    </p>

                    {status.message && (
                        <p className="mt-1 whitespace-pre-wrap text-sm leading-6">
                            {status.message}
                        </p>
                    )}

                    {(status.starts_at || status.ends_at) && (
                        <p className="mt-2 text-xs opacity-80">
                            {status.starts_at && `시작: ${formatDateTime(status.starts_at)}`}
                            {status.starts_at && status.ends_at && " · "}
                            {status.ends_at && `종료: ${formatDateTime(status.ends_at)}`}
                        </p>
                    )}
                </div>
            </div>
        </div>
    );
}