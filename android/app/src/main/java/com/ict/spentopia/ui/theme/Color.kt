package com.ict.spentopia.ui.theme

import androidx.compose.ui.graphics.Color

val SpentopiaNavy = Color(0xFF0F172A)
val SpentopiaNavyPurple = Color(0xFF1E1B4B)
val SpentopiaMutedPurple = Color(0xFF312E81)
val SpentopiaLightBackground = Color(0xFFF8FAFC)
val SpentopiaLightSurface = Color.White
val SpentopiaLightSurfaceVariant = Color(0xFFF1F5F9)
val SpentopiaLightText = Color(0xFF111827)
val SpentopiaLightTextMuted = Color(0xFF64748B)
val SpentopiaLightBorder = Color(0xFFE2E8F0)
val SpentopiaDarkBackground = Color(0xFF090B16)
val SpentopiaDarkSurface = Color(0xFF111827)
val SpentopiaDarkSurfaceVariant = Color(0xFF1E293B)
val SpentopiaDarkText = Color(0xFFF8FAFC)
val SpentopiaDarkTextMuted = Color(0xFFCBD5E1)
val SpentopiaDarkTextFaint = Color(0xFF94A3B8)
val SpentopiaDarkBorder = Color(0xFF334155)
val SpentopiaGradientStart = Color(0xFF0B1020)
val SpentopiaGradientMiddle = Color(0xFF111827)
val SpentopiaGradientPurple = Color(0xFF1E1B4B)
val SpentopiaGradientEnd = Color(0xFF2D1847)
val SpentopiaGlowPurple = Color(0xFF7C3AED)
val SpentopiaGlowBlue = Color(0xFF2F80ED)
val SpentopiaGlowCyan = Color(0xFF12C2E9)
val SpentopiaSoftPurple = Color(0xFFEDEBFF)
val SpentopiaSoftPurple2 = Color(0xFFEDE9FE)
val SpentopiaSoftBlue = Color(0xFFE0E7FF)
val SpentopiaSoftSky = Color(0xFFEFF6FF)
val SpentopiaSurfaceLine = Color(0xFFE2E8F0)
val SpentopiaText = Color(0xFF111827)
val SpentopiaTextMuted = Color(0xFF64748B)
val SpentopiaIconMuted = Color(0xFF64748B)
val SpentopiaWalletGradientColors = listOf(
    SpentopiaGradientStart,
    SpentopiaGradientMiddle,
    SpentopiaGradientPurple,
    SpentopiaGradientEnd
)
val SpentopiaActionGradientColors = SpentopiaWalletGradientColors
val SpentopiaCtaGradientColors = listOf(
    Color(0xFF7C3AED),
    Color(0xFF6D28D9),
    Color(0xFF2F80ED),
    Color(0xFF12C2E9)
)
val SpentopiaLightCtaGradientColors = listOf(
    SpentopiaSoftPurple2,
    SpentopiaSoftBlue,
    SpentopiaSoftSky
)
val SpentopiaLightCtaBorder = Color(0xFFC7D2FE)
val SpentopiaLightCtaContent = Color(0xFF4C1D95)

fun spentopiaCtaGradientColors(isDark: Boolean): List<Color> {
    return if (isDark) SpentopiaCtaGradientColors else SpentopiaLightCtaGradientColors
}

fun spentopiaCtaContentColor(isDark: Boolean): Color {
    return if (isDark) Color.White else SpentopiaLightCtaContent
}

fun spentopiaCtaBorderColor(isDark: Boolean): Color {
    return if (isDark) SpentopiaGlowPurple.copy(alpha = 0.45f) else SpentopiaLightCtaBorder
}

fun spentopiaFeatureGradientColors(isDark: Boolean): List<Color> {
    return if (isDark) SpentopiaWalletGradientColors else SpentopiaLightCtaGradientColors
}

val Purple80 = Color(0xFFC7D2FE) //이 파일이 속한 패키지  선언
val PurpleGrey80 = Color(0xFFCBD5E1) // jetpack Compose에서 Color 클래스 가져옴
val Pink80 = Color(0xFFD8B4FE) //  색깔 선언  분홍색 저장

val Purple40 = SpentopiaMutedPurple
val PurpleGrey40 = Color(0xFF475569)
val Pink40 = SpentopiaGradientEnd
