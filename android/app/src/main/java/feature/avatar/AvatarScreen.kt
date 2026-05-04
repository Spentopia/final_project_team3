package com.ict.spentopia.feature.avatar

// 아바타 화면임
// 아바타 목록/구매/보유/선택 영역

import androidx.compose.foundation.background // 수정: 카드 배경에 사용
import androidx.compose.foundation.border // 수정: 선택 테두리에 사용
import androidx.compose.foundation.clickable // 수정: 클릭 처리에 사용
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement // 수정: 정렬에 사용
import androidx.compose.foundation.layout.Box // 수정: 진행바에 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 배치에 사용
import androidx.compose.foundation.layout.ExperimentalLayoutApi // 수정: FlowRow에 사용
import androidx.compose.foundation.layout.FlowRow // 수정: 줄바꿈 배치에 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 배치에 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가에 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 전체 너비에 사용
import androidx.compose.foundation.layout.height // 수정: 높이 지정에 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백에 사용
import androidx.compose.foundation.rememberScrollState // 수정: 스크롤 상태에 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 모양에 사용
import androidx.compose.foundation.verticalScroll // 수정: 세로 스크롤에 사용
import androidx.compose.material3.Button // 수정: 액션 버튼에 사용
import androidx.compose.material3.ButtonDefaults // 수정: 버튼 색상에 사용
import androidx.compose.material3.Card // 수정: 카드 UI에 사용
import androidx.compose.material3.CardDefaults // 수정: 카드 스타일에 사용
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // 수정: 텍스트 출력에 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.runtime.collectAsState // 수정: 상태 구독에 사용
import androidx.compose.runtime.getValue // 수정: 상태 위임에 사용
import androidx.compose.runtime.remember // 수정: 눌림 상태 기억에 사용
import androidx.compose.ui.Alignment // 수정: 정렬에 사용
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color // 수정: 색상 지정에 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 텍스트 강조에 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기에 사용
import androidx.lifecycle.viewmodel.compose.viewModel // 수정: 뷰모델 연결에 사용
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple

