package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

// 마이페이지 더미 상태 VM임
// 실제 계정 API 붙으면 이쪽만 바꾸면 됨

import android.content.Context // 앱 컨텍스트를 가져옴
import android.net.Uri // 이미지 URI 타입을 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // ViewModel 코루틴 범위를 가져옴
import com.ict.spentopia.data.repository.SptBalanceRepository
import com.ict.spentopia.data.remote.ChangePasswordRequest // 비밀번호 변경 요청을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // 서버 통신 도구를 가져옴
import com.ict.spentopia.data.remote.UpdateUserProfileRequest // 프로필 수정 요청을 가져옴
import com.ict.spentopia.data.remote.UpdateUserSettingsRequest // 알림 설정 수정 요청을 가져옴
import com.ict.spentopia.data.remote.UserProfileResponse // 프로필 응답을 가져옴
import com.ict.spentopia.data.remote.UserSettingsResponse // 알림 설정 응답을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import okhttp3.MultipartBody // multipart 파일 파트를 가져옴
import okhttp3.RequestBody.Companion.toRequestBody // 바이트를 요청 본문으로 바꾸는 도구를 가져옴
import okhttp3.MediaType.Companion.toMediaTypeOrNull // content type을 미디어 타입으로 바꾸는 도구를 가져옴

// 마이페이지 상태 관리
class MyPageViewModel : ViewModel() { // MyPageViewModel 기능을 묶어둔 클래스 시작

    private var pendingProfileImageUri: String? = null // 저장 버튼에서 업로드할 새 프로필 이미지 URI를 저장함
    private val sptBalanceRepository = SptBalanceRepository()

    // UI 상태 보관
    var uiState by mutableStateOf( // 화면에서 바뀔 화면 상태를 저장함
        MyPageUiState( // My Page Ui State 함수를 실행함
            profileSummary = ProfileSummaryUi( // profileSummary 값을 정해줌
                nickname = "불러오는 중", // nickname 값을 정해줌
                realName = "", // realName 값을 정해줌
                joinedDateText = "-", // joinedDateText 값을 정해줌
                streakText = "-", // streakText 값을 정해줌
                sptBalanceText = "-", // sptBalanceText 값을 정해줌
                avatarCountText = "-", // 아바타 관련 값을 정해줌
                loginProviderText = "-" // 로그인 방식을 정해줌
            ),
            memberInfo = MemberInfoUi( // memberInfo 값을 정해줌
                name = "", // name 값을 정해줌
                nickname = "", // nickname 값을 정해줌
                email = "", // 이메일을 정해줌
                phone = "" // phone 값을 정해줌
            ),
            socialAccounts = listOf( // socialAccounts 값을 정해줌
                SocialAccountUi( // Social Account Ui 함수를 실행함
                    serviceName = "카카오", // serviceName 값을 정해줌
                    connected = true // true 값을 connected 값에 넣음
                ),
                SocialAccountUi( // Social Account Ui 함수를 실행함
                    serviceName = "구글", // serviceName 값을 정해줌
                    connected = true // true 값을 connected 값에 넣음
                )
            ),
            notificationSetting = NotificationSettingUi( // notificationSetting 값을 정해줌
                budgetAlertEnabled = true, // true 값을 예산 관련 값에 넣음
                rewardAlertEnabled = true, // true 값을 rewardAlertEnabled 값에 넣음
                streakReminderEnabled = true, // true 값을 streakReminderEnabled 값에 넣음
                marketingAlertEnabled = true // true 값을 마켓 관련 값에 넣음
            ),
            walletUi = WalletUi( // 지갑 관련 값을 정해줌
                isConnected = false, // false 값을 isConnected인지 여부에 넣음
                walletAddress = "", // 지갑 주소를 정해줌
                walletProvider = "" // 지갑 이름을 정해줌
            )
        )
    )
        private set

    init {
        loadProfile() // 화면이 처음 만들어질 때 서버 프로필을 불러옴
        loadNotificationSettings() // 화면이 처음 만들어질 때 서버 알림 설정을 불러옴
        loadAvatarCount() // 화면이 처음 만들어질 때 보유 NFT 개수를 불러옴
    }

    private fun loadProfile() { // 서버에서 프로필을 불러옴
        viewModelScope.launch {
            try {
                val profile = RetrofitClient.userSettingsApi.getProfile() // 서버 응답을 저장함
                applyProfile(profile) // 서버 응답을 화면 상태에 반영함
                loadSptBalance(profile.wallet_address.orEmpty())
                refreshProfileImage(profile.profile_image) // 프로필 이미지는 signed URL로 다시 불러옴
            } catch (_: Exception) {
                uiState = uiState.copy(
                    profileSummary = uiState.profileSummary.copy(
                        nickname = "프로필 정보 없음",
                        realName = "다시 시도해 주세요"
                    )
                )
            }
        }
    }

