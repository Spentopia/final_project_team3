import { useState, useEffect } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Slider } from "@/shared/ui/slider";
import { Badge } from "@/shared/ui/badge";
import { Popover, PopoverContent, PopoverTrigger } from "@/shared/ui/popover";
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
  CalendarDays,
  ChevronDown,
  ChevronLeft,
  ChevronRight,
  Check,
  LucideIcon,
} from "lucide-react";
import { toast } from "sonner";
import {
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";

const CUSTOM_BUDGET_STORAGE_PREFIX = "customBudget";
const SELECTED_PLAN_STORAGE_PREFIX = "selectedPlan";
const AI_PLANS_STORAGE_PREFIX = "aiPlans";


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

type AiPlanApiResponse = {
  plans: Array<{
    name: string;
    budget: number;
    savings: number;
    food: number;
    transport: number;
    living: number;
    leisure: number;
    description: string;
  }>;
};

const PLAN_ORDER_LABELS = ["기본 플랜", "중간 플랜", "여유 플랜"] as const;
const MIN_YEAR = 1900;
const MAX_YEAR = 2100;
const TOTAL_MONTHS = (MAX_YEAR - MIN_YEAR + 1) * 12;

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

type MonthSnapshot = {
  customBudget: CustomBudget | null;
  aiPlans: AiPlan[] | null;
  selectedPlan: number | null;
  budgetAmount: number | null;
};


const createEmptyBudget = (): CustomBudget => ({
  monthly: 0,
  savings: 0,
  food: 0,
  transport: 0,
  living: 0,
  leisure: 0,
});

const getCustomBudgetStorageKey = (monthKey: string) =>
  `${CUSTOM_BUDGET_STORAGE_PREFIX}:${monthKey}`;

const getSelectedPlanStorageKey = (monthKey: string) =>
  `${SELECTED_PLAN_STORAGE_PREFIX}:${monthKey}`;

const getAiPlansStorageKey = (monthKey: string) =>
  `${AI_PLANS_STORAGE_PREFIX}:${monthKey}`;

const getMonthKeyFromParts = (year: number, month: number) =>
  `${year}-${String(month + 1).padStart(2, "0")}`;

const getMonthIndexFromParts = (year: number, month: number) =>
  (year - MIN_YEAR) * 12 + month;

const getMonthIndexFromKey = (monthKey: string) => {
  const [yearPart, monthPart] = monthKey.split("-");
  const year = Number(yearPart);
  const month = Number(monthPart) - 1;

  if (!Number.isFinite(year) || !Number.isFinite(month)) return 0;

  return Math.max(0, Math.min(TOTAL_MONTHS - 1, getMonthIndexFromParts(year, month)));
};

const getMonthPartsFromIndex = (index: number) => {
  const safeIndex = Math.max(0, Math.min(TOTAL_MONTHS - 1, index));
  const year = MIN_YEAR + Math.floor(safeIndex / 12);
  const month = safeIndex % 12;
  return { year, month };
};

const getMonthLabelFromIndex = (index: number) => {
  const { year, month } = getMonthPartsFromIndex(index);
  return `${year}년 ${month + 1}월`;
};

const parseCustomBudget = (raw: string | null): CustomBudget | null => {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as Partial<CustomBudget>;
    return { ...createEmptyBudget(), ...parsed };
  } catch {
    return null;
  }
};

const parseAiPlans = (raw: string | null): AiPlan[] | null => {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as AiPlan[];
    return Array.isArray(parsed) ? parsed : null;
  } catch {
    return null;
  }
};

const readMonthSnapshot = (
  monthKey: string,
  budgetAmount: number | undefined
): MonthSnapshot | null => {
  const customBudget = parseCustomBudget(localStorage.getItem(getCustomBudgetStorageKey(monthKey)));
  const aiPlans = parseAiPlans(localStorage.getItem(getAiPlansStorageKey(monthKey)));
  const selectedPlanRaw = localStorage.getItem(getSelectedPlanStorageKey(monthKey));
  const selectedPlan =
    selectedPlanRaw === null ? null : Number.isNaN(Number(selectedPlanRaw)) ? null : Number(selectedPlanRaw);
  const hasAny =
    Boolean(customBudget) ||
    Boolean(aiPlans) ||
    selectedPlanRaw !== null ||
    (typeof budgetAmount === "number" && budgetAmount > 0);

  if (!hasAny) return null;

  return {
    customBudget,
    aiPlans,
    selectedPlan,
    budgetAmount: typeof budgetAmount === "number" ? budgetAmount : null,
  };
};

