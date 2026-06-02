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
  //
  // [수정] 동시 401 경쟁 조건 해결
  //
  // 문제:
  //   페이지 로드 시 API 요청 3개가 동시에 발사된 상태에서
  //   access token이 만료되면 3개 모두 401을 받음.
  //   기존 코드는 요청별 _retry 플래그만 있어서
  //   3개 모두 각자 refresh를 호출 → refresh token rotation으로
  //   첫 번째만 성공하고 나머지는 실패 → 강제 로그아웃.
  //
  // 해결책: isRefreshing 전역 플래그 + 대기 큐 패턴
  //   - 첫 번째 401이 refresh를 시작하면 isRefreshing = true
  //   - 이후 401들은 refresh를 직접 호출하지 않고 큐에 대기
  //   - refresh 완료 시 큐에 쌓인 요청들을 새 토큰으로 일괄 재시도
  //   - refresh 실패 시 큐에 쌓인 요청들도 모두 reject

  import axios, { AxiosError, InternalAxiosRequestConfig } from "axios";
  import { authStorage } from "@/shared/lib/auth";

  type RetryableRequestConfig = InternalAxiosRequestConfig & {
    _retry?: boolean;
  };

  const NO_REFRESH_PATHS = [
    "/auth/exchange",
    "/auth/app/exchange",
    "/auth/refresh",
    "/auth/app/refresh",
    "/auth/logout",
    "/auth/app/logout",
  ];

  // ── 전역 refresh 상태 관리 ────────────────────────────────────
  //
  // isRefreshing: 현재 refresh 요청이 진행 중인지 여부
  //   true면 새 refresh 요청을 보내지 않고 큐에 대기
  //
  // failedQueue: refresh 완료를 기다리는 요청들의 콜백 목록
  //   각 항목은 { resolve, reject } 쌍으로
  //   refresh 성공 시 resolve(새토큰), 실패 시 reject(에러) 호출
  // ─────────────────────────────────────────────────────────────
  let isRefreshing = false;
  let failedQueue: Array<{
    resolve: (token: string) => void;
    reject: (error: unknown) => void;
  }> = [];

  // refresh 완료 시 큐에 쌓인 요청들을 일괄 처리
  //
  // token이 있으면 → 모든 대기 요청에 새 토큰 전달 (재시도)
  // error가 있으면 → 모든 대기 요청에 에러 전달 (실패 처리)
  const processQueue = (error: unknown, token: string | null = null) => {
    failedQueue.forEach(({ resolve, reject }) => {
      if (error) {
        reject(error);
      } else {
        resolve(token!);
      }
    });
    // 처리 완료 후 큐 비우기
    failedQueue = [];
  };

  export async function refreshAccessTokenOnce(): Promise<string> {
    if (isRefreshing) {
      return new Promise<string>((resolve, reject) => {
        failedQueue.push({ resolve, reject });
      });
    }

    isRefreshing = true;

    try {
      const refreshResponse = await apiClient.post("/auth/refresh", {});
      const newAccessToken = refreshResponse.data.access_token as string;

      authStorage.setToken(newAccessToken);
      processQueue(null, newAccessToken);

      return newAccessToken;
    } catch (refreshError) {
      processQueue(refreshError, null);
      authStorage.clear();
      throw refreshError;
    } finally {
      isRefreshing = false;
    }
  }

  function resolveBackendUrl() {
    const configuredUrl = import.meta.env.VITE_BACKEND_URL ?? "http://localhost:1113";

    if (
      typeof window !== "undefined" &&
      window.location.hostname === "10.0.2.2" &&
      configuredUrl.includes("localhost")
    ) {
      return configuredUrl.replace("localhost", "10.0.2.2");
    }

    return configuredUrl;
  }

  export const apiClient = axios.create({
    baseURL: resolveBackendUrl(),
    withCredentials: true, // refresh 쿠키 자동 포함
  });

  // 요청 보낼 때 access token 자동 첨부
  apiClient.interceptors.request.use((config) => {
    const token = authStorage.getToken();
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
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

      if (NO_REFRESH_PATHS.some((path) => originalRequest.url?.includes(path))) {
        // refresh 요청 자체가 실패한 경우.
        //
        // 대표 케이스:
        // - refresh token 만료
        // - 로그아웃/탈퇴로 refresh session 폐기
        // - 관리자가 회원을 비활성화해서 해당 user_id의 refresh_sessions 폐기
        // - 백엔드에서 is_active=false / deleted_at으로 refresh 차단
        //
        // 어떤 이유든 refresh가 실패하면 새 access token을 받을 수 없으므로
        // 메모리 access token을 삭제한다.
        if (originalRequest.url?.includes("/auth/refresh")) {
          authStorage.clear();
        }

        return Promise.reject(error);
      }

      // access 만료/유효하지 않음이 아닌 다른 401 → 그대로 실패
      if (error.response?.status !== 401 || originalRequest._retry) {
        return Promise.reject(error);
      }

      // ── 여기서부터 첫 401 처리 ──────────────────────────────
      //
      // _retry: 이 요청이 이미 재시도된 적 있는지 표시
      // 재시도 후에도 401이 오면 무한 루프 방지를 위해 그냥 실패
      originalRequest._retry = true;

      if (isRefreshing) {
        // ── 다른 요청이 이미 refresh 중인 경우 ───────────────
        //
        // 직접 refresh를 호출하지 않고 큐에 등록.
        // refresh가 완료되면 processQueue()가 호출되어
        // 새 토큰을 받아서 원래 요청을 재시도함.
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then((newToken) => {
            // refresh 성공 → 새 토큰으로 헤더 교체 후 재시도
            originalRequest.headers = originalRequest.headers ?? {};
            originalRequest.headers.Authorization = `Bearer ${newToken}`;
            return apiClient(originalRequest);
          })
          .catch((err) => {
            // refresh 실패 → 이 요청도 실패 처리
            return Promise.reject(err);
          });
      }

      try {
        const newAccessToken = await refreshAccessTokenOnce();

        // 원래 요청 헤더 갱신 후 재시도
        originalRequest.headers = originalRequest.headers ?? {};
        originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

        return apiClient(originalRequest);
      } catch (refreshError) {
        return Promise.reject(refreshError);
      }
    }
  );
