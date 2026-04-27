package com.ict.spentopia.feature.auth

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ict.spentopia.data.remote.ExchangeTokenRequest
import com.ict.spentopia.data.remote.KakaoLoginRequest
import com.ict.spentopia.data.remote.KakaoStartResponse
import com.ict.spentopia.data.remote.NonceResponse
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.SupabaseClient
import com.ict.spentopia.data.remote.WalletLoginResponse
import com.ict.spentopia.data.repository.WalletRepository
import com.ict.spentopia.data.repository.WalletRepositoryImpl
import com.ict.spentopia.feature.auth.connector.WalletSignResult
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

class LoginViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val walletRepository: WalletRepository =
        WalletRepositoryImpl(application)

    private val _isSavingWallet = MutableStateFlow(false)
    val isSavingWallet: StateFlow<Boolean> = _isSavingWallet

    private val _walletSaveError = MutableStateFlow<String?>(null)
    val walletSaveError: StateFlow<String?> = _walletSaveError

    private val _isLoadingNonce = MutableStateFlow(false)
    val isLoadingNonce: StateFlow<Boolean> = _isLoadingNonce

    private val _walletNonce = MutableStateFlow<String?>(null)
    val walletNonce: StateFlow<String?> = _walletNonce

    private val _walletSignMessage = MutableStateFlow<String?>(null)
    val walletSignMessage: StateFlow<String?> = _walletSignMessage

    private val _walletNonceError = MutableStateFlow<String?>(null)
    val walletNonceError: StateFlow<String?> = _walletNonceError

    // ===============================
    // 이메일 로그인 추가
    // ===============================
    fun emailLogin(
        email: String,
        password: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                // 1차 Supabase 로그인
                SupabaseClient.client.auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }

                val session =
                    SupabaseClient.client.auth.currentSessionOrNull()

                val supabaseToken =
                    session?.accessToken
                        ?: throw Exception("Supabase 토큰 없음")

                // 2차 우리 서버 JWT 교환
                val response = RetrofitClient.authApi.exchangeToken(
                    request = ExchangeTokenRequest(
                        access_token = supabaseToken
                    )
                )

                // 토큰 저장
                val prefs = getApplication<Application>()
                    .getSharedPreferences(
                        "auth_prefs",
                        Context.MODE_PRIVATE
                    )

                prefs.edit {
                    putString("access_token", response.access_token)
                    putString("refresh_token", response.refresh_token)
                }

                onSuccess()

            } catch (e: Exception) {
                Log.e("Spentopia", "emailLogin 실패", e)
                onError(e.message ?: "로그인 실패")
            }
        }
    }

    fun getKakaoLoginUrl(
        onSuccess: (KakaoStartResponse) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.authApi.startKakaoLogin()
                onSuccess(response)
            } catch (e: Exception) {
                Log.e("Spentopia", "카카오 시작 실패", e)
                onError(e.message ?: "카카오 로그인 시작 실패")
            }
        }
    }

    fun kakaoLogin(
        code: String,
        state: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                val response =
                    RetrofitClient.authApi.finishKakaoLogin(
                        request = KakaoLoginRequest(
                            code = code,
                            state = state
                        )
                    )
                val prefs = getApplication<Application>()
                    .getSharedPreferences(
                        "auth_prefs",
                        Context.MODE_PRIVATE
                    )
                prefs.edit {
                    putString("access_token", response.access_token)
                    putString("refresh_token", response.refresh_token)
                }
                onSuccess()
            } catch (e: Exception) {
                Log.e("Spentopia", "카카오 로그인 실패", e)
                onError(e.message ?: "카카오 로그인 실패")
            }
        }
    }

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
                _walletSaveError.value =
                    e.message ?: "지갑 정보 저장 실패"

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
                val response =
                    walletRepository.walletLoginApp(
                        walletAddress = walletAddress,
                        nonce = nonce,
                        signature = signature
                    )

                onSuccess(response)

            } catch (e: Exception) {

                when (e) {
                    is HttpException -> {
                        val errorBody =
                            e.response()?.errorBody()?.string()

                        onError(
                            errorBody
                                ?: "지갑 로그인 실패 (${e.code()})"
                        )
                    }

                    else -> {
                        onError(
                            e.message ?: "지갑 로그인 실패"
                        )
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

                val response =
                    walletRepository.issueWalletNonce(
                        walletAddress
                    )

                _walletNonce.value = response.nonce
                _walletSignMessage.value = response.message

                onSuccess(response)

            } catch (e: Exception) {
                _walletNonceError.value =
                    e.message ?: "nonce 발급 실패"

            } finally {
                _isLoadingNonce.value = false
            }
        }
    }

    suspend fun getWalletNonceOnce(
        walletAddress: String
    ): NonceResponse {
        return walletRepository.issueWalletNonce(
            walletAddress
        )
    }

    fun signMessageWithWallet(
        walletAddress: String,
        message: String,
        onSuccess: (WalletSignResult) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            onError("실제 지갑 서명 기능은 이미 기존 구조 사용")
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
