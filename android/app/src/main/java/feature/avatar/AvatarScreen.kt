package com.ict.spentopia.feature.avatar

import androidx.compose.foundation.background // 수정: 카드와 배지 배경 표현에 사용
import androidx.compose.foundation.border // 수정: 선택 아이템 강조 테두리에 사용
import androidx.compose.foundation.layout.Arrangement // 수정: 정렬과 간격 지정에 사용
import androidx.compose.foundation.layout.Box // 수정: 진행바 및 카드 내부 강조 영역에 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.ExperimentalLayoutApi // 수정: FlowRow 사용에 필요합니다.
import androidx.compose.foundation.layout.FlowRow // 수정: 아이템 카드 줄바꿈 배치에 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가에 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 가로 전체 사용에 사용
import androidx.compose.foundation.layout.height // 수정: 높이 여백 지정에 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백 적용에 사용
import androidx.compose.foundation.rememberScrollState // 수정: 세로 스크롤 상태 기억에 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 카드 모양에 사용
import androidx.compose.foundation.verticalScroll // 수정: 전체 화면 세로 스크롤에 사용
import androidx.compose.material3.Button // 수정: 상단 액션 버튼에 사용
import androidx.compose.material3.ButtonDefaults // 수정: 버튼 색상 지정에 사용
import androidx.compose.material3.Card // 수정: 카드 UI 구성에 사용
import androidx.compose.material3.CardDefaults // 수정: 카드 배경색과 elevation 설정에 사용
import androidx.compose.material3.Text // 수정: 텍스트 출력에 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.runtime.getValue // 수정: 탭 상태 위임 사용
import androidx.compose.runtime.mutableStateOf // 수정: 카테고리 탭 상태 저장에 사용
import androidx.compose.runtime.remember // 수정: Compose 상태 기억에 사용
import androidx.compose.runtime.setValue // 수정: 탭 상태 위임 사용
import androidx.compose.ui.Alignment // 수정: 내부 정렬에 사용
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.ui.graphics.Brush // 수정: 그라데이션 배경 표현에 사용
import androidx.compose.ui.graphics.Color // 수정: 색상 지정에 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 제목 강조에 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기 지정에 사용

