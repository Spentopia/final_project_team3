package com.ict.spentopia.feature.community

// ------------------------------------------------------------
// CommunityWriteScreen.kt
// ------------------------------------------------------------
// 이 파일은 커뮤니티 글쓰기 화면을 담당합니다.
//
// 현재 단계 목표:
// 1. 제목 / 내용 / 카테고리를 입력합니다.
// 2. 등록 버튼 클릭 시 바깥으로 데이터를 전달합니다.
// 3. 바깥(AppNavGraph)에서 실제 리스트에 추가하도록 합니다.
// ------------------------------------------------------------

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors

@Composable
fun CommunityWriteScreen(
    onBackClick: () -> Unit = {},
    onSubmitClick: (CommunityCategory, String, String) -> Unit = { _, _, _ -> }
) {
    // 제목 입력 상태입니다.
    var title by remember { mutableStateOf("") }

    // 내용 입력 상태입니다.
    var content by remember { mutableStateOf("") }

    // 선택된 카테고리 상태입니다.
    var selectedCategory by remember { mutableStateOf(CommunityCategory.FREE_BOARD) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CommunityWriteTopSection(
            onBackClick = onBackClick
        )

        CommunityWriteCategoryCard(
            selectedCategory = selectedCategory,
            onCategorySelected = { clickedCategory ->
                selectedCategory = clickedCategory
            }
        )

        CommunityWriteTitleCard(
            title = title,
            onTitleChange = { newTitle ->
                title = newTitle
            }
        )

        CommunityWriteContentCard(
            content = content,
            onContentChange = { newContent ->
                content = newContent
            }
        )

        CommunityWriteSubmitSection(
            // 빈 제목/내용이면 등록되지 않도록 아주 기본 검사를 넣습니다.
            isEnabled = title.isNotBlank() && content.isNotBlank(),
            onSubmitClick = {
                // 초보자용 설명:
                // trim()은 앞뒤 공백을 정리해주는 함수입니다.
                val trimmedTitle = title.trim()
                val trimmedContent = content.trim()

                if (trimmedTitle.isNotEmpty() && trimmedContent.isNotEmpty()) {
                    onSubmitClick(selectedCategory, trimmedTitle, trimmedContent)
                }
            }
        )
    }
}

@Composable
private fun CommunityWriteTopSection(
    onBackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "커뮤니티 글쓰기",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "다른 사용자들과 공유하고 싶은 이야기를 자유롭게 작성해보세요.",
                        fontSize = 13.sp,
                        color = Color(0xFF6E7684),
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Button(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEDEFF4),
                        contentColor = Color(0xFF2B313B)
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "뒤로가기",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityWriteCategoryCard(
    selectedCategory: CommunityCategory,
    onCategorySelected: (CommunityCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "카테고리 선택",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommunityCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory

                    Box(
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .background(
                                color = if (isSelected) {
                                    Color(0xFFF0F1F5)
                                } else {
                                    Color(0xFFFDFDFD)
                                },
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable {
                                onCategorySelected(category)
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommunityWriteTitleCard(
    title: String,
    onTitleChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "제목",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "제목을 입력하세요",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(14.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = Color(0xFF1E2430),
                    unfocusedTextColor = Color(0xFF1E2430),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun CommunityWriteContentCard(
    content: String,
    onContentChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Text(
                text = "내용",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = {
                    Text(
                        text = "내용을 입력하세요",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(14.dp),
                minLines = 8,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = Color(0xFF1E2430),
                    unfocusedTextColor = Color(0xFF1E2430),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )
        }
    }
}

@Composable
private fun CommunityWriteSubmitSection(
    isEnabled: Boolean,
    onSubmitClick: () -> Unit
) {
    Button(
        onClick = onSubmitClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContentColor = Color.White
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isEnabled) {
                        Brush.horizontalGradient(
                            SpentopiaWalletGradientColors
                        )
                    } else {
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.outlineVariant,
                                MaterialTheme.colorScheme.outlineVariant
                            )
                        )
                    },
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "등록하기",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CommunityWriteScreenPreview() {
    CommunityWriteScreen()
}