const findClosestPreviousMonthSnapshot = (startIndex: number, budgets: Record<string, number>) => {
  for (let index = Math.max(0, startIndex); index >= 0; index -= 1) {
    const { year, month } = getMonthPartsFromIndex(index);
    const key = getMonthKeyFromParts(year, month);
    const snapshot = readMonthSnapshot(key, budgets[key]);
    if (snapshot) return snapshot;
  }
  return null;
};

const iconMap = {
  식비: Coffee,
  교통비: Car,
  생활비: Home,
  여가취미: HeartIcon,
  저축: PiggyBank,
} as const;

const createDefaultAiPlans = (monthlyBudget: number): AiPlan[] => {
  const baseBudget = monthlyBudget > 0 ? monthlyBudget : 1000000;
  const variants = [
    {
      id: 1,
      name: "기본 플랜",
      budget: baseBudget,
      savingsRatio: 0.28,
      description: "저축을 우선 확보하고 필수 지출 중심으로 운영하는 절약형 플랜",
      categoryRatios: { food: 0.24, transport: 0.14, living: 0.32, leisure: 0.08 },
    },
    {
      id: 2,
      name: "중간 플랜",
      budget: baseBudget,
      savingsRatio: 0.18,
      description: "저축과 생활 만족도를 균형 있게 맞춘 플랜",
      categoryRatios: { food: 0.25, transport: 0.14, living: 0.29, leisure: 0.16 },
    },
    {
      id: 3,
      name: "여유 플랜",
      budget: baseBudget,
      savingsRatio: 0.1,
      description: "여가와 생활비를 조금 더 넉넉히 배분한 플랜",
      categoryRatios: { food: 0.26, transport: 0.14, living: 0.28, leisure: 0.22 },
    },
  ];

  return variants.map((variant) => {
    const budget = variant.budget;
    const savings = Math.round((budget * variant.savingsRatio) / 10000) * 10000;
    const spendable = budget - savings;
    const food = Math.round((spendable * variant.categoryRatios.food) / 10000) * 10000;
    const transport = Math.round((spendable * variant.categoryRatios.transport) / 10000) * 10000;
    const living = Math.round((spendable * variant.categoryRatios.living) / 10000) * 10000;
    const leisure = budget - savings - food - transport - living;

    return {
      id: variant.id,
      name: variant.name,
      budget,
      savings,
      description: variant.description,
      categories: [
        { name: "식비", amount: food },
        { name: "교통비", amount: transport },
        { name: "생활비", amount: living },
        { name: "여가/취미", amount: leisure },
        { name: "저축", amount: savings },
      ],
    };
  });
};

