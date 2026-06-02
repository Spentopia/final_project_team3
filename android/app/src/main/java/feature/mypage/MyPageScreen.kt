package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

// 마이페이지 화면임
// 프로필/아바타/설정/테마 관리

import android.net.Uri // 이미지 주소 타입을 가져옴
import androidx.activity.compose.rememberLauncherForActivityResult // rememberLauncherForActivityResult 기능을 가져옴
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultContracts 기능을 가져옴
import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.isSystemInDarkTheme // 다크모드 확인 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.material3.AlertDialog // 확인 팝업 기능을 가져옴
import androidx.compose.material3.ButtonDefaults // 버튼 색상 기본값을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.Switch // Switch 기능을 가져옴
import androidx.compose.material3.SwitchDefaults // SwitchDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴
import androidx.compose.material.icons.Icons // 아이콘 묶음을 가져옴
import androidx.compose.material.icons.outlined.AccountBalanceWallet // 지갑 아이콘을 가져옴
import androidx.compose.material.icons.outlined.CheckCircle // 체크 아이콘을 가져옴
import androidx.compose.material.icons.outlined.DesktopWindows // 브라우저 지갑 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Info // 안내 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Key // 비밀번호 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Link // 연결 아이콘을 가져옴
import androidx.compose.material.icons.outlined.LinkOff // 연결 해제 아이콘을 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면 값이 바뀔 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // 현재 화면 정보를 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.input.PasswordVisualTransformation // 비밀번호 입력 표시를 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import coil.compose.AsyncImage // AsyncImage 기능을 가져옴
import com.ict.spentopia.R // R 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // SolanaWalletDialog 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast

// 기존 주석 유지
// 마이페이지 화면
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun MyPageScreen( // MyPageScreen 함수를 선언함
    isWalletConnected: Boolean = false, // 지갑 관련 값을 받음
    walletAddress: String = "", // 지갑 주소를 받음
    walletProvider: String = "", // 지갑 이름을 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 지갑 관련 값을 받음
    onWalletDisconnectClick: () -> Unit = {}, // 지갑 해제 값을 받음
    myPageViewModel: MyPageViewModel = viewModel() // myPageViewModel 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val uiState = myPageViewModel.uiState // 화면 상태를 저장함
    val context = LocalContext.current // 현재 화면 정보를 저장함

    var showWalletDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 지갑 관련 값을 저장함

    LaunchedEffect(isWalletConnected, walletAddress, walletProvider) {
        myPageViewModel.updateWalletState(
            isConnected = isWalletConnected,
            walletAddress = walletAddress,
            walletProvider = walletProvider
        )
    }

    val imageLauncher = rememberLauncherForActivityResult( // 화면이 다시 그려져도 imageLauncher 값을 기억함
        contract = ActivityResultContracts.GetContent() // contract 값을 정해줌
    ) { uri: Uri? ->
        if (uri != null) { // 조건이 맞는지 확인함
            myPageViewModel.updateProfileImage(uri.toString()) // 화면에 이미지를 보여줌
        }
    }

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp) // .padding(vertical 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = "내 프로필", // text 값을 정해줌
            fontSize = 28.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onBackground // color 값을 정해줌
        )

        Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

        Text( // 화면에 글자를 보여줌
            text = "내 정보와 설정을 관리하세요", // text 값을 정해줌
            fontSize = 14.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )

        Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

        Column( // 웹 마이페이지처럼 한 화면에 주요 영역을 이어서 보여줌
            verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
        ) {
            ProfileHeaderCard( // 프로필 요약 카드를 보여줌
                uiState = uiState, // 화면 상태를 화면 상태에 넣음
                onProfileImageClick = { imageLauncher.launch("image/*") } // 프로필 이미지를 누르면 갤러리를 열게 정함
            )

            MemberInfoCard( // 회원 정보 카드를 보여줌
                uiState = uiState, // 화면 상태를 화면 상태에 넣음
                viewModel = myPageViewModel // myPageViewModel 값을 화면 데이터 관리자에 넣음
            )

            NotificationTabContent( // 알림 설정 카드를 보여줌
                uiState = uiState, // 화면 상태를 화면 상태에 넣음
                onBudgetAlertChange = myPageViewModel::onBudgetAlertChange, // 예산 관련 값을 정해줌
                onRewardAlertChange = myPageViewModel::onRewardAlertChange, // onRewardAlertChange 때 실행할 함수를 정해줌
                onStreakReminderChange = myPageViewModel::onStreakReminderChange, // onStreakReminderChange 때 실행할 함수를 정해줌
                onMarketingAlertChange = myPageViewModel::onMarketingAlertChange // 마켓 관련 값을 정해줌
            )

            PasswordChangeCard( // 비밀번호 변경 카드를 보여줌
                onChangePassword = { currentPassword, newPassword, confirmPassword -> // 비밀번호 변경 입력값을 ViewModel에 전달함
                    myPageViewModel.changePassword(
                        currentPassword = currentPassword, // 현재 비밀번호 입력값을 전달함
                        newPassword = newPassword, // 새 비밀번호 입력값을 전달함
                        confirmPassword = confirmPassword, // 새 비밀번호 확인값을 전달함
                        onResult = { message -> // 비밀번호 변경 결과를 토스트로 표시함
                            val toastType = if (message.contains("변경되었습니다")) { // 변경 성공 여부에 따라 아이콘 유형을 나눔
                                AppToastType.SUCCESS // 성공 시 체크 아이콘을 사용함
                            } else {
                                AppToastType.ERROR // 입력 오류나 변경 실패 시 오류 아이콘을 사용함
                            }
                            showAppToast(context, message, toastType) // 정해진 결과 아이콘과 함께 문구를 표시함
                        }
                    )
                }
            )

            WalletTabContent( // 지갑 관리 카드를 보여줌
                uiState = uiState, // 화면 상태를 화면 상태에 넣음
                isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                onWalletConnectButtonClick = { // 지갑 관련 값을 정해줌
                    showWalletDialog = true // true 값을 지갑 관련 값에 넣음
                },
                onWalletDisconnectButtonClick = onWalletDisconnectClick // 지갑 해제 함수를 넣음
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // UI 크기나 여백 같은 모양을 정함
    }

    if (showWalletDialog) { // 조건이 맞는지 확인함
        SolanaWalletDialog( // 지갑 관련 함수를 실행함
            onDismiss = { // 닫을 때 실행할 함수를 정해줌
                showWalletDialog = false // false 값을 지갑 관련 값에 넣음
            },
            onSelectWallet = { walletType -> // 지갑 관련 값을 정해줌
                showWalletDialog = false // false 값을 지갑 관련 값에 넣음
                onWalletConnectClick(walletType) // 지갑 관련 함수를 실행함
            }
        )
    }
}

