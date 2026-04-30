from datetime import date, timedelta
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
        receipt_amount = ReceiptOcrService._parse_amount(ocr_result.get("total_amount"))

        # 3) 인증은 사용자가 입력한 날짜/금액과의 일치가 아니라
        #    영수증 자체에서 결제일/총액이 추출되고, 결제일이 최근 7일 이내인지로 판단한다.
        has_receipt_date = receipt_date is not None
        has_total_amount = receipt_amount is not None
        is_recent_receipt = ReceiptOcrService._is_recent_receipt_date(receipt_date)
        is_verified = has_receipt_date and has_total_amount and is_recent_receipt

        # 4) 프론트가 그대로 렌더링할 수 있도록
        #    OCR 원본 결과, 기대값, 최종 판정 결과를 한 응답에 묶어 반환한다.
        return {
            "ocr": {
                "merchant_name": ocr_result.get("merchant_name"),
                "receipt_date": receipt_date.isoformat() if receipt_date else None,
                "total_amount": receipt_amount,
                "raw_text": ocr_result.get("raw_text", ""),
                "confidence": ocr_result.get("confidence", 0.0),
                "error": ocr_result.get("error"),
            },
            "expected": {
                "date": expected_date,
                "amount": expected_amount,
            },
            "verification": {
                "is_verified": is_verified,
                "date_matched": has_receipt_date,
                "amount_matched": has_total_amount,
                "is_recent_receipt": is_recent_receipt,
                "reason": ReceiptOcrService._build_reason(
                    has_receipt_date,
                    has_total_amount,
                    is_recent_receipt,
                ),
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
    def _is_recent_receipt_date(receipt_date):
        if receipt_date is None:
            return False

        today = date.today()
        earliest_allowed_date = today - timedelta(days=7)
        return earliest_allowed_date <= receipt_date <= today

    @staticmethod
    def _build_reason(has_receipt_date: bool, has_total_amount: bool, is_recent_receipt: bool):
        # 프론트에서 토스트/상세 문구로 바로 쓰기 쉽게
        # 실패 원인을 사람이 읽는 문장으로 만들어 준다.
        if has_receipt_date and has_total_amount and is_recent_receipt:
            return "영수증 인증 완료 시 보상이 지급됩니다."
        if not has_receipt_date and not has_total_amount:
            return "영수증에서 결제 날짜와 총 금액을 추출하지 못했습니다."
        if not has_receipt_date:
            return "영수증에서 결제 날짜를 추출하지 못했습니다."
        if not has_total_amount:
            return "영수증에서 총 금액을 추출하지 못했습니다."
        return "최근 7일 이내 영수증만 인증할 수 있습니다."
