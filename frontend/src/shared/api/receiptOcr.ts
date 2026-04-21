import { apiClient } from "@/shared/api/client";

export interface ReceiptOcrRequest {
  image: File;
  expectedDate: string;
  expectedAmount: number;
  expenseId?: string; // 소비 저장 후 서버 DB 반영 시 전달
}

export interface ReceiptOcrResponse {
  ocr: {
    receipt_date: string | null;
    total_amount: number | null;
    raw_text: string;
    confidence: number;
    error?: string | null;
  };
  expected: {
    date: string;
    amount: number;
  };
  verification: {
    is_verified: boolean;
    date_matched: boolean;
    amount_matched: boolean;
    reason: string;
  };
}

export async function verifyReceiptOcr(
  payload: ReceiptOcrRequest
): Promise<ReceiptOcrResponse> {
  const formData = new FormData();
  formData.append("image", payload.image);
  formData.append("expected_date", payload.expectedDate);
  formData.append("expected_amount", String(payload.expectedAmount));

  const url = payload.expenseId
    ? `/api/receipt/ocr?expense_id=${payload.expenseId}`
    : "/api/receipt/ocr";

  try {
    const response = await apiClient.post<ReceiptOcrResponse>(url, formData);
    return response.data;
  } catch (error: unknown) {
    if (error && typeof error === "object" && "response" in error) {
      const axiosError = error as { response?: { status?: number; data?: unknown } };
      const status = axiosError.response?.status;

      if (status === 429) {
        throw new Error("영수증 인증은 하루 최대 3건까지 가능합니다.");
      }
      if (status === 409) {
        throw new Error("이미 인증된 소비 내역입니다.");
      }

      const data = axiosError.response?.data;
      const detail = typeof data === "string" ? data : "영수증 검증 중 오류가 발생했습니다.";
      throw new Error(detail);
    }
    throw error;
  }
}
