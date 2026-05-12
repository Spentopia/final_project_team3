package com.ict.spentopia.navigation // 이 파일이 속한 패키지 위치를 적음

import android.content.Context // 현재 화면 정보 타입을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import android.widget.Toast // 짧은 알림 메시지 기능을 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.navigationBarsPadding // navigationBarsPadding 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
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
import androidx.compose.material3.ModalDrawerSheet // ModalDrawerSheet 기능을 가져옴
import androidx.compose.material3.ModalNavigationDrawer // ModalNavigationDrawer 기능을 가져옴
import androidx.compose.material3.Scaffold // Scaffold 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
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
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.data.remote.RefreshTokenRequest // RefreshTokenRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletLinkRequest // WalletLinkRequest 기능을 가져옴
import com.ict.spentopia.data.remote.WalletUnlinkRequest // WalletUnlinkRequest 기능을 가져옴
import com.ict.spentopia.feature.analysis.AnalysisScreen // AnalysisScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.FindEmailScreen // FindEmailScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.FindPasswordScreen // FindPasswordScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.LoginScreen // LoginScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.SplashScreen // SplashScreen 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaBackpackConnector // MwaBackpackConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaPhantomConnector // MwaPhantomConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.MwaSolflareConnector // MwaSolflareConnector 기능을 가져옴
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
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
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

    var showThemeDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 showThemeDialog 값을 저장함
    var showNotificationDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 showNotificationDialog 값을 저장함

    // SharedPreferences는 토큰/지갑/강제로그아웃 저장용
    val prefs = remember { // 화면이 다시 그려져도 간단 저장소를 기억함
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
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

    fun shouldForceLogout(): Boolean = prefs.getBoolean("force_logout", false) // shouldForceLogout 함수를 선언함

    val navBackStackEntry by navController.currentBackStackEntryAsState() // navBackStackEntry 값을 저장함
    val currentRoute = navBackStackEntry?.destination?.route // currentRoute 값을 저장함

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
            Toast.makeText(context, "연결된 지갑 주소가 없습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            return
        }

        if (accessToken.isBlank()) { // 조건이 맞는지 확인함
            Toast.makeText(context, "로그인 토큰이 없습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
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

                        Toast.makeText(context, "지갑 연결이 해제되었습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                    }
                    is WalletSignResult.Failure -> { // 이 블록 안의 내용이 시작됨
                        Toast.makeText(context, signResult.message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                    }
                }
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "지갑 해제 실패", e) // 개발자가 확인할 로그를 찍음
                Toast.makeText(context, e.message ?: "지갑 해제 실패", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            }
        }
    }

    // 같은 지갑 재연결 흐름
    // 지갑 타입별 connector 다름
    fun startWalletReconnect(walletType: SolanaWalletType) { // startWalletReconnect 함수를 선언함
        val accessToken = prefs.getString("access_token", "") ?: "" // 접근 토큰을 저장함

        if (accessToken.isBlank()) { // 조건이 맞는지 확인함
            Toast.makeText(context, "로그인 토큰이 없습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
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
                        Toast.makeText(context, connectResult.message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                        return@launch
                    }
                }

                if (newWalletAddress.isBlank()) { // 조건이 맞는지 확인함
                    Toast.makeText(context, "지갑 주소를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
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
                        Toast.makeText(context, signResult.message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
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

                walletConnected = true // true 값을 지갑 관련 값에 넣음
                walletAddress = linkResponse.wallet_address // 지갑 주소를 정해줌
                walletProvider = walletType.name // 지갑 이름을 정해줌

                Toast.makeText(context, "지갑이 다시 연결되었습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                Log.e("Spentopia", "지갑 재연결 실패", e) // 개발자가 확인할 로그를 찍음
                Toast.makeText(context, e.message ?: "지갑 재연결 실패", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            }
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
                    CenterAlignedTopAppBar( // 가운데 제목이 있는 상단바를 보여줌
                        title = { // 제목을 정해줌
                            Text( // 화면에 글자를 보여줌
                                text = "Spentopia", // text 값을 정해줌
                                color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
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
                            IconButton(onClick = { showNotificationDialog = true }) { // 누를 수 있는 버튼을 만듦
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
                    startDestination = Route.Splash.route, // startDestination 값을 정해줌
                    modifier = Modifier.fillMaxSize() // UI 크기나 여백 같은 모양을 정함
                ) { // 이 블록 안의 내용이 시작됨
                composable(Route.Splash.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    SplashScreen(navController) // 스플래시 화면을 보여줌
                }
                composable(Route.Login.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    LoginScreen( // 로그인 화면을 보여줌
                        onLoginClick = { moveToHome() }, // onLoginClick 때 실행할 함수를 정해줌
                        walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
                        walletCallbackUri = walletCallbackUri, // 지갑 값을 요청값에 넣음
                        onWalletCallbackConsumed = onWalletCallbackConsumed, // 지갑 값을 요청값에 넣음
                        kakaoCallbackUri = kakaoCallbackUri, // kakaoCallbackUri 값을 kakaoCallbackUri 값에 넣음
                        onKakaoCallbackConsumed = onKakaoCallbackConsumed, // onKakaoCallbackConsumed 때 실행할 함수를 onKakaoCallbackConsumed 때 실행할 함수에 넣음
                        onKakaoClick = { // onKakaoClick 때 실행할 함수를 정해줌
                            Toast.makeText(context, "카카오 로그인 연결 예정", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                        },
                        onGoogleClick = { // onGoogleClick 때 실행할 함수를 정해줌
                            Toast.makeText(context, "구글 로그인 연결 예정", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                        },
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

                composable(Route.Budget.route) { BudgetScreen() } // 이 주소로 들어오면 보여줄 화면을 등록함
                composable(Route.Analysis.route) { AnalysisScreen() } // 이 주소로 들어오면 보여줄 화면을 등록함

                composable(Route.ProfileAvatar.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    MarketScreen( // 마켓 화면을 보여줌
                        isWalletConnected = walletConnected, // 지갑 값을 요청값에 넣음
                        walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                        walletProvider = walletProvider, // 지갑 이름을 지갑 이름에 넣음
                        onWalletConnectClick = { walletType -> startWalletReconnect(walletType) }, // 지갑 관련 값을 정해줌
                        onNavigateBack = { navController.popBackStack() }, // onNavigateBack 때 실행할 함수를 정해줌
                        webPath = "/profile", // webPath 값을 정해줌
                        screenTitle = "마이페이지" // screenTitle 값을 정해줌
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
                        screenTitle = "NFT 마켓" // screenTitle 값을 정해줌
                    )
                }

                composable(Route.Plaza.route) { PlazaScreen() } // 이 주소로 들어오면 보여줄 화면을 등록함

                composable(Route.Community.route) { // 이 주소로 들어오면 보여줄 화면을 등록함
                    LaunchedEffect(Unit) { // 화면이 열리거나 값이 바뀔 때 실행함
                        communityViewModel.loadPosts()
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
                            communityViewModel.updatePost(updatedPost)
                        },
                        onDeletePostClick = { deletePostId -> // onDeletePostClick 때 실행할 함수를 정해줌
                            communityViewModel.deletePost(deletePostId) { // 이 블록 안의 내용이 시작됨
                                communityViewModel.clearSelectedPost()
                            }
                        },
                        onToggleLikeClick = { targetPostId -> // onToggleLikeClick 때 실행할 함수를 정해줌
                            communityViewModel.toggleLike(targetPostId)
                        },
                        onAddCommentClick = { targetPostId, content -> // onAddCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.addComment(targetPostId, content)
                        },
                        onUpdateCommentClick = { targetPostId, commentId, content -> // onUpdateCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.updateComment(targetPostId, commentId, content)
                        },
                        onDeleteCommentClick = { targetPostId, commentId -> // onDeleteCommentClick 때 실행할 함수를 정해줌
                            communityViewModel.deleteComment(targetPostId, commentId)
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
                text = { Text("예산의 80%를 사용했어요!\n5분 전\n\n새로운 아바타를 획득했어요\n1시간 전\n\n7일 연속 기록 달성! 보상이 지급됐어요\n2시간 전") }, // 화면에 글자를 보여줌
                confirmButton = { // confirmButton 값을 정해줌
                    TextButton(onClick = { showNotificationDialog = false }) { Text("닫기") } // 화면에 글자를 보여줌
                }
            )
        }
    }
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
