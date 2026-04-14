import streamlit as st
from api_client import analyze_spending, get_history, chat_ai
from charts import category_chart

st.title("💸 AI 소비 분석 플랫폼")

# -------------------------------
# 소비 입력
# -------------------------------
st.subheader("📥 소비 입력")

user_input = st.text_input("예: 스타벅스 5000원")

if st.button("분석하기"):

    result = analyze_spending(user_input)

    st.subheader("📊 분석 결과")
    st.metric("소비 점수", result["score"])
    st.write("카테고리:", result["category"])
    st.write("패턴:", result["pattern"])
    st.write("위험도:", result["risk"])
    st.success("💡 조언: " + result["advice"])


# -------------------------------
# 소비 기록 + 그래프
# -------------------------------
st.subheader("📂 소비 기록")

if st.button("기록 불러오기"):

    history = get_history()

    for item in history:
        st.write(item)

    fig = category_chart(history)

    if fig:
        st.pyplot(fig)
h

# -------------------------------
# AI 챗봇
# -------------------------------
st.subheader("🤖 AI 소비 상담")

chat_input = st.text_input("질문 입력 (예: 요즘 소비 줄이는 방법 알려줘)")

if st.button("질문하기"):

    res = chat_ai(chat_input)

    st.info(res["response"])