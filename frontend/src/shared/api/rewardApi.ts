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

export interface BoxCountResponse {
  box_count: number;
  daily_earned: number; // 오늘 당일 영수증 인증으로 받은 뽑기권 수
  daily_limit: number; // 하루 최대 (당일 영수증 인증) 뽑기권 수
}

export interface RewardBoxItemResponse {
  inventory_id: string;
  item_id: string;
  name: string;
  image_url: string;
  is_equipped: boolean;
  slot_name: string;
}

export type RewardType = "miss" | "spt" | "avatar";

export interface OpenBoxResponse {
  remaining_box_count: number;
  reward_type: RewardType;
  is_win: boolean;
  message: string;
  spt_amount: number | null;
  item: RewardBoxItemResponse | null;
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

export async function getRewardBoxCount(): Promise<BoxCountResponse> {
  const res = await apiClient.get<BoxCountResponse>("/api/unity/rewards/box-count");
  return res.data;
}

export async function openRewardBox(): Promise<OpenBoxResponse> {
  const res = await apiClient.post<OpenBoxResponse>("/api/unity/rewards/open-box");
  return res.data;
}
