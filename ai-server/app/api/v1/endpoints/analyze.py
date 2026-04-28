from fastapi import APIRouter
from pydantic import BaseModel
from app.services.analyze_service import AnalyzeService
from typing import List

router = APIRouter()

class AnalyzeRequest(BaseModel):
    spending: str

@router.post("/")
def analyze(request: AnalyzeRequest):
    return AnalyzeService.analyze(request.spending)


# 🔥 여기부터 추가

class WeeklyData(BaseModel):
    day: str
    amount: float

class MonthlyData(BaseModel):
    month: str
    amount: float

class CategoryData(BaseModel):
    name: str
    amount: float
    value: float
    key: str | None = None


class Transaction(BaseModel):
    date: str
    amount: float
    category: str
    type: str  # expense / income


class AnalyzeReportRequest(BaseModel):
    transactions: List[Transaction]
    totalExpense: float
    budget: float
    topCategory: str
    topCategoryPercent: float

    dailyAverage: float
    expenseChangeRate: float
    budgetUsage: float

    weeklyData: List[WeeklyData]
    monthlyData: List[MonthlyData]
    categoryData: List[CategoryData]


@router.post("/report")
def analyze_report(request: AnalyzeReportRequest):
    return AnalyzeService.generate_report(request.model_dump())