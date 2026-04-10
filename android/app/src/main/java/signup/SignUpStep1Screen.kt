package com.ict.spentopia.feature.signup // 회원가입 기능 관련 화면들이 들어있는 패키지를 선언

import androidx.compose.foundation.background // 배경색이나 배경 브러시를 적용할 때 사용하는 기능
import androidx.compose.foundation.clickable // 컴포넌트에 클릭 이벤트를 달 때 사용하는 기능
import androidx.compose.foundation.layout.Arrangement // Row, Column 내부 요소 배치 방식을 정할 때 사용
import androidx.compose.foundation.layout.Box // 요소를 겹치거나 하나의 박스 영역처럼 배치할 때 사용하는 레이아웃
import androidx.compose.foundation.layout.Column // 요소를 세로 방향으로 배치하는 레이아웃
import androidx.compose.foundation.layout.Row // 요소를 가로 방향으로 배치하는 레이아웃
import androidx.compose.foundation.layout.Spacer // 여백만 차지하는 빈 컴포넌트
import androidx.compose.foundation.layout.fillMaxSize // 부모가 허용하는 최대 크기만큼 꽉 채우는 Modifier
import androidx.compose.foundation.layout.fillMaxWidth // 가로 너비를 부모 기준 최대치로 채우는 Modifier
import androidx.compose.foundation.layout.height // 높이를 지정하는 Modifier
import androidx.compose.foundation.layout.padding // 내부 또는 외부 여백을 주는 Modifier
import androidx.compose.foundation.layout.width // 너비를 지정하는 Modifier
import androidx.compose.foundation.layout.size // 가로, 세로 크기를 한 번에 지정하는 Modifier
import androidx.compose.foundation.shape.RoundedCornerShape // 모서리를 둥글게 만드는 도형 클래스
import androidx.compose.foundation.text.KeyboardOptions // 입력창에서 키보드 종류 등의 옵션을 설정할 때 사용
import androidx.compose.material3.Button // Material3 스타일의 버튼 컴포넌트
import androidx.compose.material3.ButtonDefaults // 버튼의 기본 색상이나 설정을 바꿀 때 사용
import androidx.compose.material3.OutlinedTextField // 외곽선 기반 입력창 컴포넌트
import androidx.compose.material3.OutlinedTextFieldDefaults // OutlinedTextField의 기본 색상 설정용 객체
import androidx.compose.material3.Text // 글자를 화면에 출력하는 컴포넌트
import androidx.compose.runtime.Composable // Compose UI 함수임을 나타내는 어노테이션
import androidx.compose.runtime.getValue // by 문법으로 상태값을 읽기 위해 필요한 import
import androidx.compose.runtime.mutableStateOf // 상태(State) 객체를 생성하는 함수
import androidx.compose.runtime.remember // 재구성되어도 상태를 기억하도록 해주는 함수
import androidx.compose.runtime.setValue // by 문법으로 상태값을 변경하기 위해 필요한 import
import androidx.compose.ui.Alignment // 정렬 기준을 지정할 때 사용하는 클래스
import androidx.compose.ui.Modifier // 크기, 여백, 배경 등 UI 속성을 이어 붙이는 기본 도구
import androidx.compose.ui.graphics.Brush // 단색이 아닌 그라데이션 같은 브러시를 만들 때 사용
import androidx.compose.ui.graphics.Color // 색상 표현 클래스
import androidx.compose.ui.res.painterResource // drawable 리소스 이미지를 불러오는 함수
import androidx.compose.foundation.Image // 이미지를 화면에 표시하는 컴포넌트
import androidx.compose.ui.text.font.FontWeight // 글자 굵기를 설정할 때 사용하는 값들
import androidx.compose.ui.text.input.KeyboardType // 이메일, 숫자, 비밀번호 등 키보드 종류를 지정할 때 사용
import androidx.compose.ui.text.input.PasswordVisualTransformation // 비밀번호를 ●●● 형태로 숨겨 보여주는 변환기
import androidx.compose.ui.unit.dp // 크기와 여백을 dp 단위로 쓰기 위한 import
import androidx.compose.ui.unit.sp // 글자 크기를 sp 단위로 쓰기 위한 import
import com.ict.spentopia.R // drawable 등 앱 리소스에 접근하기 위한 클래스

