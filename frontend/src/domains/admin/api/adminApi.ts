// src/domains/admin/api/adminApi.ts
//
// 관리자 API 호출 모음.
//
// 현재 포함 기능:
// 1. 신고 관리
// 2. 회원 관리
// 3. 공지사항 관리
//
// 주의:
// - 백엔드 신고 상세 컬럼명은 description이 아니라 detail이다.
// - 공지사항은 posts 테이블의 post_type = 'notice'를 사용한다.

import {apiClient} from "@/shared/api/client.ts";

// ─────────────────────────────────────────────
// 공통 타입
// ─────────────────────────────────────────────

// 신고 처리 상태
//
// pending  : 처리 대기
// resolved : 처리 완료
// rejected : 반려
export type ContentReportStatus = "pending" | "resolved" | "rejected";

// 신고 대상 타입
//
// post          : 게시글
// comment       : 댓글
// user_nickname : 사용자 닉네임
// user_profile  : 사용자 프로필 사진
//
// 주의:
// user_nickename 아님.
// 백엔드/DB 제약조건과 맞추려면 user_nickname 이어야 함.
export type ContentReportTargetType =
    | "post"
    | "comment"
    | "user_nickname"
    | "user_profile";

// 신고 사유
//
// abuse         : 욕설/비방
// inappropriate : 부적절한 내용/이미지
// spam          : 광고/도배
// other         : 기타
export type ContentReportReason =
    | "abuse"
    | "inappropriate"
    | "spam"
    | "other";

export type AdminReportSortBy = "created_at" | "reviewed_at";
export type AdminReportSortOrder = "asc" | "desc";

// ─────────────────────────────────────────────
// 페이지네이션 공통 응답 타입
// ─────────────────────────────────────────────
//
// 백엔드 AdminContentReportListResponse / AdminUserListResponse와 매칭.
// 커뮤니티 CommunityPostListResponse와 필드명 통일 (total_count, items).

export interface AdminContentReportListResponse {
    items: AdminContentReportResponse[];
    total_count: number;
    page: number;
    page_size: number;
}

export interface AdminUserListResponse {
    items: AdminUserResponse[];
    total_count: number;
    page: number;
    page_size: number;
}

// 관리자 신고 목록 조회 파라미터.
//
// 백엔드 ContentReportQuery와 매칭된다.
export type ListAdminContentReportsParams = {
    status?: ContentReportStatus;
    target_type?: ContentReportTargetType;
    reason?: ContentReportReason;
    keyword?: string;

    // 신고일 날짜 범위 필터.
    //
    // 프론트 date input의 값인 YYYY-MM-DD를 그대로 보낸다.
    // 백엔드에서 하루 시작/끝 시간으로 보정한다.
    start_date?: string;
    end_date?: string;

    // 정렬 기준/방향.
    //
    // created_at  : 신고일
    // reviewed_at : 처리일
    //
    // desc : 최신순
    // asc  : 오래된순
    sort_by?: AdminReportSortBy;
    sort_order?: AdminReportSortOrder;

    page?: number;
    page_size?: number;
};

// ─────────────────────────────────────────────
// 회원 관리 API
// ─────────────────────────────────────────────

export type ListAdminUsersParams = {
    keyword?: string;
    page?: number;
    page_size?: number;
};

// 관리자 신고 응답 타입
//
// 백엔드 AdminContentReportResponse와 맞춰야 한다.
//
// 목록에서는:
// - reporter_nickname을 메인으로 표시
// - reporter_email이 있으면 보조 표시
// - 이메일이 없으면 reporter_id 일부 표시
//
// 상세 모달에서는:
// - reporter_nickname
// - reporter_email
// - reporter_id 전체
// 를 분리해서 보여준다.
export type AdminContentReportResponse = {
    // 신고 ID
    id: string;

    // 신고자 ID
    reporter_id: string;

    // 신고자 닉네임
    // 탈퇴 유저이거나 users row join 실패 시 null 가능
    reporter_nickname: string | null;

    // 신고자 이메일
    // 소셜 계정 정책에 따라 null 가능
    reporter_email: string | null;

    // 신고 대상 타입
    target_type: ContentReportTargetType;

    // 신고 대상 ID
    target_id: string;

    // 신고 사유
    reason: ContentReportReason;

    // 신고 상세 내용
    // DB 실제 컬럼명: detail
    // description 아님
    detail: string | null;

    // 신고 처리 상태
    status: ContentReportStatus;

    // 신고 접수 시각
    created_at: string | null;

    // 관리자 처리 시각
    reviewed_at: string | null;

    // 처리한 관리자 ID
    reviewed_by: string | null;

    // 같은 대상의 누적 신고 횟수.
    target_report_count: number;
};

