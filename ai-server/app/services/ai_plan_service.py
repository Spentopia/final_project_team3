import json
import random
from app.clients.openai_client import client

def fix_budgets(plans):
    if not plans:
        return []

    possible = list(range(300000, 1500001, 100000))
    random_budgets = random.sample(possible, k=min(3, len(plans)))

    for i, plan in enumerate(plans):
        plan["budget"] = random_budgets[i]

    return plans

async def generate_ai_plans(budget: int):
    prompt = f"""
    월 예산 {budget}원을 기준으로 3개의 플랜을 만들어라.

    중요:
    - budget: 300000~1500000, 100000 단위, 서로 다름
    - 한국어
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
        model="gpt-4.1-mini",
        messages=[
            {"role": "system", "content": "너는 가계부 전문가야"},
            {"role": "user", "content": prompt},
        ],
        max_tokens=300,
        temperature=0.7,
    )

    content = response.choices[0].message.content.strip()

    print("🔥 GPT 응답:", content)

    try:
        data = json.loads(content)
    except:
        try:
            start = content.find("{")
            end = content.rfind("}") + 1
            cleaned = content[start:end]
            data = json.loads(cleaned)
        except:
            print("❌ JSON 파싱 실패")
            return {"plans": []}

    def normalize_plan(plan):
        budget = int(plan.get("budget", 0))

        UNIT = 10000

        # 👉 1. 저축 (10~30%)
        savings_units = int((budget / UNIT) * random.uniform(0.1, 0.3))
        budget_units = budget // UNIT

        remaining_units = budget_units - savings_units

        # 👉 2. 카테고리 비율
        ratios = [
            random.uniform(0.25, 0.35),  # 식비
            random.uniform(0.1, 0.2),  # 교통
            random.uniform(0.2, 0.3),  # 생활
            random.uniform(0.15, 0.3),  # 여가
        ]

        total = sum(ratios)
        ratios = [r / total for r in ratios]

        # 👉 3. 각 카테고리 unit 계산 (버림)
        food_u = int(remaining_units * ratios[0])
        transport_u = int(remaining_units * ratios[1])
        living_u = int(remaining_units * ratios[2])

        # 👉 4. 마지막 카테고리 = 남은 값 몰아주기 (핵심🔥)
        used = food_u + transport_u + living_u
        leisure_u = remaining_units - used

        return {
            "name": plan.get("name", "플랜"),
            "budget": budget,
            "savings": savings_units * UNIT,
            "food": food_u * UNIT,
            "transport": transport_u * UNIT,
            "living": living_u * UNIT,
            "leisure": leisure_u * UNIT,
            "description": plan.get("description", "")
        }

    plans = data.get("plans", [])

    # 1. budget 랜덤화
    plans = fix_budgets(plans)

    # 2. 전체 값 정리 (핵심)
    plans = [normalize_plan(p) for p in plans]

    return {"plans": plans}

    # - 반드시 모든 텍스트는 한국어로 작성해라.
    # - budget은 반드시 300000 ~ 1500000 사이
    # - 100000 단위로만 설정 (예: 300000, 400000, ...)
    # - 3개의 budget은 서로 달라야 함