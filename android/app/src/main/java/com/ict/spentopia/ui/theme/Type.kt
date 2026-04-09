package com.ict.spentopia.ui.theme // 이 파일이 속한 패키지 경로를 선언하는 부분

import androidx.compose.material3.Typography // Material3에서 글자 스타일 묶음(Typography)을 만들기 위한 클래스를 가져옴
import androidx.compose.ui.text.TextStyle // 개별 텍스트 스타일(글꼴, 크기, 두께 등)을 설정할 때 사용하는 클래스
import androidx.compose.ui.text.font.FontFamily // 글꼴 종류(Font Family)를 지정할 때 사용하는 클래스
import androidx.compose.ui.text.font.FontWeight // 글자 굵기(얇음, 보통, 굵음 등)를 지정할 때 사용하는 클래스
import androidx.compose.ui.unit.sp // 글자 크기나 줄 간격에 사용하는 단위 sp(scale-independent pixels)를 사용하기 위해 가져옴

// Set of Material typography styles to start with // Material Design의 기본 글자 스타일 세트를 시작하는 코드라는 설명 주석
val Typography = Typography( // 앱 전체에서 사용할 글자 스타일 묶음을 Typography라는 이름으로 생성
    bodyLarge = TextStyle( // bodyLarge 스타일(보통 큰 본문 텍스트 스타일)을 새로 정의
        fontFamily = FontFamily.Default, // 기본 시스템 글꼴을 사용하도록 설정
        fontWeight = FontWeight.Normal, // 글자 굵기를 보통(Normal)으로 설정
        fontSize = 16.sp, // 글자 크기를 16sp로 설정
        lineHeight = 24.sp, // 줄 간격을 24sp로 설정
        letterSpacing = 0.5.sp // 글자 사이 간격을 0.5sp로 설정
    ) // bodyLarge 스타일 정의 끝
    /* Other default text styles to override // 필요하면 아래의 다른 기본 텍스트 스타일도 직접 바꿀 수 있다는 예시 주석 시작
    titleLarge = TextStyle( // 큰 제목 스타일을 직접 지정하는 예시
        fontFamily = FontFamily.Default, // 기본 시스템 글꼴 사용
        fontWeight = FontWeight.Normal, // 글자 굵기를 보통으로 설정
        fontSize = 22.sp, // 글자 크기를 22sp로 설정
        lineHeight = 28.sp, // 줄 간격을 28sp로 설정
        letterSpacing = 0.sp // 글자 사이 간격을 0sp로 설정
    ), // titleLarge 스타일 예시 끝
    labelSmall = TextStyle( // 작은 라벨 텍스트 스타일을 직접 지정하는 예시
        fontFamily = FontFamily.Default, // 기본 시스템 글꼴 사용
        fontWeight = FontWeight.Medium, // 글자 굵기를 Medium으로 설정
        fontSize = 11.sp, // 글자 크기를 11sp로 설정
        lineHeight = 16.sp, // 줄 간격을 16sp로 설정
        letterSpacing = 0.5.sp // 글자 사이 간격을 0.5sp로 설정
    ) // labelSmall 스타일 예시 끝
    */ // 예시 주석 끝
) // Typography 전체 정의 끝