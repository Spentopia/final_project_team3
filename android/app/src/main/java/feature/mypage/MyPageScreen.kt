package com.ict.spentopia.feature.mypage

import androidx.compose.foundation.background // 수정: 배경 박스와 배지 표현에 사용
import androidx.compose.foundation.border // 수정: 지갑 카드 점선 느낌 대체 테두리에 사용
import androidx.compose.foundation.layout.Arrangement // 수정: 내부 정렬과 간격 지정에 사용
import androidx.compose.foundation.layout.Box // 수정: 배지 및 카드 내부 강조 영역에 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가에 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 가로 전체 사용에 사용
import androidx.compose.foundation.layout.height // 수정: 높이 여백 지정에 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백 적용에 사용
import androidx.compose.foundation.layout.size // 수정: 프로필 이미지 크기 지정에 사용
import androidx.compose.foundation.rememberScrollState // 수정: 세로 스크롤 상태를 기억하기 위해 사용
import androidx.compose.foundation.shape.CircleShape // 수정: 원형 프로필 이미지 표현에 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 카드 모양에 사용
import androidx.compose.foundation.verticalScroll // 수정: 화면 전체 스크롤 처리에 사용
import androidx.compose.material3.Card // 수정: 카드 UI 구성에 사용
import androidx.compose.material3.CardDefaults // 수정: 카드 색상과 스타일 설정에 사용
import androidx.compose.material3.Switch // 수정: 알림 탭의 토글 UI에 사용
import androidx.compose.material3.SwitchDefaults // 수정: Switch 색상 지정에 사용
import androidx.compose.material3.Text // 수정: 텍스트 출력에 사용
import androidx.compose.material3.TextButton // 수정: 상단 서브 탭 버튼에 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.runtime.getValue // 수정: 탭 상태 위임 사용
import androidx.compose.runtime.mutableStateOf // 수정: 현재 선택된 탭 상태 저장에 사용
import androidx.compose.runtime.remember // 수정: Compose 상태 기억에 사용
import androidx.compose.runtime.setValue // 수정: 탭 상태 위임 사용
import androidx.compose.ui.Alignment // 수정: 내부 정렬에 사용
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.ui.graphics.Brush // 수정: 프로필 카드 그라데이션에 사용
import androidx.compose.ui.graphics.Color // 수정: 색상 지정에 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 제목 강조에 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기 지정에 사용

// 기존 주석 유지
// 마이페이지 화면
@Composable
fun MyPageScreen() {
    var selectedTab by remember { mutableStateOf(MyPageTab.PROFILE) } // 수정: 기본 탭을 프로필로 설정합니다.

    Column(
        modifier = Modifier
            .fillMaxWidth() // 수정: 본문 전체 너비를 사용합니다.
            .verticalScroll(rememberScrollState()) // 수정: 마이페이지 전체가 세로로 스크롤되도록 설정합니다.
            .padding(vertical = 8.dp) // 수정: 화면 상하 기본 여백을 적용합니다.
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = Color(0xFFF1EFEE), // 수정: 웹뷰와 유사한 연한 탭 배경색을 적용합니다.
                    shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 탭 모양을 적용합니다.
                )
                .padding(4.dp), // 수정: 탭 내부 여백을 적용합니다.
            horizontalArrangement = Arrangement.spacedBy(4.dp) // 수정: 탭 간격을 적용합니다.
        ) {
            MyPageTopTabButton(
                text = "프로필", // 수정: 프로필 탭 텍스트를 표시합니다.
                selected = selectedTab == MyPageTab.PROFILE, // 수정: 현재 프로필 탭 선택 상태를 반영합니다.
                onClick = { selectedTab = MyPageTab.PROFILE } // 수정: 클릭 시 프로필 탭으로 전환합니다.
            )

            MyPageTopTabButton(
                text = "알림", // 수정: 알림 탭 텍스트를 표시합니다.
                selected = selectedTab == MyPageTab.NOTIFICATION, // 수정: 현재 알림 탭 선택 상태를 반영합니다.
                onClick = { selectedTab = MyPageTab.NOTIFICATION } // 수정: 클릭 시 알림 탭으로 전환합니다.
            )

            MyPageTopTabButton(
                text = "지갑", // 수정: 지갑 탭 텍스트를 표시합니다.
                selected = selectedTab == MyPageTab.WALLET, // 수정: 현재 지갑 탭 선택 상태를 반영합니다.
                onClick = { selectedTab = MyPageTab.WALLET } // 수정: 클릭 시 지갑 탭으로 전환합니다.
            )
        }

        Spacer(modifier = Modifier.height(16.dp)) // 수정: 상단 탭과 본문 사이 여백을 적용합니다.

        when (selectedTab) {
            MyPageTab.PROFILE -> {
                ProfileTabContent() // 수정: 프로필 탭 내용을 출력합니다.
            }

            MyPageTab.NOTIFICATION -> {
                NotificationTabContent() // 수정: 알림 탭 내용을 출력합니다.
            }

            MyPageTab.WALLET -> {
                WalletTabContent() // 수정: 지갑 탭 내용을 출력합니다.
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 수정: 하단 여백을 추가합니다.
    }
}

