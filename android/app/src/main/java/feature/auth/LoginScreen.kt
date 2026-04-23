package com.ict.spentopia.feature.auth

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ict.spentopia.R
import com.ict.spentopia.feature.auth.connector.PhantomDeepLinkConnector
import com.ict.spentopia.feature.social.SocialLoginButton
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import kotlinx.coroutines.launch

enum class SolanaWalletType(
    val title: String,
    val description: String
) {
    PHANTOM("PHANTOM", "연결하려면 클릭"),
    SOLFLARE("SOLFLARE", "연결하려면 클릭"),
    BACKPACK("BACKPACK", "연결하려면 클릭")
}

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    walletActivityResultSender: ActivityResultSender,
    walletCallbackUri: Uri?,
    onWalletCallbackConsumed: () -> Unit,
    onKakaoClick: () -> Unit = {},
    onNaverClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onWalletConnected: (String, String, String, String) -> Unit = { _, _, _, _ -> } // 콜백 시그니처 (토큰2, 주소, 제공자)
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val phantomConnector = remember { PhantomDeepLinkConnector(context) }
    val loginViewModel: LoginViewModel = viewModel()

    var showWalletDialog by remember { mutableStateOf(false) }
    var selectedWallet by remember { mutableStateOf<SolanaWalletType?>(null) }
    var isWalletLoading by remember { mutableStateOf(false) }

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

                            // 수정 포인트 1: 지갑 정보 추출 및 4개의 인자 전달 (오타 수정됨)
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_spentopia_logo),
                    contentDescription = "Spentopia Logo",
                    modifier = Modifier.size(76.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Spentopia",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                Text(
                    text = "내가 기록한 소비가 나를 만든다",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(32.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "이메일",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "아이디를 입력해 주세요.",
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "비밀번호",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "비밀번호를 입력해 주세요",
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = PasswordVisualTransformation() as? KeyboardType ?: KeyboardType.Password),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFF1F5F9),
                            unfocusedContainerColor = Color(0xFFF1F5F9),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                GradientLoginButton(
                    text = "로그인",
                    onClick = onLoginClick
                )

                Spacer(modifier = Modifier.height(14.dp))

                SolanaWalletConnectButton(
                    enabled = !isWalletLoading,
                    onClick = {
                        showWalletDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                OrDivider()

                Spacer(modifier = Modifier.height(24.dp))

                SocialLoginButton(
                    text = "카카오로 로그인",
                    iconRes = R.drawable.ic_kakao,
                    onClick = onKakaoClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                SocialLoginButton(
                    text = "구글로 로그인",
                    iconRes = R.drawable.ic_google,
                    onClick = onGoogleClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                SocialLoginButton(
                    text = "네이버로 로그인",
                    iconRes = R.drawable.ic_naver,
                    onClick = onNaverClick
                )
            }
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
                                    Toast.makeText(
                                        context,
                                        e.message ?: "nonce 요청 실패",
                                        Toast.LENGTH_SHORT
                                    ).show()
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

                                    // 수정 포인트 2: Phantom 로그인 시에도 인자 4개를 전달하도록 보강
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

                        else -> {
                            isWalletLoading = false
                            Toast.makeText(
                                context,
                                "콜백: ${uri.toString().take(180)}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    onWalletCallbackConsumed()
                }
            }
        }

        if (showWalletDialog) {
            SolanaWalletDialog(
                onDismiss = {
                    showWalletDialog = false
                },
                onSelectWallet = { walletType ->
                    startWalletLogin(walletType)
                }
            )
        }
    }
}

@Composable
private fun GradientLoginButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF16B8D9), Color(0xFF2F7DF6))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun SolanaWalletConnectButton(
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFB980FF), Color(0xFF5B4BFF))
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "👻 Solana 지갑 연결",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SolanaWalletDialog(
    onDismiss: () -> Unit,
    onSelectWallet: (SolanaWalletType) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "Solana 지갑 연결",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF222222)
                )

                Spacer(modifier = Modifier.height(18.dp))

                WalletOptionItem(
                    title = SolanaWalletType.PHANTOM.title,
                    description = SolanaWalletType.PHANTOM.description,
                    onClick = { onSelectWallet(SolanaWalletType.PHANTOM) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                WalletOptionItem(
                    title = SolanaWalletType.SOLFLARE.title,
                    description = SolanaWalletType.SOLFLARE.description,
                    onClick = { onSelectWallet(SolanaWalletType.SOLFLARE) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                WalletOptionItem(
                    title = SolanaWalletType.BACKPACK.title,
                    description = SolanaWalletType.BACKPACK.description,
                    onClick = { onSelectWallet(SolanaWalletType.BACKPACK) }
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "선택한 지갑과 실제로 열리는 지갑 앱은 현재 다를 수 있습니다.",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "닫기",
                        color = Color(0xFF2563EB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletOptionItem(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE5E7EB)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2B2B2B)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = Color(0xFF8A8A8A)
                )
            }

            Text(
                text = "연결",
                color = Color(0xFF2563EB),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { onClick() }
            )
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
            color = Color(0xFFD6D6D6)
        )

        Text(
            text = "  또는  ",
            color = Color(0xFF8A8A8A),
            fontSize = 14.sp
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFD6D6D6)
        )
    }
}