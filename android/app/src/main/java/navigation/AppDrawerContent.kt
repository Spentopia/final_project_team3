package com.ict.spentopia.navigation // 네비게이션 관련 패키지

import androidx.compose.foundation.background // 배경색 지정
import androidx.compose.foundation.clickable // 클릭 이벤트 처리
import androidx.compose.foundation.layout.Arrangement // 정렬 방식 사용
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃
import androidx.compose.foundation.layout.Spacer // 빈 공간
import androidx.compose.foundation.layout.fillMaxHeight // 부모 높이를 채움
import androidx.compose.foundation.layout.fillMaxWidth // 부모 너비를 채움
import androidx.compose.foundation.layout.height // 높이 지정
import androidx.compose.foundation.layout.padding // 안쪽 여백 지정
import androidx.compose.foundation.layout.size // 크기 지정
import androidx.compose.foundation.shape.RoundedCornerShape // 둥근 모서리 모양
import androidx.compose.material3.MaterialTheme // 머티리얼 테마 사용
import androidx.compose.material3.Surface // 카드 같은 표면 UI
import androidx.compose.material3.Text // 텍스트 표시
import androidx.compose.runtime.Composable // Compose UI 함수 표시
import androidx.compose.ui.Alignment // 정렬 기준
import androidx.compose.ui.Modifier // UI 수정자
import androidx.compose.ui.draw.clip // 모양대로 잘라내기
import androidx.compose.ui.graphics.Color // 색상 사용
import androidx.compose.ui.text.font.FontWeight // 글자 굵기 지정
import androidx.compose.ui.unit.dp // dp 단위 사용

