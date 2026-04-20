// domains/unity/api/unityHandoff.ts
// ─────────────────────────────────────────────────────────────
// 웹 부모 탭에서 유니티 새 탭을 열고,
// handoff token을 postMessage로 전달하는 로직
//
// 전체 흐름:
// 1) /auth/handoff 호출 -> 30초짜리 1회용 handoff token 발급
// 2) 유니티 새 탭 open (토큰은 URL에 넣지 않음)
// 3) 유니티가 로드 완료 후 "UNITY_READY" postMessage 전송
// 4) 부모 탭이 handoff token을 postMessage로 전달
//
// 왜 이렇게 하나?
// - URL에 토큰 노출 방지
// - 웹의 refresh 쿠키 보안 모델 유지
// - 유니티는 handoff token으로 자기 access/refresh를 따로 발급받게 하기 위함
//
// 중요:
// - origin 검증 필수
// - handoff token은 1회용
// - 유니티가 준비되기 전에 보내면 못 받을 수 있으므로 READY 신호를 기다림
// ─────────────────────────────────────────────────────────────

import { requestHandoffToken } from "@/domains/plaza/pages/api/handoff";

// 유니티 서비스 주소
// 실제 유니티가 열리는 주소로 바꿔야 함.
const UNITY_ORIGIN = "http://localhost:5174";

// 부모 탭(웹)에서 유니티 게임 시작
export async function startUnityGame(): Promise<void> {
  // 1) 백엔드에서 handoff token 발급
  const { handoff_token } = await requestHandoffToken();

  // 2) 유니티 새 탭 열기
  //
  // 주의:
  // - URL에 handoff token을 절대 넣지 않음
  // - 새 탭 주소는 "유니티 진입 페이지"만 연다
  const child = window.open(UNITY_ORIGIN, "_blank");

  if (!child) {
    throw new Error("팝업이 차단되었습니다. 팝업 허용 후 다시 시도해주세요.");
  }

  // 3) 유니티가 READY를 보내면 handoff token 전달
  const handleMessage = (event: MessageEvent) => {
    // ── origin 검증 ────────────────────────────────────────
    // 우리가 기대한 유니티 주소에서 온 메시지만 처리
    if (event.origin !== UNITY_ORIGIN) return;

    // ── source 검증 ────────────────────────────────────────
    // event.source는 타입이 MessageEventSource | null이라
    // 바로 child와 비교 시 TS 빨간줄이 날 수 있음.
    // Window 인스턴스인지 먼저 좁혀줌.
    if (!(event.source instanceof Window)) return;

    // 우리가 직접 연 자식 창에서 온 메시지인지 확인
    if (event.source !== child) return;

    // ── 메시지 타입 확인 ────────────────────────────────────
    if (event.data?.type !== "UNITY_READY") return;

    // 4) handoff token 전달
    child.postMessage(
      {
        type: "HANDOFF_TOKEN",
        token: handoff_token,
      },
      UNITY_ORIGIN
    );

    // 1회성 이벤트이므로 전달 후 리스너 제거
    window.removeEventListener("message", handleMessage);
  };

  window.addEventListener("message", handleMessage);
}