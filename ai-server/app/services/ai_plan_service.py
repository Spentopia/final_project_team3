import random

async def generate_ai_plans(budget: int):
    plans = []

    for i in range(3):
        savings = random.randint(30000, 150000)

        plans.append({
            "name": f"AI 추천 플랜 {i+1}",
            "budget": budget,
            "savings": savings,
            "description": "AI가 생성한 맞춤 예산 플랜",
            "food": random.randint(80000, 200000),
            "transport": random.randint(50000, 100000),
            "living": random.randint(80000, 200000),
            "leisure": random.randint(50000, 150000),
        })

    return plans