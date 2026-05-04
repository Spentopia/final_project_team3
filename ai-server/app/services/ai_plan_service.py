import json
import random
from app.clients.openai_client import OpenAIClient

MIN_BUDGET = 300000
MAX_BUDGET = 1500000
BUDGET_STEP = 100000

PLAN_PROFILES = [
    {
        "name": "기본 플랜",
        "description": "현재 생활 패턴을 크게 해치지 않으면서 지출과 저축의 균형을 맞춘 플랜",
        "savings_ratio": (0.15, 0.22),
    },
    {
        "name": "중간 플랜",
        "description": "조금 더 여유 있는 소비를 반영해 일상 편의와 만족도를 높인 플랜",
        "savings_ratio": (0.10, 0.18),
    },
    {
        "name": "여유 플랜",
        "description": "여유로운 생활을 위해 취미와 생활비 비중을 넉넉하게 잡은 플랜",
        "savings_ratio": (0.05, 0.12),
    },
]


def round_to_step(value):
    value = max(MIN_BUDGET, min(MAX_BUDGET, int(value)))
    return max(MIN_BUDGET, min(MAX_BUDGET, round(value / BUDGET_STEP) * BUDGET_STEP))


def build_budget_targets(base_budget):
    base = round_to_step(base_budget)
    candidates = [
        round_to_step(base - BUDGET_STEP),
        base,
        round_to_step(base + BUDGET_STEP),
    ]

    unique_targets = []
    for candidate in candidates:
        adjusted = candidate
        while adjusted in unique_targets and adjusted < MAX_BUDGET:
            adjusted += BUDGET_STEP
        while adjusted in unique_targets and adjusted > MIN_BUDGET:
            adjusted -= BUDGET_STEP
        if adjusted not in unique_targets:
            unique_targets.append(adjusted)

    fallback = MIN_BUDGET
    while len(unique_targets) < 3:
        if fallback not in unique_targets:
            unique_targets.append(fallback)
        fallback += BUDGET_STEP

    return sorted(unique_targets[:3])

async def generate_ai_plans(budget: int):
    prompt = f"""
    월 예산 {budget}원을 기준으로 3개의 플랜을 만들어라.

    중요:
    - budget: 300000~1500000, 100000 단위, 서로 다름
    - 1번 플랜은 기본 플랜, 2번 플랜은 중간 플랜, 3번 플랜은 여유 플랜
    - 기본 플랜 <= 중간 플랜 <= 여유 플랜 순서로 월 예산이 커져야 함
    - 설명은 플랜 이름과 소비 성향이 일치해야 함
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

    models = [
        "gpt-4o-mini",
        "gpt-4.1-mini",
    ]

    last_error = None

    for model in models:
        try:
            response = OpenAIClient.client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": "너는 가계부 전문가야"},
                    {"role": "user", "content": prompt},
                ],
                max_tokens=800,
                temperature=0.7,
            )

            content = response.choices[0].message.content

            if content is None:
                print("❌ GPT 응답이 비어있음")
                return {"plans": []}

            content = content.strip()

            print("🔥 사용 모델:", model)
            print("🔥 GPT 응답:", content)

            try:
                data = json.loads(content)
            except Exception:
                try:
                    start = content.find("{")
                    end = content.rfind("}") + 1
                    cleaned = content[start:end]
                    data = json.loads(cleaned)
                except Exception:
                    print("❌ JSON 파싱 실패")
                    return {"plans": []}

            # 여기까지 왔으면 성공한 거라서 다른 모델 시도 안 함
            break

        except Exception as e:
            print(f"❌ 모델 실패: {model}")
            print(e)
            last_error = e
            continue

    if data is None:
        print("❌ 모든 모델 실패:", last_error)
        return {"plans": []}

    def normalize_plan(plan, profile, target_budget):
        budget = round_to_step(target_budget)

        UNIT = 10000

        # 플랜 성격에 맞는 저축률을 고정 범위 내에서만 조정한다.
        min_ratio, max_ratio = profile["savings_ratio"]
        savings_units = int((budget / UNIT) * random.uniform(min_ratio, max_ratio))
        budget_units = budget // UNIT

        remaining_units = budget_units - savings_units

        if profile["name"] == "기본 플랜":
            ratios = [0.30, 0.14, 0.31, 0.25]
        elif profile["name"] == "중간 플랜":
            ratios = [0.29, 0.14, 0.30, 0.27]
        else:
            ratios = [0.27, 0.13, 0.31, 0.29]

        total = sum(ratios)
        ratios = [r / total for r in ratios]

        food_u = int(remaining_units * ratios[0])
        transport_u = int(remaining_units * ratios[1])
        living_u = int(remaining_units * ratios[2])
        used = food_u + transport_u + living_u
        leisure_u = remaining_units - used

        return {
            "name": profile["name"],
            "budget": budget,
            "savings": savings_units * UNIT,
            "food": food_u * UNIT,
            "transport": transport_u * UNIT,
            "living": living_u * UNIT,
            "leisure": leisure_u * UNIT,
            "description": profile["description"],
        }

    plans = data.get("plans", [])
    target_budgets = build_budget_targets(budget)
    source_plans = (plans + [{}, {}, {}])[:3]
    plans = [
        normalize_plan(source_plans[index], PLAN_PROFILES[index], target_budgets[index])
        for index in range(3)
    ]

    first_plan = plans[0] if plans else {}

    categories = [
        {"category": "food", "allocated_amount": first_plan.get("food", 0)},
        {"category": "transport", "allocated_amount": first_plan.get("transport", 0)},
        {"category": "living", "allocated_amount": first_plan.get("living", 0)},
        {"category": "leisure", "allocated_amount": first_plan.get("leisure", 0)},
        {"category": "savings", "allocated_amount": first_plan.get("savings", 0)},
    ]

    return {
        "plans": plans
    }

    # - 반드시 모든 텍스트는 한국어로 작성해라.
    # - budget은 반드시 300000 ~ 1500000 사이
    # - 100000 단위로만 설정 (예: 300000, 400000, ...)
    # - 3개의 budget은 서로 달라야 함
