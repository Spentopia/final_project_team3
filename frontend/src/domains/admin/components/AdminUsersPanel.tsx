// src/domains/admin/components/AdminUsersPanel.tsx
//
// 회원 관리 패널.
//
// 역할:
// - 이메일/닉네임 검색 입력
// - 회원 목록 테이블 표시
// - 회원 활성/비활성 처리 버튼 제공
//
// 데이터 조회/API 호출은 여기서 하지 않는다.
// 상위 AdminPage가 users, isUsersLoading, userKeyword, handler를 props로 내려준다.

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
                    <h3 className="text-lg font-bold">
                        회원 목록
                    </h3>

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
                <div className="overflow-hidden rounded-xl border border-border">
                    {/*
                        table-fixed + colgroup 사용 이유:
                        - 헤더와 본문 셀의 너비를 고정해서 칸이 흔들리지 않게 함
                        - 이메일처럼 긴 문자열이 있어도 다른 컬럼을 밀지 않게 함
                        - 관리자 테이블은 정렬감이 중요하므로 자동 너비보다 고정 너비가 안정적임
                      */}
                    <table className="w-full table-fixed text-sm">
                        <colgroup>
                            {/* 닉네임 */}
                            <col className="w-[18%]" />

                            {/* 이메일 */}
                            <col className="w-[30%]" />

                            {/* 가입일 */}
                            <col className="w-[18%]" />

                            {/* 상태 */}
                            <col className="w-[11%]" />

                            {/* 역할 */}
                            <col className="w-[10%]" />

                            {/* 관리 */}
                            <col className="w-[13%]" />
                        </colgroup>

                        <thead className="bg-[var(--surface-subtle)] text-left text-muted-foreground">
                        <tr>
                            <th className="px-4 py-3">닉네임</th>
                            <th className="px-4 py-3 pl-20">이메일</th>
                            <th className="px-4 py-3 pl-12">가입일</th>
                            <th className="px-4 py-3 pl-5">상태</th>
                            <th className="px-4 py-3">역할</th>
                            <th className="px-4 py-3 pr-15 text-right">
                                관리
                            </th>
                        </tr>
                        </thead>

                        <tbody>
                        {users.map((user) => (
                            <tr
                                key={user.id}
                                className="border-t border-border"
                            >
                                {/* 닉네임 */}
                                <td className="px-4 py-3 align-middle font-medium">
                                    <span
                                        className="block truncate"
                                        title={getTextValue(user, ["nickname"])}
                                    >
                                      {getTextValue(user, ["nickname"])}
                                    </span>
                                </td>

                                {/* 이메일 */}
                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span
                                        className="block truncate"
                                        title={getTextValue(user, ["email"])}
                                    >
                                      {getTextValue(user, ["email"])}
                                    </span>
                                </td>

                                {/* 가입일 */}
                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span className="block truncate">
                                      {formatDateTime(user.created_at)}
                                    </span>
                                </td>

                                {/* 상태 */}
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

                                {/* 역할 */}
                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span className="block truncate">
                                      {getTextValue(user, ["role_type"], "user")}
                                    </span>
                                </td>

                                {/* 관리 */}
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