package com.ict.spentopia.feature.auth // 이 파일이 속한 패키지 위치를 적음

import android.util.Log // 로그 찍는 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaBackpackConnector // MwaBackpackConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaPhantomConnector // MwaPhantomConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaSolflareConnector // MwaSolflareConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.SolanaWalletConnector // SolanaWalletConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.WalletConnectionResult // WalletConnectionResult 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.WalletSignResult // WalletSignResult 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // ActivityResultSender 기능을 가져옴

class WalletLoginCoordinator( // WalletLoginCoordinator 기능을 묶어둔 클래스 시작
    private val loginViewModel: LoginViewModel // loginViewModel 값을 저장함
) { // 이 블록 안의 내용이 시작됨

    // 1. 마지막으로 연결된 지갑 주소를 저장할 변수 추가
    private var lastWalletAddress: String? = null // 나중에 바뀔 수 있는 지갑 관련 값을 저장함
    private var lastWalletAuthToken: String? = null // 마지막 MWA 지갑 세션 토큰을 저장함

    private val phantomConnector: SolanaWalletConnector = MwaPhantomConnector() // phantomConnector 값을 저장함
    private val solflareConnector: SolanaWalletConnector = MwaSolflareConnector() // solflareConnector 값을 저장함
    private val backpackConnector: SolanaWalletConnector = MwaBackpackConnector() // backpackConnector 값을 저장함

    // 2. 외부에서 주소를 가져올 수 있는 getter 함수 추가
    fun getLastWalletAddress(): String? = lastWalletAddress // 데이터를 불러오는 함수 시작
    fun getLastWalletAuthToken(): String? = lastWalletAuthToken // 데이터를 불러오는 함수 시작

    suspend fun loginWithWallet( // 로그인 기능을 실행하는 함수 시작
        walletType: SolanaWalletType, // 지갑 관련 값을 받음
        walletActivityResultSender: ActivityResultSender, // 지갑 관련 값을 받음
        onSuccess: (String, String) -> Unit,
        onError: (String) -> Unit // 실패했을 때 실행할 함수를 받음
    ) { // 이 블록 안의 내용이 시작됨
        val connector = when (walletType) { // connector 값을 저장함
            SolanaWalletType.PHANTOM -> phantomConnector
            SolanaWalletType.SOLFLARE -> solflareConnector
            SolanaWalletType.BACKPACK -> backpackConnector
        }

        val connectResult = connector.connect(walletActivityResultSender) // connectResult 값을 저장함

        val walletAddress = when (connectResult) { // 지갑 주소를 저장함
            is WalletConnectionResult.Success -> { // 이 블록 안의 내용이 시작됨
                // 3. connect 성공 시점에 walletAddress 저장
                lastWalletAddress = connectResult.walletAddress // 지갑 관련 값을 정해줌
                lastWalletAuthToken = connectResult.authToken // 지갑 관련 값을 정해줌
                connectResult.walletAddress
            }
            is WalletConnectionResult.Failure -> { // 이 블록 안의 내용이 시작됨
                onError(connectResult.message) // 실패했을 때 넘겨받은 함수를 실행함
                return
            }
        }

        Log.d("Spentopia", "walletAddress=$walletAddress") // 개발자가 확인할 로그를 찍음

        val nonceResponse = try { // nonceResponse 값을 저장함
            loginViewModel.getWalletNonceOnce(walletAddress)
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            onError(e.message ?: "nonce 요청 실패") // 실패했을 때 넘겨받은 함수를 실행함
            return
        }

        Log.d("Spentopia", "nonce=${nonceResponse.nonce}") // 개발자가 확인할 로그를 찍음
        Log.d("Spentopia", "message=${nonceResponse.message}") // 개발자가 확인할 로그를 찍음

        val signResult = connector.signMessage( // signResult 값을 저장함
            walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
            message = nonceResponse.message.toByteArray() // 메시지를 정해줌
        )

        val signature = when (signResult) { // 지갑 서명값을 저장함
            is WalletSignResult.Success -> signResult.signature
            is WalletSignResult.Failure -> { // 이 블록 안의 내용이 시작됨
                onError(signResult.message) // 실패했을 때 넘겨받은 함수를 실행함
                return
            }
        }

        Log.d("Spentopia", "signature=$signature") // 개발자가 확인할 로그를 찍음

        loginViewModel.walletLoginApp(
            walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
            nonce = nonceResponse.nonce, // 서명용 난수를 정해줌
            signature = signature, // 지갑 서명값을 지갑 서명값에 넣음
            onSuccess = { response -> // 성공했을 때 실행할 함수를 정해줌
                onSuccess(response.access_token, response.refresh_token) // 성공했을 때 넘겨받은 함수를 실행함
            },
            onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                onError(message) // 실패했을 때 넘겨받은 함수를 실행함
            }
        )
    }
}
