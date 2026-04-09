package com.ict.spentopia.ui.theme // 이 파일이 속한 패키지 경로를 선언하는 부분

import android.app.Activity // 안드로이드의 Activity 클래스를 가져오는 부분(현재 코드에서는 실제로 사용되지는 않음)
import android.os.Build // 현재 기기의 안드로이드 버전 정보를 확인할 때 사용하는 Build 클래스를 가져옴
import androidx.compose.foundation.isSystemInDarkTheme // 시스템이 다크모드인지 자동으로 확인하는 함수
import androidx.compose.material3.MaterialTheme // Compose Material3의 전체 테마를 적용할 때 사용하는 함수/객체
import androidx.compose.material3.darkColorScheme // 다크모드용 색상 세트를 만드는 함수
import androidx.compose.material3.dynamicDarkColorScheme // 안드로이드 12 이상에서 배경화면 기반 다크 색상을 자동 생성하는 함수
import androidx.compose.material3.dynamicLightColorScheme // 안드로이드 12 이상에서 배경화면 기반 라이트 색상을 자동 생성하는 함수
import androidx.compose.material3.lightColorScheme // 라이트모드용 색상 세트를 만드는 함수
import androidx.compose.runtime.Composable // 이 함수가 Compose에서 사용하는 UI 함수라는 것을 표시하는 어노테이션
import androidx.compose.ui.platform.LocalContext // 현재 Compose 환경의 Context를 가져올 때 사용하는 객체

private val DarkColorScheme = darkColorScheme( // 이 파일 안에서만 사용하는 다크모드용 색상 테마를 생성
    primary = Purple80, // 기본(대표) 색상을 Purple80로 설정
    secondary = PurpleGrey80, // 보조 색상을 PurpleGrey80로 설정
    tertiary = Pink80 // 세 번째 강조 색상을 Pink80로 설정
) // 다크모드 색상 세트 생성 끝

private val LightColorScheme = lightColorScheme( // 이 파일 안에서만 사용하는 라이트모드용 색상 테마를 생성
    primary = Purple40, // 기본(대표) 색상을 Purple40으로 설정
    secondary = PurpleGrey40, // 보조 색상을 PurpleGrey40으로 설정
    tertiary = Pink40 // 세 번째 강조 색상을 Pink40으로 설정

    /* Other default colors to override // 필요하면 아래 기본 색상들도 직접 덮어써서 바꿀 수 있다는 예시 주석 시작
    background = Color(0xFFFFFBFE), // 앱의 전체 배경색을 지정하는 예시
    surface = Color(0xFFFFFBFE), // 카드, 시트 같은 표면 색상을 지정하는 예시
    onPrimary = Color.White, // primary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onSecondary = Color.White, // secondary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onTertiary = Color.White, // tertiary 색상 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onBackground = Color(0xFF1C1B1F), // background 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    onSurface = Color(0xFF1C1B1F), // surface 위에 올라가는 글자/아이콘 색상을 지정하는 예시
    */ // 예시 주석 끝
) // 라이트모드 색상 세트 생성 끝

@Composable // 이 함수가 Compose UI에서 호출되는 컴포저블 함수임을 나타냄
fun SpentopiaTheme( // 앱 전체에 공통 테마를 적용하는 사용자 정의 테마 함수
    darkTheme: Boolean = isSystemInDarkTheme(), // darkTheme 값을 따로 넘기지 않으면 시스템 다크모드 설정값을 기본으로 사용
    // Dynamic color is available on Android 12+ // 동적 색상 기능은 안드로이드 12 이상에서만 가능하다는 설명 주석
    dynamicColor: Boolean = true, // 동적 색상 사용 여부를 결정하며 기본값은 true
    content: @Composable () -> Unit // 이 테마 안에 표시할 화면 UI 내용을 함수 형태로 전달받음
) { // SpentopiaTheme 함수 시작
    val colorScheme = when { // 어떤 색상 테마를 사용할지 조건에 따라 결정해서 colorScheme에 저장
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { // 동적 색상 사용이 켜져 있고 안드로이드 버전이 12 이상이면
            val context = LocalContext.current // 현재 앱/화면의 Context 정보를 가져옴
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context) // 다크모드면 동적 다크 테마, 아니면 동적 라이트 테마를 적용
        } // 첫 번째 조건 처리 끝

        darkTheme -> DarkColorScheme // 동적 색상을 쓰지 않더라도 다크모드면 미리 정의한 다크 색상 테마 사용
        else -> LightColorScheme // 그 외에는 미리 정의한 라이트 색상 테마 사용
    } // colorScheme 결정 끝

    MaterialTheme( // Compose Material3의 기본 테마 시스템을 적용
        colorScheme = colorScheme, // 위에서 결정한 색상 테마를 적용
        typography = Typography, // 글꼴 스타일 세트(Typography.kt 등에 정의된 값)를 적용
        content = content // 이 테마 안에 실제로 그릴 화면 내용을 배치
    ) // MaterialTheme 적용 끝
} // SpentopiaTheme 함수 끝