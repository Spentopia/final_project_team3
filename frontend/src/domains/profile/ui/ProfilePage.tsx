import { useEffect, useRef, useState } from "react";
import { useNavigate, useSearchParams } from "react-router";
import AvatarPage from "@/domains/avatar/pages/AvatarPage";
import { syncOwnedNfts, getUserItems } from "@/domains/avatar/api/avatarApi";
import { useSptBalance } from "@/shared/hooks/useSptBalance";
import { withdrawAccount } from "@/domains/auth/api/auth";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import { WalletSection } from "./WalletSection";
import { useProfileImage } from "@/domains/auth/hooks/useProfileImage";
import { uploadProfileImage } from "@/domains/auth/api/profileImage";
import {
  getUserSettings,
  getUserProfile,
  updateUserProfile,
  updateUserSettings,
  changePassword,
  changeEmail,
} from "@/domains/profile/api/profile";
import {
  User,
  Mail,
  Phone,
  Lock,
  Bell,
  Edit,
  Save,
  Camera,
  BadgeCheck,
  Eye,
  EyeOff,
} from "lucide-react";
import { toast } from "sonner";

type NotificationSettings = {
  alertBudget: boolean;
  alertReward: boolean;
  alertStreak: boolean;
  socialActivityAlert: boolean;
};

