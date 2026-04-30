package com.ict.spentopia.feature.mypage // 마이페이지 패키지

import android.net.Uri // 이미지 uri
import androidx.activity.compose.rememberLauncherForActivityResult // 갤러리 런처
import androidx.activity.result.contract.ActivityResultContracts // 갤러리 계약
import androidx.compose.foundation.background // 배경 박스 표현
import androidx.compose.foundation.border // 테두리 표현
import androidx.compose.foundation.clickable // 클릭 처리
import androidx.compose.foundation.layout.Arrangement // 정렬과 간격 처리
import androidx.compose.foundation.layout.Box // 박스 레이아웃
import androidx.compose.foundation.layout.Column // 세로 배치
import androidx.compose.foundation.layout.Row // 가로 배치
import androidx.compose.foundation.layout.Spacer // 여백 추가
import androidx.compose.foundation.layout.fillMaxWidth // 가로 전체 사용
import androidx.compose.foundation.layout.height // 높이 지정
import androidx.compose.foundation.layout.padding // 내부 여백 적용
import androidx.compose.foundation.layout.size // 크기 지정
import androidx.compose.foundation.rememberScrollState // 스크롤 상태 기억
import androidx.compose.foundation.shape.CircleShape // 원형 모양
import androidx.compose.foundation.shape.RoundedCornerShape // 둥근 모서리
import androidx.compose.foundation.verticalScroll // 세로 스크롤
import androidx.compose.material3.Card // 카드 UI
import androidx.compose.material3.CardDefaults // 카드 스타일
import androidx.compose.material3.OutlinedTextField // 입력창 UI
import androidx.compose.material3.Switch // 스위치 UI
import androidx.compose.material3.SwitchDefaults // 스위치 색상
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // 텍스트 출력
import androidx.compose.material3.TextButton // 탭 버튼
import androidx.compose.runtime.Composable // 컴포저블 함수
import androidx.compose.runtime.getValue // 수정: state 위임 사용
import androidx.compose.runtime.mutableStateOf // 수정: 지갑 선택 다이얼로그 상태 저장
import androidx.compose.runtime.remember // 수정: Compose 상태 유지
import androidx.compose.runtime.setValue // 수정: state 위임 사용
import androidx.compose.ui.Alignment // 정렬 기준
import androidx.compose.ui.Modifier // UI 수정자
import androidx.compose.ui.graphics.Brush // 그라데이션 배경
import androidx.compose.ui.graphics.Color // 색상
import androidx.compose.ui.layout.ContentScale // 이미지 비율
import androidx.compose.ui.text.font.FontWeight // 글자 굵기
import androidx.compose.ui.unit.dp // dp 단위
import androidx.compose.ui.unit.sp // 폰트 크기 단위
import androidx.lifecycle.viewmodel.compose.viewModel // ViewModel 연결
import coil.compose.AsyncImage // 이미지 출력
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // 수정: 솔라나 지갑 선택 다이얼로그
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // 수정: 선택한 솔라나 지갑 종류
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaActionGradientColors
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors

