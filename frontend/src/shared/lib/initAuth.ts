// src/shared/lib/initAuth.ts
//
// 앱 시작 시 refresh 쿠키로 access token을 복구하는 함수
//
// 동작:
// - access token은 메모리에만 있으므로 새로고침하면 사라짐
// - 대신 refresh 쿠키는 브라우저가 유지함
// - 앱 시작 시 /auth/refresh 호출해서 새 access를 받아 메모리에 저장

import { apiClient } from "@/shared/api/client";
import { authStorage } from "@/shared/lib/auth";

export async function initAuth(): Promise<boolean> {
  try {
    const res = await apiClient.post(
      "/auth/refresh",
      {},
      {
        headers: {
          "X-Client-Type": "web",
        },
      }
    );

    authStorage.setToken(res.data.access_token);
    return true;
  } catch {
    authStorage.clear();
    return false;
  }
}