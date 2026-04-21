import { Outlet } from "react-router";
import { useState, useEffect } from "react";
import Sidebar from "./Sidebar";
import Header from "./Header";
import { MessageCircle, Zap } from "lucide-react";
import { Button } from "../ui/button";
import AiChatbotDialog from "@/components/chat/AiChatbotDialog";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
import {
  getCurrentWeeklyScore,
  getStreak,
  type WeeklyScoreResponse,
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
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [isWeeklyScoreOpen, setIsWeeklyScoreOpen] = useState(false);

  const [weeklyScore, setWeeklyScore] = useState<WeeklyScoreResponse | null>(null);
  const [streak, setStreak] = useState<StreakResponse | null>(null);

  useEffect(() => {
    void getCurrentWeeklyScore()
      .then(setWeeklyScore)
      .catch(() => {/* 미로그인 상태 등 무시 */});

    void getStreak()
      .then(setStreak)
      .catch(() => {});
  }, []);

  const totalScore = weeklyScore?.total_score ?? 0;
  const progressPct = Math.min(totalScore, 100);

  return (
    <div className="flex h-screen w-full overflow-hidden bg-background text-foreground">
      <div className="hidden h-full lg:block">
        <Sidebar
          weeklyScore={totalScore}
          onWeeklyScoreClick={() => setIsWeeklyScoreOpen(true)}
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
          onWeeklyScoreClick={() => setIsWeeklyScoreOpen(true)}
        />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header onMenuClick={() => setIsSidebarOpen(!isSidebarOpen)} />
        <main className="flex-1 overflow-auto bg-[radial-gradient(circle_at_top_right,rgba(212,175,119,0.06),transparent_30%),linear-gradient(180deg,rgba(255,255,255,0.04),transparent_240px)] p-6">
          <Outlet />
        </main>
      </div>

      <Button
        onClick={() => setIsChatbotOpen(true)}
        className="fixed bottom-8 right-8 z-30 h-14 w-14 rounded-2xl border border-border bg-card text-luxury-emerald shadow-soft transition-all hover:-translate-y-0.5 hover:border-luxury-emerald/40 hover:bg-card"
      >
        <MessageCircle className="h-6 w-6" />
      </Button>

      <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />

      <Dialog open={isWeeklyScoreOpen} onOpenChange={setIsWeeklyScoreOpen}>
        <DialogContent className="max-w-md overflow-hidden border-border bg-card p-0 shadow-soft">
          <div className="p-6 text-card-foreground">
            <DialogHeader>
              <div className="mb-4 flex items-center justify-between">
                <DialogTitle className="text-xl font-bold">이번 주 성실도</DialogTitle>
                <Zap className="h-5 w-5 text-luxury-gold" />
              </div>
            </DialogHeader>

            <p className="mb-2 text-5xl font-extrabold">
              {totalScore}점
            </p>
            <div className="mb-5 h-3 overflow-hidden rounded-full bg-muted">
              <div
                className="h-full rounded-full bg-[linear-gradient(90deg,var(--luxury-gold),var(--luxury-emerald))] transition-all duration-700"
                style={{ width: `${progressPct}%` }}
              />
            </div>

            <div className="space-y-3 text-sm">
              <ScoreRow
                label="소비 기록"
                score={weeklyScore?.record_days_score}
                max={MAX_SCORES.record_days}
              />
              <ScoreRow
                label="영수증 인증"
                score={weeklyScore?.receipt_score}
                max={MAX_SCORES.receipt}
              />
              <ScoreRow
                label="일기 작성"
                score={weeklyScore?.diary_score}
                max={MAX_SCORES.diary}
              />
              <ScoreRow
                label="예산 체크"
                score={weeklyScore?.budget_score}
                max={MAX_SCORES.budget}
              />
              <div className="flex justify-between">
                <span>연속 활동</span>
                <span className="font-bold">
                  {streak?.current_streak != null
                    ? `${streak.current_streak}일`
                    : "0일"}
                </span>
              </div>
            </div>

            {weeklyScore?.reward_granted && (
              <p className="mt-4 text-center text-xs font-medium text-luxury-gold">
                🎉 이번 주 보상 지급 완료
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
