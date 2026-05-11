from fastapi import APIRouter

from app.api.v1.endpoints import (
    chat,
    analyze,
    ai_plan,
    receipt,
    history,
)

api_router = APIRouter()

api_router.include_router(chat.router, prefix="/chat", tags=["chat"])
api_router.include_router(analyze.router, prefix="/analyze", tags=["analyze"])
api_router.include_router(ai_plan.router, prefix="/budget-plan", tags=["budget"])
api_router.include_router(receipt.router, prefix="/receipt", tags=["receipt"])
api_router.include_router(history.router, prefix="/history", tags=["history"])