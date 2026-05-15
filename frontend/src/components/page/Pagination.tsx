// src/shared/components/Pagination.tsx
//
// 재사용 가능한 페이지네이션 컴포넌트.
//
// 사용처:
// - 커뮤니티 게시글 목록
// - 관리자 신고/회원 목록
//
// 핵심 동작:
// - 전체 페이지가 적을 때(<=7): 1, 2, 3, ..., n 모두 표시
// - 많을 때: [1] ... [4] [5] [6] ... [50] 같이 현재 페이지 주변만 표시
// - "이전"/"다음" 버튼 포함
// - 현재 페이지 클릭은 동작 무시 (불필요한 재요청 방지)
// - totalPages가 1 이하면 컴포넌트 자체를 숨김

import { ChevronLeft, ChevronRight } from "lucide-react";

type PaginationProps = {
    // 현재 페이지 (1-base)
    currentPage: number;

    // 전체 페이지 수
    totalPages: number;

    // 페이지 변경 핸들러
    onPageChange: (page: number) => void;

    // 현재 페이지 양옆에 몇 개씩 보여줄지 (기본 1)
    //
    // siblingCount=1이면:
    // [1] ... [4] [5] [6] ... [50]
    //          ↑   ↑   ↑
    //         좌1  현재 우1
    //
    // siblingCount=2이면:
    // [1] ... [3] [4] [5] [6] [7] ... [50]
    siblingCount?: number;
};

// 페이지 번호 배열 생성기
//
// 반환 예시:
// totalPages=5,  currentPage=3 → [1, 2, 3, 4, 5]
// totalPages=50, currentPage=1 → [1, 2, 3, "...", 50]
// totalPages=50, currentPage=25 → [1, "...", 24, 25, 26, "...", 50]
// totalPages=50, currentPage=49 → [1, "...", 48, 49, 50]
//
// 왜 "..."을 문자열로 두는가?
// - 렌더링 시 typeof로 구분하면 코드가 간결함
// - "..."을 별도 컴포넌트로 만들 수도 있지만 단순함 우선
function buildPageRange(
    currentPage: number,
    totalPages: number,
    siblingCount: number
): (number | "...")[] {
    // 양 끝 [1], [totalPages]는 항상 보여준다.
    // 중간 영역은 현재 페이지 ± siblingCount.
    //
    // 총 표시할 페이지 수:
    // 양 끝 2개 + 현재 1개 + 양옆 siblingCount*2 + ... 2개 = 5 + siblingCount*2
    //
    // 전체 페이지가 이 수보다 적거나 같으면 그냥 1~totalPages 다 표시.
    const totalPageNumbers = siblingCount * 2 + 5;

    if (totalPages <= totalPageNumbers) {
        return Array.from({ length: totalPages }, (_, i) => i + 1);
    }

    // 현재 페이지 양옆 인덱스 계산.
    // Math.max/min으로 1 미만, totalPages 초과 방지.
    const leftSibling = Math.max(currentPage - siblingCount, 1);
    const rightSibling = Math.min(currentPage + siblingCount, totalPages);

    // 양쪽 "..."을 보여줄지 결정.
    //
    // 예) currentPage=1, siblingCount=1
    //   leftSibling=1 → 좌측 "..." 안 보임
    //   rightSibling=2 → 우측 "..." 보임
    //
    // 예) currentPage=50, siblingCount=1
    //   leftSibling=49 → 좌측 "..." 보임
    //   rightSibling=50 → 우측 "..." 안 보임
    const showLeftDots = leftSibling > 2;
    const showRightDots = rightSibling < totalPages - 1;

    const pages: (number | "...")[] = [];

    // 좌측 끝
    pages.push(1);

    // 좌측 점 영역
    if (showLeftDots) {
        pages.push("...");
    } else {
        // 점이 안 보이는 경우 = 1과 leftSibling 사이가 좁으니 그냥 다 채운다.
        for (let i = 2; i < leftSibling; i++) {
            pages.push(i);
        }
    }

    // 현재 페이지 ± siblingCount
    for (let i = leftSibling; i <= rightSibling; i++) {
        // 1, totalPages는 양 끝에서 별도 처리하므로 여기서 제외
        if (i !== 1 && i !== totalPages) {
            pages.push(i);
        }
    }

    // 우측 점 영역
    if (showRightDots) {
        pages.push("...");
    } else {
        for (let i = rightSibling + 1; i < totalPages; i++) {
            pages.push(i);
        }
    }

    // 우측 끝
    pages.push(totalPages);

    return pages;
}