// 수정: 마이페이지 내부 상단 탭 상태를 정의합니다.
private enum class MyPageTab {
    PROFILE, // 수정: 프로필 탭 상태입니다.
    NOTIFICATION, // 수정: 알림 탭 상태입니다.
    WALLET // 수정: 지갑 탭 상태입니다.
}

// 수정: 상단 탭 버튼 UI를 구성합니다.
@Composable
private fun MyPageTopTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    TextButton(
        onClick = { onClick() }, // 수정: 탭 클릭 시 전달받은 동작을 실행합니다.
        modifier = Modifier
            .background(
                color = if (selected) Color.White else Color.Transparent, // 수정: 선택된 탭만 흰색 배경으로 표시합니다.
                shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 버튼 모양을 적용합니다.
            )
    ) {
        Text(
            text = text, // 수정: 탭 이름을 표시합니다.
            color = Color(0xFF111827), // 수정: 탭 텍스트 색상을 지정합니다.
            fontSize = 13.sp, // 수정: 탭 텍스트 크기를 지정합니다.
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // 수정: 선택된 탭을 굵게 표시합니다.
        )
    }
}

// 수정: 프로필 탭 전체 내용을 구성합니다.
@Composable
private fun ProfileTabContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp) // 수정: 카드들 사이의 간격을 지정합니다.
    ) {
        ProfileHeaderCard() // 수정: 상단 프로필 요약 카드를 표시합니다.
        MemberInfoCard() // 수정: 회원 정보 카드를 표시합니다.
        SocialAccountCard() // 수정: 소셜 계정 연동 카드를 표시합니다.
    }
}

