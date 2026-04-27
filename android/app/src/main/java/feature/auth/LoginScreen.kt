package com.ict.spentopia.feature.auth

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ict.spentopia.R
import com.ict.spentopia.feature.auth.connector.PhantomDeepLinkConnector
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch
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

    var pendingWalletAddress by remember { mutableStateOf<String?>(null) }
    var pendingNonce by remember { mutableStateOf<String?>(null) }

    val walletLoginCoordinator = remember(loginViewModel) {
        WalletLoginCoordinator(loginViewModel)
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

    // 수정된 이메일 로그인 함수
    fun startEmailLogin() {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            Toast.makeText(context, "이메일을 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.isBlank()) {
            Toast.makeText(context, "비밀번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isEmailLoginLoading = true

            loginViewModel.emailLogin(
                email = trimmedEmail,
                password = password,
                onSuccess = {
                    isEmailLoginLoading = false
                    Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                    // 로그인 성공 시에만 홈 화면으로 이동
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
                            "카카오 로그인 화면을 열 수 없습니다.",
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F8FC))
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 26.dp)
                .padding(top = 34.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_spentopia_logo),
                contentDescription = "Spentopia Logo",
                modifier = Modifier.size(74.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Spentopia",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "내가 기록한 소비가 나를 만든다",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(34.dp))

            LoginInputField(
                title = "이메일",
                value = email,
                onValueChange = { email = it },
                placeholder = "이메일을 입력해주세요",
                keyboardType = KeyboardType.Email
            )

            Spacer(modifier = Modifier.height(14.dp))

            LoginInputField(
                title = "비밀번호",
                value = password,
                onValueChange = { password = it },
                placeholder = "비밀번호를 입력해주세요",
                keyboardType = KeyboardType.Password,
                visualTransformation = if (passwordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            passwordVisible = !passwordVisible
                        }
                    ) {
                        Text(
                            text = if (passwordVisible) "숨김" else "보기",
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            GradientLoginButton(
                text = if (isEmailLoginLoading) "로그인 중..." else "로그인",
                enabled = !isEmailLoginLoading && !isWalletLoading,
                onClick = {
                    startEmailLogin()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onFindEmailClick) {
                    Text(
                        text = "이메일 찾기",
                        fontSize = 13.sp,
                        color = Color(0xFF8A94A6)
                    )
                }

                Text(
                    text = "|",
                    fontSize = 13.sp,
                    color = Color(0xFFC4CBD6)
                )

                TextButton(onClick = onFindPasswordClick) {
                    Text(
                        text = "비밀번호 찾기",
                        fontSize = 13.sp,
                        color = Color(0xFF8A94A6)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OrDivider()

            Spacer(modifier = Modifier.height(18.dp))

            LoginOptionButton(
                text = "카카오 로그인",
                iconRes = R.drawable.ic_kakao_login,
                containerColor = Color(0xFFFEE500),
                textColor = Color(0xFF191919),
                borderColor = Color.Transparent,
                onClick = { startKakaoLogin() }
            )

            Spacer(modifier = Modifier.height(10.dp))

            LoginOptionButton(
                text = "구글 로그인",
                iconRes = R.drawable.ic_google_login,
                containerColor = Color.White,
                textColor = Color(0xFF111827),
                borderColor = Color(0xFFDDE3EA),
                onClick = onGoogleClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            WalletLoginOptionButton(
                text = "지갑으로 로그인",
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
                                Toast.makeText(context, "지갑 주소를 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, e.message ?: "nonce 요청 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        phantomConnector.isSignCallback(uri) -> {
                            val signature = phantomConnector.parseSignCallback(uri)
                            val walletAddress = pendingWalletAddress
                            val nonce = pendingNonce

                            if (signature.isNullOrBlank()) {
                                isWalletLoading = false
                                Toast.makeText(context, "서명값을 가져오지 못했습니다.", Toast.LENGTH_SHORT).show()
                                onWalletCallbackConsumed()
                                return@let
                            }
                            if (walletAddress.isNullOrBlank() || nonce.isNullOrBlank()) {
                                isWalletLoading = false
                                Toast.makeText(context, "로그인 중간 상태가 유실되었습니다.", Toast.LENGTH_SHORT).show()
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
                                Toast.makeText(context, "카카오 로그인 성공", Toast.LENGTH_SHORT).show()
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
private fun LoginInputField(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    visualTransformation: VisualTransformation = VisualTransformation.None,
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
            color = Color(0xFF1F2937)
        )
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            placeholder = {
                Text(text = placeholder, fontSize = 14.sp, color = Color(0xFF8B95A1))
            },
            singleLine = true,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color(0xFFE0E5EC),
                unfocusedBorderColor = Color(0xFFE0E5EC),
                cursorColor = Color(0xFF2F7DF6)
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF17BEDA), Color(0xFF2F7DF6))
                    ),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
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
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
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
                        colors = listOf(
                            Color(0xFF111827),
                            Color(0xFF1B2942),
                            Color(0xFF2D1847)
                        )
                    ),
                    shape = RoundedCornerShape(15.dp)
                )
                .border(
                    border = BorderStroke(1.dp, Color(0xFF7C3AED)),
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
            text = "  또는  ",
            color = Color(0xFF9AA4B2),
            fontSize = 13.sp
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFD6DCE5)
        )
    }
}