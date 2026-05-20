package com.ict.spentopia.feature.mypage // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.ict.spentopia.feature.avatar.AvatarScreen // AvatarScreen 기능을 가져옴

// 수정: 프로필 화면 내부 탭 상태 정의
private enum class ProfileTab { // ProfileTab에서 고를 수 있는 값들을 묶음
    MY_PAGE,
    AVATAR
}

// 기존 주석 유지
// 마이페이지 / 내 아바타 통합 화면
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun ProfileAvatarScreen( // ProfileAvatarScreen 함수를 선언함
    // 바꿀 것 1: ProfileAvatarScreen 파라미터 변경
    isWalletConnected: Boolean = false, // 지갑 관련 값을 받음
    walletAddress: String = "", // 지갑 주소를 받음
    walletProvider: String = "", // 지갑 이름을 받음
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 지갑 관련 값을 받음
    onWalletDisconnectClick: () -> Unit = {} // 지갑 해제 값을 받음
) { // 이 블록 안의 내용이 시작됨
    var selectedTab by remember { mutableStateOf(ProfileTab.MY_PAGE) } // 화면에서 바뀔 selectedTab 값을 저장함

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxWidth() // UI 크기나 여백 같은 모양을 정함
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(999.dp)
                )
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp) // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            ProfileTabButton( // 누를 수 있는 버튼을 만듦
                text = "내 프로필", // text 값을 정해줌
                selected = selectedTab == ProfileTab.MY_PAGE, // selected 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    selectedTab = ProfileTab.MY_PAGE // selectedTab 값을 정해줌
                }
            )

            ProfileTabButton( // 누를 수 있는 버튼을 만듦
                text = "내 아바타 아이템", // text 값을 정해줌
                selected = selectedTab == ProfileTab.AVATAR, // selected 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    selectedTab = ProfileTab.AVATAR // selectedTab 값을 정해줌
                }
            )
        }

        Spacer(modifier = Modifier.padding(top = 14.dp)) // UI 크기나 여백 같은 모양을 정함

        when (selectedTab) { // 값 종류에 따라 실행할 코드를 나눔
            ProfileTab.MY_PAGE -> { // 이 블록 안의 내용이 시작됨
                // 바꿀 것 2: MyPageScreen 호출부 변경 (지갑 정보 전달)
                MyPageScreen( // My Page Screen 함수를 실행함
                    isWalletConnected = isWalletConnected, // 지갑 값을 요청값에 넣음
                    walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                    walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                    onWalletConnectClick = onWalletConnectClick, // 지갑 값을 요청값에 넣음
                    onWalletDisconnectClick = onWalletDisconnectClick // 지갑 해제 값을 요청값에 넣음
                )
            }

            ProfileTab.AVATAR -> { // 이 블록 안의 내용이 시작됨
                AvatarScreen() // Avatar Screen 함수를 실행함
            }
        }
    }
}

// 수정: 프로필 전환용 탭 버튼 UI
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ProfileTabButton( // ProfileTabButton 함수를 선언함
    text: String, // text 값을 받음
    selected: Boolean, // selected 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    androidx.compose.material3.TextButton( // 누를 수 있는 버튼을 만듦
        onClick = { // 눌렀을 때 실행할 함수를 정해줌
            onClick() // on Click 함수를 실행함
        },
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .clip(RoundedCornerShape(999.dp))
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) MaterialTheme.colorScheme.outline else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent // 조건이 맞는지 확인함
            )
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
            }
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, // fontWeight 값을 정해줌
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )
    }
}
