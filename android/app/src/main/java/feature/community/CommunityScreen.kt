package com.ict.spentopia.feature.community

import androidx.activity.compose.BackHandler
// ------------------------------------------------------------
// CommunityScreen.kt
// ------------------------------------------------------------
// 이 파일은 커뮤니티 메인 화면 전체 UI를 담당합니다.
//
// 이번 버전에서 바뀐 점:
// 1. CommunityComment에 authorId를 추가했습니다.
// 2. authorId는 "댓글 작성자의 고유 식별값" 역할을 합니다.
// 3. 나중에 로그인 기능이 붙으면 실제 사용자 id로 바꿔서 쓸 수 있습니다.
// 4. 현재는 임시 문자열로만 구분합니다.
// ------------------------------------------------------------

// Compose Foundation 관련 import입니다.
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape

// Material3 관련 import입니다.
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton

// Compose 상태 관련 import입니다.
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

// UI 스타일 관련 import입니다.
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple

// ------------------------------------------------------------
// 커뮤니티 카테고리 enum 클래스입니다.
// ------------------------------------------------------------
// enum을 사용하면 카테고리를 안전하게 고정된 값으로 관리할 수 있습니다.
// ------------------------------------------------------------
enum class CommunityCategory(
    val label: String
) {
    NOTICE("공지사항"),
    AVATAR_CONTEST("아바타 콘테스트"),
    REQUEST("이거 만들어주세요"),
    FREE_BOARD("자유")
}

private enum class CommunitySortOption(
    val label: String
) {
    LATEST("최신순"),
    RECOMMENDED("추천순"),
    VIEW("조회순")
}

// ------------------------------------------------------------
// 댓글 1개를 표현하는 모델입니다.
// ------------------------------------------------------------
// authorId:
// - 댓글 작성자를 구분하기 위한 고유 문자열입니다.
// - 지금은 임시로 사용하지만,
//   나중에 로그인 기능이 연결되면 실제 사용자 id로 바꿀 수 있습니다.
//
// author:
// - 화면에 표시할 작성자 이름입니다.
//
// content:
// - 댓글 본문입니다.
//
// timeText:
// - "방금 전", "1시간 전" 같은 표시용 문자열입니다.
// ------------------------------------------------------------
data class CommunityComment(
    val id: String,         // 댓글 고유 ID입니다.
    val authorId: String,   // 댓글 작성자 고유 식별값입니다.
    val author: String,     // 댓글 작성자 이름입니다.
    val content: String,    // 댓글 내용입니다.
    val timeText: String    // 댓글 작성 시간 표시 문자열입니다.
)

// ------------------------------------------------------------
// 커뮤니티 게시글 UI 모델입니다.
// ------------------------------------------------------------
// comments:
// - 게시글에 달린 댓글 목록입니다.
//
// isLiked:
// - 현재 사용자가 이 게시글에 좋아요를 눌렀는지 여부입니다.
// ------------------------------------------------------------
data class CommunityPost(
    val id: String,                     // 게시글 ID입니다.
    val title: String,                  // 게시글 제목입니다.
    val content: String,                // 목록에서 보여줄 짧은 미리보기 내용입니다.
    val fullContent: String,            // 상세 화면에서 보여줄 전체 내용입니다.
    val authorId: String = "",          // 작성자 고유 ID입니다.
    val author: String,                 // 작성자 이름입니다.
    val timeText: String,               // 시간 표시 문자열입니다.
    val likeCount: Int,                 // 좋아요 개수입니다.
    val commentCount: Int,              // 댓글 개수입니다.
    val tagText: String,                // 하단 왼쪽 태그 텍스트입니다.
    val category: CommunityCategory,    // 카테고리입니다.
    val viewCount: Int = 0,             // 조회수입니다.
    val detailDateText: String = "",    // 상세 화면 날짜/시간 표시입니다.
    val comments: List<CommunityComment> = emptyList(), // 댓글 목록입니다.
    val isLiked: Boolean = false,       // 현재 사용자의 좋아요 여부입니다.
    val imageUrl: String? = null        // 첨부 이미지 경로 또는 URL입니다.
)

