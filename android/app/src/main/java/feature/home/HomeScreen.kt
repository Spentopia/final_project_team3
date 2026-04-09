package com.ict.spentopia.feature.home // 홈 화면 관련 코드를 모아두는 패키지

import androidx.compose.foundation.layout.Arrangement // Column이나 Row 내부 요소의 배치 방식을 정할 때 사용
import androidx.compose.foundation.layout.Column // 요소들을 세로 방향으로 배치하는 레이아웃
import androidx.compose.foundation.layout.fillMaxSize // 부모가 허용하는 최대 크기만큼 채우는 Modifier
import androidx.compose.foundation.layout.padding // 바깥 또는 안쪽 여백을 주는 Modifier
import androidx.compose.material3.Button // 클릭 가능한 버튼 컴포넌트
import androidx.compose.material3.Text // 글자를 화면에 보여주는 컴포넌트
import androidx.compose.runtime.Composable // 이 함수가 Compose UI를 그리는 함수임을 나타냄
import androidx.compose.ui.Alignment // 정렬 기준을 지정할 때 사용
import androidx.compose.ui.Modifier // 크기, 여백 등 UI 속성을 붙일 때 사용하는 도구
import androidx.compose.ui.unit.dp // 여백이나 크기를 dp 단위로 지정하기 위해 사용

@Composable // Compose에서 호출되는 화면 함수라는 뜻
fun HomeScreen( // 홈 화면 UI를 구성하는 함수
    onLedgerClick: () -> Unit, // "가계부로 이동" 버튼 클릭 시 실행할 함수
    onMyPageClick: () -> Unit // "마이페이지로 이동" 버튼 클릭 시 실행할 함수
) {
    Column( // 화면의 요소들을 세로로 배치하는 레이아웃
        modifier = Modifier
            .fillMaxSize() // 화면 전체 크기를 꽉 채움
            .padding(24.dp), // 화면 가장자리와 내용 사이에 24dp 여백 추가
        verticalArrangement = Arrangement.Center, // 세로 방향으로 가운데 정렬
        horizontalAlignment = Alignment.CenterHorizontally // 가로 방향으로 가운데 정렬
    ) {
        Text("홈 화면") // 화면 중앙에 "홈 화면"이라는 텍스트를 표시

        Button( // 첫 번째 버튼: 가계부 화면으로 이동
            onClick = onLedgerClick, // 버튼을 누르면 전달받은 onLedgerClick 함수 실행
            modifier = Modifier.padding(top = 16.dp) // 위쪽에 16dp 여백 추가
        ) {
            Text("가계부로 이동") // 버튼 안에 표시될 글자
        }

        Button( // 두 번째 버튼: 마이페이지 화면으로 이동
            onClick = onMyPageClick, // 버튼을 누르면 전달받은 onMyPageClick 함수 실행
            modifier = Modifier.padding(top = 16.dp) // 위쪽에 16dp 여백 추가
        ) {
            Text("마이페이지로 이동") // 버튼 안에 표시될 글자
        }
    }
}