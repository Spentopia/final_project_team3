package com.ict.spentopia.feature.auth // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.PaddingValues // PaddingValues 기능을 가져옴
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
import androidx.compose.material3.Button // 버튼 컴포넌트를 가져옴
import androidx.compose.material3.ButtonDefaults // ButtonDefaults 기능을 가져옴
import androidx.compose.material3.OutlinedTextField // OutlinedTextField 기능을 가져옴
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextFieldDefaults 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.material3.TextButton // 글자 버튼 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
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
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.res.stringResource // stringResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.text.input.KeyboardType // KeyboardType 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import com.ict.spentopia.R // R 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaGlowPurple // SpentopiaGlowPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavy // SpentopiaNavy 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple // SpentopiaNavyPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaWalletGradientColors // SpentopiaWalletGradientColors 기능을 가져옴
import com.ict.spentopia.ui.toast.AppToastType
import com.ict.spentopia.ui.toast.showAppToast
import kotlinx.coroutines.launch // 코루틴 실행 도구를 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun FindEmailScreen( // FindEmailScreen 함수를 선언함
    onBackToLoginClick: () -> Unit // onBackToLoginClick 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    var phoneNumber by remember { mutableStateOf("") } // 화면에서 바뀔 phoneNumber 값을 저장함
    var isLoading by remember { mutableStateOf(false) } // 화면에서 바뀔 로딩 여부를 저장함

    val context = LocalContext.current // 현재 화면 정보를 저장함
    val scope = rememberCoroutineScope() // 화면이 다시 그려져도 코루틴 실행 범위을 기억함

    fun startFindEmail() { // startFindEmail 함수를 선언함
        val trimmedPhoneNumber = phoneNumber.trim() // trimmedPhoneNumber 값을 저장함

        if (trimmedPhoneNumber.isBlank()) { // 조건이 맞는지 확인함
            showAppToast(context, context.getString(R.string.phone_number_required)) // 화면에 글자를 보여줌
            return
        }

        scope.launch { // 이 블록 안의 내용이 시작됨
            isLoading = true // true 값을 로딩 여부에 넣음

            try { // 오류가 날 수 있는 코드를 먼저 시도함
                /*
                 * 이메일 찾기 DB API 연결 위치
                 *
                 * 예시:
                 * loginViewModel.findEmail(
                 *     phoneNumber = trimmedPhoneNumber,
                 *     onSuccess = { email -> ... },
                 *     onError = { message ->
                 *         showAppToast(context, message)
                 *     }
                 * )
                 */
                showAppToast(context, context.getString(R.string.find_email_api_needed)) // 화면에 글자를 보여줌
            } catch (e: Exception) { // 이 블록 안의 내용이 시작됨
                showAppToast(
                    context,
                    e.message ?: context.getString(R.string.find_email_error),
                    AppToastType.ERROR
                )
            } finally { // 이 블록 안의 내용이 시작됨
                isLoading = false // false 값을 로딩 여부에 넣음
            }
        }
    }

    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(Color(0xFFF3F8FC))
            .imePadding(),
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Column( // 안쪽 UI를 세로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 30.dp), // .padding(horizontal 값을 정해줌
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Image( // 화면에 이미지를 보여줌
                painter = painterResource(id = R.drawable.ic_spentopia_logo), // painter 값을 정해줌
                contentDescription = stringResource(id = R.string.spentopia_logo_content_description), // contentDescription 값을 정해줌
                modifier = Modifier.size(96.dp), // UI 크기나 여백 같은 모양을 정함
                contentScale = ContentScale.Fit // contentScale 값을 정해줌
            )

            Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = stringResource(id = R.string.find_email_title), // text 값을 정해줌
                fontSize = 28.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = Color(0xFF111827) // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = stringResource(id = R.string.find_email_description), // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                color = Color(0xFF6B7280) // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(36.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                text = stringResource(id = R.string.phone_number_label), // text 값을 정해줌
                fontSize = 14.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                color = Color(0xFF1F2937) // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(8.dp)) // UI 크기나 여백 같은 모양을 정함

            OutlinedTextField( // 사용자가 입력할 칸을 만듦
                value = phoneNumber, // phoneNumber 값을 입력값에 넣음
                onValueChange = { phoneNumber = it }, // onValueChange 때 실행할 함수를 정해줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .height(54.dp),
                placeholder = { // placeholder 값을 정해줌
                    Text( // 화면에 글자를 보여줌
                        text = "010-1234-5678", // text 값을 정해줌
                        color = Color(0xFF8B95A1), // color 값을 정해줌
                        fontSize = 14.sp // fontSize 값을 정해줌
                    )
                },
                singleLine = true, // true 값을 singleLine 값에 넣음
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), // keyboardOptions 값을 정해줌
                shape = RoundedCornerShape(14.dp), // shape 값을 정해줌
                colors = OutlinedTextFieldDefaults.colors( // 사용자가 입력할 칸을 만듦
                    focusedContainerColor = Color.White, // focusedContainerColor 값을 정해줌
                    unfocusedContainerColor = Color.White, // unfocusedContainerColor 값을 정해줌
                    focusedBorderColor = SpentopiaMutedPurple.copy(alpha = 0.45f), // focusedBorderColor 값을 정해줌
                    unfocusedBorderColor = Color(0xFFE0E5EC), // unfocusedBorderColor 값을 정해줌
                    cursorColor = SpentopiaMutedPurple // SpentopiaMutedPurple 값을 cursorColor 값에 넣음
                )
            )

            Spacer(modifier = Modifier.height(26.dp)) // UI 크기나 여백 같은 모양을 정함

            AuthGradientButton( // 누를 수 있는 버튼을 만듦
                text = if (isLoading) { // text 값을 정해줌
                    stringResource(id = R.string.find_email_loading) // stringResource(id 값을 정해줌
                } else { // 이 블록 안의 내용이 시작됨
                    stringResource(id = R.string.find_email_title) // stringResource(id 값을 정해줌
                },
                enabled = !isLoading, // enabled 값을 정해줌
                onClick = { // 눌렀을 때 실행할 함수를 정해줌
                    startFindEmail() // start Find Email 함수를 실행함
                }
            )

            Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

            TextButton(onClick = onBackToLoginClick) { // 누를 수 있는 버튼을 만듦
                Text( // 화면에 글자를 보여줌
                    text = stringResource(id = R.string.back_to_login), // text 값을 정해줌
                    fontSize = 14.sp, // fontSize 값을 정해줌
                    color = Color(0xFF8A94A6) // color 값을 정해줌
                )
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun AuthGradientButton( // AuthGradientButton 함수를 선언함
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
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent), // colors 값을 정해줌
        contentPadding = PaddingValues(0.dp) // contentPadding 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient( // brush 값을 정해줌
                        colors = SpentopiaWalletGradientColors // 지갑 관련 값을 colors 값에 넣음
                    ),
                    shape = RoundedCornerShape(16.dp) // shape 값을 정해줌
                )
                .border(
                    width = 1.dp, // width 값을 정해줌
                    color = SpentopiaGlowPurple.copy(alpha = if (pressed) 0.65f else 0.45f), // color 값을 정해줌
                    shape = RoundedCornerShape(16.dp) // shape 값을 정해줌
                ),
            contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            StaticButtonShine(shape = RoundedCornerShape(16.dp), pressed = pressed) // StaticButtonShine(shape 값을 정해줌

            Text( // 화면에 글자를 보여줌
                text = text, // text 값을 text 값에 넣음
                color = Color.White, // color 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold // fontWeight 값을 정해줌
            )
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun StaticButtonShine( // StaticButtonShine 함수를 선언함
    shape: RoundedCornerShape, // shape 값을 받음
    pressed: Boolean // pressed 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient( // brush 값을 정해줌
                    colors = listOf( // colors 값을 정해줌
                        Color.Transparent,
                        Color.White.copy(alpha = if (pressed) 0.24f else 0.14f), // Color.White.copy(alpha 값을 정해줌
                        Color.Transparent
                    )
                ),
                shape = shape // shape 값을 shape 값에 넣음
            )
    )
}
