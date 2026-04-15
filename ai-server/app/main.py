from fastapi import FastAPI
from app.api.v1.endpoints import analyze, history, chat, receipt

app = FastAPI()

@app.get("/")
def root():
    return {"message": "서버 정상 작동"}

app.include_router(analyze.router, prefix="/api/v1/analyze")
app.include_router(history.router, prefix="/api/v1/history")
app.include_router(chat.router, prefix="/api/v1/chat")
app.include_router(receipt.router, prefix="/api/v1/receipt")
