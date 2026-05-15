// domains/unity/api/unityHandoff.ts
// ─────────────────────────────────────────────────────────────
// 웹에서 유니티 exe 실행을 시작하는 handoff 로직
//
// 전체 흐름 (Steam 경유):
// 1) /auth/handoff 호출 -> 30초짜리 1회용 handoff token 발급
// 2) 웹이 handoff token을 steam://run/{appid}//{token}/ URL에 실음
// 3) 브라우저가 Steam 클라이언트를 호출 -> Steam이 유니티 exe 실행
// 4) 유니티 exe는 SteamApps.GetLaunchCommandLine()으로 handoff token 받음
// 5) 유니티 exe는 받은 token으로 /auth/handoff/exchange를 호출해
//    자기 access/refresh를 발급받음
//
// 왜 Steam을 거치는가?
// - Steam 오버레이(Shift+Tab) 사용
// - Steam 친구 초대 UI 사용
// - 유저 식별은 우리 handoff token만 사용 (Steam 인증 X)
//
// 보안:
// - handoff token은 OS 명령행이 아니라 Steam 내부 메모리로 전달됨
// - 다른 프로세스가 OS args를 들여다봐도 token 노출되지 않음
//
// 중요:
// - handoff token은 1회용
// - handoff token은 짧은 TTL(30초)
// - Steam 클라이언트가 실행 중이고 로그인된 상태여야 함
// - Steam URL 호출 후 blur/visibilitychange로 실행 시도 간접 감지
// ─────────────────────────────────────────────────────────────

import { requestHandoffToken } from "@/domains/plaza/pages/api/handoff";

// Steam appid (개발 단계는 480 = Spacewar 데모용)
// 추후 본인 appid 발급 시 환경변수만 교체하면 됨
const STEAM_APP_ID = import.meta.env.VITE_STEAM_APP_ID;

// steam://run/{appid}//{handoff_token}/ 형태로 URL을 만든다.
//
// 형식 설명:
// - steam://run/{appid}        → "이 appid 게임을 실행하라"
// - //{handoff_token}          → launch command line으로 전달할 인자
// - /                          → URL 끝 슬래시 (Steam URL 규칙)
//
// Steam은 이 URL을 받으면:
// 1) Steam 클라이언트가 활성화되고
// 2) 해당 appid에 매핑된 게임을 실행하면서
// 3) handoff_token을 OS 명령행이 아닌 Steam 내부 메모리에 보관
// 4) 유니티에서 SteamApps.GetLaunchCommandLine()으로 꺼냄
function buildLauncherUrl(handoffToken: string): string {
  if (!STEAM_APP_ID) {
    throw new Error("VITE_STEAM_APP_ID가 설정되지 않았습니다.");
  }

  return `steam://run/${STEAM_APP_ID}//${encodeURIComponent(handoffToken)}/`;
}

// 커스텀 프로토콜과 마찬가지로 steam:// URL도 브라우저가
// 실행 성공/실패를 명확히 알려주지 않는다.
// 그래서 아래 두 신호로 "실행이 시도된 것 같다"는 정도만 간접 감지한다.
//
// - window blur: 브라우저 포커스를 잃음
// - visibilitychange(hidden): 브라우저 탭이 비가시 상태가 됨
//
// timeout 안에 아무 변화가 없으면
// "Steam이 안 켜져 있거나 게임 실행 실패 가능성"으로 보고 안내한다.
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

    window.addEventListener("blur", handleSuccess, { once: true });
    document.addEventListener("visibilitychange", handleVisibilityChange);

    window.setTimeout(() => {
      if (finished) return;
      finished = true;
      cleanup();
      reject(
          new Error(
              "게임 클라이언트를 실행하지 못했습니다. Steam 클라이언트가 실행 중이고 로그인되어 있는지 확인해주세요."
          )
      );
    }, timeoutMs);
  });
}

// 웹에서 유니티 게임 실행 (Steam 경유)
//
// 동작:
// 1) 백엔드에서 handoff token 발급
// 2) steam:// URL 생성
// 3) Steam 클라이언트 호출 -> Steam이 유니티 실행
// 4) blur / visibilitychange 기반으로 실행 시도를 간접 감지
//
// 사전 조건:
// - 사용자 PC에 Steam 클라이언트가 설치되어 있고 로그인된 상태
// - Steam 라이브러리에 우리 유니티 exe가 "비-Steam 게임"으로 등록되어 있음
//   (Steam이 appid 480에 매핑된 게임으로 실행 가능하게)
export async function startUnityGame(): Promise<void> {
  const { handoff_token } = await requestHandoffToken();
  const launcherUrl = buildLauncherUrl(handoff_token);

  // 프로토콜 호출 전에 감지 Promise를 먼저 준비해야
  // 아주 빠르게 blur 되는 경우도 놓치지 않는다.
  const waitPromise = waitForLauncherAttempt();

  window.location.href = launcherUrl;

  await waitPromise;
}