export interface AnalyzeReportRequest {
  transactions: {
    date: string;
    amount: number;
    category: string;
    type: string;
  }[];

  totalExpense: number;
  budget: number;
  topCategory: string;
  topCategoryPercent: number;

  dailyAverage: number;
  expenseChangeRate: number;
  budgetUsage: number;

  weeklyData: {
    day: string;
    amount: number;
  }[];

  monthlyData: {
    month: string;
    amount: number;
  }[];

  categoryData: {
    key: string;
    name: string;
    amount: number;
    value: number;
    color: string;
  }[];
}

export interface AnalyzeReportResponse {
  good: string;
  warning: string;
  advice: string;
  prediction: string;

  pattern: string;
  improvement: string;
}

export async function analyzeReport(
  payload: AnalyzeReportRequest
): Promise<AnalyzeReportResponse> {
  const response = await fetch("http://localhost:8000/api/v1/analyze/report", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error("AI 분석 실패");
  }

  return response.json();
}