    private fun applyProfile(profile: UserProfileResponse) { // 서버 프로필을 화면 상태에 반영함
        val nickname = profile.nickname.orEmpty().ifBlank { "닉네임 미설정" }
        val email = profile.email.orEmpty()
        val phone = profile.phone.orEmpty()
        val intro = profile.introduction.orEmpty()
        val provider = profile.login_provider.orEmpty()
        val isSocialLogin = provider != "email"
        val walletAddress = profile.wallet_address.orEmpty()

        uiState = uiState.copy(
            profileSummary = uiState.profileSummary.copy(
                nickname = nickname,
                realName = if (intro.isNotBlank()) intro else email.ifBlank { provider.ifBlank { "사용자" } },
                joinedDateText = formatJoinedDate(profile.created_at),
                streakText = "${profile.current_streak}일 🔥",
                sptBalanceText = if (walletAddress.isBlank()) "-" else "...",
                avatarCountText = uiState.profileSummary.avatarCountText.ifBlank { "-" },
                loginProviderText = formatLoginProvider(provider, profile.google_connected),
                profileImageUri = uiState.profileSummary.profileImageUri
            ),
            memberInfo = uiState.memberInfo.copy(
                name = intro,
                nickname = nickname,
                email = email,
                phone = phone
            ),
            socialAccounts = buildSocialAccounts(provider, profile.google_connected),
            walletUi = uiState.walletUi.copy(
                isConnected = walletAddress.isNotBlank(),
                walletAddress = walletAddress
            ),
            isSocialLogin = isSocialLogin
        )
    }

    private fun loadSptBalance(walletAddress: String) {
        if (walletAddress.isBlank()) return
        viewModelScope.launch {
            val balanceText = try {
                "%,d SPT".format(sptBalanceRepository.getSptBalance(walletAddress))
            } catch (_: Exception) {
                "0 SPT"
            }
            if (uiState.walletUi.walletAddress == walletAddress) {
                uiState = uiState.copy(
                    profileSummary = uiState.profileSummary.copy(
                        sptBalanceText = balanceText
                    )
                )
            }
        }
    }

    private fun formatJoinedDate(createdAt: String): String { // 가입일 표시 문구를 만듦
        return createdAt.take(10).ifBlank { "-" }
    }

    private fun formatLoginProvider( // 로그인 방식 표시 문구를 만듦
        loginProvider: String,
        googleConnected: Boolean
    ): String {
        return if (loginProvider == "email" && googleConnected) {
            "EMAIL / GOOGLE"
        } else {
            loginProvider.ifBlank { "-" }.uppercase()
        }
    }

    private fun buildSocialAccounts( // 로그인 방식에 맞춰 소셜 계정 표시 목록을 만듦
        loginProvider: String,
        googleConnected: Boolean
    ): List<SocialAccountUi> {
        return listOf(
            SocialAccountUi(serviceName = "카카오", connected = loginProvider == "kakao"),
            SocialAccountUi(serviceName = "구글", connected = loginProvider == "google" || googleConnected),
            SocialAccountUi(serviceName = "이메일", connected = loginProvider == "email")
        )
    }

    private fun loadNotificationSettings() { // 서버에서 알림 설정을 불러옴
        viewModelScope.launch {
            try {
                applyNotificationSettings(RetrofitClient.userSettingsApi.getSettings()) // 서버 응답을 화면 상태에 반영함
            } catch (_: Exception) {
                // 설정 조회 실패 시 기존 기본값을 유지한다.
            }
        }
    }

    private fun loadAvatarCount() { // 서버에서 보유 NFT 개수를 불러옴
        viewModelScope.launch {
            try {
                val items = RetrofitClient.avatarApi.getUserItems()
                val nftCount = items.count { it.is_nft == true }
                uiState = uiState.copy(
                    profileSummary = uiState.profileSummary.copy(
                        avatarCountText = "${nftCount}개"
                    )
                )
            } catch (_: Exception) {
                uiState = uiState.copy(
                    profileSummary = uiState.profileSummary.copy(
                        avatarCountText = "-"
                    )
                )
            }
        }
    }

    private fun applyNotificationSettings(settings: UserSettingsResponse) { // 서버 응답을 화면 상태에 반영함
        uiState = uiState.copy(
            notificationSetting = uiState.notificationSetting.copy(
                budgetAlertEnabled = settings.alert_budget ?: true, // 예산 알림 값이 없으면 기본값 true를 사용함
                rewardAlertEnabled = settings.alert_reward ?: true, // 보상 알림 값이 없으면 기본값 true를 사용함
                streakReminderEnabled = settings.alert_streak ?: true, // 스트릭 알림 값이 없으면 기본값 true를 사용함
                marketingAlertEnabled = settings.notification_listener ?: true // 전체 알림 수신 값이 없으면 기본값 true를 사용함
            )
        )
    }

