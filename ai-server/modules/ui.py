import streamlit as st


def load_css():

    st.markdown("""

<style>

.card{

padding:20px;

border-radius:18px;

background:linear-gradient(135deg,#ffffff,#f8f9fa);

box-shadow:0 6px 20px rgba(0,0,0,0.08);

text-align:center;

}

.value{

font-size:30px;

font-weight:700;

}

.label{

color:gray;

}

</style>

""",unsafe_allow_html=True)



def card(title,value):

    st.markdown(

        f"""

<div class="card">

<div class="label">{title}</div>

<div class="value">{value}</div>

</div>

"""

,

unsafe_allow_html=True

    )