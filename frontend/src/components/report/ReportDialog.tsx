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
// 이 컴포넌트는 신고 대상 타입과 ID만 props로 받아서,
// POST /api/content-reports로 신고 접수 요청을 보낸다.

import { useState, type FormEvent } from "react";
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

interface ReportDialogProps {
    // 모달 열림 여부
    open: boolean;

    // 모달 열림/닫힘 상태 변경 함수
    onOpenChange: (open: boolean) => void;

    // 신고 대상 타입
    targetType: ContentReportTargetType;

    // 신고 대상 ID
    targetId: string;

    // 화면에 보여줄 신고 대상 이름
    // 예: "게시글", "댓글", "닉네임", "프로필 사진"
    targetLabel: string;
}

// 신고 사유 목록
const REASONS: { value: ContentReportReason; label: string }[] = [
    { value: "abuse", label: "욕설/비방" },
    { value: "inappropriate", label: "부적절한 내용" },
    { value: "spam", label: "광고/도배" },
    { value: "other", label: "기타" },
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
                                         targetType,
                                         targetId,
                                         targetLabel,
                                     }: ReportDialogProps) {
    // 기본 신고 사유
    const [reason, setReason] =
        useState<ContentReportReason>("inappropriate");

    // 상세 신고 내용
    const [detail, setDetail] = useState("");

    // 중복 제출 방지용
    const [isSubmitting, setIsSubmitting] = useState(false);

    const resetForm = () => {
        setReason("inappropriate");
        setDetail("");
    };

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();

        if (isSubmitting) return;

        setIsSubmitting(true);

        try {
            await createContentReport({
                target_type: targetType,
                target_id: targetId,
                reason,
                detail: detail.trim() || null,
            });

            toast.success("신고가 접수되었습니다");

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
                        {targetLabel}에 대한 신고 사유를 선택해주세요.
                        신고 내용은 운영자가 확인 후 처리합니다.
                    </AlertDialogDescription>
                </AlertDialogHeader>

                <form onSubmit={handleSubmit} className="space-y-4">
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
                    </div>

                    {/* 상세 설명 */}
                    <div className="space-y-2">
                        <label className="text-sm font-medium text-gray-700 dark:text-gray-200">
                            상세 내용
                        </label>

                        <textarea
                            value={detail}
                            onChange={(event) => setDetail(event.target.value)}
                            maxLength={500}
                            placeholder="신고 내용을 입력해주세요. 선택사항입니다."
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
                            disabled={isSubmitting}
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