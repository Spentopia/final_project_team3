import { useEffect, useState } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";
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

const categoryColorMap: Record<string, string> = {
  food: "#f97316",
  transport: "#3b82f6",
  shopping: "#ec4899",
  entertainment: "#a855f7",
  health: "#22c55e",
  education: "#6366f1",
  utility: "#eab308",
  other: "#6b7280",
};

const categoryLabelMap: Record<string, string> = {
  food: "식비",
  transport: "교통",
  shopping: "쇼핑",
  entertainment: "여가",
  health: "의료",
  education: "교육",
  utility: "공과금",
  other: "기타",
};

export default function AnalyticsPage() {
  const { transactions, replaceTransactions, budgets } = useFinance();
  const [selectedMonth, setSelectedMonth] = useState(new Date());

  useEffect(() => {
    let cancelled = false;

    const loadExpenses = async () => {
      try {
        if (transactions.length > 0) return;

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
            receipt:
              item.transactionType === "expense"
                ? item.receiptVerified
                : undefined,
            diary:
              item.transactionType === "expense"
                ? (item.diary ?? "")
                : undefined,
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
  }, [transactions.length, replaceTransactions]);

  const currentMonthKey = `${selectedMonth.getFullYear()}-${String(
    selectedMonth.getMonth() + 1
  ).padStart(2, "0")}`;

  const currentBudget = budgets[currentMonthKey] || 0;

  const thisMonthTransactions = transactions.filter((t) => {
    const date = new Date(t.date);
    return (
      (t.type ?? "expense") === "expense" &&
      date.getFullYear() === selectedMonth.getFullYear() &&
      date.getMonth() === selectedMonth.getMonth()
    );
  });

  const previousMonth = new Date(
    selectedMonth.getFullYear(),
    selectedMonth.getMonth() - 1,
    1
  );

  const previousMonthTransactions = transactions.filter((t) => {
    const date = new Date(t.date);
    return (
      (t.type ?? "expense") === "expense" &&
      date.getFullYear() === previousMonth.getFullYear() &&
      date.getMonth() === previousMonth.getMonth()
    );
  });

  const totalExpense = thisMonthTransactions.reduce(
    (sum, t) => sum + t.amount,
    0
  );

  const previousMonthTotalExpense = previousMonthTransactions.reduce(
    (sum, t) => sum + t.amount,
    0
  );

  const expenseChangePercent =
    previousMonthTotalExpense > 0
      ? Math.round(
          ((totalExpense - previousMonthTotalExpense) /
            previousMonthTotalExpense) *
            100
        )
      : 0;

  const daysInMonth = new Date(
    selectedMonth.getFullYear(),
    selectedMonth.getMonth() + 1,
    0
  ).getDate();

  const averageDailyExpense =
    totalExpense > 0 ? Math.round(totalExpense / daysInMonth) : 0;

  const usageRate =
    currentBudget > 0 ? Math.round((totalExpense / currentBudget) * 100) : 0;

  const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

  const weeklyData = weekDays.map((day, index) => {
    const total = thisMonthTransactions
      .filter((t) => new Date(t.date).getDay() === index)
      .reduce((sum, t) => sum + t.amount, 0);

    return {
      day,
      amount: total,
    };
  });

  const monthlyData = Array.from({ length: 12 }, (_, i) => {
    const total = transactions
      .filter((t) => {
        const date = new Date(t.date);
        return (
          (t.type ?? "expense") === "expense" &&
          date.getFullYear() === selectedMonth.getFullYear() &&
          date.getMonth() === i
        );
      })
      .reduce((sum, t) => sum + t.amount, 0);

    return {
      month: `${i + 1}월`,
      amount: total,
    };
  });

  const categoryMap: Record<string, number> = {};

  thisMonthTransactions.forEach((t) => {
    const key = t.category || "other";
    categoryMap[key] = (categoryMap[key] || 0) + t.amount;
  });

  const categoryTotal = Object.values(categoryMap).reduce((a, b) => a + b, 0);

  const categoryData = Object.entries(categoryMap).map(([key, value]) => {
    const meterClassNameMap: Record<string, string> = {
      food: styles.categoryMeterFood,
      transport: styles.categoryMeterTransport,
      shopping: styles.categoryMeterShopping,
      entertainment: styles.categoryMeterLeisure,
      other: styles.categoryMeterEtc,
    };

    return {
      name: categoryLabelMap[key] || "기타",
      value: categoryTotal > 0 ? Math.round((value / categoryTotal) * 100) : 0,
      amount: value,
      color: categoryColorMap[key] || "#6b7280",
      meterClassName: meterClassNameMap[key] || styles.categoryMeterEtc,
    };
  });

  const topCategory = [...categoryData].sort((a, b) => b.amount - a.amount)[0];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <div className="mb-3 flex gap-2">
            <Button
              variant="outline"
              onClick={() =>
                setSelectedMonth(
                  new Date(
                    selectedMonth.getFullYear(),
                    selectedMonth.getMonth() - 1,
                    1
                  )
                )
              }
            >
              ◀ 이전달
            </Button>

            <div className="flex items-center rounded-lg border px-4 font-bold text-gray-900 dark:text-gray-100">
              {selectedMonth.getFullYear()}년 {selectedMonth.getMonth() + 1}월
            </div>

            <Button
              variant="outline"
              onClick={() =>
                setSelectedMonth(
                  new Date(
                    selectedMonth.getFullYear(),
                    selectedMonth.getMonth() + 1,
                    1
                  )
                )
              }
            >
              다음달 ▶
            </Button>
          </div>

          <h1 className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
            소비 패턴 분석
          </h1>
          <p className="text-gray-600 dark:text-gray-300">
            AI가 분석한 당신의 소비 습관을 확인해보세요
          </p>
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

      <div className="grid gap-6 md:grid-cols-4">
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <p className="mb-1 text-sm opacity-90">이번 달 총 지출</p>
          <p className="mb-2 text-3xl font-bold">
            {totalExpense.toLocaleString()}원
          </p>
          <div className="flex items-center gap-1 text-sm">
            {expenseChangePercent <= 0 ? (
              <TrendingDown className="h-4 w-4" />
            ) : (
              <TrendingUp className="h-4 w-4" />
            )}
            <span>
              지난 달 대비{" "}
              {expenseChangePercent > 0 ? "+" : ""}
              {expenseChangePercent}%
            </span>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <p className="mb-1 text-sm text-gray-600 dark:text-gray-300">
            일 평균 지출
          </p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
            {averageDailyExpense.toLocaleString()}원
          </p>
          <div
            className={`flex items-center gap-1 text-sm ${
              expenseChangePercent <= 0 ? "text-green-600" : "text-red-500"
            }`}
          >
            {expenseChangePercent <= 0 ? (
              <TrendingDown className="h-4 w-4" />
            ) : (
              <TrendingUp className="h-4 w-4" />
            )}
            <span>
              {expenseChangePercent <= 0
                ? "지출 관리중"
                : "지난달보다 증가"}
            </span>
          </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <p className="mb-1 text-sm text-gray-600 dark:text-gray-300">
            예산 사용률
          </p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
            {usageRate}%
          </p>
          <div className="h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
            <div
              className="h-full bg-gradient-to-r from-cyan-500 to-blue-500"
              style={{ width: `${Math.min(usageRate, 100)}%` }}
            />
          </div>
          <p className="mt-2 text-xs text-gray-500 dark:text-gray-400">
            월 예산 {currentBudget.toLocaleString()}원 기준
          </p>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <p className="mb-1 text-sm text-gray-600 dark:text-gray-300">
            최다 소비 카테고리
          </p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
            {topCategory ? topCategory.name : "데이터 없음"}
          </p>
          <p className="text-sm text-gray-600 dark:text-gray-300">
            {topCategory ? `전체의 ${topCategory.value}%` : "이번 달 기록 없음"}
          </p>
        </Card>
      </div>

      <Tabs defaultValue="weekly" className="space-y-6">
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="weekly">주간</TabsTrigger>
          <TabsTrigger value="monthly">월간</TabsTrigger>
        </TabsList>

        <TabsContent value="weekly" className="space-y-6">
          <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
            <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">
              요일별 소비 추이
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart data={weeklyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="day" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString()}원`, "지출"]}
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                  }}
                />
                <Bar dataKey="amount" fill="url(#weeklyGradient)" radius={[8, 8, 0, 0]} />
                <defs>
                  <linearGradient id="weeklyGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor="#06b6d4" />
                    <stop offset="100%" stopColor="#3b82f6" />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>

        <TabsContent value="monthly" className="space-y-6">
          <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
            <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">
              월별 소비 추이
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <LineChart data={monthlyData}>
                <CartesianGrid strokeDasharray="3 3" stroke="#e5e7eb" />
                <XAxis dataKey="month" stroke="#6b7280" />
                <YAxis stroke="#6b7280" />
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString()}원`, "지출"]}
                  contentStyle={{
                    backgroundColor: "white",
                    border: "1px solid #e5e7eb",
                    borderRadius: "8px",
                  }}
                />
                <Legend />
                <Line
                  type="monotone"
                  dataKey="amount"
                  stroke="#06b6d4"
                  strokeWidth={3}
                  dot={{ r: 4 }}
                  activeDot={{ r: 6 }}
                  name="지출"
                />
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>
      </Tabs>

      <div className="grid gap-6 lg:grid-cols-2">
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">
            카테고리별 소비 비율
          </h3>

          {categoryData.length === 0 ? (
            <div className="flex h-[300px] items-center justify-center text-gray-500 dark:text-gray-400">
              이번 달 소비 데이터가 없어요
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={300}>
              <PieChart>
                <Pie
                  data={categoryData}
                  cx="50%"
                  cy="50%"
                  innerRadius={70}
                  outerRadius={110}
                  paddingAngle={4}
                  dataKey="amount"
                  nameKey="name"
                >
                  {categoryData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.color} />
                  ))}
                </Pie>
                <Tooltip
                  formatter={(value: number) => [`${value.toLocaleString()}원`, "지출"]}
                />
                <Legend />
              </PieChart>
            </ResponsiveContainer>
          )}
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <div className="mb-6 flex items-center justify-between">
            <h3 className="font-bold text-gray-900 dark:text-gray-100">
              카테고리 상세 분석
            </h3>
            <Badge className="bg-gradient-to-r from-cyan-500 to-blue-500">
              <Sparkles className="mr-1 h-3 w-3" />
              AI 분석
            </Badge>
          </div>

          <div className="space-y-4">
            {categoryData.length === 0 ? (
              <p className="text-sm text-gray-500 dark:text-gray-400">
                분석할 소비 내역이 아직 없어요.
              </p>
            ) : (
              categoryData
                .sort((a, b) => b.amount - a.amount)
                .map((category) => (
                  <div
                    key={category.name}
                    className="rounded-xl border border-gray-100 bg-gray-50/80 p-4 dark:border-gray-700 dark:bg-gray-900/40"
                  >
                    <div className="mb-2 flex items-center justify-between">
                      <div className="flex items-center gap-2">
                        <span
                          className="h-3 w-3 rounded-full"
                          style={{ backgroundColor: category.color }}
                        />
                        <span className="font-medium text-gray-900 dark:text-gray-100">
                          {category.name}
                        </span>
                      </div>
                      <span className="font-bold text-gray-900 dark:text-gray-100">
                        {category.amount.toLocaleString()}원
                      </span>
                    </div>

                    <div className="mb-2 h-2 overflow-hidden rounded-full bg-gray-200 dark:bg-gray-700">
                      <div
                        className={`h-full ${category.meterClassName}`}
                        style={{ width: `${category.value}%` }}
                      />
                    </div>

                    <p className="text-sm text-gray-600 dark:text-gray-300">
                      전체 소비의 {category.value}%
                    </p>
                  </div>
                ))
            )}
          </div>
        </Card>
      </div>

      <Card className="border-none bg-gradient-to-r from-amber-50 to-yellow-50 p-6 dark:from-amber-50 dark:to-yellow-50">
        <div className="mb-4 flex items-center gap-2">
          <AlertTriangle className="h-5 w-5 text-amber-600" />
          <h3 className="font-bold text-gray-900">AI 소비 인사이트</h3>
        </div>

        <div className="grid gap-4 md:grid-cols-2">
          <div className="rounded-lg bg-white p-4">
            <div className="mb-2 flex items-center gap-2">
              <CheckCircle className="h-4 w-4 text-green-600" />
              <p className="font-semibold text-gray-900">잘하고 있는 점</p>
            </div>

            <p className="text-sm text-gray-700">
              {usageRate <= 100
                ? `이번 달 예산 사용률이 ${usageRate}%로 관리되고 있어요. 지금 흐름이면 예산 안에서 충분히 운영 가능해요.`
                : `예산을 ${usageRate - 100}% 초과했어요. 다음 달에는 상위 지출 카테고리를 먼저 조정해보는 게 좋아요.`}
            </p>
          </div>

          <div className="rounded-lg bg-white p-4">
            <div className="mb-2 flex items-center gap-2">
              <Sparkles className="h-4 w-4 text-cyan-600" />
              <p className="font-semibold text-gray-900">추천 액션</p>
            </div>
            <p className="text-sm text-gray-700">
              {topCategory
                ? `${topCategory.name} 비중이 가장 높아요. 이 카테고리에서 10%만 줄여도 절약 효과가 바로 보여요.`
                : "우선 한 달치 소비를 조금 더 기록하면 더 정확한 인사이트를 줄 수 있어요."}
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}