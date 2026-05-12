package com.ict.spentopia.feature.auth.wallet // 이 파일이 속한 패키지 위치를 적음

import androidx.compose.foundation.BorderStroke // BorderStroke 기능을 가져옴
import androidx.compose.foundation.clickable // clickable 기능을 가져옴
import androidx.compose.foundation.layout.Arrangement // Arrangement 기능을 가져옴
import androidx.compose.foundation.layout.Column // 세로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Row // 가로 배치 레이아웃을 가져옴
import androidx.compose.foundation.layout.Spacer // Spacer 기능을 가져옴
import androidx.compose.foundation.layout.fillMaxWidth // fillMaxWidth 기능을 가져옴
import androidx.compose.foundation.layout.height // height 기능을 가져옴
import androidx.compose.foundation.layout.padding // padding 기능을 가져옴
import androidx.compose.foundation.shape.RoundedCornerShape // RoundedCornerShape 기능을 가져옴
import androidx.compose.material3.Card // Card 기능을 가져옴
import androidx.compose.material3.CardDefaults // CardDefaults 기능을 가져옴
import androidx.compose.material3.MaterialTheme // MaterialTheme 기능을 가져옴
import androidx.compose.material3.Text // 글자 표시 컴포넌트를 가져옴
import androidx.compose.runtime.Composable // Compose 화면 함수 표시를 가져옴
import androidx.compose.ui.Modifier // UI 크기랑 여백 설정 도구를 가져옴
import androidx.compose.ui.text.font.FontWeight // FontWeight 기능을 가져옴
import androidx.compose.ui.unit.dp // 화면 크기 단위를 가져옴
import androidx.compose.ui.unit.sp // 글자 크기 단위를 가져옴
import androidx.compose.ui.window.Dialog // Dialog 기능을 가져옴
import com.ict.spentopia.ui.theme.SpentopiaMutedPurple // SpentopiaMutedPurple 기능을 가져옴

@Composable // 이 함수가 화면 UI를 그린다는 표시
fun SolanaWalletDialog( // SolanaWalletDialog 함수를 선언함
    onDismiss: () -> Unit, // 닫을 때 실행할 함수를 받음
    onSelectWallet: (SolanaWalletType) -> Unit // 지갑 관련 값을 받음
) { // 이 블록 안의 내용이 시작됨
    Dialog(onDismissRequest = onDismiss) { // Dialog(onDismissRequest 값을 정해줌
        Card( // 내용을 카드 모양으로 묶어서 보여줌
            modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
            shape = RoundedCornerShape(24.dp), // shape 값을 정해줌
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface) // colors 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 24.dp) // .padding(horizontal 값을 정해줌
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = "Solana 지갑 연결", // text 값을 정해줌
                    fontSize = 18.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(18.dp)) // UI 크기나 여백 같은 모양을 정함

                WalletOptionItem( // 지갑 관련 함수를 실행함
                    title = SolanaWalletType.PHANTOM.title, // 제목을 정해줌
                    description = SolanaWalletType.PHANTOM.description, // description 값을 정해줌
                    onClick = { onSelectWallet(SolanaWalletType.PHANTOM) } // 눌렀을 때 실행할 함수를 정해줌
                )

                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

                WalletOptionItem( // 지갑 관련 함수를 실행함
                    title = SolanaWalletType.SOLFLARE.title, // 제목을 정해줌
                    description = SolanaWalletType.SOLFLARE.description, // description 값을 정해줌
                    onClick = { onSelectWallet(SolanaWalletType.SOLFLARE) } // 눌렀을 때 실행할 함수를 정해줌
                )

                Spacer(modifier = Modifier.height(12.dp)) // UI 크기나 여백 같은 모양을 정함

                WalletOptionItem( // 지갑 관련 함수를 실행함
                    title = SolanaWalletType.BACKPACK.title, // 제목을 정해줌
                    description = SolanaWalletType.BACKPACK.description, // description 값을 정해줌
                    onClick = { onSelectWallet(SolanaWalletType.BACKPACK) } // 눌렀을 때 실행할 함수를 정해줌
                )

                Spacer(modifier = Modifier.height(14.dp)) // UI 크기나 여백 같은 모양을 정함

                Row( // 안쪽 UI를 가로로 배치함
                    modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
                    horizontalArrangement = Arrangement.End // horizontalArrangement 값을 정해줌
                ) { // 이 블록 안의 내용이 시작됨
                    Text( // 화면에 글자를 보여줌
                        text = "닫기", // text 값을 정해줌
                        color = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 color 값에 넣음
                        fontSize = 16.sp, // fontSize 값을 정해줌
                        fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                        modifier = Modifier.clickable { onDismiss() } // UI 크기나 여백 같은 모양을 정함
                    )
                }
            }
        }
    }
}

@Composable // 이 함수가 화면 UI를 그린다는 표시
private fun WalletOptionItem( // WalletOptionItem 함수를 선언함
    title: String, // 제목을 받음
    description: String, // description 값을 받음
    onClick: () -> Unit // 눌렀을 때 실행할 함수를 받음
) { // 이 블록 안의 내용이 시작됨
    Card( // 내용을 카드 모양으로 묶어서 보여줌
        modifier = Modifier.fillMaxWidth(), // UI 크기나 여백 같은 모양을 정함
        shape = RoundedCornerShape(16.dp), // shape 값을 정해줌
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // colors 값을 정해줌
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), // border 값을 정해줌
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // elevation 값을 정해줌
    ) { // 이 블록 안의 내용이 시작됨
        Row( // 안쪽 UI를 가로로 배치함
            modifier = Modifier // UI 크기나 여백 같은 모양을 정함
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp), // .padding(horizontal 값을 정해줌
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically // verticalAlignment 값을 정해줌
        ) { // 이 블록 안의 내용이 시작됨
            Column( // 안쪽 UI를 세로로 배치함
                modifier = Modifier.weight(1f) // UI 크기나 여백 같은 모양을 정함
            ) { // 이 블록 안의 내용이 시작됨
                Text( // 화면에 글자를 보여줌
                    text = title, // 제목을 text 값에 넣음
                    fontSize = 16.sp, // fontSize 값을 정해줌
                    fontWeight = FontWeight.Bold, // fontWeight 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurface // color 값을 정해줌
                )

                Spacer(modifier = Modifier.height(4.dp)) // UI 크기나 여백 같은 모양을 정함

                Text( // 화면에 글자를 보여줌
                    text = description, // description 값을 text 값에 넣음
                    fontSize = 13.sp, // fontSize 값을 정해줌
                    color = MaterialTheme.colorScheme.onSurfaceVariant // color 값을 정해줌
                )
            }

            Text( // 화면에 글자를 보여줌
                text = "연결", // text 값을 정해줌
                color = SpentopiaMutedPurple, // SpentopiaMutedPurple 값을 color 값에 넣음
                fontSize = 16.sp, // fontSize 값을 정해줌
                fontWeight = FontWeight.SemiBold, // fontWeight 값을 정해줌
                modifier = Modifier.clickable { onClick() } // UI 크기나 여백 같은 모양을 정함
            )
        }
    }
}
