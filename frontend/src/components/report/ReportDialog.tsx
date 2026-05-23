// src/components/report/ReportDialog.tsx
//
// 커뮤니티 신고 모달 컴포넌트
//
// 역할:
// - 게시글 신고
// - 댓글 신고
// - 사용자 닉네임 신고
// - 사용자 프로필 사진 신고
//
// 사용 방식:
// 1) 게시글 상세 상단의 "신고" 버튼
//    → targets에 게시글/닉네임/프로필사진 3개를 넘김
//
// 2) 댓글 오른쪽의 "신고" 버튼
//    → targets에 댓글 1개만 넘김
//
// 실제 신고 요청:
// - POST /api/content-reports
// - target_type, target_id, reason, detail 전송

import { useState, type FormEvent, useEffect } from "react";
import { toast } from "sonner";
import { AxiosError } from "axios";
import {
    createContentReport,
    type ContentReportReason,
    type ContentReportTargetType,
} from "@/domains/community/api/communityApi";
import {
    AlertDialog,
    AlertDialogCancel,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/shared/ui/alert-dialog";
import { Button } from "@/shared/ui/button";

// 신고 대상 1개를 표현하는 타입
//
// type:
// - post
// - comment
// - user_nickname
// - user_profile
//
// id:
// - post면 posts.id
// - comment면 comments.id
// - user_nickname/user_profile이면 users.id
//
// label:
// - 화면에 보여줄 이름
// - 예: 게시글, 댓글, 작성자 닉네임, 작성자 프로필 사진
interface ReportTargetOption {
    type: ContentReportTargetType;
    id: string;
    label: string;
}

interface ReportDialogProps {
    // 모달 열림 여부
    open: boolean;

    // 모달 열림/닫힘 변경 함수
    onOpenChange: (open: boolean) => void;

    // 신고 가능한 대상 목록
    //
    // 게시글 신고 버튼에서 열 때:
    // [
    //   { type: "post", id: post.id, label: "게시글" },
    //   { type: "user_nickname", id: post.user_id, label: "작성자 닉네임" },
    //   { type: "user_profile", id: post.user_id, label: "작성자 프로필 사진" }
    // ]
    //
    // 댓글 신고 버튼에서 열 때:
    // [
    //   { type: "comment", id: comment.id, label: "댓글" }
    // ]
    targets: ReportTargetOption[];

    // ── 추가 ──
    // 신고가 성공적으로 접수됐을 때 호출하는 콜백.
    //
    // 부모(Community)는 이 정보를 받아서:
    //   - reason === "inappropriate"면 백엔드가 그 글/댓글을 즉시 숨기므로
    //     프론트 화면에서도 해당 항목을 바로 제거한다.
    //   - 그 외 사유는 자동 숨김이 없으므로 화면을 건드리지 않는다.
    //
    // optional(?)로 둬서, 이 콜백을 안 넘기는 곳에서도 ReportDialog를 그대로 쓸 수 있게 한다.
    onReported?: (info: {
        targetType: ContentReportTargetType;
        targetId: string;
        reason: ContentReportReason;
    }) => void;
}

// 신고 사유 목록
// description: 사용자가 사유를 선택했을 때 버튼 아래에 보여줄 안내 문구.
//   특히 inappropriate는 1건만으로 즉시 임시 숨김되는 심각 사유라,
//   범위를 명확히 안내해서 오신고/어뷰징을 줄인다.
const REASONS: { value: ContentReportReason; label: string; description: string }[] = [
    {
        value: "abuse",
        label: "욕설/비방",
        description: "특정 사용자를 향한 욕설, 모욕, 인신공격, 비방.",
    },
    {
        value: "inappropriate",
        label: "부적절한 콘텐츠",
        description:
            "음란물·성적 콘텐츠, 폭력·잔혹 묘사, 특정 집단을 향한 혐오/차별 표현 등. 접수 시 검토 전까지 임시로 가려질 수 있습니다.",
    },
    {
        value: "spam",
        label: "광고/도배",
        description: "상업적 광고, 동일 내용 반복 게시, 무관한 링크 도배.",
    },
    {
        value: "other",
        label: "기타",
        description: "위 항목에 해당하지 않는 경우. 상세 내용에 구체적으로 적어주세요.",
    },
];

// 백엔드 에러 메시지 추출
function getReportErrorMessage(error: unknown): string {
    if (error instanceof AxiosError) {
        const message = error.response?.data;

        if (typeof message === "string" && message.trim()) {
            return message;
        }
    }

    return "신고 접수에 실패했습니다";
}

export default function ReportDialog({
                                         open,
                                         onOpenChange,
                                         targets,
                                         onReported, // ← 추가
                                     }: ReportDialogProps) {
    // 현재 선택된 신고 대상 index
    //
    // 게시글 상세 상단 신고:
    // 0 = 게시글
    // 1 = 작성자 닉네임
    // 2 = 작성자 프로필 사진
    //
    // 댓글 신고:
    // 0 = 댓글
    const [selectedTargetIndex, setSelectedTargetIndex] = useState(0);

    // 기본 신고 사유
    const [reason, setReason] = useState<ContentReportReason>("inappropriate");

    // 상세 신고 내용
    const [detail, setDetail] = useState("");

    // 중복 제출 방지
    const [isSubmitting, setIsSubmitting] = useState(false);

    // 현재 선택된 신고 대상
    const selectedTarget = targets[selectedTargetIndex];

    // 모달이 새로 열릴 때마다 첫 번째 신고 대상으로 초기화
    //
    // 예:
    // 게시글 신고 버튼 클릭 → 기본값 "게시글"
    // 댓글 신고 버튼 클릭 → 기본값 "댓글"
    useEffect(() => {
        if (open) {
            setSelectedTargetIndex(0);
            setReason("inappropriate");
            setDetail("");
        }
    }, [open, targets]);

    const resetForm = () => {
        setSelectedTargetIndex(0);
        setReason("inappropriate");
        setDetail("");
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (isSubmitting) return;

        if (!selectedTarget) {
            toast.error("신고 대상을 찾을 수 없습니다");
            return;
        }

        const trimmedDetail = detail.trim();

        // 상세 내용 필수 입력
        if (!trimmedDetail) {
            toast.error("신고 상세 내용을 입력해주세요");
            return;
        }

        setIsSubmitting(true);

        try {
            // 기존: await createContentReport(...) — 응답을 버리고 있었음
            // 변경: 응답을 받아서 onReported로 넘긴다. (target_type/target_id/reason 포함)
            const created = await createContentReport({
                target_type: selectedTarget.type,
                target_id: selectedTarget.id,
                reason,
                detail: detail.trim() || null,
            });

            toast.success("신고가 접수되었습니다");

            // ── 추가: 부모에게 신고 성공 알림 ──
            // 백엔드 응답값을 그대로 넘기는 게 가장 정확하다.
            // (혹시 응답 필드가 비어도 selectedTarget/reason으로 fallback)
            onReported?.({
                targetType: created?.target_type ?? selectedTarget.type,
                targetId: created?.target_id ?? selectedTarget.id,
                reason: created?.reason ?? reason,
            });

            resetForm();
            onOpenChange(false);
        } catch (error) {
            console.error("신고 접수 실패:", error);
            toast.error(getReportErrorMessage(error));
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <AlertDialog
            open={open}
            onOpenChange={(nextOpen) => {
                // 닫힐 때 입력값 초기화
                if (!nextOpen) {
                    resetForm();
                }

                onOpenChange(nextOpen);
            }}
        >
            <AlertDialogContent>
                <AlertDialogHeader>
                    <AlertDialogTitle>신고하기</AlertDialogTitle>

                    <AlertDialogDescription>
                        {targets.length > 1 ? (
                            <>
                                신고 대상을 선택하고 사유를 입력해주세요.
                                신고 내용은 운영자가 확인 후 처리합니다.
                            </>
                        ) : (
                            <>
                                신고 사유를 입력해주세요.
                                신고 내용은 운영자가 확인 후 처리합니다.
                            </>
                        )}
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <form onSubmit={handleSubmit} className="space-y-4">
                    {/* 신고 대상 선택
              ------------------------------------------------
              게시글 상세 상단 신고 버튼으로 들어온 경우:
              게시글 / 작성자 닉네임 / 작성자 프로필 사진 중 선택 가능.

              댓글 신고 버튼으로 들어온 경우:
              targets가 댓글 1개뿐이라 이 영역은 숨김.
          */}
                    {targets.length > 1 && (
                        <div className="space-y-2">
                            <p className="text-sm font-medium text-gray-700 dark:text-gray-200">
                                신고 대상
                            </p>

                            <div className="grid grid-cols-1 gap-2">
                                {targets.map((target, index) => (
                                    <button
                                        key={`${target.type}:${target.id}`}
                                        type="button"
                                        onClick={() => setSelectedTargetIndex(index)}
                                        className={`rounded-lg border px-3 py-2 text-left text-sm transition ${
                                            selectedTargetIndex === index
                                                ? "border-red-400 bg-red-50 text-red-600 dark:bg-red-950/30"
                                                : "border-gray-200 hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800"
                                        }`}
                                    >
                                        {target.label}
                                    </button>
                                ))}
                            </div>
                        </div>
                    )}

                    {/* 신고 사유 선택 */}
                    <div className="space-y-2">
                        <p className="text-sm font-medium text-gray-700 dark:text-gray-200">
                            신고 사유
                        </p>

                        <div className="grid grid-cols-2 gap-2">
                            {REASONS.map((item) => (
                                <button
                                    key={item.value}
                                    type="button"
                                    onClick={() => setReason(item.value)}
                                    className={`rounded-lg border px-3 py-2 text-sm transition ${
                                        reason === item.value
                                            ? "border-red-400 bg-red-50 text-red-600 dark:bg-red-950/30"
                                            : "border-gray-200 hover:bg-gray-50 dark:border-gray-700 dark:hover:bg-gray-800"
                                    }`}
                                >
                                    {item.label}
                                </button>
                            ))}
                        </div>

                        {/* 선택한 사유 안내.
                            현재 선택된 사유의 description을 보여준다.
                            특히 "부적절한 콘텐츠"가 어디까지인지 헷갈리지 않게 하고,
                            즉시 숨김될 수 있다는 점도 미리 알려준다. */}
                        <p className="rounded-md bg-gray-50 px-3 py-2 text-xs leading-5 text-gray-500 dark:bg-gray-800/60 dark:text-gray-400">
                            {REASONS.find((item) => item.value === reason)?.description}
                        </p>
                    </div>

                    {/* 상세 설명 */}
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-700 dark:text-gray-200">
                            상세 내용 <span className="text-red-500">*</span>
                        </label>

                        <textarea
                            value={detail}
                            onChange={(event) => setDetail(event.target.value)}
                            maxLength={500}
                            placeholder="신고 내용을 입력해주세요. 필수 입력입니다."
                            className="min-h-24 w-full resize-none rounded-lg border border-gray-200 bg-white p-3 text-sm outline-none focus:border-red-400 dark:border-gray-700 dark:bg-gray-900"
                        />

                        <p className="text-right text-xs text-gray-400">
                            {detail.length}/500
                        </p>
                    </div>

                    <AlertDialogFooter>
                        <AlertDialogCancel type="button" disabled={isSubmitting}>
                            취소
                        </AlertDialogCancel>

                        <Button
                            type="submit"
                            disabled={isSubmitting || !selectedTarget}
                            className="bg-red-500 text-white hover:bg-red-600"
                        >
                            {isSubmitting ? "접수 중..." : "신고하기"}
                        </Button>
                    </AlertDialogFooter>
                </form>
            </AlertDialogContent>
        </AlertDialog>
    );
}