// 비밀번호 변경 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PasswordChangeCard( // PasswordChangeCard 함수를 선언함
    onChangePassword: (String, String, String) -> Unit // 비밀번호 변경 요청 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    var currentPassword by remember { mutableStateOf("") } // 현재 비밀번호를 저장함
    var newPassword by remember { mutableStateOf("") } // 새 비밀번호를 저장함
    var confirmPassword by remember { mutableStateOf("") } // 새 비밀번호 확인을 저장함
    val isDark = isAppDarkTheme() // 다크모드인지 확인함
    val cardBorder = passwordCardBorderColor() // 카드 테두리색을 정함
    val iconBg = if (isDark) Color(0xFF312E81) else Color(0xFFEDE9FE) // 아이콘 배경색을 정함
    val iconColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9) // 아이콘 색을 정함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.5.dp, cardBorder) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(iconBg, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Key,
                        contentDescription = "비밀번호 변경",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text( // 화면에 글자를 보여줌
                    text = "비밀번호 변경", // text 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }

            PasswordField( // 현재 비밀번호 입력칸을 보여줌
                label = "현재 비밀번호",
                value = currentPassword,
                onValueChange = { currentPassword = it }
            )

            PasswordField( // 새 비밀번호 입력칸을 보여줌
                label = "새 비밀번호",
                value = newPassword,
                onValueChange = { newPassword = it }
            )

            PasswordField( // 새 비밀번호 확인 입력칸을 보여줌
                label = "새 비밀번호 확인",
                value = confirmPassword,
                onValueChange = { confirmPassword = it }
            )

            Box( // 변경 버튼을 보여줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable {
                        onChangePassword(currentPassword, newPassword, confirmPassword)
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "비밀번호 변경",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            PasswordGuideCard() // 비밀번호 안내를 보여줌
        }
    }
}

// 비밀번호 안내 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PasswordGuideCard() { // PasswordGuideCard 함수를 선언함
    val isDark = isAppDarkTheme() // 다크모드인지 확인함
    val container = if (isDark) Color(0xFF25133F) else Color(0xFFFAF5FF) // 배경색을 정함
    val border = passwordGuideCardBorderColor() // 테두리색을 정함
    val iconBg = if (isDark) Color(0xFF312E81) else Color(0xFFEDE9FE) // 아이콘 배경색을 정함
    val iconColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF6D28D9) // 아이콘 색을 정함
    val titleColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A) // 제목 색을 정함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = container), // colors 값을 정해줌
        border = BorderStroke(1.5.dp, border) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(14.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(6.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 제목과 아이콘을 가로로 배치함
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(iconBg, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "비밀번호 안내",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text( // 화면에 글자를 보여줌
                    text = "비밀번호 안내",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = titleColor
                )
            }
            WalletBenefitText(text = "• 특수문자, 영문 대문자, 영문 소문자, 숫자를 포함해야 합니다.")
            WalletBenefitText(text = "• 최소 8자 이상으로 설정해 주세요.")
            WalletBenefitText(text = "• 기존 비밀번호와 다른 조합을 사용하는 것을 권장합니다.")
        }
    }
}

