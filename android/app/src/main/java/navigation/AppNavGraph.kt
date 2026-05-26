package com.ict.spentopia.navigation // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.rememberScrollState // rememberScrollState 기능을 가져옴
import androidx.compose.foundation.verticalScroll // verticalScroll 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.navigationBarsPadding // navigationBarsPadding 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.Menu // Menu 기능을 가져옴
import androidx.compose.material.icons.filled.NotificationsNone // NotificationsNone 기능을 가져옴
import androidx.compose.material.icons.filled.Settings // Settings 기능을 가져옴
import androidx.compose.material3.AlertDialog // AlertDialog 기능을 가져옴
import androidx.compose.material3.CenterAlignedTopAppBar // CenterAlignedTopAppBar 기능을 가져옴
import androidx.compose.material3.DrawerValue // DrawerValue 기능을 가져옴
import androidx.compose.material3.ExperimentalMaterial3Api // ExperimentalMaterial3Api 기능을 가져옴
import androidx.compose.material3.FloatingActionButton // FloatingActionButton 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.IconButton // 아이콘 버튼 컴포넌트를 가져옴
import androidx.compose.material3.CircularProgressIndicator // 로딩 표시 컴포넌트를 가져옴
import androidx.compose.material3.HorizontalDivider // 구분선을 가져옴
import androidx.compose.material3.ModalDrawerSheet // ModalDrawerSheet 기능을 가져옴
import androidx.compose.material3.ModalNavigationDrawer // ModalNavigationDrawer 기능을 가져옴
import androidx.compose.material3.Scaffold // Scaffold 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Switch // Switch 기능을 가져옴
import androidx.compose.material3.SwitchDefaults // SwitchDefaults 기능을 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴
import androidx.compose.material3.TopAppBarDefaults // TopAppBarDefaults 기능을 가져옴
import androidx.compose.material3.rememberDrawerState // rememberDrawerState 기능을 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.rememberCoroutineScope // rememberCoroutineScope 기능을 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.draw.shadow // shadow 기능을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.lifecycle.compose.collectAsStateWithLifecycle // ViewModel 상태를 화면에서 안전하게 받는 도구를 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import androidx.navigation.compose.NavHost // NavHost 기능을 가져옴
import androidx.navigation.compose.composable // composable 기능을 가져옴
import androidx.navigation.compose.currentBackStackEntryAsState // currentBackStackEntryAsState 기능을 가져옴
import androidx.navigation.compose.rememberNavController // rememberNavController 기능을 가져옴
import androidx.navigation.NavType // NavType 기능을 가져옴
import androidx.navigation.navArgument // navArgument 기능을 가져옴
import com.example.spentopia.feature.plaza.PlazaScreen // PlazaScreen 기능을 가져옴
import com.ict.spentopia.data.remote.NonceRequest // NonceRequest 기능을 가져옴
import com.ict.spentopia.data.remote.NotificationResponse // NotificationResponse 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.RefreshTokenRequest // RefreshTokenRequest 기능을 가져옴
import com.ict.spentopia.data.remote.UpdateUserSettingsRequest // UpdateUserSettingsRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLinkRequest // WalletLinkRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletUnlinkRequest // WalletUnlinkRequest 기능을 가져옴
import com.ict.spentopia.feature.analysis.AnalysisScreen // AnalysisScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.FindEmailScreen // FindEmailScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.FindPasswordScreen // FindPasswordScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.LoginScreen // LoginScreen 기능을 가져옴
import com.ict.spentopia.feature.splash.SplashScreen // SplashScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaBackpackConnector // MwaBackpackConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaPhantomConnector // MwaPhantomConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaSolflareConnector // MwaSolflareConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.PhantomDeepLinkConnector // Phantom 딥링크 도구를 가져옴
import com.ict.spentopia.feature.auth.connector.SolflareDeepLinkConnector // Solflare 딥링크 도구를 가져옴
import com.ict.spentopia.feature.auth.connector.WalletConnectionResult // WalletConnectionResult 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.WalletSignResult // WalletSignResult 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.ict.spentopia.feature.budget.BudgetScreen // BudgetScreen 기능을 가져옴
import com.ict.spentopia.feature.chatbot.ChatbotScreen // ChatbotScreen 기능을 가져옴
import com.ict.spentopia.feature.community.CommunityCategory // CommunityCategory 기능을 가져옴
import com.ict.spentopia.feature.community.CommunityScreen // CommunityScreen 기능을 가져옴
import com.ict.spentopia.feature.community.CommunityViewModel // CommunityViewModel 기능을 가져옴
import com.ict.spentopia.feature.community.CommunityWriteScreen // CommunityWriteScreen 기능을 가져옴
import com.ict.spentopia.feature.home.HomeScreen // HomeScreen 기능을 가져옴
import com.ict.spentopia.feature.market.MarketScreen // MarketScreen 기능을 가져옴
import com.ict.spentopia.feature.mypage.ProfileAvatarScreen // ProfileAvatarScreen 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaDarkBackground
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // ActivityResultSender 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴


