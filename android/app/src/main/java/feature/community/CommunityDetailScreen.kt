package com.ict.spentopia.feature.community // 이 파일이 속한 패키지 위치를 적음

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

import android.content.ClipData // ClipData 기능을 가져옴
import android.content.ClipboardManager // ClipboardManager 기능을 가져옴
import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.widget.Toast // 짧은 알림 메시지 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.ExperimentalLayoutApi // ExperimentalLayoutApi 기능을 가져옴
import androidx.compose.foundation.layout.FlowRow // FlowRow 기능을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.layout.widthIn // widthIn 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.ContentCopy // ContentCopy 기능을 가져옴
import androidx.compose.material.icons.filled.DeleteOutline // DeleteOutline 기능을 가져옴
import androidx.compose.material.icons.filled.Edit // Edit 기능을 가져옴
import androidx.compose.material.icons.filled.Visibility // Visibility 기능을 가져옴
import androidx.compose.material3.AlertDialog // AlertDialog 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.IconButton // 아이콘 버튼 컴포넌트를 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.tooling.preview.Preview // Preview 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import com.ict.spentopia.R // R 기능을 가져옴
import coil.compose.AsyncImage // AsyncImage 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.spentopiaAppButtonColor
import com.ict.spentopia.ui.theme.spentopiaAppButtonContentColor

@Composable
private fun isCommunityDetailDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground
}

@Composable
private fun communityDetailSoftCardColor(): Color {
    return if (isCommunityDetailDarkTheme()) Color(0xFF171A2B) else Color(0xFFF7FBFF)
}

