from app.clients.openai_client import OpenAIClient
from app.services.storage_service import StorageService
from datetime import datetime
# from app.services.report_service import ReportService

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

# 🔥 여기부터 추가 (절대 위 코드 건드리지 말 것)
    @staticmethod
    def generate_report(data: dict):
        report_type = data.get("report_type", "monthly")
        report_label = "주간" if report_type == "weekly" else "월간"

        prompt = f"""
        당신은 전문 금융 소비 분석 AI입니다.

        사용자의 소비 데이터를 기반으로
        전문적인 {report_label} 소비 리포트를 작성하세요.

        조건:
        - report_type이 weekly이면 주간 소비 흐름과 단기 개선 행동을 중심으로 분석
        - report_type이 monthly이면 월간 소비 구조와 다음 달 예측을 중심으로 분석
        - start_date부터 end_date까지의 기간만 기준으로 분석
        - 각 항목 최소 5~7문장
        - 실제 금융 리포트처럼 자연스럽게 작성
        - 소비 금액과 소비 패턴 반드시 언급
        - 예산 대비 사용률 분석 포함
        - 가장 많이 소비한 카테고리 분석 포함
        - 절약 가능한 항목 제안 포함
        - 너무 짧은 답변 금지
        - 반드시 한국어로 작성

        반드시 아래 JSON 형식으로 반환:

        {{
          "good": "...",
          "warning": "...",
          "advice": "...",
          "prediction": "...",
          "pattern": "...",
          "improvement": "..."
        }}

        소비 데이터:
        {data}
        """

        response = OpenAIClient.client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {
                    "role": "system",
                    "content": "너는 소비 분석 전문가다."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            max_tokens=1000,
            response_format={"type": "json_object"},
        )

        content = response.choices[0].message.content.strip()

        import json
        import re

        try:
            # ```json 제거
            content = re.sub(r"```json", "", content)
            content = re.sub(r"```", "", content)

            # JSON만 추출
            match = re.search(r"\{.*\}", content, re.DOTALL)
            if match:
                content = match.group()

            parsed = json.loads(content)

            result = {
                "good": parsed.get("good", ""),
                "warning": parsed.get("warning", ""),
                "advice": parsed.get("advice", ""),
                "prediction": parsed.get("prediction", ""),
                "pattern": parsed.get("pattern", ""),
                "improvement": parsed.get("improvement", ""),
            }

            save_data = {
                **data,
                **result,
            }

            # ReportService.save_report(save_data)

            return result

        except Exception as e:
            print("❌ JSON 파싱 실패:", e)
            print("👉 GPT 응답:", content)

            return {
                "good": "분석 결과를 불러오지 못했어요",
                "warning": "데이터 해석 중 문제가 발생했어요",
                "advice": "잠시 후 다시 시도해주세요",
                "prediction": "",
                "pattern": "소비 패턴 분석을 생성하지 못했습니다",
                "improvement": "개선 방안을 생성하지 못했습니다"
            }
