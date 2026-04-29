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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

@Composable
fun CommunityDetailScreen(
    post: CommunityPost?,
    onBackClick: () -> Unit = {},
    onUpdateClick: (CommunityPost) -> Unit = {},
    onDeleteClick: (Int) -> Unit = {},
    onToggleLikeClick: (Int) -> Unit = {},
    onAddCommentClick: (Int, String) -> Unit = { _, _ -> },
    onUpdateCommentClick: (Int, Int, String) -> Unit = { _, _, _ -> },
    onDeleteCommentClick: (Int, Int) -> Unit = { _, _ -> }
) {
    // post가 null이면 안전하게 안내 화면으로 보냅니다.
    if (post == null) {
        CommunityDetailNotFoundScreen(
            onBackClick = onBackClick
        )
        return
    }

    // --------------------------------------------------------
    // currentUserId:
    // - 현재 로그인한 사용자라고 가정하는 임시 id입니다.
    // - 아직 로그인 연동 전이라 문자열로만 고정합니다.
    // - 나중에 실제 로그인 사용자 id로 교체하면 됩니다.
    // --------------------------------------------------------
    val currentUserId = "current_user"

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

    // 새 댓글 입력창 상태입니다.
    var commentInput by remember(post.id) { mutableStateOf("") }

    // 현재 수정 중인 댓글 id입니다.
    // -1이면 수정 중인 댓글이 없다는 뜻입니다.
    var editingCommentId by remember(post.id) { mutableIntStateOf(-1) }

    // 현재 수정 중인 댓글 내용입니다.
    var editingCommentText by remember(post.id) { mutableStateOf("") }

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
                post = post
            )
        }

        // 좋아요 / 삭제 / 수정저장 / 수정취소 영역입니다.
        CommunityDetailActionCard(
            post = post,
            isEditMode = isEditMode,
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
                editingCommentId = -1
                editingCommentText = ""
            },
            onSaveEditComment = { commentId ->
                val trimmedText = editingCommentText.trim()

                if (trimmedText.isNotEmpty()) {
                    onUpdateCommentClick(post.id, commentId, trimmedText)
                    editingCommentId = -1
                    editingCommentText = ""
                }
            },
            onDeleteComment = { commentId ->
                onDeleteCommentClick(post.id, commentId)

                if (editingCommentId == commentId) {
                    editingCommentId = -1
                    editingCommentText = ""
                }
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
                    color = Color(0xFF6E7684)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBackClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE24BB4),
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
                        text = if (isEditMode) "게시글 수정" else "게시글 상세",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isEditMode) {
                            "제목, 내용, 카테고리를 수정한 뒤 저장할 수 있습니다."
                        } else {
                            "선택한 게시글의 상세 내용을 확인하고 좋아요와 댓글을 남길 수 있습니다."
                        },
                        fontSize = 13.sp,
                        color = Color(0xFF6E7684),
                        lineHeight = 19.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (!isEditMode) {
                    Button(
                        onClick = onEditModeToggle,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE24BB4),
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

// ------------------------------------------------------------
// 게시글 일반 상세 내용 카드입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailContentCard(
    post: CommunityPost
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.category.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF4A7AE8)
                )

                Text(
                    text = post.timeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = post.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF20242C)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "작성자: ${post.author}",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF3B3F47)
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = post.fullContent,
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = Color(0xFF4F5663)
            )
        }
    }
}

// ------------------------------------------------------------
// 게시글 수정 입력 카드입니다.
// ------------------------------------------------------------
@Composable
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
                    focusedBorderColor = Color(0xFFE24BB4),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = Color(0xFF1E2430),
                    unfocusedTextColor = Color(0xFF1E2430),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
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
                    focusedBorderColor = Color(0xFFE24BB4),
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

// ------------------------------------------------------------
// 좋아요 / 삭제 / 수정저장 / 수정취소 카드입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityDetailActionCard(
    post: CommunityPost,
    isEditMode: Boolean,
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
                            containerColor = if (post.isLiked) Color(0xFFE24BB4) else Color(0xFFEDEFF4),
                            contentColor = if (post.isLiked) Color.White else Color(0xFF2B313B)
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = if (post.isLiked) "♥ 좋아요 취소" else "♡ 좋아요",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onDeleteRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE24BB4),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp)
                    ) {
                        Text(
                            text = "게시글 삭제",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
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
                            containerColor = Color(0xFFE24BB4),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFF3B8D9),
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
                            containerColor = Color(0xFFEDEFF4),
                            contentColor = Color(0xFF2B313B)
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
    editingCommentId: Int,
    editingCommentText: String,
    onEditingCommentTextChange: (String) -> Unit,
    onStartEditComment: (CommunityComment) -> Unit,
    onCancelEditComment: () -> Unit,
    onSaveEditComment: (Int) -> Unit,
    onDeleteComment: (Int) -> Unit
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
                    color = Color(0xFF6E7684)
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
                    focusedBorderColor = Color(0xFFE24BB4),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedTextColor = Color(0xFF1E2430),
                    unfocusedTextColor = Color(0xFF1E2430),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Button(
                onClick = onAddCommentClick,
                enabled = commentInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE24BB4),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFF3B8D9),
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
    onDelete: () -> Unit
) {
    // 이 댓글이 현재 사용자 댓글인지 판단합니다.
    val isMyComment = comment.authorId == currentUserId

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF8F9FC)
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
                    color = Color(0xFF20242C)
                )

                Text(
                    text = comment.timeText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isEditing) {
                Text(
                    text = comment.content,
                    fontSize = 14.sp,
                    color = Color(0xFF4F5663),
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
                            focusedBorderColor = Color(0xFFE24BB4),
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedTextColor = Color(0xFF1E2430),
                            unfocusedTextColor = Color(0xFF1E2430),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
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
            containerColor = Color(0xFFF1F3F7)
        )
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            fontSize = 12.sp,
            color = Color(0xFF6D7480)
        )
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
            id = 1,
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
                    id = 1,
                    authorId = "current_user",
                    author = "현재사용자",
                    content = "첫 번째 댓글입니다.",
                    timeText = "방금 전"
                ),
                CommunityComment(
                    id = 2,
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