@Composable
private fun communityDetailSoftCardBorderColor(): Color {
    return if (isCommunityDetailDarkTheme()) Color(0xFF4C3B7A) else Color(0xFF7DD3FC)
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CommunityDetailScreen( // CommunityDetailScreen 함수를 선언함
    post: CommunityPost?, // post 값을 받음
    currentUserId: String = "current_user", // currentUserId 값을 받음
    currentUserRole: String = "user", // currentUserRole 값을 받음
    onBackClick: () -> Unit = {}, // onBackClick 때 실행할 함수를 받음
    onUpdateClick: (CommunityPost) -> Unit = {}, // onUpdateClick 때 실행할 함수를 받음
    onDeleteClick: (String) -> Unit = {}, // onDeleteClick 때 실행할 함수를 받음
    onToggleLikeClick: (String) -> Unit = {}, // onToggleLikeClick 때 실행할 함수를 받음
    onAddCommentClick: (String, String) -> Unit = { _, _ -> }, // Unit 값을 정해줌
    onUpdateCommentClick: (String, String, String) -> Unit = { _, _, _ -> }, // Unit 값을 정해줌
    onDeleteCommentClick: (String, String) -> Unit = { _, _ -> }, // Unit 값을 정해줌
    onReportClick: (String, String, String, String) -> Unit = { _, _, _, _ -> } // Unit 값을 정해줌
) { // 이 블록 안의 내용이 시작됨
    // post가 null이면 안전하게 안내 화면으로 보냅니다.
    if (post == null) { // 조건이 맞는지 확인함
        CommunityDetailNotFoundScreen( // Community Detail Not Found Screen 함수를 실행함
            onBackClick = onBackClick // onBackClick 때 실행할 함수를 onBackClick 때 실행할 함수에 넣음
        )
        return
    }

    // 게시글 수정 모드 여부입니다.
    var isEditMode by remember(post.id) { mutableStateOf(false) } // 화면에서 바뀔 수정 모드인지 저장함

    // 수정 중인 제목 상태입니다.
    var editTitle by remember(post.id) { mutableStateOf(post.title) } // 화면에서 바뀔 editTitle 값을 저장함

    // 수정 중인 본문 상태입니다.
    var editFullContent by remember(post.id) { mutableStateOf(post.fullContent) } // 화면에서 바뀔 editFullContent 값을 저장함

    // 수정 중인 카테고리 상태입니다.
    var editCategory by remember(post.id) { mutableStateOf(post.category) } // 화면에서 바뀔 editCategory 값을 저장함

    // 게시글 삭제 확인 다이얼로그 표시 여부입니다.
    var showDeleteDialog by remember(post.id) { mutableStateOf(false) } // 화면에서 바뀔 showDeleteDialog 값을 저장함

    var showReportDialog by remember(post.id) { mutableStateOf(false) } // 화면에서 바뀔 showReportDialog 값을 저장함
    var reportTargets by remember(post.id) { mutableStateOf<List<ReportTargetOption>>(emptyList()) } // 화면에서 바뀔 reportTargets 값을 저장함

    // 새 댓글 입력창 상태입니다.
    var commentInput by remember(post.id) { mutableStateOf("") } // 화면에서 바뀔 commentInput 값을 저장함

    // 현재 수정 중인 댓글 id입니다.
    var editingCommentId by remember(post.id) { mutableStateOf<String?>(null) } // 화면에서 바뀔 editingCommentId 값을 저장함

    // 현재 수정 중인 댓글 내용입니다.
    var editingCommentText by remember(post.id) { mutableStateOf("") } // 화면에서 바뀔 editingCommentText 값을 저장함

    val context = LocalContext.current // 현재 화면 정보를 저장함
    val canModifyPost = post.authorId == currentUserId || // canModifyPost 값을 저장함
        (post.category == CommunityCategory.NOTICE && currentUserRole == "admin") // post.category 값을 정해줌

    // 게시글 삭제 확인 다이얼로그입니다.
    if (showDeleteDialog) { // 조건이 맞는지 확인함
        AlertDialog( // 팝업 확인창을 보여줌
            onDismissRequest = { // onDismissRequest 때 실행할 함수를 정해줌
                showDeleteDialog = false // false 값을 showDeleteDialog 값에 넣음
            },
            title = { // 제목을 정해줌
                Text(text = "게시글 삭제") // 화면에 글자를 보여줌
            },
            text = { // text 값을 정해줌
                Text(text = "정말 이 게시글을 삭제하시겠습니까?") // 화면에 글자를 보여줌
            },
            confirmButton = { // confirmButton 값을 정해줌
                TextButton( // 누를 수 있는 버튼을 만듦
                    onClick = { // 눌렀을 때 실행할 함수를 정해줌
                        onDeleteClick(post.id) // 데이터를 지우는 함수를 실행함
                        showDeleteDialog = false // false 값을 showDeleteDialog 값에 넣음
                    }
                ) { // 이 블록 안의 내용이 시작됨
                    Text(text = "삭제") // 화면에 글자를 보여줌
                }
            },
            dismissButton = { // dismissButton 값을 정해줌
                TextButton( // 누를 수 있는 버튼을 만듦
                    onClick = { // 눌렀을 때 실행할 함수를 정해줌
                        showDeleteDialog = false // false 값을 showDeleteDialog 값에 넣음
                    }
                ) { // 이 블록 안의 내용이 시작됨
                    Text(text = "취소") // 화면에 글자를 보여줌
                }
            }
        )
    }

    if (showReportDialog) { // 조건이 맞는지 확인함
        CommunityReportDialog( // Community Report Dialog 함수를 실행함
            targets = reportTargets, // reportTargets 값을 targets 값에 넣음
            onDismiss = { showReportDialog = false }, // 닫을 때 실행할 함수를 정해줌
            onReportClick = { targetType, targetId, reason, detail -> // onReportClick 때 실행할 함수를 정해줌
                onReportClick(targetType, targetId, reason, detail) // on Report Click 함수를 실행함
                showReportDialog = false // false 값을 showReportDialog 값에 넣음
                Toast.makeText(context, "신고가 접수되었습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            }
        )
    }

    // 상세 화면 전체 레이아웃입니다.
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        // 상단 제목/설명/수정하기/뒤로가기 영역입니다.
        CommunityDetailTopSection( // Community Detail Top Section 함수를 실행함
            onBackClick = onBackClick, // onBackClick 때 실행할 함수를 onBackClick 때 실행할 함수에 넣음
            isEditMode = isEditMode, // isEditMode인지 여부를 isEditMode인지 여부에 넣음
            canModifyPost = canModifyPost, // canModifyPost 값을 canModifyPost 값에 넣음
            onEditModeToggle = { // onEditModeToggle 때 실행할 함수를 정해줌
                editTitle = post.title // editTitle 값을 정해줌
                editFullContent = post.fullContent // editFullContent 값을 정해줌
                editCategory = post.category // editCategory 값을 정해줌
                isEditMode = true // true 값을 isEditMode인지 여부에 넣음
            }
        )

        // 수정 모드일 때는 입력 UI를 보여주고,
        // 아니면 일반 상세 내용을 보여줍니다.
        if (isEditMode) { // 조건이 맞는지 확인함
            CommunityDetailEditCard( // 내용을 카드 모양으로 묶어서 보여줌
                editTitle = editTitle, // editTitle 값을 editTitle 값에 넣음
                onTitleChange = { newTitle -> // onTitleChange 때 실행할 함수를 정해줌
                    editTitle = newTitle // newTitle 값을 editTitle 값에 넣음
                },
                editFullContent = editFullContent, // editFullContent 값을 editFullContent 값에 넣음
                onFullContentChange = { newContent -> // onFullContentChange 때 실행할 함수를 정해줌
                    editFullContent = newContent // newContent 값을 editFullContent 값에 넣음
                },
                selectedCategory = editCategory, // editCategory 값을 selectedCategory 값에 넣음
                onCategorySelected = { clickedCategory -> // onCategorySelected 때 실행할 함수를 정해줌
                    editCategory = clickedCategory // clickedCategory 값을 editCategory 값에 넣음
                }
            )
        } else { // 이 블록 안의 내용이 시작됨
            CommunityDetailContentCard( // 내용을 카드 모양으로 묶어서 보여줌
                post = post, // post 값을 post 값에 넣음
                onCopyLinkClick = { // onCopyLinkClick 때 실행할 함수를 정해줌
                    copyCommunityPostLink(context, post.id) // copy Community Post Link 함수를 실행함
                },
                onReportClick = { // onReportClick 때 실행할 함수를 정해줌
                    reportTargets = communityReportTargetsForPost(post) // reportTargets 값을 정해줌
                    showReportDialog = true // true 값을 showReportDialog 값에 넣음
                }
            )
        }

        // 좋아요 / 삭제 / 수정저장 / 수정취소 영역입니다.
        CommunityDetailActionCard( // 내용을 카드 모양으로 묶어서 보여줌
            post = post, // post 값을 post 값에 넣음
            isEditMode = isEditMode, // isEditMode인지 여부를 isEditMode인지 여부에 넣음
            canModifyPost = canModifyPost, // canModifyPost 값을 canModifyPost 값에 넣음
            isSaveEnabled = editTitle.isNotBlank() && editFullContent.isNotBlank(), // isSaveEnabled인지 여부를 정해줌
            onToggleLikeClick = { // onToggleLikeClick 때 실행할 함수를 정해줌
                // 초보자용 설명:
                // 여기서는 "클릭했다"는 사실만 바깥으로 보냅니다.
                // 실제 likeCount / isLiked 변경은 AppNavGraph에서 처리합니다.
                onToggleLikeClick(post.id) // on Toggle Like Click 함수를 실행함
            },
            onSaveClick = { // onSaveClick 때 실행할 함수를 정해줌
                val trimmedTitle = editTitle.trim() // trimmedTitle 값을 저장함
                val trimmedFullContent = editFullContent.trim() // trimmedFullContent 값을 저장함

                if (trimmedTitle.isNotEmpty() && trimmedFullContent.isNotEmpty()) { // 조건이 맞는지 확인함
                    val updatedPost = post.copy( // updatedPost 값을 저장함
                        title = trimmedTitle, // trimmedTitle 값을 제목에 넣음
                        content = trimmedFullContent.take(60), // 내용을 정해줌
                        fullContent = trimmedFullContent, // trimmedFullContent 값을 fullContent 값에 넣음
                        category = editCategory // editCategory 값을 카테고리에 넣음
                    )

                    onUpdateClick(updatedPost) // 데이터를 수정하는 함수를 실행함
                    isEditMode = false // false 값을 isEditMode인지 여부에 넣음
                }
            },
            onCancelEditClick = { // onCancelEditClick 때 실행할 함수를 정해줌
                editTitle = post.title // editTitle 값을 정해줌
                editFullContent = post.fullContent // editFullContent 값을 정해줌
                editCategory = post.category // editCategory 값을 정해줌
                isEditMode = false // false 값을 isEditMode인지 여부에 넣음
            },
            onDeleteRequest = { // onDeleteRequest 때 실행할 함수를 정해줌
                showDeleteDialog = true // true 값을 showDeleteDialog 값에 넣음
            }
        )

        // 댓글 목록 / 댓글 입력 / 댓글 수정 / 댓글 삭제 영역입니다.
        CommunityCommentSection( // Community Comment Section 함수를 실행함
            comments = post.comments, // comments 값을 정해줌
            currentUserId = currentUserId, // currentUserId 값을 currentUserId 값에 넣음
            commentInput = commentInput, // commentInput 값을 commentInput 값에 넣음
            onCommentInputChange = { newValue -> // onCommentInputChange 때 실행할 함수를 정해줌
                commentInput = newValue // newValue 값을 commentInput 값에 넣음
            },
            onAddCommentClick = { // onAddCommentClick 때 실행할 함수를 정해줌
                val trimmedComment = commentInput.trim() // trimmedComment 값을 저장함

                if (trimmedComment.isNotEmpty()) { // 조건이 맞는지 확인함
                    onAddCommentClick(post.id, trimmedComment) // on Add Comment Click 함수를 실행함
                    commentInput = "" // commentInput 값을 정해줌
                }
            },
            editingCommentId = editingCommentId, // editingCommentId 값을 editingCommentId 값에 넣음
            editingCommentText = editingCommentText, // editingCommentText 값을 editingCommentText 값에 넣음
            onEditingCommentTextChange = { newValue -> // onEditingCommentTextChange 때 실행할 함수를 정해줌
                editingCommentText = newValue // newValue 값을 editingCommentText 값에 넣음
            },
            onStartEditComment = { comment -> // onStartEditComment 때 실행할 함수를 정해줌
                editingCommentId = comment.id // editingCommentId 값을 정해줌
                editingCommentText = comment.content // editingCommentText 값을 정해줌
            },
            onCancelEditComment = { // onCancelEditComment 때 실행할 함수를 정해줌
                editingCommentId = null // null 값을 editingCommentId 값에 넣음
                editingCommentText = "" // editingCommentText 값을 정해줌
            },
            onSaveEditComment = { commentId -> // onSaveEditComment 때 실행할 함수를 정해줌
                val trimmedText = editingCommentText.trim() // trimmedText 값을 저장함

                if (trimmedText.isNotEmpty()) { // 조건이 맞는지 확인함
                    onUpdateCommentClick(post.id, commentId, trimmedText) // 데이터를 수정하는 함수를 실행함
                    editingCommentId = null // null 값을 editingCommentId 값에 넣음
                    editingCommentText = "" // editingCommentText 값을 정해줌
                }
            },
            onDeleteComment = { commentId -> // onDeleteComment 때 실행할 함수를 정해줌
                onDeleteCommentClick(post.id, commentId) // 데이터를 지우는 함수를 실행함

                if (editingCommentId == commentId) { // 조건이 맞는지 확인함
                    editingCommentId = null // null 값을 editingCommentId 값에 넣음
                    editingCommentText = "" // editingCommentText 값을 정해줌
                }
            },
            onReportComment = { commentId -> // onReportComment 때 실행할 함수를 정해줌
                // 댓글 신고는 게시글 신고와 같은 다이얼로그를 재사용합니다.
                // 대상만 comment로 바꿔서 서버로 보냅니다.
                reportTargets = listOf( // reportTargets 값을 정해줌
                    ReportTargetOption( // Report Target Option 함수를 실행함
                        type = "comment", // type 값을 정해줌
                        id = commentId, // commentId 값을 아이디에 넣음
                        label = "댓글" // label 값을 정해줌
                    )
                )
                showReportDialog = true // true 값을 showReportDialog 값에 넣음
            }
        )
    }
}

// ------------------------------------------------------------
// 게시글을 찾지 못했을 때 보여줄 화면입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailNotFoundScreen( // CommunityDetailNotFoundScreen 함수를 선언함
    onBackClick: () -> Unit // onBackClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors( // colors 값을 정해줌
                containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "게시글을 찾을 수 없어요", // text 값을 정해줌
                    fontSize = 20.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = "목록에서 다시 게시글을 선택해주세요.", // text 값을 정해줌
                    fontSize = 14.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onBackClick, // onBackClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
                    )
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "뒤로가기", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 상세 상단 영역입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailTopSection( // CommunityDetailTopSection 함수를 선언함
    onBackClick: () -> Unit, // onBackClick 때 실행할 함수를 받음
    isEditMode: Boolean, // isEditMode인지 여부를 받음
    canModifyPost: Boolean, // canModifyPost 값을 받음
    onEditModeToggle: () -> Unit // onEditModeToggle 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.Top // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Column( // 안쪽 UI를 세로로 배치함
                    modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "커뮤니티", // text 값을 정해줌
                        fontSize = 20.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )

                    Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

                    Text( // 화면에 글자를 보여줌
                        text = if (isEditMode) { // text 값을 정해줌
                            "제목, 내용, 카테고리를 수정한 뒤 저장할 수 있습니다."
                        } else { // 이 블록 안의 내용이 시작됨
                            "다른 사용자들과 소통하고 경험을 나눠보세요."
                        },
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                        lineHeight = 19.sp // lineHeight 값을 정해줌
                    )
                }

                Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

                if (!isEditMode && canModifyPost) { // 조건이 맞는지 확인함
                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onEditModeToggle, // onEditModeToggle 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp) // contentPadding 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = "수정하기", // text 값을 정해줌
                            fontSize = 12.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함
                }

                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onBackClick, // onBackClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = MaterialTheme.colorScheme.surfaceVariant, // containerColor 값을 정해줌
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "목록", // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold // fontWeight 값을 정해줌
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------
// 게시글 일반 상세 내용 카드입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailContentCard( // CommunityDetailContentCard 함수를 선언함
    post: CommunityPost, // post 값을 받음
    onCopyLinkClick: () -> Unit, // onCopyLinkClick 때 실행할 함수를 받음
    onReportClick: () -> Unit // onReportClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            val badgeColors = communityCategoryBadgeColors(post.category) // badgeColors 값을 저장함

            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .clip(RoundedCornerShape(6.dp))
                        .background(badgeColors.background)
                        .padding(horizontal = 9.dp, vertical = 5.dp) // .padding(horizontal 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = post.category.badgeLabel(), // text 값을 정해줌
                        fontSize = 11.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                        color = badgeColors.content // color 값을 정해줌
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = post.title, // text 값을 정해줌
                fontSize = 20.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                    horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .width(42.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = post.author.take(1), // text 값을 정해줌
                            fontSize = 16.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                        )
                    }

                    Column { // 안쪽 UI를 세로로 배치함
                        Text( // 화면에 글자를 보여줌
                            text = post.author, // text 값을 정해줌
                            fontSize = 14.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                        )

                        Row( // 안쪽 UI를 가로로 배치함
                            verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                            horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 화면에 글자를 보여줌
                                text = post.detailDateText.ifBlank { post.timeText }, // text 값을 정해줌
                                fontSize = 12.sp, // fontSize 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                            )

                            Row( // 안쪽 UI를 가로로 배치함
                                verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                                horizontalArrangement = Arrangement.spacedBy(3.dp) // horizontalArrangement 값을 정해줌
                            ) { // 이 블록 안의 내용이 시작됨
                                Icon( // 화면에 아이콘을 보여줌
                                    imageVector = Icons.Filled.Visibility, // imageVector 값을 정해줌
                                    contentDescription = "조회수", // contentDescription 값을 정해줌
                                    modifier = Modifier.width(14.dp).height(14.dp), // UI 크기나 여백 같은 모양을 정함
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                                )
                                Text( // 화면에 글자를 보여줌
                                    text = post.viewCount.toString(), // text 값을 정해줌
                                    fontSize = 12.sp, // fontSize 값을 정해줌
                                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                                )
                            }
                        }
                    }
                }

                Row( // 안쪽 UI를 가로로 배치함
                    horizontalArrangement = Arrangement.spacedBy(6.dp), // horizontalArrangement 값을 정해줌
                    verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    IconButton(onClick = onCopyLinkClick) { // 누를 수 있는 버튼을 만듦
                        // 게시글 링크를 클립보드에 복사하는 버튼입니다.
                        Icon( // 화면에 아이콘을 보여줌
                            imageVector = Icons.Filled.ContentCopy, // imageVector 값을 정해줌
                            contentDescription = "링크 복사", // contentDescription 값을 정해줌
                            modifier = Modifier.size(20.dp) // UI 크기나 여백 같은 모양을 정함
                        )
                    }

                    IconButton(onClick = onReportClick) { // 누를 수 있는 버튼을 만듦
                        // 신고 아이콘은 비상등 모양으로 표시해서
                        // 경고/신고 기능이라는 점이 바로 보이도록 했습니다.
                        Icon( // 화면에 아이콘을 보여줌
                            painter = painterResource(id = R.drawable.ic_emergency_light), // painter 값을 정해줌
                            contentDescription = "신고하기", // contentDescription 값을 정해줌
                            modifier = Modifier.size(22.dp), // UI 크기나 여백 같은 모양을 정함
                            tint = MaterialTheme.colorScheme.error // tint 값을 정해줌
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            if (!post.imageUrl.isNullOrBlank()) { // 조건이 맞는지 확인함
                AsyncImage( // 화면에 이미지를 보여줌
                    model = post.imageUrl, // model 값을 정해줌
                    contentDescription = "첨부 이미지", // contentDescription 값을 정해줌
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop // contentScale 값을 정해줌
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            Text( // 화면에 글자를 보여줌
                text = post.fullContent, // text 값을 정해줌
                fontSize = 15.sp, // fontSize 값을 정해줌
                lineHeight = 24.sp, // lineHeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

// ------------------------------------------------------------
// 게시글 수정 입력 카드입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
private fun CommunityDetailEditCard( // CommunityDetailEditCard 함수를 선언함
    editTitle: String, // editTitle 값을 받음
    onTitleChange: (String) -> Unit, // onTitleChange 때 실행할 함수를 받음
    editFullContent: String, // editFullContent 값을 받음
    onFullContentChange: (String) -> Unit, // onFullContentChange 때 실행할 함수를 받음
    selectedCategory: CommunityCategory, // selectedCategory 값을 받음
    onCategorySelected: (CommunityCategory) -> Unit // onCategorySelected 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "게시글 수정", // text 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Text( // 화면에 글자를 보여줌
                text = "카테고리", // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            FlowRow( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
                verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                CommunityCategory.entries.forEach { category ->
                    val isSelected = category == selectedCategory // 선택된 항목인지 저장함

                    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .widthIn(min = 88.dp) // .widthIn(min 값을 정해줌
                            .background(
                                color = if (isSelected) { // color 값을 정해줌
                                    MaterialTheme.colorScheme.primaryContainer
                                } else { // 이 블록 안의 내용이 시작됨
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(999.dp) // shape 값을 정해줌
                            )
                            .clickable { // 이 블록 안의 내용이 시작됨
                                onCategorySelected(category) // on Category Selected 함수를 실행함
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = category.label, // text 값을 정해줌
                            fontSize = 12.sp, // fontSize 값을 정해줌
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, // fontWeight 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                        )
                    }
                }
            }

            Text( // 화면에 글자를 보여줌
                text = "제목", // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = editTitle, // editTitle 값을 입력값에 넣음
                onValueChange = onTitleChange, // onTitleChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "제목을 입력하세요", // text 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                singleLine = true, // true 값을 singleLine 값에 넣음
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f), // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                    focusedContainerColor = MaterialTheme.colorScheme.surface, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface // unfocusedContainerColor 값을 정해줌
                )
            )

            Text( // 화면에 글자를 보여줌
                text = "내용", // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = editFullContent, // editFullContent 값을 입력값에 넣음
                onValueChange = onFullContentChange, // onFullContentChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "내용을 입력하세요", // text 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                minLines = 8, // minLines 값을 정해줌
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f), // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                    focusedContainerColor = MaterialTheme.colorScheme.surface, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface // unfocusedContainerColor 값을 정해줌
                )
            )
        }
    }
}

