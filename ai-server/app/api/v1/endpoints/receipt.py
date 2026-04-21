from fastapi import APIRouter, File, Form, HTTPException, UploadFile

from app.services.receipt_ocr_service import ReceiptOcrService

router = APIRouter()


@router.post("/ocr")
async def verify_receipt(
    image: UploadFile = File(...),
    expected_date: str = Form(...),
    expected_amount: int = Form(...),
):
    # FastAPI가 multipart/form-data에서
    # 파일은 File(...), 텍스트 값은 Form(...)으로 분리해서 받는다.
    # 프론트에서 FormData로 보내는 구조와 정확히 맞아야 한다.

    # OCR 모델 호출 전에 가장 기본적인 입력 검증을 먼저 처리한다.
    # 여기서 걸러주면 서비스/모델 호출까지 가지 않아서 디버깅이 단순해진다.
    if not image.content_type or not image.content_type.startswith("image/"):
        raise HTTPException(status_code=400, detail="이미지 파일만 업로드할 수 있습니다.")

    image_bytes = await image.read()
    if not image_bytes:
        raise HTTPException(status_code=400, detail="빈 이미지 파일입니다.")

    # 엔드포인트는 HTTP 입출력만 담당하고,
    # 실제 OCR 추출/비교 로직은 서비스 계층으로 넘긴다.
    return ReceiptOcrService.verify(
        image_bytes=image_bytes,
        mime_type=image.content_type,
        expected_date=expected_date,
        expected_amount=expected_amount,
    )
