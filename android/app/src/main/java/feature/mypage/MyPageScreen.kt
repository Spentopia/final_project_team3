package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

// 마이페이지 화면임
// 프로필/아바타/설정/테마 관리

import android.net.Uri // 이미지 주소 타입을 가져옴
import androidx.activity.compose.rememberLauncherForActivityResult // rememberLauncherForActivityResult 기능을 가져옴
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultContracts 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.isSystemInDarkTheme // isSystemInDarkTheme 기능을 가져옴
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
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.Switch // Switch 기능을 가져옴
import androidx.compose.material3.SwitchDefaults // SwitchDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
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
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import coil.compose.AsyncImage // AsyncImage 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // SolanaWalletDialog 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴

// 기존 주석 유지
// 마이페이지 화면
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun MyPageScreen( // MyPageScreen 함수를 선언함
    isWalletConnected: Boolean = false, // 지갑 관련 값을 받음
    walletAddress: String = "", // 지갑 주소를 받음
    walletProvider: String = "", // 지갑 이름을 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 지갑 관련 값을 받음
    myPageViewModel: MyPageViewModel = viewModel() // myPageViewModel 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val uiState = myPageViewModel.uiState // 화면 상태를 저장함

    var showWalletDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 지갑 관련 값을 저장함

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
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // color 값을 정해줌
                    shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp) // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            MyPageTopTabButton( // 누를 수 있는 버튼을 만듦
                text = "프로필", // text 값을 정해줌
                selected = uiState.selectedTab == MyPageTab.PROFILE, // selected 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    myPageViewModel.onTabChange(MyPageTab.PROFILE)
                }
            )

            MyPageTopTabButton( // 누를 수 있는 버튼을 만듦
                text = "알림", // text 값을 정해줌
                selected = uiState.selectedTab == MyPageTab.NOTIFICATION, // selected 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    myPageViewModel.onTabChange(MyPageTab.NOTIFICATION)
                }
            )

            MyPageTopTabButton( // 누를 수 있는 버튼을 만듦
                text = "지갑", // text 값을 정해줌
                selected = uiState.selectedTab == MyPageTab.WALLET, // selected 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    myPageViewModel.onTabChange(MyPageTab.WALLET)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

        when (uiState.selectedTab) { // 값 종류에 따라 실행할 코드를 나눔
            MyPageTab.PROFILE -> { // 이 블록 안의 내용이 시작됨
                ProfileTabContent( // Profile Tab Content 함수를 실행함
                    uiState = uiState, // 화면 상태를 화면 상태에 넣음
                    myPageViewModel = myPageViewModel, // myPageViewModel 값을 myPageViewModel 값에 넣음
                    onProfileImageClick = { imageLauncher.launch("image/*") } // 프로필 이미지를 누르면 갤러리를 열게 정함
                )
            }

            MyPageTab.NOTIFICATION -> { // 이 블록 안의 내용이 시작됨
                NotificationTabContent( // Notification Tab Content 함수를 실행함
                    uiState = uiState, // 화면 상태를 화면 상태에 넣음
                    onBudgetAlertChange = myPageViewModel::onBudgetAlertChange, // 예산 관련 값을 정해줌
                    onRewardAlertChange = myPageViewModel::onRewardAlertChange, // onRewardAlertChange 때 실행할 함수를 정해줌
                    onStreakReminderChange = myPageViewModel::onStreakReminderChange, // onStreakReminderChange 때 실행할 함수를 정해줌
                    onMarketingAlertChange = myPageViewModel::onMarketingAlertChange // 마켓 관련 값을 정해줌
                )
            }

            MyPageTab.WALLET -> { // 이 블록 안의 내용이 시작됨
                WalletTabContent( // 지갑 관련 함수를 실행함
                    uiState = uiState, // 화면 상태를 화면 상태에 넣음
                    isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                    walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                    walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                    onWalletConnectButtonClick = { // 지갑 관련 값을 정해줌
                        showWalletDialog = true // true 값을 지갑 관련 값에 넣음
                    }
                )
            }
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

// 프로필 탭 본문
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ProfileTabContent( // ProfileTabContent 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    myPageViewModel: MyPageViewModel, // myPageViewModel 값을 받음
    onProfileImageClick: () -> Unit // onProfileImageClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        ProfileHeaderCard( // 내용을 카드 모양으로 묶어서 보여줌
            uiState = uiState, // 화면 상태를 화면 상태에 넣음
            onProfileImageClick = onProfileImageClick // onProfileImageClick 때 실행할 함수를 onProfileImageClick 때 실행할 함수에 넣음
        )
        MemberInfoCard( // 내용을 카드 모양으로 묶어서 보여줌
            uiState = uiState, // 화면 상태를 화면 상태에 넣음
            viewModel = myPageViewModel // myPageViewModel 값을 화면 데이터 관리자에 넣음
        )
        SocialAccountCard(uiState = uiState) // 내용을 카드 모양으로 묶어서 보여줌
    }
}

