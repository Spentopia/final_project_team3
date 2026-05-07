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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.spentopia.feature.plaza.PlazaScreen
import com.ict.spentopia.data.remote.NonceRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.data.remote.RefreshTokenRequest
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
import com.ict.spentopia.feature.community.CommunityScreen
import com.ict.spentopia.feature.community.CommunityViewModel
import com.ict.spentopia.feature.community.CommunityWriteScreen
import com.ict.spentopia.feature.home.HomeScreen
import com.ict.spentopia.feature.market.MarketScreen
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
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
    // 앱 화면 전환 중심임
    // 로그인/지갑/테마/드로어/플로팅 버튼 연결
    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var showThemeDialog by remember { mutableStateOf(false) }
    var showNotificationDialog by remember { mutableStateOf(false) }

    // SharedPreferences는 토큰/지갑/강제로그아웃 저장용
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

    val communityViewModel: CommunityViewModel = viewModel()
    val communityUiState by communityViewModel.uiState.collectAsStateWithLifecycle()

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

    // 서버 로그인 토큰 저장
    // AuthInterceptor가 읽어서 Authorization 붙임
    fun saveAuthTokens(accessToken: String, refreshToken: String) {
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    // 지갑 정보는 토큰이랑 별도
    fun saveWalletInfo(walletAddress: String, walletProvider: String) {
        prefs.edit()
            .putBoolean("wallet_connected", true)
            .putString("wallet_address", walletAddress)
            .putString("wallet_provider", walletProvider)
            .apply()
    }

    // 지갑만 해제할 때 씀
    fun clearWalletInfo() {
        prefs.edit()
            .remove("wallet_connected")
            .remove("wallet_address")
            .remove("wallet_provider")
            .apply()
    }

    // 지갑 해제 흐름
    // nonce -> 서명 -> unlink
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

    // 같은 지갑 재연결 흐름
    // 지갑 타입별 connector 다름
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

    // 로그아웃 시 인증 정보만 초기화
    // 테마 설정은 유지함
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

    LaunchedEffect(currentRoute) {
        if (drawerState.isOpen) {
            drawerState.close()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = false,
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
                                    val refreshToken = prefs.getString("refresh_token", "") ?: ""
                                    RetrofitClient.walletApi.logout(
                                        request = RefreshTokenRequest(
                                            refresh_token = refreshToken
                                        )
                                    )
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
                    MarketScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) },
                        onNavigateBack = { navController.popBackStack() },
                        webPath = "/profile",
                        screenTitle = "마이페이지"
                    )
                }

                composable(Route.Market.route) {
                    MarketScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) },
                        onNavigateBack = { navController.popBackStack() },
                        webPath = "/nft-market",
                        screenTitle = "NFT 마켓"
                    )
                }

                composable(Route.Plaza.route) { PlazaScreen() }

                composable(Route.Community.route) {
                    LaunchedEffect(Unit) {
                        communityViewModel.loadPosts()
                    }

                    CommunityScreen(
                        posts = communityUiState.posts,
                        isLoading = communityUiState.isLoading,
                        errorMessage = communityUiState.errorMessage,
                        onRetryClick = {
                            communityViewModel.clearError()
                            communityViewModel.loadPosts()
                        },
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
                            communityViewModel.createPost(
                                category = category,
                                title = title,
                                content = content,
                                onSuccess = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    )
                }

                composable(
                    route = Route.CommunityDetail.route,
                    arguments = listOf(navArgument("postId") {})
                ) { backStackEntry ->
                    val postId = backStackEntry.arguments?.getString("postId").orEmpty()

                    LaunchedEffect(postId) {
                        communityViewModel.loadPostDetail(postId)
                    }

                    CommunityDetailScreen(
                        post = communityUiState.selectedPost?.takeIf { it.id == postId },
                        currentUserId = prefs.getString("user_id", "") ?: "",
                        onBackClick = { navController.popBackStack() },
                        onUpdateClick = { updatedPost ->
                            communityViewModel.updatePost(updatedPost)
                        },
                        onDeleteClick = { deletePostId ->
                            communityViewModel.deletePost(deletePostId) {
                                navController.popBackStack()
                            }
                        },
                        onToggleLikeClick = { targetPostId ->
                            communityViewModel.toggleLike(targetPostId)
                        },
                        onAddCommentClick = { targetPostId, content ->
                            communityViewModel.addComment(targetPostId, content)
                        },
                        onUpdateCommentClick = { targetPostId, commentId, content ->
                            communityViewModel.updateComment(targetPostId, commentId, content)
                        },
                        onDeleteCommentClick = { targetPostId, commentId ->
                            communityViewModel.deleteComment(targetPostId, commentId)
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    FloatingActionButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = MaterialTheme.shapes.extraLarge,
                ambientColor = SpentopiaMutedPurple.copy(alpha = 0.18f),
                spotColor = SpentopiaMutedPurple.copy(alpha = 0.25f)
            )
            .graphicsLayer {
                scaleX = if (pressed) 0.965f else 1f
                scaleY = if (pressed) 0.965f else 1f
            }
            .size(62.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.extraLarge
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🤖",
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
