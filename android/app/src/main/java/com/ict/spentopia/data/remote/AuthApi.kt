package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.Header // 서버 요청 헤더 표시 도구를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴

data class ExchangeTokenRequest( // ExchangeTokenRequest 데이터를 묶어둘 클래스 시작
    val access_token: String // 접근 토큰을 저장함
)

data class ExchangeTokenResponse( // ExchangeTokenResponse 데이터를 묶어둘 클래스 시작
    val access_token: String, // 접근 토큰을 저장함
    val refresh_token: String, // 갱신 토큰을 저장함
    val is_new_user: Boolean // 새 사용자 여부를 저장함
)

// 카카오 로그인 시작 응답
data class KakaoStartResponse( // KakaoStartResponse 데이터를 묶어둘 클래스 시작
    val auth_url: String, // 로그인 주소를 저장함
    val state: String // 상태값을 저장함
)

// 카카오 로그인 완료 요청
data class KakaoLoginRequest( // KakaoLoginRequest 데이터를 묶어둘 클래스 시작
    val code: String, // 인증 코드를 저장함
    val state: String // 상태값을 저장함
)

// 카카오 로그인 완료 응답
data class KakaoLoginResponse( // KakaoLoginResponse 데이터를 묶어둘 클래스 시작
    val access_token: String, // 접근 토큰을 저장함
    val refresh_token: String, // 갱신 토큰을 저장함
    val is_new_user: Boolean // 새 사용자 여부를 저장함
)

data class WebviewIssueRequest( // WebviewIssueRequest 데이터를 묶어둘 클래스 시작
    val redirect_path: String // 이동할 주소를 저장함
)

data class WebviewIssueResponse( // WebviewIssueResponse 데이터를 묶어둘 클래스 시작
    val webview_token: String, // 웹뷰 토큰을 저장함
    val expires_in: Int // 만료 시간을 저장함
)

data class HandoffRequest( // HandoffRequest 데이터를 묶어둘 클래스 시작
    val target_service: String = "unity" // 게임 로그인 코드를 사용할 대상을 저장함
)

data class HandoffResponse( // HandoffResponse 데이터를 묶어둘 클래스 시작
    val handoff_token: String, // 화면에 표시할 게임 로그인 코드를 저장함
    val expires_in: Int // 코드 만료 시간을 초 단위로 저장함
)

interface AuthApi { // AuthApi에서 꼭 만들어야 할 함수 규칙을 정함

    @POST("/auth/app/exchange") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun exchangeToken( // exchangeToken 함수를 선언함
        @Header("X-Client-Type") clientType: String = "app", // 이 값을 서버 요청 헤더에 넣는다는 표시
        @Body request: ExchangeTokenRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): ExchangeTokenResponse

    @POST("/auth/app/kakao/start") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun startKakaoLogin( // 로그인 기능을 실행하는 함수 시작
        @Header("X-Client-Type") clientType: String = "app" // 이 값을 서버 요청 헤더에 넣는다는 표시
    ): KakaoStartResponse

    @POST("/auth/app/kakao/login") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun finishKakaoLogin( // 로그인 기능을 실행하는 함수 시작
        @Header("X-Client-Type") clientType: String = "app", // 이 값을 서버 요청 헤더에 넣는다는 표시
        @Body request: KakaoLoginRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): KakaoLoginResponse

    @POST("/auth/webview/issue") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun issueWebviewToken( // issueWebviewToken 함수를 선언함
        @Header("X-Client-Type") clientType: String = "app", // 이 값을 서버 요청 헤더에 넣는다는 표시
        @Body request: WebviewIssueRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): WebviewIssueResponse

    @POST("/auth/handoff") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun issueGameLoginCode( // issueGameLoginCode 함수를 선언함
        @Header("X-Client-Type") clientType: String = "app", // 앱 요청임을 서버에 알려줌
        @Body request: HandoffRequest = HandoffRequest() // Unity 게임 로그인 코드 발급 요청
    ): HandoffResponse
}
