package com.ict.spentopia.navigation

// ------------------------------------------------------------
// AppNavGraph.kt
// ------------------------------------------------------------
// 이 파일은 앱 전체 네비게이션을 담당합니다.
//
// 이번 버전에서 바뀐 점:
// 1. 새 댓글 등록 시 authorId를 함께 저장합니다.
// 2. 댓글 수정 / 댓글 삭제는 "내 댓글만" 가능하도록 안전 장치를 넣었습니다.
//
// 중요:
// - UI에서 버튼을 숨기는 것만으로 끝내지 않고,
//   실제 데이터 변경 로직에서도 authorId를 검사합니다.
// - 즉, 혹시 UI를 우회하더라도 남의 댓글은 수정/삭제되지 않도록 한 번 더 막습니다.
// ------------------------------------------------------------

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ict.spentopia.feature.analysis.AnalysisScreen
import com.ict.spentopia.feature.avatar.AvatarScreen
import com.ict.spentopia.feature.auth.LoginScreen
import com.ict.spentopia.feature.budget.BudgetScreen
import com.ict.spentopia.feature.community.CommunityCategory
import com.ict.spentopia.feature.community.CommunityComment
import com.ict.spentopia.feature.community.CommunityDetailScreen
import com.ict.spentopia.feature.community.CommunityPost
import com.ict.spentopia.feature.community.CommunityScreen
import com.ict.spentopia.feature.community.CommunityWriteScreen
import com.ict.spentopia.feature.community.getInitialCommunityPosts
import com.ict.spentopia.feature.home.HomeScreen
import com.ict.spentopia.feature.market.MarketScreen
import com.ict.spentopia.feature.mypage.MyPageScreen
import com.ict.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.feature.signup.SignUpStep1Screen
import com.ict.spentopia.feature.signup.SignUpStep2Screen
import com.ict.spentopia.feature.signup.SignUpStep3Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph() {
    // NavigationController입니다.
    val navController = rememberNavController()

    // Drawer 상태입니다.
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // 코루틴 scope입니다.
    val scope = rememberCoroutineScope()

    // 현재 back stack entry 상태입니다.
    val navBackStackEntry = navController.currentBackStackEntryAsState()

    // 현재 route 문자열입니다.
    val currentRoute = navBackStackEntry.value?.destination?.route

    // --------------------------------------------------------
    // 현재 로그인한 사용자라고 가정하는 임시 id입니다.
    // --------------------------------------------------------
    // 아직 실제 로그인 연동 전이기 때문에 문자열로 고정합니다.
    // 나중에 로그인 기능이 붙으면 실제 사용자 id로 바꾸면 됩니다.
    // --------------------------------------------------------
    val currentUserId = "current_user"

    // --------------------------------------------------------
    // 커뮤니티 게시글 목록을 메모리 상태로 관리합니다.
    // --------------------------------------------------------
    val communityPosts = remember {
        mutableStateListOf<CommunityPost>().apply {
            addAll(getInitialCommunityPosts())
        }
    }

    // Drawer를 보여줄 화면 route 집합입니다.
    val showDrawerScreens = setOf(
        Route.Home.route,
        Route.Budget.route,
        Route.Analysis.route,
        Route.Avatar.route,
        Route.Market.route,
        Route.Plaza.route,
        Route.Community.route,
        Route.MyPage.route,
        Route.CommunityWrite.route,
        Route.CommunityDetail.route
    )

    // 현재 route가 Drawer를 보여줄 화면인지 검사합니다.
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
                            navController.navigate(Route.Home.route) {
                                launchSingleTop = true
                            }
                        },
                        onBudgetClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Budget.route) {
                                launchSingleTop = true
                            }
                        },
                        onAnalysisClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Analysis.route) {
                                launchSingleTop = true
                            }
                        },
                        onAvatarClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Avatar.route) {
                                launchSingleTop = true
                            }
                        },
                        onMarketClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Market.route) {
                                launchSingleTop = true
                            }
                        },
                        onPlazaClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Plaza.route) {
                                launchSingleTop = true
                            }
                        },
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
        Scaffold(
            topBar = {
                if (shouldShowDrawer) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text("Spentopia")
                        },
                        navigationIcon = {
                            IconButton(
                                onClick = {
                                    scope.launch { drawerState.open() }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "메뉴"
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정"
                                )
                            }
                            IconButton(onClick = { }) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsNone,
                                    contentDescription = "알림"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors()
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Login.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Route.Login.route) {
                    LoginScreen(
                        onLoginClick = {
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onSignUpClick = {
                            navController.navigate(Route.SignUpStep1.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Route.SignUpStep1.route) {
                    SignUpStep1Screen(
                        onNextClick = {
                            navController.navigate(Route.SignUpStep2.route) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Route.SignUpStep2.route) {
                    SignUpStep2Screen(
                        onNextClick = {
                            navController.navigate(Route.SignUpStep3.route) {
                                launchSingleTop = true
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Route.SignUpStep3.route) {
                    SignUpStep3Screen(
                        onFinishClick = {
                            navController.navigate(Route.Home.route) {
                                popUpTo(Route.Login.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        },
                        onBackClick = {
                            navController.popBackStack()
                        }
                    )
                }

                composable(Route.Home.route) {
                    HomeScreen(
                        onLedgerClick = {
                            navController.navigate(Route.Home.route) {
                                launchSingleTop = true
                            }
                        },
                        onMyPageClick = {
                            navController.navigate(Route.MyPage.route) {
                                launchSingleTop = true
                            }
                        },
                        onBudgetClick = {
                            navController.navigate(Route.Budget.route) {
                                launchSingleTop = true
                            }
                        },
                        onAnalysisClick = {
                            navController.navigate(Route.Analysis.route) {
                                launchSingleTop = true
                            }
                        },
                        onAvatarClick = {
                            navController.navigate(Route.Avatar.route) {
                                launchSingleTop = true
                            }
                        },
                        onMarketClick = {
                            navController.navigate(Route.Market.route) {
                                launchSingleTop = true
                            }
                        },
                        onPlazaClick = {
                            navController.navigate(Route.Plaza.route) {
                                launchSingleTop = true
                            }
                        },
                        onCommunityClick = {
                            navController.navigate(Route.Community.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Route.Budget.route) {
                    BudgetScreen()
                }

                composable(Route.Analysis.route) {
                    AnalysisScreen()
                }

                composable(Route.Avatar.route) {
                    AvatarScreen()
                }

                composable(Route.Market.route) {
                    MarketScreen()
                }

                composable(Route.Plaza.route) {
                    PlazaScreen()
                }

                composable(Route.Community.route) {
                    CommunityScreen(
                        posts = communityPosts,
                        onWriteClick = {
                            navController.navigate(Route.CommunityWrite.route) {
                                launchSingleTop = true
                            }
                        },
                        onChatClick = {
                            println("채팅 시작 클릭됨")
                        },
                        onPostClick = { post ->
                            navController.navigate(
                                Route.CommunityDetail.createRoute(post.id)
                            ) {
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Route.CommunityWrite.route) {
                    CommunityWriteScreen(
                        onBackClick = {
                            navController.popBackStack()
                        },
                        onSubmitClick = { category, title, content ->
                            val nextId = (communityPosts.maxOfOrNull { it.id } ?: 0) + 1

                            val defaultTagText = when (category) {
                                CommunityCategory.AVATAR_CONTEST -> "새아바타글"
                                CommunityCategory.FREE_BOARD -> "새자유글"
                                CommunityCategory.SAVING_TIP -> "새꿀팁글"
                            }

                            val newPost = CommunityPost(
                                id = nextId,
                                title = title,
                                content = content.take(60),
                                fullContent = content,
                                author = "현재사용자",
                                timeText = "방금 전",
                                likeCount = 0,
                                commentCount = 0,
                                tagText = defaultTagText,
                                category = category,
                                comments = emptyList(),
                                isLiked = false
                            )

                            communityPosts.add(0, newPost)
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Route.CommunityDetail.route,
                    arguments = listOf(
                        navArgument("postId") {
                            type = NavType.IntType
                        }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getInt("postId") ?: -1

                    val selectedPost = communityPosts.find { post ->
                        post.id == postId
                    }

                    CommunityDetailScreen(
                        post = selectedPost,
                        onBackClick = {
                            navController.popBackStack()
                        },

                        // 게시글 수정 처리입니다.
                        onUpdateClick = { updatedPost ->
                            val targetIndex = communityPosts.indexOfFirst { post ->
                                post.id == updatedPost.id
                            }

                            if (targetIndex != -1) {
                                communityPosts[targetIndex] = updatedPost
                            }
                        },

                        // 게시글 삭제 처리입니다.
                        onDeleteClick = { deletePostId ->
                            communityPosts.removeAll { post ->
                                post.id == deletePostId
                            }

                            navController.popBackStack()
                        },

                        // 좋아요 처리입니다.
                        onToggleLikeClick = { targetPostId ->
                            val targetIndex = communityPosts.indexOfFirst { post ->
                                post.id == targetPostId
                            }

                            if (targetIndex != -1) {
                                val oldPost = communityPosts[targetIndex]
                                val newLikeState = !oldPost.isLiked

                                val newLikeCount = if (newLikeState) {
                                    oldPost.likeCount + 1
                                } else {
                                    (oldPost.likeCount - 1).coerceAtLeast(0)
                                }

                                communityPosts[targetIndex] = oldPost.copy(
                                    likeCount = newLikeCount,
                                    isLiked = newLikeState
                                )
                            }
                        },

                        // 댓글 추가 처리입니다.
                        onAddCommentClick = { targetPostId, commentContent ->
                            val targetIndex = communityPosts.indexOfFirst { post ->
                                post.id == targetPostId
                            }

                            if (targetIndex != -1) {
                                val oldPost = communityPosts[targetIndex]

                                val nextCommentId =
                                    (oldPost.comments.maxOfOrNull { it.id } ?: 0) + 1

                                val newComment = CommunityComment(
                                    id = nextCommentId,
                                    authorId = currentUserId,
                                    author = "현재사용자",
                                    content = commentContent,
                                    timeText = "방금 전"
                                )

                                val updatedComments = oldPost.comments + newComment

                                communityPosts[targetIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // ----------------------------------------------------
                        // 댓글 수정 처리입니다.
                        // ----------------------------------------------------
                        // 중요:
                        // 1. 해당 게시글을 찾습니다.
                        // 2. 해당 댓글을 찾습니다.
                        // 3. 그 댓글의 authorId가 currentUserId와 같은지 검사합니다.
                        // 4. 내 댓글일 때만 수정합니다.
                        // ----------------------------------------------------
                        onUpdateCommentClick = { targetPostId, commentId, updatedContent ->
                            val targetIndex = communityPosts.indexOfFirst { post ->
                                post.id == targetPostId
                            }

                            if (targetIndex != -1) {
                                val oldPost = communityPosts[targetIndex]

                                val updatedComments = oldPost.comments.map { comment ->
                                    if (
                                        comment.id == commentId &&
                                        comment.authorId == currentUserId
                                    ) {
                                        comment.copy(
                                            content = updatedContent,
                                            timeText = "방금 수정됨"
                                        )
                                    } else {
                                        comment
                                    }
                                }

                                communityPosts[targetIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        },

                        // ----------------------------------------------------
                        // 댓글 삭제 처리입니다.
                        // ----------------------------------------------------
                        // 중요:
                        // authorId가 currentUserId인 댓글만 삭제합니다.
                        // 즉, 내 댓글만 삭제 가능합니다.
                        // ----------------------------------------------------
                        onDeleteCommentClick = { targetPostId, commentId ->
                            val targetIndex = communityPosts.indexOfFirst { post ->
                                post.id == targetPostId
                            }

                            if (targetIndex != -1) {
                                val oldPost = communityPosts[targetIndex]

                                val updatedComments = oldPost.comments.filter { comment ->
                                    // 남길 조건을 쓰는 방식입니다.
                                    // commentId가 다르거나,
                                    // authorId가 currentUserId가 아니면 남깁니다.
                                    !(comment.id == commentId && comment.authorId == currentUserId)
                                }

                                communityPosts[targetIndex] = oldPost.copy(
                                    comments = updatedComments,
                                    commentCount = updatedComments.size
                                )
                            }
                        }
                    )
                }

                composable(Route.MyPage.route) {
                    MyPageScreen()
                }
            }
        }
    }
}