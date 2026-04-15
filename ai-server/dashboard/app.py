import streamlit as st
import matplotlib.pyplot as plt
import streamlit.components.v1 as components
import base64
from io import BytesIO

st.set_page_config(page_title="소비 패턴 분석", layout="wide")

# =========================
# CSS
# =========================
st.markdown("""
<style>
.block-container {
    padding: 4rem 3rem 2rem 3rem;
    background-color: #f5f7fb;
}

.card {
    background: white;
    border-radius: 16px;
    padding: 24px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    margin-bottom:20px;
}

.title {
    font-size:28px;
    font-weight:700;
}

.subtitle {
    color:#6b7280;
    margin-bottom:20px;
}

.gradient {
    background: linear-gradient(135deg,#9333ea,#ec4899);
    color:white;
}

.progress {
    height:8px;
    background:#e5e7eb;
    border-radius:10px;
}

.progress-bar {
    height:8px;
    border-radius:10px;
    background:linear-gradient(90deg,#9333ea,#ec4899);
}

.card-fixed {
    height: 150px;   /* 🔥 원하는 높이 */
    display: flex;
    flex-direction: column;
    justify-content: space-between;
}

/* 카테고리 카드 높이 맞추기 */
.equal-card {
    height: 100%;
    min-height: 420px;  /* 🔥 높이 통일 핵심 */
}
</style>
""", unsafe_allow_html=True)

# =========================
# 제목
# =========================
st.markdown("""
<div class="title">소비 패턴 분석</div>
<div class="subtitle">AI가 분석한 당신의 소비 습관을 확인해보세요</div>
""", unsafe_allow_html=True)

# =========================
# 상단 카드
# =========================
c1,c2,c3,c4 = st.columns(4)

with c1:
    st.markdown("""
    <div class="card card-fixed gradient">
        <div>이번 달 총 지출</div>
        <h2>300,000원</h2>
        <div style="font-size:12px;">▼ 지난 달 대비 -12%</div>
    </div>
    """, unsafe_allow_html=True)

with c2:
    st.markdown("""
    <div class="card card-fixed">
        <div>일 평균 지출</div>
        <h3>23,571원</h3>
        <div style="color:#16a34a;">▼ -5% 절약중</div>
    </div>
    """, unsafe_allow_html=True)

with c3:
    st.markdown("""
    <div class="card card-fixed">
        <div>예산 사용률</div>
        <h3>60%</h3>
        <div class="progress">
            <div class="progress-bar" style="width:60%"></div>
        </div>
    </div>
    """, unsafe_allow_html=True)

with c4:
    st.markdown("""
    <div class="card card-fixed">
        <div>최대 소비 카테고리</div>
        <h3>🍔 식비</h3>
        <div style="color:#6b7280;">전체의 45%</div>
    </div>
    """, unsafe_allow_html=True)

# =========================
# 그래프
# =========================
st.markdown("### 주간 소비 추이")

# 🔥 한글 폰트 설정 (Windows)
plt.rcParams['font.family'] = 'Malgun Gothic'

# 마이너스 깨짐 방지
plt.rcParams['axes.unicode_minus'] = False

labels = ["월","화","수","목","금","토","일"]
values = [15000,8000,20000,12000,35000,45000,28000]

fig, ax = plt.subplots(figsize=(6, 1.0), dpi=1500)  # 🔥 기존보다 높이 절반
ax.bar(labels, values)
ax.tick_params(axis='x', labelsize=4)  # 요일 글자
ax.tick_params(axis='y', labelsize=4)  # 숫자 글자
st.pyplot(fig)

st.markdown('</div>', unsafe_allow_html=True)

# =========================
# 카테고리 영역
# =========================
left, right = st.columns(2)

fig2, ax2 = plt.subplots(figsize=(3.5,3.5), dpi=1500)
ax2.pie(
    [45,20,15,12,8],
    labels=["식비","교통","쇼핑","여가","기타"],
    autopct='%1.0f%%',
    textprops={'fontsize':15}
)