// 기존 주석 유지
// 마이페이지 화면
@Composable
fun MyPageScreen(
    isWalletConnected: Boolean = false, // 수정: AppNavGraph에서 전달받은 실제 지갑 연결 여부
    walletAddress: String = "", // 수정: AppNavGraph에서 전달받은 실제 지갑 주소
    walletProvider: String = "", // 수정: AppNavGraph에서 전달받은 실제 지갑 종류
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 수정: 선택한 지갑 종류를 AppNavGraph로 넘기는 함수
    myPageViewModel: MyPageViewModel = viewModel() // ViewModel 연결
) {
    val uiState = myPageViewModel.uiState // 현재 화면 상태 읽기

    var showWalletDialog by remember { mutableStateOf(false) } // 수정: 지갑 선택 팝업 표시 여부 상태

    val imageLauncher = rememberLauncherForActivityResult( // 이미지 선택 런처
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) { // 선택 이미지 확인
            myPageViewModel.updateProfileImage(uri.toString()) // 이미지 반영
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth() // 전체 너비 사용
            .verticalScroll(rememberScrollState()) // 전체 화면 스크롤
            .padding(vertical = 8.dp) // 상하 여백
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // 탭 배경색
                    shape = RoundedCornerShape(999.dp) // 캡슐형 모양
                )
                .padding(4.dp), // 내부 여백
            horizontalArrangement = Arrangement.spacedBy(4.dp) // 탭 간격
        ) {
            MyPageTopTabButton(
                text = "프로필", // 프로필 탭 텍스트
                selected = uiState.selectedTab == MyPageTab.PROFILE, // 선택 상태 반영
                onClick = {
                    myPageViewModel.onTabChange(MyPageTab.PROFILE) // 프로필 탭 변경
                }
            )

            MyPageTopTabButton(
                text = "알림", // 알림 탭 텍스트
                selected = uiState.selectedTab == MyPageTab.NOTIFICATION, // 선택 상태 반영
                onClick = {
                    myPageViewModel.onTabChange(MyPageTab.NOTIFICATION) // 알림 탭 변경
                }
            )

            MyPageTopTabButton(
                text = "지갑", // 지갑 탭 텍스트
                selected = uiState.selectedTab == MyPageTab.WALLET, // 선택 상태 반영
                onClick = {
                    myPageViewModel.onTabChange(MyPageTab.WALLET) // 지갑 탭 변경
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // 탭과 본문 사이 여백

        when (uiState.selectedTab) { // 현재 선택 탭 기준 분기
            MyPageTab.PROFILE -> {
                ProfileTabContent(
                    uiState = uiState, // 프로필 상태 전달
                    myPageViewModel = myPageViewModel, // 뷰모델 전달
                    onProfileImageClick = { imageLauncher.launch("image/*") } // 이미지 선택
                )
            }

            MyPageTab.NOTIFICATION -> {
                NotificationTabContent(
                    uiState = uiState, // 알림 상태 전달
                    onBudgetAlertChange = myPageViewModel::onBudgetAlertChange, // 예산 알림 변경 연결
                    onRewardAlertChange = myPageViewModel::onRewardAlertChange, // 보상 알림 변경 연결
                    onStreakReminderChange = myPageViewModel::onStreakReminderChange, // 스트릭 알림 변경 연결
                    onMarketingAlertChange = myPageViewModel::onMarketingAlertChange // 마케팅 알림 변경 연결
                )
            }

            MyPageTab.WALLET -> {
                WalletTabContent(
                    uiState = uiState, // 지갑 탭 본문
                    isWalletConnected = isWalletConnected, // 수정: 실제 지갑 연결 여부 전달
                    walletAddress = walletAddress, // 수정: 실제 지갑 주소 전달
                    walletProvider = walletProvider, // 수정: 실제 지갑 종류 전달
                    onWalletConnectButtonClick = {
                        showWalletDialog = true // 수정: 지갑 연결 버튼 클릭 시 선택창 표시
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 하단 여백
    }

    if (showWalletDialog) { // 수정: 지갑 선택 팝업 표시
        SolanaWalletDialog(
            onDismiss = {
                showWalletDialog = false // 수정: 팝업 닫기
            },
            onSelectWallet = { walletType ->
                showWalletDialog = false // 수정: 선택 후 팝업 닫기
                onWalletConnectClick(walletType) // 수정: 선택한 지갑 종류를 AppNavGraph로 전달
            }
        )
    }
}

// 상단 탭 버튼
@Composable
private fun MyPageTopTabButton(
    text: String, // 탭 이름
    selected: Boolean, // 선택 여부
    onClick: () -> Unit // 클릭 이벤트
) {
    TextButton(
        onClick = { onClick() }, // 클릭 시 동작 실행
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                shape = RoundedCornerShape(999.dp) // 캡슐형 모양
            )
    ) {
        Text(
            text = text, // 탭 텍스트 출력
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp, // 탭 글자 크기
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // 선택 탭 강조
        )
    }
}

// 프로필 탭 본문
@Composable
private fun ProfileTabContent(
    uiState: MyPageUiState, // 전체 상태
    myPageViewModel: MyPageViewModel, // 뷰모델
    onProfileImageClick: () -> Unit // 이미지 클릭
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp) // 카드 간격
    ) {
        ProfileHeaderCard(
            uiState = uiState, // 프로필 상태
            onProfileImageClick = onProfileImageClick // 이미지 선택 연결
        )
        MemberInfoCard(
            uiState = uiState, // 회원 정보 상태
            viewModel = myPageViewModel // 회원 정보 수정 연결
        )
        SocialAccountCard(uiState = uiState) // 소셜 연동 카드
    }
}

// 상단 프로필 카드
@Composable
private fun ProfileHeaderCard(
    uiState: MyPageUiState, // 전체 상태 받기
    onProfileImageClick: () -> Unit // 이미지 클릭
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드 전체 너비 사용
        shape = RoundedCornerShape(20.dp), // 둥근 카드
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // 내부 그라데이션 사용
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = SpentopiaWalletGradientColors
                    ),
                    shape = RoundedCornerShape(20.dp) // 둥근 그라데이션
                )
                .padding(20.dp), // 카드 내부 여백
            horizontalAlignment = Alignment.CenterHorizontally // 중앙 정렬
        ) {
            Box(
                contentAlignment = Alignment.Center, // 중앙 배치
                modifier = Modifier.clickable { onProfileImageClick() } // 이미지 클릭
            ) {
                if (uiState.profileSummary.profileImageUri.isBlank()) {
                    Box(
                        modifier = Modifier
                            .size(92.dp) // 프로필 원 크기
                            .background(
                                color = Color.White.copy(alpha = 0.18f), // 반투명 배경
                                shape = CircleShape // 원형
                            ),
                        contentAlignment = Alignment.Center // 중앙 정렬
                    ) {
                        Text(
                            text = "😊", // 기본 이모지
                            fontSize = 36.sp // 이모지 크기
                        )
                    }
                } else {
                    AsyncImage(
                        model = uiState.profileSummary.profileImageUri, // 이미지 uri
                        contentDescription = "프로필 이미지", // 이미지 설명
                        modifier = Modifier
                            .size(92.dp) // 이미지 크기
                            .background(
                                color = Color.White.copy(alpha = 0.18f), // 배경색
                                shape = CircleShape // 원형
                            ),
                        contentScale = ContentScale.Crop // 꽉 채우기
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // 오른쪽 아래 배치
                        .background(
                            color = MaterialTheme.colorScheme.surface, // 배경
                            shape = CircleShape // 원형 배지
                        )
                        .padding(8.dp) // 배지 여백
                ) {
                    Text(
                        text = "📷", // 카메라 이모지
                        fontSize = 12.sp // 이모지 크기
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp)) // 이미지와 이름 사이 여백

            Text(
                text = uiState.profileSummary.nickname, // 닉네임 출력
                fontSize = 28.sp, // 닉네임 크기
                fontWeight = FontWeight.Bold, // 닉네임 강조
                color = Color.White // 흰색 글자
            )

            Spacer(modifier = Modifier.height(4.dp)) // 닉네임과 실명 사이 여백

            Text(
                text = uiState.profileSummary.realName, // 실명 출력
                fontSize = 14.sp, // 실명 크기
                fontWeight = FontWeight.SemiBold, // 실명 강조
                color = Color.White.copy(alpha = 0.92f) // 반투명 흰색
            )

            Spacer(modifier = Modifier.height(20.dp)) // 프로필 정보와 통계 카드 사이 여백

            ProfileStatBox(
                title = "가입일", // 가입일 제목
                value = uiState.profileSummary.joinedDateText // 가입일 값
            )

            ProfileStatBox(
                title = "연속 기록", // 연속 기록 제목
                value = uiState.profileSummary.streakText // 연속 기록 값
            )

            ProfileStatBox(
                title = "보유 SPT", // 보유 SPT 제목
                value = uiState.profileSummary.sptBalanceText // 보유 SPT 값
            )

            ProfileStatBox(
                title = "보유 아바타", // 보유 아바타 제목
                value = uiState.profileSummary.avatarCountText // 보유 아바타 값
            )
        }
    }
}

