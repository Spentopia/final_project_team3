package com.ict.spentopia.feature.auth // 이 파일이 속한 패키지 위치를 적음

import android.app.Application // 앱 전체 정보 타입을 가져옴
import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.core.content.edit // edit 기능을 가져옴
import androidx.lifecycle.AndroidViewModel // AndroidViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // viewModelScope 기능을 가져옴
import com.ict.spentopia.data.remote.ExchangeTokenRequest // ExchangeTokenRequest 기능을 가져옴
import com.ict.spentopia.data.remote.KakaoLoginRequest // KakaoLoginRequest 기능을 가져옴
import com.ict.spentopia.data.remote.KakaoStartResponse // KakaoStartResponse 기능을 가져옴
import com.ict.spentopia.data.remote.NonceResponse // NonceResponse 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.SupabaseClient // SupabaseClient 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLoginResponse // WalletLoginResponse 기능을 가져옴
import com.ict.spentopia.data.repository.WalletRepository // WalletRepository 기능을 가져옴
import com.ict.spentopia.data.repository.WalletRepositoryImpl // WalletRepositoryImpl 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.WalletSignResult // WalletSignResult 기능을 가져옴
import io.github.jan.supabase.auth.auth // auth 기능을 가져옴
import io.github.jan.supabase.auth.providers.Google // Google 기능을 가져옴
import io.github.jan.supabase.auth.providers.builtin.Email // Email 기능을 가져옴
import io.github.jan.supabase.auth.providers.builtin.IDToken // IDToken 기능을 가져옴
import kotlinx.coroutines.flow.MutableStateFlow // 바뀌는 상태값 도구를 가져옴
import kotlinx.coroutines.flow.StateFlow // 읽기 전용 상태값 도구를 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import retrofit2.HttpException // 서버 오류 타입을 가져옴