    private fun saveNotificationSettings() { // 현재 알림 설정을 서버에 저장함
        val setting = uiState.notificationSetting // 현재 화면에 선택된 토글 값을 가져옴
        viewModelScope.launch {
            try {
                val updated = RetrofitClient.userSettingsApi.updateSettings(
                    UpdateUserSettingsRequest(
                        alert_budget = setting.budgetAlertEnabled, // 예산 알림 사용 여부를 서버에 보냄
                        alert_reward = setting.rewardAlertEnabled, // 보상 알림 사용 여부를 서버에 보냄
                        alert_streak = setting.streakReminderEnabled, // 스트릭 알림 사용 여부를 서버에 보냄
                        alert_social = setting.marketingAlertEnabled, // 커뮤니티 알림 사용 여부를 서버에 보냄
                        notification_listener = setting.marketingAlertEnabled // 전체 알림 수신 여부를 서버에 보냄
                    )
                )
                applyNotificationSettings(updated) // 서버에 저장된 최종 값을 다시 화면에 반영함
            } catch (_: Exception) {
                // 실패 시 화면에서 바꾼 값은 유지하고 다음 변경 때 다시 저장을 시도한다.
            }
        }
    }

    // 상단 탭 변경
    fun onTabChange(tab: MyPageTab) { // onTabChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            selectedTab = tab // tab 값을 selectedTab 값에 넣음
        )
    }

    // 예산 초과 알림 변경
    fun onBudgetAlertChange(enabled: Boolean) { // onBudgetAlertChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            notificationSetting = uiState.notificationSetting.copy( // notificationSetting 값을 정해줌
                budgetAlertEnabled = enabled // enabled 값을 예산 관련 값에 넣음
            )
        )
        saveNotificationSettings() // 변경된 토글 값을 서버에 저장함
    }

    // 보상 알림 변경
    fun onRewardAlertChange(enabled: Boolean) { // onRewardAlertChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            notificationSetting = uiState.notificationSetting.copy( // notificationSetting 값을 정해줌
                rewardAlertEnabled = enabled // enabled 값을 rewardAlertEnabled 값에 넣음
            )
        )
        saveNotificationSettings() // 변경된 토글 값을 서버에 저장함
    }

    // 스트릭 알림 변경
    fun onStreakReminderChange(enabled: Boolean) { // onStreakReminderChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            notificationSetting = uiState.notificationSetting.copy( // notificationSetting 값을 정해줌
                streakReminderEnabled = enabled // enabled 값을 streakReminderEnabled 값에 넣음
            )
        )
        saveNotificationSettings() // 변경된 토글 값을 서버에 저장함
    }

    // 마케팅 알림 변경
    fun onMarketingAlertChange(enabled: Boolean) { // onMarketingAlertChange 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            notificationSetting = uiState.notificationSetting.copy( // notificationSetting 값을 정해줌
                marketingAlertEnabled = enabled // enabled 값을 마켓 관련 값에 넣음
            )
        )
        saveNotificationSettings() // 변경된 토글 값을 서버에 저장함
    }

    // 지갑 상태 반영
    fun updateWalletState( // 데이터를 수정하는 함수 시작
        isConnected: Boolean, // isConnected인지 여부를 받음
        walletAddress: String, // 지갑 주소를 받음
        walletProvider: String // 지갑 이름을 받음
    ) { // 이 블록 안의 내용이 시작됨
        uiState = uiState.copy( // 화면 상태를 정해줌
            profileSummary = uiState.profileSummary.copy(
                sptBalanceText = if (walletAddress.isBlank()) "-" else "..."
            ),
            walletUi = uiState.walletUi.copy( // 지갑 관련 값을 정해줌
                isConnected = isConnected, // isConnected인지 여부를 isConnected인지 여부에 넣음
                walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                walletProvider = walletProvider // 지갑 이름을 지갑 이름에 넣음
            )
        )
        loadSptBalance(walletAddress)
    }

    fun toggleEditMode(context: Context, onResult: (String) -> Unit = {}) { // toggleEditMode 함수를 선언함
        if (uiState.isEditMode) { // 수정 모드에서 누르면 서버에 저장함
            saveProfile(context, onResult)
            return
        }

        uiState = uiState.copy( // 화면 상태를 정해줌
            isEditMode = true // isEditMode인지 여부를 정해줌
        )
    }

    private fun saveProfile(context: Context, onResult: (String) -> Unit) { // 현재 회원 정보를 서버에 저장함
        val member = uiState.memberInfo
        viewModelScope.launch {
            try {
                val uploadedProfileImagePath = uploadPendingProfileImage(context) // 새 프로필 이미지가 있으면 먼저 업로드함
                val updated = RetrofitClient.userSettingsApi.updateProfile(
                    UpdateUserProfileRequest(
                        nickname = member.nickname.ifBlank { null },
                        phone = null,
                        introduction = member.name.ifBlank { null },
                        profile_image = uploadedProfileImagePath
                    )
                )
                applyProfile(updated)
                loadSptBalance(updated.wallet_address.orEmpty())
                refreshProfileImage(updated.profile_image)
                pendingProfileImageUri = null
                uiState = uiState.copy(isEditMode = false)
                onResult("프로필이 저장되었습니다")
            } catch (e: Exception) {
                onResult(e.message ?: "프로필 저장에 실패했습니다")
            }
        }
    }

    private suspend fun uploadPendingProfileImage(context: Context): String? { // 선택된 프로필 이미지를 업로드함
        val uriText = pendingProfileImageUri ?: return null
        val uri = Uri.parse(uriText)
        val contentType = context.contentResolver.getType(uri) ?: "image/png"
        val extension = when (contentType) {
            "image/png" -> "png"
            "image/jpeg", "image/jpg" -> "jpg"
            "image/webp" -> "webp"
            else -> throw IllegalArgumentException("png, jpg, webp 이미지만 업로드 가능합니다")
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            input.readBytes()
        } ?: throw IllegalArgumentException("프로필 이미지를 읽을 수 없습니다")
        val body = bytes.toRequestBody(contentType.toMediaTypeOrNull())
        val part = MultipartBody.Part.createFormData("file", "profile.$extension", body)
        return RetrofitClient.userSettingsApi.uploadProfileImage(part).path
    }

    private fun refreshProfileImage(profileImagePath: String?) { // 저장된 path를 화면에서 쓸 URL로 바꿈
        val path = profileImagePath.orEmpty()
        if (path.isBlank()) return

        viewModelScope.launch {
            try {
                val signedUrl = RetrofitClient.userSettingsApi.getProfileImageUrl(path).signed_url
                uiState = uiState.copy(
                    profileSummary = uiState.profileSummary.copy(
                        profileImageUri = signedUrl
                    )
                )
            } catch (_: Exception) {
                // signed URL 갱신 실패 시 기존 표시값을 유지한다.
            }
        }
    }

    fun changePassword( // 비밀번호 변경을 처리함
        currentPassword: String,
        newPassword: String,
        confirmPassword: String,
        onResult: (String) -> Unit
    ) {
        if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
            onResult("비밀번호를 모두 입력해 주세요")
            return
        }

        if (newPassword != confirmPassword) {
            onResult("새 비밀번호가 일치하지 않습니다")
            return
        }

        if (newPassword.length < 8) {
            onResult("새 비밀번호는 8자 이상 입력해 주세요")
            return
        }

        viewModelScope.launch {
            try {
                RetrofitClient.userSettingsApi.changePassword(
                    ChangePasswordRequest(
                        current_password = currentPassword,
                        new_password = newPassword
                    )
                )
                onResult("비밀번호가 변경되었습니다")
            } catch (_: Exception) {
                onResult("비밀번호 변경에 실패했습니다")
            }
        }
    }

    // 회원 정보 수정
    fun updateMemberInfo( // 데이터를 수정하는 함수 시작
        name: String, // name 값을 받음
        nickname: String, // nickname 값을 받음
        email: String, // 이메일을 받음
        phone: String // phone 값을 받음
    ) { // 이 블록 안의 내용이 시작됨
        val currentMember = uiState.memberInfo // 현재 회원 정보를 저장함
        uiState = uiState.copy( // 화면 상태를 정해줌
            memberInfo = uiState.memberInfo.copy( // memberInfo 값을 정해줌
                name = name, // name 값을 name 값에 넣음
                nickname = nickname, // nickname 값을 nickname 값에 넣음
                email = if (uiState.isSocialLogin) currentMember.email else email, // 소셜 계정은 이메일을 변경하지 않음
                phone = currentMember.phone // 전화번호는 앱 마이페이지에서 변경하지 않음
            ),
            profileSummary = uiState.profileSummary.copy( // profileSummary 값을 정해줌
                nickname = nickname, // nickname 값을 nickname 값에 넣음
                realName = name // name 값을 realName 값에 넣음
            )
        )
    }

    // 프로필 이미지 변경
    fun updateProfileImage(profileImageUri: String) { // 데이터를 수정하는 함수 시작
        pendingProfileImageUri = profileImageUri // 저장 버튼을 누를 때 업로드할 이미지 URI를 보관함
        uiState = uiState.copy( // 화면 상태를 정해줌
            profileSummary = uiState.profileSummary.copy( // profileSummary 값을 정해줌
                profileImageUri = profileImageUri // profileImageUri 값을 profileImageUri 값에 넣음
            )
        )
    }
}
