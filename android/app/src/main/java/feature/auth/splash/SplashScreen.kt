package com.ict.spentopia.feature.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    navController: NavController,
    isDarkTheme: Boolean
) {
    LaunchedEffect(Unit) {
        delay(3500) // 썸네일 시간
        navController.navigate("login") {
            popUpTo("splash") { inclusive = true }
        }
    }

    if (isDarkTheme) { //다크 모드설정시 다크모드 썸네일이 나옴 라이트모드 설정시 라이트모드 설정으로 나옴  /
        //처음 들어갔을때 기본으로 라이트모드
        DarkSplashContent()
    } else {
        LightSplashContent()
    }
}