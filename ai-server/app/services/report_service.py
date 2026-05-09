from app.clients.supabase_client import supabase


class ReportService:

    @staticmethod
    def save_report(data: dict):

        supabase.table("ai_reports").insert({
            "total_expense": data["totalExpense"],
            "budget": data["budget"],
            "budget_usage": data["budgetUsage"],

            "top_category": data["topCategory"],
            "top_category_percent": data["topCategoryPercent"],

            "daily_average": data["dailyAverage"],
            "expense_change_rate": data["expenseChangeRate"],

            "good": data["good"],
            "warning": data["warning"],
            "advice": data["advice"],
            "prediction": data["prediction"],

            "pattern": data["pattern"],
            "improvement": data["improvement"],
        }).execute()