// 기존 주석 유지
// 내 아바타 화면
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용
@Composable
fun AvatarScreen(
    viewModel: AvatarViewModel = viewModel() // 수정: 뷰모델 연결
) {
    val uiState by viewModel.uiState.collectAsState() // 수정: 상태 구독
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth() // 수정: 전체 너비
            .verticalScroll(rememberScrollState()) // 수정: 세로 스크롤
            .padding(vertical = 8.dp) // 수정: 상하 여백
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 한 줄 배치
            horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양끝 정렬
            verticalAlignment = Alignment.Top // 수정: 상단 정렬
        ) {
            Column {
                Text(
                    text = uiState.screenTitle, // 수정: 제목 연결
                    fontSize = 28.sp, // 수정: 제목 크기
                    fontWeight = FontWeight.Bold, // 수정: 제목 강조
                    color = MaterialTheme.colorScheme.onBackground // 수정: 제목 색상
                )

                Spacer(modifier = Modifier.height(4.dp)) // 수정: 여백 추가

                Text(
                    text = uiState.ownedItemText, // 수정: 보유 수 연결
                    fontSize = 15.sp, // 수정: 텍스트 크기
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 수정: 보조 색상
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // 수정: 여백 추가

        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 버튼 한 줄 배치
            horizontalArrangement = Arrangement.spacedBy(8.dp) // 수정: 버튼 간격
        ) {
            AvatarActionButton(
                text = "🔀 랜덤 코디", // 수정: 버튼 문구
                modifier = Modifier.weight(1f), // 수정: 균등 너비
                onClick = { viewModel.randomizeAvatar() } // 수정: 랜덤 코디 연결
            )

            AvatarActionButton(
                text = "📷 스크린샷", // 수정: 버튼 문구
                modifier = Modifier.weight(1f), // 수정: 균등 너비
                onClick = { viewModel.captureAvatar() } // 수정: 스크린샷 연결
            )

            AvatarActionButton(
                text = "🔗 공유하기", // 수정: 버튼 문구
                modifier = Modifier.weight(1f), // 수정: 균등 너비
                highlighted = true, // 수정: 강조 스타일
                onClick = { viewModel.shareAvatar() } // 수정: 공유 연결
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 여백 추가

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
            shape = RoundedCornerShape(22.dp), // 수정: 둥근 카드
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 수정: 카드 배경
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp) // 수정: 내부 여백
            ) {
                Text(
                    text = "미리보기", // 수정: 카드 제목
                    color = MaterialTheme.colorScheme.onSurface, // 수정: 텍스트 색상
                    fontSize = 18.sp, // 수정: 제목 크기
                    fontWeight = FontWeight.Bold // 수정: 제목 강조
                )

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 여백 추가

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
                    shape = RoundedCornerShape(18.dp), // 수정: 둥근 박스
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f) // 수정: 반투명 배경
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth() // 수정: 전체 너비
                            .padding(vertical = 38.dp), // 수정: 상하 여백
                        horizontalAlignment = Alignment.CenterHorizontally // 수정: 중앙 정렬
                    ) {
                        Text(text = uiState.preview.bodyEmoji, fontSize = 28.sp) // 수정: 몸 이모지 연결
                        Spacer(modifier = Modifier.height(12.dp)) // 수정: 여백 추가
                        Text(text = uiState.preview.hairEmoji, fontSize = 56.sp) // 수정: 헤어 이모지 연결
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 여백 추가
                        Text(text = uiState.preview.faceEmoji, fontSize = 56.sp) // 수정: 표정 이모지 연결
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 여백 추가
                        Text(text = uiState.preview.clothesEmoji, fontSize = 52.sp) // 수정: 옷 이모지 연결
                        Spacer(modifier = Modifier.height(10.dp)) // 수정: 여백 추가
                        Text(text = uiState.preview.accessoryEmoji, fontSize = 48.sp) // 수정: 액세서리 이모지 연결
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 여백 추가

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
                    shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.14f) // 수정: 반투명 배경
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // 수정: 내부 여백
                        verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 항목 간격
                    ) {
                        AvatarInfoRow(
                            label = "총 희귀도",
                            value = uiState.summary.totalRarity // 수정: 희귀도 연결
                        )
                        AvatarInfoRow(
                            label = "착용 아이템",
                            value = uiState.summary.equippedItemCount // 수정: 착용 수 연결
                        )
                        AvatarInfoRow(
                            label = "획득 날짜",
                            value = uiState.summary.acquiredDate // 수정: 날짜 연결
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // 수정: 여백 추가

                Card(
                    modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
                    shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f) // 수정: 반투명 배경
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp), // 수정: 내부 여백
                        verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 항목 간격
                    ) {
                        Text(
                            text = uiState.reward.title, // 수정: 보상 제목 연결
                            color = MaterialTheme.colorScheme.onSurface, // 수정: 텍스트 색상
                            fontSize = 18.sp, // 수정: 제목 크기
                            fontWeight = FontWeight.Bold // 수정: 제목 강조
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth() // 수정: 전체 너비
                                .height(8.dp) // 수정: 진행바 높이
                                .background(
                                    color = if (isDark) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant, // 수정: 배경 색상
                                    shape = RoundedCornerShape(999.dp) // 수정: 둥근 진행바
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(uiState.reward.progress) // 수정: 진행률 반영
                                    .height(8.dp) // 수정: 채움 높이
                                    .background(
                                        color = MaterialTheme.colorScheme.primary, // 수정: 채움 색상
                                        shape = RoundedCornerShape(999.dp) // 수정: 둥근 채움
                                    )
                            )
                        }

                        Text(
                            text = uiState.reward.description, // 수정: 보상 설명 연결
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, // 수정: 텍스트 색상
                            fontSize = 13.sp // 수정: 텍스트 크기
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 여백 추가

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp), // 수정: 가로 간격
            verticalArrangement = Arrangement.spacedBy(8.dp) // 수정: 세로 간격
        ) {
            uiState.categories.forEach { category ->
                AvatarCategoryChip(
                    text = category.label, // 수정: 카테고리 문구 연결
                    selected = uiState.selectedCategory == category, // 수정: 선택 상태 연결
                    onClick = { viewModel.selectCategory(category) } // 수정: 카테고리 변경
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // 수정: 여백 추가

        uiState.visibleSections.forEach { section ->
            AvatarItemSection(
                title = section.title, // 수정: 섹션 제목 연결
                category = section.category, // 수정: 카테고리 전달
                items = section.items, // 수정: 아이템 목록 연결
                onItemClick = { itemName ->
                    viewModel.selectItem(section.category, itemName) // 수정: 아이템 선택
                }
            )
        }

        Spacer(modifier = Modifier.height(22.dp)) // 수정: 여백 추가

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) // 수정: 카드 배경
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 내부 여백
                verticalArrangement = Arrangement.spacedBy(16.dp) // 수정: 항목 간격
            ) {
                Text(
                    text = "컬렉션 진행도", // 수정: 카드 제목
                    fontSize = 22.sp, // 수정: 제목 크기
                    fontWeight = FontWeight.Bold, // 수정: 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 수정: 제목 색상
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 가로 간격
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 세로 간격
                ) {
                    uiState.collectionProgressList.forEach { progressItem ->
                        CollectionProgressCard(
                            title = progressItem.title, // 수정: 제목 연결
                            value = progressItem.value, // 수정: 수치 연결
                            progress = progressItem.progress, // 수정: 진행률 연결
                            progressColor = progressColor(progressItem.title), // 수정: 진행 색상 연결
                            bgColor = progressBackgroundColor(progressItem.title) // 수정: 배경 색상 연결
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp)) // 수정: 여백 추가

        Card(
            modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
            shape = RoundedCornerShape(18.dp), // 수정: 둥근 카드
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF4FF)) // 수정: 카드 배경
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // 수정: 내부 여백
                verticalArrangement = Arrangement.spacedBy(16.dp) // 수정: 항목 간격
            ) {
                Text(
                    text = "✨ 아이템 획득 방법", // 수정: 카드 제목
                    fontSize = 22.sp, // 수정: 제목 크기
                    fontWeight = FontWeight.Bold, // 수정: 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 수정: 제목 색상
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 가로 간격
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 세로 간격
                ) {
                    uiState.methodList.forEach { method ->
                        MethodInfoCard(
                            icon = method.icon, // 수정: 아이콘 연결
                            title = method.title, // 수정: 제목 연결
                            desc = method.desc // 수정: 설명 연결
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 수정: 하단 여백
    }
}

// 수정: 액션 버튼
@Composable
private fun AvatarActionButton(
    text: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick, // 수정: 클릭 처리
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = if (pressed) 0.985f else 1f
            scaleY = if (pressed) 0.985f else 1f
        }, // 수정: modifier 연결
        shape = RoundedCornerShape(10.dp), // 수정: 둥근 버튼
        colors = ButtonDefaults.buttonColors(
            containerColor = if (highlighted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }, // 수정: 배경 색상
            contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface // 수정: 글자 색상
        )
    ) {
        Text(
            text = text, // 수정: 버튼 텍스트
            fontSize = 13.sp, // 수정: 텍스트 크기
            fontWeight = FontWeight.SemiBold // 수정: 텍스트 강조
        )
    }
}

// 수정: 요약 정보 행
@Composable
private fun AvatarInfoRow(label: String, value: String) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
        horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양끝 정렬
        verticalAlignment = Alignment.CenterVertically // 수정: 세로 중앙
    ) {
        Text(
            text = label, // 수정: 라벨 출력
            color = labelColor, // 수정: 텍스트 색상
            fontSize = 14.sp, // 수정: 텍스트 크기
            fontWeight = FontWeight.SemiBold // 수정: 라벨 강조
        )

        Text(
            text = value, // 수정: 값 출력
            color = contentColor, // 수정: 텍스트 색상
            fontSize = 14.sp, // 수정: 텍스트 크기
            fontWeight = FontWeight.Bold // 수정: 값 강조
        )
    }
}

