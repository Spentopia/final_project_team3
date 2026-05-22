package com.ict.spentopia.feature.auth // 이 파일이 속한 패키지 위치를 적음

// 로그인 화면임
// 이메일/비번, Google/Kakao, 지갑 로그인 한 화면

import android.content.Context
import android.content.Intent // Intent 기능을 가져옴
import android.net.Uri // 이미지 주소 타입을 가져옴
import android.widget.Toast // 짧은 알림 메시지 기능을 가져옴
import androidx.activity.compose.rememberLauncherForActivityResult // rememberLauncherForActivityResult 기능을 가져옴
import androidx.activity.result.contract.ActivityResultContracts // ActivityResultContracts 기능을 가져옴
import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.imePadding // imePadding 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.interaction.MutableInteractionSource // MutableInteractionSource 기능을 가져옴
import androidx.compose.foundation.interaction.collectIsPressedAsState // collectIsPressedAsState 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.foundation.text.KeyboardOptions // KeyboardOptions 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.outlined.Email // Email 기능을 가져옴
import androidx.compose.material.icons.outlined.Lock // Lock 기능을 가져옴
import androidx.compose.material.icons.outlined.Visibility // Visibility 기능을 가져옴
import androidx.compose.material.icons.outlined.VisibilityOff // VisibilityOff 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.HorizontalDivider // HorizontalDivider 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.IconButton // 아이콘 버튼 컴포넌트를 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.rememberCoroutineScope // rememberCoroutineScope 기능을 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.Brush // Brush 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.draw.shadow // shadow 기능을 가져옴
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.res.stringResource // stringResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.input.KeyboardType // KeyboardType 기능을 가져옴
import androidx.compose.ui.text.input.PasswordVisualTransformation // PasswordVisualTransformation 기능을 가져옴
import androidx.compose.ui.text.input.VisualTransformation // VisualTransformation 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.core.net.toUri // toUri 기능을 가져옴
import androidx.lifecycle.viewmodel.compose.viewModel // Compose에서 ViewModel 연결하는 도구를 가져옴
import com.google.android.gms.auth.api.signin.GoogleSignIn // GoogleSignIn 기능을 가져옴
import com.google.android.gms.auth.api.signin.GoogleSignInOptions // GoogleSignInOptions 기능을 가져옴
import com.google.android.gms.common.api.ApiException // ApiException 기능을 가져옴
import com.ict.spentopia.BuildConfig // BuildConfig 기능을 가져옴
import com.ict.spentopia.R // R 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.PhantomDeepLinkConnector // PhantomDeepLinkConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.connector.SolflareDeepLinkConnector // SolflareDeepLinkConnector 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // SolanaWalletDialog 기능을 가져옴
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // SolanaWalletType 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple // SpentopiaGlowPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaIconMuted // SpentopiaIconMuted 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavy // SpentopiaNavy 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple // SpentopiaNavyPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaActionGradientColors // SpentopiaActionGradientColors 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors // SpentopiaWalletGradientColors 기능을 가져옴
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender // ActivityResultSender 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import android.util.Log // 로그 찍는 기능을 가져옴
import androidx.compose.animation.core.RepeatMode // RepeatMode 기능을 가져옴
import androidx.compose.animation.core.animateFloat // animateFloat 기능을 가져옴
import androidx.compose.animation.core.infiniteRepeatable // infiniteRepeatable 기능을 가져옴
import androidx.compose.animation.core.rememberInfiniteTransition // rememberInfiniteTransition 기능을 가져옴
import androidx.compose.animation.core.tween // tween 기능을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun LoginScreen( // 로그인 기능을 실행하는 함수 시작
    onLoginClick: () -> Unit, // onLoginClick 때 실행할 함수를 받음
    walletActivityResultSender: ActivityResultSender, // 지갑 관련 값을 받음
    walletCallbackUri: Uri?, // 지갑 관련 값을 받음
    onWalletCallbackConsumed: () -> Unit, // 지갑 관련 값을 받음
    kakaoCallbackUri: Uri?, // kakaoCallbackUri 값을 받음
    onKakaoCallbackConsumed: () -> Unit, // onKakaoCallbackConsumed 때 실행할 함수를 받음
    onKakaoClick: () -> Unit = {}, // onKakaoClick 때 실행할 함수를 받음
    onNaverClick: () -> Unit = {}, // onNaverClick 때 실행할 함수를 받음
    onGoogleClick: () -> Unit = {}, // onGoogleClick 때 실행할 함수를 받음
    onFindEmailClick: () -> Unit = {}, // 이메일 값을 받음
    onFindPasswordClick: () -> Unit = {}, // 비밀번호 값을 받음
    onWalletConnected: (String, String, String, String) -> Unit = { _, _, _, _ -> } // Unit 값을 정해줌
) { // 이 블록 안의 내용이 시작됨
    val colorScheme = MaterialTheme.colorScheme // colorScheme 값을 저장함
    var email by remember { mutableStateOf("") } // 화면에서 바뀔 이메일을 저장함
    var password by remember { mutableStateOf("") } // 화면에서 바뀔 비밀번호를 저장함
    var passwordVisible by remember { mutableStateOf(false) } // 화면에서 바뀔 비밀번호 값을 저장함

    val scope = rememberCoroutineScope() // 화면이 다시 그려져도 코루틴 실행 범위을 기억함
    val context = LocalContext.current // 현재 화면 정보를 저장함
    val phantomConnector = remember { PhantomDeepLinkConnector(context) } // 화면이 다시 그려져도 phantomConnector 값을 기억함
    val solflareConnector = remember { SolflareDeepLinkConnector(context) } // 화면이 다시 그려져도 solflareConnector 값을 기억함
    val loginViewModel: LoginViewModel = viewModel() // loginViewModel 값을 저장함

    var showWalletDialog by remember { mutableStateOf(false) } // 화면에서 바뀔 지갑 관련 값을 저장함
    var selectedWallet by remember { mutableStateOf<SolanaWalletType?>(null) } // 화면에서 바뀔 지갑 관련 값을 저장함
    var isWalletLoading by remember { mutableStateOf(false) } // 화면에서 바뀔 지갑 관련 값을 저장함
    var isEmailLoginLoading by remember { mutableStateOf(false) } // 화면에서 바뀔 이메일 값을 저장함
    var isGoogleLoginLoading by remember { mutableStateOf(false) } // 화면에서 바뀔 로딩 상태를 저장함

    var pendingWalletAddress by remember { mutableStateOf<String?>(null) } // 화면에서 바뀔 지갑 관련 값을 저장함
    var pendingNonce by remember { mutableStateOf<String?>(null) } // 화면에서 바뀔 pendingNonce 값을 저장함

    val walletLoginCoordinator = remember(loginViewModel) { // 화면이 다시 그려져도 지갑 관련 값을 기억함
        WalletLoginCoordinator(loginViewModel) // 로그인 관련 함수를 실행함
    }

    val googleSignInClient = remember(BuildConfig.GOOGLE_WEB_CLIENT_ID) { // 화면이 다시 그려져도 googleSignInClient 값을 기억함
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) { // 조건이 맞는지 확인함
            null
        } else { // 이 블록 안의 내용이 시작됨
            val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN) // options 값을 저장함
                .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                .requestEmail()
                .build()

            GoogleSignIn.getClient(context, options)
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult( // 화면이 다시 그려져도 googleSignInLauncher 값을 기억함
        contract = ActivityResultContracts.StartActivityForResult() // contract 값을 정해줌
    ) { result ->
        try { // 오류가 날 수 있는 코드를 먼저 시도함
            val account = GoogleSignIn // account 값을 저장함
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)

            val idToken = account.idToken // 구글 로그인 토큰을 저장함
            if (idToken.isNullOrBlank()) { // 조건이 맞는지 확인함
                isGoogleLoginLoading = false // false 값을 로딩 상태에 넣음
                Toast.makeText(context, context.getString(R.string.google_id_token_missing), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                return@rememberLauncherForActivityResult
            }

            loginViewModel.googleLogin(
                idToken = idToken, // 구글 로그인 토큰을 구글 로그인 토큰에 넣음
                onSuccess = { // 성공했을 때 실행할 함수를 정해줌
                    isGoogleLoginLoading = false // false 값을 로딩 상태에 넣음
                    Toast.makeText(context, context.getString(R.string.google_login_success), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                    onLoginClick() // 로그인 관련 함수를 실행함
                },
                onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                    isGoogleLoginLoading = false // false 값을 로딩 상태에 넣음
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                }
            )
        } catch (e: ApiException) { // 이 블록 안의 내용이 시작됨
            isGoogleLoginLoading = false // false 값을 로딩 상태에 넣음
            Toast.makeText(context, context.getString(R.string.google_login_failed_with_code, e.statusCode), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
        } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
            isGoogleLoginLoading = false // false 값을 로딩 상태에 넣음
            Toast.makeText(context, e.message ?: context.getString(R.string.google_login_failed), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
        }
    }

    fun startWalletLogin(walletType: SolanaWalletType) { // 로그인 기능을 실행하는 함수 시작
        selectedWallet = walletType // 지갑 값을 요청값에 넣음
        showWalletDialog = false // false 값을 지갑 관련 값에 넣음

        if (walletType == SolanaWalletType.PHANTOM || walletType == SolanaWalletType.SOLFLARE) {
            isWalletLoading = true
            pendingWalletAddress = null
            pendingNonce = null
            phantomConnector.clearPendingLogin()
            solflareConnector.clearPendingLogin()
            val opened = if (walletType == SolanaWalletType.PHANTOM) {
                phantomConnector.connect()
            } else {
                solflareConnector.connect()
            }
            if (!opened) {
                isWalletLoading = false
                val walletName = if (walletType == SolanaWalletType.PHANTOM) "Phantom" else "Solflare"
                Toast.makeText(context, "${walletName} 지갑 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        scope.launch { // 이 블록 안의 내용이 시작됨
            isWalletLoading = true // true 값을 지갑 관련 값에 넣음

            walletLoginCoordinator.loginWithWallet(
                walletType = walletType, // 지갑 값을 요청값에 넣음
                walletActivityResultSender = walletActivityResultSender, // 지갑 값을 요청값에 넣음
                onSuccess = { accessToken, refreshToken -> // 성공했을 때 실행할 함수를 정해줌
                            isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                            val walletAddress = walletLoginCoordinator.getLastWalletAddress().orEmpty() // 지갑 주소를 저장함
                            val walletProvider = walletType.name // 지갑 이름을 저장함
                            val walletAuthToken = walletLoginCoordinator.getLastWalletAuthToken().orEmpty() // MWA 세션 토큰을 저장함
                            context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("wallet_auth_token_${walletProvider}", walletAuthToken)
                                .apply()

                            onWalletConnected( // 지갑 관련 함수를 실행함
                        accessToken,
                        refreshToken,
                        walletAddress,
                        walletProvider
                    )
                },
                onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                    isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                }
            )
        }
    }

    fun startEmailLogin() { // 로그인 기능을 실행하는 함수 시작
        val trimmedEmail = email.trim() // 이메일 값을 저장함

        if (trimmedEmail.isBlank()) { // 조건이 맞는지 확인함
            Toast.makeText(context, context.getString(R.string.email_required), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            return
        }

        if (password.isBlank()) { // 조건이 맞는지 확인함
            Toast.makeText(context, context.getString(R.string.password_required), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            return
        }

        scope.launch { // 이 블록 안의 내용이 시작됨
            isEmailLoginLoading = true // true 값을 이메일 값에 넣음

            loginViewModel.emailLogin(
                email = trimmedEmail, // 이메일 값을 이메일에 넣음
                password = password, // 비밀번호를 비밀번호에 넣음
                onSuccess = { // 성공했을 때 실행할 함수를 정해줌
                    isEmailLoginLoading = false // false 값을 이메일 값에 넣음
                    Toast.makeText(context, context.getString(R.string.email_login_success), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                    onLoginClick() // 로그인 관련 함수를 실행함
                },
                onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                    isEmailLoginLoading = false // false 값을 이메일 값에 넣음
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                }
            )
        }
    }

    fun startKakaoLogin() { // 로그인 기능을 실행하는 함수 시작
        scope.launch { // 이 블록 안의 내용이 시작됨
            loginViewModel.getKakaoLoginUrl(
                onSuccess = { response -> // 성공했을 때 실행할 함수를 정해줌
                    try { // 오류가 날 수 있는 코드를 먼저 시도함
                        val intent = Intent( // intent 값을 저장함
                            Intent.ACTION_VIEW,
                            response.auth_url.toUri()
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                        Toast.makeText( // 화면에 글자를 보여줌
                            context,
                            context.getString(R.string.kakao_login_open_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                    Toast.makeText( // 화면에 글자를 보여줌
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    fun startGoogleLogin() { // 로그인 기능을 실행하는 함수 시작
        if (BuildConfig.DEBUG) { // 조건이 맞는지 확인함
            Log.d("Spentopia", "WEB_ID=${BuildConfig.GOOGLE_WEB_CLIENT_ID}") // 개발자가 확인할 로그를 찍음
        }

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) { // 조건이 맞는지 확인함
            Toast.makeText(context, context.getString(R.string.google_web_client_id_missing), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
            return
        }

        val client = googleSignInClient ?: return // 통신 클라이언트를 저장함

        isGoogleLoginLoading = true // true 값을 로딩 상태에 넣음
        googleSignInLauncher.launch(client.signInIntent)
    }

    val isDarkTheme = colorScheme.surface == Color(0xFF111827) // 다크 테마인지 저장함

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(
                brush = if (isDarkTheme) { // brush 값을 정해줌
                    Brush.verticalGradient(
                        colors = listOf( // colors 값을 정해줌
                            Color(0xFF090B16), // Color 함수를 실행함
                            Color(0xFF111827), // Color 함수를 실행함
                            Color(0xFF24103F) // Color 함수를 실행함
                        )
                    )
                } else { // 이 블록 안의 내용이 시작됨
                    Brush.verticalGradient(
                        colors = listOf(colorScheme.background, colorScheme.background) // colors 값을 정해줌
                    )
                }
            )
            .imePadding()
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxSize()
                .padding(horizontal = 24.dp) // .padding(horizontal 값을 정해줌
                .padding(top = 8.dp, bottom = 8.dp), // .padding(top 값을 정해줌
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            SplashLikeLogoSection(
                isDarkTheme = isDarkTheme
            ) // 다크/라이트 모드에 맞는 움직이는 로고 영역을 보여줌

            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = stringResource(id = R.string.app_name), // text 값을 정해줌
                fontSize = 28.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = colorScheme.onBackground // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(3.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = stringResource(id = R.string.login_tagline), // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                color = colorScheme.onSurfaceVariant // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(16.dp)) // UI 크기나 여백 같은 모양을 정함

            LoginInputField( // 로그인 관련 함수를 실행함
                title = stringResource(id = R.string.login_email_label), // 제목을 정해줌
                value = email, // 이메일을 입력값에 넣음
                onValueChange = { email = it }, // onValueChange 때 실행할 함수를 정해줌
                placeholder = stringResource(id = R.string.login_email_placeholder), // placeholder 값을 정해줌
                keyboardType = KeyboardType.Email, // keyboardType 값을 정해줌
                leadingIcon = { // leadingIcon 값을 정해줌
                    ShimmerLeadingIcon(imageVector = Icons.Outlined.Email, isDarkTheme = isDarkTheme) // 화면에 아이콘을 보여줌
                }
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            LoginInputField( // 로그인 관련 함수를 실행함
                title = stringResource(id = R.string.login_password_label), // 제목을 정해줌
                value = password, // 비밀번호를 입력값에 넣음
                onValueChange = { password = it }, // onValueChange 때 실행할 함수를 정해줌
                placeholder = stringResource(id = R.string.login_password_placeholder), // placeholder 값을 정해줌
                keyboardType = KeyboardType.Password, // keyboardType 값을 정해줌
                visualTransformation = if (passwordVisible) { // visualTransformation 값을 정해줌
                    VisualTransformation.None
                } else { // 이 블록 안의 내용이 시작됨
                    PasswordVisualTransformation() // Password Visual Transformation 함수를 실행함
                },
                leadingIcon = { // leadingIcon 값을 정해줌
                    ShimmerLeadingIcon(imageVector = Icons.Outlined.Lock, isDarkTheme = isDarkTheme) // 화면에 아이콘을 보여줌
                },
                trailingIcon = { // trailingIcon 값을 정해줌
                    IconButton( // 누를 수 있는 버튼을 만듦
                        onClick = { // 눌렀을 때 실행할 함수를 정해줌
                            passwordVisible = !passwordVisible // 비밀번호 값을 정해줌
                        }
                    ) { // 이 블록 안의 내용이 시작됨
                        Icon( // 화면에 아이콘을 보여줌
                            imageVector = if (passwordVisible) { // imageVector 값을 정해줌
                                Icons.Outlined.VisibilityOff
                            } else { // 이 블록 안의 내용이 시작됨
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) { // contentDescription 값을 정해줌
                                stringResource(id = R.string.login_password_hide) // stringResource(id 값을 정해줌
                            } else { // 이 블록 안의 내용이 시작됨
                                stringResource(id = R.string.login_password_show) // stringResource(id 값을 정해줌
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant // tint 값을 정해줌
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            GradientLoginButton( // 누를 수 있는 버튼을 만듦
                text = if (isEmailLoginLoading) { // text 값을 정해줌
                    stringResource(id = R.string.login_button_loading) // stringResource(id 값을 정해줌
                } else { // 이 블록 안의 내용이 시작됨
                    stringResource(id = R.string.login_button) // stringResource(id 값을 정해줌
                },
                enabled = !isEmailLoginLoading && !isWalletLoading, // enabled 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    startEmailLogin() // 로그인 관련 함수를 실행함
                }
            )

            Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

            OrDivider() // Or Divider 함수를 실행함

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            LoginOptionButton( // 누를 수 있는 버튼을 만듦
                text = stringResource(id = R.string.kakao_login_button), // text 값을 정해줌
                iconRes = R.drawable.ic_kakao_login, // iconRes 값을 정해줌
                containerColor = Color(0xFFFEE500), // containerColor 값을 정해줌
                textColor = Color(0xFF191919), // textColor 값을 정해줌
                borderColor = Color.Transparent, // borderColor 값을 정해줌
                onClick = { startKakaoLogin() } // 눌렀을 때 실행할 함수를 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            LoginOptionButton( // 누를 수 있는 버튼을 만듦
                text = stringResource(id = R.string.google_login_button), // text 값을 정해줌
                iconRes = R.drawable.ic_google_login, // iconRes 값을 정해줌
                containerColor = Color.White, // containerColor 값을 정해줌
                textColor = Color(0xFF111827), // textColor 값을 정해줌
                borderColor = Color(0xFFDDE3EA), // borderColor 값을 정해줌
                enabled = !isGoogleLoginLoading && !isEmailLoginLoading && !isWalletLoading, // enabled 값을 정해줌
                onClick = { startGoogleLogin() } // 눌렀을 때 실행할 함수를 정해줌
            )

            Spacer(modifier = Modifier.height(6.dp)) // UI 크기나 여백 같은 모양을 정함

            WalletLoginOptionButton( // 누를 수 있는 버튼을 만듦
                text = stringResource(id = R.string.wallet_login_button), // text 값을 정해줌
                iconRes = R.drawable.ic_wallet_login, // iconRes 값을 정해줌
                enabled = !isWalletLoading && !isEmailLoginLoading, // enabled 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    showWalletDialog = true // true 값을 지갑 관련 값에 넣음
                }
            )
        }

        LaunchedEffect(walletCallbackUri) { // 화면이 열리거나 값이 바뀔 때 실행함
            walletCallbackUri?.let { uri ->
                if (uri.scheme == "spentopia" && uri.host == "wallet-callback") { // 조건이 맞는지 확인함
                    Log.d("Spentopia", "wallet callback=$uri") // 개발자가 확인할 로그를 찍음
                    val callbackWallet = selectedWallet ?: if (solflareConnector.isConnectCallback(uri)) {
                        SolanaWalletType.SOLFLARE
                    } else if (!solflareConnector.getPendingWalletAddress().isNullOrBlank()) {
                        SolanaWalletType.SOLFLARE
                    } else {
                        SolanaWalletType.PHANTOM
                    }
                    val isSolflareCallback = callbackWallet == SolanaWalletType.SOLFLARE
                    when { // 값 종류에 따라 실행할 코드를 나눔
                        phantomConnector.isErrorCallback(uri) || solflareConnector.isErrorCallback(uri) -> { // 이 블록 안의 내용이 시작됨
                            isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                            pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                            pendingNonce = null // null 값을 pendingNonce 값에 넣음
                            phantomConnector.clearPendingLogin()
                            solflareConnector.clearPendingLogin()
                            val message = if (isSolflareCallback) {
                                solflareConnector.parseErrorCallback(uri)
                            } else {
                                phantomConnector.parseErrorCallback(uri)
                            } // 메시지를 저장함
                            Log.e("Spentopia", "${callbackWallet.name} callback error=$message") // 개발자가 확인할 로그를 찍음
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                        }

                        phantomConnector.isConnectCallback(uri) || solflareConnector.isConnectCallback(uri) -> { // 이 블록 안의 내용이 시작됨
                            val walletAddress = if (isSolflareCallback) {
                                solflareConnector.parseConnectCallback(uri)
                            } else {
                                phantomConnector.parseConnectCallback(uri)
                            } // 지갑 주소를 저장함
                            if (walletAddress.isNullOrBlank()) { // 조건이 맞는지 확인함
                                isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                phantomConnector.clearPendingLogin()
                                solflareConnector.clearPendingLogin()
                                Log.e("Spentopia", "${callbackWallet.name} connect callback missing wallet address") // 개발자가 확인할 로그를 찍음
                                Toast.makeText(context, context.getString(R.string.wallet_address_missing), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                onWalletCallbackConsumed() // 지갑 관련 함수를 실행함
                                return@let
                            }
                            pendingWalletAddress = walletAddress // 지갑 주소를 지갑 관련 값에 넣음
                            Log.d("Spentopia", "${callbackWallet.name} connected walletAddress=$walletAddress") // 개발자가 확인할 로그를 찍음
                            scope.launch { // 이 블록 안의 내용이 시작됨
                                try { // 오류가 날 수 있는 코드를 먼저 시도함
                                    val nonceResponse = loginViewModel.getWalletNonceOnce(walletAddress) // nonceResponse 값을 저장함
                                    pendingNonce = nonceResponse.nonce // pendingNonce 값을 정해줌
                                    if (isSolflareCallback) {
                                        solflareConnector.savePendingLogin(walletAddress, nonceResponse.nonce)
                                    } else {
                                        phantomConnector.savePendingLogin(walletAddress, nonceResponse.nonce)
                                    }
                                    Log.d("Spentopia", "${callbackWallet.name} nonce issued nonce=${nonceResponse.nonce}") // 개발자가 확인할 로그를 찍음
                                    val opened = if (isSolflareCallback) {
                                        solflareConnector.signMessage(nonceResponse.message)
                                    } else {
                                        phantomConnector.signMessage(nonceResponse.message)
                                    } // opened 값을 저장함
                                    Log.d("Spentopia", "${callbackWallet.name} signMessage opened=$opened") // 개발자가 확인할 로그를 찍음
                                    if (!opened) { // 조건이 맞는지 확인함
                                        isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                        pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                        pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                        phantomConnector.clearPendingLogin()
                                        solflareConnector.clearPendingLogin()
                                        val walletName = if (isSolflareCallback) "Solflare" else "Phantom"
                                        Toast.makeText(context, "${walletName} 지갑 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                    }
                                } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                                    isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                    pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                    pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                    phantomConnector.clearPendingLogin()
                                    solflareConnector.clearPendingLogin()
                                    Log.e("Spentopia", "${callbackWallet.name} nonce/sign start failed", e) // 개발자가 확인할 로그를 찍음
                                    Toast.makeText(context, e.message ?: context.getString(R.string.wallet_nonce_failed), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                }
                            }
                        }

                        phantomConnector.isSignCallback(uri) || solflareConnector.isSignCallback(uri) -> { // 이 블록 안의 내용이 시작됨
                            val hasPendingLogin = !pendingWalletAddress.isNullOrBlank() ||
                                !pendingNonce.isNullOrBlank() ||
                                !phantomConnector.getPendingWalletAddress().isNullOrBlank() ||
                                !solflareConnector.getPendingWalletAddress().isNullOrBlank()
                            val signedTransactionFromPhantom = phantomConnector.parseSignedTransactionCallback(uri)
                            val signedTransactionFromSolflare = solflareConnector.parseSignedTransactionCallback(uri)
                            if (!hasPendingLogin &&
                                (!signedTransactionFromPhantom.isNullOrBlank() || !signedTransactionFromSolflare.isNullOrBlank())
                            ) {
                                Log.d("Spentopia", "payment transaction callback ignored on login screen") // 결제 콜백은 로그인 처리에서 제외함
                                return@let
                            }
                            val signature = if (isSolflareCallback) {
                                solflareConnector.parseSignCallback(uri)
                            } else {
                                phantomConnector.parseSignCallback(uri)
                            } // 지갑 서명값을 저장함
                            val walletAddress = pendingWalletAddress // 지갑 주소를 저장함
                                ?: if (isSolflareCallback) solflareConnector.getPendingWalletAddress() else phantomConnector.getPendingWalletAddress()
                            val nonce = pendingNonce // 서명용 난수을 저장함
                                ?: if (isSolflareCallback) solflareConnector.getPendingNonce() else phantomConnector.getPendingNonce()

                            if (signature.isNullOrBlank()) { // 조건이 맞는지 확인함
                                isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                phantomConnector.clearPendingLogin()
                                solflareConnector.clearPendingLogin()
                                Log.e("Spentopia", "${callbackWallet.name} sign callback missing signature") // 개발자가 확인할 로그를 찍음
                                Toast.makeText(context, context.getString(R.string.wallet_signature_missing), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                onWalletCallbackConsumed() // 지갑 관련 함수를 실행함
                                return@let
                            }
                            if (walletAddress.isNullOrBlank() || nonce.isNullOrBlank()) { // 조건이 맞는지 확인함
                                isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                phantomConnector.clearPendingLogin()
                                solflareConnector.clearPendingLogin()
                                Log.e("Spentopia", "${callbackWallet.name} login state lost wallet=$walletAddress nonce=$nonce") // 개발자가 확인할 로그를 찍음
                                Toast.makeText(context, context.getString(R.string.wallet_login_state_lost), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                onWalletCallbackConsumed() // 지갑 관련 함수를 실행함
                                return@let
                            }

                            loginViewModel.walletLoginApp(
                                walletAddress = walletAddress, // 지갑 주소를 지갑 주소에 넣음
                                nonce = nonce, // 서명용 난수를 서명용 난수에 넣음
                                signature = signature, // 지갑 서명값을 지갑 서명값에 넣음
                                onSuccess = { response -> // 성공했을 때 실행할 함수를 정해줌
                                    Log.d("Spentopia", "${callbackWallet.name} walletLoginApp success accessTokenBlank=${response.access_token.isBlank()} refreshTokenBlank=${response.refresh_token.isBlank()}") // 개발자가 확인할 로그를 찍음
                                    isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                    pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                    pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                    phantomConnector.clearPendingLogin()
                                    solflareConnector.clearPendingLogin()
                                    onWalletConnected( // 지갑 관련 함수를 실행함
                                        response.access_token,
                                        response.refresh_token,
                                        walletAddress,
                                        callbackWallet.name
                                    )
                                },
                                onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                                    Log.e("Spentopia", "${callbackWallet.name} walletLoginApp failed=$message") // 개발자가 확인할 로그를 찍음
                                    isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                                    pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                                    pendingNonce = null // null 값을 pendingNonce 값에 넣음
                                    phantomConnector.clearPendingLogin()
                                    solflareConnector.clearPendingLogin()
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                }
                            )
                        }

                        else -> { // 위 조건이 아니면 이쪽을 실행함
                            isWalletLoading = false // false 값을 지갑 관련 값에 넣음
                            pendingWalletAddress = null // null 값을 지갑 관련 값에 넣음
                            pendingNonce = null // null 값을 pendingNonce 값에 넣음
                            phantomConnector.clearPendingLogin()
                            solflareConnector.clearPendingLogin()
                            Log.e("Spentopia", "Unknown wallet callback=$uri") // 개발자가 확인할 로그를 찍음
                            Toast.makeText(context, context.getString(R.string.wallet_login_state_lost), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                        }
                    }
                    onWalletCallbackConsumed() // 지갑 관련 함수를 실행함
                }
            }
        }

        LaunchedEffect(kakaoCallbackUri) { // 화면이 열리거나 값이 바뀔 때 실행함
            kakaoCallbackUri?.let { uri ->

                val isCustomScheme = // 앱 전용 주소인지 저장함
                    uri.scheme == "spentopia" && uri.host == "kakao-callback" // uri.scheme 값을 정해줌

                val apiBaseUri = Uri.parse(BuildConfig.API_BASE_URL) // apiBaseUri 값을 저장함
                val isHttpCallback = // 웹 콜백 주소인지 저장함
                    (uri.scheme == "http" || uri.scheme == "https") && // uri.scheme 값을 정해줌
                            uri.host == apiBaseUri.host && // uri.host 값을 정해줌
                            uri.path == "/auth/kakao/callback" // uri.path 값을 정해줌

                if (isCustomScheme || isHttpCallback) { // 조건이 맞는지 확인함
                    val code = uri.getQueryParameter("code") // 인증 코드를 저장함
                    val state = uri.getQueryParameter("state") // 상태값을 저장함

                    if (!code.isNullOrBlank() && !state.isNullOrBlank()) { // 조건이 맞는지 확인함
                        loginViewModel.kakaoLogin(
                            code = code, // 인증 코드를 인증 코드에 넣음
                            state = state, // 상태값을 상태값에 넣음
                            onSuccess = { // 성공했을 때 실행할 함수를 정해줌
                                Toast.makeText(context, context.getString(R.string.kakao_login_success), Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                                onLoginClick() // 로그인 관련 함수를 실행함
                            },
                            onError = { message -> // 실패했을 때 실행할 함수를 정해줌
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show() // 화면에 글자를 보여줌
                            }
                        )
                    }

                    onKakaoCallbackConsumed() // on Kakao Callback Consumed 함수를 실행함
                }
            }
        }

        if (showWalletDialog) { // 조건이 맞는지 확인함
            SolanaWalletDialog( // 지갑 관련 함수를 실행함
                onDismiss = { showWalletDialog = false }, // 닫을 때 실행할 함수를 정해줌
                onSelectWallet = { walletType -> startWalletLogin(walletType) } // 지갑 관련 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun SplashLikeLogoSection( // SplashLikeLogoSection 함수를 선언함
    isDarkTheme: Boolean // 다크모드인지 라이트모드인지 받음
) {
    val transition = rememberInfiniteTransition(label = "login-splash-logo") // 화면이 다시 그려져도 transition 값을 기억함

    val logoAlpha by transition.animateFloat( // logoAlpha 값을 저장함
        initialValue = 0.65f, // initialValue 값을 정해줌
        targetValue = 1f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(durationMillis = 1800), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "logo-alpha" // label 값을 정해줌
    )

    val logoScale by transition.animateFloat( // logoScale 값을 저장함
        initialValue = 0.96f, // initialValue 값을 정해줌
        targetValue = 1.04f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(durationMillis = 2200), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "logo-scale" // label 값을 정해줌
    )

    val sparkleAlpha by transition.animateFloat( // sparkleAlpha 값을 저장함
        initialValue = 0.28f, // initialValue 값을 정해줌
        targetValue = 0.82f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(durationMillis = 1200), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkle-alpha" // label 값을 정해줌
    )

    val glowColor1 = if (isDarkTheme) { // 다크모드인지 확인함
        Color(0xFF7C3AED).copy(alpha = 0.42f) // 다크모드 보라색 빛을 정해줌
    } else {
        Color(0xFF93C5FD).copy(alpha = 0.42f) // 라이트모드 파란색 빛을 정해줌
    }

    val glowColor2 = if (isDarkTheme) { // 다크모드인지 확인함
        Color(0xFF2F80ED).copy(alpha = 0.24f) // 다크모드 파란 빛을 정해줌
    } else {
        Color(0xFFE0F2FE).copy(alpha = 0.35f) // 라이트모드 연한 하늘빛을 정해줌
    }

    val sparkleColor1 = if (isDarkTheme) { // 다크모드인지 확인함
        Color.White // 다크모드 별 색을 정해줌
    } else {
        Color(0xFF2563EB) // 라이트모드 진한 파란 별 색을 정해줌
    }

    val sparkleColor2 = if (isDarkTheme) { // 다크모드인지 확인함
        Color(0xFFD8B4FE) // 다크모드 연보라 별 색을 정해줌
    } else {
        Color(0xFF60A5FA) // 라이트모드 밝은 파란 별 색을 정해줌
    }

    val sparkleColor3 = if (isDarkTheme) { // 다크모드인지 확인함
        Color(0xFFC7D2FE) // 다크모드 연보라 파란 별 색을 정해줌
    } else {
        Color(0xFF93C5FD) // 라이트모드 연한 파란 별 색을 정해줌
    }

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .size(220.dp),
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) {
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .size(200.dp)
                .background(
                    brush = Brush.radialGradient( // brush 값을 정해줌
                        colors = listOf( // colors 값을 정해줌
                            glowColor1, // 첫 번째 빛 색을 넣음
                            glowColor2, // 두 번째 빛 색을 넣음
                            Color.Transparent // 투명색을 넣음
                        )
                    ),
                    shape = CircleShape // CircleShape 값을 shape 값에 넣음
                )
        )

        Text(
            text = "✦",
            fontSize = 20.sp,
            color = sparkleColor1.copy(alpha = sparkleAlpha),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 26.dp)
        )

        Text(
            text = "✧",
            fontSize = 18.sp,
            color = sparkleColor1.copy(alpha = sparkleAlpha * 0.95f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 44.dp)
        )

        Text(
            text = "✦",
            fontSize = 19.sp,
            color = sparkleColor1.copy(alpha = sparkleAlpha * 0.82f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 32.dp)
        )

        Text(
            text = "✧",
            fontSize = 16.sp,
            color = sparkleColor1.copy(alpha = sparkleAlpha * 0.78f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 44.dp)
        )

        Text(
            text = "✦",
            fontSize = 14.sp,
            color = sparkleColor2.copy(alpha = sparkleAlpha * 0.85f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, top = 12.dp)
        )

        Text(
            text = "✧",
            fontSize = 13.sp,
            color = sparkleColor3.copy(alpha = sparkleAlpha * 0.8f),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp, top = 4.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.ic_spentopia_logo),
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .graphicsLayer {
                    alpha = logoAlpha
                    scaleX = logoScale
                    scaleY = logoScale
                },
            contentScale = ContentScale.Fit
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun LoginInputField( // 로그인 기능을 실행하는 함수 시작
    title: String, // 제목을 받음
    value: String, // 입력값을 받음
    onValueChange: (String) -> Unit, // onValueChange 때 실행할 함수를 받음
    placeholder: String, // placeholder 값을 받음
    keyboardType: KeyboardType, // keyboardType 값을 받음
    visualTransformation: VisualTransformation = VisualTransformation.None, // visualTransformation 값을 받음
    leadingIcon: @Composable (() -> Unit)? = null, // leadingIcon 값을 받음
    trailingIcon: @Composable (() -> Unit)? = null // trailingIcon 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalAlignment = Alignment.Start // horizontalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text( // 화면에 글자를 보여줌
            text = title, // 제목을 text 값에 넣음
            fontSize = 14.sp, // fontSize 값을 정해줌
            fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
            color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
        )
        Spacer(modifier = Modifier.height(7.dp)) // UI 크기나 여백 같은 모양을 정함
        OutlinedTextField( // 사용자가 입력할 칸을 만듦
            value = value, // 입력값을 입력값에 넣음
            onValueChange = onValueChange, // onValueChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .height(54.dp),
            placeholder = { // placeholder 값을 정해줌
                Text(text = placeholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) // 화면에 글자를 보여줌
            },
            singleLine = true, // true 값을 singleLine 값에 넣음
            visualTransformation = visualTransformation, // visualTransformation 값을 visualTransformation 값에 넣음
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType), // keyboardOptions 값을 정해줌
            leadingIcon = leadingIcon, // leadingIcon 값을 leadingIcon 값에 넣음
            trailingIcon = trailingIcon, // trailingIcon 값을 trailingIcon 값에 넣음
            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
            colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // focusedContainerColor 값을 정해줌
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, // unfocusedContainerColor 값을 정해줌
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // focusedBorderColor 값을 정해줌
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                focusedTextColor = MaterialTheme.colorScheme.onSurface, // focusedTextColor 값을 정해줌
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface, // unfocusedTextColor 값을 정해줌
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // focusedPlaceholderColor 값을 정해줌
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant, // unfocusedPlaceholderColor 값을 정해줌
                cursorColor = MaterialTheme.colorScheme.primary // cursorColor 값을 정해줌
            )
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun GradientLoginButton( // 로그인 기능을 실행하는 함수 시작
    text: String, // text 값을 받음
    enabled: Boolean = true, // enabled 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    Button( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        enabled = enabled, // enabled 값을 enabled 값에 넣음
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
            },
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = MaterialTheme.colorScheme.primaryContainer, // containerColor 값을 정해줌
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer, // contentColor 값을 정해줌
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
        ),
        contentPadding = PaddingValues(0.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxSize()
                .graphicsLayer { // 이 블록 안의 내용이 시작됨
                    alpha = if (enabled) 1f else 0.55f // alpha 값을 정해줌
                }
                .shadow(
                    elevation = if (enabled) 10.dp else 0.dp, // elevation 값을 정해줌
                    shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), // ambientColor 값을 정해줌
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) // spotColor 값을 정해줌
                ),
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = text, // text 값을 text 값에 넣음
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun LoginOptionButton( // 로그인 기능을 실행하는 함수 시작
    text: String, // text 값을 받음
    iconRes: Int, // iconRes 값을 받음
    containerColor: Color, // containerColor 값을 받음
    textColor: Color, // textColor 값을 받음
    borderColor: Color, // borderColor 값을 받음
    enabled: Boolean = true, // enabled 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val interactionSource = remember { MutableInteractionSource() } // 화면이 다시 그려져도 interactionSource 값을 기억함
    val pressed by interactionSource.collectIsPressedAsState() // pressed 값을 저장함

    Button( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        enabled = enabled, // enabled 값을 enabled 값에 넣음
        interactionSource = interactionSource, // interactionSource 값을 interactionSource 값에 넣음
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer { // 이 블록 안의 내용이 시작됨
                scaleX = if (pressed) 0.985f else 1f // scaleX 값을 정해줌
                scaleY = if (pressed) 0.985f else 1f // scaleY 값을 정해줌
            }
            .border(
                border = BorderStroke(1.dp, borderColor), // border 값을 정해줌
                shape = RoundedCornerShape(15.dp) // shape 값을 정해줌
            ),
        shape = RoundedCornerShape(15.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = containerColor, // containerColor 값을 containerColor 값에 넣음
            disabledContainerColor = Color(0xFFF2F4F7) // disabledContainerColor 값을 정해줌
        ),
        contentPadding = PaddingValues(horizontal = 20.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier.fillMaxSize(), // UI 크기나 여백 같은 모양을 정함
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            StaticButtonShine( // Static Button Shine 함수를 실행함
                shape = RoundedCornerShape(15.dp), // shape 값을 정해줌
                pressed = pressed // pressed 값을 pressed 값에 넣음
            )

            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Image( // 화면에 이미지를 보여줌
                    painter = painterResource(id = iconRes), // painter 값을 정해줌
                    contentDescription = text, // text 값을 contentDescription 값에 넣음
                    modifier = Modifier.size(25.dp), // UI 크기나 여백 같은 모양을 정함
                    contentScale = ContentScale.Fit // contentScale 값을 정해줌
                )
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = text, // text 값을 text 값에 넣음
                        color = textColor, // textColor 값을 color 값에 넣음
                        fontSize = 15.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                    )
                }
                Spacer(modifier = Modifier.size(25.dp)) // UI 크기나 여백 같은 모양을 정함
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletLoginOptionButton( // 로그인 기능을 실행하는 함수 시작
    text: String, // text 값을 받음
    iconRes: Int, // iconRes 값을 받음
    enabled: Boolean = true, // enabled 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Button( // 누를 수 있는 버튼을 만듦
        onClick = onClick, // 눌렀을 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
        enabled = enabled, // enabled 값을 enabled 값에 넣음
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(15.dp), // shape 값을 정해줌
        colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
            containerColor = Color.Transparent, // containerColor 값을 정해줌
            disabledContainerColor = Color(0xFFF2F4F7) // disabledContainerColor 값을 정해줌
        ),
        contentPadding = PaddingValues(0.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient( // brush 값을 정해줌
                        colors = SpentopiaWalletGradientColors // 지갑 관련 값을 colors 값에 넣음
                    ),
                    shape = RoundedCornerShape(15.dp) // shape 값을 정해줌
                )
                .border(
                    border = BorderStroke(1.dp, SpentopiaGlowPurple), // border 값을 정해줌
                    shape = RoundedCornerShape(15.dp) // shape 값을 정해줌
                )
                .padding(horizontal = 20.dp), // .padding(horizontal 값을 정해줌
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Row( // 안쪽 UI를 가로로 배치함
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Image( // 화면에 이미지를 보여줌
                    painter = painterResource(id = iconRes), // painter 값을 정해줌
                    contentDescription = text, // text 값을 contentDescription 값에 넣음
                    modifier = Modifier.size(27.dp), // UI 크기나 여백 같은 모양을 정함
                    contentScale = ContentScale.Fit // contentScale 값을 정해줌
                )
                Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
                    modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
                    contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = text, // text 값을 text 값에 넣음
                        color = Color.White, // color 값을 정해줌
                        fontSize = 15.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
                    )
                }
                Spacer(modifier = Modifier.size(27.dp)) // UI 크기나 여백 같은 모양을 정함
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun StaticButtonShine( // StaticButtonShine 함수를 선언함
    shape: RoundedCornerShape, // shape 값을 받음
    pressed: Boolean = false // pressed 값을 받음
) { // 이 블록 안의 내용이 시작됨
    val transition = rememberInfiniteTransition(label = "button-shine") // 화면이 다시 그려져도 transition 값을 기억함
    val shineAlpha by transition.animateFloat( // shineAlpha 값을 저장함
        initialValue = if (pressed) 0.30f else 0.18f, // initialValue 값을 정해줌
        targetValue = if (pressed) 0.46f else 0.28f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(durationMillis = 1400), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "shine-alpha" // label 값을 정해줌
    )

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient( // brush 값을 정해줌
                    colors = listOf( // colors 값을 정해줌
                        Color.Transparent,
                        Color.White.copy(alpha = shineAlpha), // Color.White.copy(alpha 값을 정해줌
                        Color.Transparent
                    )
                ),
                shape = shape // shape 값을 shape 값에 넣음
            )
    )
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ShimmerLeadingIcon( // ShimmerLeadingIcon 함수를 선언함
    imageVector: androidx.compose.ui.graphics.vector.ImageVector, // imageVector 값을 받음
    isDarkTheme: Boolean // 다크모드인지 라이트모드인지 받음
) { // 이 블록 안의 내용이 시작됨
    val transition = rememberInfiniteTransition(label = "login-icon-shimmer") // 화면이 다시 그려져도 transition 값을 기억함
    val shimmerAlpha by transition.animateFloat( // shimmerAlpha 값을 저장함
        initialValue = 0.24f, // initialValue 값을 정해줌
        targetValue = 0.56f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(durationMillis = 1200), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "icon-glow" // label 값을 정해줌
    )
    val glowColor1 = if (isDarkTheme) SpentopiaGlowPurple else Color(0xFF93C5FD) // glowColor1 값을 모드별로 정함
    val glowColor2 = if (isDarkTheme) SpentopiaMutedPurple else Color(0xFFE0F2FE) // glowColor2 값을 모드별로 정함
    val iconTint = if (isDarkTheme) SpentopiaIconMuted else Color(0xFF2563EB) // iconTint 값을 모드별로 정함

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .size(28.dp)
            .background(
                brush = Brush.radialGradient( // brush 값을 정해줌
                    colors = listOf( // colors 값을 정해줌
                        glowColor1.copy(alpha = shimmerAlpha), // glowColor1.copy(alpha 값을 정해줌
                        glowColor2.copy(alpha = shimmerAlpha * 0.55f), // glowColor2.copy(alpha 값을 정해줌
                        Color.Transparent
                    )
                ),
                shape = CircleShape // CircleShape 값을 shape 값에 넣음
            ),
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Icon( // 화면에 아이콘을 보여줌
            imageVector = imageVector, // imageVector 값을 imageVector 값에 넣음
            contentDescription = null, // null 값을 contentDescription 값에 넣음
            tint = iconTint // iconTint 값을 tint 값에 넣음
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun OrDivider() { // OrDivider 함수를 선언함
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        HorizontalDivider( // Horizontal Divider 함수를 실행함
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            color = Color(0xFFD6DCE5) // color 값을 정해줌
        )
        Text( // 화면에 글자를 보여줌
            text = "  ${stringResource(id = R.string.login_or_divider)}  ", // text 값을 정해줌
            color = Color(0xFF9AA4B2), // color 값을 정해줌
            fontSize = 13.sp // fontSize 값을 정해줌
        )
        HorizontalDivider( // Horizontal Divider 함수를 실행함
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            color = Color(0xFFD6DCE5) // color 값을 정해줌
        )
    }
}
