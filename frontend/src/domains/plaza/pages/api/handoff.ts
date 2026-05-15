// domains/plaza/api/handoff.ts
// ─────────────────────────────────────────────────────────────
// 게임 로그인 코드 발급 API 함수
//
// 웹에서 "게임 로그인 코드 생성" 버튼 클릭 시 호출한다.
//
// 기존 handoff 구조를 그대로 재사용하지만,
// 이제 이 값은 유니티 실행 인자로 자동 전달하지 않는다.
//
// 현재 정책:
// - 백엔드는 8자리 게임 로그인 코드를 발급한다.
// - 유효시간은 60초다.
// - 서버에는 코드 원문이 아니라 hash만 저장된다.
// - Unity는 사용자가 직접 입력한 코드를 /auth/handoff/exchange로 교환한다.
//
// 왜 기존 /auth/handoff를 재사용하나?
// - 이미 로그인된 웹 사용자 기준으로 1회용 토큰을 발급하는 구조가 있음
// - 이미 Unity가 /auth/handoff/exchange로 access/refresh를 받는 구조가 있음
// - Steam 실행 인자 전달만 불안정했을 뿐, 서버의 handoff 교환 구조는 재사용 가능함
//
// 주의:
// - apiClient interceptor가 Authorization 헤더를 자동으로 붙여준다.
// - 이 요청은 보호 라우트(/auth/handoff)라 로그인 상태여야 한다.
// ─────────────────────────────────────────────────────────────

import { apiClient } from "@/shared/api/client";

export interface HandoffResponse {
  // 백엔드 필드명은 기존 호환을 위해 handoff_token 그대로 둔다.
  // 화면에서는 "게임 로그인 코드"라고 표시하면 된다.
  handoff_token: string;

  // 코드 유효시간. 현재 백엔드 기준 60초.
  expires_in: number;
}

// 게임 로그인 코드 발급 요청.
//
// 웹에서 코드 생성 버튼 클릭 시 호출한다.
// 서버는 로그인된 유저 기준으로 1회용 게임 로그인 코드를 발급한다.
export const requestHandoffToken = async (): Promise<HandoffResponse> => {
  const res = await apiClient.post<HandoffResponse>("/auth/handoff", {
    target_service: "unity",
  });

  return res.data;
};