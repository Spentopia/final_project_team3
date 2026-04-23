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

const WEEKLY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];
const MONTHLY_LABELS = Array.from({ length: 12 }, (_, index) => `${index + 1}월`);

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

const categoryData = [
  {
    name: "식비",
    value: 45,
    amount: 135000,
    color: "#f97316",
    meterClassName: styles.categoryMeterFood,
  },
  {
    name: "교통",
    value: 20,
    amount: 60000,
    color: "#3b82f6",
    meterClassName: styles.categoryMeterTransport,
  },
  {
    name: "쇼핑",
    value: 15,
    amount: 45000,
    color: "#ec4899",
    meterClassName: styles.categoryMeterShopping,
  },
  {
    name: "여가",
    value: 12,
    amount: 36000,
    color: "#a855f7",
    meterClassName: styles.categoryMeterLeisure,
  },
  {
    name: "기타",
    value: 8,
    amount: 24000,
    color: "#6b7280",
    meterClassName: styles.categoryMeterEtc,
  },
];

export default function Analytics() {
  const { transactions, replaceTransactions } = useFinance();
  const now = new Date();

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

  const weeklyData = buildWeeklyData(transactions, now);
  const monthlyData = buildMonthlyData(transactions, now.getFullYear());

  const thisMonthTransactions = expenseTransactionsOnly(transactions).filter((transaction) => {
    const date = parseTransactionDate(transaction.date);
    if (!date) return false;

    return (
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth()
    );
  });

  const totalExpense = thisMonthTransactions.reduce(
    (sum, transaction) => sum + transaction.amount,
    0
  );
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
            <span>지난 달 대비 -12%</span>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">일 평균 지출</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">23,571원</p>
          <div className="flex items-center gap-1 text-sm text-green-600">
            <TrendingDown className="h-4 w-4" />
            <span>-5% 절약중</span>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">예산 사용률</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">60%</p>
          <div className="h-2 overflow-hidden rounded-full bg-gray-200">
            <div className="h-full w-[60%] bg-gradient-to-r from-cyan-500 to-blue-500"></div>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">최다 소비 카테고리</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">🍔 식비</p>
          <p className="text-sm text-gray-600">전체의 45%</p>
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
                label={({ name, value }) => `${name} ${value}%`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {categoryData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <h3 className="mb-6 font-bold text-gray-900">카테고리 상세</h3>
          <div className="space-y-4">
            {categoryData.map((cat) => (
              <div key={cat.name} className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900">{cat.name}</span>
                  <span className="font-bold text-gray-900">{cat.amount.toLocaleString()}원</span>
                </div>
                <div className="flex items-center gap-2">
                  <div className="h-2 flex-1 overflow-hidden rounded-full bg-gray-200">
                    <div
                      className={`${styles.categoryMeter} ${cat.meterClassName}`}
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
