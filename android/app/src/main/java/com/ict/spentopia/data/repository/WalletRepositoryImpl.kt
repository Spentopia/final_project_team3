package com.ict.spentopia.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.ict.spentopia.data.local.walletDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalletRepositoryImpl(
    private val context: Context
) : WalletRepository {

    // 지갑 주소 key입니다.
    private val walletAddressKey = stringPreferencesKey("wallet_address")

    // 지갑 종류 key입니다.
    private val walletProviderKey = stringPreferencesKey("wallet_provider")

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
}