// 기존 주석 유지
// 내 아바타 화면
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용을 위해 OptIn을 적용합니다.
@Composable
fun AvatarScreen() {
    var selectedCategory by remember { mutableStateOf("전체") } // 수정: 현재 선택된 카테고리 칩 상태를 저장합니다.

    Column(
        modifier = Modifier
            .fillMaxWidth() // 수정: 화면 전체 너비를 사용합니다.
            .verticalScroll(rememberScrollState()) // 수정: 아바타 화면 전체가 세로 스크롤되도록 설정합니다.
            .padding(vertical = 8.dp) // 수정: 상하 기본 여백을 적용합니다.
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 제목과 버튼 영역을 한 줄에 배치합니다.
            horizontalArrangement = Arrangement.SpaceBetween, // 수정: 제목 영역과 버튼 영역을 양끝 정렬합니다.
            verticalAlignment = Alignment.Top // 수정: 상단 기준으로 정렬합니다.
        ) {
            Column {
                Text(
                    text = "내 아바타", // 수정: 화면 제목을 표시합니다.
                    fontSize = 28.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF11243D) // 수정: 제목 색상을 지정합니다.
                )

                Spacer(modifier = Modifier.height(4.dp)) // 수정: 제목과 보유 아이템 수 사이 여백을 추가합니다.

                Text(
                    text = "보유 아이템: 10/19", // 수정: 보유 아이템 수 더미 데이터를 표시합니다.
                    fontSize = 15.sp, // 수정: 설명 텍스트 크기를 지정합니다.
                    color = Color(0xFF5C6B80) // 수정: 보조 텍스트 색상을 지정합니다.
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // 수정: 제목 영역과 액션 버튼 사이 여백을 추가합니다.

        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 액션 버튼을 한 줄에 배치합니다.
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 수정: 버튼 사이 간격을 지정합니다.
        ) {
            AvatarActionButton(
                text = "🔀 랜덤 코디", // 수정: 랜덤 코디 버튼 텍스트를 표시합니다.
                modifier = Modifier.weight(1f) // 수정: 버튼 너비를 균등 분배합니다.
            )

            AvatarActionButton(
                text = "📷 스크린샷", // 수정: 스크린샷 버튼 텍스트를 표시합니다.
                modifier = Modifier.weight(1f) // 수정: 버튼 너비를 균등 분배합니다.
            )

            AvatarActionButton(
                text = "🔗 공유하기", // 수정: 공유하기 버튼 텍스트를 표시합니다.
                modifier = Modifier.weight(1f), // 수정: 버튼 너비를 균등 분배합니다.
                highlighted = true // 수정: 공유하기 버튼을 강조 스타일로 표시합니다.
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 버튼과 미리보기 카드 사이 여백을 추가합니다.

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 미리보기 카드가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(22.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color.Transparent) // 수정: 내부 그라데이션 배경을 사용하기 위해 투명 카드로 설정합니다.
        ) {
            Column(
                modifier = Modifier
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFA349F5), // 수정: 시작 보라색을 지정합니다.
                                Color(0xFFE73AAE) // 수정: 끝 핑크색을 지정합니다.
                            )
                        ),
                        shape = RoundedCornerShape(22.dp) // 수정: 그라데이션 영역에도 둥근 모서리를 적용합니다.
                    )
                    .padding(18.dp) // 수정: 카드 내부 여백을 적용합니다.
            ) {
                Text(
                    text = "미리보기", // 수정: 미리보기 카드 제목을 표시합니다.
                    color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                    fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold // 수정: 제목을 강조합니다.
                )

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 제목과 내부 미리보기 박스 사이 여백을 추가합니다.

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 내부 미리보기 박스가 가로 전체를 사용하도록 설정합니다.
                    shape = RoundedCornerShape(18.dp), // 수정: 내부 박스 둥근 모양을 적용합니다.
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.15f) // 수정: 반투명 흰색 배경을 적용합니다.
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth() // 수정: 내부 내용이 가로 전체를 사용하도록 설정합니다.
                            .padding(vertical = 38.dp), // 수정: 상하 여백을 크게 적용해 웹 느낌을 살립니다.
                        horizontalAlignment = Alignment.CenterHorizontally // 수정: 내부 이모지들을 중앙 정렬합니다.
                    ) {
                        Text(text = "🧍", fontSize = 28.sp) // 수정: 몸 이모지를 표시합니다.
                        Spacer(modifier = Modifier.height(12.dp)) // 수정: 이모지 사이 간격을 추가합니다.
                        Text(text = "👱", fontSize = 56.sp) // 수정: 헤어/얼굴 이모지를 표시합니다.
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 이모지 사이 간격을 추가합니다.
                        Text(text = "😊", fontSize = 56.sp) // 수정: 표정 이모지를 표시합니다.
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 이모지 사이 간격을 추가합니다.
                        Text(text = "👕", fontSize = 52.sp) // 수정: 옷 이모지를 표시합니다.
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 이모지 사이 간격을 추가합니다.
                        Text(text = "✨", fontSize = 48.sp) // 수정: 액세서리 이모지를 표시합니다.
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 미리보기와 요약 정보 카드 사이 여백을 추가합니다.

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 요약 정보 카드가 가로 전체를 사용하도록 설정합니다.
                    shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드 모양을 적용합니다.
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.14f) // 수정: 반투명 배경을 적용합니다.
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                        verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 내부 항목 간격을 지정합니다.
                    ) {
                        AvatarInfoRow(label = "총 희귀도", value = "에픽") // 수정: 총 희귀도 정보를 표시합니다.
                        AvatarInfoRow(label = "착용 아이템", value = "5개") // 수정: 착용 아이템 수 정보를 표시합니다.
                        AvatarInfoRow(label = "획득 날짜", value = "2026.04.08") // 수정: 획득 날짜 정보를 표시합니다.
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 요약 정보와 다음 보상 카드 사이 여백을 추가합니다.

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 다음 보상 카드가 가로 전체를 사용하도록 설정합니다.
                    shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드 모양을 적용합니다.
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.12f) // 수정: 반투명 배경을 적용합니다.
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                        verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 내부 항목 간격을 지정합니다.
                    ) {
                        Text(
                            text = "🎁 다음 보상까지", // 수정: 다음 보상 카드 제목을 표시합니다.
                            color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                            fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                            fontWeight = FontWeight.Bold // 수정: 제목을 강조합니다.
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth() // 수정: 진행바 전체 너비를 사용합니다.
                                .height(8.dp) // 수정: 진행바 높이를 지정합니다.
                                .background(
                                    color = Color.White.copy(alpha = 0.25f), // 수정: 진행바 배경색을 적용합니다.
                                    shape = RoundedCornerShape(999.dp) // 수정: 둥근 진행바 모양을 적용합니다.
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.63f) // 수정: 예시 진행률 63%를 표시합니다.
                                    .height(8.dp) // 수정: 채워진 진행바 높이를 지정합니다.
                                    .background(
                                        color = Color.White, // 수정: 채워진 진행바 색상을 적용합니다.
                                        shape = RoundedCornerShape(999.dp) // 수정: 둥근 채움 모양을 적용합니다.
                                    )
                            )
                        }

                        Text(
                            text = "성실도 점수 25점만 더 모으면 랜덤 아바타!", // 수정: 보상 설명 문구를 표시합니다.
                            color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                            fontSize = 13.sp // 수정: 설명 텍스트 크기를 지정합니다.
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 미리보기 카드와 카테고리 칩 사이 여백을 추가합니다.

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), // 수정: 칩 사이 가로 간격을 지정합니다.
            verticalArrangement = Arrangement.spacedBy(8.dp) // 수정: 칩 사이 세로 간격을 지정합니다.
        ) {
            listOf("전체", "몸", "헤어", "표정", "옷", "액세서리").forEach { category ->
                AvatarCategoryChip(
                    text = category, // 수정: 카테고리 칩 텍스트를 표시합니다.
                    selected = selectedCategory == category, // 수정: 현재 선택된 카테고리 상태를 반영합니다.
                    onClick = { selectedCategory = category } // 수정: 클릭 시 해당 카테고리로 선택 상태를 변경합니다.
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // 수정: 카테고리 칩과 아이템 섹션 사이 여백을 추가합니다.

        AvatarItemSection(
            title = "몸", // 수정: 몸 섹션 제목을 표시합니다.
            items = listOf(
                AvatarItemUi("🧍", "기본 몸", "일반", true, false),
                AvatarItemUi("💪", "근육질", "레어", false, false),
                AvatarItemUi("🦴", "전사 몸", "레어", false, true)
            ),
            visible = selectedCategory == "전체" || selectedCategory == "몸" // 수정: 전체 또는 몸 카테고리에서만 보이도록 설정합니다.
        )

        AvatarItemSection(
            title = "헤어", // 수정: 헤어 섹션 제목을 표시합니다.
            items = listOf(
                AvatarItemUi("👱", "기본 헤어", "일반", true, false),
                AvatarItemUi("🧑", "긴 머리", "일반", false, false),
                AvatarItemUi("🦱", "금발 머리", "레어", false, true),
                AvatarItemUi("🧓", "붉은 머리", "에픽", false, true)
            ),
            visible = selectedCategory == "전체" || selectedCategory == "헤어" // 수정: 전체 또는 헤어 카테고리에서만 보이도록 설정합니다.
        )

        AvatarItemSection(
            title = "표정", // 수정: 표정 섹션 제목을 표시합니다.
            items = listOf(
                AvatarItemUi("😊", "미소", "일반", true, false),
                AvatarItemUi("😎", "쿨", "일반", false, false),
                AvatarItemUi("😍", "하트 눈", "레어", false, true),
                AvatarItemUi("🤩", "스타", "에픽", false, true)
            ),
            visible = selectedCategory == "전체" || selectedCategory == "표정" // 수정: 전체 또는 표정 카테고리에서만 보이도록 설정합니다.
        )

        AvatarItemSection(
            title = "옷", // 수정: 옷 섹션 제목을 표시합니다.
            items = listOf(
                AvatarItemUi("👕", "티셔츠", "일반", true, false),
                AvatarItemUi("👔", "정장", "레어", false, false),
                AvatarItemUi("👗", "드레스", "에픽", false, true),
                AvatarItemUi("🛡️", "갑옷", "전설", false, true)
            ),
            visible = selectedCategory == "전체" || selectedCategory == "옷" // 수정: 전체 또는 옷 카테고리에서만 보이도록 설정합니다.
        )

        AvatarItemSection(
            title = "액세서리", // 수정: 액세서리 섹션 제목을 표시합니다.
            items = listOf(
                AvatarItemUi("✨", "없음", "일반", true, false),
                AvatarItemUi("🎩", "모자", "일반", false, false),
                AvatarItemUi("👑", "왕관", "전설", false, true),
                AvatarItemUi("🦋", "나비", "에픽", false, true)
            ),
            visible = selectedCategory == "전체" || selectedCategory == "액세서리" // 수정: 전체 또는 액세서리 카테고리에서만 보이도록 설정합니다.
        )

        Spacer(modifier = Modifier.height(22.dp)) // 수정: 아이템 섹션과 컬렉션 진행도 사이 여백을 추가합니다.

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 컬렉션 진행도 카드가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)) // 수정: 연한 흰색 카드 배경을 적용합니다.
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(16.dp) // 수정: 내부 요소 간격을 지정합니다.
            ) {
                Text(
                    text = "컬렉션 진행도", // 수정: 컬렉션 진행도 제목을 표시합니다.
                    fontSize = 22.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 카드 사이 가로 간격을 지정합니다.
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 카드 사이 세로 간격을 지정합니다.
                ) {
                    CollectionProgressCard("일반", "8/10", 0.80f, Color(0xFF6B7280), Color(0xFFF8FAFC)) // 수정: 일반 등급 진행도 카드를 표시합니다.
                    CollectionProgressCard("레어", "5/8", 0.62f, Color(0xFF3B82F6), Color(0xFFEFF6FF)) // 수정: 레어 등급 진행도 카드를 표시합니다.
                    CollectionProgressCard("에픽", "2/6", 0.34f, Color(0xFFA855F7), Color(0xFFFAF5FF)) // 수정: 에픽 등급 진행도 카드를 표시합니다.
                    CollectionProgressCard("전설", "0/3", 0.08f, Color(0xFFEAB308), Color(0xFFFEFCE8)) // 수정: 전설 등급 진행도 카드를 표시합니다.
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp)) // 수정: 컬렉션 진행도와 획득 방법 사이 여백을 추가합니다.

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 획득 방법 카드가 가로 전체를 사용하도록 설정합니다.
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드 모양을 적용합니다.
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF4FF)) // 수정: 연한 보라 배경을 적용합니다.
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
                verticalArrangement = Arrangement.spacedBy(16.dp) // 수정: 내부 요소 간격을 지정합니다.
            ) {
                Text(
                    text = "✨ 아이템 획득 방법", // 수정: 획득 방법 제목을 표시합니다.
                    fontSize = 22.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 카드 사이 가로 간격을 지정합니다.
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 카드 사이 세로 간격을 지정합니다.
                ) {
                    MethodInfoCard(
                        icon = "✅", // 수정: 첫 번째 카드 아이콘을 표시합니다.
                        title = "성실도 보상", // 수정: 첫 번째 카드 제목을 표시합니다.
                        desc = "주간 성실도 70점 이상 달성 시 랜덤 아바타 지급" // 수정: 첫 번째 카드 설명을 표시합니다.
                    )

                    MethodInfoCard(
                        icon = "📥", // 수정: 두 번째 카드 아이콘을 표시합니다.
                        title = "NFT 마켓", // 수정: 두 번째 카드 제목을 표시합니다.
                        desc = "다른 유저와 아이템을 SPT로 거래할 수 있어요" // 수정: 두 번째 카드 설명을 표시합니다.
                    )

                    MethodInfoCard(
                        icon = "✨", // 수정: 세 번째 카드 아이콘을 표시합니다.
                        title = "특별 이벤트", // 수정: 세 번째 카드 제목을 표시합니다.
                        desc = "시즌 이벤트와 콘테스트에서 한정 아이템 획득" // 수정: 세 번째 카드 설명을 표시합니다.
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 수정: 하단 여백을 추가합니다.
    }
}

// 수정: 상단 액션 버튼 UI를 구성합니다.
@Composable
private fun AvatarActionButton(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false
) {
    Button(
        onClick = { }, // 수정: 현재는 더미 버튼 동작으로 비워둡니다.
        modifier = modifier, // 수정: 외부에서 전달된 modifier를 적용합니다.
        shape = RoundedCornerShape(10.dp), // 수정: 둥근 버튼 모양을 적용합니다.
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlighted) Color(0xFFEC4899) else Color.White, // 수정: 강조 여부에 따라 배경색을 다르게 적용합니다.
            contentColor = if (highlighted) Color.White else Color(0xFF111827) // 수정: 강조 여부에 따라 글자색을 다르게 적용합니다.
        )
    ) {
        Text(
            text = text, // 수정: 버튼 텍스트를 표시합니다.
            fontSize = 13.sp, // 수정: 버튼 텍스트 크기를 지정합니다.
            fontWeight = FontWeight.SemiBold // 수정: 버튼 텍스트를 강조합니다.
        )
    }
}

