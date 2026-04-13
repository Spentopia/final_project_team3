import requests

BASE_URL = "http://127.0.0.1:8000/api/v1/analyze"


def analyze_spending(data):
    try:
        response = requests.post(
            BASE_URL,
            json={"spending": data}
        )
        return response.json()

    except Exception as e:
        return {
            "score": 0,
            "pattern": "서버 연결 실패",
            "risk_level": "오류",
            "category_amount": {},
            "monthly_estimate": "-",
            "saving_possible": "-",
            "report": str(e)
        }