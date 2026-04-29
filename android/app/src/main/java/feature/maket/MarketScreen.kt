package com.ict.spentopia.feature.market

import androidx.compose.foundation.background // 수정: 배경 표현에 사용
import androidx.compose.foundation.layout.Arrangement // 수정: 정렬과 간격 지정에 사용
import androidx.compose.foundation.layout.Box // 수정: 강조 박스와 배지 표현에 사용
import androidx.compose.foundation.layout.Column // 수정: 세로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.ExperimentalLayoutApi // 수정: FlowRow 사용에
import androidx.compose.foundation.layout.FlowRow // 수정: 카드 줄바꿈 배치에 사용
import androidx.compose.foundation.layout.Row // 수정: 가로 레이아웃 구성에 사용
import androidx.compose.foundation.layout.Spacer // 수정: 여백 추가에 사용
import androidx.compose.foundation.layout.fillMaxWidth // 수정: 가로 전체 사용에 사용
import androidx.compose.foundation.layout.height // 수정: 높이 여백 지정에 사용
import androidx.compose.foundation.layout.padding // 수정: 내부 여백 적용에 사용
// 수정: 제목 영역 weight 사용을 위해 추가
import androidx.compose.foundation.rememberScrollState // 수정: 세로 스크롤 상태 기억에 사용
import androidx.compose.foundation.shape.RoundedCornerShape // 수정: 둥근 카드 모양에 사용
import androidx.compose.foundation.verticalScroll // 수정: 전체 화면 세로 스크롤에 사용
import androidx.compose.material3.Button // 수정: 액션 버튼에 사용
import androidx.compose.material3.ButtonDefaults // 수정: 버튼 색상 지정에 사용
import androidx.compose.material3.Card // 수정: 카드 UI 구성에 사용
import androidx.compose.material3.CardDefaults // 수정: 카드 스타일 지정에 사용
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text // 수정: 텍스트 출력에 사용
import androidx.compose.material3.TextButton // 수정: 탭 버튼에 사용
import androidx.compose.runtime.Composable // 기존 유지
import androidx.compose.runtime.getValue // 수정: delegate 사용을 위해 추가
import androidx.compose.runtime.mutableStateOf // 수정: 다이얼로그 상태 관리를 위해 추가
import androidx.compose.runtime.remember // 수정: 상태 기억을 위해 추가
import androidx.compose.runtime.setValue // 수정: delegate 사용을 위해 추가
import androidx.compose.ui.Alignment // 수정: 내부 정렬에 사용
import androidx.compose.ui.Modifier // 기존 유지
import androidx.compose.ui.graphics.Brush // 수정: 그라데이션 카드 배경에 사용
import androidx.compose.ui.graphics.Color // 수정: 색상 지정에 사용
import androidx.compose.ui.text.font.FontWeight // 수정: 제목 강조에 사용
import androidx.compose.ui.unit.dp // 기존 유지
import androidx.compose.ui.unit.sp // 수정: 폰트 크기 지정에 사용
import androidx.lifecycle.viewmodel.compose.viewModel // 수정: MarketViewModel을 화면에서 주입받기 위해 사용
import com.ict.spentopia.feature.auth.wallet.SolanaWalletDialog // 수정: 지갑 선택 다이얼로그 추가
import com.ict.spentopia.feature.auth.wallet.SolanaWalletType // 수정: 지갑 타입 열거형 추가

