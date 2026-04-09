package com.ict.spentopia.feature.auth // 로그인 화면 관련 코드를 모아둔 패키지

import androidx.compose.foundation.Image // 이미지 표시용 컴포넌트
import androidx.compose.foundation.background // 배경색/배경 브러시 적용
import androidx.compose.foundation.layout.Arrangement // Row/Column 내부 정렬 방식 설정
import androidx.compose.foundation.layout.Box // 겹쳐서 배치할 수 있는 레이아웃
import androidx.compose.foundation.layout.Column // 세로로 배치하는 레이아웃
import androidx.compose.foundation.layout.Row // 가로로 배치하는 레이아웃
import androidx.compose.foundation.layout.Spacer // 빈 공간(여백) 만들기
import androidx.compose.foundation.layout.fillMaxSize // 가능한 최대 크기만큼 채우기
import androidx.compose.foundation.layout.fillMaxWidth // 가로를 최대 너비로 채우기
import androidx.compose.foundation.layout.height // 높이 지정
import androidx.compose.foundation.layout.padding // 안쪽 여백 지정
import androidx.compose.foundation.layout.size // 가로/세로 크기 지정
import androidx.compose.foundation.text.KeyboardOptions // 키보드 옵션 설정
import androidx.compose.material3.Button // 기본 버튼 컴포넌트
import androidx.compose.material3.ButtonDefaults // 버튼 기본 스타일 관련 설정
import androidx.compose.material3.HorizontalDivider // 가로 구분선
import androidx.compose.material3.OutlinedTextField // 외곽선 형태의 입력창
import androidx.compose.material3.OutlinedTextFieldDefaults // 입력창 기본 색상 설정
import androidx.compose.material3.Text // 텍스트 출력 컴포넌트
import androidx.compose.material3.TextButton // 텍스트 형태 버튼
import androidx.compose.runtime.Composable // Compose UI 함수임을 나타내는 어노테이션
import androidx.compose.runtime.getValue // by 위임 문법으로 상태값을 읽기 위해 필요
import androidx.compose.runtime.mutableStateOf // 상태(State) 생성
import androidx.compose.runtime.remember // 재구성(recomposition)돼도 상태를 유지
import androidx.compose.runtime.setValue // by 위임 문법으로 상태값 변경을 위해 필요
import androidx.compose.ui.Alignment // 정렬 기준 설정
import androidx.compose.ui.Modifier // 크기, 배경, 여백 등 UI 속성 연결용
import androidx.compose.ui.graphics.Brush // 그라데이션 같은 브러시 표현
import androidx.compose.ui.graphics.Color // 색상 표현
import androidx.compose.ui.res.painterResource // drawable 리소스 이미지를 불러올 때 사용
import androidx.compose.ui.text.font.FontWeight // 글자 굵기 설정
import androidx.compose.ui.text.input.KeyboardType // 키보드 타입(이메일, 비밀번호 등) 설정
import androidx.compose.ui.text.input.PasswordVisualTransformation // 비밀번호를 ●●● 형태로 가려줌
import androidx.compose.ui.text.style.TextDecoration // 밑줄 등 텍스트 장식 설정
import androidx.compose.ui.unit.dp // 여백/크기 단위
import androidx.compose.ui.unit.sp // 글자 크기 단위
import com.ict.spentopia.R // 프로젝트 리소스(drawable 등) 접근
import com.ict.spentopia.feature.social.SocialLoginButton // 커스텀 소셜 로그인 버튼 컴포넌트 가져오기

