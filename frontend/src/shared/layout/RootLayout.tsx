import { Outlet, useNavigate } from "react-router";
import { useEffect, useState } from "react";
import Sidebar from "./Sidebar";
import Header from "./Header";
import { MessageCircle } from "lucide-react";
import { Button } from "../ui/button";
import { toast } from "sonner";
import { authStorage } from "@/shared/lib/auth";

export default function RootLayout() {
  const navigate = useNavigate();
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [isSidebarOpen, setIsSidebarOpen] = useState(false);

  useEffect(() => {
    if (!authStorage.isLoggedIn()) {
      setIsAuthenticated(false);
      navigate("/login");
    } else {
      setIsAuthenticated(true);
    }
  }, [navigate]);

  const handleChatbotClick = () => {
    toast.info("AI챗바타 기능은 곧 제공될 예정입니다!");
  };

  if (!isAuthenticated) {
    return null;
  }

  return (
    <div className="flex h-screen w-full overflow-hidden bg-gradient-to-br from-cyan-50 via-blue-50 to-teal-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
      <div className="hidden lg:block">
        <Sidebar />
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
        <Sidebar />
      </div>

      <div className="flex flex-1 flex-col overflow-hidden">
        <Header onMenuClick={() => setIsSidebarOpen(!isSidebarOpen)} />
        <main className="flex-1 overflow-auto p-6">
          <Outlet />
        </main>
      </div>

      <Button
        onClick={handleChatbotClick}
        className="fixed bottom-8 right-8 z-30 h-20 w-20 rounded-full bg-gradient-to-br from-cyan-500 to-blue-600 shadow-2xl transition-all hover:scale-110 hover:from-cyan-600 hover:to-blue-700 hover:shadow-cyan-500/50 dark:shadow-cyan-900/50"
      >
        <div className="flex flex-col items-center gap-1">
          <MessageCircle className="h-8 w-8" />
          <span className="text-[10px] font-bold">AI챗바타</span>
        </div>
      </Button>
    </div>
  );
}