// 프로필 통계 박스
@Composable
private fun ProfileStatBox(
    title: String, // 제목
    value: String // 값
) {
        Card(
            modifier = Modifier
                .fillMaxWidth() // 전체 너비 사용
                .padding(bottom = 10.dp), // 하단 간격
            shape = RoundedCornerShape(14.dp), // 둥근 박스
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f) // 반투명 배경
            )
        ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp) // 내부 여백
        ) {
            Text(
                text = title, // 제목 출력
                fontSize = 14.sp, // 제목 크기
                color = MaterialTheme.colorScheme.onSurfaceVariant // 반투명 흰색
            )

            Spacer(modifier = Modifier.height(6.dp)) // 제목과 값 사이 여백

            Text(
                text = value, // 값 출력
                fontSize = 18.sp, // 값 크기
                fontWeight = FontWeight.Bold, // 값 강조
                color = MaterialTheme.colorScheme.onSurface // 흰색 글자
            )
        }
    }
}

// 회원 정보 카드
@Composable
private fun MemberInfoCard(
    uiState: MyPageUiState, // 전체 상태
    viewModel: MyPageViewModel // 뷰모델
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드 전체 너비 사용
        shape = RoundedCornerShape(18.dp), // 둥근 카드
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 흰색 배경
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 카드 내부 여백
            verticalArrangement = Arrangement.spacedBy(12.dp) // 내부 간격
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 제목 줄 전체 너비 사용
                horizontalArrangement = Arrangement.SpaceBetween, // 양 끝 정렬
                verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
            ) {
                Text(
                    text = "회원 정보", // 카드 제목
                    fontSize = 18.sp, // 제목 크기
                    fontWeight = FontWeight.Bold, // 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 제목 색상
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, // 버튼 배경색
                            shape = RoundedCornerShape(10.dp) // 둥근 버튼
                        )
                        .clickable {
                            viewModel.toggleEditMode() // 수정 모드 변경
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp) // 버튼 여백
                ) {
                    Text(
                        text = if (uiState.isEditMode) "💾 저장" else "✏️ 수정", // 버튼 문구
                        fontSize = 13.sp, // 버튼 글자 크기
                        fontWeight = FontWeight.SemiBold, // 버튼 글자 강조
                        color = MaterialTheme.colorScheme.onPrimaryContainer // 버튼 글자색
                    )
                }
            }

            if (uiState.isEditMode) {
                EditableField(
                    label = "이름", // 이름 라벨
                    value = uiState.memberInfo.name, // 이름 값
                    onValueChange = { newValue ->
                        viewModel.updateMemberInfo(
                            name = newValue, // 이름 반영
                            nickname = uiState.memberInfo.nickname, // 닉네임 유지
                            email = uiState.memberInfo.email, // 이메일 유지
                            phone = uiState.memberInfo.phone // 전화번호 유지
                        )
                    }
                )

                EditableField(
                    label = "닉네임", // 닉네임 라벨
                    value = uiState.memberInfo.nickname, // 닉네임 값
                    onValueChange = { newValue ->
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // 이름 유지
                            nickname = newValue, // 닉네임 반영
                            email = uiState.memberInfo.email, // 이메일 유지
                            phone = uiState.memberInfo.phone // 전화번호 유지
                        )
                    }
                )

                EditableField(
                    label = "이메일", // 이메일 라벨
                    value = uiState.memberInfo.email, // 이메일 값
                    onValueChange = { newValue ->
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // 이름 유지
                            nickname = uiState.memberInfo.nickname, // 닉네임 유지
                            email = newValue, // 이메일 반영
                            phone = uiState.memberInfo.phone // 전화번호 유지
                        )
                    }
                )

                EditableField(
                    label = "전화번호", // 전화번호 라벨
                    value = uiState.memberInfo.phone, // 전화번호 값
                    onValueChange = { newValue ->
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // 이름 유지
                            nickname = uiState.memberInfo.nickname, // 닉네임 유지
                            email = uiState.memberInfo.email, // 이메일 유지
                            phone = newValue // 전화번호 반영
                        )
                    }
                )
            } else {
                MemberField(label = "이름", value = uiState.memberInfo.name) // 이름 표시
                MemberField(label = "닉네임", value = uiState.memberInfo.nickname) // 닉네임 표시
                MemberField(label = "이메일", value = uiState.memberInfo.email) // 이메일 표시
                MemberField(label = "전화번호", value = uiState.memberInfo.phone) // 전화번호 표시
            }
        }
    }
}

