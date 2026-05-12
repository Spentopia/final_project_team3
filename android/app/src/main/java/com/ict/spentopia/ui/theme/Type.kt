package com.ict.spentopia.ui.theme // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.material3.Typography // Typography 기능을 가져옴
import androidx.compose.ui.text.TextStyle // TextStyle 기능을 가져옴
import androidx.compose.ui.text.font.FontFamily // FontFamily 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴

// Set of Material typography styles to start with // Material Design의 기본 글자 스타일 세트를 시작하는 코드라는 설명 주석
val Typography = Typography( // Typography 값을 저장함
    bodyLarge = TextStyle( // bodyLarge 값을 정해줌
        fontFamily = FontFamily.Default, // fontFamily 값을 정해줌
        fontWeight = FontWeight.Normal, // fontWeight 값을 정해줌
        fontSize = 16.sp, // fontSize 값을 정해줌
        lineHeight = 24.sp, // lineHeight 값을 정해줌
        letterSpacing = 0.5.sp // letterSpacing 값을 정해줌
    )
    /* Other default text styles to override
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
)