package com.ict.spentopia.feature.auth

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ict.spentopia.R
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple
import com.ict.spentopia.ui.theme.SpentopiaNavyPurple
import kotlinx.coroutines.delay
@Composable
fun SplashScreen(navController: NavController) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val sparkleAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(920),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha1"
    )

    val sparkleAlpha2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1180),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha2"
    )

    val sparkleAlpha3 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1360),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha3"
    )

    val sparkleAlpha4 by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1040),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha4"
    )

    val sparkleAlpha5 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1260),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkleAlpha5"
    )

    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090B16),
                        Color(0xFF111827),
                        Color(0xFF24103F)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0x886D28D9),
                            Color(0x331E1B4B),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                )
        )

        Text(
            text = "✦",
            fontSize = 20.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-118).dp, y = (-112).dp)
                .graphicsLayer { this.alpha = sparkleAlpha1 }
        )

        Text(
            text = "✧",
            fontSize = 18.sp,
            color = SpentopiaNavyPurple,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 124.dp, y = (-96).dp)
                .graphicsLayer { this.alpha = sparkleAlpha2 }
        )

        Text(
            text = "✦",
            fontSize = 14.sp,
            color = Color(0xFF6366A8),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-138).dp, y = 24.dp)
                .graphicsLayer { this.alpha = sparkleAlpha3 }
        )

        Text(
            text = "✧",
            fontSize = 16.sp,
            color = SpentopiaMutedPurple,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 132.dp, y = 38.dp)
                .graphicsLayer { this.alpha = sparkleAlpha4 }
        )

        Text(
            text = "✦",
            fontSize = 12.sp,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 72.dp, y = (-142).dp)
                .graphicsLayer { this.alpha = sparkleAlpha5 }
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_spentopia_logo),
                contentDescription = "Spentopia Logo",
                modifier = Modifier
                    .size(300.dp)
                    .graphicsLayer {
                        this.alpha = alpha
                        scaleX = scale
                        scaleY = scale
                    }
            )

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Spentopia",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "내가 기록한 소비가 나를 만든다",
                fontSize = 16.sp,
                color = Color(0xFFD1D5DB)
            )
        }
    }
}
