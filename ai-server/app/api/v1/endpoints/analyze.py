from fastapi import APIRouter, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from app.services.analyze_service import AnalyzeService
from typing import List, Literal, Optional

router = APIRouter()


class AnalyzeRequest(BaseModel):
    spending: str


@router.post("/")
def analyze(request: AnalyzeRequest):
    return AnalyzeService.analyze(request.spending)


class CategoryData(BaseModel):
    key: Optional[str] = None
    name: str
    amount: float
    value: float
    color: Optional[str] = None


class Transaction(BaseModel):
    date: str
    amount: float
    category: str
    type: str


class GenerateReportRequest(BaseModel):
    user_id: Optional[str] = None
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


@router.post("/report")
async def analyze_report(req: GenerateReportRequest):
    result = AnalyzeService.generate_report(req.model_dump())

    return result