// 관리자 감사 로그 응답 타입.
//
// 백엔드 AdminAuditLogResponse와 매칭된다.
export type AdminAuditLogResponse = {
    id: string;

    // 작업한 관리자 ID.
    admin_id: string;

    // 작업 종류.
    //
    // 예:
    // content_report_resolved
    // content_report_rejected
    action: string;

    // 작업 대상 타입.
    //
    // 이번 기능에서는 content_report.
    target_type: string;

    // 작업 대상 ID.
    target_id: string;

    // 변경 전 상태.
    before_status: string | null;

    // 변경 후 상태.
    after_status: string | null;

    // 추가 메타데이터.
    metadata: Record<string, unknown>;

    // 로그 생성 시각.
    created_at: string | null;
};

export type UpdateAdminUserActiveRequest = {
    is_active: boolean;

    // 비활성화할 때만 사용.
    // 활성화할 때는 보내지 않아도 된다.
    reason?: string | null;

    // 비활성 해제 예정일.
    // ISO 문자열로 보낸다.
    // null이면 무기한 또는 미정.
    inactive_until?: string | null;
};

// ─────────────────────────────────────────────
// 회원 관리 타입
// ─────────────────────────────────────────────

export interface AdminUserResponse {
    // 사용자 ID
    id: string;

    // 이메일
    email: string | null;

    // 닉네임
    nickname: string | null;

    // 전화번호
    phone: string | null;

    // 프로필 이미지 URL 또는 path
    profile_image: string | null;

    // 로그인 제공자
    // 예: google, kakao, wallet, email 등
    login_provider: string | null;

    // 지갑 주소
    wallet_address: string | null;

    // 사용자 역할
    // 현재 프론트에서는 조회/표시용으로만 사용
    // 관리자 페이지에서 role 변경 API는 만들지 않음
    role_type: "user" | "admin" | string;

    // 프로필 완성 여부
    profile_completed: boolean;

    // 활성 상태
    //
    // true  : 정상 활성 회원
    // false : 운영자가 비활성 처리한 회원
    //
    // 단, deleted_at이 있으면 "비활성"이 아니라 "탈퇴"로 표시한다.
    is_active: boolean;

    // 탈퇴 시각.
    //
    // null이면 미탈퇴 회원.
    // 문자열이면 탈퇴 회원.
    deleted_at: string | null;

    // 비활성 사유.
// is_active=false일 때 표시한다.
    inactive_reason: string | null;

// 비활성 처리 시각.
    inactive_at: string | null;

// 비활성 해제 예정일.
// null이면 무기한 또는 미정.
    inactive_until: string | null;

// 비활성 처리한 관리자 ID.
    inactive_by: string | null;

    // 가입 시각
    created_at: string | null;

    // 마지막 수정 시각
    updated_at: string | null;
}

// ─────────────────────────────────────────────
// 공지사항 관리 타입
// ─────────────────────────────────────────────

export type AdminNoticeResponse = {
    id: string;
    user_id: string;
    title: string;
    content: string;
    post_type: "notice" | string;
    view_count: number;
    is_deleted: boolean;
    deleted_at: string | null;
    created_at: string | null;
    updated_at: string | null;
};

export type CreateAdminNoticeRequest = {
    title: string;
    content: string;
};

export type UpdateAdminNoticeRequest = {
    title?: string;
    content?: string;
};

// ─────────────────────────────────────────────
// 아바타 콘테스트 관리 타입
// ─────────────────────────────────────────────

export type AdminContestStatus = "upcoming" | "active" | "ended";

export type AdminContestResponse = {
    id: string;
    title: string;
    description: string | null;
    start_date: string;
    end_date: string;
    status: AdminContestStatus | string;
    reward_description: string | null;
    created_at: string | null;
};

export type CreateAdminContestRequest = {
    title: string;
    description?: string | null;
    start_date: string;
    end_date: string;
    status?: AdminContestStatus;
    reward_description?: string | null;
};

export type UpdateAdminContestRequest = {
    title?: string;
    description?: string | null;
    start_date?: string;
    end_date?: string;
    status?: AdminContestStatus;
    reward_description?: string | null;
};


// ─────────────────────────────────────────────
// 신고 관리 API
// ─────────────────────────────────────────────

