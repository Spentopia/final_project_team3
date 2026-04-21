package com.ict.spentopia.navigation

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
// 수정: LoginViewModel을 composable 안에서 생성하기 위해 viewModel import를 추가합니다.
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument

import com.example.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.feature.analysis.AnalysisScreen
// import com.ict.spentopia.feature.avatar.AvatarScreen // 수정: 개별 아바타 화면 직접 연결 대신 통합 화면에서 사용하므로 여기서는 제거합니다.
import com.ict.spentopia.feature.auth.LoginScreen
// 수정: 지갑 저장 로직을 호출할 LoginViewModel import를 추가합니다.
import com.ict.spentopia.feature.auth.LoginViewModel
import com.ict.spentopia.feature.budget.BudgetScreen
import com.ict.spentopia.feature.community.*
import com.ict.spentopia.feature.home.HomeScreen
import com.ict.spentopia.feature.market.MarketScreen
// import com.ict.spentopia.feature.mypage.MyPageScreen // 수정: 개별 마이페이지 화면 직접 연결 대신 통합 화면에서 사용하므로 여기서는 제거합니다.
import com.ict.spentopia.feature.mypage.ProfileAvatarScreen // 수정: 마이페이지/내 아바타 통합 화면 import를 추가합니다.
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    walletActivityResultSender: ActivityResultSender
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // ------------------------------------------------------------
    // 현재 로그인 사용자 id라고 가정하는 임시 값입니다.
    // 아직 로그인 사용자 정보 연결 전이므로 문자열로만 사용합니다.
    // 나중에는 실제 로그인한 사용자 id로 바꿔주면 됩니다.
    // ------------------------------------------------------------
    val currentUserId = "current_user"

    // ------------------------------------------------------------
    // communityPosts:
    // 커뮤니티 게시글 전체 상태를 이곳에서 관리합니다.
    //
    // 왜 여기서 관리하나요?
    // - CommunityScreen은 목록을 보여주는 화면
    // - CommunityDetailScreen은 상세를 보여주는 화면
    // - CommunityWriteScreen은 새 글을 입력하는 화면
    //
    // 이 3개 화면이 같은 게시글 데이터를 함께 써야 하므로
    // 공통 부모인 AppNavGraph에서 상태를 들고 있어야 합니다.
    // ------------------------------------------------------------
    val communityPosts = remember {
        mutableStateListOf<CommunityPost>().apply {
            addAll(getInitialCommunityPosts())
        }
    }

    val showDrawerScreens = setOf(
        Route.Home.route,
        Route.Budget.route,
        Route.Analysis.route,
        Route.ProfileAvatar.route, // 수정: 아바타/마이페이지 개별 route 대신 통합 route를 Drawer 노출 대상에 추가합니다.
        Route.Market.route,
        Route.Plaza.route,
        Route.Community.route,
        Route.CommunityWrite.route,
        Route.CommunityDetail.route
    )

    val shouldShowDrawer = currentRoute in showDrawerScreens

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = shouldShowDrawer,
        drawerContent = {
            if (shouldShowDrawer) {
                ModalDrawerSheet {
                    AppDrawerContent(
                        onLedgerClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Home.route) { launchSingleTop = true }
                        },
                        onBudgetClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Budget.route) { launchSingleTop = true }
                        },
                        onAnalysisClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Analysis.route) { launchSingleTop = true }
                        },
                        onProfileAvatarClick = { // 수정: 마이페이지/내 아바타 통합 메뉴 클릭 콜백으로 변경합니다.
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.ProfileAvatar.route) { launchSingleTop = true } // 수정: 통합 화면 route로 이동합니다.
                        },
                        onMarketClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Market.route) { launchSingleTop = true }
                        },
                        onPlazaClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Plaza.route) { launchSingleTop = true }
                        },
                        onCommunityClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Community.route) { launchSingleTop = true }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                if (shouldShowDrawer) {
                    CenterAlignedTopAppBar(
                        title = { Text("Spentopia") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "메뉴")
                            }
                        },
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

            NavHost(
                navController = navController,
                startDestination = Route.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {

                // ================================
                // 🔥 로그인 화면 (수정 완료)
                // ================================
                composable(Route.Login.route) {
                    // 수정: 로그인 화면에서 사용할 LoginViewModel을 생성합니다.
                    val loginViewModel: LoginViewModel = viewModel()

                    LoginScreen(
                        onLoginClick = {
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },

                        // ❌ 회원가입 완전 제거
                        walletActivityResultSender = walletActivityResultSender,

                        // 🔥 지갑 주소 + 지갑 종류 같이 받음
                        onWalletConnected = { walletAddress, walletProvider ->

                            Log.d("Spentopia", "지갑 주소: $walletAddress")
                            Log.d("Spentopia", "지갑 종류: $walletProvider")

                            if (walletAddress.isNotBlank()) {
                                // 수정: 지갑 주소와 지갑 종류를 DataStore에 저장한 뒤 홈으로 이동합니다.
                                loginViewModel.saveWalletSession(
                                    walletAddress = walletAddress,
                                    walletProvider = walletProvider,
                                    onSuccess = {
                                        navController.navigate(Route.Home.route) {
                                            popUpTo(Route.Login.route) { inclusive = true }
                                            launchSingleTop = true
                                        }
                                    }
                                )
                            } else {
                                Log.e("Spentopia", "지갑 주소 없음")
                            }
                        }
                    )
                }

                // ================================
                // 홈
                // ================================
                composable(Route.Home.route) {
                    HomeScreen(
                        onLedgerClick = { navController.navigate(Route.Home.route) },
                        onMyPageClick = { navController.navigate(Route.ProfileAvatar.route) }, // 수정: 홈에서 마이페이지 진입도 통합 화면으로 연결합니다.
                        onBudgetClick = { navController.navigate(Route.Budget.route) },
                        onAnalysisClick = { navController.navigate(Route.Analysis.route) },
                        onAvatarClick = { navController.navigate(Route.ProfileAvatar.route) }, // 수정: 홈에서 아바타 진입도 통합 화면으로 연결합니다.
                        onMarketClick = { navController.navigate(Route.Market.route) },
                        onPlazaClick = { navController.navigate(Route.Plaza.route) },
                        onCommunityClick = { navController.navigate(Route.Community.route) }
                    )
                }

                composable(Route.Budget.route) { BudgetScreen() }
                composable(Route.Analysis.route) { AnalysisScreen() }
                composable(Route.ProfileAvatar.route) { ProfileAvatarScreen() } // 수정: 마이페이지/내 아바타 통합 화면을 등록합니다.
                composable(Route.Market.route) { MarketScreen() }
                composable(Route.Plaza.route) { PlazaScreen() }

                // ================================
                // 커뮤니티 메인 화면
                // ================================
                composable(Route.Community.route) {
                    CommunityScreen(
                        posts = communityPosts,
                        onWriteClick = {
                            navController.navigate(Route.CommunityWrite.route)
                        },
                        onChatClick = { },
                        onPostClick = { post ->
                            navController.navigate(
                                Route.CommunityDetail.createRoute(post.id)
                            )
                        }
                    )
                }

                // ================================
                // 커뮤니티 글쓰기 화면
                // ================================
                composable(Route.CommunityWrite.route) {
                    CommunityWriteScreen(
                        onBackClick = { navController.popBackStack() },
                        onSubmitClick = { category, title, content ->
                            // 새 게시글 id를 만들기 위해 현재 최대 id + 1 을 사용합니다.
                            val nextId = (communityPosts.maxOfOrNull { it.id } ?: 0) + 1

                            val newPost = CommunityPost(
                                id = nextId,
                                title = title,
                                content = content.take(60),
                                fullContent = content,
                                author = "현재사용자",
                                timeText = "방금 전",
                                likeCount = 0,
                                commentCount = 0,
                                tagText = "새글",
                                category = category,
                                comments = emptyList(),
                                isLiked = false
                            )

                            // 맨 앞에 새 글을 추가합니다.
                            communityPosts.add(0, newPost)

                            // 글 작성이 끝나면 이전 화면으로 돌아갑니다.
                            navController.popBackStack()
                        }
                    )
                }

                // ================================
                // 커뮤니티 상세 화면
                // ================================
                composable(
                    route = Route.CommunityDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.IntType })
                ) { backStackEntry ->

                    val postId = backStackEntry.arguments?.getInt("postId") ?: -1

                    // ------------------------------------------------------------
                    // selectedPost:
                    // 현재 선택한 게시글 id에 맞는 "최신 게시글"을
                    // communityPosts 리스트에서 다시 찾습니다.
                    //
                    // - 좋아요/댓글/수정/삭제가 일어나면 communityPosts가 바뀝니다.
                    // - 그러면 여기서 다시 찾은 selectedPost도 최신값이 됩니다.
                    // - 그래서 상세 화면이 자동으로 최신 데이터로 다시 그려집니다.
                    // ------------------------------------------------------------
                    val selectedPost = communityPosts.find { it.id == postId }

                    CommunityDetailScreen(
                        post = selectedPost,
                        onBackClick = { navController.popBackStack() },

                        // --------------------------------------------------------
                        // 게시글 수정
                        // --------------------------------------------------------
                        onUpdateClick = { updatedPost ->
                            val index = communityPosts.indexOfFirst { it.id == updatedPost.id }

                            if (index != -1) {
                                // 기존 댓글/좋아요 상태가 유지되도록
                                // 넘어온 updatedPost로 해당 위치만 교체합니다.
                                communityPosts[index] = updatedPost
                            }
                        },

                        // --------------------------------------------------------
                        // 게시글 삭제
                        // --------------------------------------------------------
                        onDeleteClick = { deletePostId ->
                            val index = communityPosts.indexOfFirst { it.id == deletePostId }

                            if (index != -1) {
                                communityPosts.removeAt(index)
                            }

                            // 삭제 후에는 상세 화면에 남아 있을 수 없으므로 뒤로 이동합니다.
                            navController.popBackStack()
                        },

                        // --------------------------------------------------------
                        // 좋아요 토글
                        // --------------------------------------------------------
                        onToggleLikeClick = { targetPostId ->
                            val index = communityPosts.indexOfFirst { it.id == targetPostId }

                            if (index != -1) {
                                val oldPost = communityPosts[index]

                                val newIsLiked = !oldPost.isLiked

                                // 초보자용 설명:
                                // 이미 좋아요를 누른 상태였다면 취소이므로 -1
                                // 아직 안 눌렀다면 새로 누르는 것이므로 +1
                                val newLikeCount = if (oldPost.isLiked) {
                                    (oldPost.likeCount - 1).coerceAtLeast(0)
                                } else {
                                    oldPost.likeCount + 1
                                }

                                communityPosts[index] = oldPost.copy(
                                    isLiked = newIsLiked,
                                    likeCount = newLikeCount
                                )
                            }
                        },

                        // --------------------------------------------------------
                        // 댓글 추가
                        // --------------------------------------------------------
                        onAddCommentClick = { targetPostId, newCommentText ->
                            val postIndex = communityPosts.indexOfFirst { it.id == targetPostId }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                // 댓글 id는 현재 댓글들 중 최대값 + 1
                                val nextCommentId =
                                    (oldPost.comments.maxOfOrNull { it.id } ?: 0) + 1

                                val newComment = CommunityComment(
                                    id = nextCommentId,
                                    authorId = currentUserId,
                                    author = "현재사용자",
                                    content = newCommentText,
                                    timeText = "방금 전"
                                )

                                val updatedComments = oldPost.comments + newComment

                                communityPosts[postIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // --------------------------------------------------------
                        // 댓글 수정
                        // --------------------------------------------------------
                        onUpdateCommentClick = { targetPostId, commentId, updatedText ->
                            val postIndex = communityPosts.indexOfFirst { it.id == targetPostId }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                val updatedComments = oldPost.comments.map { comment ->
                                    if (comment.id == commentId && comment.authorId == currentUserId) {
                                        comment.copy(
                                            content = updatedText,
                                            timeText = "방금 전"
                                        )
                                    } else {
                                        comment
                                    }
                                }

                                communityPosts[postIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // --------------------------------------------------------
                        // 댓글 삭제
                        // --------------------------------------------------------
                        onDeleteCommentClick = { targetPostId, commentId ->
                            val postIndex = communityPosts.indexOfFirst { it.id == targetPostId }

                            if (postIndex != -1) {
                                val oldPost = communityPosts[postIndex]

                                // 내 댓글만 삭제되도록 한 번 더 안전하게 검사합니다.
                                val updatedComments = oldPost.comments.filterNot { comment ->
                                    comment.id == commentId && comment.authorId == currentUserId
                                }

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