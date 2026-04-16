import os
import json
import base64

from openai import OpenAI
from dotenv import load_dotenv

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))
TEXT_MODEL = os.getenv("OPENAI_TEXT_MODEL", "gpt-4o-mini")
VISION_MODEL = os.getenv("OPENAI_VISION_MODEL", "gpt-4o-mini")


class OpenAIClient:

    @staticmethod
    def analyze(spending: str):
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
            response = client.chat.completions.create(
                model=TEXT_MODEL,
                messages=[
                    {"role": "system", "content": "너는 소비 분석 AI다."},
                    {"role": "user", "content": prompt},
                ],
            )

            return json.loads(response.choices[0].message.content)

        except Exception as e:
            return {
                "score": 0,
                "category": "오류",
                "pattern": "분석 실패",
                "risk": "오류",
                "advice": str(e),
            }

    @staticmethod
    def chat(message: str):
        try:
            response = client.chat.completions.create(
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
        image_base64 = base64.b64encode(image_bytes).decode("utf-8")
        data_url = f"data:{mime_type};base64,{image_base64}"

        prompt = """
영수증 이미지에서 날짜와 결제 총액만 추출해줘.

규칙:
- 상호명, 품목, 카테고리는 추출하지 않는다.
- 날짜는 실제 결제일만 추출한다.
- 날짜는 YYYY-MM-DD 형식으로 반환한다.
- 총액은 최종 결제 금액만 숫자로 반환한다.
- 애매하거나 읽을 수 없으면 null로 반환한다.
- 반드시 JSON만 반환한다.

반환 형식:
{
  "receipt_date": "YYYY-MM-DD 또는 null",
  "total_amount": 0 또는 null,
  "raw_text": "날짜/금액 판단에 사용한 짧은 근거 텍스트",
  "confidence": 0.0
}
"""

        try:
            response = client.chat.completions.create(
                model=VISION_MODEL,
                response_format={"type": "json_object"},
                messages=[
                    {"role": "system", "content": "너는 영수증 OCR 전용 AI다. 날짜와 총액만 정확히 추출한다."},
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

            return json.loads(response.choices[0].message.content)

        except Exception as e:
            return {
                "receipt_date": None,
                "total_amount": None,
                "raw_text": "",
                "confidence": 0.0,
                "error": str(e),
            }