// 수정: 요약 정보 한 줄 UI를 구성합니다.
@Composable
private fun AvatarInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(), // 수정: 한 줄 전체 너비를 사용합니다.
        horizontalArrangement = Arrangement.SpaceBetween, // 수정: 라벨과 값을 양 끝 정렬합니다.
        verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙 정렬을 적용합니다.
    ) {
        Text(
            text = label, // 수정: 라벨 텍스트를 표시합니다.
            color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
            fontSize = 14.sp, // 수정: 라벨 크기를 지정합니다.
            fontWeight = FontWeight.SemiBold // 수정: 라벨을 약간 강조합니다.
        )

        Text(
            text = value, // 수정: 값 텍스트를 표시합니다.
            color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
            fontSize = 14.sp, // 수정: 값 크기를 지정합니다.
            fontWeight = FontWeight.Bold // 수정: 값을 강조합니다.
        )
    }
}

// 수정: 카테고리 칩 UI를 구성합니다.
@Composable
private fun AvatarCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) Color.White else Color(0xFFF1F3F5), // 수정: 선택 여부에 따라 배경색을 다르게 적용합니다.
                shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 칩 모양을 적용합니다.
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp, // 수정: 선택된 칩만 테두리를 표시합니다.
                color = if (selected) Color(0xFFE5E7EB) else Color.Transparent, // 수정: 선택된 칩의 테두리색을 지정합니다.
                shape = RoundedCornerShape(999.dp) // 수정: 테두리도 캡슐형으로 적용합니다.
            )
            .padding(horizontal = 14.dp, vertical = 8.dp) // 수정: 칩 내부 여백을 적용합니다.
            .background(Color.Transparent)
    ) {
        Text(
            text = text, // 수정: 칩 텍스트를 표시합니다.
            color = Color(0xFF111827), // 수정: 텍스트 색상을 지정합니다.
            fontSize = 13.sp, // 수정: 텍스트 크기를 지정합니다.
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // 수정: 선택된 칩을 굵게 표시합니다.
        )
    }
}

