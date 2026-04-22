import { apiClient } from "@/shared/api/client";

export interface MeResponse {
  profile_completed?: boolean;
  wallet_address?: string | null;
}

let meRequestInFlight: Promise<MeResponse> | null = null;

export async function getMe(): Promise<MeResponse> {
  if (!meRequestInFlight) {
    meRequestInFlight = apiClient
      .get<MeResponse>("/me")
      .then((response) => response.data)
      .finally(() => {
        meRequestInFlight = null;
      });
  }

  return meRequestInFlight;
}
