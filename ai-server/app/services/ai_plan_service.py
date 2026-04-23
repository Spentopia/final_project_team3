import json
from app.clients.openai_client import client

async def generate_ai_plans(budget: int):
    prompt = f"""
    월 예산 {budget}원을 기준으로 3개의 플랜을 만들어라.

    중요:
    - 반드시 모든 텍스트는 한국어로 작성해라.
    - budget은 반드시 300000 ~ 1500000 사이
    - 100000 단위로만 설정 (예: 300000, 400000, ...)
    - 3개의 budget은 서로 달라야 함
    - JSON만 출력

    {{
      "plans": [
        {{
          "name": "",
          "budget": 0,
          "savings": 0,
          "food": 0,
          "transport": 0,
          "living": 0,
          "leisure": 0,
          "description": ""
        }}
      ]
    }}
    """

    response = client.chat.completions.create(
        model="gpt-4o-mini",
        messages=[
            {"role": "system", "content": "너는 가계부 전문가야"},
            {"role": "user", "content": prompt},
        ],
        max_tokens=300,
    )

    content = response.choices[0].message.content.strip()

    print("🔥 GPT 응답:", content)

    try:
        data = json.loads(content)
    except:
        # JSON만 잘라서 파싱
        start = content.find("{")
        end = content.rfind("}") + 1
        cleaned = content[start:end]
        data = json.loads(cleaned)

    return {"plans": data.get("plans", [])}