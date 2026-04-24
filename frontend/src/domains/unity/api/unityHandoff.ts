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
//
// 왜 이렇게 하나?
// - 웹의 access/refresh token 원문을 exe에 직접 넘기지 않기 위해
// - 짧은 TTL의 1회용 handoff token만 넘겨 피해 범위를 줄이기 위해
// - 유니티가 자기 전용 세션(access/refresh)을 따로 갖게 하기 위해
//
// 중요:
// - handoff token은 1회용
// - handoff token은 짧은 TTL(30초)
// - 프론트는 handoff token을 장기 저장하지 않고 즉시 실행 URL에만 실어 보냄
// - 커스텀 프로토콜 직결 방식은 exe 실행 성공 여부를 100% 정확히 알 수 없어서
//   blur / visibilitychange로 "간접 감지"만 수행함
// ─────────────────────────────────────────────────────────────

import { requestHandoffToken } from "@/domains/plaza/pages/api/handoff";

// 예:
// VITE_UNITY_LAUNCHER_URL=spentopia://launch
// 런처 브리지 URL 규칙에 맞춰 설정해야 함.
const UNITY_LAUNCHER_URL = import.meta.env.VITE_UNITY_LAUNCHER_URL;

// 예:
// VITE_UNITY_LAUNCHER_URL=spentopia://launch
//
// 실제 값은 운영체제에 등록된 커스텀 프로토콜 규칙에 맞춰야 한다.
function buildLauncherUrl(handoffToken: string): string {
  if (!UNITY_LAUNCHER_URL) {
    throw new Error("VITE_UNITY_LAUNCHER_URL이 설정되지 않았습니다.");
  }

  const separator = UNITY_LAUNCHER_URL.includes("?") ? "&" : "?";

  return `${UNITY_LAUNCHER_URL}${separator}handoff_token=${encodeURIComponent(
      handoffToken
  )}`;
}

// 커스텀 프로토콜 직결은 브라우저가 exe 실행 성공/실패를 명확히 알려주지 않는다.
// 그래서 아래 두 신호를 이용해 "실행이 시도된 것 같다"는 정도만 간접 감지한다.
//
// - window blur: 브라우저 포커스를 잃음
// - visibilitychange(hidden): 브라우저 탭이 비가시 상태가 됨
//
// timeout 안에 아무 변화가 없으면
// "게임 클라이언트를 실행하지 못했을 가능성"으로 보고 에러를 띄운다.
function waitForLauncherAttempt(timeoutMs = 2500): Promise<void> {
  return new Promise((resolve, reject) => {
    let finished = false;

    const cleanup = () => {
      window.removeEventListener("blur", handleSuccess);
      document.removeEventListener("visibilitychange", handleVisibilityChange);
    };

    const handleSuccess = () => {
      if (finished) return;
      finished = true;
      cleanup();
      resolve();
    };

    const handleVisibilityChange = () => {
      if (document.visibilityState === "hidden") {
        handleSuccess();
      }
    };

    // 브라우저가 포커스를 잃으면 exe/런처 실행이 시도된 것으로 간주
    window.addEventListener("blur", handleSuccess, { once: true });

    // 탭이 hidden으로 전환돼도 실행 시도로 간주
    document.addEventListener("visibilitychange", handleVisibilityChange);

    // 일정 시간 내 아무 변화가 없으면 실패 가능성이 높다고 보고 안내
    window.setTimeout(() => {
      if (finished) return;
      finished = true;
      cleanup();
      reject(
          new Error(
              "게임 클라이언트를 실행하지 못했습니다. 설치 여부와 프로토콜 연결을 확인해주세요."
          )
      );
    }, timeoutMs);
  });
}

// 웹에서 유니티 게임 실행
//
// 동작:
// 1) 백엔드에서 handoff token 발급
// 2) launcher URL 생성
// 3) 커스텀 프로토콜 호출
// 4) blur / visibilitychange 기반으로 실행 시도를 간접 감지
export async function startUnityGame(): Promise<void> {
  const { handoff_token } = await requestHandoffToken();
  const launcherUrl = buildLauncherUrl(handoff_token);

  // 프로토콜 호출 전에 감지 Promise를 먼저 준비해야
  // 아주 빠르게 blur 되는 경우도 놓치지 않는다.
  const waitPromise = waitForLauncherAttempt();

  window.location.href = launcherUrl;

  await waitPromise;
}