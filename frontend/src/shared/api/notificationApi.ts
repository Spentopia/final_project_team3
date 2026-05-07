import { apiClient } from "@/shared/api/client";

export type NotificationItem = {
  id: string;
  user_id: string;
  notification_type: string;
  message: string;
  is_read: boolean;
  created_at: string | null;
};

const extractApiErrorMessage = (error: unknown, fallback: string) => {
  if (error && typeof error === "object" && "response" in error) {
    const response = (
      error as {
        response?: {
          data?: unknown;
        };
      }
    ).response;

    if (typeof response?.data === "string" && response.data.trim()) {
      return response.data;
    }
  }

  if (error instanceof Error && error.message.trim()) {
    return error.message;
  }

  return fallback;
};

export async function getNotifications(): Promise<NotificationItem[]> {
  try {
    const { data } = await apiClient.get<NotificationItem[]>("/api/notifications");
    return data;
  } catch (error) {
    throw new Error(extractApiErrorMessage(error, "알림을 불러오지 못했습니다."));
  }
}

export async function markAllNotificationsRead(): Promise<void> {
  try {
    await apiClient.patch("/api/notifications/read-all");
  } catch (error) {
    throw new Error(extractApiErrorMessage(error, "알림 읽음 처리에 실패했습니다."));
  }
}
