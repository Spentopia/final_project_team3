import { useEffect } from "react";
import axios from "axios";
import styles from "./AnalyticsPage.module.css";
import { useFinance, type Transaction } from "@/shared/providers/FinanceProvider";
import { listExpenses } from "@/shared/api/expenseApi";

import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/shared/ui/tabs";
import { analyzeReport } from "@/shared/api/aiApi";
import { isSolana402Body, sendSolanaX402Payment } from "@/shared/api/solanaX402";
import { useState } from "react";
import html2canvas from "html2canvas";
import jsPDF from "jspdf";
import { useRef } from "react";
import type { AnalyzeReportRequest } from "@/shared/api/aiApi";
import { useConnection, useWallet } from "@solana/wallet-adapter-react";
import { toast } from "sonner";
import { useTheme } from "next-themes";

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

type AnalysisReportType = AnalyzeReportRequest["report_type"];
type AnalysisKind = AnalyzeReportRequest["analysis_kind"];
type ReportStateByPeriod = Record<AnalysisReportType, AIReport | null>;


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

const formatDateKey = (date: Date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;

const getWeekStart = (date: Date) => {
  const weekStart = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  const mondayOffset = (weekStart.getDay() + 6) % 7;
  weekStart.setDate(weekStart.getDate() - mondayOffset);
  weekStart.setHours(0, 0, 0, 0);
  return weekStart;
};

const isDateInRange = (dateValue: string, start: Date, end: Date) => {
  const date = parseTransactionDate(dateValue);
  if (!date) return false;

  const day = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  return day.getTime() >= start.getTime() && day.getTime() <= end.getTime();
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
  const { connection } = useConnection();
  const { publicKey, signTransaction } = useWallet();
  const { resolvedTheme } = useTheme();

const reportRef = useRef<HTMLDivElement>(null);

useEffect(() => {
  const fetchExpenses = async () => {
    try {
      const data = await listExpenses();

      const formatted = data.map((item) => ({
        id: item.id,
        date: item.date,
        amount: item.amount,
        category: item.category,
        memo: item.memo ?? "",
        type: item.transactionType,
      }));

      replaceTransactions(formatted);
    } catch (error) {
      toast.error("지출 내역을 불러오지 못했습니다.", {
        description: "잠시 후 다시 시도해주세요.",
      });
    }
  };

  fetchExpenses();
}, []);

  const now = new Date();

  const thisMonthTransactions = expenseTransactionsOnly(transactions).filter((transaction) => {
  const date = parseTransactionDate(transaction.date);
  if (!date) return false;

  return (
    date.getFullYear() === now.getFullYear() &&
    date.getMonth() === now.getMonth()
  );
});

const [aiReports, setAiReports] = useState<ReportStateByPeriod>({
  weekly: null,
  monthly: null,
});

const [patternReports, setPatternReports] = useState<ReportStateByPeriod>({
  weekly: null,
  monthly: null,
});

const [isReportLoading, setIsReportLoading] = useState(false);

const [isPatternLoading, setIsPatternLoading] = useState(false);

const [isPdfMode, setIsPdfMode] = useState(false);
const [isDownloading, setIsDownloading] = useState(false);

const [selectedReportType, setSelectedReportType] = useState<AnalysisReportType>("weekly");
const isAnalysisBusy = isReportLoading || isPatternLoading;
const aiReport = aiReports[selectedReportType];
const patternReport = patternReports[selectedReportType];

// 총 지출
const totalExpense = getMonthlyExpenseTotal(transactions, now);

// ✅ 일 평균
const days = new Date().getDate();
const dailyAverage = Math.round(totalExpense / days);

// 이번 달 / 지난 달 계산
const lastMonth = new Date(now.getFullYear(), now.getMonth() - 1, 1);

const currentMonthExpense = getMonthlyExpenseTotal(transactions, now);
const lastMonthExpense = getMonthlyExpenseTotal(transactions, lastMonth);

// 총 지출 변화율
const expenseChangeRate =
  lastMonthExpense > 0
    ? Math.round(((currentMonthExpense - lastMonthExpense) / lastMonthExpense) * 100)
    : 0;

// 지난달 일 평균
const lastMonthDays = new Date(now.getFullYear(), now.getMonth(), 0).getDate();
const lastMonthDailyAverage =
  lastMonthExpense > 0 ? Math.round(lastMonthExpense / lastMonthDays) : 0;

// 일 평균 변화율
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

const isDarkMode = resolvedTheme === "dark" && !isPdfMode;
const chartTheme = isDarkMode
  ? {
      axis: "#c4b5fd",
      grid: "rgba(196, 181, 253, 0.22)",
      tooltipBg: "#111827",
      tooltipBorder: "rgba(196, 181, 253, 0.36)",
      tooltipText: "#f8fafc",
      barStart: "#a78bfa",
      barMid: "#7c3aed",
      barEnd: "#4c1d95",
      line: "#a78bfa",
      dotFill: "#7c3aed",
      dotStroke: "#ddd6fe",
      pie: ["#a78bfa", "#7c3aed", "#c084fc", "#22c55e", "#f59e0b", "#f87171"],
    }
  : {
      axis: "#64748b",
      grid: "#dbeafe",
      tooltipBg: "#ffffff",
      tooltipBorder: "#bfdbfe",
      tooltipText: "#0f172a",
      barStart: "#38bdf8",
      barMid: "#2563eb",
      barEnd: "#1e3a8a",
      line: "#2563eb",
      dotFill: "#38bdf8",
      dotStroke: "#1e3a8a",
      pie: ["#2563eb", "#38bdf8", "#22c55e", "#f59e0b", "#1e3a8a", "#ef4444"],
    };

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
const COLORS = chartTheme.pie;

const categoryData = Object.entries(categoryTotals).map(([name, amount], index) => ({
  key: name,
  name: CATEGORY_MAP[name]?.label ?? name,
  amount,
  value: totalExpense ? Math.round((amount / totalExpense) * 100) : 0,
  color: COLORS[index % COLORS.length],
}));

const weeklyData = buildWeeklyData(thisMonthTransactions, now);
const monthlyData = buildMonthlyData(transactions, now.getFullYear());

const buildAnalysisPayload = (
  reportType: AnalysisReportType,
  analysisKind: AnalysisKind
): AnalyzeReportRequest => {
  const periodStart =
    reportType === "weekly"
      ? getWeekStart(now)
      : new Date(now.getFullYear(), now.getMonth(), 1);
  const periodEnd = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  periodEnd.setHours(23, 59, 59, 999);

  const periodTransactions = expenseTransactionsOnly(transactions).filter((transaction) =>
    isDateInRange(transaction.date, periodStart, periodEnd)
  );
  const periodTotalExpense = periodTransactions.reduce((sum, transaction) => sum + transaction.amount, 0);
  const periodDays =
    Math.floor(
      (new Date(periodEnd.getFullYear(), periodEnd.getMonth(), periodEnd.getDate()).getTime() -
        periodStart.getTime()) /
        (1000 * 60 * 60 * 24)
    ) + 1;
  const periodDailyAverage = Math.round(periodTotalExpense / Math.max(periodDays, 1));

  const periodCategoryTotals = periodTransactions.reduce((acc, cur) => {
    const key = cur.category ?? "기타";
    acc[key] = (acc[key] ?? 0) + cur.amount;
    return acc;
  }, {} as Record<string, number>);

  const periodTopCategory = Object.entries(periodCategoryTotals).sort((a, b) => b[1] - a[1])[0];
  const periodTopCategoryName = CATEGORY_MAP[periodTopCategory?.[0] ?? ""]?.label ?? "없음";
  const periodTopCategoryPercent = periodTotalExpense
    ? Math.round(((periodTopCategory?.[1] ?? 0) / periodTotalExpense) * 100)
    : 0;

  const periodCategoryData = Object.entries(periodCategoryTotals).map(([name, amount], index) => ({
    key: name,
    name: CATEGORY_MAP[name]?.label ?? name,
    amount,
    value: periodTotalExpense ? Math.round((amount / periodTotalExpense) * 100) : 0,
    color: COLORS[index % COLORS.length],
  }));

  const daysInMonth = new Date(now.getFullYear(), now.getMonth() + 1, 0).getDate();
  const periodBudget =
    reportType === "weekly" && currentBudget > 0
      ? Math.round((currentBudget / daysInMonth) * periodDays)
      : currentBudget;
  const periodBudgetUsage =
    periodBudget > 0 ? Math.round((periodTotalExpense / periodBudget) * 100) : 0;

  return {
    analysis_kind: analysisKind,
    report_type: reportType,
    start_date: formatDateKey(periodStart),
    end_date: formatDateKey(periodEnd),
    transactions: periodTransactions.map((t) => ({
      date: t.date,
      amount: t.amount,
      category: t.category,
      type: t.type ?? "expense",
    })),
    total_expense: periodTotalExpense,
    budget: periodBudget,
    top_category: periodTopCategoryName,
    top_category_percent: periodTopCategoryPercent,
    daily_average: periodDailyAverage,
    expense_change_rate: reportType === "monthly" ? expenseChangeRate : 0,
    budget_usage: periodBudgetUsage,
    category_data: periodCategoryData,
  };
};

const getPaymentCacheKey = async (payload: AnalyzeReportRequest) => {
  const bytes = new TextEncoder().encode(JSON.stringify(payload));
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  const hash = Array.from(new Uint8Array(digest))
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
  return `spentopia:x402:${hash}`;
};

const getFriendlyAnalysisErrorMessage = (error: unknown) => {
  if (error instanceof Error) {
    if (error.message.includes("관리자에게 문의")) return error.message;
    if (error.message.includes("잔액")) return error.message;
    if (error.message.includes("서명")) return error.message;
    if (error.message.includes("지갑")) return error.message;
  }

  if (axios.isAxiosError(error) && error.response?.status && error.response.status >= 500) {
    return "서버 오류가 발생했습니다. 결제 내역이 있다면 관리자에게 문의해주세요.";
  }

  return "분석을 완료하지 못했습니다. 잠시 후 다시 시도해주세요.";
};

const requestPaidAnalysis = async (payload: AnalyzeReportRequest) => {
  const cacheKey = await getPaymentCacheKey(payload);
  const cachedPaymentHeader = sessionStorage.getItem(cacheKey);
  if (cachedPaymentHeader) {
    try {
      const result = await analyzeReport(payload, cachedPaymentHeader);
      sessionStorage.removeItem(cacheKey);
      return result;
    } catch (error) {
      sessionStorage.removeItem(cacheKey);
      if (!axios.isAxiosError(error) || error.response?.status !== 402) {
        throw error;
      }
    }
  }

  let paymentCompleted = false;

  try {
    return await analyzeReport(payload);
  } catch (error) {
    if (!axios.isAxiosError(error) || error.response?.status !== 402) {
      throw error;
    }
    if (!isSolana402Body(error.response.data)) {
      throw error;
    }

    const requirement = error.response.data.accepts[0];
    const amount = requirement
      ? (Number(requirement.maxAmountRequired) / 1_000_000).toFixed(2)
      : "";

    toast.info("무료 분석 횟수를 모두 사용했어요.", {
      description: `${amount} USDC 결제가 필요합니다. 지갑 창이 열리면 결제 내용을 확인하고 서명해주세요.`,
    });

    const paymentHeader = await sendSolanaX402Payment({
      body: error.response.data,
      connection,
      publicKey,
      signTransaction,
    });
    paymentCompleted = true;
    sessionStorage.setItem(cacheKey, paymentHeader);

    toast.success("결제가 확인됐어요.", {
      description: "이제 분석 결과를 불러오고 있습니다.",
    });

    try {
      const result = await analyzeReport(payload, paymentHeader);
      sessionStorage.removeItem(cacheKey);
      return result;
    } catch (retryError) {
      if (paymentCompleted) {
        throw new Error("결제는 완료됐지만 분석 결과를 불러오지 못했습니다. 관리자에게 문의해주세요.");
      }
      throw retryError;
    }
  }
};

const handleGenerateReport = async () => {
  if (thisMonthTransactions.length === 0 || isAnalysisBusy) return;

  try {
    setIsReportLoading(true);

    const payload = buildAnalysisPayload(selectedReportType, "report");

    const result = await requestPaidAnalysis(payload) as AIReport;

    setAiReports((prev) => ({
      ...prev,
      [selectedReportType]: {
        good: result.good,
        warning: result.warning,
        advice: result.advice,
        prediction: result.prediction,
        pattern: "",
        improvement: "",
      },
    }));
  } catch (error) {
    toast.error("AI 분석 리포트를 생성하지 못했습니다.", {
      description: getFriendlyAnalysisErrorMessage(error),
    });
  } finally {
    setIsReportLoading(false);
  }
};

const handleGeneratePattern = async () => {
  if (thisMonthTransactions.length === 0 || isAnalysisBusy) return;

  try {
    setIsPatternLoading(true);

    const payload = buildAnalysisPayload(selectedReportType, "pattern");

    const result = await requestPaidAnalysis(payload) as AIReport;

    setPatternReports((prev) => ({
      ...prev,
      [selectedReportType]: {
        good: "",
        warning: "",
        advice: "",
        prediction: "",
        pattern: result.pattern,
        improvement: result.improvement,
      },
    }));
  } catch (error) {
    toast.error("AI 소비 패턴 분석을 생성하지 못했습니다.", {
      description: getFriendlyAnalysisErrorMessage(error),
    });
  } finally {
    setIsPatternLoading(false);
  }
};

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
    }
  } catch (err) {
    toast.error("공유에 실패했습니다.", {
      description: "잠시 후 다시 시도해주세요.",
    });
  }
};

