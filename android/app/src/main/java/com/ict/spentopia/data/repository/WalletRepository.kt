package com.ict.spentopia.data.repository // 이 파일이 속한 패키지 위치를 적음

// Flow import 입니다.
import com.ict.spentopia.data.remote.NonceResponse // NonceResponse 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLinkResponse // WalletLinkResponse 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLoginResponse // WalletLoginResponse 기능을 가져옴
import com.ict.spentopia.data.remote.WalletUnlinkResponse // WalletUnlinkResponse 기능을 가져옴
import kotlinx.coroutines.flow.Flow // Flow 기능을 가져옴

interface WalletRepository { // WalletRepository에서 꼭 만들어야 할 함수 규칙을 정함

    // 지갑 주소와 지갑 종류를 저장합니다.
    suspend fun saveWallet(address: String, provider: String) // 데이터를 저장하는 함수 시작
    /*  상세 설명:

 사용자가 로그인하거나 지갑을 연결했을 때, 그 지갑의 **주소(address)**와 어떤 서비스(메타마스크, 카이카스 등)인지 나타내는 **종류(provider)**를 넘겨받아 영구적으로 저장합니다.

   suspend 키워드가 붙은 이유는 저장 과정에서 I/O 작업(파일 쓰기 등)이 발생할 수 있어, 앱의 메인 화면이 멈추지 않게 비동기적으로 실행하기 위함입니다. */

    // 저장된 지갑 주소를 반환합니다.
    fun getWalletAddress(): Flow<String?> // 데이터를 불러오는 함수 시작
    /*   기능: 저장된 지갑 주소를 실시간으로 관찰합니다.
 상세 설명:
    Flow를 반환하기 때문에, 데이터가 변경될 때마다 앱 화면에 자동으로 바뀐 값을 알려줄 수 있습니다.
   반환 타입이 String?인 이유는 저장된 주소가 없을 경우 null을 반환할 수 있기 때문입니다*/

    // 저장된 지갑 종류를 반환합니다.
    fun getWalletProvider(): Flow<String?> // 데이터를 불러오는 함수 시작

    // 저장된 지갑 정보를 삭제합니다.
    suspend fun clearWallet() // clearWallet 함수를 선언함

    // 지갑 인증용 nonce를 발급받습니다.
    suspend fun issueWalletNonce(walletAddress: String): NonceResponse // issueWalletNonce 함수를 선언함
    /*
    상세 설명:

    백엔드 /auth/wallet/nonce API에 wallet_address를 보내서
    nonce와 message를 발급받습니다.

    - walletAddress: 실제 연결할 Solana 지갑 주소(Base58)
    - 반환값:
        nonce   -> 서버가 발급한 1회용 인증 코드
        message -> 실제 지갑으로 서명해야 하는 전체 메시지


    주의:
    안드로이드에서는 nonce 문자열만 서명하는 것이 아니라,
    서버가 내려준 message 전체를 지갑으로 서명해야 합니다.
    */


    // 로그인된 계정에 지갑을 연동합니다.
    suspend fun linkWallet( // linkWallet 함수를 선언함
        token: String, // 토큰을 받음
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletLinkResponse


    suspend fun walletLoginApp( // 로그인 기능을 실행하는 함수 시작
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletLoginResponse
    /*
    상세 설명:

    /wallet/link 보호 API를 호출하여
    현재 로그인된 사용자 계정에 지갑을 연동합니다.

    - token: 우리 앱 JWT
    - walletAddress: Solana 지갑 주소
    - nonce: nonce 발급 API에서 받은 값
    - signature: 서버가 내려준 message를 지갑으로 서명한 값
    */

    // 로그인된 계정에서 지갑 연동을 해제합니다.
    suspend fun unlinkWallet( // unlinkWallet 함수를 선언함
        token: String, // 토큰을 받음
        walletAddress: String, // 지갑 주소를 받음
        nonce: String, // 서명용 난수를 받음
        signature: String // 지갑 서명값을 받음
    ): WalletUnlinkResponse




    /*
    상세 설명:

    /wallet/unlink 보호 API를 호출하여
    현재 로그인된 사용자 계정에서 지갑 연동을 해제합니다.

    - token: 우리 앱 JWT
    - walletAddress: Solana 지갑 주소
    - nonce: nonce 발급 API에서 받은 값
    - signature: 서버가 내려준 message를 지갑으로 서명한 값
    */
}