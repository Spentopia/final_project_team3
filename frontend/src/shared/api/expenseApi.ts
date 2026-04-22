import { apiClient } from "@/shared/api/client";

export interface CreateExpenseRequest {
  date: string;
  amount: number;
  category: string;
  memo: string;
  transactionType: "expense" | "income";
  diary: string;
}

export interface CreateExpenseResponse {
  id: string;
  date: string;
  amount: number;
  category: string;
  memo: string | null;
  transactionType: "expense" | "income";
  receiptVerified: boolean;
  diary: string | null;
}

export interface ListExpensesResponse {
  items: CreateExpenseResponse[];
}

let listExpensesInFlight: Promise<CreateExpenseResponse[]> | null = null;

export async function listExpenses(): Promise<CreateExpenseResponse[]> {
  if (listExpensesInFlight) {
    return listExpensesInFlight;
  }

  try {
    listExpensesInFlight = apiClient
      .get<ListExpensesResponse>("/api/expenses")
      .then((response) =>
        Array.isArray(response.data?.items) ? response.data.items : []
      )
      .finally(() => {
        listExpensesInFlight = null;
      });

    return await listExpensesInFlight;
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const axiosError = error as {
        response?: { status?: number; data?: unknown };
      };
      const status = axiosError.response?.status;
      const data = axiosError.response?.data;
      const detail = typeof data === "string" ? data : JSON.stringify(data);
      throw new Error(`소비 조회 실패: ${status ?? "unknown"} ${detail ?? ""}`);
    }

    throw error;
  }
}

export async function createExpense(
  payload: CreateExpenseRequest
): Promise<CreateExpenseResponse> {
  try {
    const response = await apiClient.post<CreateExpenseResponse>("/api/expenses", payload);
    return response.data;
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const axiosError = error as {
        response?: { status?: number; data?: unknown };
      };
      const status = axiosError.response?.status;
      const data = axiosError.response?.data;
      const detail = typeof data === "string" ? data : JSON.stringify(data);
      throw new Error(`소비 저장 실패: ${status ?? "unknown"} ${detail ?? ""}`);
    }

    throw error;
  }
}

export async function deleteExpense(expenseId: string): Promise<void> {
  try {
    await apiClient.delete(`/api/expenses/${expenseId}`);
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const axiosError = error as {
        response?: { status?: number; data?: unknown };
      };
      const status = axiosError.response?.status;
      const data = axiosError.response?.data;
      const detail = typeof data === "string" ? data : JSON.stringify(data);
      throw new Error(`소비 삭제 실패: ${status ?? "unknown"} ${detail ?? ""}`);
    }

    throw error;
  }
}
