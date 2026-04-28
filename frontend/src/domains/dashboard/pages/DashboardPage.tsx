import { useEffect, useState } from "react";
import { useFinance } from "@/shared/providers/FinanceProvider";
import { deleteExpense } from "@/shared/api/expenseApi";


import { Calendar } from "@/shared/ui/calendar";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { verifyReceiptOcr, ReceiptOcrResponse } from "@/shared/api/receiptOcr";
import {
  createExpense,
  listExpenses,
  type CreateExpenseResponse,
} from "@/shared/api/expenseApi";
import { Badge } from "@/shared/ui/badge";

import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/shared/ui/select";
import {
  Plus,
  Upload,
  CheckCircle,
  TrendingDown,
  TrendingUp,
  Zap,
  Trash2, // 👈 추가
} from "lucide-react";
import { format, isValid, parse } from "date-fns";
import { ko } from "date-fns/locale";
import { toast } from "sonner";
import {
  getMonthlyExpenseTotal,
  getMonthlyIncomeTotal,
} from "@/shared/utils/finance";

interface Expense {
  id: string | number;
  date: Date;
  amount: number;
  category: string;
  memo: string;
  type: "expense" | "income";
  receipt?: boolean;
  diary?: string;
}

const toDashboardExpense = (savedExpense: CreateExpenseResponse): Expense => ({
  id: savedExpense.id,
  date: parse(savedExpense.date, "yyyy-MM-dd", new Date()),
  amount: savedExpense.amount,
  category: savedExpense.category,
  memo: savedExpense.memo ?? "",
  type: savedExpense.transactionType,
  receipt: savedExpense.transactionType === "expense" ? savedExpense.receiptVerified : undefined,
  diary: savedExpense.transactionType === "expense" ? (savedExpense.diary ?? "") : undefined,
});

const categories = [
  { value: "food", label: "🍔 식비", color: "bg-orange-500" },
  { value: "transport", label: "🚗 교통", color: "bg-blue-500" },
  { value: "shopping", label: "🛍️ 쇼핑", color: "bg-pink-500" },
  { value: "entertainment", label: "🎮 여가", color: "bg-cyan-500" },
  { value: "health", label: "💊 의료", color: "bg-green-500" },
  { value: "education", label: "📚 교육", color: "bg-indigo-500" },
  { value: "utility", label: "💡 공과금", color: "bg-yellow-500" },
  { value: "other", label: "📦 기타", color: "bg-gray-500" },
];

const incomeCategories = [
  { value: "salary", label: "💼 월급", color: "bg-emerald-500" },
  { value: "allowance", label: "💵 용돈", color: "bg-green-500" },
  { value: "bonus", label: "🎁 보너스", color: "bg-lime-500" },
  { value: "side_job", label: "🧩 부수입", color: "bg-teal-500" },
  { value: "investment", label: "📈 투자", color: "bg-cyan-500" },
  { value: "other", label: "📦 기타", color: "bg-gray-500" },
];

