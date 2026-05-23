import { useState, useEffect } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Slider } from "@/shared/ui/slider";
import { Badge } from "@/shared/ui/badge";
import { Popover, PopoverContent, PopoverTrigger } from "@/shared/ui/popover";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/shared/ui/dialog";
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
  Zap,
  LucideIcon,
} from "lucide-react";
import { toast } from "sonner";
import {
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";

const CUSTOM_BUDGET_STORAGE_PREFIX = "customBudget";
const SELECTED_PLAN_STORAGE_PREFIX = "selectedPlan";
const AI_PLANS_STORAGE_PREFIX = "aiPlans";
const BUDGET_LOCK_STORAGE_PREFIX = "budgetLocked";


type PlanCategory = {
  name: string;
  amount: number;
};

type AiPlan = {
  id: string;
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
const today = new Date();

const MIN_YEAR = 1900;

const CURRENT_YEAR = today.getFullYear();
const CURRENT_MONTH = today.getMonth();

const MAX_YEAR = CURRENT_YEAR;

const TOTAL_MONTHS =
  (MAX_YEAR - MIN_YEAR) * 12 + (CURRENT_MONTH + 1);

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
  selectedPlan: string | null;
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

const getBudgetLockStorageKey = (monthKey: string) =>
  `${BUDGET_LOCK_STORAGE_PREFIX}:${monthKey}`;

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
  selectedPlanRaw === null
    ? null
    : selectedPlanRaw;
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

const createDefaultAiPlans = (
  monthlyBudget: number,
  savingsGoal: number = 0
): AiPlan[] => {
  const baseBudget = monthlyBudget > 9999 ? monthlyBudget : 0;
  const variants = [
    {
      id: "기본 플랜",
      name: "기본 플랜",
      budget: baseBudget,
      savingsRatio: 0.28,
      description: "저축을 우선 확보하고 필수 지출 중심으로 운영하는 절약형 플랜",
      categoryRatios: { food: 0.24, transport: 0.14, living: 0.32, leisure: 0.08 },
    },
    {
      id: "중간 플랜",
      name: "중간 플랜",
      budget: baseBudget,
      savingsRatio: 0.18,
      description: "저축과 생활 만족도를 균형 있게 맞춘 플랜",
      categoryRatios: { food: 0.25, transport: 0.14, living: 0.29, leisure: 0.16 },
    },
    {
      id: "여유 플랜",
      name: "여유 플랜",
      budget: baseBudget,
      savingsRatio: 0.1,
      description: "여가와 생활비를 조금 더 넉넉히 배분한 플랜",
      categoryRatios: { food: 0.26, transport: 0.14, living: 0.28, leisure: 0.22 },
    },
  ];

  return variants.map((variant) => {
  const budget = variant.budget;

  const savings = Math.min(
    Math.round((savingsGoal || 0) / 10000) * 10000,
    budget
  );

  const spendable = budget - savings;

  const food =
    Math.round((spendable * variant.categoryRatios.food) / 10000) * 10000;

  const transport =
    Math.round((spendable * variant.categoryRatios.transport) / 10000) * 10000;

  const living =
    Math.round((spendable * variant.categoryRatios.living) / 10000) * 10000;

  const leisure =
    budget - savings - food - transport - living;

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

  const [aiPlans, setAiPlans] = useState<AiPlan[]>(() => {
  const saved = localStorage.getItem(
    getAiPlansStorageKey(
      `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`
    )
  );

  return parseAiPlans(saved) || [];
});
  const [customBudget, setCustomBudget] = useState<CustomBudget>(createEmptyBudget);
  const [selectedPlan, setSelectedPlan] = useState<string | null>(null);
  const [pendingApplyPlanId, setPendingApplyPlanId] = useState<string | null>(null);
  const [isBudgetLocked, setIsBudgetLocked] = useState(() =>
    localStorage.getItem(
      getBudgetLockStorageKey(
        `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, "0")}`
      )
    ) === "true"
  );
  const selectedMonthDate = new Date(selectedYear, selectedMonth, 1);
  const monthlyIncomeBudget = getMonthlyIncomeTotal(transactions, selectedMonthDate);
  const currentBudget = budgets[monthKey] || monthlyIncomeBudget || 0;
  const canEditBudget = !isBudgetLocked;
  const budgetDisabledMessage = isBudgetLocked
    ? "이번 달 예산 설정이 완료되었습니다. 예산 설정은 월 1회만 가능합니다."
    : "예산 설정은 월 1회만 가능합니다.";

  useEffect(() => {
    const savedBudget = localStorage.getItem(getCustomBudgetStorageKey(monthKey));
    const savedPlans = localStorage.getItem(getAiPlansStorageKey(monthKey));
    const savedSelectedPlan = localStorage.getItem(getSelectedPlanStorageKey(monthKey));

    const hasSavedMonthData =
      Boolean(savedBudget) || Boolean(savedPlans) || savedSelectedPlan !== null || budgets[monthKey] !== undefined;

    const fallbackBudgetForMonth = () => {
  const previousSnapshot = findClosestPreviousMonthSnapshot(
    selectedMonthIndex - 1,
    budgets
  );

  if (previousSnapshot?.customBudget?.monthly) {
    return previousSnapshot.customBudget.monthly;
  }

  if (
    previousSnapshot?.budgetAmount &&
    previousSnapshot.budgetAmount > 0
  ) {
    return previousSnapshot.budgetAmount;
  }

  return 1000000;
};

    if (savedBudget) {
  const parsedBudget = parseCustomBudget(savedBudget);

  if (parsedBudget) {
    setCustomBudget(parsedBudget);

    if (parsedBudget.monthly > 0) {
      setMonthlyBudget(monthKey, parsedBudget.monthly);
    }
  }
} else if (hasSavedMonthData) {
  const previousSnapshot = findClosestPreviousMonthSnapshot(
    selectedMonthIndex - 1,
    budgets
  );

  const nextBudget = previousSnapshot?.customBudget
    ? { ...previousSnapshot.customBudget }
    : { ...createEmptyBudget(), monthly: fallbackBudgetForMonth() };

  if (!nextBudget.monthly) {
    nextBudget.monthly = fallbackBudgetForMonth();
  }

  setCustomBudget(nextBudget);

  localStorage.setItem(
    getCustomBudgetStorageKey(monthKey),
    JSON.stringify(nextBudget)
  );

  if (
    nextBudget.monthly > 0 &&
    budgets[monthKey] !== nextBudget.monthly
  ) {
    setMonthlyBudget(monthKey, nextBudget.monthly);
  }
} else {
  const previousSnapshot = findClosestPreviousMonthSnapshot(
    selectedMonthIndex - 1,
    budgets
  );

  let nextBudget;

  if (previousSnapshot?.customBudget) {
    nextBudget = { ...previousSnapshot.customBudget };
  } else {
    nextBudget = createEmptyBudget();

    nextBudget.monthly =
      previousSnapshot?.budgetAmount || 1000000;
  }

  setCustomBudget(nextBudget);

  localStorage.setItem(
    getCustomBudgetStorageKey(monthKey),
    JSON.stringify(nextBudget)
  );

  if (nextBudget.monthly > 0) {
    setMonthlyBudget(monthKey, nextBudget.monthly);
  }
} // ← 바깥 else 종료

    const parsedPlans = parseAiPlans(savedPlans);

if (parsedPlans && parsedPlans.length > 0) {
  setAiPlans(parsedPlans);
} else {
  // 이전 달 플랜 자동 복원
  const previousSnapshot = findClosestPreviousMonthSnapshot(
    selectedMonthIndex - 1,
    budgets
  );

  if (previousSnapshot?.aiPlans?.length) {
    setAiPlans(previousSnapshot.aiPlans);

    localStorage.setItem(
      getAiPlansStorageKey(monthKey),
      JSON.stringify(previousSnapshot.aiPlans)
    );
  } else {
    setAiPlans([]);
  }
}

// 이미 위에서 읽은 값 사용
setSelectedPlan(savedSelectedPlan ?? null);
setIsBudgetLocked(localStorage.getItem(getBudgetLockStorageKey(monthKey)) === "true");

if (savedSelectedPlan && parsedPlans?.length) {
  const appliedPlan = parsedPlans.find(
    (p) => p.id === savedSelectedPlan
  );

  if (appliedPlan) {
    setCustomBudget({
      monthly: appliedPlan.budget,
      savings: appliedPlan.savings,
      food:
        appliedPlan.categories.find((c) => c.name === "식비")?.amount ?? 0,
      transport:
        appliedPlan.categories.find((c) => c.name === "교통비")?.amount ?? 0,
      living:
        appliedPlan.categories.find((c) => c.name === "생활비")?.amount ?? 0,
      leisure:
        appliedPlan.categories.find((c) => c.name === "여가/취미")?.amount ?? 0,
    });

    setMonthlyBudget(monthKey, appliedPlan.budget);
  }
} // ← 이거 추가

}, [monthKey]);

  useEffect(() => {
  if (!monthKey) return;

  localStorage.setItem(
    getAiPlansStorageKey(monthKey),
    JSON.stringify(aiPlans)
  );
}, [aiPlans, monthKey]);


  const handleApplyPlan = async (planId: string) => {

  if (!canEditBudget) {
    toast.error(budgetDisabledMessage);
    return;
  }

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

localStorage.setItem(
  getSelectedPlanStorageKey(monthKey),
  planId
);

localStorage.setItem(getBudgetLockStorageKey(monthKey), "true");
setIsBudgetLocked(true);

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

  const pendingApplyPlan = pendingApplyPlanId
    ? aiPlans.find((plan) => plan.id === pendingApplyPlanId) ?? null
    : null;

  const [loading, setLoading] = useState(false);

  const handleGenerateAiPlans = async () => {
  if (!canEditBudget) {
    toast.error(budgetDisabledMessage);
    return;
  }

  const sourceBudget = currentBudget || customBudget.monthly;

  if (!sourceBudget || sourceBudget <= 0) {
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
      `/api/budget/${budgetId}/ai-plan`,
      {
    total_budget: sourceBudget,
    savings_goal: customBudget.savings,
    year: selectedYear,
    month,
  }
    );

    const data = aiRes.data;

    // 👉 4. 프론트 형식으로 변환
    const mappedPlans: AiPlan[] = data.plans
      .map((p, idx) => ({
        id: PLAN_ORDER_LABELS[idx],
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

    // 새 AI 플랜 전체 교체
setAiPlans(mappedPlans);

// localStorage 저장
localStorage.setItem(
  getAiPlansStorageKey(monthKey),
  JSON.stringify(mappedPlans)
);

// 기존 적용 플랜 초기화
setSelectedPlan(null);

localStorage.removeItem(
  getSelectedPlanStorageKey(monthKey)
);

    toast.success("AI 플랜 생성 완료!");
  } catch (err) {
    console.error(err);
    toast.error("AI 플랜 생성 실패");
  } finally {
    setLoading(false);
  }
};

  const handleSaveCustomBudget = () => {
  if (!canEditBudget) {
    toast.error(budgetDisabledMessage);
    return;
  }

  const monthlyBudget = Number(customBudget.monthly) || 0;

  if (monthlyBudget < 300000) {
    toast.error("월 전체 예산은 최소 300,000원 이상이어야 합니다.");
    return;
  }

  setMonthlyBudget(monthKey, monthlyBudget);

  localStorage.setItem(
    getCustomBudgetStorageKey(monthKey),
    JSON.stringify(customBudget)
  );

  localStorage.setItem(getBudgetLockStorageKey(monthKey), "true");
  setIsBudgetLocked(true);

  toast.success(
    `${selectedYear}년 ${selectedMonth + 1}월 맞춤 예산이 저장되었습니다!`
  );
};

  const fitCategoryBudgetsToMonthly = (budget: CustomBudget): CustomBudget => {
    const monthlyLimit = Math.max(
  0,
  (Number(budget.monthly) || 0) - (Number(budget.savings) || 0)
);
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
      const monthlyLimit = Math.max(
  0,
  (Number(prev.monthly) || 0) - (Number(prev.savings) || 0)
);
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
  const remainingCategoryBudget = Math.max(
  0,
  Number(customBudget.monthly) -
    Number(customBudget.savings) -
    totalBudget
);
  const getCategorySliderMax = (key: BudgetCategoryKey) => {
    const otherTotal = BUDGET_CATEGORY_KEYS
      .filter((categoryKey) => categoryKey !== key)
      .reduce((sum, categoryKey) => sum + Number(customBudget[categoryKey]), 0);

    return Math.max(
  Number(customBudget[key]),
  Number(customBudget.monthly) -
    Number(customBudget.savings) -
    otherTotal,
  0
);
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
              className="h-9 w-9 rounded-lg border-sky-200/90 bg-[#f0f7ff] text-blue-700 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_8px_20px_rgba(37,99,235,0.08)] hover:border-blue-300 hover:bg-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
            >
              <ChevronLeft className="h-4 w-4" />
            </Button>

            <Popover open={isMonthPickerOpen} onOpenChange={setIsMonthPickerOpen}>
              <PopoverTrigger asChild>
                <button
                  type="button"
                  className="flex h-9 w-[184px] items-center justify-between rounded-[13px] border border-sky-200/90 bg-white px-2.5 text-left text-blue-900 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_8px_20px_rgba(37,99,235,0.08)] transition hover:border-blue-300 hover:bg-[#f0f7ff] dark:border-slate-700 dark:bg-[#111827] dark:text-gray-100 dark:hover:bg-slate-800"
                >
                  <span className="flex items-center gap-1 text-sm font-semibold">
                    <CalendarDays className="h-3.5 w-3.5 text-blue-600 dark:text-violet-300" />
                    {currentMonthLabel}
                  </span>
                  <ChevronDown className="h-3.5 w-3.5 text-blue-600 dark:text-gray-300" />
                </button>
              </PopoverTrigger>
              <PopoverContent
                align="start"
                sideOffset={6}
                className="z-50 w-[264px] rounded-2xl border border-sky-200/90 bg-white p-2 shadow-[0_18px_44px_rgba(37,99,235,0.12),0_4px_16px_rgba(15,23,42,0.06)] outline-none dark:border-slate-700 dark:bg-[#0b1020] dark:shadow-[0_18px_36px_rgba(0,0,0,0.35)]"
              >
                <div className="mb-1.5 flex items-center justify-between">
                  <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    onClick={() => setSelectedYear((prev) => Math.max(MIN_YEAR, prev - 1))}
                    disabled={selectedYear <= MIN_YEAR}
                    className="h-7 w-7 rounded-md border-sky-200/90 bg-[#f0f7ff] text-blue-700 hover:border-blue-300 hover:bg-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
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
                    className="h-7 w-7 rounded-md border-sky-200/90 bg-[#f0f7ff] text-blue-700 hover:border-blue-300 hover:bg-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
                  >
                    <ChevronRight className="h-3.5 w-3.5" />
                  </Button>
                </div>

                <div className="grid grid-cols-3 gap-0.5">
  {currentYearMonths.map(({ monthIndex, label, selected }) => {
    const isFutureMonth =
      selectedYear > CURRENT_YEAR ||
      (selectedYear === CURRENT_YEAR &&
        monthIndex > CURRENT_MONTH);

    return (
      <button
        key={`${selectedYear}-${monthIndex}`}
        type="button"
        disabled={isFutureMonth}
        onClick={() => {
          if (isFutureMonth) return;

          setSelectedMonth(monthIndex);
          setIsMonthPickerOpen(false);
        }}
        className={`flex h-9 items-center justify-center rounded-lg text-sm font-medium transition
          ${
            selected
              ? "bg-sky-100 text-blue-800 ring-1 ring-blue-200 shadow-[0_8px_20px_rgba(37,99,235,0.12)] dark:bg-violet-500"
              : isFutureMonth
              ? "cursor-not-allowed bg-gray-100 text-gray-400 dark:bg-slate-800 dark:text-gray-500"
              : "text-blue-800 hover:bg-sky-50 dark:text-gray-200 dark:hover:bg-slate-800"
          }
        `}
      >
        {label}
        {selected && <Check className="ml-1 h-3.5 w-3.5" />}
      </button>
    );
  })}
</div>
              </PopoverContent>
            </Popover>

            <Button
              type="button"
              variant="outline"
              size="icon"
              onClick={() => updateSelectedMonthByIndex(selectedMonthIndex + 1)}
              disabled={selectedMonthIndex >= TOTAL_MONTHS - 1}
              className="h-9 w-9 rounded-lg border-sky-200/90 bg-[#f0f7ff] text-blue-700 shadow-[inset_0_1px_0_rgba(255,255,255,0.9),0_8px_20px_rgba(37,99,235,0.08)] hover:border-blue-300 hover:bg-sky-100 dark:border-slate-700 dark:bg-slate-900 dark:text-gray-100 dark:hover:bg-slate-800"
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
          disabled={loading || !canEditBudget}
          className="spentopia-primary-button"
          title={!canEditBudget ? budgetDisabledMessage : undefined}
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

        {aiPlans.length === 0 ? (
  <Card className="border-none spentopia-surface-card p-6">
    <div className="rounded-3xl border border-dashed border-slate-300 dark:border-slate-700 px-6 py-20">
      <div className="flex flex-col items-center justify-center text-center">
        <Sparkles className="mb-5 h-10 w-10 text-slate-400 dark:text-violet-300" />

        <h3 className="mb-3 text-2xl font-bold text-slate-900 dark:text-white">
          아직 생성된 AI 플랜이 없어요
        </h3>

        <p className="text-sm text-slate-500 dark:text-slate-400">
          월 예산과 저축 목표를 입력한 뒤 AI 플랜 추천 버튼을 눌러보세요.
        </p>
      </div>
    </div>
  </Card>
) : (
  <div className="grid gap-6 md:grid-cols-3">
    {aiPlans.map((plan) => {

  return (
    <Card
      key={plan.id}
              className={`border-2 spentopia-surface-card p-6 backdrop-blur-xl transition-all  ${
                selectedPlan === plan.id
  ? "border-blue-400 bg-sky-50/70 shadow-[0_18px_44px_rgba(37,99,235,0.16)] dark:border-violet-400 dark:bg-violet-950/20 dark:shadow-[0_18px_44px_rgba(124,58,237,0.18)]"
                  : "border-transparent hover:border-sky-300 dark:hover:border-[#7c3aed]/40"
              }`}
            >
              <div className="mb-4">
                <div className="mb-2 flex items-start justify-between">
                  <h3 className="font-bold text-gray-900 dark:text-gray-100">
                    {plan.name}
                  </h3>
                  {selectedPlan === plan.id && (
                    <Badge className="bg-[#3b82f6] text-white dark:bg-[#2d1847] dark:text-white">적용중</Badge>
                  )}
                </div>
                <p className="text-sm text-gray-600 dark:text-gray-300">
                  {plan.description}
                </p>
              </div>

              <div className="mb-4 space-y-3">
                <div className="flex items-center justify-between rounded-lg spentopia-soft-card p-3">
                  <span className="text-sm font-semibold text-gray-900 dark:text-gray-100">월 예산</span>
                  <span className="font-bold text-gray-900 dark:text-gray-100">
                    {Number(plan.budget || 0).toLocaleString()}원
                  </span>
                </div>
                <div className="flex items-center justify-between rounded-lg spentopia-soft-card p-3">
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
  type="button"
  onClick={() => {
    setPendingApplyPlanId(plan.id);
  }}
  disabled={selectedPlan === plan.id || !canEditBudget}
  className={`w-full transition-all duration-300 ${
    selectedPlan === plan.id || !canEditBudget
      ? "bg-[#f0f7ff] border border-blue-200 text-blue-700 hover:bg-[#f0f7ff] cursor-default dark:border-violet-400/45 dark:bg-violet-950/35 dark:text-violet-100 dark:hover:bg-violet-950/35"
      : "spentopia-primary-button"
  }`}
>
  {selectedPlan === plan.id ? "적용됨 ✓" : "이 플랜 적용하기"}
</Button>
                </Card>
  );
})}
  </div>
)}
</div>

      <Card className="border-none spentopia-surface-card p-6 backdrop-blur-xl">
        <h2 className="mb-6 flex items-center gap-2 font-bold text-gray-900 dark:text-gray-100">
          <Target className="h-5 w-5 text-slate-700 dark:text-violet-300" />
          맞춤 예산 설정
        </h2>

        <div className="grid gap-8 lg:grid-cols-2">
          <div className="space-y-6">
            <div>
              <Label className="font-semibold text-gray-900 dark:text-gray-100">월 전체 예산</Label>
              <Input
  value={customBudget.monthly === 0 ? "" : customBudget.monthly}
  onChange={(e) => updateMonthlyBudget(Number(e.target.value) || 0)}
  className="mt-2"
  placeholder="월 예산을 입력하세요."
/>
            </div>

            <div>
              <Label className="font-semibold text-gray-900 dark:text-gray-100">목표 저축액</Label>
              <Input
  value={customBudget.savings === 0 ? "" : customBudget.savings}
  onChange={(e) => {
    const inputValue = Number(e.target.value) || 0;
    const maxSavings = Number(customBudget.monthly) || 0;

    setCustomBudget({
      ...customBudget,
      savings: Math.min(inputValue, maxSavings),
    });

    if (inputValue > maxSavings) {
      toast.error("목표 저축액은 월 전체 예산을 초과할 수 없습니다.");
    }
  }}
  max={customBudget.monthly}
  className="mt-2"
  placeholder="저축액을 입력하세요."
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
              onClick={handleSaveCustomBudget}
              disabled={!canEditBudget}
              className="w-full spentopia-primary-button"
              title={!canEditBudget ? budgetDisabledMessage : undefined}
            >
              <Wallet className="mr-2 h-4 w-4" />
              {selectedMonth + 1}월 맞춤 예산 저장
            </Button>
          </div>

          <div className="space-y-4">
            <Card className="border-none spentopia-hero-card p-6">
              <p className="mb-1 text-sm opacity-90">현재 설정한 월 예산</p>
              <p className="text-3xl font-bold">
                {Number(customBudget.monthly).toLocaleString()}원
              </p>
              <p className="mt-2 text-sm opacity-90">
                {selectedYear}년 {selectedMonth + 1}월 기준
              </p>
            </Card>

            <Card className="border-none spentopia-surface-card p-6">
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
                    <div className="h-3 overflow-hidden rounded-full bg-sky-100 shadow-[inset_0_0_0_1px_rgba(125,211,252,0.42),0_8px_20px_rgba(37,99,235,0.08)] dark:bg-gray-700">
                      <div
                        className="h-full bg-[#3b82f6] shadow-[0_0_14px_rgba(37,99,235,0.28)] dark:bg-[#2d1847] dark:shadow-[0_0_16px_rgba(124,58,237,0.36)]"
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

            <Card className="border-none spentopia-surface-card p-6">
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

      <Dialog
        open={pendingApplyPlan !== null}
        onOpenChange={(open) => {
          if (!open) setPendingApplyPlanId(null);
        }}
      >
        <DialogContent className="max-w-md overflow-hidden border-border bg-card p-0 shadow-soft">
          <div className="p-6 text-card-foreground">
            <DialogHeader>
              <div className="mb-4 flex items-center justify-between">
                <DialogTitle className="text-xl font-bold">예산 플랜 적용</DialogTitle>
                <Zap className="h-5 w-5 text-[#2563eb] dark:text-luxury-gold" />
              </div>
            </DialogHeader>

            <p className="mb-2 text-3xl font-extrabold text-slate-900 dark:text-gray-100">
              {pendingApplyPlan?.name ?? "선택한 플랜"}
            </p>
            <div className="mb-5 h-3 overflow-hidden rounded-full bg-[#dbeafe] shadow-inner dark:bg-slate-800">
              <div className="h-full w-full rounded-full bg-[linear-gradient(135deg,#3b82f6,#2563eb)] shadow-[0_0_14px_rgba(37,99,235,0.34)] dark:bg-[linear-gradient(90deg,#0f172a,#4338ca,#7c3aed)] dark:shadow-[0_0_18px_rgba(124,58,237,0.6)]" />
            </div>

            <div className="space-y-3 text-sm">
              <div className="flex justify-between">
                <span>월 예산</span>
                <span className="font-bold">
                  {Number(pendingApplyPlan?.budget ?? 0).toLocaleString()}원
                </span>
              </div>
              <div className="flex justify-between">
                <span>목표 저축</span>
                <span className="font-bold">
                  {Number(pendingApplyPlan?.savings ?? 0).toLocaleString()}원
                </span>
              </div>
            </div>

            <p className="mt-5 text-sm leading-6 text-muted-foreground">
              예산 설정은 월 1회만 적용 가능합니다. 이 플랜을 적용하시겠습니까?
            </p>

            <DialogFooter className="mt-6">
              <Button
                type="button"
                variant="outline"
                onClick={() => setPendingApplyPlanId(null)}
              >
                취소
              </Button>
              <Button
                type="button"
                className="spentopia-primary-button"
                onClick={async () => {
                  if (!pendingApplyPlan) return;
                  const planId = pendingApplyPlan.id;
                  setPendingApplyPlanId(null);
                  await handleApplyPlan(planId);
                }}
              >
                적용
              </Button>
            </DialogFooter>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  );
}
