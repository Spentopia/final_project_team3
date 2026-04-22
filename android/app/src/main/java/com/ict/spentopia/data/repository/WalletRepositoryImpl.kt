package com.ict.spentopia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ict.spentopia.data.local.walletDataStore
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.WalletLinkRequest
import com.ict.spentopia.data.remote.WalletUnlinkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletRepositoryImpl(
    private val context: Context
) : WalletRepository {

    // 지갑 주소 key입니다.
    private val walletAddressKey = stringPreferencesKey("wallet_address")

    // 지갑 종류 key입니다.
    private val walletProviderKey = stringPreferencesKey("wallet_provider")

    private val walletApi = RetrofitClient.walletApi

    override suspend fun saveWallet(address: String, provider: String) {
        context.walletDataStore.edit { preferences ->
            preferences[walletAddressKey] = address
            preferences[walletProviderKey] = provider
        }
    }

    override fun getWalletAddress(): Flow<String?> {
        return context.walletDataStore.data.map { preferences ->
            preferences[walletAddressKey]
        }
    }

    override fun getWalletProvider(): Flow<String?> {
        return context.walletDataStore.data.map { preferences ->
            preferences[walletProviderKey]
        }
    }

    override suspend fun clearWallet() {
        context.walletDataStore.edit { preferences ->
            preferences.remove(walletAddressKey)
            preferences.remove(walletProviderKey)
        }
    }

    override suspend fun issueWalletNonce(): String {
        return walletApi.issueWalletNonce().nonce
    }

    override suspend fun linkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    ) {
        walletApi.linkWallet(
            authorization = "Bearer $token",
            request = WalletLinkRequest(
                wallet_address = walletAddress,
                nonce = nonce,
                signature = signature
            )
        )
    }

    override suspend fun unlinkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    ) {
        walletApi.unlinkWallet(
            authorization = "Bearer $token",
            request = WalletUnlinkRequest(
                wallet_address = walletAddress,
                nonce = nonce,
                signature = signature
            )
        )
    }
}