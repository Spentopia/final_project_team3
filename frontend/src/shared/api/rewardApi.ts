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

let currentWeeklyScoreInFlight: Promise<WeeklyScoreResponse> | null = null;
let streakInFlight: Promise<StreakResponse> | null = null;

export async function getCurrentWeeklyScore(): Promise<WeeklyScoreResponse> {
  if (!currentWeeklyScoreInFlight) {
    currentWeeklyScoreInFlight = apiClient
      .get<WeeklyScoreResponse>("/api/rewards/weekly-score/current")
      .then((res) => res.data)
      .finally(() => {
        currentWeeklyScoreInFlight = null;
      });
  }

  return currentWeeklyScoreInFlight;
}

export async function getStreak(): Promise<StreakResponse> {
  if (!streakInFlight) {
    streakInFlight = apiClient
      .get<StreakResponse>("/api/rewards/streak")
      .then((res) => res.data)
      .finally(() => {
        streakInFlight = null;
      });
  }

  return streakInFlight;
}
