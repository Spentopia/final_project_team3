// domains/unity/api/gameLoginCode.ts
// ─────────────────────────────────────────────────────────────
// 웹에서 Unity 게임 로그인 코드를 발급받는 로직
//
// 기존에는 웹에서 Steam 또는 Unity exe를 자동 실행하면서
// handoff token을 실행 인자로 전달하려고 했다.
//
// 하지만 Steam 실행 환경에서는:
// - steam://run/480은 Spacewar로 매핑됨
// - 비-Steam 게임 shortcut id는 PC마다 달라짐
// - 실행 인자가 Unity까지 안정적으로 전달된다는 보장이 약함
//
// 그래서 현재 정책은 다음과 같다.
//
// 전체 흐름:
// 1) 웹에서 "게임 로그인 코드 생성" 버튼 클릭
// 2) /auth/handoff 호출
// 3) 백엔드가 8자리 1회용 코드를 발급
// 4) 웹 화면에 코드와 남은 유효시간 표시
// 5) 사용자가 Steam 라이브러리에서 게임을 직접 실행
// 6) Unity 로그인 화면에 코드를 입력
// 7) Unity가 /auth/handoff/exchange로 코드를 교환
// 8) Unity용 access/refresh token 발급
//
// 이 파일의 책임:
// - 백엔드에서 게임 로그인 코드를 발급받아 반환한다.
// - Steam 실행은 하지 않는다.
// ─────────────────────────────────────────────────────────────

import { requestHandoffToken } from "@/domains/plaza/pages/api/handoff.ts";

export type GameLoginCodeResult = {
  // 화면에 표시할 8자리 코드.
  code: string;

  // 코드 유효시간. 초 단위.
  expiresIn: number;
};

// 게임 로그인 코드 생성.
//
// UI에서는 이 함수를 호출한 뒤
// result.code를 크게 보여주면 된다.
//
// 예:
// "게임 로그인 코드: AB7K2Q9P"
// "60초 안에 Unity 게임 화면에 입력해 주세요."
export async function createGameLoginCode(): Promise<GameLoginCodeResult> {
  const { handoff_token, expires_in } = await requestHandoffToken();

  return {
    code: handoff_token,
    expiresIn: expires_in,
  };
}