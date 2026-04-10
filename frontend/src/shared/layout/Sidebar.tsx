import { Link, useLocation } from "react-router";
import { 
  Calendar, 
  BarChart3, 
  Wallet, 
  Store, 
  User, 
  Users, 
  Gamepad2,
  Sparkles
} from "lucide-react";

const menuItems = [
  { path: "/", icon: Calendar, label: "가계부" },
  { path: "/budget", icon: Wallet, label: "예산 설정" },
  { path: "/analytics", icon: BarChart3, label: "소비 분석" },
  { path: "/avatar", icon: Sparkles, label: "내 아바타" },
  { path: "/marketplace", icon: Store, label: "NFT 마켓" },
  { path: "/plaza", icon: Gamepad2, label: "광장" },
  { path: "/community", icon: Users, label: "커뮤니티" },
  { path: "/profile", icon: User, label: "마이페이지" },
];

export default function Sidebar() {
  const location = useLocation();

  return (
    <aside className="flex w-64 flex-col border-r border-white/50 dark:border-gray-700/50 bg-white/80 dark:bg-gray-900/80 backdrop-blur-xl">
      {/* Logo */}
      <div className="flex h-16 items-center gap-3 border-b border-white/50 dark:border-gray-700/50 px-6">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-500">
          <Sparkles className="h-6 w-6 text-white" />
        </div>
        <div>
          <h1 className="font-bold text-gray-900 dark:text-gray-100">Spentopia</h1>
          <p className="text-xs text-gray-500 dark:text-gray-400">소비를 자산으로</p>
        </div>
      </div>

      {/* Navigation */}
      <nav className="flex-1 space-y-1 overflow-y-auto p-4">
        {menuItems.map((item) => {
          const Icon = item.icon;
          const isActive = location.pathname === item.path;
          
          return (
            <Link
              key={item.path}
              to={item.path}
              className={`flex items-center gap-3 rounded-xl px-4 py-3 transition-all ${
                isActive
                  ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white shadow-lg shadow-cyan-200 dark:shadow-cyan-900/50"
                  : "text-gray-700 dark:text-gray-300 hover:bg-white/60 dark:hover:bg-gray-800/60"
              }`}
            >
              <Icon className="h-5 w-5" />
              <span className="font-medium">{item.label}</span>
            </Link>
          );
        })}
      </nav>

      {/* Footer */}
      <div className="border-t border-white/50 dark:border-gray-700/50 p-4">
        <div className="rounded-xl bg-gradient-to-br from-cyan-100 to-blue-100 dark:from-cyan-900/30 dark:to-blue-900/30 p-4">
          <p className="mb-2 font-bold text-gray-900 dark:text-gray-100">이번 주 성실도</p>
          <div className="mb-2 h-2 overflow-hidden rounded-full bg-white dark:bg-gray-700">
            <div className="h-full w-[85%] bg-gradient-to-r from-cyan-500 to-blue-500"></div>
          </div>
          <p className="text-sm text-gray-600 dark:text-gray-400">85점 - 거의 다 왔어요! 🎉</p>
        </div>
      </div>
    </aside>
  );
}