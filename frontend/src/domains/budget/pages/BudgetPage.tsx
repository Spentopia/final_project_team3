import { useState, useEffect } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider"; // ✅ 추가

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Slider } from "@/shared/ui/slider";
import { Badge } from "@/shared/ui/badge";
import {
  Wallet,
  Target,
  TrendingUp,
  Sparkles,
  PiggyBank,
  Coffee,
  Home,
  Car,
  Heart
} from "lucide-react";
import { toast } from "sonner";

const aiPlans = [
  {
    id: 1,
    name: "월 50만원 생활 플랜",
    budget: 500000,
    savings: 50000,
    description: "합리적인 소비와 저축을 위한 균형잡힌 플랜",
    categories: [
      { name: "식비", amount: 150000, icon: Coffee },
      { name: "교통비", amount: 80000, icon: Car },
      { name: "생활비", amount: 120000, icon: Home },
      { name: "여가/취미", amount: 100000, icon: Heart },
      { name: "저축", amount: 50000, icon: PiggyBank },
    ],
  },
  {
    id: 2,
    name: "7년 1억 만들기",
    budget: 400000,
    savings: 150000,
    description: "목표 지향적인 저축 중심 플랜",
    categories: [
      { name: "식비", amount: 100000, icon: Coffee },
      { name: "교통비", amount: 60000, icon: Car },
      { name: "생활비", amount: 90000, icon: Home },
      { name: "여가/취미", amount: 50000, icon: Heart },
      { name: "저축", amount: 150000, icon: PiggyBank },
    ],
  },
  {
    id: 3,
    name: "자유로운 소비 플랜",
    budget: 700000,
    savings: 30000,
    description: "현재의 삶을 즐기면서도 미래를 준비하는 플랜",
    categories: [
      { name: "식비", amount: 200000, icon: Coffee },
      { name: "교통비", amount: 100000, icon: Car },
      { name: "생활비", amount: 200000, icon: Home },
      { name: "여가/취미", amount: 170000, icon: Heart },
      { name: "저축", amount: 30000, icon: PiggyBank },
    ],
  },
];
const STORAGE_KEY = "customBudget";

