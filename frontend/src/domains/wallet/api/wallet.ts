// 지갑 인증 관련 API 모음.
// 프론트는 Solana 지갑에서 nonce를 서명만 하고,
// 최종 검증과 사용자-지갑 매핑 저장은 모두 백엔드가 담당한다.
import {
  LinkWalletRequest, LinkWalletResponse, UnlinkWalletRequest, UnlinkWalletResponse,
  WalletLoginRequest,
  WalletLoginResponse,
  WalletNonceRequest,
  WalletNonceResponse
} from "@/domains/wallet/model/types";
import {apiClient} from "@/shared/api/client";

// POST /auth/wallet/nonce
// 로그인과 연동 모두 "서버가 nonce를 발급 -> 프론트가 signMessage -> 다시 서버로 verify"
// 흐름을 쓰므로 nonce 발급 API를 공통으로 사용한다.
export async function requestWalletNonce(
    payload: WalletNonceRequest,
): Promise<WalletNonceResponse> {
  const response = await apiClient.post<WalletNonceResponse>('/auth/wallet/nonce',payload);
  return response.data;
}

// POST /auth/wallet/login
// 이미 계정에 연결된 지갑만 이 API를 통해 로그인할 수 있다.
// 백엔드는 wallet_address가 어떤 사용자에 매핑되어 있는지 Supabase에서 찾고,
// nonce/서명을 검증한 뒤 access/refresh token을 발급한다.
export async function loginWithWalletApi(
  payload: WalletLoginRequest,
): Promise<WalletLoginResponse>{
  const response = await apiClient.post<WalletLoginResponse>('/auth/wallet/login', payload);
  return response.data;
}

// POST /wallet/link
// 일반 로그인된 사용자가 "내 계정에 현재 지갑을 연결"할 때 호출한다.
// 이 요청은 보호 라우트여야 하므로 Authorization 헤더가 반드시 실려야 한다.
// 백엔드는 토큰으로 현재 사용자를 식별하고, 서명 검증이 끝나면 Supabase에
// user_id <-> wallet_address 매핑을 저장한다.
export async function linkWalletApi(
    payload: LinkWalletRequest,
): Promise<LinkWalletResponse>{
  const response = await apiClient.post<LinkWalletResponse>('/wallet/link', payload);
  return response.data;
}

// DELETE /wallet/unlink
// 브라우저 wallet adapter 연결을 끊는 것이 아니라,
// 서버에 저장된 "내 계정과 지갑 주소의 연동 정보"를 제거하는 요청이다.
export async function unlinkWalletApi(payload: UnlinkWalletRequest): Promise<UnlinkWalletResponse>{
  const response = await apiClient.delete<UnlinkWalletResponse>('/wallet/unlink', {
    data: payload,
  });
  return response.data;
}
