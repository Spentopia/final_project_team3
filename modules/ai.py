import os
from dotenv import load_dotenv
from openai import OpenAI


load_dotenv()

client = OpenAI(

    api_key=os.getenv(

        "OPENAI_API_KEY"

    )

)


def analyze(df):

    summary = df.groupby(

        "category"

    )["amount"].sum()


    txt = "\n".join(

        [f"{k}:{v}" for k,v in summary.items()]

    )


    prompt = f"""

소비패턴 분석:

{txt}

절약 방법 제시

"""


    res = client.chat.completions.create(

        model="gpt-4o-mini",

        messages=[

            {

                "role":"user",

                "content":prompt

            }

        ]

    )


    return res.choices[0].message.content