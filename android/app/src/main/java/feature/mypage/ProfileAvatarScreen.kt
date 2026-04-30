package com.ict.spentopia.feature.mypage

import androidx.compose.foundation.background // 수정: 탭 배경 표현을 위해 사용
import androidx.compose.foundation.layout.Arrangement // 수정: 탭 간격 정렬을 위해 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 레이아웃 구성을 위해 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 탭 배치를 위해 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가를 위해 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 전체 너비 사용을 위해 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백 적용을 위해 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 탭 모양 적용을 위해 사용
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // 수정: 탭 텍스트 표시를 위해 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.runtime.getValue // 수정: state 위임 사용
import androidx.compose.runtime.mutableStateOf // 수정: 현재 선택 탭 상태 저장
import androidx.compose.runtime.remember // 수정: Compose 상태 유지
import androidx.compose.runtime.setValue // 수정: state 위임 사용
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.ui.draw.clip // 수정: 둥근 모양 클리핑 적용을 위해 사용
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color // 수정: 색상 적용을 위해 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 선택 탭 강조를 위해 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기 지정에 사용
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // 수정: SolanaWalletType import 추가
import com.ict.spentopia.feature.avatar.AvatarScreen // 수정: 내 아바타 콘텐츠를 불러오기 위해 사용

// 수정: 프로필 화면 내부 탭 상태 정의
private enum class ProfileTab {
    MY_PAGE, // 수정: 마이페이지 탭 상태
    AVATAR // 수정: 내 아바타 탭 상태
}

// 기존 주석 유지
// 마이페이지 / 내 아바타 통합 화면
@Composable
fun ProfileAvatarScreen(
    // 바꿀 것 1: ProfileAvatarScreen 파라미터 변경
    isWalletConnected: Boolean = false,
    walletAddress: String = "",
    walletProvider: String = "",
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(ProfileTab.MY_PAGE) } // 수정: 기본 진입을 마이페이지로 설정

    Column(
        modifier = Modifier.fillMaxWidth() // 수정: 전체 너비 사용
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp)) // 수정: 캡슐형 탭 배경 적용
                .background(MaterialTheme.colorScheme.surfaceVariant) // 수정: 테마 기반 탭 배경 적용
                .padding(4.dp), // 수정: 탭 내부 패딩 적용
            horizontalArrangement = Arrangement.spacedBy(6.dp) // 수정: 탭 간격 지정
        ) {
            ProfileTabButton(
                text = "마이페이지", // 수정: 첫 번째 탭 텍스트
                selected = selectedTab == ProfileTab.MY_PAGE, // 수정: 선택 상태 비교
                onClick = {
                    selectedTab = ProfileTab.MY_PAGE // 수정: 클릭 시 마이페이지 탭으로 전환
                }
            )

            ProfileTabButton(
                text = "내 아바타", // 수정: 두 번째 탭 텍스트
                selected = selectedTab == ProfileTab.AVATAR, // 수정: 선택 상태 비교
                onClick = {
                    selectedTab = ProfileTab.AVATAR // 수정: 클릭 시 내 아바타 탭으로 전환
                }
            )
        }

        Spacer(modifier = Modifier.padding(top = 14.dp)) // 수정: 탭과 본문 사이 여백

        when (selectedTab) {
            ProfileTab.MY_PAGE -> {
                // 바꿀 것 2: MyPageScreen 호출부 변경 (지갑 정보 전달)
                MyPageScreen(
                    isWalletConnected = isWalletConnected,
                    walletAddress = walletAddress,
                    walletProvider = walletProvider,
                    onWalletConnectClick = onWalletConnectClick
                )
            }

            ProfileTab.AVATAR -> {
                AvatarScreen() // 수정: 내 아바타 탭 선택 시 아바타 본문 표시
            }
        }
    }
}

// 수정: 프로필 전환용 탭 버튼 UI
@Composable
private fun ProfileTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    androidx.compose.material3.TextButton(
        onClick = {
            onClick() // 수정: 탭 클릭 이벤트 실행
        },
        interactionSource = interactionSource,
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp)) // 수정: 탭 버튼 둥근 모양 적용
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            )
            .graphicsLayer {
                scaleX = if (pressed) 0.985f else 1f
                scaleY = if (pressed) 0.985f else 1f
            }
    ) {
        Text(
            text = text, // 수정: 탭 이름 표시
            fontSize = 14.sp, // 수정: 탭 폰트 크기 지정
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, // 수정: 선택 탭 강조
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
