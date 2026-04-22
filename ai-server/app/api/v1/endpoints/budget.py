from fastapi import APIRouter
from pydantic import BaseModel
from app.services.ai_service import generate_budget_plan_service

router = APIRouter()


class BudgetRequest(BaseModel):
    total_budget: int
    savings_goal: int


@router.post("/budget-plan")
def generate_budget_plan(req: BudgetRequest):
    return generate_budget_plan_service(
        req.total_budget,
        req.savings_goal
    )