import streamlit as st

from modules.data import load_data, save_data
from modules.auth import auth_page, logout
from modules.charts import *
from modules.ui import *
from modules.ai import analyze
from modules.risk import risk_score
from modules.report import monthly_report


st.set_page_config(

layout="wide"

)


load_css()


if "login" not in st.session_state:

    st.session_state["login"]=False


if not st.session_state["login"]:

    auth_page()

    st.stop()


logout()


df = load_data()


user = st.session_state["user"]


df = df[df["user"]==user]


st.title("AI 가계부")


budget = st.sidebar.number_input(

"예산",

0

)


income = df[df["type"]=="수입"]["amount"].sum()

expense = df[df["type"]=="지출"]["amount"].sum()


c1,c2 = st.columns(2)


with c1:

    card("수입",income)


with c2:

    card("지출",expense)


st.divider()


with st.form("input"):

    date = st.date_input("날짜")

    amount = st.number_input("금액",0)

    type = st.radio("구분",["수입","지출"])

    category = st.text_input("카테고리")

    merchant = st.text_input("상호")


    if st.form_submit_button("저장"):

        save_data({

            "date":date,

            "amount":amount,

            "type":type,

            "category":category,

            "merchant":merchant,

            "user":user

        })


st.dataframe(df)


st.subheader("차트")


st.pyplot(

    pie_chart(df)

)


st.plotly_chart(

    monthly_chart(df)

)


st.plotly_chart(

    trend_chart(df)

)


st.subheader("소비 위험도")


score = risk_score(df)


st.metric(

"위험 점수",

score

)


st.subheader("월간 리포트")


st.text(

    monthly_report(df)

)


if st.button("AI 분석"):

    st.write(

        analyze(df)

    )