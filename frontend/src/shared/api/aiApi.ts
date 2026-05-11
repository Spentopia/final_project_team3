import { aiClient } from "./client";

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
  try {
    const response = await aiClient.post(
      "/api/v1/analyze/report",
      payload
    );

    return response.data;
  } catch (error) {
    console.error("AI 분석 실패:", error);
    throw error;
  }
}