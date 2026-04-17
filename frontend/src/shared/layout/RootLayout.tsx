import { Outlet } from "react-router";
import { useState } from "react";
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

export default function RootLayout() {
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);
  const [isChatbotOpen, setIsChatbotOpen] = useState(false);
  const [isWeeklyScoreOpen, setIsWeeklyScoreOpen] = useState(false);

  return (
    <div className="flex h-screen w-full overflow-hidden bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
      <div className="hidden lg:block">
        <Sidebar onWeeklyScoreClick={() => setIsWeeklyScoreOpen(true)} />
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
        <Sidebar onWeeklyScoreClick={() => setIsWeeklyScoreOpen(true)} />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header onMenuClick={() => setIsSidebarOpen(!isSidebarOpen)} />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>

      <Button
        onClick={() => setIsChatbotOpen(true)}
        className="fixed bottom-8 right-8 z-30 h-20 w-20 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 shadow-2xl transition-all hover:scale-110 hover:from-cyan-600 hover:to-blue-700 hover:shadow-cyan-500/50 dark:shadow-cyan-900/50"
      >
        <div className="flex flex-col items-center gap-1">
          <MessageCircle className="h-8 w-8" />
          <span className="text-[10px] font-bold">AI챗바타</span>
        </div>
      </Button>

      <AiChatbotDialog open={isChatbotOpen} onOpenChange={setIsChatbotOpen} />

      <Dialog open={isWeeklyScoreOpen} onOpenChange={setIsWeeklyScoreOpen}>
        <DialogContent className="max-w-md overflow-hidden border-none p-0">
          <div className="bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white">
            <DialogHeader>
              <div className="mb-4 flex items-center justify-between">
                <DialogTitle className="text-xl font-bold">이번 주 성실도</DialogTitle>
                <Zap className="h-6 w-6" />
              </div>
            </DialogHeader>

            <p className="mb-2 text-5xl font-extrabold">85점</p>
            <div className="mb-5 h-3 overflow-hidden rounded-full bg-white/30">
              <div className="h-full w-[85%] rounded-full bg-white" />
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span>소비 기록</span>
                <span className="font-bold">35/40</span>
              </div>
              <div className="flex justify-between">
                <span>영수증 인증</span>
                <span className="font-bold">18/20</span>
              </div>
              <div className="flex justify-between">
                <span>일기 작성</span>
                <span className="font-bold">12/15</span>
              </div>
              <div className="flex justify-between">
                <span>예산 체크</span>
                <span className="font-bold">15/15</span>
              </div>
              <div className="flex justify-between">
                <span>연속 활동</span>
                <span className="font-bold">7일 🔥</span>
              </div>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
