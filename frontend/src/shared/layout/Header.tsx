import { useState } from "react";
import { toast } from "sonner";
import { Bell, Gamepad2, Menu, Moon, Sun } from "lucide-react";
import { ConnectWalletButton } from "@/domains/wallet/ui/ConnectWalletButton";
import { Button } from "../ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "../ui/sheet";
import { useTheme } from "next-themes";
import { startUnityGame } from "@/domains/unity/api/unityHandoff";

type HeaderProps = {
  onMenuClick?: () => void;
};

export default function Header({ onMenuClick }: HeaderProps) {
  const [notifications] = useState([
    { id: 1, text: "예산의 80%를 사용했어요!", time: "5분 전" },
    { id: 2, text: "새로운 아바타를 획득했어요", time: "1시간 전" },
    { id: 3, text: "7일 연속 기록 달성! 보상이 지급됐어요", time: "2시간 전" },
  ]);

  const { theme, setTheme } = useTheme();

  // 게임 시작 버튼 클릭 시
  // 1) 백엔드에서 handoff token 발급
  // 2) 유니티 새 탭 오픈
  // 3) 유니티 READY 수신 후 postMessage로 handoff 전달
  const handleStartGame = async () => {
    try {
      await startUnityGame();
    } catch (error) {
      console.error("게임 시작 실패:", error);
      toast.error("게임 시작에 실패했어요.");
    }
  };

  return (
    <header className="flex min-h-20 items-center justify-between border-b border-border bg-[var(--surface-elevated)] px-6 py-3 shadow-[0_1px_0_rgba(255,255,255,0.04)] backdrop-blur-xl">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          className="lg:hidden"
          onClick={onMenuClick}
        >
          <Menu className="h-5 w-5" />
        </Button>

        <div className="flex min-w-0 flex-col">
          <span className="text-[11px] font-bold uppercase tracking-[0.18em] text-luxury-gold">
            Spentopia
          </span>
          <p className="mt-0.5 text-base font-semibold tracking-normal text-foreground">
            지출을 관리하면 열리는 나만의 세계
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2 rounded-xl border border-border bg-[var(--surface-subtle)] p-1.5 shadow-card backdrop-blur-xl">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          >
            <Sun className="h-5 w-5 dark:hidden" />
            <Moon className="hidden h-5 w-5 dark:block" />
          </Button>

          <Sheet>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="relative">
                <Bell className="h-5 w-5" />
                {notifications.length > 0 && (
                  <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500" />
                )}
              </Button>
            </SheetTrigger>

            <SheetContent>
              <SheetHeader>
                <SheetTitle>알림</SheetTitle>
              </SheetHeader>

              <div className="mt-6 space-y-4">
                {notifications.map((notif) => (
                  <div key={notif.id} className="rounded border p-3">
                    {notif.text}
                    <div className="text-xs text-gray-400">{notif.time}</div>
                  </div>
                ))}
              </div>
            </SheetContent>
          </Sheet>

          <ConnectWalletButton className="hidden sm:flex" />
        </div>

        <Button
          onClick={handleStartGame}
          className="h-14 rounded-lg border border-orange-300 bg-gradient-to-r from-orange-400 via-amber-500 to-orange-600 px-8 text-lg font-extrabold text-white shadow-xl shadow-orange-500/40 transition-all hover:-translate-y-0.5 hover:scale-[1.03] hover:from-orange-300 hover:via-amber-400 hover:to-orange-500 hover:shadow-orange-500/60 dark:border-orange-300/70 dark:shadow-orange-950/60"
        >
          <Gamepad2 className="mr-3 h-6 w-6" />
          <span>게임시작</span>
        </Button>
      </div>
    </header>
  );
}