// ------------------------------------------------------------
// 좋아요 / 삭제 / 수정저장 / 수정취소 카드입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailActionCard( // CommunityDetailActionCard 함수를 선언함
    post: CommunityPost, // post 값을 받음
    isEditMode: Boolean, // isEditMode인지 여부를 받음
    canModifyPost: Boolean, // canModifyPost 값을 받음
    isSaveEnabled: Boolean, // isSaveEnabled인지 여부를 받음
    onToggleLikeClick: () -> Unit, // onToggleLikeClick 때 실행할 함수를 받음
    onSaveClick: () -> Unit, // onSaveClick 때 실행할 함수를 받음
    onCancelEditClick: () -> Unit, // onCancelEditClick 때 실행할 함수를 받음
    onDeleteRequest: () -> Unit // onDeleteRequest 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                CommunityDetailInfoChip( // Community Detail Info Chip 함수를 실행함
                    text = "좋아요 ${post.likeCount}" // text 값을 정해줌
                )

                CommunityDetailInfoChip( // Community Detail Info Chip 함수를 실행함
                    text = "댓글 ${post.commentCount}" // text 값을 정해줌
                )
            }

            if (!isEditMode) { // 조건이 맞는지 확인함
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onToggleLikeClick, // onToggleLikeClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = if (post.isLiked) SpentopiaMutedPurple else MaterialTheme.colorScheme.surfaceVariant, // containerColor 값을 정해줌
                            contentColor = if (post.isLiked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant // contentColor 값을 정해줌
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp) // contentPadding 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = if (post.isLiked) "♥ 좋아요 취소" else "♡ 좋아요", // text 값을 정해줌
                            fontSize = 15.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                        )
                    }

                    if (canModifyPost) { // 조건이 맞는지 확인함
                        Button( // 누를 수 있는 버튼을 만듦
                            onClick = onDeleteRequest, // onDeleteRequest 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                            colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                                containerColor = MaterialTheme.colorScheme.errorContainer, // containerColor 값을 정해줌
                                contentColor = MaterialTheme.colorScheme.onErrorContainer // contentColor 값을 정해줌
                            ),
                            contentPadding = PaddingValues(vertical = 14.dp) // contentPadding 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Icon( // 화면에 아이콘을 보여줌
                                imageVector = Icons.Filled.DeleteOutline, // imageVector 값을 정해줌
                                contentDescription = null, // null 값을 contentDescription 값에 넣음
                                modifier = Modifier.width(17.dp).height(17.dp) // UI 크기나 여백 같은 모양을 정함
                            )
                            Spacer(modifier = Modifier.width(5.dp)) // UI 크기나 여백 같은 모양을 정함
                            Text( // 화면에 글자를 보여줌
                                text = "삭제", // text 값을 정해줌
                                fontSize = 15.sp, // fontSize 값을 정해줌
                                fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                            )
                        }
                    }
                }
            } else { // 이 블록 안의 내용이 시작됨
                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onSaveClick, // onSaveClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        enabled = isSaveEnabled, // isSaveEnabled인지 여부를 enabled 값에 넣음
                        modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // contentColor 값을 정해줌
                            disabledContainerColor = MaterialTheme.colorScheme.outlineVariant, // disabledContainerColor 값을 정해줌
                            disabledContentColor = Color.White // disabledContentColor 값을 정해줌
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp) // contentPadding 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = "수정 저장", // text 값을 정해줌
                            fontSize = 15.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                        )
                    }

                    Button( // 누를 수 있는 버튼을 만듦
                        onClick = onCancelEditClick, // onCancelEditClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                            containerColor = MaterialTheme.colorScheme.surfaceVariant, // containerColor 값을 정해줌
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant // contentColor 값을 정해줌
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp) // contentPadding 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        Text( // 화면에 글자를 보여줌
                            text = "수정 취소", // text 값을 정해줌
                            fontSize = 15.sp, // fontSize 값을 정해줌
                            fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
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
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityCommentSection( // CommunityCommentSection 함수를 선언함
    comments: List<CommunityComment>, // comments 값을 받음
    currentUserId: String, // currentUserId 값을 받음
    commentInput: String, // commentInput 값을 받음
    onCommentInputChange: (String) -> Unit, // onCommentInputChange 때 실행할 함수를 받음
    onAddCommentClick: () -> Unit, // onAddCommentClick 때 실행할 함수를 받음
    editingCommentId: String?, // editingCommentId 값을 받음
    editingCommentText: String, // editingCommentText 값을 받음
    onEditingCommentTextChange: (String) -> Unit, // onEditingCommentTextChange 때 실행할 함수를 받음
    onStartEditComment: (CommunityComment) -> Unit, // onStartEditComment 때 실행할 함수를 받음
    onCancelEditComment: () -> Unit, // onCancelEditComment 때 실행할 함수를 받음
    onSaveEditComment: (String) -> Unit, // onSaveEditComment 때 실행할 함수를 받음
    onDeleteComment: (String) -> Unit, // onDeleteComment 때 실행할 함수를 받음
    onReportComment: (String) -> Unit // onReportComment 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surface // containerColor 값을 정해줌
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "댓글", // text 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            if (comments.isEmpty()) { // 조건이 맞는지 확인함
                Text( // 화면에 글자를 보여줌
                    text = "아직 댓글이 없어요. 첫 댓글을 남겨보세요.", // text 값을 정해줌
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            } else { // 이 블록 안의 내용이 시작됨
                comments.forEach { comment ->
                    CommunityCommentItem( // Community Comment Item 함수를 실행함
                        comment = comment, // comment 값을 comment 값에 넣음
                        currentUserId = currentUserId, // currentUserId 값을 currentUserId 값에 넣음
                        isEditing = editingCommentId == comment.id, // isEditing인지 여부를 정해줌
                        editingCommentText = editingCommentText, // editingCommentText 값을 editingCommentText 값에 넣음
                        onEditingCommentTextChange = onEditingCommentTextChange, // onEditingCommentTextChange 때 실행할 함수를 onEditingCommentTextChange 때 실행할 함수에 넣음
                        onStartEdit = { // onStartEdit 때 실행할 함수를 정해줌
                            onStartEditComment(comment) // on Start Edit Comment 함수를 실행함
                        },
                        onCancelEdit = onCancelEditComment, // onCancelEditComment 때 실행할 함수를 onCancelEdit 때 실행할 함수에 넣음
                        onSaveEdit = { // onSaveEdit 때 실행할 함수를 정해줌
                            onSaveEditComment(comment.id) // 데이터를 저장하는 함수를 실행함
                        },
                        onDelete = { // onDelete 때 실행할 함수를 정해줌
                            onDeleteComment(comment.id) // 데이터를 지우는 함수를 실행함
                        },
                        onReport = { // onReport 때 실행할 함수를 정해줌
                            onReportComment(comment.id) // on Report Comment 함수를 실행함
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = commentInput, // commentInput 값을 입력값에 넣음
                onValueChange = onCommentInputChange, // onCommentInputChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "댓글을 입력하세요", // text 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                },
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f), // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                    focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                    focusedContainerColor = MaterialTheme.colorScheme.surface, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface // unfocusedContainerColor 값을 정해줌
                )
            )

            Button( // 누를 수 있는 버튼을 만듦
                onClick = onAddCommentClick, // onAddCommentClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                enabled = commentInput.isNotBlank(), // enabled 값을 정해줌
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // contentColor 값을 정해줌
                    disabledContainerColor = MaterialTheme.colorScheme.outlineVariant, // disabledContainerColor 값을 정해줌
                    disabledContentColor = Color.White // disabledContentColor 값을 정해줌
                ),
                contentPadding = PaddingValues(vertical = 14.dp) // contentPadding 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "댓글 등록", // text 값을 정해줌
                    fontSize = 15.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
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
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityCommentItem( // CommunityCommentItem 함수를 선언함
    comment: CommunityComment, // comment 값을 받음
    currentUserId: String, // currentUserId 값을 받음
    isEditing: Boolean, // isEditing인지 여부를 받음
    editingCommentText: String, // editingCommentText 값을 받음
    onEditingCommentTextChange: (String) -> Unit, // onEditingCommentTextChange 때 실행할 함수를 받음
    onStartEdit: () -> Unit, // onStartEdit 때 실행할 함수를 받음
    onCancelEdit: () -> Unit, // onCancelEdit 때 실행할 함수를 받음
    onSaveEdit: () -> Unit, // onSaveEdit 때 실행할 함수를 받음
    onDelete: () -> Unit, // onDelete 때 실행할 함수를 받음
    onReport: () -> Unit // onReport 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    // 이 댓글이 현재 사용자 댓글인지 판단합니다.
    val isMyComment = comment.authorId == currentUserId // 내가 쓴 댓글인지 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surfaceVariant // containerColor 값을 정해줌
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(14.dp), // UI 크기나 여백 같은 모양을 정함
            verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = comment.author, // text 값을 정해줌
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Row( // 안쪽 UI를 가로로 배치함
                    verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
                    horizontalArrangement = Arrangement.spacedBy(4.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = comment.timeText, // text 값을 정해줌
                        fontSize = 11.sp, // fontSize 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )

                    if (!isMyComment) { // 조건이 맞는지 확인함
                        IconButton( // 누를 수 있는 버튼을 만듦
                            onClick = onReport, // onReport 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                            modifier = Modifier.size(28.dp) // UI 크기나 여백 같은 모양을 정함
                        ) { // 이 블록 안의 내용이 시작됨
                            Icon( // 화면에 아이콘을 보여줌
                                painter = painterResource(id = R.drawable.ic_emergency_light), // painter 값을 정해줌
                                contentDescription = "댓글 신고", // contentDescription 값을 정해줌
                                modifier = Modifier.size(16.dp), // UI 크기나 여백 같은 모양을 정함
                                tint = MaterialTheme.colorScheme.error // tint 값을 정해줌
                            )
                        }
                    }
                }
            }

            if (!isEditing) { // 조건이 맞는지 확인함
                Text( // 화면에 글자를 보여줌
                    text = comment.content, // text 값을 정해줌
                    fontSize = 14.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                    lineHeight = 20.sp // lineHeight 값을 정해줌
                )

                // ------------------------------------------------
                // 내 댓글일 때만 수정/삭제 버튼을 보여줍니다.
                // 남의 댓글이면 이 영역 자체를 출력하지 않습니다.
                // ------------------------------------------------
                if (isMyComment) { // 조건이 맞는지 확인함
                    Row( // 안쪽 UI를 가로로 배치함
                        horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        TextButton( // 누를 수 있는 버튼을 만듦
                            onClick = onStartEdit // onStartEdit 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        ) { // 이 블록 안의 내용이 시작됨
                            Text(text = "댓글 수정") // 화면에 글자를 보여줌
                        }

                        TextButton( // 누를 수 있는 버튼을 만듦
                            onClick = onDelete // onDelete 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        ) { // 이 블록 안의 내용이 시작됨
                            Text(text = "댓글 삭제") // 화면에 글자를 보여줌
                        }
                    }
                }
            } else { // 이 블록 안의 내용이 시작됨
                // ------------------------------------------------
                // 수정 모드는 내 댓글일 때만 의미가 있습니다.
                // 혹시라도 잘못 들어왔을 경우를 막기 위해
                // 내 댓글인지 한 번 더 검사합니다.
                // ------------------------------------------------
                if (isMyComment) { // 조건이 맞는지 확인함
                    OutlinedTextField( // 사용자가 입력할 칸을 만듦
                        value = editingCommentText, // editingCommentText 값을 입력값에 넣음
                        onValueChange = onEditingCommentTextChange, // onEditingCommentTextChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
                        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                        shape = RoundedCornerShape(12.dp), // shape 값을 정해줌
                        colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                            focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f), // focusedBorderColor 값을 정해줌
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                            focusedContainerColor = MaterialTheme.colorScheme.surface, // focusedContainerColor 값을 정해줌
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface // unfocusedContainerColor 값을 정해줌
                        )
                    )

                    Row( // 안쪽 UI를 가로로 배치함
                        horizontalArrangement = Arrangement.spacedBy(10.dp) // horizontalArrangement 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        TextButton( // 누를 수 있는 버튼을 만듦
                            onClick = onSaveEdit, // onSaveEdit 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                            enabled = editingCommentText.isNotBlank() // enabled 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text(text = "수정 저장") // 화면에 글자를 보여줌
                        }

                        TextButton( // 누를 수 있는 버튼을 만듦
                            onClick = onCancelEdit // onCancelEdit 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                        ) { // 이 블록 안의 내용이 시작됨
                            Text(text = "수정 취소") // 화면에 글자를 보여줌
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
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailInfoChip( // CommunityDetailInfoChip 함수를 선언함
    text: String // text 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        shape = RoundedCornerShape(999.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.surfaceVariant // containerColor 값을 정해줌
        )
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp), // UI 크기나 여백 같은 모양을 정함
            fontSize = 12.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
        )
    }
}