const handleDownload = async () => {
  if (!reportRef.current) return;

  try {
    setIsDownloading(true);

    // ✅ PDF 전용 모드 활성화
    setIsPdfMode(true);

    // 렌더링 기다리기
    await new Promise((resolve) => setTimeout(resolve, 700));

    if (!reportRef.current) return;

const element = reportRef.current;

    const canvas = await html2canvas(element, {
  scale: 2,
  useCORS: true,
  backgroundColor: "#ffffff",

  logging: false,

  scrollX: 0,
  scrollY: 0,

  windowWidth: element.scrollWidth,
  height: element.scrollHeight,
  windowHeight: element.scrollHeight,

  foreignObjectRendering: false,

  removeContainer: true,

  onclone: (clonedDoc) => {
    // 모든 요소의 oklch 제거
    const all = clonedDoc.querySelectorAll("*");

    all.forEach((el) => {
      const htmlEl = el as HTMLElement;
      const style = window.getComputedStyle(htmlEl);

      // color
      if (style.color.includes("oklch")) {
        htmlEl.style.color = "#111827";
      }

      // background
      if (style.backgroundColor.includes("oklch")) {
        htmlEl.style.backgroundColor = "#ffffff";
      }

      // border
      if (style.borderColor.includes("oklch")) {
        htmlEl.style.borderColor = "#d1d5db";
      }

      // fill (svg)
      if (style.fill.includes("oklch")) {
        htmlEl.style.fill = "#2563eb";
      }

      // stroke (svg)
      if (style.stroke.includes("oklch")) {
        htmlEl.style.stroke = "#2563eb";
      }
    });
  },
});

    const imgData = canvas.toDataURL("image/png");

    const pdf = new jsPDF("p", "mm", "a4");

    const pdfWidth = 210;
    const pdfHeight = 297;

    const margin = 10;

    const pageWidth = pdf.internal.pageSize.getWidth();
const pageHeight = pdf.internal.pageSize.getHeight();

const imgWidth = pageWidth - margin * 2;
const imgHeight = (canvas.height * imgWidth) / canvas.width

    let heightLeft = imgHeight;
    let position = margin;

    pdf.addImage(
  imgData,
  "PNG",
  margin,
  position,
  imgWidth,
  imgHeight,
  undefined,
  "FAST"
);

    heightLeft -= pdfHeight - margin * 2;

    while (heightLeft > 0) {
      position = margin - (imgHeight - heightLeft);

      pdf.addPage();

      pdf.addImage(
  imgData,
  "PNG",
  margin,
  position,
  imgWidth,
  imgHeight,
  undefined,
  "FAST"
);

      heightLeft -= pdfHeight - margin * 2;
    }

    pdf.save("소비_분석_리포트.pdf");

  } catch (err) {
    console.error("PDF 생성 오류:", err);

    toast.error("PDF 생성 실패", {
      description: "잠시 후 다시 시도해주세요.",
    });

  } finally {


    // ✅ PDF 모드 해제
    setIsPdfMode(false);

    setIsDownloading(false);


  }
};



  const timePatternData = buildTimePatternData(thisMonthTransactions);
  const timePatternTotal = timePatternData.reduce((sum, slot) => sum + slot.amount, 0);
  const weekdayPatternData = buildWeekdayPatternData(thisMonthTransactions);
  const paymentPatternData = buildPaymentPatternData(thisMonthTransactions);

  const marketCardStyle = isPdfMode
  ? {
      border: "1px solid #dbe4f0",
      background: "#f8fbff",
      boxShadow: "0 2px 10px rgba(15, 23, 42, 0.04)",
    }
  : {
      border: "1px solid rgba(125, 211, 252, 0.62)",
      backgroundImage:
        "linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 251, 255, 0.96)), radial-gradient(circle at 12% 0%, rgba(125, 211, 252, 0.18), transparent 32%), radial-gradient(circle at 88% 8%, rgba(37, 99, 235, 0.1), transparent 30%)",
      boxShadow:
        "inset 0 1px 0 rgba(255, 255, 255, 0.96), inset 0 0 0 1px rgba(37, 99, 235, 0.07), 0 0 0 1px rgba(125, 211, 252, 0.34), 0 18px 44px rgba(37, 99, 235, 0.12), 0 4px 16px rgba(15, 23, 42, 0.06)",
    };

  return (
    <>
    <div className="space-y-6">
    <div
  ref={reportRef}
  className={`
    ${isDownloading ? styles.pdfDownload : ""}
    ${isPdfMode ? styles.pdfMode : ""}
  `}
>
  <div>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1
  className={`mb-2 text-3xl font-bold ${
    isPdfMode ? "text-black" : "text-gray-900 dark:text-gray-100"
  }`}
>소비 패턴 분석</h1>
          <p className="mb-6 text-gray-600 dark:text-gray-300">
  AI가 분석한 당신의 소비 습관을 확인해보세요
</p>
        </div>
        <div className="flex gap-2">
          <Button variant="outline" onClick={handleShare} className="spentopia-primary-button">
            <Share2 className="mr-2 h-4 w-4" />
            공유
          </Button>
          <Button
  className={isPdfMode ? "bg-slate-900 text-white" : "spentopia-primary-button"}
  onClick={handleDownload}
>
            <Download className="mr-2 h-4 w-4" />
            리포트 다운로드
          </Button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid gap-6 md:grid-cols-4">
        <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
          <p className="mb-1 text-sm opacity-90">이번 달 총 지출 </p>
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

        <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
          <p className="mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">일 평균 지출</p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">
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

        <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
          <p className="mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">예산 사용률</p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{budgetUsage}%</p>
          <div className={`${styles.budgetGaugeTrack} h-3`}>
            <div
  className={styles.budgetGaugeFill}
  style={{ width: `${Math.min(budgetUsage, 100)}%` }}
></div>
          </div>
        </Card>

        <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
          <p className="mb-1 text-sm font-medium text-gray-800 dark:text-gray-200">최다 소비 카테고리</p>
          <p className="mb-2 text-3xl font-bold text-gray-900 dark:text-gray-100">{topCategoryName}</p>
          <p className="text-sm text-gray-700 dark:text-gray-300">전체의 {topCategoryPercent}%</p>
        </Card>
      </div>

      {/* Main Charts */}
      <Tabs
        value={selectedReportType}
        onValueChange={(value) => {
          if (!isAnalysisBusy) {
            setSelectedReportType(value as AnalysisReportType);
          }
        }}
        className="space-y-6"
      >
        <TabsList className="grid w-full max-w-md grid-cols-2">
          <TabsTrigger value="weekly" disabled={isAnalysisBusy}>주간</TabsTrigger>
          <TabsTrigger value="monthly" disabled={isAnalysisBusy}>월간</TabsTrigger>
        </TabsList>

        <TabsContent value="weekly" className="space-y-6">
          <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
            <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">주간 소비 추이</h3>
            <ResponsiveContainer width="99%" height={300}>
              <BarChart
  data={weeklyData}
  margin={{ top: 10, right: 20, left: 30, bottom: 10 }}
>
                <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.grid} />
                <XAxis dataKey="day" stroke={chartTheme.axis} />
                <YAxis stroke={chartTheme.axis} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: chartTheme.tooltipBg,
                    border: `1px solid ${chartTheme.tooltipBorder}`,
                    borderRadius: "8px",
                    color: chartTheme.tooltipText,
                  }}
                />
                <Bar
  dataKey="amount"
  isAnimationActive={!isPdfMode} fill={isPdfMode ? "#2563eb" : "url(#colorGradient)"} radius={[8, 8, 0, 0]} />
                <defs>
                  <linearGradient id="colorGradient" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="0%" stopColor={chartTheme.barStart} />
                    <stop offset="55%" stopColor={chartTheme.barMid} />
                    <stop offset="100%" stopColor={chartTheme.barEnd} />
                  </linearGradient>
                </defs>
              </BarChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>

        <TabsContent value="monthly" className="space-y-6">
          <Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
            <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">월간 소비 추이</h3>
            <ResponsiveContainer width="99%" height={300}>
              <LineChart
  data={monthlyData}
  margin={{ top: 10, right: 20, left: 30, bottom: 10 }}