@Composable // Compose에서 화면을 그리는 함수라는 뜻
fun LoginScreen( // 로그인 화면 전체 UI를 담당하는 함수
    onLoginClick: () -> Unit, // 로그인 버튼 클릭 시 실행할 함수
    onSignUpClick: () -> Unit, // 회원가입 버튼 클릭 시 실행할 함수
    onKakaoClick: () -> Unit = {}, // 카카오 로그인 버튼 클릭 시 실행할 함수, 기본값은 빈 함수
    onNaverClick: () -> Unit = {}, // 네이버 로그인 버튼 클릭 시 실행할 함수, 기본값은 빈 함수
    onGoogleClick: () -> Unit = {} // 구글 로그인 버튼 클릭 시 실행할 함수, 기본값은 빈 함수
) {
    var email by remember { mutableStateOf("") } // 이메일 입력값을 저장하는 상태 변수
    var password by remember { mutableStateOf("") } // 비밀번호 입력값을 저장하는 상태 변수

    Column( // 화면 전체를 세로 방향으로 배치
        modifier = Modifier
            .fillMaxSize() // 화면 전체 크기를 채움
            .background(Color(0xFFF3F6FA)) // 전체 배경색 지정
            .padding(horizontal = 24.dp, vertical = 28.dp), // 화면 안쪽 여백 지정
        horizontalAlignment = Alignment.CenterHorizontally // 내부 요소들을 가로 기준 가운데 정렬
    ) {
        Spacer(modifier = Modifier.height(8.dp)) // 위쪽에 약간의 여백 추가

        Image( // 앱 로고 이미지 표시
            painter = painterResource(id = R.drawable.ic_spentopia_logo), // drawable에 있는 로고 이미지 불러오기
            contentDescription = "Spentopia Logo", // 접근성용 설명 텍스트
            modifier = Modifier.size(72.dp) // 이미지 크기 지정
        )

        Spacer(modifier = Modifier.height(16.dp)) // 로고와 제목 사이 여백

        Text( // 앱 이름 텍스트
            text = "Spentopia",
            fontSize = 24.sp, // 글자 크기
            fontWeight = FontWeight.Bold, // 굵은 글씨
            color = Color(0xFF111827) // 글자 색상
        )

        Spacer(modifier = Modifier.height(8.dp)) // 제목과 부제목 사이 여백

        Text( // 앱 슬로건 텍스트
            text = "내가 기록한 소비가 나를 만든다",
            fontSize = 14.sp, // 글자 크기
            color = Color(0xFF6B7280) // 회색 계열 글자색
        )

        Spacer(modifier = Modifier.height(34.dp)) // 상단 소개 영역과 입력창 영역 사이 여백

        Column( // 이메일/비밀번호 입력 영역을 묶는 세로 레이아웃
            modifier = Modifier.fillMaxWidth(), // 가로를 꽉 채움
            horizontalAlignment = Alignment.Start // 내부 텍스트와 입력창을 왼쪽 정렬
        ) {
            Text( // 이메일 라벨
                text = "이메일",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(8.dp)) // 라벨과 입력창 사이 여백

            OutlinedTextField( // 이메일 입력창
                value = email, // 현재 이메일 상태값 표시
                onValueChange = { email = it }, // 입력값이 바뀔 때마다 email 상태 업데이트
                modifier = Modifier
                    .fillMaxWidth() // 가로 전체 사용
                    .height(56.dp), // 입력창 높이 지정
                placeholder = { // 아무것도 입력하지 않았을 때 보이는 안내 문구
                    Text(
                        text = "your@email.com",
                        color = Color(0xFF9CA3AF)
                    )
                },
                singleLine = true, // 한 줄만 입력 가능
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 이메일 입력에 적합한 키보드 표시
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), // 모서리를 둥글게 설정
                colors = OutlinedTextFieldDefaults.colors( // 입력창 색상 관련 설정
                    focusedContainerColor = Color(0xFFF0F1F3), // 포커스 상태일 때 배경색
                    unfocusedContainerColor = Color(0xFFF0F1F3), // 포커스 아닐 때 배경색
                    focusedBorderColor = Color.Transparent, // 포커스 시 테두리를 투명하게 해서 안 보이게 함
                    unfocusedBorderColor = Color.Transparent, // 평소에도 테두리를 투명하게 함
                    cursorColor = Color(0xFF1D9BF0) // 커서 색상
                )
            )

            Spacer(modifier = Modifier.height(16.dp)) // 이메일 입력창과 비밀번호 라벨 사이 여백

            Text( // 비밀번호 라벨
                text = "비밀번호",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF222222)
            )

            Spacer(modifier = Modifier.height(8.dp)) // 라벨과 입력창 사이 여백

            OutlinedTextField( // 비밀번호 입력창
                value = password, // 현재 비밀번호 상태값 표시
                onValueChange = { password = it }, // 입력값이 바뀔 때마다 password 상태 업데이트
                modifier = Modifier
                    .fillMaxWidth() // 가로 전체 사용
                    .height(56.dp), // 입력창 높이 지정
                placeholder = { // 비밀번호 입력 전 안내 문구
                    Text(
                        text = "••••••••",
                        color = Color(0xFF9CA3AF)
                    )
                },
                singleLine = true, // 한 줄만 입력 가능
                visualTransformation = PasswordVisualTransformation(), // 비밀번호를 화면에 점으로 가려서 표시
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 비밀번호 입력용 키보드 설정
                shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), // 모서리를 둥글게 설정
                colors = OutlinedTextFieldDefaults.colors( // 입력창 색상 설정
                    focusedContainerColor = Color(0xFFF0F1F3), // 포커스 상태 배경색
                    unfocusedContainerColor = Color(0xFFF0F1F3), // 기본 배경색
                    focusedBorderColor = Color.Transparent, // 포커스 시 테두리 숨김
                    unfocusedBorderColor = Color.Transparent, // 기본 테두리 숨김
                    cursorColor = Color(0xFF1D9BF0) // 커서 색상
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp)) // 입력창 영역과 로그인 버튼 사이 여백

        GradientLoginButton( // 직접 만든 그라데이션 로그인 버튼 호출
            text = "로그인",
            onClick = onLoginClick
        )

        Spacer(modifier = Modifier.height(24.dp)) // 로그인 버튼과 구분선 사이 여백

        OrDivider() // "또는" 구분선 표시

        Spacer(modifier = Modifier.height(24.dp)) // 구분선과 소셜 로그인 버튼 사이 여백

        SocialLoginButton( // 카카오 로그인 버튼
            text = "카카오로 계속하기",
            iconRes = R.drawable.ic_kakao,
            onClick = onKakaoClick
        )

        Spacer(modifier = Modifier.height(12.dp)) // 버튼 사이 여백

        SocialLoginButton( // 네이버 로그인 버튼
            text = "네이버로 계속하기",
            iconRes = R.drawable.ic_naver,
            onClick = onNaverClick
        )

        Spacer(modifier = Modifier.height(12.dp)) // 버튼 사이 여백

        SocialLoginButton( // 구글 로그인 버튼
            text = "구글로 계속하기",
            iconRes = R.drawable.ic_google,
            onClick = onGoogleClick
        )

        Spacer(modifier = Modifier.height(28.dp)) // 소셜 로그인 영역과 회원가입 영역 사이 여백

        Row( // "계정이 없으신가요? 회원가입" 을 가로로 배치
            verticalAlignment = Alignment.CenterVertically, // 세로 기준 가운데 정렬
            horizontalArrangement = Arrangement.Center // 가로 정렬을 가운데 기준으로
        ) {
            Text( // 안내 문구 텍스트
                text = "계정이 없으신가요? ",
                color = Color(0xFF6B7280),
                fontSize = 14.sp
            )

            TextButton( // 회원가입 텍스트 버튼
                onClick = onSignUpClick, // 클릭 시 회원가입 이동 함수 실행
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp) // 텍스트 버튼의 기본 패딩 제거
            ) {
                Text( // 회원가입 버튼 안의 텍스트
                    text = "회원가입",
                    color = Color(0xFF1497E8), // 파란색 계열 강조
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold, // 약간 굵게
                    textDecoration = TextDecoration.None // 밑줄 없이 표시
                )
            }
        }
    }
}

