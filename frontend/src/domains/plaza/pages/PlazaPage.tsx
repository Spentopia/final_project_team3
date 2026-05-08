import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Gamepad2, Users, Crown, Sparkles, Play } from "lucide-react";

export default function Plaza() {
  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">광장</h1>
          <p className="text-gray-600 dark:text-gray-300">아바타와 함께 다른 유저들을 만나보세요</p>
        </div>
        <Badge className="bg-amber-500">PC 전용</Badge>
      </div>

      {/* Unity WebGL Info */}
      <Card className="border-none bg-gradient-to-br from-slate-900 via-cyan-600 to-teal-500 p-8 text-center text-white shadow-xl shadow-cyan-700/20 backdrop-blur-xl">
        <Gamepad2 className="mx-auto mb-4 h-16 w-16" />
        <h2 className="mb-2 text-2xl font-bold">Unity WebGL 광장</h2>
        <p className="mb-6 opacity-90">
          PC 웹에서 Unity 기반의 3D 가상 공간을 체험하세요
        </p>
        <Button size="lg" variant="secondary">
          <Play className="mr-2 h-5 w-5" />
          광장 입장하기
        </Button>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Features */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-4 font-bold text-gray-900">광장 기능</h3>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-cyan-100">
                <Users className="h-5 w-5 text-cyan-600" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900">아바타 이동 & 채팅</h4>
                <p className="text-sm text-gray-600">
                  내 아바타를 움직이며 다른 유저들과 실시간 채팅을 즐겨보세요
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-pink-100">
                <Sparkles className="h-5 w-5 text-pink-600" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900">커스터마이징 반영</h4>
                <p className="text-sm text-gray-600">
                  내 아바타에 적용한 모든 아이템이 3D로 표현돼요
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-amber-100">
                <Crown className="h-5 w-5 text-amber-600" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900">프리미엄 공간</h4>
                <p className="text-sm text-gray-600">
                  특별한 칭호와 전용 부스를 획득할 수 있어요
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* System Requirements */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-4 font-bold text-gray-900">시스템 요구사항</h3>
          <div className="space-y-3">
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900">브라우저</h4>
              <p className="text-sm text-gray-600">Chrome, Edge, Firefox (최신 버전)</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900">운영체제</h4>
              <p className="text-sm text-gray-600">Windows 10 이상, macOS 10.15 이상</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900">메모리</h4>
              <p className="text-sm text-gray-600">최소 4GB RAM (8GB 권장)</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900">그래픽</h4>
              <p className="text-sm text-gray-600">WebGL 2.0 지원</p>
            </div>
          </div>

          <div className="mt-4 rounded-lg bg-blue-50 p-3 text-sm text-blue-700">
            💡 모바일에서는 광장 기능을 이용할 수 없습니다
          </div>
        </Card>

        {/* Online Users */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-4 font-bold text-gray-900">현재 접속 중</h3>
          <div className="mb-4 flex items-center gap-2">
            <div className="h-3 w-3 animate-pulse rounded-full bg-green-500"></div>
            <span className="font-bold text-gray-900">124명</span>
            <span className="text-sm text-gray-600">접속 중</span>
          </div>
          <div className="space-y-2">
            {[
              { name: "절약왕", avatar: "💰", status: "광장 중앙" },
              { name: "패션왕", avatar: "👗", status: "프리미엄 존" },
              { name: "목표달성", avatar: "🎯", status: "채팅 중" },
              { name: "알뜰맨", avatar: "🏃", status: "광장 입구" },
            ].map((user, i) => (
              <div key={i} className="flex items-center justify-between rounded-lg bg-gray-50 p-3">
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{user.avatar}</span>
                  <span className="font-medium text-gray-900">{user.name}</span>
                </div>
                <Badge variant="outline" className="text-xs">
                  {user.status}
                </Badge>
              </div>
            ))}
          </div>
        </Card>

        {/* Tips */}
        <Card className="border-none bg-gradient-to-br from-purple-50 to-pink-50 p-6 backdrop-blur-xl">
          <h3 className="mb-4 font-bold text-gray-900">광장 이용 팁</h3>
          <ul className="space-y-2 text-sm text-gray-700">
            <li>• WASD 키로 아바타를 움직일 수 있어요</li>
            <li>• 다른 유저 클릭 시 1:1 채팅이 가능해요</li>
            <li>• 특정 구역에서는 미니게임을 즐길 수 있어요</li>
            <li>• 성실도 점수가 높으면 특별한 공간이 열려요</li>
            <li>• 친구 추가 기능으로 함께 즐겨보세요</li>
          </ul>
        </Card>
      </div>

      {/* Coming Soon Features */}
      <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
        <h3 className="mb-4 font-bold text-gray-900">곧 추가될 기능</h3>
        <div className="grid gap-4 md:grid-cols-3">
          <div className="rounded-lg border-2 border-dashed border-cyan-300 bg-cyan-50 p-4 text-center">
            <Sparkles className="mx-auto mb-2 h-8 w-8 text-cyan-600" />
            <h4 className="mb-1 font-bold text-gray-900">미니게임</h4>
            <p className="text-sm text-gray-600">다양한 미니게임으로 SPT 획득</p>
          </div>
          <div className="rounded-lg border-2 border-dashed border-pink-300 bg-pink-50 p-4 text-center">
            <Users className="mx-auto mb-2 h-8 w-8 text-pink-600" />
            <h4 className="mb-1 font-bold text-gray-900">길드 시스템</h4>
            <p className="text-sm text-gray-600">친구들과 길드를 만들어보세요</p>
          </div>
          <div className="rounded-lg border-2 border-dashed border-amber-300 bg-amber-50 p-4 text-center">
            <Crown className="mx-auto mb-2 h-8 w-8 text-amber-600" />
            <h4 className="mb-1 font-bold text-gray-900">이벤트 홀</h4>
            <p className="text-sm text-gray-600">특별 이벤트 전용 공간</p>
          </div>
        </div>
      </Card>
    </div>
  );
}
