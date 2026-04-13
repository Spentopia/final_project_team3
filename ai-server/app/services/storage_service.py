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