package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버 요청 본문을 보내는 표시를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.PATCH // PATCH API 표시를 가져옴
import retrofit2.http.Path // 주소 중간에 들어갈 값 표시를 가져옴

data class NotificationResponse( // 서버에서 받은 알림 1개 데이터를 담는 클래스
    val id: String, // 알림 아이디를 저장함
    val user_id: String, // 알림을 받을 사용자 아이디를 저장함
    val notification_type: String, // 예산/보상/스트릭 같은 알림 종류를 저장함
    val message: String, // 화면에 보여줄 알림 내용을 저장함
    val is_read: Boolean, // 읽음 처리됐는지 여부를 저장함
    val created_at: String? // 알림 생성 시간을 저장함
)

data class MarkReadRequest( // 여러 알림을 읽음 처리할 때 보낼 요청 데이터
    val notification_ids: List<String> // 읽음 처리할 알림 아이디 목록을 저장함
)

interface NotificationApi { // 알림 관련 백엔드 API 규칙을 모아둠
    @GET("/api/notifications") // 내 알림 목록을 조회하는 API 주소
    suspend fun getNotifications(): List<NotificationResponse> // 서버에서 알림 목록을 불러오는 함수

    @PATCH("/api/notifications/{id}/read") // 알림 1개를 읽음 처리하는 API 주소
    suspend fun readNotification( // 알림 1개를 읽음 처리하는 함수
        @Path("id") notificationId: String // 주소의 {id} 자리에 넣을 알림 아이디
    )

    @PATCH("/api/notifications/read") // 선택한 여러 알림을 읽음 처리하는 API 주소
    suspend fun markRead( // 여러 알림을 읽음 처리하는 함수
        @Body request: MarkReadRequest // 서버에 보낼 알림 아이디 목록
    )

    @PATCH("/api/notifications/read-all") // 내 모든 알림을 읽음 처리하는 API 주소
    suspend fun readAllNotifications() // 모든 알림을 읽음 처리하는 함수
}
