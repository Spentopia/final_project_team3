import { useEffect } from "react";
import { useFinance, type Transaction } from "@/shared/providers/FinanceProvider";
import { listExpenses } from "@/shared/api/expenseApi";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { analyzeReport } from "@/shared/api/aiApi";
import { useState } from "react";
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import { useRef } from "react";

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
  Download,
  Share2,
  Sparkles,
} from "lucide-react";
import styles from "./AnalyticsPage.module.css";
import {
  getMonthlyExpenseTotal,
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";

type AIReport = {
  good: string;
  warning: string;
  advice: string;
  prediction: string;
  pattern: string;
  improvement: string;
};

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

  const reportRef = useRef<HTMLDivElement>(null);

  const now = new Date();

  const thisMonthTransactions = expenseTransactionsOnly(transactions).filter((transaction) => {
  const date = parseTransactionDate(transaction.date);
  if (!date) return false;

  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth()
  );
});

const [aiReport, setAiReport] = useState<AIReport | null>(null);

const [isPdfMode, setIsPdfMode] = useState(false);

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

  useEffect(() => {
  if (transactions.length === 0) return;

  const fetchAIReport = async () => {
    try {
      const payload = {
        transactions: transactions.map((t) => ({
          date: t.date,
          amount: t.amount,
          category: t.category,
          type: t.type,
        })),

        totalExpense,
        budget: currentBudget,
        topCategory: topCategoryName,
        topCategoryPercent,

        dailyAverage,
        expenseChangeRate,
        budgetUsage,

        weeklyData,
        monthlyData,
        categoryData,
      };

      const result = await analyzeReport(payload) as AIReport;
setAiReport(result);
    } catch (error) {
      console.error("AI 리포트 실패", error);
    }
  };

  fetchAIReport();
}, [transactions]);

const handleShare = async () => {
  const text = `
📊 소비 분석 결과
총 지출: ${totalExpense.toLocaleString()}원
일 평균: ${dailyAverage.toLocaleString()}원
예산 사용률: ${budgetUsage}%
`;

  try {
    if (navigator.share) {
      await navigator.share({
        title: "소비 분석 리포트",
        text,
        url: window.location.href,
      });
    } else {
      await navigator.clipboard.writeText(text);
console.log("복사 완료");
    }
  } catch (err) {
    console.error(err);
  }
};

const handleDownload = async () => {
  if (!reportRef.current) return;

  try {
    setIsPdfMode(true); // 🔥 먼저 상태 변경

    reportRef.current.classList.add(styles["pdf-mode"]);

    // 🔥 렌더 반영 기다림
    await new Promise((r) => setTimeout(r, 100));

    const canvas = await html2canvas(reportRef.current, {
      scale: 2,
      useCORS: true,
      backgroundColor: "#ffffff",
    });

    reportRef.current.classList.remove(styles["pdf-mode"]);
    setIsPdfMode(false); // 🔥 다시 원래 상태

    const imgData = canvas.toDataURL("image/png");
    const pdf = new jsPDF("p", "mm", "a4");

    const imgWidth = 210;
    const imgHeight = (canvas.height * imgWidth) / canvas.width;

    let heightLeft = imgHeight;
    let position = 0;

    pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
    heightLeft -= 297;

    while (heightLeft > 0) {
      position = -(imgHeight - heightLeft);
      pdf.addPage();
      pdf.addImage(imgData, "PNG", 0, position, imgWidth, imgHeight);
      heightLeft -= 297;
    }

    pdf.save("소비_분석_리포트.pdf");
  } catch (err) {
    console.error("PDF 생성 실패", err);
  }
};



  const timePatternData = buildTimePatternData(thisMonthTransactions);
  const timePatternTotal = timePatternData.reduce((sum, slot) => sum + slot.amount, 0);
  const weekdayPatternData = buildWeekdayPatternData(thisMonthTransactions);
  const paymentPatternData = buildPaymentPatternData(thisMonthTransactions);

  return (
    <div ref={reportRef} className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">소비 패턴 분석</h1>
          <p className="text-gray-600 dark:text-gray-300">AI가 분석한 당신의 소비 습관을 확인해보세요</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleShare}>
            <Share2 className="mr-2 h-4 w-4" />
            공유
          </Button>
          <Button
  className={isPdfMode ? "bg-blue-500 text-white" : "bg-gradient-to-r from-cyan-500 to-blue-500"}
  onClick={handleDownload}