// 비밀번호 입력 필드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun PasswordField( // PasswordField 함수를 선언함
    label: String, // label 값을 받음
    value: String, // 입력값을 받음
    onValueChange: (String) -> Unit // 값 변경 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    OutlinedTextField( // 사용자가 입력할 칸을 만듦
        value = value, // 입력값을 입력값에 넣음
        onValueChange = onValueChange, // 값 변경 함수를 넣음
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        label = { Text(text = label) }, // label 값을 정해줌
        singleLine = true, // 한 줄로 입력하게 함
        visualTransformation = PasswordVisualTransformation(), // 비밀번호를 가려서 보여줌
        shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
    )
}

// 상단 탭 버튼
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MyPageTopTabButton( // MyPageTopTabButton 함수를 선언함
    text: String, // text 값을 받음
    selected: Boolean, // selected 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    TextButton( // 누를 수 있는 버튼을 만듦
        onClick = { onClick() }, // 눌렀을 때 실행할 함수를 정해줌
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .background(
                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, // color 값을 정해줌
                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
            )
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
            }
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
            fontSize = 13.sp, // fontSize 값을 정해줌
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // fontWeight 값을 정해줌
        )
    }
}

// 상단 프로필 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ProfileHeaderCard( // ProfileHeaderCard 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    onProfileImageClick: () -> Unit // onProfileImageClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val borderColor = profileCardBorderColor() // 프로필 카드 테두리색을 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.5.dp, borderColor) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                contentAlignment = Alignment.Center, // contentAlignment 값을 정해줌
                modifier = Modifier.clickable { onProfileImageClick() } // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                if (uiState.profileSummary.profileImageUri.isBlank()) { // 조건이 맞는지 확인함
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .size(92.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                                shape = CircleShape // CircleShape 값을 shape 값에 넣음
                            ),
                        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = "😊", // text 값을 정해줌
                            fontSize = 36.sp // fontSize 값을 정해줌
                        )
                    }
                } else { // 이 블록 안의 내용이 시작됨
                    AsyncImage( // 화면에 이미지를 보여줌
                        model = uiState.profileSummary.profileImageUri, // model 값을 정해줌
                        contentDescription = "프로필 이미지", // contentDescription 값을 정해줌
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .size(92.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                                shape = CircleShape // CircleShape 값을 shape 값에 넣음
                            ),
                        contentScale = ContentScale.Crop // contentScale 값을 정해줌
                    )
                }

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .align(Alignment.BottomEnd)
                        .background(
                            color = MaterialTheme.colorScheme.surface, // color 값을 정해줌
                            shape = CircleShape // CircleShape 값을 shape 값에 넣음
                        )
                        .padding(8.dp)
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "📷", // text 값을 정해줌
                        fontSize = 12.sp // fontSize 값을 정해줌
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = uiState.profileSummary.nickname, // text 값을 정해줌
                fontSize = 28.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = uiState.profileSummary.realName, // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

            ProfileStatBox( // 안쪽 UI를 한 영역에 겹쳐 배치함
                title = "가입일", // 제목을 정해줌
                value = uiState.profileSummary.joinedDateText // 입력값을 정해줌
            )

            ProfileStatBox( // 안쪽 UI를 한 영역에 겹쳐 배치함
                title = "연속 기록", // 제목을 정해줌
                value = uiState.profileSummary.streakText // 입력값을 정해줌
            )

            ProfileStatBox( // 안쪽 UI를 한 영역에 겹쳐 배치함
                title = "보유 SPT", // 제목을 정해줌
                value = uiState.profileSummary.sptBalanceText // 입력값을 정해줌
            )

            ProfileStatBox( // 안쪽 UI를 한 영역에 겹쳐 배치함
                title = "보유 NFT", // 제목을 정해줌
                value = uiState.profileSummary.avatarCountText // 입력값을 정해줌
            )

            ProfileStatBox( // 안쪽 UI를 한 영역에 겹쳐 배치함
                title = "로그인 방식", // 제목을 정해줌
                value = uiState.profileSummary.loginProviderText // 입력값을 정해줌
            )
        }
    }
}