>
                <CartesianGrid strokeDasharray="3 3" stroke={chartTheme.grid} />
                <XAxis dataKey="month" stroke={chartTheme.axis} />
                <YAxis stroke={chartTheme.axis} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: chartTheme.tooltipBg,
                    border: `1px solid ${chartTheme.tooltipBorder}`,
                    borderRadius: "8px",
                    color: chartTheme.tooltipText,
                  }}
                />
                <Line
  type="monotone"
  dataKey="amount"
  stroke={isPdfMode ? "#2563eb" : chartTheme.line}
  strokeWidth={3}
  dot={{ fill: isPdfMode ? "#2563eb" : chartTheme.dotFill, stroke: chartTheme.dotStroke, strokeWidth: 2, r: 6 }}
/>
              </LineChart>
            </ResponsiveContainer>
          </Card>
        </TabsContent>
      </Tabs>

      {/* Category Analysis */}
      <div className="grid gap-6 lg:grid-cols-2 items-stretch">
        <Card style={marketCardStyle} className={`${styles.marketCard} h-full min-h-[400px] flex flex-col border-none p-6 backdrop-blur-xl`}>
          <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">카테고리별 지출</h3>
          <div className="flex-1">
          <ResponsiveContainer width="100%" height={320}>
            <PieChart margin={{ top: 20, right: 20, left: 20, bottom: 20 }}>
              <Pie
                data={categoryData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent, cx, cy, midAngle, innerRadius, outerRadius }) => {
  const RADIAN = Math.PI / 180;

  const radius =
    Number(innerRadius) +
    (Number(outerRadius) - Number(innerRadius)) * 1.25;

  const x =
    Number(cx) + radius * Math.cos(-Number(midAngle) * RADIAN);

  const y =
    Number(cy) + radius * Math.sin(-Number(midAngle) * RADIAN);

  return (
    <text
      x={x}
      y={y}
      fill="#111827"
      textAnchor={x > Number(cx) ? "start" : "end"}
      dominantBaseline="central"
      fontSize={12}
fontWeight={600}
    >
      {`${name} ${(Number(percent) * 100).toFixed(0)}%`}
    </text>
  );
}}
                outerRadius={110}
                fill="#2563eb"
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
  contentStyle={{
    backgroundColor: chartTheme.tooltipBg,
    border: `1px solid ${chartTheme.tooltipBorder}`,
    borderRadius: "8px",
    color: chartTheme.tooltipText,
  }}
