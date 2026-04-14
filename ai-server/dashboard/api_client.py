import requests

BASE_URL = "http://127.0.0.1:8000/api/v1"

def analyze_spending(data):
    return requests.post(
        f"{BASE_URL}/analyze/",
        json={"spending": data}
    ).json()

def get_history():
    return requests.get(f"{BASE_URL}/history/").json()

def chat_ai(message):
    return requests.post(
        f"{BASE_URL}/chat/",
        json={"message": message}
    ).json()