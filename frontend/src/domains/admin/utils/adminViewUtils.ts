// src/domains/admin/utils/adminViewUtils.ts
//
// 관리자 화면에서 공통으로 사용하는 표시용 유틸/라벨 모음.
//
// 역할:
// - 신고 상태 라벨
// - 신고 상태 배지 스타일
// - 신고 대상 타입 라벨
// - 신고 사유 라벨
// - 날짜 포맷
// - UUID 짧게 표시
// - 백엔드 응답 객체에서 안전하게 텍스트 추출
//
// 왜 분리하나?
// - 신고 목록, 최근 신고, 상세 모달에서 같은 라벨/포맷 로직을 반복 사용한다.
// - 컴포넌트 파일에 라벨과 포맷 함수가 섞이면 JSX 가독성이 떨어진다.

import type {
    AdminContentReportResponse,
    ContentReportStatus,
} from "@/domains/admin/api/adminApi.ts";

// ─────────────────────────────────────────────
// 신고 상태 표시 텍스트
// ─────────────────────────────────────────────

export const REPORT_STATUS_LABEL: Record<ContentReportStatus, string> = {
    pending: "대기중",
    resolved: "처리완료",
    rejected: "반려",
}

// ─────────────────────────────────────────────
// 신고 상태별 배지 스타일
// ─────────────────────────────────────────────
//
// Tailwind className을 상태별로 관리한다.
// 테이블/모달/대시보드에서 같은 스타일을 재사용한다.

export const REPORT_STATUS_STYLE: Record<ContentReportStatus, string> = {
    pending:
        "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/40 dark:text-yellow-300",

    resolved:
        "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300",

    rejected:
        "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300",
};

// ─────────────────────────────────────────────
// 신고 대상 타입 표시 텍스트
// ─────────────────────────────────────────────

export const TARGET_TYPE_LABEL: Record<AdminContentReportResponse["target_type"],string> = {
    post: "게시글",
    comment: "댓글",
    user_nickname: "닉네임",
    user_profile: "프로필 사진",
}

// ─────────────────────────────────────────────
// 신고 사유 표시 텍스트
// ─────────────────────────────────────────────

export const REASON_LABEL: Record<AdminContentReportResponse["reason"], string> = {
    abuse: "욕설/비방",
    inappropriate: "부적합",
    spam: "광고/도배",
    other: "기타",
}

// ─────────────────────────────────────────────
// 날짜 포맷 함수
// ─────────────────────────────────────────────
//
// 예:
// 2026-05-08T12:30:00Z
// → 2026.05.08 21:30
//
// value가 null/undefined/잘못된 날짜면 "-" 표시.

export function formatDateTime(value: string | null | undefined) {
    if (!value) return "-";

    const date = new Date(value);

    if (Number.isNaN(date.getTime())) {
        return "-";
    }

    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    const hour = String(date.getHours()).padStart(2, "0");
    const minute = String(date.getMinutes()).padStart(2, "0");

    return `${year}.${month}.${day} ${hour}:${minute}`;
}

// ─────────────────────────────────────────────
// UUID 짧게 표시
// ─────────────────────────────────────────────
//
// 관리자 테이블에서 UUID 전체를 보여주면 너무 길어서 가독성이 떨어진다.
// 그래서 앞 8자리만 보여준다.
// 전체 값은 title 속성으로 확인할 수 있게 각 컴포넌트에서 처리한다.

export function shortId(id: string | null | undefined) {
    if (!id) return "-";
    return `${id.slice(0, 8)}...`;
}

// ─────────────────────────────────────────────
// 객체에서 안전하게 텍스트 추출
// ─────────────────────────────────────────────
//
// 백엔드 DTO 필드명이 조금씩 달라도 화면이 깨지지 않게 하기 위한 helper.
//
// 예:
// getTextValue(report, ["reporter_nickname", "reporter_email", "reporter_id"])
//
// 위 순서대로 값을 찾아서 첫 번째로 존재하는 문자열/숫자/boolean 값을 반환한다.
// 아무 값도 없으면 fallback을 반환한다.

export function getTextValue(
    source: unknown,
    keys: string[],
    fallback = "-"
): string {
    if (!source || typeof source !== "object") {
        return fallback;
    }

    const record = source as Record<string, unknown>;

    for (const key of keys) {
        const value = record[key];

        if (typeof value === "string" && value.trim()) {
            return value;
        }

        if (typeof value === "number") {
            return String(value);
        }

        if (typeof value === "boolean") {
            return value ? "true" : "false";
        }
    }

    return fallback;
}

// ─────────────────────────────────────────────
// 신고자 메인 표시값
// ─────────────────────────────────────────────
//
// 실무 관리자 화면에서는 UUID보다 사람이 읽을 수 있는 값이 먼저 보여야 한다.
//
// 우선순위:
// 1. reporter_nickname
// 2. reporter_email
// 3. reporter_id 일부
//
// 예:
// 김은영
// 또는 email@example.com
// 또는 c8e25294...

export function getReporterPrimaryText(report: AdminContentReportResponse) {
    if (report.reporter_nickname?.trim()) {
        return report.reporter_nickname;
    }

    if (report.reporter_email?.trim()) {
        return report.reporter_email;
    }

    return shortId(report.reporter_id);
}

// ─────────────────────────────────────────────
// 신고자 보조 표시값
// ─────────────────────────────────────────────
//
// 목록 테이블에서는 보통 이렇게 보여준다.
//
// 메인 줄:
// - 닉네임
//
// 보조 줄:
// - 이메일
// - 이메일이 없으면 ID 일부
//
// 닉네임과 이메일이 같은 값이면 중복 표시하지 않는다.

export function getReporterSecondaryText(report: AdminContentReportResponse) {
    const nickname = report.reporter_nickname?.trim();
    const email = report.reporter_email?.trim();

    if (email && email !== nickname) {
        return email;
    }

    return shortId(report.reporter_id);
}