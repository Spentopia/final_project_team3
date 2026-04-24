// 현재 파일이 속한 패키지(폴더 경로 같은 개념)
// 보통 프로젝트 구조를 구분할 때 사용합니다.
package com.ict.spentopia.data.remote

// Retrofit에서 사용하는 어노테이션들을 import 합니다.
// @Body   : 요청 본문(body)에 데이터를 담아서 보낼 때 사용
// @DELETE : DELETE 요청을 보낼 때 사용
// @GET    : GET 요청을 보낼 때 사용
// @HTTP   : DELETE인데 body도 같이 보내고 싶을 때 사용
// @Header : 헤더 값을 직접 넣을 때 사용
// @POST   : POST 요청을 보낼 때 사용
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Header
import retrofit2.http.POST


// ------------------------------
// 1) 서버에 nonce 발급 요청할 때 보내는 데이터
// ------------------------------

// 백엔드 auth/handler.rs 기준으로
// /auth/wallet/nonce 는 body로 wallet_address를 받습니다.
//
// 요청 예:
// {
//   "wallet_address": "7xKXtg2CW87d..."
// }
data class NonceRequest(

    // nonce를 발급받을 대상 Solana 지갑 주소
    // Base58 형식의 공개키 문자열입니다.
    val wallet_address: String
)


// ------------------------------
// 2) 서버가 nonce를 발급해줄 때 받는 응답 데이터
// ------------------------------

// 백엔드는 nonce만 주는 것이 아니라,
// 실제 지갑이 서명해야 하는 message도 함께 내려줍니다.
//
// 응답 예:
// {
//   "nonce": "abc123...",
//   "message": "Spentopia 지갑 인증 ..."
// }
data class NonceResponse(

    // 서버가 내려주는 nonce 값
    // nonce는 보통 "한 번만 사용하는 임시 문자열" 같은 개념입니다.
    // 지갑 서명 요청 시 보안 목적으로 많이 사용합니다.
    val nonce: String,

    // 실제 지갑으로 서명해야 하는 인증 메시지입니다.
    // 안드로이드에서는 nonce 원문이 아니라
    // 이 message 전체를 지갑으로 서명해야 합니다.
    val message: String
)


// ------------------------------
// 3) 지갑 로그인 요청 보낼 때 사용하는 데이터
// ------------------------------

// 백엔드 /auth/wallet/login 또는 /auth/app/wallet/login 에
// 지갑 주소 + nonce + signature를 보낼 때 사용합니다.
//
// 주의:
// 실제 서명 대상은 nonce 자체가 아니라
// nonce 응답에 포함된 message 전체입니다.
data class WalletLoginRequest(

    // 로그인에 사용할 지갑 주소
    val wallet_address: String,

    // nonce 발급 API에서 받은 nonce
    val nonce: String,

    // 사용자가 지갑으로 message를 서명한 결과값
    // 백엔드에서는 Base58 형식을 기대합니다.
    val signature: String
)


// ------------------------------
// 4) 지갑 로그인 성공 후 서버가 주는 응답 데이터
// ------------------------------

// 백엔드 코드 기준으로 앱 로그인 응답은
// access_token, refresh_token, is_new_user 를 반환합니다.
//
// 응답 예:
// {
//   "access_token": "...",
//   "refresh_token": "...",
//   "is_new_user": false
// }
data class WalletLoginResponse(

    // 우리 앱 access token
    val access_token: String,

    // 우리 앱 refresh token
    val refresh_token: String,

    // 신규 유저 여부
    val is_new_user: Boolean
)


// ------------------------------
// 5) 지갑 연결(link) 요청 보낼 때 사용하는 데이터
// ------------------------------

// 서버에 지갑 연결 요청을 보낼 때 body에 담기는 값들입니다.
// 예:
// {
//   "wallet_address": "7xKXtg2CW87d...",
//   "nonce": "abc123",
//   "signature": "서명값"
// }
data class WalletLinkRequest(

    // 연결하려는 지갑 주소
    val wallet_address: String,

    // 서버에서 먼저 발급받은 nonce
    val nonce: String,

    // 사용자가 지갑으로 message에 서명한 결과값
    val signature: String
)


// ------------------------------
// 6) 지갑 연결(link) 성공 후 서버가 주는 응답 데이터
// ------------------------------

// 서버가 예를 들어
// {
//   "wallet_address": "7xKXtg2CW87d...",
//   "message": "지갑이 연동되었습니다."
// }
// 이런 식으로 보내주면 이 클래스로 받습니다.
data class WalletLinkResponse(

    // 서버가 확인한 지갑 주소
    val wallet_address: String,

    // 결과 메시지
    val message: String
)


// ------------------------------
// 7) 지갑 연결 해제(unlink) 요청 보낼 때 사용하는 데이터
// ------------------------------

// unlink도 link와 비슷하게
// 지갑 주소 + nonce + signature를 서버에 보낼 수 있습니다.
data class WalletUnlinkRequest(

    // 연결 해제할 지갑 주소
    val wallet_address: String,

    // unlink 요청용 nonce
    val nonce: String,

    // 사용자가 지갑으로 message를 서명한 값
    val signature: String
)