@Composable // 이 함수가 Compose에서 화면을 그리는 함수라는 뜻
fun SignUpStep1Screen( // 회원가입 1단계 화면을 구성하는 함수 시작
    onNextClick: () -> Unit, // "다음" 버튼을 눌렀을 때 실행할 함수
    onBackClick: () -> Unit // "로그인" 텍스트를 눌렀을 때 실행할 함수
) { // 함수 본문 시작
    var email by remember { mutableStateOf("") } // 이메일 입력값을 저장하는 상태 변수, 처음 값은 빈 문자열
    var password by remember { mutableStateOf("") } // 비밀번호 입력값을 저장하는 상태 변수, 처음 값은 빈 문자열
    var passwordCheck by remember { mutableStateOf("") } // 비밀번호 확인 입력값을 저장하는 상태 변수, 처음 값은 빈 문자열

    Column( // 화면 전체를 세로 방향으로 배치하는 최상위 레이아웃
        modifier = Modifier // 아래에서 이 Column에 적용할 속성들을 연결 시작
            .fillMaxSize() // 화면 전체 크기를 꽉 채움
            .background(Color(0xFFF3F6FA)) // 전체 배경색을 연한 회색빛 파란색으로 설정
            .padding(horizontal = 24.dp, vertical = 32.dp), // 좌우 24dp, 상하 32dp 여백을 줌
        horizontalAlignment = Alignment.CenterHorizontally // Column 내부 요소들을 가로 기준 가운데 정렬
    ) { // Column 내용 시작
        Spacer(modifier = Modifier.height(40.dp)) // 맨 위에 40dp만큼 빈 공간을 만들어 위쪽 여백 확보

        Image( // 앱 로고 이미지를 화면에 표시
            painter = painterResource(id = R.drawable.ic_spentopia_logo), // drawable 폴더의 ic_spentopia_logo 이미지를 불러옴
            contentDescription = "Spentopia Logo", // 접근성(스크린리더)용 설명 문구
            modifier = Modifier.size(90.dp) // 로고 크기를 가로세로 90dp로 설정
        ) // Image 끝

        Spacer(modifier = Modifier.height(24.dp)) // 로고 아래에 24dp 여백 추가

        Text( // "회원가입" 제목 텍스트 출력
            text = "회원가입", // 화면에 표시할 실제 문자열
            fontSize = 28.sp, // 글자 크기를 28sp로 설정
            fontWeight = FontWeight.Bold, // 글자를 굵게 표시
            color = Color(0xFF163D8F) // 진한 파란색 계열 글자색 지정
        ) // 제목 Text 끝

        Spacer(modifier = Modifier.height(10.dp)) // 제목과 단계 표시 사이에 10dp 여백 추가

        Text( // 현재 회원가입 진행 단계를 보여주는 텍스트
            text = "Step 1 / 3", // 3단계 중 1단계라는 뜻의 문자열
            fontSize = 16.sp, // 글자 크기 16sp
            color = Color(0xFF3C6DD8), // 조금 더 밝은 파란색 계열 글자색
            fontWeight = FontWeight.Medium // 보통보다 약간 굵은 글자 두께
        ) // 단계 표시 Text 끝

        Spacer(modifier = Modifier.height(28.dp)) // 단계 텍스트와 진행 바 사이 여백 추가

        Row( // 가로 방향으로 3개의 진행 상태 바를 배치하는 레이아웃
            modifier = Modifier.fillMaxWidth(), // Row의 가로 길이를 부모 너비만큼 꽉 채움
            horizontalArrangement = Arrangement.SpaceBetween // 내부 요소들을 좌우로 균등하게 떨어뜨려 배치
        ) { // Row 내용 시작
            Box( // 첫 번째 진행 바 칸을 표시하는 박스
                modifier = Modifier // Box에 적용할 속성 시작
                    .weight(1f) // Row 안에서 동일 비율로 공간을 차지하도록 설정
                    .height(8.dp) // 높이를 8dp로 지정
                    .background( // 배경 설정 시작
                        color = Color(0xFF2F80ED), // 현재 단계이므로 파란색으로 표시
                        shape = RoundedCornerShape(10.dp) // 양 끝이 둥글게 보이도록 모서리 10dp 설정
                    ) // background 끝
            ) // 첫 번째 Box 끝

            Spacer(modifier = Modifier.width(8.dp)) // 첫 번째와 두 번째 진행 바 사이 8dp 간격

            Box( // 두 번째 진행 바 칸을 표시하는 박스
                modifier = Modifier // Box에 적용할 속성 시작
                    .weight(1f) // 첫 번째와 같은 비율의 너비를 차지
                    .height(8.dp) // 높이를 8dp로 지정
                    .background( // 배경 설정 시작
                        color = Color(0xFFD5DDE8), // 아직 진행 전 단계라 회색 계열로 표시
                        shape = RoundedCornerShape(10.dp) // 둥근 모서리 적용
                    ) // background 끝
            ) // 두 번째 Box 끝

            Spacer(modifier = Modifier.width(8.dp)) // 두 번째와 세 번째 진행 바 사이 8dp 간격

            Box( // 세 번째 진행 바 칸을 표시하는 박스
                modifier = Modifier // Box에 적용할 속성 시작
                    .weight(1f) // 나머지 두 칸과 같은 비율의 너비 차지
                    .height(8.dp) // 높이를 8dp로 지정
                    .background( // 배경 설정 시작
                        color = Color(0xFFD5DDE8), // 아직 진행 전 단계라 회색 계열 사용
                        shape = RoundedCornerShape(10.dp) // 둥근 모서리 적용
                    ) // background 끝
            ) // 세 번째 Box 끝
        } // Row 끝

        Spacer(modifier = Modifier.height(36.dp)) // 진행 바와 입력 영역 사이 36dp 여백

        Column( // 입력 필드들을 세로로 묶는 레이아웃
            modifier = Modifier.fillMaxWidth() // 입력 영역이 화면 가로를 꽉 채우도록 설정
        ) { // 입력 Column 시작
            Text( // 이메일 입력창 위 라벨 텍스트
                text = "이메일", // 라벨 문구
                fontSize = 15.sp, // 글자 크기 15sp
                fontWeight = FontWeight.SemiBold, // 반굵기보다 약간 더 굵은 스타일
                color = Color(0xFF163D8F) // 파란색 계열 라벨 색상
            ) // 이메일 라벨 끝

            Spacer(modifier = Modifier.height(10.dp)) // 라벨과 입력창 사이 10dp 여백

            OutlinedTextField( // 이메일을 입력받는 입력창
                value = email, // 현재 입력창에 보여줄 값은 email 상태값
                onValueChange = { email = it }, // 사용자가 입력할 때마다 email 변수 값을 새 문자열로 변경
                modifier = Modifier // 입력창에 적용할 속성 시작
                    .fillMaxWidth() // 가로를 부모만큼 꽉 채움
                    .height(56.dp), // 입력창 높이를 56dp로 지정
                placeholder = { // 값이 비어 있을 때 보일 힌트 문구 영역
                    Text( // placeholder 안에 보여줄 텍스트
                        text = "your@email.com", // 이메일 입력 예시 문구
                        color = Color(0xFF6C84B5) // 힌트 텍스트 색상
                    ) // placeholder Text 끝
                }, // placeholder 끝
                singleLine = true, // 여러 줄 입력이 아니라 한 줄 입력만 허용
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), // 이메일 입력에 적합한 키보드를 띄움
                shape = RoundedCornerShape(10.dp), // 입력창 모서리를 10dp 둥글게 설정
                colors = OutlinedTextFieldDefaults.colors( // 입력창 색상 설정
                    focusedContainerColor = Color(0xFFE9EFF6), // 포커스(선택) 되었을 때 배경색
                    unfocusedContainerColor = Color(0xFFE9EFF6), // 포커스되지 않았을 때도 같은 배경색 사용
                    focusedBorderColor = Color.Transparent, // 포커스 시 테두리를 투명하게 만들어 안 보이게 함
                    unfocusedBorderColor = Color.Transparent, // 포커스 전에도 테두리를 안 보이게 함
                    cursorColor = Color(0xFF2F80ED) // 입력 커서 색상을 파란색으로 설정
                ) // colors 끝
            ) // 이메일 OutlinedTextField 끝

            Spacer(modifier = Modifier.height(20.dp)) // 이메일 입력창과 비밀번호 라벨 사이 20dp 여백

            Text( // 비밀번호 입력창 위 라벨 텍스트
                text = "비밀번호", // 라벨 문구
                fontSize = 15.sp, // 글자 크기 15sp
                fontWeight = FontWeight.SemiBold, // 글자 굵기를 SemiBold로 설정
                color = Color(0xFF163D8F) // 라벨 색상을 진한 파란색으로 설정
            ) // 비밀번호 라벨 끝

            Spacer(modifier = Modifier.height(10.dp)) // 라벨과 입력창 사이 여백

            OutlinedTextField( // 비밀번호를 입력받는 입력창
                value = password, // 현재 입력창에 표시할 값은 password 상태값
                onValueChange = { password = it }, // 사용자가 입력한 값으로 password 상태를 갱신
                modifier = Modifier // 입력창 속성 시작
                    .fillMaxWidth() // 가로를 꽉 채움
                    .height(56.dp), // 높이 56dp
                placeholder = { // 입력값이 비어 있을 때 보여주는 힌트
                    Text( // placeholder 텍스트
                        text = "8자 이상 입력해주세요", // 비밀번호 입력 규칙 힌트
                        color = Color(0xFF6C84B5) // 힌트 색상
                    ) // placeholder Text 끝
                }, // placeholder 끝
                singleLine = true, // 한 줄만 입력 가능
                visualTransformation = PasswordVisualTransformation(), // 입력한 비밀번호를 ●●● 형태로 숨겨서 표시
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 비밀번호 입력용 키보드 옵션 사용
                shape = RoundedCornerShape(10.dp), // 모서리 둥글게
                colors = OutlinedTextFieldDefaults.colors( // 색상 설정
                    focusedContainerColor = Color(0xFFE9EFF6), // 포커스된 상태 배경색
                    unfocusedContainerColor = Color(0xFFE9EFF6), // 포커스되지 않은 상태 배경색
                    focusedBorderColor = Color.Transparent, // 포커스 시 테두리 숨김
                    unfocusedBorderColor = Color.Transparent, // 평소 테두리 숨김
                    cursorColor = Color(0xFF2F80ED) // 커서 색상
                ) // colors 끝
            ) // 비밀번호 입력창 끝

            Spacer(modifier = Modifier.height(20.dp)) // 비밀번호 입력창과 비밀번호 확인 라벨 사이 여백

            Text( // 비밀번호 확인 입력창 위 라벨 텍스트
                text = "비밀번호 확인", // 라벨 문구
                fontSize = 15.sp, // 글자 크기 15sp
                fontWeight = FontWeight.SemiBold, // 글자 굵기 SemiBold
                color = Color(0xFF163D8F) // 라벨 글자색
            ) // 비밀번호 확인 라벨 끝

            Spacer(modifier = Modifier.height(10.dp)) // 라벨과 입력창 사이 여백

            OutlinedTextField( // 비밀번호를 한 번 더 입력받는 입력창
                value = passwordCheck, // 현재 입력창 값은 passwordCheck 상태값
                onValueChange = { passwordCheck = it }, // 사용자가 입력할 때마다 passwordCheck 상태 갱신
                modifier = Modifier // 입력창 속성 시작
                    .fillMaxWidth() // 가로 전체 사용
                    .height(56.dp), // 높이 56dp
                placeholder = { // 비어 있을 때 보일 힌트
                    Text( // placeholder 텍스트
                        text = "비밀번호를 다시 입력해주세요", // 안내 문구
                        color = Color(0xFF6C84B5) // 힌트 색상
                    ) // placeholder Text 끝
                }, // placeholder 끝
                singleLine = true, // 한 줄 입력만 허용
                visualTransformation = PasswordVisualTransformation(), // 입력한 내용을 ●●● 형태로 숨김 처리
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), // 비밀번호 입력용 키보드 설정
                shape = RoundedCornerShape(10.dp), // 둥근 모서리 적용
                colors = OutlinedTextFieldDefaults.colors( // 색상 설정
                    focusedContainerColor = Color(0xFFE9EFF6), // 포커스 상태 배경색
                    unfocusedContainerColor = Color(0xFFE9EFF6), // 비포커스 상태 배경색
                    focusedBorderColor = Color.Transparent, // 포커스 상태 테두리 숨김
                    unfocusedBorderColor = Color.Transparent, // 기본 테두리 숨김
                    cursorColor = Color(0xFF2F80ED) // 커서 색상 파란색
                ) // colors 끝
            ) // 비밀번호 확인 입력창 끝
        } // 입력 Column 끝

        Spacer(modifier = Modifier.height(32.dp)) // 입력 영역과 다음 버튼 사이 32dp 여백

        Button( // "다음" 버튼 컴포넌트 시작
            onClick = onNextClick, // 버튼 클릭 시 외부에서 전달받은 onNextClick 함수 실행
            modifier = Modifier // 버튼에 적용할 속성 시작
                .fillMaxWidth() // 버튼 가로 길이를 꽉 채움
                .height(56.dp), // 버튼 높이를 56dp로 지정
            shape = RoundedCornerShape(12.dp), // 버튼 모서리를 12dp만큼 둥글게 설정
            colors = ButtonDefaults.buttonColors( // 버튼 색상 설정 시작
                containerColor = Color.Transparent // 기본 버튼 배경을 투명으로 두어 내부 Box 그라데이션이 보이게 함
            ), // colors 끝
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp) // 버튼 내부 기본 패딩을 0으로 제거
        ) { // Button 내용 시작
            Box( // 버튼 내부를 꽉 채우는 박스
                modifier = Modifier // Box 속성 시작
                    .fillMaxSize() // 버튼 내부 전체를 꽉 채움
                    .background( // 배경 설정 시작
                        brush = Brush.horizontalGradient( // 왼쪽에서 오른쪽으로 이어지는 가로 그라데이션 적용
                            colors = listOf( // 그라데이션에 사용할 색상 리스트
                                Color(0xFF16B8D9), // 시작 색상
                                Color(0xFF2F7DF6) // 끝 색상
                            ) // 리스트 끝
                        ), // brush 끝
                        shape = RoundedCornerShape(12.dp) // 배경도 버튼과 동일하게 둥근 모서리 적용
                    ), // background 끝
                contentAlignment = Alignment.Center // Box 내부의 텍스트를 정가운데 정렬
            ) { // Box 내용 시작
                Text( // 버튼 안에 표시될 텍스트
                    text = "다음", // 버튼 문구
                    color = Color.White, // 글자색 흰색
                    fontSize = 18.sp, // 글자 크기 18sp
                    fontWeight = FontWeight.Bold // 굵은 글씨
                ) // 버튼 텍스트 끝
            } // Box 끝
        } // Button 끝

        Spacer(modifier = Modifier.height(28.dp)) // 다음 버튼과 하단 로그인 안내 사이 여백

        Row { // "이미 계정이 있으신가요? 로그인" 문구를 가로로 배치하는 레이아웃
            Text( // 앞부분 안내 텍스트
                text = "이미 계정이 있으신가요? ", // 안내 문구
                color = Color(0xFF163D8F), // 글자색
                fontSize = 14.sp // 글자 크기
            ) // 안내 텍스트 끝

            Text( // 클릭 가능한 "로그인" 텍스트
                text = "로그인", // 표시 문구
                color = Color(0xFF163D8F), // 글자색
                fontSize = 14.sp, // 글자 크기
                fontWeight = FontWeight.Bold, // 굵은 글씨로 강조
                modifier = Modifier.clickable { onBackClick() } // 클릭하면 onBackClick 함수를 실행해서 이전 화면 등으로 이동
            ) // 로그인 텍스트 끝
        } // Row 끝
    } // 최상위 Column 끝
} // SignUpStep1Screen 함수 끝