// 프로필 통계 박스
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ProfileStatBox( // ProfileStatBox 함수를 선언함
    title: String, // 제목을 받음
    value: String // 입력값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAppDarkTheme() // 다크모드인지 확인함
    val borderColor = profileStatCardBorderColor() // 통계 카드 테두리색을 정함
    val containerColor = if (isDark) {
        Color(0xFF172033)
    } else {
        Color(0xFFF8FAFC)
    }

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(bottom = 10.dp), // .padding(bottom 값을 정해줌
            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors( // colors 값을 정해줌
                containerColor = containerColor // containerColor 값을 정해줌
            ),
            border = BorderStroke(1.5.dp, borderColor) // border 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                fontSize = 14.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = value, // 입력값을 text 값에 넣음
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

// 회원 정보 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MemberInfoCard( // MemberInfoCard 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    viewModel: MyPageViewModel // 화면 데이터 관리자를 받음
) { // 이 블록 안의 내용이 시작됨
    val cardBorder = memberCardBorderColor() // 카드 테두리색을 정함
    val context = LocalContext.current // 현재 화면 정보를 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.5.dp, cardBorder) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "회원 정보", // text 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                            shape = RoundedCornerShape(10.dp) // shape 값을 정해줌
                        )
                        .clickable { // 이 블록 안의 내용이 시작됨
                            viewModel.toggleEditMode(context) { message ->
                                val toastType = if (message.contains("저장되었습니다")) { // 프로필 저장 성공은 체크 아이콘, 실패는 오류 아이콘으로 나눔
                                    AppToastType.SUCCESS
                                } else {
                                    AppToastType.ERROR
                                }
                                showAppToast(context, message, toastType)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = if (uiState.isEditMode) "💾 저장" else "✏️ 수정", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onPrimaryContainer // color 값을 정해줌
                    )
                }
            }

            if (uiState.isEditMode) { // 조건이 맞는지 확인함
                EditableField( // Editable Field 함수를 실행함
                    label = "한 줄 소개", // label 값을 정해줌
                    value = uiState.memberInfo.name, // 입력값을 정해줌
                    onValueChange = { newValue -> // onValueChange 때 실행할 함수를 정해줌
                        viewModel.updateMemberInfo(
                            name = newValue, // newValue 값을 name 값에 넣음
                            nickname = uiState.memberInfo.nickname, // nickname 값을 정해줌
                            email = uiState.memberInfo.email, // 이메일을 정해줌
                            phone = uiState.memberInfo.phone // phone 값을 정해줌
                        )
                    }
                )

                EditableField( // Editable Field 함수를 실행함
                    label = "닉네임", // label 값을 정해줌
                    value = uiState.memberInfo.nickname, // 입력값을 정해줌
                    onValueChange = { newValue -> // onValueChange 때 실행할 함수를 정해줌
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // name 값을 정해줌
                            nickname = newValue, // newValue 값을 nickname 값에 넣음
                            email = uiState.memberInfo.email, // 이메일을 정해줌
                            phone = uiState.memberInfo.phone // phone 값을 정해줌
                        )
                    }
                )

                EditableField( // Editable Field 함수를 실행함
                    label = "이메일", // label 값을 정해줌
                    value = uiState.memberInfo.email, // 입력값을 정해줌
                    onValueChange = { newValue -> // onValueChange 때 실행할 함수를 정해줌
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // name 값을 정해줌
                            nickname = uiState.memberInfo.nickname, // nickname 값을 정해줌
                            email = newValue, // newValue 값을 이메일에 넣음
                            phone = uiState.memberInfo.phone // phone 값을 정해줌
                        )
                    },
                    readOnly = uiState.isSocialLogin,
                    helperText = if (uiState.isSocialLogin) {
                        "소셜 로그인 계정은 이메일을 변경할 수 없습니다"
                    } else {
                        null
                    }
                )

                EditableField( // Editable Field 함수를 실행함
                    label = "전화번호", // label 값을 정해줌
                    value = uiState.memberInfo.phone, // 입력값을 정해줌
                    onValueChange = { newValue -> // onValueChange 때 실행할 함수를 정해줌
                        viewModel.updateMemberInfo(
                            name = uiState.memberInfo.name, // name 값을 정해줌
                            nickname = uiState.memberInfo.nickname, // nickname 값을 정해줌
                            email = uiState.memberInfo.email, // 이메일을 정해줌
                            phone = newValue // newValue 값을 phone 값에 넣음
                        )
                    },
                    readOnly = true,
                    helperText = "전화번호는 변경할 수 없습니다"
                )
            } else { // 이 블록 안의 내용이 시작됨
                MemberField(label = "한 줄 소개", value = uiState.memberInfo.name.ifBlank { "-" }) // MemberField(label 값을 정해줌
                MemberField(label = "닉네임", value = uiState.memberInfo.nickname) // MemberField(label 값을 정해줌
                MemberField(label = "이메일", value = uiState.memberInfo.email) // MemberField(label 값을 정해줌
                MemberField(label = "전화번호", value = uiState.memberInfo.phone) // MemberField(label 값을 정해줌
            }
        }
    }
}

