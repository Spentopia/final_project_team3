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
  const baseUrl = import.meta.env.VITE_AI_SERVER_URL;

  if (!baseUrl) {
    throw new Error("VITE_AI_SERVER_URL이 설정되지 않았습니다.");
  }

  const formData = new FormData();
  formData.append("image", payload.image);
  formData.append("expected_date", payload.expectedDate);
  formData.append("expected_amount", String(payload.expectedAmount));

  const response = await fetch(`${baseUrl}/api/v1/receipt/ocr`, {
    method: "POST",
    body: formData,
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`OCR 요청 실패: ${response.status} ${text}`);
  }

  return response.json();
}