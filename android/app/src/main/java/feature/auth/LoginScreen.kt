package com.ict.spentopia.feature.auth

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ict.spentopia.R
import com.ict.spentopia.feature.social.SocialLoginButton
import com.solana.mobilewalletadapter.clientlib.ActivityResultSender
import com.solana.mobilewalletadapter.clientlib.ConnectionIdentity
import com.solana.mobilewalletadapter.clientlib.MobileWalletAdapter
import com.solana.mobilewalletadapter.clientlib.TransactionResult
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    walletActivityResultSender: ActivityResultSender,
    onKakaoClick: () -> Unit = {},
    onNaverClick: () -> Unit = {},
    onGoogleClick: () -> Unit = {},
    onWalletConnected: (String, String) -> Unit = { _, _ -> }
) {
    // 이메일 입력 상태입니다.
    var email by remember { mutableStateOf("") }

    // 비밀번호 입력 상태입니다.
    var password by remember { mutableStateOf("") }

    // 지갑 선택 다이얼로그 표시 여부입니다.
    var showWalletDialog by remember { mutableStateOf(false) }

    // 코루틴 스코프입니다.
    val scope = rememberCoroutineScope()

    // 지갑 어댑터 객체입니다.
    val walletAdapter = remember {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://spentopia.com"),
                iconUri = Uri.parse("icon.png"),
                identityName = "Spentopia"
            )
        )
    }

    // 화면 전체 배경입니다.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA)),
        contentAlignment = Alignment.Center
    ) {
        // 로그인 카드입니다.
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
                // 앱 로고입니다.
                Image(
                    painter = painterResource(id = R.drawable.ic_spentopia_logo),
                    contentDescription = "Spentopia Logo",
                    modifier = Modifier.size(76.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 앱 이름입니다.
                Text(
                    text = "Spentopia",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                // 앱 설명입니다.
                Text(
                    text = "내가 기록한 소비가 나를 만든다",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 입력 영역입니다.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 이메일 라벨입니다.
                    Text(
                        text = "이메일",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 이메일 입력창입니다.
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "test@test.com",
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

                    // 비밀번호 라벨입니다.
                    Text(
                        text = "비밀번호",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 비밀번호 입력창입니다.
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = {
                            Text(
                                text = "Test1234!",
                                color = Color(0xFF9CA3AF)
                            )
                        },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
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
    }

    if (showWalletDialog) {
        SolanaWalletDialog(
            onDismiss = {
                showWalletDialog = false
            },
            onWalletSelected = { selectedWalletName ->
                showWalletDialog = false

                Log.d("Spentopia", "선택한 지갑: $selectedWalletName")

                scope.launch {
                    try {
                        when (val result = walletAdapter.connect(walletActivityResultSender)) {
                            is TransactionResult.Success -> {
                                val publicKeyBytes = result.authResult
                                    .accounts
                                    .firstOrNull()
                                    ?.publicKey

                                val walletAddress = publicKeyBytes
                                    ?.joinToString(separator = "") { byte ->
                                        "%02x".format(byte)
                                    }
                                    .orEmpty()

                                Log.d("Spentopia", "지갑 연결 성공")
                                Log.d("Spentopia", "지갑 주소(hex): $walletAddress")
                                Log.d("Spentopia", "연결된 지갑 앱: $selectedWalletName")

                                onWalletConnected(walletAddress, selectedWalletName)
                            }

                            is TransactionResult.NoWalletFound -> {
                                Log.e("Spentopia", "설치된 Solana 지갑이 없습니다.")
                            }

                            is TransactionResult.Failure -> {
                                Log.e(
                                    "Spentopia",
                                    "지갑 연결 실패: ${result.e.message}",
                                    result.e
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("Spentopia", "지갑 연결 중 예외 발생", e)
                    }
                }
            }
        )
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
private fun SolanaWalletConnectButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
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

@Composable
private fun SolanaWalletDialog(
    onDismiss: () -> Unit,
    onWalletSelected: (String) -> Unit
) {
    val wallets = listOf("Phantom", "Solflare", "Backpack")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Solana 지갑 연결",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                wallets.forEach { wallet ->
                    WalletOptionCard(
                        walletName = wallet,
                        description = "연결하려면 클릭하세요",
                        onClick = {
                            onWalletSelected(wallet)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "닫기",
                        color = Color(0xFF2F7DF6)
                    )
                }
            }
        }
    }
}

@Composable
private fun WalletOptionCard(
    walletName: String,
    description: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color(0xFFE5E7EB),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = walletName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
        }

        Text(
            text = "연결",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2F7DF6)
        )
    }
}