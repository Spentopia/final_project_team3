// src/domains/admin/components/AdminUsersPanel.tsx
//
// 회원 관리 패널.
//
// 이번 수정:
// - 표 헤더와 셀 정렬을 전부 왼쪽으로 통일.
//   숫자 컬럼이 없으므로(텍스트/배지/버튼) 좌측 정렬이 가장 깔끔하고
//   헤더-셀 어긋남이 사라진다.

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";
import { Search } from "lucide-react";

import type { AdminUserResponse } from "@/domains/admin/api/adminApi";

import Pagination from "@/components/page/Pagination.tsx";

import {
    formatDateTime,
    getTextValue,
} from "@/domains/admin/utils/adminViewUtils";

type AdminUsersPanelProps = {
    users: AdminUserResponse[];
    userKeyword: string;
    isUsersLoading: boolean;
    processingId: string | null;
    page: number;
    totalCount: number;
    pageSize: number;
    onPageChange: (page: number) => void;
    onUserKeywordChange: (keyword: string) => void;
    onToggleUserActive: (user: AdminUserResponse) => void;
};

function getUserStatusLabel(user: AdminUserResponse): string {
    if (user.deleted_at) {
        return "탈퇴";
    }

    return user.is_active ? "활성" : "비활성";
}

function getUserStatusClassName(user: AdminUserResponse): string {
    if (user.deleted_at) {
        return "bg-red-100 text-red-700 dark:bg-red-900/40 dark:text-red-300";
    }

    if (user.is_active) {
        return "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300";
    }

    return "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300";
}

function canToggleUserActive(user: AdminUserResponse): boolean {
    if (user.deleted_at) {
        return false;
    }

    if (user.role_type === "admin") {
        return false;
    }

    return true;
}

function getToggleDisabledReason(user: AdminUserResponse): string {
    if (user.deleted_at) {
        return "탈퇴한 회원은 활성/비활성 상태를 변경할 수 없습니다.";
    }

    if (user.role_type === "admin") {
        return "운영자 계정은 활성/비활성 상태를 변경할 수 없습니다.";
    }

    return "";
}