// 회원 정보 필드
@Composable
private fun MemberField(
    label: String, // 필드명
    value: String // 필드값
) {
    Column {
        Text(
            text = label, // 라벨 출력
            fontSize = 14.sp, // 라벨 크기
            fontWeight = FontWeight.SemiBold, // 라벨 강조
            color = MaterialTheme.colorScheme.onSurface // 라벨 색상
        )

        Spacer(modifier = Modifier.height(6.dp)) // 라벨과 입력창 사이 여백

        Box(
            modifier = Modifier
                .fillMaxWidth() // 입력창 전체 너비 사용
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // 입력창 배경
                    shape = RoundedCornerShape(12.dp) // 둥근 입력창
                )
                .padding(horizontal = 14.dp, vertical = 12.dp) // 입력창 내부 여백
        ) {
            Text(
                text = value, // 값 출력
                fontSize = 15.sp, // 값 크기
                color = MaterialTheme.colorScheme.onSurfaceVariant // 비활성 느낌 색상
            )
        }
    }
}

// 수정 입력 필드
@Composable
private fun EditableField(
    label: String, // 필드명
    value: String, // 필드값
    onValueChange: (String) -> Unit // 값 변경
) {
    Column {
        Text(
            text = label, // 라벨 출력
            fontSize = 14.sp, // 라벨 크기
            fontWeight = FontWeight.SemiBold, // 라벨 강조
            color = MaterialTheme.colorScheme.onSurface // 라벨 색상
        )

        Spacer(modifier = Modifier.height(6.dp)) // 라벨과 입력창 사이 여백

        OutlinedTextField(
            value = value, // 값 반영
            onValueChange = onValueChange, // 값 변경 반영
            modifier = Modifier.fillMaxWidth(), // 전체 너비 사용
            singleLine = true, // 한 줄 입력
            shape = RoundedCornerShape(12.dp) // 둥근 입력창
        )
    }
}

