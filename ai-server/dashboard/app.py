import streamlit as st
import matplotlib.pyplot as plt
from api_client import analyze_spending

st.set_page_config(page_title="AI 소비 분석", layout="centered")

st.title("💰 AI 소비 패턴 분석")

user_input = st.text_area(
    "소비 내역 입력",
    placeholder="예: 스타벅스 5000원, 택시 12000원"
)

if st.button("분석하기"):

    if not user_input.strip():
        st.warning("소비 데이터를 입력해주세요")
    else:
        with st.spinner("AI 분석 중..."):
            result = analyze_spending(user_input)

        st.success("분석 완료")

        st.subheader("📊 소비 점수")
        st.metric("점수", f"{result['score']} / 100")

        st.subheader("📌 분석 요약")
        st.write("패턴:", result["pattern"])
        st.write("위험도:", result["risk_level"])
        st.write("월 예상 지출:", result["monthly_estimate"])
        st.write("절약 가능 금액:", result["saving_possible"])

        st.subheader("📈 소비 카테고리")

        if result["category_amount"]:
            labels = list(result["category_amount"].keys())
            sizes = list(result["category_amount"].values())

            fig, ax = plt.subplots()
            ax.pie(sizes, labels=labels, autopct="%1.1f%%")
            st.pyplot(fig)
        else:
            st.info("데이터 없음")

        st.subheader("🧠 AI 리포트")
        st.info(result["report"])