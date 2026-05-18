from fastapi import APIRouter
from pydantic import BaseModel
from app.services.chat_service import ChatService

router = APIRouter()

class ChatRequest(BaseModel):
    message: str

@router.post("")
@router.post("/")
def chat(request: ChatRequest):
    return ChatService.chat(request.message)
