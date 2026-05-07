import { apiClient } from "@/shared/api/client";

export interface ReceiptOcrRequest {
  image: File;
  expectedDate: string;
  expectedAmount: number;
  expenseId?: string; // 소비 저장 후 서버 DB 반영 시 전달
}

export interface ReceiptOcrResponse {
  ocr: {
    document_type: "receipt" | "card_slip" | "handwritten_note" | "unknown";
    merchant_name?: string | null;
    receipt_date: string | null;
    total_amount: number | null;
    approval_number?: string | null;
    payment_time?: string | null;
    has_printed_layout?: boolean;
    handwriting_suspected?: boolean;
    line_item_count?: number;
    evidence_keywords?: string[];
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
    is_recent_receipt?: boolean;
    receipt_like?: boolean;
    fraud_suspected?: boolean;
    layout_score?: number;
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

  const trimmedExpenseId = payload.expenseId?.trim();
  const isUuid =
    trimmedExpenseId !== undefined &&
    /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(
      trimmedExpenseId
    );

  if (trimmedExpenseId && !isUuid) {
    throw new Error("소비 저장 응답에 유효한 expense_id가 없습니다.");
  }

  const url = isUuid
    ? `/api/receipt/ocr?expense_id=${encodeURIComponent(trimmedExpenseId)}`
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
