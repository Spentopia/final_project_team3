import type { PropsWithChildren } from "react";
import { useEffect, useState } from "react";
import { ThemeProvider } from "@/shared/providers/ThemeProvider";
import { SolanaWalletProvider } from "@/domains/wallet/providers/SolanaWalletProvider";
import { apiClient } from "@/shared/api/client";
import { authStorage } from "@/shared/lib/auth";

// 앱 시작 시 자동 로그인 복구
//
// 왜 필요하냐?
// - access token은 메모리에만 저장하므로 새로고침하면 사라짐
// - 대신 refresh token은 HttpOnly 쿠키로 브라우저에 남아 있음
// - 앱 시작 시 /auth/refresh를 호출하면 새 access token을 다시 받을 수 있음
async function initAuth() {
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
  } catch {
    // refresh 실패면 비로그인 상태로 둠
    authStorage.clear();
  }
}

export default function AppProviders({ children }: PropsWithChildren) {
  const [booting, setBooting] = useState(true);

  useEffect(() => {
    (async () => {
      await initAuth();
      setBooting(false);
    })();
  }, []);

  if (booting) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">앱 초기화 중...</p>
      </div>
    );
  }

  return (
    <ThemeProvider attribute="class" defaultTheme="light" enableSystem>
      <SolanaWalletProvider>{children}</SolanaWalletProvider>
    </ThemeProvider>
  );
}