/>
            </PieChart>
          </ResponsiveContainer>
          </div>
        </Card>

        <Card style={marketCardStyle} className={`${styles.marketCard} h-full min-h-[400px] border-none p-6 backdrop-blur-xl`}>
          <h3 className="mb-6 font-bold text-gray-900 dark:text-gray-100">카테고리 상세</h3>
          <div className="space-y-4">
            {categoryData.map((cat) => (
              <div key={cat.name} className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="font-medium text-gray-900 dark:text-gray-100">
  {CATEGORY_MAP[cat.key]?.icon} {cat.name}
</span>
                  <span className="font-bold text-gray-900 dark:text-gray-100">{cat.amount.toLocaleString()}원</span>
                </div>
                <div className="flex items-center gap-3">
  <div className={styles.categoryProgressTrack}>
    <div
      className={styles.categoryProgressFill}
      style={{
        width: `${cat.value}%`,
        backgroundColor: cat.color,
      }}
    ></div>
  </div>

  <span className="min-w-[45px] text-sm font-semibold text-gray-700 dark:text-gray-200">
    {cat.value}%
  </span>
</div>
</div>
            ))}
            </div>
        </Card>
      </div>

{/* AI Insights */}
<Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>

  <div className="mb-4 flex items-center justify-between">
    <div className="flex items-center gap-2">
      <Sparkles className="h-5 w-5 text-slate-700 dark:text-violet-300" />

      <h3 className="font-bold text-gray-900 dark:text-white">
        AI 소비 분석 리포트
      </h3>
    </div>

    <Button
      onClick={handleGenerateReport}
      disabled={isAnalysisBusy}
      className="spentopia-primary-button"
    >
      {isReportLoading ? "AI 분석 중..." : "AI 분석 시작"}
    </Button>
  </div>

  {aiReport ? (
  <div className="grid gap-4 md:grid-cols-2">

    <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-4`}>
      <h4 className="font-bold text-gray-900 dark:text-white">👍 좋은 점</h4>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        {aiReport.good}
      </p>
    </div>

    <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-4`}>
      <h4 className="font-bold text-gray-900 dark:text-white">⚠️ 주의</h4>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        {aiReport.warning}
      </p>
    </div>

    <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-4`}>
      <h4 className="font-bold text-gray-900 dark:text-white">💡 조언</h4>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        {aiReport.advice}
      </p>
    </div>

    <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-4`}>
      <h4 className="font-bold text-gray-900 dark:text-white">📈 예측</h4>
      <p className="text-sm text-gray-700 dark:text-gray-300">
        {aiReport.prediction}
      </p>
    </div>

  </div>
) : (
  <div className="rounded-xl border-2 border-dashed border-gray-300 p-10 text-center">
    <p className="text-gray-500 dark:text-gray-400">
      아직 AI 소비 분석 리포트가 생성되지 않았습니다
    </p>
  </div>
)}

