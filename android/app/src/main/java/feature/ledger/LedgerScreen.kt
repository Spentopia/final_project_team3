package com.ict.spentopia.feature.ledger // 이 파일이 속한 패키지 위치를 적음

// 가계부 화면임
// 현재는 자리표시용 구조

import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.fillMaxSize // fillMaxSize 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun LedgerScreen() { // LedgerScreen 함수를 선언함
    Column( // 안쪽 UI를 세로로 배치함
        modifier = Modifier.fillMaxSize(), // UI 크기나 여백 같은 모양을 정함
        verticalArrangement = Arrangement.Center, // verticalArrangement 값을 정해줌
        horizontalAlignment = Alignment.CenterHorizontally // horizontalAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Text("가계부 화면") // 화면에 글자를 보여줌
    }
}
