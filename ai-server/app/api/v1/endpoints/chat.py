from fastapi import APIRouter
from pydantic import BaseModel
from app.services.chat_service import ChatService

router = APIRouter()

class ChatRequest(BaseModel):
    message: str

@router.post("/")
def chat(request: ChatRequest):
    if request.message == "예산 추천 생성":
        return ChatService.get_ai_budget_plan()

    return ChatService.chat(request.message)

# ✅ 여기 추가
@router.post("/ai-plan")
def ai_plan():
    return ChatService.get_ai_budget_plan()