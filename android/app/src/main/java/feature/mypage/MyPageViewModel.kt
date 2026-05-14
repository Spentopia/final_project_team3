package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

// 마이페이지 더미 상태 VM임
// 실제 계정 API 붙으면 이쪽만 바꾸면 됨

import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.lifecycle.ViewModel // ViewModel 기능을 가져옴
import androidx.lifecycle.viewModelScope // ViewModel 코루틴 범위를 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // 서버 통신 도구를 가져옴
import com.ict.spentopia.data.remote.UpdateUserSettingsRequest // 알림 설정 수정 요청을 가져옴
import com.ict.spentopia.data.remote.UserSettingsResponse // 알림 설정 응답을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴

// 마이페이지 상태 관리
class MyPageViewModel : ViewModel() { // MyPageViewModel 기능을 묶어둔 클래스 시작

    // UI 상태 보관
    var uiState by mutableStateOf( // 화면에서 바뀔 화면 상태를 저장함
        MyPageUiState( // My Page Ui State 함수를 실행함
            profileSummary = ProfileSummaryUi( // profileSummary 값을 정해줌
                nickname = "길동이", // nickname 값을 정해줌
                realName = "홍길동", // realName 값을 정해줌
                joinedDateText = "2026년 4월 1일", // joinedDateText 값을 정해줌
                streakText = "7일 🔥", // streakText 값을 정해줌
                sptBalanceText = "1,250 SPT", // sptBalanceText 값을 정해줌
                avatarCountText = "15개" // 아바타 관련 값을 정해줌
            ),
            memberInfo = MemberInfoUi( // memberInfo 값을 정해줌
                name = "홍길동", // name 값을 정해줌
                nickname = "길동이", // nickname 값을 정해줌
                email = "hong@example.com", // 이메일을 정해줌
                phone = "010-1234-5678" // phone 값을 정해줌
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
        loadNotificationSettings() // 화면이 처음 만들어질 때 서버 알림 설정을 불러옴
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
            walletUi = uiState.walletUi.copy( // 지갑 관련 값을 정해줌
                isConnected = isConnected, // isConnected인지 여부를 isConnected인지 여부에 넣음
                walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                walletProvider = walletProvider // 지갑 이름을 지갑 이름에 넣음
            )
        )
    }

    fun toggleEditMode() { // toggleEditMode 함수를 선언함
        uiState = uiState.copy( // 화면 상태를 정해줌
            isEditMode = !uiState.isEditMode // isEditMode인지 여부를 정해줌
        )
    }

    // 회원 정보 수정
    fun updateMemberInfo( // 데이터를 수정하는 함수 시작
        name: String, // name 값을 받음
        nickname: String, // nickname 값을 받음
        email: String, // 이메일을 받음
        phone: String // phone 값을 받음
    ) { // 이 블록 안의 내용이 시작됨
        uiState = uiState.copy( // 화면 상태를 정해줌
            memberInfo = uiState.memberInfo.copy( // memberInfo 값을 정해줌
                name = name, // name 값을 name 값에 넣음
                nickname = nickname, // nickname 값을 nickname 값에 넣음
                email = email, // 이메일을 이메일에 넣음
                phone = phone // phone 값을 phone 값에 넣음
            ),
            profileSummary = uiState.profileSummary.copy( // profileSummary 값을 정해줌
                nickname = nickname, // nickname 값을 nickname 값에 넣음
                realName = name // name 값을 realName 값에 넣음
            )
        )
    }

    // 프로필 이미지 변경
    fun updateProfileImage(profileImageUri: String) { // 데이터를 수정하는 함수 시작
        uiState = uiState.copy( // 화면 상태를 정해줌
            profileSummary = uiState.profileSummary.copy( // profileSummary 값을 정해줌
                profileImageUri = profileImageUri // profileImageUri 값을 profileImageUri 값에 넣음
            )
        )
    }
}