private data class ReportTargetOption( // ReportTargetOption 데이터를 묶어둘 클래스 시작
    val type: String, // type 값을 저장함
    val id: String, // 아이디를 저장함
    val label: String // label 값을 저장함
)

private fun communityReportTargetsForPost(post: CommunityPost): List<ReportTargetOption> { // communityReportTargetsForPost 함수를 선언함
    // 웹뷰 신고 화면처럼 별도 신고 대상 선택 없이 게시글 자체를 신고합니다.
    return listOf( // 이 값을 함수 결과로 돌려줌
        ReportTargetOption("post", post.id, "게시글") // Report Target Option 함수를 실행함
    )
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
private fun CommunityReportDialog( // CommunityReportDialog 함수를 선언함
    targets: List<ReportTargetOption>, // targets 값을 받음
    onDismiss: () -> Unit, // 닫을 때 실행할 함수를 받음
    onReportClick: (String, String, String, String) -> Unit
) { // 이 블록 안의 내용이 시작됨
    var reason by remember { mutableStateOf("inappropriate") } // 화면에서 바뀔 reason 값을 저장함
    var detail by remember { mutableStateOf("") } // 화면에서 바뀔 detail 값을 저장함
    val selectedTarget = targets.firstOrNull() // 신고 대상 선택 없이 첫 번째 대상을 사용함
    val isDark = isCommunityDetailDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val dialogColor = communityDetailSoftCardColor() // 신고 다이얼로그 배경색을 정함
    val dialogBorderColor = communityDetailSoftCardBorderColor() // 신고 다이얼로그 테두리색을 정함
    val selectedColor = spentopiaAppButtonColor(isDark) // 선택 버튼 색을 모드별로 분리함
    val selectedContentColor = spentopiaAppButtonContentColor(isDark)
    val unselectedColor = if (isDark) Color(0xFF111827) else Color(0xFFEFF6FF) // 미선택 버튼 색을 모드별로 분리함
    val unselectedTextColor = if (isDark) Color(0xFFD8D6F5) else Color(0xFF1E3A8A) // 미선택 버튼 글자색을 모드별로 분리함
    val inputContainerColor = if (isDark) Color(0xFF111827) else Color(0xFFFFFFFF) // 입력창 배경색을 모드별로 분리함
    val inputBorderColor = if (isDark) Color(0xFF4C3B7A) else Color(0xFF93C5FD) // 입력창 테두리색을 모드별로 분리함
    val reasons = listOf( // reasons 값을 저장함
        "abuse" to "욕설/비방",
        "inappropriate" to "부적절한 내용",
        "spam" to "광고/도배",
        "other" to "기타"
    )

    androidx.compose.runtime.LaunchedEffect(targets) { // 화면이 열리거나 값이 바뀔 때 실행함
        reason = "inappropriate" // reason 값을 정해줌
        detail = "" // detail 값을 정해줌
    }

    AlertDialog( // 팝업 확인창을 보여줌
        onDismissRequest = onDismiss, // 닫을 때 실행할 함수를 onDismissRequest 때 실행할 함수에 넣음
        containerColor = dialogColor,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(text = "신고하기") }, // 화면에 글자를 보여줌
        text = { // text 값을 정해줌
            Column( // 안쪽 UI를 세로로 배치함
                verticalArrangement = Arrangement.spacedBy(12.dp), // verticalArrangement 값을 정해줌
                modifier = Modifier
                    .border(1.dp, dialogBorderColor, RoundedCornerShape(18.dp))
                    .padding(2.dp)
                    .verticalScroll(rememberScrollState()) // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "신고 사유를 입력해주세요.\n신고 내용은 운영자가 확인 후 처리합니다.",
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = "신고 사유", // text 값을 정해줌
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                FlowRow( // 안쪽 UI를 가로로 배치함
                    maxItemsInEachRow = 2, // 안쪽 UI를 가로로 배치함
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
                    verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    reasons.forEach { (value, label) ->
                        val selected = reason == value // selected 값을 저장함
                        Button( // 누를 수 있는 버튼을 만듦
                            onClick = { reason = value }, // 눌렀을 때 실행할 함수를 정해줌
                            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                            shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                            colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                                containerColor = if (selected) { // containerColor 값을 정해줌
                                    selectedColor
                                } else { // 이 블록 안의 내용이 시작됨
                                    unselectedColor
                                },
                                contentColor = if (selected) { // contentColor 값을 정해줌
                                    Color.White
                                } else { // 이 블록 안의 내용이 시작됨
                                    unselectedTextColor
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp) // contentPadding 값을 정해줌
                        ) { // 이 블록 안의 내용이 시작됨
                            Text( // 화면에 글자를 보여줌
                                text = label, // label 값을 text 값에 넣음
                                fontSize = 13.sp, // fontSize 값을 정해줌
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal // fontWeight 값을 정해줌
                            )
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { // 안쪽 UI를 세로로 배치함
                    Text( // 화면에 글자를 보여줌
                        text = "상세 내용", // text 값을 정해줌
                        fontSize = 13.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                    )

                    OutlinedTextField( // 사용자가 입력할 칸을 만듦
                        value = detail, // detail 값을 입력값에 넣음
                        onValueChange = { newValue -> // onValueChange 때 실행할 함수를 정해줌
                            detail = if (newValue.length <= 500) newValue else newValue.take(500) // detail 값을 정해줌
                        },
                        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                        placeholder = { // placeholder 값을 정해줌
                            Text(text = "신고 내용을 입력해주세요. 필수 입력입니다.") // 화면에 글자를 보여줌
                        },
                        minLines = 4, // minLines 값을 정해줌
                        maxLines = 6, // maxLines 값을 정해줌
                        shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                        colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                            focusedBorderColor = selectedColor, // focusedBorderColor 값을 정해줌
                            unfocusedBorderColor = inputBorderColor, // unfocusedBorderColor 값을 정해줌
                            focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                            focusedContainerColor = inputContainerColor, // focusedContainerColor 값을 정해줌
                            unfocusedContainerColor = inputContainerColor, // unfocusedContainerColor 값을 정해줌
                            cursorColor = selectedColor // cursorColor 값을 정해줌
                        )
                    )

                    Text( // 화면에 글자를 보여줌
                        text = "${detail.length}/500", // text 값을 정해줌
                        modifier = Modifier.align(Alignment.End), // UI 크기나 여백 같은 모양을 정함
                        fontSize = 11.sp, // fontSize 값을 정해줌
                        color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                    )
                }
            }
        },
        confirmButton = { // confirmButton 값을 정해줌
            val canSubmit = selectedTarget != null && detail.isNotBlank() // canSubmit 값을 저장함
            Button( // 누를 수 있는 버튼을 만듦
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    // 선택한 대상 + 사유 + 상세 내용이 모두 모이면
                    // 바깥 ViewModel로 신고 데이터를 전달합니다.
                    val target = selectedTarget ?: return@Button // target 값을 저장함
                    val trimmedDetail = detail.trim() // trimmedDetail 값을 저장함
                    if (trimmedDetail.isEmpty()) return@Button // 조건이 맞는지 확인함
                    onReportClick(target.type, target.id, reason, trimmedDetail) // on Report Click 함수를 실행함
                },
                enabled = canSubmit, // canSubmit 값을 enabled 값에 넣음
                shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                    containerColor = selectedColor, // selectedColor 값을 containerColor 값에 넣음
                    contentColor = selectedContentColor, // contentColor 값을 정해줌
                    disabledContainerColor = if (isDark) Color(0xFF2E3352) else Color(0xFFDBEAFE), // disabledContainerColor 값을 정해줌
                    disabledContentColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B) // disabledContentColor 값을 정해줌
                )
            ) { // 이 블록 안의 내용이 시작됨
                Text(text = "신고하기") // 화면에 글자를 보여줌
            }
        },
        dismissButton = { // dismissButton 값을 정해줌
            TextButton(onClick = onDismiss) { // 누를 수 있는 버튼을 만듦
                Text(text = "취소", color = unselectedTextColor) // 화면에 글자를 보여줌
            }
        }
    )
}