// 관리자 신고 목록 조회
//
// status를 넘기면 해당 상태만 조회.
// status를 생략하면 전체 신고 목록 조회.
//
// 예:
// listAdminContentReports()
// listAdminContentReports("pending")
export async function listAdminContentReports(
    params: ListAdminContentReportsParams = {}
): Promise<AdminContentReportListResponse> {
    const res = await apiClient.get<AdminContentReportListResponse>(
        "/api/admin/content-reports",
        {
            params: {
                status: params.status,
                target_type: params.target_type,
                reason: params.reason,
                keyword: params.keyword || undefined,

                // 날짜 범위 필터
                start_date: params.start_date || undefined,
                end_date: params.end_date || undefined,

                // 정렬
                sort_by: params.sort_by,
                sort_order: params.sort_order,

                // 페이지네이션
                page: params.page,
                page_size: params.page_size,
            },
        }
    );

    return res.data;
}

// 신고 처리 완료
export const resolveAdminContentReport = async (
    reportId: string
): Promise<AdminContentReportResponse> => {
    const res = await apiClient.patch(
        `/api/admin/content-reports/${reportId}/resolve`
    );

    return res.data;
};

// 신고 반려
export const rejectAdminContentReport = async (
    reportId: string
): Promise<AdminContentReportResponse> => {
    const res = await apiClient.patch(
        `/api/admin/content-reports/${reportId}/reject`
    );

    return res.data;
};

// 특정 신고의 관리자 감사 로그 조회.
//
// GET /api/admin/content-reports/{reportId}/audit-logs
export async function listAdminContentReportAuditLogs(
    reportId: string
): Promise<AdminAuditLogResponse[]> {
    const res = await apiClient.get<AdminAuditLogResponse[]>(
        `/api/admin/content-reports/${reportId}/audit-logs`
    );

    return res.data;
}

// ─────────────────────────────────────────────
// 회원 관리 API
// ─────────────────────────────────────────────

// 관리자 회원 목록 조회
//
// keyword:
// - 닉네임 또는 이메일 검색어
// - 생략하면 전체 회원 조회
//
// 예:
// listAdminUsers()
// listAdminUsers("test@example.com")
// listAdminUsers("은영")
export async function listAdminUsers(
    params: ListAdminUsersParams = {}
): Promise<AdminUserListResponse> {
    const res = await apiClient.get<AdminUserListResponse>("/api/admin/users", {
        params: {
            keyword: params.keyword || undefined,
            page: params.page,
            page_size: params.page_size,
        },
    });
    return res.data;
}

// 회원 활성/비활성 변경
//
// isActive = true
// → 회원 활성화
//
// isActive = false
// → 회원 비활성화/정지 처리
//
// role_type 변경은 여기서 하지 않음.
// 관리자 권한 부여/해제는 실수 위험이 커서 DB에서 직접 관리하는 구조 추천.
export async function updateAdminUserActive(
    id: string,
    payload: UpdateAdminUserActiveRequest
): Promise<AdminUserResponse> {
    const res = await apiClient.patch<AdminUserResponse>(
        `/api/admin/users/${id}/active`,
        payload
    );

    return res.data;
}

// ─────────────────────────────────────────────
// 공지사항 관리 API
// ─────────────────────────────────────────────

export const listAdminNotices = async (): Promise<AdminNoticeResponse[]> => {
    const res = await apiClient.get("/api/admin/notices");

    return res.data;
};

export const createAdminNotice = async (
    payload: CreateAdminNoticeRequest
): Promise<AdminNoticeResponse> => {
    const res = await apiClient.post("/api/admin/notices", payload);

    return res.data;
};

export const updateAdminNotice = async (
    noticeId: string,
    payload: UpdateAdminNoticeRequest
): Promise<AdminNoticeResponse> => {
    const res = await apiClient.patch(`/api/admin/notices/${noticeId}`, payload);

    return res.data;
};

export const deleteAdminNotice = async (
    noticeId: string
): Promise<AdminNoticeResponse> => {
    const res = await apiClient.delete(`/api/admin/notices/${noticeId}`);

    return res.data;
};

// ─────────────────────────────────────────────
// 아바타 콘테스트 관리 API
// ─────────────────────────────────────────────
export const listAdminContests = async (): Promise<AdminContestResponse[]> => {
    const res = await apiClient.get<AdminContestResponse[]>("/api/admin/contests");

    return res.data;
};

export const createAdminContest = async (
    payload: CreateAdminContestRequest
): Promise<AdminContestResponse> => {
    const res = await apiClient.post("/api/admin/contests", payload);
    return res.data;
};

export const updateAdminContest = async (
    contestId: string,
    payload: UpdateAdminContestRequest
): Promise<AdminContestResponse> => {
    const res = await apiClient.patch(`/api/admin/contests/${contestId}`, payload);
    return res.data;
};

export const updateAdminContestStatus = async (
    contestId: string,
    status: AdminContestStatus
): Promise<AdminContestResponse> => {
    const res = await apiClient.patch(`/api/admin/contests/${contestId}/status`, {
        status,
    });

    return res.data;
};