@OptIn(ExperimentalMaterial3Api::class) // 이 코드에 특별한 역할을 붙이는 표시
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AppNavGraph( // AppNavGraph 함수를 선언함
    walletActivityResultSender: ActivityResultSender, // 지갑 관련 값을 받음
    walletCallbackUri: Uri?, // 지갑 관련 값을 받음
    onWalletCallbackConsumed: () -> Unit, // 지갑 관련 값을 받음
    kakaoCallbackUri: Uri?, // kakaoCallbackUri 값을 받음
    onKakaoCallbackConsumed: () -> Unit, // onKakaoCallbackConsumed 때 실행할 함수를 받음
    isDarkTheme: Boolean, // isDarkTheme인지 여부를 받음
    onThemeChange: (Boolean) -> Unit // onThemeChange 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    // 앱 화면 전환 중심임
    // 로그인/지갑/테마/드로어/플로팅 버튼 연결
    val context = LocalContext.current // 현재 화면 정보를 저장함
    val navController = rememberNavController() // 화면이 다시 그려져도 화면 이동 도구를 기억함
    val drawerState = rememberDrawerState(DrawerValue.Closed) // 화면이 다시 그려져도 drawerState 값을 기억함
    val scope = rememberCoroutineScope() // 화면이 다시 그려져도 코루틴 실행 범위을 기억함
    val phantomDeepLinkConnector = remember { PhantomDeepLinkConnector(context) } // Phantom 딥링크 연결 도구를 기억함
    val solflareDeepLinkConnector = remember { SolflareDeepLinkConnector(context) } // Solflare 딥링크 연결 도구를 기억함

    var showThemeDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 showThemeDialog 값을 저장함
    var showNotificationDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 showNotificationDialog 값을 저장함
    var notifications by remember { mutableStateOf<List<NotificationResponse>>(emptyList()) } // 서버에서 받아온 알림 목록을 저장함
    var notificationsLoading by remember { mutableStateOf(false) } // 알림을 불러오는 중인지 저장함
    var notificationsError by remember { mutableStateOf<String?>(null) } // 알림 조회 실패 문구를 저장함
    var notificationEnabled by remember { mutableStateOf(true) } // 전체 알림 수신 여부를 저장함
    var notificationSettingLoading by remember { mutableStateOf(false) } // 알림 설정 저장/조회 중인지 저장함

    // SharedPreferences는 토큰/지갑/강제로그아웃 저장용
    val prefs = remember { // 화면이 다시 그려져도 간단 저장소를 기억함
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    }
    val initialDestination = remember {
        val isAuthCallbackEntry = walletCallbackUri != null || kakaoCallbackUri != null
        val hasSavedSession = !prefs.getString("access_token", "").isNullOrBlank()
        when {
            !isAuthCallbackEntry -> Route.Splash.route
            hasSavedSession -> Route.Home.route
            else -> Route.Login.route
        }
    }

    var walletConnected by remember { // 화면이 다시 그려져도 지갑 관련 값을 기억함
        mutableStateOf(prefs.getBoolean("wallet_connected", false)) // 화면 상태값을 만듦
    }

    var walletAddress by remember { // 화면이 다시 그려져도 지갑 주소를 기억함
        mutableStateOf(prefs.getString("wallet_address", "") ?: "") // 화면 상태값을 만듦
    }

    var walletProvider by remember { // 화면이 다시 그려져도 지갑 이름을 기억함
        mutableStateOf(prefs.getString("wallet_provider", "") ?: "") // 화면 상태값을 만듦
    }

    var pendingReconnectWallet by remember { mutableStateOf<SolanaWalletType?>(null) } // 딥링크 지갑 재연결 상태를 저장함
    var pendingReconnectWalletAddress by remember { mutableStateOf<String?>(null) } // 딥링크 재연결 지갑 주소를 저장함
    var pendingReconnectNonce by remember { mutableStateOf<String?>(null) } // 딥링크 재연결 nonce를 저장함

    fun shouldForceLogout(): Boolean = prefs.getBoolean("force_logout", false) // shouldForceLogout 함수를 선언함

    fun loadNotifications() { // 백엔드에서 내 알림 목록을 불러오는 함수
        scope.launch {
            notificationsLoading = true // 로딩 화면을 보여주기 위해 true로 바꿈
            notificationsError = null // 이전 오류 메시지를 지움
            try {
                notifications = RetrofitClient.notificationApi.getNotifications() // 실제 서버 알림 목록을 받아옴
            } catch (e: Exception) {
                Log.e("SpentopiaNotification", "알림 조회 실패", e)
                notificationsError = "알림을 불러오지 못했습니다." // 화면에 보여줄 오류 문구를 저장함
            } finally {
                notificationsLoading = false // 로딩 상태를 끝냄
            }
        }
    }

    fun loadNotificationSetting() { // 알림 팝업 상단의 전체 알림 수신 값을 불러오는 함수
        scope.launch {
            notificationSettingLoading = true // 설정 조회 중임을 표시함
            try {
                val settings = RetrofitClient.userSettingsApi.getSettings() // 서버에 저장된 알림 설정을 가져옴
                notificationEnabled = settings.notification_listener ?: true // 값이 없으면 기본 ON으로 보여줌
            } catch (e: Exception) {
                Log.e("SpentopiaNotification", "알림 설정 조회 실패", e)
            } finally {
                notificationSettingLoading = false // 설정 조회 상태를 끝냄
            }
        }
    }

    fun updateNotificationEnabled(enabled: Boolean) { // 전체 알림 수신 여부를 서버에 저장하는 함수
        notificationEnabled = enabled // 먼저 화면 스위치를 바로 바꿔줌
        scope.launch {
            notificationSettingLoading = true // 저장 중임을 표시함
            try {
                val updated = RetrofitClient.userSettingsApi.updateSettings( // 전체 알림 수신 여부만 서버에 보냄
                    UpdateUserSettingsRequest(
                        alert_budget = null,
                        alert_reward = null,
                        alert_streak = null,
                        notification_listener = enabled
                    )
                )
                notificationEnabled = updated.notification_listener ?: enabled // 서버 최종 값을 화면에 반영함
            } catch (e: Exception) {
                notificationEnabled = !enabled // 실패하면 이전 상태로 되돌림
                Log.e("SpentopiaNotification", "알림 설정 저장 실패", e)
                showAppToast(context, "알림 설정 저장에 실패했습니다.")
            } finally {
                notificationSettingLoading = false // 저장 상태를 끝냄
            }
        }
    }

    fun readNotification(notificationId: String) { // 알림 1개를 서버에 읽음 처리하는 함수
        scope.launch {
            try {
                RetrofitClient.notificationApi.readNotification(notificationId) // 서버에 읽음 처리 요청을 보냄
                notifications = notifications.map { item ->
                    if (item.id == notificationId) item.copy(is_read = true) else item // 화면 목록도 읽음 상태로 바꿈
                }
            } catch (e: Exception) {
                Log.e("SpentopiaNotification", "알림 읽음 처리 실패", e)
                showAppToast(context, "알림 읽음 처리에 실패했습니다.")
            }
        }
    }

    fun readAllNotifications() { // 모든 알림을 서버에 읽음 처리하는 함수
        scope.launch {
            try {
                RetrofitClient.notificationApi.readAllNotifications() // 서버에 전체 읽음 처리 요청을 보냄
                notifications = notifications.map { it.copy(is_read = true) } // 화면 목록도 모두 읽음 상태로 바꿈
            } catch (e: Exception) {
                Log.e("SpentopiaNotification", "전체 알림 읽음 처리 실패", e)
                showAppToast(context, "전체 읽음 처리에 실패했습니다.")
            }
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState() // navBackStackEntry 값을 저장함
    val currentRoute = navBackStackEntry?.destination?.route // currentRoute 값을 저장함

    LaunchedEffect(walletCallbackUri, currentRoute) {
        val uri = walletCallbackUri ?: return@LaunchedEffect
        if (uri.scheme != "spentopia" || uri.host != "wallet-callback") return@LaunchedEffect
        if (currentRoute == Route.Analysis.route) return@LaunchedEffect

        val paymentPrefs = context.getSharedPreferences("analysis_payment_prefs", Context.MODE_PRIVATE)
        val hasPendingPayment = !paymentPrefs
            .getString("pending_payment_wallet_address", "")
            .isNullOrBlank() &&
            !paymentPrefs
                .getString("pending_payment_network", "")
                .isNullOrBlank()

        if (hasPendingPayment) {
            Log.d("SpentopiaPayment", "pending payment callback routed to analysis")
            navController.navigate(Route.Analysis.route) {
                launchSingleTop = true
            }
        }
    }

    val communityViewModel: CommunityViewModel = viewModel() // 커뮤니티 관련 값을 저장함
    val communityUiState by communityViewModel.uiState.collectAsStateWithLifecycle() // 커뮤니티 관련 값을 저장함

    val showDrawerScreens = setOf( // showDrawerScreens 값을 저장함
        Route.Home.route,
        Route.Budget.route,
        Route.Analysis.route,
        Route.ProfileAvatar.route,
        Route.Market.route,
        Route.Plaza.route,
        Route.Community.route,
        Route.CommunityWrite.route
    )

    val shouldShowDrawer = currentRoute in showDrawerScreens // shouldShowDrawer 값을 저장함

    // 로그인 이후 화면에서만 오른쪽 하단 챗봇 버튼을 보여줍니다.
    // splash/login/find 화면과 챗봇 화면 자체에서는 숨깁니다.
    val showChatbotFloatingButtonScreens = setOf( // 채팅 관련 값을 저장함
        Route.Home.route,
        Route.Budget.route,
        Route.Analysis.route,
        Route.ProfileAvatar.route,
        Route.Market.route,
        Route.Plaza.route
    )

    val shouldShowChatbotFloatingButton = currentRoute in showChatbotFloatingButtonScreens // 채팅 관련 값을 저장함

    // 서버 로그인 토큰 저장
    // AuthInterceptor가 읽어서 Authorization 붙임
    fun saveAuthTokens(accessToken: String, refreshToken: String) { // 데이터를 저장하는 함수 시작
        prefs.edit()
            .putString("access_token", accessToken)
            .putString("refresh_token", refreshToken)
            .apply()
    }

    // 지갑 정보는 토큰이랑 별도
    fun saveWalletInfo(walletAddress: String, walletProvider: String) { // 데이터를 저장하는 함수 시작
        prefs.edit()
            .putBoolean("wallet_connected", true)
            .putString("wallet_address", walletAddress)
            .putString("wallet_provider", walletProvider)
            .apply()
    }

    // 지갑만 해제할 때 씀
    fun clearWalletInfo() { // clearWalletInfo 함수를 선언함
        prefs.edit()
            .remove("wallet_connected")
            .remove("wallet_address")
            .remove("wallet_provider")
            .apply()
    }

    // 지갑 해제 흐름
    // nonce -> 서명 -> unlink
    fun startWalletUnlink() { // startWalletUnlink 함수를 선언함
        val currentWalletAddress = walletAddress // 지갑 관련 값을 저장함
        val accessToken = prefs.getString("access_token", "") ?: "" // 접근 토큰을 저장함

        if (currentWalletAddress.isBlank()) { // 조건이 맞는지 확인함
            showAppToast(context, "연결된 지갑 주소가 없습니다.") // 화면에 글자를 보여줌
            return
        }

        if (accessToken.isBlank()) { // 조건이 맞는지 확인함
            showAppToast(context, "로그인 토큰이 없습니다.") // 화면에 글자를 보여줌
            return
        }

        scope.launch { // 이 블록 안의 내용이 시작됨
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val nonceResponse = RetrofitClient.walletApi.issueWalletNonce( // nonceResponse 값을 저장함
                    NonceRequest(wallet_address = currentWalletAddress) // 지갑 관련 값을 정해줌
                )

                val connector = MwaPhantomConnector() // connector 값을 저장함
                val signResult = connector.signMessage( // signResult 값을 저장함
                    walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
                    message = nonceResponse.message.toByteArray() // 메시지를 정해줌
                )

                when (signResult) { // 값 종류에 따라 실행할 코드를 나눔
                    is WalletSignResult.Success -> { // 이 블록 안의 내용이 시작됨
                        RetrofitClient.walletApi.unlinkWallet( // 서버 통신 도구를 설정함
                            authorization = "Bearer $accessToken", // authorization 값을 정해줌
                            request = WalletUnlinkRequest( // 서버 요청값을 정해줌
                                wallet_address = currentWalletAddress, // 지갑 값을 요청값에 넣음
                                nonce = nonceResponse.nonce, // 서명용 난수를 정해줌
                                signature = signResult.signature // 지갑 서명값을 정해줌
                            )
                        )

                        clearWalletInfo() // 저장된 지갑 정보를 지움
                        walletConnected = false // false 값을 지갑 관련 값에 넣음
                        walletAddress = "" // 지갑 주소를 정해줌
                        walletProvider = "" // 지갑 이름을 정해줌

                        showAppToast(context, "지갑 연결이 해제되었습니다.") // 화면에 글자를 보여줌
                    }
                    is WalletSignResult.Failure -> { // 이 블록 안의 내용이 시작됨
                        showAppToast(context, signResult.message) // 화면에 글자를 보여줌
                    }
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "지갑 해제 실패", e) // 개발자가 확인할 로그를 찍음
                showAppToast(context, e.message ?: "지갑 해제 실패") // 화면에 글자를 보여줌
            }
        }
    }

    // 같은 지갑 재연결 흐름
    // 지갑 타입별 connector 다름
    fun startWalletReconnect(walletType: SolanaWalletType) { // startWalletReconnect 함수를 선언함
        val accessToken = prefs.getString("access_token", "") ?: "" // 접근 토큰을 저장함

        if (accessToken.isBlank()) { // 조건이 맞는지 확인함
            showAppToast(context, "로그인 토큰이 없습니다.") // 화면에 글자를 보여줌
            return
        }

        if (walletType == SolanaWalletType.PHANTOM || walletType == SolanaWalletType.SOLFLARE) {
            pendingReconnectWallet = walletType
            pendingReconnectWalletAddress = null
            pendingReconnectNonce = null
            phantomDeepLinkConnector.clearPendingLogin()
            solflareDeepLinkConnector.clearPendingLogin()
            val opened = if (walletType == SolanaWalletType.PHANTOM) {
                phantomDeepLinkConnector.connect()
            } else {
                solflareDeepLinkConnector.connect()
            }
            if (!opened) {
                pendingReconnectWallet = null
                val walletName = if (walletType == SolanaWalletType.PHANTOM) "Phantom" else "Solflare"
                showAppToast(context, "${walletName} 지갑 앱을 찾을 수 없습니다.")
            }
            return
        }

        scope.launch { // 이 블록 안의 내용이 시작됨
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val connector = when (walletType) { // connector 값을 저장함
                    SolanaWalletType.PHANTOM -> MwaPhantomConnector()
                    SolanaWalletType.SOLFLARE -> MwaSolflareConnector()
                    SolanaWalletType.BACKPACK -> MwaBackpackConnector()
                }

                val connectResult = connector.connect(walletActivityResultSender) // connectResult 값을 저장함
                val newWalletAddress = when (connectResult) { // 지갑 관련 값을 저장함
                    is WalletConnectionResult.Success -> connectResult.walletAddress
                    is WalletConnectionResult.Failure -> { // 이 블록 안의 내용이 시작됨
                        showAppToast(context, connectResult.message) // 화면에 글자를 보여줌
                        return@launch
                    }
                }

                if (newWalletAddress.isBlank()) { // 조건이 맞는지 확인함
                    showAppToast(context, "지갑 주소를 가져오지 못했습니다.") // 화면에 글자를 보여줌
                    return@launch
                }

                val nonceResponse = RetrofitClient.walletApi.issueWalletNonce( // nonceResponse 값을 저장함
                    NonceRequest(wallet_address = newWalletAddress) // 지갑 관련 값을 정해줌
                )

                val signResult = connector.signMessage( // signResult 값을 저장함
                    walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
                    message = nonceResponse.message.toByteArray() // 메시지를 정해줌
                )

                val signature = when (signResult) { // 지갑 서명값을 저장함
                    is WalletSignResult.Success -> signResult.signature
                    is WalletSignResult.Failure -> { // 이 블록 안의 내용이 시작됨
                        showAppToast(context, signResult.message) // 화면에 글자를 보여줌
                        return@launch
                    }
                }

                val linkResponse = RetrofitClient.walletApi.linkWallet( // linkResponse 값을 저장함
                    authorization = "Bearer $accessToken", // authorization 값을 정해줌
                    request = WalletLinkRequest( // 서버 요청값을 정해줌
                        wallet_address = newWalletAddress, // 지갑 값을 요청값에 넣음
                        nonce = nonceResponse.nonce, // 서명용 난수를 정해줌
                        signature = signature // 지갑 서명값을 지갑 서명값에 넣음
                    )
                )

                saveWalletInfo( // 지갑 정보를 저장함
                    walletAddress = linkResponse.wallet_address, // 지갑 주소를 정해줌
                    walletProvider = walletType.name // 지갑 이름을 정해줌
                )
                prefs.edit()
                    .putString("wallet_auth_token_${walletType.name}", (connectResult as WalletConnectionResult.Success).authToken.orEmpty())
                    .apply()

                walletConnected = true // true 값을 지갑 관련 값에 넣음
                walletAddress = linkResponse.wallet_address // 지갑 주소를 정해줌
                walletProvider = walletType.name // 지갑 이름을 정해줌

                showAppToast(context, "지갑이 다시 연결되었습니다.") // 화면에 글자를 보여줌
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "지갑 재연결 실패", e) // 개발자가 확인할 로그를 찍음
                showAppToast(context, e.message ?: "지갑 재연결 실패") // 화면에 글자를 보여줌
            }
        }
    }

    LaunchedEffect(walletCallbackUri, pendingReconnectWallet) {
        val uri = walletCallbackUri ?: return@LaunchedEffect
        val reconnectWallet = pendingReconnectWallet ?: return@LaunchedEffect
        if (uri.scheme != "spentopia" || uri.host != "wallet-callback") return@LaunchedEffect

        val isSolflare = reconnectWallet == SolanaWalletType.SOLFLARE
        val hasError = if (isSolflare) {
            solflareDeepLinkConnector.isErrorCallback(uri)
        } else {
            phantomDeepLinkConnector.isErrorCallback(uri)
        }

        if (hasError) {
            val message = if (isSolflare) {
                solflareDeepLinkConnector.parseErrorCallback(uri)
            } else {
                phantomDeepLinkConnector.parseErrorCallback(uri)
            }
            pendingReconnectWallet = null
            pendingReconnectWalletAddress = null
            pendingReconnectNonce = null
            showAppToast(context, message)
            onWalletCallbackConsumed()
            return@LaunchedEffect
        }

        val isConnectCallback = if (isSolflare) {
            solflareDeepLinkConnector.isConnectCallback(uri)
        } else {
            phantomDeepLinkConnector.isConnectCallback(uri)
        }

        if (isConnectCallback) {
            val newWalletAddress = if (isSolflare) {
                solflareDeepLinkConnector.parseConnectCallback(uri)
            } else {
                phantomDeepLinkConnector.parseConnectCallback(uri)
            }

            if (newWalletAddress.isNullOrBlank()) {
                pendingReconnectWallet = null
                pendingReconnectWalletAddress = null
                pendingReconnectNonce = null
                showAppToast(context, "지갑 주소를 가져오지 못했습니다.")
                onWalletCallbackConsumed()
                return@LaunchedEffect
            }

            pendingReconnectWalletAddress = newWalletAddress
            try {
                val nonceResponse = RetrofitClient.walletApi.issueWalletNonce(
                    NonceRequest(wallet_address = newWalletAddress)
                )
                pendingReconnectNonce = nonceResponse.nonce
                val opened = if (isSolflare) {
                    solflareDeepLinkConnector.savePendingLogin(newWalletAddress, nonceResponse.nonce)
                    solflareDeepLinkConnector.signMessage(nonceResponse.message)
                } else {
                    phantomDeepLinkConnector.savePendingLogin(newWalletAddress, nonceResponse.nonce)
                    phantomDeepLinkConnector.signMessage(nonceResponse.message)
                }
                if (!opened) {
                    val walletName = if (isSolflare) "Solflare" else "Phantom"
                    pendingReconnectWallet = null
                    pendingReconnectWalletAddress = null
                    pendingReconnectNonce = null
                    showAppToast(context, "${walletName} 지갑 앱을 찾을 수 없습니다.")
                }
            } catch (e: Exception) {
                pendingReconnectWallet = null
                pendingReconnectWalletAddress = null
                pendingReconnectNonce = null
                showAppToast(context, e.message ?: "지갑 nonce 발급 실패")
            }
            onWalletCallbackConsumed()
            return@LaunchedEffect
        }

        val isSignCallback = if (isSolflare) {
            solflareDeepLinkConnector.isSignCallback(uri)
        } else {
            phantomDeepLinkConnector.isSignCallback(uri)
        }

        if (isSignCallback) {
            val signature = if (isSolflare) {
                solflareDeepLinkConnector.parseSignCallback(uri)
            } else {
                phantomDeepLinkConnector.parseSignCallback(uri)
            }
            val newWalletAddress = pendingReconnectWalletAddress
                ?: if (isSolflare) solflareDeepLinkConnector.getPendingWalletAddress() else phantomDeepLinkConnector.getPendingWalletAddress()
            val nonce = pendingReconnectNonce
                ?: if (isSolflare) solflareDeepLinkConnector.getPendingNonce() else phantomDeepLinkConnector.getPendingNonce()

            if (signature.isNullOrBlank() || newWalletAddress.isNullOrBlank() || nonce.isNullOrBlank()) {
                pendingReconnectWallet = null
                pendingReconnectWalletAddress = null
                pendingReconnectNonce = null
                showAppToast(context, "지갑 서명 상태를 확인하지 못했습니다.")
                onWalletCallbackConsumed()
                return@LaunchedEffect
            }

            try {
                val accessToken = prefs.getString("access_token", "") ?: ""
                val linkResponse = RetrofitClient.walletApi.linkWallet(
                    authorization = "Bearer $accessToken",
                    request = WalletLinkRequest(
                        wallet_address = newWalletAddress,
                        nonce = nonce,
                        signature = signature
                    )
                )

                saveWalletInfo(
                    walletAddress = linkResponse.wallet_address,
                    walletProvider = reconnectWallet.name
                )
                walletConnected = true
                walletAddress = linkResponse.wallet_address
                walletProvider = reconnectWallet.name
                pendingReconnectWallet = null
                pendingReconnectWalletAddress = null
                pendingReconnectNonce = null
                phantomDeepLinkConnector.clearPendingLogin()
                solflareDeepLinkConnector.clearPendingLogin()
                showAppToast(context, "지갑이 다시 연결되었습니다.")
            } catch (e: Exception) {
                pendingReconnectWallet = null
                pendingReconnectWalletAddress = null
                pendingReconnectNonce = null
                showAppToast(context, e.message ?: "지갑 재연결 실패")
            }
            onWalletCallbackConsumed()
        }
    }

    // 로그아웃 시 인증 정보만 초기화
    // 테마 설정은 유지함
    fun clearAuthState() { // clearAuthState 함수를 선언함
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

    fun moveToHome() { // moveToHome 함수를 선언함
        navController.navigate(Route.Home.route) { // 다른 화면으로 이동함
            popUpTo(Route.Login.route) { inclusive = true } // inclusive 값을 정해줌
            launchSingleTop = true // true 값을 launchSingleTop 값에 넣음
        }
    }

    fun moveToLogin() { // 로그인 기능을 실행하는 함수 시작
        navController.navigate(Route.Login.route) { // 다른 화면으로 이동함
            popUpTo(0) { inclusive = true } // inclusive 값을 정해줌
            launchSingleTop = true // true 값을 launchSingleTop 값에 넣음
        }
    }

    LaunchedEffect(Unit) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (shouldForceLogout()) { // 조건이 맞는지 확인함
            clearAuthState() // 로그인 정보를 지움
            moveToLogin() // 로그인 화면으로 이동함
        }
    }

    LaunchedEffect(currentRoute) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (drawerState.isOpen) { // 조건이 맞는지 확인함
            drawerState.close()
        }
    }

    ModalNavigationDrawer( // 왼쪽 메뉴 서랍을 보여줌
        drawerState = drawerState, // drawerState 값을 drawerState 값에 넣음
        gesturesEnabled = false, // false 값을 gesturesEnabled 값에 넣음
        drawerContent = { // drawerContent 값을 정해줌
            if (shouldShowDrawer) { // 조건이 맞는지 확인함
                ModalDrawerSheet( // 메뉴 서랍 내용을 담는 영역을 만듦
                    drawerContainerColor = MaterialTheme.colorScheme.surface, // drawerContainerColor 값을 정해줌
                    drawerContentColor = MaterialTheme.colorScheme.onSurface // drawerContentColor 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    AppDrawerContent( // 앱 왼쪽 메뉴 내용을 보여줌
                        onCloseClick = { scope.launch { drawerState.close() } }, // onCloseClick 때 실행할 함수를 정해줌
                        onLedgerClick = { // onLedgerClick 때 실행할 함수를 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Home.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onBudgetClick = { // 예산 관련 값을 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Budget.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onAnalysisClick = { // onAnalysisClick 때 실행할 함수를 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Analysis.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onProfileAvatarClick = { // 아바타 관련 값을 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.ProfileAvatar.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onMarketClick = { // 마켓 관련 값을 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Market.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onPlazaClick = { // onPlazaClick 때 실행할 함수를 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Plaza.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onCommunityClick = { // 커뮤니티 관련 값을 정해줌
                            scope.launch { drawerState.close() }
                            navController.navigate(Route.Community.route) { launchSingleTop = true } // 다른 화면으로 이동함
                        },
                        onLogoutClick = { // onLogoutClick 때 실행할 함수를 정해줌
                            scope.launch { // 이 블록 안의 내용이 시작됨
                                try { // 오류가 날 수 있는 코드를 먼저 시도함
                                    val refreshToken = prefs.getString("refresh_token", "") ?: "" // 갱신 토큰을 저장함
                                    RetrofitClient.walletApi.logout( // 서버 통신 도구를 설정함
                                        request = RefreshTokenRequest( // 서버 요청값을 정해줌
                                            refresh_token = refreshToken // 갱신 토큰을 갱신 토큰에 넣음
                                        )
                                    )
                                } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                                    Log.e("Spentopia", "로그아웃 API 실패", e) // 개발자가 확인할 로그를 찍음
                                } finally { // 이 블록 안의 내용이 시작됨
                                    clearAuthState() // 로그인 정보를 지움
                                    moveToLogin() // 로그인 화면으로 이동함
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { // 이 블록 안의 내용이 시작됨
        Scaffold( // 상단바나 본문 같은 화면 기본 틀을 만듦
            containerColor = MaterialTheme.colorScheme.background, // containerColor 값을 정해줌
            contentColor = MaterialTheme.colorScheme.onBackground, // contentColor 값을 정해줌
            topBar = { // topBar 값을 정해줌
                if (shouldShowDrawer) { // 조건이 맞는지 확인함
                    val isDark = MaterialTheme.colorScheme.background == SpentopiaDarkBackground
                    val brandColor = if (isDark) Color(0xFFC4B5FD) else Color(0xFF2563EB)
                    CenterAlignedTopAppBar( // 가운데 제목이 있는 상단바를 보여줌
                        title = { // 제목을 정해줌
                            Text( // 화면에 글자를 보여줌
                                text = "Spentopia", // text 값을 정해줌
                                color = brandColor // color 값을 정해줌
                            )
                        },
                        navigationIcon = { // navigationIcon 값을 정해줌
                            IconButton(onClick = { scope.launch { drawerState.open() } }) { // 누를 수 있는 버튼을 만듦
                                Icon( // 화면에 아이콘을 보여줌
                                    Icons.Default.Menu,
                                    contentDescription = "메뉴", // contentDescription 값을 정해줌
                                    tint = MaterialTheme.colorScheme.onSurface // tint 값을 정해줌
                                )
                            }
                        },
                        actions = { // actions 값을 정해줌
                            IconButton(onClick = { showThemeDialog = true }) { // 누를 수 있는 버튼을 만듦
                                Icon( // 화면에 아이콘을 보여줌
                                    Icons.Default.Settings,
                                    contentDescription = "설정", // contentDescription 값을 정해줌
                                    tint = MaterialTheme.colorScheme.onSurface // tint 값을 정해줌
                                )
                            }
                            IconButton(onClick = { // 누를 수 있는 버튼을 만듦
                                showNotificationDialog = true // 알림 팝업을 열어줌
                                loadNotificationSetting() // 팝업을 열 때 알림 수신 설정을 불러옴
                                loadNotifications() // 팝업을 열 때 최신 알림을 다시 불러옴
                            }) {
                                Icon( // 화면에 아이콘을 보여줌
                                    Icons.Default.NotificationsNone,
                                    contentDescription = "알림", // contentDescription 값을 정해줌
                                    tint = MaterialTheme.colorScheme.onSurface // tint 값을 정해줌
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors( // colors 값을 정해줌
                            containerColor = MaterialTheme.colorScheme.surface, // containerColor 값을 정해줌
                            titleContentColor = MaterialTheme.colorScheme.onSurface, // titleContentColor 값을 정해줌
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface, // navigationIconContentColor 값을 정해줌
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface // actionIconContentColor 값을 정해줌
                        )
                    )
                }
            }
        ) { innerPadding ->
            Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxSize()
                    .padding(innerPadding)
            ) { // 이 블록 안의 내용이 시작됨
                NavHost( // 화면 이동 틀을 만듦
                    navController = navController, // 화면 이동 도구를 화면 이동 도구에 넣음
                    startDestination = initialDestination, // 최초 실행만 스플래시를 거치고 인증 콜백 복귀는 바로 처리함
                    modifier = Modifier.fillMaxSize() // UI 크기나 여백 같은 모양을 정함
                ) { // 이 블록 안의 내용이 시작됨
                    composable(Route.Splash.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                        SplashScreen( // 스플래시 화면을 보여줌
                            navController = navController,
                            isDarkTheme = isDarkTheme
                        )
                    }
                composable(Route.Login.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    LoginScreen( // 로그인 화면을 보여줌
                        onLoginClick = { moveToHome() }, // onLoginClick 때 실행할 함수를 정해줌
                        walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
                        walletCallbackUri = walletCallbackUri, // 지갑 값을 요청값에 넣음
                        onWalletCallbackConsumed = onWalletCallbackConsumed, // 지갑 값을 요청값에 넣음
                        kakaoCallbackUri = kakaoCallbackUri, // kakaoCallbackUri 값을 kakaoCallbackUri 값에 넣음
                        onKakaoCallbackConsumed = onKakaoCallbackConsumed, // onKakaoCallbackConsumed 때 실행할 함수를 onKakaoCallbackConsumed 때 실행할 함수에 넣음
                        onKakaoClick = {}, // 로그인 화면 토스트는 표시하지 않음
                        onGoogleClick = {}, // 로그인 화면 토스트는 표시하지 않음
                        onFindEmailClick = { // 이메일 값을 정해줌
                            navController.navigate(Route.FindEmail.route) // 다른 화면으로 이동함
                        },
                        onFindPasswordClick = { // 비밀번호 값을 정해줌
                            navController.navigate(Route.FindPassword.route) // 다른 화면으로 이동함
                        },
                        onWalletConnected = { accessToken, refreshToken, newWalletAddress, newWalletProvider -> // 지갑 관련 값을 정해줌
                            if (accessToken.isNotBlank() && refreshToken.isNotBlank()) { // 조건이 맞는지 확인함
                                saveAuthTokens(accessToken, refreshToken) // 로그인 토큰을 저장함
                                saveWalletInfo(newWalletAddress, newWalletProvider) // 지갑 정보를 저장함
                                walletConnected = true // true 값을 지갑 관련 값에 넣음
                                walletAddress = newWalletAddress // 지갑 관련 값을 지갑 주소에 넣음
                                walletProvider = newWalletProvider // 지갑 관련 값을 지갑 이름에 넣음
                                moveToHome() // 홈 화면으로 이동함
                            }
                        }
                    )
                }

                composable(Route.FindEmail.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    FindEmailScreen(onBackToLoginClick = { navController.popBackStack() }) // 이메일 값을 정해줌
                }

                composable(Route.FindPassword.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    FindPasswordScreen(onBackToLoginClick = { navController.popBackStack() }) // 비밀번호 값을 정해줌
                }

                composable(Route.Home.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    HomeScreen( // 홈 화면을 보여줌
                        isWalletConnected = walletConnected, // 지갑 값을 요청값에 넣음
                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                        walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                        onWalletDisconnectClick = { startWalletUnlink() }, // 지갑 관련 값을 정해줌
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }, // 지갑 관련 값을 정해줌
                        onLedgerClick = { navController.navigate(Route.Home.route) }, // 다른 화면으로 이동함
                        onMyPageClick = { navController.navigate(Route.ProfileAvatar.route) }, // 다른 화면으로 이동함
                        onBudgetClick = { navController.navigate(Route.Budget.route) }, // 다른 화면으로 이동함
                        onAnalysisClick = { navController.navigate(Route.Analysis.route) }, // 다른 화면으로 이동함
                        onAvatarClick = { navController.navigate(Route.ProfileAvatar.route) }, // 다른 화면으로 이동함
                        onMarketClick = { navController.navigate(Route.Market.route) }, // 다른 화면으로 이동함
                        onPlazaClick = { navController.navigate(Route.Plaza.route) }, // 다른 화면으로 이동함
                        onCommunityClick = { navController.navigate(Route.Community.route) } // 다른 화면으로 이동함
                    )
                }

                composable(Route.Budget.route) {
                    BudgetScreen(
                        isWalletConnected = walletConnected,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }
                    )
                } // 이 주소로 들어오면 보여줄 화면을 등록함
                composable(Route.Analysis.route) {
                    AnalysisScreen(
                        isWalletConnected = walletConnected,
                        walletAddress = walletAddress,
                        walletProvider = walletProvider,
                        walletActivityResultSender = walletActivityResultSender,
                        walletCallbackUri = walletCallbackUri,
                        onWalletCallbackConsumed = onWalletCallbackConsumed,
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }
                    )
                } // 이 주소로 들어오면 보여줄 화면을 등록함

                composable(Route.ProfileAvatar.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    ProfileAvatarScreen( // 마이페이지와 아바타 아이템 네이티브 화면을 보여줌
                        isWalletConnected = walletConnected, // 지갑 값을 요청값에 넣음
                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                        walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }, // 지갑 관련 값을 정해줌
                        onWalletDisconnectClick = { startWalletUnlink() } // 지갑 해제 값을 정해줌
                    )
                }

                composable(Route.Market.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    MarketScreen( // 마켓 화면을 보여줌
                        isWalletConnected = walletConnected, // 지갑 값을 요청값에 넣음
                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                        walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }, // 지갑 관련 값을 정해줌
                        onNavigateBack = { navController.popBackStack() }, // onNavigateBack 때 실행할 함수를 정해줌
                        webPath = "/nft-market", // webPath 값을 정해줌
                        screenTitle = "NFT 마켓", // screenTitle 값을 정해줌
                        isDarkTheme = isDarkTheme // 앱 테마 값을 웹뷰에 전달함
                    )
                }

                composable(Route.Plaza.route) { PlazaScreen() } // 이 주소로 들어오면 보여줄 화면을 등록함

                composable(Route.Community.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    LaunchedEffect(Unit) { // 화면이 열리거나 값이 바뀔 때 실행함
                        communityViewModel.loadPosts()
                    }
                    LaunchedEffect(communityUiState.errorMessage) {
                        communityUiState.errorMessage
                            ?.takeIf { it.isNotBlank() }
                            ?.let { message ->
                                showAppToast(context, message)
                            }
                    }

                    CommunityScreen( // 커뮤니티 화면을 보여줌
                        posts = communityUiState.posts, // posts 값을 정해줌
                        contests = communityUiState.contests, // contests 값을 정해줌
                        selectedPost = communityUiState.selectedPost, // selectedPost 값을 정해줌
                        currentUserId = communityUiState.currentUserId, // currentUserId 값을 정해줌
                        currentUserRole = communityUiState.currentUserRole, // currentUserRole 값을 정해줌
                        isLoading = communityUiState.isLoading, // 로딩 여부를 정해줌
                        errorMessage = communityUiState.errorMessage, // 오류 내용을 정해줌
                        onRetryClick = { // onRetryClick 때 실행할 함수를 정해줌
                            communityViewModel.clearError()
                            communityViewModel.loadPosts()
                        },
                        onWriteClick = { // onWriteClick 때 실행할 함수를 정해줌
                            navController.navigate(Route.CommunityWrite.createRoute()) // 다른 화면으로 이동함
                        },
                        onContestWriteClick = { contestId -> // onContestWriteClick 때 실행할 함수를 정해줌
                            navController.navigate( // 다른 화면으로 이동함
                                Route.CommunityWrite.createRoute(
                                    category = "contest", // 카테고리를 정해줌
                                    contestId = contestId // contestId 값을 contestId 값에 넣음
                                )
                            )
                        },
                        onPostClick = { post -> // onPostClick 때 실행할 함수를 정해줌
                            communityViewModel.selectPost(post)
                            communityViewModel.loadPostDetail(post.id)
                        },
                        onCloseDetailClick = { // onCloseDetailClick 때 실행할 함수를 정해줌
                            communityViewModel.clearSelectedPost()
                        },
                        onUpdatePostClick = { updatedPost -> // onUpdatePostClick 때 실행할 함수를 정해줌
                            communityViewModel.updatePost(updatedPost) {
                                showAppToast(context, "게시글이 수정되었습니다.", AppToastType.SUCCESS)
                            }
                        },
                        onDeletePostClick = { deletePostId -> // onDeletePostClick 때 실행할 함수를 정해줌
                            communityViewModel.deletePost(deletePostId) { // 이 블록 안의 내용이 시작됨
                                communityViewModel.clearSelectedPost()
                                showAppToast(context, "게시글이 삭제 되었어요 !", AppToastType.DELETE)
                            }
                        },
                        onToggleLikeClick = { targetPostId -> // onToggleLikeClick 때 실행할 함수를 정해줌
                            communityViewModel.toggleLike(targetPostId)
                        },
                        onAddCommentClick = { targetPostId, content -> // onAddCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.addComment(targetPostId, content) {
                                showAppToast(context, "댓글이 등록되었습니다.", AppToastType.SUCCESS)
                            }
                        },
                        onUpdateCommentClick = { targetPostId, commentId, content -> // onUpdateCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.updateComment(targetPostId, commentId, content) {
                                showAppToast(context, "댓글이 수정되었습니다.", AppToastType.SUCCESS)
                            }
                        },
                        onDeleteCommentClick = { targetPostId, commentId -> // onDeleteCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.deleteComment(targetPostId, commentId) {
                                showAppToast(context, "댓글이 삭제되었습니다.")
                            }
                        },
                        onReportClick = { targetType, targetId, reason, detail -> // onReportClick 때 실행할 함수를 정해줌
                            communityViewModel.reportContent(targetType, targetId, reason, detail)
                        }
                    )
                }

                composable(Route.Chatbot.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    ChatbotScreen( // 챗봇 화면을 보여줌
                        onBackClick = { navController.popBackStack() } // onBackClick 때 실행할 함수를 정해줌
                    )
                }

                composable( // 이 주소로 들어오면 보여줄 화면을 등록함
                    route = Route.CommunityWrite.route, // route 값을 정해줌
                    arguments = listOf( // arguments 값을 정해줌
                        navArgument("category") { // 이 블록 안의 내용이 시작됨
                            type = NavType.StringType // type 값을 정해줌
                            nullable = true // true 값을 nullable 값에 넣음
                            defaultValue = null // null 값을 defaultValue 값에 넣음
                        },
                        navArgument("contestId") { // 이 블록 안의 내용이 시작됨
                            type = NavType.StringType // type 값을 정해줌
                            nullable = true // true 값을 nullable 값에 넣음
                            defaultValue = null // null 값을 defaultValue 값에 넣음
                        }
                    )
                ) { backStackEntry ->
                    val categoryArg = backStackEntry.arguments?.getString("category") // categoryArg 값을 저장함
                    val initialCategory = when (categoryArg) { // initialCategory 값을 저장함
                        "contest" -> CommunityCategory.AVATAR_CONTEST
                        "notice" -> CommunityCategory.NOTICE
                        "request" -> CommunityCategory.REQUEST
                        else -> CommunityCategory.FREE_BOARD // 위 조건이 아니면 이쪽을 실행함
                    }
                    val contestId = backStackEntry.arguments?.getString("contestId") // contestId 값을 저장함

                    CommunityWriteScreen( // 커뮤니티 글쓰기 화면을 보여줌
                        initialCategory = initialCategory, // initialCategory 값을 initialCategory 값에 넣음
                        initialContestId = contestId, // contestId 값을 initialContestId 값에 넣음
                        onSubmitClick = { category, title, content, imageUri, selectedContestId -> // onSubmitClick 때 실행할 함수를 정해줌
                            val submitContestId = selectedContestId // submitContestId 값을 저장함
                                ?: communityUiState.contests.firstOrNull { it.status == "active" }?.id // it.status 값을 정해줌
                                ?: communityUiState.contests.firstOrNull()?.id
                            communityViewModel.createPost(
                                category = category, // 카테고리를 카테고리에 넣음
                                title = title, // 제목을 제목에 넣음
                                content = content, // 내용을 내용에 넣음
                                imageUri = imageUri, // imageUri 값을 imageUri 값에 넣음
                                contentResolver = context.contentResolver, // contentResolver 값을 정해줌
                                contestId = if (category == CommunityCategory.AVATAR_CONTEST) { // contestId 값을 정해줌
                                    submitContestId
                                } else { // 이 블록 안의 내용이 시작됨
                                    null
                                },
                                onSuccess = { // 성공했을 때 실행할 함수를 정해줌
                                    showAppToast(context, "게시글이 등록 되었어요 !", AppToastType.SUCCESS)
                                    navController.popBackStack()
                                }
                            )
                        }
                    )
                }
            }

                if (shouldShowChatbotFloatingButton) { // 조건이 맞는지 확인함
                    FloatingChatbotButton( // 누를 수 있는 버튼을 만듦
                        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                            .align(Alignment.BottomEnd)
                            .navigationBarsPadding()
                            .padding(end = 18.dp, bottom = 18.dp), // .padding(end 값을 정해줌
                        onClick = { // 눌렀을 때 실행할 함수를 정해줌
                            navController.navigate(Route.Chatbot.route) { // 다른 화면으로 이동함
                                launchSingleTop = true // true 값을 launchSingleTop 값에 넣음
                            }
                        }
                    )
                }
            }
        }

        if (showThemeDialog) { // 조건이 맞는지 확인함
            AlertDialog( // 팝업 확인창을 보여줌
                onDismissRequest = { showThemeDialog = false }, // onDismissRequest 때 실행할 함수를 정해줌
                title = { Text("화면 설정") }, // 화면에 글자를 보여줌
                text = { Text(if (isDarkTheme) "현재 다크모드가 적용되어 있습니다." else "현재 라이트모드가 적용되어 있습니다.") }, // 화면에 글자를 보여줌
                confirmButton = { // confirmButton 값을 정해줌
                    TextButton(onClick = { onThemeChange(true); showThemeDialog = false }) { Text("다크모드") } // 화면에 글자를 보여줌
                },
                dismissButton = { // dismissButton 값을 정해줌
                    TextButton(onClick = { onThemeChange(false); showThemeDialog = false }) { Text("라이트모드") } // 화면에 글자를 보여줌
                }
            )
        }

        if (showNotificationDialog) { // 조건이 맞는지 확인함
            AlertDialog( // 팝업 확인창을 보여줌
                onDismissRequest = { showNotificationDialog = false }, // onDismissRequest 때 실행할 함수를 정해줌
                title = { Text("알림") }, // 화면에 글자를 보여줌
                text = {
                    NotificationDialogContent(
                        notifications = notifications,
                        loading = notificationsLoading,
                        errorMessage = notificationsError,
                        notificationEnabled = notificationEnabled,
                        notificationSettingLoading = notificationSettingLoading,
                        onNotificationEnabledChange = { enabled -> updateNotificationEnabled(enabled) },
                        onReadClick = { notificationId -> readNotification(notificationId) },
                        onRetryClick = { loadNotifications() }
                    )
                },
                confirmButton = { // confirmButton 값을 정해줌
                    TextButton(onClick = { showNotificationDialog = false }) { Text("닫기") } // 화면에 글자를 보여줌
                },
                dismissButton = {
                    TextButton(
                        onClick = { readAllNotifications() },
                        enabled = notifications.any { !it.is_read } && !notificationsLoading
                    ) {
                        Text("모두 읽음")
                    }
                }
            )
        }
    }
}