private fun copyCommunityPostLink(context: Context, postId: String) { // copyCommunityPostLink 함수를 선언함
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager // clipboard 값을 저장함
    val text = "spentopia://community/posts/$postId" // text 값을 저장함
    clipboard.setPrimaryClip(ClipData.newPlainText("커뮤니티 게시글 링크", text)) // 화면에 글자를 보여줌
    Toast.makeText(context, "링크가 복사되었습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
}

private data class CommunityDetailBadgeColors( // CommunityDetailBadgeColors 데이터를 묶어둘 클래스 시작
    val background: Color, // background 값을 저장함
    val content: Color // 내용을 저장함
)

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun communityCategoryBadgeColors(category: CommunityCategory): CommunityDetailBadgeColors { // communityCategoryBadgeColors 함수를 선언함
    val isDark = isCommunityDetailDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    return when (category) { // 이 값을 함수 결과로 돌려줌
        CommunityCategory.NOTICE -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF164E63), Color(0xFFBAE6FD)) // Community Detail Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF0284C7), Color.White) // Community Detail Badge Colors 함수를 실행함
        }
        CommunityCategory.AVATAR_CONTEST -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF713F12), Color(0xFFFEF3C7)) // Community Detail Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFFB45309), Color.White) // Community Detail Badge Colors 함수를 실행함
        }
        CommunityCategory.REQUEST -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF581C87), Color(0xFFE9D5FF)) // Community Detail Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF7E22CE), Color.White) // Community Detail Badge Colors 함수를 실행함
        }
        CommunityCategory.FREE_BOARD -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF064E3B), Color(0xFFA7F3D0)) // Community Detail Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityDetailBadgeColors(Color(0xFF059669), Color.White) // Community Detail Badge Colors 함수를 실행함
        }
    }
}

