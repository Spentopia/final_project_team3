import { useState } from "react";
import { Bell, Menu, Moon, Sun, LogOut } from "lucide-react";
import { ConnectWalletButton } from "@/domains/wallet/ui/ConnectWalletButton";
import { Button } from "../ui/button";
import { signOut } from "@/domains/auth/api/auth";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "../ui/sheet";
import { useTheme } from "next-themes";
import { authStorage } from "@/shared/lib/auth";


interface HeaderProps {
  onMenuClick?: () => void;
}

export default function Header({ onMenuClick }: HeaderProps) {
  const [notifications] = useState([
    { id: 1, text: "예산의 80%를 사용했어요!", time: "5분 전" },
    { id: 2, text: "새로운 아바타를 획득했어요! 🎉", time: "1시간 전" },
    { id: 3, text: "7일 연속 기록 달성! 보상이 지급됐어요", time: "2시간 전" },
  ]);

  const { theme, setTheme } = useTheme();

  const handleLogout = async () => {
    await signOut();
    window.location.replace("/login");
};

  return (
    <header className="flex h-16 items-center justify-between border-b border-white/50 dark:border-gray-700/50 bg-white/60 dark:bg-gray-900/60 px-6 backdrop-blur-xl">
      
      {/* 왼쪽 영역 */}
      <div className="flex items-center gap-4">
        
        {/* 모바일 메뉴 버튼 */}
        <Button
          variant="ghost"
          size="icon"
          className="text-gray-700 dark:text-gray-300 lg:hidden"
          onClick={onMenuClick}
        >
          <Menu className="h-5 w-5" />
        </Button>

        {/* PC 메뉴 (Sheet) */}
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="hidden lg:flex text-gray-700 dark:text-gray-300">
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>

          <SheetContent side="left" className="w-80">
            <SheetHeader>
              <SheetTitle>메뉴</SheetTitle>
            </SheetHeader>

            {/* 🔥 메뉴 리스트 복구 */}
            <div className="mt-6 space-y-4">
              <Button variant="outline" className="w-full justify-start">예산 설정</Button>
              <Button variant="outline" className="w-full justify-start">공유 기능</Button>
              <Button variant="outline" className="w-full justify-start">소비 패턴 분석 보고서</Button>
              <Button variant="outline" className="w-full justify-start">알림 시스템</Button>
              <Button variant="outline" className="w-full justify-start">내 아바타 보기</Button>
              <Button variant="outline" className="w-full justify-start">보유 쿠폰, 아이템함</Button>
              <Button variant="outline" className="w-full justify-start">AI 챗봇 고객센터</Button>
              <Button variant="outline" className="w-full justify-start">이용 가이드</Button>
            </div>
          </SheetContent>
        </Sheet>

        {/* 인사 문구 */}
        <div>
          <h2 className="font-bold text-gray-900 dark:text-gray-100">
            안녕하세요! 👋
          </h2>
          <p className="text-sm text-gray-600 dark:text-gray-400">
            오늘도 알뜰한 소비 하세요
          </p>
        </div>
      </div>

      {/* 오른쪽 영역 */}
      <div className="flex items-center gap-3">

        {/* 다크모드 */}
        <Button
          variant="ghost"
          size="icon"
          onClick={() => setTheme(theme === "dark" ? "light" : "dark")}
        >
          <Sun className="h-5 w-5 dark:hidden" />
          <Moon className="h-5 w-5 hidden dark:block" />
        </Button>

        {/* 알림 */}
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon" className="relative">
              <Bell className="h-5 w-5" />
              {notifications.length > 0 && (
                <span className="absolute right-1 top-1 h-2 w-2 bg-red-500 rounded-full"></span>
              )}
            </Button>
          </SheetTrigger>

          <SheetContent>
            <SheetHeader>
              <SheetTitle>알림</SheetTitle>
            </SheetHeader>

            <div className="mt-6 space-y-4">
              {notifications.map((notif) => (
                <div key={notif.id} className="p-3 border rounded">
                  {notif.text}
                  <div className="text-xs text-gray-400">{notif.time}</div>
                </div>
              ))}
            </div>
          </SheetContent>
        </Sheet>

        {/* 지갑 */}
        <ConnectWalletButton className="hidden sm:flex" />

        {/* 🔥 게임 시작 */}
        <Button className="bg-gradient-to-r from-emerald-500 to-teal-600">
          게임시작
        </Button>

        {/* 🔥 로그아웃 (게임시작 오른쪽) */}
        <Button
          variant="outline"
          onClick={handleLogout}
          className="hidden sm:flex"
        >
          <LogOut className="mr-2 h-4 w-4" />
          로그아웃
        </Button>

      </div>
    </header>
  );
}