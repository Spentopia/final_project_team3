// shared/api/systemStatusApi.ts
//
// 점검/장애 공지 API.
//
// public:
// - getSystemStatus()
//
// admin:
// - updateSystemStatus()
//
// 로그인 페이지와 로그인 후 전역 배너는 getSystemStatus를 사용한다.
// 관리자 대시보드는 updateSystemStatus를 사용한다.

import { apiClient } from "@/shared/api/client";

export type SystemStatusType = "maintenance" | "incident";

export type SystemStatusResponse = {
    enabled: boolean;
    blocking: boolean;
    status_type: SystemStatusType;
    title: string;
    message: string;
    starts_at: string | null;
    ends_at: string | null;
    updated_at: string;
};

export type UpdateSystemStatusRequest = {
    enabled: boolean;
    blocking?: boolean;
    status_type: SystemStatusType;
    title: string;
    message: string;
    starts_at?: string | null;
    ends_at?: string | null;
};

export const getSystemStatus = async (): Promise<SystemStatusResponse> => {
    const res = await apiClient.get<SystemStatusResponse>("/api/system/status");
    return res.data;
};

export const updateSystemStatus = async (
    payload: UpdateSystemStatusRequest,
): Promise<SystemStatusResponse> => {
    const res = await apiClient.patch<SystemStatusResponse>(
        "/api/admin/system/status",
        payload,
    );

    return res.data;
};