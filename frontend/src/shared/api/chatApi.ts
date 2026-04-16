export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  response: string;
}

export async function sendChatMessage(payload: ChatRequest): Promise<ChatResponse> {
  const baseUrl = import.meta.env.VITE_AI_SERVER_URL;

  if (!baseUrl) {
    throw new Error("VITE_AI_SERVER_URL이 설정되지 않았습니다.");
  }

  const response = await fetch(`${baseUrl}/api/v1/chat/`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(`AI 상담 요청 실패: ${response.status} ${text}`);
  }

  return response.json();
}
