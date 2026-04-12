from fastapi import FastAPI
from app.api.v1.endpoints.analyze import router as analyze_router

app = FastAPI()

app.include_router(analyze_router, prefix="/api/v1/analyze")


@app.get("/")
def root():
    return {"message": "서버 정상 작동"}