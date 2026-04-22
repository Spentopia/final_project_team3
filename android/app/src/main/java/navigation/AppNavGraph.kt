package com.ict.spentopia.navigation

// 로그 찍을 때 쓰는 도구
// 실행 중에 값이 잘 들어오는지 확인할 때 많이씀
import android.util.Log

// Compose UI에서 padding 줄 때 씀.
import androidx.compose.foundation.layout.padding

// 상단바 아이콘들
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings

// Material3 UI 구성요소들
// Scaffold, TopAppBar, Drawer 같은 것들 쓸 수 있게 해줌
import androidx.compose.material3.*

// Compose 상태 관리용
import androidx.compose.runtime.*

// Modifier는 UI 모양, 크기, 여백 같은 거 조절할 때 씀
import androidx.compose.ui.Modifier

// ViewModel을 Composable 안에서 만들거나 가져올 때 필요함
import androidx.lifecycle.viewmodel.compose.viewModel

// 네비게이션에서 인자 타입 지정할 때 씀
import androidx.navigation.NavType

// Compose Navigation 관련 기능들이 들어 있음
// 화면 이동할 때 꼭 필요함
import androidx.navigation.compose.*
import androidx.navigation.navArgument

// 각 화면 import
import com.example.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.feature.analysis.AnalysisScreen

// 예전 개별 AvatarScreen은 안 쓰고 통합 화면을 쓸 거라서 제거된 상태
// import com.ict.spentopia.feature.avatar.AvatarScreen

import com.ict.spentopia.feature.auth.LoginScreen

// 로그인 후 지갑 정보 저장하는 ViewModel
import com.ict.spentopia.feature.auth.LoginViewModel

import com.ict.spentopia.feature.budget.BudgetScreen
import com.ict.spentopia.feature.community.*
import com.ict.spentopia.feature.home.HomeScreen
import com.ict.spentopia.feature.market.MarketScreen

// 예전 개별 MyPageScreen도 안 쓰고 통합 화면으로 바뀐 상태
// import com.ict.spentopia.feature.mypage.MyPageScreen

// 마이페이지 + 아바타를 같이 보여주는 통합 화면
import com.ict.spentopia.feature.mypage.ProfileAvatarScreen

// 솔라나 지갑 연결 결과를 받는 데 쓰는 객체
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender

// Drawer 열고 닫을 때 코루틴이 필요해서 import
import kotlinx.coroutines.launch

