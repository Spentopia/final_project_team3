import json
from app.clients.openai_client import analyze_with_ai


class AnalyzeService:

    @staticmethod
    def analyze(spending: str):

        try:
            ai_result = analyze_with_ai(spending)

            # 🔥 문자열 → JSON 변환
            result = json.loads(ai_result)

            return result

        except Exception as e:
            return {
                "score": 0,
                "pattern": "분석 실패",
                "risk_level": "오류",
                "category_amount": {},
                "monthly_estimate": "0원",
                "saving_possible": "0원",
                "report": f"에러 발생: {str(e)}"
            }