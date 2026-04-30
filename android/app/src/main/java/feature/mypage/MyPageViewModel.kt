package com.ict.spentopia.feature.mypage // 마이페이지 패키지

// 마이페이지 더미 상태 VM임
// 실제 계정 API 붙으면 이쪽만 바꾸면 됨

import androidx.compose.runtime.getValue // 상태 읽기
import androidx.compose.runtime.mutableStateOf // 상태 저장
import androidx.compose.runtime.setValue // 상태 변경
import androidx.lifecycle.ViewModel // ViewModel 사용

// 마이페이지 상태 관리
class MyPageViewModel : ViewModel() { // 마이페이지 ViewModel 시작

    // UI 상태 보관
    var uiState by mutableStateOf( // 화면 전체 상태 저장
        MyPageUiState( // 초기 상태 세팅
            profileSummary = ProfileSummaryUi( // 상단 프로필 요약 데이터
                nickname = "길동이", // 닉네임 더미값
                realName = "홍길동", // 실명 더미값
                joinedDateText = "2026년 4월 1일", // 가입일 더미값
                streakText = "7일 🔥", // 연속 기록 더미값
                sptBalanceText = "1,250 SPT", // 보유 SPT 더미값
                avatarCountText = "15개" // 보유 아바타 수 더미값
            ), // 프로필 요약 끝
            memberInfo = MemberInfoUi( // 회원 정보 데이터
                name = "홍길동", // 이름 더미값
                nickname = "길동이", // 닉네임 더미값
                email = "hong@example.com", // 이메일 더미값
                phone = "010-1234-5678" // 전화번호 더미값
            ), // 회원 정보 끝
            socialAccounts = listOf( // 소셜 연동 목록 시작
                SocialAccountUi( // 카카오 연동 상태
                    serviceName = "카카오", // 서비스 이름
                    connected = true // 연동 여부
                ), // 카카오 끝
                SocialAccountUi( // 구글 연동 상태
                    serviceName = "구글", // 서비스 이름
                    connected = true // 연동 여부
                ) // 구글 끝
            ), // 소셜 연동 목록 끝
            notificationSetting = NotificationSettingUi( // 알림 설정 데이터
                budgetAlertEnabled = true, // 예산 알림 기본값
                rewardAlertEnabled = true, // 보상 알림 기본값
                streakReminderEnabled = true, // 스트릭 알림 기본값
                marketingAlertEnabled = true // 마케팅 알림 기본값
            ), // 알림 설정 끝
            walletUi = WalletUi( // 지갑 상태 데이터
                isConnected = false, // 지갑 연결 여부 기본값
                walletAddress = "", // 지갑 주소 기본값
                walletProvider = "" // 지갑 종류 기본값
            ) // 지갑 상태 끝
        ) // 초기 상태 끝
    ) // 상태 저장 끝
        private set // 외부 직접 수정 방지

    // 상단 탭 변경
    fun onTabChange(tab: MyPageTab) { // 탭 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            selectedTab = tab // 선택 탭 변경
        ) // 상태 반영 끝
    } // 함수 끝

    // 예산 초과 알림 변경
    fun onBudgetAlertChange(enabled: Boolean) { // 예산 알림 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            notificationSetting = uiState.notificationSetting.copy( // 알림 상태 복사
                budgetAlertEnabled = enabled // 예산 알림 값 변경
            ) // 알림 상태 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    // 보상 알림 변경
    fun onRewardAlertChange(enabled: Boolean) { // 보상 알림 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            notificationSetting = uiState.notificationSetting.copy( // 알림 상태 복사
                rewardAlertEnabled = enabled // 보상 알림 값 변경
            ) // 알림 상태 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    // 스트릭 알림 변경
    fun onStreakReminderChange(enabled: Boolean) { // 스트릭 알림 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            notificationSetting = uiState.notificationSetting.copy( // 알림 상태 복사
                streakReminderEnabled = enabled // 스트릭 알림 값 변경
            ) // 알림 상태 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    // 마케팅 알림 변경
    fun onMarketingAlertChange(enabled: Boolean) { // 마케팅 알림 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            notificationSetting = uiState.notificationSetting.copy( // 알림 상태 복사
                marketingAlertEnabled = enabled // 마케팅 알림 값 변경
            ) // 알림 상태 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    // 지갑 상태 반영
    fun updateWalletState( // 지갑 상태 변경 함수
        isConnected: Boolean, // 연결 여부 받기
        walletAddress: String, // 지갑 주소 받기
        walletProvider: String // 지갑 종류 받기
    ) { // 함수 시작
        uiState = uiState.copy( // 기존 상태 복사
            walletUi = uiState.walletUi.copy( // 지갑 상태 복사
                isConnected = isConnected, // 연결 여부 반영
                walletAddress = walletAddress, // 지갑 주소 반영
                walletProvider = walletProvider // 지갑 종류 반영
            ) // 지갑 상태 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    fun toggleEditMode() {   //함수정의
        uiState = uiState.copy(    // 현재의 uiState.copy 복사를 하여 새로운 상태를 만듬
            isEditMode = !uiState.isEditMode //isEditMode 라는 항목의 값을 현재값의 반대로 바꿈
        )
    }

    // 회원 정보 수정
    fun updateMemberInfo( // 회원 정보 변경 함수
        name: String, // 이름 받기
        nickname: String, // 닉네임 받기
        email: String, // 이메일 받기
        phone: String // 전화번호 받기
    ) { // 함수 시작
        uiState = uiState.copy( // 기존 상태 복사
            memberInfo = uiState.memberInfo.copy( // 회원 정보 복사
                name = name, // 이름 반영
                nickname = nickname, // 닉네임 반영
                email = email, // 이메일 반영
                phone = phone // 전화번호 반영
            ), // 회원 정보 반영 끝
            profileSummary = uiState.profileSummary.copy( // 프로필 요약 복사
                nickname = nickname, // 상단 닉네임도 같이 반영
                realName = name // 상단 실명도 같이 반영
            ) // 프로필 요약 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝

    // 프로필 이미지 변경
    fun updateProfileImage(profileImageUri: String) { // 프로필 이미지 변경 함수
        uiState = uiState.copy( // 기존 상태 복사
            profileSummary = uiState.profileSummary.copy( // 프로필 요약 복사
                profileImageUri = profileImageUri // 프로필 이미지 uri 반영
            ) // 프로필 요약 반영 끝
        ) // 전체 상태 반영 끝
    } // 함수 끝
} // ViewModel 끝
