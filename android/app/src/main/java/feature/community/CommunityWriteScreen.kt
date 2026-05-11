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

import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import coil.compose.AsyncImage
import java.io.File
import java.io.FileOutputStream

@Composable
fun CommunityWriteScreen(
    initialCategory: CommunityCategory = CommunityCategory.FREE_BOARD,
    initialContestId: String? = null,
    onSubmitClick: (CommunityCategory, String, String, Uri?, String?) -> Unit = { _, _, _, _, _ -> }
) {
    val context = LocalContext.current

    // 제목 입력 상태입니다.
    var title by remember { mutableStateOf("") }

    // 내용 입력 상태입니다.
    var content by remember { mutableStateOf("") }

    // 선택된 카테고리 상태입니다.
    var selectedCategory by remember(initialCategory) {
        mutableStateOf(
            // 게시판 선택은 요청/콘테스트/자유만 허용합니다.
            // 그 외 값이 들어오면 기본값인 자유 게시판으로 돌립니다.
            if (initialCategory == CommunityCategory.REQUEST ||
                initialCategory == CommunityCategory.AVATAR_CONTEST ||
                initialCategory == CommunityCategory.FREE_BOARD
            ) initialCategory else CommunityCategory.FREE_BOARD
        )
    }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        // 앨범에서 고른 이미지를 그대로 저장합니다.
        // Uri는 미리보기와 서버 업로드 전 단계에서 재사용됩니다.
        selectedImageUri = uri
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let {
            // 카메라 미리보기 Bitmap을 임시 파일 Uri로 바꿉니다.
            // 갤러리 이미지와 같은 방식으로 다루기 위해서입니다.
            selectedImageUri = saveBitmapToCacheUri(
                context = context,
                bitmap = it,
                filePrefix = "community_camera"
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        CommunityWriteTopSection()

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

        CommunityWriteAttachmentCard(
            selectedImageUri = selectedImageUri,
            onPickImageClick = {
                imagePickerLauncher.launch("image/*")
            },
            onTakePhotoClick = {
                cameraLauncher.launch(null)
            },
            onRemoveImageClick = {
                selectedImageUri = null
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
                    onSubmitClick(
                        selectedCategory,
                        trimmedTitle,
                        trimmedContent,
                        selectedImageUri,
                        if (selectedCategory == CommunityCategory.AVATAR_CONTEST) initialContestId else null
                    )
                }
            }
        )
    }
}

@Composable
private fun CommunityWriteTopSection() {
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
                text = "커뮤니티 글쓰기",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "다른 사용자들과 공유하고 싶은 이야기를 자유롭게 작성해보세요.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "게시판",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 각 칩은 게시판 선택 버튼입니다.
                // 현재 선택된 칩만 primary 색으로 바뀝니다.
                listOf(
                    CommunityCategory.REQUEST,
                    CommunityCategory.AVATAR_CONTEST,
                    CommunityCategory.FREE_BOARD
                ).forEach { category ->
                    val isSelected = category == selectedCategory

                    Box(
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
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
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onPrimary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
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
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
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
                    focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

@Composable
private fun CommunityWriteAttachmentCard(
    selectedImageUri: Uri?,
    onPickImageClick: () -> Unit,
    onTakePhotoClick: () -> Unit,
    onRemoveImageClick: () -> Unit
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
                text = "첨부파일",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 이미지 앨범 업로드 버튼입니다.
                // image/*만 허용해서 사진만 고를 수 있습니다.
                Button(
                    onClick = onPickImageClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "업로드",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 카메라 촬영 버튼입니다.
                // 촬영한 이미지는 임시 Uri로 바꿔서 같은 흐름으로 처리합니다.
                Button(
                    onClick = onTakePhotoClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "카메라",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (selectedImageUri != null) {
                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp)
                        )
                ) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = "첨부 이미지 미리보기",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(1.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRemoveImageClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "첨부 이미지 삭제",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityWriteSubmitSection(
    isEnabled: Boolean,
    onSubmitClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val enabledGradient = listOf(
        colorScheme.primary,
        colorScheme.primary.copy(alpha = 0.72f),
        colorScheme.primaryContainer,
        colorScheme.primary
    )

    Button(
        onClick = onSubmitClick,
        enabled = isEnabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = if (isEnabled) {
                        Brush.horizontalGradient(enabledGradient)
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
                fontWeight = FontWeight.Bold,
                color = if (isEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun saveBitmapToCacheUri(context: Context, bitmap: Bitmap, filePrefix: String): Uri? {
    return try {
        // 카메라로 찍은 이미지를 앱 캐시 폴더에 저장합니다.
        // Uri가 있어야 화면 미리보기와 업로드가 가능해집니다.
        val cacheDir = File(context.cacheDir, "community_media").apply {
            if (!exists()) mkdirs()
        }
        val file = File(cacheDir, "${filePrefix}_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
private fun CommunityWriteScreenPreview() {
    CommunityWriteScreen()
}
