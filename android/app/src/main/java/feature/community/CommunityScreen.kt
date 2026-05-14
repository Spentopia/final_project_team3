package com.ict.spentopia.feature.community // 이 파일이 속한 패키지 위치를 적음

import androidx.activity.compose.BackHandler // BackHandler 기능을 가져옴
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
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
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
import androidx.compose.foundation.lazy.LazyColumn // 세로 스크롤 목록을 가져옴
import androidx.compose.foundation.lazy.LazyRow // LazyRow 기능을 가져옴
import androidx.compose.foundation.lazy.items // items 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴

// Material3 관련 import입니다.
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.CircularProgressIndicator // CircularProgressIndicator 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.Search // Search 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴

// Compose 상태 관련 import입니다.
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableIntStateOf // mutableIntStateOf 기능을 가져옴
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌

// UI 스타일 관련 import입니다.
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.clip // clip 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.style.TextOverflow // TextOverflow 기능을 가져옴
import androidx.compose.ui.tooling.preview.Preview // Preview 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground // 앱 다크모드 배경색을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴

// ------------------------------------------------------------
// 커뮤니티 카테고리 enum 클래스입니다.
// ------------------------------------------------------------
// enum을 사용하면 카테고리를 안전하게 고정된 값으로 관리할 수 있습니다.
// ------------------------------------------------------------
enum class CommunityCategory( // CommunityCategory에서 고를 수 있는 값들을 묶음
    val label: String // label 값을 저장함
) { // 이 블록 안의 내용이 시작됨
    NOTICE("공지사항"), // NOTICE 함수를 실행함
    AVATAR_CONTEST("아바타 콘테스트"), // AVATAR CONTEST 함수를 실행함
    REQUEST("이거 만들어주세요"), // REQUEST 함수를 실행함
    FREE_BOARD("자유") // FREE BOARD 함수를 실행함
}