// Material3의 실험 기능을 쓰겠다는 뜻
// 경고 없애려고 붙이는 경우가 많음
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    // 지갑 연결 결과를 LoginScreen에 넘겨주기 위해 받음
    walletActivityResultSender: ActivityResultSender
) {
    // 네비게이션 전체를 관리하는 컨트롤러
    // 화면 이동할 때 핵심 역할을 함
    val navController = rememberNavController()

    // Drawer(왼쪽 메뉴)의 현재 상태를 기억
    // 처음엔 닫힌 상태로 시작
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // 코루틴 실행용 scope
    // Drawer 열고 닫을 때 launch 안에서 씀
    val scope = rememberCoroutineScope()

    // 현재 어떤 화면(route)에 있는지 실시간으로 확인하기 위한 상태
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // 지금은 로그인한 사용자 id가 아직 없어서
    // 임시로 문자열 하나 넣어둠
    // 나중엔 실제 로그인 사용자 id로 바꿔야 함
    val currentUserId = "current_user"

    // 커뮤니티 게시글 목록 상태를 여기서 관리
    // 왜 여기서 하냐면,
    // 목록 화면 / 상세 화면 / 글쓰기 화면이
    // 전부 같은 게시글 데이터를 같이 써야 하기 때문
    val communityPosts = remember {
        mutableStateListOf<CommunityPost>().apply {
            // 처음 보여줄 기본 게시글들 넣는 부분
            addAll(getInitialCommunityPosts())
        }
    }

    // Drawer를 보여줄 화면 목록
    // 로그인 화면 같은 곳에서는 Drawer를 안 보여주려고 따로 정해둠
    val showDrawerScreens = setOf(
        Route.Home.route,
        Route.Budget.route,
        Route.Analysis.route,
        Route.ProfileAvatar.route,
        Route.Market.route,
        Route.Plaza.route,
        Route.Community.route,
        Route.CommunityWrite.route,
        Route.CommunityDetail.route
    )

    // 현재 화면이 Drawer를 보여줘야 하는 화면인지 검사
    val shouldShowDrawer = currentRoute in showDrawerScreens

    // 왼쪽에서 나오는 네비게이션 Drawer 전체 구조
    ModalNavigationDrawer(
        drawerState = drawerState,

        // shouldShowDrawer가 true일 때만 손가락 제스처로 열 수 있게 함
        gesturesEnabled = shouldShowDrawer,

        drawerContent = {
            // Drawer를 보여줘야 하는 화면일 때만 내용 표시
            if (shouldShowDrawer) {
                ModalDrawerSheet {
                    AppDrawerContent(
                        // 가계부(ledger/home) 메뉴 눌렀을 때
                        onLedgerClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Home.route) {
                                launchSingleTop = true
                            }
                        },

                        // 예산 화면 이동
                        onBudgetClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Budget.route) {
                                launchSingleTop = true
                            }
                        },

                        // 분석 화면 이동
                        onAnalysisClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Analysis.route) {
                                launchSingleTop = true
                            }
                        },

                        // 마이페이지 + 아바타 통합 화면 이동
                        onProfileAvatarClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.ProfileAvatar.route) {
                                launchSingleTop = true
                            }
                        },

                        // 마켓 화면 이동
                        onMarketClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Market.route) {
                                launchSingleTop = true
                            }
                        },

                        // 광장 화면 이동
                        onPlazaClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Plaza.route) {
                                launchSingleTop = true
                            }
                        },

                        // 커뮤니티 화면 이동
                        onCommunityClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Community.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    ) {
        // 전체 화면의 기본 골격
        // topBar, bottomBar, 본문 영역 같은 걸 쉽게 나눌 수 있음
        Scaffold(
            topBar = {
                // Drawer가 필요한 화면에서만 상단바 보여줌
                if (shouldShowDrawer) {
                    CenterAlignedTopAppBar(
                        // 가운데 제목
                        title = { Text("Spentopia") },

                        // 왼쪽 햄버거 메뉴 버튼
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    // 버튼 누르면 Drawer 열어.
                                    scope.launch { drawerState.open() }
                                }
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "메뉴")
                            }
                        },

                        // 오른쪽 액션 버튼들
                        actions = {
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.Settings, contentDescription = "설정")
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Default.NotificationsNone, contentDescription = "알림")
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->

            // 실제 화면 이동을 담당하는 NavHost
            NavHost(
                navController = navController,

                // 앱 시작 시 첫 화면은 로그인 화면
                startDestination = Route.Login.route,

                // Scaffold가 준 padding을 적용해서
                // 상단바에 화면이 안 가려지게 함
                modifier = Modifier.padding(innerPadding)
            ) {

                // -----------------------------------
                // 로그인 화면
                // -----------------------------------
                composable(Route.Login.route) {

                    // LoginViewModel을 여기서 생성하거나 가져옴
                    val loginViewModel: LoginViewModel = viewModel()

                    LoginScreen(
                        // 일반 로그인 성공 시 홈으로 이동
                        onLoginClick = {
                            navController.navigate(Route.Home.route) {
                                // 로그인 화면은 뒤로 가기에 남기지 않으려고 제거해.
                                popUpTo(Route.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },

                        // 지갑 연결 결과를 LoginScreen에 넘겨줌
                        walletActivityResultSender = walletActivityResultSender,

                        // 지갑 연결 성공하면 주소랑 지갑 종류를 받음
                        onWalletConnected = { walletAddress, walletProvider ->

                            // 로그로 값 확인
                            Log.d("Spentopia", "지갑 주소: $walletAddress")
                            Log.d("Spentopia", "지갑 종류: $walletProvider")

                            // 지갑 주소가 비어 있지 않으면 저장 진행
                            if (walletAddress.isNotBlank()) {
                                loginViewModel.saveWalletSession(
                                    walletAddress = walletAddress,
                                    walletProvider = walletProvider,

                                    // 저장 성공하면 홈으로 이동
                                    onSuccess = {
                                        navController.navigate(Route.Home.route) {
                                            popUpTo(Route.Login.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            } else {
                                // 주소가 없으면 에러 로그
                                Log.e("Spentopia", "지갑 주소 없음")
                            }
                        }
                    )
                }

                // -----------------------------------
                // 홈 화면
                // -----------------------------------
                composable(Route.Home.route) {
                    HomeScreen(
                        onLedgerClick = {
                            navController.navigate(Route.Home.route)
                        },
                        onMyPageClick = {
                            navController.navigate(Route.ProfileAvatar.route)
                        },
                        onBudgetClick = {
                            navController.navigate(Route.Budget.route)
                        },
                        onAnalysisClick = {
                            navController.navigate(Route.Analysis.route)
                        },
                        onAvatarClick = {
                            navController.navigate(Route.ProfileAvatar.route)
                        },
                        onMarketClick = {
                            navController.navigate(Route.Market.route)
                        },
                        onPlazaClick = {
                            navController.navigate(Route.Plaza.route)
                        },
                        onCommunityClick = {
                            navController.navigate(Route.Community.route)
                        }
                    )
                }

                // 각각 화면 등록
                composable(Route.Budget.route) { BudgetScreen() }
                composable(Route.Analysis.route) { AnalysisScreen() }
                composable(Route.ProfileAvatar.route) { ProfileAvatarScreen() }
                composable(Route.Market.route) { MarketScreen() }
                composable(Route.Plaza.route) { PlazaScreen() }

                // -----------------------------------
                // 커뮤니티 메인 화면
                // -----------------------------------
                composable(Route.Community.route) {
                    CommunityScreen(
                        // 게시글 목록 넘겨줌
                        posts = communityPosts,

                        // 글쓰기 버튼 누르면 글쓰기 화면 이동
                        onWriteClick = {
                            navController.navigate(Route.CommunityWrite.route)
                        },

                        // 채팅 버튼은 아직 기능 없음
                        onChatClick = { },

                        // 게시글 하나 누르면 상세 화면으로 이동
                        onPostClick = { post ->
                            navController.navigate(
                                Route.CommunityDetail.createRoute(post.id)
                            )
                        }
                    )
                }

                // -----------------------------------
                // 커뮤니티 글쓰기 화면
                // -----------------------------------
                composable(Route.CommunityWrite.route) {
                    CommunityWriteScreen(
                        // 뒤로가기
                        onBackClick = {
                            navController.popBackStack()
                        },

                        // 글 등록 버튼 눌렀을 때
                        onSubmitClick = { category, title, content ->

                            // 새 글 id 만들기
                            // 기존 글 중 가장 큰 id + 1
                            val nextId = (communityPosts.maxOfOrNull { it.id } ?: 0) + 1

                            // 새 게시글 객체 생성
                            val newPost = CommunityPost(
                                id = nextId,
                                title = title,

                                // 목록에선 너무 길면 보기 힘들어서 60글자만 저장
                                content = content.take(60),

                                // 상세 화면용 전체 내용
                                fullContent = content,

                                // 지금은 임시 작성자 이름
                                author = "현재사용자",

                                // 방금 쓴 글이니까 "방금 전"
                                timeText = "방금 전",

                                likeCount = 0,
                                commentCount = 0,
                                tagText = "새글",
                                category = category,
                                comments = emptyList(),
                                isLiked = false
                            )

                            // 최신 글이 위에 보이게 맨 앞에 추가
                            communityPosts.add(0, newPost)

                            // 작성 끝나면 이전 화면으로 돌아감
                            navController.popBackStack()
                        }
                    )
                }

                // -----------------------------------
                // 커뮤니티 상세 화면
                // -----------------------------------
                composable(
                    route = Route.CommunityDetail.route,

                    // postId라는 Int 타입 인자를 받음
                    arguments = listOf(
                        navArgument("postId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->

                    // 전달받은 postId 꺼내기
                    // 없으면 -1
                    val postId = backStackEntry.arguments?.getInt("postId") ?: -1

                    // 현재 id에 맞는 게시글을 다시 찾음
                    // 이렇게 해야 좋아요/댓글 수정 후에도 최신 데이터가 반영
                    val selectedPost = communityPosts.find { it.id == postId }

                    CommunityDetailScreen(
                        // 찾은 게시글 넘겨줌
                        post = selectedPost,

                        // 뒤로가기
                        onBackClick = {
                            navController.popBackStack()
                        },

                        // -------------------------
                        // 게시글 수정
                        // -------------------------
                        onUpdateClick = { updatedPost ->

                            // 수정할 게시글 위치 찾기
                            val index = communityPosts.indexOfFirst {
                                it.id == updatedPost.id
                            }

                            // 찾았으면 그 자리만 새 글로 교체
                            if (index != -1) {
                                communityPosts[index] = updatedPost
                            }
                        },

                        // -------------------------
                        // 게시글 삭제
                        // -------------------------
                        onDeleteClick = { deletePostId ->

                            // 삭제할 글 위치 찾기
                            val index = communityPosts.indexOfFirst {
                                it.id == deletePostId
                            }

                            // 찾았으면 삭제
                            if (index != -1) {
                                communityPosts.removeAt(index)
                            }

                            // 삭제됐으니 상세 화면에 있을 수 없어서 뒤로 이동
                            navController.popBackStack()
                        },

                        // -------------------------
                        // 좋아요 토글
                        // -------------------------
                        onToggleLikeClick = { targetPostId ->

                            // 눌린 게시글 위치 찾기
                            val index = communityPosts.indexOfFirst {
                                it.id == targetPostId
                            }

                            if (index != -1) {
                                val oldPost = communityPosts[index]

                                // 좋아요 상태 반대로 바꾸기
                                val newIsLiked = !oldPost.isLiked

                                // 이미 좋아요 상태였으면 취소니까 -1
                                // 아니었으면 새로 누른 거니까 +1
                                val newLikeCount = if (oldPost.isLiked) {
                                    (oldPost.likeCount - 1).coerceAtLeast(0)
                                } else {
                                    oldPost.likeCount + 1
                                }

                                // copy로 기존 글을 복사하면서
                                // 좋아요 관련 값만 바꿔서 저장
                                communityPosts[index] = oldPost.copy(
                                    isLiked = newIsLiked,
                                    likeCount = newLikeCount
                                )
                            }
                        },

                        // -------------------------
                        // 댓글 추가
                        // -------------------------
                        onAddCommentClick = { targetPostId, newCommentText ->

                            // 댓글 달 게시글 찾기
                            val postIndex = communityPosts.indexOfFirst {
                                it.id == targetPostId
                            }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                // 새 댓글 id 만들기
                                // 기존 댓글 중 최대 id + 1
                                val nextCommentId =
                                    (oldPost.comments.maxOfOrNull { it.id } ?: 0) + 1

                                // 새 댓글 객체 생성
                                val newComment = CommunityComment(
                                    id = nextCommentId,
                                    authorId = currentUserId,
                                    author = "현재사용자",
                                    content = newCommentText,
                                    timeText = "방금 전"
                                )

                                // 기존 댓글 목록 뒤에 새 댓글 추가
                                val updatedComments = oldPost.comments + newComment

                                // 게시글 안의 댓글 목록, 댓글 수 갱신
                                communityPosts[postIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // -------------------------
                        // 댓글 수정
                        // -------------------------
                        onUpdateCommentClick = { targetPostId, commentId, updatedText ->

                            // 대상 게시글 찾기
                            val postIndex = communityPosts.indexOfFirst {
                                it.id == targetPostId
                            }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                // 댓글들을 하나씩 보면서
                                // 수정 대상 댓글이면 내용 바꾸기
                                val updatedComments = oldPost.comments.map { comment ->
                                    if (
                                        comment.id == commentId &&
                                        comment.authorId == currentUserId
                                    ) {
                                        comment.copy(
                                            content = updatedText,
                                            timeText = "방금 전"
                                        )
                                    } else {
                                        comment
                                    }
                                }

                                // 수정된 댓글 목록으로 교체
                                communityPosts[postIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // -------------------------
                        // 댓글 삭제
                        // -------------------------
                        onDeleteCommentClick = { targetPostId, commentId ->

                            // 대상 게시글 찾기
                            val postIndex = communityPosts.indexOfFirst {
                                it.id == targetPostId
                            }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                // 내 댓글이고, 삭제 대상 id인 댓글만 제거
                                val updatedComments = oldPost.comments.filterNot { comment ->
                                    comment.id == commentId &&
                                            comment.authorId == currentUserId
                                }

                                // 댓글 목록, 댓글 수 갱신
                                communityPosts[postIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}