// 수정: 아바타 아이템 UI 모델을 정의합니다.
private data class AvatarItemUi(
    val emoji: String, // 수정: 카드에 표시할 이모지입니다.
    val name: String, // 수정: 아이템 이름입니다.
    val rarity: String, // 수정: 희귀도 텍스트입니다.
    val selected: Boolean, // 수정: 현재 선택된 아이템 여부입니다.
    val locked: Boolean // 수정: 잠금 상태 여부입니다.
)

// 수정: 카테고리별 아이템 섹션을 구성합니다.
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용을 위해 OptIn을 적용합니다.
@Composable
private fun AvatarItemSection(
    title: String,
    items: List<AvatarItemUi>,
    visible: Boolean
) {
    if (!visible) return // 수정: 현재 선택된 카테고리와 맞지 않으면 섹션을 표시하지 않습니다.

    Spacer(modifier = Modifier.height(18.dp)) // 수정: 이전 섹션과의 간격을 추가합니다.

    Text(
        text = title, // 수정: 섹션 제목을 표시합니다.
        fontSize = 22.sp, // 수정: 제목 크기를 지정합니다.
        fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
        color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
    )

    Spacer(modifier = Modifier.height(12.dp)) // 수정: 제목과 카드 목록 사이 여백을 추가합니다.

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 카드 사이 가로 간격을 지정합니다.
        verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 카드 사이 세로 간격을 지정합니다.
    ) {
        items.forEach { item ->
            AvatarItemCard(item = item) // 수정: 각 아이템 카드를 출력합니다.
        }
    }
}

