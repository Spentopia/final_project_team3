package com.ict.spentopia.feature.chatbot

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.data.remote.ChatRequest
import com.ict.spentopia.data.remote.RetrofitClient
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavy
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors
import kotlinx.coroutines.launch
import retrofit2.HttpException

private data class ChatUiMessage(
    val id: Long,
    val role: ChatRole,
    val content: String
)

private enum class ChatRole {
    Assistant,
    User
}

@Composable
fun ChatbotScreen(
    onBackClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val messages = remember {
        mutableStateListOf(
            ChatUiMessage(
                id = 1L,
                role = ChatRole.Assistant,
                content = "안녕하세요. 소비 기록, 예산 관리, 절약 방법, 영수증 인증에 대해 편하게 물어보세요."
            )
        )
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage() {
        val message = input.trim()
        if (message.isBlank() || isSending) return

        messages.add(
            ChatUiMessage(
                id = System.currentTimeMillis(),
                role = ChatRole.User,
                content = message
            )
        )
        input = ""
        isSending = true

        scope.launch {
            try {
                val response = RetrofitClient.chatApi.sendMessage(
                    ChatRequest(message = message)
                )
                messages.add(
                    ChatUiMessage(
                        id = System.currentTimeMillis() + 1,
                        role = ChatRole.Assistant,
                        content = response.response.ifBlank { "답변을 생성하지 못했어요." }
                    )
                )
            } catch (e: HttpException) {
                val errorMessage = when (e.code()) {
                    400 -> "질문 내용을 다시 입력해주세요."
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    502 -> "AI 서버 응답을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                    else -> "챗봇 요청에 실패했습니다. (${e.code()})"
                }
                messages.add(
                    ChatUiMessage(
                        id = System.currentTimeMillis() + 1,
                        role = ChatRole.Assistant,
                        content = errorMessage
                    )
                )
            } catch (e: Exception) {
                messages.add(
                    ChatUiMessage(
                        id = System.currentTimeMillis() + 1,
                        role = ChatRole.Assistant,
                        content = e.message ?: "지금은 답변을 불러오지 못했어요. 잠시 후 다시 시도해주세요."
                    )
                )
            } finally {
                isSending = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) {
        ChatbotHeader(onBackClick = onBackClick)

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageBubble(message = message)
            }

            if (isSending) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = SpentopiaMutedPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "답변 생성 중...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        ChatInputBar(
            input = input,
            isSending = isSending,
            onInputChange = { input = it },
            onSendClick = { sendMessage() }
        )
    }
}

@Composable
private fun ChatbotHeader(
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = SpentopiaWalletGradientColors
                )
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "뒤로가기",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .background(Color.White.copy(alpha = 0.18f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "AI", color = Color.White, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "AI 챗바타 상담",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "소비 고민을 짧게 남기면 바로 답변해드려요.",
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatUiMessage
) {
    val isUser = message.role == ChatRole.User

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.82f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isUser) 16.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 16.dp
            ),
            color = if (isUser) SpentopiaMutedPurple else MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 0.dp
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    input: String,
    isSending: Boolean,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    text = "메시지를 입력하세요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            minLines = 1,
            maxLines = 4,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = SpentopiaMutedPurple
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSendClick,
            enabled = input.isNotBlank() && !isSending,
            modifier = Modifier.size(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SpentopiaMutedPurple,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "전송"
            )
        }
    }
}
