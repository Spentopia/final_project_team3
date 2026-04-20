import { apiClient } from "@/shared/api/client";

export interface ReceiptOcrRequest {
  image: File;
  expectedDate: string;
  expectedAmount: number;
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

  const response = await apiClient.post<ReceiptOcrResponse>(
    "/api/receipt/ocr",
    formData
  );

  return response.data;
}
