import os
from openai import OpenAI
from dotenv import load_dotenv
import json

load_dotenv()
client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))

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
                model="gpt-4.1-mini",
                messages=[
                    {"role": "system", "content": "너는 소비 분석 AI다."},
                    {"role": "user", "content": prompt}
                ]
            )

            return json.loads(response.choices[0].message.content)

        except Exception as e:
            return {
                "score": 0,
                "category": "오류",
                "pattern": "분석 실패",
                "risk": "오류",
                "advice": str(e)
            }

    @staticmethod
    def chat(message: str):

        try:
            response = client.chat.completions.create(
                model="gpt-4.1-mini",
                messages=[
                    {"role": "system", "content": "너는 소비 상담 AI다."},
                    {"role": "user", "content": message}
                ]
            )

            return {"response": response.choices[0].message.content}

        except Exception as e:
            return {"response": str(e)}