export default function AdminUsersPanel({
                                            users,
                                            userKeyword,
                                            isUsersLoading,
                                            processingId,
                                            page,
                                            totalCount,
                                            pageSize,
                                            onPageChange,
                                            onUserKeywordChange,
                                            onToggleUserActive,
                                        }: AdminUsersPanelProps) {
    const totalPages = Math.max(1, Math.ceil(totalCount / Math.max(1, pageSize)));

    const rangeStart = totalCount === 0 ? 0 : (page - 1) * pageSize + 1;
    const rangeEnd = Math.min(page * pageSize, totalCount);

    return (
        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
            <div className="mb-4 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                    <h3 className="text-lg font-bold">회원 목록</h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                        이메일 또는 닉네임으로 회원을 검색합니다.
                    </p>
                </div>

                <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <label htmlFor="user-search" className="sr-only">
                            닉네임 또는 이메일 검색
                        </label>
                        <Input
                            id="user-search"
                            name="userSearch"
                            type="search"
                            placeholder="닉네임/이메일 검색"
                            className="h-11 w-64 pl-10 text-base"
                            value={userKeyword}
                            onChange={(e) => onUserKeywordChange(e.target.value)}
                        />
                    </div>

                    <span className="ml-auto text-sm text-muted-foreground">
                        {totalCount > 0
                            ? `총 ${totalCount.toLocaleString()}명 · ${rangeStart.toLocaleString()}-${rangeEnd.toLocaleString()} 표시 중`
                            : "총 0명"}
                    </span>
                </div>
            </div>

            {isUsersLoading && (
                <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                    회원 목록을 불러오는 중입니다.
                </div>
            )}

            {!isUsersLoading && users.length === 0 && (
                <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                    조건에 맞는 회원이 없습니다.
                </div>
            )}

            {!isUsersLoading && users.length > 0 && (
                <div className="overflow-x-auto rounded-2xl border border-border bg-white/60 dark:bg-gray-900/20">
                    <table className="min-w-[1120px] w-full table-fixed text-sm">
                        <colgroup>
                            <col className="w-[170px]" /> {/* 닉네임 */}
                            <col className="w-[260px]" /> {/* 이메일 */}
                            <col className="w-[170px]" /> {/* 가입일 */}
                            <col className="w-[280px]" /> {/* 상태 */}
                            <col className="w-[110px]" /> {/* 역할 */}
                            <col className="w-[130px]" /> {/* 관리 */}
                        </colgroup>

                        <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                        <tr>
                            <th className="px-4 py-3">닉네임</th>
                            <th className="px-4 py-3">이메일</th>
                            <th className="px-4 py-3">가입일</th>
                            <th className="px-4 py-3">상태</th>
                            <th className="px-4 py-3">역할</th>
                            <th className="px-4 py-3">관리</th>
                        </tr>
                        </thead>

                        <tbody>
                        {users.map((user) => (
                            <tr
                                key={user.id}
                                className="border-t border-border transition-colors hover:bg-[var(--surface-subtle)]/70"
                            >
                                <td className="px-4 py-3 align-middle font-medium">
                                    <span
                                        className="block truncate"
                                        title={getTextValue(user, ["nickname"])}
                                    >
                                        {getTextValue(user, ["nickname"])}
                                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span
                                        className="block truncate"
                                        title={getTextValue(user, ["email"])}
                                    >
                                        {getTextValue(user, ["email"])}
                                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span className="block truncate">
                                        {formatDateTime(user.created_at)}
                                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle">
                                    <div className="space-y-2">
                                        <span
                                            className={`inline-flex rounded-full px-2.5 py-1 text-xs font-bold ${getUserStatusClassName(user)}`}
                                            title={
                                                user.deleted_at
                                                    ? `탈퇴일: ${formatDateTime(user.deleted_at)}`
                                                    : undefined
                                            }
                                        >
                                            {getUserStatusLabel(user)}
                                        </span>

                                        {!user.is_active && !user.deleted_at && (
                                            <div className="max-w-[250px] rounded-xl border border-red-100 bg-red-50 px-3 py-2 text-xs leading-5 text-red-800 dark:border-red-900/50 dark:bg-red-950/30 dark:text-red-200">
                                                <p className="whitespace-nowrap">
                                                    <span className="font-semibold">해제 예정: </span>
                                                    {user.inactive_until
                                                        ? formatDateTime(user.inactive_until)
                                                        : "미정"}
                                                </p>

                                                <p
                                                    className="mt-1 line-clamp-2 break-words"
                                                    title={user.inactive_reason || "미정"}
                                                >
                                                    <span className="font-semibold">사유: </span>
                                                    {user.inactive_reason || "미정"}
                                                </p>
                                            </div>
                                        )}
                                    </div>
                                </td>

                                <td className="px-4 py-3 align-middle">
                                    <span
                                        className={[
                                            "inline-flex rounded-full px-2.5 py-1 text-xs font-bold",
                                            user.role_type === "admin"
                                                ? "bg-violet-100 text-violet-700 dark:bg-violet-950/40 dark:text-violet-300"
                                                : "bg-gray-100 text-gray-600 dark:bg-gray-900 dark:text-gray-300",
                                        ].join(" ")}
                                    >
                                        {user.role_type === "admin" ? "관리자" : "일반회원"}
                                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle">
                                    <div className="flex justify-start">
                                        {canToggleUserActive(user) ? (
                                            <button
                                                type="button"
                                                disabled={processingId === user.id}
                                                onClick={() => onToggleUserActive(user)}
                                                className={`rounded-lg px-3 py-1.5 text-xs font-semibold text-white transition disabled:cursor-not-allowed disabled:opacity-50 ${
                                                    user.is_active
                                                        ? "bg-gray-500 hover:bg-gray-600"
                                                        : "bg-emerald-500 hover:bg-emerald-600"
                                                }`}
                                            >
                                                {processingId === user.id
                                                    ? "처리 중..."
                                                    : user.is_active
                                                        ? "비활성화"
                                                        : "활성화"}
                                            </button>
                                        ) : (
                                            <span
                                                className="rounded-lg bg-gray-100 px-3 py-1.5 text-xs font-semibold text-gray-400 dark:bg-gray-700 dark:text-gray-500"
                                                title={getToggleDisabledReason(user)}
                                            >
                                                변경 불가
                                            </span>
                                        )}
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}

            <Pagination
                currentPage={page}
                totalPages={totalPages}
                onPageChange={onPageChange}
            />
        </Card>
    );
}