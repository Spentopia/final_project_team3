import { useState } from "react";
import { Card } from "@/shared/ui/card";
import { Badge } from "@/shared/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import {
  BookOpen,
  Receipt,
  PenSquare,
  BarChart3,
  ShieldCheck,
  Sparkles,
  Coins,
  Wallet,
  Smartphone,
  Monitor,
  ShoppingBag,
  Tag,
  PiggyBank,
  BrainCircuit,
  Users,
  MessageSquare,
  Store,
  AlertTriangle,
  CheckCircle,
  Trophy,
  Gift,
  CalendarCheck,
  CreditCard,
  FileText,
} from "lucide-react";

// ─── 지갑 연동 탭 내부 앱/웹 서브탭 ──────────────────────────────────────────

function WalletAppGuide() {
  const wallets = [
    {
      name: "Phantom",
      steps: [
        "Google Play에서 Phantom 앱 설치",
        "Spentopia 앱 → 프로필 → 지갑 연동",
        "'Phantom으로 연결' 버튼 탭",
        "Phantom 앱에서 연결 승인",
      ],
    },
    {
      name: "Solflare",
      steps: [
        "Google Play에서 Solflare 앱 설치",
        "Spentopia 앱 → 프로필 → 지갑 연동",
        "'Solflare로 연결' 버튼 탭",
        "Solflare 앱에서 서명 승인",
      ],
    },
    {
      name: "Backpack",
      steps: [
        "Google Play에서 Backpack 앱 설치",
        "Spentopia 앱 → 프로필 → 지갑 연동",
        "'Backpack으로 연결' 버튼 탭",
        "Backpack 앱에서 연결 승인",
      ],
    },
  ];

  return (
    <div className="space-y-4">
      <Card className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
          <p className="text-sm text-gray-600 dark:text-gray-400">
            MWA 방식이라 지갑 앱이 기기에 설치되어 있어야 연결됩니다.
          </p>
        </div>
      </Card>

      <div className="grid gap-4 lg:grid-cols-3">
        {wallets.map((w) => (
          <Card key={w.name} className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
            <p className="mb-4 font-bold text-gray-900 dark:text-gray-100">{w.name}</p>
            <ol className="space-y-3">
              {w.steps.map((s, i) => (
                <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                    {i + 1}
                  </span>
                  {s}
                </li>
              ))}
            </ol>
          </Card>
        ))}
      </div>

      <Card className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
        <p className="mb-3 font-bold text-gray-900 dark:text-gray-100">연동 해제</p>
        <div className="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-400">
          <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
          프로필 → 지갑 연동 섹션 → '연동 해제' 버튼. 해제해도 소비 기록과 SPT 잔액은 유지됩니다.
        </div>
      </Card>
    </div>
  );
}

function WalletWebGuide() {
  const wallets = [
    {
      name: "Phantom (브라우저 확장)",
      steps: [
        "Chrome 웹 스토어에서 Phantom 확장 설치",
        "Phantom에서 Solana 지갑 생성 또는 복구",
        "Spentopia 웹 → 지갑 연결 버튼 클릭",
        "팝업에서 Phantom 선택 후 승인",
      ],
    },
    {
      name: "Solflare (브라우저 확장)",
      steps: [
        "Chrome 웹 스토어에서 Solflare 확장 설치",
        "Solflare에서 지갑 생성 또는 시드 구문으로 복구",
        "Spentopia 웹 → 지갑 연결 버튼 클릭",
        "팝업에서 Solflare 선택 후 승인",
      ],
    },
  ];

  return (
    <div className="space-y-4">
      <Card className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
          <div className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
            <p>
              앱에서 지갑을 연동했더라도 웹에서 Google, Kakao, 이메일 로그인으로 접속한 경우 브라우저 지갑은 별도로 연결해야 합니다.
            </p>
            <p>
              시드 구문(복구 구문)은 절대 타인에게 공유하지 마세요. Spentopia는 시드 구문을 요구하지 않습니다.
            </p>
          </div>
        </div>
      </Card>

      <div className="grid gap-4 lg:grid-cols-2">
        {wallets.map((w) => (
          <Card key={w.name} className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
            <p className="mb-4 font-bold text-gray-900 dark:text-gray-100">{w.name}</p>
            <ol className="space-y-3">
              {w.steps.map((s, i) => (
                <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                  <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                    {i + 1}
                  </span>
                  {s}
                </li>
              ))}
            </ol>
          </Card>
        ))}
      </div>

      <Card className="border-none spentopia-soft-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
        <p className="mb-3 font-bold text-gray-900 dark:text-gray-100">공통 안내</p>
        <div className="space-y-2">
          {[
            "지갑 연결은 서명 요청만 발생하며, 자산이 자동으로 이동하지 않습니다.",
            "웹 결제와 NFT 거래는 현재 브라우저에 연결된 확장 지갑을 기준으로 진행됩니다.",
            "연결된 지갑 주소는 프로필 → 지갑 연동 섹션에서 언제든 해제할 수 있습니다.",
          ].map((t) => (
            <div key={t} className="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-400">
              <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-emerald-500" />
              {t}
            </div>
          ))}
        </div>
      </Card>
    </div>
  );
}

