from app.utils.json_utils import safe_json_loads
import os
import json
import base64

from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()

# 텍스트 작업과 비전 작업 모델을 분리해 두면
# 기능별로 성능/비용을 조절하기 쉽다.
TEXT_MODEL = os.getenv("OPENAI_TEXT_MODEL", "gpt-4o-mini")
VISION_MODEL = os.getenv("OPENAI_VISION_MODEL", "gpt-4o-mini")


class OpenAIClient:
    client = OpenAI(
        api_key=os.getenv("OPENAI_API_KEY"),
        timeout=20.0,
        max_retries=1,
    )

    @staticmethod
    def analyze(spending: str):
        # 모델이 자유 텍스트를 길게 답하는 걸 막기 위해
        # 응답 형식을 프롬프트에서 JSON으로 강하게 제한한다.
        prompt = f"""
다음 소비를 분석해줘:

{spending}

반드시 JSON으로 답변:
{{
  "score": 0~100 숫자,
  "category": "카테고리",
  "pattern": "소비 패턴",
  "risk": "낮음/중간/높음",
  "advice": "절약 조언"
}}
"""

        try:
            # chat.completions.create는 가장 단순한 텍스트 질의 방식이다.
            # 여기서는 소비 분석 결과를 구조화된 JSON으로 받는 용도다.
            response = OpenAIClient.client.chat.completions.create(
                model=TEXT_MODEL,
                messages=[
                    {"role": "system", "content": "너는 소비 분석 AI다."},
                    {"role": "user", "content": prompt},
                ],
            )

            return safe_json_loads(response.choices[0].message.content)

        except Exception as e:
            return {
                "score": 0,
                "category": "오류",
                "pattern": "분석 실패",
                "risk": "오류",
                # 외부 API 에러는 우선 문자열로 노출한다.
                # 운영 단계에서는 로깅과 사용자 메시지를 분리하는 편이 더 좋다.
                "advice": str(e),
            }

    @staticmethod
    def chat(message: str):
        try:
            response = OpenAIClient.client.chat.completions.create(
                model=TEXT_MODEL,
                messages=[
                    {"role": "system", "content": "너는 소비 상담 AI다."},
                    {"role": "user", "content": message},
                ],
            )

            return {"response": response.choices[0].message.content}

        except Exception as e:
            return {"response": str(e)}

    @staticmethod
    def extract_receipt_fields(image_bytes: bytes, mime_type: str):
        # 이미지 파일은 그대로 보낼 수 없어서 base64 data URL로 감싼다.
        # 비전 모델 입력 포맷에 맞추기 위한 전처리 단계다.
        image_base64 = base64.b64encode(image_bytes).decode("utf-8")
        data_url = f"data:{mime_type};base64,{image_base64}"

        # 단순 필드 추출만으로는 손글씨 위조를 막기 어려워서
        # 영수증/카드 승인 전표 특징과 손글씨 의심 여부까지 함께 추출한다.
        prompt = """
이미지를 보고 다음 둘 중 무엇에 가까운지 판별해줘.
- 실제 영수증 또는 카드 승인 전표
- 손글씨 메모, 임의로 적은 종이, 영수증이 아닌 일반 문서

규칙:
- merchant_name은 매장명/가맹점명이 읽히는 경우만 반환한다.
- document_type은 "receipt", "card_slip", "handwritten_note", "unknown" 중 하나로 반환한다.
- handwritten_note는 종이에 손으로 날짜/금액만 적은 경우나 영수증 형식이 없는 경우에만 사용한다.
- 영수증/전표처럼 보여도 손글씨가 본문 대부분을 차지하면 handwriting_suspected를 true로 둔다.
- 날짜는 실제 결제일만 추출한다.
- 날짜는 YYYY-MM-DD 형식으로 반환한다.
- 총액은 최종 결제 금액만 숫자로 반환한다.
- approval_number는 승인번호/거래번호/승인코드가 보이면 반환한다.
- payment_time은 HH:MM 또는 HH:MM:SS 형식으로 반환한다.
- evidence_keywords는 실제 이미지에서 읽힌 영수증/전표 근거 단어만 넣는다.
- has_printed_layout는 인쇄된 전표/영수증 레이아웃이 보이면 true로 둔다.
- line_item_count는 품목/거래 줄 수를 대략 추정해서 숫자로 넣는다.
- 애매하거나 읽을 수 없으면 null로 반환한다.
- 반드시 JSON만 반환한다.

반환 형식:
{
  "document_type": "receipt | card_slip | handwritten_note | unknown",
  "merchant_name": "상호명 또는 null",
  "receipt_date": "YYYY-MM-DD 또는 null",
  "total_amount": 0 또는 null,
  "approval_number": "문자열 또는 null",
  "payment_time": "HH:MM 또는 null",
  "has_printed_layout": true,
  "handwriting_suspected": false,
  "line_item_count": 0,
  "evidence_keywords": ["승인", "합계"],
  "raw_text": "날짜/금액 판단에 사용한 짧은 근거 텍스트",
  "confidence": 0.0
}
"""

        try:
            # response_format=json_object를 줘서
            # 모델이 설명문 대신 JSON 한 덩어리로 응답하도록 유도한다.
            response = OpenAIClient.client.chat.completions.create(
                model=VISION_MODEL,
                response_format={"type": "json_object"},
                messages=[
                    {
                        "role": "system",
                        "content": "너는 영수증 OCR 및 위조 방지 보조 AI다. 실제 영수증/카드 승인 전표의 특징과 손글씨 메모 여부를 구분하고, 보이는 정보만 JSON으로 반환한다.",
                    },
                    {
                        "role": "user",
                        "content": [
                            {"type": "text", "text": prompt},
                            {
                                "type": "image_url",
                                "image_url": {"url": data_url},
                            },
                        ],
                    },
                ],
            )

            return safe_json_loads(response.choices[0].message.content)

        except Exception as e:
            # OCR 실패도 응답 스키마를 최대한 유지하면
            # 서비스 레이어와 프론트가 예외 처리하기 쉬워진다.
            return {
                "document_type": "unknown",
                "merchant_name": None,
                "receipt_date": None,
                "total_amount": None,
                "approval_number": None,
                "payment_time": None,
                "has_printed_layout": False,
                "handwriting_suspected": False,
                "line_item_count": 0,
                "evidence_keywords": [],
                "raw_text": "",
                "confidence": 0.0,
                "error": str(e),
            }
