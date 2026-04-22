import { useState, useEffect } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";

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
  Heart,
} from "lucide-react";
import { toast } from "sonner";

const STORAGE_KEY = "customBudget";
const SELECTED_PLAN_KEY = "selectedPlan";

const getIcon = (name: string) => {
  if (name.includes("식")) return Coffee;
  if (name.includes("교통")) return Car;
  if (name.includes("생활")) return Home;
  if (name.includes("여가")) return Heart;
  if (name.includes("저축")) return PiggyBank;
  return Coffee;
};

export default function BudgetPage() {
  const { budgets, setMonthlyBudget } = useFinance();

  const [aiPlans, setAiPlans] = useState<any[]>([]);
const [loading, setLoading] = useState(false);

  const [customBudget, setCustomBudget] = useState(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) {
      return {
        monthly: 0,
        savings: 0,
        food: 0,
        transport: 0,
        living: 0,
        leisure: 0,
      };
    }

    try {
      return JSON.parse(saved);
    } catch {
      return {
        monthly: 0,
        savings: 0,
        food: 0,
        transport: 0,
        living: 0,
        leisure: 0,
      };
    }
  });

  const [selectedPlan, setSelectedPlan] = useState<number | null>(() => {
    const saved = localStorage.getItem(SELECTED_PLAN_KEY);
    return saved ? Number(saved) : null;
  });

  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth());
  const [selectedYear] = useState(new Date().getFullYear());

  const monthKey = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}`;
  const currentBudget = budgets[monthKey] || 0;

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(customBudget));
  }, [customBudget]);

  useEffect(() => {
    if (selectedPlan === null) {
      localStorage.removeItem(SELECTED_PLAN_KEY);
      return;
    }
    localStorage.setItem(SELECTED_PLAN_KEY, String(selectedPlan));
  }, [selectedPlan]);

  useEffect(() => {
    if (currentBudget > 0) {
      setCustomBudget((prev: typeof customBudget) => ({
        ...prev,
        monthly: currentBudget,
      }));
    }
  }, [currentBudget]);

  const handleGenerateAI = async () => {
  setLoading(true);

  // 로딩 UI
  setAiPlans([
    { id: "loading1" },
    { id: "loading2" },
    { id: "loading3" },
  ]);

  try {
    const res = await fetch("http://localhost:8000/api/v1/chat/ai-plan", {
      method: "POST",
    });

    const data = await res.json();

    console.log("AI 응답:", data);

    const parsedPlans = data.map((plan: any, idx: number) => ({
  id: idx + 1,
  name: plan.name,
  budget: plan.budget,
  savings: plan.savings,
  description: plan.description,
  categories: plan.categories.map((c: any) => ({
    name: c.name,
    amount: c.amount,
    icon: getIcon(c.name),
  })),
}));

    setAiPlans(parsedPlans);
  } catch (e) {
    console.error("AI 생성 실패:", e);
    setAiPlans([]);
  } finally {
    setLoading(false);
  }
};

const handleSaveCustomBudget = () => {
  setMonthlyBudget(monthKey, Number(customBudget.monthly) || 0);
  toast.success(`${selectedMonth + 1}월 맞춤 예산이 저장되었습니다!`);
};

const handleApplyPlan = (planId: number) => {
  const plan = aiPlans.find((p) => p.id === planId);
  if (!plan) return;

  setSelectedPlan(planId);
  setMonthlyBudget(monthKey, plan.budget);

  toast.success(`${selectedMonth + 1}월 예산 적용 완료!`);
};

  const totalBudget =
    Number(customBudget.food) +
    Number(customBudget.transport) +
    Number(customBudget.living) +
    Number(customBudget.leisure);

  const withSavings = totalBudget + Number(customBudget.savings);

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
            예산 설정
          </h1>
          <p className="text-gray-600 dark:text-gray-300">
            AI가 추천하는 플랜으로 시작하거나 직접 설정해보세요
          </p>

          <div className="mt-4 mb-2 flex flex-wrap gap-2">
            {Array.from({ length: 12 }, (_, i) => (
              <button
                key={i}
                onClick={() => setSelectedMonth(i)}
                className={`rounded-lg px-3 py-1 text-sm transition ${
                  selectedMonth === i
                    ? "bg-cyan-500 text-white"
                    : "bg-gray-200 text-gray-700 dark:bg-gray-700 dark:text-gray-200"
                }`}
              >
                {i + 1}월
              </button>
            ))}
          </div>

          <p className="text-sm text-gray-500 dark:text-gray-400">
            현재 선택: {selectedYear}년 {selectedMonth + 1}월 / 저장된 예산{" "}
            <span className="font-semibold text-cyan-600 dark:text-cyan-400">
              {currentBudget.toLocaleString()}원
            </span>
          </p>
        </div>

        <Button
  onClick={handleGenerateAI}
  disabled={loading}
  className="bg-gradient-to-r from-cyan-500 to-blue-500"
>
  <Sparkles className="mr-2 h-4 w-4" />
  {loading ? "생성 중..." : "AI 플랜 생성"}
</Button>
      </div>

      <div>
        <h2 className="mb-4 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
          <Sparkles className="h-5 w-5 text-cyan-600" />
          AI 추천 플랜
        </h2>

        <div className="grid gap-6 md:grid-cols-3">
          {aiPlans.map((plan) => {
  // ✅ 로딩 상태
  if (!plan.name) {
    return (
      <Card
        key={plan.id}
        className="p-6 animate-pulse bg-gray-100 dark:bg-gray-700"
      >
        <div className="h-6 w-1/2 bg-gray-300 mb-4 rounded"></div>
        <div className="h-4 w-full bg-gray-300 mb-2 rounded"></div>
        <div className="h-4 w-2/3 bg-gray-300 rounded"></div>
      </Card>
    );
  }

  // ✅ 실제 카드
  return (
    <Card
      key={plan.id}
      className={`border-2 bg-white/80 p-6 backdrop-blur-xl transition-all dark:bg-gray-800/80 ${
        selectedPlan === plan.id
          ? "border-cyan-500 shadow-xl"
          : "border-transparent hover:border-cyan-300"
      }`}
    >
      <div className="mb-4">
        <div className="mb-2 flex items-start justify-between">
          <h3 className="font-bold text-gray-900 dark:text-gray-100">
            {plan.name}
          </h3>
          {selectedPlan === plan.id && (
            <Badge className="bg-cyan-500">적용중</Badge>
          )}
        </div>
        <p className="text-sm text-gray-600 dark:text-gray-300">
          {plan.description}
        </p>
      </div>

      <div className="mb-4 space-y-3">
        <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-purple-50 to-pink-50 p-3">
          <span className="text-sm font-medium text-gray-700">월 예산</span>
          <span className="font-bold text-gray-900">
            {plan.budget?.toLocaleString()}원
          </span>
        </div>
        <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 p-3">
          <span className="text-sm font-medium text-gray-700">목표 저축</span>
          <span className="font-bold text-green-700">
            {plan.savings?.toLocaleString()}원
          </span>
        </div>
      </div>

      <div className="mb-4 space-y-2">
        {plan.categories?.map((cat: any) => {
          const Icon = cat.icon || Coffee;
          return (
            <div
              key={cat.name}
              className="flex items-center justify-between text-sm"
            >
              <div className="flex items-center gap-2">
                <Icon className="h-4 w-4 text-gray-500" />
                <span className="text-gray-700 dark:text-gray-300">
                  {cat.name}
                </span>
              </div>
              <span className="font-medium text-gray-900 dark:text-gray-100">
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
  );
})}
        </div>
      </div>

      <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
        <h2 className="mb-6 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
          <Target className="h-5 w-5 text-cyan-600" />
          맞춤 예산 설정
        </h2>

        <div className="grid gap-8 lg:grid-cols-2">
          <div className="space-y-6">
            <div>
              <Label className="text-gray-700 dark:text-gray-200">월 전체 예산</Label>
              <Input
                type="number"
                value={customBudget.monthly}
                onChange={(e) =>
                  setCustomBudget({
                    ...customBudget,
                    monthly: Number(e.target.value) || 0,
                  })
                }
                className="mt-2"
                placeholder="예: 500000"
              />
            </div>

            <div>
              <Label className="text-gray-700 dark:text-gray-200">목표 저축액</Label>
              <Input
                type="number"
                value={customBudget.savings}
                onChange={(e) =>
                  setCustomBudget({
                    ...customBudget,
                    savings: Number(e.target.value) || 0,
                  })
                }
                className="mt-2"
                placeholder="예: 100000"
              />
            </div>

            <div className="space-y-5">
              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label className="flex items-center gap-2 text-gray-700 dark:text-gray-200">
                    <Coffee className="h-4 w-4" />
                    식비
                  </Label>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {Number(customBudget.food).toLocaleString()}원
                  </span>
                </div>
                <Slider
                  value={[Number(customBudget.food)]}
                  onValueChange={(value) =>
                    setCustomBudget({ ...customBudget, food: value[0] })
                  }
                  max={1000000}
                  step={10000}
                />
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label className="flex items-center gap-2 text-gray-700 dark:text-gray-200">
                    <Car className="h-4 w-4" />
                    교통비
                  </Label>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {Number(customBudget.transport).toLocaleString()}원
                  </span>
                </div>
                <Slider
                  value={[Number(customBudget.transport)]}
                  onValueChange={(value) =>
                    setCustomBudget({ ...customBudget, transport: value[0] })
                  }
                  max={1000000}
                  step={10000}
                />
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label className="flex items-center gap-2 text-gray-700 dark:text-gray-200">
                    <Home className="h-4 w-4" />
                    생활비
                  </Label>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {Number(customBudget.living).toLocaleString()}원
                  </span>
                </div>
                <Slider
                  value={[Number(customBudget.living)]}
                  onValueChange={(value) =>
                    setCustomBudget({ ...customBudget, living: value[0] })
                  }
                  max={1000000}
                  step={10000}
                />
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label className="flex items-center gap-2 text-gray-700 dark:text-gray-200">
                    <Heart className="h-4 w-4" />
                    여가/취미
                  </Label>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {Number(customBudget.leisure).toLocaleString()}원
                  </span>
                </div>
                <Slider
                  value={[Number(customBudget.leisure)]}
                  onValueChange={(value) =>
                    setCustomBudget({ ...customBudget, leisure: value[0] })
                  }
                  max={1000000}
                  step={10000}
                />
              </div>
            </div>

            <Button
              onClick={handleSaveCustomBudget}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500"
            >
              <Wallet className="mr-2 h-4 w-4" />
              {selectedMonth + 1}월 맞춤 예산 저장
            </Button>
          </div>

          <div className="space-y-4">
            <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white">
              <p className="mb-1 text-sm opacity-90">현재 설정한 월 예산</p>
              <p className="text-3xl font-bold">
                {Number(customBudget.monthly).toLocaleString()}원
              </p>
              <p className="mt-2 text-sm opacity-90">
                {selectedYear}년 {selectedMonth + 1}월 기준
              </p>
            </Card>

            <Card className="border-none bg-white/90 p-6 dark:bg-gray-900/80">
              <div className="mb-4 flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-green-600" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">
                  예산 요약
                </h3>
              </div>

              <div className="space-y-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="text-gray-600 dark:text-gray-300">카테고리 합계</span>
                  <span className="font-medium text-gray-900 dark:text-gray-100">
                    {totalBudget.toLocaleString()}원
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-gray-600 dark:text-gray-300">목표 저축액 포함</span>
                  <span className="font-medium text-gray-900 dark:text-gray-100">
                    {withSavings.toLocaleString()}원
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="text-gray-600 dark:text-gray-300">저장된 월 예산</span>
                  <span className="font-medium text-cyan-600 dark:text-cyan-400">
                    {currentBudget.toLocaleString()}원
                  </span>
                </div>

                <div className="pt-2">
                  {Number(customBudget.monthly) > 0 && (
                    <div className="h-3 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className="h-full bg-gradient-to-r from-cyan-500 to-blue-500"
                        style={{
                          width: `${Math.min(
                            100,
                            Math.round((withSavings / Number(customBudget.monthly)) * 100)
                          )}%`,
                        }}
                      />
                    </div>
                  )}
                </div>

                <p className="text-xs text-gray-500 dark:text-gray-400">
                  카테고리 예산 + 저축액이 월 전체 예산 안에 들어오도록 맞추면 좋아요.
                </p>
              </div>
            </Card>

            <Card className="border-none bg-gradient-to-br from-amber-50 to-yellow-50 p-6 text-gray-900">
              <div className="mb-3 flex items-center gap-2">
                <PiggyBank className="h-5 w-5 text-amber-600" />
                <h3 className="font-bold">테오의 한마디</h3>
              </div>

              <p className="text-sm leading-6 text-gray-700">
                지금 선택한 {selectedMonth + 1}월 예산은{" "}
                <span className="font-semibold">
                  {currentBudget.toLocaleString()}원
                </span>
                이야.
                AI 플랜을 먼저 적용한 뒤, 맞춤 예산에서 세부 카테고리를 다듬으면 훨씬 편하게
                관리할 수 있어!
              </p>
            </Card>
          </div>
        </div>
      </Card>
    </div>
  );
}