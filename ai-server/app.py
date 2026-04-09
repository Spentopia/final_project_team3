import streamlit as st
from modules.auth import auth_page, logout


if "login" not in st.session_state:
    st.session_state.login = False


if not st.session_state.login:

    auth_page()

else:

    st.title("Spentopia")

    st.success("로그인 성공")

    logout()

    st.divider()

    st.subheader("대시보드 예시")

    col1, col2 = st.columns(2)

    with col1:
        st.metric("이번달 소비", "₩320,000")

    with col2:
        st.metric("예산 사용률", "78%")

    st.line_chart([10,20,15,40,25])