// 상단 프로필 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ProfileHeaderCard( // ProfileHeaderCard 함수를 선언함
    uiState: MyPageUiState, // 화면 상태를 받음
    onProfileImageClick: () -> Unit // onProfileImageClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) // border 값을 정해줌
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
                title = "보유 아바타", // 제목을 정해줌
                value = uiState.profileSummary.avatarCountText // 입력값을 정해줌
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
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(bottom = 10.dp), // .padding(bottom 값을 정해줌
            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors( // colors 값을 정해줌
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f) // containerColor 값을 정해줌
            )
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
    val isDark = isSystemInDarkTheme() // 다크모드인지 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
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
                            color = if (isDark) Color(0xFF6D28D9) else MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                            shape = RoundedCornerShape(10.dp) // shape 값을 정해줌
                        )
                        .clickable { // 이 블록 안의 내용이 시작됨
                            viewModel.toggleEditMode()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = if (uiState.isEditMode) "💾 저장" else "✏️ 수정", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.onPrimaryContainer // color 값을 정해줌
                    )
                }
            }

            if (uiState.isEditMode) { // 조건이 맞는지 확인함
                EditableField( // Editable Field 함수를 실행함
                    label = "이름", // label 값을 정해줌
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
                    }
                )
            } else { // 이 블록 안의 내용이 시작됨
                MemberField(label = "이름", value = uiState.memberInfo.name) // MemberField(label 값을 정해줌
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
    onValueChange: (String) -> Unit // onValueChange 때 실행할 함수를 받음
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
            singleLine = true, // true 값을 singleLine 값에 넣음
            shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
        )
    }
}

// 소셜 계정 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SocialAccountCard( // SocialAccountCard 함수를 선언함
    uiState: MyPageUiState // 화면 상태를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(16.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "소셜 계정 연동", // text 값을 정해줌
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            uiState.socialAccounts.forEach { account ->
                SocialAccountRow( // 안쪽 UI를 가로로 배치함
                    serviceName = account.serviceName, // serviceName 값을 정해줌
                    connected = account.connected // connected 값을 정해줌
                )
            }
        }
    }
}

// 소셜 계정 한 줄
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SocialAccountRow( // SocialAccountRow 함수를 선언함
    serviceName: String, // serviceName 값을 받음
    connected: Boolean // connected 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
            verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
            horizontalArrangement = Arrangement.SpaceBetween // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .size(40.dp)
                        .background(
                            color = when (serviceName) { // color 값을 정해줌
                                "카카오" -> Color(0xFFF7C600)
                                else -> Color(0xFF4285F4) // 위 조건이 아니면 이쪽을 실행함
                            },
                            shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                        ),
                    contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = if (serviceName == "카카오") "💬" else serviceName.first().toString(), // text 값을 정해줌
                        color = Color.White, // color 값을 정해줌
                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                    )
                }

                Column { // 안쪽 UI를 세로로 배치함
                    Text( // 화면에 글자를 보여줌
                        text = serviceName, // serviceName 값을 text 값에 넣음
                        fontSize = 16.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )

                    Text( // 화면에 글자를 보여줌
                        text = if (connected) "연동됨" else "미연동", // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                }
            }

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .background(
                        color = if (connected) Color(0xFF22C55E) else Color(0xFFF3F4F6), // color 값을 정해줌
                        shape = RoundedCornerShape(10.dp) // shape 값을 정해줌
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = if (connected) "연동됨" else "연동", // text 값을 정해줌
                    color = if (connected) Color.White else MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                )
            }
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
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
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
                title = "마케팅 알림", // 제목을 정해줌
                desc = "이벤트와 프로모션 정보를 받아보세요", // desc 값을 정해줌
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
    onWalletConnectButtonClick: () -> Unit // 지갑 관련 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isSystemInDarkTheme() // 다크모드인지 저장함

    Column( // 안쪽 UI를 세로로 배치함
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(16.dp), // verticalArrangement 값을 정해줌
                horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "지갑 관리", // text 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                    modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
                )

                if (!isWalletConnected) { // 조건이 맞는지 확인함
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth()
                            .border(
                                width = 1.dp, // width 값을 정해줌
                                color = MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                                shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                            )
                            .padding(horizontal = 20.dp, vertical = 24.dp), // .padding(horizontal 값을 정해줌
                        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Column( // 안쪽 UI를 세로로 배치함
                            horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
                            verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 화면에 글자를 보여줌
                                text = "👛", // text 값을 정해줌
                                fontSize = 42.sp // fontSize 값을 정해줌
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
                                        color = if (isDark) Color(0xFF7C3AED) else Color(0xFF2563EB), // color 값을 정해줌
                                        shape = RoundedCornerShape(12.dp) // shape 값을 정해줌
                                    )
                                    .clickable { // 이 블록 안의 내용이 시작됨
                                        onWalletConnectButtonClick() // 지갑 관련 함수를 실행함
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp) // .padding(horizontal 값을 정해줌
                            ) { // 이 블록 안의 내용이 시작됨
                                Text( // 화면에 글자를 보여줌
                                    text = "🔗 지갑 연결하기", // text 값을 정해줌
                                    color = Color.White, // color 값을 정해줌
                                    fontSize = 14.sp, // fontSize 값을 정해줌
                                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                                )
                            }
                        }
                    }
                } else { // 이 블록 안의 내용이 시작됨
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant, // color 값을 정해줌
                                shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                            )
                            .padding(16.dp)
                    ) { // 이 블록 안의 내용이 시작됨
                        Column( // 안쪽 UI를 세로로 배치함
                            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 화면에 글자를 보여줌
                                text = "연결된 지갑", // text 값을 정해줌
                                fontSize = 14.sp, // fontSize 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                                fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
                            )

                            Text( // 화면에 글자를 보여줌
                                text = walletProvider, // 지갑 이름을 text 값에 넣음
                                fontSize = 18.sp, // fontSize 값을 정해줌
                                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                            )

                            Text( // 화면에 글자를 보여줌
                                text = formatWalletAddress(walletAddress), // text 값을 정해줌
                                fontSize = 13.sp, // fontSize 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                            )
                        }
                    }
                }
            }
        }

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "💡 지갑 연결 혜택", // text 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                WalletBenefitText(text = "• NFT로 아바타 아이템 발행") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 마켓에서 자유롭게 거래") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 블록체인 기반 수익 흐름 적용") // 화면에 글자를 보여줌
                WalletBenefitText(text = "• 지갑으로 간편 로그인") // 화면에 글자를 보여줌
            }
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