private fun CommunityCategory.badgeLabel(): String { // CommunityCategory 함수를 선언함
    return when (this) { // 이 값을 함수 결과로 돌려줌
        CommunityCategory.NOTICE -> "공지"
        CommunityCategory.AVATAR_CONTEST -> "콘테스트"
        CommunityCategory.REQUEST -> "아이템 요청"
        CommunityCategory.FREE_BOARD -> "자유"
    }
}

// ------------------------------------------------------------
// 프리뷰입니다.
// ------------------------------------------------------------
@Preview(showBackground = true) // 미리보기에서 화면을 볼 수 있게 표시함
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityDetailScreenPreview() { // CommunityDetailScreenPreview 함수를 선언함
    CommunityDetailScreen( // Community Detail Screen 함수를 실행함
        post = CommunityPost( // post 값을 정해줌
            id = "1", // 아이디를 정해줌
            title = "미리보기용 제목입니다", // 제목을 정해줌
            content = "미리보기용 짧은 내용입니다.", // 내용을 정해줌
            fullContent = "미리보기용 전체 내용입니다. 상세 화면에서는 전체 내용이 보이도록 구성했습니다.", // fullContent 값을 정해줌
            author = "미리보기작성자", // author 값을 정해줌
            timeText = "방금 전", // timeText 값을 정해줌
            likeCount = 3, // likeCount 값을 정해줌
            commentCount = 2, // commentCount 값을 정해줌
            tagText = "미리보기", // tagText 값을 정해줌
            category = CommunityCategory.FREE_BOARD, // 카테고리를 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "current_user", // authorId 값을 정해줌
                    author = "현재사용자", // author 값을 정해줌
                    content = "첫 번째 댓글입니다.", // 내용을 정해줌
                    timeText = "방금 전" // timeText 값을 정해줌
                ),
                CommunityComment( // Community Comment 함수를 실행함
                    id = "2", // 아이디를 정해줌
                    authorId = "user_x", // authorId 값을 정해줌
                    author = "다른사용자", // author 값을 정해줌
                    content = "두 번째 댓글입니다.", // 내용을 정해줌
                    timeText = "1분 전" // timeText 값을 정해줌
                )
            ),
            isLiked = true // true 값을 isLiked인지 여부에 넣음
        )
    )
}