// 소셜 계정 카드
@Composable
private fun SocialAccountCard(
    uiState: MyPageUiState // 전체 상태 받기
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드 전체 너비 사용
        shape = RoundedCornerShape(18.dp), // 둥근 카드
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 흰색 배경
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 카드 내부 여백
            verticalArrangement = Arrangement.spacedBy(16.dp) // 내부 간격
        ) {
            Text(
                text = "소셜 계정 연동", // 카드 제목
                fontSize = 18.sp, // 제목 크기
                fontWeight = FontWeight.Bold, // 제목 강조
                color = MaterialTheme.colorScheme.onSurface // 제목 색상
            )

            uiState.socialAccounts.forEach { account -> // 소셜 목록 반복
                SocialAccountRow(
                    serviceName = account.serviceName, // 서비스 이름 전달
                    connected = account.connected // 연동 상태 전달
                )
            }
        }
    }
}

// 소셜 계정 한 줄
@Composable
private fun SocialAccountRow(
    serviceName: String, // 서비스 이름
    connected: Boolean // 연동 여부
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 행 카드 전체 너비 사용
        shape = RoundedCornerShape(14.dp), // 둥근 카드
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 흰색 배경
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 살짝 떠 있는 느낌
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() // 행 전체 너비 사용
                .padding(horizontal = 14.dp, vertical = 14.dp), // 내부 여백
            verticalAlignment = Alignment.CenterVertically, // 세로 중앙 정렬
            horizontalArrangement = Arrangement.SpaceBetween // 양 끝 정렬
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, // 아이콘과 텍스트 중앙 정렬
                horizontalArrangement = Arrangement.spacedBy(10.dp) // 간격
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 아이콘 박스 크기
                        .background(
                            color = when (serviceName) { // 서비스별 배경색
                                "카카오" -> Color(0xFFF7C600) // 카카오 색상
                                else -> Color(0xFF4285F4) // 구글 색상
                            },
                            shape = RoundedCornerShape(999.dp) // 원형
                        ),
                    contentAlignment = Alignment.Center // 가운데 정렬
                ) {
                    Text(
                        text = if (serviceName == "카카오") "💬" else serviceName.first().toString(), // 서비스 아이콘 텍스트
                        color = Color.White, // 흰색 글자
                        fontWeight = FontWeight.Bold // 글자 강조
                    )
                }

                Column {
                    Text(
                        text = serviceName, // 서비스 이름 출력
                        fontSize = 16.sp, // 이름 크기
                        fontWeight = FontWeight.Bold, // 이름 강조
                        color = MaterialTheme.colorScheme.onSurface // 이름 색상
                    )

                    Text(
                        text = if (connected) "연동됨" else "미연동", // 상태 텍스트
                        fontSize = 12.sp, // 상태 크기
                        color = MaterialTheme.colorScheme.onSurfaceVariant // 상태 색상
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (connected) Color(0xFF22C55E) else Color(0xFFF3F4F6), // 상태별 버튼색
                        shape = RoundedCornerShape(10.dp) // 둥근 버튼
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp) // 버튼 여백
            ) {
                Text(
                    text = if (connected) "연동됨" else "연동", // 버튼 텍스트
                    color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface, // 버튼 글자색
                    fontSize = 12.sp, // 버튼 글자 크기
                    fontWeight = FontWeight.Bold // 버튼 글자 강조
                )
            }
        }
    }
}

