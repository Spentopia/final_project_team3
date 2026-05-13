// src/domains/admin/components/AdminUsersPanel.tsx
//
// 회원 관리 패널.
//
// 역할:
// - 이메일/닉네임 검색 입력
// - 회원 목록 테이블 표시
// - 회원 활성/비활성 처리 버튼 제공
// - 페이지네이션 (공통 Pagination 컴포넌트 사용)
//
// 이번 수정:
// - 페이지네이션 props 추가 (page, totalCount, pageSize, onPageChange)
// - 검색 input 옆에 총 회원 수 표시
// - 테이블 하단에 Pagination 컴포넌트 추가
// - totalPages는 totalCount / pageSize로 계산
//
// 기존에 유지된 것:
// - table-fixed + colgroup
// - 닉네임/이메일 truncate
// - row hover
// - 비활성/탈퇴 상태 뱃지 + 사유/해제 예정일 표시
// - 운영자/탈퇴자 변경 불가 처리

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

    // ─────────────────────────────────────────────
    // 페이지네이션 props
    // ─────────────────────────────────────────────
    //
    // page       : 현재 페이지 번호 (1-base)
    // totalCount : 검색 조건에 맞는 전체 회원 수 (서버 응답의 total_count)
    // pageSize   : 페이지당 회원 수 (보통 AdminPage에서 20으로 고정)
    // onPageChange: 페이지 클릭 시 상위(AdminPage)에서 호출되는 핸들러
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
    // 탈퇴자는 활성/비활성 변경 불가.
    if (user.deleted_at) {
        return false;
    }

    // 운영자 계정은 활성/비활성 변경 불가.
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
    // ─────────────────────────────────────────────
    // 전체 페이지 수 계산
    // ─────────────────────────────────────────────
    //
    // pageSize=0 같은 잘못된 값이 들어와도 안전하게 1 이상으로 강제.
    // Pagination 컴포넌트는 totalPages <= 1이면 자동으로 숨겨주므로
    // 회원이 0명이거나 한 페이지에 다 들어가는 경우는 별도 처리 필요 없음.
    const totalPages = Math.max(1, Math.ceil(totalCount / Math.max(1, pageSize)));

    // ─────────────────────────────────────────────
    // 현재 페이지 표시 범위 계산
    // ─────────────────────────────────────────────
    //
    // "153명 중 21-40번째" 형태로 보여주기 위해 시작/끝 인덱스 계산.
    // totalCount=0이면 0으로 표시.
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

                {/* 결과 카운트 + 검색 input */}
                {/* 검색 + 결과 수 */}
                <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
                    <div className="relative">
                        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                        <Input
                            placeholder="닉네임/이메일 검색"
                            className="h-11 w-64 pl-10 text-base"
                            value={userKeyword}
                            onChange={(e) => onUserKeywordChange(e.target.value)}
                        />
                    </div>

                    <span className="ml-auto text-xs text-muted-foreground">
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

                                <td className="min-w-[220px] px-4 py-3 align-middle">
                                    <span
                                        className={`inline-flex rounded-full px-2 py-1 text-xs font-bold ${getUserStatusClassName(user)}`}
                                        title={user.deleted_at ? `탈퇴일: ${formatDateTime(user.deleted_at)}` : undefined}
                                    >
                                        {getUserStatusLabel(user)}
                                    </span>

                                    {!user.is_active && !user.deleted_at && (
                                        <div className="mt-1 text-xs leading-relaxed text-muted-foreground">
                                            <p className="whitespace-nowrap">
                                                {user.inactive_until
                                                    ? `해제 예정: ${formatDateTime(user.inactive_until)}`
                                                    : "해제 예정: 미정"}
                                            </p>

                                            {user.inactive_reason && (
                                                <p
                                                    className="mt-0.5 max-w-[220px] truncate"
                                                    title={user.inactive_reason}
                                                >
                                                    사유: {user.inactive_reason}
                                                </p>
                                            )}
                                        </div>
                                    )}
                                </td>

                                <td className="px-4 py-3 align-middle text-muted-foreground">
                                    <span className="block truncate">
                                        {getTextValue(user, ["role_type"], "user")}
                                    </span>
                                </td>

                                <td className="px-4 py-3 align-middle">
                                    <div className="flex justify-end">
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

            {/* ─────────────────────────────────────────────
                페이지네이션
                ─────────────────────────────────────────────

                공통 Pagination 컴포넌트 사용.
                내부적으로 totalPages <= 1이면 null을 반환하므로
                여기서 별도 조건 분기 없이 그냥 렌더링한다.

                onPageChange는 상위 AdminPage의 setUserPage로 연결되어 있고,
                userPage가 바뀌면 AdminPage의 fetchUsers useEffect가 다시 실행되어
                새 페이지 데이터를 가져온다. */}
            <Pagination
                currentPage={page}
                totalPages={totalPages}
                onPageChange={onPageChange}
            />
        </Card>
    );
}