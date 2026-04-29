package com.ict.spentopia.data.remote

import retrofit2.http.Body
import retrofit2.http.POST

// 백엔드 /api/chat 요청 body입니다.
// 프론트 웹의 shared/api/chatApi.ts와 같은 구조입니다.
data class ChatRequest(
    val message: String
)

// 백엔드 /api/chat 응답 body입니다.
// 백엔드는 AI 서버 응답을 response 필드로 내려줍니다.
data class ChatResponse(
    val response: String
)

interface ChatApi {
    // 로그인 보호 API입니다.
    // Authorization: Bearer accessToken 헤더는 AuthInterceptor가 자동으로 붙입니다.
    @POST("/api/chat")
    suspend fun sendMessage(
        @Body request: ChatRequest
    ): ChatResponse
}
