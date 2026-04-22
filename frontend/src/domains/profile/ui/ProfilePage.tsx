import { useState } from "react";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Switch } from "@/shared/ui/switch";
import { Badge } from "@/shared/ui/badge";
import { WalletSection } from "./WalletSection";
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
} from "lucide-react";
import { toast } from "sonner";

type NotificationSettings = {
  alertBudget: boolean;
  alertReward: boolean;
  alertStreak: boolean;
  notificationListener: boolean;
};

export default function ProfilePage() {
  const [isEditing, setIsEditing] = useState(false);
  const [profile, setProfile] = useState({
    nickname: "길동이",
    email: "hong@example.com",
    phone: "010-1234-5678",
    loginProvider: "kakao",
  });

  const [notifications, setNotifications] = useState<NotificationSettings>({
    alertBudget: true,
    alertReward: true,
    alertStreak: true,
    notificationListener: false,
  });

  const [connectedAccounts] = useState({
    kakao: true,
    naver: false,
    google: true,
  });

  const handleSave = () => {
    setIsEditing(false);
    toast.success("프로필이 저장되었습니다");
  };

  const handleNotificationToggle = (key: keyof typeof notifications) => {
    setNotifications({ ...notifications, [key]: !notifications[key] });
    toast.success("알림 설정이 변경되었습니다");
  };

  const isProfileComplete = Boolean(profile.nickname && profile.phone);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">내 프로필</h1>
          <p className="text-gray-600 dark:text-gray-400">내 정보와 설정을 관리하세요</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[340px_1fr] xl:grid-cols-[380px_1fr]">
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <div className="mb-6 text-center">
            <div className="relative mx-auto mb-4 inline-block">
              <div className="flex h-24 w-24 items-center justify-center rounded-full bg-white/20 text-4xl backdrop-blur-sm">
                😊
              </div>
              <button className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-white text-cyan-600 shadow-lg">
                <Camera className="h-4 w-4" />
              </button>
            </div>
            <h2 className="mb-1 font-bold">{profile.nickname}</h2>
            <div className="flex items-center justify-center gap-2 text-sm opacity-90">
              <span>{profile.email}</span>
              {isProfileComplete ? <BadgeCheck className="h-4 w-4" /> : null}
            </div>
          </div>

          <div className="space-y-5">
            <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
              <p className="mb-1 text-sm opacity-90">가입일</p>
              <p className="font-bold">2026년 4월 1일</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
              <p className="mb-1 text-sm opacity-90">연속 기록</p>
              <p className="font-bold">7일 🔥</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
              <p className="mb-1 text-sm opacity-90">보유 SPT</p>
              <p className="font-bold">1,250 SPT</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
              <p className="mb-1 text-sm opacity-90">보유 아바타</p>
              <p className="font-bold">15개</p>
            </div>
            <div className="rounded-lg bg-white/10 p-3 backdrop-blur-sm">
              <p className="mb-1 text-sm opacity-90">로그인 방식</p>
              <p className="font-bold uppercase">{profile.loginProvider}</p>
            </div>
          </div>
        </Card>

        <div className="grid gap-6 xl:grid-cols-2">
          <Card className="h-full border-none min-h-[500px] bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
            <div className="mb-6 flex items-center justify-between">
              <h3 className="font-bold text-gray-900 dark:text-gray-100">회원 정보</h3>
              {!isEditing ? (
                <Button onClick={() => setIsEditing(true)} variant="outline" size="sm">
                  <Edit className="mr-2 h-4 w-4" />
                  수정
                </Button>
              ) : (
                <Button
                  onClick={handleSave}
                  className="bg-gradient-to-r from-cyan-500 to-blue-500"
                  size="sm"
                >
                  <Save className="mr-2 h-4 w-4" />
                  저장
                </Button>
              )}
            </div>

            <div className="space-y-8">
              <div>
                <Label htmlFor="nickname">닉네임</Label>
                <div className="relative mt-2">
                  <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input
                    id="nickname"
                    value={profile.nickname}
                    onChange={(e) => setProfile({ ...profile, nickname: e.target.value })}
                    disabled={!isEditing}
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
                    onChange={(e) => setProfile({ ...profile, email: e.target.value })}
                    disabled={!isEditing}
                    className="pl-10"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="phone">전화번호</Label>
                <div className="relative mt-2">
                  <Phone className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input
                    id="phone"
                    value={profile.phone}
                    onChange={(e) => setProfile({ ...profile, phone: e.target.value })}
                    disabled={!isEditing}
                    className="pl-10"
                  />
                </div>
              </div>

              <div>
                <Label htmlFor="nickname">한 줄 소개</Label>
                <div className="relative mt-2">
                  <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input
                      id="nickname"
                      value={profile.nickname}
                      onChange={(e) => setProfile({ ...profile, nickname: e.target.value })}
                      disabled={!isEditing}
                      className="pl-10"
                  />
                </div>
              </div>

            </div>
          </Card>

          <Card className="h-full border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
            <h3 className="mb-8 font-bold text-gray-900 dark:text-gray-100">알림 설정</h3>
            <div className="space-y-10">
              <div className="flex items-center justify-between">
                <div className="flex items-start gap-3">
                  <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                  <div>
                    <p className="font-bold text-gray-900 dark:text-gray-100">예산 초과 알림</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      예산의 80%를 초과하면 알림을 보내드려요
                    </p>
                  </div>
                </div>
                <Switch
                  checked={notifications.alertBudget}
                  onCheckedChange={() => handleNotificationToggle("alertBudget")}
                />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-start gap-3">
                  <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                  <div>
                    <p className="font-bold text-gray-900 dark:text-gray-100">보상 획득 알림</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">SPT나 아바타를 획득하면 알려드려요</p>
                  </div>
                </div>
                <Switch
                  checked={notifications.alertReward}
                  onCheckedChange={() => handleNotificationToggle("alertReward")}
                />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-start gap-3">
                  <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                  <div>
                    <p className="font-bold text-gray-900 dark:text-gray-100">스트릭 리마인드</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      오늘 기록하지 않았다면 알려드려요
                    </p>
                  </div>
                </div>
                <Switch
                  checked={notifications.alertStreak}
                  onCheckedChange={() => handleNotificationToggle("alertStreak")}
                />
              </div>

              <div className="flex items-center justify-between">
                <div className="flex items-start gap-3">
                  <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                  <div>
                    <p className="font-bold text-gray-900 dark:text-gray-100">알림 리스너 동의</p>
                    <p className="text-sm text-gray-600 dark:text-gray-400">
                      카드/은행 알림을 자동 소비 기록에 활용합니다
                    </p>
                  </div>
                </div>
                <Switch
                  checked={notifications.notificationListener}
                  onCheckedChange={() => handleNotificationToggle("notificationListener")}
                />
              </div>
            </div>
          </Card>

          <Card className="h-full border-none bg-white/80 p-5 backdrop-blur-xl dark:bg-gray-800/80">
            <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100 ">비밀번호 변경</h3>
            <div className="space-y-8">
              <div>
                <Label htmlFor="current-password">현재 비밀번호</Label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input id="current-password" type="password" className="pl-10" />
                </div>
              </div>

              <div>
                <Label htmlFor="new-password">새 비밀번호</Label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input id="new-password" type="password" className="pl-10" />
                </div>
              </div>

              <div>
                <Label htmlFor="confirm-password">새 비밀번호 확인</Label>
                <div className="relative mt-1">
                  <Lock className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                  <Input id="confirm-password" type="password" className="pl-10" />
                </div>
              </div>

              <Button className="mt-1 w-full bg-gradient-to-r from-cyan-500 to-blue-500">
                비밀번호 변경
              </Button>

              <Card className="bg-gradient-to-br from-cyan-50 to-blue-50 p-4 dark:from-cyan-900/30 dark:to-blue-900/30">
                <h4 className="mb-2 font-bold text-gray-900 dark:text-gray-100">💡 비밀번호 안내</h4>
                <ul className="space-y-1 text-sm text-gray-700 dark:text-gray-300">
                  <li>• 특수문자, 영문 대문자, 영문 소문자, 숫자를 포함해야 합니다.</li>
                  <li>• 최소 8자 이상으로 설정해 주세요.</li>
                  <li>• 기존 비밀번호와 다른 조합을 사용하는 것을 권장합니다.</li>
                </ul>
              </Card>


            </div>
          </Card>

          <div className="h-full">
            <WalletSection isLoggedIn isProfileComplete={isProfileComplete} />
          </div>
        </div>
      </div>
    </div>
  );
}
