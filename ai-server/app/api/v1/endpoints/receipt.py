from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.services.receipt_ocr_service import ReceiptOcrService

router = APIRouter()


@router.post("/ocr")
async def verify_receipt(
    image: UploadFile = File(...),
    expected_date: str = Form(...),
    expected_amount: int = Form(...),
):
    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드할 수 있습니다.")

    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 이미지 파일입니다.")

    return ReceiptOcrService.verify(
        image_bytes=image_bytes,
        mime_type=image.content_type,
        expected_date=expected_date,
        expected_amount=expected_amount,
    )
