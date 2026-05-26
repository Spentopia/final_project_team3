package com.ict.spentopia.ui.toast

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.ict.spentopia.R

// 앱 전체 화면에서 공통으로 사용할 토스트 아이콘과 강조색 종류입니다.
enum class AppToastType {
    SUCCESS, // 등록, 저장, 수정 완료처럼 성공 결과에 체크 아이콘을 보여줌
    DELETE, // 삭제 완료처럼 제거 결과에 빨간 휴지통 아이콘을 보여줌
    WALLET, // 지갑 연결과 결제 안내에 지갑 아이콘을 보여줌
    GAME, // 게임 로그인 코드 발급처럼 게임 기능 안내에 쓰는 유형임
    INFO, // 기본 알림 메시지에 알림 아이콘을 보여줌
    WARNING, // 확인이 필요한 입력 안내에 경고 아이콘을 보여줌
    ERROR // 요청 실패나 오류 안내에 취소 아이콘을 보여줌
}

// 메시지와 종류를 받아 앱 공통 디자인의 하단 토스트를 화면에 표시합니다.
fun showAppToast(
    context: Context, // 토스트를 표시할 현재 화면 정보를 받음
    message: String, // 사용자에게 보여줄 안내 문구를 받음
    type: AppToastType? = null // 종류가 없으면 메시지 내용에서 자동으로 결정함
) {
    if (message.isBlank()) return // 빈 문구는 화면에 표시하지 않고 종료함

    val appContext = context.applicationContext // 화면 생명주기와 무관한 앱 Context를 사용함
    val resolvedType = type ?: message.inferToastType() // 전달 타입이 없으면 문구로 알맞은 종류를 추론함
    val isDark = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES // 현재 시스템 테마가 다크 모드인지 확인함
    val accentColor = resolvedType.accentColor(isDark) // 유형과 테마에 맞는 포인트 색을 가져옴
    val backgroundColor = resolvedType.backgroundColor(isDark) // 토스트 종류별로 라이트/다크 카드 배경색을 가져옴
    val borderColor = resolvedType.borderColor(isDark, accentColor) // 흰 배경에서도 구분되는 테두리색을 가져옴
    val textColor = if (isDark) Color.WHITE else Color.parseColor("#101828") // 문구는 테마 배경에서 읽히는 기본색을 사용함

    val container = LinearLayout(appContext).apply { // 아이콘과 문구를 담을 둥근 토스트 카드 영역을 만듦
        orientation = LinearLayout.HORIZONTAL // 아이콘과 텍스트를 가로로 배치함
        gravity = Gravity.CENTER_VERTICAL // 카드 안의 내용을 세로 중앙에 맞춤
        setPadding(dp(14), dp(10), dp(16), dp(10)) // 카드 안쪽 여백을 설정함
        minimumHeight = dp(54) // 토스트가 너무 작아지지 않도록 최소 높이를 정함
        elevation = dp(10).toFloat() // 배경에서 떠 보이도록 그림자를 넣음
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE // 둥근 사각형 카드 형태를 사용함
            cornerRadius = dp(26).toFloat() // 기존 디자인과 맞는 둥근 모서리를 설정함
            setColor(backgroundColor) // 라이트/다크 모드에 맞는 카드 배경을 적용함
            setStroke(dp(1), borderColor) // 토스트 종류와 모드에 맞는 테두리색을 적용함
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ) // 문구 길이에 맞춰 카드 크기가 정해지게 함
    }

    val iconShell = FrameLayout(appContext).apply { // 아이콘 뒤에 표시할 원형 배경 영역을 만듦
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL // 아이콘 배경을 원 형태로 표시함
            setColor(accentColor.withAlpha(if (isDark) 42 else 28)) // 포인트색을 연하게 적용해 아이콘을 강조함
        }
        addView(
            ImageView(appContext).apply {
                setImageResource(resolvedType.iconRes()) // 유형에 맞는 체크, 삭제, 게임 등의 아이콘을 넣음
                imageTintList = ColorStateList.valueOf(accentColor) // 벡터 아이콘에도 포인트색이 확실히 적용되도록 tint를 지정함
                scaleType = ImageView.ScaleType.CENTER_INSIDE // 아이콘이 원형 영역 안에 맞게 표시되도록 함
            },
            FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        ) // 아이콘을 원형 배경 중앙에 배치함
    }

    container.addView(iconShell, LinearLayout.LayoutParams(dp(appContext, 34), dp(appContext, 34))) // 왼쪽에 아이콘 영역을 추가함
    container.addView(
        TextView(appContext).apply { // 아이콘 옆에 표시할 안내 문구 영역을 만듦
            text = message // 받은 토스트 문구를 표시함
            setTextColor(textColor) // 테마에 맞는 문구 색상을 적용함
            textSize = 14f // 작은 알림 카드에 맞는 글자 크기를 적용함
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) // 결과 문구가 잘 보이도록 굵게 표시함
            maxLines = 2 // 긴 문구도 카드가 과도하게 커지지 않도록 제한함
            includeFontPadding = false // 글자 기본 여백을 빼 카드 정렬을 맞춤
            gravity = Gravity.CENTER_VERTICAL // 텍스트를 아이콘 높이 중앙에 맞춤
        },
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(appContext, 10) // 아이콘과 문구 사이 여백을 줌
        }
    )

    Toast(appContext).apply { // 완성한 카드 View를 실제 Android 토스트로 표시함
        duration = Toast.LENGTH_SHORT // 짧은 결과 안내에 맞는 노출 시간을 사용함
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(appContext, 88)) // 하단 중앙에서 네비게이션 영역 위에 표시함
        view = container // 기본 토스트 대신 커스텀 카드 View를 사용함
        show() // 화면에 토스트를 출력함
    }
}

