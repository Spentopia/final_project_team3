from app.clients.openai_client import OpenAIClient


def generate_budget_plan_service(total_budget: int, savings_goal: int):
    return OpenAIClient.generate_budget_plan(
        total_budget,
        savings_goal
    )