package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import retrofit2.http.Body // 서버 요청 본문을 보내는 표시를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.PATCH // PATCH API 표시를 가져옴

data class UserSettingsResponse( // 서버에서 받은 사용자 알림 설정 데이터를 담는 클래스
    val alert_budget: Boolean?, // 예산 알림 사용 여부를 저장함
    val alert_reward: Boolean?, // 보상 알림 사용 여부를 저장함
    val alert_streak: Boolean?, // 스트릭 알림 사용 여부를 저장함
    val notification_listener: Boolean? // 전체 알림 수신 사용 여부를 저장함
)

data class UpdateUserSettingsRequest( // 사용자 알림 설정을 바꿀 때 서버에 보낼 데이터
    val alert_budget: Boolean?, // 예산 알림 사용 여부를 보냄
    val alert_reward: Boolean?, // 보상 알림 사용 여부를 보냄
    val alert_streak: Boolean?, // 스트릭 알림 사용 여부를 보냄
    val notification_listener: Boolean? // 전체 알림 수신 사용 여부를 보냄
)

interface UserSettingsApi { // 사용자 설정 관련 백엔드 API 규칙을 모아둠
    @GET("/api/user/settings") // 내 알림 설정을 조회하는 API 주소
    suspend fun getSettings(): UserSettingsResponse // 서버에서 알림 설정을 불러오는 함수

    @PATCH("/api/user/settings") // 내 알림 설정을 수정하는 API 주소
    suspend fun updateSettings( // 알림 설정을 서버에 저장하는 함수
        @Body request: UpdateUserSettingsRequest // 서버에 보낼 알림 설정값
    ): UserSettingsResponse
}
