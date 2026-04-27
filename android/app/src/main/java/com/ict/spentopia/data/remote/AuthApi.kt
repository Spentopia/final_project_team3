package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

data class ExchangeTokenRequest(
    val access_token: String
)

data class ExchangeTokenResponse(
    val access_token: String,
    val refresh_token: String,
    val is_new_user: Boolean
)

// 카카오 로그인 시작 응답
data class KakaoStartResponse(
    val auth_url: String,
    val state: String
)

// 카카오 로그인 완료 요청
data class KakaoLoginRequest(
    val code: String,
    val state: String
)

// 카카오 로그인 완료 응답
data class KakaoLoginResponse(
    val access_token: String,
    val refresh_token: String,
    val is_new_user: Boolean
)

interface AuthApi {

    @POST("/auth/app/exchange")   // 앱 전용 로그인 토큰
    suspend fun exchangeToken(
        @Header("X-Client-Type") clientType: String = "app",
        @Body request: ExchangeTokenRequest
    ): ExchangeTokenResponse

    @POST("/auth/app/kakao/start")
    suspend fun startKakaoLogin(
        @Header("X-Client-Type") clientType: String = "app"
    ): KakaoStartResponse

    @POST("/auth/app/kakao/login")
    suspend fun finishKakaoLogin(
        @Header("X-Client-Type") clientType: String = "app",
        @Body request: KakaoLoginRequest
    ): KakaoLoginResponse
}