// 수정: 카테고리 칩
@Composable
private fun AvatarCategoryChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) Color.White else Color(0xFFF1F3F5), // 수정: 배경 색상
                shape = RoundedCornerShape(999.dp) // 수정: 캡슐 모양
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp, // 수정: 선택 테두리
                color = if (selected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, // 수정: 테두리 색상
                shape = RoundedCornerShape(999.dp) // 수정: 둥근 테두리
            )
            .clickable(onClick = onClick) // 수정: 클릭 처리
            .padding(horizontal = 14.dp, vertical = 8.dp) // 수정: 내부 여백
    ) {
        Text(
            text = text, // 수정: 칩 텍스트
            color = MaterialTheme.colorScheme.onSurface, // 수정: 텍스트 색상
            fontSize = 13.sp, // 수정: 텍스트 크기
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // 수정: 선택 강조
        )
    }
}

// 수정: 아이템 섹션
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용
@Composable
private fun AvatarItemSection(
    title: String,
    category: AvatarCategory,
    items: List<AvatarItemUi>,
    onItemClick: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(18.dp)) // 수정: 여백 추가

    Text(
        text = title, // 수정: 섹션 제목
        fontSize = 22.sp, // 수정: 제목 크기
        fontWeight = FontWeight.Bold, // 수정: 제목 강조
        color = MaterialTheme.colorScheme.onSurface // 수정: 제목 색상
    )

    Spacer(modifier = Modifier.height(12.dp)) // 수정: 여백 추가

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 가로 간격
        verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 세로 간격
    ) {
        items.forEach { item ->
            AvatarItemCard(
                item = item, // 수정: 아이템 연결
                onClick = { onItemClick(item.name) } // 수정: 클릭 처리
            )
        }
    }
}

