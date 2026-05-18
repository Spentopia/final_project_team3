import json
import re


def safe_json_loads(content: str):

    if not content:
        return None

    content = content.strip()

    # ```json 제거
    content = re.sub(r"^```json", "", content)
    content = re.sub(r"```$", "", content)

    # 앞뒤 공백 제거
    content = content.strip()

    return json.loads(content)