// 유형별 포인트색을 라이트/다크 모드에 맞게 반환합니다.
private fun AppToastType.accentColor(isDark: Boolean): Int {
    return when (this) {
        AppToastType.SUCCESS -> Color.parseColor(if (isDark) "#34D399" else "#16A34A") // 체크 표시는 모드에 맞는 선명한 초록색을 사용함
        AppToastType.DELETE -> Color.parseColor(if (isDark) "#FB7185" else "#E11D48")
        AppToastType.WALLET -> Color.parseColor(if (isDark) "#00E5C3" else "#00C7A3")
        AppToastType.GAME -> Color.parseColor(if (isDark) "#C4B5FD" else "#6D28D9") // 게임 색은 배경 대비가 나도록 모드별로 분리함
        AppToastType.INFO -> Color.parseColor(if (isDark) "#4D78FF" else "#3D7BFF")
        AppToastType.WARNING -> Color.parseColor("#FFB020")
        AppToastType.ERROR -> Color.parseColor("#FF4D6D")
    }
}

// 토스트 종류별 카드 배경색을 반환해 라이트 화면에서도 상태별 카드가 구분되게 합니다.
private fun AppToastType.backgroundColor(isDark: Boolean): Int {
    return when {
        isDark -> Color.parseColor("#1B1D2A") // 다크 모드에서는 기존의 짙은 카드 배경을 공통으로 사용함
        this == AppToastType.SUCCESS -> Color.parseColor("#F0FDF4") // 성공 토스트는 연한 초록 배경을 사용함
        this == AppToastType.GAME -> Color.parseColor("#F5F3FF") // 게임 안내는 연한 보라 배경을 사용함
        else -> Color.WHITE // 그 외 라이트 토스트는 기본 흰색 배경을 사용함
    }
}

// 포인트색과 배경 대비에 맞춘 테두리색을 반환합니다.
private fun AppToastType.borderColor(isDark: Boolean, accentColor: Int): Int {
    return when {
        isDark -> accentColor.withAlpha(150) // 다크 모드에서는 밝은 포인트 테두리를 반투명으로 표시함
        this == AppToastType.SUCCESS -> Color.parseColor("#86EFAC") // 라이트 성공 토스트는 밝은 초록 테두리로 경계를 보여줌
        this == AppToastType.GAME -> Color.parseColor("#C4B5FD") // 라이트 게임 토스트는 보라색 테두리로 구분함
        else -> accentColor.withAlpha(120) // 나머지는 기존 포인트색 테두리를 유지함
    }
}

// 유형별로 화면에 표시할 drawable 아이콘을 반환합니다.
private fun AppToastType.iconRes(): Int {
    return when (this) {
        AppToastType.SUCCESS -> R.drawable.ic_toast_check_circle
        AppToastType.DELETE -> R.drawable.ic_toast_delete
        AppToastType.WALLET -> R.drawable.ic_toast_wallet
        AppToastType.GAME -> R.drawable.ic_toast_gamepad // 게임 코드 안내에는 게임패드 아이콘을 보여줌
        AppToastType.INFO -> R.drawable.ic_toast_notifications
        AppToastType.WARNING -> R.drawable.ic_toast_warning
        AppToastType.ERROR -> R.drawable.ic_toast_cancel
    }
}

// 호출부에서 타입을 넘기지 않았을 때 메시지 키워드로 토스트 유형을 결정합니다.
private fun String.inferToastType(): AppToastType {
    val text = lowercase() // 영문 메시지도 같은 기준으로 검사할 수 있게 소문자로 바꿈
    return when {
        listOf("실패", "오류", "에러", "못", "취소", "없습니다", "만료", "lost", "failed", "error").any { text.contains(it) } -> AppToastType.ERROR
        listOf("삭제", "제거").any { text.contains(it) } -> AppToastType.DELETE
        listOf("완료", "성공", "저장", "등록", "수정", "생성", "연결", "다운로드").any { text.contains(it) } -> AppToastType.SUCCESS
        listOf("지갑", "결제", "서명", "wallet", "payment").any { text.contains(it) } -> AppToastType.WALLET
        listOf("필요", "입력", "확인", "주의", "경고").any { text.contains(it) } -> AppToastType.WARNING
        else -> AppToastType.INFO
    }
}

// 포인트색에 테두리나 배경용 투명도를 입힌 새 색상을 만듭니다.
private fun Int.withAlpha(alpha: Int): Int {
    return Color.argb(alpha.coerceIn(0, 255), Color.red(this), Color.green(this), Color.blue(this))
}

// View가 가진 Context 기준으로 dp 값을 픽셀 값으로 바꿉니다.
private fun View.dp(value: Int): Int {
    return dp(context, value)
}

// 기기 화면 밀도에 맞춰 dp 단위를 실제 픽셀 수로 변환합니다.
private fun dp(context: Context, value: Int): Int {
    return (value * context.resources.displayMetrics.density).toInt()
}