// 수정: 개별 아바타 아이템 카드를 구성합니다.
@Composable
private fun AvatarItemCard(item: AvatarItemUi) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.30f) // 수정: 한 줄에 대략 3개 정도 보이도록 너비를 설정합니다.
            .then(
                if (item.selected) {
                    Modifier.border(
                        width = 1.5.dp, // 수정: 선택된 카드에 보라색 강조 테두리를 적용합니다.
                        color = Color(0xFFA855F7), // 수정: 선택 카드 테두리 색상을 지정합니다.
                        shape = RoundedCornerShape(14.dp) // 수정: 카드와 같은 둥근 테두리를 적용합니다.
                    )
                } else {
                    Modifier // 수정: 선택되지 않은 카드는 기본 Modifier만 사용합니다.
                }
            ),
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(
            containerColor = if (item.locked) Color(0xFFD1D5DB) else Color.White // 수정: 잠금 여부에 따라 카드 배경색을 다르게 적용합니다.
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 수정: 살짝 떠 있는 느낌을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp), // 수정: 카드 내부 여백을 적용합니다.
            horizontalAlignment = Alignment.CenterHorizontally, // 수정: 카드 내용을 중앙 정렬합니다.
            verticalArrangement = Arrangement.spacedBy(6.dp) // 수정: 내부 요소 간격을 지정합니다.
        ) {
            Text(
                text = item.emoji, // 수정: 아이템 이모지를 표시합니다.
                fontSize = 30.sp // 수정: 이모지 크기를 지정합니다.
            )

            Text(
                text = item.name, // 수정: 아이템 이름을 표시합니다.
                fontSize = 14.sp, // 수정: 이름 크기를 지정합니다.
                fontWeight = FontWeight.Medium, // 수정: 이름을 약간 강조합니다.
                color = Color(0xFF374151) // 수정: 이름 색상을 지정합니다.
            )

            Box(
                modifier = Modifier
                    .background(
                        color = rarityColor(item.rarity), // 수정: 희귀도별 배경색을 적용합니다.
                        shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 배지를 적용합니다.
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp) // 수정: 배지 내부 여백을 적용합니다.
            ) {
                Text(
                    text = item.rarity, // 수정: 희귀도 텍스트를 표시합니다.
                    color = Color.White, // 수정: 흰색 텍스트를 적용합니다.
                    fontSize = 12.sp, // 수정: 희귀도 텍스트 크기를 지정합니다.
                    fontWeight = FontWeight.Bold // 수정: 희귀도 텍스트를 강조합니다.
                )
            }

            if (item.locked) {
                Text(
                    text = "🔒", // 수정: 잠금 상태 아이콘을 표시합니다.
                    fontSize = 16.sp // 수정: 잠금 아이콘 크기를 지정합니다.
                )
            }
        }
    }
}