private enum class CommunitySortOption( // CommunitySortOption에서 고를 수 있는 값들을 묶음
    val label: String // label 값을 저장함
) { // 이 블록 안의 내용이 시작됨
    LATEST("최신순"), // LATEST 함수를 실행함
    RECOMMENDED("추천순"), // RECOMMENDED 함수를 실행함
    VIEW("조회순") // VIEW 함수를 실행함
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
data class CommunityComment( // CommunityComment 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val authorId: String, // authorId 값을 저장함
    val author: String, // author 값을 저장함
    val content: String, // 내용을 저장함
    val timeText: String // timeText 값을 저장함
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
data class CommunityPost( // CommunityPost 데이터를 묶어둘 클래스 시작
    val id: String, // 아이디를 저장함
    val title: String, // 제목을 저장함
    val content: String, // 내용을 저장함
    val fullContent: String, // fullContent 값을 저장함
    val authorId: String = "", // authorId 값을 저장함
    val author: String, // author 값을 저장함
    val timeText: String, // timeText 값을 저장함
    val likeCount: Int, // likeCount 값을 저장함
    val commentCount: Int, // commentCount 값을 저장함
    val tagText: String, // tagText 값을 저장함
    val category: CommunityCategory, // 카테고리을 저장함
    val viewCount: Int = 0, // viewCount 값을 저장함
    val detailDateText: String = "", // detailDateText 값을 저장함
    val comments: List<CommunityComment> = emptyList(), // comments 값을 저장함
    val isLiked: Boolean = false, // 좋아요를 눌렀는지 저장함
    val imageUrl: String? = null // imageUrl 값을 저장함
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
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun CommunityScreen( // CommunityScreen 함수를 선언함
    posts: List<CommunityPost>, // posts 값을 받음
    contests: List<CommunityContest> = emptyList(), // contests 값을 받음
    selectedPost: CommunityPost? = null, // selectedPost 값을 받음
    currentUserId: String = "", // currentUserId 값을 받음
    currentUserRole: String = "user", // currentUserRole 값을 받음
    isLoading: Boolean = false, // 로딩 여부를 받음
    errorMessage: String? = null, // 오류 내용을 받음
    onRetryClick: () -> Unit = {}, // onRetryClick 때 실행할 함수를 받음
    onWriteClick: () -> Unit = {}, // onWriteClick 때 실행할 함수를 받음
    onContestWriteClick: (String?) -> Unit = {}, // onContestWriteClick 때 실행할 함수를 받음
    onPostClick: (CommunityPost) -> Unit = {}, // onPostClick 때 실행할 함수를 받음
    onCloseDetailClick: () -> Unit = {}, // onCloseDetailClick 때 실행할 함수를 받음
    onUpdatePostClick: (CommunityPost) -> Unit = {}, // onUpdatePostClick 때 실행할 함수를 받음
    onDeletePostClick: (String) -> Unit = {}, // onDeletePostClick 때 실행할 함수를 받음
    onToggleLikeClick: (String) -> Unit = {}, // onToggleLikeClick 때 실행할 함수를 받음
    onAddCommentClick: (String, String) -> Unit = { _, _ -> }, // Unit 값을 정해줌
    onUpdateCommentClick: (String, String, String) -> Unit = { _, _, _ -> }, // Unit 값을 정해줌
    onDeleteCommentClick: (String, String) -> Unit = { _, _ -> }, // Unit 값을 정해줌
    onReportClick: (String, String, String, String) -> Unit = { _, _, _, _ -> } // Unit 값을 정해줌
) { // 이 블록 안의 내용이 시작됨
    var searchQuery by remember { mutableStateOf("") } // 화면에서 바뀔 searchQuery 값을 저장함
    var selectedCategoryIndex by remember { mutableIntStateOf(0) } // 화면이 다시 그려져도 selectedCategoryIndex 값을 기억함
    var selectedSortOption by remember { mutableStateOf(CommunitySortOption.LATEST) } // 화면에서 바뀔 selectedSortOption 값을 저장함
    var currentPage by remember { mutableIntStateOf(1) } // 화면이 다시 그려져도 currentPage 값을 기억함
    val pageSize = 10 // pageSize 값을 저장함

    val categoryTabs = remember { // 화면이 다시 그려져도 categoryTabs 값을 기억함
        listOf<Pair<String, CommunityCategory?>>(
            "전체" to null,
            CommunityCategory.NOTICE.label to CommunityCategory.NOTICE,
            CommunityCategory.AVATAR_CONTEST.label to CommunityCategory.AVATAR_CONTEST,
            CommunityCategory.REQUEST.label to CommunityCategory.REQUEST,
            CommunityCategory.FREE_BOARD.label to CommunityCategory.FREE_BOARD
        )
    }

    val selectedCategory = categoryTabs[selectedCategoryIndex].second // selectedCategory 값을 저장함
    val activeContest = remember(contests) { // 화면이 다시 그려져도 activeContest 값을 기억함
        contests.firstOrNull { it.status == "active" } // it.status 값을 정해줌
            ?: contests.firstOrNull { it.title == "5월 아바타 콘테스트" } // it.title 값을 정해줌
            ?: contests.firstOrNull()
    }

    val filteredPosts = remember(searchQuery, selectedCategory, selectedSortOption, posts) { // 화면이 다시 그려져도 filteredPosts 값을 기억함
        val normalizedQuery = searchQuery.trim() // normalizedQuery 값을 저장함
        posts
            .asSequence()
            .filter { post ->
                selectedCategory == null || post.category == selectedCategory // selectedCategory 값을 정해줌
            }
            .filter { post ->
                normalizedQuery.isBlank() || post.title.contains(normalizedQuery, ignoreCase = true) // ignoreCase 값을 정해줌
            }
            .let { sequence ->
                when (selectedSortOption) { // 값 종류에 따라 실행할 코드를 나눔
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
    val totalPages = ((filteredPosts.size + pageSize - 1) / pageSize).coerceAtLeast(1) // totalPages 값을 저장함
    val safeCurrentPage = currentPage.coerceIn(1, totalPages) // safeCurrentPage 값을 저장함
    val pagedPosts = filteredPosts // pagedPosts 값을 저장함
        .drop((safeCurrentPage - 1) * pageSize)
        .take(pageSize)

    LaunchedEffect(searchQuery, selectedCategoryIndex, selectedSortOption, posts.size) { // 화면이 열리거나 값이 바뀔 때 실행함
        currentPage = 1 // currentPage 값을 정해줌
    }

    LaunchedEffect(totalPages) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (currentPage > totalPages) { // 조건이 맞는지 확인함
            currentPage = totalPages // totalPages 값을 currentPage 값에 넣음
        }
    }

    if (selectedPost != null) { // 조건이 맞는지 확인함
        BackHandler { // 이 블록 안의 내용이 시작됨
            onCloseDetailClick() // on Close Detail Click 함수를 실행함
        }

        CommunityDetailScreen( // Community Detail Screen 함수를 실행함
            post = selectedPost, // selectedPost 값을 post 값에 넣음
            currentUserId = currentUserId, // currentUserId 값을 currentUserId 값에 넣음
            currentUserRole = currentUserRole, // currentUserRole 값을 currentUserRole 값에 넣음
            onBackClick = onCloseDetailClick, // onCloseDetailClick 때 실행할 함수를 onBackClick 때 실행할 함수에 넣음
            onUpdateClick = onUpdatePostClick, // onUpdatePostClick 때 실행할 함수를 onUpdateClick 때 실행할 함수에 넣음
            onDeleteClick = onDeletePostClick, // onDeletePostClick 때 실행할 함수를 onDeleteClick 때 실행할 함수에 넣음
            onToggleLikeClick = onToggleLikeClick, // onToggleLikeClick 때 실행할 함수를 onToggleLikeClick 때 실행할 함수에 넣음
            onAddCommentClick = onAddCommentClick, // onAddCommentClick 때 실행할 함수를 onAddCommentClick 때 실행할 함수에 넣음
            onUpdateCommentClick = onUpdateCommentClick, // onUpdateCommentClick 때 실행할 함수를 onUpdateCommentClick 때 실행할 함수에 넣음
            onDeleteCommentClick = onDeleteCommentClick, // onDeleteCommentClick 때 실행할 함수를 onDeleteCommentClick 때 실행할 함수에 넣음
            onReportClick = onReportClick // onReportClick 때 실행할 함수를 onReportClick 때 실행할 함수에 넣음
        )
        return
    }

    // 전체 화면을 세로 스크롤 가능한 LazyColumn으로 구성합니다.
    LazyColumn( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues( // contentPadding 값을 정해줌
            start = 16.dp, // start 값을 정해줌
            end = 16.dp, // end 값을 정해줌
            top = 20.dp, // top 값을 정해줌
            bottom = 24.dp // bottom 값을 정해줌
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        // 상단 커뮤니티 제목/설명/글쓰기 버튼 영역입니다.
        item { // 이 블록 안의 내용이 시작됨
            CommunityTopHeader( // Community Top Header 함수를 실행함
                onWriteClick = onWriteClick // onWriteClick 때 실행할 함수를 onWriteClick 때 실행할 함수에 넣음
            )
        }

        item { // 이 블록 안의 내용이 시작됨
            CommunitySearchField( // Community Search Field 함수를 실행함
                query = searchQuery, // searchQuery 값을 query 값에 넣음
                onQueryChange = { searchQuery = it } // onQueryChange 때 실행할 함수를 정해줌
            )
        }

        item { // 이 블록 안의 내용이 시작됨
            CommunityContestBannerCard( // 내용을 카드 모양으로 묶어서 보여줌
                contest = activeContest, // activeContest 값을 contest 값에 넣음
                onViewPostsClick = { // onViewPostsClick 때 실행할 함수를 정해줌
                    selectedCategoryIndex = categoryTabs.indexOfFirst { // selectedCategoryIndex 값을 정해줌
                        it.second == CommunityCategory.AVATAR_CONTEST // it.second 값을 정해줌
                    }.coerceAtLeast(0)
                },
                onWriteClick = { // onWriteClick 때 실행할 함수를 정해줌
                    onContestWriteClick(activeContest?.id) // on Contest Write Click 함수를 실행함
                }
            )
        }

        item { // 이 블록 안의 내용이 시작됨
            CommunityCategoryChipRow( // 안쪽 UI를 가로로 배치함
                categoryTabs = categoryTabs, // categoryTabs 값을 categoryTabs 값에 넣음
                selectedCategoryIndex = selectedCategoryIndex, // selectedCategoryIndex 값을 selectedCategoryIndex 값에 넣음
                onCategorySelected = { clickedIndex -> // onCategorySelected 때 실행할 함수를 정해줌
                    selectedCategoryIndex = clickedIndex // clickedIndex 값을 selectedCategoryIndex 값에 넣음
                }
            )
        }

        item { // 이 블록 안의 내용이 시작됨
            CommunitySortOptionRow( // 안쪽 UI를 가로로 배치함
                selectedSortOption = selectedSortOption, // selectedSortOption 값을 selectedSortOption 값에 넣음
                onSortOptionSelected = { selectedSortOption = it } // onSortOptionSelected 때 실행할 함수를 정해줌
            )
        }

        if (isLoading) { // 조건이 맞는지 확인함
            item { // 이 블록 안의 내용이 시작됨
                CommunityLoadingCard() // 내용을 카드 모양으로 묶어서 보여줌
            }
        }

        if (errorMessage != null) { // 조건이 맞는지 확인함
            item { // 이 블록 안의 내용이 시작됨
                CommunityErrorCard( // 내용을 카드 모양으로 묶어서 보여줌
                    message = errorMessage, // 오류 내용을 메시지에 넣음
                    onRetryClick = onRetryClick // onRetryClick 때 실행할 함수를 onRetryClick 때 실행할 함수에 넣음
                )
            }
        }

        // 현재 카테고리에 게시글이 하나도 없으면 안내 카드를 보여줍니다.
        if (!isLoading && filteredPosts.isEmpty()) { // 조건이 맞는지 확인함
            item { // 이 블록 안의 내용이 시작됨
                EmptyPostCard() // 내용을 카드 모양으로 묶어서 보여줌
            }
        }

        // 게시글 목록을 카드 형태로 렌더링합니다.
        items( // items 함수를 실행함
            items = pagedPosts, // pagedPosts 값을 items 값에 넣음
            key = { post -> post.id } // key 값을 정해줌
        ) { post ->
            CommunityPostCard( // 내용을 카드 모양으로 묶어서 보여줌
                post = post, // post 값을 post 값에 넣음
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    onPostClick(post) // on Post Click 함수를 실행함
                }
            )
        }

        if (!isLoading && filteredPosts.size > pageSize) { // 조건이 맞는지 확인함
            item { // 이 블록 안의 내용이 시작됨
                CommunityPaginationRow( // 안쪽 UI를 가로로 배치함
                    currentPage = safeCurrentPage, // safeCurrentPage 값을 currentPage 값에 넣음
                    totalPages = totalPages, // totalPages 값을 totalPages 값에 넣음
                    onPageSelected = { page -> // onPageSelected 때 실행할 함수를 정해줌
                        currentPage = page // page 값을 currentPage 값에 넣음
                    }
                )
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityLoadingCard() { // 데이터를 불러오는 함수 시작
    val cardColor = communitySoftCardColor()
    val cardBorderColor = communitySoftCardBorderColor()
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically, // verticalAlignment 값을 정해줌
            horizontalArrangement = Arrangement.Center // horizontalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            CircularProgressIndicator(modifier = Modifier.size(22.dp)) // UI 크기나 여백 같은 모양을 정함
            Spacer(modifier = Modifier.width(10.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = "게시글을 불러오는 중입니다.", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityErrorCard( // CommunityErrorCard 함수를 선언함
    message: String, // 메시지를 받음
    onRetryClick: () -> Unit // onRetryClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val cardColor = communitySoftCardColor()
    val cardBorderColor = communitySoftCardBorderColor()
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = message, // 메시지를 text 값에 넣음
                modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
            TextButton(onClick = onRetryClick) { // 누를 수 있는 버튼을 만듦
                Text(text = "다시 시도") // 화면에 글자를 보여줌
            }
        }
    }
}

// ------------------------------------------------------------
// 게시글이 없을 때 보여줄 카드입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun EmptyPostCard() { // EmptyPostCard 함수를 선언함
    val cardColor = communitySoftCardColor()
    val cardBorderColor = communitySoftCardBorderColor()
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(20.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "아직 게시글이 없어요", // text 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "첫 번째 글을 작성해서 커뮤니티를 시작해보세요.", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                lineHeight = 19.sp // lineHeight 값을 정해줌
            )
        }
    }
}

// ------------------------------------------------------------
// 상단 헤더 영역입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityTopHeader( // CommunityTopHeader 함수를 선언함
    onWriteClick: () -> Unit // onWriteClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isCommunityDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val buttonColor = if (isDark) Color(0xFF6D5BD0) else Color(0xFF2563EB) // 글쓰기 버튼색을 모드별로 분리함
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
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "다른 사용자들과 소통하고 경험을 나눠보세요", // text 값을 정해줌
                fontSize = 12.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                lineHeight = 18.sp // lineHeight 값을 정해줌
            )
        }

        Spacer(modifier = Modifier.width(12.dp)) // UI 크기나 여백 같은 모양을 정함

        Button( // 누를 수 있는 버튼을 만듦
            onClick = onWriteClick, // onWriteClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
            shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
            colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                containerColor = buttonColor, // containerColor 값을 정해줌
                contentColor = Color.White // contentColor 값을 정해줌
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp) // contentPadding 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "글쓰기", // text 값을 정해줌
                fontSize = 12.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = Color.White
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityContestBannerCard( // CommunityContestBannerCard 함수를 선언함
    contest: CommunityContest?, // contest 값을 받음
    onViewPostsClick: () -> Unit, // onViewPostsClick 때 실행할 함수를 받음
    onWriteClick: () -> Unit // onWriteClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isCommunityDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val buttonColor = if (isDark) Color(0xFF6D5BD0) else Color(0xFF2563EB) // 참가 버튼색을 모드별로 분리함
    val cardColor = communitySoftCardColor()
    val cardBorderColor = communitySoftCardBorderColor()
    val statusText = "진행중" // statusText 값을 저장함
    val title = "5월 아바타 콘테스트" // 제목을 저장함
    val period = "2026.05.09 ~ 2026.05.31" // period 값을 저장함
    val reward = "1등 기분좋음" // reward 값을 저장함
    val description = "아바타 콘테스트를 개최합니다 ~! 많관부" // description 값을 저장함

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(24.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke( // border 값을 정해줌
            1.dp,
            cardBorderColor
        )
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(18.dp)
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = statusText, // statusText 값을 text 값에 넣음
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .background(Color(0xFF12B981), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp), // .padding(horizontal 값을 정해줌
                    fontSize = 11.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = Color.White // color 값을 정해줌
                )

                Text( // 화면에 글자를 보여줌
                    text = "아바타 콘테스트", // text 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.primary // color 값을 정해줌
                )
            }

            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = title, // 제목을 text 값에 넣음
                fontSize = 19.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.ExtraBold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                maxLines = 1, // maxLines 값을 정해줌
                overflow = TextOverflow.Ellipsis // overflow 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = period, // period 값을 text 값에 넣음
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "보상: $reward", // text 값을 정해줌
                fontSize = 13.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.primary // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = description, // description 값을 text 값에 넣음
                fontSize = 13.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                lineHeight = 19.sp, // lineHeight 값을 정해줌
                maxLines = 2, // maxLines 값을 정해줌
                overflow = TextOverflow.Ellipsis // overflow 값을 정해줌
            )

            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.spacedBy(8.dp) // horizontalArrangement 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onViewPostsClick, // onViewPostsClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = buttonColor, // buttonColor 값을 containerColor 값에 넣음
                        contentColor = Color.White // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "참가글 보기", // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = Color.White
                    )
                }

                Button( // 누를 수 있는 버튼을 만듦
                    onClick = onWriteClick, // onWriteClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    shape = RoundedCornerShape(10.dp), // shape 값을 정해줌
                    colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                        containerColor = buttonColor, // containerColor 값을 정해줌
                        contentColor = Color.White // contentColor 값을 정해줌
                    ),
                    contentPadding = PaddingValues(vertical = 10.dp) // contentPadding 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "참가글 작성", // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunitySearchField( // CommunitySearchField 함수를 선언함
    query: String, // query 값을 받음
    onQueryChange: (String) -> Unit // onQueryChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    OutlinedTextField( // 사용자가 입력할 칸을 만듦
        value = query, // query 값을 입력값에 넣음
        onValueChange = onQueryChange, // onQueryChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        leadingIcon = { // leadingIcon 값을 정해줌
            Icon( // 화면에 아이콘을 보여줌
                imageVector = Icons.Filled.Search, // imageVector 값을 정해줌
                contentDescription = "검색", // contentDescription 값을 정해줌
                tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
            )
        },
        placeholder = { // placeholder 값을 정해줌
            Text( // 화면에 글자를 보여줌
                text = "제목 검색", // text 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
            )
        },
        singleLine = true, // true 값을 singleLine 값에 넣음
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
            focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.65f), // focusedBorderColor 값을 정해줌
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
            focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
            focusedContainerColor = MaterialTheme.colorScheme.surface, // focusedContainerColor 값을 정해줌
            unfocusedContainerColor = MaterialTheme.colorScheme.surface, // unfocusedContainerColor 값을 정해줌
            cursorColor = SpentopiaMutedPurple // SpentopiaMutedPurple 값을 cursorColor 값에 넣음
        )
    )
}

