package com.ict.spentopia.navigation // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxHeight // fillMaxHeight 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Surface // Surface 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AppDrawerContent( // AppDrawerContent 함수를 선언함
    onCloseClick: () -> Unit, // onCloseClick 때 실행할 함수를 받음
    onLedgerClick: () -> Unit, // onLedgerClick 때 실행할 함수를 받음
    onBudgetClick: () -> Unit, // 예산 관련 값을 받음
    onAnalysisClick: () -> Unit, // onAnalysisClick 때 실행할 함수를 받음
    onProfileAvatarClick: () -> Unit, // 아바타 관련 값을 받음
    onMarketClick: () -> Unit, // 마켓 관련 값을 받음
    onPlazaClick: () -> Unit, // onPlazaClick 때 실행할 함수를 받음
    onCommunityClick: () -> Unit, // 커뮤니티 관련 값을 받음
    onLogoutClick: () -> Unit // onLogoutClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxHeight()
            .fillMaxWidth(0.82f)
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
        verticalArrangement = Arrangement.Top // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨

        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column { // 안쪽 UI를 세로로 배치함
                Text( // 화면에 글자를 보여줌
                    text = "Spentopia", // text 값을 정해줌
                    style = MaterialTheme.typography.headlineSmall, // style 값을 정해줌
                    fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
                Text( // 화면에 글자를 보여줌
                    text = "원하는 메뉴로 바로 이동해보세요", // text 값을 정해줌
                    style = MaterialTheme.typography.bodyMedium, // style 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = "닫기", // text 값을 정해줌
                color = MaterialTheme.colorScheme.primary, // color 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onCloseClick() }
                    .padding(horizontal = 8.dp, vertical = 6.dp) // .padding(horizontal 값을 정해줌
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // UI 크기나 여백 같은 모양을 정함

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "📒", // emoji 값을 정해줌
            title = "가계부", // 제목을 정해줌
            onClick = onLedgerClick // onLedgerClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "💰", // emoji 값을 정해줌
            title = "예산 설정", // 제목을 정해줌
            onClick = onBudgetClick // 예산 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "📊", // emoji 값을 정해줌
            title = "소비 분석", // 제목을 정해줌
            onClick = onAnalysisClick // onAnalysisClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "🧍", // emoji 값을 정해줌
            title = "마이페이지 / 내 아바타", // 제목을 정해줌
            onClick = onProfileAvatarClick // 아바타 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "🖼️", // emoji 값을 정해줌
            title = "NFT 마켓", // 제목을 정해줌
            onClick = onMarketClick // 마켓 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "🎮", // emoji 값을 정해줌
            title = "게임 코드", // 제목을 정해줌
            onClick = onPlazaClick // onPlazaClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "💬", // emoji 값을 정해줌
            title = "커뮤니티", // 제목을 정해줌
            onClick = onCommunityClick // 커뮤니티 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            emoji = "🚪", // emoji 값을 정해줌
            title = "로그아웃", // 제목을 정해줌
            onClick = onLogoutClick // onLogoutClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun DrawerMenuItem( // DrawerMenuItem 함수를 선언함
    emoji: String, // emoji 값을 받음
    title: String, // 제목을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val borderColor = drawerMenuBorderColor() // 메뉴 카드 테두리색을 저장함
    val iconBorderColor = drawerMenuIconBorderColor() // 메뉴 아이콘 박스 테두리색을 저장함
    val cardColor = drawerMenuCardColor() // 메뉴 카드 배경색을 저장함
    val iconSurfaceColor = drawerMenuIconSurfaceColor() // 메뉴 아이콘 박스 배경색을 저장함
    Surface( // Surface 함수를 실행함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .padding(vertical = 6.dp) // .padding(vertical 값을 정해줌
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        border = BorderStroke(1.dp, borderColor), // 메뉴 카드 테두리색을 정함
        tonalElevation = 1.dp, // tonalElevation 값을 정해줌
        color = cardColor // color 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp), // .padding(horizontal 값을 정해줌
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Surface( // Surface 함수를 실행함
                modifier = Modifier.size(38.dp), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                border = BorderStroke(1.dp, iconBorderColor), // 아이콘 박스 테두리색을 정함
                color = iconSurfaceColor // color 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.Center, // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text(text = emoji) // 화면에 글자를 보여줌
                }
            }

            Spacer(modifier = Modifier.size(12.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )
        }
    }
}

@Composable
private fun drawerMenuBorderColor(): Color {
    return if (MaterialTheme.colorScheme.background == SpentopiaDarkBackground) {
        Color(0xFF8B5CF6).copy(alpha = 0.42f) // 다크모드 메뉴 테두리색을 정함
    } else {
        Color(0xFF7DD3FC).copy(alpha = 0.72f) // 라이트모드 메뉴 테두리색을 정함
    }
}

@Composable
private fun drawerMenuIconBorderColor(): Color {
    return if (MaterialTheme.colorScheme.background == SpentopiaDarkBackground) {
        Color(0xFFC4B5FD).copy(alpha = 0.34f) // 다크모드 아이콘 테두리색을 정함
    } else {
        Color(0xFF60A5FA).copy(alpha = 0.42f) // 라이트모드 아이콘 테두리색을 정함
    }
}

@Composable
private fun drawerMenuCardColor(): Color {
    return if (MaterialTheme.colorScheme.background == SpentopiaDarkBackground) {
        Color(0xFF111A2A) // 다크모드 메뉴 카드 배경색을 정함
    } else {
        Color(0xFFF7FBFF) // 라이트모드 메뉴 카드 배경색을 정함
    }
}

@Composable
private fun drawerMenuIconSurfaceColor(): Color {
    return if (MaterialTheme.colorScheme.background == SpentopiaDarkBackground) {
        Color(0xFF1E1B4B).copy(alpha = 0.74f) // 다크모드 아이콘 박스 배경색을 정함
    } else {
        Color(0xFFE0F2FE) // 라이트모드 아이콘 박스 배경색을 정함
    }
}
