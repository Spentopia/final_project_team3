package com.ict.spentopia.feature.community

// ------------------------------------------------------------
// CommunityDetailScreen.kt
// ------------------------------------------------------------
// 이 파일은 게시글 상세 화면을 담당합니다.
//
// 이번 버전에서 바뀐 점:
//  좋아요 버튼은 현재 글에만 적용되도록 유지합니다.
//  댓글 추가 / 댓글 수정 / 댓글 삭제 기능을 유지합니다.
// 3내 댓글(authorId == currentUserId)일 때만
//    수정 / 삭제 버튼이 보이게 만들었습니다.

// 중요:
// - 실제 현재 사용자 id는 아직 로그인 연동 전이므로
//   임시 문자열 "current_user"를 사용합니다.
// - 나중에 로그인 기능이 붙으면 이 값을 실제 사용자 id로 바꾸면 됩니다.
// - 실제 게시글/댓글 데이터 변경은 이 화면 바깥(AppNavGraph, ViewModel)에서 합니다.
// ------------------------------------------------------------

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.R
import coil.compose.AsyncImage
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple

@Composable
fun CommunityDetailScreen(
    post: CommunityPost?,
    currentUserId: String = "current_user",
    currentUserRole: String = "user",
    onBackClick: () -> Unit = {},
    onUpdateClick: (CommunityPost) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    onToggleLikeClick: (String) -> Unit = {},
    onAddCommentClick: (String, String) -> Unit = { _, _ -> },
    onUpdateCommentClick: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteCommentClick: (String, String) -> Unit = { _, _ -> },
    onReportClick: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    // post가 null이면 안전하게 안내 화면으로 보냅니다.
    if (post == null) {
        CommunityDetailNotFoundScreen(
            onBackClick = onBackClick
        )
        return
    }

    // 게시글 수정 모드 여부입니다.
    var isEditMode by remember(post.id) { mutableStateOf(false) }

    // 수정 중인 제목 상태입니다.
    var editTitle by remember(post.id) { mutableStateOf(post.title) }

    // 수정 중인 본문 상태입니다.
    var editFullContent by remember(post.id) { mutableStateOf(post.fullContent) }

    // 수정 중인 카테고리 상태입니다.
    var editCategory by remember(post.id) { mutableStateOf(post.category) }

    // 게시글 삭제 확인 다이얼로그 표시 여부입니다.
    var showDeleteDialog by remember(post.id) { mutableStateOf(false) }

    var showReportDialog by remember(post.id) { mutableStateOf(false) }
    var reportTargets by remember(post.id) { mutableStateOf<List<ReportTargetOption>>(emptyList()) }

    // 새 댓글 입력창 상태입니다.
    var commentInput by remember(post.id) { mutableStateOf("") }

    // 현재 수정 중인 댓글 id입니다.
    var editingCommentId by remember(post.id) { mutableStateOf<String?>(null) }

    // 현재 수정 중인 댓글 내용입니다.
    var editingCommentText by remember(post.id) { mutableStateOf("") }

    val context = LocalContext.current
    val canModifyPost = post.authorId == currentUserId ||
        (post.category == CommunityCategory.NOTICE && currentUserRole == "admin")

    // 게시글 삭제 확인 다이얼로그입니다.
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
            },
            title = {
                Text(text = "게시글 삭제")
            },
            text = {
                Text(text = "정말 이 게시글을 삭제하시겠습니까?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteClick(post.id)
                        showDeleteDialog = false
                    }
                ) {
                    Text(text = "삭제")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                    }
                ) {
                    Text(text = "취소")
                }
            }
        )
    }

    if (showReportDialog) {
        CommunityReportDialog(
            targets = reportTargets,
            onDismiss = { showReportDialog = false },
            onReportClick = { targetType, targetId, reason, detail ->
                onReportClick(targetType, targetId, reason, detail)
                showReportDialog = false
                Toast.makeText(context, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // 상세 화면 전체 레이아웃입니다.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 상단 제목/설명/수정하기/뒤로가기 영역입니다.
        CommunityDetailTopSection(
            onBackClick = onBackClick,
            isEditMode = isEditMode,
            canModifyPost = canModifyPost,
            onEditModeToggle = {
                editTitle = post.title
                editFullContent = post.fullContent
                editCategory = post.category
                isEditMode = true
            }
        )

        // 수정 모드일 때는 입력 UI를 보여주고,
        // 아니면 일반 상세 내용을 보여줍니다.
        if (isEditMode) {
            CommunityDetailEditCard(
                editTitle = editTitle,
                onTitleChange = { newTitle ->
                    editTitle = newTitle
                },
                editFullContent = editFullContent,
                onFullContentChange = { newContent ->
                    editFullContent = newContent
                },
                selectedCategory = editCategory,
                onCategorySelected = { clickedCategory ->
                    editCategory = clickedCategory
                }
            )
        } else {
            CommunityDetailContentCard(
                post = post,
                onCopyLinkClick = {
                    copyCommunityPostLink(context, post.id)
                },
                onReportClick = {
                    reportTargets = communityReportTargetsForPost(post)
                    showReportDialog = true
                }
            )
        }

        // 좋아요 / 삭제 / 수정저장 / 수정취소 영역입니다.
        CommunityDetailActionCard(
            post = post,
            isEditMode = isEditMode,
            canModifyPost = canModifyPost,
            isSaveEnabled = editTitle.isNotBlank() && editFullContent.isNotBlank(),
            onToggleLikeClick = {
                // 초보자용 설명:
                // 여기서는 "클릭했다"는 사실만 바깥으로 보냅니다.
                // 실제 likeCount / isLiked 변경은 AppNavGraph에서 처리합니다.
                onToggleLikeClick(post.id)
            },
            onSaveClick = {
                val trimmedTitle = editTitle.trim()
                val trimmedFullContent = editFullContent.trim()

                if (trimmedTitle.isNotEmpty() && trimmedFullContent.isNotEmpty()) {
                    val updatedPost = post.copy(
                        title = trimmedTitle,
                        content = trimmedFullContent.take(60),
                        fullContent = trimmedFullContent,
                        category = editCategory
                    )

                    onUpdateClick(updatedPost)
                    isEditMode = false
                }
            },
            onCancelEditClick = {
                editTitle = post.title
                editFullContent = post.fullContent
                editCategory = post.category
                isEditMode = false
            },
            onDeleteRequest = {
                showDeleteDialog = true
            }
        )

        // 댓글 목록 / 댓글 입력 / 댓글 수정 / 댓글 삭제 영역입니다.
        CommunityCommentSection(
            comments = post.comments,
            currentUserId = currentUserId,
            commentInput = commentInput,
            onCommentInputChange = { newValue ->
                commentInput = newValue
            },
            onAddCommentClick = {
                val trimmedComment = commentInput.trim()

                if (trimmedComment.isNotEmpty()) {
                    onAddCommentClick(post.id, trimmedComment)
                    commentInput = ""
                }
            },
            editingCommentId = editingCommentId,
            editingCommentText = editingCommentText,
            onEditingCommentTextChange = { newValue ->
                editingCommentText = newValue
            },
            onStartEditComment = { comment ->
                editingCommentId = comment.id
                editingCommentText = comment.content
            },
            onCancelEditComment = {
                editingCommentId = null
                editingCommentText = ""
            },
            onSaveEditComment = { commentId ->
                val trimmedText = editingCommentText.trim()

                if (trimmedText.isNotEmpty()) {
                    onUpdateCommentClick(post.id, commentId, trimmedText)
                    editingCommentId = null
                    editingCommentText = ""
                }
            },
            onDeleteComment = { commentId ->
                onDeleteCommentClick(post.id, commentId)

                if (editingCommentId == commentId) {
                    editingCommentId = null
                    editingCommentText = ""
                }
            },
            onReportComment = { commentId ->
                // 댓글 신고는 게시글 신고와 같은 다이얼로그를 재사용합니다.
                // 대상만 comment로 바꿔서 서버로 보냅니다.
                reportTargets = listOf(
                    ReportTargetOption(
                        type = "comment",
                        id = commentId,
                        label = "댓글"
                    )
                )
                showReportDialog = true
            }
        )
    }
}

// ------------------------------------------------------------
// 게시글을 찾지 못했을 때 보여줄 화면입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailNotFoundScreen(
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    text = "게시글을 찾을 수 없어요",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "목록에서 다시 게시글을 선택해주세요.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpentopiaMutedPurple,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "뒤로가기",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 상세 상단 영역입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailTopSection(
    onBackClick: () -> Unit,
    isEditMode: Boolean,
    canModifyPost: Boolean,
    onEditModeToggle: () -> Unit
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
                        text = "커뮤니티",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isEditMode) {
                            "제목, 내용, 카테고리를 수정한 뒤 저장할 수 있습니다."
                        } else {
                            "다른 사용자들과 소통하고 경험을 나눠보세요."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isEditMode && canModifyPost) {
                    Button(
                        onClick = onEditModeToggle,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpentopiaMutedPurple,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "수정하기",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "목록",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 게시글 일반 상세 내용 카드입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailContentCard(
    post: CommunityPost,
    onCopyLinkClick: () -> Unit,
    onReportClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            val badgeColors = communityCategoryBadgeColors(post.category)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColors.background)
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = post.category.badgeLabel(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColors.content
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = post.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = post.author.take(1),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column {
                        Text(
                            text = post.author,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = post.detailDateText.ifBlank { post.timeText },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Visibility,
                                    contentDescription = "조회수",
                                    modifier = Modifier.width(14.dp).height(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = post.viewCount.toString(),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCopyLinkClick) {
                        // 게시글 링크를 클립보드에 복사하는 버튼입니다.
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "링크 복사",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = onReportClick) {
                        // 신고 아이콘은 비상등 모양으로 표시해서
                        // 경고/신고 기능이라는 점이 바로 보이도록 했습니다.
                        Icon(
                            painter = painterResource(id = R.drawable.ic_emergency_light),
                            contentDescription = "신고하기",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (!post.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "첨부 이미지",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(18.dp))
            }

            Text(
                text = post.fullContent,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ------------------------------------------------------------
// 게시글 수정 입력 카드입니다.
// ------------------------------------------------------------
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CommunityDetailEditCard(
    editTitle: String,
    onTitleChange: (String) -> Unit,
    editFullContent: String,
    onFullContentChange: (String) -> Unit,
    selectedCategory: CommunityCategory,
    onCategorySelected: (CommunityCategory) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "게시글 수정",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "카테고리",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CommunityCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory

                    Box(
                        modifier = Modifier
                            .widthIn(min = 88.dp)
                            .background(
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
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
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Text(
                text = "제목",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = editTitle,
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
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Text(
                text = "내용",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = editFullContent,
                onValueChange = onFullContentChange,
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
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    }
}

// ------------------------------------------------------------
// 좋아요 / 삭제 / 수정저장 / 수정취소 카드입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailActionCard(
    post: CommunityPost,
    isEditMode: Boolean,
    canModifyPost: Boolean,
    isSaveEnabled: Boolean,
    onToggleLikeClick: () -> Unit,
    onSaveClick: () -> Unit,
    onCancelEditClick: () -> Unit,
    onDeleteRequest: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CommunityDetailInfoChip(
                    text = "좋아요 ${post.likeCount}"
                )

                CommunityDetailInfoChip(
                    text = "댓글 ${post.commentCount}"
                )
            }

            if (!isEditMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onToggleLikeClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (post.isLiked) SpentopiaMutedPurple else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (post.isLiked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = if (post.isLiked) "♥ 좋아요 취소" else "♡ 좋아요",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (canModifyPost) {
                        Button(
                            onClick = onDeleteRequest,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            contentPadding = PaddingValues(vertical = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.width(17.dp).height(17.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "삭제",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onSaveClick,
                        enabled = isSaveEnabled,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SpentopiaMutedPurple,
                            contentColor = Color.White,
                            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                            disabledContentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "수정 저장",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onCancelEditClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "수정 취소",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 댓글 전체 섹션입니다.
// ------------------------------------------------------------
// currentUserId:
// - 현재 로그인한 사용자라고 가정하는 id입니다.
// - 각 댓글의 authorId와 비교해서
//   내 댓글인지 아닌지를 판단합니다.
// ------------------------------------------------------------
@Composable
private fun CommunityCommentSection(
    comments: List<CommunityComment>,
    currentUserId: String,
    commentInput: String,
    onCommentInputChange: (String) -> Unit,
    onAddCommentClick: () -> Unit,
    editingCommentId: String?,
    editingCommentText: String,
    onEditingCommentTextChange: (String) -> Unit,
    onStartEditComment: (CommunityComment) -> Unit,
    onCancelEditComment: () -> Unit,
    onSaveEditComment: (String) -> Unit,
    onDeleteComment: (String) -> Unit,
    onReportComment: (String) -> Unit
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
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "댓글",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (comments.isEmpty()) {
                Text(
                    text = "아직 댓글이 없어요. 첫 댓글을 남겨보세요.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                comments.forEach { comment ->
                    CommunityCommentItem(
                        comment = comment,
                        currentUserId = currentUserId,
                        isEditing = editingCommentId == comment.id,
                        editingCommentText = editingCommentText,
                        onEditingCommentTextChange = onEditingCommentTextChange,
                        onStartEdit = {
                            onStartEditComment(comment)
                        },
                        onCancelEdit = onCancelEditComment,
                        onSaveEdit = {
                            onSaveEditComment(comment.id)
                        },
                        onDelete = {
                            onDeleteComment(comment.id)
                        },
                        onReport = {
                            onReportComment(comment.id)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = commentInput,
                onValueChange = onCommentInputChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "댓글을 입력하세요",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            Button(
                onClick = onAddCommentClick,
                enabled = commentInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpentopiaMutedPurple,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 14.dp)
            ) {
                Text(
                    text = "댓글 등록",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ------------------------------------------------------------
// 댓글 1개 카드입니다.
// ------------------------------------------------------------
// currentUserId와 comment.authorId를 비교해서
// 내 댓글이면 수정/삭제 버튼을 보여주고,
// 남의 댓글이면 버튼을 숨깁니다.
// ------------------------------------------------------------
@Composable
private fun CommunityCommentItem(
    comment: CommunityComment,
    currentUserId: String,
    isEditing: Boolean,
    editingCommentText: String,
    onEditingCommentTextChange: (String) -> Unit,
    onStartEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit,
    onReport: () -> Unit
) {
    // 이 댓글이 현재 사용자 댓글인지 판단합니다.
    val isMyComment = comment.authorId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.author,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = comment.timeText,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isMyComment) {
                        IconButton(
                            onClick = onReport,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_emergency_light),
                                contentDescription = "댓글 신고",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            if (!isEditing) {
                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )

                // ------------------------------------------------
                // 내 댓글일 때만 수정/삭제 버튼을 보여줍니다.
                // 남의 댓글이면 이 영역 자체를 출력하지 않습니다.
                // ------------------------------------------------
                if (isMyComment) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onStartEdit
                        ) {
                            Text(text = "댓글 수정")
                        }

                        TextButton(
                            onClick = onDelete
                        ) {
                            Text(text = "댓글 삭제")
                        }
                    }
                }
            } else {
                // ------------------------------------------------
                // 수정 모드는 내 댓글일 때만 의미가 있습니다.
                // 혹시라도 잘못 들어왔을 경우를 막기 위해
                // 내 댓글인지 한 번 더 검사합니다.
                // ------------------------------------------------
                if (isMyComment) {
                    OutlinedTextField(
                        value = editingCommentText,
                        onValueChange = onEditingCommentTextChange,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = onSaveEdit,
                            enabled = editingCommentText.isNotBlank()
                        ) {
                            Text(text = "수정 저장")
                        }

                        TextButton(
                            onClick = onCancelEdit
                        ) {
                            Text(text = "수정 취소")
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 정보 칩 UI입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailInfoChip(
    text: String
) {
    Card(
        shape = RoundedCornerShape(999.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private data class ReportTargetOption(
    val type: String,
    val id: String,
    val label: String
)

private fun communityReportTargetsForPost(post: CommunityPost): List<ReportTargetOption> {
    // 게시글 신고는 1개 대상이 아니라 3개 후보를 보여줍니다.
    // 실제 저장 시에는 사용자가 고른 대상 하나만 서버로 갑니다.
    return listOf(
        ReportTargetOption("post", post.id, "게시글"),
        ReportTargetOption("user_nickname", post.authorId, "작성자 닉네임"),
        ReportTargetOption("user_profile", post.authorId, "작성자 프로필 사진")
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CommunityReportDialog(
    targets: List<ReportTargetOption>,
    onDismiss: () -> Unit,
    onReportClick: (String, String, String, String) -> Unit
) {
    var selectedTargetIndex by remember { mutableStateOf(0) }
    var reason by remember { mutableStateOf("inappropriate") }
    var detail by remember { mutableStateOf("") }
    val selectedTarget = targets.getOrNull(selectedTargetIndex)
    val reasons = listOf(
        "abuse" to "욕설/비방",
        "inappropriate" to "부적절한 내용",
        "spam" to "광고/도배",
        "other" to "기타"
    )

    androidx.compose.runtime.LaunchedEffect(targets) {
        selectedTargetIndex = 0
        reason = "inappropriate"
        detail = ""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "신고하기") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (targets.size > 1) {
                        // 게시글은 대상이 여러 개일 수 있어서
                        // 신고 대상까지 선택하게 합니다.
                        "신고 대상을 선택하고 사유를 입력해주세요.\n신고 내용은 운영자가 확인 후 처리합니다."
                    } else {
                        // 댓글 신고처럼 대상이 하나면
                        // 사유와 상세 내용만 입력하게 합니다.
                        "신고 사유를 입력해주세요.\n신고 내용은 운영자가 확인 후 처리합니다."
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (targets.size > 1) {
                        Text(
                            text = "신고 대상",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                    )

                    FlowRow(
                        maxItemsInEachRow = 1,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        targets.forEachIndexed { index, target ->
                            val selected = selectedTargetIndex == index
                            Button(
                                onClick = { selectedTargetIndex = index },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (selected) {
                                        SpentopiaMutedPurple
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (selected) {
                                        Color.White
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = target.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "신고 사유",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    maxItemsInEachRow = 2,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    reasons.forEach { (value, label) ->
                        val selected = reason == value
                        Button(
                            onClick = { reason = value },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) {
                                    SpentopiaMutedPurple
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                contentColor = if (selected) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "상세 내용",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    OutlinedTextField(
                        value = detail,
                        onValueChange = { newValue ->
                            detail = if (newValue.length <= 500) newValue else newValue.take(500)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(text = "신고 내용을 입력해주세요. 필수 입력입니다.")
                        },
                        minLines = 4,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.65f),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Text(
                        text = "${detail.length}/500",
                        modifier = Modifier.align(Alignment.End),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            val canSubmit = selectedTarget != null && detail.isNotBlank()
            Button(
                onClick = {
                    // 선택한 대상 + 사유 + 상세 내용이 모두 모이면
                    // 바깥 ViewModel로 신고 데이터를 전달합니다.
                    val target = selectedTarget ?: return@Button
                    val trimmedDetail = detail.trim()
                    if (trimmedDetail.isEmpty()) return@Button
                    onReportClick(target.type, target.id, reason, trimmedDetail)
                },
                enabled = canSubmit,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SpentopiaMutedPurple,
                    contentColor = Color.White,
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant,
                    disabledContentColor = Color.White
                )
            ) {
                Text(text = "신고하기")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "취소")
            }
        }
    )
}

private fun copyCommunityPostLink(context: Context, postId: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = "spentopia://community/posts/$postId"
    clipboard.setPrimaryClip(ClipData.newPlainText("커뮤니티 게시글 링크", text))
    Toast.makeText(context, "링크가 복사되었습니다.", Toast.LENGTH_SHORT).show()
}

private data class CommunityDetailBadgeColors(
    val background: Color,
    val content: Color
)

@Composable
private fun communityCategoryBadgeColors(category: CommunityCategory): CommunityDetailBadgeColors {
    val isDark = isSystemInDarkTheme()
    return when (category) {
        CommunityCategory.NOTICE -> if (isDark) {
            CommunityDetailBadgeColors(Color(0xFF164E63), Color(0xFFBAE6FD))
        } else {
            CommunityDetailBadgeColors(Color(0xFF0284C7), Color.White)
        }
        CommunityCategory.AVATAR_CONTEST -> if (isDark) {
            CommunityDetailBadgeColors(Color(0xFF713F12), Color(0xFFFEF3C7))
        } else {
            CommunityDetailBadgeColors(Color(0xFFB45309), Color.White)
        }
        CommunityCategory.REQUEST -> if (isDark) {
            CommunityDetailBadgeColors(Color(0xFF581C87), Color(0xFFE9D5FF))
        } else {
            CommunityDetailBadgeColors(Color(0xFF7E22CE), Color.White)
        }
        CommunityCategory.FREE_BOARD -> if (isDark) {
            CommunityDetailBadgeColors(Color(0xFF064E3B), Color(0xFFA7F3D0))
        } else {
            CommunityDetailBadgeColors(Color(0xFF059669), Color.White)
        }
    }
}

private fun CommunityCategory.badgeLabel(): String {
    return when (this) {
        CommunityCategory.NOTICE -> "공지"
        CommunityCategory.AVATAR_CONTEST -> "콘테스트"
        CommunityCategory.REQUEST -> "아이템 요청"
        CommunityCategory.FREE_BOARD -> "자유"
    }
}

// ------------------------------------------------------------
// 프리뷰입니다.
// ------------------------------------------------------------
@Preview(showBackground = true)
@Composable
private fun CommunityDetailScreenPreview() {
    CommunityDetailScreen(
        post = CommunityPost(
            id = "1",
            title = "미리보기용 제목입니다",
            content = "미리보기용 짧은 내용입니다.",
            fullContent = "미리보기용 전체 내용입니다. 상세 화면에서는 전체 내용이 보이도록 구성했습니다.",
            author = "미리보기작성자",
            timeText = "방금 전",
            likeCount = 3,
            commentCount = 2,
            tagText = "미리보기",
            category = CommunityCategory.FREE_BOARD,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "current_user",
                    author = "현재사용자",
                    content = "첫 번째 댓글입니다.",
                    timeText = "방금 전"
                ),
                CommunityComment(
                    id = "2",
                    authorId = "user_x",
                    author = "다른사용자",
                    content = "두 번째 댓글입니다.",
                    timeText = "1분 전"
                )
            ),
            isLiked = true
        )
    )
}
