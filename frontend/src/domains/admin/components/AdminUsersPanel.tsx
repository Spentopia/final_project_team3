// src/domains/admin/components/AdminUsersPanel.tsx
//
// 회원 관리 패널.
//
// 역할:
// - 이메일/닉네임 검색 입력
// - 회원 목록 테이블 표시
// - 회원 활성/비활성 처리 버튼 제공
//
// 이번 수정:
// - 테이블 구조는 유지
// - rounded table + hover row 적용
// - 컬럼 폭을 고정해서 헤더/내용 정렬감 개선
// - 긴 닉네임/이메일은 truncate 처리

import { Card } from "@/shared/ui/card";
import { Input } from "@/shared/ui/input";

import type { AdminUserResponse } from "@/domains/admin/api/adminApi";

import {
    formatDateTime,
    getTextValue,
} from "@/domains/admin/utils/adminViewUtils";

type AdminUsersPanelProps = {
    users: AdminUserResponse[];
    userKeyword: string;
    isUsersLoading: boolean;
    processingId: string | null;
    onUserKeywordChange: (keyword: string) => void;
    onToggleUserActive: (user: AdminUserResponse) => void;
};

export default function AdminUsersPanel({
                                            users,
                                            userKeyword,
                                            isUsersLoading,
                                            processingId,
                                            onUserKeywordChange,
                                            onToggleUserActive,
                                        }: AdminUsersPanelProps) {
    return (
        <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
            <div className="mb-4 flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
                <div>
                    <h3 className="text-lg font-bold">회원 목록</h3>
                    <p className="mt-1 text-sm text-muted-foreground">
                        이메일 또는 닉네임으로 회원을 검색합니다.
                    </p>
                </div>

                <Input
                    value={userKeyword}
                    onChange={(e) => onUserKeywordChange(e.target.value)}
                    placeholder="이메일 / 닉네임 검색"
                    className="w-full md:w-72"
                />
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
                    {/*
            회원 목록은 테이블 구조를 유지한다.

            핵심:
            - table-fixed + colgroup으로 컬럼 기준을 통일
            - 닉네임/이메일은 truncate 처리
            - row hover로 커뮤니티처럼 부드러운 느낌 추가
          */}
                    <table className="min-w-[960px] w-full table-fixed text-sm">
                        <colgroup>
                            <col className="w-[150px]" />
                            <col className="w-[300px]" />
                            <col className="w-[170px]" />
                            <col className="w-[100px]" />
                            <col className="w-[100px]" />
                            <col className="w-[140px]" />
                        </colgroup>

                        <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                        <tr>
                            <th className="px-4 py-3">닉네임</th>
                            <th className="px-4 py-3">이메일</th>
                            <th className="px-4 py-3">가입일</th>
                            <th className="px-4 py-3">상태</th>
                            <th className="px-4 py-3">역할</th>
                            <th className="px-4 py-3 text-right">관리</th>
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
                    <span
                        className={`inline-flex rounded-full px-2 py-1 text-xs font-bold ${
                            user.is_active
                                ? "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/40 dark:text-emerald-300"
                                : "bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300"
                        }`}
                    >
                      {user.is_active ? "활성" : "비활성"}
                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle text-muted-foreground">
                    <span className="block truncate">
                      {getTextValue(user, ["role_type"], "user")}
                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle">
                                    <div className="flex justify-end">
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
                                    </div>
                                </td>
                            </tr>
                        ))}
                        </tbody>
                    </table>
                </div>
            )}
        </Card>
    );
}