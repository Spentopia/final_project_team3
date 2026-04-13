from app.clients.openai_client import OpenAIClient
from app.services.storage_service import StorageService
from datetime import datetime

class AnalyzeService:

    @staticmethod
    def analyze(spending: str):

        result = OpenAIClient.analyze(spending)

        # 저장 (여기만 나중에 Supabase로 교체됨)
        StorageService.save({
            "spending": spending,
            "score": result.get("score", 0),
            "category": result.get("category", ""),
            "pattern": result.get("pattern", ""),
            "risk": result.get("risk", ""),
            "advice": result.get("advice", ""),
            "date": datetime.now().strftime("%Y-%m-%d")
        })

        return result