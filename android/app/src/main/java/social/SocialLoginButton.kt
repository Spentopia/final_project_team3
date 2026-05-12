package com.ict.spentopia.feature.social // 이 파일이 속한 패키지 위치를 적음

import androidx.annotation.DrawableRes // DrawableRes 기능을 가져옴
import androidx.compose.foundation.Image // 이미지 표시 컴포넌트를 가져옴
import androidx.compose.foundation.border // border 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Box // 겹쳐서 배치하는 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.layout.size // size 기능을 가져옴
import androidx.compose.foundation.layout.width // width 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Alignment // Alignment 기능을 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.graphics.Color // 색상 타입을 가져옴
import androidx.compose.ui.layout.ContentScale // ContentScale 기능을 가져옴
import androidx.compose.ui.res.painterResource // painterResource 기능을 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SocialLoginButton( // 로그인 기능을 실행하는 함수 시작
    text: String, // text 값을 받음
    @DrawableRes iconRes: Int?, // 이 코드에 특별한 역할을 붙이는 표시
    onClick: () -> Unit, // 눌렀을 때 실행할 함수를 받음
    modifier: Modifier = Modifier // modifier 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Box( // 안쪽 UI를 한 영역에 겹쳐 배치함
        modifier = modifier // modifier 값을 modifier 값에 넣음
            .fillMaxWidth()
            .height(52.dp)
            .border(
                width = 1.dp, // width 값을 정해줌
                color = Color(0xFFD9D9D9), // color 값을 정해줌
                shape = RoundedCornerShape(10.dp) // shape 값을 정해줌
            )
            .clickable { onClick() }
            .padding(horizontal = 18.dp), // .padding(horizontal 값을 정해줌
        contentAlignment = Alignment.Center // contentAlignment 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            verticalAlignment = Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            if (iconRes != null) { // 조건이 맞는지 확인함
                Image( // 화면에 이미지를 보여줌
                    painter = painterResource(id = iconRes), // painter 값을 정해줌
                    contentDescription = text, // text 값을 contentDescription 값에 넣음
                    modifier = Modifier.size(20.dp), // UI 크기나 여백 같은 모양을 정함
                    contentScale = ContentScale.Fit // contentScale 값을 정해줌
                )
            } else { // 이 블록 안의 내용이 시작됨
                Spacer(modifier = Modifier.size(20.dp)) // UI 크기나 여백 같은 모양을 정함
            }

            Spacer(modifier = Modifier.width(16.dp)) // UI 크기나 여백 같은 모양을 정함

            Text( // 화면에 글자를 보여줌
                text = text, // text 값을 text 값에 넣음
                color = Color(0xFF222222), // color 값을 정해줌
                fontWeight = FontWeight.Medium // fontWeight 값을 정해줌
            )
        }
    }
}
