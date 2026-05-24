// src/domains/admin/components/AdminWithdrawnUsersPanel.tsx
//
// 탈퇴 회원 모니터링 패널.
//
// 역할:
// - deleted_at이 있는 탈퇴 회원 목록 표시
// - 탈퇴일 표시
// - 재가입 제한 상태 표시
// - 보관 만료 예정일 표시
// - 보관 만료까지 남은 일수 표시
//
// 주의:
// - 실제 완전 삭제 기능은 넣지 않는다.
// - 지금은 모니터링/시연용 운영 화면이다.
// - 정책 수치(30일/5년)는 문구에 박지 않고, 컬럼의 계산된 값으로 보여준다.

import { Card } from "@/shared/ui/card";

import type { AdminWithdrawnUserResponse } from "@/domains/admin/api/adminApi";
import { formatDateTime } from "@/domains/admin/utils/adminViewUtils";

type AdminWithdrawnUsersPanelProps = {
    users: AdminWithdrawnUserResponse[];
    keyword: string;
    isLoading: boolean;
    page: number;
    totalCount: number;
    pageSize: number;
    onKeywordChange: (value: string) => void;
    onPageChange: (page: number) => void;
};

export default function AdminWithdrawnUsersPanel({
                                                     users,
                                                     keyword,
                                                     isLoading,
                                                     page,
                                                     totalCount,
                                                     pageSize,
                                                     onKeywordChange,
                                                     onPageChange,
                                                 }: AdminWithdrawnUsersPanelProps) {
    const totalPages = Math.max(1, Math.ceil(totalCount / pageSize));

    return (
        <div className="space-y-5">
            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                        <h3 className="text-lg font-bold">탈퇴 회원 모니터링</h3>

                        {/* 정책 수치(30일/5년)는 문구에 넣지 않는다.
                            바뀌면 문구와 실제 로직이 어긋나기 쉽기 때문.
                            실제 기간은 아래 표의 계산된 컬럼 값으로 보여준다. */}
                        <p className="mt-1 text-sm text-muted-foreground">
                            탈퇴 회원의 재가입 제한 및 보관 만료 상태를 확인합니다.
                        </p>
                    </div>

                    <div className="w-full max-w-sm">
                        <label htmlFor="withdrawn-search" className="sr-only">
                            닉네임 또는 이메일 검색
                        </label>
                        <input
                            id="withdrawn-search"
                            name="withdrawnSearch"
                            type="search"
                            value={keyword}
                            onChange={(event) => onKeywordChange(event.target.value)}
                            placeholder="닉네임 또는 이메일 검색"
                            className="w-full rounded-xl border border-border bg-background px-3 py-2 text-sm outline-none transition focus:border-cyan-500"
                        />
                    </div>
                </div>
            </Card>

            <Card className="border-none bg-white/80 p-5 shadow-card dark:bg-gray-800/80">
                {isLoading && (
                    <div className="rounded-xl border border-border p-4 text-sm text-muted-foreground">
                        탈퇴 회원 목록을 불러오는 중입니다.
                    </div>
                )}

                {!isLoading && users.length === 0 && (
                    <div className="rounded-xl border border-dashed border-border p-4 text-sm text-muted-foreground">
                        표시할 탈퇴 회원이 없습니다.
                    </div>
                )}

                {!isLoading && users.length > 0 && (
                    <div className="overflow-hidden rounded-2xl border border-border bg-white/60 dark:bg-gray-900/20">
                        <table className="w-full table-fixed text-sm">
                            <colgroup>
                                <col className="w-[220px]" />
                                <col className="w-[180px]" />
                                <col className="w-[200px]" />
                                <col className="w-[160px]" />
                                <col className="w-[150px]" />
                            </colgroup>

                            <thead className="bg-[var(--surface-subtle)] text-left text-sm font-bold text-muted-foreground">
                            <tr>
                                <th className="px-4 py-3">회원</th>
                                <th className="px-4 py-3">탈퇴일</th>
                                <th className="px-4 py-3">재가입 제한</th>
                                <th className="px-4 py-3">보관 만료 예정일</th>
                                {/* 일수는 숫자라 우측 정렬이 읽기 편하다.
                                    헤더와 셀을 둘 다 text-right로 맞춘다. */}
                                <th className="px-4 py-3 text-right">남은 보관 기간</th>
                            </tr>
                            </thead>

                            <tbody>
                            {users.map((user) => (
                                <tr
                                    key={user.id}
                                    className="border-t border-border transition-colors hover:bg-[var(--surface-subtle)]/70"
                                >
                                    <td className="px-4 py-3 align-middle">
                                        <p className="truncate font-semibold">
                                            {user.nickname || "닉네임 없음"}
                                        </p>

                                        <p className="mt-1 truncate text-xs text-muted-foreground">
                                            {user.email || "이메일 없음"}
                                        </p>
                                    </td>

                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                                        {formatDateTime(user.deleted_at)}
                                    </td>

                                    {/* 재가입 제한:
                                        배지(알약)를 떼고 상태 텍스트 + 재가입 가능일로 표현.
                                        제한 중일 때만 주황 글씨로 한 톤 강조.
                                        "제한 중"이라는 상태는 그 아래 날짜로 이미 설명되므로
                                        배지 배경 색은 노이즈에 가깝다. */}
                                    <td className="px-4 py-3 align-middle">
                                        <p
                                            className={
                                                user.is_rejoin_cooldown_active
                                                    ? "font-semibold text-orange-600 dark:text-orange-300"
                                                    : "font-semibold text-muted-foreground"
                                            }
                                        >
                                            {user.is_rejoin_cooldown_active
                                                ? "제한 중"
                                                : "재가입 가능"}
                                        </p>

                                        {user.rejoin_cooldown_until && (
                                            <p className="mt-0.5 text-xs text-muted-foreground">
                                                ~ {formatDateTime(user.rejoin_cooldown_until)}
                                            </p>
                                        )}
                                    </td>

                                    <td className="px-4 py-3 align-middle text-muted-foreground">
                                        {formatDateTime(user.retention_expires_at)}
                                    </td>

                                    {/* 셀도 헤더와 동일하게 우측 정렬 */}
                                    <td className="px-4 py-3 align-middle text-right">
                                        <span className="font-semibold">
                                            {user.retention_days_left.toLocaleString()}일
                                        </span>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>
                )}

                <div className="mt-4 flex items-center justify-between text-sm text-muted-foreground">
                    <p>
                        총 <span className="font-bold text-foreground">{totalCount}</span>명
                    </p>

                    <div className="flex items-center gap-2">
                        <button
                            type="button"
                            disabled={page <= 1}
                            onClick={() => onPageChange(page - 1)}
                            className="rounded-lg border border-border px-3 py-2 font-semibold transition hover:bg-[var(--surface-subtle)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            이전
                        </button>

                        <span>
                            {page} / {totalPages}
                        </span>

                        <button
                            type="button"
                            disabled={page >= totalPages}
                            onClick={() => onPageChange(page + 1)}
                            className="rounded-lg border border-border px-3 py-2 font-semibold transition hover:bg-[var(--surface-subtle)] disabled:cursor-not-allowed disabled:opacity-50"
                        >
                            다음
                        </button>
                    </div>
                </div>
            </Card>
        </div>
    );
}