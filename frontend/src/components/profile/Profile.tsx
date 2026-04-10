import { useState } from "react";
import { Card } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Switch } from "../ui/switch";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../ui/tabs";
import { Badge } from "../ui/badge";
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
} from "lucide-react";
import { toast } from "sonner";

export default function Profile() {
  const [isEditing, setIsEditing] = useState(false);
  const [profile, setProfile] = useState({
    name: "홍길동",
    nickname: "길동이",
    email: "hong@example.com",
    phone: "010-1234-5678",
  });

  const [notifications, setNotifications] = useState({
    budget: true,
    reward: true,
    streak: true,
    marketing: false,
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

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">마이페이지</h1>
          <p className="text-gray-600 dark:text-gray-400">내 정보와 설정을 관리하세요</p>
        </div>
      </div>

      <div className="grid gap-6 lg:grid-cols-[300px_1fr]">
        {/* Profile Card */}
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <div className="mb-6 text-center">
            <div className="relative mx-auto mb-4 inline-block">
              <div className="flex h-24 w-24 items-center justify-center rounded-full bg-white/20 text-4xl backdrop-blur-sm">
                😊
              </div>
              <button className="absolute bottom-0 right-0 flex h-8 w-8 items-center justify-center rounded-full bg-white text-purple-600 shadow-lg">
                <Camera className="h-4 w-4" />
              </button>
            </div>
            <h2 className="mb-1 font-bold">{profile.nickname}</h2>
            <p className="text-sm opacity-90">{profile.name}</p>
          </div>

          <div className="space-y-3">
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
          </div>
        </Card>

        {/* Settings */}
        <Tabs defaultValue="profile" className="space-y-6">
          <TabsList>
            <TabsTrigger value="profile">프로필</TabsTrigger>
            <TabsTrigger value="security">보안</TabsTrigger>
            <TabsTrigger value="notifications">알림</TabsTrigger>
            <TabsTrigger value="wallet">지갑</TabsTrigger>
          </TabsList>

          {/* Profile Tab */}
          <TabsContent value="profile">
            <Card className="border-none bg-white/80 dark:bg-gray-800/80 p-6 backdrop-blur-xl">
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

              <div className="space-y-4">
                <div>
                  <Label htmlFor="name">이름</Label>
                  <div className="relative mt-1">
                    <User className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400" />
                    <Input
                      id="name"
                      value={profile.name}
                      onChange={(e) => setProfile({ ...profile, name: e.target.value })}
                      disabled={!isEditing}
                      className="pl-10"
                    />
                  </div>
                </div>

                <div>
                  <Label htmlFor="nickname">닉네임</Label>
                  <Input
                    id="nickname"
                    value={profile.nickname}
                    onChange={(e) => setProfile({ ...profile, nickname: e.target.value })}
                    disabled={!isEditing}
                    className="mt-1"
                  />
                </div>

                <div>
                  <Label htmlFor="email">이메일</Label>
                  <div className="relative mt-1">
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
                  <div className="relative mt-1">
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
              </div>
            </Card>

            {/* Connected Accounts */}
            <Card className="border-none bg-white/80 dark:bg-gray-800/80 p-6 backdrop-blur-xl">
              <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">소셜 계정 연동</h3>
              <div className="space-y-4">
                <div className="flex items-center justify-between rounded-lg border dark:border-gray-700 bg-white dark:bg-gray-900/50 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-yellow-400">
                      <span className="text-lg">💬</span>
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">카카오</p>
                      <p className="text-xs text-gray-600 dark:text-gray-400">
                        {connectedAccounts.kakao ? "연동됨" : "미연동"}
                      </p>
                    </div>
                  </div>
                  {connectedAccounts.kakao ? (
                    <Badge className="bg-green-500">연동됨</Badge>
                  ) : (
                    <Button variant="outline" size="sm">
                      연동
                    </Button>
                  )}
                </div>

                <div className="flex items-center justify-between rounded-lg border dark:border-gray-700 bg-white dark:bg-gray-900/50 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-green-500">
                      <span className="text-lg">N</span>
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">네이버</p>
                      <p className="text-xs text-gray-600 dark:text-gray-400">
                        {connectedAccounts.naver ? "연동됨" : "미연동"}
                      </p>
                    </div>
                  </div>
                  {connectedAccounts.naver ? (
                    <Badge className="bg-green-500">연동됨</Badge>
                  ) : (
                    <Button variant="outline" size="sm">
                      연동
                    </Button>
                  )}
                </div>

                <div className="flex items-center justify-between rounded-lg border dark:border-gray-700 bg-white dark:bg-gray-900/50 p-4">
                  <div className="flex items-center gap-3">
                    <div className="flex h-10 w-10 items-center justify-center rounded-full bg-blue-500">
                      <span className="text-lg">G</span>
                    </div>
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">구글</p>
                      <p className="text-xs text-gray-600 dark:text-gray-400">
                        {connectedAccounts.google ? "연동됨" : "미연동"}
                      </p>
                    </div>
                  </div>
                  {connectedAccounts.google ? (
                    <Badge className="bg-green-500">연동됨</Badge>
                  ) : (
                    <Button variant="outline" size="sm">
                      연동
                    </Button>
                  )}
                </div>
              </div>
            </Card>
          </TabsContent>

          {/* Security Tab */}
          <TabsContent value="security">
            <Card className="border-none bg-white/80 dark:bg-gray-800/80 p-6 backdrop-blur-xl">
              <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">비밀번호 변경</h3>
              <div className="space-y-4">
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

                <Button className="w-full bg-gradient-to-r from-cyan-500 to-blue-500">
                  비밀번호 변경
                </Button>
              </div>
            </Card>
          </TabsContent>

          {/* Notifications Tab */}
          <TabsContent value="notifications">
            <Card className="border-none bg-white/80 dark:bg-gray-800/80 p-6 backdrop-blur-xl">
              <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">알림 설정</h3>
              <div className="space-y-6">
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
                    checked={notifications.budget}
                    onCheckedChange={() => handleNotificationToggle("budget")}
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
                    checked={notifications.reward}
                    onCheckedChange={() => handleNotificationToggle("reward")}
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
                    checked={notifications.streak}
                    onCheckedChange={() => handleNotificationToggle("streak")}
                  />
                </div>

                <div className="flex items-center justify-between">
                  <div className="flex items-start gap-3">
                    <Bell className="mt-1 h-5 w-5 text-cyan-600 dark:text-cyan-400" />
                    <div>
                      <p className="font-bold text-gray-900 dark:text-gray-100">마케팅 알림</p>
                      <p className="text-sm text-gray-600 dark:text-gray-400">
                        이벤트와 프로모션 정보를 받아보세요
                      </p>
                    </div>
                  </div>
                  <Switch
                    checked={notifications.marketing}
                    onCheckedChange={() => handleNotificationToggle("marketing")}
                  />
                </div>
              </div>
            </Card>
          </TabsContent>

          {/* Wallet Tab */}
          <TabsContent value="wallet">
            <WalletSection isLoggedIn isProfileComplete={false} />
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