// ------------------------------------------------------------
// CommunityScreen 메인 Composable입니다.
// ------------------------------------------------------------
// posts:
// - 외부(AppNavGraph)에서 전달받은 게시글 목록입니다.
//
// onWriteClick:
// - 글쓰기 버튼 클릭 시 실행됩니다.
//
// onPostClick:
// - 게시글 카드 클릭 시 실행됩니다.
// ------------------------------------------------------------
@Composable
fun CommunityScreen(
    posts: List<CommunityPost>,
    contests: List<CommunityContest> = emptyList(),
    selectedPost: CommunityPost? = null,
    currentUserId: String = "",
    currentUserRole: String = "user",
    isLoading: Boolean = false,
    errorMessage: String? = null,
    onRetryClick: () -> Unit = {},
    onWriteClick: () -> Unit = {},
    onContestWriteClick: (String?) -> Unit = {},
    onPostClick: (CommunityPost) -> Unit = {},
    onCloseDetailClick: () -> Unit = {},
    onUpdatePostClick: (CommunityPost) -> Unit = {},
    onDeletePostClick: (String) -> Unit = {},
    onToggleLikeClick: (String) -> Unit = {},
    onAddCommentClick: (String, String) -> Unit = { _, _ -> },
    onUpdateCommentClick: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteCommentClick: (String, String) -> Unit = { _, _ -> },
    onReportClick: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedSortOption by remember { mutableStateOf(CommunitySortOption.LATEST) }
    var currentPage by remember { mutableIntStateOf(1) }
    val pageSize = 10

    val categoryTabs = remember {
        listOf<Pair<String, CommunityCategory?>>(
            "전체" to null,
            CommunityCategory.NOTICE.label to CommunityCategory.NOTICE,
            CommunityCategory.AVATAR_CONTEST.label to CommunityCategory.AVATAR_CONTEST,
            CommunityCategory.REQUEST.label to CommunityCategory.REQUEST,
            CommunityCategory.FREE_BOARD.label to CommunityCategory.FREE_BOARD
        )
    }

    val selectedCategory = categoryTabs[selectedCategoryIndex].second
    val activeContest = remember(contests) {
        contests.firstOrNull { it.status == "active" }
            ?: contests.firstOrNull { it.title == "5월 아바타 콘테스트" }
            ?: contests.firstOrNull()
    }

    val filteredPosts = remember(searchQuery, selectedCategory, selectedSortOption, posts) {
        val normalizedQuery = searchQuery.trim()
        posts
            .asSequence()
            .filter { post ->
                selectedCategory == null || post.category == selectedCategory
            }
            .filter { post ->
                normalizedQuery.isBlank() || post.title.contains(normalizedQuery, ignoreCase = true)
            }
            .let { sequence ->
                when (selectedSortOption) {
                    CommunitySortOption.LATEST -> sequence.sortedWith(
                        compareBy<CommunityPost> { communityRecencyRank(it.timeText) }
                            .thenByDescending { it.timeText }
                    )
                    CommunitySortOption.RECOMMENDED -> sequence.sortedWith(
                        compareByDescending<CommunityPost> { it.likeCount }
                            .thenByDescending { it.timeText }
                    )
                    CommunitySortOption.VIEW -> sequence.sortedWith(
                        compareByDescending<CommunityPost> { it.viewCount }
                            .thenByDescending { it.timeText }
                    )
                }
            }
            .toList()
    }
    val totalPages = ((filteredPosts.size + pageSize - 1) / pageSize).coerceAtLeast(1)
    val safeCurrentPage = currentPage.coerceIn(1, totalPages)
    val pagedPosts = filteredPosts
        .drop((safeCurrentPage - 1) * pageSize)
        .take(pageSize)

    LaunchedEffect(searchQuery, selectedCategoryIndex, selectedSortOption, posts.size) {
        currentPage = 1
    }

    LaunchedEffect(totalPages) {
        if (currentPage > totalPages) {
            currentPage = totalPages
        }
    }

    if (selectedPost != null) {
        BackHandler {
            onCloseDetailClick()
        }

        CommunityDetailScreen(
            post = selectedPost,
            currentUserId = currentUserId,
            currentUserRole = currentUserRole,
            onBackClick = onCloseDetailClick,
            onUpdateClick = onUpdatePostClick,
            onDeleteClick = onDeletePostClick,
            onToggleLikeClick = onToggleLikeClick,
            onAddCommentClick = onAddCommentClick,
            onUpdateCommentClick = onUpdateCommentClick,
            onDeleteCommentClick = onDeleteCommentClick,
            onReportClick = onReportClick
        )
        return
    }

    // 전체 화면을 세로 스크롤 가능한 LazyColumn으로 구성합니다.
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 20.dp,
            bottom = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 상단 커뮤니티 제목/설명/글쓰기 버튼 영역입니다.
        item {
            CommunityTopHeader(
                onWriteClick = onWriteClick
            )
        }

        item {
            CommunitySearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        item {
            CommunityContestBannerCard(
                contest = activeContest,
                onViewPostsClick = {
                    selectedCategoryIndex = categoryTabs.indexOfFirst {
                        it.second == CommunityCategory.AVATAR_CONTEST
                    }.coerceAtLeast(0)
                },
                onWriteClick = {
                    onContestWriteClick(activeContest?.id)
                }
            )
        }

        item {
            CommunityCategoryChipRow(
                categoryTabs = categoryTabs,
                selectedCategoryIndex = selectedCategoryIndex,
                onCategorySelected = { clickedIndex ->
                    selectedCategoryIndex = clickedIndex
                }
            )
        }

        item {
            CommunitySortOptionRow(
                selectedSortOption = selectedSortOption,
                onSortOptionSelected = { selectedSortOption = it }
            )
        }

        if (isLoading) {
            item {
                CommunityLoadingCard()
            }
        }

        if (errorMessage != null) {
            item {
                CommunityErrorCard(
                    message = errorMessage,
                    onRetryClick = onRetryClick
                )
            }
        }

        // 현재 카테고리에 게시글이 하나도 없으면 안내 카드를 보여줍니다.
        if (!isLoading && filteredPosts.isEmpty()) {
            item {
                EmptyPostCard()
            }
        }

        // 게시글 목록을 카드 형태로 렌더링합니다.
        items(
            items = pagedPosts,
            key = { post -> post.id }
        ) { post ->
            CommunityPostCard(
                post = post,
                onClick = {
                    onPostClick(post)
                }
            )
        }

        if (!isLoading && filteredPosts.size > pageSize) {
            item {
                CommunityPaginationRow(
                    currentPage = safeCurrentPage,
                    totalPages = totalPages,
                    onPageSelected = { page ->
                        currentPage = page
                    }
                )
            }
        }
    }
}