// 수정: 상단 프로필 요약 카드를 구성합니다.
@Composable
private fun ProfileHeaderCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), // 수정: 카드가 가로 전체를 사용하도록 설정합니다.
        shape = RoundedCornerShape(20.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.Transparent) // 수정: 내부 그라데이션 배경을 사용하기 위해 카드 배경을 투명 처리합니다.
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF14B8D4), // 수정: 프로필 카드 시작 색상을 지정합니다.
                            Color(0xFF3B82F6) // 수정: 프로필 카드 끝 색상을 지정합니다.
                        )
                    ),
                    shape = RoundedCornerShape(20.dp) // 수정: 그라데이션 영역에도 둥근 모서리를 적용합니다.
                )
                .padding(20.dp), // 수정: 카드 내부 여백을 적용합니다.
            horizontalAlignment = Alignment.CenterHorizontally // 수정: 내부 내용을 중앙 정렬합니다.
        ) {
            Box(
                contentAlignment = Alignment.Center // 수정: 프로필 원 안의 이모지를 중앙 정렬합니다.
            ) {
                Box(
                    modifier = Modifier
                        .size(92.dp) // 수정: 프로필 원형 영역 크기를 지정합니다.
                        .background(
                            color = Color.White.copy(alpha = 0.18f), // 수정: 반투명 원형 배경을 적용합니다.
                            shape = CircleShape // 수정: 완전한 원형 모양을 적용합니다.
                        )
                )

                Text(
                    text = "😊", // 수정: 임시 프로필 이모지를 표시합니다.
                    fontSize = 36.sp // 수정: 프로필 이모지 크기를 지정합니다.
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd) // 수정: 카메라 배지를 우하단에 배치합니다.
                        .background(
                            color = Color.White, // 수정: 흰색 배경을 적용합니다.
                            shape = CircleShape // 수정: 원형 배지를 적용합니다.
                        )
                        .padding(8.dp) // 수정: 배지 내부 여백을 적용합니다.
                ) {
                    Text(
                        text = "📷", // 수정: 카메라 이모지를 표시합니다.
                        fontSize = 12.sp // 수정: 카메라 이모지 크기를 지정합니다.
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp)) // 수정: 프로필 이미지와 이름 사이 여백을 추가합니다.

            Text(
                text = "길동이", // 수정: 닉네임을 표시합니다.
                fontSize = 28.sp, // 수정: 닉네임 크기를 지정합니다.
                fontWeight = FontWeight.Bold, // 수정: 닉네임을 강조합니다.
                color = Color.White // 수정: 흰색 텍스트를 적용합니다.
            )

            Spacer(modifier = Modifier.height(4.dp)) // 수정: 닉네임과 실명 사이 여백을 추가합니다.

            Text(
                text = "홍길동", // 수정: 실명을 표시합니다.
                fontSize = 14.sp, // 수정: 실명 텍스트 크기를 지정합니다.
                fontWeight = FontWeight.SemiBold, // 수정: 실명을 약간 강조합니다.
                color = Color.White.copy(alpha = 0.9f) // 수정: 반투명 흰색 텍스트를 적용합니다.
            )

            Spacer(modifier = Modifier.height(20.dp)) // 수정: 프로필 정보와 통계 카드 사이 여백을 추가합니다.

            ProfileStatBox(title = "가입일", value = "2026년 4월 1일") // 수정: 가입일 통계 박스를 표시합니다.
            ProfileStatBox(title = "연속 기록", value = "7일 🔥") // 수정: 연속 기록 통계 박스를 표시합니다.
            ProfileStatBox(title = "보유 SPT", value = "1,250 SPT") // 수정: 보유 SPT 통계 박스를 표시합니다.
            ProfileStatBox(title = "보유 아바타", value = "15개") // 수정: 보유 아바타 통계 박스를 표시합니다.
        }
    }
}

// 수정: 프로필 요약 카드 안의 통계 박스를 구성합니다.
@Composable
private fun ProfileStatBox(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth() // 수정: 통계 박스가 가로 전체를 사용하도록 설정합니다.
            .padding(bottom = 10.dp), // 수정: 박스 사이 하단 간격을 적용합니다.
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 박스 모양을 적용합니다.
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f) // 수정: 반투명 흰색 박스 배경을 적용합니다.
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp) // 수정: 박스 내부 여백을 적용합니다.
        ) {
            Text(
                text = title, // 수정: 통계 제목을 표시합니다.
                fontSize = 14.sp, // 수정: 제목 텍스트 크기를 지정합니다.
                color = Color.White.copy(alpha = 0.88f) // 수정: 반투명 흰색 텍스트를 적용합니다.
            )

            Spacer(modifier = Modifier.height(6.dp)) // 수정: 제목과 값 사이 여백을 추가합니다.

            Text(
                text = value, // 수정: 통계 값을 표시합니다.
                fontSize = 18.sp, // 수정: 값 텍스트 크기를 지정합니다.
                fontWeight = FontWeight.Bold, // 수정: 값을 강조합니다.
                color = Color.White // 수정: 흰색 텍스트를 적용합니다.
            )
        }
    }
}