// 수정: 아이템 카드
@Composable
private fun AvatarItemCard(
    item: AvatarItemUi,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(0.30f) // 수정: 카드 너비
            .then(
                if (item.selected) {
                    Modifier.border(
                        width = 1.5.dp, // 수정: 선택 테두리
                        color = SpentopiaMutedPurple, // 수정: 테두리 색상
                        shape = RoundedCornerShape(14.dp) // 수정: 둥근 테두리
                    )
                } else {
                    Modifier // 수정: 기본 상태
                }
            )
            .clickable(
                enabled = !item.locked, // 수정: 잠금 비활성화
                onClick = onClick // 수정: 클릭 처리
            ),
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드
        colors = CardDefaults.cardColors(
            containerColor = if (item.locked) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.surface // 수정: 배경 색상
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // 수정: 그림자
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp), // 수정: 내부 여백
            horizontalAlignment = Alignment.CenterHorizontally, // 수정: 중앙 정렬
            verticalArrangement = Arrangement.spacedBy(6.dp) // 수정: 항목 간격
        ) {
            Text(
                text = item.emoji, // 수정: 이모지 출력
                fontSize = 30.sp // 수정: 이모지 크기
            )

            Text(
                text = item.name, // 수정: 이름 출력
                fontSize = 14.sp, // 수정: 이름 크기
                fontWeight = FontWeight.Medium, // 수정: 이름 강조
                color = MaterialTheme.colorScheme.onSurface // 수정: 이름 색상
            )

            Box(
                modifier = Modifier
                    .background(
                        color = rarityColor(item.rarity), // 수정: 희귀도 배경
                        shape = RoundedCornerShape(999.dp) // 수정: 캡슐 배지
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp) // 수정: 배지 여백
            ) {
                Text(
                    text = item.rarity, // 수정: 희귀도 출력
                    color = Color.White, // 수정: 흰색 텍스트
                    fontSize = 12.sp, // 수정: 텍스트 크기
                    fontWeight = FontWeight.Bold // 수정: 텍스트 강조
                )
            }

            if (item.locked) {
                Text(
                    text = "🔒", // 수정: 잠금 아이콘
                    fontSize = 16.sp // 수정: 아이콘 크기
                )
            }
        }
    }
}

// 수정: 희귀도 색상
@Composable
private fun rarityColor(rarity: String): Color {
    return when (rarity) {
        "일반" -> MaterialTheme.colorScheme.onSurfaceVariant // 수정: 일반 색상
        "레어" -> SpentopiaNavyPurple // 수정: 레어 색상
        "에픽" -> SpentopiaMutedPurple // 수정: 에픽 색상
        "전설" -> Color(0xFFEAB308) // 수정: 전설 색상
        else -> MaterialTheme.colorScheme.onSurfaceVariant // 수정: 기본 색상
    }
}