export default function ProfilePage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const isWebView = searchParams.get("webview") === "true";
  const [isEditing, setIsEditing] = useState(false);
  const [isProfileLoading, setIsProfileLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isWithdrawing, setIsWithdrawing] = useState(false);
  const [showWithdrawConfirm, setShowWithdrawConfirm] = useState(false);
  const [profile, setProfile] = useState({
    nickname: "",
    email: "",
    phone: "",
    bio: "",
    loginProvider: "",
    googleConnected: false,
    createdAt: "",
    sptBalance: 0,
    imagePath: "",
    walletAddress: "",
    currentStreak: 0,
  });

  const { sptBalance, sptLoading } = useSptBalance(profile.walletAddress || null);
  const [nftCount, setNftCount] = useState<number | null>(null);

  const originalEmailRef = useRef("");

  // 카메라 버튼으로 선택한 파일의 로컬 미리보기 (즉시 업로드 전용)
  const [localImagePreview, setLocalImagePreview] = useState<string | null>(null);
  const [isUploadingImage, setIsUploadingImage] = useState(false);

  const [passwordData, setPasswordData] = useState({
    current: "",
    next: "",
    confirm: "",
  });
  const [isChangingPassword, setIsChangingPassword] = useState(false);
  const [showPasswords, setShowPasswords] = useState({
    current: false,
    next: false,
    confirm: false,
  });

  const fileInputRef = useRef<HTMLInputElement | null>(null);

  // 서버에 저장된 기존 이미지 signed URL 로딩용
  const profileImage = useProfileImage({
    initialPath: profile.imagePath || undefined,
  });

  // 로컬 미리보기가 있으면 우선 표시, 없으면 서버 signed URL 표시
  const profileImageSrc = localImagePreview ?? profileImage.previewUrl;

  const [notifications, setNotifications] = useState<NotificationSettings>({
    alertBudget: true,
    alertReward: true,
    alertStreak: true,
    socialActivityAlert: true,
  });
  const [isSavingNotifications, setIsSavingNotifications] = useState(false);

  // 마이페이지 진입 시 NFT sync + 보유 NFT 수 조회
  useEffect(() => {
    void syncOwnedNfts().catch(() => {});
    void getUserItems()
      .then((items) => setNftCount(items.filter((i) => i.is_nft === true).length))
      .catch(() => setNftCount(0));
  }, []);

  useEffect(() => {
    let cancelled = false;

    const loadProfile = async () => {
      try {
        setIsProfileLoading(true);
        const [data, settings] = await Promise.all([
          getUserProfile(),
          getUserSettings(),
        ]);
        if (cancelled) return;

        setProfile((prev) => ({
          ...prev,
          nickname: data.nickname ?? "",
          email: data.email ?? "",
          phone: data.phone ?? "",
          bio: data.introduction ?? "",
          loginProvider: data.login_provider ?? "",
          googleConnected: data.google_connected ?? false,
          createdAt: data.created_at,
          sptBalance: data.spt_balance ?? 0,
          imagePath: data.profile_image ?? "",
          walletAddress: data.wallet_address ?? "",
          currentStreak: data.current_streak ?? 0,
        }));
        setNotifications({
          alertBudget: settings.alert_budget ?? true,
          alertReward: settings.alert_reward ?? true,
          alertStreak: settings.alert_streak ?? true,
          socialActivityAlert: settings.notification_listener ?? true,
        });

        originalEmailRef.current = data.email ?? "";
      } catch {
        if (!cancelled) toast.error("프로필 정보를 불러오지 못했습니다");
      } finally {
        if (!cancelled) setIsProfileLoading(false);
      }
    };

    void loadProfile();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const handleWalletChange = (event: Event) => {
      const walletAddress = (
          event as CustomEvent<{ walletAddress: string | null }>
      ).detail?.walletAddress;
      setProfile((prev) => ({ ...prev, walletAddress: walletAddress ?? "" }));
    };

    window.addEventListener("spentopia:wallet-change", handleWalletChange);
    return () => {
      window.removeEventListener("spentopia:wallet-change", handleWalletChange);
    };
  }, []);

  const isEmailUser = profile.loginProvider === "email";

  // 카메라 버튼 클릭 → 파일 선택 → 즉시 업로드 + DB 저장
  const handleImageChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // 같은 파일 다시 선택 가능하도록 초기화
    e.target.value = "";

    if (!["image/png", "image/jpeg", "image/jpg", "image/webp"].includes(file.type)) {
      toast.error("png, jpg, webp 이미지만 업로드 가능합니다");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      toast.error("파일 크기는 5MB 이하여야 합니다");
      return;
    }

    // 선택 즉시 로컬 미리보기 표시
    const localUrl = URL.createObjectURL(file);
    setLocalImagePreview(localUrl);

    try {
      setIsUploadingImage(true);
      const { path } = await uploadProfileImage(file);
      await updateUserProfile({ profile_image: path });
      setProfile((prev) => ({ ...prev, imagePath: path }));
      toast.success("프로필 사진이 변경되었습니다");
    } catch (error) {
      // 실패 시 로컬 미리보기 되돌리기
      setLocalImagePreview(null);
      toast.error(
          error instanceof Error ? error.message : "이미지 업로드에 실패했습니다"
      );
    } finally {
      setIsUploadingImage(false);
    }
  };

  const handleEditStart = () => {
    originalEmailRef.current = profile.email;
    setIsEditing(true);
  };

  const handleSave = async () => {
    try {
      setIsSaving(true);

      const emailChanged =
          isEmailUser && profile.email.trim() !== originalEmailRef.current;

      // 닉네임/전화번호/이미지만 백엔드로 저장 (이메일은 Supabase 직접 처리)
      const updated = await updateUserProfile({
        nickname: profile.nickname || undefined,
        phone: profile.phone || undefined,
        introduction: profile.bio.trim() ? profile.bio.trim() : null,
      });

      setProfile((prev) => ({
        ...prev,
        nickname: updated.nickname ?? "",
        phone: updated.phone ?? "",
        bio: updated.introduction ?? "",
        loginProvider: updated.login_provider ?? "",
        createdAt: updated.created_at,
        sptBalance: updated.spt_balance ?? 0,
        walletAddress: updated.wallet_address ?? prev.walletAddress,
      }));

      setIsEditing(false);

      if (emailChanged) {
        // Supabase 직접 호출 → 새 이메일로 인증 메일 발송
        await changeEmail(profile.email.trim());
        navigate("/email-confirmed?sent=true");
      } else {
        toast.success("프로필이 저장되었습니다");
      }
    } catch (error) {
      toast.error(
          error instanceof Error ? error.message : "프로필 저장에 실패했습니다"
      );
    } finally {
      setIsSaving(false);
    }
  };

  const handleChangePassword = async () => {
    if (!passwordData.current) {
      toast.error("현재 비밀번호를 입력해주세요");
      return;
    }
    if (!passwordData.next) {
      toast.error("새 비밀번호를 입력해주세요");
      return;
    }
    if (passwordData.next !== passwordData.confirm) {
      toast.error("새 비밀번호가 일치하지 않습니다");
      return;
    }

    try {
      setIsChangingPassword(true);
      await changePassword(passwordData.current, passwordData.next);
      setPasswordData({ current: "", next: "", confirm: "" });
      toast.success("비밀번호가 변경되었습니다");
    } catch (error) {
      toast.error(
          error instanceof Error ? error.message : "비밀번호 변경에 실패했습니다"
      );
    } finally {
      setIsChangingPassword(false);
    }
  };

  const performWithdraw = async () => {
    setShowWithdrawConfirm(false);
    try {
      setIsWithdrawing(true);
      await withdrawAccount();
      toast.success("회원탈퇴가 완료되었습니다");
      navigate("/login", { replace: true });
    } catch (error) {
      toast.error(
        error instanceof Error ? error.message : "회원탈퇴 처리에 실패했습니다"
      );
    } finally {
      setIsWithdrawing(false);
    }
  };

  const handleWithdraw = () => {
    setShowWithdrawConfirm(true);
  };

  const handleNotificationToggle = async (key: keyof typeof notifications) => {
    const nextValue = !notifications[key];
    const nextNotifications = { ...notifications, [key]: nextValue };
    setNotifications(nextNotifications);

    try {
      setIsSavingNotifications(true);
      const updated = await updateUserSettings({
        alert_budget: nextNotifications.alertBudget,
        alert_reward: nextNotifications.alertReward,
        alert_streak: nextNotifications.alertStreak,
        notification_listener: nextNotifications.socialActivityAlert,
      });
      setNotifications({
        alertBudget: updated.alert_budget ?? true,
        alertReward: updated.alert_reward ?? true,
        alertStreak: updated.alert_streak ?? true,
        socialActivityAlert: updated.notification_listener ?? true,
      });
      toast.success("알림 설정이 변경되었습니다");
    } catch (error) {
      setNotifications(notifications);
      toast.error(
        error instanceof Error ? error.message : "알림 설정 저장에 실패했습니다"
      );
    } finally {
      setIsSavingNotifications(false);
    }
  };

  const isProfileComplete = Boolean(profile.nickname && profile.phone);
  const joinedDateLabel = profile.createdAt
      ? new Date(profile.createdAt).toLocaleDateString("ko-KR")
      : "-";

  return (
      <div className={isWebView ? "w-full max-w-full space-y-6 overflow-x-hidden px-0 pb-6" : "space-y-6"}>
        {showWithdrawConfirm && (
          <div
            className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-sm"
            onClick={() => setShowWithdrawConfirm(false)}
          >
            <div
              className="relative w-[420px] rounded-[14px] border border-white/10 bg-white/90 p-5 shadow-2xl backdrop-blur-xl dark:bg-gray-900/90"
              style={{ borderLeft: "3px solid #f79009" }}
              onClick={(e) => e.stopPropagation()}
            >
              <p className="text-sm font-semibold text-gray-900 dark:text-gray-100">
                정말 회원탈퇴하시겠습니까?
              </p>
              <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">
                탈퇴 후에는 계정을 복구할 수 없습니다.
              </p>
              <div className="mt-4 flex justify-end gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setShowWithdrawConfirm(false)}
                >
                  취소
                </Button>
                <Button
                  size="sm"
                  disabled={isWithdrawing}
                  className="bg-red-500 text-white hover:bg-red-600"
                  onClick={() => void performWithdraw()}
                >
                  {isWithdrawing ? "탈퇴 처리 중..." : "탈퇴하기"}
                </Button>
              </div>
            </div>
          </div>
        )}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              내 프로필
            </h1>
            <p className="text-gray-600 dark:text-gray-400">
              내 정보와 설정을 관리하세요
            </p>
          </div>
        </div>

        <div className={isWebView ? "grid gap-4" : "grid gap-6 lg:grid-cols-[340px_1fr] xl:grid-cols-[380px_1fr]"}>
          {/* 왼쪽 프로필 카드 */}
          <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
            {/* 파일 선택 즉시 업로드 */}
            <input
                ref={fileInputRef}
                type="file"
                accept="image/png,image/jpeg,image/jpg,image/webp"
                className="hidden"
                onChange={handleImageChange}
            />
            <div className="mb-6 text-center">
              <div className="relative mx-auto mb-4 inline-block">
                <div className="flex h-24 w-24 items-center justify-center overflow-hidden rounded-full bg-white/20 text-4xl
  backdrop-blur-sm">
                  {profileImageSrc ? (
                      <img
                          src={profileImageSrc}
                          alt={profile.nickname || "프로필 이미지"}
                          className="h-full w-full object-cover"
                      />
                  ) : (
                      "😊"
                  )}
                </div>
                <button
                    type="button"
                    onClick={() => !isUploadingImage && fileInputRef.current?.click()}
                    disabled={isUploadingImage}
                    className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-white text-cyan-600
  shadow-lg disabled:opacity-60"
                >
                  {isUploadingImage ? (
                      <div className="h-4 w-4 animate-spin rounded-full border-2 border-cyan-600 border-t-transparent" />
                  ) : (
                      <Camera className="h-4 w-4" />
                  )}
                </button>
              </div>
              <h2 className="mb-1 font-bold">{profile.nickname || "닉네임 미설정"}</h2>
              <div className="flex items-center justify-center gap-2 text-sm opacity-90">
                <span>{profile.email || "이메일 정보 없음"}</span>
                {isProfileComplete ? <BadgeCheck className="h-4 w-4" /> : null}
              </div>
              {profile.bio ? (
                <p className="mt-3 text-center text-sm leading-6 text-white/90">
                  {profile.bio}
                </p>
              ) : null}
            </div>

            <div className="space-y-5">
              <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
                <p className="mb-1 text-sm opacity-90">가입일</p>
                <p className="font-bold">{joinedDateLabel}</p>
              </div>
              <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
                <p className="mb-1 text-sm opacity-90">연속 기록</p>
                <p className="font-bold">{profile.currentStreak}🔥</p>
              </div>
              <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
                <p className="mb-1 text-sm opacity-90">보유 SPT</p>
                <p className="font-bold">
                  {!profile.walletAddress
                    ? "—"
                    : sptLoading
                    ? "..."
                    : (sptBalance ?? 0).toLocaleString("ko-KR")} SPT
                </p>
              </div>
              <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
                <p className="mb-1 text-sm opacity-90">보유 NFT</p>
                <p className="font-bold">
                  {nftCount === null ? "..." : `${nftCount}개`}
                </p>
              </div>
              <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
                <p className="mb-1 text-sm opacity-90">로그인 방식</p>
                <p className="font-bold uppercase">
                  {profile.loginProvider === "email" && profile.googleConnected
                    ? "EMAIL / GOOGLE"
                    : (profile.loginProvider || "-")}
                </p>
              </div>
            </div>
          </Card>

          <div className="grid gap-6 xl:grid-cols-2">
            {/* 회원 정보 */}
            <Card className="h-full min-h-[500px] border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
              <div className="mb-6 flex items-center justify-between">
                <h3 className="font-bold text-gray-900 dark:text-gray-100">
                  회원 정보
                </h3>
                <div className="flex items-center gap-2">
                  <Button
                      onClick={handleWithdraw}
                      variant="outline"
                      size="sm"
                      disabled={isWithdrawing}
                      className="border-red-200 text-red-600 hover:bg-red-50 hover:text-red-700 dark:border-red-900/60 dark:text-red-400 dark:hover:bg-red-950/30"
                  >
                    {isWithdrawing ? "탈퇴 처리 중..." : "회원탈퇴"}
                  </Button>
                  {!isEditing ? (
                      <Button onClick={handleEditStart} variant="outline" size="sm">
                        <Edit className="mr-2 h-4 w-4" />
                        수정
                      </Button>
                  ) : (
                      <Button
                          onClick={handleSave}
                          disabled={isSaving}
                          className="bg-gradient-to-r from-cyan-500 to-blue-500"
                          size="sm"
                      >
                        <Save className="mr-2 h-4 w-4" />
                        {isSaving ? "저장 중..." : "저장"}
                      </Button>
                  )}
                </div>
              </div>

              <div className="space-y-8">
                <div>
                  <Label htmlFor="nickname">닉네임</Label>
                  <div className="relative mt-2">
                    <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="nickname"
                        value={profile.nickname}
                        onChange={(e) =>
                            setProfile({ ...profile, nickname: e.target.value })
                        }
                        disabled={!isEditing || isProfileLoading}
                        className="pl-10"
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="email">이메일</Label>
                  <div className="relative mt-2">
                    <Mail className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="email"
                        type="email"
                        value={profile.email}
                        onChange={(e) =>
                            setProfile({ ...profile, email: e.target.value })
                        }
                        disabled={!isEditing || isProfileLoading || !isEmailUser}
                        className="pl-10"
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">
                    {isEmailUser
                        ? "변경 시 새 이메일로 인증 메일이 발송됩니다"
                        : "소셜 로그인 계정은 이메일을 변경할 수 없습니다"}
                  </p>
                </div>

                <div>
                  <Label htmlFor="phone">전화번호</Label>
                  <div className="relative mt-2">
                    <Phone className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="phone"
                        value={profile.phone}
                        disabled
                        className="pl-10"
                    />
                  </div>
                  <p className="mt-1 text-xs text-gray-400 dark:text-gray-500">
                    전화번호는 변경할 수 없습니다
                  </p>
                </div>

                <div>
                  <Label htmlFor="bio">한 줄 소개</Label>
                  <div className="relative mt-2">
                    <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="bio"
                        value={profile.bio}
                        onChange={(e) =>
                            setProfile({ ...profile, bio: e.target.value })
                        }
                        disabled={!isEditing}
                        className="pl-10"
                    />
                  </div>
                </div>
              </div>
            </Card>

            {/* 알림 설정 */}
            <Card className="h-full border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
              <h3 className="mb-8 font-bold text-gray-900 dark:text-gray-100">
                알림 설정
              </h3>
              <div className="space-y-10">
                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">
                        예산 초과 알림
                      </p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        예산의 80%를 초과하면 알림을 보내드려요
                      </p>
                    </div>
                  </div>
                  <Switch
                      checked={notifications.alertBudget}
                      disabled={isSavingNotifications}
                      onCheckedChange={() => handleNotificationToggle("alertBudget")}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">
                        보상 획득 알림
                      </p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        SPT나 아바타를 획득하면 알려드려요
                      </p>
                    </div>
                  </div>
                  <Switch
                      checked={notifications.alertReward}
                      disabled={isSavingNotifications}
                      onCheckedChange={() => handleNotificationToggle("alertReward")}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">
                        스트릭 리마인드
                      </p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        오늘 기록하지 않았다면 알려드려요
                      </p>
                    </div>
                  </div>
                  <Switch
                      checked={notifications.alertStreak}
                      disabled={isSavingNotifications}
                      onCheckedChange={() => handleNotificationToggle("alertStreak")}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">
                        게시물/댓글/좋아요 알림
                      </p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        내가 쓴 글과 반응에 대한 커뮤니티 알림을 받아요
                      </p>
                    </div>
                  </div>
                  <Switch
                      checked={notifications.socialActivityAlert}
                      disabled={isSavingNotifications}
                      onCheckedChange={() => handleNotificationToggle("socialActivityAlert")}
                  />
                </div>

              </div>
            </Card>

            {/* 비밀번호 변경 */}
            <Card className="h-full border-none bg-white/80 p-5 backdrop-blur-xl dark:bg-gray-800/80">
              <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">
                비밀번호 변경
              </h3>

              {!isEmailUser && (
                  <p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
                    소셜 로그인 계정은 비밀번호를 변경할 수 없습니다.
                  </p>
              )}

              <div className="space-y-8">
                <div>
                  <Label htmlFor="current-password">현재 비밀번호</Label>
                  <div className="relative mt-1">
                    <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="current-password"
                        type={showPasswords.current ? "text" : "password"}
                        className="pl-10 pr-10"
                        value={passwordData.current}
                        onChange={(e) =>
                            setPasswordData({ ...passwordData, current: e.target.value })
                        }
                        disabled={!isEmailUser}
                    />
                    <button
                        type="button"
                        tabIndex={-1}
                        onClick={() => setShowPasswords((prev) => ({ ...prev, current: !prev.current }))}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 disabled:opacity-40"
                        disabled={!isEmailUser}
                    >
                      {showPasswords.current ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <Label htmlFor="new-password">새 비밀번호</Label>
                  <div className="relative mt-1">
                    <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="new-password"
                        type={showPasswords.next ? "text" : "password"}
                        className="pl-10 pr-10"
                        value={passwordData.next}
                        onChange={(e) =>
                            setPasswordData({ ...passwordData, next: e.target.value })
                        }
                        disabled={!isEmailUser}
                    />
                    <button
                        type="button"
                        tabIndex={-1}
                        onClick={() => setShowPasswords((prev) => ({ ...prev, next: !prev.next }))}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 disabled:opacity-40"
                        disabled={!isEmailUser}
                    >
                      {showPasswords.next ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <div>
                  <Label htmlFor="confirm-password">새 비밀번호 확인</Label>
                  <div className="relative mt-1">
                    <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                        id="confirm-password"
                        type={showPasswords.confirm ? "text" : "password"}
                        className="pl-10 pr-10"
                        value={passwordData.confirm}
                        onChange={(e) =>
                            setPasswordData({ ...passwordData, confirm: e.target.value })
                        }
                        disabled={!isEmailUser}
                    />
                    <button
                        type="button"
                        tabIndex={-1}
                        onClick={() => setShowPasswords((prev) => ({ ...prev, confirm: !prev.confirm }))}
                        className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 disabled:opacity-40"
                        disabled={!isEmailUser}
                    >
                      {showPasswords.confirm ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                    </button>
                  </div>
                </div>

                <Button
                    onClick={handleChangePassword}
                    disabled={isChangingPassword || !isEmailUser}
                    className="mt-1 w-full bg-gradient-to-r from-cyan-500 to-blue-500"
                >
                  {isChangingPassword ? "변경 중..." : "비밀번호 변경"}
                </Button>

                <Card className="bg-gradient-to-br from-cyan-50 to-blue-50 p-4 dark:from-cyan-900/30 dark:to-blue-900/30">
                  <h4 className="mb-2 font-bold text-gray-900 dark:text-gray-100">
                    💡 비밀번호 안내
                  </h4>
                  <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
                    <li>• 특수문자, 영문 대문자, 영문 소문자, 숫자를 포함해야 합니다.</li>
                    <li>• 최소 8자 이상으로 설정해 주세요.</li>
                    <li>• 기존 비밀번호와 다른 조합을 사용하는 것을 권장합니다.</li>
                  </ul>
                </Card>
              </div>
            </Card>

            {/* 지갑 연동 */}
            {!isWebView && (
              <div className="h-full">
                <WalletSection
                    isLoggedIn
                    isProfileComplete={isProfileComplete}
                    linkedWalletAddress={profile.walletAddress}
                    onWalletLinked={(walletAddress) => {
                      setProfile((prev) => ({ ...prev, walletAddress }));
                      window.dispatchEvent(
                          new CustomEvent("spentopia:wallet-change", {
                            detail: { walletAddress },
                          })
                      );
                    }}
                    onWalletUnlinked={() => {
                      setProfile((prev) => ({ ...prev, walletAddress: "" }));
                      window.dispatchEvent(
                          new CustomEvent("spentopia:wallet-change", {
                            detail: { walletAddress: null },
                          })
                      );
                    }}
                />
              </div>
            )}
          </div>
        </div>

        {isWebView && (
          <div className="w-full max-w-full overflow-x-hidden pt-2">
            <AvatarPage />
          </div>
        )}
      </div>
  );
}
