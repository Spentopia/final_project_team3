import { apiClient } from "./client";

export interface AnalyzeReportRequest {
  analysis_kind: "report" | "pattern";

  report_type: "weekly" | "monthly";

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
  payload: AnalyzeReportRequest,
  xPayment?: string
): Promise<AnalyzeReportResponse> {
  try {

    const response = await apiClient.post(
      "/api/reports",
      payload,
      {
        headers: {
          "Content-Type": "application/json",
          ...(xPayment ? { "X-PAYMENT": xPayment } : {}),
        },
      }
    );

    return response.data;

  } catch (error) {
    throw error;
  }
}
