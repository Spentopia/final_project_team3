import { useCallback, useEffect, useState } from "react";
import { toast } from "sonner";
import { Bell, Gamepad2, Menu, Moon, Sun, RefreshCw } from "lucide-react";
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
  SheetDescription,
  SheetTrigger,
} from "../ui/sheet";
import { useTheme } from "next-themes";
import { createGameLoginCode } from "@/domains/unity/api/gameLoginCode";
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
  const [authUserId, setAuthUserId] = useState<string | null>(null);
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isNotificationSheetOpen, setIsNotificationSheetOpen] = useState(false);
  const [isLoadingNotifications, setIsLoadingNotifications] = useState(false);
  const [isMarkingRead, setIsMarkingRead] = useState(false);

  // ─────────────────────────────────────────────
  // Unity 게임 로그인 코드 상태
  // ─────────────────────────────────────────────
  //
  // 기존에는 웹에서 Steam/Unity를 자동 실행하면서 handoff token을 넘기려 했지만,
  // Steam 실행 인자 전달이 불안정해서 사용자가 직접 입력하는 코드 방식으로 변경했다.
  //
  // gameLoginCode:
  // - 백엔드 /auth/handoff에서 발급받은 8자리 1회용 코드.
  // - Unity 로그인 화면에 사용자가 직접 입력한다.
  //
  // gameLoginExpiresIn:
  // - 코드 유효시간. 현재 백엔드 기준 60초.
  //
  // isGameCodeSheetOpen:
  // - 코드 표시 Sheet 열림 여부.
  //
  // isCreatingGameLoginCode:
  // - 코드 생성 요청 중 로딩 상태.
  const [gameLoginCode, setGameLoginCode] = useState<string | null>(null);
  const [gameLoginExpiresIn, setGameLoginExpiresIn] = useState<number | null>(null);
  const [gameLoginRemainingSeconds, setGameLoginRemainingSeconds] =
      useState<number | null>(null);
  const [isGameCodeSheetOpen, setIsGameCodeSheetOpen] = useState(false);
  const [isCreatingGameLoginCode, setIsCreatingGameLoginCode] = useState(false);

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
    let cancelled = false;

    const syncUser = async () => {
      const { data } = await supabase.auth.getUser();
      if (!cancelled) {
        setAuthUserId(data.user?.id ?? null);
      }
    };

    void syncUser();

    const { data } = supabase.auth.onAuthStateChange((_event, session) => {
      setAuthUserId(session?.user.id ?? null);
      setNotifications([]);
      if (session?.user) {
        void loadNotifications();
      }
    });

    return () => {
      cancelled = true;
      data.subscription.unsubscribe();
    };
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
      if (!authUserId) return;

      channel = supabase
        .channel(`notifications:${authUserId}`)
        .on(
          "postgres_changes",
          {
            event: "INSERT",
            schema: "public",
            table: "notifications",
            filter: `user_id=eq.${authUserId}`,
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
  }, [authUserId]);

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

  // 게임 로그인 코드 생성 버튼 클릭 시
  //
  // 기존:
  // - 웹에서 Steam/Unity 자동 실행
  // - handoff token을 실행 인자로 전달
  //
  // 변경:
  // - 웹에서는 8자리 게임 로그인 코드만 생성
  // - 사용자는 Steam 라이브러리에서 게임을 직접 실행
  // - Unity 로그인 화면에 이 코드를 입력
  // - Unity가 /auth/handoff/exchange로 코드를 교환해 access/refresh를 받음
  const handleCreateGameLoginCode = async () => {
    try {
      setIsCreatingGameLoginCode(true);

      const result = await createGameLoginCode();

      setGameLoginCode(result.code);
      setGameLoginExpiresIn(result.expiresIn);
      setGameLoginRemainingSeconds(result.expiresIn);
      setIsGameCodeSheetOpen(true);

      toast.success("게임 로그인 코드가 생성되었습니다.");
    } catch (error) {
      console.error("게임 로그인 코드 생성 실패:", error);

      toast.error(
          error instanceof Error
              ? error.message
              : "게임 로그인 코드를 생성하지 못했습니다.",
      );
    } finally {
      setIsCreatingGameLoginCode(false);
    }
  };

  // 게임 로그인 코드 남은 시간 카운트다운.
  //
  // gameLoginRemainingSeconds가 null이면 타이머를 돌리지 않는다.
  // 0이 되면 코드가 만료된 것으로 보고 화면에는 만료 상태를 보여준다.
  useEffect(() => {
    if (gameLoginRemainingSeconds == null) {
      return;
    }

    if (gameLoginRemainingSeconds <= 0) {
      return;
    }

    const timer = window.setInterval(() => {
      setGameLoginRemainingSeconds((prev) => {
        if (prev == null) {
          return null;
        }

        return Math.max(prev - 1, 0);
      });
    }, 1000);

    return () => window.clearInterval(timer);
  }, [gameLoginRemainingSeconds]);

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
                <SheetDescription className="sr-only">알림</SheetDescription>
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

        <Sheet
            open={isGameCodeSheetOpen}
            onOpenChange={(open) => {
              setIsGameCodeSheetOpen(open);

              if (!open) {
                setGameLoginCode(null);
                setGameLoginExpiresIn(null);
                setGameLoginRemainingSeconds(null);
              }
            }}
        >
          <SheetTrigger asChild>
            <Button
                variant="outline"
                onClick={(event) => {
                  // SheetTrigger는 기본적으로 클릭 시 Sheet를 열려고 한다.
                  // 하지만 우리는 코드 생성 성공 후에만 Sheet를 열고 싶으므로
                  // 기본 열림 동작을 막고, handleCreateGameLoginCode에서 직접 연다.
                  event.preventDefault();
                  void handleCreateGameLoginCode();
                }}
                disabled={isCreatingGameLoginCode}
                className="h-14 rounded-lg border border-orange-300 bg-gradient-to-r from-orange-400 via-amber-500 to-orange-600 px-8 text-lg font-extrabold text-white shadow-xl shadow-orange-500/40 transition-all hover:-translate-y-0.5 hover:scale-[1.03] hover:from-orange-300 hover:via-amber-400 hover:to-orange-500 hover:shadow-orange-500/60 disabled:cursor-not-allowed disabled:opacity-70 dark:border-orange-300/70 dark:shadow-orange-950/60"
            >
              <Gamepad2 className="mr-3 h-6 w-6" />
              <span>
        {isCreatingGameLoginCode ? "코드 생성 중..." : "게임 코드 생성"}
      </span>
            </Button>
          </SheetTrigger>

          <SheetContent>
            <SheetHeader>
              <SheetTitle>게임 로그인 코드</SheetTitle>
              <SheetDescription className="sr-only">게임 코드</SheetDescription>
            </SheetHeader>

            <div className="mt-6 space-y-5">
              <div className="relative rounded-2xl border border-cyan-200 bg-cyan-50 p-5 text-center dark:border-cyan-900/60 dark:bg-cyan-950/30">
                {/* 코드 재생성 버튼 */}
                <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    onClick={() => void handleCreateGameLoginCode()}
                    disabled={isCreatingGameLoginCode}
                    title="코드 다시 생성"
                    aria-label="코드 다시 생성"
                    className="absolute right-3 top-1.5 h-9 w-9 rounded-full text-cyan-700 hover:bg-cyan-100 disabled:cursor-not-allowed disabled:opacity-60 dark:text-cyan-300 dark:hover:bg-cyan-900/40"
                >
                  <RefreshCw
                      className={[
                        "h-4 w-4",
                        isCreatingGameLoginCode ? "animate-spin" : "",
                      ].join(" ")}
                  />
                </Button>

                <div className="mt-6 rounded-2xl bg-white px-4 py-6 shadow-sm dark:bg-gray-900/80">
                  <p
                      className={[
                        "select-all break-all text-4xl font-black tracking-[0.25em]",
                        gameLoginRemainingSeconds === 0
                            ? "text-gray-400 line-through dark:text-gray-500"
                            : "text-cyan-600 dark:text-cyan-300",
                      ].join(" ")}
                  >
                    {gameLoginCode ?? "--------"}
                  </p>
                </div>

                <p
                    className={[
                      "mt-4 text-sm font-bold",
                      gameLoginRemainingSeconds === 0
                          ? "text-rose-600 dark:text-rose-300"
                          : "text-cyan-700 dark:text-cyan-300",
                    ].join(" ")}
                >
                  {gameLoginRemainingSeconds === 0
                      ? "코드가 만료되었습니다. 새 코드를 생성해 주세요."
                      : `남은 시간: ${gameLoginRemainingSeconds ?? gameLoginExpiresIn ?? 60}초`}
                </p>
              </div>

              <div className="rounded-xl border border-border bg-[var(--surface-subtle)] p-4 text-sm leading-6 text-muted-foreground">
                <p className="font-semibold text-foreground">사용 방법</p>

                <ol className="mt-2 list-decimal space-y-1 pl-5">
                  <li>Steam 라이브러리에서 Spentopia 게임을 실행합니다.</li>
                  <li>게임 로그인 화면의 입력창에 위 코드를 입력합니다.</li>
                  <li>인증이 완료되면 게임에서 유저 정보와 아이템 정보를 불러옵니다.</li>
                </ol>
              </div>
            </div>
          </SheetContent>
        </Sheet>
      </div>
    </header>
  );
}
