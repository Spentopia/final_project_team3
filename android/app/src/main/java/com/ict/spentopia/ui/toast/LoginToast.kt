package com.ict.spentopia.ui.toast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ict.spentopia.R

// 로그인 방식에 따라 브랜드 아이콘과 강조색을 나누기 위한 종류입니다.
enum class LoginToastType {
    EMAIL,
    KAKAO,
    GOOGLE,
    WALLET
}

// 로그인 요청 중인지 인증이 끝났는지 토스트 상태를 구분합니다.
enum class LoginToastStatus {
    IN_PROGRESS,
    SUCCESS
}

// 로그인 화면에서 토스트 컴포넌트에 전달할 표시 데이터입니다.
data class LoginToastData(
    val type: LoginToastType,
    val status: LoginToastStatus,
    val message: String
)

// 로그인 전용 토스트 카드입니다. 테마와 로그인 방식에 맞춰 아이콘/색상을 구성합니다.
@Composable
fun LoginToast(
    data: LoginToastData,
    modifier: Modifier = Modifier
) {
    // 현재 Material 테마 표면 밝기를 기준으로 라이트/다크 전용 색상을 선택합니다.
    val isDark = androidx.compose.material3.MaterialTheme.colorScheme.surface.luminance() < 0.4f
    val colors = loginToastColors(data.type, data.status, isDark)
    val shape = RoundedCornerShape(25.dp)

    Row(
        modifier = modifier
            .widthIn(min = 270.dp, max = 344.dp)
            .shadow(
                elevation = 14.dp,
                shape = shape,
                ambientColor = colors.accent.copy(alpha = 0.18f),
                spotColor = colors.accent.copy(alpha = 0.24f)
            )
            .background(colors.background, shape)
            .border(1.dp, colors.border, shape)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoginToastIcon(data = data, colors = colors)

        Spacer(modifier = Modifier.width(11.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.type.label,
                color = colors.title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = data.message,
                color = colors.text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 인증 처리 중에는 사용자가 대기 상태임을 알 수 있도록 로딩 표시를 붙입니다.
        if (data.status == LoginToastStatus.IN_PROGRESS) {
            CircularProgressIndicator(
                modifier = Modifier.size(17.dp),
                color = colors.accent,
                strokeWidth = 2.dp
            )
        }
    }
}

// 로그인 종류와 성공 상태에 맞는 메인 아이콘 및 성공 배지를 표시합니다.
@Composable
private fun LoginToastIcon(
    data: LoginToastData,
    colors: LoginToastColors
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(colors.iconBackground, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when (data.type) {
            LoginToastType.EMAIL -> Icon(
                imageVector = if (data.status == LoginToastStatus.SUCCESS) {
                    Icons.Rounded.CheckCircle
                } else {
                    Icons.Outlined.AccountCircle
                },
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(24.dp)
            )

            LoginToastType.KAKAO -> LoginToastImageIcon(R.drawable.ic_kakao_login)
            LoginToastType.GOOGLE -> LoginToastImageIcon(R.drawable.ic_google_login)
            LoginToastType.WALLET -> LoginToastImageIcon(R.drawable.ic_wallet_login)
        }

        // 브랜드 아이콘은 유지하면서 성공 결과도 확인할 수 있도록 작은 체크 배지를 겹칩니다.
        if (data.status == LoginToastStatus.SUCCESS && data.type != LoginToastType.EMAIL) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = colors.success,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(15.dp)
                    .background(colors.background, CircleShape)
            )
        }
    }
}

// 카카오/Google/지갑 로그인에 이미 준비된 브랜드 이미지 리소스를 재사용합니다.
@Composable
private fun LoginToastImageIcon(iconRes: Int) {
    Image(
        painter = painterResource(id = iconRes),
        contentDescription = null,
        modifier = Modifier.size(25.dp),
        contentScale = ContentScale.Fit
    )
}

// 토스트 카드에서 함께 사용하는 모드별 색상 묶음입니다.
private data class LoginToastColors(
    val background: Color,
    val border: Color,
    val accent: Color,
    val iconBackground: Color,
    val title: Color,
    val text: Color,
    val success: Color
)

// 토스트 상단에 표시할 로그인 방식 제목입니다.
private val LoginToastType.label: String
    get() = when (this) {
        LoginToastType.EMAIL -> "일반 로그인"
        LoginToastType.KAKAO -> "카카오 로그인"
        LoginToastType.GOOGLE -> "Google 로그인"
        LoginToastType.WALLET -> "지갑 로그인"
    }

// 로그인 방식의 브랜드색과 라이트/다크 배경 대비를 한 곳에서 결정합니다.
private fun loginToastColors(
    type: LoginToastType,
    status: LoginToastStatus,
    isDark: Boolean
): LoginToastColors {
    val background = if (isDark) Color(0xFF171A29) else Color.White
    val text = if (isDark) Color(0xFFF8FAFC) else Color(0xFF111827)
    val success = if (isDark) Color(0xFF34D399) else Color(0xFF16A34A) // 체크 표시는 모드에 맞는 선명한 초록색을 사용함
    val accent = when (type) {
        LoginToastType.EMAIL -> if (status == LoginToastStatus.SUCCESS) {
            success
        } else if (isDark) {
            Color(0xFF60A5FA)
        } else {
            Color(0xFF2563EB)
        }

        LoginToastType.KAKAO -> if (isDark) Color(0xFFFFE812) else Color(0xFFB88600)
        LoginToastType.GOOGLE -> if (isDark) Color(0xFF7BAAF7) else Color(0xFF4285F4)
        LoginToastType.WALLET -> if (isDark) Color(0xFF22D3EE) else Color(0xFF7C3AED)
    }

    return LoginToastColors(
        background = background,
        border = accent.copy(alpha = if (isDark) 0.52f else 0.28f),
        accent = accent,
        iconBackground = accent.copy(alpha = if (isDark) 0.16f else 0.10f),
        title = accent,
        text = text,
        success = success
    )
}