// 알림 탭 본문
@Composable
private fun NotificationTabContent(
    uiState: MyPageUiState, // 전체 상태 받기
    onBudgetAlertChange: (Boolean) -> Unit, // 예산 알림 변경 함수
    onRewardAlertChange: (Boolean) -> Unit, // 보상 알림 변경 함수
    onStreakReminderChange: (Boolean) -> Unit, // 스트릭 알림 변경 함수
    onMarketingAlertChange: (Boolean) -> Unit // 마케팅 알림 변경 함수
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 카드 전체 너비 사용
        shape = RoundedCornerShape(18.dp), // 둥근 카드
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 흰색 배경
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 카드 내부 여백
            verticalArrangement = Arrangement.spacedBy(18.dp) // 내부 간격
        ) {
            Text(
                text = "알림 설정", // 카드 제목
                fontSize = 18.sp, // 제목 크기
                fontWeight = FontWeight.Bold, // 제목 강조
                color = MaterialTheme.colorScheme.onSurface // 제목 색상
            )

            NotificationToggleRow(
                title = "예산 초과 알림", // 항목 제목
                desc = "예산의 80%를 초과하면 알림을 보내드려요", // 항목 설명
                checked = uiState.notificationSetting.budgetAlertEnabled, // 현재 상태
                onCheckedChange = onBudgetAlertChange // 상태 변경 연결
            )

            NotificationToggleRow(
                title = "보상 획득 알림", // 항목 제목
                desc = "SPT나 아바타를 획득하면 알려드려요", // 항목 설명
                checked = uiState.notificationSetting.rewardAlertEnabled, // 현재 상태
                onCheckedChange = onRewardAlertChange // 상태 변경 연결
            )

            NotificationToggleRow(
                title = "스트릭 리마인드", // 항목 제목
                desc = "오늘 기록하지 않았다면 알려드려요", // 항목 설명
                checked = uiState.notificationSetting.streakReminderEnabled, // 현재 상태
                onCheckedChange = onStreakReminderChange // 상태 변경 연결
            )

            NotificationToggleRow(
                title = "마케팅 알림", // 항목 제목
                desc = "이벤트와 프로모션 정보를 받아보세요", // 항목 설명
                checked = uiState.notificationSetting.marketingAlertEnabled, // 현재 상태
                onCheckedChange = onMarketingAlertChange // 상태 변경 연결
            )
        }
    }
}

