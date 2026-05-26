package com.ict.spentopia.ui.toast

import android.content.Context
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

enum class AppToastType {
    SUCCESS,
    DELETE,
    WALLET,
    INFO,
    WARNING,
    ERROR
}

fun showAppToast(
    context: Context,
    message: String,
    type: AppToastType? = null
) {
    if (message.isBlank()) return

    val appContext = context.applicationContext
    val resolvedType = type ?: message.inferToastType()
    val isDark = (appContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
        Configuration.UI_MODE_NIGHT_YES
    val accentColor = resolvedType.accentColor(isDark)
    val backgroundColor = if (isDark) Color.parseColor("#1B1D2A") else Color.WHITE
    val textColor = if (isDark) Color.WHITE else Color.parseColor("#101828")

    val container = LinearLayout(appContext).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(16), dp(10))
        minimumHeight = dp(54)
        elevation = dp(10).toFloat()
        background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(26).toFloat()
            setColor(backgroundColor)
            setStroke(dp(1), accentColor.withAlpha(if (isDark) 150 else 120))
        }
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    val iconShell = FrameLayout(appContext).apply {
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(accentColor.withAlpha(if (isDark) 42 else 28))
        }
        addView(
            ImageView(appContext).apply {
                setImageResource(resolvedType.iconRes())
                setColorFilter(accentColor)
                scaleType = ImageView.ScaleType.CENTER_INSIDE
            },
            FrameLayout.LayoutParams(dp(20), dp(20), Gravity.CENTER)
        )
    }

    container.addView(iconShell, LinearLayout.LayoutParams(dp(appContext, 34), dp(appContext, 34)))
    container.addView(
        TextView(appContext).apply {
            text = message
            setTextColor(textColor)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            maxLines = 2
            includeFontPadding = false
            gravity = Gravity.CENTER_VERTICAL
        },
        LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = dp(appContext, 10)
        }
    )

    Toast(appContext).apply {
        duration = Toast.LENGTH_SHORT
        setGravity(Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL, 0, dp(appContext, 88))
        view = container
        show()
    }
}

private fun AppToastType.accentColor(isDark: Boolean): Int {
    return when (this) {
        AppToastType.SUCCESS -> Color.parseColor(if (isDark) "#34D399" else "#16A34A")
        AppToastType.DELETE -> Color.parseColor(if (isDark) "#FB7185" else "#E11D48")
        AppToastType.WALLET -> Color.parseColor(if (isDark) "#00E5C3" else "#00C7A3")
        AppToastType.INFO -> Color.parseColor(if (isDark) "#4D78FF" else "#3D7BFF")
        AppToastType.WARNING -> Color.parseColor("#FFB020")
        AppToastType.ERROR -> Color.parseColor("#FF4D6D")
    }
}

private fun AppToastType.iconRes(): Int {
    return when (this) {
        AppToastType.SUCCESS -> R.drawable.ic_toast_check_circle
        AppToastType.DELETE -> R.drawable.ic_toast_delete
        AppToastType.WALLET -> R.drawable.ic_toast_wallet
        AppToastType.INFO -> R.drawable.ic_toast_notifications
        AppToastType.WARNING -> R.drawable.ic_toast_warning
        AppToastType.ERROR -> R.drawable.ic_toast_cancel
    }
}

private fun String.inferToastType(): AppToastType {
    val text = lowercase()
    return when {
        listOf("실패", "오류", "에러", "못", "취소", "없습니다", "만료", "lost", "failed", "error").any { text.contains(it) } -> AppToastType.ERROR
        listOf("삭제", "제거").any { text.contains(it) } -> AppToastType.DELETE
        listOf("완료", "성공", "저장", "등록", "수정", "생성", "연결", "다운로드").any { text.contains(it) } -> AppToastType.SUCCESS
        listOf("지갑", "결제", "서명", "wallet", "payment").any { text.contains(it) } -> AppToastType.WALLET
        listOf("필요", "입력", "확인", "주의", "경고").any { text.contains(it) } -> AppToastType.WARNING
        else -> AppToastType.INFO
    }
}

private fun Int.withAlpha(alpha: Int): Int {
    return Color.argb(alpha.coerceIn(0, 255), Color.red(this), Color.green(this), Color.blue(this))
}

private fun View.dp(value: Int): Int {
    return dp(context, value)
}

private fun dp(context: Context, value: Int): Int {
    return (value * context.resources.displayMetrics.density).toInt()
}