// 수정: 진행 색상
@Composable
private fun progressColor(title: String): Color {
    return when (title) {
        "일반" -> MaterialTheme.colorScheme.onSurfaceVariant // 수정: 일반 색상
        "레어" -> SpentopiaNavyPurple // 수정: 레어 색상
        "에픽" -> SpentopiaMutedPurple // 수정: 에픽 색상
        "전설" -> Color(0xFFEAB308) // 수정: 전설 색상
        else -> MaterialTheme.colorScheme.onSurfaceVariant // 수정: 기본 색상
    }
}

// 수정: 진행 배경색
private fun progressBackgroundColor(title: String): Color {
    return when (title) {
        "일반" -> Color(0xFFF8FAFC) // 수정: 일반 배경
        "레어" -> Color(0xFFEFF6FF) // 수정: 레어 배경
        "에픽" -> Color(0xFFFAF5FF) // 수정: 에픽 배경
        "전설" -> Color(0xFFFEFCE8) // 수정: 전설 배경
        else -> Color(0xFFF8FAFC) // 수정: 기본 배경
    }
}

// 수정: 컬렉션 카드
@Composable
private fun CollectionProgressCard(
    title: String,
    value: String,
    progress: Float,
    progressColor: Color,
    bgColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 카드 너비
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드
        colors = CardDefaults.cardColors(containerColor = bgColor) // 수정: 카드 배경
    ) {
        Column(
            modifier = Modifier.padding(14.dp), // 수정: 내부 여백
            verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 항목 간격
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(), // 수정: 전체 너비
                horizontalArrangement = Arrangement.SpaceBetween // 수정: 양끝 정렬
            ) {
                Text(
                    text = title, // 수정: 제목 출력
                    fontSize = 16.sp, // 수정: 제목 크기
                    fontWeight = FontWeight.Bold, // 수정: 제목 강조
                    color = MaterialTheme.colorScheme.onSurface // 수정: 제목 색상
                )

                Text(
                    text = value, // 수정: 수치 출력
                    fontSize = 14.sp, // 수정: 수치 크기
                    color = progressColor, // 수정: 수치 색상
                    fontWeight = FontWeight.SemiBold // 수정: 수치 강조
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth() // 수정: 전체 너비
                    .height(8.dp) // 수정: 진행바 높이
                    .background(
                        color = progressColor.copy(alpha = 0.22f), // 수정: 배경 색상
                        shape = RoundedCornerShape(999.dp) // 수정: 둥근 진행바
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress) // 수정: 진행률 반영
                        .height(8.dp) // 수정: 채움 높이
                        .background(
                            color = progressColor, // 수정: 채움 색상
                            shape = RoundedCornerShape(999.dp) // 수정: 둥근 채움
                        )
                )
            }
        }
    }
}

// 수정: 획득 방법 카드
@Composable
private fun MethodInfoCard(
    icon: String,
    title: String,
    desc: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f), // 수정: 카드 너비
        shape = RoundedCornerShape(14.dp), // 수정: 둥근 카드
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // 수정: 카드 배경
    ) {
        Column(
            modifier = Modifier.padding(16.dp), // 수정: 내부 여백
            verticalArrangement = Arrangement.spacedBy(8.dp) // 수정: 항목 간격
        ) {
            Text(
                text = icon, // 수정: 아이콘 출력
                fontSize = 24.sp // 수정: 아이콘 크기
            )

            Text(
                text = title, // 수정: 제목 출력
                fontSize = 18.sp, // 수정: 제목 크기
                fontWeight = FontWeight.Bold, // 수정: 제목 강조
                color = MaterialTheme.colorScheme.onSurface // 수정: 제목 색상
            )

            Text(
                text = desc, // 수정: 설명 출력
                fontSize = 14.sp, // 수정: 설명 크기
                color = MaterialTheme.colorScheme.onSurfaceVariant // 수정: 설명 색상
            )
        }
    }
}
