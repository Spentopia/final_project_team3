import json

from app.clients.openai_client import OpenAIClient

MIN_BUDGET = 0
MAX_BUDGET = 100000000
UNIT = 10000

PLAN_PROFILES = [
    {
        "name": "기본 플랜",
        "description": "저축을 우선 확보하고 필수 지출을 중심으로 운영하는 절약형 플랜",
        "savings_ratio": (0.28, 0.38),
        "category_bias": {"food": 0.24, "transport": 0.14, "living": 0.37, "leisure": 0.10},
    },
    {
        "name": "중간 플랜",
        "description": "저축과 생활 만족도를 균형 있게 맞춘 플랜",
        "savings_ratio": (0.20, 0.28),
        "category_bias": {"food": 0.26, "transport": 0.14, "living": 0.33, "leisure": 0.15},
    },
    {
        "name": "여유 플랜",
        "description": "취미와 여가를 조금 더 넉넉히 반영한 플랜",
        "savings_ratio": (0.14, 0.22),
        "category_bias": {"food": 0.27, "transport": 0.14, "living": 0.31, "leisure": 0.20},
    },
]

FIXED_EXPENSE_CATEGORY_MAP = {
    "food": "food",
    "meal": "food",
    "dining": "food",
    "transport": "transport",
    "traffic": "transport",
    "living": "living",
    "utility": "living",
    "rent": "living",
    "housing": "living",
    "leisure": "leisure",
    "hobby": "leisure",
    "entertainment": "leisure",
}


def clamp_budget(value):
    return max(MIN_BUDGET, min(MAX_BUDGET, int(value or MIN_BUDGET)))


def round_to_unit(value):
    return max(0, int(round(max(0, value) / UNIT) * UNIT))


def normalize_category_key(raw_category):
    if not raw_category:
        return "living"
    category = str(raw_category).strip().lower()
    return FIXED_EXPENSE_CATEGORY_MAP.get(category, "living")


def build_fixed_expense_summary(fixed_expenses):
    category_totals = {
        "food": 0,
        "transport": 0,
        "living": 0,
        "leisure": 0,
    }

    total = 0
    normalized_items = []
    for item in fixed_expenses or []:
        amount = max(0, int(item.get("amount") or 0))
        category = normalize_category_key(item.get("category"))
        name = str(item.get("name") or "").strip() or "고정 지출"
        total += amount
        category_totals[category] += amount
        normalized_items.append({
            "name": name,
            "amount": amount,
            "category": category,
        })

    return {
        "total": total,
        "category_totals": category_totals,
        "items": normalized_items,
    }


def build_prompt(payload, fixed_summary, total_budget):
    savings_goal = max(0, int(payload.get("savings_goal") or 0))

    return f"""
사용자의 월 예산 데이터를 바탕으로 실제로 적용 가능한 3개의 소비 플랜을 추천해줘.

사용자 입력:
- 현재 입력 월 예산: {clamp_budget(payload.get("total_budget"))}원
- 희망 저축액: {savings_goal}원
- 연도: {payload.get("year")}
- 월: {payload.get("month")}
- 고정 지출 총합: {fixed_summary["total"]}원
- 고정 지출 항목: {json.dumps(fixed_summary["items"], ensure_ascii=False)}

규칙:
- 3개 플랜의 budget은 모두 사용자가 입력한 월 예산과 같은 {total_budget}원으로 맞춘다.
- 1번 플랜은 저축 우선, 2번 플랜은 균형형, 3번 플랜은 여유형이다.
- 플랜 차이는 총예산이 아니라 저축/식비/교통비/생활비/여가취미 배분 방식에서만 드러나야 한다.
- 각 플랜의 savings + food + transport + living + leisure 합계는 정확히 budget과 같아야 한다.
- 사용자의 고정 지출 총합보다 생활비/교통비/식비 합산이 비현실적으로 작으면 안 된다.
- savings는 가능하면 사용자의 희망 저축액을 반영하되, 기본 플랜 > 중간 플랜 > 여유 플랜 순으로 작아져야 한다.
- savings는 너무 보수적으로 잡지 말고, 각 플랜이 월 예산 대비 대략 기본 28~38%, 중간 20~28%, 여유 14~22% 범위를 우선 기준으로 삼는다.
- 모든 금액은 10000원 단위 정수로 맞춘다.
- 사용자가 입력한 월 예산이 크더라도 임의로 150만원 같은 상한으로 줄이지 않는다.
- 한국어로 작성한다.
- 반드시 JSON만 반환한다.

반환 형식:
{{
  "plans": [
    {{
      "name": "기본 플랜",
      "budget": {total_budget},
      "savings": 0,
      "food": 0,
      "transport": 0,
      "living": 0,
      "leisure": 0,
      "description": "설명"
    }},
    {{
      "name": "중간 플랜",
      "budget": {total_budget},
      "savings": 0,
      "food": 0,
      "transport": 0,
      "living": 0,
      "leisure": 0,
      "description": "설명"
    }},
    {{
      "name": "여유 플랜",
      "budget": {total_budget},
      "savings": 0,
      "food": 0,
      "transport": 0,
      "living": 0,
      "leisure": 0,
      "description": "설명"
    }}
  ]
}}
"""


