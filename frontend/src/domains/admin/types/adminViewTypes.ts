// src/domains/admin/types/adminViewTypes.ts
//
// 관리자 화면 전용 타입 모음.
//
// 왜 분리하나?
// - AdminPage, AdminSidebar, AdminDashboard 등 여러 컴포넌트가
//   같은 탭 타입과 필터 타입을 공유한다.
// - 각 파일마다 타입을 중복 선언하면 나중에 탭이 추가될 때 수정 누락이 생긴다.
// - 그래서 관리자 화면에서 공통으로 쓰는 UI 타입은 여기서 관리한다.

import type {ContentReportStatus} from "@/domains/admin/api/adminApi.ts";

// 관리자 좌측 사이드바에서 선택 가능한 실제 구현 탭.
//
// 현재 1차 구현:
// - dashboard: 대시보드
// - reports: 신고 관리
// - users: 회원 관리
//
// 공지사항 / 콘테스트 관리는 사이드바에 "준비중"으로만 표시하고,
// 실제 탭 타입에는 아직 넣지 않는다.
// 이유:
// - 타입에 넣으면 해당 탭 화면도 반드시 분기 처리해야 해서,
//   미구현 상태에서 불필요한 코드가 늘어난다.
export type AdminTab = "dashboard" | "reports" | "users" | "notices" | "contests";

// 신고 상태 필터 타입.
//
// all은 DB status 값이 아니라 프론트 필터용 값.
// 나머지는 백엔드/DB에서 사용하는 실제 신고 상태값이다.
export type ReportStatusFilter = ContentReportStatus | "all";