import { useEffect } from "react";
import { useFinance, type Transaction } from "@/shared/providers/FinanceProvider";
import { listExpenses } from "@/shared/api/expenseApi";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Badge } from "@/shared/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";

import {
  BarChart,
  Bar,
  LineChart,
  Line,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from "recharts";
import {
  TrendingDown,
  TrendingUp,
  Download,
  Share2,
  Sparkles,
  AlertTriangle,
  CheckCircle,
} from "lucide-react";
import styles from "./AnalyticsPage.module.css";
import {
  getMonthlyExpenseTotal,
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";

const WEEKLY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];
const MONTHLY_LABELS = Array.from({ length: 12 }, (_, index) => `${index + 1}월`);

const CATEGORY_MAP: Record<string, { label: string; icon: string }> = {
  food: { label: "식비", icon: "🍔" },
  transport: { label: "교통", icon: "🚌" },
  shopping: { label: "쇼핑", icon: "🛍️" },
  entertainment: { label: "여가", icon: "🎮" },
  health: { label: "의료", icon: "💊" },
  education: { label: "교육", icon: "📚" },
  utility: { label: "공과금", icon: "💡" },
  other: { label: "기타", icon: "📦" },
};

const parseTransactionDate = (dateValue: string) => {
  const [year, month, day] = dateValue.split("-").map(Number);

  if (year && month && day) {
    return new Date(year, month - 1, day);
  }

  const fallbackDate = new Date(dateValue);
  return Number.isNaN(fallbackDate.getTime()) ? null : fallbackDate;
};

const getWeekStart = (date: Date) => {
  const weekStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const mondayOffset = (weekStart.getDay() + 6) % 7;
  weekStart.setDate(weekStart.getDate() - mondayOffset);
  weekStart.setHours(0, 0, 0, 0);
  return weekStart;
};

const expenseTransactionsOnly = (transactions: Transaction[]) =>
  transactions.filter((transaction) => (transaction.type ?? "expense") === "expense");

const buildWeeklyData = (transactions: Transaction[], baseDate: Date) => {
  const weekStart = getWeekStart(baseDate);
  const dailyTotals = Array(7).fill(0);

  expenseTransactionsOnly(transactions).forEach((transaction) => {
    const date = parseTransactionDate(transaction.date);
    if (!date) return;

    const dayIndex = Math.floor((date.getTime() - weekStart.getTime()) / (1000 * 60 * 60 * 24));

    if (dayIndex >= 0 && dayIndex < 7) {
      dailyTotals[dayIndex] += transaction.amount;
    }
  });

  return WEEKLY_LABELS.map((day, index) => ({
    day,
    amount: dailyTotals[index],
  }));
};

const buildMonthlyData = (transactions: Transaction[], year: number) => {
  const monthlyTotals = Array(12).fill(0);

  expenseTransactionsOnly(transactions).forEach((transaction) => {
    const date = parseTransactionDate(transaction.date);
    if (!date || date.getFullYear() !== year) return;

    monthlyTotals[date.getMonth()] += transaction.amount;
  });

  return MONTHLY_LABELS.map((month, index) => ({
    month,
    amount: monthlyTotals[index],
  }));
};

const hasExplicitTime = (dateValue: string) => /[T\s]\d{1,2}:\d{2}/.test(dateValue);

const calculatePercent = (amount: number, total: number) =>
  total > 0 ? Math.round((amount / total) * 100) : 0;

const buildTimePatternData = (transactions: Transaction[]) => {
  const slots = [
    { label: "새벽 (00-06시)", amount: 0 },
    { label: "오전 (06-12시)", amount: 0 },
    { label: "오후 (12-18시)", amount: 0 },
    { label: "저녁 (18-24시)", amount: 0 },
  ];

  transactions.forEach((transaction) => {
    if (!hasExplicitTime(transaction.date)) return;

    const date = parseTransactionDate(transaction.date);
    if (!date) return;

    const hour = date.getHours();
    const slotIndex = hour < 6 ? 0 : hour < 12 ? 1 : hour < 18 ? 2 : 3;
    slots[slotIndex].amount += transaction.amount;
  });

  const total = slots.reduce((sum, slot) => sum + slot.amount, 0);

  return slots.map((slot) => ({
    ...slot,
    percent: calculatePercent(slot.amount, total),
  }));
};

const buildWeekdayPatternData = (transactions: Transaction[]) => {
  const totals = transactions.reduce(
    (result, transaction) => {
      const date = parseTransactionDate(transaction.date);
      if (!date) return result;

      const day = date.getDay();
      const key = day === 0 || day === 6 ? "weekend" : "weekday";
      result[key] += transaction.amount;
      return result;
    },
    { weekday: 0, weekend: 0 }
  );

  if (totals.weekday === 0 && totals.weekend === 0) {
    return {
      ...totals,
      description: "이번 달 소비 데이터가 없어요",
    };
  }

  if (totals.weekday === totals.weekend) {
    return {
      ...totals,
      description: "평일과 주말 소비가 같아요",
    };
  }

  if (totals.weekday === 0) {
    return {
      ...totals,
      description: "이번 달 주말 소비만 기록됐어요",
    };
  }

  if (totals.weekend === 0) {
    return {
      ...totals,
      description: "이번 달 평일 소비만 기록됐어요",
    };
  }

  const [largerLabel, smallerAmount, largerAmount] =
    totals.weekend > totals.weekday
      ? ["주말", totals.weekday, totals.weekend]
      : ["평일", totals.weekend, totals.weekday];
  const increaseRate = Math.round(((largerAmount - smallerAmount) / smallerAmount) * 100);

  return {
    ...totals,
    description: `${largerLabel} 소비가 ${increaseRate}% 더 많아요`,
  };
};

const getPaymentMethod = (transaction: Transaction) => {
  const patternTransaction = transaction as Transaction & {
    paymentMethod?: string;
    payment?: string;
    method?: string;
  };

  return patternTransaction.paymentMethod ?? patternTransaction.payment ?? patternTransaction.method;
};

const buildPaymentPatternData = (transactions: Transaction[]) => {
  const totals = new Map<string, number>();

  transactions.forEach((transaction) => {
    const method = getPaymentMethod(transaction);
    if (!method) return;

    totals.set(method, (totals.get(method) ?? 0) + transaction.amount);
  });

  const total = Array.from(totals.values()).reduce((sum, amount) => sum + amount, 0);

  return Array.from(totals.entries())
    .map(([method, amount]) => ({
      method,
      amount,
      percent: calculatePercent(amount, total),
    }))
    .sort((a, b) => b.amount - a.amount);
};

export default function Analytics() {
  const { transactions, replaceTransactions, budgets } = useFinance();
  const now = new Date();

  const thisMonthTransactions = expenseTransactionsOnly(transactions).filter((transaction) => {
  const date = parseTransactionDate(transaction.date);
  if (!date) return false;

  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth()
  );
});

// 총 지출
const totalExpense = getMonthlyExpenseTotal(transactions, now);

// ✅ 일 평균
const days = new Date().getDate();
const dailyAverage = Math.round(totalExpense / days);

// 🔥 이번 달 / 지난 달 계산 (여기로 이동)
const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);

