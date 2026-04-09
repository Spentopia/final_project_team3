import { useState } from "react";
import { Bell, Menu, Wallet, X, Moon, Sun, LogOut } from "lucide-react";
import { Button } from "./ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "./ui/sheet";
import { useTheme } from "next-themes";

interface HeaderProps {
  onMenuClick?: () => void;
}

export default function Header({ onMenuClick }: HeaderProps) {
  const [notifications] = useState([
    { id: 1, text: "예산의 80%를 사용했어요!", type: "warning", time: "5분 전" },
    { id: 2, text: "새로운 아바타를 획득했어요! 🎉", type: "success", time: "1시간 전" },
    { id: 3, text: "7일 연속 기록 달성! 보상이 지급됐어요", type: "success", time: "2시간 전" },
  ]);

  const [walletConnected] = useState(false);
  const { theme, setTheme } = useTheme();

  const handleLogout = () => {
  localStorage.removeItem("spentopia_auth");
  sessionStorage.removeItem("spentopia_auth");
  window.location.replace("/login");
};

  return (
    <header className="flex h-16 items-center justify-between border-b border-white/50 dark:border-gray-700/50 bg-white/60 dark:bg-gray-900/60 px-6 backdrop-blur-xl">
      <div className="flex items-center gap-4">
        <Button
          variant="ghost"
          size="icon"
          className="text-gray-700 dark:text-gray-300 lg:hidden"
          onClick={onMenuClick}
        >
          <Menu className="h-5 w-5" />
        </Button>

        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="hidden text-gray-700 dark:text-gray-300 lg:flex">
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-80">
            <SheetHeader>
              <SheetTitle>메뉴</SheetTitle>
            </SheetHeader>
          </SheetContent>
        </Sheet>

        <div>
          <h2 className="font-bold text-gray-900 dark:text-gray-100">안녕하세요! 👋</h2>
          <p className="text-sm text-gray-600 dark:text-gray-400">오늘도 알뜰한 소비 하세요</p>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
          className="text-gray-700 dark:text-gray-300"
        >
          <Sun className="h-5 w-5 rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0" />
          <Moon className="absolute h-5 w-5 rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100" />
          <span className="sr-only">테마 변경</span>
        </Button>

        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="relative text-gray-700 dark:text-gray-300">
              <Bell className="h-5 w-5" />
              {notifications.length > 0 && (
                <span className="absolute right-1 top-1 h-2 w-2 rounded-full bg-red-500"></span>
              )}
            </Button>
          </SheetTrigger>
          <SheetContent>
            <SheetHeader>
              <SheetTitle>알림</SheetTitle>
            </SheetHeader>
          </SheetContent>
        </Sheet>

        {walletConnected ? (
          <Button className="hidden bg-gradient-to-r from-cyan-500 to-blue-500 sm:flex">
            <Wallet className="mr-2 h-4 w-4" />
            <span>연결됨</span>
          </Button>
        ) : (
          <Button variant="outline" className="hidden sm:flex">
            <Wallet className="mr-2 h-4 w-4" />
            <span>지갑 연결</span>
          </Button>
        )}

        <Button
          variant="outline"
          onClick={handleLogout}
          className="hidden sm:flex"
        >
          <LogOut className="mr-2 h-4 w-4" />
          <span>로그아웃</span>
        </Button>

        <Button className="bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700">
          게임시작
        </Button>
      </div>
    </header>
  );
}