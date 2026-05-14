package com.ict.spentopia.feature.avatar // 이 파일이 속한 패키지 위치를 적음

// 아바타 화면임
// 아바타 목록/구매/보유/선택 영역

import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.isSystemInDarkTheme // isSystemInDarkTheme 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.ExperimentalLayoutApi // ExperimentalLayoutApi 기능을 가져옴
import androidx.compose.foundation.layout.FlowRow // FlowRow 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.collectAsState // collectAsState 기능을 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple // SpentopiaNavyPurple 기능을 가져옴

// 기존 주석 유지
// 내 아바타 화면
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AvatarScreen( // AvatarScreen 함수를 선언함
    viewModel: AvatarViewModel = viewModel() // 화면 데이터 관리자를 받음
) { // 이 블록 안의 내용이 시작됨
    val uiState by viewModel.uiState.collectAsState() // 화면 상태를 저장함
    val isDark = isSystemInDarkTheme() // 다크모드인지 저장함

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp) // .padding(vertical 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
            verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column { // 안쪽 UI를 세로로 배치함
                Text( // 화면에 글자를 보여줌
                    text = uiState.screenTitle, // text 값을 정해줌
                    fontSize = 28.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onBackground // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = uiState.ownedItemText, // text 값을 정해줌
                    fontSize = 15.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            horizontalArrangement = Arrangement.spacedBy(8.dp) // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            AvatarActionButton( // 누를 수 있는 버튼을 만듦
                text = "🔀 랜덤 코디", // text 값을 정해줌
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                onClick = { viewModel.randomizeAvatar() } // 눌렀을 때 실행할 함수를 정해줌
            )

            AvatarActionButton( // 누를 수 있는 버튼을 만듦
                text = "📷 스크린샷", // text 값을 정해줌
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                onClick = { viewModel.captureAvatar() } // 눌렀을 때 실행할 함수를 정해줌
            )

            AvatarActionButton( // 누를 수 있는 버튼을 만듦
                text = "🔗 공유하기", // text 값을 정해줌
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                highlighted = true, // true 값을 highlighted 값에 넣음
                onClick = { viewModel.shareAvatar() } // 눌렀을 때 실행할 함수를 정해줌
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(22.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .padding(18.dp)
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "미리보기", // text 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                )

                Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

                Card( // 내용을 카드 모양으로 묶어서 보여줌
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
                    colors = CardDefaults.cardColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.15f) // containerColor 값을 정해줌
                    )
                ) { // 이 블록 안의 내용이 시작됨
                    Column( // 안쪽 UI를 세로로 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .fillMaxWidth()
                            .padding(vertical = 38.dp), // .padding(vertical 값을 정해줌
                        horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text(text = uiState.preview.bodyEmoji, fontSize = 28.sp) // 화면에 글자를 보여줌
                        Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text(text = uiState.preview.hairEmoji, fontSize = 56.sp) // 화면에 글자를 보여줌
                        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text(text = uiState.preview.faceEmoji, fontSize = 56.sp) // 화면에 글자를 보여줌
                        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text(text = uiState.preview.clothesEmoji, fontSize = 52.sp) // 화면에 글자를 보여줌
                        Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text(text = uiState.preview.accessoryEmoji, fontSize = 48.sp) // 화면에 글자를 보여줌
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

                Card( // 내용을 카드 모양으로 묶어서 보여줌
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
                    colors = CardDefaults.cardColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.14f) // containerColor 값을 정해줌
                    )
                ) { // 이 블록 안의 내용이 시작됨
                    Column( // 안쪽 UI를 세로로 배치함
                        modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                        verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        AvatarInfoRow( // 안쪽 UI를 가로로 배치함
                            label = "총 희귀도", // label 값을 정해줌
                            value = uiState.summary.totalRarity // 입력값을 정해줌
                        )
                        AvatarInfoRow( // 안쪽 UI를 가로로 배치함
                            label = "착용 아이템", // label 값을 정해줌
                            value = uiState.summary.equippedItemCount // 입력값을 정해줌
                        )
                        AvatarInfoRow( // 안쪽 UI를 가로로 배치함
                            label = "획득 날짜", // label 값을 정해줌
                            value = uiState.summary.acquiredDate // 입력값을 정해줌
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

                Card( // 내용을 카드 모양으로 묶어서 보여줌
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
                    colors = CardDefaults.cardColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.12f) // containerColor 값을 정해줌
                    )
                ) { // 이 블록 안의 내용이 시작됨
                    Column( // 안쪽 UI를 세로로 배치함
                        modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                        verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = uiState.reward.title, // text 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                            fontSize = 18.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                        )

                        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(
                                    color = if (isDark) Color.White.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outlineVariant, // color 값을 정해줌
                                    shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                                )
                        ) { // 이 블록 안의 내용이 시작됨
                            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                                modifier = Modifier // UI 크기나 여백 같은 모양을 정함dksl rms
                                    .fillMaxWidth(uiState.reward.progress)
                                    .height(8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary, // color 값을 정해줌
                                        shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                                    )
                            )
                        }

                        Text( // 화면에 글자를 보여줌
                            text = uiState.reward.description, // text 값을 정해줌
                            color = if (isDark) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                            fontSize = 13.sp // fontSize 값을 정해줌
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

        FlowRow( // 안쪽 UI를 가로로 배치함
            horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            uiState.categories.forEach { category ->
                AvatarCategoryChip( // Avatar Category Chip 함수를 실행함
                    text = category.label, // text 값을 정해줌
                    selected = uiState.selectedCategory == category, // selected 값을 정해줌
                    onClick = { viewModel.selectCategory(category) } // 눌렀을 때 실행할 함수를 정해줌
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp)) // UI 크기나 여백 같은 모양을 정함

        uiState.visibleSections.forEach { section ->
            AvatarItemSection( // Avatar Item Section 함수를 실행함
                title = section.title, // 제목을 정해줌
                category = section.category, // 카테고리를 정해줌
                items = section.items, // items 값을 정해줌
                onItemClick = { itemName -> // onItemClick 때 실행할 함수를 정해줌
                    viewModel.selectItem(section.category, itemName)
                }
            )
        }

        Spacer(modifier = Modifier.height(22.dp)) // UI 크기나 여백 같은 모양을 정함

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(16.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "컬렉션 진행도", // text 값을 정해줌
                    fontSize = 22.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                FlowRow( // 안쪽 UI를 가로로 배치함
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // horizontalArrangement 값을 정해줌
                    verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    uiState.collectionProgressList.forEach { progressItem ->
                        CollectionProgressCard( // 내용을 카드 모양으로 묶어서 보여줌
                            title = progressItem.title, // 제목을 정해줌
                            value = progressItem.value, // 입력값을 정해줌
                            progress = progressItem.progress, // progress 값을 정해줌
                            progressColor = progressColor(progressItem.title), // progressColor 값을 정해줌
                            bgColor = progressBackgroundColor(progressItem.title) // bgColor 값을 정해줌
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(22.dp)) // UI 크기나 여백 같은 모양을 정함

        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(18.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFDF4FF)) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
                verticalArrangement = Arrangement.spacedBy(16.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "✨ 아이템 획득 방법", // text 값을 정해줌
                    fontSize = 22.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                FlowRow( // 안쪽 UI를 가로로 배치함
                    horizontalArrangement = Arrangement.spacedBy(12.dp), // horizontalArrangement 값을 정해줌
                    verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    uiState.methodList.forEach { method ->
                        MethodInfoCard( // 내용을 카드 모양으로 묶어서 보여줌
                            icon = method.icon, // icon 값을 정해줌
                            title = method.title, // 제목을 정해줌
                            desc = method.desc // desc 값을 정해줌
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // UI 크기나 여백 같은 모양을 정함
    }
}

// 수정: 액션 버튼
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AvatarActionButton( // AvatarActionButton 함수를 선언함
    text: String, // text 값을 받음
    modifier: Modifier = Modifier, // modifier 값을 받음
    highlighted: Boolean = false, // highlighted 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    Button( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = modifier.graphicsLayer { // modifier 값을 정해줌
            scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
            scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
        },
        shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = if (highlighted) { // containerColor 값을 정해줌
                MaterialTheme.colorScheme.primaryContainer
            } else { // 이 블록 안의 내용이 시작됨
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface // contentColor 값을 정해줌
        )
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            fontSize = 13.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
        )
    }
}

// 수정: 요약 정보 행
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AvatarInfoRow(label: String, value: String) { // AvatarInfoRow 함수를 선언함
    val contentColor = MaterialTheme.colorScheme.onSurface // contentColor 값을 저장함
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant // labelColor 값을 저장함

    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = label, // label 값을 text 값에 넣음
            color = labelColor, // labelColor 값을 color 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = value, // 입력값을 text 값에 넣음
            color = contentColor, // contentColor 값을 color 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
        )
    }
}

// 수정: 카테고리 칩
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AvatarCategoryChip( // AvatarCategoryChip 함수를 선언함
    text: String, // text 값을 받음
    selected: Boolean, // selected 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .background(
                color = if (selected) Color.White else Color(0xFFF1F3F5), // color 값을 정해줌
                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
            )
            .border(
                width = if (selected) 1.5.dp else 0.dp, // width 값을 정해줌
                color = if (selected) MaterialTheme.colorScheme.outlineVariant else Color.Transparent, // color 값을 정해줌
                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
            )
            .clickable(onClick = onClick) // .clickable(onClick 값을 정해줌
            .padding(horizontal = 14.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
            fontSize = 13.sp, // fontSize 값을 정해줌
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium // fontWeight 값을 정해줌
        )
    }
}

// 수정: 아이템 섹션
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AvatarItemSection( // AvatarItemSection 함수를 선언함
    title: String, // 제목을 받음
    category: AvatarCategory, // 카테고리를 받음
    items: List<AvatarItemUi>, // items 값을 받음
    onItemClick: (String) -> Unit // onItemClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

    Text( // 화면에 글자를 보여줌
        text = title, // 제목을 text 값에 넣음
        fontSize = 22.sp, // fontSize 값을 정해줌
        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
    )

    Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

    FlowRow( // 안쪽 UI를 가로로 배치함
        horizontalArrangement = Arrangement.spacedBy(12.dp), // horizontalArrangement 값을 정해줌
        verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        items.forEach { item ->
            AvatarItemCard( // 내용을 카드 모양으로 묶어서 보여줌
                item = item, // item 값을 item 값에 넣음
                onClick = { onItemClick(item.name) } // 눌렀을 때 실행할 함수를 정해줌
            )
        }
    }
}

// 수정: 아이템 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun AvatarItemCard( // AvatarItemCard 함수를 선언함
    item: AvatarItemUi, // item 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth(0.30f)
            .then(
                if (item.selected) { // 조건이 맞는지 확인함
                    Modifier.border( // UI 크기나 여백 같은 모양을 정함
                        width = 1.5.dp, // width 값을 정해줌
                        color = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 color 값에 넣음
                        shape = RoundedCornerShape(14.dp) // shape 값을 정해줌
                    )
                } else { // 이 블록 안의 내용이 시작됨
                    Modifier // UI 크기나 여백 같은 모양을 정함
                }
            )
            .clickable(
                enabled = !item.locked, // enabled 값을 정해줌
                onClick = onClick // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
            ),
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = if (item.locked) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp), // UI 크기나 여백 같은 모양을 정함
            horizontalAlignment = Alignment.CenterHorizontally, // horizontalAlignment 값을 정해줌
            verticalArrangement = Arrangement.spacedBy(6.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = item.emoji, // text 값을 정해줌
                fontSize = 30.sp // fontSize 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = item.name, // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Medium, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .background(
                        color = rarityColor(item.rarity), // color 값을 정해줌
                        shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                    )
                    .padding(horizontal = 10.dp, vertical = 4.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = item.rarity, // text 값을 정해줌
                    color = Color.White, // color 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                )
            }

            if (item.locked) { // 조건이 맞는지 확인함
                Text( // 화면에 글자를 보여줌
                    text = "🔒", // text 값을 정해줌
                    fontSize = 16.sp // fontSize 값을 정해줌
                )
            }
        }
    }
}

