import csv
import os

FILE_PATH = "data.csv"

class StorageService:

    @staticmethod
    def save(data: dict):
        file_exists = os.path.isfile(FILE_PATH)

        with open(FILE_PATH, mode="a", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=data.keys())

            if not file_exists:
                writer.writeheader()

            writer.writerow(data)

    @staticmethod
    def load():
        if not os.path.exists(FILE_PATH):
            return []

        with open(FILE_PATH, mode="r", encoding="utf-8") as f:
            reader = csv.DictReader(f)
            return list(reader)

    # ✅ 추가된 부분 (핵심🔥)
    @staticmethod
    def delete(expense_id: str):
        if not os.path.exists(FILE_PATH):
            return False

        try:
            rows = []

            # 1. 기존 데이터 불러오기
            with open(FILE_PATH, mode="r", encoding="utf-8") as f:
                reader = csv.DictReader(f)
                rows = list(reader)
                fieldnames = reader.fieldnames

            # 2. 삭제 대상 제외
            new_rows = [row for row in rows if str(row.get("id")) != str(expense_id)]

            # 👉 삭제된 게 없으면 False
            if len(rows) == len(new_rows):
                return False

            # 3. 다시 덮어쓰기
            with open(FILE_PATH, mode="w", newline="", encoding="utf-8") as f:
                writer = csv.DictWriter(f, fieldnames=fieldnames)
                writer.writeheader()
                writer.writerows(new_rows)

            return True

        except Exception as e:
            print("삭제 실패:", e)
            return False