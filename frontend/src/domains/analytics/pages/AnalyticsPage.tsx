<<<<<<< HEAD
import { useState, useEffect } from "react";
=======
import { useEffect } from "react";
>>>>>>> develop
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

<<<<<<< HEAD
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

export default function Analytics() {

  const { transactions, budgets } = useFinance(); // ✅ 여기 (제일 위)
  const [expenses, setExpenses] = useState<any[]>([]);

  useEffect(() => {
    const stored = localStorage.getItem("expenses");
    if (stored) {
      const parsed = JSON.parse(stored).map((e: any) => ({
        ...e,
        date: new Date(e.date),
      }));
      setExpenses(parsed);
    }
  }, []);

  const [selectedMonth, setSelectedMonth] = useState(new Date());

  const now = new Date();

const currentMonthKey = `${selectedMonth.getFullYear()}-${String(
  selectedMonth.getMonth() + 1
).padStart(2, "0")}`;

const currentBudget = budgets[currentMonthKey] || 0;

  // 👉 여기부터 계산 코드들
  const thisMonthTransactions = expenses.filter((t: any) => {
  const date = new Date(t.date);

  return (
    date.getFullYear() === selectedMonth.getFullYear() &&
    date.getMonth() === selectedMonth.getMonth()
  );
});
=======
const monthlyData = [
  { month: "1월", amount: 320000 },
  { month: "2월", amount: 280000 },
  { month: "3월", amount: 350000 },
  { month: "4월", amount: 310000 },
  { month: "5월", amount: 290000 },
  { month: "6월", amount: 265000 },
];

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

  const thisMonthTransactions = transactions.filter((t: any) => {
    const date = new Date(t.date);
    return (
      (t.type ?? "expense") === "expense" &&
      date.getFullYear() === now.getFullYear() &&
      date.getMonth() === now.getMonth()
    );
  });
>>>>>>> develop

  const totalExpense = thisMonthTransactions.reduce(
    (sum: number, t: any) => sum + t.amount,
    0
  );

  const daysInMonth = new Date(
  selectedMonth.getFullYear(),
  selectedMonth.getMonth() + 1,
  0
).getDate();

const averageDailyExpense =
  thisMonthTransactions.length > 0
    ? Math.round(totalExpense / daysInMonth)
    : 0;
    const weekDays = ["일", "월", "화", "수", "목", "금", "토"];

const weeklyData = weekDays.map((day, index) => {
  const total = thisMonthTransactions
    .filter((t: any) => new Date(t.date).getDay() === index)
    .reduce((sum: number, t: any) => sum + t.amount, 0);

  return {
    day,
    amount: total,
  };
});
// 🔥 월별 데이터 (여기에 추가)
const monthlyData = Array.from({ length: 12 }, (_, i) => {
  const total = expenses
    .filter((t: any) => {
      const date = new Date(t.date);
      return date.getMonth() === i;
    })
    .reduce((sum: number, t: any) => sum + t.amount, 0);

  return {
    month: `${i + 1}월`,
    amount: total,
  };
});
    // 🔥 예산 사용률
const usageRate =
  currentBudget > 0 ? Math.round((totalExpense / currentBudget) * 100) : 0;

// 🔥 카테고리 계산
const categoryMap: Record<string, number> = {};

thisMonthTransactions.forEach((t: any) => {
  const key = t.category || "기타";
  categoryMap[key] = (categoryMap[key] || 0) + t.amount;
});

const total = Object.values(categoryMap).reduce((a, b) => a + b, 0);

const categoryData = Object.entries(categoryMap).map(([key, value]) => ({
  name: categoryLabelMap[key] || "기타", // ✅ 한글 변환
  value: Math.round((value / total) * 100),
  amount: value,
  color: categoryColorMap[key] || "#6b7280", // ✅ 색상
}));

// 🔥 최다 카테고리
const topCategory = Object.entries(categoryMap).sort(
  (a, b) => b[1] - a[1]
)[0];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex gap-2 mb-4 items-center">
  <Button
    onClick={() =>
      setSelectedMonth(
        new Date(
          selectedMonth.getFullYear(),
          selectedMonth.getMonth() - 1
        )
      )
    }
  >
    ◀ 이전달
  </Button>

  <span className="font-bold text-lg">
    {selectedMonth.getFullYear()}년 {selectedMonth.getMonth() + 1}월
  </span>

  <Button
    onClick={() =>
      setSelectedMonth(
        new Date(
          selectedMonth.getFullYear(),
          selectedMonth.getMonth() + 1
        )
      )
    }
  >
    다음달 ▶
  </Button>
</div>
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

  <p className="mb-2 text-3xl font-bold text-gray-900">
    {averageDailyExpense.toLocaleString()}원
  </p>

  <div className="flex items-center gap-1 text-sm text-green-600">
    <TrendingDown className="h-4 w-4" />
    <span>평균 소비</span>
  </div>
</Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">예산 사용률</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">
  {usageRate}%
</p>
          <div className="h-2 overflow-hidden rounded-full bg-gray-200">
    <div
      className="h-full bg-gradient-to-r from-cyan-500 to-blue-500"
      style={{ width: `${usageRate}%` }}  // ✅ 여기 있어야 정상
    ></div>
  </div>
        </Card>

        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl">
          <p className="mb-1 text-sm text-gray-600">최다 소비 카테고리</p>
          <p className="mb-2 text-3xl font-bold text-gray-900">
  {topCategory ? categoryLabelMap[topCategory[0]] : "없음"}
</p>

<p className="text-sm text-gray-600">
  {topCategory
    ? `${Math.round((topCategory[1] / totalExpense) * 100)}%`
    : ""}
</p>
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
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">오전 (06-12시)</span>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
                    <div className="h-full w-[30%] bg-cyan-500"></div>
                  </div>
                  <span className="text-sm font-bold text-gray-900">30%</span>
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">오후 (12-18시)</span>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
                    <div className="h-full w-[50%] bg-cyan-500"></div>
                  </div>
                  <span className="text-sm font-bold text-gray-900">50%</span>
                </div>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">저녁 (18-24시)</span>
                <div className="flex items-center gap-2">
                  <div className="h-2 w-24 overflow-hidden rounded-full bg-gray-200">
                    <div className="h-full w-[20%] bg-cyan-500"></div>
                  </div>
                  <span className="text-sm font-bold text-gray-900">20%</span>
                </div>
              </div>
            </div>
          </div>

          <div>
            <h4 className="mb-3 font-bold text-gray-900">요일별 소비</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">평일</span>
                <Badge>65,000원</Badge>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">주말</span>
                <Badge variant="secondary">100,000원</Badge>
              </div>
              <p className="text-sm text-gray-600">주말 소비가 54% 더 많아요</p>
            </div>
          </div>

          <div>
            <h4 className="mb-3 font-bold text-gray-900">결제 방법</h4>
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">카드</span>
                <span className="text-sm font-bold text-gray-900">75%</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">현금</span>
                <span className="text-sm font-bold text-gray-900">20%</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-sm text-gray-600">기타</span>
                <span className="text-sm font-bold text-gray-900">5%</span>
              </div>
            </div>
          </div>
        </div>
      </Card>
    </div>
  );
}
