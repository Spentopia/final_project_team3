from fastapi import APIRouter
from pydantic import BaseModel
from typing import List, Literal, Optional

from app.services.analyze_service import AnalyzeService

router = APIRouter()


class Transaction(BaseModel):
    date: str
    amount: float
    category: str
    type: str


class CategoryData(BaseModel):
    key: Optional[str] = None
    name: str
    amount: float
    value: float
    color: Optional[str] = None


class GenerateReportRequest(BaseModel):
    report_type: Literal["weekly", "monthly"]
    start_date: str
    end_date: str

    transactions: List[Transaction]

    total_expense: float
    budget: float

    top_category: str
    top_category_percent: float

    daily_average: float
    expense_change_rate: float
    budget_usage: float

    category_data: List[CategoryData]


@router.post("/api/v1/analyze/report")
async def analyze_report(request: GenerateReportRequest):
    result = AnalyzeService.generate_report(request.model_dump())

    return result
