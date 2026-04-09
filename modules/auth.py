import streamlit as st


def auth_page():

    st.set_page_config(layout="centered")

    st.markdown("""
    <style>

    @import url('https://fonts.googleapis.com/css2?family=Pretendard:wght@400;500;600;700&display=swap');

    html, body, [class*="css"]  {
        font-family: 'Pretendard', sans-serif;
    }

    .stApp {
        background-color: #f5f6f8;
    }

    .login-card {
        max-width: 420px;
        margin: 90px auto;
        padding: 45px 35px;
        background: white;
        border-radius: 22px;
        box-shadow: 0 15px 40px rgba(0,0,0,0.08);
    }

    .logo-box {
        width:72px;
        height:72px;
        border-radius:18px;
        background:#eef2ff;
        display:flex;
        align-items:center;
        justify-content:center;
        margin:auto;
        font-size:34px;
    }

    .title {
        text-align:center;
        font-size:34px;
        font-weight:700;
        margin-top:18px;
        color:#111827;
    }

    .subtitle {
        text-align:center;
        color:#6b7280;
        margin-bottom:28px;
        font-size:14px;
    }

    .stTextInput input {
        border-radius:12px;
        padding:12px;
        border:1px solid #e5e7eb;
    }

    .stTextInput input:focus {
        border:1px solid #6366f1;
        box-shadow:none;
    }

    .stButton>button {
        width:100%;
        background:#111827;
        color:white;
        padding:13px;
        border-radius:12px;
        font-weight:600;
        border:none;
        margin-top:8px;
    }

    .stButton>button:hover {
        background:black;
    }

    .divider {
        text-align:center;
        color:#9ca3af;
        margin:22px 0;
        font-size:13px;
    }

    .social-btn {
        border:1px solid #e5e7eb;
        padding:13px;
        border-radius:12px;
        margin-top:10px;
        background:white;
        text-align:center;
        font-weight:500;
        cursor:pointer;
    }

    .social-btn img {
        width:18px;
        margin-right:8px;
        vertical-align:middle;
    }

    .signup {
        text-align:center;
        margin-top:18px;
        font-size:14px;
    }

    </style>
    """, unsafe_allow_html=True)

    st.markdown('<div class="login-card">', unsafe_allow_html=True)

    # 로고
    st.markdown('<div class="logo-box">💰</div>', unsafe_allow_html=True)

    st.markdown('<div class="title">Spentopia</div>', unsafe_allow_html=True)

    st.markdown(
        '<div class="subtitle">내가 기록한 소비가 나를 만든다</div>',
        unsafe_allow_html=True
    )

    email = st.text_input("이메일", placeholder="your@email.com")

    password = st.text_input("비밀번호", type="password")

    if st.button("로그인"):
        if email == "test@test.com" and password == "1234":
            st.session_state.login = True
            st.rerun()
        else:
            st.error("이메일 또는 비밀번호가 올바르지 않습니다.")

    st.markdown('<div class="divider">또는</div>', unsafe_allow_html=True)

    # 소셜 버튼
    st.markdown(
        '<div class="social-btn">🟡 카카오로 계속하기</div>',
        unsafe_allow_html=True
    )

    st.markdown(
        '<div class="social-btn">🟢 네이버로 계속하기</div>',
        unsafe_allow_html=True
    )

    st.markdown(
        '<div class="social-btn">🔵 구글로 계속하기</div>',
        unsafe_allow_html=True
    )

    st.markdown(
        '<div class="signup">계정이 없으신가요? <b>회원가입</b></div>',
        unsafe_allow_html=True
    )

    st.markdown('</div>', unsafe_allow_html=True)



def logout():

    if st.button("로그아웃"):
        st.session_state.login = False
        st.rerun()