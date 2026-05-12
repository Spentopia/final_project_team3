package com.ict.spentopia.feature.chatbot // 이 파일이 속한 패키지 위치를 적음

// AI 챗봇 화면임
// 질문/답변 주고받는 대화형 UI

import androidx.compose.foundation.background // background 기능을 가져옴
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
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.lazy.LazyColumn // 세로 스크롤 목록을 가져옴
import androidx.compose.foundation.lazy.items // items 기능을 가져옴
import androidx.compose.foundation.lazy.rememberLazyListState // rememberLazyListState 기능을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material.icons.Icons // Icons 기능을 가져옴
import androidx.compose.material.icons.filled.ArrowBack // ArrowBack 기능을 가져옴
import androidx.compose.material.icons.filled.Send // Send 기능을 가져옴
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.CircularProgressIndicator // CircularProgressIndicator 기능을 가져옴
import androidx.compose.material3.Icon // 아이콘 표시 컴포넌트를 가져옴
import androidx.compose.material3.IconButton // 아이콘 버튼 컴포넌트를 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.Surface // Surface 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.runtime.mutableStateListOf // mutableStateListOf 기능을 가져옴
import androidx.compose.runtime.mutableStateOf // 화면 상태를 만드는 도구를 가져옴
import androidx.compose.runtime.remember // 값을 기억하는 Compose 도구를 가져옴
import androidx.compose.runtime.rememberCoroutineScope // rememberCoroutineScope 기능을 가져옴
import androidx.compose.runtime.setValue // by로 상태를 바꾸게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import com.ict.spentopia.data.remote.ChatRequest // ChatRequest 기능을 가져옴
import com.ict.spentopia.data.remote.RetrofitClient // RetrofitClient 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴
import retrofit2.HttpException // 서버 오류 타입을 가져옴

private data class ChatUiMessage( // ChatUiMessage 데이터를 묶어둘 클래스 시작
    val id: Long, // 아이디를 저장함
    val role: ChatRole, // role 값을 저장함
    val content: String // 내용을 저장함
)

