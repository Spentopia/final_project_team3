from fastapi import FastAPI
from app.api.v1.endpoints import analyze, history, chat, receipt
# 맨 위쪽 import 추가
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "http://localhost:5173",
        "http://127.0.0.1:5173",
    ],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def root():
    return {"message": "서버 정상 작동"}

app.include_router(analyze.router, prefix="/api/v1/analyze")
app.include_router(history.router, prefix="/api/v1/history")
app.include_router(chat.router, prefix="/api/v1/chat")
app.include_router(receipt.router, prefix="/api/v1/receipt")
