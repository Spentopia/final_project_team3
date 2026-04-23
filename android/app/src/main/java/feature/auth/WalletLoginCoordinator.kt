package com.ict.spentopia.feature.auth

import android.util.Log
import com.ict.spentopia.feature.auth.connector.MwaBackpackConnector
import com.ict.spentopia.feature.auth.connector.MwaPhantomConnector
import com.ict.spentopia.feature.auth.connector.MwaSolflareConnector
import com.ict.spentopia.feature.auth.connector.SolanaWalletConnector
import com.ict.spentopia.feature.auth.connector.WalletConnectionResult
import com.ict.spentopia.feature.auth.connector.WalletSignResult
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

class WalletLoginCoordinator(
    private val loginViewModel: LoginViewModel
) {

    // 1. 마지막으로 연결된 지갑 주소를 저장할 변수 추가
    private var lastWalletAddress: String? = null

    private val phantomConnector: SolanaWalletConnector = MwaPhantomConnector()
    private val solflareConnector: SolanaWalletConnector = MwaSolflareConnector()
    private val backpackConnector: SolanaWalletConnector = MwaBackpackConnector()

    // 2. 외부에서 주소를 가져올 수 있는 getter 함수 추가
    fun getLastWalletAddress(): String? = lastWalletAddress

    suspend fun loginWithWallet(
        walletType: SolanaWalletType,
        walletActivityResultSender: ActivityResultSender,
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit
    ) {
        val connector = when (walletType) {
            SolanaWalletType.PHANTOM -> phantomConnector
            SolanaWalletType.SOLFLARE -> solflareConnector
            SolanaWalletType.BACKPACK -> backpackConnector
        }

        val connectResult = connector.connect(walletActivityResultSender)

        val walletAddress = when (connectResult) {
            is WalletConnectionResult.Success -> {
                // 3. connect 성공 시점에 walletAddress 저장
                lastWalletAddress = connectResult.walletAddress
                connectResult.walletAddress
            }
            is WalletConnectionResult.Failure -> {
                onError(connectResult.message)
                return
            }
        }

        Log.d("Spentopia", "walletAddress=$walletAddress")

        val nonceResponse = try {
            loginViewModel.getWalletNonceOnce(walletAddress)
        } catch (e: Exception) {
            onError(e.message ?: "nonce 요청 실패")
            return
        }

        Log.d("Spentopia", "nonce=${nonceResponse.nonce}")
        Log.d("Spentopia", "message=${nonceResponse.message}")

        val signResult = connector.signMessage(
            walletActivityResultSender = walletActivityResultSender,
            message = nonceResponse.message.toByteArray()
        )

        val signature = when (signResult) {
            is WalletSignResult.Success -> signResult.signature
            is WalletSignResult.Failure -> {
                onError(signResult.message)
                return
            }
        }

        Log.d("Spentopia", "signature=$signature")

        loginViewModel.walletLoginApp(
            walletAddress = walletAddress,
            nonce = nonceResponse.nonce,
            signature = signature,
            onSuccess = { response ->
                onSuccess(response.access_token, response.refresh_token)
            },
            onError = { message ->
                onError(message)
            }
        )
    }
}