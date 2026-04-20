// domains/plaza/api/handoff.ts
// ─────────────────────────────────────────────────────────────
// handoff token 발급 API 함수
//
// 웹에서 "게임 시작" 버튼 클릭 시 호출.
// 백엔드가 30초짜리 1회용 handoff token을 발급한다.
// 프론트는 이 토큰을 URL에 넣지 않고,
// 부모 탭 -> 유니티 탭 postMessage로만 전달한다.
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
// 현재 서버는 target_service = "unity"만 허용한다.
// 그래서 굳이 파라미터로 열어두지 않고 고정값으로 보낸다.
export const requestHandoffToken = async (): Promise<HandoffResponse> => {
  const res = await apiClient.post("/auth/handoff", {
    target_service: "unity",
  });

  return res.data;
};