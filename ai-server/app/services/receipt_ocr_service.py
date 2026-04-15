from datetime import date
from typing import Optional

from app.clients.openai_client import OpenAIClient


class ReceiptOcrService:

    @staticmethod
    def verify(image_bytes: bytes, mime_type: str, expected_date: str, expected_amount: int):
        ocr_result = OpenAIClient.extract_receipt_fields(image_bytes, mime_type)

        receipt_date = ReceiptOcrService._parse_date(ocr_result.get("receipt_date"))
        user_date = ReceiptOcrService._parse_date(expected_date)
        receipt_amount = ReceiptOcrService._parse_amount(ocr_result.get("total_amount"))

        date_matched = receipt_date is not None and user_date is not None and receipt_date == user_date
        amount_matched = receipt_amount is not None and receipt_amount == expected_amount
        is_verified = date_matched and amount_matched

        return {
            "ocr": {
                "receipt_date": receipt_date.isoformat() if receipt_date else None,
                "total_amount": receipt_amount,
                "raw_text": ocr_result.get("raw_text", ""),
                "confidence": ocr_result.get("confidence", 0.0),
            },
            "expected": {
                "date": user_date.isoformat() if user_date else expected_date,
                "amount": expected_amount,
            },
            "verification": {
                "is_verified": is_verified,
                "date_matched": date_matched,
                "amount_matched": amount_matched,
                "reason": ReceiptOcrService._build_reason(date_matched, amount_matched),
            },
        }

    @staticmethod
    def _parse_date(value: Optional[str]):
        if not value:
            return None

        try:
            return date.fromisoformat(value)
        except ValueError:
            return None

    @staticmethod
    def _parse_amount(value):
        if value is None:
            return None

        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _build_reason(date_matched: bool, amount_matched: bool):
        if date_matched and amount_matched:
            return "영수증 날짜와 금액이 모두 정확히 일치합니다."
        if not date_matched and not amount_matched:
            return "영수증 날짜와 금액이 모두 일치하지 않습니다."
        if not date_matched:
            return "영수증 날짜가 입력한 날짜와 정확히 일치하지 않습니다."
        return "영수증 금액이 입력한 금액과 정확히 일치하지 않습니다."
