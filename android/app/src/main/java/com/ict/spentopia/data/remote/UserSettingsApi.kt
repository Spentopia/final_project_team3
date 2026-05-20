package com.ict.spentopia.data.remote // 이 파일이 속한 패키지 위치를 적음

import okhttp3.MultipartBody // multipart 파일 파트를 가져옴
import retrofit2.http.Body // 서버 요청 본문을 보내는 표시를 가져옴
import retrofit2.http.GET // GET API 표시를 가져옴
import retrofit2.http.Multipart // multipart 요청 표시를 가져옴
import retrofit2.http.PATCH // PATCH API 표시를 가져옴
import retrofit2.http.POST // POST API 표시를 가져옴
import retrofit2.http.Query // query string 표시를 가져옴
import retrofit2.http.Part // multipart 파트 표시를 가져옴

data class UserProfileResponse( // 서버에서 받은 사용자 프로필 데이터를 담는 클래스
    val id: String, // 사용자 id를 저장함
    val email: String?, // 이메일을 저장함
    val nickname: String?, // 닉네임을 저장함
    val phone: String?, // 전화번호를 저장함
    val introduction: String?, // 한 줄 소개를 저장함
    val profile_image: String?, // 프로필 이미지 path를 저장함
    val login_provider: String?, // 로그인 방식을 저장함
    val google_connected: Boolean, // 구글 연결 여부를 저장함
    val wallet_address: String?, // 지갑 주소를 저장함
    val role_type: String, // 사용자 역할을 저장함
    val profile_completed: Boolean, // 프로필 완성 여부를 저장함
    val spt_balance: Int, // SPT 잔액을 저장함
    val created_at: String, // 가입일을 저장함
    val current_streak: Int // 연속 기록을 저장함
)

data class UpdateUserProfileRequest( // 사용자 프로필을 바꿀 때 서버에 보낼 데이터
    val nickname: String?, // 닉네임을 보냄
    val phone: String?, // 전화번호를 보냄
    val introduction: String?, // 한 줄 소개를 보냄
    val profile_image: String? = null // 프로필 이미지 path를 보냄
)

data class UploadProfileImageResponse( // 프로필 이미지 업로드 응답 데이터
    val path: String // 저장된 프로필 이미지 path를 저장함
)

data class ProfileImageUrlResponse( // 프로필 이미지 signed URL 응답 데이터
    val signed_url: String // 접근 가능한 이미지 URL을 저장함
)

data class ChangePasswordRequest( // 비밀번호를 바꿀 때 서버에 보낼 데이터
    val current_password: String, // 현재 비밀번호를 보냄
    val new_password: String // 새 비밀번호를 보냄
)

data class UserSettingsResponse( // 서버에서 받은 사용자 알림 설정 데이터를 담는 클래스
    val alert_budget: Boolean?, // 예산 알림 사용 여부를 저장함
    val alert_reward: Boolean?, // 보상 알림 사용 여부를 저장함
    val alert_streak: Boolean?, // 스트릭 알림 사용 여부를 저장함
    val alert_social: Boolean?, // 커뮤니티 알림 사용 여부를 저장함
    val notification_listener: Boolean? // 전체 알림 수신 사용 여부를 저장함
)

data class UpdateUserSettingsRequest( // 사용자 알림 설정을 바꿀 때 서버에 보낼 데이터
    val alert_budget: Boolean?, // 예산 알림 사용 여부를 보냄
    val alert_reward: Boolean?, // 보상 알림 사용 여부를 보냄
    val alert_streak: Boolean?, // 스트릭 알림 사용 여부를 보냄
    val alert_social: Boolean? = null, // 커뮤니티 알림 사용 여부를 보냄
    val notification_listener: Boolean? // 전체 알림 수신 사용 여부를 보냄
)

interface UserSettingsApi { // 사용자 설정 관련 백엔드 API 규칙을 모아둠
    @GET("/api/user/profile") // 내 프로필을 조회하는 API 주소
    suspend fun getProfile(): UserProfileResponse // 서버에서 프로필을 불러오는 함수

    @PATCH("/api/user/profile") // 내 프로필을 수정하는 API 주소
    suspend fun updateProfile( // 프로필을 서버에 저장하는 함수
        @Body request: UpdateUserProfileRequest // 서버에 보낼 프로필 값
    ): UserProfileResponse

    @Multipart // multipart 요청임을 표시함
    @POST("/profile/image/upload") // 프로필 이미지를 업로드하는 API 주소
    suspend fun uploadProfileImage( // 프로필 이미지를 서버에 업로드하는 함수
        @Part file: MultipartBody.Part // 서버에 보낼 이미지 파일
    ): UploadProfileImageResponse

    @GET("/profile/image-url") // 프로필 이미지 signed URL을 가져오는 API 주소
    suspend fun getProfileImageUrl( // 프로필 이미지 접근 URL을 가져오는 함수
        @Query("path") path: String // 서버에 보낼 이미지 path
    ): ProfileImageUrlResponse

    @PATCH("/api/user/password") // 비밀번호를 변경하는 API 주소
    suspend fun changePassword( // 비밀번호 변경을 서버에 요청하는 함수
        @Body request: ChangePasswordRequest // 서버에 보낼 비밀번호 값
    )

    @GET("/api/user/settings") // 내 알림 설정을 조회하는 API 주소
    suspend fun getSettings(): UserSettingsResponse // 서버에서 알림 설정을 불러오는 함수

    @PATCH("/api/user/settings") // 내 알림 설정을 수정하는 API 주소
    suspend fun updateSettings( // 알림 설정을 서버에 저장하는 함수
        @Body request: UpdateUserSettingsRequest // 서버에 보낼 알림 설정값
    ): UserSettingsResponse
}
