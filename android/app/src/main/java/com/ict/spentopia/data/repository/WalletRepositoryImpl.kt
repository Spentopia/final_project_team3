package com.ict.spentopia.data.repository // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import androidx.datastore.preferences.core.edit // edit 기능을 가져옴
import androidx.datastore.preferences.core.stringPreferencesKey // stringPreferencesKey 기능을 가져옴
import com.ict.spentopia.data.local.walletDataStore // walletDataStore 기능을 가져옴
import com.ict.spentopia.data.remote.NonceRequest // NonceRequest 기능을 가져옴
import com.ict.spentopia.data.remote.NonceResponse // NonceResponse 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLinkRequest // WalletLinkRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLinkResponse // WalletLinkResponse 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLoginRequest // WalletLoginRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLoginResponse // WalletLoginResponse 기능을 가져옴
import com.ict.spentopia.data.remote.WalletUnlinkRequest // WalletUnlinkRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletUnlinkResponse // WalletUnlinkResponse 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴
import kotlinx.coroutines.flow.map // map 기능을 가져옴

class WalletRepositoryImpl( // WalletRepositoryImpl 기능을 묶어둔 클래스 시작
    private val context: Context // 현재 화면 정보를 저장함
) : WalletRepository { // 이 블록 안의 내용이 시작됨

    // 지갑 주소 key입니다.
    // DataStore에 어떤 값을 어떤 이름으로 저장할지 고정합니다.
    private val walletAddressKey = stringPreferencesKey("wallet_address") // 지갑 관련 값을 저장함

    // 지갑 종류 key입니다.
    private val walletProviderKey = stringPreferencesKey("wallet_provider") // 지갑 관련 값을 저장함

    // Retrofit 지갑 API 객체입니다.
    // 서버와 통신하는 실제 엔드포인트는 여기서만 사용합니다.
    private val walletApi = RetrofitClient.walletApi // 지갑 관련 값을 저장함

    // 지갑 정보를 앱 로컬 저장소에 기록합니다.
    override suspend fun saveWallet(address: String, provider: String) { // 데이터를 저장하는 함수 시작
        context.walletDataStore.edit { preferences ->
            preferences[walletAddressKey] = address // 지갑 관련 값을 정해줌
            preferences[walletProviderKey] = provider // 지갑 관련 값을 정해줌
        }
    }

    // 저장된 지갑 주소를 Flow로 읽어옵니다.
    override fun getWalletAddress(): Flow<String?> { // 데이터를 불러오는 함수 시작
        return context.walletDataStore.data.map { preferences -> // 이 값을 함수 결과로 돌려줌
            preferences[walletAddressKey]
        }
    }

    // 저장된 지갑 종류를 Flow로 읽어옵니다.
    override fun getWalletProvider(): Flow<String?> { // 데이터를 불러오는 함수 시작
        return context.walletDataStore.data.map { preferences -> // 이 값을 함수 결과로 돌려줌
            preferences[walletProviderKey]
        }
    }

    // 지갑 정보 삭제
    override suspend fun clearWallet() { // clearWallet 함수를 선언함
        context.walletDataStore.edit { preferences ->
            preferences.remove(walletAddressKey)
            preferences.remove(walletProviderKey)
        }
    }

    // 서버에서 서명용 nonce를 발급받습니다.
    override suspend fun issueWalletNonce(walletAddress: String): NonceResponse { // issueWalletNonce 함수를 선언함
        return walletApi.issueWalletNonce( // 이 값을 함수 결과로 돌려줌
            request = NonceRequest( // 서버 요청값을 정해줌
                wallet_address = walletAddress // 지갑 주소를 지갑 관련 값에 넣음
            )
        )
    }

    // 서버에 지갑 연결 요청을 보냅니다.
    override suspend fun linkWallet( // linkWallet 함수를 선언함
        token: String, // 토큰을 받음
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletLinkResponse { // 이 블록 안의 내용이 시작됨
        return walletApi.linkWallet( // 이 값을 함수 결과로 돌려줌
            authorization = "Bearer $token", // authorization 값을 정해줌
            request = WalletLinkRequest( // 서버 요청값을 정해줌
                wallet_address = walletAddress, // 지갑 주소를 지갑 관련 값에 넣음
                nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                signature = signature // 지갑 서명값을 지갑 서명값에 넣음
            )
        )
    }

    // 지갑 로그인용 검증 API를 호출합니다.
    override suspend fun walletLoginApp( // 로그인 기능을 실행하는 함수 시작
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletLoginResponse { // 이 블록 안의 내용이 시작됨
        return walletApi.walletLoginApp( // 이 값을 함수 결과로 돌려줌
            request = WalletLoginRequest( // 서버 요청값을 정해줌
                wallet_address = walletAddress, // 지갑 주소를 지갑 관련 값에 넣음
                nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                signature = signature // 지갑 서명값을 지갑 서명값에 넣음
            )
        )
    }

    // 서버에서 지갑 연결 해제를 처리합니다.
    override suspend fun unlinkWallet( // unlinkWallet 함수를 선언함
        token: String, // 토큰을 받음
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletUnlinkResponse { // 이 블록 안의 내용이 시작됨
        return walletApi.unlinkWallet( // 이 값을 함수 결과로 돌려줌
            authorization = "Bearer $token", // authorization 값을 정해줌
            request = WalletUnlinkRequest( // 서버 요청값을 정해줌
                wallet_address = walletAddress, // 지갑 주소를 지갑 관련 값에 넣음
                nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                signature = signature // 지갑 서명값을 지갑 서명값에 넣음
            )
        )
    }
}
