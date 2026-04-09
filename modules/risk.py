def risk_score(df):

    total = df["amount"].sum()

    food = df[df["category"]=="식비"]["amount"].sum()

    cafe = df[df["category"]=="카페"]["amount"].sum()

    score = 0


    if food/total > 0.4:

        score += 40


    if cafe/total > 0.2:

        score += 30


    if total > 2000000:

        score += 30


    return score