// 알림 토글 한 줄
@Composable
private fun NotificationToggleRow(
    title: String, // 항목 제목
    desc: String, // 항목 설명
    checked: Boolean, // 현재 스위치 상태
    onCheckedChange: (Boolean) -> Unit // 상태 변경 함수
) {
    Row(
        modifier = Modifier.fillMaxWidth(), // 전체 너비 사용
        horizontalArrangement = Arrangement.SpaceBetween, // 양 끝 정렬
        verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
    ) {
        Row(
            modifier = Modifier.weight(1f), // 왼쪽 텍스트 영역 확장
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 간격
            verticalAlignment = Alignment.Top // 위쪽 정렬
        ) {
            Text(
                text = "🔔", // 알림 이모지
                fontSize = 14.sp // 이모지 크기
            )

            Column {
                Text(
                    text = title, // 제목 출력
                    fontSize = 16.sp, // 제목 크기
                    fontWeight = FontWeight.SemiBold, // 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 제목 색상
                )

                Text(
                    text = desc, // 설명 출력
                    fontSize = 13.sp, // 설명 크기
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 설명 색상
                )
            }
        }

        Switch(
            checked = checked, // 현재 상태 반영
            onCheckedChange = { onCheckedChange(it) }, // 상태 변경 전달
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, // 켜짐 썸 색
                checkedTrackColor = MaterialTheme.colorScheme.primary, // 켜짐 트랙 색
                uncheckedThumbColor = Color.White, // 꺼짐 썸 색
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant // 꺼짐 트랙 색
            )
        )
    }
}