// ─── 메인 페이지 ──────────────────────────────────────────────────────────────

export default function GuidePage() {
  const [walletSub, setWalletSub] = useState<"app" | "web">("app");

  const startSteps = [
    {
      icon: Receipt,
      step: "01",
      title: "소비 기록",
      desc: "홈에서 날짜·금액·카테고리를 입력해 바로 기록하세요.",
      points: ["카테고리로 소비 패턴 분류", "메모로 내용 기억", "과거 날짜 소급 입력 가능"],
    },
    {
      icon: ShieldCheck,
      step: "02",
      title: "영수증 인증",
      desc: "영수증 사진을 올리면 OCR이 날짜·금액을 자동 추출해 검증해요.",
      points: ["인증 성공 시 기록에 인증 마크", "실제 결제 내역 교차 확인", "인증 데이터 분석에 반영"],
    },
    {
      icon: PenSquare,
      step: "03",
      title: "한 줄 소비 일기",
      desc: "기록과 함께 한 줄 일기를 남기면 소비 습관을 돌아볼 수 있어요.",
      points: ["충동구매 여부 셀프 체크", "만족 여부 기록", "나중에 소비 이유 파악"],
    },
    {
      icon: BarChart3,
      step: "04",
      title: "분석 리포트",
      desc: "누적 데이터가 카테고리별 지출과 패턴으로 시각화돼요.",
      points: ["카테고리별 소비 비중", "예산 초과 여부 확인", "주간·월간 흐름 파악"],
    },
  ];

  return (
    <div className="min-h-full w-full p-6">
      <div className="w-full space-y-6">

        {/* 헤더 */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">이용 가이드</h1>
            <p className="text-gray-600 dark:text-gray-400">Spentopia의 모든 기능을 한눈에 확인하세요</p>
          </div>
          <div className="hidden items-center gap-2 lg:flex">
            <Badge className="bg-white text-slate-900 ring-1 ring-slate-200 dark:bg-[#090b16] dark:text-white dark:ring-[#7c3aed]/35">
              <BookOpen className="mr-1 h-3 w-3" />
              가이드
            </Badge>
          </div>
        </div>

        {/* 메인 탭 */}
        <Tabs defaultValue="start">
          <TabsList className="h-auto w-full flex-wrap justify-start gap-1 spentopia-soft-card spentopia-nft-card-tone p-1.5 backdrop-blur-xl">
            {[
              { value: "start",     icon: Sparkles,  label: "시작하기" },
              { value: "reward",    icon: Trophy,    label: "성실도·보상" },
              { value: "wallet",    icon: Wallet,    label: "지갑 연동" },
              { value: "market",    icon: Store,     label: "NFT 마켓" },
              { value: "budget",    icon: PiggyBank, label: "예산·분석" },
              { value: "community", icon: Users,     label: "커뮤니티" },
            ].map(({ value, icon: Icon, label }) => (
              <TabsTrigger key={value} value={value} className="flex items-center gap-1.5 px-4 py-2">
                <Icon className="h-4 w-4" />
                {label}
              </TabsTrigger>
            ))}
          </TabsList>

          {/* ── 시작하기 ── */}
          <TabsContent value="start" className="mt-4 space-y-4">
            <div className="grid gap-4 lg:grid-cols-2">
              {startSteps.map(({ icon: Icon, step, title, desc, points }) => (
                <Card key={step} className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                  <div className="mb-4 flex items-center gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-2xl spentopia-guide-icon">
                      <Icon className="h-5 w-5" />
                    </div>
                    <div>
                      <p className="text-xs font-semibold spentopia-guide-label">STEP {step}</p>
                      <h3 className="font-bold text-gray-900 dark:text-gray-100">{title}</h3>
                    </div>
                  </div>
                  <p className="mb-3 text-sm text-gray-600 dark:text-gray-400">{desc}</p>
                  <ul className="space-y-1.5">
                    {points.map((p) => (
                      <li key={p} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                        <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-slate-400 dark:bg-violet-400" />
                        {p}
                      </li>
                    ))}
                  </ul>
                </Card>
              ))}
            </div>

            <div className="grid gap-4 lg:grid-cols-[1fr_auto]">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <h3 className="mb-4 font-bold text-gray-900 dark:text-gray-100">추천 루틴</h3>
                <div className="flex flex-wrap items-center gap-2">
                  {["소비 기록", "영수증 인증", "한 줄 일기", "분석 확인"].map((s, i, arr) => (
                    <div key={s} className="flex items-center gap-2">
                      <Badge variant="outline" className="spentopia-guide-chip">
                        {s}
                      </Badge>
                      {i < arr.length - 1 && <span className="text-gray-400">→</span>}
                    </div>
                  ))}
                </div>
              </Card>
              <Card className="border-none spentopia-soft-card spentopia-nft-card-tone p-6 text-gray-900 shadow-xl">
                <Coins className="mb-3 h-6 w-6 spentopia-guide-label" />
                <p className="font-bold text-gray-900 dark:text-gray-100">매일 1회</p>
                <p className="mt-1 text-sm text-gray-700 dark:text-gray-200">짧게라도 꾸준히</p>
              </Card>
            </div>
          </TabsContent>

          {/* ── 성실도·보상 ── */}
          <TabsContent value="reward" className="mt-4 space-y-4">
            <div className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-5 flex items-center gap-3">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl spentopia-guide-icon">
                    <Trophy className="h-5 w-5" />
                  </div>
                  <div>
                    <h2 className="font-bold text-gray-900 dark:text-gray-100">월간 성실도 점수</h2>
                    <p className="text-sm text-gray-500 dark:text-gray-400">
                      매월 1일부터 말일까지 기록된 활동을 기준으로 계산돼요
                    </p>
                  </div>
                </div>

                <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-5">
                  {[
                    { label: "소비 기록", score: "30점", desc: "기록한 날짜 수가 많을수록 상승" },
                    { label: "영수증 인증", score: "25점", desc: "인증 완료된 날짜 수 기준" },
                    { label: "한 줄 일기", score: "20점", desc: "소비 기록에 남긴 일기 기준" },
                    { label: "예산 체크", score: "15점", desc: "해당 월 예산 설정 시 지급" },
                    { label: "연속 활동", score: "10점", desc: "현재 스트릭에 따라 보너스" },
                  ].map((item) => (
                    <div key={item.label} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4 dark:border-[#7c3aed]/35">
                      <p className="text-xs font-semibold spentopia-guide-label">{item.label}</p>
                      <p className="mt-1 text-2xl font-extrabold text-gray-900 dark:text-gray-100">{item.score}</p>
                      <p className="mt-2 text-xs leading-relaxed text-gray-500 dark:text-gray-400">{item.desc}</p>
                    </div>
                  ))}
                </div>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <CalendarCheck className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">월말 확정 방식</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "이번 달 점수는 홈에서 실시간으로 확인",
                    "보상은 바로 지급되지 않고 월말 이후 확정",
                    "다음 달 배치에서 지난달 점수 60점 이상 여부 확인",
                    "지급 완료 시 월간 성실도에 보상 지급 완료 표시",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
              </Card>
            </div>

            <div className="grid gap-4 lg:grid-cols-3">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <PiggyBank className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">예산 체크 15점</h3>
                </div>
                <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-400">
                  월 예산은 한 달에 한 번 설정합니다. 해당 월 예산이 등록되어 있으면 성실도 예산 체크 항목 15점이 반영돼요.
                </p>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <Gift className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">보상 기준</h3>
                </div>
                <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-400">
                  월간 성실도 총점이 60점 이상이면 보상 대상이 됩니다. 보상은 같은 달에 중복 지급되지 않아요.
                </p>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <Wallet className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">지갑 연동 여부</h3>
                </div>
                <p className="text-sm leading-relaxed text-gray-600 dark:text-gray-400">
                  지갑을 연동한 유저는 NFT 아바타와 SPT 보상을 받을 수 있고, 지갑 미연동 유저는 교환불가 아바타 보상을 받아요.
                </p>
              </Card>
            </div>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <Coins className="h-5 w-5 spentopia-guide-label" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">SPT 보상 구간</h3>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-5">
                {[
                  { range: "60~69점", amount: "10 SPT" },
                  { range: "70~79점", amount: "20 SPT" },
                  { range: "80~89점", amount: "35 SPT" },
                  { range: "90~99점", amount: "55 SPT" },
                  { range: "100점", amount: "80 SPT" },
                ].map((item) => (
                  <div key={item.range} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4 text-center dark:border-[#7c3aed]/35">
                    <p className="text-xs font-semibold text-gray-500 dark:text-gray-400">{item.range}</p>
                    <p className="mt-1 text-lg font-extrabold text-gray-900 dark:text-gray-100">{item.amount}</p>
                  </div>
                ))}
              </div>
              <p className="mt-4 text-xs text-gray-500 dark:text-gray-400">
                실제 지급량은 서비스 반감기 정책에 따라 조정될 수 있으며, 최소 지급 조건을 만족하면 보상 기록이 생성됩니다.
              </p>
            </Card>
          </TabsContent>

          {/* ── 지갑 연동 ── */}
          <TabsContent value="wallet" className="mt-4 space-y-4">
            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl spentopia-guide-icon">
                  <Wallet className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="font-bold text-gray-900 dark:text-gray-100">지갑 연동</h2>
                  <p className="text-sm text-gray-500 dark:text-gray-400">앱과 웹에서 Solana 지갑을 연결하는 방법이에요</p>
                </div>
              </div>

              {/* 앱 / 웹 서브탭 */}
              <div className="flex gap-2">
                {(["app", "web"] as const).map((sub) => (
                  <button
                    key={sub}
                    onClick={() => setWalletSub(sub)}
                    className={`flex items-center gap-2 rounded-xl px-4 py-2 text-sm font-semibold transition-all ${
                      walletSub === sub
                      ? "bg-white text-gray-900 ring-1 ring-slate-300 shadow-md dark:bg-gradient-to-r dark:from-[#090b16] dark:via-[#4338ca] dark:to-[#7c3aed] dark:text-white dark:ring-transparent"
                        : "bg-white text-gray-700 ring-1 ring-gray-200 hover:bg-gray-50 dark:bg-gray-700/60 dark:text-gray-300 dark:ring-gray-600 dark:hover:bg-gray-700"
                    }`}
                  >
                    {sub === "app" ? <Smartphone className="h-4 w-4" /> : <Monitor className="h-4 w-4" />}
                    {sub === "app" ? "앱 (Android)" : "웹 (브라우저)"}
                  </button>
                ))}
              </div>
            </Card>

            {walletSub === "app" ? <WalletAppGuide /> : <WalletWebGuide />}

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <CreditCard className="h-5 w-5 spentopia-guide-label" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">지갑 결제 안내</h3>
              </div>
              <div className="grid gap-3 md:grid-cols-3">
                {[
                  { label: "연결", desc: "프로필 또는 상단 지갑 버튼에서 Solana 지갑을 연결합니다." },
                  { label: "서명", desc: "NFT 구매나 유료 분석 요청 시 지갑에서 결제 서명을 승인합니다." },
                  { label: "확인", desc: "결제 완료 후 SPT 차감, 구매 내역, 분석 결과가 화면에 반영됩니다." },
                ].map((item) => (
                  <div key={item.label} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4 dark:border-[#7c3aed]/35">
                    <p className="text-xs font-semibold spentopia-guide-label">{item.label}</p>
                    <p className="mt-1 text-sm leading-relaxed text-gray-600 dark:text-gray-400">{item.desc}</p>
                  </div>
                ))}
              </div>
              <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                <p className="text-xs text-gray-700 dark:text-gray-300">
                  결제 요청은 지갑 서명 전에는 실행되지 않습니다. 승인 전 금액과 요청 내용을 꼭 확인하세요.
                </p>
              </div>
            </Card>
          </TabsContent>

          {/* ── NFT 마켓 ── */}
          <TabsContent value="market" className="mt-4 space-y-4">
            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-xl spentopia-guide-icon">
                  <Coins className="h-5 w-5" />
                </div>
                <div>
                  <h2 className="font-bold text-gray-900 dark:text-gray-100">SPT 토큰</h2>
                  <p className="text-sm text-gray-500 dark:text-gray-400">Spentopia 생태계의 기본 보상 토큰</p>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { label: "획득", value: "소비 기록 · 영수증 인증 · 미션" },
                  { label: "사용", value: "NFT 마켓 아바타 구매" },
                  { label: "조회", value: "프로필 → 보유 SPT" },
                ].map((item) => (
                  <div key={item.label} className="rounded-xl spentopia-market-light-soft p-3">
                    <p className="text-xs font-semibold spentopia-guide-label">{item.label}</p>
                    <p className="mt-1 text-sm font-bold text-gray-900 dark:text-gray-100">{item.value}</p>
                  </div>
                ))}
              </div>
            </Card>

            <div className="grid gap-4 lg:grid-cols-2">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <ShoppingBag className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">아바타 구매</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "지갑 연동 완료 후 마켓플레이스 진입",
                    "원하는 아바타 NFT 선택",
                    "'구매하기' 클릭 → 지갑 서명 요청",
                    "승인 시 SPT 차감 + 아바타 지급",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
                <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                  <p className="text-xs text-gray-700 dark:text-gray-300">구매 전 프로필에서 SPT 잔액을 확인하세요.</p>
                </div>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <Tag className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">아바타 판매 등록</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "마켓플레이스 → '내 아이템' 탭",
                    "판매할 아바타 선택 → '판매 등록'",
                    "판매 가격(SPT) 입력 후 등록",
                    "구매 완료 시 SPT가 내 지갑으로 전송",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
                <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                  <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                  <p className="text-xs text-gray-700 dark:text-gray-300">판매 등록된 NFT는 에스크로 컨트랙트에 안전하게 보관됩니다.</p>
                </div>
              </Card>
            </div>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <ShieldCheck className="h-5 w-5 spentopia-guide-label" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">에스크로 거래 흐름</h3>
              </div>
              <div className="grid grid-cols-3 gap-3">
                {[
                  { step: "01", label: "판매 등록", desc: "NFT → 에스크로 컨트랙트" },
                  { step: "02", label: "구매 결제", desc: "SPT → 컨트랙트 예치" },
                  { step: "03", label: "자동 정산", desc: "NFT → 구매자 / SPT → 판매자" },
                ].map((item) => (
                  <div key={item.step} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4">
                    <p className="text-xs font-semibold spentopia-guide-label">STEP {item.step}</p>
                    <p className="mt-1 font-bold text-gray-900 dark:text-gray-100">{item.label}</p>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">{item.desc}</p>
                  </div>
                ))}
              </div>
            </Card>
          </TabsContent>

          {/* ── 예산·분석 ── */}
          <TabsContent value="budget" className="mt-4 space-y-4">
            <div className="grid gap-4 lg:grid-cols-2">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                <PiggyBank className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">월 예산 설정</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "사이드바 → '예산' 메뉴 진입",
                    "월 선택 후 전체 또는 카테고리별 예산 입력",
                    "'저장' 클릭으로 예산 등록",
                    "월간 성실도 예산 체크 15점 반영",
                    "대시보드·분석에서 예산 대비 현황 확인",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <BrainCircuit className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">AI 예산 플랜</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "예산 페이지 → 'AI 플랜 생성' 클릭",
                    "AI가 최근 소비 패턴 분석 후 카테고리별 추천",
                    "추천 금액 그대로 적용하거나 수정",
                    "확정 시 해당 월 예산으로 즉시 반영",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
                <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                  <p className="text-xs text-gray-700 dark:text-gray-300">2주 이상 기록이 쌓일수록 추천 정확도가 올라가요.</p>
                </div>
              </Card>
            </div>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <BarChart3 className="h-5 w-5 spentopia-guide-label" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">분석 리포트</h3>
              </div>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                {[
                  { label: "카테고리 분석", desc: "식비·교통·쇼핑 등 소비 비중 파이 차트" },
                  { label: "기간별 추이", desc: "주간·월간 소비 흐름 그래프" },
                  { label: "예산 대비 현황", desc: "설정 예산 vs 실제 소비 비율" },
                  { label: "AI 채팅 분석", desc: "챗봇에 질문하면 데이터 기반 답변" },
                ].map((item) => (
                  <div key={item.label} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4">
                    <p className="text-sm font-bold text-gray-900 dark:text-gray-100">{item.label}</p>
                    <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">{item.desc}</p>
                  </div>
                ))}
              </div>
            </Card>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
              <div className="mb-4 flex items-center gap-3">
                <FileText className="h-5 w-5 spentopia-guide-label" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">추가 분석 결제</h3>
              </div>
              <div className="grid gap-4 lg:grid-cols-[1fr_1fr_1fr]">
                {[
                  {
                    title: "무료 횟수 사용",
                    desc: "주간·월간 소비 패턴 분석은 정해진 무료 횟수 안에서 먼저 사용할 수 있어요.",
                  },
                  {
                    title: "결제 요청",
                    desc: "무료 횟수를 모두 사용하면 추가 분석 요청 시 지갑 결제 화면이 열립니다.",
                  },
                  {
                    title: "분석 생성",
                    desc: "지갑에서 결제를 승인하면 AI가 현재 소비 데이터와 예산을 기준으로 리포트를 생성합니다.",
                  },
                ].map((item, index) => (
                  <div key={item.title} className="rounded-xl border border-slate-200 spentopia-market-light-soft p-4 dark:border-[#7c3aed]/35">
                    <p className="text-xs font-semibold spentopia-guide-label">STEP {String(index + 1).padStart(2, "0")}</p>
                    <p className="mt-1 font-bold text-gray-900 dark:text-gray-100">{item.title}</p>
                    <p className="mt-2 text-sm leading-relaxed text-gray-600 dark:text-gray-400">{item.desc}</p>
                  </div>
                ))}
              </div>
              <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                <Wallet className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                <p className="text-xs text-gray-700 dark:text-gray-300">
                  추가 분석 결제를 사용하려면 지갑 연동이 필요합니다. 결제 성공 후 분석 결과가 저장되어 다시 확인할 수 있어요.
                </p>
              </div>
            </Card>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
              <div className="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-400">
                <CheckCircle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                월 예산은 한 달에 한 번 설정하는 기준값입니다. 예산을 등록하면 이번 달 성실도 예산 체크 점수가 반영됩니다.
              </div>
            </Card>
          </TabsContent>

          {/* ── 커뮤니티 ── */}
          <TabsContent value="community" className="mt-4 space-y-4">
            <div className="grid gap-3 sm:grid-cols-3">
              {[
                { icon: MessageSquare, label: "게시글", desc: "소비 노하우·절약 팁 자유롭게 공유" },
                { icon: Sparkles,      label: "좋아요",  desc: "유용한 게시글에 공감 표현" },
                { icon: Users,         label: "광장",    desc: "다른 유저 소비 기록 실시간 피드" },
              ].map(({ icon: Icon, label, desc }) => (
                <Card key={label} className="border-none spentopia-surface-card spentopia-nft-card-tone p-5 backdrop-blur-xl">
                  <div className="mb-3 flex h-10 w-10 items-center justify-center rounded-xl spentopia-guide-icon">
                    <Icon className="h-5 w-5" />
                  </div>
                  <p className="font-bold text-gray-900 dark:text-gray-100">{label}</p>
                  <p className="mt-1 text-sm text-gray-500 dark:text-gray-400">{desc}</p>
                </Card>
              ))}
            </div>

            <div className="grid gap-4 lg:grid-cols-2">
              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <MessageSquare className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">게시글 작성</h3>
                </div>
                <ol className="space-y-3">
                  {[
                    "사이드바 → '커뮤니티' 진입",
                    "상단 '글쓰기' 버튼 클릭",
                    "제목·내용 입력 후 등록",
                    "등록 즉시 피드에 노출",
                  ].map((s, i) => (
                    <li key={i} className="flex items-start gap-3 text-sm text-gray-700 dark:text-gray-300">
                      <span className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full spentopia-step-badge text-[10px] font-bold">
                        {i + 1}
                      </span>
                      {s}
                    </li>
                  ))}
                </ol>
              </Card>

              <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
                <div className="mb-4 flex items-center gap-3">
                  <Users className="h-5 w-5 spentopia-guide-label" />
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">광장 (Plaza)</h3>
                </div>
                <ul className="space-y-3">
                  {[
                    "사이드바 → '광장' 진입",
                    "다른 유저의 소비 기록과 한 줄 일기 확인",
                    "좋은 소비 습관 유저에게 동기부여 받기",
                  ].map((t, i) => (
                    <li key={i} className="flex items-center gap-2 text-sm text-gray-700 dark:text-gray-300">
                      <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-slate-400 dark:bg-violet-400" />
                      {t}
                    </li>
                  ))}
                </ul>
                <div className="mt-4 flex items-start gap-2 rounded-lg spentopia-market-light-soft p-3">
                  <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-slate-500 dark:text-violet-300" />
                  <p className="text-xs text-gray-700 dark:text-gray-300">광장에는 닉네임만 공개돼요. 민감한 금액 정보는 직접 공유하지 않도록 주의하세요.</p>
                </div>
              </Card>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </div>
  );
}
