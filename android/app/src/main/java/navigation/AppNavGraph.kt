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

    val currentUserId = "current_user"

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
                // 커뮤니티
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

                composable(Route.CommunityWrite.route) {
                    CommunityWriteScreen(
                        onBackClick = { navController.popBackStack() },
                        onSubmitClick = { category, title, content ->
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

                            communityPosts.add(0, newPost)
                            navController.popBackStack()
                        }
                    )
                }

                composable(
                    route = Route.CommunityDetail.route,
                    arguments = listOf(navArgument("postId") { type = NavType.IntType })
                ) { backStackEntry ->

                    val postId = backStackEntry.arguments?.getInt("postId") ?: -1
                    val selectedPost = communityPosts.find { it.id == postId }

                    CommunityDetailScreen(
                        post = selectedPost,
                        onBackClick = { navController.popBackStack() },
                        onUpdateClick = {},
                        onDeleteClick = {},
                        onToggleLikeClick = {},
                        onAddCommentClick = { _, _ -> },
                        onUpdateCommentClick = { _, _, _ -> },
                        onDeleteCommentClick = { _, _ -> }
                    )
                }
            }
        }
    }
}