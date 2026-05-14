package com.ict.spentopia.ui.theme // 이 파일이 속한 패키지 위치를 적음

import android.app.Activity // Activity 기능을 가져옴
import android.os.Build // Build 기능을 가져옴
import androidx.compose.foundation.isSystemInDarkTheme // isSystemInDarkTheme 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.darkColorScheme // darkColorScheme 기능을 가져옴
import androidx.compose.material3.dynamicDarkColorScheme // dynamicDarkColorScheme 기능을 가져옴
import androidx.compose.material3.dynamicLightColorScheme // dynamicLightColorScheme 기능을 가져옴
import androidx.compose.material3.lightColorScheme // lightColorScheme 기능을 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.platform.LocalContext // LocalContext 기능을 가져옴

private val DarkColorScheme = darkColorScheme( // DarkColorScheme 값을 저장함
    primary = SpentopiaGlowPurple, // SpentopiaGlowPurple 값을 primary 값에 넣음
    secondary = SpentopiaGlowBlue, // SpentopiaGlowBlue 값을 secondary 값에 넣음
    tertiary = SpentopiaGlowCyan, // SpentopiaGlowCyan 값을 tertiary 값에 넣음
    background = SpentopiaDarkBackground, // SpentopiaDarkBackground 값을 background 값에 넣음
    onBackground = SpentopiaDarkText, // SpentopiaDarkText 값을 onBackground 때 실행할 함수에 넣음
    surface = SpentopiaDarkSurface, // SpentopiaDarkSurface 값을 surface 값에 넣음
    onSurface = SpentopiaDarkText, // SpentopiaDarkText 값을 onSurface 때 실행할 함수에 넣음
    surfaceVariant = SpentopiaDarkSurfaceVariant, // SpentopiaDarkSurfaceVariant 값을 surfaceVariant 값에 넣음
    onSurfaceVariant = SpentopiaDarkTextMuted, // SpentopiaDarkTextMuted 값을 onSurfaceVariant 때 실행할 함수에 넣음
    outline = SpentopiaDarkBorder, // SpentopiaDarkBorder 값을 outline 값에 넣음
    outlineVariant = Color(0xFF475569), // outlineVariant 값을 정해줌
    secondaryContainer = SpentopiaDarkSurfaceVariant, // SpentopiaDarkSurfaceVariant 값을 secondaryContainer 값에 넣음
    onSecondaryContainer = SpentopiaDarkText, // SpentopiaDarkText 값을 onSecondaryContainer 때 실행할 함수에 넣음
    primaryContainer = Color(0xFF1E1B4B), // primaryContainer 값을 정해줌
    onPrimaryContainer = SpentopiaDarkText, // SpentopiaDarkText 값을 onPrimaryContainer 때 실행할 함수에 넣음
    inverseSurface = SpentopiaDarkText, // SpentopiaDarkText 값을 inverseSurface 값에 넣음
    inverseOnSurface = SpentopiaDarkBackground // SpentopiaDarkBackground 값을 inverseOnSurface 값에 넣음
)

private val LightColorScheme = lightColorScheme( // LightColorScheme 값을 저장함
    primary = Color(0xFF2563EB), // 라이트모드의 주요 포인트를 선명한 블루로 정함
    secondary = Color(0xFF0EA5E9), // 라이트모드 보조 포인트를 스카이블루로 정함
    tertiary = SpentopiaGlowPurple, // 보라색은 라이트모드에서 보조 포인트로만 사용함
    background = SpentopiaLightBackground, // SpentopiaLightBackground 값을 background 값에 넣음
    onBackground = SpentopiaLightText, // SpentopiaLightText 값을 onBackground 때 실행할 함수에 넣음
    surface = SpentopiaLightSurface, // SpentopiaLightSurface 값을 surface 값에 넣음
    onSurface = SpentopiaLightText, // SpentopiaLightText 값을 onSurface 때 실행할 함수에 넣음
    surfaceVariant = SpentopiaLightSurfaceVariant, // SpentopiaLightSurfaceVariant 값을 surfaceVariant 값에 넣음
    onSurfaceVariant = SpentopiaLightTextMuted, // SpentopiaLightTextMuted 값을 onSurfaceVariant 때 실행할 함수에 넣음
    primaryContainer = Color(0xFFDBEAFE), // 라이트모드 카드/버튼 배경을 밝은 스카이블루로 정함
    onPrimaryContainer = Color(0xFF1E3A8A), // 밝은 블루 배경 위에 읽히는 진한 블루 글자색임
    secondaryContainer = Color(0xFFF0F9FF), // 일반 보조 영역은 더 밝은 하늘색으로 정함
    onSecondaryContainer = Color(0xFF075985), // 보조 영역 위의 글자색임
    outline = SpentopiaLightBorder, // SpentopiaLightBorder 값을 outline 값에 넣음
    outlineVariant = SpentopiaLightBorder // SpentopiaLightBorder 값을 outlineVariant 값에 넣음

    /* Other default colors to override
    background = Color(0xFFFFFBFE), // 앱의 전체 배경색을 지정하는 예시
    surface = Color(0xFFFFFBFE), // 카드, 시트 같은 표면 색상을 지정하는 예시
    onPrimary = Color.White, // primary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onSecondary = Color.White, // secondary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onTertiary = Color.White, // tertiary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onBackground = Color(0xFF1C1B1F), // background 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onSurface = Color(0xFF1C1B1F), // surface 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    */ // 예시 주석 끝
)

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SpentopiaTheme( // SpentopiaTheme 함수를 선언함
    darkTheme: Boolean = isSystemInDarkTheme(), // darkTheme 값을 받음
    // Dynamic color is available on Android 12+ // 동적 색상 기능은 안드로이드 12 이상에서만 가능하다는 설명 주석
    dynamicColor: Boolean = false, // dynamicColor 값을 받음
    content: @Composable () -> Unit // 내용을 받음
) { // 이 블록 안의 내용이 시작됨
    val colorScheme = when { // colorScheme 값을 저장함
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // > 값을 정해줌
            val context = LocalContext.current // 현재 화면 정보를 저장함
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // 조건이 맞는지 확인함
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme // 위 조건이 아니면 이쪽을 실행함
    }

    MaterialTheme( // Material Theme 함수를 실행함
        colorScheme = colorScheme, // colorScheme 값을 colorScheme 값에 넣음
        typography = Typography, // Typography 값을 typography 값에 넣음
        content = content // 내용을 내용에 넣음
    )
}