// 회원 정보 필드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MemberField( // MemberField 함수를 선언함
    label: String, // label 값을 받음
    value: String // 입력값을 받음
) { // 이 블록 안의 내용이 시작됨
    Column { // 안쪽 UI를 세로로 배치함
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )

        Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // color 값을 정해줌
                    shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
                )
                .padding(horizontal = 14.dp, vertical = 12.dp) // .padding(horizontal 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = value, // 입력값을 text 값에 넣음
                fontSize = 15.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

// 수정 입력 필드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun EditableField( // EditableField 함수를 선언함
    label: String, // label 값을 받음
    value: String, // 입력값을 받음
    onValueChange: (String) -> Unit, // onValueChange 때 실행할 함수를 받음
    readOnly: Boolean = false, // 읽기 전용인지 받음
    helperText: String? = null // 안내 문구를 받음
) { // 이 블록 안의 내용이 시작됨
    Column { // 안쪽 UI를 세로로 배치함
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )

        Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

        OutlinedTextField( // 사용자가 입력할 칸을 만듦
            value = value, // 입력값을 입력값에 넣음
            onValueChange = onValueChange, // onValueChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            readOnly = readOnly, // 읽기 전용 여부를 넣음
            enabled = !readOnly, // 읽기 전용 필드는 수정할 수 없게 함
            singleLine = true, // true 값을 singleLine 값에 넣음
            shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
        )

        if (!helperText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = helperText,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 알림 탭 본문
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun NotificationTabContent( // NotificationTabContent 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    onBudgetAlertChange: (Boolean) -> Unit, // 예산 관련 값을 받음
    onRewardAlertChange: (Boolean) -> Unit, // onRewardAlertChange 때 실행할 함수를 받음
    onStreakReminderChange: (Boolean) -> Unit, // onStreakReminderChange 때 실행할 함수를 받음
    onMarketingAlertChange: (Boolean) -> Unit // 마켓 관련 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val cardBorder = notificationCardBorderColor() // 카드 테두리색을 정함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.5.dp, cardBorder) // border 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(18.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "알림 설정", // text 값을 정해줌
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            NotificationToggleRow( // 안쪽 UI를 가로로 배치함
                title = "예산 초과 알림", // 제목을 정해줌
                desc = "예산의 80%를 초과하면 알림을 보내드려요", // desc 값을 정해줌
                checked = uiState.notificationSetting.budgetAlertEnabled, // checked 값을 정해줌
                onCheckedChange = onBudgetAlertChange // 예산 관련 값을 onCheckedChange 때 실행할 함수에 넣음
            )

            NotificationToggleRow( // 안쪽 UI를 가로로 배치함
                title = "보상 획득 알림", // 제목을 정해줌
                desc = "SPT나 아바타를 획득하면 알려드려요", // desc 값을 정해줌
                checked = uiState.notificationSetting.rewardAlertEnabled, // checked 값을 정해줌
                onCheckedChange = onRewardAlertChange // onRewardAlertChange 때 실행할 함수를 onCheckedChange 때 실행할 함수에 넣음
            )

            NotificationToggleRow( // 안쪽 UI를 가로로 배치함
                title = "스트릭 리마인드", // 제목을 정해줌
                desc = "오늘 기록하지 않았다면 알려드려요", // desc 값을 정해줌
                checked = uiState.notificationSetting.streakReminderEnabled, // checked 값을 정해줌
                onCheckedChange = onStreakReminderChange // onStreakReminderChange 때 실행할 함수를 onCheckedChange 때 실행할 함수에 넣음
            )

            NotificationToggleRow( // 안쪽 UI를 가로로 배치함
                title = "게시물/댓글/좋아요 알림", // 제목을 정해줌
                desc = "내가 쓴 글과 반응에 대한 커뮤니티 알림을 받아요", // desc 값을 정해줌
                checked = uiState.notificationSetting.marketingAlertEnabled, // checked 값을 정해줌
                onCheckedChange = onMarketingAlertChange // 마켓 관련 값을 onCheckedChange 때 실행할 함수에 넣음
            )
        }
    }
}

