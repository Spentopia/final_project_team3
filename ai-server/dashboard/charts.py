import pandas as pd
import matplotlib.pyplot as plt

def category_chart(data):

    df = pd.DataFrame(data)

    if df.empty:
        return None

    count = df["category"].value_counts()

    fig, ax = plt.subplots()
    count.plot(kind="bar", ax=ax)

    ax.set_title("카테고리별 소비")
    ax.set_ylabel("횟수")

    return fig