// 지갑 탭 본문
@Composable
private fun WalletTabContent(
    uiState: MyPageUiState, // 전체 상태 받기
    isWalletConnected: Boolean, // 수정: 실제 지갑 연결 여부
    walletAddress: String, // 수정: 실제 지갑 주소
    walletProvider: String, // 수정: 실제 지갑 종류
    onWalletConnectButtonClick: () -> Unit // 수정: 지갑 연결 버튼 클릭 이벤트
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp) // 카드 간격
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(), // 카드 전체 너비 사용
            shape = RoundedCornerShape(18.dp), // 둥근 카드
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 흰색 배경
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 카드 내부 여백
                verticalArrangement = Arrangement.spacedBy(16.dp), // 내부 간격
                horizontalAlignment = Alignment.CenterHorizontally // 중앙 정렬
            ) {
                Text(
                    text = "지갑 관리", // 카드 제목
                    fontSize = 18.sp, // 제목 크기
                    fontWeight = FontWeight.Bold, // 제목 강조
                    color = MaterialTheme.colorScheme.onSurface, // 제목 색상
                    modifier = Modifier.fillMaxWidth() // 전체 너비 사용
                )

                if (!isWalletConnected) { // 수정: AppNavGraph의 실제 지갑 연결 상태 기준으로 미연결 분기
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() // 전체 너비 사용
                            .border(
                                width = 1.dp, // 테두리 두께
                                color = MaterialTheme.colorScheme.outlineVariant, // 테두리 색상
                                shape = RoundedCornerShape(14.dp) // 둥근 테두리
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp), // 내부 여백
                        contentAlignment = Alignment.Center // 중앙 정렬
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally, // 중앙 정렬
                            verticalArrangement = Arrangement.spacedBy(10.dp) // 간격
                        ) {
                            Text(
                                text = "👛", // 지갑 이모지
                                fontSize = 42.sp // 이모지 크기
                            )

                            Text(
                                text = "지갑이 연결되지 않았어요", // 안내 문구
                                fontSize = 20.sp, // 문구 크기
                                fontWeight = FontWeight.Bold, // 문구 강조
                                color = MaterialTheme.colorScheme.onSurface // 문구 색상
                            )

                            Text(
                                text = "지갑을 연결하면 NFT 거래와\n블록체인 기능을 이용할 수 있어요", // 설명 문구
                                fontSize = 14.sp, // 설명 크기
                                color = MaterialTheme.colorScheme.onSurfaceVariant // 설명 색상
                            )

                            Box(
                                modifier = Modifier
                                    .background(
                                        brush = Brush.linearGradient(
                                            colors = SpentopiaWalletGradientColors
                                        ),
                                        shape = RoundedCornerShape(12.dp) // 둥근 버튼
                                    )
                                    .clickable {
                                        onWalletConnectButtonClick() // 수정: 지갑 선택 다이얼로그 열기
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp) // 버튼 여백
                            ) {
                                Text(
                                    text = "🔗 지갑 연결하기", // 버튼 텍스트
                                    color = Color.White, // 버튼 글자색
                                    fontSize = 14.sp, // 버튼 글자 크기
                                    fontWeight = FontWeight.Bold // 버튼 글자 강조
                                )
                            }
                        }
                    }
                } else { // 연결 상태 분기
                    Box(
                        modifier = Modifier
                            .fillMaxWidth() // 전체 너비 사용
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant, // 박스 배경색
                                shape = RoundedCornerShape(14.dp) // 둥근 박스
                            )
                            .padding(16.dp) // 내부 여백
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp) // 간격
                        ) {
                            Text(
                                text = "연결된 지갑", // 제목
                                fontSize = 14.sp, // 제목 크기
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // 제목 색상
                                fontWeight = FontWeight.Medium // 제목 강조
                            )

                            Text(
                                text = walletProvider, // 수정: 실제 지갑 종류 표시
                                fontSize = 18.sp, // 지갑 종류 크기
                                fontWeight = FontWeight.Bold, // 지갑 종류 강조
                                color = MaterialTheme.colorScheme.onSurface // 지갑 종류 색상
                            )

                            Text(
                                text = formatWalletAddress(walletAddress), // 수정: 실제 지갑 주소 표시
                                fontSize = 13.sp, // 주소 크기
                                color = MaterialTheme.colorScheme.onSurfaceVariant // 주소 색상
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(), // 혜택 카드 전체 너비 사용
            shape = RoundedCornerShape(18.dp), // 둥근 카드
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // 연한 하늘색 배경
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 카드 내부 여백
                verticalArrangement = Arrangement.spacedBy(10.dp) // 내부 간격
            ) {
                Text(
                    text = "💡 지갑 연결 혜택", // 카드 제목
                    fontSize = 18.sp, // 제목 크기
                    fontWeight = FontWeight.Bold, // 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 제목 색상
                )

                WalletBenefitText(text = "• NFT로 아바타 아이템 발행") // 혜택 문구
                WalletBenefitText(text = "• 마켓에서 자유롭게 거래") // 혜택 문구
                WalletBenefitText(text = "• 블록체인 기반 수익 흐름 적용") // 혜택 문구
                WalletBenefitText(text = "• 지갑으로 간편 로그인") // 혜택 문구
            }
        }
    }
}

// 지갑 혜택 텍스트
@Composable
private fun WalletBenefitText(
    text: String // 혜택 문구
) {
    Text(
        text = text, // 문구 출력
        fontSize = 14.sp, // 글자 크기
        color = MaterialTheme.colorScheme.onSurface, // 글자 색상
        fontWeight = FontWeight.Medium // 글자 강조
    )
}

// 지갑 주소를 보기 좋게 줄여서 표시하는 함수
private fun formatWalletAddress(address: String): String {
    return if (address.length <= 10) {
        address
    } else {
        "${address.take(4)}...${address.takeLast(4)}"
    }
}