// 알림 토글 한 줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun NotificationToggleRow( // NotificationToggleRow 함수를 선언함
    title: String, // 제목을 받음
    desc: String, // desc 값을 받음
    checked: Boolean, // checked 값을 받음
    onCheckedChange: (Boolean) -> Unit // onCheckedChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.spacedBy(10.dp), // horizontalArrangement 값을 정해줌
            verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "🔔", // text 값을 정해줌
                fontSize = 14.sp // fontSize 값을 정해줌
            )

            Column { // 안쪽 UI를 세로로 배치함
                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 16.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = desc, // desc 값을 text 값에 넣음
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            }
        }

        Switch( // Switch 함수를 실행함
            checked = checked, // checked 값을 checked 값에 넣음
            onCheckedChange = { onCheckedChange(it) }, // onCheckedChange 때 실행할 함수를 정해줌
            colors = SwitchDefaults.colors( // colors 값을 정해줌
                checkedThumbColor = Color.White, // checkedThumbColor 값을 정해줌
                checkedTrackColor = MaterialTheme.colorScheme.primary, // checkedTrackColor 값을 정해줌
                uncheckedThumbColor = Color.White, // uncheckedThumbColor 값을 정해줌
                uncheckedTrackColor = MaterialTheme.colorScheme.outlineVariant // uncheckedTrackColor 값을 정해줌
            )
        )
    }
}

