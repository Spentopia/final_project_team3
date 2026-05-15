package com.ict.spentopia.feature.splash // 이 파일이 속한 패키지 위치를 적음

// 스플래시 화면임
// 앱 시작 전 썸네일 역할

import androidx.compose.animation.core.RepeatMode // RepeatMode 기능을 가져옴
import androidx.compose.animation.core.animateFloat // animateFloat 기능을 가져옴
import androidx.compose.animation.core.infiniteRepeatable // infiniteRepeatable 기능을 가져옴
import androidx.compose.animation.core.rememberInfiniteTransition // rememberInfiniteTransition 기능을 가져옴
import androidx.compose.animation.core.tween // tween 기능을 가져옴
import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.background // background 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.offset // offset 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.shape.CircleShape // CircleShape 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.runtime.LaunchedEffect // 화면이 열릴 때 실행하는 도구를 가져옴
import androidx.compose.runtime.getValue // by로 상태를 읽게 해줌
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.Brush // Brush 기능을 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.graphics.graphicsLayer // graphicsLayer 기능을 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.navigation.NavController // NavController 기능을 가져옴
import com.ict.spentopia.R // R 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple // SpentopiaNavyPurple 기능을 가져옴
import kotlinx.coroutines.delay // delay 기능을 가져옴
@Composable // 이 함수가 화면 UI를 그린다는 표시
fun DarkSplashContent() { // SplashScreen 함수를 선언함
    val infiniteTransition = rememberInfiniteTransition(label = "splash") // 화면이 다시 그려져도 infiniteTransition 값을 기억함

    val alpha by infiniteTransition.animateFloat( // alpha 값을 저장함
        initialValue = 0.65f, // initialValue 값을 정해줌
        targetValue = 1f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(700), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "alpha" // label 값을 정해줌
    )

    val scale by infiniteTransition.animateFloat( // scale 값을 저장함
        initialValue = 0.96f, // initialValue 값을 정해줌
        targetValue = 1.04f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(700), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "scale" // label 값을 정해줌
    )

    val sparkleAlpha1 by infiniteTransition.animateFloat( // sparkleAlpha1 값을 저장함
        initialValue = 0.25f, // initialValue 값을 정해줌
        targetValue = 1f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(920), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha1" // label 값을 정해줌
    )

    val sparkleAlpha2 by infiniteTransition.animateFloat( // sparkleAlpha2 값을 저장함
        initialValue = 1f, // initialValue 값을 정해줌
        targetValue = 0.25f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1180), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha2" // label 값을 정해줌
    )

    val sparkleAlpha3 by infiniteTransition.animateFloat( // sparkleAlpha3 값을 저장함
        initialValue = 0.25f, // initialValue 값을 정해줌
        targetValue = 1f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1360), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha3" // label 값을 정해줌
    )

    val sparkleAlpha4 by infiniteTransition.animateFloat( // sparkleAlpha4 값을 저장함
        initialValue = 0.45f, // initialValue 값을 정해줌
        targetValue = 1f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1040), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha4" // label 값을 정해줌
    )

    val sparkleAlpha5 by infiniteTransition.animateFloat( // sparkleAlpha5 값을 저장함
        initialValue = 1f, // initialValue 값을 정해줌
        targetValue = 0.25f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1260), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha5" // label 값을 정해줌
    )

    val sparkleAlpha6 by infiniteTransition.animateFloat( // sparkleAlpha6 값을 저장함
        initialValue = 0.30f, // initialValue 값을 정해줌
        targetValue = 0.95f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1500), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha6" // label 값을 정해줌
    )

    val sparkleAlpha7 by infiniteTransition.animateFloat( // sparkleAlpha7 값을 저장함
        initialValue = 0.18f, // initialValue 값을 정해줌
        targetValue = 0.82f, // targetValue 값을 정해줌
        animationSpec = infiniteRepeatable( // animationSpec 값을 정해줌
            animation = tween(1320), // animation 값을 정해줌
            repeatMode = RepeatMode.Reverse // repeatMode 값을 정해줌
        ),
        label = "sparkleAlpha7" // label 값을 정해줌
    )



    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = Modifier // UI 크기나 여백 같은 모양을 정함
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf( // list Of 함수를 실행함
                        Color(0xFF090B16), // Color 함수를 실행함
                        Color(0xFF111827), // Color 함수를 실행함
                        Color(0xFF24103F) // Color 함수를 실행함
                    )
                )
            ),
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient( // brush 값을 정해줌
                        colors = listOf( // colors 값을 정해줌
                            Color(0xAA7C3AED), // Color 함수를 실행함
                            Color(0x441E1B4B), // Color 함수를 실행함
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape // CircleShape 값을 shape 값에 넣음
                )
        )

        Text( // 화면에 글자를 보여줌
            text = "✦", // text 값을 정해줌
            fontSize = 20.sp, // fontSize 값을 정해줌
            color = Color.White, // color 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = (-118).dp, y = (-112).dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha1 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✧", // text 값을 정해줌
            fontSize = 18.sp, // fontSize 값을 정해줌
            color = SpentopiaNavyPurple, // SpentopiaNavyPurple 값을 color 값에 넣음
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = 124.dp, y = (-96).dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha2 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✦", // text 값을 정해줌
            fontSize = 14.sp, // fontSize 값을 정해줌
            color = Color(0xFF6366A8), // color 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = (-138).dp, y = 24.dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha3 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✧", // text 값을 정해줌
            fontSize = 16.sp, // fontSize 값을 정해줌
            color = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 color 값에 넣음
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = 132.dp, y = 38.dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha4 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✦", // text 값을 정해줌
            fontSize = 12.sp, // fontSize 값을 정해줌
            color = Color.White, // color 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = 72.dp, y = (-142).dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha5 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✧", // text 값을 정해줌
            fontSize = 22.sp, // fontSize 값을 정해줌
            color = Color.White, // color 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = (-92).dp, y = (-148).dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha6 } // this.alpha 값을 정해줌
        )

        Text( // 화면에 글자를 보여줌
            text = "✦", // text 값을 정해줌
            fontSize = 17.sp, // fontSize 값을 정해줌
            color = Color(0xFFD8B4FE), // color 값을 정해줌
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .align(Alignment.Center)
                .offset(x = 110.dp, y = 118.dp) // .offset(x 값을 정해줌
                .graphicsLayer { this.alpha = sparkleAlpha7 } // this.alpha 값을 정해줌
        )

        Column( // 안쪽 UI를 세로로 배치함
            horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Image( // 화면에 이미지를 보여줌
                painter = painterResource(id = R.drawable.ic_spentopia_logo), // painter 값을 정해줌
                contentDescription = "Spentopia Logo", // contentDescription 값을 정해줌
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .size(300.dp)
                    .graphicsLayer { // 이 블록 안의 내용이 시작됨
                        this.alpha = alpha // alpha 값을 alpha 값에 넣음
                        scaleX = scale // scale 값을 scaleX 값에 넣음
                        scaleY = scale // scale 값을 scaleY 값에 넣음
                    }
            )

            Spacer(modifier = Modifier.height(22.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "Spentopia", // text 값을 정해줌
                fontSize = 34.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                color = Color.White // color 값을 정해줌
            )

            Spacer(modifier = Modifier.height(10.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = "지출을 관리하면 열리는 나만의 세계", // text 값을 정해줌
                fontSize = 16.sp, // fontSize 값을 정해줌
                color = Color(0xFFD1D5DB) // color 값을 정해줌
            )
        }
    }
}
