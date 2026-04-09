from openai import OpenAI
from collections import defaultdict

# ✅ API 키 입력 (여기 꼭 바꿔야 함)
client = OpenAI(api_key="여기에_API_KEY")

# ✅ 테스트용 데이터
transactions = []

while True:
    date = input("날짜 입력 (예: 2026-04-01, 종료는 q): ")
    if date == "q":
        break

    amount = int(input("금액: "))
    category = input("카테고리 (식비/카페/생활): ")
    merchant = input("상호명: ")

    transactions.append({
        "date": date,
        "amount": amount,
        "category": category,
        "merchant": merchant
    })

# ✅ 카테고리별 합계
category_sum = defaultdict(int)

for t in transactions:
    category_sum[t["category"]] += t["amount"]

# ✅ 문자열 변환
data_str = "\n".join([f"{k}: {v}" for k, v in category_sum.items()])

print("📊 소비 요약")
print(data_str)

# ✅ AI 분석 요청
response = client.chat.completions.create(
    model="gpt-4o-mini",
    messages=[
        {"role": "system", "content": "너는 소비 분석 전문가야."},
        {"role": "user", "content": f"""
다음 소비 데이터를 분석해줘:

1. 문제점
2. 개선 방법
3. 절약 팁

{data_str}
"""}
    ]
)

# ✅ 결과 출력
print("\n🤖 AI 분석 결과")
print(response.choices[0].message.content)