def fallback_plan(profile, total_budget, savings_goal, fixed_summary, plan_index):
    budget = clamp_budget(total_budget)
    fixed_by_category = fixed_summary["category_totals"]
    fixed_total = fixed_summary["total"]

    min_savings, max_savings = calculate_savings_bounds(profile, budget, savings_goal, plan_index)
    savings = round_to_unit(min_savings)

    remaining = max(0, budget - savings)
    baseline_needs = min(remaining, fixed_total)

    category_allocations = {key: 0 for key in fixed_by_category}
    if fixed_total > 0 and baseline_needs > 0:
        for key, amount in fixed_by_category.items():
            proportional = baseline_needs * amount / fixed_total if fixed_total else 0
            category_allocations[key] = round_to_unit(proportional)

    remaining_after_fixed = max(0, remaining - sum(category_allocations.values()))
    bias_total = sum(profile["category_bias"].values()) or 1
    ordered_keys = ["food", "transport", "living", "leisure"]
    for key in ordered_keys[:-1]:
        ratio = profile["category_bias"][key] / bias_total
        extra = round_to_unit(remaining_after_fixed * ratio)
        category_allocations[key] += extra
    category_allocations["leisure"] += max(
        0,
        remaining - (
            category_allocations["food"]
            + category_allocations["transport"]
            + category_allocations["living"]
            + category_allocations["leisure"]
        ),
    )

    spent = (
        category_allocations["food"]
        + category_allocations["transport"]
        + category_allocations["living"]
        + category_allocations["leisure"]
        + savings
    )
    diff = budget - spent
    category_allocations["living"] += diff

    return {
        "name": profile["name"],
        "budget": budget,
        "savings": savings,
        "food": category_allocations["food"],
        "transport": category_allocations["transport"],
        "living": category_allocations["living"],
        "leisure": category_allocations["leisure"],
        "description": profile["description"],
    }


def calculate_savings_bounds(profile, budget, savings_goal, plan_index):
    min_ratio, max_ratio = profile["savings_ratio"]
    ratio_floor = int(budget * min_ratio)
    ratio_ceiling = int(budget * max_ratio)

    if savings_goal > 0:
        if plan_index == 0:
            desired = int(savings_goal * 1.2)
        elif plan_index == 1:
            desired = int(savings_goal * 1.05)
        else:
            desired = int(savings_goal * 0.85)
    else:
        desired = ratio_floor

    min_savings = max(ratio_floor, desired)
    max_savings = max(min_savings, ratio_ceiling)
    return min_savings, max_savings


def normalize_plan(raw_plan, profile, total_budget, savings_goal, fixed_summary, plan_index):
    fallback = fallback_plan(profile, total_budget, savings_goal, fixed_summary, plan_index)
    budget = clamp_budget(total_budget)
    min_savings, max_savings = calculate_savings_bounds(profile, budget, savings_goal, plan_index)
    raw_savings = round_to_unit(raw_plan.get("savings") or fallback["savings"])

    values = {
        "savings": max(round_to_unit(min_savings), min(raw_savings, round_to_unit(max_savings))),
        "food": round_to_unit(raw_plan.get("food") or fallback["food"]),
        "transport": round_to_unit(raw_plan.get("transport") or fallback["transport"]),
        "living": round_to_unit(raw_plan.get("living") or fallback["living"]),
        "leisure": round_to_unit(raw_plan.get("leisure") or fallback["leisure"]),
    }

    fixed_total = fixed_summary["total"]
    min_required = min(budget, fixed_total)
    current_spend = values["food"] + values["transport"] + values["living"] + values["leisure"]
    if current_spend < min_required:
        values["living"] += min_required - current_spend

    total = sum(values.values())
    if total != budget:
        values["living"] += budget - total

    if values["living"] < 0:
        deficit = -values["living"]
        values["living"] = 0
        for key in ["leisure", "food", "transport", "savings"]:
            cut = min(values[key], deficit)
            values[key] -= cut
            deficit -= cut
            if deficit == 0:
                break
        values["living"] += budget - sum(values.values())

    return {
        "name": profile["name"],
        "budget": budget,
        "savings": values["savings"],
        "food": values["food"],
        "transport": values["transport"],
        "living": values["living"],
        "leisure": values["leisure"],
        "description": str(raw_plan.get("description") or profile["description"]).strip() or profile["description"],
    }


async def generate_ai_plans(payload: dict):
    total_budget = clamp_budget(payload.get("total_budget"))
    savings_goal = max(0, int(payload.get("savings_goal") or 0))
    fixed_summary = build_fixed_expense_summary(payload.get("fixed_expenses") or [])

    prompt = build_prompt(payload, fixed_summary, total_budget)
    data = None
    last_error = None

    for model in ["gpt-4o-mini", "gpt-4.1-mini"]:
        try:
            response = OpenAIClient.client.chat.completions.create(
                model=model,
                response_format={"type": "json_object"},
                messages=[
                    {"role": "system", "content": "너는 가계부 예산 추천 전문가다. 사용자의 실제 예산과 고정 지출을 기반으로 현실적인 월간 배분 플랜을 JSON으로만 반환한다."},
                    {"role": "user", "content": prompt},
                ],
                max_tokens=1200,
                temperature=0.4,
            )
            content = response.choices[0].message.content
            if not content:
                continue
            data = json.loads(content)
            break
        except Exception as error:
            last_error = error
            continue

    if data is None:
        print("❌ AI 예산 플랜 생성 실패, fallback 사용:", last_error)
        data = {"plans": []}

    raw_plans = data.get("plans", []) if isinstance(data, dict) else []
    source_plans = (raw_plans + [{}, {}, {}])[:3]
    plans = [
        normalize_plan(
            source_plans[index],
            PLAN_PROFILES[index],
            total_budget,
            savings_goal,
            fixed_summary,
            index,
        )
        for index in range(3)
    ]

    return {"plans": plans}
