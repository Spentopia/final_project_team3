// domains/auth/hooks/useAuth.ts
// ─────────────────────────────────────────────────────────────
// 인증 상태를 실시간으로 감지하고 관리하는 커스텀 훅
//
// 왜 필요한가?
// 소셜 로그인은 외부 페이지(구글/카카오)로 리다이렉트된 후 돌아오는 방식임.
// 돌아왔을 때 Supabase SDK가 URL 파라미터에서 세션을 자동으로 추출하는데,
// // 이걸 감지해서 ProtectedRoute에서 /auth/exchange로 앱 JWT를 교환함
//
// onAuthStateChange가 감지하는 이벤트들:
// - SIGNED_IN: 로그인 성공 (소셜 로그인 리다이렉트 후 포함)
// - SIGNED_OUT: 로그아웃
// - TOKEN_REFRESHED: access_token 만료되어 자동 갱신됨
// - USER_UPDATED: 유저 정보 변경 (비밀번호 변경 등)
//
// 사용 예시:
//   const { user, loading } = useAuth();
//   if (loading) return <로딩중/>;
//   if (!user) return <로그인필요/>;
//   return <메인화면 user={user} />;

import { useEffect, useState } from "react";
import { supabase } from "@/shared/lib/supabase";
import { authStorage } from "@/shared/lib/auth";
import type { User } from "@supabase/supabase-js";

export function useAuth() {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // ── 1) 현재 세션 확인 ───────────────────────────────────
    // 앱이 처음 열릴 때 (새로고침 포함) 기존 세션이 있는지 확인
    // Supabase SDK가 내부 저장소에 저장해둔 세션을 읽어옴
    supabase.auth.getSession().then(({ data }) => {
      if (data.session) {
        setUser(data.session.user);
      }
      setLoading(false);
    });

    // ── 2) 세션 변경 실시간 감지 ────────────────────────────
    // 이 리스너는 앱이 살아있는 동안 계속 동작함
    // 로그인/로그아웃/토큰갱신 등이 발생하면 콜백이 호출됨
    const { data: listener } = supabase.auth.onAuthStateChange(
      (_event, session) => {
        if (session) {
          setUser(session.user);
        } else {
          setUser(null);
          authStorage.clear();
        }
      }
    );

    // ── 3) 컴포넌트 언마운트 시 리스너 해제 ─────────────────
    // 메모리 누수 방지.
    // 리스너를 안 해제하면 컴포넌트가 사라져도 이벤트 감지가 계속됨.
    // React의 useEffect cleanup 패턴.
    return () => {
      listener.subscription.unsubscribe();
    };
  }, []);

  return { user, loading };
}