buf = BytesIO()
plt.savefig(buf, format="png", bbox_inches='tight')
buf.seek(0)
img = base64.b64encode(buf.read()).decode()

# =========================
# LEFT (그래프 카드)
# =========================
with left:
    components.html(f"""
        <div style="
            font-size:23px;
            margin-top:-8px;
            background:white;
            border-radius:16px;
            padding:5px;
            box-shadow:0 4px 12px rgba(0,0,0,0.05);
            min-height:470px;
            overflow:hidden;
        ">
            <h3 style="
    position: relative;
    z-index: 2;
    margin-top:25px;
    margin-bottom:8px;
    margin-left:20px;
">
카테고리별 지출
</h3>
            <img src="data:image/png;base64,{img}" style=" width:100%;
        height:480px;       /* 🔥 높이 강제 제한 */
        margin-top:-74px;
        object-fit:contain; /* 🔥 비율 유지 */
        display:block;">
        </div>
        """, height=485)


# =========================
# RIGHT (상세 카드)
# =========================
with right:
    st.markdown("""
    <div class="card equal-card">

    <h3>카테고리 상세</h3>

    <div style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between;">
            <div>식비</div><div>135,000원</div>
        </div>
        <div style="height:8px;background:#e5e7eb;border-radius:10px;margin-top:6px;">
            <div style="width:45%;background:#f97316;height:8px;border-radius:10px;"></div>
        </div>
        <div style="text-align:right;font-size:12px;color:#6b7280;">45%</div>
    </div>

    <div style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between;">
            <div>교통</div><div>60,000원</div>
        </div>
        <div style="height:8px;background:#e5e7eb;border-radius:10px;margin-top:6px;">
            <div style="width:20%;background:#3b82f6;height:8px;border-radius:10px;"></div>
        </div>
        <div style="text-align:right;font-size:12px;color:#6b7280;">20%</div>
    </div>

    <div style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between;">
            <div>쇼핑</div><div>45,000원</div>
        </div>
        <div style="height:8px;background:#e5e7eb;border-radius:10px;margin-top:6px;">
            <div style="width:15%;background:#ec4899;height:8px;border-radius:10px;"></div>
        </div>
        <div style="text-align:right;font-size:12px;color:#6b7280;">15%</div>
    </div>

    <div style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between;">
            <div>여가</div><div>36,000원</div>
        </div>
        <div style="height:8px;background:#e5e7eb;border-radius:10px;margin-top:6px;">
            <div style="width:12%;background:#8b5cf6;height:8px;border-radius:10px;"></div>
        </div>
        <div style="text-align:right;font-size:12px;color:#6b7280;">12%</div>
    </div>

    <div style="margin-bottom:16px;">
        <div style="display:flex; justify-content:space-between;">
            <div>기타</div><div>24,000원</div>
        </div>
        <div style="height:8px;background:#e5e7eb;border-radius:10px;margin-top:6px;">
            <div style="width:8%;background:#6b7280;height:8px;border-radius:10px;"></div>
        </div>
        <div style="text-align:right;font-size:12px;color:#6b7280;">8%</div>
    </div>

    </div>
    """, unsafe_allow_html=True)

