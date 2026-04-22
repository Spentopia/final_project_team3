package com.ict.spentopia.feature.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.repository.WalletRepository
import com.ict.spentopia.data.repository.WalletRepositoryImpl
import feature.auth.WalletSignResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

    fun issueWalletNonce(
        onSuccess: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isLoadingNonce.value = true
                _walletNonceError.value = null

                val nonce = walletRepository.issueWalletNonce()
                _walletNonce.value = nonce

                onSuccess(nonce)
            } catch (e: Exception) {
                _walletNonceError.value = e.message ?: "nonce 발급 실패"
            } finally {
                _isLoadingNonce.value = false
            }
        }
    }
    suspend fun getWalletNonceOnce(): String {
        return walletRepository.issueWalletNonce()
    }
    fun signNonceWithWallet(
        nonce: String,
        onSuccess: (WalletSignResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // TODO: 실제 지갑 서명 연동
                // 지금은 테스트용 더미 값

                val fakeAddress = "TEST_WALLET_ADDRESS"
                val fakeSignature = "TEST_SIGNATURE"

                onSuccess(
                    WalletSignResult(
                        walletAddress = fakeAddress,
                        signature = fakeSignature
                    )
                )

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
                walletRepository.linkWallet(
                    token = token,
                    walletAddress = walletAddress,
                    nonce = nonce,
                    signature = signature
                )

                walletRepository.saveWallet(
                    address = walletAddress,
                    provider = provider
                )

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "지갑 연동 실패")
            }
        }
    }
}