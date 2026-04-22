package com.ict.spentopia.feature.mypage

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
enum class MyPageTab {
    PROFILE, // 프로필 탭
    NOTIFICATION, // 알림 탭
    WALLET // 지갑 탭
}

// ------------------------------------------------------------
// 회원 정보 UI 모델
// ------------------------------------------------------------
// 프로필 탭에서 보여줄 기본 회원 정보를 담는다.
// ------------------------------------------------------------
data class MemberInfoUi(
    val name: String = "", // 이름
    val nickname: String = "", // 닉네임
    val email: String = "", // 이메일
    val phone: String = "" // 전화번호
)

// ------------------------------------------------------------
// 소셜 연동 정보 UI 모델
// ------------------------------------------------------------
// 카카오 / 네이버 / 구글 등의 연동 상태를 담는다.
// ------------------------------------------------------------
data class SocialAccountUi(
    val serviceName: String = "", // 소셜 서비스 이름
    val connected: Boolean = false // 연동 여부
)

// ------------------------------------------------------------
// 알림 설정 상태 UI 모델
// ------------------------------------------------------------
// 알림 탭에서 사용하는 토글 상태를 담는다.
// ------------------------------------------------------------
data class NotificationSettingUi(
    val budgetAlertEnabled: Boolean = true, // 예산 초과 알림 여부
    val rewardAlertEnabled: Boolean = true, // 보상 획득 알림 여부
    val streakReminderEnabled: Boolean = true, // 스트릭 리마인드 알림 여부
    val marketingAlertEnabled: Boolean = true // 마케팅 알림 여부
)

// ------------------------------------------------------------
// 지갑 상태 UI 모델
// ------------------------------------------------------------
// 지갑 연결 여부와 지갑 표시 정보를 담는다.
// ------------------------------------------------------------
data class WalletUi(
    val isConnected: Boolean = false, // 지갑 연결 여부
    val walletAddress: String = "", // 지갑 주소
    val walletProvider: String = "" // 지갑 종류
)

// ------------------------------------------------------------
// 프로필 요약 정보 UI 모델
// ------------------------------------------------------------
// 상단 파란 프로필 카드에 보여줄 요약 정보를 담는다.
// ------------------------------------------------------------
data class ProfileSummaryUi(
    val nickname: String = "", // 대표 닉네임
    val realName: String = "", // 실명
    val joinedDateText: String = "", // 가입일 텍스트
    val streakText: String = "", // 연속 기록 텍스트
    val sptBalanceText: String = "", // 보유 SPT 텍스트
    val avatarCountText: String = "", // 보유 아바타 수 텍스트
    val profileImageUri: String = "" // 프로필 이미지 uri
)

// ------------------------------------------------------------
// 마이페이지 전체 UI 상태
// ------------------------------------------------------------
// MyPageScreen 전체에서 필요한 상태를 하나로 묶음
// ------------------------------------------------------------
data class MyPageUiState(
    val selectedTab: MyPageTab = MyPageTab.PROFILE, // 현재 선택된 내부 탭
    val profileSummary: ProfileSummaryUi = ProfileSummaryUi(), // 상단 프로필 요약 카드 정보
    val memberInfo: MemberInfoUi = MemberInfoUi(), // 회원 정보
    val socialAccounts: List<SocialAccountUi> = emptyList(), // 소셜 연동 목록
    val notificationSetting: NotificationSettingUi = NotificationSettingUi(), // 알림 설정 상태
    val walletUi: WalletUi = WalletUi(), // 지갑 상태
    val isEditMode: Boolean = false // 수정 모드
)