// 기존 주석 유지
// NFT 마켓 화면
@OptIn(ExperimentalLayoutApi::class) // 수정: FlowRow 사용을 위해 OptIn을 적용
@Composable
fun MarketScreen(
    // 바꿀 것 1: MarketScreen() 파라미터 추가
    isWalletConnected: Boolean = false,
    walletAddress: String = "",
    walletProvider: String = "",
    onWalletConnectClick: (SolanaWalletType) -> Unit = {}, // 수정: 지갑 연결 클릭 콜백 추가
    marketViewModel: MarketViewModel = viewModel() // 수정: 화면에서 사용할 MarketViewModel을 주입
) {
    val uiState = marketViewModel.uiState // 수정: ViewModel의 현재 UI 상태를 읽어옴

    // 수정: 지갑 연결 다이얼로그 표시 상태 관리
    var showWalletDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth() // 수정: 화면 전체 너비를 사용
            .verticalScroll(rememberScrollState()) // 수정: 마켓 화면 전체를 세로 스크롤 가능하게 설정
            .padding(vertical = 8.dp) // 수정: 상하 기본 여백을 적용함
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(), // 수정: 제목 영역과 버튼 영역을 한 줄에 배치함
            horizontalArrangement = Arrangement.SpaceBetween, // 수정: 양 끝 정렬을 적용함
            verticalAlignment = Alignment.Top // 수정: 상단 기준 정렬을 적용함
        ) {
            Column(
                modifier = Modifier.weight(1f) // 수정: 제목 영역이 남은 공간을 차지하도록 설정함
            ) {
                Text(
                    text = "NFT 마켓플레이스", // 수정: 화면 제목을 표시
                    fontSize = 28.sp, // 수정: 제목 크기를 지정
                    fontWeight = FontWeight.Bold, // 수정: 제목을 강조
                    color = MaterialTheme.colorScheme.onBackground // 수정: 제목 색상을 지정
                )

                Spacer(modifier = Modifier.height(4.dp)) // 수정: 제목과 설명 사이 여백을 추가

                Text(
                    text = "아바타 아이템을 자유롭게 거래해보세요", // 수정: 화면 설명 문구를 표시
                    fontSize = 15.sp, // 수정: 설명 텍스트 크기를 지정
                    color = MaterialTheme.colorScheme.onSurfaceVariant // 수정: 보조 텍스트 색상을 지정
                )
            }

            Spacer(modifier = Modifier.padding(horizontal = 6.dp)) // 수정: 제목 영역과 버튼 사이 간격을 추가

            Button(
                onClick = {
                    // 수정: 지갑이 연결되지 않았을 때만 다이얼로그를 띄움
                    if (!isWalletConnected) {
                        showWalletDialog = true
                    }
                },
                shape = RoundedCornerShape(12.dp), // 수정: 둥근 버튼 모양을 적용
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isWalletConnected) Color(0xFF16A34A) else Color(0xFFA855F7), // 수정: 연결 시 초록색, 미연결 시 보라색 적용
                    contentColor = Color.White // 수정: 흰색 버튼 텍스트를 적용
                )
            ) {
                // 바꿀 것 3: 상단 버튼 텍스트 분기
                Text(
                    text = if (isWalletConnected) "✅ ${walletProvider} 연결됨" else "👛 지갑 연결하기", // 수정: 연결 상태에 따른 텍스트 표시
                    fontSize = 13.sp, // 수정: 버튼 텍스트 크기를 지정
                    fontWeight = FontWeight.Bold // 수정: 버튼 텍스트를 강조
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 상단 영역과 경고 배너 사이 여백을 추가

        // 바꿀 것 4: 경고 배너를 연결 상태에 따라 교체
        if (!isWalletConnected) {
            Card(
                modifier = Modifier.fillMaxWidth(), // 수정: 경고 배너가 가로 전체를 사용하도록 설정
                shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드 모양을 적용
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)) // 수정: 연한 주황 배경을 적용
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용
                    verticalArrangement = Arrangement.spacedBy(6.dp) // 수정: 내부 항목 간격을 지정
                ) {
                    Text(
                        text = "⚠️ 지갑 연결이 필요해요", // 수정: 배너 제목을 표시
                        fontSize = 18.sp, // 수정: 제목 크기를 지정
                        fontWeight = FontWeight.Bold, // 수정: 제목을 강조
                        color = Color(0xFFEA580C) // 수정: 주황색 강조 텍스트를 적용
                    )

                    Text(
                        text = "지갑을 연결하면 아이템을 NFT로 발행하고 거래할 수 있어요. 지갑이 없어도 개인 소장은 가능합니다.", // 수정: 안내 문구를 표시
                        fontSize = 14.sp, // 수정: 설명 텍스트 크기를 지정
                        color = Color(0xFF9A3412) // 수정: 설명 텍스트 색상을 지정
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp)) // 수정: 배너 하단 여백 추가
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(), // 수정: 연결 완료 배너가 가로 전체를 사용하도록 설정
                shape = RoundedCornerShape(16.dp), // 수정: 둥근 카드 모양을 적용
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)) // 수정: 연한 초록 배경을 적용
            ) {
                Column(
                    modifier = Modifier.padding(16.dp), // 수정: 카드 내부 여백을 적용
                    verticalArrangement = Arrangement.spacedBy(6.dp) // 수정: 내부 항목 간격을 지정
                ) {
                    Text(
                        text = "✅ 지갑이 연결되어 있어요", // 수정: 연결 완료 제목을 표시
                        fontSize = 18.sp, // 수정: 제목 크기를 지정
                        fontWeight = FontWeight.Bold, // 수정: 제목을 강조
                        color = Color(0xFF15803D) // 수정: 초록색 강조 텍스트를 적용
                    )

                    Text(
                        text = "${walletProvider} · ${formatWalletAddress(walletAddress)}", // 수정: 연결된 지갑 정보와 포맷팅된 주소 표시
                        fontSize = 14.sp, // 수정: 설명 텍스트 크기를 지정
                        color = Color(0xFF166534) // 수정: 설명 텍스트 색상을 지정
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp)) // 수정: 배너 하단 여백 추가
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp), // 수정: 카드 사이 가로 간격을 지정
            verticalArrangement = Arrangement.spacedBy(12.dp) // 수정: 카드 사이 세로 간격을 지정
        ) {
            MarketStatCard(
                title = "총 거래량", // 수정: 첫 번째 통계 제목을 표시
                value = "12,345", // 수정: 첫 번째 통계 수치를 표시
                subText = "+15% 이번 주", // 수정: 첫 번째 통계 부가 설명을 표시
                bgColor = MaterialTheme.colorScheme.surface, // 수정: 기본 흰색 카드 배경을 적용
                subTextColor = Color(0xFF16A34A) // 수정: 상승 텍스트 색상을 적용
            )

            MarketStatCard(
                title = "등록된 아이템", // 수정: 두 번째 통계 제목을 표시
                value = "248", // 수정: 두 번째 통계 수치를 표시
                subText = "현재 판매중", // 수정: 두 번째 통계 부가 설명을 표시
                bgColor = MaterialTheme.colorScheme.surface, // 수정: 기본 흰색 카드 배경을 적용
                subTextColor = MaterialTheme.colorScheme.onSurfaceVariant // 수정: 기본 회색 설명 색상을 적용
            )

            MarketStatCard(
                title = "평균 거래가", // 수정: 세 번째 통계 제목을 표시
                value = "650 SPT", // 수정: 세 번째 통계 수치를 표시
                subText = "+8% 상승", // 수정: 세 번째 통계 부가 설명을 표시
                bgColor = MaterialTheme.colorScheme.surface, // 수정: 기본 흰색 카드 배경을 적용합
                subTextColor = Color(0xFF16A34A) // 수정: 상승 텍스트 색상을 적용
            )

            MarketStatCard(
                title = "내 잔액", // 수정: 네 번째 통계 제목을 표시
                value = "1,250 SPT", // 수정: 네 번째 통계 수치를 표시
                subText = "사용 가능", // 수정: 네 번째 통계 부가 설명을 표시.
                bgColor = Color.Transparent, // 수정: 내부 그라데이션을 사용하기 위해 투명 카드로 처리
                subTextColor = Color.White, // 수정: 흰색 설명 텍스트를 적용
                gradient = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFA855F7), // 수정: 시작 보라색을 적용
                        Color(0xFFEC4899) // 수정: 끝 핑크색을 적용
                    )
                ) // 수정: 잔액 카드를 그라데이션으로 강조
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 통계 카드와 상단 탭 사이 여백을 추가

        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant, // 수정: 연한 회색 탭 배경을 적용합
                    shape = RoundedCornerShape(999.dp) // 수정: 캡슐형 탭 배경을 적용합니다.
                )
                .padding(4.dp), // 수정: 탭 내부 여백을 적용합니다.
            horizontalArrangement = Arrangement.spacedBy(4.dp) // 수정: 탭 간 간격을 지정
        ) {
            MarketTabButton(
                text = "마켓", // 수정: 첫 번째 탭 텍스트를 표시합니다.
                selected = uiState.selectedTab == MarketTab.MARKET, // 수정: ViewModel의 현재 마켓 탭 선택 상태를 반영
                onClick = { marketViewModel.onTabChange(MarketTab.MARKET) } // 수정: 클릭 시 ViewModel을 통해 마켓 탭으로 전환
            )

            MarketTabButton(
                text = "내 판매 목록", // 수정: 두 번째 탭 텍스트를 표시
                selected = uiState.selectedTab == MarketTab.MY_SELL, // 수정: ViewModel의 현재 내 판매 목록 탭 상태를 반영
                onClick = { marketViewModel.onTabChange(MarketTab.MY_SELL) } // 수정: 클릭 시 ViewModel을 통해 내 판매 목록 탭으로 전환
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 탭과 검색 영역 사이 여백을 추가

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp), // 수정: 검색/필터 카드 사이 가로 간격을 지정
            verticalArrangement = Arrangement.spacedBy(10.dp) // 수정: 검색/필터 카드 사이 세로 간격을 지정
        ) {
            FilterBox(
                text = if (uiState.searchText.isBlank()) "🔍 아이템 검색..." else "🔍 ${uiState.searchText}", // 수정: 검색어 상태를 화면에 반영
                modifier = Modifier.fillMaxWidth(1f) // 수정: 검색창은 한 줄 전체를 먼저 사용하도록 설정
            )

            FilterBox(
                text = "전체 희귀도 ⌄", // 수정: 희귀도 필터 텍스트를 표시
                modifier = Modifier.fillMaxWidth(0.47f) // 수정: 필터 박스 너비를 설정
            )

            FilterBox(
                text = "최신순 ⌄", // 수정: 정렬 필터 텍스트를 표시
                modifier = Modifier.fillMaxWidth(0.47f) // 수정: 필터 박스 너비를 설정
            )
        }

        Spacer(modifier = Modifier.height(18.dp)) // 수정: 검색/필터와 아이템 리스트 사이 여백을 추가

        val itemsToShow = if (uiState.selectedTab == MarketTab.MARKET) uiState.marketItems else uiState.mySellItems // 수정: 현재 선택된 탭에 따라 ViewModel의 아이템 목록을 분기합니다.

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp), // 수정: 카드 사이 가로 간격을 지정
            verticalArrangement = Arrangement.spacedBy(14.dp) // 수정: 카드 사이 세로 간격을 지정
        ) {
            itemsToShow.forEach { item ->
                MarketItemCard(item = item) // 수정: 각 아이템 카드를 출력
            }
        }

        Spacer(modifier = Modifier.height(24.dp)) // 수정: 화면 하단 여백을 추가
    }

    // 수정: 지갑 선택 다이얼로그 표시 로직 추가
    if (showWalletDialog) {
        SolanaWalletDialog(
            onDismiss = {
                showWalletDialog = false
            },
            onSelectWallet = { walletType ->
                showWalletDialog = false
                onWalletConnectClick(walletType)
            }
        )
    }
}