// ------------------------------------------------------------
// 카테고리 칩 Row입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
@OptIn(ExperimentalLayoutApi::class) // 이 코드에 특별한 역할을 붙이는 표시
private fun CommunityCategoryChipRow( // CommunityCategoryChipRow 함수를 선언함
    categoryTabs: List<Pair<String, CommunityCategory?>>,
    selectedCategoryIndex: Int, // selectedCategoryIndex 값을 받음
    onCategorySelected: (Int) -> Unit // onCategorySelected 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    FlowRow( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.spacedBy(8.dp), // horizontalArrangement 값을 정해줌
        verticalArrangement = Arrangement.spacedBy(8.dp) // verticalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        categoryTabs.forEachIndexed { index, _ ->
            val tab = categoryTabs[index] // tab 값을 저장함
            val isSelected = index == selectedCategoryIndex // 선택된 항목인지 저장함

            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .widthIn(min = 80.dp) // .widthIn(min 값을 정해줌
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .clickable { // 이 블록 안의 내용이 시작됨
                        onCategorySelected(index) // on Category Selected 함수를 실행함
                    }
                    .padding(horizontal = 12.dp, vertical = 7.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = tab.first, // text 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, // fontWeight 값을 정해줌
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunitySortOptionRow( // CommunitySortOptionRow 함수를 선언함
    selectedSortOption: CommunitySortOption, // selectedSortOption 값을 받음
    onSortOptionSelected: (CommunitySortOption) -> Unit // onSortOptionSelected 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    LazyRow( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.spacedBy(8.dp) // horizontalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        items( // items 함수를 실행함
            items = CommunitySortOption.entries, // items 값을 정해줌
            key = { option -> option.label } // key 값을 정해줌
        ) { option ->
            val isSelected = option == selectedSortOption // 선택된 항목인지 저장함
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) { // 조건이 맞는지 확인함
                            MaterialTheme.colorScheme.primaryContainer
                        } else { // 이 블록 안의 내용이 시작됨
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                    .clickable { onSortOptionSelected(option) }
                    .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = if (isSelected) "✓ ${option.label}" else option.label, // text 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, // fontWeight 값을 정해줌
                    color = if (isSelected) { // color 값을 정해줌
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else { // 이 블록 안의 내용이 시작됨
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
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityPostCard( // CommunityPostCard 함수를 선언함
    post: CommunityPost, // post 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val badgeColors = communityCategoryBadgeColors(post.category) // badgeColors 값을 저장함
    val cardColor = communitySoftCardColor()
    val cardBorderColor = communitySoftCardBorderColor()

    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .clickable { // 이 블록 안의 내용이 시작됨
                onClick() // on Click 함수를 실행함
            },
        shape = RoundedCornerShape(20.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors( // colors 값을 정해줌
            containerColor = cardColor // containerColor 값을 정해줌
        ),
        border = BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier.padding(18.dp) // UI 크기나 여백 같은 모양을 정함
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeColors.background)
                        .padding(horizontal = 12.dp, vertical = 8.dp) // .padding(horizontal 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = post.category.badgeLabel(), // text 값을 정해줌
                        fontSize = 12.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                        color = badgeColors.content // color 값을 정해줌
                    )
                }

                Text( // 화면에 글자를 보여줌
                    text = post.timeText, // text 값을 정해줌
                    fontSize = 12.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            }

            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = post.title, // text 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                maxLines = 1, // maxLines 값을 정해줌
                overflow = TextOverflow.Ellipsis // overflow 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = post.content, // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                lineHeight = 20.sp, // lineHeight 값을 정해줌
                maxLines = 2, // maxLines 값을 정해줌
                overflow = TextOverflow.Ellipsis // overflow 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                horizontalArrangement = Arrangement.SpaceBetween, // horizontalArrangement 값을 정해줌
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = post.tagText, // text 값을 정해줌
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Row( // 안쪽 UI를 가로로 배치함
                    horizontalArrangement = Arrangement.spacedBy(8.dp) // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    SmallCountChip( // Small Count Chip 함수를 실행함
                        text = "좋아요 ${post.likeCount}" // text 값을 정해줌
                    )

                    SmallCountChip( // Small Count Chip 함수를 실행함
                        text = "댓글 ${post.commentCount}" // text 값을 정해줌
                    )

                    SmallCountChip( // Small Count Chip 함수를 실행함
                        text = "조회 ${post.viewCount}" // text 값을 정해줌
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityPaginationRow( // CommunityPaginationRow 함수를 선언함
    currentPage: Int, // currentPage 값을 받음
    totalPages: Int, // totalPages 값을 받음
    onPageSelected: (Int) -> Unit // onPageSelected 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val isDark = isCommunityDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    val selectedColor = if (isDark) Color(0xFF6D5BD0) else Color(0xFF2563EB) // 페이지 선택 버튼색을 모드별로 분리함
    val unselectedColor = if (isDark) Color(0xFF171A2B) else Color(0xFFEFF6FF) // 선택 안 된 페이지 배경색을 모드별로 분리함
    val unselectedBorderColor = if (isDark) Color(0xFF2E3352) else Color(0xFFBFDBFE) // 선택 안 된 페이지 테두리색을 모드별로 분리함
    val unselectedTextColor = if (isDark) Color(0xFFD8D6F5) else Color(0xFF1E3A8A) // 선택 안 된 페이지 글자색을 모드별로 분리함
    LazyRow( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = Arrangement.Center, // horizontalArrangement 값을 정해줌
        contentPadding = PaddingValues(vertical = 4.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        items( // items 함수를 실행함
            items = (1..totalPages).toList(), // items 값을 정해줌
            key = { page -> page } // key 값을 정해줌
        ) { page ->
            val isSelected = page == currentPage // 선택된 항목인지 저장함
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .padding(horizontal = 3.dp) // .padding(horizontal 값을 정해줌
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) selectedColor // 조건이 맞는지 확인함
                        else unselectedColor // 위 조건이 아니면 이쪽을 실행함
                    )
                    .border(
                        width = 1.dp, // width 값을 정해줌
                        color = if (isSelected) selectedColor else unselectedBorderColor, // color 값을 정해줌
                        shape = RoundedCornerShape(10.dp) // shape 값을 정해줌
                    )
                    .clickable { onPageSelected(page) },
                contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = page.toString(), // text 값을 정해줌
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, // fontWeight 값을 정해줌
                    color = if (isSelected) Color.White else unselectedTextColor // color 값을 정해줌
                )
            }
        }
    }
}

private data class CommunityBadgeColors( // CommunityBadgeColors 데이터를 묶어둘 클래스 시작
    val background: Color, // background 값을 저장함
    val content: Color // 내용을 저장함
)

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun communityCategoryBadgeColors(category: CommunityCategory): CommunityBadgeColors { // communityCategoryBadgeColors 함수를 선언함
    val isDark = isCommunityDarkTheme() // 앱 설정 기준으로 다크모드인지 저장함
    return when (category) { // 이 값을 함수 결과로 돌려줌
        CommunityCategory.NOTICE -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF164E63), Color(0xFFBAE6FD)) // Community Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF0284C7), Color.White) // Community Badge Colors 함수를 실행함
        }
        CommunityCategory.AVATAR_CONTEST -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF713F12), Color(0xFFFEF3C7)) // Community Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFFB45309), Color.White) // Community Badge Colors 함수를 실행함
        }
        CommunityCategory.REQUEST -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF581C87), Color(0xFFE9D5FF)) // Community Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF7E22CE), Color.White) // Community Badge Colors 함수를 실행함
        }
        CommunityCategory.FREE_BOARD -> if (isDark) { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF064E3B), Color(0xFFA7F3D0)) // Community Badge Colors 함수를 실행함
        } else { // 이 블록 안의 내용이 시작됨
            CommunityBadgeColors(Color(0xFF059669), Color.White) // Community Badge Colors 함수를 실행함
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun isCommunityDarkTheme(): Boolean { // 앱 테마 기준으로 커뮤니티 다크모드 여부를 확인함
    return MaterialTheme.colorScheme.background == SpentopiaDarkBackground // 시스템 설정이 아니라 앱 설정 기준으로 판단함
}

@Composable
private fun communitySoftCardColor(): Color {
    return if (isCommunityDarkTheme()) Color(0xFF171A2B) else Color(0xFFF8FBFF)
}

@Composable
private fun communitySoftCardBorderColor(): Color {
    return if (isCommunityDarkTheme()) Color(0xFF4C3B7A) else Color(0xFFBFDBFE)
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
// 좋아요 / 댓글 개수용 작은 칩 UI입니다.
// ------------------------------------------------------------
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SmallCountChip( // SmallCountChip 함수를 선언함
    text: String // text 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 8.dp, vertical = 7.dp) // .padding(horizontal 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = text, // text 값을 text 값에 넣음
            fontSize = 11.sp, // fontSize 값을 정해줌
            color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
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
fun getInitialCommunityPosts(): List<CommunityPost> { // 데이터를 불러오는 함수 시작
    return listOf( // 이 값을 함수 결과로 돌려줌
        CommunityPost( // Community Post 함수를 실행함
            id = "7", // 아이디를 정해줌
            title = "커뮤니티 이용 안내", // 제목을 정해줌
            content = "서로에게 도움이 되는 소비 기록, 아바타, 아이디어 이야기를 편하게 나눠주세요.", // 내용을 정해줌
            fullContent = "서로에게 도움이 되는 소비 기록, 아바타, 아이디어 이야기를 편하게 나눠주세요. 비방이나 개인정보가 포함된 글은 예고 없이 삭제될 수 있습니다.", // fullContent 값을 정해줌
            author = "Spentopia", // author 값을 정해줌
            timeText = "방금 전", // timeText 값을 정해줌
            likeCount = 3, // likeCount 값을 정해줌
            commentCount = 0, // commentCount 값을 정해줌
            tagText = "공지", // tagText 값을 정해줌
            category = CommunityCategory.NOTICE, // 카테고리를 정해줌
            viewCount = 312, // viewCount 값을 정해줌
            comments = emptyList(), // comments 값을 정해줌
            isLiked = false // false 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "1", // 아이디를 정해줌
            title = "이번 달 아바타 7일 연속 기록 성공했어요!", // 제목을 정해줌
            content = "작은 금액은 놓칠 때도 있었지만, 그래도 소비 패턴이 조금씩 보이기 시작해서 뿌듯해요.", // 내용을 정해줌
            fullContent = "작은 금액은 놓칠 때도 있었지만, 그래도 소비 패턴이 조금씩 보이기 시작해서 뿌듯해요. 처음에는 귀찮았는데 습관이 생기니까 훨씬 편해졌어요.", // fullContent 값을 정해줌
            author = "기록요정", // author 값을 정해줌
            timeText = "1시간 전", // timeText 값을 정해줌
            likeCount = 8, // likeCount 값을 정해줌
            commentCount = 2, // commentCount 값을 정해줌
            tagText = "기록초보", // tagText 값을 정해줌
            category = CommunityCategory.AVATAR_CONTEST, // 카테고리를 정해줌
            viewCount = 128, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "user_a", // authorId 값을 정해줌
                    author = "절약메이트", // author 값을 정해줌
                    content = "와 7일 연속이면 진짜 대단해요!", // 내용을 정해줌
                    timeText = "50분 전" // timeText 값을 정해줌
                ),
                CommunityComment( // Community Comment 함수를 실행함
                    id = "2", // 아이디를 정해줌
                    authorId = "current_user", // authorId 값을 정해줌
                    author = "현재사용자", // author 값을 정해줌
                    content = "저도 이번 달에는 꾸준히 기록해보려고요.", // 내용을 정해줌
                    timeText = "30분 전" // timeText 값을 정해줌
                )
            ),
            isLiked = false // false 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "2", // 아이디를 정해줌
            title = "아바타 꾸미기 보상 받으려면 어떤 미션부터 하는 게 좋을까요?", // 제목을 정해줌
            content = "출석이랑 소비기록 중에서 어떤 걸 먼저 챙기는 게 효율적인지 궁금해요.", // 내용을 정해줌
            fullContent = "출석이랑 소비기록 중에서 어떤 걸 먼저 챙기는 게 효율적인지 궁금해요. 시작 단계라 어떤 순서가 좋은지 잘 모르겠어요.", // fullContent 값을 정해줌
            author = "코디초보", // author 값을 정해줌
            timeText = "3시간 전", // timeText 값을 정해줌
            likeCount = 5, // likeCount 값을 정해줌
            commentCount = 1, // commentCount 값을 정해줌
            tagText = "미션질문", // tagText 값을 정해줌
            category = CommunityCategory.AVATAR_CONTEST, // 카테고리를 정해줌
            viewCount = 94, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "user_b", // authorId 값을 정해줌
                    author = "보상수집가", // author 값을 정해줌
                    content = "저는 출석부터 챙기고 기록 습관을 붙였어요.", // 내용을 정해줌
                    timeText = "2시간 전" // timeText 값을 정해줌
                )
            ),
            isLiked = false // false 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "3", // 아이디를 정해줌
            title = "주말 지출이 평일보다 두 배인 이유를 찾았어요", // 제목을 정해줌
            content = "모임, 카페, 충동구매가 한 번에 몰려 있더라고요. 이번 주부터는 주말 예산을 따로 잡아보려 합니다.", // 내용을 정해줌
            fullContent = "모임, 카페, 충동구매가 한 번에 몰려 있더라고요. 그래서 이번 주부터는 주말 예산을 따로 잡아보려고 합니다. 평일보다 지출이 커지는 이유가 확실히 보였어요.", // fullContent 값을 정해줌
            author = "토요소비왕", // author 값을 정해줌
            timeText = "5시간 전", // timeText 값을 정해줌
            likeCount = 11, // likeCount 값을 정해줌
            commentCount = 2, // commentCount 값을 정해줌
            tagText = "분석해보는중", // tagText 값을 정해줌
            category = CommunityCategory.FREE_BOARD, // 카테고리를 정해줌
            viewCount = 216, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "current_user", // authorId 값을 정해줌
                    author = "현재사용자", // author 값을 정해줌
                    content = "주말 예산 따로 잡는 방법 괜찮네요.", // 내용을 정해줌
                    timeText = "4시간 전" // timeText 값을 정해줌
                ),
                CommunityComment( // Community Comment 함수를 실행함
                    id = "2", // 아이디를 정해줌
                    authorId = "user_c", // authorId 값을 정해줌
                    author = "카페중독탈출", // author 값을 정해줌
                    content = "저도 모임비 때문에 주말이 항상 문제였어요.", // 내용을 정해줌
                    timeText = "3시간 전" // timeText 값을 정해줌
                )
            ),
            isLiked = true // true 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "4", // 아이디를 정해줌
            title = "가계부 쓰다 보니 생각보다 배달비가 너무 크네요", // 제목을 정해줌
            content = "한 번 주문할 때는 얼마 안 되는 것 같았는데, 한 달 합계를 보니까 꽤 부담이 되더라고요.", // 내용을 정해줌
            fullContent = "한 번 주문할 때는 얼마 안 되는 것 같았는데, 한 달 합계를 보니까 꽤 부담이 되더라고요. 이번 달부터는 주 1회만 배달을 허용해보려 합니다.", // fullContent 값을 정해줌
            author = "배달줄이기중", // author 값을 정해줌
            timeText = "7시간 전", // timeText 값을 정해줌
            likeCount = 6, // likeCount 값을 정해줌
            commentCount = 1, // commentCount 값을 정해줌
            tagText = "배달줄이기", // tagText 값을 정해줌
            category = CommunityCategory.FREE_BOARD, // 카테고리를 정해줌
            viewCount = 173, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "user_d", // authorId 값을 정해줌
                    author = "식비절약러", // author 값을 정해줌
                    content = "배달앱 삭제하고 확실히 줄었어요.", // 내용을 정해줌
                    timeText = "6시간 전" // timeText 값을 정해줌
                )
            ),
            isLiked = false // false 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "5", // 아이디를 정해줌
            title = "카페 지출 줄이려면 예산을 먼저 따로 빼두는 게 좋더라고요", // 제목을 정해줌
            content = "저는 아예 주간 간식비를 따로 정해두니까 훨씬 덜 흔들렸어요. 생각보다 효과가 꽤 컸습니다.", // 내용을 정해줌
            fullContent = "저는 아예 주간 간식비를 따로 정해두니까 훨씬 덜 흔들렸어요. 그냥 아껴야지 하는 것보다 실제 숫자를 정하는 게 더 효과적이었어요.", // fullContent 값을 정해줌
            author = "절약실험러", // author 값을 정해줌
            timeText = "2시간 전", // timeText 값을 정해줌
            likeCount = 14, // likeCount 값을 정해줌
            commentCount = 2, // commentCount 값을 정해줌
            tagText = "절약실험중", // tagText 값을 정해줌
            category = CommunityCategory.REQUEST, // 카테고리를 정해줌
            viewCount = 241, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "user_e", // authorId 값을 정해줌
                    author = "예산지킴이", // author 값을 정해줌
                    content = "숫자로 정하는 게 진짜 중요한 것 같아요.", // 내용을 정해줌
                    timeText = "1시간 전" // timeText 값을 정해줌
                ),
                CommunityComment( // Community Comment 함수를 실행함
                    id = "2", // 아이디를 정해줌
                    authorId = "current_user", // authorId 값을 정해줌
                    author = "현재사용자", // author 값을 정해줌
                    content = "저도 이번 주부터 따라해보겠습니다.", // 내용을 정해줌
                    timeText = "40분 전" // timeText 값을 정해줌
                )
            ),
            isLiked = true // true 값을 isLiked인지 여부에 넣음
        ),
        CommunityPost( // Community Post 함수를 실행함
            id = "6", // 아이디를 정해줌
            title = "장보기 전에 냉장고 사진 찍는 습관이 은근 도움 됩니다", // 제목을 정해줌
            content = "이미 있는 재료를 또 사는 일이 줄어들어서 식비를 아끼는 데 꽤 효과가 있었어요.", // 내용을 정해줌
            fullContent = "이미 있는 재료를 또 사는 일이 줄어들어서 식비를 아끼는 데 꽤 효과가 있었어요. 특히 퇴근 후 급하게 장볼 때 중복 구매가 줄더라고요.", // fullContent 값을 정해줌
            author = "장보기고수", // author 값을 정해줌
            timeText = "8시간 전", // timeText 값을 정해줌
            likeCount = 9, // likeCount 값을 정해줌
            commentCount = 1, // commentCount 값을 정해줌
            tagText = "식비절약", // tagText 값을 정해줌
            category = CommunityCategory.REQUEST, // 카테고리를 정해줌
            viewCount = 187, // viewCount 값을 정해줌
            comments = listOf( // comments 값을 정해줌
                CommunityComment( // Community Comment 함수를 실행함
                    id = "1", // 아이디를 정해줌
                    authorId = "user_f", // authorId 값을 정해줌
                    author = "냉장고정리왕", // author 값을 정해줌
                    content = "이 팁 좋네요. 저도 바로 써먹어볼게요.", // 내용을 정해줌
                    timeText = "7시간 전" // timeText 값을 정해줌
                )
            ),
            isLiked = false // false 값을 isLiked인지 여부에 넣음
        )
    )
}

private fun communityRecencyRank(timeText: String): Int { // communityRecencyRank 함수를 선언함
    if (timeText.contains("방금")) return 0 // 조건이 맞는지 확인함

    val number = Regex("""\d+""") // number 값을 저장함
        .find(timeText)
        ?.value
        ?.toIntOrNull()
        ?: return Int.MAX_VALUE

    return when { // 이 값을 함수 결과로 돌려줌
        timeText.contains("분") -> number
        timeText.contains("시간") -> number * 60
        timeText.contains("일") -> number * 24 * 60
        else -> Int.MAX_VALUE // 위 조건이 아니면 이쪽을 실행함
    }
}

// ------------------------------------------------------------
// 프리뷰입니다.
// ------------------------------------------------------------
@Preview(showBackground = true) // 미리보기에서 화면을 볼 수 있게 표시함
@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun CommunityScreenPreview() { // CommunityScreenPreview 함수를 선언함
    CommunityScreen( // 커뮤니티 화면을 보여줌
        posts = getInitialCommunityPosts() // posts 값을 정해줌
    )
}
