import { apiClient } from "./client";

export interface AnalyzeReportRequest {
  report_type: string;

  start_date: string;

  end_date: string;

  transactions: {
    date: string;
    amount: number;
    category: string;
    type: string;
  }[];

  total_expense: number;

  budget: number;

  top_category: string;

  top_category_percent: number;

  daily_average: number;

  expense_change_rate: number;

  budget_usage: number;

  category_data: {
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

  console.log("🔥 FINAL REQUEST =", JSON.stringify(payload, null, 2));

  try {

    const response = await apiClient.post(
      "/api/reports",
      payload,
      {
        headers: {
          "Content-Type": "application/json",
        },
      }
    );

    return response.data;

  } catch (error) {

    console.error("AI 분석 실패:", error);

    throw error;
  }
}