@Composable // Compose UI 함수
private fun GradientLoginButton( // 로그인 화면에서만 사용할 내부 전용 버튼 함수
    text: String, // 버튼에 표시할 텍스트
    onClick: () -> Unit // 버튼 클릭 시 실행할 함수
) {
    Button( // Material3 기본 버튼 사용
        onClick = onClick, // 클릭 이벤트 연결
        modifier = Modifier
            .fillMaxWidth() // 가로 전체 사용
            .height(54.dp), // 버튼 높이 지정
        shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp), // 버튼 모서리를 둥글게 설정
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent // 기본 버튼 배경을 투명하게 해서 내부 Box의 그라데이션이 보이게 함
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues() // 버튼 기본 안쪽 여백 제거
    ) {
        Box( // 버튼 내부를 꽉 채우는 박스
            modifier = Modifier
                .fillMaxSize() // 버튼 내부 전체 크기 채우기
                .background(
                    brush = Brush.horizontalGradient( // 좌우 방향의 그라데이션 적용
                        colors = listOf(
                            Color(0xFF16B8D9), // 시작 색상
                            Color(0xFF2F7DF6) // 끝 색상
                        )
                    ),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp) // 배경도 버튼과 동일하게 둥글게 설정
                ),
            contentAlignment = Alignment.Center // 내부 텍스트를 정가운데 배치
        ) {
            Text( // 버튼 안의 글자
                text = text,
                color = Color.White, // 흰색 글자
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable // Compose UI 함수
private fun OrDivider() { // 로그인 방식 구분용 "또는" 선을 그리는 함수
    Row( // 왼쪽 선 + 텍스트 + 오른쪽 선을 가로 배치
        modifier = Modifier.fillMaxWidth(), // 가로 전체 사용
        verticalAlignment = Alignment.CenterVertically // 세로 가운데 정렬
    ) {
        HorizontalDivider( // 왼쪽 구분선
            modifier = Modifier.weight(1f), // 남는 공간을 균등하게 차지
            color = Color(0xFFD6D6D6) // 선 색상
        )

        Text( // 가운데 "또는" 텍스트
            text = "  또는  ",
            color = Color(0xFF8A8A8A),
            fontSize = 14.sp
        )

        HorizontalDivider( // 오른쪽 구분선
            modifier = Modifier.weight(1f), // 왼쪽과 동일 비율로 공간 차지
            color = Color(0xFFD6D6D6) // 선 색상
        )
    }
}