>
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
            <div
  className="h-full bg-gradient-to-r from-cyan-500 to-blue-500"
  style={{ width: `${budgetUsage}%` }}
></div>
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
                <Bar dataKey="amount" fill={isPdfMode ? "#8884d8" : "url(#colorGradient)"} radius={[8, 8, 0, 0]} />
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
  stroke={isPdfMode ? "#8884d8" : "#a855f7"}
  strokeWidth={3}
  dot={{ fill: isPdfMode ? "#8884d8" : "#a855f7", r: 6 }}
/>
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Category Analysis */}
      <div className="grid gap-6 lg:grid-cols-2 items-stretch">
        <Card className="h-full min-h-[400px] flex flex-col border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-6 font-bold text-gray-900">카테고리별 지출</h3>
          <div className="flex-1">
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={categoryData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) =>
  `${name} ${(percent * 100).toFixed(0)}%`
}
                outerRadius="80%"
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
          </div>
        </Card>

        <Card className="h-full min-h-[400px] border-none bg-white/80 p-6 backdrop-blur-xl">
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
<Card className="border-none bg-gradient-to-br from-purple-50 to-pink-50 dark:from-gray-800 dark:to-gray-900 p-6 backdrop-blur-xl">
  <div className="mb-4 flex items-center gap-2">
    <Sparkles className="h-5 w-5 text-cyan-600" />
    <h3 className="font-bold text-gray-900 dark:text-white">AI 소비 분석 리포트</h3>
  </div>

  {aiReport && (
    <div className="grid gap-4 md:grid-cols-2">
      
      <div className="rounded-lg border bg-white dark:bg-gray-800 p-4">
        <h4 className="font-bold text-gray-900 dark:text-white">👍 좋은 점</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300">{aiReport.good}</p>
      </div>

      <div className="rounded-lg border bg-white dark:bg-gray-800 p-4">
        <h4 className="font-bold text-gray-900 dark:text-white">⚠️ 주의</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300">{aiReport.warning}</p>
      </div>

      <div className="rounded-lg border bg-white dark:bg-gray-800 p-4">
        <h4 className="font-bold text-gray-900 dark:text-white">💡 조언</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300">{aiReport.advice}</p>
      </div>

      <div className="rounded-lg border bg-white dark:bg-gray-800 p-4">
        <h4 className="font-bold text-gray-900 dark:text-white">📈 예측</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300">{aiReport.prediction}</p>
      </div>

    </div>
  )}
</Card>

      {/* Spending Patterns */}
<Card className="border-none bg-white/80 dark:bg-gray-900 p-6 backdrop-blur-xl">
  <h3 className="mb-6 font-bold text-gray-900 dark:text-white">소비 패턴 분석</h3>

  {aiReport ? (
    <div className="grid gap-6 md:grid-cols-2">

      <div className="rounded-lg bg-gray-50 dark:bg-gray-800 p-5">
        <h4 className="mb-2 font-bold text-gray-900 dark:text-white">📊 분석</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          {aiReport.pattern.replace("소비 패턴 분석:", "")}
        </p>
      </div>

      <div className="rounded-lg bg-gray-50 dark:bg-gray-800 p-5">
        <h4 className="mb-2 font-bold text-gray-900 dark:text-white">💡 개선 방안</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          {aiReport.improvement}
        </p>
      </div>

    </div>
  ) : (
    <p className="text-sm text-gray-500 dark:text-gray-400">AI가 분석 중입니다...</p>
  )}
</Card>
    </div>
  );
}
