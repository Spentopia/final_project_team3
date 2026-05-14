export interface ProfileStatus {
  isProfileComplete: boolean;
  email: string | null;
  nickname: string | null;
}

export interface UserProfile {
  id: string;
  email: string | null;
  nickname: string | null;
  phone: string | null;
  introduction: string | null;
  profile_image: string | null;
  login_provider: string | null;
  google_connected: boolean;
  wallet_address: string | null;
  role_type: string;
  profile_completed: boolean;
  spt_balance: number;
  created_at: string;
  current_streak: number;
}

export interface UpdateUserProfileRequest {
  nickname?: string;
  phone?: string;
  introduction?: string | null;
  profile_image?: string;
}

export interface UserSettings {
  alert_budget: boolean | null;
  alert_reward: boolean | null;
  alert_streak: boolean | null;
  alert_social: boolean | null;
  notification_listener: boolean | null;
}

export interface UpdateUserSettingsRequest {
  alert_budget?: boolean;
  alert_reward?: boolean;
  alert_streak?: boolean;
  alert_social?: boolean;
  notification_listener?: boolean;
}