// 수정: 회원 정보 카드를 구성합니다.
@Composable
private fun MemberInfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), // 수정: 카드가 가로 전체를 사용하도록 설정합니다.
        shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White) // 수정: 흰색 카드 배경을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 내부 항목 간격을 지정합니다.
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 수정: 제목과 수정 버튼을 한 줄에 배치합니다.
                horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양 끝 정렬을 적용합니다.
                verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙 정렬을 적용합니다.
            ) {
                Text(
                    text = "회원 정보", // 수정: 카드 제목을 표시합니다.
                    fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                )

                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0xFFF8FAFC), // 수정: 수정 버튼 배경색을 적용합니다.
                            shape = RoundedCornerShape(10.dp) // 수정: 둥근 버튼 모양을 적용합니다.
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp) // 수정: 버튼 내부 여백을 적용합니다.
                ) {
                    Text(
                        text = "✏️ 수정", // 수정: 수정 버튼 텍스트를 표시합니다.
                        fontSize = 13.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
                        fontWeight = FontWeight.SemiBold, // 수정: 버튼 텍스트를 강조합니다.
                        color = Color(0xFF374151) // 수정: 버튼 텍스트 색상을 지정합니다.
                    )
                }
            }

            MemberField(label = "이름", value = "홍길동") // 수정: 이름 입력 형태 영역을 표시합니다.
            MemberField(label = "닉네임", value = "길동이") // 수정: 닉네임 입력 형태 영역을 표시합니다.
            MemberField(label = "이메일", value = "hong@example.com") // 수정: 이메일 입력 형태 영역을 표시합니다.
            MemberField(label = "전화번호", value = "010-1234-5678") // 수정: 전화번호 입력 형태 영역을 표시합니다.
        }
    }
}

// 수정: 회원 정보 한 줄 입력 형태 UI를 구성합니다.
@Composable
private fun MemberField(label: String, value: String) {
    Column {
        Text(
            text = label, // 수정: 필드명을 표시합니다.
            fontSize = 14.sp, // 수정: 필드명 크기를 지정합니다.
            fontWeight = FontWeight.SemiBold, // 수정: 필드명을 약간 강조합니다.
            color = Color(0xFF374151) // 수정: 필드명 색상을 지정합니다.
        )

        Spacer(modifier = Modifier.height(6.dp)) // 수정: 라벨과 입력창 사이 여백을 추가합니다.

        Box(
            modifier = Modifier
                .fillMaxWidth() // 수정: 입력창이 가로 전체를 사용하도록 설정합니다.
                .background(
                    color = Color(0xFFF5F7FA), // 수정: 연한 회색 입력창 배경을 적용합니다.
                    shape = RoundedCornerShape(12.dp) // 수정: 둥근 입력창 모양을 적용합니다.
                )
                .padding(horizontal = 14.dp, vertical = 12.dp) // 수정: 입력창 내부 여백을 적용합니다.
        ) {
            Text(
                text = value, // 수정: 필드 값을 표시합니다.
                fontSize = 15.sp, // 수정: 값 텍스트 크기를 지정합니다.
                color = Color(0xFF9CA3AF) // 수정: 비활성 입력창 느낌의 텍스트 색상을 적용합니다.
            )
        }
    }
}

// 수정: 소셜 계정 연동 카드를 구성합니다.
@Composable
private fun SocialAccountCard() {
    Card(
        modifier = Modifier.fillMaxWidth(), // 수정: 카드가 가로 전체를 사용하도록 설정합니다.
        shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White) // 수정: 흰색 카드 배경을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(16.dp) // 수정: 내부 항목 간격을 지정합니다.
        ) {
            Text(
                text = "소셜 계정 연동", // 수정: 카드 제목을 표시합니다.
                fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
            )

            SocialAccountRow(
                iconEmoji = "🟡", // 수정: 카카오 아이콘 대체 이모지를 표시합니다.
                serviceName = "카카오", // 수정: 서비스 이름을 표시합니다.
                subText = "연동됨", // 수정: 현재 상태를 표시합니다.
                connected = true // 수정: 연동 상태를 true로 설정합니다.
            )

            SocialAccountRow(
                iconEmoji = "🟢", // 수정: 네이버 아이콘 대체 이모지를 표시합니다.
                serviceName = "네이버", // 수정: 서비스 이름을 표시합니다.
                subText = "미연동", // 수정: 현재 상태를 표시합니다.
                connected = false // 수정: 연동 상태를 false로 설정합니다.
            )

            SocialAccountRow(
                iconEmoji = "🔵", // 수정: 구글 아이콘 대체 이모지를 표시합니다.
                serviceName = "구글", // 수정: 서비스 이름을 표시합니다.
                subText = "연동됨", // 수정: 현재 상태를 표시합니다.
                connected = true // 수정: 연동 상태를 true로 설정합니다.
            )
        }
    }
}

