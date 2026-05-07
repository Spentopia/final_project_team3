from datetime import date, timedelta
from typing import Optional
import re

from app.clients.openai_client import OpenAIClient


class ReceiptOcrService:

    @staticmethod
    def verify(image_bytes: bytes, mime_type: str, expected_date: str, expected_amount: int):
        # 1) 비전 모델에 영수증 이미지를 보내서 날짜/총액 후보를 추출한다.
        ocr_result = OpenAIClient.extract_receipt_fields(image_bytes, mime_type)

        # 2) 모델 응답은 문자열/null 형태일 수 있으므로
        #    서비스 레이어에서 비교 가능한 타입으로 정규화한다.
        document_type = ocr_result.get("document_type") or "unknown"
        merchant_name = ReceiptOcrService._normalize_string(ocr_result.get("merchant_name"))
        receipt_date = ReceiptOcrService._parse_date(ocr_result.get("receipt_date"))
        receipt_amount = ReceiptOcrService._parse_amount(ocr_result.get("total_amount"))
        approval_number = ReceiptOcrService._normalize_string(ocr_result.get("approval_number"))
        payment_time = ReceiptOcrService._normalize_time(ocr_result.get("payment_time"))
        has_printed_layout = bool(ocr_result.get("has_printed_layout"))
        handwriting_suspected = bool(ocr_result.get("handwriting_suspected"))
        line_item_count = ReceiptOcrService._parse_amount(ocr_result.get("line_item_count")) or 0
        evidence_keywords = ReceiptOcrService._normalize_keywords(ocr_result.get("evidence_keywords"))
        raw_text = ocr_result.get("raw_text", "") or ""
        confidence = ReceiptOcrService._parse_confidence(ocr_result.get("confidence"))
        expected_date_value = ReceiptOcrService._parse_date(expected_date)

        # 3) 손글씨 위조와 일반 문서를 최대한 걸러내기 위해
        #    영수증/카드전표 근거를 점수화해 판별한다.
        has_receipt_date = receipt_date is not None
        has_total_amount = receipt_amount is not None
        is_recent_receipt = ReceiptOcrService._is_recent_receipt_date(receipt_date)
        date_matched = ReceiptOcrService._dates_match(receipt_date, expected_date_value)
        amount_matched = ReceiptOcrService._amount_matches(receipt_amount, expected_amount)
        layout_score = ReceiptOcrService._calculate_layout_score(
            document_type=document_type,
            merchant_name=merchant_name,
            approval_number=approval_number,
            payment_time=payment_time,
            has_printed_layout=has_printed_layout,
            line_item_count=line_item_count,
            evidence_keywords=evidence_keywords,
            raw_text=raw_text,
            confidence=confidence,
        )
        receipt_like = layout_score >= 4
        fraud_suspected = handwriting_suspected or document_type == "handwritten_note"
        is_verified = (
            has_receipt_date
            and has_total_amount
            and is_recent_receipt
            and receipt_like
            and not fraud_suspected
        )

        # 4) 프론트가 그대로 렌더링할 수 있도록
        #    OCR 원본 결과, 기대값, 최종 판정 결과를 한 응답에 묶어 반환한다.
        return {
            "ocr": {
                "document_type": document_type,
                "merchant_name": merchant_name,
                "receipt_date": receipt_date.isoformat() if receipt_date else None,
                "total_amount": receipt_amount,
                "approval_number": approval_number,
                "payment_time": payment_time,
                "has_printed_layout": has_printed_layout,
                "handwriting_suspected": handwriting_suspected,
                "line_item_count": line_item_count,
                "evidence_keywords": evidence_keywords,
                "raw_text": raw_text,
                "confidence": confidence,
                "error": ocr_result.get("error"),
            },
            "expected": {
                "date": expected_date,
                "amount": expected_amount,
            },
            "verification": {
                "is_verified": is_verified,
                "date_matched": date_matched,
                "amount_matched": amount_matched,
                "is_recent_receipt": is_recent_receipt,
                "receipt_like": receipt_like,
                "fraud_suspected": fraud_suspected,
                "layout_score": layout_score,
                "reason": ReceiptOcrService._build_reason(
                    is_verified=is_verified,
                    fraud_suspected=fraud_suspected,
                    receipt_like=receipt_like,
                    has_receipt_date=has_receipt_date,
                    has_total_amount=has_total_amount,
                    is_recent_receipt=is_recent_receipt,
                    date_matched=date_matched,
                    amount_matched=amount_matched,
                    document_type=document_type,
                    evidence_keywords=evidence_keywords,
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
    def _normalize_string(value: Optional[str]):
        if value is None:
            return None
        text = str(value).strip()
        return text or None

    @staticmethod
    def _normalize_time(value: Optional[str]):
        if not value:
            return None
        text = str(value).strip()
        if re.fullmatch(r"\d{2}:\d{2}(:\d{2})?", text):
            return text
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
    def _parse_confidence(value):
        try:
            parsed = float(value)
        except (TypeError, ValueError):
            return 0.0
        return max(0.0, min(parsed, 1.0))

    @staticmethod
    def _normalize_keywords(value):
        if not isinstance(value, list):
            return []
        keywords = []
        for item in value:
            text = str(item).strip()
            if text:
                keywords.append(text)
        return keywords[:12]

    @staticmethod
    def _is_recent_receipt_date(receipt_date):
        if receipt_date is None:
            return False

        today = date.today()
        earliest_allowed_date = today - timedelta(days=7)
        return earliest_allowed_date <= receipt_date <= today

    @staticmethod
    def _dates_match(receipt_date, expected_date):
        if receipt_date is None or expected_date is None:
            return False
        return receipt_date == expected_date

    @staticmethod
    def _amount_matches(receipt_amount, expected_amount):
        if receipt_amount is None or expected_amount is None or expected_amount <= 0:
            return False
        return abs(receipt_amount - expected_amount) <= max(500, int(expected_amount * 0.05))

    @staticmethod
    def _calculate_layout_score(
        document_type,
        merchant_name,
        approval_number,
        payment_time,
        has_printed_layout,
        line_item_count,
        evidence_keywords,
        raw_text,
        confidence,
    ):
        score = 0

        if document_type in {"receipt", "card_slip"}:
            score += 2
        if has_printed_layout:
            score += 1
        if merchant_name:
            score += 1
        if approval_number:
            score += 2
        if payment_time:
            score += 1
        if line_item_count >= 2:
            score += 1
        if confidence >= 0.6:
            score += 1

        keyword_hits = set()
        combined_text = " ".join(evidence_keywords + [raw_text]).lower()
        receipt_patterns = [
            "승인",
            "카드",
            "합계",
            "총액",
            "거래",
            "매출전표",
            "단말기",
            "가맹점",
            "vat",
            "approval",
            "auth",
            "sale",
            "total",
        ]
        for pattern in receipt_patterns:
            if pattern.lower() in combined_text:
                keyword_hits.add(pattern)
        score += min(2, len(keyword_hits))

        return score

    @staticmethod
    def _build_reason(
        *,
        is_verified: bool,
        fraud_suspected: bool,
        receipt_like: bool,
        has_receipt_date: bool,
        has_total_amount: bool,
        is_recent_receipt: bool,
        date_matched: bool,
        amount_matched: bool,
        document_type: str,
        evidence_keywords,
    ):
        # 프론트에서 토스트/상세 문구로 바로 쓰기 쉽게
        # 실패 원인을 사람이 읽는 문장으로 만들어 준다.
        if is_verified:
            if date_matched and amount_matched:
                return "영수증 인증이 완료되었습니다. 날짜와 금액도 입력값과 일치합니다."
            return "영수증 또는 카드 승인 전표로 판정되어 인증이 완료되었습니다."
        if fraud_suspected:
            return "손글씨 메모 또는 영수증이 아닌 이미지로 보여 인증할 수 없습니다."
        if not receipt_like:
            if document_type == "unknown" and evidence_keywords:
                return f"영수증/카드전표 근거가 부족합니다. 확인된 단서: {', '.join(evidence_keywords[:4])}"
            return "영수증 또는 카드 승인 전표로 보기 어려워 인증할 수 없습니다."
        if not has_receipt_date and not has_total_amount:
            return "영수증에서 결제 날짜와 총 금액을 추출하지 못했습니다."
        if not has_receipt_date:
            return "영수증에서 결제 날짜를 추출하지 못했습니다."
        if not has_total_amount:
            return "영수증에서 총 금액을 추출하지 못했습니다."
        if not is_recent_receipt:
            return "최근 7일 이내 영수증 또는 카드 승인 전표만 인증할 수 있습니다."
        if not date_matched and not amount_matched:
            return "영수증은 맞지만 현재 입력한 날짜와 금액이 영수증 내용과 다를 수 있습니다."
        if not date_matched:
            return "영수증은 맞지만 입력한 날짜와 영수증 결제일이 다를 수 있습니다."
        if not amount_matched:
            return "영수증은 맞지만 입력한 금액과 영수증 총액이 다를 수 있습니다."
        return "최근 7일 이내 영수증만 인증할 수 있습니다."
