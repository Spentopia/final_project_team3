from fastapi import APIRouter
from app.services.analyze_service import AnalyzeService

router = APIRouter()


@router.post("")
def analyze(data: dict):
    spending = data.get("spending", "")
    return AnalyzeService.analyze(spending)