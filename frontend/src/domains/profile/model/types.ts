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
  profile_image: string | null;
  login_provider: string | null;
  wallet_address: string | null;
  role_type: string;
  profile_completed: boolean;
  spt_balance: number;
  created_at: string;
}

export interface UpdateUserProfileRequest {
  nickname?: string;
  phone?: string;
  profile_image?: string;
}
