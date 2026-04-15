from fastapi import APIRouter
from app.services.storage_service import StorageService

router = APIRouter()

@router.get("/")
def get_history():
    return StorageService.load()
print("import test")