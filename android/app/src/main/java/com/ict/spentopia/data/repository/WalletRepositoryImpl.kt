package com.ict.spentopia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ict.spentopia.data.local.walletDataStore
import com.ict.spentopia.data.remote.NonceRequest
import com.ict.spentopia.data.remote.NonceResponse
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.WalletLinkRequest
import com.ict.spentopia.data.remote.WalletLinkResponse
import com.ict.spentopia.data.remote.WalletLoginRequest
import com.ict.spentopia.data.remote.WalletLoginResponse
import com.ict.spentopia.data.remote.WalletUnlinkRequest
import com.ict.spentopia.data.remote.WalletUnlinkResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletRepositoryImpl(
    private val context: Context
) : WalletRepository {

    // 지갑 주소 key입니다.
    // DataStore에 어떤 값을 어떤 이름으로 저장할지 고정합니다.
    private val walletAddressKey = stringPreferencesKey("wallet_address")

    // 지갑 종류 key입니다.
    private val walletProviderKey = stringPreferencesKey("wallet_provider")

    // Retrofit 지갑 API 객체입니다.
    // 서버와 통신하는 실제 엔드포인트는 여기서만 사용합니다.
    private val walletApi = RetrofitClient.walletApi

    // 지갑 정보를 앱 로컬 저장소에 기록합니다.
    override suspend fun saveWallet(address: String, provider: String) {
        context.walletDataStore.edit { preferences ->
            preferences[walletAddressKey] = address
            preferences[walletProviderKey] = provider
        }
    }

    // 저장된 지갑 주소를 Flow로 읽어옵니다.
    override fun getWalletAddress(): Flow<String?> {
        return context.walletDataStore.data.map { preferences ->
            preferences[walletAddressKey]
        }
    }

    // 저장된 지갑 종류를 Flow로 읽어옵니다.
    override fun getWalletProvider(): Flow<String?> {
        return context.walletDataStore.data.map { preferences ->
            preferences[walletProviderKey]
        }
    }

    // 지갑 정보 삭제
    override suspend fun clearWallet() {
        context.walletDataStore.edit { preferences ->
            preferences.remove(walletAddressKey)
            preferences.remove(walletProviderKey)
        }
    }

    // 서버에서 서명용 nonce를 발급받습니다.
    override suspend fun issueWalletNonce(walletAddress: String): NonceResponse {
        return walletApi.issueWalletNonce(
            request = NonceRequest(
                wallet_address = walletAddress
            )
        )
    }

    // 서버에 지갑 연결 요청을 보냅니다.
    override suspend fun linkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    ): WalletLinkResponse {
        return walletApi.linkWallet(
            authorization = "Bearer $token",
            request = WalletLinkRequest(
                wallet_address = walletAddress,
                nonce = nonce,
                signature = signature
            )
        )
    }

    // 지갑 로그인용 검증 API를 호출합니다.
    override suspend fun walletLoginApp(
        walletAddress: String,
        nonce: String,
        signature: String
    ): WalletLoginResponse {
        return walletApi.walletLoginApp(
            request = WalletLoginRequest(
                wallet_address = walletAddress,
                nonce = nonce,
                signature = signature
            )
        )
    }

    // 서버에서 지갑 연결 해제를 처리합니다.
    override suspend fun unlinkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    ): WalletUnlinkResponse {
        return walletApi.unlinkWallet(
            authorization = "Bearer $token",
            request = WalletUnlinkRequest(
                wallet_address = walletAddress,
                nonce = nonce,
                signature = signature
            )
        )
    }
}