export default function Budget() {
  const { setBudget } = useFinance();

  // ✅ localStorage에서 불러오기
  const [customBudget, setCustomBudget] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved
      ? JSON.parse(saved)
        : {
          monthly: 0,
          savings: 0,
          food: 0,
          transport: 0,
          living: 0,
          leisure: 0,
        };
  });

  const [selectedPlan, setSelectedPlan] = useState<number | null>(() => {
  const saved = localStorage.getItem("selectedPlan");
  return saved ? Number(saved) : null;
});

  // ✅ ⭐⭐⭐ 여기 추가 (중요 포인트)
  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(customBudget));
  }, [customBudget]);

  // =========================
  // AI 플랜 적용
  // =========================
  const handleApplyPlan = (planId: number) => {
    const plan = aiPlans.find((p) => p.id === planId);
    if (plan) {
      setSelectedPlan(planId);

      setBudget(plan.budget);

      toast.success(
        <div>
          <p className="font-bold">{plan.name} 적용 완료! 🎉</p>
          <p className="text-sm">이제 AI가 당신의 소비를 분석해드려요</p>
        </div>
      );
    }
  };

  const handleSaveCustomBudget = () => {
    setBudget(customBudget.monthly);

    toast.success("맞춤 예산이 저장되었습니다!");
  };

  const totalBudget = customBudget.food + customBudget.transport + customBudget.living + customBudget.leisure;
  const withSavings = totalBudget + customBudget.savings;

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">예산 설정</h1>
          <p className="text-gray-600 dark:text-gray-300">AI가 추천하는 플랜으로 시작하거나 직접 설정해보세요</p>
        </div>
        <Button className="bg-gradient-to-r from-cyan-500 to-blue-500">
          <Sparkles className="mr-2 h-4 w-4" />
          AI 플랜 생성
        </Button>
      </div>

      {/* AI Recommended Plans */}
      <div>
        <h2 className="mb-4 flex items-center gap-2 font-bold text-gray-900">
          <Sparkles className="h-5 w-5 text-cyan-600" />
          AI 추천 플랜
        </h2>
        <div className="grid gap-6 md:grid-cols-3">
          {aiPlans.map((plan) => (
            <Card
              key={plan.id}
              className={`border-2 bg-white/80 p-6 backdrop-blur-xl transition-all ${
                selectedPlan === plan.id
                  ? "border-cyan-500 shadow-xl"
                  : "border-transparent hover:border-cyan-300"
              }`}
            >
              <div className="mb-4">
                <div className="mb-2 flex items-start justify-between">
                  <h3 className="font-bold text-gray-900">{plan.name}</h3>
                  {selectedPlan === plan.id && (
                    <Badge className="bg-cyan-500">적용중</Badge>
                  )}
                </div>
                <p className="text-sm text-gray-600">{plan.description}</p>
              </div>

              <div className="mb-4 space-y-3">
                <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-purple-50 to-pink-50 p-3">
                  <span className="text-sm font-medium text-gray-700">월 예산</span>
                  <span className="font-bold text-gray-900">{plan.budget.toLocaleString()}원</span>
                </div>
                <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 p-3">
                  <span className="text-sm font-medium text-gray-700">목표 저축</span>
                  <span className="font-bold text-green-700">{plan.savings.toLocaleString()}원</span>
                </div>
              </div>

              <div className="mb-4 space-y-2">
                {plan.categories.map((cat) => {
                  const Icon = cat.icon;
                  return (
                    <div key={cat.name} className="flex items-center justify-between text-sm">
                      <div className="flex items-center gap-2">
                        <Icon className="h-4 w-4 text-gray-500" />
                        <span className="text-gray-700">{cat.name}</span>
                      </div>
                      <span className="font-medium text-gray-900">
                        {cat.amount.toLocaleString()}원
                      </span>
                    </div>
                  );
                })}
              </div>

              <Button
                onClick={() => handleApplyPlan(plan.id)}
                variant={selectedPlan === plan.id ? "default" : "outline"}
                className={`w-full ${
                  selectedPlan === plan.id
                    ? "bg-gradient-to-r from-cyan-500 to-blue-500"
                    : ""
                }`}
              >
                {selectedPlan === plan.id ? "적용됨" : "이 플랜 적용하기"}
              </Button>
            </Card>
          ))}
        </div>
      </div>

      {/* Custom Budget Setting */}
      <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
        <h2 className="mb-6 flex items-center gap-2 font-bold text-gray-900">
          <Target className="h-5 w-5 text-cyan-600" />
          맞춤 예산 설정
        </h2>

        <div className="grid gap-8 lg:grid-cols-2">
          {/* Left: Input Controls */}
          <div className="space-y-6">
            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label>월 예산</Label>
                <span className="font-bold text-gray-900">
                  {customBudget.monthly.toLocaleString()}원
                </span>
              </div>
              <Input
                type="text"
                inputMode="numeric"
                value={customBudget.monthly === 0 ? "" : String(customBudget.monthly)}
                onChange={(e) => {
                  const nextValue = Number(e.target.value.replace(/[^0-9]/g, ""));
                  setCustomBudget({
                    ...customBudget,
                    monthly: Number.isNaN(nextValue) ? 0 : nextValue,
                  });
                }}
                className="mt-1"
                placeholder="월 예산을 입력해주세요."
              />
            </div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label>저축 목표</Label>
                <span className="font-bold text-green-700">
                  {customBudget.savings.toLocaleString()}원
                </span>
              </div>
              <Slider
                value={[customBudget.savings]}
                onValueChange={([value]) => setCustomBudget({ ...customBudget, savings: value })}
                min={0}
                max={customBudget.monthly}
                step={10000}
                className="mb-2"
              />
            </div>

            <div className="h-px bg-gray-200"></div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label className="flex items-center gap-2">
                  <Coffee className="h-4 w-4" />
                  식비
                </Label>
                <span className="font-bold text-gray-900">
                  {customBudget.food.toLocaleString()}원
                </span>
              </div>
              <Slider
                value={[customBudget.food]}
                onValueChange={([value]) => setCustomBudget({ ...customBudget, food: value })}
                min={0}
                max={10000000}
                step={10000}
              />
            </div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label className="flex items-center gap-2">
                  <Car className="h-4 w-4" />
                  교통비
                </Label>
                <span className="font-bold text-gray-900">
                  {customBudget.transport.toLocaleString()}원
                </span>
              </div>
              <Slider
                value={[customBudget.transport]}
                onValueChange={([value]) => setCustomBudget({ ...customBudget, transport: value })}
                min={0}
                max={10000000}
                step={10000}
              />
            </div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label className="flex items-center gap-2">
                  <Home className="h-4 w-4" />
                  생활비
                </Label>
                <span className="font-bold text-gray-900">
                  {customBudget.living.toLocaleString()}원
                </span>
              </div>
              <Slider
                value={[customBudget.living]}
                onValueChange={([value]) => setCustomBudget({ ...customBudget, living: value })}
                min={0}
                max={10000000}
                step={10000}
              />
            </div>

            <div>
              <div className="mb-3 flex items-center justify-between">
                <Label className="flex items-center gap-2">
                  <Heart className="h-4 w-4" />
                  여가/취미
                </Label>
                <span className="font-bold text-gray-900">
                  {customBudget.leisure.toLocaleString()}원
                </span>
              </div>
              <Slider
                value={[customBudget.leisure]}
                onValueChange={([value]) => setCustomBudget({ ...customBudget, leisure: value })}
                min={0}
                max={10000000}
                step={10000}
              />
            </div>

            <Button
              onClick={handleSaveCustomBudget}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500"
            >
              설정 저장
            </Button>
          </div>

          {/* Right: Summary */}
          <div className="space-y-6">
            <div className="rounded-2xl bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white">
              <h3 className="mb-4 font-bold">예산 요약</h3>
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <span>월 예산</span>
                  <span className="font-bold">{customBudget.monthly.toLocaleString()}원</span>
                </div>
                <div className="h-px bg-white/30"></div>
                <div className="flex items-center justify-between">
                  <span>총 지출 예정</span>
                  <span className="font-bold">{totalBudget.toLocaleString()}원</span>
                </div>
                <div className="flex items-center justify-between">
                  <span>저축 목표</span>
                  <span className="font-bold">{customBudget.savings.toLocaleString()}원</span>
                </div>
                <div className="h-px bg-white/30"></div>
                <div className="flex items-center justify-between text-lg">
                  <span>합계</span>
                  <span className="font-bold">{withSavings.toLocaleString()}원</span>
                </div>
                {withSavings > customBudget.monthly && (
                  <div className="mt-3 rounded-lg bg-red-500/20 p-3 text-sm">
                    ⚠️ 예산이 수입을 {(withSavings - customBudget.monthly).toLocaleString()}원 초과했어요!
                  </div>
                )}
                {withSavings <= customBudget.monthly && (
                  <div className="mt-3 rounded-lg bg-green-500/20 p-3 text-sm">
                    ✅ 균형잡힌 예산이에요! 남은 금액: {(customBudget.monthly - withSavings).toLocaleString()}원
                  </div>
                )}
              </div>
            </div>

            <Card className="p-6">
              <h3 className="mb-4 font-bold text-gray-900">AI 분석</h3>
              <div className="space-y-3 text-sm text-gray-700">
                <div className="flex items-start gap-2">
                  <TrendingUp className="mt-0.5 h-4 w-4 shrink-0 text-green-600" />
                  <p>식비 비중이 적정 수준이에요. 건강한 소비 습관이에요!</p>
                </div>
                <div className="flex items-start gap-2">
                  <Wallet className="mt-0.5 h-4 w-4 shrink-0 text-blue-600" />
                  <p>
                    저축 비율이{" "}
                    {customBudget.monthly > 0
                      ? Math.round((customBudget.savings / customBudget.monthly) * 100)
                      : 0}
                    %예요. 목표 달성 가능해요!
                  </p>
                </div>
                <div className="flex items-start gap-2">
                  <PiggyBank className="mt-0.5 h-4 w-4 shrink-0 text-cyan-600" />
                  <p>
                    {customBudget.savings > 0
                      ? `이 속도면 ${Math.round(10000000 / (customBudget.savings * 12))}년 후 1천만원을 모을 수 있어요!`
                      : "저축 목표를 입력하면 목표 달성 기간을 계산해드려요!"}
                  </p>
                </div>
              </div>
            </Card>

            <Card className="bg-gradient-to-br from-amber-50 to-yellow-50 p-6">
              <h4 className="mb-3 font-bold text-gray-900">💡 절약 팁</h4>
              <ul className="space-y-2 text-sm text-gray-700">
                <li>• 식비는 외식을 줄이면 월 5만원 절약 가능해요</li>
                <li>• 대중교통 정기권으로 교통비 20% 절감하세요</li>
                <li>• 구독 서비스를 정리하면 월 3만원 아낄 수 있어요</li>
              </ul>
            </Card>
          </div>
        </div>
      </Card>
    </div>
  );
}
