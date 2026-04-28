from fastapi import APIRouter, HTTPException
from app.services.storage_service import StorageService

router = APIRouter()

@router.delete("/expenses/{expense_id}")
async def delete_expense(expense_id: str):
    success = StorageService.delete(expense_id)

    if not success:
        raise HTTPException(status_code=404, detail="데이터 없음")

    return {"message": "삭제 완료"}