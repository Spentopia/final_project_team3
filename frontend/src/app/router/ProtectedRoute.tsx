// app/router/ProtectedRoute.tsx
//
// 보호된 페이지 접근 전 인증 상태 확인 컴포넌트
//
// 최종 흐름:
// 1) 먼저 메모리에 access token이 있는지 확인
// 2) 없으면 Supabase session이 있는지 확인
//    - 구글 로그인 직후 / 이메일 로그인 직후일 수 있음
// 3) session이 있으면 /auth/exchange 로 백엔드 앱 JWT 교환
// 4) 교환 성공 후 access token을 메모리에 저장
// 5) /me 호출해서 profile_completed 여부 확인
//
// 웹뷰 흐름:
// 1) URL fragment에 access token이 있는지 확인 (안드로이드 웹뷰 진입)
// 2) 없으면 메모리 → refresh 쿠키 → Supabase session 순으로 시도
// 3) /me 호출해서 profile_completed 여부 확인
//
// 보안:
// - access token은 localStorage가 아니라 메모리에만 저장
// - refresh token은 HttpOnly 쿠키라 프론트 JS가 직접 만지지 않음
// - 안드로이드 웹뷰 진입 시 access token은 URL fragment(#)로 전달
//   → fragment는 서버 로그/Referer에 안 남음

import { useEffect, useState } from "react";
import { Navigate, useLocation } from "react-router";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import { apiClient } from "@/shared/api/client";
import { initAuth } from "@/shared/lib/initAuth";
import { useUser } from "@/shared/context/UserContext";
import type { MeResponse } from "@/domains/auth/api/auth";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

type AuthStatus = "loading" | "logged_in" | "need_profile" | "not_logged_in" | "forbidden" | "admin_redirect";

let refreshRecoveryInFlight: Promise<boolean> | null = null;

function recoverAccessTokenOnce() {
  if (!refreshRecoveryInFlight) {
    refreshRecoveryInFlight = initAuth().finally(() => {
      refreshRecoveryInFlight = null;
    });
  }

  return refreshRecoveryInFlight;
}

