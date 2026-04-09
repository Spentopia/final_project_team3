import streamlit as st
from modules.data import load_users, save_user, update_password


def auth_page():

    tab1, tab2, tab3 = st.tabs(
        ["로그인","회원가입","계정찾기"]
    )


    with tab1:

        id = st.text_input("아이디")

        pw = st.text_input(
            "비밀번호",
            type="password"
        )

        if st.button("로그인"):

            users = load_users()

            match = users[
                (users["id"]==id) &
                (users["pw"]==pw)
            ]

            if len(match)>0:

                st.session_state["login"]=True

                st.session_state["user"]=id

                st.rerun()

            else:

                st.error("로그인 실패")


    with tab2:

        new_id = st.text_input("아이디",key="j1")

        new_pw = st.text_input(
            "비밀번호",
            type="password",
            key="j2"
        )

        if st.button("회원가입"):

            users = load_users()

            if new_id in users["id"].values:

                st.warning("이미 존재")

            else:

                save_user(new_id,new_pw)

                st.success("가입 완료")


    with tab3:

        users = load_users()

        find_id = st.text_input("아이디 입력")

        new_pw = st.text_input(
            "새 비밀번호",
            type="password"
        )

        if st.button("비밀번호 재설정"):

            if find_id in users["id"].values:

                update_password(
                    find_id,
                    new_pw
                )

                st.success("변경 완료")

            else:

                st.warning("아이디 없음")



def logout():

    if st.button("로그아웃"):

        st.session_state["login"]=False

        st.rerun()