package com.ict.spentopia.feature.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.R
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import kotlinx.coroutines.launch

@Composable
fun FindPasswordScreen(
    onBackToLoginClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun startFindPassword() {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank()) {
            Toast.makeText(context, context.getString(R.string.find_password_email_required), Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            isLoading = true

            try {
                /*
                 * 비밀번호 찾기 DB API 연결 위치
                 *
                 * 예시:
                 * loginViewModel.findPassword(
                 *     email = trimmedEmail,
                 *     onSuccess = { ... },
                 *     onError = { message ->
                 *         Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                 *     }
                 * )
                 */
                Toast.makeText(context, context.getString(R.string.find_password_api_needed), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    e.message ?: context.getString(R.string.find_password_error),
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
                contentDescription = stringResource(id = R.string.spentopia_logo_content_description),
                modifier = Modifier.size(96.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = stringResource(id = R.string.find_password_title),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.find_password_description),
                fontSize = 14.sp,
                color = Color(0xFF6B7280)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(id = R.string.login_email_label),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF1F2937)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.login_email_placeholder),
                        color = Color(0xFF8B95A1),
                        fontSize = 14.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.45f),
                    unfocusedBorderColor = Color(0xFFE0E5EC),
                    cursorColor = SpentopiaMutedPurple
                )
            )

            Spacer(modifier = Modifier.height(26.dp))

            AuthGradientButton(
                text = if (isLoading) {
                    stringResource(id = R.string.find_password_loading)
                } else {
                    stringResource(id = R.string.send_reset_link)
                },
                enabled = !isLoading,
                onClick = {
                    startFindPassword()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(onClick = onBackToLoginClick) {
                Text(
                    text = stringResource(id = R.string.back_to_login),
                    fontSize = 14.sp,
                    color = Color(0xFF8A94A6)
                )
            }
        }
    }
}
