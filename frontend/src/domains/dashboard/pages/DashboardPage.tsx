import { useState } from "react";
import { Calendar } from "@/shared/ui/calendar";
import { Card } from "@/shared/ui/card";
import { Button } from "@/shared/ui/button";
import { Input } from "@/shared/ui/input";
import { Label } from "@/shared/ui/label";
import { Textarea } from "@/shared/ui/textarea";
import { verifyReceiptOcr, ReceiptOcrResponse } from "@/shared/api/receiptOcr";
import { createExpense, type CreateExpenseResponse } from "@/shared/api/expenseApi";
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
  Trash2,
  CheckCircle,
  TrendingDown,
  Zap,
} from "lucide-react";
import { format, isValid, parse } from "date-fns";
import { ko } from "date-fns/locale";
import { toast } from "sonner";

interface Expense {
  id: string | number;
  date: Date;
  amount: number;
  category: string;
  memo: string;
  receipt?: boolean;
  diary?: string;
}

const toDashboardExpense = (savedExpense: CreateExpenseResponse): Expense => ({
  id: savedExpense.id,
  date: parse(savedExpense.date, "yyyy-MM-dd", new Date()),
  amount: savedExpense.amount,
  category: savedExpense.category,
  memo: savedExpense.memo ?? "",
  receipt: savedExpense.receiptVerified,
  diary: savedExpense.diary ?? "",
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

export default function DashboardPage() {
  const [selectedDate, setSelectedDate] = useState<Date | undefined>(new Date());
  const [expenses, setExpenses] = useState<Expense[]>([
    {
      id: 1,
      date: new Date(),
      amount: 12000,
      category: "food",
      memo: "점심 식사",
      receipt: true,
      diary: "오늘은 친구들과 맛있는 점심을 먹었다",
    },
    {
      id: 2,
      date: new Date(),
      amount: 3500,
      category: "transport",
      memo: "택시",
      receipt: false,
    },
  ]);

  const [newExpense, setNewExpense] = useState({
    amount: "",
    category: "",
    memo: "",
    diary: "",
  });

  const [receiptFile, setReceiptFile] = useState<File | null>(null);
  const [ocrLoading, setOcrLoading] = useState(false);
  const [saveLoading, setSaveLoading] = useState(false);
  const [ocrResult, setOcrResult] = useState<ReceiptOcrResponse | null>(null);
  const [isReceiptVerified, setIsReceiptVerified] = useState(false);
  const [ocrError, setOcrError] = useState("");

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
      toast.error("영수증 검증 실패");
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
        receiptVerified: isReceiptVerified,
        diary: newExpense.diary,
      };

      const savedExpense = await createExpense(payload);

      const expense = toDashboardExpense(savedExpense);

      setExpenses((prev) => [expense, ...prev]);
      setSelectedDate(expense.date);

      let reward = 10;
      if (isReceiptVerified) reward += 20;
      if (newExpense.diary.trim()) reward += 15;

      toast.success(
        <div>
          <p className="font-bold">소비 기록 완료! 🎉</p>
          <p className="text-sm">+{reward} SPT 획득</p>
        </div>
      );

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
    } catch (error) {
      const message =
        error instanceof Error ? error.message : "소비 저장 중 오류가 발생했습니다.";
      toast.error(message);
    } finally {
      setSaveLoading(false);
    }
  };

  const handleDeleteExpense = (id: string | number) => {
    setExpenses((prev) => prev.filter((e) => e.id !== id));
    toast.success("삭제되었습니다");
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

  const selectedDateExpenses = expenses.filter(
    (e) => format(e.date, "yyyy-MM-dd") === format(selectedDate || new Date(), "yyyy-MM-dd")
  );

  const dailyTotal = selectedDateExpenses.reduce((sum, e) => sum + e.amount, 0);

  const monthlyTotal = expenses
    .filter(
      (e) =>
        e.date.getFullYear() === (selectedDate || new Date()).getFullYear() &&
        e.date.getMonth() === (selectedDate || new Date()).getMonth()
    )
    .reduce((sum, e) => sum + e.amount, 0);

  const recordedDates = expenses.map((expense) => expense.date);
  const selectedDateInputValue = selectedDate ? format(selectedDate, "yyyy-MM-dd") : "";

  const getCategoryInfo = (categoryValue: string) => {
    return categories.find((c) => c.value === categoryValue) || categories[categories.length - 1];
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
              <p className="text-sm text-gray-600 dark:text-gray-400">이번 달 소비 내역</p>
            </div>
            <div className="text-right">
              <p className="text-3xl font-bold text-gray-900 dark:text-gray-100">
                {monthlyTotal.toLocaleString()}원
              </p>
              <div className="mt-1 flex items-center justify-end gap-1 text-sm text-green-600 dark:text-green-400">
                <TrendingDown className="h-4 w-4" />
                <span>지난달 대비 -12%</span>
              </div>
            </div>
          </div>

          <div className="grid grid-cols-3 gap-4">
            <div className="rounded-lg bg-gradient-to-br from-cyan-50 to-cyan-100 p-4 dark:from-cyan-900/30 dark:to-cyan-800/30">
              <p className="mb-1 text-sm text-cyan-700 dark:text-cyan-300">예산</p>
              <p className="font-bold text-cyan-900 dark:text-cyan-100">500,000원</p>
            </div>
            <div className="rounded-lg bg-gradient-to-br from-blue-50 to-blue-100 p-4 dark:from-blue-900/30 dark:to-blue-800/30">
              <p className="mb-1 text-sm text-blue-700 dark:text-blue-300">남은 예산</p>
              <p className="font-bold text-blue-900 dark:text-blue-100">
                {(500000 - monthlyTotal).toLocaleString()}원
              </p>
            </div>
            <div className="rounded-lg bg-gradient-to-br from-teal-50 to-teal-100 p-4 dark:from-teal-900/30 dark:to-teal-800/30">
              <p className="mb-1 text-sm text-teal-700 dark:text-teal-300">사용률</p>
              <p className="font-bold text-teal-900 dark:text-teal-100">
                {Math.round((monthlyTotal / 500000) * 100)}%
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
            className="rounded-lg"
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
                총 {dailyTotal.toLocaleString()}원 · {selectedDateExpenses.length}건
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
                <p className="text-gray-500">아직 기록된 소비가 없어요</p>
                <p className="mt-1 text-sm text-gray-400">오른쪽에서 소비를 기록해보세요!</p>
              </div>
            ) : (
              selectedDateExpenses.map((expense) => {
                const categoryInfo = getCategoryInfo(expense.category);

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
                          {expense.memo || "메모 없음"}
                        </p>
                        <div className="mt-1 flex items-center gap-2">
                          <p className="text-sm text-gray-600 dark:text-gray-400">
                            {categoryInfo.label}
                          </p>
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
                      <p className="font-bold text-gray-900 dark:text-gray-100">
                        {expense.amount.toLocaleString()}원
                      </p>
                      <Button
                        variant="ghost"
                        size="icon"
                        onClick={() => handleDeleteExpense(expense.id)}
                      >
                        <Trash2 className="h-4 w-4 text-red-500" />
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
        {/* Quick Stats */}
        <Card className="border-none bg-gradient-to-br from-cyan-500 to-blue-500 p-6 text-white backdrop-blur-xl">
          <div className="mb-4 flex items-center justify-between">
            <h3 className="font-bold">이번 주 성실도</h3>
            <Zap className="h-5 w-5" />
          </div>
          <p className="mb-2 text-3xl font-bold">85점</p>
          <div className="mb-4 h-2 overflow-hidden rounded-full bg-white/30">
            <div className="h-full w-[85%] bg-white"></div>
          </div>
          <div className="space-y-2 text-sm">
            <div className="flex justify-between">
              <span>소비 기록</span>
              <span className="font-bold">35/40</span>
            </div>
            <div className="flex justify-between">
              <span>영수증 인증</span>
              <span className="font-bold">18/20</span>
            </div>
            <div className="flex justify-between">
              <span>일기 작성</span>
              <span className="font-bold">12/15</span>
            </div>
            <div className="flex justify-between">
              <span>예산 체크</span>
              <span className="font-bold">15/15</span>
            </div>
            <div className="flex justify-between">
              <span>연속 활동</span>
              <span className="font-bold">7일 🔥</span>
            </div>
          </div>
        </Card>

        {/* Add Expense Form */}
        <Card className="border-none bg-white/80 p-6 backdrop-blur-xl dark:bg-gray-800/80">
          <div className="mb-4 flex items-center gap-2">
            <Plus className="h-5 w-5 text-cyan-600 dark:text-cyan-400" />
            <h3 className="font-bold text-gray-900 dark:text-gray-100">소비 기록하기</h3>
          </div>

          <div className="space-y-4">
            <div>
              <Label>날짜</Label>
              <Input
                type="date"
                value={selectedDateInputValue}
                onChange={(e) => handleExpenseDateChange(e.target.value)}
                className="mt-1"
              />
              <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                날짜를 직접 입력하거나 왼쪽 달력에서 선택할 수 있어요.
              </p>
            </div>

            <div>
              <Label htmlFor="amount">금액 *</Label>
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
              <Label htmlFor="category">카테고리 *</Label>
              <Select
                value={newExpense.category}
                onValueChange={(value) => setNewExpense({ ...newExpense, category: value })}
              >
                <SelectTrigger className="mt-1">
                  <SelectValue placeholder="카테고리 선택" />
                </SelectTrigger>
                <SelectContent>
                  {categories.map((cat) => (
                    <SelectItem key={cat.value} value={cat.value}>
                      {cat.label}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div>
              <Label htmlFor="memo">메모</Label>
              <Input
                id="memo"
                type="text"
                placeholder="무엇을 구매했나요?"
                value={newExpense.memo}
                onChange={(e) => setNewExpense({ ...newExpense, memo: e.target.value })}
                className="mt-1"
              />
            </div>

            <div>
              <Label htmlFor="receipt">영수증 인증 (+20 SPT)</Label>
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
                <div className="mt-3 rounded-lg border border-gray-200 bg-gray-50 p-3 text-sm dark:border-gray-700 dark:bg-gray-900/50">
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
              <Label htmlFor="diary">한줄 소비 일기 (+15 SPT)</Label>
              <Textarea
                id="diary"
                placeholder="오늘 소비에 대한 생각을 기록해보세요"
                value={newExpense.diary}
                onChange={(e) => setNewExpense({ ...newExpense, diary: e.target.value })}
                className="mt-1"
                rows={3}
              />
            </div>

            <Button
              onClick={handleAddExpense}
              disabled={saveLoading}
              className="w-full bg-gradient-to-r from-cyan-500 to-blue-500 hover:from-cyan-600 hover:to-blue-600"
            >
              <Plus className="mr-2 h-4 w-4" />
              {saveLoading ? "저장 중..." : "기록 완료"}
            </Button>
          </div>
        </Card>

        {/* Rewards Info */}
        <Card className="border-none bg-gradient-to-br from-amber-50 to-yellow-50 p-6 backdrop-blur-xl">
          <h4 className="mb-3 font-bold text-gray-900">💰 보상 안내</h4>
          <div className="space-y-2 text-sm text-gray-700">
            <div className="flex items-center justify-between">
              <span>기본 기록</span>
              <span className="font-bold text-amber-600">+10 SPT</span>
            </div>
            <div className="flex items-center justify-between">
              <span>영수증 인증</span>
              <span className="font-bold text-amber-600">+20 SPT</span>
            </div>
            <div className="flex items-center justify-between">
              <span>일기 작성</span>
              <span className="font-bold text-amber-600">+15 SPT</span>
            </div>
            <div className="mt-3 rounded-lg border border-amber-300 bg-white p-2 text-center">
              <p className="font-bold text-amber-700">주간 성실도 90점 이상 시</p>
              <p className="text-xs text-amber-600">랜덤 아바타 + 보너스 SPT!</p>
            </div>
          </div>
        </Card>
      </div>
    </div>
  );
}
