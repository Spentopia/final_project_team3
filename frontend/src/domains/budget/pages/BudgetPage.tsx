import { useState, useEffect } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Slider } from "@/shared/ui/slider";
import { Badge } from "@/shared/ui/badge";
import { apiClient } from "@/shared/api/client";
import {
  Wallet,
  Target,
  TrendingUp,
  Sparkles,
  PiggyBank,
  Coffee,
  Home,
  Car,
  Heart as HeartIcon,
  LucideIcon,
} from "lucide-react";
import { toast } from "sonner";
import {
  getMonthlyExpenseTotal,
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";




const STORAGE_KEY = "customBudget";
const SELECTED_PLAN_KEY = "selectedPlan";
const AI_PLANS_KEY = "aiPlans";


type PlanCategory = {
  name: string;
  amount: number;
};

type AiPlan = {
  id: number;
  name: string;
  budget: number;
  savings: number;
  description: string;
  categories: PlanCategory[];
};

type CustomBudget = {
  monthly: number;
  savings: number;
  food: number;
  transport: number;
  living: number;
  leisure: number;
};

type BudgetCategoryKey = "food" | "transport" | "living" | "leisure";

const BUDGET_CATEGORY_KEYS: BudgetCategoryKey[] = ["food", "transport", "living", "leisure"];


const createEmptyBudget = (): CustomBudget => ({
  monthly: 0,
  savings: 0,
  food: 0,
  transport: 0,
  living: 0,
  leisure: 0,
});

const iconMap = {
  식비: Coffee,
  교통비: Car,
  생활비: Home,
  여가취미: HeartIcon,
  저축: PiggyBank,
} as const;

export default function BudgetPage() {
  const { budgets, setMonthlyBudget } = useFinance();

  const [aiPlans, setAiPlans] = useState<AiPlan[]>(() => {
  const saved = localStorage.getItem(AI_PLANS_KEY);


  if (saved) {
    try {
      return JSON.parse(saved) as AiPlan[];
    } catch {}
  }

  // fallback (처음 접속 시만)
  return [
  {
    id: 1,
    name: "월 50만원 생활 플랜",
    budget: 500000,
    savings: 50000,
    description: "합리적인 소비와 저축을 위한 균형잡힌 플랜",
    categories: [
  { name: "식비", amount: 150000 },
  { name: "교통비", amount: 80000 },
  { name: "생활비", amount: 120000 },
  { name: "여가/취미", amount: 100000 },
  { name: "저축", amount: 50000 },
],
  },
  {
    id: 2,
    name: "7년 1억 만들기",
    budget: 400000,
    savings: 150000,
    description: "목표 지향적인 저축 중심 플랜",
    categories: [
      { name: "식비", amount: 100000 },
      { name: "교통비", amount: 60000 },
      { name: "생활비", amount: 90000 },
      { name: "여가/취미", amount: 50000 },
      { name: "저축", amount: 150000 },
    ],
  },
  {
    id: 3,
    name: "자유로운 소비 플랜",
    budget: 700000,
    savings: 30000,
    description: "현재의 삶을 즐기면서도 미래를 준비하는 플랜",
    categories: [
      { name: "식비", amount: 200000 },
      { name: "교통비", amount: 100000 },
      { name: "생활비", amount: 200000 },
      { name: "여가/취미", amount: 170000 },
      { name: "저축", amount: 30000 },
    ],
  },
  ]});

  const [customBudget, setCustomBudget] = useState<CustomBudget>(() => {
    const saved = localStorage.getItem(STORAGE_KEY);
    if (!saved) {
      return createEmptyBudget();
    }

    try {
      return { ...createEmptyBudget(), ...JSON.parse(saved) };
    } catch {
      return createEmptyBudget();
    }
  });

  const [selectedPlan, setSelectedPlan] = useState<number | null>(() => {
    const saved = localStorage.getItem(SELECTED_PLAN_KEY);
    return saved ? Number(saved) : null;
  });

  const [selectedMonth, setSelectedMonth] = useState(new Date().getMonth());
  const [selectedYear] = useState(new Date().getFullYear());

  const monthKey = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}`;
  const currentBudget = budgets[monthKey] ?? 0;

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(customBudget));
  }, [customBudget]);

  useEffect(() => {
  localStorage.setItem(AI_PLANS_KEY, JSON.stringify(aiPlans));
}, [aiPlans]);

  useEffect(() => {
    if (selectedPlan === null) {
      localStorage.removeItem(SELECTED_PLAN_KEY);
      return;
    }
    localStorage.setItem(SELECTED_PLAN_KEY, String(selectedPlan));
  }, [selectedPlan]);

  useEffect(() => {
  if (currentBudget > 0 && selectedPlan === null) {
    setCustomBudget((prev) => ({
      ...prev,
      monthly: currentBudget,
    }));
  }
}, [currentBudget, selectedPlan]);

  const handleApplyPlan = async (planId: number) => {
  const plan = aiPlans.find((p) => p.id === planId);
  if (!plan) return;

  try {
    const month = selectedMonth + 1;

    // 1️⃣ budget 조회
    const res = await apiClient.get("/api/budget", {
      params: {
        year: selectedYear,
        month,
      },
    });

    const budgetId = res.data.id;

    // 2️⃣ DB 업데이트 🔥
    await apiClient.patch(`/api/budget/${budgetId}`, {
      total_budget: plan.budget,
      savings_goal: plan.savings,
    });

    // 3️⃣ 카테고리도 저장 (선택 but 강추)
    await apiClient.patch(`/api/budget/${budgetId}/categories`, {
      categories: plan.categories.map((cat) => ({
        category:
          cat.name === "식비"
            ? "food"
            : cat.name === "교통비"
            ? "transport"
            : cat.name === "생활비"
            ? "living"
            : cat.name === "여가/취미"
            ? "leisure"
            : "savings",
        allocated_amount: cat.amount,
      })),
    });

    // 4️⃣ 프론트 상태 업데이트
    setSelectedPlan(planId);
    setMonthlyBudget(monthKey, plan.budget);

    setCustomBudget({
      monthly: plan.budget,
      savings: plan.savings,
      food: plan.categories.find((c) => c.name === "식비")?.amount ?? 0,
      transport: plan.categories.find((c) => c.name === "교통비")?.amount ?? 0,
      living: plan.categories.find((c) => c.name === "생활비")?.amount ?? 0,
      leisure: plan.categories.find((c) => c.name === "여가/취미")?.amount ?? 0,
    });

    toast.success("플랜이 적용되었습니다! 🚀");
  } catch (err) {
    console.error(err);
    toast.error("플랜 적용 실패");
  }
};

  const [loading, setLoading] = useState(false);

  const handleGenerateAiPlans = async () => {
  if (!selectedPlan && !customBudget.monthly) {
    toast.error("먼저 예산을 설정하세요!");
    return;
  }

  setLoading(true);
  try {
    // 👉 1. 현재 선택된 budget id 필요
    // 지금 구조에서는 budget id가 없으니까
    // → 먼저 budget을 생성 or 조회해야 함

    const month = selectedMonth + 1;

let budgetId;

try {
  const res = await apiClient.get("/api/budget", {
    params: {
      year: selectedYear,
      month,
    },
  });

  budgetId = res.data.id;
} catch (err: any) {
  if (err.response?.status === 404) {
    const createRes = await apiClient.post("/api/budget", {
      year: selectedYear,
      month,
      total_budget: customBudget.monthly || 500000,
      savings_goal: customBudget.savings || 0,
    });

    budgetId = createRes.data.id;
  } else {
    throw err;
  }
}

    // 👉 3. AI 플랜 생성 요청 (핵심)
    const aiRes = await apiClient.post(
  `/api/budget/${budgetId}/ai-plan`
);

    const data = aiRes.data;

    console.log("AI 응답:", data);

    // 👉 4. 프론트 형식으로 변환
    const mappedPlans: AiPlan[] = data.plans.map((p: any, idx: number) => ({
  id: Date.now() + idx,
  name: p.name,
  budget: p.budget,
  savings: p.savings,
  description: p.description,
  categories: [
    { name: "식비", amount: p.food },
    { name: "교통비", amount: p.transport },
    { name: "생활비", amount: p.living },
    { name: "여가/취미", amount: p.leisure },
    { name: "저축", amount: p.savings },
  ],
}));

    setAiPlans(mappedPlans);

    toast.success("AI 플랜 생성 완료!");
  } catch (err) {
    console.error(err);
    toast.error("AI 플랜 생성 실패");
  } finally {
    setLoading(false);
  }
};

  const handleSaveCustomBudget = () => {
    setMonthlyBudget(monthKey, Number(customBudget.monthly) || 0);
    toast.success(`${selectedMonth + 1}월 맞춤 예산이 저장되었습니다!`);
  };

  const fitCategoryBudgetsToMonthly = (budget: CustomBudget): CustomBudget => {
    const monthlyLimit = Math.max(0, Number(budget.monthly) || 0);
    let remaining = monthlyLimit;
    const next = { ...budget };

    BUDGET_CATEGORY_KEYS.forEach((key) => {
      const amount = Math.max(0, Number(next[key]) || 0);
      const fittedAmount = Math.min(amount, remaining);
      next[key] = fittedAmount;
      remaining -= fittedAmount;
    });

    return next;
  };

  const updateMonthlyBudget = (monthly: number) => {
    setCustomBudget((prev) => fitCategoryBudgetsToMonthly({ ...prev, monthly }));
  };

  const updateCategoryBudget = (key: BudgetCategoryKey, amount: number) => {
    setCustomBudget((prev) => {
      const monthlyLimit = Math.max(0, Number(prev.monthly) || 0);
      const otherTotal = BUDGET_CATEGORY_KEYS
        .filter((categoryKey) => categoryKey !== key)
        .reduce((sum, categoryKey) => sum + Number(prev[categoryKey]), 0);
      const maxAmount = Math.max(0, monthlyLimit - otherTotal);

      return {
        ...prev,
        [key]: Math.min(Math.max(0, amount), maxAmount),
      };
    });
  };

  const totalBudget =
    Number(customBudget.food) +
    Number(customBudget.transport) +
    Number(customBudget.living) +
    Number(customBudget.leisure);

  const withSavings = totalBudget + Number(customBudget.savings);
  const remainingCategoryBudget = Math.max(0, Number(customBudget.monthly) - totalBudget);
  const getCategorySliderMax = (key: BudgetCategoryKey) => {
    const otherTotal = BUDGET_CATEGORY_KEYS
      .filter((categoryKey) => categoryKey !== key)
      .reduce((sum, categoryKey) => sum + Number(customBudget[categoryKey]), 0);

    return Math.max(Number(customBudget[key]), Number(customBudget.monthly) - otherTotal, 0);
  };

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

        <Button onClick={handleGenerateAiPlans} disabled={loading}>
  {loading ? "AI 생성 중..." : "AI 플랜 추천"}
</Button>
      </div>

      <div>
        <h2 className="mb-4 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
  <Sparkles className="h-5 w-5 text-cyan-600" />
  AI 추천 플랜
</h2>

<p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
  기본 추천 플랜입니다. AI로 새로 추천받을 수 있어요.
</p>

        <div className="grid gap-6 md:grid-cols-3">
          {aiPlans.map((plan) => (
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
                    {Number(plan.budget || 0).toLocaleString()}원
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg bg-gradient-to-r from-green-50 to-emerald-50 p-3">
                  <span className="text-sm font-medium text-gray-700">목표 저축</span>
                  <span className="font-bold text-green-700">
                    {Number(plan.savings || 0).toLocaleString()}원
                  </span>
                </div>
              </div>

              <div className="mb-4 space-y-2">
                {plan.categories.map((cat) => {
                  const Icon = iconMap[cat.name as keyof typeof iconMap] || Coffee;
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
                        {Number(cat.amount || 0).toLocaleString()}원
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
                onChange={(e) => updateMonthlyBudget(Number(e.target.value) || 0)}
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
                  onValueChange={(value) => updateCategoryBudget("food", value[0])}
                  max={getCategorySliderMax("food")}
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
                  onValueChange={(value) => updateCategoryBudget("transport", value[0])}
                  max={getCategorySliderMax("transport")}
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
                  onValueChange={(value) => updateCategoryBudget("living", value[0])}
                  max={getCategorySliderMax("living")}
                  step={10000}
                />
              </div>

              <div>
                <div className="mb-2 flex items-center justify-between">
                  <Label className="flex items-center gap-2 text-gray-700 dark:text-gray-200">
                    <HeartIcon className="h-4 w-4" />
                    여가/취미
                  </Label>
                  <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
                    {Number(customBudget.leisure).toLocaleString()}원
                  </span>
                </div>
                <Slider
                  value={[Number(customBudget.leisure)]}
                  onValueChange={(value) => updateCategoryBudget("leisure", value[0])}
                  max={getCategorySliderMax("leisure")}
                  step={10000}
                />
              </div>
            </div>

            <p className="text-sm text-gray-500 dark:text-gray-400">
              카테고리에 배분 가능한 남은 예산은{" "}
              <span className="font-semibold text-cyan-600 dark:text-cyan-400">
                {remainingCategoryBudget.toLocaleString()}원
              </span>
              입니다.
            </p>

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
                  카테고리 예산은 월 전체 예산 안에서만 배분되며, 목표 저축액은 별도로 관리됩니다.
                </p>
              </div>
            </Card>

            <Card className="border-none bg-gradient-to-br from-amber-50 to-yellow-50 p-6 text-gray-900">
              <div className="mb-3 flex items-center gap-2">
                <PiggyBank className="h-5 w-5 text-amber-600" />
                <h3 className="font-bold">예산 설정 한마디</h3>
              </div>

              <p className="text-sm leading-6 text-gray-700">
                지금 선택한 {selectedMonth + 1}월 예산은{" "}
                <span className="font-semibold">
                  {currentBudget.toLocaleString()}원
                </span>
                입니다.
                AI 플랜을 먼저 적용한 뒤, 맞춤 예산에서 세부 카테고리를 다듬으면 더 편리하게
                관리할 수 있습니다.
              </p>
            </Card>
          </div>
        </div>
      </Card>
    </div>
  );
}
