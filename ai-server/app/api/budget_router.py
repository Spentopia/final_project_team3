from fastapi import APIRouter
from pydantic import BaseModel
from app.db.supabase import supabase

router = APIRouter()

class BudgetRequest(BaseModel):
    year: int
    month: int
    total_budget: int

@router.post("/")
def save_budget(req: BudgetRequest):
    try:
        data = {
            "year": req.year,
            "month": req.month,
            "total_budget": req.total_budget,
        }

        result = supabase.table("budgets").insert(data).execute()

        return {"success": True, "data": result.data}

    except Exception as e:
        return {"success": False, "error": str(e)}