export default function DashboardPage() {
  const { budget, budgets, transactions, replaceTransactions, addTransaction } = useFinance();
  const draftStorageKey = "dashboard-expense-draft";
  const selectedDateStorageKey = "dashboard-selected-date";
  const entryTypeStorageKey = "dashboard-entry-type";

  const [selectedDate, setSelectedDate] = useState<Date | undefined>(() => {
    const saved = localStorage.getItem(selectedDateStorageKey);
    if (!saved) return new Date();

    const parsed = parse(saved, "yyyy-MM-dd", new Date());
    return isValid(parsed) ? parsed : new Date();
  });
  const [entryType, setEntryType] = useState<"expense" | "income">(() => {
    const saved = localStorage.getItem(entryTypeStorageKey);
    return saved === "income" ? "income" : "expense";
  });
  const [newExpense, setNewExpense] = useState(() => {
    const saved = localStorage.getItem(draftStorageKey);
    if (!saved) {
      return {
        amount: "",
        category: "",
        memo: "",
        diary: "",
      };
    }

    try {
      const parsed = JSON.parse(saved);
      return {
        amount: typeof parsed.amount === "string" ? parsed.amount : "",
        category: typeof parsed.category === "string" ? parsed.category : "",
        memo: typeof parsed.memo === "string" ? parsed.memo : "",
        diary: typeof parsed.diary === "string" ? parsed.diary : "",
      };
    } catch {
      return {
        amount: "",
        category: "",
        memo: "",
        diary: "",
      };
    }
  });

  const resetForm = () => {
    setNewExpense({
      amount: "",
      category: "",
      memo: "",
      diary: "",
    });
    setReceiptFile(null);
    setOcrResult(null);
    setOcrError("");
    setIsReceiptVerified(false);
  };

  useEffect(() => {
    localStorage.setItem(draftStorageKey, JSON.stringify(newExpense));
  }, [newExpense]);

  useEffect(() => {
    if (!selectedDate) {
      localStorage.removeItem(selectedDateStorageKey);
      return;
    }

    localStorage.setItem(selectedDateStorageKey, format(selectedDate, "yyyy-MM-dd"));
  }, [selectedDate]);

  useEffect(() => {
    localStorage.setItem(entryTypeStorageKey, entryType);
  }, [entryType]);

  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [ocrLoading, setOcrLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [listLoading, setListLoading] = useState(false);
  const [ocrResult, setOcrResult] = useState<ReceiptOcrResponse | null>(null);
  const [isReceiptVerified, setIsReceiptVerified] = useState(false);
  const [ocrError, setOcrError] = useState("");

  useEffect(() => {
    let cancelled = false;

    const loadExpenses = async () => {
      try {
        setListLoading(true);
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
            receipt: item.receiptVerified,
            diary: item.diary ?? "",
          }))
        );
      } catch (error) {
        if (!cancelled) {
          const message =
            error instanceof Error ? error.message : "소비 내역을 불러오지 못했습니다.";
          toast.error(message);
        }
      } finally {
        if (!cancelled) {
          setListLoading(false);
        }
      }
    };

    void loadExpenses();

    return () => {
      cancelled = true;
    };
  }, []);

  const handleVerifyReceipt = async () => {
    if (!receiptFile) {
      toast.error("영수증 이미지를 먼저 업로드해주세요");
      return;
    }

    if (!selectedDate) {
      toast.error("날짜를 먼저 선택해주세요");
      return;
    }

    if (!newExpense.amount || Number.isNaN(Number(newExpense.amount))) {
      toast.error("금액을 먼저 입력해주세요");
      return;
    }

    try {
      setOcrLoading(true);
      setOcrError("");
      setOcrResult(null);
      setIsReceiptVerified(false);

      const expectedDate = format(selectedDate, "yyyy-MM-dd");
      const expectedAmount = Number(newExpense.amount);

      const result = await verifyReceiptOcr({
        image: receiptFile,
        expectedDate,
        expectedAmount,
      });

      setOcrResult(result);
      setIsReceiptVerified(result.verification.is_verified);

      if (result.verification.is_verified) {
        toast.success("영수증 인증 성공");
      } else {
        toast.error(result.verification.reason);
      }
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "영수증 검증 중 오류가 발생했습니다.";
      setOcrError(message);
      setIsReceiptVerified(false);
      toast.error(message);
    } finally {
      setOcrLoading(false);
    }
  };

  const handleAddExpense = async () => {
    if (!newExpense.amount || !newExpense.category) {
      toast.error("금액과 카테고리를 입력해주세요");
      return;
    }

    if (!selectedDate) {
      toast.error("날짜를 먼저 선택해주세요");
      return;
    }

    if (receiptFile && !isReceiptVerified) {
      toast.error("영수증을 업로드했다면 검증을 먼저 완료해주세요");
      return;
    }

    try {
      setSaveLoading(true);

      const payload = {
        date: format(selectedDate, "yyyy-MM-dd"),
        amount: Number(newExpense.amount),
        category: newExpense.category,
        memo: newExpense.memo,
        transactionType: entryType,
        diary: entryType === "expense" ? newExpense.diary : "",
      };

      const savedExpense = await createExpense(payload);

      // 영수증 인증 서버 반영 (receipt_verified 서버 제어)
      // 프리뷰 인증 성공 → expense_id 포함해서 OCR 재호출 → 서버가 DB 업데이트
      // 재호출 실패 시 저장된 소비를 롤백(DELETE)해서 불일치 방지
      let serverReceiptVerified = false;
      if (entryType === "expense" && receiptFile && isReceiptVerified) {
        try {
          const ocrResult = await verifyReceiptOcr({
            image: receiptFile,
            expectedDate: format(selectedDate, "yyyy-MM-dd"),
            expectedAmount: Number(newExpense.amount),
            expenseId: String(savedExpense.id),
          });
          serverReceiptVerified = ocrResult.verification.is_verified;
          if (serverReceiptVerified) {
            window.dispatchEvent(new CustomEvent("spentopia:score-refresh"));
          }
        } catch (ocrError) {
          // OCR 재호출 실패 → 소비는 유지, 영수증 인증만 미반영
          const message =
            ocrError instanceof Error ? ocrError.message : "영수증 인증 중 오류가 발생했습니다.";
          toast.error(`영수증 인증 실패: ${message}`);
          serverReceiptVerified = false;
        }
      }

      const expense = toDashboardExpense(savedExpense);

      addTransaction({
  id: String(expense.id),
  date: format(expense.date, "yyyy-MM-dd"),
  amount: expense.amount,
  category: expense.category,
  memo: expense.memo,
  type: entryType,
  receipt: entryType === "expense" ? serverReceiptVerified : undefined,
  diary: entryType === "expense" ? expense.diary : undefined,
});
      setSelectedDate(expense.date);

      if (savedExpense.transactionType !== entryType) {
        toast.error(
          `저장 응답 타입이 예상과 다릅니다. 서버 응답: ${savedExpense.transactionType}, 입력 타입: ${entryType}`
        );
      }

      if (newExpense.diary.trim()) {
        // diary 점수 안내 (필요 시 확장)
      }
      toast.success(entryType === "income" ? "수입 기록 완료!" : "소비 기록 완료! 🎉");

      resetForm();
    } catch (error) {
      const message =
        error instanceof Error
          ? error.message
          : entryType === "income"
            ? "수입 저장 중 오류가 발생했습니다."
            : "소비 저장 중 오류가 발생했습니다.";
      toast.error(message);
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDeleteExpense = async (id: string) => {
  try {
    await deleteExpense(id);

    // 상태에서 제거
    replaceTransactions(
      transactions.filter((t) => String(t.id) !== id)
    );

    toast.success("삭제 완료");
  } catch (error) {
    const message =
      error instanceof Error ? error.message : "삭제 실패";
    toast.error(message);
  }
};

  const handleExpenseDateChange = (value: string) => {
    if (!value) {
      setSelectedDate(undefined);
      return;
    }

    const nextDate = parse(value, "yyyy-MM-dd", new Date());

    if (isValid(nextDate)) {
      setSelectedDate(nextDate);
    }
  };

  const expenses = transactions.map((transaction) => ({
    id: transaction.id,
    date: parse(transaction.date, "yyyy-MM-dd", new Date()),
    amount: transaction.amount,
    category: transaction.category,
    memo: transaction.memo ?? "",
    type: transaction.type,
    receipt: transaction.type === "expense" ? transaction.receipt : undefined,
    diary: transaction.type === "expense" ? transaction.diary : undefined,
  }));

  const selectedDateExpenses = expenses.filter(
    (e) => format(e.date, "yyyy-MM-dd") === format(selectedDate || new Date(), "yyyy-MM-dd")
  );

  const dailyExpenseTotal = selectedDateExpenses
    .filter((e) => e.type === "expense")
    .reduce((sum, e) => sum + e.amount, 0);
  const dailyIncomeTotal = selectedDateExpenses
    .filter((e) => e.type === "income")
    .reduce((sum, e) => sum + e.amount, 0);

  const monthlyTotal = getMonthlyExpenseTotal(
  transactions,
  selectedDate || new Date()
);
    const monthKey = format(selectedDate || new Date(), "yyyy-MM");
const currentBudget = budgets[monthKey] ?? budget;

  const monthlyIncomeTotal = getMonthlyIncomeTotal(
  transactions,
  selectedDate || new Date()
);

  const recordedDates = expenses.map((expense) => expense.date);
  const selectedDateInputValue = selectedDate ? format(selectedDate, "yyyy-MM-dd") : "";
  const activeCategories = entryType === "income" ? incomeCategories : categories;

  const getCategoryInfo = (categoryValue: string, type: "expense" | "income") => {
    const source = type === "income" ? incomeCategories : categories;
    return source.find((c) => c.value === categoryValue) || source[source.length - 1];
  };

  return (
    <div className="grid gap-6 lg:grid-cols-[1fr_400px]">
      {/* Left Column - Calendar & Expenses */}
      <div className="space-y-6">
        {/* Monthly Summary */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-2xl font-bold text-gray-900 dark:text-gray-100">
                {format(selectedDate || new Date(), "yyyy년 M월", { locale: ko })}
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-300">이번 달 소비 내역</p>
            </div>
            <div className="text-right text-gray-900 dark:text-gray-100">
              <p className="text-3xl font-bold">
                {monthlyTotal.toLocaleString()}원
              </p>
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <div className="rounded-lg bg-cyan-100 p-4 text-gray-900 dark:bg-cyan-100 dark:text-gray-900">
              <p className="text-sm font-medium">예산</p>
              <p className="font-bold">
                {currentBudget.toLocaleString()}원
              </p>
            </div>

            <div className="rounded-lg bg-blue-100 p-4 text-gray-900 dark:bg-blue-100 dark:text-gray-900">
              <p className="text-sm font-medium">남은 예산</p>
              <p className="font-bold">
                {(currentBudget - monthlyTotal).toLocaleString()}원
              </p>
            </div>

            <div className="rounded-lg bg-teal-100 p-4 text-gray-900 dark:bg-teal-100 dark:text-gray-900">
              <p className="text-sm font-medium">사용률</p>
              <p className="font-bold">
                {currentBudget > 0
  ? Math.round((monthlyTotal / currentBudget) * 100)
  : 0}
                %
              </p>
            </div>

            <div className="rounded-lg bg-emerald-100 p-4 text-gray-900 dark:bg-emerald-100 dark:text-gray-900">
              <p className="text-sm font-medium">수입</p>
              <p className="font-bold">
                {monthlyIncomeTotal.toLocaleString()}원
              </p>
            </div>
          </div>
        </Card>

        {/* Calendar */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <Calendar
            mode="single"
            selected={selectedDate}
            onSelect={setSelectedDate}
            className="w-full rounded-lg p-0"
            classNames={{
              months: "w-full",
              month: "flex w-full flex-col gap-4",
              caption: "relative flex w-full items-center justify-center pt-1",
              caption_label: "text-xl font-bold text-gray-900 dark:text-gray-100 sm:text-2xl",
              table: "w-full border-collapse",
              head_row: "grid grid-cols-7",
              head_cell:
                "flex h-10 items-center justify-center rounded-md text-sm font-medium text-muted-foreground",
              row: "mt-2 grid grid-cols-7 gap-2",
              cell: "relative aspect-square p-0 text-center text-sm focus-within:relative focus-within:z-20 [&:has([aria-selected])]:rounded-md [&:has([aria-selected])]:bg-accent",
              day: "flex h-full w-full items-center justify-center rounded-md p-0 text-base font-medium transition-colors hover:bg-accent hover:text-accent-foreground focus:bg-accent focus:text-accent-foreground aria-selected:opacity-100 sm:text-lg",
            }}
            locale={ko}
            modifiers={{ recorded: recordedDates }}
            modifiersClassNames={{
              recorded:
                "relative font-bold text-cyan-700 after:absolute after:bottom-1 after:left-1/2 after:h-1 after:w-1 after:-translate-x-1/2 after:rounded-full after:bg-cyan-500 dark:text-cyan-300 dark:after:bg-cyan-300",
            }}
          />
        </Card>

        {/* Daily Expenses List */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="font-bold text-gray-900 dark:text-gray-100">
                {format(selectedDate || new Date(), "M월 d일", { locale: ko })} 소비 내역
              </h3>
              <p className="text-sm text-gray-600 dark:text-gray-400">
                소비 {dailyExpenseTotal.toLocaleString()}원 · 수입 {dailyIncomeTotal.toLocaleString()}원 · {selectedDateExpenses.length}건
              </p>
            </div>
            {selectedDateExpenses.some((e) => e.diary) && (
              <Badge className="bg-gradient-to-r from-cyan-500 to-blue-500">
                <Zap className="mr-1 h-3 w-3" />
                일기 작성 완료
              </Badge>
            )}
          </div>

          <div className="space-y-3">
            {selectedDateExpenses.length === 0 ? (
              <div className="py-12 text-center">
                <p className="text-gray-500">
                  {listLoading ? "소비 내역을 불러오는 중이에요" : "아직 기록된 내역이 없어요"}
                </p>
                <p className="mt-1 text-sm text-gray-400">오른쪽에서 소비나 수입을 기록해보세요!</p>
              </div>
            ) : (
              selectedDateExpenses.map((expense) => {
                const categoryInfo = getCategoryInfo(expense.category, expense.type);
                const isIncome = expense.type === "income";

                return (
                  <div
                    key={expense.id}
                    className="flex items-center justify-between rounded-lg border bg-white p-4 transition-all hover:shadow-md dark:border-gray-700 dark:bg-gray-900/50"
                  >
                    <div className="flex items-center gap-3">
                      <div className={`rounded-lg ${categoryInfo.color} p-3 text-white`}>
                        {categoryInfo.label.split(" ")[0]}
                      </div>
                      <div>
                        <p className="font-bold text-gray-900 dark:text-gray-100">
                          {expense.memo || "구매품목 없음"}
                        </p>
                        <div className="mt-1 flex items-center gap-2">
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {categoryInfo.label}
                          </p>
                          {isIncome && (
                            <Badge
                              variant="outline"
                              className="h-5 border-emerald-500 text-emerald-700 dark:text-emerald-400"
                            >
                              수입
                            </Badge>
                          )}
                          {expense.receipt && (
                            <Badge
                              variant="outline"
                              className="h-5 border-green-500 text-green-700 dark:text-green-400"
                            >
                              <CheckCircle className="mr-1 h-3 w-3" />
                              영수증
                            </Badge>
                          )}
                        </div>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
  <p
    className={`font-bold ${
      isIncome
        ? "text-emerald-600 dark:text-emerald-400"
        : "text-gray-900 dark:text-gray-100"
    }`}
  >
    {isIncome ? "+" : "-"}
    {expense.amount.toLocaleString()}원
  </p>

  <Button
  variant="ghost"
  size="icon"
  onClick={() => handleDeleteExpense(String(expense.id))}
  className="text-red-500 hover:bg-red-100 dark:hover:bg-red-900/30"
>
  <Trash2 className="h-4 w-4" />
</Button>
</div>
                  </div>
                );
              })
            )}
          </div>

          {/* Daily Diary */}
          {selectedDateExpenses.length > 0 && (
            <div className="mt-6 rounded-lg border-2 border-dashed border-cyan-300 bg-cyan-50/50 p-4 dark:border-cyan-600 dark:bg-cyan-900/20">
              <p className="mb-2 font-bold text-cyan-900 dark:text-cyan-100">오늘의 소비 일기</p>
              {selectedDateExpenses.find((e) => e.diary) ? (
                <p className="text-sm text-gray-700 dark:text-gray-300">
                  {selectedDateExpenses.find((e) => e.diary)?.diary}
                </p>
              ) : (
                <p className="text-sm text-gray-500 dark:text-gray-400">
                  일기를 작성하면 보상이 추가돼요!
                </p>
              )}
            </div>
          )}
        </Card>
      </div>

      {/* Right Column - Add Expense Form */}
      <div className="space-y-6">
        {/* Add Expense Form */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <div className="mb-4 flex items-center justify-between gap-3">
            <div className="flex items-center gap-2">
              {entryType === "income" ? (
                <TrendingUp className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
              ) : (
                <TrendingDown className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
              )}
              <h3 className="font-bold text-gray-900 dark:text-gray-100">
                {entryType === "income" ? "수입 입력하기" : "소비 입력하기"}
              </h3>
            </div>

            <div className="flex rounded-lg border border-gray-200 bg-gray-100 p-1 shadow-sm dark:border-gray-600 dark:bg-gray-700/60">
              <Button
                type="button"
                size="sm"
                variant={entryType === "expense" ? "default" : "ghost"}
                className={
                  entryType === "expense"
                    ? "bg-gradient-to-r from-cyan-500 to-blue-500 text-white hover:from-cyan-600 hover:to-blue-600"
                    : "text-gray-600 hover:text-gray-900 dark:text-gray-200 dark:hover:text-white"
                }
                onClick={() => {
                  setEntryType("expense");
                  resetForm();
                }}
              >
                소비 입력
              </Button>
              <Button
                type="button"
                size="sm"
                variant={entryType === "income" ? "default" : "ghost"}
                className={
                  entryType === "income"
                    ? "bg-gradient-to-r from-emerald-500 to-green-500 text-white hover:from-emerald-600 hover:to-green-600"
                    : "text-gray-600 hover:text-gray-900 dark:text-gray-200 dark:hover:text-white"
                }
                onClick={() => {
                  setEntryType("income");
                  resetForm();
                }}
              >
                수입 입력
              </Button>
            </div>
          </div>

          <div className="space-y-4">
            <div>
              <Label className="text-gray-700 dark:text-gray-200">날짜</Label>
              <Input
                type="date"
                value={selectedDateInputValue}
                onChange={(e) => handleExpenseDateChange(e.target.value)}
                className="mt-1 dark:text-gray-100 dark:[color-scheme:dark]"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-300">
                날짜를 직접 입력하거나 왼쪽 달력에서 선택할 수 있어요.
              </p>
            </div>

            <div>
              <Label htmlFor="amount" className="text-gray-700 dark:text-gray-200">금액 *</Label>
              <Input
                id="amount"
                type="number"
                placeholder="10,000"
                value={newExpense.amount}
                onChange={(e) => {
                  setNewExpense({ ...newExpense, amount: e.target.value });
                  setIsReceiptVerified(false);
                  setOcrResult(null);
                  setOcrError("");
                }}
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="category" className="text-gray-700 dark:text-gray-200">카테고리 *</Label>
              <Select
                value={newExpense.category}
                onValueChange={(value) => setNewExpense({ ...newExpense, category: value })}
              >
                <SelectTrigger className="mt-1">
                  <SelectValue placeholder="카테고리 선택" />
                </SelectTrigger>
                <SelectContent>
                  {activeCategories.map((cat) => (
                    <SelectItem key={cat.value} value={cat.value}>
                      {cat.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="memo" className="text-gray-700 dark:text-gray-200">{entryType === "income" ? "수입 내용" : "구매품목"}</Label>
              <Input
                id="memo"
                type="text"
                placeholder={entryType === "income" ? "어떤 수입인가요?" : "무엇을 구매했나요?"}
                value={newExpense.memo}
                onChange={(e) => setNewExpense({ ...newExpense, memo: e.target.value })}
                className="mt-1"
              />
            </div>

            {entryType === "expense" && (
              <>
                <div>
                  <Label htmlFor="receipt" className="text-gray-700 dark:text-gray-200">영수증 인증</Label>
                  <div className="mt-2 flex items-center gap-3">
                    <Button
                      type="button"
                      variant="outline"
                      size="sm"
                      onClick={() => {
                        const input = document.createElement("input");
                        input.type = "file";
                        input.accept = "image/*";
                        input.onchange = (e) => {
                          const file = (e.target as HTMLInputElement).files?.[0];
                          if (file) {
                            setReceiptFile(file);
                            setIsReceiptVerified(false);
                            setOcrResult(null);
                            setOcrError("");
                            toast.success("영수증이 업로드되었습니다");
                          }
                        };
                        input.click();
                      }}
                    >
                      <Upload className="mr-2 h-4 w-4" />
                      {receiptFile ? "변경" : "업로드"}
                    </Button>

                    {receiptFile && (
                      <Badge className={isReceiptVerified ? "bg-green-500" : "bg-gray-500"}>
                        <CheckCircle className="mr-1 h-3 w-3" />
                        {receiptFile.name}
                      </Badge>
                    )}
                  </div>

                  {receiptFile && (
                    <Button
                      type="button"
                      variant="secondary"
                      size="sm"
                      className="mt-3"
                      onClick={handleVerifyReceipt}
                      disabled={ocrLoading}
                    >
                      {ocrLoading ? "영수증 확인 중..." : "영수증 검증하기"}
                    </Button>
                  )}

                  {ocrError && <p className="mt-3 text-sm text-red-500">{ocrError}</p>}

                  {ocrResult && (
                    <div className="mt-3 rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm text-gray-900 dark:border-gray-700 dark:bg-gray-50 dark:text-gray-900">
                      <p className="font-semibold">OCR 결과</p>
                      <p>추출 날짜: {ocrResult.ocr.receipt_date ?? "없음"}</p>
                      <p>추출 금액: {ocrResult.ocr.total_amount ?? "없음"}</p>
                      <p>근거 텍스트: {ocrResult.ocr.raw_text || "없음"}</p>

                      <div className="mt-2">
                        <p
                          className={
                            ocrResult.verification.is_verified
                              ? "font-semibold text-green-600"
                              : "font-semibold text-red-500"
                          }
                        >
                          {ocrResult.verification.is_verified ? "인증 성공" : "인증 실패"}
                        </p>
                        <p>사유: {ocrResult.verification.reason}</p>
                      </div>
                    </div>
                  )}
                </div>

                <div>
                  <Label htmlFor="diary" className="text-gray-700 dark:text-gray-200">한줄 소비 일기</Label>
                  <Textarea
                    id="diary"
                    placeholder="오늘 소비에 대한 생각을 기록해보세요"
                    value={newExpense.diary}
                    onChange={(e) => setNewExpense({ ...newExpense, diary: e.target.value })}
                    className="mt-1"
                    rows={3}
                  />
                </div>
              </>
            )}

            <Button
              onClick={handleAddExpense}
              disabled={saveLoading}
              className={`w-full ${
                entryType === "income"
                  ? "bg-gradient-to-r from-emerald-500 to-green-500 hover:from-emerald-600 hover:to-green-600"
                  : "bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
              }`}
            >
              <Plus className="mr-2 h-4 w-4" />
              {saveLoading ? "저장 중..." : entryType === "income" ? "수입 입력 완료" : "소비 입력 완료"}
            </Button>
          </div>
        </Card>

        {/* Rewards Info */}
        <Card className="border-none bg-gradient-to-br from-amber-50 to-yellow-50 p-6 text-gray-900 backdrop-blur-xl dark:from-amber-50 dark:to-yellow-50 dark:text-gray-900">
          <h4 className="mb-3 font-bold text-gray-900">💰 보상 안내</h4>
          <div className="space-y-2 text-sm text-gray-700">
            <div className="flex items-center justify-between">
              <span>기본 기록</span>
            </div>
            <div className="flex items-center justify-between">
              <span>영수증 인증</span>
              <span className="font-bold text-amber-600">아바타 뽑기권 지급</span>
            </div>
            <div className="flex items-center justify-between">
              <span>일기 작성</span>
            </div>
            <div className="mt-3 rounded-lg border border-amber-300 bg-white p-2 text-center">
              <p className="font-bold text-amber-700">주간 성실도 90점 이상 시</p>
              <p className="text-xs text-amber-600">랜덤 아바타 뽑기권 지급!</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
