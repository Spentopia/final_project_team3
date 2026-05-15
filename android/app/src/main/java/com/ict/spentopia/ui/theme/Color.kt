package com.ict.spentopia.ui.theme // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴

val SpentopiaNavy = Color(0xFF0F172A) // SpentopiaNavy 값을 저장함
val SpentopiaNavyPurple = Color(0xFF1E1B4B) // SpentopiaNavyPurple 값을 저장함
val SpentopiaMutedPurple = Color(0xFF312E81) // SpentopiaMutedPurple 값을 저장함
val SpentopiaLightBackground = Color(0xFFF8FBFF) // SpentopiaLightBackground 값을 저장함
val SpentopiaLightSurface = Color.White // SpentopiaLightSurface 값을 저장함
val SpentopiaLightSurfaceVariant = Color(0xFFF7FBFF) // SpentopiaLightSurfaceVariant 값을 저장함
val SpentopiaLightText = Color(0xFF0F172A) // SpentopiaLightText 값을 저장함
val SpentopiaLightTextMuted = Color(0xFF53657D) // SpentopiaLightTextMuted 값을 저장함
val SpentopiaLightBorder = Color(0xFF7DD3FC) // SpentopiaLightBorder 값을 저장함
val SpentopiaLightButton = Color(0xFFE0F2FE) // 라이트모드 버튼 배경색을 저장함
val SpentopiaLightButtonPressed = Color(0xFFDBEAFE) // 라이트모드 버튼 눌림색을 저장함
val SpentopiaLightButtonContent = Color(0xFF1D4ED8) // 라이트모드 버튼 글자색을 저장함
val SpentopiaDarkBackground = Color(0xFF090B16) // SpentopiaDarkBackground 값을 저장함
val SpentopiaDarkSurface = Color(0xFF111827) // SpentopiaDarkSurface 값을 저장함
val SpentopiaDarkSurfaceVariant = Color(0xFF1E293B) // SpentopiaDarkSurfaceVariant 값을 저장함
val SpentopiaDarkText = Color(0xFFF8FAFC) // SpentopiaDarkText 값을 저장함
val SpentopiaDarkTextMuted = Color(0xFFCBD5E1) // SpentopiaDarkTextMuted 값을 저장함
val SpentopiaDarkTextFaint = Color(0xFF94A3B8) // SpentopiaDarkTextFaint 값을 저장함
val SpentopiaDarkBorder = Color(0xFF334155) // SpentopiaDarkBorder 값을 저장함
val SpentopiaGradientStart = Color(0xFF0B1020) // SpentopiaGradientStart 값을 저장함
val SpentopiaGradientMiddle = Color(0xFF111827) // SpentopiaGradientMiddle 값을 저장함
val SpentopiaGradientPurple = Color(0xFF1E1B4B) // SpentopiaGradientPurple 값을 저장함
val SpentopiaGradientEnd = Color(0xFF2D1847) // SpentopiaGradientEnd 값을 저장함
val SpentopiaGlowPurple = Color(0xFF7C3AED) // SpentopiaGlowPurple 값을 저장함
val SpentopiaGlowBlue = Color(0xFF2F80ED) // SpentopiaGlowBlue 값을 저장함
val SpentopiaGlowCyan = Color(0xFF12C2E9) // SpentopiaGlowCyan 값을 저장함
val SpentopiaSoftPurple = Color(0xFFEDEBFF) // SpentopiaSoftPurple 값을 저장함
val SpentopiaSoftPurple2 = Color(0xFFEDE9FE) // SpentopiaSoftPurple2 값을 저장함
val SpentopiaSoftBlue = Color(0xFFE0E7FF) // SpentopiaSoftBlue 값을 저장함
val SpentopiaSoftSky = Color(0xFFEFF6FF) // SpentopiaSoftSky 값을 저장함
val SpentopiaSurfaceLine = Color(0xFFE2E8F0) // SpentopiaSurfaceLine 값을 저장함
val SpentopiaText = Color(0xFF111827) // SpentopiaText 값을 저장함
val SpentopiaTextMuted = Color(0xFF64748B) // SpentopiaTextMuted 값을 저장함
val SpentopiaIconMuted = Color(0xFF64748B) // SpentopiaIconMuted 값을 저장함
val SpentopiaWalletGradientColors = listOf( // 지갑 관련 값을 저장함
    SpentopiaGradientStart,
    SpentopiaGradientMiddle,
    SpentopiaGradientPurple,
    SpentopiaGradientEnd
)
val SpentopiaActionGradientColors = SpentopiaWalletGradientColors // SpentopiaActionGradientColors 값을 저장함
val SpentopiaCtaGradientColors = listOf( // SpentopiaCtaGradientColors 값을 저장함
    Color(0xFF7C3AED), // Color 함수를 실행함
    Color(0xFF6D28D9), // Color 함수를 실행함
    Color(0xFF2F80ED), // Color 함수를 실행함
    Color(0xFF12C2E9) // Color 함수를 실행함
)
val SpentopiaLightCtaGradientColors = listOf( // SpentopiaLightCtaGradientColors 값을 저장함
    SpentopiaLightButton,
    SpentopiaLightButton,
    SpentopiaLightButtonPressed
)
val SpentopiaLightCtaBorder = Color(0xFFBFDBFE) // SpentopiaLightCtaBorder 값을 저장함
val SpentopiaLightCtaContent = SpentopiaLightButtonContent // SpentopiaLightCtaContent 값을 저장함