# =========================
# ✅ AI 리포트 (완전 해결)
# =========================
components.html("""
<div style="background:white; border-radius:16px; padding:24px; margin-top:20px;">

<h3>✨ AI 소비 분석 리포트</h3>

<div style="display:grid; grid-template-columns:1fr 1fr; gap:16px; margin-top:20px;">

    <div style="border:1px solid #e5e7eb;border-radius:12px;padding:16px;">
        <b style="color:#16a34a;">✔ 잘하고 있어요!</b>
        <div style="margin-top:8px;">소비가 감소했어요 👏</div>
    </div>

    <div style="border:1px solid #e5e7eb;border-radius:12px;padding:16px;">
        <b style="color:#16a34a;">✔ 절약 습관 형성</b>
        <div style="margin-top:8px;">교통비 절약 🚇</div>
    </div>

    <div style="border:1px solid #facc15;border-radius:12px;padding:16px;">
        <b style="color:#f59e0b;">⚠ 주의 필요</b>
        <div style="margin-top:8px;">여가 지출 과다</div>
    </div>

    <div style="border:1px solid #60a5fa;border-radius:12px;padding:16px;">
        <b style="color:#2563eb;">📈 목표 가능</b>
        <div style="margin-top:8px;">저축 가능 💪</div>
    </div>

</div>

</div>
""", height=420)

# =========================
# 소비 패턴 분석 (최종 하단 카드)
# =========================
components.html("""
<div style="background:white; border-radius:16px; padding:24px; margin-top:20px;">

<h3 style="margin-bottom:24px;">소비 패턴 분석</h3>

<div style="display:flex; gap:60px;">

    <!-- 시간대별 소비 -->
    <div style="flex:1;">
        <div style="font-weight:600; margin-bottom:16px;">시간대별 소비</div>

        <div style="margin-bottom:12px;">
            오전 (06-12시)
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:30%; height:6px; background:#9333ea; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">30%</div>
            </div>
        </div>

        <div style="margin-bottom:12px;">
            오후 (12-18시)
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:50%; height:6px; background:#9333ea; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">50%</div>
            </div>
        </div>

        <div>
            저녁 (18-24시)
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:20%; height:6px; background:#9333ea; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">20%</div>
            </div>
        </div>
    </div>

    <!-- 요일별 소비 -->
<div style="flex:1;">
    <div style="font-weight:600; margin-bottom:16px;">요일별 소비</div>

    <!-- 🔥 가로 배치 -->
    <div style="display:flex; gap:16px; align-items:flex-start;">

        <!-- 왼쪽: 평일/주말 -->
        <div style="flex:1;">
            <div style="margin-bottom:12px;">
                평일
                <div style="margin-top:10px;">
                    <span style="background:#111827;color:white;padding:6px 12px;border-radius:999px;font-size:12px;font-weight:600;">
                        65,000원
                    </span>
                </div>
            </div>

            <div>
            <div style="margin-bottom:12px;">
                주말
                <div style="margin-top:10px;">
                    <span style="background:#e5e7eb;color:#111827;padding:6px 12px;border-radius:999px;font-size:12px;font-weight:600;">
                        100,000원
                    </span>
                </div>
            </div>
            </div>
        </div>

        <!-- 🔥 오른쪽: 카드 -->
        <div style="width:850px; margin-top:30px;">
            <div style="border:1px solid #60a5fa;border-radius:12px;padding:16px;">
                <b style="color:#60a5fa;">평일,주말 소비 비교</b>
                <div style="margin-top:8px;">주말 소비가 54% 더 많아요</div>
            </div>
        </div>

    </div>
</div>

    <!-- 결제 방법 -->
    <div style="flex:1;">
        <div style="font-weight:600; margin-bottom:16px;">결제 방법</div>

        <div style="margin-bottom:12px;">
            카드
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:75%; height:6px; background:#0ea5e9; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">75%</div>
            </div>
        </div>

        <div style="margin-bottom:12px;">
            현금
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:20%; height:6px; background:#0ea5e9; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">20%</div>
            </div>
        </div>

        <div>
            기타
            <div style="display:flex; align-items:center; gap:10px;">
                <div style="flex:1; height:6px; background:#e5e7eb; border-radius:10px;">
                    <div style="width:5%; height:6px; background:#0ea5e9; border-radius:10px;"></div>
                </div>
                <div style="font-size:13px;">5%</div>
            </div>
        </div>
    </div>

</div>

</div>
""", height=350)