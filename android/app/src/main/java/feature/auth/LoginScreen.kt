package com.ict.spentopia.feature.auth

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
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
    // 이메일 상태
    var email by remember { mutableStateOf("") }

    // 비밀번호 상태
    var password by remember { mutableStateOf("") }

    // 지갑 다이얼로그 상태
    var showWalletDialog by remember { mutableStateOf(false) }

    // 코루틴 스코프
    val scope = rememberCoroutineScope()

    // 컨텍스트
    val context = LocalContext.current

    // 지갑 어댑터
    // 나중 연결 로직 붙일 때 사용
    val walletAdapter = remember {
        MobileWalletAdapter(
            connectionIdentity = ConnectionIdentity(
                identityUri = Uri.parse("https://spentopia.com"),
                iconUri = Uri.parse("icon.png"),
                identityName = "Spentopia"
            )
        )
    }

    // 사용 안 해도 구조 유지용
    walletAdapter
    walletActivityResultSender
    onWalletConnected

    // 전체 배경
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F6FA)),
        contentAlignment = Alignment.Center
    ) {
        // 로그인 카드
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
                // 로고
                Image(
                    painter = painterResource(id = R.drawable.ic_spentopia_logo),
                    contentDescription = "Spentopia Logo",
                    modifier = Modifier.size(76.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 앱 이름
                Text(
                    text = "Spentopia",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                // 설명
                Text(
                    text = "내가 기록한 소비가 나를 만든다",
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 입력 영역
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    // 이메일 라벨
                    Text(
                        text = "이메일",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 이메일 입력
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

                    // 비밀번호 라벨
                    Text(
                        text = "비밀번호",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF222222)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 비밀번호 입력
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

                // 로그인 버튼
                GradientLoginButton(
                    text = "로그인",
                    onClick = onLoginClick
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 지갑 연결 버튼
                SolanaWalletConnectButton(
                    onClick = {
                        showWalletDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 구분선
                OrDivider()

                Spacer(modifier = Modifier.height(24.dp))

                // 카카오 로그인
                SocialLoginButton(
                    text = "카카오로 로그인",
                    iconRes = R.drawable.ic_kakao,
                    onClick = onKakaoClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 구글 로그인
                SocialLoginButton(
                    text = "구글로 로그인",
                    iconRes = R.drawable.ic_google,
                    onClick = onGoogleClick
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 네이버 로그인
                SocialLoginButton(
                    text = "네이버로 로그인",
                    iconRes = R.drawable.ic_naver,
                    onClick = onNaverClick
                )
            }
        }
    }

    // 지갑 선택 다이얼로그
    if (showWalletDialog) {
        SolanaWalletDialog(
            onDismiss = {
                showWalletDialog = false
            },
            onWalletSelected = { selectedWalletName ->
                // 다이얼로그 닫기
                showWalletDialog = false

                // 선택 로그
                Log.d("Spentopia", "선택한 지갑: $selectedWalletName")

                scope.launch {
                    try {
                        // 지갑 앱 인텐트 생성
                        val intent = when (selectedWalletName) {
                            // 팬텀 실행
                            "Phantom" -> context.packageManager
                                .getLaunchIntentForPackage("app.phantom")

                            // 솔플레어 실행
                            "Solflare" -> context.packageManager
                                .getLaunchIntentForPackage("com.solflare.mobile")

                            // 백팩 실행
                            // 백팩 실행
                            "Backpack" -> {
                                // 백팩 딥링크
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://backpack.app/ul/v1/connect")
                                )
                            }

                            // 예외 처리
                            else -> null
                        }

                        // 앱 없으면 종료
                        if (intent == null) {
                            Toast.makeText(
                                context,
                                "$selectedWalletName 설치 안됨",
                                Toast.LENGTH_SHORT
                            ).show()

                            Log.e("Spentopia", "$selectedWalletName 인텐트 없음")
                            return@launch
                        }

                        // 앱 실행 플래그
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                        // 앱 실행
                        context.startActivity(intent)

                        // 실행 성공 토스트
                        Toast.makeText(
                            context,
                            "$selectedWalletName 실행 성공",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 실행 성공 로그
                        Log.d("Spentopia", "$selectedWalletName 실행 성공")

                    } catch (e: ActivityNotFoundException) {
                        // 실행 실패 토스트
                        Toast.makeText(
                            context,
                            "$selectedWalletName 실행 실패",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 실행 실패 로그
                        Log.e("Spentopia", "$selectedWalletName 실행 실패", e)
                    } catch (e: Exception) {
                        // 예외 토스트
                        Toast.makeText(
                            context,
                            "$selectedWalletName 실행 중 오류",
                            Toast.LENGTH_SHORT
                        ).show()

                        // 예외 로그
                        Log.e("Spentopia", "$selectedWalletName 실행 중 예외", e)
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
    // 로그인 버튼
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        // 버튼 배경
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
            // 버튼 글자
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
    // 지갑 버튼
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues(0.dp)
    ) {
        // 버튼 배경
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
            // 버튼 내용
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // 버튼 글자
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
    // 구분선 행
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 왼쪽 선
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFFD6D6D6)
        )

        // 가운데 글자
        Text(
            text = "  또는  ",
            color = Color(0xFF8A8A8A),
            fontSize = 14.sp
        )

        // 오른쪽 선
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
    // 지갑 목록
    val wallets = listOf("Phantom", "Solflare", "Backpack")

    Dialog(onDismissRequest = onDismiss) {
        // 다이얼로그 표면
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // 제목
                Text(
                    text = "Solana 지갑 연결",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 지갑 목록 출력
                wallets.forEach { wallet ->
                    WalletOptionCard(
                        walletName = wallet,
                        description = "연결하려면 클릭",
                        onClick = {
                            onWalletSelected(wallet)
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // 닫기 버튼
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
    // 지갑 카드
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
        // 텍스트 영역
        Column(modifier = Modifier.weight(1f)) {
            // 지갑 이름
            Text(
                text = walletName,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )

            // 설명
            Text(
                text = description,
                fontSize = 13.sp,
                color = Color(0xFF6B7280)
            )
        }

        // 연결 글자
        Text(
            text = "연결",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2F7DF6)
        )
    }
}