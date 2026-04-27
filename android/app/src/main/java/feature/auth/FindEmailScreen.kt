package com.ict.spentopia.feature.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.R
import kotlinx.coroutines.launch

@Composable
fun FindEmailScreen(
    onBackToLoginClick: () -> Unit
) {
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun startFindEmail() {
        val trimmedPhoneNumber = phoneNumber.trim()

        if (trimmedPhoneNumber.isBlank()) {
            Toast.makeText(context, "가입 시 입력한 전화번호를 입력해 주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isLoading = true

            try {
                /*
                 * 이메일 찾기 DB API 연결 위치
                 *
                 * 예시:
                 * loginViewModel.findEmail(
                 *     phoneNumber = trimmedPhoneNumber,
                 *     onSuccess = { email ->
                 *         Toast.makeText(context, "가입된 이메일: $email", Toast.LENGTH_LONG).show()
                 *     },
                 *     onError = { message ->
                 *         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                 *     }
                 * )
                 */
                Toast.makeText(context, "이메일 찾기 API 연결이 필요합니다.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: "이메일 찾기 중 오류가 발생했습니다.",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3F8FC))
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_spentopia_logo),
                contentDescription = "Spentopia Logo",
                modifier = Modifier.size(74.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "이메일 찾기",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "가입 시 사용한 전화번호를 입력해 주세요.",
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = "전화번호",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = {
                    Text(
                        text = "010-1234-5678",
                        color = Color(0xFF8B95A1),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = Color(0xFFE0E5EC),
                    unfocusedBorderColor = Color(0xFFE0E5EC),
                    cursorColor = Color(0xFF2F7DF6)
                )
            )

            Spacer(modifier = Modifier.height(26.dp))

            AuthGradientButton(
                text = if (isLoading) "확인 중..." else "이메일 찾기",
                enabled = !isLoading,
                onClick = {
                    startFindEmail()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onBackToLoginClick) {
                Text(
                    text = "로그인으로 돌아가기",
                    fontSize = 14.sp,
                    color = Color(0xFF8A94A6)
                )
            }
        }
    }
}

@Composable
fun AuthGradientButton(
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
                        colors = listOf(
                            Color(0xFF17BEDA),
                            Color(0xFF2F7DF6)
                        )
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