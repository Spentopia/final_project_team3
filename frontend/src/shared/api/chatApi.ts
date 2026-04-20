import { apiClient } from "@/shared/api/client";

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  response: string;
}

export async function sendChatMessage(payload: ChatRequest): Promise<ChatResponse> {
  const response = await apiClient.post<ChatResponse>("/api/chat", payload);
  return response.data;
}
