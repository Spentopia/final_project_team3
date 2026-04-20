import {
  BookOpen,
  Receipt,
  PenSquare,
  BarChart3,
  ShieldCheck,
  Sparkles,
  Coins,
  ChevronRight,
} from "lucide-react";

const guideSteps = [
  {
    icon: Receipt,
    title: "소비 기록 작성",
    description:
      "날짜, 금액, 카테고리, 메모를 입력해서 오늘의 소비를 빠르게 기록할 수 있어요.",
    details: [
      "날짜를 선택하거나 직접 입력해서 기록 가능",
      "카테고리를 선택해 소비 패턴 분류",
      "메모를 남겨서 나중에 쉽게 회상 가능",
    ],
  },
  {
    icon: ShieldCheck,
    title: "영수증 인증",
    description:
      "영수증 이미지를 업로드하면 OCR로 구매 내역을 확인하고 인증 상태를 반영할 수 있어요.",
    details: [
      "실제 결제 내역 확인에 활용",
      "소비 기록의 신뢰도 향상",
      "추후 보상/미션 기능과 연계 가능",
    ],
  },
  {
    icon: PenSquare,
    title: "한줄 소비 일기",
    description:
      "오늘의 소비에 대해 짧게 기록하면서 감정과 소비 습관을 함께 돌아볼 수 있어요.",
    details: [
      "충동구매였는지 스스로 체크",
      "만족한 소비인지 기록",
      "나만의 소비 패턴 회고 가능",
    ],
  },
  {
    icon: BarChart3,
    title: "분석 리포트 확인",
    description:
      "누적된 소비 데이터를 바탕으로 카테고리별 지출과 패턴을 한눈에 분석할 수 있어요.",
    details: [
      "카테고리별 소비 비중 확인",
      "예산 초과 여부 체크",
      "주간/월간 흐름 파악",
    ],
  },
];

const quickTips = [
  "영수증 인증까지 함께 하면 기록의 정확도가 높아져요.",
  "한줄 일기를 같이 남기면 나중에 소비 이유를 파악하기 쉬워요.",
  "분석 페이지를 주기적으로 보면 불필요한 지출을 줄이는 데 도움이 돼요.",
];