private enum class ChatRole { // ChatRole에서 고를 수 있는 값들을 묶음
    Assistant,
    User
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun ChatbotScreen( // ChatbotScreen 함수를 선언함
    onBackClick: () -> Unit = {} // onBackClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    val scope = rememberCoroutineScope() // 화면이 다시 그려져도 코루틴 실행 범위을 기억함
    val listState = rememberLazyListState() // 화면이 다시 그려져도 listState 값을 기억함
    var input by remember { mutableStateOf("") } // 화면에서 바뀔 input 값을 저장함
    var isSending by remember { mutableStateOf(false) } // 화면에서 바뀔 메시지 보내는 중인지 저장함
    val messages = remember { // 화면이 다시 그려져도 messages 값을 기억함
        mutableStateListOf( // mutable State List Of 함수를 실행함
            ChatUiMessage( // Chat Ui Message 함수를 실행함
                id = 1L, // 아이디를 정해줌
                role = ChatRole.Assistant, // role 값을 정해줌
                content = "안녕하세요. 소비 기록, 예산 관리, 절약 방법, 영수증 인증에 대해 편하게 물어보세요." // 내용을 정해줌
            )
        )
    }

    LaunchedEffect(messages.size) { // 화면이 열리거나 값이 바뀔 때 실행함
        if (messages.isNotEmpty()) { // 조건이 맞는지 확인함
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    fun sendMessage() { // sendMessage 함수를 선언함
        val message = input.trim() // 메시지를 저장함
        if (message.isBlank() || isSending) return // 조건이 맞는지 확인함

        messages.add(
            ChatUiMessage( // Chat Ui Message 함수를 실행함
                id = System.currentTimeMillis(), // 아이디를 정해줌
                role = ChatRole.User, // role 값을 정해줌
                content = message // 메시지를 내용에 넣음
            )
        )
        input = "" // input 값을 정해줌
        isSending = true // true 값을 isSending인지 여부에 넣음

        scope.launch { // 이 블록 안의 내용이 시작됨
            try { // 오류가 날 수 있는 코드를 먼저 시도함
                val response = RetrofitClient.chatApi.sendMessage( // 서버 응답을 저장함
                    ChatRequest(message = message) // 채팅 관련 값을 정해줌
                )
                messages.add(
                    ChatUiMessage( // Chat Ui Message 함수를 실행함
                        id = System.currentTimeMillis() + 1, // 아이디를 정해줌
                        role = ChatRole.Assistant, // role 값을 정해줌
                        content = response.response.ifBlank { "답변을 생성하지 못했어요." } // 내용을 정해줌
                    )
                )
            } catch (e: HttpException) { // 이 블록 안의 내용이 시작됨
                val errorMessage = when (e.code()) { // 오류 내용을 저장함
                    400 -> "질문 내용을 다시 입력해주세요."
                    401 -> "로그인이 만료되었습니다. 다시 로그인해주세요."
                    502 -> "AI 서버 응답을 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                    else -> "챗봇 요청에 실패했습니다. (${e.code()})" // 위 조건이 아니면 이쪽을 실행함
                }
                messages.add(
                    ChatUiMessage( // Chat Ui Message 함수를 실행함
                        id = System.currentTimeMillis() + 1, // 아이디를 정해줌
                        role = ChatRole.Assistant, // role 값을 정해줌
                        content = errorMessage // 오류 내용을 내용에 넣음
                    )
                )
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                messages.add(
                    ChatUiMessage( // Chat Ui Message 함수를 실행함
                        id = System.currentTimeMillis() + 1, // 아이디를 정해줌
                        role = ChatRole.Assistant, // role 값을 정해줌
                        content = e.message ?: "지금은 답변을 불러오지 못했어요. 잠시 후 다시 시도해주세요." // 내용을 정해줌
                    )
                )
            } finally { // 이 블록 안의 내용이 시작됨
                isSending = false // false 값을 isSending인지 여부에 넣음
            }
        }
    }

    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
    ) { // 이 블록 안의 내용이 시작됨
        ChatbotHeader(onBackClick = onBackClick) // 채팅 관련 값을 정해줌

        LazyColumn( // 안쪽 UI를 세로로 배치함
            state = listState, // listState 값을 상태값에 넣음
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp), // contentPadding 값을 정해줌
            verticalArrangement = Arrangement.spacedBy(12.dp) // verticalArrangement 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            items(messages, key = { it.id }) { message -> // key 값을 정해줌
                ChatMessageBubble(message = message) // 채팅 관련 값을 정해줌
            }

            if (isSending) { // 조건이 맞는지 확인함
                item { // 이 블록 안의 내용이 시작됨
                    Row( // 안쪽 UI를 가로로 배치함
                        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
                    ) { // 이 블록 안의 내용이 시작됨
                        CircularProgressIndicator( // Circular Progress Indicator 함수를 실행함
                            modifier = Modifier.size(18.dp), // UI 크기나 여백 같은 모양을 정함
                            strokeWidth = 2.dp, // strokeWidth 값을 정해줌
                            color = SpentopiaMutedPurple // SpentopiaMutedPurple 값을 color 값에 넣음
                        )
                        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함
                        Text( // 화면에 글자를 보여줌
                            text = "답변 생성 중...", // text 값을 정해줌
                            color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                            fontSize = 13.sp // fontSize 값을 정해줌
                        )
                    }
                }
            }
        }

        ChatInputBar( // Chat Input Bar 함수를 실행함
            input = input, // input 값을 input 값에 넣음
            isSending = isSending, // isSending인지 여부를 isSending인지 여부에 넣음
            onInputChange = { input = it }, // onInputChange 때 실행할 함수를 정해줌
            onSendClick = { sendMessage() } // onSendClick 때 실행할 함수를 정해줌
        )
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ChatbotHeader( // ChatbotHeader 함수를 선언함
    onBackClick: () -> Unit // onBackClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface // color 값을 정해줌
            )
            .padding(horizontal = 12.dp, vertical = 14.dp), // .padding(horizontal 값을 정해줌
        verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        IconButton(onClick = onBackClick) { // 누를 수 있는 버튼을 만듦
            Icon( // 화면에 아이콘을 보여줌
                imageVector = Icons.Default.ArrowBack, // imageVector 값을 정해줌
                contentDescription = "뒤로가기", // contentDescription 값을 정해줌
                tint = MaterialTheme.colorScheme.onSurface // tint 값을 정해줌
            )
        }

        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = "AI", // text 값을 정해줌
                color = MaterialTheme.colorScheme.onPrimaryContainer, // color 값을 정해줌
                fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
            )
        }

        Spacer(modifier = Modifier.width(12.dp)) // UI 크기나 여백 같은 모양을 정함

        Column { // 안쪽 UI를 세로로 배치함
            Text( // 화면에 글자를 보여줌
                text = "AI 챗바타 상담", // text 값을 정해줌
                color = MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                fontSize = 18.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
            )
            Spacer(modifier = Modifier.height(2.dp)) // UI 크기나 여백 같은 모양을 정함
            Text( // 화면에 글자를 보여줌
                text = "소비 고민을 짧게 남기면 바로 답변해드려요.", // text 값을 정해줌
                color = MaterialTheme.colorScheme.onSurfaceVariant, // color 값을 정해줌
                fontSize = 12.sp // fontSize 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ChatMessageBubble( // ChatMessageBubble 함수를 선언함
    message: ChatUiMessage // 메시지를 받음
) { // 이 블록 안의 내용이 시작됨
    val isUser = message.role == ChatRole.User // 사용자가 보낸 메시지인지 저장함

    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start // horizontalArrangement 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Surface( // Surface 함수를 실행함
            modifier = Modifier.fillMaxWidth(0.82f), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape( // shape 값을 정해줌
                topStart = 16.dp, // topStart 값을 정해줌
                topEnd = 16.dp, // topEnd 값을 정해줌
                bottomStart = if (isUser) 16.dp else 4.dp, // bottomStart 값을 정해줌
                bottomEnd = if (isUser) 4.dp else 16.dp // bottomEnd 값을 정해줌
            ),
            color = if (isUser) SpentopiaMutedPurple else MaterialTheme.colorScheme.surface, // color 값을 정해줌
            tonalElevation = 1.dp, // tonalElevation 값을 정해줌
            shadowElevation = 0.dp // shadowElevation 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Text( // 화면에 글자를 보여줌
                text = message.content, // text 값을 정해줌
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp), // UI 크기나 여백 같은 모양을 정함
                color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface, // color 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                lineHeight = 20.sp // lineHeight 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun ChatInputBar( // ChatInputBar 함수를 선언함
    input: String, // input 값을 받음
    isSending: Boolean, // isSending인지 여부를 받음
    onInputChange: (String) -> Unit, // onInputChange 때 실행할 함수를 받음
    onSendClick: () -> Unit // onSendClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Row( // 안쪽 UI를 가로로 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.Bottom // verticalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        OutlinedTextField( // 사용자가 입력할 칸을 만듦
            value = input, // input 값을 입력값에 넣음
            onValueChange = onInputChange, // onInputChange 때 실행할 함수를 onValueChange 때 실행할 함수에 넣음
            modifier = Modifier.weight(1f), // UI 크기나 여백 같은 모양을 정함
            placeholder = { // placeholder 값을 정해줌
                Text( // 화면에 글자를 보여줌
                    text = "메시지를 입력하세요", // text 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            },
            minLines = 1, // minLines 값을 정해줌
            maxLines = 4, // maxLines 값을 정해줌
            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
            colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                focusedContainerColor = MaterialTheme.colorScheme.background, // focusedContainerColor 값을 정해줌
                unfocusedContainerColor = MaterialTheme.colorScheme.background, // unfocusedContainerColor 값을 정해줌
                focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.5f), // focusedBorderColor 값을 정해줌
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant, // unfocusedBorderColor 값을 정해줌
                focusedTextColor = MaterialTheme.colorScheme.onBackground, // focusedTextColor 값을 정해줌
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground, // unfocusedTextColor 값을 정해줌
                cursorColor = SpentopiaMutedPurple // SpentopiaMutedPurple 값을 cursorColor 값에 넣음
            )
        )

        Spacer(modifier = Modifier.width(8.dp)) // UI 크기나 여백 같은 모양을 정함

        Button( // 누를 수 있는 버튼을 만듦
            onClick = onSendClick, // onSendClick 때 실행할 함수를 눌렀을 때 실행할 함수에 넣음
            enabled = input.isNotBlank() && !isSending, // enabled 값을 정해줌
            modifier = Modifier.size(54.dp), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
            colors = ButtonDefaults.buttonColors( // colors 값을 정해줌
                containerColor = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 containerColor 값에 넣음
                contentColor = Color.White, // contentColor 값을 정해줌
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant, // disabledContainerColor 값을 정해줌
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant // disabledContentColor 값을 정해줌
            ),
            contentPadding = PaddingValues(0.dp) // contentPadding 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Icon( // 화면에 아이콘을 보여줌
                imageVector = Icons.Default.Send, // imageVector 값을 정해줌
                contentDescription = "전송" // contentDescription 값을 정해줌
            )
        }
    }
}
