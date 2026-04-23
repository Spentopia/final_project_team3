package com.ict.spentopia.feature.auth.connector

import android.net.Uri
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import com.solana.mobilewalletadapter.clientlib.successPayload
import org.bitcoinj.core.Base58

class MwaPhantomConnector : SolanaWalletConnector {

    private val walletAdapter = MobileWalletAdapter(
        connectionIdentity = ConnectionIdentity(
            identityUri = Uri.parse("https://spentopia.com"),
            iconUri = Uri.parse("icon.png"),
            identityName = "Spentopia"
        )
    )

    override suspend fun connect(
        walletActivityResultSender: ActivityResultSender
    ): WalletConnectionResult {
        return when (val result = walletAdapter.connect(walletActivityResultSender)) {
            is TransactionResult.Success -> {
                val account = result.authResult.accounts.firstOrNull()
                if (account == null) {
                    WalletConnectionResult.Failure("지갑 계정을 찾을 수 없습니다.")
                } else {
                    WalletConnectionResult.Success(
                        walletAddress = Base58.encode(account.publicKey)
                    )
                }
            }

            is TransactionResult.NoWalletFound -> {
                WalletConnectionResult.Failure("Phantom 지갑 앱을 찾을 수 없습니다.")
            }

            is TransactionResult.Failure -> {
                WalletConnectionResult.Failure(
                    result.e.message ?: "Phantom 지갑 연결 실패"
                )
            }
        }
    }

    override suspend fun signMessage(
        walletActivityResultSender: ActivityResultSender,
        message: ByteArray
    ): WalletSignResult {
        return try {
            when (
                val result = walletAdapter.transact(walletActivityResultSender) { authResult ->
                    signMessagesDetached(
                        arrayOf(message),
                        arrayOf(authResult.accounts.first().publicKey)
                    )
                }
            ) {
                is TransactionResult.Success -> {
                    val signatureBytes = result.successPayload
                        ?.messages
                        ?.firstOrNull()
                        ?.signatures
                        ?.firstOrNull()

                    if (signatureBytes == null) {
                        WalletSignResult.Failure("서명 결과가 비어 있습니다.")
                    } else {
                        WalletSignResult.Success(
                            signature = Base58.encode(signatureBytes)
                        )
                    }
                }

                is TransactionResult.NoWalletFound -> {
                    WalletSignResult.Failure("Phantom 지갑 앱을 찾을 수 없습니다.")
                }

                is TransactionResult.Failure -> {
                    WalletSignResult.Failure(
                        result.e.message ?: "Phantom 지갑 서명 실패"
                    )
                }
            }
        } catch (e: Exception) {
            WalletSignResult.Failure(e.message ?: "Phantom 지갑 서명 중 오류")
        }
    }
}