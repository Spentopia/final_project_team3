import pandas as pd
import os

DATA_FILE = "data.csv"


def load_data():

    if os.path.exists(DATA_FILE):

        df = pd.read_csv(DATA_FILE)

    else:

        df = pd.DataFrame(
            columns=[
                "date",
                "type",
                "amount",
                "category",
                "merchant",
                "user"
            ]
        )

    if len(df) > 0:

        df["date"] = pd.to_datetime(df["date"])

    return df



def save_data(row):

    df = load_data()

    df = pd.concat(
        [df, pd.DataFrame([row])]
    )

    df.to_csv(DATA_FILE, index=False)



def load_users():

    if os.path.exists(USER_FILE):

        return pd.read_csv(USER_FILE)

    else:

        df = pd.DataFrame(
            columns=[
                "id",
                "pw"
            ]
        )

        df.to_csv(USER_FILE, index=False)

        return df



def save_user(id, pw):

    df = load_users()

    df = pd.concat(
        [
            df,
            pd.DataFrame(
                [{"id":id, "pw":pw}]
            )
        ]
    )

    df.to_csv(USER_FILE, index=False)



def update_password(id,new_pw):

    df = load_users()

    df.loc[df["id"]==id,"pw"] = new_pw

    df.to_csv(USER_FILE,index=False)