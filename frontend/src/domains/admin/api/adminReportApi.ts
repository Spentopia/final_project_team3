// src/domains/admin/api/adminReportApi.ts
//
// 관리자 신고 관리 API
//
// 역할:
// - 신고 목록 조회
// - 신고 처리 완료
// - 신고 반려
//
// 이 파일은 관리자 페이지에서 사용한다.
// 일반 사용자의 신고 접수 API는 communityApi.ts에 있다.

import {apiClient} from "@/shared/api/client.ts";
import type {
    ContentReportReason,
    ContentReportTargetType,
    ContentReportStatus
} from "@/domains/community/api/communityApi.ts";

export interface AdminContentReportResponse {
    id: string;
    reporter_id: string;
    target_type: ContentReportTargetType;
    target_id: string;
    reason: ContentReportReason;
    detail: string | null;
    status: ContentReportStatus;
    created_at: string | null;
    reviewed_at: string | null;
    reviewed_by: string | null;
}

// 관리자 신고 목록 조회
//
// status를 넘기면 해당 상태만 조회.
// status 생략 시 전체 조회.
export async function listAdminContentReports(
    status?: ContentReportStatus
): Promise<AdminContentReportResponse[]> {
    const res = await apiClient.get<AdminContentReportResponse[]>(
        "/api/admin/content-reports",
        {
            params: {
                status,
            }
        }
    );

    return res.data;
}

// 신고 처리 완료
export async function resolveAdminContentReport(
    id: string
): Promise<AdminContentReportResponse> {
    const res = await apiClient.patch<AdminContentReportResponse>(
        `/api/admin/content-reports/${id}/resolve`
    );

    return res.data;
}

// 신고 반려
export async function rejectAdminContentReport(
    id: string
): Promise<AdminContentReportResponse> {
    const res = await apiClient.patch<AdminContentReportResponse>(
        `/api/admin/content-reports/${id}/reject`
    );

    return res.data;
}