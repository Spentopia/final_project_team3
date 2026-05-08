// src/domains/admin/api/adminApi.ts
//
// 관리자 페이지 전용 API 모음
//
// 현재 포함 기능:
// 1. 신고 관리
//    - 신고 목록 조회
//    - 신고 처리 완료
//    - 신고 반려
//
// 2. 회원 관리
//    - 회원 목록 조회
//    - 회원 활성/비활성 변경
//
// 일반 사용자가 신고를 접수하는 API는
// communityApi.ts의 createContentReport()에서 처리한다.

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

// ─────────────────────────────────────────────
// 신고 관리 타입
// ─────────────────────────────────────────────

export interface AdminContentReportResponse {
    // 신고 ID
    id: string;

    // 신고한 사용자 ID
    reporter_id: string;

    // 신고 대상 타입
    target_type: ContentReportTargetType;

    // 신고 대상 ID
    //
    // target_type에 따라 의미가 달라짐:
    // - post          → posts.id
    // - comment       → comments.id
    // - user_nickname → users.id
    // - user_profile  → users.id
    target_id: string;

    // 신고 사유
    reason: ContentReportReason;

    // 신고 상세 내용
    detail: string | null;

    // 신고 처리 상태
    status: ContentReportStatus;

    // 신고 접수 시각
    created_at: string | null;

    // 관리자 처리 시각
    reviewed_at: string | null;

    // 처리한 관리자 ID
    reviewed_by: string | null;
}

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
    // false : 비활성/정지/탈퇴 처리된 회원으로 간주
    is_active: boolean;

    // 가입 시각
    created_at: string | null;

    // 마지막 수정 시각
    updated_at: string | null;
}

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
    status?: ContentReportStatus
): Promise<AdminContentReportResponse[]> {
    const res = await apiClient.get<AdminContentReportResponse[]>(
        "/api/admin/content-reports",{
            params: {
                status,
            }
        }
    );
    return res.data;
}

// 신고 처리 완료
//
// 관리자 페이지에서 "처리" 버튼을 눌렀을 때 호출.
// 백엔드는 status = "resolved",
// reviewed_at = 현재 시각,
// reviewed_by = 현재 관리자 user_id 로 업데이트.
export async function resolveAdminContentReport(
    id: string
): Promise<AdminContentReportResponse> {
    const res = await apiClient.patch<AdminContentReportResponse>(
        `/api/admin/content-reports/${id}/resolve`
    );

    return res.data;
}

// 신고 반려
//
// 관리자 페이지에서 "반려" 버튼을 눌렀을 때 호출.
// 백엔드는 status = "rejected",
// reviewed_at = 현재 시각,
// reviewed_by = 현재 관리자 user_id 로 업데이트.
export async function rejectAdminContentReport(
    id: string
): Promise<AdminContentReportResponse> {
    const res = await apiClient.patch<AdminContentReportResponse>(
        `/api/admin/content-reports/${id}/reject`
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
    keyword?: string
): Promise<AdminUserResponse[]> {
    const res = await apiClient.get<AdminUserResponse[]>("/api/admin/users", {
        params: {
            keyword: keyword || undefined,
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
    isActive: boolean
): Promise<AdminUserResponse> {
    const res = await apiClient.patch<AdminUserResponse>(
        `/api/admin/users/${id}/active`,
        {
            is_active: isActive,
        }
    );

    return res.data;
}