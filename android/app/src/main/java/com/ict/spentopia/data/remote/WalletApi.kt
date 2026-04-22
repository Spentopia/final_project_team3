// 현재 파일이 속한 패키지(폴더 경로 같은 개념)
// 보통 프로젝트 구조를 구분할 때 사용합니다.
package com.ict.spentopia.data.remote

// Retrofit에서 사용하는 어노테이션들을 import 합니다.
// @Body   : 요청 본문(body)에 데이터를 담아서 보낼 때 사용
// @DELETE : DELETE 요청을 보낼 때 사용
// @HTTP   : DELETE인데 body도 같이 보내고 싶을 때 사용
// @Header : 헤더 값을 직접 넣을 때 사용
// @POST   : POST 요청을 보낼 때 사용
import retrofit2.http.Body
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST


// ------------------------------
// 1) 서버가 nonce를 발급해줄 때 받는 응답 데이터
// ------------------------------

// data class는 "데이터를 담는 용도"의 클래스입니다.
// 서버가 JSON으로 { "nonce": "abc123" } 이런 식으로 보내주면
// 이 클래스로 받아서 사용할 수 있습니다.
data class NonceResponse(

    // 서버가 내려주는 nonce 값
    // nonce는 보통 "한 번만 사용하는 임시 문자열" 같은 개념입니다.
    // 지갑 서명 요청 시 보안 목적으로 많이 사용합니다.
    val nonce: String
)


// ------------------------------
// 2) 지갑 연결(link) 요청 보낼 때 사용하는 데이터
// ------------------------------

// 서버에 지갑 연결 요청을 보낼 때 body에 담기는 값들입니다.
// 예:
// {
//   "wallet_address": "0x123...",
//   "nonce": "abc123",
//   "signature": "서명값"
// }
data class WalletLinkRequest(

    // 연결하려는 지갑 주소
    val wallet_address: String,

    // 서버에서 먼저 발급받은 nonce
    val nonce: String,

    // 사용자가 지갑으로 nonce에 서명한 결과값
    val signature: String
)


// ------------------------------
// 3) 지갑 연결(link) 성공 후 서버가 주는 응답 데이터
// ------------------------------

// 서버가 예를 들어
// {
//   "wallet_address": "0x123...",
//   "message": "Wallet linked successfully"
// }
// 이런 식으로 보내주면 이 클래스로 받습니다.
data class WalletLinkResponse(

    // 서버가 확인한 지갑 주소
    val wallet_address: String,

    // 결과 메시지
    val message: String
)


// ------------------------------
// 4) 지갑 연결 해제(unlink) 요청 보낼 때 사용하는 데이터
// ------------------------------

// unlink도 link와 비슷하게
// 지갑 주소 + nonce + signature를 서버에 보낼 수 있습니다.
data class WalletUnlinkRequest(

    // 연결 해제할 지갑 주소
    val wallet_address: String,

    // unlink 요청용 nonce
    val nonce: String,

    // 사용자가 지갑으로 서명한 값
    val signature: String
)


// ------------------------------
// 5) 지갑 연결 해제(unlink) 후 서버 응답 데이터
// ------------------------------

// 서버가 예:
// {
//   "message": "Wallet unlinked successfully"
// }
// 이런 식으로 보내준다고 가정합니다.
data class WalletUnlinkResponse(

    // 결과 메시지
    val message: String
)


// ------------------------------
// 6) 실제 API 목록을 정의하는 인터페이스
// ------------------------------

// Retrofit은 이 interface를 보고
// "아, 이런 API들이 있구나" 하고 실제 네트워크 코드를 자동 생성해줍니다.
interface WalletApi {

    // @POST("/auth/wallet/nonce")
    // -> POST 방식으로 /auth/wallet/nonce 주소에 요청을 보냅니다.
    @POST("/auth/wallet/nonce")

    // suspend fun
    // -> 코루틴에서 호출하기 위한 함수입니다.
    // -> 네트워크 요청은 시간이 걸리므로 비동기 처리에 자주 사용합니다.
    //
    // issueWalletNonce()
    // -> 파라미터 없이 nonce 발급 요청
    //
    // : NonceResponse
    // -> 응답 결과를 NonceResponse 형태로 받겠다는 뜻
    suspend fun issueWalletNonce(): NonceResponse


    // @POST("/wallet/link")
    // -> POST 방식으로 /wallet/link 주소에 요청을 보냅니다.
    @POST("/wallet/link")

    // linkWallet 함수는 지갑 연결 API입니다.
    suspend fun linkWallet(

        // @Header("Authorization")
        // -> HTTP 헤더의 Authorization 값으로 들어갑니다.
        // 예: "Bearer eyJhbGciOi..."
        //
        // authorization: String
        // -> 함수 호출할 때 토큰 문자열을 넘겨줍니다.
        @Header("Authorization") authorization: String,

        // @Body
        // -> HTTP 요청 body에 request 객체를 JSON 형태로 넣어 보냅니다.
        //
        // request: WalletLinkRequest
        // -> wallet_address, nonce, signature가 들어있는 요청 데이터
        @Body request: WalletLinkRequest

        // 함수 결과는 WalletLinkResponse로 받습니다.
    ): WalletLinkResponse


    // 일반적인 @DELETE는 body를 넣기 어려운 경우가 있습니다.
    // 그래서 Retrofit에서는 body가 필요한 DELETE 요청에
    // @HTTP를 대신 사용하기도 합니다.
    //
    // method = "DELETE"
    // -> HTTP 메서드는 DELETE
    //
    // path = "/wallet/unlink"
    // -> 요청 주소는 /wallet/unlink
    //
    // hasBody = true
    // -> DELETE 요청이지만 body도 함께 보낸다는 뜻
    @HTTP(method = "DELETE", path = "/wallet/unlink", hasBody = true)

    // unlinkWallet 함수는 지갑 연결 해제 API입니다.
    suspend fun unlinkWallet(

        // Authorization 헤더에 인증 토큰 전달
        @Header("Authorization") authorization: String,

        // 요청 body에 wallet_address, nonce, signature 전달
        @Body request: WalletUnlinkRequest

        // 응답은 WalletUnlinkResponse로 받음
    ): WalletUnlinkResponse
}