// 수정: 희귀도에 따른 배경색을 반환합니다.
private fun rarityColor(rarity: String): Color {
    return when (rarity) {
        "일반" -> Color(0xFF6B7280) // 수정: 일반 등급 색상을 반환합니다.
        "레어" -> Color(0xFF3B82F6) // 수정: 레어 등급 색상을 반환합니다.
        "에픽" -> Color(0xFFA855F7) // 수정: 에픽 등급 색상을 반환합니다.
        "전설" -> Color(0xFFEAB308) // 수정: 전설 등급 색상을 반환합니다.
        else -> Color(0xFF6B7280) // 수정: 기본 색상으로 일반 등급 색상을 반환합니다.
    }
}

// 수정: 컬렉션 진행도 개별 카드를 구성합니다.
@Composable
private fun CollectionProgressCard(
    title: String,
    value: String,
    progress: Float,
    progressColor: Color,
    bgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 두 칸 배치 느낌으로 너비를 설정합니다.
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = bgColor) // 수정: 카드 배경색을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(14.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 내부 요소 간격을 지정합니다.
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 수정: 제목과 수치를 양 끝 배치합니다.
                horizontalArrangement = Arrangement.SpaceBetween // 수정: 양 끝 정렬을 적용합니다.
            ) {
                Text(
                    text = title, // 수정: 희귀도 제목을 표시합니다.
                    fontSize = 16.sp, // 수정: 제목 크기를 지정합니다.
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                    color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
                )

                Text(
                    text = value, // 수정: 진행 수치를 표시합니다.
                    fontSize = 14.sp, // 수정: 수치 크기를 지정합니다.
                    color = progressColor, // 수정: 진행 수치 색상을 진행바 색상과 맞춥니다.
                    fontWeight = FontWeight.SemiBold // 수정: 수치를 약간 강조합니다.
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth() // 수정: 진행바 전체 너비를 사용합니다.
                    .height(8.dp) // 수정: 진행바 높이를 지정합니다.
                    .background(
                        color = progressColor.copy(alpha = 0.22f), // 수정: 진행바 배경색을 적용합니다.
                        shape = RoundedCornerShape(999.dp) // 수정: 둥근 진행바 모양을 적용합니다.
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress) // 수정: 전달받은 진행률만큼 채운 진행바를 표시합니다.
                        .height(8.dp) // 수정: 채워진 진행바 높이를 지정합니다.
                        .background(
                            color = progressColor, // 수정: 진행바 채움 색상을 적용합니다.
                            shape = RoundedCornerShape(999.dp) // 수정: 둥근 채움 모양을 적용합니다.
                        )
                )
            }
        }
    }
}

