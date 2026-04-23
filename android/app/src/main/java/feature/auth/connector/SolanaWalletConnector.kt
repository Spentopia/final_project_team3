package com.ict.spentopia.feature.auth.connector

import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

interface SolanaWalletConnector {
    suspend fun connect(
        walletActivityResultSender: ActivityResultSender
    ): WalletConnectionResult

    suspend fun signMessage(
        walletActivityResultSender: ActivityResultSender,
        message: ByteArray
    ): WalletSignResult
}