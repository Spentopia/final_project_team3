import { Outlet, useSearchParams } from "react-router";
import { useState, useEffect, useRef } from "react";
import { useWallet } from "@solana/wallet-adapter-react";
import { syncOwnedNfts } from "@/domains/avatar/api/avatarApi";
import Sidebar from "./Sidebar";
import Header from "./Header";
import { Zap } from "lucide-react";
import { Button } from "../ui/button";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  getCurrentMonthlyScore,
  getStreak,
  type MonthlyScoreResponse,
  type StreakResponse,
} from "@/shared/api/rewardApi";

// 각 항목 최대 점수 (백엔드 로직 기준)
const MAX_SCORES = {
  record_days: 30,
  receipt: 25,
  diary: 20,
  budget: 15,
  streak: 10,
} as const;

export default function RootLayout() {
  const { connected, publicKey } = useWallet();
  const syncedWalletRef = useRef<string | null>(null);
  const [searchParams] = useSearchParams();
  const isWebView = searchParams.get("webview") === "true";

  // 지갑 연결될 때 한 번만 NFT sync (로그인 후 메인, 새로고침 등)
  useEffect(() => {
    const walletAddr = publicKey?.toBase58() ?? null;
    if (!connected || !walletAddr) {
      syncedWalletRef.current = null;
      return;
    }
    if (syncedWalletRef.current === walletAddr) return; // 이미 sync 완료
    syncedWalletRef.current = walletAddr;
    void syncOwnedNfts().catch(() => {});
  }, [connected, publicKey]);

  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [isMonthlyScoreOpen, setIsMonthlyScoreOpen] = useState(false);

  const [monthlyScore, setMonthlyScore] = useState<MonthlyScoreResponse | null>(null);
  const [streak, setStreak] = useState<StreakResponse | null>(null);

  useEffect(() => {
    void getCurrentMonthlyScore()
      .then(setMonthlyScore)
      .catch(() => {/* 미로그인 상태 등 무시 */});

    void getStreak()
      .then(setStreak)
      .catch(() => {});
  }, []);

  useEffect(() => {
    const handler = () => {
      setTimeout(() => {
        void getCurrentMonthlyScore().then(setMonthlyScore).catch(() => {});
        void getStreak().then(setStreak).catch(() => {});
      }, 800);
    };
    window.addEventListener("spentopia:score-refresh", handler);
    return () => window.removeEventListener("spentopia:score-refresh", handler);
  }, []);

  const totalScore = monthlyScore?.total_score ?? 0;
  const progressPct = Math.min(totalScore, 100);

  if (isWebView) {
    return (
      <main className="min-h-dvh w-full overflow-x-hidden bg-background text-foreground">
        <div className="w-full max-w-full overflow-x-hidden px-0 py-0">
          <Outlet />
        </div>
      </main>
    );
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      <div className="hidden h-full lg:block">
        <Sidebar
          weeklyScore={totalScore}
          onWeeklyScoreClick={() => setIsMonthlyScoreOpen(true)}
        />
      </div>

      {isSidebarOpen && (
        <div
          className="fixed inset-0 z-40 bg-black/50 lg:hidden"
          onClick={() => setIsSidebarOpen(false)}
        />
      )}

      <div
        className={`fixed inset-y-0 left-0 z-50 transform transition-transform duration-300 lg:hidden ${
          isSidebarOpen ? "translate-x-0" : "-translate-x-full"
        }`}
      >
        <Sidebar
          weeklyScore={totalScore}
          onWeeklyScoreClick={() => setIsMonthlyScoreOpen(true)}
        />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header onMenuClick={() => setIsSidebarOpen(!isSidebarOpen)} />
        <main className="flex-1 overflow-auto bg-[radial-gradient(circle_at_top_right,rgba(96,165,250,0.12),transparent_32%),linear-gradient(180deg,rgba(255,255,255,0.72),rgba(239,246,255,0.32)_260px,transparent)] p-6 dark:bg-[radial-gradient(circle_at_top_right,rgba(212,175,119,0.06),transparent_30%),linear-gradient(180deg,rgba(255,255,255,0.04),transparent_240px)]">
          <Outlet />
        </main>
      </div>

      <button
        type="button"
        onClick={() => setIsChatbotOpen(true)}
        className="fixed bottom-8 right-8 z-30 flex flex-col items-center gap-0 transition-all hover:-translate-y-0.5"
      >
        <img src="/aichatbot.png" alt="AI 챗봇" className="h-20 w-20 object-contain" />
        <span className="-mt-2 text-[11px] font-bold tracking-tight text-luxury-gold drop-shadow">챗봇 상담</span>
      </button>

      <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />

      <Dialog open={isMonthlyScoreOpen} onOpenChange={setIsMonthlyScoreOpen}>
        <DialogContent className="max-w-md overflow-hidden border-border bg-card p-0 shadow-soft">
          <div className="p-6 text-card-foreground">
            <DialogHeader>
              <div className="mb-4 flex items-center justify-between">
                <DialogTitle className="text-xl font-bold">이번 달 성실도</DialogTitle>
                <Zap className="h-5 w-5 text-[#2563eb] dark:text-luxury-gold" />
              </div>
            </DialogHeader>

            <p className="mb-2 text-5xl font-extrabold">
              {totalScore}점
            </p>
            <div className="mb-5 h-3 overflow-hidden rounded-full bg-[#dbeafe] shadow-inner dark:bg-slate-800">
              <div
                className="h-full rounded-full bg-[linear-gradient(135deg,#3b82f6,#2563eb)] shadow-[0_0_14px_rgba(37,99,235,0.34)] transition-all duration-700 dark:bg-[linear-gradient(90deg,#0f172a,#4338ca,#7c3aed)] dark:shadow-[0_0_18px_rgba(124,58,237,0.6)]"
                style={{ width: `${progressPct}%` }}
              />
            </div>

            <div className="space-y-3 text-sm">
              <ScoreRow
                label="소비 기록"
                score={monthlyScore?.record_days_score}
                max={MAX_SCORES.record_days}
              />
              <ScoreRow
                label="영수증 인증"
                score={monthlyScore?.receipt_score}
                max={MAX_SCORES.receipt}
              />
              <ScoreRow
                label="일기 작성"
                score={monthlyScore?.diary_score}
                max={MAX_SCORES.diary}
              />
              <ScoreRow
                label="예산 체크"
                score={monthlyScore?.budget_score}
                max={MAX_SCORES.budget}
              />
              <div className="flex justify-between">
                <span>연속 활동</span>
                <span className="font-bold">
                  {streak?.current_streak != null
                    ? `${streak.current_streak}일 · ${monthlyScore?.streak_score ?? 0}/${MAX_SCORES.streak}`
                    : `0일 · 0/${MAX_SCORES.streak}`}
                </span>
              </div>
            </div>

            {monthlyScore?.reward_granted && (
              <p className="mt-4 text-center text-xs font-medium text-luxury-gold">
                월간 보상 지급 완료
              </p>
            )}
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}

function ScoreRow({
  label,
  score,
  max,
}: {
  label: string;
  score: number | null | undefined;
  max: number;
}) {
  return (
    <div className="flex justify-between">
      <span>{label}</span>
      <span className="font-bold">
        {`${score ?? 0}/${max}`}
      </span>
    </div>
  );
}