// 수정: 소셜 계정 한 줄 행 UI를 구성합니다.
@Composable
private fun SocialAccountRow(
    iconEmoji: String,
    serviceName: String,
    subText: String,
    connected: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(), // 수정: 행 카드가 가로 전체를 사용하도록 설정합니다.
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 행 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White), // 수정: 흰색 배경을 적용합니다.
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 수정: 살짝 떠 있는 느낌을 적용합니다.
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth() // 수정: 내부 행이 가로 전체를 사용하도록 설정합니다.
                .padding(horizontal = 14.dp, vertical = 14.dp), // 수정: 행 내부 여백을 적용합니다.
            verticalAlignment = Alignment.CenterVertically, // 수정: 세로 중앙 정렬을 적용합니다.
            horizontalArrangement = Arrangement.SpaceBetween // 수정: 양 끝 정렬을 적용합니다.
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, // 수정: 아이콘과 텍스트를 세로 중앙 정렬합니다.
                horizontalArrangement = Arrangement.spacedBy(10.dp) // 수정: 아이콘과 텍스트 사이 간격을 지정합니다.
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp) // 수정: 아이콘 원형 영역 크기를 지정합니다.
                        .background(
                            color = when (serviceName) {
                                "카카오" -> Color(0xFFF7C600) // 수정: 카카오 배경색을 적용합니다.
                                "네이버" -> Color(0xFF03C75A) // 수정: 네이버 배경색을 적용합니다.
                                else -> Color(0xFF4285F4) // 수정: 구글 배경색을 적용합니다.
                            },
                            shape = RoundedCornerShape(999.dp) // 수정: 원형 배경을 적용합니다.
                        ),
                    contentAlignment = Alignment.Center // 수정: 아이콘 텍스트를 중앙 정렬합니다.
                ) {
                    Text(
                        text = if (serviceName == "카카오") "💬" else serviceName.first().toString(), // 수정: 서비스별 간단 아이콘을 표시합니다.
                        color = Color.White, // 수정: 아이콘 색상을 흰색으로 적용합니다.
                        fontWeight = FontWeight.Bold // 수정: 아이콘 문자를 강조합니다.
                    )
                }

                Column {
                    Text(
                        text = serviceName, // 수정: 서비스 이름을 표시합니다.
                        fontSize = 16.sp, // 수정: 서비스명 크기를 지정합니다.
                        fontWeight = FontWeight.Bold, // 수정: 서비스명을 강조합니다.
                        color = Color(0xFF111827) // 수정: 텍스트 색상을 지정합니다.
                    )

                    Text(
                        text = subText, // 수정: 연동 상태 서브 텍스트를 표시합니다.
                        fontSize = 12.sp, // 수정: 서브 텍스트 크기를 지정합니다.
                        color = Color(0xFF6B7280) // 수정: 회색 서브 텍스트 색상을 적용합니다.
                    )
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        color = if (connected) Color(0xFF22C55E) else Color(0xFFF3F4F6), // 수정: 연동 여부에 따라 버튼 배경색을 다르게 적용합니다.
                        shape = RoundedCornerShape(10.dp) // 수정: 둥근 버튼 모양을 적용합니다.
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp) // 수정: 버튼 내부 여백을 적용합니다.
            ) {
                Text(
                    text = if (connected) "연동됨" else "연동", // 수정: 연동 상태에 따라 버튼 텍스트를 다르게 표시합니다.
                    color = if (connected) Color.White else Color(0xFF111827), // 수정: 연동 여부에 따라 텍스트 색상을 적용합니다.
                    fontSize = 12.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
                    fontWeight = FontWeight.Bold // 수정: 버튼 텍스트를 강조합니다.
                )
            }
        }
    }
}

