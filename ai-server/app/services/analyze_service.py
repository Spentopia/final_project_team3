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

# 🔥 여기부터 추가 (절대 위 코드 건드리지 말 것)
    @staticmethod
    def generate_report(data: dict):

        prompt = f"""
        너는 매우 정교한 소비 분석 전문가다.

        절대 짧게 쓰지 말고, 반드시 구체적이고 길게 작성해라.
        최소 5~7문장 이상으로 작성해라.

        "소비 패턴 분석:" 같은 제목이나 라벨은 절대 포함하지 마라.
        자연스러운 문장으로 바로 시작해라.

        [데이터]
        총 지출: {data['totalExpense']}
        예산 사용률: {data['budgetUsage']}%
        일 평균 지출: {data['dailyAverage']}
        전월 대비 변화율: {data['expenseChangeRate']}%

        주간 소비 데이터:
        {data['weeklyData']}

        월간 소비 흐름:
        {data['monthlyData']}

        카테고리별 소비:
        {data['categoryData']}

        [분석 규칙]
        - 소비가 몰린 요일을 분석해라
        - 증가/감소 흐름을 설명해라
        - 가장 많이 소비한 카테고리를 설명해라
        - 소비 습관의 특징을 구체적으로 설명해라
        - 불필요한 소비 경향이 있다면 지적해라
        - 실천 가능한 행동 위주로 작성해라
        - 구체적인 절약 방법을 포함해라

        [출력]
        반드시 JSON만 출력:

        {{
          "good": "...",
          "warning": "...",
          "advice": "...",
          "prediction": "...",
          "pattern": "전체 소비 데이터를 기반으로 자연스럽게 서술된 분석 문단"
          "improvement": "소비 패턴을 기반으로 한 구체적인 개선 방안"
        }}
        """

        response = OpenAIClient.client.chat.completions.create(
            model="gpt-4o-mini",
            messages=[
                {"role": "system", "content": "너는 소비 분석 전문가야. 반드시 JSON으로만 답해."},
                {"role": "user", "content": prompt},
            ],
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

            return {
                "good": parsed.get("good", ""),
                "warning": parsed.get("warning", ""),
                "advice": parsed.get("advice", ""),
                "prediction": parsed.get("prediction", ""),
                "pattern": parsed.get("pattern", ""),
                "improvement": parsed.get("improvement", ""),
            }

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