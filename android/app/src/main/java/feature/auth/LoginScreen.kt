package com.ict.spentopia.feature.auth

// 로그인 화면임
// 이메일/비번, Google/Kakao, 지갑 로그인 한 화면

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.ict.spentopia.BuildConfig
import com.ict.spentopia.R
import com.ict.spentopia.feature.auth.connector.PhantomDeepLinkConnector
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple
import com.ict.spentopia.ui.theme.SpentopiaIconMuted
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaActionGradientColors
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch
import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    walletActivityResultSender: ActivityResultSender,
    walletCallbackUri: Uri?,
    onWalletCallbackConsumed: () -> Unit,
    kakaoCallbackUri: Uri?,
    onKakaoCallbackConsumed: () -> Unit,
    onKakaoClick: () -> Unit = {},
    onNaverClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onFindEmailClick: () -> Unit = {},
    onFindPasswordClick: () -> Unit = {},
    onWalletConnected: (String, String, String, String) -> Unit = { _, _, _, _ -> }
) {
    val colorScheme = MaterialTheme.colorScheme
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val phantomConnector = remember { PhantomDeepLinkConnector(context) }
    val loginViewModel: LoginViewModel = viewModel()

    var showWalletDialog by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<SolanaWalletType?>(null) }
    var isWalletLoading by remember { mutableStateOf(false) }
    var isEmailLoginLoading by remember { mutableStateOf(false) }
    var isGoogleLoginLoading by remember { mutableStateOf(false) }

    var pendingWalletAddress by remember { mutableStateOf<String?>(null) }
    var pendingNonce by remember { mutableStateOf<String?>(null) }

    val walletLoginCoordinator = remember(loginViewModel) {
        WalletLoginCoordinator(loginViewModel)
    }

    val googleSignInClient = remember {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .requestEmail()
            .build()

        GoogleSignIn.getClient(context, options)
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val account = GoogleSignIn
                .getSignedInAccountFromIntent(result.data)
                .getResult(ApiException::class.java)

            val idToken = account.idToken
            if (idToken.isNullOrBlank()) {
                isGoogleLoginLoading = false
                Toast.makeText(context, context.getString(R.string.google_id_token_missing), Toast.LENGTH_SHORT).show()
                return@rememberLauncherForActivityResult
            }

            loginViewModel.googleLogin(
                idToken = idToken,
                onSuccess = {
                    isGoogleLoginLoading = false
                    Toast.makeText(context, context.getString(R.string.google_login_success), Toast.LENGTH_SHORT).show()
                    onLoginClick()
                },
                onError = { message ->
                    isGoogleLoginLoading = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: ApiException) {
            isGoogleLoginLoading = false
            Toast.makeText(context, context.getString(R.string.google_login_failed_with_code, e.statusCode), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            isGoogleLoginLoading = false
            Toast.makeText(context, e.message ?: context.getString(R.string.google_login_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun startWalletLogin(walletType: SolanaWalletType) {
        selectedWallet = walletType
        showWalletDialog = false

        when (walletType) {
            SolanaWalletType.PHANTOM -> {
                isWalletLoading = true
                pendingWalletAddress = null
                pendingNonce = null
                phantomConnector.connect()
            }

            else -> {
                scope.launch {
                    isWalletLoading = true

                    walletLoginCoordinator.loginWithWallet(
                        walletType = walletType,
                        walletActivityResultSender = walletActivityResultSender,
                        onSuccess = { accessToken, refreshToken ->
                            isWalletLoading = false
                            val walletAddress = walletLoginCoordinator.getLastWalletAddress().orEmpty()
                            val walletProvider = walletType.name

                            onWalletConnected(
                                accessToken,
                                refreshToken,
                                walletAddress,
                                walletProvider
                            )
                        },
                        onError = { message ->
                            isWalletLoading = false
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    fun startEmailLogin() {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            Toast.makeText(context, context.getString(R.string.email_required), Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isBlank()) {
            Toast.makeText(context, context.getString(R.string.password_required), Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isEmailLoginLoading = true

            loginViewModel.emailLogin(
                email = trimmedEmail,
                password = password,
                onSuccess = {
                    isEmailLoginLoading = false
                    Toast.makeText(context, context.getString(R.string.email_login_success), Toast.LENGTH_SHORT).show()
                    onLoginClick()
                },
                onError = { message ->
                    isEmailLoginLoading = false
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    fun startKakaoLogin() {
        scope.launch {
            loginViewModel.getKakaoLoginUrl(
                onSuccess = { response ->
                    try {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            response.auth_url.toUri()
                        )
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.kakao_login_open_failed),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                onError = { message ->
                    Toast.makeText(
                        context,
                        message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    fun startGoogleLogin() {
        if (BuildConfig.DEBUG) {
            Log.d("Spentopia", "WEB_ID=${BuildConfig.GOOGLE_WEB_CLIENT_ID}")
        }

        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            Toast.makeText(context, context.getString(R.string.google_web_client_id_missing), Toast.LENGTH_SHORT).show()
            return
        }

        isGoogleLoginLoading = true
        googleSignInLauncher.launch(googleSignInClient.signInIntent)
    }

    val isDarkTheme = colorScheme.surface == Color(0xFF111827)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = if (isDarkTheme) {
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF090B16),
                            Color(0xFF111827),
                            Color(0xFF24103F)
                        )
                    )
                } else {
                    Brush.verticalGradient(
                        colors = listOf(colorScheme.background, colorScheme.background)
                    )
                }
            )
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isDarkTheme) {
                SplashLikeLogoSection()
            } else {
                Image(
                    painter = painterResource(id = R.drawable.ic_spentopia_logo),
                    contentDescription = stringResource(id = R.string.spentopia_logo_content_description),
                    modifier = Modifier.size(200.dp),
                    contentScale = ContentScale.Fit
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(id = R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = stringResource(id = R.string.login_tagline),
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            LoginInputField(
                title = stringResource(id = R.string.login_email_label),
                value = email,
                onValueChange = { email = it },
                placeholder = stringResource(id = R.string.login_email_placeholder),
                keyboardType = KeyboardType.Email,
                leadingIcon = {
                    ShimmerLeadingIcon(imageVector = Icons.Outlined.Email)
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            LoginInputField(
                title = stringResource(id = R.string.login_password_label),
                value = password,
                onValueChange = { password = it },
                placeholder = stringResource(id = R.string.login_password_placeholder),
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                leadingIcon = {
                    ShimmerLeadingIcon(imageVector = Icons.Outlined.Lock)
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Icon(
                            imageVector = if (passwordVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = if (passwordVisible) {
                                stringResource(id = R.string.login_password_hide)
                            } else {
                                stringResource(id = R.string.login_password_show)
                            },
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            GradientLoginButton(
                text = if (isEmailLoginLoading) {
                    stringResource(id = R.string.login_button_loading)
                } else {
                    stringResource(id = R.string.login_button)
                },
                enabled = !isEmailLoginLoading && !isWalletLoading,
                onClick = {
                    startEmailLogin()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            OrDivider()

            Spacer(modifier = Modifier.height(8.dp))

            LoginOptionButton(
                text = stringResource(id = R.string.kakao_login_button),
                iconRes = R.drawable.ic_kakao_login,
                containerColor = Color(0xFFFEE500),
                textColor = Color(0xFF191919),
                borderColor = Color.Transparent,
                onClick = { startKakaoLogin() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            LoginOptionButton(
                text = stringResource(id = R.string.google_login_button),
                iconRes = R.drawable.ic_google_login,
                containerColor = Color.White,
                textColor = Color(0xFF111827),
                borderColor = Color(0xFFDDE3EA),
                enabled = !isGoogleLoginLoading && !isEmailLoginLoading && !isWalletLoading,
                onClick = { startGoogleLogin() }
            )

            Spacer(modifier = Modifier.height(6.dp))

            WalletLoginOptionButton(
                text = stringResource(id = R.string.wallet_login_button),
                iconRes = R.drawable.ic_wallet_login,
                enabled = !isWalletLoading && !isEmailLoginLoading,
                onClick = {
                    showWalletDialog = true
                }
            )
        }

        LaunchedEffect(walletCallbackUri) {
            walletCallbackUri?.let { uri ->
                if (uri.scheme == "spentopia" && uri.host == "wallet-callback") {
                    when {
                        phantomConnector.isConnectCallback(uri) -> {
                            val walletAddress = phantomConnector.parseConnectCallback(uri)
                            if (walletAddress.isNullOrBlank()) {
                                isWalletLoading = false
                                Toast.makeText(context, context.getString(R.string.wallet_address_missing), Toast.LENGTH_SHORT).show()
                                onWalletCallbackConsumed()
                                return@let
                            }
                            pendingWalletAddress = walletAddress
                            scope.launch {
                                try {
                                    val nonceResponse = loginViewModel.getWalletNonceOnce(walletAddress)
                                    pendingNonce = nonceResponse.nonce
                                    phantomConnector.signMessage(nonceResponse.message)
                                } catch (e: Exception) {
                                    isWalletLoading = false
                                    Toast.makeText(context, e.message ?: context.getString(R.string.wallet_nonce_failed), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        phantomConnector.isSignCallback(uri) -> {
                            val signature = phantomConnector.parseSignCallback(uri)
                            val walletAddress = pendingWalletAddress
                            val nonce = pendingNonce

                            if (signature.isNullOrBlank()) {
                                isWalletLoading = false
                                Toast.makeText(context, context.getString(R.string.wallet_signature_missing), Toast.LENGTH_SHORT).show()
                                onWalletCallbackConsumed()
                                return@let
                            }
                            if (walletAddress.isNullOrBlank() || nonce.isNullOrBlank()) {
                                isWalletLoading = false
                                Toast.makeText(context, context.getString(R.string.wallet_login_state_lost), Toast.LENGTH_SHORT).show()
                                onWalletCallbackConsumed()
                                return@let
                            }

                            loginViewModel.walletLoginApp(
                                walletAddress = walletAddress,
                                nonce = nonce,
                                signature = signature,
                                onSuccess = { response ->
                                    isWalletLoading = false
                                    pendingWalletAddress = null
                                    pendingNonce = null
                                    onWalletConnected(
                                        response.access_token,
                                        response.refresh_token,
                                        walletAddress,
                                        "PHANTOM"
                                    )
                                },
                                onError = { message ->
                                    isWalletLoading = false
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                    onWalletCallbackConsumed()
                }
            }
        }

        LaunchedEffect(kakaoCallbackUri) {
            kakaoCallbackUri?.let { uri ->

                val isCustomScheme =
                    uri.scheme == "spentopia" && uri.host == "kakao-callback"

                val isHttpCallback =
                    uri.scheme == "http" &&
                            uri.host == "10.0.2.2" &&
                            uri.path == "/auth/kakao/callback"

                if (isCustomScheme || isHttpCallback) {
                    val code = uri.getQueryParameter("code")
                    val state = uri.getQueryParameter("state")

                    if (!code.isNullOrBlank() && !state.isNullOrBlank()) {
                        loginViewModel.kakaoLogin(
                            code = code,
                            state = state,
                            onSuccess = {
                                Toast.makeText(context, context.getString(R.string.kakao_login_success), Toast.LENGTH_SHORT).show()
                                onLoginClick()
                            },
                            onError = { message ->
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }

                    onKakaoCallbackConsumed()
                }
            }
        }

        if (showWalletDialog) {
            SolanaWalletDialog(
                onDismiss = { showWalletDialog = false },
                onSelectWallet = { walletType -> startWalletLogin(walletType) }
            )
        }
    }
}

@Composable
private fun SplashLikeLogoSection() {
    val transition = rememberInfiniteTransition(label = "login-splash-logo")
    val logoAlpha by transition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-alpha"
    )
    val logoScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo-scale"
    )
    val sparkleAlpha by transition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle-alpha"
    )

    Box(
        modifier = Modifier
            .size(220.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7C3AED).copy(alpha = 0.42f),
                        Color(0xFF2F80ED).copy(alpha = 0.24f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            )
        )

        Text(
            text = "✦",
            fontSize = 20.sp,
            color = Color.White.copy(alpha = sparkleAlpha),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 18.dp, top = 26.dp)
        )
        Text(
            text = "✧",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = sparkleAlpha * 0.95f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 22.dp, top = 44.dp)
        )
        Text(
            text = "✦",
            fontSize = 19.sp,
            color = Color.White.copy(alpha = sparkleAlpha * 0.82f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 32.dp)
        )
        Text(
            text = "✧",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = sparkleAlpha * 0.78f),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 30.dp, bottom = 44.dp)
        )
        Text(
            text = "✦",
            fontSize = 14.sp,
            color = Color(0xFFD8B4FE).copy(alpha = sparkleAlpha * 0.85f),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp, top = 12.dp)
        )
        Text(
            text = "✧",
            fontSize = 13.sp,
            color = Color(0xFFC7D2FE).copy(alpha = sparkleAlpha * 0.8f),
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

@Composable
private fun LoginInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            placeholder = {
                Text(text = placeholder, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@Composable
private fun GradientLoginButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.985f else 1f
                scaleY = if (pressed) 0.985f else 1f
            },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = if (enabled) 1f else 0.55f
                }
                .shadow(
                    elevation = if (enabled) 10.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color(0xFF7C3AED).copy(alpha = 0.32f),
                    spotColor = Color(0xFF2F80ED).copy(alpha = 0.22f)
                )
                .background(
                    brush = Brush.horizontalGradient(
                        colors = SpentopiaActionGradientColors
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .border(
                    border = BorderStroke(1.dp, SpentopiaGlowPurple.copy(alpha = 0.38f)),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            StaticButtonShine(
                shape = RoundedCornerShape(16.dp),
                pressed = pressed
            )

            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LoginOptionButton(
    text: String,
    iconRes: Int,
    containerColor: Color,
    textColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .graphicsLayer {
                scaleX = if (pressed) 0.985f else 1f
                scaleY = if (pressed) 0.985f else 1f
            }
            .border(
                border = BorderStroke(1.dp, borderColor),
                shape = RoundedCornerShape(15.dp)
            ),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            disabledContainerColor = Color(0xFFF2F4F7)
        ),
        contentPadding = PaddingValues(horizontal = 20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            StaticButtonShine(
                shape = RoundedCornerShape(15.dp),
                pressed = pressed
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(25.dp),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(25.dp))
            }
        }
    }
}

@Composable
private fun WalletLoginOptionButton(
    text: String,
    iconRes: Int,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(15.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = Color(0xFFF2F4F7)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = SpentopiaWalletGradientColors
                    ),
                    shape = RoundedCornerShape(15.dp)
                )
                .border(
                    border = BorderStroke(1.dp, SpentopiaGlowPurple),
                    shape = RoundedCornerShape(15.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = text,
                    modifier = Modifier.size(27.dp),
                    contentScale = ContentScale.Fit
                )
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.size(27.dp))
            }
        }
    }
}

@Composable
private fun StaticButtonShine(
    shape: RoundedCornerShape,
    pressed: Boolean = false
) {
    val transition = rememberInfiniteTransition(label = "button-shine")
    val shineAlpha by transition.animateFloat(
        initialValue = if (pressed) 0.30f else 0.18f,
        targetValue = if (pressed) 0.46f else 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shine-alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = shineAlpha),
                        Color.Transparent
                    )
                ),
                shape = shape
            )
    )
}

@Composable
private fun ShimmerLeadingIcon(
    imageVector: androidx.compose.ui.graphics.vector.ImageVector
) {
    val transition = rememberInfiniteTransition(label = "login-icon-shimmer")
    val shimmerAlpha by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.56f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon-glow"
    )

    Box(
        modifier = Modifier
            .size(28.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SpentopiaGlowPurple.copy(alpha = shimmerAlpha),
                        SpentopiaMutedPurple.copy(alpha = shimmerAlpha * 0.55f),
                        Color.Transparent
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = SpentopiaIconMuted
        )
    }
}

@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFD6DCE5)
        )
        Text(
            text = "  ${stringResource(id = R.string.login_or_divider)}  ",
            color = Color(0xFF9AA4B2),
            fontSize = 13.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFD6DCE5)
        )
    }
}
