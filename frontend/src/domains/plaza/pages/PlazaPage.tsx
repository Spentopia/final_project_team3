import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Gamepad2, Users, Crown, Sparkles, Play } from "lucide-react";
import styles from "./PlazaPage.module.css";

export default function Plaza() {
  // 1. 🚨 구글 드라이브에서 공유한 ZIP 파일의 ID를 여기에 입력하세요!
  const fileId = '1GB_v4G2FHY7dYIjAWLXZsNxpi5kCfKGh';

  // 2. 대용량 바이러스 경고창을 강제로 패스하는 다이렉트 다운로드 주소 조합
  const bypassUrl = `https://drive.google.com/uc?export=download&confirm=t&id=${fileId}`;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">광장</h1>
          <p className="text-gray-600 dark:text-gray-300">아바타와 함께 다른 유저들을 만나보세요</p>
        </div>
        <Badge className="bg-white text-slate-900 ring-1 ring-slate-200 dark:bg-[#090b16] dark:text-white dark:ring-[#7c3aed]/35">PC 전용</Badge>
      </div>

      {/* Unity WebGL Info */}
      <Card className={`${styles.heroCard} border-none spentopia-market-card-view p-8 text-center backdrop-blur-xl`}>
        <div className={styles.heroDecorPlanet} aria-hidden="true" />
        <div className={styles.heroDecorStar} aria-hidden="true">✦</div>
        <div className={styles.heroDecorStarAlt} aria-hidden="true">✧</div>
        <div className={styles.heroDecorSpark} aria-hidden="true">✦</div>
        <div className={styles.heroDecorCloud} aria-hidden="true" />
        <div className={styles.heroDecorCloudAlt} aria-hidden="true" />

        <div className={styles.heroContent}>
          <div className={`${styles.heroIconCircle} mx-auto mb-4 flex h-16 w-16 items-center justify-center`}>
            <Gamepad2 className="h-16 w-16 dark:text-violet-300" />
          </div>
          <h2 className="mb-2 text-2xl font-bold text-gray-900 dark:text-gray-100">Unity WebGL 광장</h2>
          <p className="mb-6 text-gray-700 dark:text-gray-300">
            PC 웹에서 Unity 기반의 3D 가상 공간을 체험하세요
          </p>
          <Button
              size="lg"
              variant="secondary"
              className={`${styles.plazaEntryButton} spentopia-plaza-entry-button`}
              asChild
          >
            <a href={bypassUrl}>
              <Play className="mr-2 h-5 w-5" />
              다운로드
            </a>
          </Button>
        </div>
      </Card>

      <div className="grid gap-6 md:grid-cols-2">
        {/* Features */}
        <Card className={`${styles.plazaCard} ${styles.plazaCardMedium} border-none spentopia-market-card-view p-6 backdrop-blur-xl`}>
          <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">게임 기능</h3>
          <div className="space-y-4">
            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-[#0f172a]">
                <Users className="h-5 w-5 text-slate-700 dark:text-violet-300" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">아바타 이동 & 채팅</h4>
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  내 아바타를 움직이며 다른 유저들과 실시간 채팅을 즐겨보세요
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-[#0f172a]">
                <Sparkles className="h-5 w-5 text-slate-700 dark:text-violet-300" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">커스터마이징 반영</h4>
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  내 아바타에 적용한 모든 아이템이 3D로 표현돼요
                </p>
              </div>
            </div>

            <div className="flex items-start gap-3">
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 dark:bg-[#0f172a]">
                <Crown className="h-5 w-5 text-slate-700 dark:text-violet-300" />
              </div>
              <div>
                <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">프리미엄 공간</h4>
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  특별한 칭호와 전용 부스를 획득할 수 있어요
                </p>
              </div>
            </div>
          </div>
        </Card>

        {/* System Requirements */}
        <Card className={`${styles.plazaCard} ${styles.plazaCardMedium} border-none spentopia-market-card-view p-6 backdrop-blur-xl`}>
          <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">시스템 요구사항</h3>
          <div className="space-y-3">
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900 dark:text-gray-100">브라우저</h4>
              <p className="text-sm text-gray-700 dark:text-gray-300">Chrome, Edge, Firefox (최신 버전)</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900 dark:text-gray-100">운영체제</h4>
              <p className="text-sm text-gray-700 dark:text-gray-300">Windows 10 이상, macOS 10.15 이상</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900 dark:text-gray-100">메모리</h4>
              <p className="text-sm text-gray-700 dark:text-gray-300">최소 4GB RAM (8GB 권장)</p>
            </div>
            <div>
              <h4 className="mb-1 font-bold text-sm text-gray-900 dark:text-gray-100">그래픽</h4>
              <p className="text-sm text-gray-700 dark:text-gray-300">WebGL 2.0 지원</p>
            </div>
          </div>

          <div className="mt-4 rounded-lg bg-slate-50 p-3 text-sm text-slate-700 dark:bg-violet-950/20 dark:text-violet-200">
            💡 모바일에서는 광장 기능을 이용할 수 없습니다
          </div>
        </Card>

        {/* Online Users */}
        <Card className={`${styles.plazaCard} ${styles.plazaCardSoft} border-none spentopia-market-card-view p-6 backdrop-blur-xl`}>
          <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">현재 접속 중</h3>
          <div className="mb-4 flex items-center gap-2">
            <div className="h-3 w-3 animate-pulse rounded-full bg-green-500"></div>
            <span className="font-bold text-gray-900 dark:text-gray-100">124명</span>
            <span className="text-sm text-gray-700 dark:text-gray-300">접속 중</span>
          </div>
          <div className="space-y-2">
            {[
              { name: "절약왕", avatar: "💰", status: "광장 중앙" },
              { name: "패션왕", avatar: "👗", status: "프리미엄 존" },
              { name: "목표달성", avatar: "🎯", status: "채팅 중" },
              { name: "알뜰맨", avatar: "🏃", status: "광장 입구" },
            ].map((user, i) => (
              <div key={i} className={`${styles.plazaInnerCard} flex items-center justify-between rounded-lg spentopia-market-card-view p-3`}>
                <div className="flex items-center gap-2">
                  <span className="text-2xl">{user.avatar}</span>
                  <span className="font-medium text-gray-900 dark:text-gray-100">{user.name}</span>
                </div>
                <Badge variant="outline" className="text-xs text-gray-700 dark:text-gray-200">
                  {user.status}
                </Badge>
              </div>
            ))}
          </div>
        </Card>

        {/* Tips */}
        <Card className={`${styles.plazaCard} ${styles.plazaCardSoft} border-none spentopia-market-card-view p-6 backdrop-blur-xl`}>
          <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">광장 이용 팁</h3>
          <ul className="space-y-2 text-sm text-gray-700 dark:text-gray-300">
            <li>• WASD 키로 아바타를 움직일 수 있어요</li>
            <li>• 다른 유저 클릭 시 1:1 채팅이 가능해요</li>
            <li>• 특정 구역에서는 미니게임을 즐길 수 있어요</li>
            <li>• 성실도 점수가 높으면 특별한 공간이 열려요</li>
            <li>• 친구 추가 기능으로 함께 즐겨보세요</li>
          </ul>
        </Card>
      </div>

      {/* Coming Soon Features */}
      <Card className={`${styles.plazaCard} ${styles.plazaCardComing} border-none spentopia-market-card-view p-6 backdrop-blur-xl`}>
        <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">곧 추가될 기능</h3>
        <div className="grid gap-4 md:grid-cols-3">
          <div className={`${styles.plazaInnerCard} rounded-lg border-2 border-dashed border-slate-200 spentopia-market-card-view p-4 text-center`}>
            <Sparkles className="mx-auto mb-2 h-8 w-8 text-slate-700 dark:text-violet-300" />
            <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">미니게임</h4>
            <p className="text-sm text-gray-700 dark:text-gray-300">다양한 미니게임으로 SPT 획득</p>
          </div>
          <div className={`${styles.plazaInnerCard} rounded-lg border-2 border-dashed border-slate-200 spentopia-market-card-view p-4 text-center`}>
            <Users className="mx-auto mb-2 h-8 w-8 text-slate-700 dark:text-violet-300" />
            <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">길드 시스템</h4>
            <p className="text-sm text-gray-700 dark:text-gray-300">친구들과 길드를 만들어보세요</p>
          </div>
          <div className={`${styles.plazaInnerCard} rounded-lg border-2 border-dashed border-slate-200 spentopia-market-card-view p-4 text-center`}>
            <Crown className="mx-auto mb-2 h-8 w-8 text-slate-700 dark:text-violet-300" />
            <h4 className="mb-1 font-bold text-gray-900 dark:text-gray-100">이벤트 홀</h4>
            <p className="text-sm text-gray-700 dark:text-gray-300">특별 이벤트 전용 공간</p>
          </div>
        </div>
      </Card>
    </div>
  );
}