// 수정: 희귀도 색상
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun rarityColor(rarity: String): Color { // rarityColor 함수를 선언함
    return when (rarity) { // 이 값을 함수 결과로 돌려줌
        "일반" -> MaterialTheme.colorScheme.onSurfaceVariant
        "레어" -> SpentopiaNavyPurple
        "에픽" -> SpentopiaMutedPurple
        "전설" -> Color(0xFFEAB308)
        else -> MaterialTheme.colorScheme.onSurfaceVariant // 위 조건이 아니면 이쪽을 실행함
    }
}

// 수정: 진행 색상
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun progressColor(title: String): Color { // progressColor 함수를 선언함
    return when (title) { // 이 값을 함수 결과로 돌려줌
        "일반" -> MaterialTheme.colorScheme.onSurfaceVariant
        "레어" -> SpentopiaNavyPurple
        "에픽" -> SpentopiaMutedPurple
        "전설" -> Color(0xFFEAB308)
        else -> MaterialTheme.colorScheme.onSurfaceVariant // 위 조건이 아니면 이쪽을 실행함
    }
}

// 수정: 진행 배경색
private fun progressBackgroundColor(title: String): Color { // progressBackgroundColor 함수를 선언함
    return when (title) { // 이 값을 함수 결과로 돌려줌
        "일반" -> Color(0xFFF8FAFC)
        "레어" -> Color(0xFFEFF6FF)
        "에픽" -> Color(0xFFFAF5FF)
        "전설" -> Color(0xFFFEFCE8)
        else -> Color(0xFFF8FAFC) // 위 조건이 아니면 이쪽을 실행함
    }
}

