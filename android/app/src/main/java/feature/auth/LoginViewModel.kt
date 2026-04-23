package com.ict.spentopia.feature.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.remote.NonceResponse
import com.ict.spentopia.data.remote.WalletLoginResponse
import com.ict.spentopia.data.repository.WalletRepository
import com.ict.spentopia.data.repository.WalletRepositoryImpl
import com.ict.spentopia.feature.auth.connector.WalletSignResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    // 지갑 저장소 객체입니다.
    private val walletRepository: WalletRepository = WalletRepositoryImpl(application)

    // 저장 진행 상태입니다.
    private val _isSavingWallet = MutableStateFlow(false)

    // 저장 진행 상태 공개용입니다.
    val isSavingWallet: StateFlow<Boolean> = _isSavingWallet

    // 저장 에러 상태입니다.
    private val _walletSaveError = MutableStateFlow<String?>(null)

    // 저장 에러 상태 공개용입니다.
    val walletSaveError: StateFlow<String?> = _walletSaveError

    // nonce 로딩 상태입니다.
    private val _isLoadingNonce = MutableStateFlow(false)
    val isLoadingNonce: StateFlow<Boolean> = _isLoadingNonce

    // 발급된 nonce 값입니다.
    private val _walletNonce = MutableStateFlow<String?>(null)
    val walletNonce: StateFlow<String?> = _walletNonce

    // 서버가 내려준 실제 서명 대상 message 입니다.
    private val _walletSignMessage = MutableStateFlow<String?>(null)
    val walletSignMessage: StateFlow<String?> = _walletSignMessage

    // nonce 에러 상태입니다.
    private val _walletNonceError = MutableStateFlow<String?>(null)
    val walletNonceError: StateFlow<String?> = _walletNonceError

    fun saveWalletSession(
        walletAddress: String,
        walletProvider: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isSavingWallet.value = true
                _walletSaveError.value = null

                walletRepository.saveWallet(
                    address = walletAddress,
                    provider = walletProvider
                )

                onSuccess()
            } catch (e: Exception) {
                _walletSaveError.value = e.message ?: "지갑 정보 저장 실패"
            } finally {
                _isSavingWallet.value = false
            }
        }
    }

    fun walletLoginApp(
        walletAddress: String,
        nonce: String,
        signature: String,
        onSuccess: (WalletLoginResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = walletRepository.walletLoginApp(
                    walletAddress = walletAddress,
                    nonce = nonce,
                    signature = signature
                )

                Log.d("Spentopia", "walletLoginApp 성공: accessToken=${response.access_token}")
                onSuccess(response)
            } catch (e: Exception) {
                when (e) {
                    is HttpException -> {
                        val errorBody = e.response()?.errorBody()?.string()
                        Log.e(
                            "Spentopia",
                            "walletLoginApp HTTP 실패 code=${e.code()} body=$errorBody",
                            e
                        )
                        onError(errorBody ?: "지갑 로그인 실패 (${e.code()})")
                    }

                    else -> {
                        Log.e("Spentopia", "walletLoginApp 실패", e)
                        onError(e.message ?: "지갑 로그인 실패")
                    }
                }
            }
        }
    }

    fun issueWalletNonce(
        walletAddress: String,
        onSuccess: (NonceResponse) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isLoadingNonce.value = true
                _walletNonceError.value = null

                val response = walletRepository.issueWalletNonce(walletAddress)

                _walletNonce.value = response.nonce
                _walletSignMessage.value = response.message

                Log.d(
                    "Spentopia",
                    "issueWalletNonce 성공: nonce=${response.nonce}, message=${response.message}"
                )

                onSuccess(response)
            } catch (e: Exception) {
                _walletNonceError.value = e.message ?: "nonce 발급 실패"
                Log.e("Spentopia", "issueWalletNonce 실패", e)
            } finally {
                _isLoadingNonce.value = false
            }
        }
    }

    suspend fun getWalletNonceOnce(walletAddress: String): NonceResponse {
        return try {
            val response = walletRepository.issueWalletNonce(walletAddress)

            _walletNonce.value = response.nonce
            _walletSignMessage.value = response.message

            Log.d(
                "Spentopia",
                "getWalletNonceOnce 성공: nonce=${response.nonce}, message=${response.message}"
            )

            response
        } catch (e: Exception) {
            Log.e("Spentopia", "getWalletNonceOnce 실패", e)
            throw e
        }
    }

    fun signMessageWithWallet(
        walletAddress: String,
        message: String,
        onSuccess: (WalletSignResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: 실제 Solana Mobile Wallet Adapter 연동
                // 현재는 아직 실제 지갑 authorize / sign_messages 구현 전 상태입니다.
                //
                // 백엔드 기준으로는 nonce 원문이 아니라
                // build_wallet_sign_message(wallet_address, nonce) 로 생성된
                // message 전체를 서명해야 검증이 통과합니다.

                Log.d(
                    "Spentopia",
                    "signMessageWithWallet 호출됨: walletAddress=$walletAddress, message=$message"
                )

                onError("실제 지갑 서명 기능이 아직 구현되지 않았습니다.")
            } catch (e: Exception) {
                onError(e.message ?: "서명 실패")
            }
        }
    }

    fun linkWalletToServer(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String,
        provider: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response = walletRepository.linkWallet(
                    token = token,
                    walletAddress = walletAddress,
                    nonce = nonce,
                    signature = signature
                )

                walletRepository.saveWallet(
                    address = walletAddress,
                    provider = provider
                )

                Log.d("Spentopia", "linkWalletToServer 성공: ${response.message}")
                onSuccess()
            } catch (e: Exception) {
                Log.e("Spentopia", "linkWalletToServer 실패", e)
                onError(e.message ?: "지갑 연동 실패")
            }
        }
    }
}