@Composable
private fun CommunityLoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "게시글을 불러오는 중입니다.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CommunityErrorCard(
    message: String,
    onRetryClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRetryClick) {
                Text(text = "다시 시도")
            }
        }
    }
}

// ------------------------------------------------------------
// 게시글이 없을 때 보여줄 카드입니다.
// ------------------------------------------------------------
@Composable
private fun EmptyPostCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "아직 게시글이 없어요",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "첫 번째 글을 작성해서 커뮤니티를 시작해보세요.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )
        }
    }
}

// ------------------------------------------------------------
// 상단 헤더 영역입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityTopHeader(
    onWriteClick: () -> Unit
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
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "다른 사용자들과 소통하고 경험을 나눠보세요",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Button(
            onClick = onWriteClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = "글쓰기",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun CommunityContestBannerCard(
    contest: CommunityContest?,
    onViewPostsClick: () -> Unit,
    onWriteClick: () -> Unit
) {
    val statusText = "진행중"
    val title = "5월 아바타 콘테스트"
    val period = "2026.05.09 ~ 2026.05.31"
    val reward = "1등 기분좋음"
    val description = "아바타 콘테스트를 개최합니다 ~! 많관부"

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier
                        .background(Color(0xFF12B981), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "아바타 콘테스트",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                fontSize = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = period,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "보상: $reward",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = description,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewPostsClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SpentopiaMutedPurple,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "참가글 보기",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onWriteClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0F172A),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    Text(
                        text = "참가글 작성",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunitySearchField(
    query: String,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "검색",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        placeholder = {
            Text(
                text = "제목 검색",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.65f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = SpentopiaMutedPurple
        )
    )
}

// ------------------------------------------------------------
// 카테고리 칩 Row입니다.
// ------------------------------------------------------------
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun CommunityCategoryChipRow(
    categoryTabs: List<Pair<String, CommunityCategory?>>,
    selectedCategoryIndex: Int,
    onCategorySelected: (Int) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categoryTabs.forEachIndexed { index, _ ->
            val tab = categoryTabs[index]
            val isSelected = index == selectedCategoryIndex

            Box(
                modifier = Modifier
                    .widthIn(min = 80.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable {
                        onCategorySelected(index)
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = tab.first,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun CommunitySortOptionRow(
    selectedSortOption: CommunitySortOption,
    onSortOptionSelected: (CommunitySortOption) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = CommunitySortOption.entries,
            key = { option -> option.label }
        ) { option ->
            val isSelected = option == selectedSortOption
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { onSortOptionSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (isSelected) "✓ ${option.label}" else option.label,
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

// ------------------------------------------------------------
// 게시글 카드 UI입니다.
// ------------------------------------------------------------
@Composable
private fun CommunityPostCard(
    post: CommunityPost,
    onClick: () -> Unit
) {
    val badgeColors = communityCategoryBadgeColors(post.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onClick()
            },
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
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeColors.background)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = post.category.badgeLabel(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = badgeColors.content
                    )
                }

                Text(
                    text = post.timeText,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = post.content,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = post.tagText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SmallCountChip(
                        text = "좋아요 ${post.likeCount}"
                    )

                    SmallCountChip(
                        text = "댓글 ${post.commentCount}"
                    )

                    SmallCountChip(
                        text = "조회 ${post.viewCount}"
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityPaginationRow(
    currentPage: Int,
    totalPages: Int,
    onPageSelected: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(
            items = (1..totalPages).toList(),
            key = { page -> page }
        ) { page ->
            val isSelected = page == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) SpentopiaMutedPurple
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(
                        width = 1.dp,
                        color = if (isSelected) SpentopiaMutedPurple else MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onPageSelected(page) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = page.toString(),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private data class CommunityBadgeColors(
    val background: Color,
    val content: Color
)

@Composable
private fun communityCategoryBadgeColors(category: CommunityCategory): CommunityBadgeColors {
    val isDark = isSystemInDarkTheme()
    return when (category) {
        CommunityCategory.NOTICE -> if (isDark) {
            CommunityBadgeColors(Color(0xFF164E63), Color(0xFFBAE6FD))
        } else {
            CommunityBadgeColors(Color(0xFF0284C7), Color.White)
        }
        CommunityCategory.AVATAR_CONTEST -> if (isDark) {
            CommunityBadgeColors(Color(0xFF713F12), Color(0xFFFEF3C7))
        } else {
            CommunityBadgeColors(Color(0xFFB45309), Color.White)
        }
        CommunityCategory.REQUEST -> if (isDark) {
            CommunityBadgeColors(Color(0xFF581C87), Color(0xFFE9D5FF))
        } else {
            CommunityBadgeColors(Color(0xFF7E22CE), Color.White)
        }
        CommunityCategory.FREE_BOARD -> if (isDark) {
            CommunityBadgeColors(Color(0xFF064E3B), Color(0xFFA7F3D0))
        } else {
            CommunityBadgeColors(Color(0xFF059669), Color.White)
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
// 좋아요 / 댓글 개수용 작은 칩 UI입니다.
// ------------------------------------------------------------
@Composable
private fun SmallCountChip(
    text: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ------------------------------------------------------------
// 기본 더미 게시글 목록을 반환하는 함수입니다.
// ------------------------------------------------------------
// 중요:
// - authorId를 일부는 "current_user"로,
//   일부는 다른 값으로 넣어두었습니다.
// - 이렇게 해야 "내 댓글만 수정/삭제 가능"한지 테스트할 수 있습니다.
// ------------------------------------------------------------
fun getInitialCommunityPosts(): List<CommunityPost> {
    return listOf(
        CommunityPost(
            id = "7",
            title = "커뮤니티 이용 안내",
            content = "서로에게 도움이 되는 소비 기록, 아바타, 아이디어 이야기를 편하게 나눠주세요.",
            fullContent = "서로에게 도움이 되는 소비 기록, 아바타, 아이디어 이야기를 편하게 나눠주세요. 비방이나 개인정보가 포함된 글은 예고 없이 삭제될 수 있습니다.",
            author = "Spentopia",
            timeText = "방금 전",
            likeCount = 3,
            commentCount = 0,
            tagText = "공지",
            category = CommunityCategory.NOTICE,
            viewCount = 312,
            comments = emptyList(),
            isLiked = false
        ),
        CommunityPost(
            id = "1",
            title = "이번 달 아바타 7일 연속 기록 성공했어요!",
            content = "작은 금액은 놓칠 때도 있었지만, 그래도 소비 패턴이 조금씩 보이기 시작해서 뿌듯해요.",
            fullContent = "작은 금액은 놓칠 때도 있었지만, 그래도 소비 패턴이 조금씩 보이기 시작해서 뿌듯해요. 처음에는 귀찮았는데 습관이 생기니까 훨씬 편해졌어요.",
            author = "기록요정",
            timeText = "1시간 전",
            likeCount = 8,
            commentCount = 2,
            tagText = "기록초보",
            category = CommunityCategory.AVATAR_CONTEST,
            viewCount = 128,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "user_a",
                    author = "절약메이트",
                    content = "와 7일 연속이면 진짜 대단해요!",
                    timeText = "50분 전"
                ),
                CommunityComment(
                    id = "2",
                    authorId = "current_user",
                    author = "현재사용자",
                    content = "저도 이번 달에는 꾸준히 기록해보려고요.",
                    timeText = "30분 전"
                )
            ),
            isLiked = false
        ),
        CommunityPost(
            id = "2",
            title = "아바타 꾸미기 보상 받으려면 어떤 미션부터 하는 게 좋을까요?",
            content = "출석이랑 소비기록 중에서 어떤 걸 먼저 챙기는 게 효율적인지 궁금해요.",
            fullContent = "출석이랑 소비기록 중에서 어떤 걸 먼저 챙기는 게 효율적인지 궁금해요. 시작 단계라 어떤 순서가 좋은지 잘 모르겠어요.",
            author = "코디초보",
            timeText = "3시간 전",
            likeCount = 5,
            commentCount = 1,
            tagText = "미션질문",
            category = CommunityCategory.AVATAR_CONTEST,
            viewCount = 94,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "user_b",
                    author = "보상수집가",
                    content = "저는 출석부터 챙기고 기록 습관을 붙였어요.",
                    timeText = "2시간 전"
                )
            ),
            isLiked = false
        ),
        CommunityPost(
            id = "3",
            title = "주말 지출이 평일보다 두 배인 이유를 찾았어요",
            content = "모임, 카페, 충동구매가 한 번에 몰려 있더라고요. 이번 주부터는 주말 예산을 따로 잡아보려 합니다.",
            fullContent = "모임, 카페, 충동구매가 한 번에 몰려 있더라고요. 그래서 이번 주부터는 주말 예산을 따로 잡아보려고 합니다. 평일보다 지출이 커지는 이유가 확실히 보였어요.",
            author = "토요소비왕",
            timeText = "5시간 전",
            likeCount = 11,
            commentCount = 2,
            tagText = "분석해보는중",
            category = CommunityCategory.FREE_BOARD,
            viewCount = 216,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "current_user",
                    author = "현재사용자",
                    content = "주말 예산 따로 잡는 방법 괜찮네요.",
                    timeText = "4시간 전"
                ),
                CommunityComment(
                    id = "2",
                    authorId = "user_c",
                    author = "카페중독탈출",
                    content = "저도 모임비 때문에 주말이 항상 문제였어요.",
                    timeText = "3시간 전"
                )
            ),
            isLiked = true
        ),
        CommunityPost(
            id = "4",
            title = "가계부 쓰다 보니 생각보다 배달비가 너무 크네요",
            content = "한 번 주문할 때는 얼마 안 되는 것 같았는데, 한 달 합계를 보니까 꽤 부담이 되더라고요.",
            fullContent = "한 번 주문할 때는 얼마 안 되는 것 같았는데, 한 달 합계를 보니까 꽤 부담이 되더라고요. 이번 달부터는 주 1회만 배달을 허용해보려 합니다.",
            author = "배달줄이기중",
            timeText = "7시간 전",
            likeCount = 6,
            commentCount = 1,
            tagText = "배달줄이기",
            category = CommunityCategory.FREE_BOARD,
            viewCount = 173,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "user_d",
                    author = "식비절약러",
                    content = "배달앱 삭제하고 확실히 줄었어요.",
                    timeText = "6시간 전"
                )
            ),
            isLiked = false
        ),
        CommunityPost(
            id = "5",
            title = "카페 지출 줄이려면 예산을 먼저 따로 빼두는 게 좋더라고요",
            content = "저는 아예 주간 간식비를 따로 정해두니까 훨씬 덜 흔들렸어요. 생각보다 효과가 꽤 컸습니다.",
            fullContent = "저는 아예 주간 간식비를 따로 정해두니까 훨씬 덜 흔들렸어요. 그냥 아껴야지 하는 것보다 실제 숫자를 정하는 게 더 효과적이었어요.",
            author = "절약실험러",
            timeText = "2시간 전",
            likeCount = 14,
            commentCount = 2,
            tagText = "절약실험중",
            category = CommunityCategory.REQUEST,
            viewCount = 241,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "user_e",
                    author = "예산지킴이",
                    content = "숫자로 정하는 게 진짜 중요한 것 같아요.",
                    timeText = "1시간 전"
                ),
                CommunityComment(
                    id = "2",
                    authorId = "current_user",
                    author = "현재사용자",
                    content = "저도 이번 주부터 따라해보겠습니다.",
                    timeText = "40분 전"
                )
            ),
            isLiked = true
        ),
        CommunityPost(
            id = "6",
            title = "장보기 전에 냉장고 사진 찍는 습관이 은근 도움 됩니다",
            content = "이미 있는 재료를 또 사는 일이 줄어들어서 식비를 아끼는 데 꽤 효과가 있었어요.",
            fullContent = "이미 있는 재료를 또 사는 일이 줄어들어서 식비를 아끼는 데 꽤 효과가 있었어요. 특히 퇴근 후 급하게 장볼 때 중복 구매가 줄더라고요.",
            author = "장보기고수",
            timeText = "8시간 전",
            likeCount = 9,
            commentCount = 1,
            tagText = "식비절약",
            category = CommunityCategory.REQUEST,
            viewCount = 187,
            comments = listOf(
                CommunityComment(
                    id = "1",
                    authorId = "user_f",
                    author = "냉장고정리왕",
                    content = "이 팁 좋네요. 저도 바로 써먹어볼게요.",
                    timeText = "7시간 전"
                )
            ),
            isLiked = false
        )
    )
}

private fun communityRecencyRank(timeText: String): Int {
    if (timeText.contains("방금")) return 0

    val number = Regex("""\d+""")
        .find(timeText)
        ?.value
        ?.toIntOrNull()
        ?: return Int.MAX_VALUE

    return when {
        timeText.contains("분") -> number
        timeText.contains("시간") -> number * 60
        timeText.contains("일") -> number * 24 * 60
        else -> Int.MAX_VALUE
    }
}

// ------------------------------------------------------------
// 프리뷰입니다.
// ------------------------------------------------------------
@Preview(showBackground = true)
@Composable
private fun CommunityScreenPreview() {
    CommunityScreen(
        posts = getInitialCommunityPosts()
    )
}
