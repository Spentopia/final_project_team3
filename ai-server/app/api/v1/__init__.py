from fastapi import APIRouter
from app.api.v1.endpoints import chat

api_router = APIRouter()

# ✅ chat.py 연결
api_router.include_router(chat.router, prefix="/api")