from fastapi import APIRouter
from pydantic import BaseModel
from app.services.analyze_service import AnalyzeService

router = APIRouter()

class AnalyzeRequest(BaseModel):
    spending: str

@router.post("/")
def analyze(request: AnalyzeRequest):
    return AnalyzeService.analyze(request.spending)