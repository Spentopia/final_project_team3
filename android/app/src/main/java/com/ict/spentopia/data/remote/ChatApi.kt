package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버로 보낼 값을 표시하는 도구를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴

// 백엔드 /api/chat 요청 body입니다.
// 프론트 웹의 shared/api/chatApi.ts와 같은 구조입니다.
data class ChatRequest( // ChatRequest 데이터를 묶어둘 클래스 시작
    val message: String // 메시지를 저장함
)

// 백엔드 /api/chat 응답 body입니다.
// 백엔드는 AI 서버 응답을 response 필드로 내려줍니다.
data class ChatResponse( // ChatResponse 데이터를 묶어둘 클래스 시작
    val response: String // 서버 응답을 저장함
)

interface ChatApi { // ChatApi에서 꼭 만들어야 할 함수 규칙을 정함
    // 로그인 보호 API입니다.
    // Authorization: Bearer accessToken 헤더는 AuthInterceptor가 자동으로 붙입니다.
    @POST("/api/chat") // 서버에 데이터를 보내는 API 주소를 적음
    suspend fun sendMessage( // sendMessage 함수를 선언함
        @Body request: ChatRequest // 이 값을 서버 요청 본문에 넣는다는 표시
    ): ChatResponse
}
