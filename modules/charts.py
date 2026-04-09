import matplotlib.pyplot as plt
import plotly.express as px


def pie_chart(df):

    d = df[df["type"]=="지출"].groupby(

        "category"

    )["amount"].sum()

    fig,ax = plt.subplots()

    ax.pie(

        d,

        labels=d.index,

        autopct="%1.1f%%"

    )

    return fig



def monthly_chart(df):

    df["month"]=df["date"].dt.to_period(

        "M"

    ).astype(str)

    d = df.groupby(

        ["month","type"]

    )["amount"].sum().reset_index()

    fig = px.bar(

        d,

        x="month",

        y="amount",

        color="type"

    )

    return fig



def trend_chart(df):

    d = df.groupby("date")["amount"].sum().reset_index()

    return px.line(

        d,

        x="date",

        y="amount"

    )