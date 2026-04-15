// src/client.ts
//
// Axios 공용 클라이언트
//
// 역할:
// 1) access token을 Authorization 헤더에 자동 첨부
// 2) 401 발생 시 /auth/refresh 호출
// 3) refresh 성공하면 새 access로 원래 요청 재시도
// 4) refresh도 실패하면 메모리 토큰 삭제
//
// 중요:
// - withCredentials: true
//   -> 브라우저가 refresh 쿠키를 자동으로 포함하게 함
//
// 최종 구조:
// - access token: 메모리(authStorage)
// - refresh token: HttpOnly 쿠키

import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
import { authStorage } from "@/shared/lib/auth";

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _retry?: boolean;
};

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_URL ?? "http://localhost:1113",
  withCredentials: true, // refresh 쿠키 자동 포함
});

// 요청 보낼 때 access token 자동 첨부
apiClient.interceptors.request.use((config) => {
    const token = authStorage.getToken();
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    // 웹 프론트는 항상 web
    config.headers["X-Client-Type"] = "web";
    return config;
});

// 응답에서 401이면 refresh 시도
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as RetryableRequestConfig | undefined;

    // 요청 정보가 없으면 그대로 실패
    if (!originalRequest) {
      return Promise.reject(error);
    }

    // refresh 요청 자체가 실패한 경우 재귀 방지
    if (originalRequest.url?.includes("/auth/refresh")) {
      authStorage.clear();
      return Promise.reject(error);
    }

    // access 만료/유효하지 않음 -> refresh 시도
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        // 웹은 refresh token이 쿠키에 있으므로 body 없이 호출 가능
        const refreshResponse = await apiClient.post("/auth/refresh", {});

        const newAccessToken = refreshResponse.data.access_token as string;

        // 새 access token 메모리에 저장
        authStorage.setToken(newAccessToken);

        // 원래 요청 헤더 갱신 후 재시도
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        return apiClient(originalRequest);
      } catch (refreshError) {
        // refresh도 실패 -> 세션 만료로 간주
        authStorage.clear();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);