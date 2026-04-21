from datetime import date
from typing import Optional

from app.clients.openai_client import OpenAIClient


class ReceiptOcrService:

    @staticmethod
    def verify(image_bytes: bytes, mime_type: str, expected_date: str, expected_amount: int):
        # 1) 비전 모델에 영수증 이미지를 보내서 날짜/총액 후보를 추출한다.
        ocr_result = OpenAIClient.extract_receipt_fields(image_bytes, mime_type)

        # 2) 모델 응답은 문자열/null 형태일 수 있으므로
        #    서비스 레이어에서 비교 가능한 타입으로 정규화한다.
        receipt_date = ReceiptOcrService._parse_date(ocr_result.get("receipt_date"))
        user_date = ReceiptOcrService._parse_date(expected_date)
        receipt_amount = ReceiptOcrService._parse_amount(ocr_result.get("total_amount"))

        # 3) 날짜/금액이 모두 정확히 일치할 때만 인증 성공으로 본다.
        date_matched = receipt_date is not None and user_date is not None and receipt_date == user_date
        amount_matched = receipt_amount is not None and receipt_amount == expected_amount
        is_verified = date_matched and amount_matched

        # 4) 프론트가 그대로 렌더링할 수 있도록
        #    OCR 원본 결과, 기대값, 최종 판정 결과를 한 응답에 묶어 반환한다.
        return {
            "ocr": {
                "receipt_date": receipt_date.isoformat() if receipt_date else None,
                "total_amount": receipt_amount,
                "raw_text": ocr_result.get("raw_text", ""),
                "confidence": ocr_result.get("confidence", 0.0),
                "error": ocr_result.get("error"),
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
        # 모델이 날짜를 잘못된 문자열로 반환해도
        # 예외를 터뜨리지 않고 None으로 흡수해서 비교 단계로 넘긴다.
        if not value:
            return None

        try:
            return date.fromisoformat(value)
        except ValueError:
            return None

    @staticmethod
    def _parse_amount(value):
        # 영수증 총액도 문자열/숫자/None이 섞여 들어올 수 있어
        # 비교 전에 int로 통일한다.
        if value is None:
            return None

        try:
            return int(value)
        except (TypeError, ValueError):
            return None

    @staticmethod
    def _build_reason(date_matched: bool, amount_matched: bool):
        # 프론트에서 토스트/상세 문구로 바로 쓰기 쉽게
        # 실패 원인을 사람이 읽는 문장으로 만들어 준다.
        if date_matched and amount_matched:
            return "영수증 날짜와 금액이 모두 정확히 일치합니다."
        if not date_matched and not amount_matched:
            return "영수증 날짜와 금액이 모두 일치하지 않습니다."
        if not date_matched:
            return "영수증 날짜가 입력한 날짜와 정확히 일치하지 않습니다."
        return "영수증 금액이 입력한 금액과 정확히 일치하지 않습니다."
