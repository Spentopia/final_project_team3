from fastapi import APIRouter
from app.services.ai_plan_service import generate_ai_plans

router = APIRouter()

@router.post("/ai-plans")
async def create_ai_plans(data: dict):
    budget = data.get("budget", 500000)

    result = await generate_ai_plans(budget)

    return result