// 수정: 알림 탭 전체 내용을 구성합니다.
@Composable
private fun NotificationTabContent() {
    Card(
        modifier = Modifier.fillMaxWidth(), // 수정: 카드가 가로 전체를 사용하도록 설정합니다.
        shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White) // 수정: 흰색 카드 배경을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(18.dp) // 수정: 내부 항목 간격을 지정합니다.
        ) {
            Text(
                text = "알림 설정", // 수정: 카드 제목을 표시합니다.
                fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
            )

            NotificationToggleRow(
                title = "예산 초과 알림", // 수정: 첫 번째 알림 항목 제목을 표시합니다.
                desc = "예산의 80%를 초과하면 알림을 보내드려요" // 수정: 첫 번째 알림 설명을 표시합니다.
            )

            NotificationToggleRow(
                title = "보상 획득 알림", // 수정: 두 번째 알림 항목 제목을 표시합니다.
                desc = "SPT나 아바타를 획득하면 알려드려요" // 수정: 두 번째 알림 설명을 표시합니다.
            )

            NotificationToggleRow(
                title = "스트릭 리마인드", // 수정: 세 번째 알림 항목 제목을 표시합니다.
                desc = "오늘 기록하지 않았다면 알려드려요" // 수정: 세 번째 알림 설명을 표시합니다.
            )

            NotificationToggleRow(
                title = "마케팅 알림", // 수정: 네 번째 알림 항목 제목을 표시합니다.
                desc = "이벤트와 프로모션 정보를 받아보세요" // 수정: 네 번째 알림 설명을 표시합니다.
            )
        }
    }
}

// 수정: 알림 설정 한 줄 UI를 구성합니다.
@Composable
private fun NotificationToggleRow(
    title: String,
    desc: String
) {
    var checked by remember { mutableStateOf(true) } // 수정: 알림 스위치 기본 상태를 true로 설정합니다.

    Row(
        modifier = Modifier.fillMaxWidth(), // 수정: 한 줄 전체 너비를 사용합니다.
        horizontalArrangement = Arrangement.SpaceBetween, // 수정: 내용과 스위치를 양 끝 배치합니다.
        verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙 정렬을 적용합니다.
    ) {
        Row(
            modifier = Modifier.weight(1f), // 수정: 왼쪽 텍스트 영역이 남은 공간을 차지하도록 설정합니다.
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 수정: 아이콘과 텍스트 사이 간격을 지정합니다.
            verticalAlignment = Alignment.Top // 수정: 텍스트 상단 기준 정렬을 적용합니다.
        ) {
            Text(
                text = "🔔", // 수정: 알림 아이콘 이모지를 표시합니다.
                fontSize = 14.sp // 수정: 알림 아이콘 크기를 지정합니다.
            )

            Column {
                Text(
                    text = title, // 수정: 알림 항목 제목을 표시합니다.
                    fontSize = 16.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.SemiBold, // 수정: 제목을 약간 강조합니다.
                    color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                )

                Text(
                    text = desc, // 수정: 알림 설명을 표시합니다.
                    fontSize = 13.sp, // 수정: 설명 크기를 지정합니다.
                    color = Color(0xFF6B7280) // 수정: 회색 설명 텍스트를 적용합니다.
                )
            }
        }

        Switch(
            checked = checked, // 수정: 현재 스위치 상태를 반영합니다.
            onCheckedChange = { checked = it }, // 수정: 스위치 상태가 바뀌면 상태를 업데이트합니다.
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, // 수정: 켜짐 상태 썸 색상을 지정합니다.
                checkedTrackColor = Color(0xFF111827), // 수정: 켜짐 상태 트랙 색상을 지정합니다.
                uncheckedThumbColor = Color.White, // 수정: 꺼짐 상태 썸 색상을 지정합니다.
                uncheckedTrackColor = Color(0xFFD1D5DB) // 수정: 꺼짐 상태 트랙 색상을 지정합니다.
            )
        )
    }
}