// 수정: 아이템 획득 방법 카드를 구성합니다.
@Composable
private fun MethodInfoCard(
    icon: String,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 두 칸 배치 느낌으로 너비를 설정합니다.
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드 모양을 적용합니다.
        colors = CardDefaults.cardColors(containerColor = Color.White) // 수정: 흰색 카드 배경을 적용합니다.
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용합니다.
            verticalArrangement = Arrangement.spacedBy(8.dp) // 수정: 내부 요소 간격을 지정합니다.
        ) {
            Text(
                text = icon, // 수정: 카드 상단 아이콘을 표시합니다.
                fontSize = 24.sp // 수정: 아이콘 크기를 지정합니다.
            )

            Text(
                text = title, // 수정: 카드 제목을 표시합니다.
                fontSize = 18.sp, // 수정: 제목 크기를 지정합니다.
                fontWeight = FontWeight.Bold, // 수정: 제목을 강조합니다.
                color = Color(0xFF111827) // 수정: 제목 색상을 지정합니다.
            )

            Text(
                text = desc, // 수정: 카드 설명을 표시합니다.
                fontSize = 14.sp, // 수정: 설명 크기를 지정합니다.
                color = Color(0xFF4B5563) // 수정: 설명 텍스트 색상을 지정합니다.
            )
        }
    }
}