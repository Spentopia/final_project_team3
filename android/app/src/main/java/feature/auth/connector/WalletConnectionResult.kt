package com.ict.spentopia.feature.auth.connector

sealed class WalletConnectionResult {
    data class Success(
        val walletAddress: String
    ) : WalletConnectionResult()

    data class Failure(
        val message: String
    ) : WalletConnectionResult()
}