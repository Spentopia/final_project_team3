from fastapi import APIRouter
from app.services.ai_plan_service import generate_ai_plans

router = APIRouter()

@router.post("/budget-plan")
async def create_ai_plans(data: dict):
    budget = data.get("total_budget", 500000)

    result = await generate_ai_plans(budget)

    # 🔥 Rust 맞춰주기
    return result