export default function BudgetPage() {
  const { budgets, setMonthlyBudget, transactions } = useFinance();
  const today = new Date();
  const [selectedMonth, setSelectedMonth] = useState(today.getMonth());
  const [selectedYear, setSelectedYear] = useState(today.getFullYear());
  const monthKey = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}`;
  const selectedMonthIndex = getMonthIndexFromParts(selectedYear, selectedMonth);
  const [isMonthPickerOpen, setIsMonthPickerOpen] = useState(false);
  const currentMonthLabel = `${selectedYear}년 ${selectedMonth + 1}월`;

  const [aiPlans, setAiPlans] = useState<AiPlan[]>(() => createDefaultAiPlans(1000000));
  const [customBudget, setCustomBudget] = useState<CustomBudget>(createEmptyBudget);
  const [selectedPlan, setSelectedPlan] = useState<number | null>(null);
  const selectedMonthDate = new Date(selectedYear, selectedMonth, 1);
  const monthlyIncomeBudget = getMonthlyIncomeTotal(transactions, selectedMonthDate);
  const currentBudget = monthlyIncomeBudget || budgets[monthKey] || 0;

  useEffect(() => {
    const savedBudget = localStorage.getItem(getCustomBudgetStorageKey(monthKey));
    const savedPlans = localStorage.getItem(getAiPlansStorageKey(monthKey));
    const savedSelectedPlan = localStorage.getItem(getSelectedPlanStorageKey(monthKey));

    const hasSavedMonthData =
      Boolean(savedBudget) || Boolean(savedPlans) || savedSelectedPlan !== null || budgets[monthKey] !== undefined;

    const fallbackBudgetForMonth = () => {
      const previousSnapshot = findClosestPreviousMonthSnapshot(selectedMonthIndex - 1, budgets);
      if (previousSnapshot?.customBudget?.monthly) {
        return previousSnapshot.customBudget.monthly;
      }

      if (previousSnapshot?.budgetAmount && previousSnapshot.budgetAmount > 0) {
        return previousSnapshot.budgetAmount;
      }

      if (currentBudget > 0) {
        return currentBudget;
      }

      return 1000000;
    };

    if (savedBudget) {
      const parsedBudget = parseCustomBudget(savedBudget);
      setCustomBudget(parsedBudget ?? createEmptyBudget());
    } else if (hasSavedMonthData) {
      const previousSnapshot = findClosestPreviousMonthSnapshot(selectedMonthIndex - 1, budgets);
      const nextBudget = previousSnapshot?.customBudget
        ? { ...previousSnapshot.customBudget }
        : { ...createEmptyBudget(), monthly: fallbackBudgetForMonth() };

      if (!nextBudget.monthly) {
        nextBudget.monthly = fallbackBudgetForMonth();
      }

      setCustomBudget(nextBudget);
      localStorage.setItem(getCustomBudgetStorageKey(monthKey), JSON.stringify(nextBudget));
      if (nextBudget.monthly > 0 && budgets[monthKey] !== nextBudget.monthly) {
        setMonthlyBudget(monthKey, nextBudget.monthly);
      }
    } else {
      const nextBudget = { ...createEmptyBudget(), monthly: fallbackBudgetForMonth() };
      setCustomBudget(nextBudget);
      localStorage.setItem(getCustomBudgetStorageKey(monthKey), JSON.stringify(nextBudget));
      if (nextBudget.monthly > 0) {
        setMonthlyBudget(monthKey, nextBudget.monthly);
      }
    }

    if (savedPlans) {
      const parsedPlans = parseAiPlans(savedPlans);
      setAiPlans(parsedPlans ?? createDefaultAiPlans(fallbackBudgetForMonth()));
    } else {
      const previousSnapshot = findClosestPreviousMonthSnapshot(selectedMonthIndex - 1, budgets);
      const nextPlans =
        previousSnapshot?.aiPlans ?? createDefaultAiPlans(fallbackBudgetForMonth());
      setAiPlans(nextPlans);
      localStorage.setItem(getAiPlansStorageKey(monthKey), JSON.stringify(nextPlans));
    }

    if (savedSelectedPlan) {
      setSelectedPlan(Number(savedSelectedPlan));
    } else {
      const previousSnapshot = findClosestPreviousMonthSnapshot(selectedMonthIndex - 1, budgets);
      const nextSelectedPlan = previousSnapshot?.selectedPlan ?? null;
      setSelectedPlan(nextSelectedPlan);
      if (nextSelectedPlan === null) {
        localStorage.removeItem(getSelectedPlanStorageKey(monthKey));
      } else {
        localStorage.setItem(getSelectedPlanStorageKey(monthKey), String(nextSelectedPlan));
      }
    }
  }, [monthKey, selectedMonthIndex, currentBudget, budgets, setMonthlyBudget]);

  useEffect(() => {
    localStorage.setItem(getCustomBudgetStorageKey(monthKey), JSON.stringify(customBudget));
  }, [customBudget, monthKey]);

  useEffect(() => {
    localStorage.setItem(getAiPlansStorageKey(monthKey), JSON.stringify(aiPlans));
  }, [aiPlans, monthKey]);

  useEffect(() => {
    if (selectedPlan === null) {
      localStorage.removeItem(getSelectedPlanStorageKey(monthKey));
      return;
    }
    localStorage.setItem(getSelectedPlanStorageKey(monthKey), String(selectedPlan));
  }, [selectedPlan, monthKey]);

  useEffect(() => {
  if (currentBudget > 0 && selectedPlan === null) {
    setCustomBudget((prev) => ({
      ...prev,
      monthly: currentBudget,
    }));
  }
}, [currentBudget, selectedPlan]);

  useEffect(() => {
    if (selectedPlan !== null) return;
    setAiPlans(createDefaultAiPlans(customBudget.monthly || currentBudget || 1000000));
  }, [customBudget.monthly, currentBudget, selectedPlan]);

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
  const sourceBudget = currentBudget || customBudget.monthly;

  if (!selectedPlan && !sourceBudget) {
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

  await apiClient.patch(`/api/budget/${budgetId}`, {
    total_budget: sourceBudget,
    savings_goal: customBudget.savings || 0,
  });
} catch (err: any) {
  if (err.response?.status === 404) {
    const createRes = await apiClient.post("/api/budget", {
      year: selectedYear,
      month,
      total_budget: sourceBudget || 1000000,
      savings_goal: customBudget.savings || 0,
    });

    budgetId = createRes.data.id;
  } else {
    throw err;
  }
}

    // 👉 3. AI 플랜 생성 요청 (핵심)
    const aiRes = await apiClient.post<AiPlanApiResponse>(
      `/api/budget/${budgetId}/ai-plan`
    );

    const data = aiRes.data;

    console.log("AI 응답:", data);

    // 👉 4. 프론트 형식으로 변환
    const mappedPlans: AiPlan[] = data.plans
      .map((p, idx) => ({
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
      }))
      .map((plan, idx) => ({
        ...plan,
        name: PLAN_ORDER_LABELS[idx] ?? plan.name,
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
    toast.success(`${selectedYear}년 ${selectedMonth + 1}월 맞춤 예산이 저장되었습니다!`);
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

  const updateSelectedMonthByIndex = (index: number) => {
    const safeIndex = Math.max(0, Math.min(TOTAL_MONTHS - 1, index));
    const { year, month } = getMonthPartsFromIndex(safeIndex);
    setSelectedYear(year);
    setSelectedMonth(month);
  };

  const currentYearMonths = Array.from({ length: 12 }, (_, monthIndex) => ({
    monthIndex,
    label: `${monthIndex + 1}월`,
    selected: selectedMonth === monthIndex,
  }));

  return (
    <div className="space-y-6">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
        <div className="space-y-3">
          <div>
            <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
              예산 설정
            </h1>
            <p className="text-gray-600 dark:text-gray-300">
              AI가 추천하는 플랜으로 시작하거나 직접 설정해보세요
            </p>
          </div>

          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => updateSelectedMonthByIndex(selectedMonthIndex - 1)}
              disabled={selectedMonthIndex <= 0}
              className="h-9 w-9 rounded-lg border-slate-200 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>

            <Popover open={isMonthPickerOpen} onOpenChange={setIsMonthPickerOpen}>
              <PopoverTrigger asChild>
                <button
                  type="button"
                  className="flex h-9 w-[184px] items-center justify-between rounded-[13px] border border-slate-200 bg-white px-2.5 text-left text-slate-900 shadow-sm transition hover:bg-slate-50 dark:border-slate-700 dark:bg-[#111827] dark:text-gray-100 dark:hover:bg-slate-800"
                >
                  <span className="flex items-center gap-1 text-sm font-semibold">
                    <CalendarDays className="h-3.5 w-3.5 text-slate-500 dark:text-violet-300" />
                    {currentMonthLabel}
                  </span>
                  <ChevronDown className="h-3.5 w-3.5 text-slate-500 dark:text-gray-300" />
                </button>
              </PopoverTrigger>
              <PopoverContent
                align="start"
                sideOffset={6}
                className="z-50 w-[264px] rounded-2xl border border-slate-200 bg-white p-2 shadow-[0_12px_30px_rgba(15,23,42,0.12)] outline-none dark:border-slate-700 dark:bg-[#0b1020] dark:shadow-[0_18px_36px_rgba(0,0,0,0.35)]"
              >
                <div className="mb-1.5 flex items-center justify-between">
                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    onClick={() => setSelectedYear((prev) => Math.max(MIN_YEAR, prev - 1))}
                    disabled={selectedYear <= MIN_YEAR}
                    className="h-7 w-7 rounded-md border-slate-200 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
                  >
                    <ChevronLeft className="h-3.5 w-3.5" />
                  </Button>

                  <div className="text-sm font-semibold text-slate-900 dark:text-gray-100">
                    {selectedYear}년
                  </div>

                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    onClick={() => setSelectedYear((prev) => Math.min(MAX_YEAR, prev + 1))}
                    disabled={selectedYear >= MAX_YEAR}
                    className="h-7 w-7 rounded-md border-slate-200 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
                  >
                    <ChevronRight className="h-3.5 w-3.5" />
                  </Button>
                </div>

                <div className="grid grid-cols-3 gap-0.5">
                  {currentYearMonths.map(({ monthIndex, label, selected }) => (
                    <button
                      key={`${selectedYear}-${monthIndex}`}
                      type="button"
                      onClick={() => {
                        setSelectedMonth(monthIndex);
                        setIsMonthPickerOpen(false);
                      }}
                      className={`flex h-8 items-center justify-center rounded-md text-[12.5px] font-semibold transition ${
                        selected
                          ? "bg-slate-900 text-white shadow-sm dark:bg-[#090b16] dark:text-white"
                          : "border border-slate-200 bg-white text-slate-800 hover:bg-slate-100 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-200 dark:hover:bg-slate-800"
                      }`}
                    >
                      <span>{label}</span>
                      {selected ? <Check className="ml-1 h-3.5 w-3.5" /> : null}
                    </button>
                  ))}
                </div>
              </PopoverContent>
            </Popover>

            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => updateSelectedMonthByIndex(selectedMonthIndex + 1)}
              disabled={selectedMonthIndex >= TOTAL_MONTHS - 1}
              className="h-9 w-9 rounded-lg border-slate-200 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
            >
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>

          <p className="text-sm text-gray-500 dark:text-gray-400">
            현재 선택: {currentMonthLabel} / 수입 기준 월 예산{" "}
            <span className="font-semibold text-slate-900 dark:text-violet-300">
              {currentBudget.toLocaleString()}원
            </span>
          </p>
        </div>

        <Button
          onClick={handleGenerateAiPlans}
          disabled={loading}
          className="spentopia-primary-button"
        >
          {loading ? "AI 생성 중..." : "AI 플랜 추천"}
        </Button>
      </div>

      <div>
        <h2 className="mb-4 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
  <Sparkles className="h-5 w-5 text-slate-700 dark:text-violet-300" />
  AI 추천 플랜
</h2>

<p className="mb-4 text-sm text-gray-500 dark:text-gray-400">
  기본 추천 플랜입니다. AI로 새로 추천받을 수 있어요.
</p>

        <div className="grid gap-6 md:grid-cols-3">
          {aiPlans.map((plan) => (
            <Card
              key={plan.id}
              className={`border-2 spentopia-plan-light-card spentopia-nft-card-tone p-6 backdrop-blur-xl transition-all  ${
                selectedPlan === plan.id
                  ? "border-cyan-300 shadow-xl dark:border-[#7c3aed]/70"
                  : "border-transparent hover:border-cyan-300 dark:hover:border-[#7c3aed]/40"
              }`}
            >
              <div className="mb-4">
                <div className="mb-2 flex items-start justify-between">
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">
                    {plan.name}
                  </h3>
                  {selectedPlan === plan.id && (
                    <Badge className="bg-slate-900 text-white">적용중</Badge>
                  )}
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-300">
                  {plan.description}
                </p>
              </div>

              <div className="mb-4 space-y-3">
                <div className="flex items-center justify-between rounded-lg spentopia-soft-card spentopia-nft-card-tone p-3">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">월 예산</span>
                  <span className="font-bold text-gray-900 dark:text-gray-100">
                    {Number(plan.budget || 0).toLocaleString()}원
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg spentopia-soft-card spentopia-nft-card-tone p-3">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">목표 저축</span>
                  <span className="font-bold text-emerald-700 dark:text-emerald-300">
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
                variant="outline"
                className="w-full spentopia-light-nft-button"
              >
                {selectedPlan === plan.id ? "적용됨" : "이 플랜 적용하기"}
              </Button>
            </Card>
          ))}
        </div>
      </div>

      <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6 backdrop-blur-xl">
        <h2 className="mb-6 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
          <Target className="h-5 w-5 text-slate-700 dark:text-violet-300" />
          맞춤 예산 설정
        </h2>

        <div className="grid gap-8 lg:grid-cols-2">
          <div className="space-y-6">
            <div>
              <Label className="font-semibold text-gray-900 dark:text-gray-100">월 전체 예산</Label>
              <Input
                type="number"
                value={customBudget.monthly}
                onChange={(e) => updateMonthlyBudget(Number(e.target.value) || 0)}
                className="mt-2"
                placeholder="예: 500000"
              />
            </div>

            <div>
              <Label className="font-semibold text-gray-900 dark:text-gray-100">목표 저축액</Label>
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
                  <Label className="flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
                    <Coffee className="h-4 w-4" />
                    식비
                  </Label>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
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
                  <Label className="flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
                    <Car className="h-4 w-4" />
                    교통비
                  </Label>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
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
                  <Label className="flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
                    <Home className="h-4 w-4" />
                    생활비
                  </Label>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
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
                  <Label className="flex items-center gap-2 font-semibold text-gray-900 dark:text-gray-100">
                    <HeartIcon className="h-4 w-4" />
                    여가/취미
                  </Label>
                  <span className="text-sm font-medium text-gray-900 dark:text-gray-100">
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
              <span className="font-semibold text-slate-900 dark:text-violet-300">
                {remainingCategoryBudget.toLocaleString()}원
              </span>
              입니다.
            </p>

            <Button
              variant="outline"
              onClick={handleSaveCustomBudget}
              className="w-full spentopia-light-nft-button"
            >
              <Wallet className="mr-2 h-4 w-4" />
              {selectedMonth + 1}월 맞춤 예산 저장
            </Button>
          </div>

          <div className="space-y-4">
            <Card className="border-none spentopia-hero-card spentopia-nft-card-tone p-6">
              <p className="mb-1 text-sm opacity-90">현재 설정한 월 예산</p>
              <p className="text-3xl font-bold">
                {Number(customBudget.monthly).toLocaleString()}원
              </p>
              <p className="mt-2 text-sm opacity-90">
                {selectedYear}년 {selectedMonth + 1}월 기준
              </p>
            </Card>

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6">
              <div className="mb-4 flex items-center gap-2">
                <TrendingUp className="h-5 w-5 text-slate-700 dark:text-violet-300" />
                <h3 className="font-bold text-gray-900 dark:text-gray-100">
                  예산 요약
                </h3>
              </div>

              <div className="space-y-3 text-sm">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900 dark:text-gray-100">카테고리 합계</span>
                  <span className="font-medium text-gray-900 dark:text-gray-100">
                    {totalBudget.toLocaleString()}원
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900 dark:text-gray-100">목표 저축액 포함</span>
                  <span className="font-medium text-gray-900 dark:text-gray-100">
                    {withSavings.toLocaleString()}원
                  </span>
                </div>

                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900 dark:text-gray-100">수입 기준 월 예산</span>
                  <span className="font-medium text-slate-900 dark:text-violet-300">
                    {currentBudget.toLocaleString()}원
                  </span>
                </div>

                <div className="pt-2">
                  {Number(customBudget.monthly) > 0 && (
                    <div className="spentopia-xp-track h-3">
                      <div
                        className="h-full spentopia-xp-bar"
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

            <Card className="border-none spentopia-surface-card spentopia-nft-card-tone p-6">
              <div className="mb-3 flex items-center gap-2">
                <PiggyBank className="h-5 w-5 text-slate-700 dark:text-violet-300" />
                <h3 className="font-bold">예산 설정 한마디</h3>
              </div>

              <p className="text-sm leading-6 text-gray-800 dark:text-gray-200">
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