// ------------------------------
// 8) 지갑 연결 해제(unlink) 후 서버 응답 데이터
// ------------------------------

// 서버가 예:
// {
//   "message": "지갑 연동이 해제되었습니다."
// }
// 이런 식으로 보내준다고 가정합니다.
data class WalletUnlinkResponse(

    // 결과 메시지
    val message: String
)


// ------------------------------
// 9) 토큰 재발급 요청할 때 사용하는 데이터
// ------------------------------

// access_token이 만료되어 401 Unauthorized가 발생했을 때
// 저장된 refresh_token을 서버에 보내 새 access_token을 받기 위해 사용합니다.
//
// 요청 예:
// {
//   "refresh_token": "..."
// }
data class RefreshTokenRequest(

    // 로그인 성공 시 서버에서 받은 refresh token
    val refresh_token: String
)


// ------------------------------
// 10) 토큰 재발급 성공 후 서버가 주는 응답 데이터
// ------------------------------

// 서버가 예:
// {
//   "access_token": "...",
//   "refresh_token": "..."
// }
// 이런 식으로 새 토큰을 내려준다고 가정합니다.
data class RefreshTokenResponse(

    // 새로 발급받은 access token
    val access_token: String,

    // 새로 발급받은 refresh token
    val refresh_token: String
)


// ------------------------------
// 11) 내 정보 조회 응답 데이터
// ------------------------------

// 보호 API 테스트용 내 정보 응답 데이터입니다.
// 실제 백엔드 응답 필드가 다르면 여기를 서버 응답에 맞게 수정하면 됩니다.
//
// 응답 예:
// {
//   "id": 1,
//   "wallet_address": "7xKXtg2CW87d...",
//   "nickname": "user",
//   "email": "test@example.com"
// }
data class MeResponse(

    // 사용자 id
    val id: String? = null,

    // 연결된 지갑 주소
    val wallet_address: String? = null,

    // 사용자 닉네임
    val nickname: String? = null,

    // 사용자 이메일
    val email: String? = null
)


// ------------------------------
// 12) 실제 API 목록을 정의하는 인터페이스
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
    // issueWalletNonce(request)
    // -> wallet_address를 body에 담아 nonce 발급 요청
    //
    // : NonceResponse
    // -> 응답 결과를 NonceResponse 형태로 받겠다는 뜻
    suspend fun issueWalletNonce(

        // 요청 body에 wallet_address 전달
        @Body request: NonceRequest
    ): NonceResponse


    // @POST("/auth/wallet/login")
    // -> 웹/공통 지갑 로그인 API입니다.
    @POST("/auth/wallet/login")

    // walletLogin 함수는 지갑 로그인 API입니다.
    suspend fun walletLogin(

        // 요청 body에 wallet_address, nonce, signature 전달
        @Body request: WalletLoginRequest

        // 응답은 WalletLoginResponse로 받음
    ): WalletLoginResponse


    // @POST("/auth/app/wallet/login")
    // -> 앱 전용 지갑 로그인 API입니다.
    // 백엔드에서 app 요청 여부를 체크하는 경우 이 엔드포인트를 사용합니다.
    @POST("/auth/app/wallet/login")
    suspend fun walletLoginApp(
        @Header("X-Client-Type") clientType: String = "app",
        @Body request: WalletLoginRequest
    ): WalletLoginResponse


    // @POST("/auth/app/refresh")
    // -> 앱 전용 토큰 재발급 API입니다.
    // access_token이 만료되었을 때 refresh_token으로 새 토큰을 발급받습니다.
    @POST("/auth/app/refresh")
    suspend fun refreshToken(
        @Header("X-Client-Type") clientType: String = "app",
        @Body request: RefreshTokenRequest
    ): RefreshTokenResponse


    // @GET("/auth/me")
    // -> 로그인된 사용자의 내 정보 조회 API입니다.
    // -> Authorization 헤더는 AuthInterceptor가 자동으로 붙여줍니다.
    //
    // 주의:
    // 백엔드 실제 경로가 /auth/me가 아니면 이 경로를 수정해야 합니다.
    @GET("/me")
    suspend fun getMe(): MeResponse


    // @POST("/wallet/link")
    // -> POST 방식으로 /wallet/link 주소에 요청을 보냅니다.
    // -> 이미 로그인된 사용자의 계정에 지갑을 연동하는 보호 API입니다.
    @POST("/wallet/link")

    // linkWallet 함수는 지갑 연결 API입니다.
    suspend fun linkWallet(

        // @Header("Authorization")
        // -> HTTP 헤더의 Authorization 값으로 들어갑니다.
        // 예: "Bearer eyJhbGciOi..."
        //
        // authorization: String
        // -> 함수 호출할 때 실제 로그인 JWT를 넘겨줍니다.
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


    //api 로그아웃 추가
    @POST("/auth/app/logout")
    suspend fun logout(
        @Header("X-Client-Type") clientType: String = "app"
    )
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