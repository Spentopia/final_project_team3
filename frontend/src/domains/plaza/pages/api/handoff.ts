// domains/plaza/api/handoff.ts
// ─────────────────────────────────────────────────────────────
// handoff token 발급 API 함수
//
// 웹에서 "게임 시작" 또는 "게임 실행" 버튼 클릭 시 호출.
// 백엔드가 30초짜리 1회용 handoff token을 발급한다.
// 프론트는 이 토큰을 브라우저 탭 간 postMessage로 전달하지 않고,
// 유니티 exe 실행용 프로토콜/런처 URL에 실어서 넘긴다.
//
// 왜 분리하나?
// - 백엔드 통신 책임을 한 파일에 모으기 위해
// - 나중에 응답 구조가 바뀌어도 여기만 수정하면 되게 하려고
//
// 주의:
// - apiClient interceptor가 Authorization 헤더를 자동으로 붙여줌
// - 이 요청은 보호 라우트(/auth/handoff)라 로그인 상태여야 함
// ─────────────────────────────────────────────────────────────

import { apiClient } from "@/shared/api/client";

export interface HandoffResponse {
  handoff_token: string;
  expires_in: number;
}

// handoff token 발급 요청
//
// 웹에서 게임 실행 버튼 클릭 시 호출.
// 서버는 로그인된 유저 기준으로 30초짜리 1회용 handoff token을 발급한다.
// 프론트는 이 값을 exe 실행용 커스텀 프로토콜/런처에 실어 전달한다.
export const requestHandoffToken = async (): Promise<HandoffResponse> => {
  const res = await apiClient.post("/auth/handoff", {
    target_service: "unity",
  });

  return res.data;
};