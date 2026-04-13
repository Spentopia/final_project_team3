from app.clients.openai_client import OpenAIClient

class ChatService:

    @staticmethod
    def chat(message: str):
        return OpenAIClient.chat(message)