fun spentopiaAppButtonColor(isDark: Boolean): Color { // 앱 공통 버튼 배경색을 돌려줌
    return if (isDark) SpentopiaGradientEnd else SpentopiaLightButton
}

fun spentopiaAppButtonContentColor(isDark: Boolean): Color { // 앱 공통 버튼 글자색을 돌려줌
    return if (isDark) Color.White else SpentopiaLightButtonContent
}

fun spentopiaAppButtonBorderColor(isDark: Boolean): Color { // 앱 공통 버튼 테두리색을 돌려줌
    return if (isDark) Color(0xFFC4B5FD).copy(alpha = 0.48f) else Color(0xFF2563EB).copy(alpha = 0.22f)
}

fun spentopiaCtaGradientColors(isDark: Boolean): List<Color> { // spentopiaCtaGradientColors 함수를 선언함
    return if (isDark) SpentopiaCtaGradientColors else SpentopiaLightCtaGradientColors // 이 값을 함수 결과로 돌려줌
}

fun spentopiaCtaContentColor(isDark: Boolean): Color { // spentopiaCtaContentColor 함수를 선언함
    return if (isDark) Color.White else SpentopiaLightCtaContent // 이 값을 함수 결과로 돌려줌
}

fun spentopiaCtaBorderColor(isDark: Boolean): Color { // spentopiaCtaBorderColor 함수를 선언함
    return if (isDark) SpentopiaGlowPurple.copy(alpha = 0.45f) else SpentopiaLightCtaBorder // 이 값을 함수 결과로 돌려줌
}

fun spentopiaFeatureGradientColors(isDark: Boolean): List<Color> { // spentopiaFeatureGradientColors 함수를 선언함
    return if (isDark) SpentopiaWalletGradientColors else SpentopiaLightCtaGradientColors // 이 값을 함수 결과로 돌려줌
}

val Purple80 = Color(0xFFC7D2FE) // Purple80 값을 저장함
val PurpleGrey80 = Color(0xFFCBD5E1) // PurpleGrey80 값을 저장함
val Pink80 = Color(0xFFD8B4FE) // Pink80 값을 저장함

val Purple40 = SpentopiaMutedPurple // Purple40 값을 저장함
val PurpleGrey40 = Color(0xFF475569) // PurpleGrey40 값을 저장함
val Pink40 = SpentopiaGradientEnd // Pink40 값을 저장함
