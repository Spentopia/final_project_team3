package com.ict.spentopia.navigation

import android.content.Context
import android.net.Uri
import android.util.Log
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
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.feature.analysis.AnalysisScreen
import com.ict.spentopia.feature.auth.LoginScreen
import com.ict.spentopia.feature.budget.BudgetScreen
import com.ict.spentopia.feature.community.CommunityDetailScreen
import com.ict.spentopia.feature.community.CommunityPost
import com.ict.spentopia.feature.community.CommunityScreen
import com.ict.spentopia.feature.community.CommunityWriteScreen
import com.ict.spentopia.feature.community.getInitialCommunityPosts
import com.ict.spentopia.feature.home.HomeScreen
import com.ict.spentopia.feature.market.MarketScreen
import com.ict.spentopia.feature.mypage.ProfileAvatarScreen
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    walletActivityResultSender: ActivityResultSender,
    walletCallbackUri: Uri?,
    onWalletCallbackConsumed: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val prefs = remember {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    fun hasSavedToken(): Boolean {
        val accessToken = prefs.getString("access_token", null)
        return !accessToken.isNullOrBlank()
    }

    val startRoute = remember {
        if (hasSavedToken()) Route.Home.route else Route.Login.route
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val communityPosts = remember {
        mutableStateListOf<CommunityPost>().apply {
            addAll(getInitialCommunityPosts())
        }
    }

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

    val shouldShowDrawer = currentRoute in showDrawerScreens

    fun saveAuthTokens(
        accessToken: String,
        refreshToken: String
    ) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun saveWalletInfo(
        walletAddress: String,
        walletProvider: String
    ) {
        prefs.edit()
            .putBoolean("wallet_connected", true)
            .putString("wallet_address", walletAddress)
            .putString("wallet_provider", walletProvider)
            .apply()
    }

    fun clearAuthState() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("wallet_connected")
            .remove("wallet_address")
            .remove("wallet_provider")
            .apply()
    }

    fun moveToHome() {
        navController.navigate(Route.Home.route) {
            popUpTo(Route.Login.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    fun moveToLogin() {
        navController.navigate(Route.Login.route) {
            popUpTo(0) { inclusive = true }
            launchSingleTop = true
        }
    }

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
                        onProfileAvatarClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.ProfileAvatar.route) {
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
                        title = { Text("Spentopia") },
                        navigationIcon = {
                            IconButton(onClick = {
                                scope.launch { drawerState.open() }
                            }) {
                                Icon(Icons.Default.Menu, contentDescription = "메뉴")
                            }
                        },
                        actions = {
                            IconButton(onClick = {
                                clearAuthState()
                                moveToLogin()
                            }) {
                                Icon(Icons.Default.Settings, contentDescription = "로그아웃")
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
                startDestination = startRoute,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Route.Login.route) {
                    LoginScreen(
                        onLoginClick = {
                            moveToHome()
                        },
                        walletActivityResultSender = walletActivityResultSender,
                        walletCallbackUri = walletCallbackUri,
                        onWalletCallbackConsumed = onWalletCallbackConsumed,
                        onWalletConnected = { accessToken, refreshToken, walletAddress, walletProvider ->
                            Log.d("Spentopia", "onWalletConnected called")
                            Log.d("Spentopia", "wallet accessToken: '$accessToken'")
                            Log.d("Spentopia", "wallet refreshToken: '$refreshToken'")
                            Log.d("Spentopia", "wallet walletAddress: '$walletAddress'")
                            Log.d("Spentopia", "wallet walletProvider: '$walletProvider'")

                            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                                saveAuthTokens(
                                    accessToken = accessToken,
                                    refreshToken = refreshToken
                                )

                                saveWalletInfo(
                                    walletAddress = walletAddress,
                                    walletProvider = walletProvider
                                )

                                Log.d("Spentopia", "토큰 + 지갑 정보 저장 완료")
                                moveToHome()
                            } else {
                                Log.e("Spentopia", "토큰 없음 - 홈 이동 안 함")
                            }
                        }
                    )
                }

                composable(Route.Home.route) {
                    HomeScreen(
                        isWalletConnected = prefs.getBoolean("wallet_connected", false),
                        walletAddress = prefs.getString("wallet_address", "") ?: "",
                        walletProvider = prefs.getString("wallet_provider", "") ?: "",
                        onLedgerClick = { navController.navigate(Route.Home.route) },
                        onMyPageClick = { navController.navigate(Route.ProfileAvatar.route) },
                        onBudgetClick = { navController.navigate(Route.Budget.route) },
                        onAnalysisClick = { navController.navigate(Route.Analysis.route) },
                        onAvatarClick = { navController.navigate(Route.ProfileAvatar.route) },
                        onMarketClick = { navController.navigate(Route.Market.route) },
                        onPlazaClick = { navController.navigate(Route.Plaza.route) },
                        onCommunityClick = { navController.navigate(Route.Community.route) }
                    )
                }

                composable(Route.Budget.route) { BudgetScreen() }
                composable(Route.Analysis.route) { AnalysisScreen() }
                composable(Route.ProfileAvatar.route) { ProfileAvatarScreen() }

                composable(Route.Market.route) {
                    MarketScreen(
                        isWalletConnected = prefs.getBoolean("wallet_connected", false),
                        walletAddress = prefs.getString("wallet_address", "") ?: "",
                        walletProvider = prefs.getString("wallet_provider", "") ?: ""
                    )
                }

                composable(Route.Plaza.route) { PlazaScreen() }

                composable(Route.Community.route) {
                    CommunityScreen(
                        posts = communityPosts,
                        onWriteClick = { navController.navigate(Route.CommunityWrite.route) },
                        onChatClick = {},
                        onPostClick = { post ->
                            navController.navigate(Route.CommunityDetail.createRoute(post.id))
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
                    arguments = listOf(
                        navArgument("postId") { type = NavType.IntType }
                    )
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getInt("postId") ?: -1
                    val selectedPost = communityPosts.find { it.id == postId }

                    CommunityDetailScreen(
                        post = selectedPost,
                        onBackClick = { navController.popBackStack() },
                        onUpdateClick = { updatedPost ->
                            val index = communityPosts.indexOfFirst { it.id == updatedPost.id }
                            if (index != -1) communityPosts[index] = updatedPost
                        },
                        onDeleteClick = { deletePostId ->
                            val index = communityPosts.indexOfFirst { it.id == deletePostId }
                            if (index != -1) communityPosts.removeAt(index)
                            navController.popBackStack()
                        },
                        onToggleLikeClick = { targetPostId ->
                            val index = communityPosts.indexOfFirst { it.id == targetPostId }
                            if (index != -1) {
                                val oldPost = communityPosts[index]
                                val newIsLiked = !oldPost.isLiked
                                val newLikeCount =
                                    if (oldPost.isLiked) (oldPost.likeCount - 1).coerceAtLeast(0)
                                    else oldPost.likeCount + 1

                                communityPosts[index] = oldPost.copy(
                                    isLiked = newIsLiked,
                                    likeCount = newLikeCount
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}