// ─────────────────────────────────────────────────────────────
// URL fragment에서 안드로이드 웹뷰용 access token 추출
//
// 백엔드 /auth/webview/callback이 다음 형태로 리다이렉트:
//   https://spentopia.com/marketplace#access_token=xxx
//
// 왜 fragment(#)인가?
// - URL 쿼리(?)는 서버 로그, Referer 헤더, 브라우저 히스토리에 남음
// - fragment는 서버에 전송되지 않고 브라우저 메모리에만 존재
// - 웹뷰 진입 직후 즉시 추출하고 URL을 정리하면 로그/북마크 노출 방지
//
// 추출 후 처리:
// - 메모리(authStorage)에 저장
// - URL에서 fragment 제거 (새로고침 시 재사용 방지)
// ─────────────────────────────────────────────────────────────
function consumeWebViewTokenFromHash(): string | null {
  const hash = window.location.hash;
  if (!hash || !hash.includes("access_token=")) {
    return null;
  }

  const hashParams = new URLSearchParams(hash.replace(/^#/, ""));
  const token = hashParams.get("access_token");
  if (!token) {
    return null;
  }

  // URL에서 fragment 제거 → 새로고침/북마크 시 토큰 재사용 방지
  // pathname + search는 그대로 유지 (페이지 위치는 보존)
  window.history.replaceState(
      {},
      document.title,
      window.location.pathname + window.location.search
  );

  return token;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const location = useLocation();
  const { user, setUser } = useUser();

  // Supabase session의 access_token을 백엔드 앱 JWT로 교환
  const exchangeSupabaseToken = async (accessToken: string) => {
    const res = await apiClient.post("/auth/exchange", {
      access_token: accessToken,
    });

    return res.data;
  };

  const checkAuth = async (): Promise<AuthStatus> => {
    // ── 1) 안드로이드 웹뷰 진입 시 fragment에서 토큰을 최우선 추출 ───
    //
    // 백엔드 /auth/webview/callback이 검증/발급 후 다음 형태로 리다이렉트:
    //   /marketplace#access_token=xxx
    //
    // 이 시점에 refresh token은 이미 httpOnly 쿠키로 셋팅된 상태.
    // 기존 메모리 토큰이 있어도 새 웹뷰 진입 토큰이 있으면 새 토큰을 우선한다.
    const fragmentToken = consumeWebViewTokenFromHash();
    if (fragmentToken) {
      authStorage.setToken(fragmentToken);
    }

    // 2) 현재 메모리에 저장된 앱 access token 확인
    let token = authStorage.getToken();

    const justLoggedIn = sessionStorage.getItem("just_logged_in") === "true";
    if (justLoggedIn) {
      sessionStorage.removeItem("just_logged_in");
    }

    // 3) access token이 없으면 refresh 쿠키로 앱 토큰 복구 시도
    //
    // 이 단계는 보호 라우트에서만 실행한다.
    // 그래서 공개 페이지(/login 등)에서는 불필요한 /auth/refresh가 발생하지 않는다.
    if (!token && !justLoggedIn) {
      const recovered = await recoverAccessTokenOnce();
      if (recovered) {
        token = authStorage.getToken();
      }
    }

    // 4) 그래도 access token이 없으면 Supabase session 확인
    //
    // 구글 로그인 직후 / 이메일 로그인 직후에는
    // 아직 앱 JWT로 교환되지 않고 Supabase session만 있을 수 있음
    if (!token) {
      const {
        data: { session },
      } = await supabase.auth.getSession();

      if (session?.access_token) {
        const exchanged = await exchangeSupabaseToken(session.access_token);

        token = exchanged.access_token;

        // 교환된 앱 access token을 메모리에 저장
        authStorage.setToken(exchanged.access_token);

        // 앱 내부 보호 API는 이제 우리 앱 JWT만 사용하므로
        // Supabase session은 정리
        try {
          await supabase.auth.signOut();
        } catch {
          // Supabase 세션 정리에 실패해도 앱 JWT 인증 흐름은 계속 진행한다.
        }
      }
    }

    // 5) 그래도 token이 없으면 비로그인
    if (!token) {
      authStorage.clear();
      return "not_logged_in";
    }

    // 6) /me 호출해서 실제 로그인 유저 정보 확인 후 전역 context에 저장
    const res = await apiClient.get("/me");
    const me = res.data as MeResponse;
    setUser(me);

    // ─────────────────────────────────────────────
// 관리자 페이지 접근 검사
//
// 현재 경로가 /admin 으로 시작하면
// role_type = admin 인지 추가 확인.
//
// 프론트 검사는 UX용.
// 실제 보안은 백엔드 admin_middleware가 담당.
// ─────────────────────────────────────────────
    if (location.pathname.startsWith("/admin")) {
      if (me.role_type !== "admin") {
        return "forbidden";
      }

      // admin이 /admin에 들어온 경우는 정상 접근
      return "logged_in";
    }

// ─────────────────────────────────────────────
// 관리자 계정 자동 이동
//
// 운영자 계정은 일반 사용자 대시보드로 보내지 않고
// 항상 관리자 페이지로 보낸다.
//
// 필요한 이유:
// - access token은 메모리 저장이라 창을 닫으면 사라짐
// - 재접속 시 refresh 쿠키로 access token을 복구함
// - 이때 LoginPage의 roleType 분기는 실행되지 않음
// - 따라서 ProtectedRoute에서 /me 결과를 보고 다시 /admin으로 보내야 함
// ─────────────────────────────────────────────
    if (me.role_type === "admin") {
      return "admin_redirect";
    }

// 7) 프로필 완성 여부에 따라 라우팅 분기
    if (!me.profile_completed) {
      return "need_profile";
    }

    return "logged_in";
  };

  // 마운트 시 딱 한 번만 인증 확인 + /me 호출
  // pathname이 바뀌어도 이 effect는 재실행되지 않음 → 탭 이동 시 429 방지
  useEffect(() => {
    let cancelled = false;

    setStatus("loading");

    void checkAuth()
      .then((nextStatus) => {
        if (!cancelled) {
          setStatus(nextStatus);
        }
      })
      .catch((error) => {
        if (cancelled) return;

        const errStatus = (error as { response?: { status?: number } })?.response?.status;

        // 429(과다 요청) / 5xx(서버 오류)는 인증 문제가 아님
        // 토큰을 지우지 않고, 토큰 유무로만 상태 판단
        if (errStatus === 429 || (errStatus !== undefined && errStatus >= 500)) {
          setStatus(authStorage.getToken() ? "logged_in" : "not_logged_in");
          return;
        }

        authStorage.clear();
        setStatus("not_logged_in");
      });

    return () => {
      cancelled = true;
    };
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  // pathname이 바뀔 때마다 캐시된 user로 redirect 판단 (API 호출 없음)
  useEffect(() => {
    if (!user || status === "loading") return;

    if (location.pathname.startsWith("/admin")) {
      setStatus(user.role_type !== "admin" ? "forbidden" : "logged_in");
    } else if (user.role_type === "admin") {
      setStatus("admin_redirect");
    } else if (!user.profile_completed && location.pathname !== "/complete-profile") {
      setStatus("need_profile");
    } else {
      setStatus("logged_in");
    }
  }, [location.pathname, user]); // eslint-disable-line react-hooks/exhaustive-deps

  if (status === "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">로딩 중...</p>
      </div>
    );
  }

  if (status === "not_logged_in") {
    return <Navigate to="/login" replace />;
  }

  // 관리자 계정이 일반 사용자 경로로 들어온 경우 관리자 페이지로 이동
  if (status === "admin_redirect") {
    return <Navigate to="/admin" replace />;
  }


  if (status === "need_profile") {
    if (location.pathname === "/complete-profile") {
      return <>{children}</>;
    }
    return <Navigate to="/complete-profile" replace />;
  }

  // ─────────────────────────────────────────────
  // 관리자 권한 없음
  //
  // 일반 사용자가 /admin 접근 시 홈으로 이동.
  // ─────────────────────────────────────────────
  if (status === "forbidden") {
    return <Navigate to="/" replace />;
  }


  return <>{children}</>;
}