// 수정: 지갑 탭 전체 내용을 구성합니다.
@Composable
private fun WalletTabContent() {
    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp) // 수정: 카드들 사이 간격을 지정합니다.
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 카드가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color.White) // 수정: 흰색 카드 배경을 적용합니다.
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(16.dp), // 수정: 내부 항목 간격을 지정합니다.
                horizontalAlignment = Alignment.CenterHorizontally // 수정: 내부 내용을 중앙 정렬합니다.
            ) {
                Text(
                    text = "지갑 관리", // 수정: 카드 제목을 표시합니다.
                    fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF111827), // 수정: 제목 색상을 지정합니다.
                    modifier = Modifier.fillMaxWidth() // 수정: 제목을 좌측 정렬 가능한 전체 너비로 설정합니다.
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth() // 수정: 내부 연결 박스가 가로 전체를 사용하도록 설정합니다.
                        .border(
                            width = 1.dp, // 수정: 점선 대체용 얇은 테두리를 적용합니다.
                            color = Color(0xFFD1D5DB), // 수정: 연한 회색 테두리를 적용합니다.
                            shape = RoundedCornerShape(14.dp) // 수정: 둥근 테두리 모양을 적용합니다.
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp), // 수정: 박스 내부 여백을 적용합니다.
                    contentAlignment = Alignment.Center // 수정: 내부 내용을 중앙 정렬합니다.
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally, // 수정: 세로 내용 전체를 중앙 정렬합니다.
                        verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 내부 요소 간격을 지정합니다.
                    ) {
                        Text(
                            text = "👛", // 수정: 지갑 아이콘 이모지를 표시합니다.
                            fontSize = 42.sp // 수정: 지갑 아이콘 크기를 지정합니다.
                        )

                        Text(
                            text = "지갑이 연결되지 않았어요", // 수정: 지갑 미연결 안내 문구를 표시합니다.
                            fontSize = 20.sp, // 수정: 안내 문구 크기를 지정합니다.
                            fontWeight = FontWeight.Bold, // 수정: 안내 문구를 강조합니다.
                            color = Color(0xFF111827) // 수정: 안내 문구 색상을 지정합니다.
                        )

                        Text(
                            text = "지갑을 연결하면 NFT 거래와\n블록체인 기능을 이용할 수 있어요", // 수정: 지갑 연결 설명을 두 줄로 표시합니다.
                            fontSize = 14.sp, // 수정: 설명 텍스트 크기를 지정합니다.
                            color = Color(0xFF6B7280) // 수정: 회색 설명 텍스트를 적용합니다.
                        )

                        Box(
                            modifier = Modifier
                                .background(
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF06B6D4), // 수정: 버튼 시작 색상을 지정합니다.
                                            Color(0xFF2563EB) // 수정: 버튼 끝 색상을 지정합니다.
                                        )
                                    ),
                                    shape = RoundedCornerShape(12.dp) // 수정: 둥근 버튼 모양을 적용합니다.
                                )
                                .padding(horizontal = 18.dp, vertical = 10.dp) // 수정: 버튼 내부 여백을 적용합니다.
                        ) {
                            Text(
                                text = "🔗 지갑 연결하기", // 수정: 지갑 연결 버튼 텍스트를 표시합니다.
                                color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                                fontSize = 14.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
                                fontWeight = FontWeight.Bold // 수정: 버튼 텍스트를 강조합니다.
                            )
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 혜택 카드가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4FBFF)) // 수정: 연한 하늘색 카드 배경을 적용합니다.
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 목록 간격을 지정합니다.
            ) {
                Text(
                    text = "💡 지갑 연결 혜택", // 수정: 혜택 카드 제목을 표시합니다.
                    fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF374151) // 수정: 제목 색상을 지정합니다.
                )

                WalletBenefitText(text = "• NFT로 아바타 아이템 발행") // 수정: 첫 번째 혜택을 표시합니다.
                WalletBenefitText(text = "• 마켓에서 자유롭게 거래") // 수정: 두 번째 혜택을 표시합니다.
                WalletBenefitText(text = "• 블록체인 기반 수익 흐름 적용") // 수정: 세 번째 혜택을 표시합니다.
                WalletBenefitText(text = "• 지갑으로 간편 로그인") // 수정: 네 번째 혜택을 표시합니다.
            }
        }
    }
}

// 수정: 지갑 혜택 한 줄 텍스트 UI를 구성합니다.
@Composable
private fun WalletBenefitText(text: String) {
    Text(
        text = text, // 수정: 혜택 문구를 표시합니다.
        fontSize = 14.sp, // 수정: 혜택 텍스트 크기를 지정합니다.
        color = Color(0xFF374151), // 수정: 텍스트 색상을 지정합니다.
        fontWeight = FontWeight.Medium // 수정: 텍스트를 약간 강조합니다.
    )
}