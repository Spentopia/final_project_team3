package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

// ------------------------------------------------------------
// MyPageUiState.kt
// ------------------------------------------------------------
// 이 파일은 마이페이지 화면에서 사용하는 모든 상태를
// 한 곳에 모아서 관리하는 데이터 클래스 파일임
//
// 왜 필요하나?
// - 화면 상태를 한 객체로 묶으면 ViewModel 연결이 쉬워진다
// - 나중에 Supabase / DataStore / 지갑 상태 연결 시 확장하기 쉬움
// - MyPageScreen 안에 값들을 직접 박아두지 않고 상태로 관리할 수 있다.
// ------------------------------------------------------------

// ------------------------------------------------------------
// 마이페이지 내부 상단 탭 상태
// ------------------------------------------------------------
// 현재 마이페이지는
// - 프로필
// - 알림
// - 지갑
// 3개의 내부 탭을 가지고 있으므로 enum으로 분리함
// ------------------------------------------------------------
enum class MyPageTab { // MyPageTab에서 고를 수 있는 값들을 묶음
    PROFILE,
    NOTIFICATION,
    WALLET
}

// ------------------------------------------------------------
// 회원 정보 UI 모델
// ------------------------------------------------------------
// 프로필 탭에서 보여줄 기본 회원 정보를 담는다.
// ------------------------------------------------------------
data class MemberInfoUi( // MemberInfoUi 데이터를 묶어둘 클래스 시작
    val name: String = "", // name 값을 저장함
    val nickname: String = "", // nickname 값을 저장함
    val email: String = "", // 이메일을 저장함
    val phone: String = "" // phone 값을 저장함
)

// ------------------------------------------------------------
// 소셜 연동 정보 UI 모델
// ------------------------------------------------------------
// 카카오 / 네이버 / 구글 등의 연동 상태를 담는다.
// ------------------------------------------------------------
data class SocialAccountUi( // SocialAccountUi 데이터를 묶어둘 클래스 시작
    val serviceName: String = "", // serviceName 값을 저장함
    val connected: Boolean = false // connected 값을 저장함
)

// ------------------------------------------------------------
// 알림 설정 상태 UI 모델
// ------------------------------------------------------------
// 알림 탭에서 사용하는 토글 상태를 담는다.
// ------------------------------------------------------------
data class NotificationSettingUi( // NotificationSettingUi 데이터를 묶어둘 클래스 시작
    val budgetAlertEnabled: Boolean = true, // 예산 관련 값을 저장함
    val rewardAlertEnabled: Boolean = true, // rewardAlertEnabled 값을 저장함
    val streakReminderEnabled: Boolean = true, // streakReminderEnabled 값을 저장함
    val marketingAlertEnabled: Boolean = true // 마켓 관련 값을 저장함
)

// ------------------------------------------------------------
// 지갑 상태 UI 모델
// ------------------------------------------------------------
// 지갑 연결 여부와 지갑 표시 정보를 담는다.
// ------------------------------------------------------------
data class WalletUi( // WalletUi 데이터를 묶어둘 클래스 시작
    val isConnected: Boolean = false, // 연결됐는지 저장함
    val walletAddress: String = "", // 지갑 주소를 저장함
    val walletProvider: String = "" // 지갑 이름을 저장함
)

// ------------------------------------------------------------
// 프로필 요약 정보 UI 모델
// ------------------------------------------------------------
// 상단 파란 프로필 카드에 보여줄 요약 정보를 담는다.
// ------------------------------------------------------------
data class ProfileSummaryUi( // ProfileSummaryUi 데이터를 묶어둘 클래스 시작
    val nickname: String = "", // nickname 값을 저장함
    val realName: String = "", // realName 값을 저장함
    val joinedDateText: String = "", // joinedDateText 값을 저장함
    val streakText: String = "", // streakText 값을 저장함
    val sptBalanceText: String = "", // sptBalanceText 값을 저장함
    val avatarCountText: String = "", // 아바타 관련 값을 저장함
    val profileImageUri: String = "" // profileImageUri 값을 저장함
)

// ------------------------------------------------------------
// 마이페이지 전체 UI 상태
// ------------------------------------------------------------
// MyPageScreen 전체에서 필요한 상태를 하나로 묶음
// ------------------------------------------------------------
data class MyPageUiState( // MyPageUiState 데이터를 묶어둘 클래스 시작
    val selectedTab: MyPageTab = MyPageTab.PROFILE, // selectedTab 값을 저장함
    val profileSummary: ProfileSummaryUi = ProfileSummaryUi(), // profileSummary 값을 저장함
    val memberInfo: MemberInfoUi = MemberInfoUi(), // memberInfo 값을 저장함
    val socialAccounts: List<SocialAccountUi> = emptyList(), // socialAccounts 값을 저장함
    val notificationSetting: NotificationSettingUi = NotificationSettingUi(), // notificationSetting 값을 저장함
    val walletUi: WalletUi = WalletUi(), // 지갑 관련 값을 저장함
    val isEditMode: Boolean = false // 수정 모드인지 저장함
)