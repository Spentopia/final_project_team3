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
import androidx.compose.foundation.rememberScrollState // 스크롤 상태를 기억하는 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // 세로 스크롤 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.outlined.AccountBalanceWallet // 가계부 아이콘을 가져옴
import androidx.compose.material.icons.outlined.BarChart // 분석 아이콘을 가져옴
import androidx.compose.material.icons.outlined.ChevronRight // 오른쪽 화살표 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Forum // 커뮤니티 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Hexagon // NFT 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Key // 게임 코드 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Logout // 로그아웃 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Person // 마이페이지 아이콘을 가져옴
import androidx.compose.material.icons.outlined.Savings // 예산 아이콘을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Surface // Surface 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.draw.shadow // shadow 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.graphics.vector.ImageVector // ImageVector 타입을 가져옴
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
    val isDark = MaterialTheme.colorScheme.background == SpentopiaDarkBackground
    val brandColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxHeight()
            .fillMaxWidth(0.82f)
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
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
                    color = brandColor // color 값을 정해줌
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

        Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.AccountBalanceWallet, // icon 값을 정해줌
            title = "가계부", // 제목을 정해줌
            tone = DrawerMenuTone.YELLOW,
            onClick = onLedgerClick // onLedgerClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Savings, // icon 값을 정해줌
            title = "예산 설정", // 제목을 정해줌
            tone = DrawerMenuTone.ORANGE,
            onClick = onBudgetClick // 예산 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.BarChart, // icon 값을 정해줌
            title = "소비 분석", // 제목을 정해줌
            tone = DrawerMenuTone.BLUE,
            onClick = onAnalysisClick // onAnalysisClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Person, // icon 값을 정해줌
            title = "마이페이지 / 내 아바타", // 제목을 정해줌
            tone = DrawerMenuTone.MINT,
            onClick = onProfileAvatarClick // 아바타 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Hexagon, // icon 값을 정해줌
            title = "NFT 마켓", // 제목을 정해줌
            tone = DrawerMenuTone.PURPLE,
            onClick = onMarketClick // 마켓 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Key, // icon 값을 정해줌
            title = "게임 코드", // 제목을 정해줌
            tone = DrawerMenuTone.INDIGO,
            onClick = onPlazaClick // onPlazaClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Forum, // icon 값을 정해줌
            title = "커뮤니티", // 제목을 정해줌
            tone = DrawerMenuTone.GRAY,
            onClick = onCommunityClick // 커뮤니티 관련 값을 눌렀을 때 실행할 함수에 넣음
        )

        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

        DrawerMenuItem( // Drawer Menu Item 함수를 실행함
            icon = Icons.Outlined.Logout, // icon 값을 정해줌
            title = "로그아웃", // 제목을 정해줌
            tone = DrawerMenuTone.RED,
            onClick = onLogoutClick // onLogoutClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        )
    }
}

private enum class DrawerMenuTone {
    YELLOW,
    ORANGE,
    BLUE,
    MINT,
    PURPLE,
    INDIGO,
    GRAY,
    RED
}

private data class DrawerMenuColors(
    val icon: Color,
    val iconBackground: Color,
    val card: Color,
    val border: Color,
    val chevron: Color
)

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun DrawerMenuItem( // DrawerMenuItem 함수를 선언함
    icon: ImageVector, // icon 값을 받음
    title: String, // 제목을 받음
    tone: DrawerMenuTone, // 메뉴 색상 타입을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val colors = drawerMenuColors(tone) // 메뉴 색상 값을 저장함
    Surface( // Surface 함수를 실행함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .padding(vertical = 5.dp) // .padding(vertical 값을 정해줌
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
        border = BorderStroke(1.dp, colors.border), // 메뉴 카드 테두리색을 정함
        tonalElevation = 1.dp, // tonalElevation 값을 정해줌
        color = colors.card // color 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 15.dp), // .padding(horizontal 값을 정해줌
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Surface( // Surface 함수를 실행함
                modifier = Modifier
                    .size(40.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(13.dp),
                        ambientColor = Color.Black.copy(alpha = 0.04f),
                        spotColor = Color.Black.copy(alpha = 0.04f)
                    ), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(13.dp), // shape 값을 정해줌
                color = colors.iconBackground // color 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.Center, // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.icon,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.size(14.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium, // style 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = colors.chevron,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun drawerMenuColors(tone: DrawerMenuTone): DrawerMenuColors {
    val isDark = MaterialTheme.colorScheme.background == SpentopiaDarkBackground
    val accent = when (tone) {
        DrawerMenuTone.YELLOW -> if (isDark) Color(0xFFFACC15) else Color(0xFFD97706)
        DrawerMenuTone.ORANGE -> if (isDark) Color(0xFFFB923C) else Color(0xFFEA580C)
        DrawerMenuTone.BLUE -> if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
        DrawerMenuTone.MINT -> if (isDark) Color(0xFF5EEAD4) else Color(0xFF0F766E)
        DrawerMenuTone.PURPLE -> if (isDark) Color(0xFFC084FC) else Color(0xFF7C3AED)
        DrawerMenuTone.INDIGO -> if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)
        DrawerMenuTone.GRAY -> if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)
        DrawerMenuTone.RED -> if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626)
    }

    return if (isDark) {
        DrawerMenuColors(
            icon = accent,
            iconBackground = accent.copy(alpha = 0.16f),
            card = Color(0xFF101827),
            border = Color(0xFF7C3AED).copy(alpha = 0.20f),
            chevron = Color(0xFF94A3B8).copy(alpha = 0.72f)
        )
    } else {
        DrawerMenuColors(
            icon = accent,
            iconBackground = accent.copy(alpha = 0.12f),
            card = Color.White,
            border = Color(0xFFEAF2FF),
            chevron = Color(0xFFCBD5E1)
        )
    }
}
