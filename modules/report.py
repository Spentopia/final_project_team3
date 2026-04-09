def monthly_report(df):

    summary = df.groupby(

        "category"

    )["amount"].sum()


    text = "월간 리포트\n\n"


    for k,v in summary.items():

        text += f"{k}: {v}\n"


    return text