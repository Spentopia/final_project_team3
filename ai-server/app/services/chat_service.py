from app.clients.openai_client import OpenAIClient

class ChatService:

    @staticmethod
    def chat(message: str):
        return OpenAIClient.chat(message)

    # ✅ AI 가계부 플랜 추가
    @staticmethod
    def get_ai_budget_plan():
        # 테스트용 (나중에 Rust에서 받아도 됨)
        total_budget = 1000000
        savings_goal = 200000

        return OpenAIClient.generate_budget_plan(
            total_budget,
            savings_goal
        )