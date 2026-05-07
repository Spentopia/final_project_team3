import { ChangeEvent, FormEvent, useState } from "react";
import {
  ReceiptOcrResponse,
  verifyReceiptOcr,
} from "@/shared/api/receiptOcr";
import styles from "./ReceiptOcrPanel.module.css";

type Props = {
  expectedDate: string;
  expectedAmount: string;
  onApplyOcrResult: (data: {
    receiptDate: string | null;
    totalAmount: number | null;
    verified: boolean;
    response: ReceiptOcrResponse;
  }) => void;
};

export default function ReceiptOcrPanel({
  expectedDate,
  expectedAmount,
  onApplyOcrResult,
}: Props) {
  const [imageFile, setImageFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<ReceiptOcrResponse | null>(null);
  const [error, setError] = useState("");

  const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] ?? null;
    setImageFile(file);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();

    if (!imageFile) {
      setError("영수증 이미지를 선택해 주세요.");
      return;
    }

    if (!expectedDate) {
      setError("날짜를 먼저 입력해 주세요.");
      return;
    }

    if (!expectedAmount || Number.isNaN(Number(expectedAmount))) {
      setError("금액을 먼저 입력해 주세요.");
      return;
    }

    try {
      setLoading(true);
      setError("");
      setResult(null);

      const data = await verifyReceiptOcr({
        image: imageFile,
        expectedDate,
        expectedAmount: Number(expectedAmount),
      });

      setResult(data);

      onApplyOcrResult({
        receiptDate: data.ocr.receipt_date,
        totalAmount: data.ocr.total_amount,
        verified: data.verification.is_verified,
        response: data,
      });
    } catch (err) {
      const message =
        err instanceof Error ? err.message : "알 수 없는 오류가 발생했습니다.";
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles.panel}>
      <form onSubmit={handleSubmit} className={styles.form}>
        <div>
          <label>영수증 이미지</label>
          <input type="file" accept="image/*" onChange={handleFileChange} />
        </div>

        <button type="submit" disabled={loading}>
          {loading ? "영수증 확인 중..." : "영수증 검증하기"}
        </button>
      </form>

      {error && (
        <p className={styles.error}>{error}</p>
      )}

      {result && (
        <div className={styles.result}>
          <h4>OCR 결과</h4>
          <p>문서 유형: {result.ocr.document_type}</p>
          <p>상호명: {result.ocr.merchant_name ?? "없음"}</p>
          <p>추출 날짜: {result.ocr.receipt_date ?? "없음"}</p>
          <p>추출 금액: {result.ocr.total_amount ?? "없음"}</p>
          <p>승인번호: {result.ocr.approval_number ?? "없음"}</p>
          <p>인쇄 레이아웃: {result.ocr.has_printed_layout ? "예" : "아니오"}</p>
          <p>손글씨 의심: {result.ocr.handwriting_suspected ? "예" : "아니오"}</p>
          <p>근거 키워드: {result.ocr.evidence_keywords?.join(", ") || "없음"}</p>
          <p>근거 텍스트: {result.ocr.raw_text || "없음"}</p>

          <h4 className={styles.verificationTitle}>검증 결과</h4>
          <p
            className={`${styles.verificationStatus} ${
              result.verification.is_verified
                ? styles.verified
                : styles.rejected
            }`}
          >
            {result.verification.is_verified ? "인증 성공" : "인증 실패"}
          </p>
          <p>전표 판정 점수: {result.verification.layout_score ?? 0}</p>
          <p>위조 의심: {result.verification.fraud_suspected ? "예" : "아니오"}</p>
          <p>사유: {result.verification.reason}</p>
        </div>
      )}
    </div>
  );
}
