package com.ict.spentopia.feature.auth.connector

sealed class WalletSignResult {
    data class Success(
        val signature: String
    ) : WalletSignResult()

    data class Failure(
        val message: String
    ) : WalletSignResult()
}