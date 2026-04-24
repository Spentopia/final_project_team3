// domains/unity/api/unityHandoff.ts
// ─────────────────────────────────────────────────────────────
// 웹에서 유니티 exe 실행을 시작하는 handoff 로직
//
// 전체 흐름:
// 1) /auth/handoff 호출 -> 30초짜리 1회용 handoff token 발급
// 2) 웹이 handoff token을 exe 실행용 프로토콜/런처 URL에 실음
// 3) 브라우저가 운영체제에 유니티 exe 실행을 요청
// 4) 유니티 exe는 전달받은 handoff token으로
//    /auth/handoff/exchange를 호출해 자기 access/refresh를 발급받음
import { requestHandoffToken } from "@/domains/plaza/pages/api/handoff";

// 예:
// VITE_UNITY_LAUNCHER_URL=spentopia://launch
// 런처 브리지 URL 규칙에 맞춰 설정해야 함.
const UNITY_LAUNCHER_URL = import.meta.env.VITE_UNITY_LAUNCHER_URL;

function buildLauncherUrl(handoffToken: string): string {
  if (!UNITY_LAUNCHER_URL) {
    throw new Error("VITE_UNITY_LAUNCHER_URL이 설정되지 않았습니다.");
  }

  const separator = UNITY_LAUNCHER_URL.includes("?") ? "&" : "?";

  return `${UNITY_LAUNCHER_URL}${separator}handoff_token=${encodeURIComponent(
      handoffToken
  )}`;
}

// 웹에서 유니티 게임 실행
//
// 동작:
// 1) 백엔드에서 handoff token 발급
// 2) launcher URL 생성
// 3) 브라우저가 해당 URL로 이동하면서 OS에 exe 실행 요청
export async function startUnityGame(): Promise<void> {
  const { handoff_token } = await requestHandoffToken();
  const launcherUrl = buildLauncherUrl(handoff_token);

  window.location.href = launcherUrl;
}