// 수정: 컬렉션 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CollectionProgressCard( // CollectionProgressCard 함수를 선언함
    title: String, // 제목을 받음
    value: String, // 입력값을 받음
    progress: Float, // progress 값을 받음
    progressColor: Color, // progressColor 값을 받음
    bgColor: Color // bgColor 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(0.47f), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = bgColor) // colors 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(14.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(10.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 16.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = value, // 입력값을 text 값에 넣음
                    fontSize = 14.sp, // fontSize 값을 정해줌
                    color = progressColor, // progressColor 값을 color 값에 넣음
                    fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                )
            }

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(
                        color = progressColor.copy(alpha = 0.22f), // color 값을 정해줌
                        shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                    )
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth(progress)
                        .height(8.dp)
                        .background(
                            color = progressColor, // progressColor 값을 color 값에 넣음
                            shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                        )
                )
            }
        }
    }
}

// 수정: 획득 방법 카드
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun MethodInfoCard( // MethodInfoCard 함수를 선언함
    icon: String, // icon 값을 받음
    title: String, // 제목을 받음
    desc: String // desc 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(0.47f), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(16.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = icon, // icon 값을 text 값에 넣음
                fontSize = 24.sp // fontSize 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = desc, // desc 값을 text 값에 넣음
                fontSize = 14.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}