export default function GuidePage() {
  return (
    <div className="min-h-full bg-gradient-to-br from-cyan-50 via-white to-blue-50 p-6 dark:from-gray-950 dark:via-gray-900 dark:to-cyan-950/30">
      <div className="mx-auto max-w-6xl space-y-6">
        {/* Hero */}
        <section className="overflow-hidden rounded-3xl border border-white/60 bg-white/80 p-8 shadow-xl backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
          <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
            <div className="max-w-3xl">
              <div className="mb-4 inline-flex items-center gap-2 rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 px-4 py-1.5 text-sm font-semibold text-white shadow-lg">
                <BookOpen className="h-4 w-4" />
                Spentopia 이용가이드
              </div>

              <h1 className="text-2xl font-bold tracking-tight text-gray-900 dark:text-gray-100 sm:text-3xl">
                소비를 기록하고,
                <br className="hidden sm:block" />
                더 똑똑하게 관리하는 방법
              </h1>

              <p className="mt-4 max-w-2xl text-base leading-7 text-gray-600 dark:text-gray-300">
                Spentopia는 단순한 가계부를 넘어서, 소비 기록 · 영수증 인증 ·
                소비 일기 · 분석 리포트를 하나로 연결해주는 서비스예요.
                아래 순서대로 따라가면 처음 사용하는 사람도 쉽게 적응할 수 있어요.
              </p>
            </div>

            <div className="grid grid-cols-2 gap-4 lg:w-[320px]">
              <div className="rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 p-5 text-white shadow-lg">
                <div className="text-sm opacity-90">핵심 기능</div>
                <div className="mt-2 text-2xl font-bold">4단계</div>
                <div className="mt-1 text-sm opacity-90">기록부터 분석까지</div>
              </div>

              <div className="rounded-2xl border border-cyan-100 bg-cyan-50 p-5 dark:border-cyan-900/40 dark:bg-cyan-950/30">
                <div className="text-sm text-gray-500 dark:text-gray-400">추천 습관</div>
                <div className="mt-2 text-2xl font-bold text-gray-900 dark:text-gray-100">
                  매일 1회
                </div>
                <div className="mt-1 text-sm text-gray-600 dark:text-gray-300">
                  짧게라도 꾸준히
                </div>
              </div>
            </div>
          </div>
        </section>

        {/* Quick summary cards */}
        <section className="grid grid-cols-1 gap-4 md:grid-cols-3">
          <div className="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-md backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
            <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-500 text-white">
              <Receipt className="h-5 w-5" />
            </div>
            <h2 className="font-bold text-gray-900 dark:text-gray-100">기록 중심</h2>
            <p className="mt-2 text-sm leading-6 text-gray-600 dark:text-gray-300">
              소비를 빠짐없이 남기면 데이터가 쌓이고, 그게 곧 분석의 기반이 돼요.
            </p>
          </div>

          <div className="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-md backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
            <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-500 text-white">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <h2 className="font-bold text-gray-900 dark:text-gray-100">인증 강화</h2>
            <p className="mt-2 text-sm leading-6 text-gray-600 dark:text-gray-300">
              영수증 인증으로 소비 기록의 정확도를 높이고 신뢰도 있는 관리가 가능해요.
            </p>
          </div>

          <div className="rounded-2xl border border-white/60 bg-white/80 p-5 shadow-md backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
            <div className="mb-3 flex h-11 w-11 items-center justify-center rounded-xl bg-gradient-to-br from-cyan-500 to-blue-500 text-white">
              <BarChart3 className="h-5 w-5" />
            </div>
            <h2 className="font-bold text-gray-900 dark:text-gray-100">분석 연결</h2>
            <p className="mt-2 text-sm leading-6 text-gray-600 dark:text-gray-300">
              누적된 기록은 소비 분석, 예산 관리, 향후 보상 시스템과 연결될 수 있어요.
            </p>
          </div>
        </section>

        {/* Step guide */}
        <section className="rounded-3xl border border-white/60 bg-white/80 p-6 shadow-xl backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
          <div className="mb-6 flex items-center gap-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 text-white">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <h2 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                단계별 이용 방법
              </h2>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                처음 사용하는 사람도 쉽게 따라올 수 있도록 순서대로 정리했어요.
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 gap-5 xl:grid-cols-2">
            {guideSteps.map((step, index) => {
              const Icon = step.icon;

              return (
                <div
                  key={step.title}
                  className="group rounded-2xl border border-cyan-100/70 bg-gradient-to-br from-white to-cyan-50/60 p-6 shadow-sm transition-all hover:-translate-y-1 hover:shadow-lg dark:border-cyan-900/30 dark:from-gray-900 dark:to-cyan-950/20"
                >
                  <div className="mb-4 flex items-start justify-between gap-4">
                    <div className="flex items-center gap-4">
                      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 text-white shadow-md">
                        <Icon className="h-5 w-5" />
                      </div>
                      <div>
                        <div className="text-sm font-semibold text-cyan-600 dark:text-cyan-400">
                          STEP {index + 1}
                        </div>
                        <h3 className="text-lg font-bold text-gray-900 dark:text-gray-100">
                          {step.title}
                        </h3>
                      </div>
                    </div>

                    <ChevronRight className="h-5 w-5 text-gray-300 transition-transform group-hover:translate-x-1 dark:text-gray-600" />
                  </div>

                  <p className="text-sm leading-6 text-gray-600 dark:text-gray-300">
                    {step.description}
                  </p>

                  <ul className="mt-4 space-y-2">
                    {step.details.map((detail) => (
                      <li
                        key={detail}
                        className="flex items-start gap-2 text-sm text-gray-700 dark:text-gray-300"
                      >
                        <span className="mt-1 h-2 w-2 rounded-full bg-gradient-to-r from-cyan-500 to-blue-500" />
                        <span>{detail}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              );
            })}
          </div>
        </section>

        {/* Tips + reward */}
        <section className="grid grid-cols-1 gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <div className="rounded-3xl border border-white/60 bg-white/80 p-6 shadow-xl backdrop-blur-xl dark:border-gray-700/50 dark:bg-gray-900/70">
            <h2 className="text-xl font-bold text-gray-900 dark:text-gray-100">
              이용 팁
            </h2>
            <p className="mt-2 text-sm text-gray-600 dark:text-gray-400">
              더 잘 활용하려면 아래 팁을 같이 기억해두면 좋아요.
            </p>

            <div className="mt-5 space-y-3">
              {quickTips.map((tip, index) => (
                <div
                  key={tip}
                  className="flex items-start gap-3 rounded-2xl border border-cyan-100 bg-cyan-50/70 p-4 dark:border-cyan-900/30 dark:bg-cyan-950/20"
                >
                  <div className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-gradient-to-r from-cyan-500 to-blue-500 text-sm font-bold text-white">
                    {index + 1}
                  </div>
                  <p className="text-sm leading-6 text-gray-700 dark:text-gray-300">
                    {tip}
                  </p>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-3xl bg-gradient-to-br from-cyan-500 to-blue-600 p-6 text-white shadow-xl">
            <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-white/20">
              <Coins className="h-6 w-6" />
            </div>

            <h2 className="mt-5 text-2xl font-bold">기록이 쌓일수록 가치도 커져요</h2>
            <p className="mt-3 text-sm leading-6 text-white/90">
              Spentopia는 단순한 입력 화면이 아니라, 소비 습관을 자산처럼 관리하는
              경험을 목표로 해요. 꾸준한 기록은 앞으로 더 큰 분석과 보상으로 이어질 수 있어요.
            </p>

            <div className="mt-6 rounded-2xl bg-white/15 p-4 backdrop-blur-sm">
              <div className="text-sm text-white/80">추천 루틴</div>
              <div className="mt-1 text-lg font-bold">기록 → 인증 → 일기 → 분석 확인</div>
            </div>
          </div>
        </section>
      </div>
    </div>
  );
}