const currentMonthExpense = getMonthlyExpenseTotal(transactions, now);
const lastMonthExpense = getMonthlyExpenseTotal(transactions, lastMonth);

// 🔥 총 지출 변화율
const expenseChangeRate =
  lastMonthExpense > 0
    ? Math.round(((currentMonthExpense - lastMonthExpense) / lastMonthExpense) * 100)
    : 0;

// 🔥 지난달 일 평균
const lastMonthDays = new Date(now.getFullYear(), now.getMonth(), 0).getDate();
const lastMonthDailyAverage =
  lastMonthExpense > 0 ? Math.round(lastMonthExpense / lastMonthDays) : 0;

// 🔥 일 평균 변화율
const dailyChangeRate =
  lastMonthDailyAverage > 0
    ? Math.round(((dailyAverage - lastMonthDailyAverage) / lastMonthDailyAverage) * 100)
    : 0;

// ✅ 2. 예산 사용률
const monthKey = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}`;
const currentBudget = budgets[monthKey] ?? 0;

const budgetUsage =
  currentBudget > 0
    ? Math.round((totalExpense / currentBudget) * 100)
    : 0;

// ✅ 3. 카테고리 계산
const categoryTotals = thisMonthTransactions.reduce((acc, cur) => {
  const key = cur.category ?? "기타";
  acc[key] = (acc[key] ?? 0) + cur.amount;
  return acc;
}, {} as Record<string, number>);

// 최다 카테고리
const topCategory = Object.entries(categoryTotals).sort((a, b) => b[1] - a[1])[0];

const topCategoryName =
  CATEGORY_MAP[topCategory?.[0] ?? ""]?.label ?? "없음";
const topCategoryPercent = totalExpense
  ? Math.round((topCategory?.[1] ?? 0) / totalExpense * 100)
  : 0;

// 파이 차트용 데이터
const COLORS = ["#a855f7", "#ec4899", "#22c55e", "#f59e0b", "#3b82f6", "#ef4444"];

const categoryData = Object.entries(categoryTotals).map(([name, amount], index) => ({
  key: name, // 원래 key 따로 보관
  name: CATEGORY_MAP[name]?.label ?? name, // 🔥 한국어 변환
  amount,
  value: totalExpense ? Math.round((amount / totalExpense) * 100) : 0,
  color: COLORS[index % COLORS.length],
}));

  useEffect(() => {
    if (transactions.length > 0) return;

    let cancelled = false;

    const loadExpenses = async () => {
      try {
        const items = await listExpenses();
        if (cancelled) return;

        replaceTransactions(
          items.map((item) => ({
            id: item.id,
            date: item.date,
            amount: item.amount,
            category: item.category,
            memo: item.memo ?? "",
            type: item.transactionType,
            receipt: item.transactionType === "expense" ? item.receiptVerified : undefined,
            diary: item.transactionType === "expense" ? (item.diary ?? "") : undefined,
          }))
        );
      } catch (error) {
        console.error("소비 내역 조회 실패:", error);
      }
    };

    void loadExpenses();

    return () => {
      cancelled = true;
    };
  }, [transactions.length]);

  const weeklyData = buildWeeklyData(thisMonthTransactions, now);
  const monthlyData = buildMonthlyData(transactions, now.getFullYear());

  const timePatternData = buildTimePatternData(thisMonthTransactions);
  const timePatternTotal = timePatternData.reduce((sum, slot) => sum + slot.amount, 0);
  const weekdayPatternData = buildWeekdayPatternData(thisMonthTransactions);
  const paymentPatternData = buildPaymentPatternData(thisMonthTransactions);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">소비 패턴 분석</h1>
          <p className="text-gray-600 dark:text-gray-300">AI가 분석한 당신의 소비 습관을 확인해보세요</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline">
            <Share2 className="mr-2 h-4 w-4" />
            공유
          </Button>
          <Button className="bg-gradient-to-r from-cyan-500 to-blue-500">
            <Download className="mr-2 h-4 w-4" />
            리포트 다운로드
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-6 md:grid-cols-4">
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <p className="mb-1 text-sm opacity-90">이번 달 총 지출</p>
          <p className="mb-2 text-3xl font-bold">
  {totalExpense.toLocaleString()}원
</p>
          <div className="flex items-center gap-1 text-sm">
            <TrendingDown className="h-4 w-4" />
            <span>
  지난 달 대비 {expenseChangeRate > 0 ? "+" : ""}
  {expenseChangeRate}%
</span>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">일 평균 지출</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">
  {dailyAverage.toLocaleString()}원
</p>
<div className="flex items-center gap-1 text-sm">
  <TrendingDown className="h-4 w-4" />
  <span>
    {dailyChangeRate > 0 ? "+" : ""}
    {dailyChangeRate}%
  </span>
</div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">예산 사용률</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">{budgetUsage}%</p>
          <div className="h-2 overflow-hidden rounded-full bg-gray-200">
            <div className="h-full w-[60%] bg-gradient-to-r from-cyan-500 to-blue-500"></div>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">최다 소비 카테고리</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">{topCategoryName}</p>
          <p className="text-sm text-gray-600">전체의 {topCategoryPercent}%</p>
        </Card>
      </div>

      {/* Main Charts */}
      <Tabs defaultValue="weekly" className="space-y-6">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="weekly">주간</TabsTrigger>
          <TabsTrigger value="monthly">월간</TabsTrigger>
        </TabsList>

        <TabsContent value="weekly" className="space-y-6">
          <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
            <h3 className="mb-6 font-bold text-gray-900">주간 소비 추이</h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={weeklyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="day" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                  }}
                />
                <Bar dataKey="amount" fill="url(#colorGradient)" radius={[8, 8, 0, 0]} />
                <defs>
                  <linearGradient id="colorGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#a855f7" />
                    <stop offset="100%" stopColor="#ec4899" />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>

        <TabsContent value="monthly" className="space-y-6">
          <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
            <h3 className="mb-6 font-bold text-gray-900">월간 소비 추이</h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={monthlyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="month" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                  }}
                />
                <Line
                  type="monotone"
                  dataKey="amount"
                  stroke="#a855f7"
                  strokeWidth={3}
                  dot={{ fill: "#a855f7", r: 6 }}
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Category Analysis */}
      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-6 font-bold text-gray-900">카테고리별 지출</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={categoryData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) =>
  `${CATEGORY_MAP[name]?.label ?? name} ${(percent * 100).toFixed(0)}%`
}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {categoryData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
  formatter={(value, name) => [
    `${value}%`,
    CATEGORY_MAP[name as string]?.label ?? name,
  ]}
/>
            </PieChart>
          </ResponsiveContainer>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-6 font-bold text-gray-900">카테고리 상세</h3>
          <div className="space-y-4">
            {categoryData.map((cat) => (
              <div key={cat.name} className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">
  {CATEGORY_MAP[cat.key]?.icon} {cat.name}
</span>
                  <span className="font-bold text-gray-900">{cat.amount.toLocaleString()}원</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-gray-200">
                    <div
  className="h-full"
  style={{ backgroundColor: cat.color }}
></div>
                  </div>
                  <span className="text-sm font-medium text-gray-600">{cat.value}%</span>
                </div>
              </div>
            ))}
          </div>
        </Card>
      </div>

      {/* AI Insights */}
      <Card className="border-none bg-gradient-to-br from-purple-50 to-pink-50 p-6 backdrop-blur-xl">
        <div className="mb-4 flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-cyan-600" />
          <h3 className="font-bold text-gray-900">AI 소비 분석 리포트</h3>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg border border-cyan-200 bg-white p-4">
            <div className="mb-2 flex items-start gap-2">
              <CheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-green-500" />
              <div>
                <h4 className="mb-1 font-bold text-gray-900">잘하고 있어요!</h4>
                <p className="text-sm text-gray-700">
                  식비 지출이 지난 달 대비 15% 감소했어요. 집밥 먹기를 실천하고 계시네요! 👏
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-cyan-200 bg-white p-4">
            <div className="mb-2 flex items-start gap-2">
              <CheckCircle className="mt-0.5 h-5 w-5 shrink-0 text-green-500" />
              <div>
                <h4 className="mb-1 font-bold text-gray-900">절약 습관 형성</h4>
                <p className="text-sm text-gray-700">
                  대중교통 이용이 늘어나면서 교통비가 20% 절약되었어요. 환경도 지키고 돈도 아끼고! 🚇
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-amber-200 bg-white p-4">
            <div className="mb-2 flex items-start gap-2">
              <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-amber-500" />
              <div>
                <h4 className="mb-1 font-bold text-gray-900">주의가 필요해요</h4>
                <p className="text-sm text-gray-700">
                  여가/취미 지출이 예산의 150%를 초과했어요. 다음 달엔 조금만 줄여보는 건 어떨까요?
                </p>
              </div>
            </div>
          </div>

          <div className="rounded-lg border border-blue-200 bg-white p-4">
            <div className="mb-2 flex items-start gap-2">
              <TrendingUp className="mt-0.5 h-5 w-5 shrink-0 text-blue-500" />
              <div>
                <h4 className="mb-1 font-bold text-gray-900">목표 달성 예상</h4>
                <p className="text-sm text-gray-700">
                  이 속도면 연말까지 저축 목표 100만원을 달성할 수 있어요! 화이팅! 💪
                </p>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* Spending Patterns */}
      <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
        <h3 className="mb-6 font-bold text-gray-900">소비 패턴 분석</h3>
        <div className="grid gap-6 md:grid-cols-3">
          <div>
            <h4 className="mb-3 font-bold text-gray-900">시간대별 소비</h4>
            <div className="space-y-3">
              {timePatternTotal > 0 ? (
                timePatternData.map((slot) => (
                  <div key={slot.label} className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">{slot.label}</span>
                    <div className="flex items-center gap-2">
                      <div className="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
                        <div
                          className="h-full bg-cyan-500"
                          style={{ width: `${slot.percent}%` }}
                        ></div>
                      </div>
                      <span className="text-sm font-bold text-gray-900">{slot.percent}%</span>
                    </div>
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-500">
                  시간 정보가 입력된 소비가 없어요
                </p>
              )}
            </div>
          </div>

          <div>
            <h4 className="mb-3 font-bold text-gray-900">요일별 소비</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">평일</span>
                <Badge>{weekdayPatternData.weekday.toLocaleString()}원</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">주말</span>
                <Badge variant="secondary">{weekdayPatternData.weekend.toLocaleString()}원</Badge>
              </div>
              <p className="text-sm text-gray-600">{weekdayPatternData.description}</p>
            </div>
          </div>

          <div>
            <h4 className="mb-3 font-bold text-gray-900">결제 방법</h4>
            <div className="space-y-3">
              {paymentPatternData.length > 0 ? (
                paymentPatternData.map((payment) => (
                  <div key={payment.method} className="flex items-center justify-between">
                    <span className="text-sm text-gray-600">{payment.method}</span>
                    <span className="text-sm font-bold text-gray-900">{payment.percent}%</span>
                  </div>
                ))
              ) : (
                <p className="text-sm text-gray-500">
                  결제 방법이 입력된 소비가 없어요
                </p>
              )}
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