// 바꿀 것 2: 주소 포맷 함수 추가
private fun formatWalletAddress(address: String): String {
    return if (address.length <= 10) address
    else "${address.take(4)}...${address.takeLast(4)}"
}

// 수정: 상단 탭 버튼 UI를 구성
@Composable
private fun MarketTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // ... (기존과 동일하므로 생략)
    TextButton(
        onClick = { onClick() },
        modifier = Modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
                shape = RoundedCornerShape(999.dp)
            )
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

// ... (이하 MarketStatCard, FilterBox, MarketItemCard, marketRarityColor 함수들은 기존 코드 유지)
@Composable
private fun MarketStatCard(
    title: String,
    value: String,
    subText: String,
    bgColor: Color,
    subTextColor: Color,
    gradient: Brush? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .background(
                    brush = gradient ?: Brush.linearGradient(listOf(bgColor, bgColor)),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = if (gradient != null) Color.White.copy(alpha = 0.9f) else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                fontSize = 24.sp,
                color = if (gradient != null) Color.White else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = subText,
                fontSize = 14.sp,
                color = subTextColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun FilterBox(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MarketItemCard(item: MarketItemUi) {
    Card(
        modifier = Modifier.fillMaxWidth(0.47f),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(
                        color = Color(0xFFF6EAF8),
                        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.emoji,
                    fontSize = 60.sp
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Box(
                        modifier = Modifier
                            .background(
                                color = marketRarityColor(item.rarity),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.rarity,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = item.seller,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🪙 ${item.price}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = item.time,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFEC4899),
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "구매하기",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun marketRarityColor(rarity: String): Color {
    return when (rarity) {
        "일반" -> MaterialTheme.colorScheme.onSurfaceVariant
        "레어" -> Color(0xFF3B82F6)
        "에픽" -> Color(0xFFA855F7)
        "전설" -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