// 지갑 탭 본문
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletTabContent( // WalletTabContent 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    isWalletConnected: Boolean, // 지갑 관련 값을 받음
    walletAddress: String, // 지갑 주소를 받음
    walletProvider: String, // 지갑 이름을 받음
    onWalletConnectButtonClick: () -> Unit, // 지갑 관련 값을 받음
    onWalletDisconnectButtonClick: () -> Unit // 지갑 해제 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val connected = isWalletConnected || uiState.walletUi.isConnected // 지갑 연결 상태를 정함
    val visibleWalletAddress = walletAddress.ifBlank { uiState.walletUi.walletAddress } // 표시할 지갑 주소를 정함
    val visibleWalletProvider = walletProvider.ifBlank { uiState.walletUi.walletProvider.ifBlank { "연결된 지갑" } } // 표시할 지갑 이름을 정함
    var showWalletDisconnectDialog by remember { mutableStateOf(false) } // 지갑 해제 확인 팝업 표시 여부를 저장함
    val isDark = isAppDarkTheme() // 다크모드인지 확인함
    val walletHeaderBg = if (isDark) Color(0xFF102A36) else Color(0xFFEFF6FF) // 헤더 배경색을 정함
    val walletHeaderBorder = walletCardBorderColor() // 헤더 테두리색을 정함
    val walletIconBg = if (isDark) Color(0xFF164E63) else Color(0xFFDFF4FF) // 아이콘 배경색을 정함
    val walletIconColor = if (isDark) Color(0xFF67E8F9) else Color(0xFF0369A1) // 아이콘 색을 정함
    val linkedBorder = walletLinkedCardBorderColor() // 연결 카드 테두리색을 정함
    val linkedBg = if (isDark) Color(0xFF052E16) else Color(0xFFF0FDF4) // 연결 카드 배경색을 정함
    val browserBorder = walletBrowserCardBorderColor(connected) // 브라우저 카드 테두리색을 정함
    val browserBg = if (connected) {
        if (isDark) Color(0xFF102A36) else Color(0xFFEFF6FF)
    } else {
        if (isDark) Color(0xFF111827) else Color(0xFFF8FAFC)
    } // 브라우저 카드 배경색을 정함
    val guideBg = if (isDark) Color(0xFF24133F) else Color(0xFFFAF5FF) // 혜택 카드 배경색을 정함
    val guideBorder = walletGuideCardBorderColor() // 혜택 카드 테두리색을 정함
    val outerBorder = walletCardBorderColor() // 바깥 카드 테두리색을 정함

    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
            border = BorderStroke(1.5.dp, outerBorder) // border 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(16.dp), // verticalArrangement 값을 정해줌
                horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                WalletHeaderBox( // 지갑 관리 헤더를 보여줌
                    title = "지갑 관리",
                    desc = "계정에 등록된 지갑과 앱에 연결된 지갑을 구분해서 관리합니다.",
                    iconTint = walletIconColor,
                    iconBg = walletIconBg,
                    container = walletHeaderBg,
                    border = walletHeaderBorder
                )

                if (!connected) { // 조건이 맞는지 확인함
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth()
                            .border(
                                width = 1.dp, // width 값을 정해줌
                                color = browserBorder, // color 값을 정해줌
                                shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                            )
                            .background(browserBg, RoundedCornerShape(14.dp))
                            .padding(horizontal = 20.dp, vertical = 24.dp), // .padding(horizontal 값을 정해줌
                        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Column( // 안쪽 UI를 세로로 배치함
                            horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
                            verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Icon(
                                imageVector = Icons.Outlined.AccountBalanceWallet,
                                contentDescription = "지갑",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(42.dp)
                            )

                            Text( // 화면에 글자를 보여줌
                                text = "지갑이 연결되지 않았어요", // text 값을 정해줌
                                fontSize = 20.sp, // fontSize 값을 정해줌
                                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                            )

                            Text( // 화면에 글자를 보여줌
                                text = "지갑을 연결하면 NFT 거래와\n블록체인 기능을 이용할 수 있어요", // text 값을 정해줌
                                fontSize = 14.sp, // fontSize 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                            )

                            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                                    .background(
                                        color = MaterialTheme.colorScheme.primary, // color 값을 정해줌
                                        shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
                                    )
                                    .clickable { // 이 블록 안의 내용이 시작됨
                                        onWalletConnectButtonClick() // 지갑 관련 함수를 실행함
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp) // .padding(horizontal 값을 정해줌
                            ) { // 이 블록 안의 내용이 시작됨
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Link,
                                        contentDescription = "지갑 연결",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text( // 화면에 글자를 보여줌
                                        text = "지갑 연결하기", // text 값을 정해줌
                                        color = Color.White, // color 값을 정해줌
                                        fontSize = 14.sp, // fontSize 값을 정해줌
                                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                                    )
                                }
                            }
                        }
                    }

                    WalletInfoCard( // 브라우저 지갑 상태 카드를 보여줌
                        title = "브라우저 지갑",
                        value = "연결 안 됨",
                        desc = "지갑 연동을 시작하면 앱 지갑 선택창이 열립니다.",
                        iconType = WalletInfoIcon.BROWSER,
                        borderColor = browserBorder,
                        containerColor = browserBg
                    )
                } else { // 이 블록 안의 내용이 시작됨
                    WalletInfoCard( // 연동된 지갑 카드를 보여줌
                        title = "연동된 지갑",
                        value = visibleWalletAddress.ifBlank { "-" },
                        desc = "서비스 계정에 등록된 지갑 주소입니다.",
                        iconType = WalletInfoIcon.LINKED,
                        walletProvider = visibleWalletProvider,
                        borderColor = linkedBorder,
                        containerColor = linkedBg
                    )

                    WalletInfoCard( // 브라우저 지갑 상태 카드를 보여줌
                        title = "브라우저 지갑",
                        value = "${formatWalletAddress(visibleWalletAddress)} 연결됨",
                        desc = "연동된 지갑이 앱에도 연결되어 있어요.",
                        iconType = WalletInfoIcon.BROWSER,
                        walletProvider = visibleWalletProvider,
                        borderColor = browserBorder,
                        containerColor = browserBg
                    )

                    Box( // 지갑 연동 해제 버튼을 보여줌
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isDark) Color(0xFF7F1D1D) else Color(0xFFDC2626),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                color = if (isDark) Color.Transparent else Color(0xFFFFF1F2),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { showWalletDisconnectDialog = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LinkOff,
                                contentDescription = "지갑 연동 해제",
                                tint = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "지갑 연동 해제",
                                color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = guideBg), // colors 값을 정해줌
            border = BorderStroke(1.5.dp, guideBorder) // border 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "지갑 연결 혜택",
                        tint = walletIconColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text( // 화면에 글자를 보여줌
                        text = "지갑 연결 혜택", // text 값을 정해줌
                        fontSize = 18.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )
                }

                WalletBenefitText(text = "• NFT로 아바타 아이템 발행") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 마켓에서 자유롭게 거래") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 블록체인 기반 수익 흐름 적용") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 지갑으로 간편 로그인") // 화면에 글자를 보여줌
            }
        }
    }

    if (showWalletDisconnectDialog) {
        AlertDialog(
            onDismissRequest = {
                showWalletDisconnectDialog = false
            },
            title = {
                Text(text = "지갑을 해제하시겠습니까?")
            },
            text = {
                Column {
                    Text(text = "현재 연결된 지갑: ${formatWalletAddress(visibleWalletAddress)}")

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(text = "해제 후 기존 지갑 재등록 및 새로운 지갑 등록이 가능합니다.")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showWalletDisconnectDialog = false
                        onWalletDisconnectButtonClick()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "지갑 해제",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showWalletDisconnectDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "취소", fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }
}

// 지갑 카드 아이콘 타입
private enum class WalletInfoIcon {
    LINKED,
    BROWSER
}

// 지갑 관리 헤더
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletHeaderBox( // WalletHeaderBox 함수를 선언함
    title: String, // 제목을 받음
    desc: String, // 설명을 받음
    iconTint: Color, // 아이콘 색을 받음
    iconBg: Color, // 아이콘 배경색을 받음
    container: Color, // 배경색을 받음
    border: Color // 테두리색을 받음
) { // 이 블록 안의 내용이 시작됨
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(container, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(iconBg, RoundedCornerShape(10.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.AccountBalanceWallet,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 지갑 정보 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletInfoCard( // WalletInfoCard 함수를 선언함
    title: String, // 제목을 받음
    value: String, // 값을 받음
    desc: String, // 설명을 받음
    iconType: WalletInfoIcon, // 아이콘 타입을 받음
    walletProvider: String = "", // 지갑 이름을 받음
    borderColor: Color, // 테두리색을 받음
    containerColor: Color // 배경색을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isAppDarkTheme() // 다크모드인지 확인함
    val walletLogoRes = walletProviderIconRes(walletProvider) // 지갑 이름에 맞는 로고를 정함
    val iconBg = when (iconType) {
        WalletInfoIcon.LINKED -> if (isDark) Color(0xFF14532D) else Color(0xFFDCFCE7)
        WalletInfoIcon.BROWSER -> if (isDark) Color(0xFF164E63) else Color(0xFFE0F2FE)
    } // 아이콘 배경색을 정함
    val iconColor = when (iconType) {
        WalletInfoIcon.LINKED -> if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
        WalletInfoIcon.BROWSER -> if (isDark) Color(0xFF67E8F9) else Color(0xFF0369A1)
    } // 아이콘 색을 정함

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(iconBg, RoundedCornerShape(8.dp))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (walletLogoRes != null) { // 지갑 로고가 있으면 로고를 보여줌
                        Image(
                            painter = painterResource(id = walletLogoRes),
                            contentDescription = title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = if (iconType == WalletInfoIcon.LINKED) {
                                Icons.Outlined.CheckCircle
                            } else {
                                Icons.Outlined.DesktopWindows
                            },
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 지갑 혜택 텍스트
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletBenefitText( // WalletBenefitText 함수를 선언함
    text: String // text 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Text( // 화면에 글자를 보여줌
        text = text, // text 값을 text 값에 넣음
        fontSize = 14.sp, // fontSize 값을 정해줌
        color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
        fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
    )
}

// 지갑 주소를 보기 좋게 줄여서 표시하는 함수
private fun formatWalletAddress(address: String): String { // formatWalletAddress 함수를 선언함
    return if (address.length <= 10) { // 이 값을 함수 결과로 돌려줌
        address
    } else { // 이 블록 안의 내용이 시작됨
        "${address.take(4)}...${address.takeLast(4)}"
    }
}

private fun walletProviderIconRes(walletProvider: String): Int? { // 지갑 이름에 맞는 로고 리소스를 돌려줌
    return when (walletProvider.uppercase()) {
        "PHANTOM" -> R.drawable.ic_wallet_phantom_logo
        "SOLFLARE" -> R.drawable.ic_wallet_solflare_logo
        "BACKPACK" -> R.drawable.ic_wallet_backpack_logo
        else -> null
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun isAppDarkTheme(): Boolean { // 앱에 적용된 실제 테마가 다크인지 확인함
    return MaterialTheme.colorScheme.background == Color(0xFF090B16)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun profileCardBorderColor(): Color { // 프로필 카드 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFFA78BFA) else Color(0xFF2563EB)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun memberCardBorderColor(): Color { // 회원 정보 카드 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFF8B5CF6) else Color(0xFF1D4ED8)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun notificationCardBorderColor(): Color { // 알림 카드 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFFC084FC) else Color(0xFF0284C7)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun passwordCardBorderColor(): Color { // 비밀번호 카드 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFF9333EA) else Color(0xFF0EA5E9)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun profileStatCardBorderColor(): Color { // 프로필 통계 카드 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFF7C3AED) else Color(0xFF60A5FA)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun passwordGuideCardBorderColor(): Color { // 비밀번호 안내 카드 테두리색을 돌려줌
    return myPageAccentBorderColor()
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun walletCardBorderColor(): Color { // 지갑 카드 테두리색을 돌려줌
    return myPageAccentBorderColor()
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun walletLinkedCardBorderColor(): Color { // 연동된 지갑 카드 테두리색을 돌려줌
    return myPageAccentBorderColor()
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun walletBrowserCardBorderColor(connected: Boolean): Color { // 브라우저 지갑 카드 테두리색을 돌려줌
    return if (connected) {
        myPageAccentBorderColor()
    } else {
        if (isAppDarkTheme()) Color(0xFF6D28D9) else Color(0xFF93C5FD)
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun walletGuideCardBorderColor(): Color { // 지갑 혜택 카드 테두리색을 돌려줌
    return myPageAccentBorderColor()
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun myPageAccentBorderColor(): Color { // 지갑/안내 공통 포인트 테두리색을 돌려줌
    return if (isAppDarkTheme()) Color(0xFFA855F7) else Color(0xFF38BDF8)
}
