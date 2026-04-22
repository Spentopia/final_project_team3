

package com.ict.spentopia.data.repository
////Flow impotr 입니다
import kotlinx.coroutines.flow.Flow

interface WalletRepository {

    // 지갑 주소와 지갑 종류를 저장합니다.
    suspend fun saveWallet(address: String, provider: String)
    /*  상세 설명:

 사용자가 로그인하거나 지갑을 연결했을 때, 그 지갑의 **주소(address)**와 어떤 서비스(메타마스크, 카이카스 등)인지 나타내는 **종류(provider)**를 넘겨받아 영구적으로 저장합니다.

   suspend 키워드가 붙은 이유는 저장 과정에서 I/O 작업(파일 쓰기 등)이 발생할 수 있어, 앱의 메인 화면이 멈추지 않게 비동기적으로 실행하기 위함입니다. */

    // 저장된 지갑 주소를 반환합니다.
    fun getWalletAddress(): Flow<String?>
   /*   기능: 저장된 지갑 주소를 실시간으로 관찰합니다.
 상세 설명:
    Flow를 반환하기 때문에, 데이터가 변경될 때마다 앱 화면에 자동으로 바뀐 값을 알려줄 수 있습니다. /
   반환 타입이 String?인 이유는 저장된 주소가 없을 경우 null을 반환할 수 있기 때문입니다*/

    // 저장된 지갑 종류를 반환합니다.
    fun getWalletProvider(): Flow<String?>

    // 저장된 지갑 정보를 삭제합니다.
    suspend fun clearWallet()

    suspend fun issueWalletNonce(): String

    suspend fun linkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    )

    suspend fun unlinkWallet(
        token: String,
        walletAddress: String,
        nonce: String,
        signature: String
    )
}
