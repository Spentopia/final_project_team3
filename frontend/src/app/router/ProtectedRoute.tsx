// app/router/ProtectedRoute.tsx
// ─────────────────────────────────────────────────────────────
// 보호된 페이지에 접근하기 전 인증 상태를 확인하는 컴포넌트
// 스프링부트의 Spring Security FilterChain과 비슷한 역할
// 백엔드의 jwt_middleware와 짝을 이루는 프론트 측 보호 장치
//
// 확인하는 것 2가지:
// 1) 로그인했는지 → 안 했으면 /login으로
// 2) 프로필 완성했는지 → 안 했으면 /complete-profile로
//
// 왜 프로필 완성을 체크하나?
// 소셜 로그인(구글/카카오)으로 처음 가입하면
// handle_new_user() 트리거가 public.users에 row를 만들지만
// 닉네임/전화번호는 비어있음 (트리거가 id, email, login_provider만 넣어줌)
// 이 상태로 메인에 들어가면 닉네임이 없어서 UI가 깨지니까
// 프로필 완성 페이지로 먼저 보내는 것

import {useEffect, useState} from "react";
import { Navigate } from "react-router";
import {supabase} from "@/shared/lib/supabase"
import { authStorage } from "@/shared/lib/auth";

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export default function ProtectedRoute({ children }: ProtectedRouteProps) {
  // 4가지 상태:
  // loading      — 세션 확인 중 (로딩 화면 표시)
  // logged_in    — 정상 로그인 + 프로필 완성 (원래 페이지 표시)
  // need_profile — 로그인은 됐지만 프로필 미완성 (/complete-profile로 이동)
  // not_logged_in — 비로그인 (/login으로 이동)
  const [status, setStatus] = useState<"loading" | "logged_in" | "need_profile" | "not_logged_in">("loading");

  //컴포넌트가 마운트되면 인증 상태 확인 시작
  useEffect(() => {
    checkAuth();
  }, []);

  const checkAuth = async () => {
    // ── 1) Supabase 세션 확인 ───────────────────────────────
    // Supabase SDK가 내부적으로 localStorage에 저장한 세션을 읽어옴
    // 소셜 로그인 후 리다이렉트로 돌아왔을 때도 여기서 세션이 잡힘
    const {data: {session}} = await supabase.auth.getSession();

    if (!session) {
      if (authStorage.isLoggedIn()) {
        setStatus("logged_in");
      } else {
        setStatus("not_logged_in");
      }
      return;
    }

    // 세션 있으면 토큰을 localStorage에도 저장
    // (백엔드 API 호출 시 authStorage.getToken()으로 동기적으로 꺼내 쓰기 위함)
    authStorage.setToken(session.access_token);
    localStorage.setItem("spentopia_auth", session.access_token);

    // ── 2) public.users에서 프로필 완성 여부 확인 ────────────
    // DDL의 profile_completed 컬럼:
    //   handle_profile_completed() 트리거가
    //   nickname + phone 모두 입력 시 true로 자동 변경
    // false면 아직 닉네임/전화번호를 안 넣은 것
    const {data: profile} = await supabase
    .from("users")
    .select("profile_completed")
    .eq("id", session.user.id)
    .single();

    if (profile && !profile.profile_completed) {
      //프로필 미완성 -> /complete-profile 페이지로
      setStatus("need_profile");
    } else {
      // 정상 -> 원래 페이지 표시
      setStatus("logged_in");
    }

  };

  // ── 상태별 렌더링 ─────────────────────────────────────────
  
  if (status == "loading") {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">로딩 중...</p>
      </div>
    );
  }

   // replace: 브라우저 뒤로가기로 돌아오는 것 방지
  if (status === "not_logged_in") {
    return <Navigate to="/login" replace />;
  }

  if (status === "need_profile") {
    return <Navigate to="/complete-profile" replace />;
  }

  // 정상 로그인 + 프로필 완성 → children(원래 페이지) 렌더링
  return <>{children}</>;

}