@Composable // Compose UI 함수
fun AppDrawerContent( // 공통 드로어 메뉴 UI 함수
    onLedgerClick: () -> Unit, // 가계부 클릭 콜백
    onBudgetClick: () -> Unit, // 예산 설정 클릭 콜백
    onAnalysisClick: () -> Unit, // 소비 분석 클릭 콜백
    onProfileAvatarClick: () -> Unit, // 수정: 마이페이지/내 아바타 통합 클릭 콜백으로 변경
    onMarketClick: () -> Unit, // NFT 마켓 클릭 콜백
    onPlazaClick: () -> Unit, // 광장 클릭 콜백
    onCommunityClick: () -> Unit // 커뮤니티 클릭 콜백
) {
    Column( // 전체 드로어를 세로로 배치
        modifier = Modifier // Modifier 시작
            .fillMaxHeight() // 화면 높이 끝까지 채움
            .fillMaxWidth(0.82f) // 화면 너비의 82%만 사용
            .background(Color.White) // 배경을 흰색으로 지정
            .padding(20.dp), // 전체 안쪽 여백 20dp
        verticalArrangement = Arrangement.Top // 위에서부터 차례대로 배치
    ) {

        Row( // 상단 제목과 닫기 텍스트를 가로 배치
            modifier = Modifier.fillMaxWidth(), // 가로로 꽉 채움
            horizontalArrangement = Arrangement.SpaceBetween, // 양쪽 끝으로 배치
            verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
        ) {
            Column { // 제목과 설명을 세로로 배치
                Text( // 앱 이름 텍스트
                    text = "Spentopia", // 표시할 글자
                    style = MaterialTheme.typography.headlineSmall, // 큰 제목 스타일
                    fontWeight = FontWeight.ExtraBold // 아주 굵게 표시
                )
                Text( // 설명 텍스트
                    text = "원하는 메뉴로 바로 이동해보세요", // 안내 문구
                    style = MaterialTheme.typography.bodyMedium, // 일반 본문 스타일
                    color = Color.Gray // 회색 글자
                )
            }

            Text( // 닫기 텍스트 버튼
                text = "닫기", // 표시할 글자
                color = Color(0xFF2563EB), // 파란색 글자
                fontWeight = FontWeight.SemiBold // 반굵게 표시
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // 위쪽과 메뉴 사이 여백

        DrawerMenuItem( // 가계부 메뉴 아이템
            emoji = "📒", // 아이콘 대신 이모지 사용
            title = "가계부", // 메뉴 이름
            onClick = onLedgerClick // 클릭 시 실행할 함수
        )

        DrawerMenuItem( // 예산 설정 메뉴 아이템
            emoji = "💰", // 이모지
            title = "예산 설정", // 메뉴 이름
            onClick = onBudgetClick // 클릭 시 실행
        )

        DrawerMenuItem( // 소비 분석 메뉴 아이템
            emoji = "📊", // 이모지
            title = "소비 분석", // 메뉴 이름
            onClick = onAnalysisClick // 클릭 시 실행
        )

        DrawerMenuItem( // 수정: 마이페이지 + 내 아바타 통합 메뉴 아이템
            emoji = "🧍", // 이모지 유지
            title = "마이페이지 / 내 아바타", // 수정: 두 메뉴를 하나로 통합
            onClick = onProfileAvatarClick // 수정: 통합 클릭 콜백 실행
        )

        DrawerMenuItem( // NFT 마켓 메뉴 아이템
            emoji = "🖼️", // 이모지
            title = "NFT 마켓", // 메뉴 이름
            onClick = onMarketClick // 클릭 시 실행
        )

        DrawerMenuItem( // 광장 메뉴 아이템
            emoji = "🏛️", // 이모지
            title = "광장", // 메뉴 이름
            onClick = onPlazaClick // 클릭 시 실행
        )

        DrawerMenuItem( // 커뮤니티 메뉴 아이템
            emoji = "💬", // 이모지
            title = "커뮤니티", // 메뉴 이름
            onClick = onCommunityClick // 클릭 시 실행
        )
    }
}

@Composable // Compose UI 함수
private fun DrawerMenuItem( // 드로어 안의 메뉴 한 줄 UI
    emoji: String, // 왼쪽 이모지
    title: String, // 메뉴 제목
    onClick: () -> Unit // 클릭 이벤트
) {
    Surface( // 카드 같은 배경을 만드는 Surface
        modifier = Modifier // Modifier 시작
            .fillMaxWidth() // 가로 꽉 채움
            .padding(vertical = 6.dp) // 위아래 여백 6dp
            .clip(RoundedCornerShape(18.dp)) // 모서리를 둥글게 자름
            .clickable { onClick() }, // 클릭하면 onClick 실행
        shape = RoundedCornerShape(18.dp), // 카드 모양도 둥글게 설정
        tonalElevation = 1.dp, // 살짝 떠 보이는 효과
        color = Color(0xFFF8FAFC) // 연한 회색 배경
    ) {
        Row( // 이모지와 텍스트를 가로 배치
            modifier = Modifier // Modifier 시작
                .fillMaxWidth() // 가로 꽉 채움
                .padding(horizontal = 16.dp, vertical = 18.dp), // 안쪽 여백 지정
            verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
        ) {
            Surface( // 이모지 배경 원형/둥근 사각형 역할
                modifier = Modifier.size(38.dp), // 크기 38dp
                shape = RoundedCornerShape(12.dp), // 둥근 모서리
                color = Color(0xFFE2E8F0) // 연한 회색 배경
            ) {
                Row( // 이모지를 가운데 정렬하기 위한 Row
                    modifier = Modifier.fillMaxWidth(), // 가로 꽉 채움
                    horizontalArrangement = Arrangement.Center, // 가로 중앙 정렬
                    verticalAlignment = Alignment.CenterVertically // 세로 중앙 정렬
                ) {
                    Text(text = emoji) // 이모지 출력
                }
            }

            Spacer(modifier = Modifier.size(12.dp)) // 이모지와 제목 사이 간격

            Text( // 메뉴 제목 텍스트
                text = title, // 표시할 메뉴 이름
                style = MaterialTheme.typography.titleMedium, // 중간 제목 스타일
                fontWeight = FontWeight.SemiBold, // 약간 굵게 표시
                color = Color(0xFF1E293B) // 진한 회색 글자
            )
        }
    }
}