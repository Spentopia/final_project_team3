import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Bell, Gamepad2, Menu, Moon, Sun } from "lucide-react";
import { ConnectWalletButton } from "@/domains/wallet/ui/ConnectWalletButton";
import {
  getNotifications,
  markAllNotificationsRead,
  markNotificationRead,
  type NotificationItem,
} from "@/shared/api/notificationApi";
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
import { supabase } from "@/shared/lib/supabase";

type HeaderProps = {
  onMenuClick?: () => void;
};

function formatRelativeTime(createdAt: string | null) {
  if (!createdAt) return "방금 전";

  const date = new Date(createdAt);
  const diffMs = date.getTime() - Date.now();

  if (Number.isNaN(date.getTime())) return "방금 전";

  const minute = 60 * 1000;
  const hour = 60 * minute;
  const day = 24 * hour;
  const rtf = new Intl.RelativeTimeFormat("ko", { numeric: "auto" });

  if (Math.abs(diffMs) < hour) {
    return rtf.format(Math.round(diffMs / minute), "minute");
  }
  if (Math.abs(diffMs) < day) {
    return rtf.format(Math.round(diffMs / hour), "hour");
  }
  return rtf.format(Math.round(diffMs / day), "day");
}

export default function Header({ onMenuClick }: HeaderProps) {
  const { theme, setTheme } = useTheme();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isNotificationSheetOpen, setIsNotificationSheetOpen] = useState(false);
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false);
  const [isMarkingRead, setIsMarkingRead] = useState(false);

  const unreadCount = notifications.filter((notification) => !notification.is_read).length;

  const loadNotifications = useCallback(async () => {
    try {
      setIsLoadingNotifications(true);
      const items = await getNotifications();
      setNotifications(items);
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "알림을 불러오지 못했습니다."
      );
    } finally {
      setIsLoadingNotifications(false);
    }
  }, []);

  useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  useEffect(() => {
    const handleRefresh = () => {
      setTimeout(() => {
        void loadNotifications();
      }, 800);
    };

    window.addEventListener("spentopia:score-refresh", handleRefresh);
    return () => window.removeEventListener("spentopia:score-refresh", handleRefresh);
  }, [loadNotifications]);

  // ── Supabase Realtime: notifications 테이블 INSERT 구독 ────────────────
  //
  // 백엔드가 service_role로 notifications에 INSERT 하면 WAL 이벤트가 발생하고,
  // Supabase Realtime이 WebSocket으로 클라이언트에 전달한다.
  //
  // 필터 user_id=eq.{userId}로 본인 알림만 수신.
  //
  // 알림 도착 시 종 아이콘 옆 빨간 점이 즉시 켜지고,
  // Sheet를 열면 최상단에 새 알림이 들어와 있다.
  useEffect(() => {
    let cancelled = false;
    let channel: ReturnType<typeof supabase.channel> | null = null;

    const setup = async () => {
      const { data, error } = await supabase.auth.getUser();
      if (cancelled) return;
      if (error || !data.user) return;

      const userId = data.user.id;

      channel = supabase
        .channel(`notifications:${userId}`)
        .on(
          "postgres_changes",
          {
            event: "INSERT",
            schema: "public",
            table: "notifications",
            filter: `user_id=eq.${userId}`,
          },
          (payload) => {
            const next = payload.new as NotificationItem;
            if (!next || !next.id) return;

            setNotifications((prev) => {
              if (prev.some((n) => n.id === next.id)) return prev;
              return [next, ...prev];
            });
          },
        )
        .subscribe();
    };

    void setup();

    return () => {
      cancelled = true;
      if (channel) {
        void supabase.removeChannel(channel);
      }
    };
  }, []);

  const handleReadAll = async () => {
    try {
      setIsMarkingRead(true);
      await markAllNotificationsRead();
      setNotifications((prev) =>
        prev.map((notification) => ({ ...notification, is_read: true })),
      );
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "알림 읽음 처리에 실패했습니다."
      );
    } finally {
      setIsMarkingRead(false);
    }
  };

  const handleReadOne = async (notificationId: string) => {
    // optimistic: 읽음 상태만 먼저 반영하고, 실패하면 복구
    const snapshot = notifications;
    setNotifications((prev) =>
      prev.map((notification) =>
        notification.id === notificationId
          ? { ...notification, is_read: true }
          : notification,
      ),
    );
    try {
      await markNotificationRead(notificationId);
    } catch (error) {
      setNotifications(snapshot);
      toast.error(
        error instanceof Error ? error.message : "알림 읽음 처리에 실패했습니다."
      );
    }
  };

  // 게임 실행 버튼 클릭 시
  // 1) 백엔드에서 handoff token 발급
  // 2) 커스텀 프로토콜로 유니티 exe 실행 시도
  // 3) 프론트는 blur / visibilitychange로 실행 시도만 간접 감지
  // 4) 실제 인증 교환은 exe가 /auth/handoff/exchange에서 수행
  const handleStartGame = async () => {
    try {
      await startUnityGame();
    } catch (error) {
      console.error("게임 실행 실패:", error);

      toast.error(
          error instanceof Error ? error.message : "게임 실행에 실패했어요."
      );
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
          <span className="text-[11px] font-bold uppercase tracking-[0.18em] text-[#2563eb] dark:text-luxury-gold">
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

          <Sheet
            open={isNotificationSheetOpen}
            onOpenChange={(open) => {
              setIsNotificationSheetOpen(open);
              if (open) {
                void loadNotifications();
              }
            }}
          >
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="relative">
                <Bell className="h-5 w-5" />
                {unreadCount > 0 && (
                  <span className="absolute right-1 top-1 flex h-2 w-2">
                    <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-red-500 opacity-75" />
                    <span className="relative inline-flex h-2 w-2 rounded-full bg-red-500" />
                  </span>
                )}
              </Button>
            </SheetTrigger>

            <SheetContent>
              <SheetHeader>
                <div className="flex items-center justify-between gap-3">
                  <SheetTitle>알림</SheetTitle>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    disabled={unreadCount === 0 || isMarkingRead}
                    onClick={() => void handleReadAll()}
                  >
                    {isMarkingRead ? "처리 중..." : "모두 읽음"}
                  </Button>
                </div>
              </SheetHeader>

              <div className="mt-6 space-y-4">
                {isLoadingNotifications && (
                  <div className="rounded border border-border p-3 text-sm text-muted-foreground">
                    알림을 불러오는 중입니다.
                  </div>
                )}

                {!isLoadingNotifications && notifications.length === 0 && (
                  <div className="rounded border border-dashed border-border p-4 text-sm text-muted-foreground">
                    아직 도착한 알림이 없습니다.
                  </div>
                )}

                {!isLoadingNotifications &&
                  notifications.map((notification) => (
                    <button
                      type="button"
                      key={notification.id}
                      onClick={() =>
                        !notification.is_read && void handleReadOne(notification.id)
                      }
                      className={[
                        "w-full rounded border p-3 text-left transition-colors",
                        notification.is_read
                          ? "border-border bg-[var(--surface-subtle)] hover:border-border"
                          : "border-luxury-gold/30 bg-luxury-gold/5 hover:border-luxury-gold/60 hover:bg-luxury-gold/10",
                      ].join(" ")}
                      title={notification.is_read ? "읽은 알림" : "클릭하면 읽음 처리됩니다"}
                    >
                      <div className="flex items-start justify-between gap-3">
                        <p className="text-sm text-foreground">{notification.message}</p>
                        {!notification.is_read && (
                          <span className="mt-1 h-2 w-2 shrink-0 rounded-full bg-red-500" />
                        )}
                      </div>
                      <div className="mt-2 text-xs text-gray-400">
                        {formatRelativeTime(notification.created_at)}
                      </div>
                    </button>
                  ))}
              </div>
            </SheetContent>
          </Sheet>

          <ConnectWalletButton className="hidden sm:flex" />
        </div>

        <Button
          variant="outline"
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
