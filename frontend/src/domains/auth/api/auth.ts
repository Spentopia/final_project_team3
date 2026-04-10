import type { LoginRequest, LoginResponse } from "@/domains/auth/model/types";

const USE_MOCK = true;

export const login = async (payload: LoginRequest): Promise<LoginResponse> => {
  if (USE_MOCK) {
    await new Promise((r) => setTimeout(r, 500));

    if (payload.email === "test@test.com" && payload.password === "Test1234!") {
      return {
        accessToken: "mock-token",
        user: {
          id: 1,
          email: payload.email,
          nickname: "길동이",
        },
      };
    }

    throw new Error("로그인 실패");
  }

  throw new Error("API 연결 안됨");
};