package com.ict.spentopia.navigation

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.data.remote.NonceRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.WalletLinkRequest
import com.ict.spentopia.data.remote.WalletUnlinkRequest
import com.ict.spentopia.feature.analysis.AnalysisScreen
import com.ict.spentopia.feature.auth.FindEmailScreen
import com.ict.spentopia.feature.auth.FindPasswordScreen
import com.ict.spentopia.feature.auth.LoginScreen
import com.ict.spentopia.feature.auth.SplashScreen
import com.ict.spentopia.feature.auth.connector.MwaBackpackConnector
import com.ict.spentopia.feature.auth.connector.MwaPhantomConnector
import com.ict.spentopia.feature.auth.connector.MwaSolflareConnector
import com.ict.spentopia.feature.auth.connector.WalletConnectionResult
import com.ict.spentopia.feature.auth.connector.WalletSignResult
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import com.ict.spentopia.feature.budget.BudgetScreen
import com.ict.spentopia.feature.chatbot.ChatbotScreen
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
    onWalletCallbackConsumed: () -> Unit,
    kakaoCallbackUri: Uri?,
    onKakaoCallbackConsumed: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // SharedPreferences 초기화
    val prefs = remember {
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }

    var walletConnected by remember {
        mutableStateOf(prefs.getBoolean("wallet_connected", false))
    }

    var walletAddress by remember {
        mutableStateOf(prefs.getString("wallet_address", "") ?: "")
    }

    var walletProvider by remember {
        mutableStateOf(prefs.getString("wallet_provider", "") ?: "")
    }

    fun shouldForceLogout(): Boolean = prefs.getBoolean("force_logout", false)

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

    // 로그인 이후 화면에서만 오른쪽 하단 챗봇 버튼을 보여줍니다.
    // splash/login/find 화면과 챗봇 화면 자체에서는 숨깁니다.
    val showChatbotFloatingButtonScreens = setOf(
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

    val shouldShowChatbotFloatingButton = currentRoute in showChatbotFloatingButtonScreens

    // 인증 토큰 저장 함수
    fun saveAuthTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    fun saveWalletInfo(walletAddress: String, walletProvider: String) {
        prefs.edit()
            .putBoolean("wallet_connected", true)
            .putString("wallet_address", walletAddress)
            .putString("wallet_provider", walletProvider)
            .apply()
    }

    fun clearWalletInfo() {
        prefs.edit()
            .remove("wallet_connected")
            .remove("wallet_address")
            .remove("wallet_provider")
            .apply()
    }

    fun startWalletUnlink() {
        val currentWalletAddress = walletAddress
        val accessToken = prefs.getString("access_token", "") ?: ""

        if (currentWalletAddress.isBlank()) {
            Toast.makeText(context, "연결된 지갑 주소가 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (accessToken.isBlank()) {
            Toast.makeText(context, "로그인 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val nonceResponse = RetrofitClient.walletApi.issueWalletNonce(
                    NonceRequest(wallet_address = currentWalletAddress)
                )

                val connector = MwaPhantomConnector()
                val signResult = connector.signMessage(
                    walletActivityResultSender = walletActivityResultSender,
                    message = nonceResponse.message.toByteArray()
                )

                when (signResult) {
                    is WalletSignResult.Success -> {
                        RetrofitClient.walletApi.unlinkWallet(
                            authorization = "Bearer $accessToken",
                            request = WalletUnlinkRequest(
                                wallet_address = currentWalletAddress,
                                nonce = nonceResponse.nonce,
                                signature = signResult.signature
                            )
                        )

                        clearWalletInfo()
                        walletConnected = false
                        walletAddress = ""
                        walletProvider = ""

                        Toast.makeText(context, "지갑 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show()
                    }
                    is WalletSignResult.Failure -> {
                        Toast.makeText(context, signResult.message, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("Spentopia", "지갑 해제 실패", e)
                Toast.makeText(context, e.message ?: "지갑 해제 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun startWalletReconnect(walletType: SolanaWalletType) {
        val accessToken = prefs.getString("access_token", "") ?: ""

        if (accessToken.isBlank()) {
            Toast.makeText(context, "로그인 토큰이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            try {
                val connector = when (walletType) {
                    SolanaWalletType.PHANTOM -> MwaPhantomConnector()
                    SolanaWalletType.SOLFLARE -> MwaSolflareConnector()
                    SolanaWalletType.BACKPACK -> MwaBackpackConnector()
                }

                val connectResult = connector.connect(walletActivityResultSender)
                val newWalletAddress = when (connectResult) {
                    is WalletConnectionResult.Success -> connectResult.walletAddress
                    is WalletConnectionResult.Failure -> {
                        Toast.makeText(context, connectResult.message, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                if (newWalletAddress.isBlank()) {
                    Toast.makeText(context, "지갑 주소를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val nonceResponse = RetrofitClient.walletApi.issueWalletNonce(
                    NonceRequest(wallet_address = newWalletAddress)
                )

                val signResult = connector.signMessage(
                    walletActivityResultSender = walletActivityResultSender,
                    message = nonceResponse.message.toByteArray()
                )

                val signature = when (signResult) {
                    is WalletSignResult.Success -> signResult.signature
                    is WalletSignResult.Failure -> {
                        Toast.makeText(context, signResult.message, Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                }

                val linkResponse = RetrofitClient.walletApi.linkWallet(
                    authorization = "Bearer $accessToken",
                    request = WalletLinkRequest(
                        wallet_address = newWalletAddress,
                        nonce = nonceResponse.nonce,
                        signature = signature
                    )
                )

                saveWalletInfo(
                    walletAddress = linkResponse.wallet_address,
                    walletProvider = walletType.name
                )

                walletConnected = true
                walletAddress = linkResponse.wallet_address
                walletProvider = walletType.name

                Toast.makeText(context, "지갑이 다시 연결되었습니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("Spentopia", "지갑 재연결 실패", e)
                Toast.makeText(context, e.message ?: "지갑 재연결 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearAuthState() {
        prefs.edit()
            .remove("access_token")
            .remove("refresh_token")
            .remove("wallet_connected")
            .remove("wallet_address")
            .remove("wallet_provider")
            .remove("force_logout")
            .remove("user_id")
            .remove("nickname")
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

    LaunchedEffect(Unit) {
        if (shouldForceLogout()) {
            clearAuthState()
            moveToLogin()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = shouldShowDrawer,
        drawerContent = {
            if (shouldShowDrawer) {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    AppDrawerContent(
                        onCloseClick = { scope.launch { drawerState.close() } },
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
                        onProfileAvatarClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.ProfileAvatar.route) { launchSingleTop = true }
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
                        },
                        onLogoutClick = {
                            scope.launch {
                                try {
                                    RetrofitClient.walletApi.logout()
                                } catch (e: Exception) {
                                    Log.e("Spentopia", "로그아웃 API 실패", e)
                                } finally {
                                    clearAuthState()
                                    moveToLogin()
                                }
                            }
                        }
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentColor = MaterialTheme.colorScheme.onBackground,
            topBar = {
                if (shouldShowDrawer) {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = "Spentopia",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "메뉴",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showThemeDialog = true }) {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(onClick = { showNotificationDialog = true }) {
                                Icon(
                                    Icons.Default.NotificationsNone,
                                    contentDescription = "알림",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Route.Splash.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                composable(Route.Splash.route) {
                    SplashScreen(navController)
                }
                composable(Route.Login.route) {
                    LoginScreen(
                        onLoginClick = { moveToHome() },
                        walletActivityResultSender = walletActivityResultSender,
                        walletCallbackUri = walletCallbackUri,
                        onWalletCallbackConsumed = onWalletCallbackConsumed,
                        kakaoCallbackUri = kakaoCallbackUri,
                        onKakaoCallbackConsumed = onKakaoCallbackConsumed,
                        onKakaoClick = {
                            Toast.makeText(context, "카카오 로그인 연결 예정", Toast.LENGTH_SHORT).show()
                        },
                        onGoogleClick = {
                            Toast.makeText(context, "구글 로그인 연결 예정", Toast.LENGTH_SHORT).show()
                        },
                        onFindEmailClick = {
                            navController.navigate(Route.FindEmail.route)
                        },
                        onFindPasswordClick = {
                            navController.navigate(Route.FindPassword.route)
                        },
                        onWalletConnected = { accessToken, refreshToken, newWalletAddress, newWalletProvider ->
                            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) {
                                saveAuthTokens(accessToken, refreshToken)
                                saveWalletInfo(newWalletAddress, newWalletProvider)
                                walletConnected = true
                                walletAddress = newWalletAddress
                                walletProvider = newWalletProvider
                                moveToHome()
                            }
                        }
                    )
                }

                composable(Route.FindEmail.route) {
                    FindEmailScreen(onBackToLoginClick = { navController.popBackStack() })
                }

                composable(Route.FindPassword.route) {
                    FindPasswordScreen(onBackToLoginClick = { navController.popBackStack() })
                }

                composable(Route.Home.route) {
                    HomeScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        onWalletDisconnectClick = { startWalletUnlink() },
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) },
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

                composable(Route.ProfileAvatar.route) {
                    ProfileAvatarScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }
                    )
                }

                composable(Route.Market.route) {
                    MarketScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }
                    )
                }

                composable(Route.Plaza.route) { PlazaScreen() }

                composable(Route.Community.route) {
                    CommunityScreen(
                        posts = communityPosts,
                        onWriteClick = { navController.navigate(Route.CommunityWrite.route) },
                        onChatClick = { navController.navigate(Route.Chatbot.route) },
                        onPostClick = { post ->
                            navController.navigate(Route.CommunityDetail.createRoute(post.id))
                        }
                    )
                }

                composable(Route.Chatbot.route) {
                    ChatbotScreen(
                        onBackClick = { navController.popBackStack() }
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
                                communityPosts[index] = oldPost.copy(
                                    isLiked = !oldPost.isLiked,
                                    likeCount = if (oldPost.isLiked) (oldPost.likeCount - 1).coerceAtLeast(0) else oldPost.likeCount + 1
                                )
                            }
                        }
                    )
                }
            }

                if (shouldShowChatbotFloatingButton) {
                    FloatingChatbotButton(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 18.dp),
                        onClick = {
                            navController.navigate(Route.Chatbot.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }

        if (showThemeDialog) {
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = { Text("화면 설정") },
                text = { Text(if (isDarkTheme) "현재 다크모드가 적용되어 있습니다." else "현재 라이트모드가 적용되어 있습니다.") },
                confirmButton = {
                    TextButton(onClick = { onThemeChange(true); showThemeDialog = false }) { Text("다크모드") }
                },
                dismissButton = {
                    TextButton(onClick = { onThemeChange(false); showThemeDialog = false }) { Text("라이트모드") }
                }
            )
        }

        if (showNotificationDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationDialog = false },
                title = { Text("알림") },
                text = { Text("예산의 80%를 사용했어요!\n5분 전\n\n새로운 아바타를 획득했어요\n1시간 전\n\n7일 연속 기록 달성! 보상이 지급됐어요\n2시간 전") },
                confirmButton = {
                    TextButton(onClick = { showNotificationDialog = false }) { Text("닫기") }
                }
            )
        }
    }
}

@Composable
private fun FloatingChatbotButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.size(62.dp),
        containerColor = Color.Transparent,
        contentColor = Color.White
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF12C2E9),
                            Color(0xFF8B5CF6)
                        )
                    ),
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                color = Color.White
            )
        }
    }
}
