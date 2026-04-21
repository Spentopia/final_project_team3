import { apiClient } from "@/shared/api/client";

export interface WeeklyScoreResponse {
  id: string;
  week_start: string;
  record_days_score: number | null;
  receipt_score: number | null;
  diary_score: number | null;
  budget_score: number | null;
  streak_score: number | null;
  total_score: number | null;
  reward_granted: boolean | null;
}

export interface StreakResponse {
  current_streak: number | null;
  longest_streak: number | null;
  last_record_date: string | null;
}

export async function getCurrentWeeklyScore(): Promise<WeeklyScoreResponse> {
  const res = await apiClient.get<WeeklyScoreResponse>(
    "/api/rewards/weekly-score/current"
  );
  return res.data;
}

export async function getStreak(): Promise<StreakResponse> {
  const res = await apiClient.get<StreakResponse>("/api/rewards/streak");
  return res.data;
}
