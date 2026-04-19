import { apiClient } from "@/shared/api/client";

// 유니티 서비스 주소
// 로컬 개발 기준 예시. 실제 포트/도메인에 맞게 수정.
const UNITY_ORIGIN = "http://localhost:5174";

// 부모 탭에서 유니티 게임을 여는 함수
//
// 흐름:
// 1) 백엔드 /auth/handoff 호출 → 30초짜리 1회용 handoff token 발급
// 2) 유니티 새 탭 open
// 3) 유니티가 로드 완료 후 UNITY_READY postMessage 전송
// 4) 부모 탭이 handoff token을 postMessage로 전달
//
// 주의:
// - handoff token은 URL에 넣지 않음
// - origin 검증 필수
// - event.source 타입이 넓어서 child 비교 시 타입 가드 필요
export async function startUnityGame(): Promise<void> {
  // 1. handoff token 발급
  const res = await apiClient.post("/auth/handoff", {
    target_service: "unity",
  });

  const handoffToken = res.data.handoff_token as string;

  // 2. 유니티 새 탭 열기
  const child = window.open(UNITY_ORIGIN, "_blank");

  if (!child) {
    throw new Error("팝업이 차단되었습니다.");
  }

  // 메시지 수신 핸들러
  const handleMessage = (event: MessageEvent) => {
    // 3-1. 유니티 origin인지 확인
    if (event.origin !== UNITY_ORIGIN) return;

    // 3-2. event.source는 MessageEventSource | null 타입이라
    //      바로 child(Window)와 비교하면 TS 빨간줄이 날 수 있음.
    //      Window 인스턴스인지 먼저 좁혀준다.
    if (!(event.source instanceof Window)) return;

    // 3-3. 우리가 연 그 자식창에서 온 메시지인지 확인
    if (event.source !== child) return;

    // 3-4. 유니티가 준비됐는지 확인
    if (event.data?.type !== "UNITY_READY") return;

    // 4. handoff token 전달
    child.postMessage(
      {
        type: "HANDOFF_TOKEN",
        token: handoffToken,
      },
      UNITY_ORIGIN
    );

    // 한 번 전달했으면 리스너 제거
    window.removeEventListener("message", handleMessage);
  };

  window.addEventListener("message", handleMessage);
}