export default function Pagination({
                                       currentPage,
                                       totalPages,
                                       onPageChange,
                                       siblingCount = 1,
                                   }: PaginationProps) {
    // 전체 페이지가 1개 이하면 페이지네이션 자체를 숨긴다.
    if (totalPages <= 1) {
        return null;
    }

    const pages = buildPageRange(currentPage, totalPages, siblingCount);

    const canGoPrev = currentPage > 1;
    const canGoNext = currentPage < totalPages;

    return (
        <nav className="mt-5 flex items-center justify-center gap-1.5">
            {/* 이전 버튼 */}
            <button
                type="button"
                onClick={() => canGoPrev && onPageChange(currentPage - 1)}
                disabled={!canGoPrev}
                className="flex h-10 w-10 items-center justify-center rounded-lg border border-blue-100 bg-white text-blue-700 shadow-[0_6px_16px_rgba(37,99,235,0.08)] transition-colors hover:border-blue-200 hover:bg-[#f0f7ff] disabled:cursor-not-allowed disabled:opacity-40 dark:border-violet-300/25 dark:bg-[#111827] dark:text-violet-200 dark:shadow-[0_8px_18px_rgba(124,58,237,0.12)] dark:hover:border-violet-300/45 dark:hover:bg-[#2d1847]"
                aria-label="이전 페이지"
            >
                <ChevronLeft className="h-4 w-4" />
            </button>

            {/* 페이지 번호 */}
            {pages.map((page, index) => {
                // "..."은 클릭 불가, 표시만.
                // key는 index 기반으로 안전 (점이 두 번 들어가도 충돌 안 남)
                if (page === "...") {
                    return (
                        <span
                            key={`dots-${index}`}
                            className="flex h-10 w-10 items-center justify-center text-sm text-gray-400 dark:text-gray-500"
                        >
              ...
            </span>
                    );
                }

                const isActive = page === currentPage;

                return (
                    <button
                        key={page}
                        type="button"
                        // 현재 페이지를 다시 누르면 불필요한 재요청 발생.
                        // isActive면 onClick 자체를 호출하지 않는다.
                        onClick={() => !isActive && onPageChange(page)}
                        disabled={isActive}
                        className={`flex h-10 w-10 items-center justify-center rounded-lg border text-sm transition-colors ${
                            isActive
                                ? "border-[#2563eb] bg-[#3b82f6] font-medium text-white shadow-[0_10px_22px_rgba(37,99,235,0.18)] dark:border-violet-300/45 dark:bg-[#2d1847] dark:text-white dark:shadow-[0_10px_24px_rgba(124,58,237,0.28)]"
                                : "border-blue-100 bg-white text-blue-700 hover:border-blue-200 hover:bg-[#f0f7ff] dark:border-violet-300/25 dark:bg-[#111827] dark:text-violet-200 dark:hover:border-violet-300/45 dark:hover:bg-[#2d1847]"
                        }`}
                        aria-current={isActive ? "page" : undefined}
                    >
                        {page}
                    </button>
                );
            })}

            {/* 다음 버튼 */}
            <button
                type="button"
                onClick={() => canGoNext && onPageChange(currentPage + 1)}
                disabled={!canGoNext}
                className="flex h-10 w-10 items-center justify-center rounded-lg border border-blue-100 bg-white text-blue-700 shadow-[0_6px_16px_rgba(37,99,235,0.08)] transition-colors hover:border-blue-200 hover:bg-[#f0f7ff] disabled:cursor-not-allowed disabled:opacity-40 dark:border-violet-300/25 dark:bg-[#111827] dark:text-violet-200 dark:shadow-[0_8px_18px_rgba(124,58,237,0.12)] dark:hover:border-violet-300/45 dark:hover:bg-[#2d1847]"
                aria-label="다음 페이지"
            >
                <ChevronRight className="h-4 w-4" />
            </button>
        </nav>
    );
}