@Composable
private fun NotificationDialogContent(
    notifications: List<NotificationResponse>, // 화면에 보여줄 알림 목록
    loading: Boolean, // 알림을 불러오는 중인지 여부
    errorMessage: String?, // 오류가 있을 때 보여줄 문구
    notificationEnabled: Boolean, // 전체 알림 수신 여부
    notificationSettingLoading: Boolean, // 알림 수신 설정을 저장/조회 중인지 여부
    onNotificationEnabledChange: (Boolean) -> Unit, // 전체 알림 스위치를 바꿨을 때 실행할 함수
    onReadClick: (String) -> Unit, // 알림 읽음 버튼을 눌렀을 때 실행할 함수
    onRetryClick: () -> Unit // 다시 불러오기 버튼을 눌렀을 때 실행할 함수
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(), // 스위치 영역을 가로로 채움
            horizontalArrangement = Arrangement.SpaceBetween, // 문구와 스위치를 양끝에 배치함
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "알림 받기",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (notificationEnabled) "앱 알림을 받고 있어요." else "앱 알림을 받지 않아요.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = notificationEnabled,
                onCheckedChange = onNotificationEnabledChange,
                enabled = !notificationSettingLoading,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }

        HorizontalDivider()

        when {
            loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // 오류 문구와 재시도 버튼을 세로로 배치함
                    Text(errorMessage) // 오류 문구를 보여줌
                    TextButton(onClick = onRetryClick) {
                        Text("다시 불러오기") // 알림 목록 재조회 버튼을 보여줌
                    }
                }
            }

            notifications.isEmpty() -> {
                Text("아직 도착한 알림이 없습니다.") // 알림이 없을 때 보여줄 빈 상태 문구
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    notifications.forEachIndexed { index, notification ->
                        NotificationDialogItem(
                            notification = notification, // 알림 1개 데이터를 넘김
                            onReadClick = { onReadClick(notification.id) } // 해당 알림 아이디로 읽음 처리함
                        )
                        if (index < notifications.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationDialogItem(
    notification: NotificationResponse, // 알림 1개 데이터
    onReadClick: () -> Unit // 읽음 버튼을 눌렀을 때 실행할 함수
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { // 알림 종류, 내용, 시간을 세로로 배치함
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = notification.notification_type.toNotificationTypeLabel(), // 서버 알림 타입을 사용자용 문구로 바꿈
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            if (!notification.is_read) {
                TextButton(onClick = onReadClick) {
                    Text("읽음") // 읽지 않은 알림만 읽음 버튼을 보여줌
                }
            }
        }
        Text(
            text = notification.message, // 서버에서 받은 알림 메시지를 보여줌
            style = MaterialTheme.typography.bodyMedium,
            color = if (notification.is_read) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontWeight = if (notification.is_read) FontWeight.Normal else FontWeight.SemiBold
        )
        Text(
            text = notification.created_at.toNotificationTimeText(), // 서버 시간을 간단한 표시 형식으로 바꿈
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun String.toNotificationTypeLabel(): String { // 서버 알림 타입을 화면용 문구로 바꾸는 함수
    return when {
        startsWith("budget_alert") -> "예산 알림"
        startsWith("reward") -> "보상 알림"
        startsWith("streak") -> "스트릭 알림"
        else -> "알림"
    }
}

private fun String?.toNotificationTimeText(): String { // ISO 시간 문자열을 화면에 보기 좋게 바꾸는 함수
    if (isNullOrBlank()) return "" // 시간이 없으면 빈 문자열을 돌려줌
    return replace("T", " ")
        .replace("Z", "")
        .substringBefore(".")
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun FloatingChatbotButton( // FloatingChatbotButton 함수를 선언함
    modifier: Modifier = Modifier, // modifier 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    FloatingActionButton( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = modifier // modifier 값을 modifier 값에 넣음
            .shadow(
                elevation = 12.dp, // elevation 값을 정해줌
                shape = MaterialTheme.shapes.extraLarge, // shape 값을 정해줌
                ambientColor = SpentopiaMutedPurple.copy(alpha = 0.18f), // ambientColor 값을 정해줌
                spotColor = SpentopiaMutedPurple.copy(alpha = 0.25f) // spotColor 값을 정해줌
            )
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.965f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.965f else 1f // scaleY 값을 정해줌
            }
            .size(62.dp),
        containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // contentColor 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxSize()
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer, // color 값을 정해줌
                    shape = MaterialTheme.shapes.extraLarge // shape 값을 정해줌
                ),
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "🤖", // text 값을 정해줌
                color = MaterialTheme.colorScheme.onPrimaryContainer // color 값을 정해줌
            )
        }
    }
}
