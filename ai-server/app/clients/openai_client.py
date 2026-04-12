from openai import OpenAI
import os
from dotenv import load_dotenv

load_dotenv()

client = OpenAI(api_key=os.getenv("OPENAI_API_KEY"))


def analyze_with_ai(spending: str):

    prompt = f"""
너는 개인 재무 분석 전문가야.

다음 소비 데이터를 분석해서 JSON으로만 답해.

소비 데이터:
{spending}

반드시 아래 형식으로만 응답해:

{{
  "score": 숫자 (0~100),
  "pattern": "소비 패턴 설명",
  "risk_level": "낮음/중간/높음",
  "category_amount": {{
    "식비": 숫자,
    "교통": 숫자,
    "쇼핑": 숫자,
    "기타": 숫자
  }},
  "monthly_estimate": "문자열 (예: 약 120만원)",
  "saving_possible": "문자열 (예: 약 30만원)",
  "report": "자세한 분석 리포트"
}}
"""

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "너는 소비 분석 AI다."},
            {"role": "user", "content": prompt}
        ],
        temperature=0.7
    )

    return response.choices[0].message.content