class LoginViewModel( // LoginViewModel 기능을 묶어둔 클래스 시작
    application: Application // 앱 전체 정보를 받음
) : AndroidViewModel(application) { // 이 블록 안의 내용이 시작됨

    // 로그인 로직 담당 VM임
    // 인증/토큰교환/저장 분리용
    private val walletRepository: WalletRepository = // 지갑 관련 값을 저장함
        WalletRepositoryImpl(application) // 지갑 관련 함수를 실행함

    private val _isSavingWallet = MutableStateFlow(false) // 화면에서 바뀔 지갑 관련 값을 저장함
    val isSavingWallet: StateFlow<Boolean> = _isSavingWallet // 화면에서 지갑 관련 값을 읽을 수 있게 열어둠

    private val _walletSaveError = MutableStateFlow<String?>(null) // 화면에서 바뀔 지갑 관련 값을 저장함
    val walletSaveError: StateFlow<String?> = _walletSaveError // 화면에서 지갑 관련 값을 읽을 수 있게 열어둠

    private val _isLoadingNonce = MutableStateFlow(false) // 화면에서 바뀔 로딩 상태를 저장함
    val isLoadingNonce: StateFlow<Boolean> = _isLoadingNonce // 화면에서 로딩 상태를 읽을 수 있게 열어둠

    private val _walletNonce = MutableStateFlow<String?>(null) // 화면에서 바뀔 지갑 관련 값을 저장함
    val walletNonce: StateFlow<String?> = _walletNonce // 화면에서 지갑 관련 값을 읽을 수 있게 열어둠

    private val _walletSignMessage = MutableStateFlow<String?>(null) // 화면에서 바뀔 지갑 관련 값을 저장함
    val walletSignMessage: StateFlow<String?> = _walletSignMessage // 화면에서 지갑 관련 값을 읽을 수 있게 열어둠

    private val _walletNonceError = MutableStateFlow<String?>(null) // 화면에서 바뀔 지갑 관련 값을 저장함
    val walletNonceError: StateFlow<String?> = _walletNonceError // 화면에서 지갑 관련 값을 읽을 수 있게 열어둠

    // 이메일 로그인 흐름
    // Supabase -> 우리 서버 -> 토큰 저장
    fun emailLogin( // 로그인 기능을 실행하는 함수 시작
        email: String, // 이메일을 받음
        password: String, // 비밀번호를 받음
        onSuccess: () -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                // 1차 Supabase 로그인
                SupabaseClient.client.auth.signInWith(Email) { // 이 블록 안의 내용이 시작됨
                    this.email = email // 사용자가 입력한 이메일을 로그인 요청에 넣음
                    this.password = password // 사용자가 입력한 비밀번호를 로그인 요청에 넣음
                }

                // Supabase 세션 토큰 꺼냄
                val session = // 로그인 세션을 저장함
                    SupabaseClient.client.auth.currentSessionOrNull()

                val supabaseToken = // 토큰 값을 저장함
                    session?.accessToken
                        ?: throw Exception("Supabase 토큰 없음")

                // 2차 우리 서버 JWT 교환
                val response = RetrofitClient.authApi.exchangeToken( // 서버 응답을 저장함
                    request = ExchangeTokenRequest( // 서버 요청값을 정해줌
                        access_token = supabaseToken // 받아온 토큰을 접근 토큰 값에 넣음
                    )
                )

                // 로그인 유지 토큰 저장
                val prefs = getApplication<Application>() // 토큰을 저장할 간단 저장소를 가져옴
                    .getSharedPreferences(
                        "auth_prefs",
                        Context.MODE_PRIVATE
                    )

                prefs.edit { // 이 블록 안의 내용이 시작됨
                    putString("access_token", response.access_token) // 문자 값을 간단 저장소에 저장함
                    putString("refresh_token", response.refresh_token) // 문자 값을 간단 저장소에 저장함
                }

                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "emailLogin 실패", e) // 개발자가 확인할 로그를 찍음
                onError(e.message ?: "로그인 실패") // 실패했을 때 넘겨받은 함수를 실행함
            }
        }
    }

    // 구글 로그인도 같은 구조임
    // 외부 인증 후 우리 서버 토큰으로 바꿈
    fun googleLogin( // 로그인 기능을 실행하는 함수 시작
        idToken: String, // 구글 로그인 토큰을 받음
        onSuccess: () -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                if (idToken.isBlank()) { // 조건이 맞는지 확인함
                    onError("Google idToken이 없습니다.") // 실패했을 때 넘겨받은 함수를 실행함
                    return@launch
                }

                // Google idToken 전달함
                SupabaseClient.client.auth.signInWith(IDToken) { // 이 블록 안의 내용이 시작됨
                    this.idToken = idToken // 사용자가 입력한 구글 로그인 토큰을 로그인 요청에 넣음
                    provider = Google // Google 값을 provider 값에 넣음
                }

                val session = // 로그인 세션을 저장함
                    SupabaseClient.client.auth.currentSessionOrNull()

                val supabaseToken = // 토큰 값을 저장함
                    session?.accessToken
                        ?: throw Exception("Supabase 토큰 없음")

                val response = RetrofitClient.authApi.exchangeToken( // 서버 응답을 저장함
                    request = ExchangeTokenRequest( // 서버 요청값을 정해줌
                        access_token = supabaseToken // 받아온 토큰을 접근 토큰 값에 넣음
                    )
                )

                val prefs = getApplication<Application>() // 토큰을 저장할 간단 저장소를 가져옴
                    .getSharedPreferences(
                        "auth_prefs",
                        Context.MODE_PRIVATE
                    )

                prefs.edit { // 이 블록 안의 내용이 시작됨
                    putString("access_token", response.access_token) // 문자 값을 간단 저장소에 저장함
                    putString("refresh_token", response.refresh_token) // 문자 값을 간단 저장소에 저장함
                }

                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "구글 로그인 실패", e) // 개발자가 확인할 로그를 찍음
                onError(e.message ?: "구글 로그인 실패") // 실패했을 때 넘겨받은 함수를 실행함
            }
        }
    }

    // 카카오 시작 URL 먼저 받음
    fun getKakaoLoginUrl( // 로그인 기능을 실행하는 함수 시작
        onSuccess: (KakaoStartResponse) -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val response = // 서버 응답을 저장함
                    RetrofitClient.authApi.startKakaoLogin() // 서버 통신 도구를 설정함
                onSuccess(response) // 성공했을 때 넘겨받은 함수를 실행함
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "카카오 시작 실패", e) // 개발자가 확인할 로그를 찍음
                onError(e.message ?: "카카오 로그인 시작 실패") // 실패했을 때 넘겨받은 함수를 실행함
            }
        }
    }

    // 카카오 code/state로 최종 토큰 받음
    fun kakaoLogin( // 로그인 기능을 실행하는 함수 시작
        code: String, // 인증 코드를 받음
        state: String, // 상태값을 받음
        onSuccess: () -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val response = // 서버 응답을 저장함
                    RetrofitClient.authApi.finishKakaoLogin( // 서버 통신 도구를 설정함
                        request = KakaoLoginRequest( // 서버 요청값을 정해줌
                            code = code, // 인증 코드를 인증 코드에 넣음
                            state = state // 상태값을 상태값에 넣음
                        )
                    )
                val prefs = getApplication<Application>() // 토큰을 저장할 간단 저장소를 가져옴
                    .getSharedPreferences(
                        "auth_prefs",
                        Context.MODE_PRIVATE
                    )
                prefs.edit { // 이 블록 안의 내용이 시작됨
                    putString("access_token", response.access_token) // 문자 값을 간단 저장소에 저장함
                    putString("refresh_token", response.refresh_token) // 문자 값을 간단 저장소에 저장함
                }
                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "카카오 로그인 실패", e) // 개발자가 확인할 로그를 찍음
                onError(e.message ?: "카카오 로그인 실패") // 실패했을 때 넘겨받은 함수를 실행함
            }
        }
    }

    // 지갑 세션은 일반 토큰과 별도임
    fun saveWalletSession( // 데이터를 저장하는 함수 시작
        walletAddress: String, // 지갑 주소를 받음
        walletProvider: String, // 지갑 이름을 받음
        onSuccess: () -> Unit = {} // 성공했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                _isSavingWallet.value = true // 지갑 관련 값을 정해줌
                _walletSaveError.value = null // 지갑 관련 값을 정해줌

                walletRepository.saveWallet(
                    address = walletAddress, // 지갑 주소를 address 값에 넣음
                    provider = walletProvider // 지갑 이름을 provider 값에 넣음
                )

                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _walletSaveError.value = // 지갑 관련 값을 정해줌
                    e.message ?: "지갑 정보 저장 실패"

            } finally { // 이 블록 안의 내용이 시작됨
                _isSavingWallet.value = false // 지갑 관련 값을 정해줌
            }
        }
    }

    // 지갑 로그인은 nonce -> 서명 -> 검증 순서임
    fun walletLoginApp( // 로그인 기능을 실행하는 함수 시작
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String, // 지갑 서명값을 받음
        onSuccess: (WalletLoginResponse) -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val response = // 서버 응답을 저장함
                    walletRepository.walletLoginApp(
                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                        nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                        signature = signature // 지갑 서명값을 지갑 서명값에 넣음
                    )

                onSuccess(response) // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨

                when (e) { // 값 종류에 따라 실행할 코드를 나눔
                    is HttpException -> { // 이 블록 안의 내용이 시작됨
                        val errorBody = // 오류 내용을 저장함
                            e.response()?.errorBody()?.string()

                        onError( // 실패했을 때 넘겨받은 함수를 실행함
                            errorBody
                                ?: "지갑 로그인 실패 (${e.code()})"
                        )
                    }

                    else -> { // 위 조건이 아니면 이쪽을 실행함
                        onError( // 실패했을 때 넘겨받은 함수를 실행함
                            e.message ?: "지갑 로그인 실패"
                        )
                    }
                }
            }
        }
    }

    fun issueWalletNonce( // issueWalletNonce 함수를 선언함
        walletAddress: String, // 지갑 주소를 받음
        onSuccess: (NonceResponse) -> Unit = {} // 성공했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                _isLoadingNonce.value = true // 로딩 상태를 정해줌
                _walletNonceError.value = null // 지갑 관련 값을 정해줌

                val response = // 서버 응답을 저장함
                    walletRepository.issueWalletNonce(
                        walletAddress
                    )

                _walletNonce.value = response.nonce // 지갑 관련 값을 정해줌
                _walletSignMessage.value = response.message // 지갑 관련 값을 정해줌

                onSuccess(response) // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                _walletNonceError.value = // 지갑 관련 값을 정해줌
                    e.message ?: "nonce 발급 실패"

            } finally { // 이 블록 안의 내용이 시작됨
                _isLoadingNonce.value = false // 로딩 상태를 정해줌
            }
        }
    }

    suspend fun getWalletNonceOnce( // 데이터를 불러오는 함수 시작
        walletAddress: String // 지갑 주소를 받음
    ): NonceResponse { // 이 블록 안의 내용이 시작됨
        return walletRepository.issueWalletNonce( // 이 값을 함수 결과로 돌려줌
            walletAddress
        )
    }

    fun signMessageWithWallet( // signMessageWithWallet 함수를 선언함
        walletAddress: String, // 지갑 주소를 받음
        message: String, // 메시지를 받음
        onSuccess: (WalletSignResult) -> Unit, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            onError("실제 지갑 서명 기능은 이미 기존 구조 사용") // 실패했을 때 넘겨받은 함수를 실행함
        }
    }

    fun linkWalletToServer( // linkWalletToServer 함수를 선언함
        token: String, // 토큰을 받음
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String, // 지갑 서명값을 받음
        provider: String, // provider 값을 받음
        onSuccess: () -> Unit = {}, // 성공했을 때 실행할 함수를 받음
        onError: (String) -> Unit = {} // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        viewModelScope.launch { // 화면이 멈추지 않게 코루틴으로 실행함
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                walletRepository.linkWallet(
                    token = token, // 토큰을 토큰에 넣음
                    walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                    nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                    signature = signature // 지갑 서명값을 지갑 서명값에 넣음
                )

                walletRepository.saveWallet(
                    address = walletAddress, // 지갑 주소를 address 값에 넣음
                    provider = provider // provider 값을 provider 값에 넣음
                )

                onSuccess() // 성공했을 때 넘겨받은 함수를 실행함

            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                onError(e.message ?: "지갑 연동 실패") // 실패했을 때 넘겨받은 함수를 실행함
            }
        }
    }
}
