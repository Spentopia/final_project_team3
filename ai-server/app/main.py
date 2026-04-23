from fastapi import FastAPI
from app.api.v1.endpoints import analyze, history, chat, receipt
from fastapi.middleware.cors import CORSMiddleware

# FastAPI 앱 생성.
# 이 파일은 AI 서버의 진입점이라서
# "어떤 라우터를 어디에 붙였는지"를 한눈에 보기 좋게 유지하는 게 중요하다.
app = FastAPI()

# 프론트 개발 서버에서 AI 서버를 직접 호출할 수 있도록 CORS를 허용한다.
# 배포 환경에서는 실제 프론트 도메인 기준으로 더 좁게 관리하는 편이 안전하다.
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
    # 헬스체크에 가까운 가장 단순한 엔드포인트.
    # 서버가 떴는지 빠르게 확인할 때 사용한다.
    return {"message": "서버 정상 작동"}

# 기능별 라우터를 /api/v1 하위에 연결한다.
# 실제 비즈니스 로직은 각 endpoint/service 파일로 내려보낸다.
app.include_router(analyze.router, prefix="/api/v1/analyze")
app.include_router(history.router, prefix="/api/v1/history")
app.include_router(chat.router, prefix="/api/v1/chat")
app.include_router(receipt.router, prefix="/api/v1/receipt")