</Card>

      {/* Spending Patterns */}
<Card style={marketCardStyle} className={`${styles.marketCard} border-none p-6 backdrop-blur-xl`}>
  <div className="mb-6 flex items-center justify-between">
  <h3 className="font-bold text-gray-900 dark:text-white">
    AI 소비 패턴 분석
  </h3>

  <Button
    onClick={handleGeneratePattern}
    disabled={isAnalysisBusy}
    className="spentopia-primary-button"
  >
    {isPatternLoading ? "AI 분석 중..." : "AI 분석 시작"}
  </Button>
</div>

  {patternReport ? (
    <div className="grid gap-6 md:grid-cols-2">

      <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-5`}>
        <h4 className="mb-2 font-bold text-gray-900 dark:text-white">📊 분석</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
  {(patternReport.pattern ?? "").replace("소비 패턴 분석:", "")}
</p>
      </div>

      <div style={marketCardStyle} className={`${styles.marketCard} rounded-lg p-5`}>
        <h4 className="mb-2 font-bold text-gray-900 dark:text-white">💡 개선 방안</h4>
        <p className="text-sm text-gray-700 dark:text-gray-300 leading-relaxed">
          {patternReport.improvement}
        </p>
      </div>

    </div>
  ) : (
    <div className="rounded-xl border-2 border-dashed border-gray-300 p-10 text-center">
  <p className="text-gray-500 dark:text-gray-400">
    아직 AI 소비 패턴 분석이 생성되지 않았습니다
  </p>
</div>
  )}
</Card>
          </div>
</div>
</div>
</>
);
}
